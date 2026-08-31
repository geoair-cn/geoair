package cn.geoair.base.log.processor;

import cn.geoair.base.log.GirLog4j;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

/**
 * {@link GirLog4j} 注解处理器，在编译期向被标注的类中自动注入日志字段。
 *
 * <p>实现原理与 Lombok 一致：在 javac 的注解处理阶段，通过 {@code com.sun.tools.javac} 内部 API
 * 直接修改被标注类的语法树（AST），追加如下字段：
 *
 * <pre>
 * private static GiLogger log = GirLoggerFactory.getLogger(当前类.class);
 * </pre>
 *
 * <p>为避免编译期对 {@code tools.jar} 的依赖，内部 API 全部通过反射调用（不 import 任何 {@code com.sun.tools.javac} 类），从而在
 * JDK 8 ~ JDK 17 上均可用； JDK 16+ 因模块强封装，运行 javac 时需添加 {@code --add-opens
 * jdk.compiler/com.sun.tools.javac.*=ALL-UNNAMED}，失败时本处理器会输出明确的编译错误提示。
 *
 * @author geoair
 * @since J8-dev-SNAPSHOT
 * @see GirLog4j
 */
public class GirLog4jProcessor extends AbstractProcessor {

    /** 日志接口全限定名 */
    private static final String GI_LOGGER = "cn.geoair.base.log.GiLogger";

    /** 日志工厂全限定名 */
    private static final String GIR_LOGGER_FACTORY = "cn.geoair.base.log.GirLoggerFactory";

    private static final String JPE_CLASS =
            "com.sun.tools.javac.processing.JavacProcessingEnvironment";
    private static final String TREE_MAKER_CLASS = "com.sun.tools.javac.tree.TreeMaker";
    private static final String NAMES_CLASS = "com.sun.tools.javac.util.Names";
    private static final String CONTEXT_CLASS = "com.sun.tools.javac.util.Context";
    private static final String FLAGS_CLASS = "com.sun.tools.javac.code.Flags";
    private static final String JAVAC_ELEMENTS_CLASS = "com.sun.tools.javac.model.JavacElements";
    private static final String UTIL_LIST_CLASS = "com.sun.tools.javac.util.List";

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(GirLog4j.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(GirLog4j.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                error(element, "@GirLog4j 只能标注在类上，当前标注在 [" + element.getKind() + "] 上，已忽略");
                continue;
            }
            try {
                injectLogField((TypeElement) element);
            } catch (Throwable e) {
                error(
                        element,
                        "自动注入日志字段失败: "
                                + e.getClass().getSimpleName()
                                + ": "
                                + e.getMessage()
                                + "。可手动添加字段替代；若错误与模块访问有关，请为 javac 添加 --add-opens"
                                + " jdk.compiler/com.sun.tools.javac.*=ALL-UNNAMED");
            }
        }
        return false;
    }

    /**
     * 向被标注的类注入日志字段。
     *
     * @param typeElement 被 {@link GirLog4j} 标注的类
     */
    private void injectLogField(TypeElement typeElement) throws Exception {
        GirLog4j annotation = typeElement.getAnnotation(GirLog4j.class);
        String fieldName = annotation.fieldName();

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD
                    && enclosed.getSimpleName().contentEquals(fieldName)) {
                return;
            }
        }

        Object treeMaker = instance(TREE_MAKER_CLASS, "instance", context(processingEnv));
        Object names = instance(NAMES_CLASS, "instance", context(processingEnv));

        Object typeExpr = chainDots(treeMaker, names, GI_LOGGER);
        Object init;
        if (annotation.topic().isEmpty()) {
            Object classLiteral = invoke(treeMaker, "ClassLiteral", typeElement.asType());
            init = makeApply(treeMaker, names, GIR_LOGGER_FACTORY, "getLogger", classLiteral);
        } else {
            Object literal = invoke(treeMaker, "Literal", (Object) annotation.topic());
            init = makeApply(treeMaker, names, GIR_LOGGER_FACTORY, "getLogger", literal);
        }

        long flags = flag("PRIVATE") | flag("STATIC");
        if (annotation.useFinal()) {
            flags |= flag("FINAL");
        }
        Object mods = invoke(treeMaker, "Modifiers", flags);
        Object name = invoke(names, "fromString", fieldName);
        Object varDef = invoke(treeMaker, "VarDef", mods, name, typeExpr, init);

        Object classDecl = getTree(typeElement);
        Field defsField = classDecl.getClass().getField("defs");
        Object defs = defsField.get(classDecl);
        defsField.set(classDecl, invoke(defs, "append", varDef));
    }

    /** 构造形如 {@code getLogger(...)} 的方法调用树。 */
    private static Object makeApply(
            Object treeMaker, Object names, String factoryFqn, String methodName, Object arg)
            throws Exception {
        Object factoryExpr = chainDots(treeMaker, names, factoryFqn);
        Object methodExpr =
                invoke(treeMaker, "Select", factoryExpr, invoke(names, "fromString", methodName));
        Object nil = invokeStatic(UTIL_LIST_CLASS, "nil");
        Object argList = invoke(nil, "append", arg);
        return invoke(treeMaker, "Apply", nil, methodExpr, argList);
    }

    /** 按点分全限定名构造 {@code Select} 链（如 {@code cn.geoair.base.log.GiLogger}）。 */
    private static Object chainDots(Object treeMaker, Object names, String fqn) throws Exception {
        String[] parts = fqn.split("\\.");
        Object expr = invoke(treeMaker, "Ident", invoke(names, "fromString", parts[0]));
        for (int i = 1; i < parts.length; i++) {
            expr = invoke(treeMaker, "Select", expr, invoke(names, "fromString", parts[i]));
        }
        return expr;
    }

    /** 获取被标注类对应的语法树节点（JCClassDecl）。 */
    private Object getTree(TypeElement typeElement) throws Exception {
        Object javacElements = processingEnv.getElementUtils();
        return invoke(javacElements, "getTree", (Element) typeElement);
    }

    /** 从注解处理环境中获取 javac 的 Context。 */
    private static Object context(ProcessingEnvironment processingEnv) throws Exception {
        return invoke(processingEnv, "getContext");
    }

    /** 获取 javac 内部标记常量（Flags.PRIVATE / STATIC / FINAL）。 */
    private static long flag(String name) throws Exception {
        return Class.forName(FLAGS_CLASS).getField(name).getInt(null);
    }

    /** 反射调用静态工厂方法：Class.instance(Context)。 */
    private static Object instance(String className, String methodName, Object arg)
            throws Exception {
        return invokeStatic(className, methodName, arg);
    }

    private static Object invokeStatic(String className, String methodName, Object... args)
            throws Exception {
        return findMethod(Class.forName(className), methodName, args).invoke(null, args);
    }

    /** 反射调用实例方法，按方法名 + 参数个数 + 参数类型兼容匹配重载。 */
    private static Object invoke(Object target, String methodName, Object... args)
            throws Exception {
        return findMethod(target.getClass(), methodName, args).invoke(target, args);
    }

    /** 按方法名、参数个数、参数运行时类型查找最匹配的方法（兼容基本类型拆箱）。 */
    private static Method findMethod(Class<?> clazz, String name, Object... args)
            throws NoSuchMethodException {
        for (Method method : clazz.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            boolean matched = true;
            for (int i = 0; i < types.length; i++) {
                if (args[i] != null && !matches(types[i], args[i].getClass())) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return method;
            }
        }
        throw new NoSuchMethodException(clazz.getName() + "#" + name + "(" + args.length + " 个参数)");
    }

    /** 参数类型是否兼容，基本类型按包装类拆箱匹配。 */
    private static boolean matches(Class<?> paramType, Class<?> argType) {
        if (paramType.isPrimitive()) {
            return (paramType == int.class && argType == Integer.class)
                    || (paramType == long.class && argType == Long.class)
                    || (paramType == boolean.class && argType == Boolean.class)
                    || (paramType == char.class && argType == Character.class)
                    || (paramType == byte.class && argType == Byte.class)
                    || (paramType == short.class && argType == Short.class)
                    || (paramType == float.class && argType == Float.class)
                    || (paramType == double.class && argType == Double.class);
        }
        return paramType.isAssignableFrom(argType);
    }

    /** 输出编译错误。 */
    private void error(Element element, String message) {
        processingEnv
                .getMessager()
                .printMessage(javax.tools.Diagnostic.Kind.ERROR, message, element);
    }
}
