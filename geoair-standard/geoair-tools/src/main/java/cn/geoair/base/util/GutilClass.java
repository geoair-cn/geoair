package cn.geoair.base.util;

import cn.geoair.base.bean.GkNullWrapperBean;
import cn.geoair.base.lang.GkBasicType;
import cn.geoair.base.tool.GkConcurrentReferenceHashMap;
import java.beans.Introspector;
import java.io.Closeable;
import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.security.ProtectionDomain;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 来自 spring ClassUtil
 *
 * @author
 */
public abstract class GutilClass {

    /** 数组类名的后缀："[]" */
    public static final String ARRAY_SUFFIX = "[]";

    /** 内部数组类名的前缀："[" */
    private static final String INTERNAL_ARRAY_PREFIX = "[";

    /** 内部非原始类型数组类名的前缀："[L" */
    private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";

    /** 包分隔符：'.' */
    private static final char PACKAGE_SEPARATOR = '.';

    /** 路径分隔符：'/' */
    private static final char PATH_SEPARATOR = '/';

    /** 内部类分隔符：'$' */
    private static final char INNER_CLASS_SEPARATOR = '$';

    /** CGLIB类分隔符："$$" */
    public static final String CGLIB_CLASS_SEPARATOR = "$$";

    /** LAMBDA类分隔符："$$Lambda" */
    public static final String LAMBDA_CLASS_SIGN = "$$Lambda";

    /** ".class"文件后缀 */
    public static final String CLASS_FILE_SUFFIX = ".class";

    /** 以包装类型为键、对应原始类型为值的Map，例如：Integer.class -> int.class。 */
    private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(8);

    /** 以原始类型为键、对应包装类型为值的Map，例如：int.class -> Integer.class。 */
    private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap =
            new IdentityHashMap<>(8);

    /** 以原始类型名为键、对应原始类型为值的Map，例如："int" -> int.class。 */
    private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);

    /** 以常见Java语言类名为键、对应Class为值的Map，主要用于远程调用的高效反序列化。 */
    private static final Map<String, Class<?>> commonClassCache = new HashMap<>(64);

    /** 在查找'主'用户级接口时应忽略的常见Java语言接口集合。 */
    private static final Set<Class<?>> javaLanguageInterfaces;

    /** 声明类实现的接口上的等价方法的缓存。 */
    private static final Map<Method, Method> interfaceMethodCache =
            new GkConcurrentReferenceHashMap<>(256);

    static {
        primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
        primitiveWrapperTypeMap.put(Byte.class, byte.class);
        primitiveWrapperTypeMap.put(Character.class, char.class);
        primitiveWrapperTypeMap.put(Double.class, double.class);
        primitiveWrapperTypeMap.put(Float.class, float.class);
        primitiveWrapperTypeMap.put(Integer.class, int.class);
        primitiveWrapperTypeMap.put(Long.class, long.class);
        primitiveWrapperTypeMap.put(Short.class, short.class);

        // Map的entry迭代比使用forEach配合lambda初始化开销更低
        for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
            primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
            registerCommonClasses(entry.getKey());
        }

        Set<Class<?>> primitiveTypes = new HashSet<>(32);
        primitiveTypes.addAll(primitiveWrapperTypeMap.values());
        Collections.addAll(
                primitiveTypes,
                boolean[].class,
                byte[].class,
                char[].class,
                double[].class,
                float[].class,
                int[].class,
                long[].class,
                short[].class);
        primitiveTypes.add(void.class);
        for (Class<?> primitiveType : primitiveTypes) {
            primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
        }

        registerCommonClasses(
                Boolean[].class,
                Byte[].class,
                Character[].class,
                Double[].class,
                Float[].class,
                Integer[].class,
                Long[].class,
                Short[].class);
        registerCommonClasses(
                Number.class,
                Number[].class,
                String.class,
                String[].class,
                Class.class,
                Class[].class,
                Object.class,
                Object[].class);
        registerCommonClasses(
                Throwable.class,
                Exception.class,
                RuntimeException.class,
                Error.class,
                StackTraceElement.class,
                StackTraceElement[].class);
        registerCommonClasses(
                Enum.class,
                Iterable.class,
                Iterator.class,
                Enumeration.class,
                Collection.class,
                List.class,
                Set.class,
                Map.class,
                Map.Entry.class,
                Optional.class);

        Class<?>[] javaLanguageInterfaceArray = {
            Serializable.class,
            Externalizable.class,
            Closeable.class,
            AutoCloseable.class,
            Cloneable.class,
            Comparable.class
        };
        registerCommonClasses(javaLanguageInterfaceArray);
        javaLanguageInterfaces = new HashSet<>(Arrays.asList(javaLanguageInterfaceArray));
    }

    /** 将给定的常见类注册到ClassUtils缓存中。 */
    private static void registerCommonClasses(Class<?>... commonClasses) {
        for (Class<?> clazz : commonClasses) {
            commonClassCache.put(clazz.getName(), clazz);
        }
    }

    /**
     * 返回默认的ClassLoader：通常优先使用线程上下文ClassLoader（如果可用）； 否则回退到加载ClassUtils类的ClassLoader。
     *
     * <p>如果你打算在明确希望得到非null ClassLoader引用的场景中使用线程上下文ClassLoader， 请调用此方法：例如用于类路径资源加载（但对于{@code
     * Class.forName}则不是必需的， 因为它也接受{@code null} ClassLoader引用）。
     *
     * @return 默认ClassLoader（仅当系统ClassLoader也不可访问时返回{@code null}）
     * @see Thread#getContextClassLoader()
     * @see ClassLoader#getSystemClassLoader()
     */
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // 无法访问线程上下文ClassLoader - 回退...
        }
        if (cl == null) {
            // 没有线程上下文类加载器 -> 使用本类的类加载器
            cl = GutilClass.class.getClassLoader();
            if (cl == null) {
                // getClassLoader()返回null表示bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                } catch (Throwable ex) {
                    // 无法访问系统ClassLoader - 没关系，调用方可以接受null...
                }
            }
        }
        return cl;
    }

    /**
     * 必要时用环境的bean ClassLoader覆盖线程上下文ClassLoader， 即当bean ClassLoader与当前线程上下文ClassLoader不等价时。
     *
     * @param classLoaderToUse 用于线程上下文的实际ClassLoader
     * @return 原始的线程上下文ClassLoader，如果未覆盖则返回{@code null}
     */
    public static ClassLoader overrideThreadContextClassLoader(ClassLoader classLoaderToUse) {
        Thread currentThread = Thread.currentThread();
        ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
        if (classLoaderToUse != null && !classLoaderToUse.equals(threadContextClassLoader)) {
            currentThread.setContextClassLoader(classLoaderToUse);
            return threadContextClassLoader;
        } else {
            return null;
        }
    }

    /**
     * 是{@code Class.forName()}的替代方法，同样支持原始类型（如"int"）和数组类名（如"String[]"）。
     * 此外，它还支持以Java源码风格解析内部类名（如用"java.lang.Thread.State"代替 "java.lang.Thread$State"）。
     *
     * @param name 类的名称
     * @param classLoader 要使用的类加载器（可以为{@code null}，表示使用默认类加载器）
     * @return 给定名称对应的类实例
     * @throws ClassNotFoundException 如果找不到该类
     * @throws LinkageError 如果类文件无法加载
     * @see Class#forName(String, boolean, ClassLoader)
     */
    public static Class<?> forName(String name, ClassLoader classLoader)
            throws ClassNotFoundException, LinkageError {

        GutilAssert.notNull(name, () -> "Name must not be null");

        Class<?> clazz = resolvePrimitiveClassName(name);
        if (clazz == null) {
            clazz = commonClassCache.get(name);
        }
        if (clazz != null) {
            return clazz;
        }

        // "java.lang.String[]"风格的数组
        if (name.endsWith(ARRAY_SUFFIX)) {
            String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
            Class<?> elementClass = forName(elementClassName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        // "[Ljava.lang.String;"风格的数组
        if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
            String elementName =
                    name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
            Class<?> elementClass = forName(elementName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        // "[[I"或"[[Ljava.lang.String;"风格的数组
        if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
            String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
            Class<?> elementClass = forName(elementName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        ClassLoader clToUse = classLoader;
        if (clToUse == null) {
            clToUse = getDefaultClassLoader();
        }
        try {
            return (clToUse != null ? clToUse.loadClass(name) : Class.forName(name));
        } catch (ClassNotFoundException ex) {
            int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
            if (lastDotIndex != -1) {
                String innerClassName =
                        name.substring(0, lastDotIndex)
                                + INNER_CLASS_SEPARATOR
                                + name.substring(lastDotIndex + 1);
                try {
                    return (clToUse != null
                            ? clToUse.loadClass(innerClassName)
                            : Class.forName(innerClassName));
                } catch (ClassNotFoundException ex2) {
                    // 吞掉异常 - 让原始异常抛出来
                }
            }
            throw ex;
        }
    }

    /**
     * 将给定的类名解析为Class实例。支持原始类型（如"int"）和数组类名（如"String[]"）。
     *
     * <p>这实际上等价于参数相同的{@code forName}方法，唯一区别是类加载失败时抛出的异常不同。
     *
     * @param className 类的名称
     * @param classLoader 要使用的类加载器（可以为{@code null}，表示使用默认类加载器）
     * @return 给定名称对应的类实例
     * @throws IllegalArgumentException 如果类名无法解析（即找不到该类或类文件无法加载）
     * @see #forName(String, ClassLoader)
     */
    public static Class<?> resolveClassName(String className, ClassLoader classLoader)
            throws IllegalArgumentException {

        try {
            return forName(className, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Could not find class [" + className + "]", ex);
        } catch (LinkageError err) {
            throw new IllegalArgumentException(
                    "Unresolvable class definition for class [" + className + "]", err);
        }
    }

    /**
     * 判断由给定名称标识的{@link Class}是否存在且可加载。 如果类或其某个依赖不存在或无法加载，返回{@code false}。
     *
     * @param className 要检查的类名
     * @param classLoader 要使用的类加载器（可以为{@code null}，表示使用默认类加载器）
     * @return 指定类是否存在
     */
    public static boolean isPresent(String className, ClassLoader classLoader) {
        try {
            forName(className, classLoader);
            return true;
        } catch (Throwable ex) {
            // 类或其某个依赖不存在...
            return false;
        }
    }

    /**
     * 检查给定类在给定ClassLoader中是否可见。
     *
     * @param clazz 要检查的类（通常是接口）
     * @param classLoader 要检查的ClassLoader（可以为{@code null}，此时此方法始终返回{@code true}）
     */
    public static boolean isVisible(Class<?> clazz, ClassLoader classLoader) {
        if (classLoader == null) {
            return true;
        }
        try {
            if (clazz.getClassLoader() == classLoader) {
                return true;
            }
        } catch (SecurityException ex) {
            // 转入下面的可加载性检查
        }

        // 如果可以从给定ClassLoader加载相同的Class则可见
        return isLoadable(clazz, classLoader);
    }

    /**
     * 检查给定类在给定上下文中是否可安全缓存，即该类是否由给定ClassLoader或其父级加载。
     *
     * @param clazz 要分析的类
     * @param classLoader 可能用于缓存元数据的ClassLoader（可以为{@code null}，表示系统类加载器）
     */
    public static boolean isCacheSafe(Class<?> clazz, ClassLoader classLoader) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        try {
            ClassLoader target = clazz.getClassLoader();
            // 常见情况
            if (target == classLoader || target == null) {
                return true;
            }
            if (classLoader == null) {
                return false;
            }
            // 在祖先中查找匹配 -> 正向
            ClassLoader current = classLoader;
            while (current != null) {
                current = current.getParent();
                if (current == target) {
                    return true;
                }
            }
            // 在子代中查找匹配 -> 反向
            while (target != null) {
                target = target.getParent();
                if (target == classLoader) {
                    return false;
                }
            }
        } catch (SecurityException ex) {
            // 转入下面的可加载性检查
        }

        // 对没有父子关系的ClassLoader的回退判断：
        // 如果可以从给定ClassLoader加载相同的Class则安全
        return (classLoader != null && isLoadable(clazz, classLoader));
    }

    /**
     * 检查给定类在给定ClassLoader中是否可加载。
     *
     * @param clazz 要检查的类（通常是接口）
     * @param classLoader 要检查的ClassLoader
     * @since 5.0.6
     */
    private static boolean isLoadable(Class<?> clazz, ClassLoader classLoader) {
        try {
            return (clazz == classLoader.loadClass(clazz.getName()));
            // 否则：找到的是同名但不同的类
        } catch (ClassNotFoundException ex) {
            // 完全没有找到对应的类
            return false;
        }
    }

    /**
     * 根据JVM对原始类名的命名规则，将给定的类名解析为原始类（如果合适）。
     *
     * <p>也支持原始数组的JVM内部类名。但<i>不</i>支持原始数组的"[]"后缀记法； 该记法仅由{@link #forName(String, ClassLoader)}支持。
     *
     * @param name 可能是原始类的类名
     * @return 原始类，如果该名称不表示原始类或原始数组类则返回{@code null}
     */
    public static Class<?> resolvePrimitiveClassName(String name) {
        Class<?> result = null;
        // 大多数类名都会很长（考虑到它们
        // 应当位于包中），因此长度检查是值得的
        if (name != null && name.length() <= 8) {
            // 可能是原始类型
            result = primitiveTypeNameMap.get(name);
        }
        return result;
    }

    /**
     * 检查给定类是否表示原始类型包装类，即Boolean、Byte、Character、Short、Integer、Long、Float或Double。
     *
     * @param clazz 要检查的类
     * @return 给定类是否为原始类型包装类
     */
    public static boolean isPrimitiveWrapper(Class<?> clazz) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        return primitiveWrapperTypeMap.containsKey(clazz);
    }

    /**
     * 检查给定类是否表示原始类型（即boolean、byte、char、short、int、long、float或double）
     * 或原始类型包装类（即Boolean、Byte、Character、Short、Integer、Long、Float或Double）。
     *
     * @param clazz 要检查的类
     * @return 给定类是否为原始类型或原始类型包装类
     */
    public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
    }

    /**
     * 检查给定类是否表示原始类型数组，即boolean、byte、char、short、int、long、float或double的数组。
     *
     * @param clazz 要检查的类
     * @return 给定类是否为原始类型数组类
     */
    public static boolean isPrimitiveArray(Class<?> clazz) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        return (clazz.isArray() && clazz.getComponentType().isPrimitive());
    }

    /**
     * 检查给定类是否表示原始类型包装类数组，即Boolean、Byte、Character、Short、Integer、Long、Float或Double的数组。
     *
     * @param clazz 要检查的类
     * @return 给定类是否为原始类型包装类数组类
     */
    public static boolean isPrimitiveWrapperArray(Class<?> clazz) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        return (clazz.isArray() && isPrimitiveWrapper(clazz.getComponentType()));
    }

    /**
     * 如果给定类是原始类型，则将其解析为对应的原始类型包装类。
     *
     * @param clazz 要检查的类
     * @return 原始类，或原始类型对应的包装类
     */
    public static Class<?> resolvePrimitiveIfNecessary(Class<?> clazz) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        return (clazz.isPrimitive() && clazz != void.class
                ? primitiveTypeToWrapperMap.get(clazz)
                : clazz);
    }

    /**
     * 检查右侧类型是否可以赋给左侧类型，假设是通过反射进行设置。 将原始类型包装类视为可赋给对应的原始类型。
     *
     * @param lhsType 目标类型
     * @param rhsType 要赋给目标类型的值类型
     * @return 目标类型是否可由值类型赋值
     * @see TypeUtils#isAssignable
     */
    public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
        GutilAssert.notNull(lhsType, () -> "Left-hand side type must not be null");
        GutilAssert.notNull(rhsType, () -> "Right-hand side type must not be null");
        if (lhsType.isAssignableFrom(rhsType)) {
            return true;
        }
        if (lhsType.isPrimitive()) {
            Class<?> resolvedPrimitive = primitiveWrapperTypeMap.get(rhsType);
            if (lhsType == resolvedPrimitive) {
                return true;
            }
        } else {
            Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
            if (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断给定类型是否可以由给定值赋值，假设是通过反射进行设置。 将原始类型包装类视为可赋给对应的原始类型。
     *
     * @param type 目标类型
     * @param value 要赋给该类型的值
     * @return 类型是否可由该值赋值
     */
    public static boolean isAssignableValue(Class<?> type, Object value) {
        GutilAssert.notNull(type, () -> "Type must not be null");
        return (value != null ? isAssignable(type, value.getClass()) : !type.isPrimitive());
    }

    /**
     * 将"/"形式的资源路径转换为"."形式的标准类名。
     *
     * @param resourcePath 指向类的资源路径
     * @return 对应的标准类名
     */
    public static String convertResourcePathToClassName(String resourcePath) {
        GutilAssert.notNull(resourcePath, () -> "Resource path must not be null");
        return resourcePath.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR);
    }

    /**
     * 将"."形式的标准类名转换为"/"形式的资源路径。
     *
     * @param className 标准类名
     * @return 指向类的资源路径
     */
    public static String convertClassNameToResourcePath(String className) {
        GutilAssert.notNull(className, () -> "Class name must not be null");
        return className.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
    }

    /**
     * 返回适合与{@code ClassLoader.getResource}一起使用的路径（也适合与{@code Class.getResource}
     * 一起使用，只需在返回值前加斜杠'/'）。构建方式：取指定类文件的包名，将所有点（'.'）转换为 斜杠（'/'），必要时添加尾斜杠，并将指定资源名拼接到其后。 <br>
     * 因此，此方法可用于构建加载与类文件位于同一包中的资源文件的路径， 不过通常使用{@link
     * org.springframework.core.io.ClassPathResource}更方便。
     *
     * @param clazz 用作基础的类
     * @param resourceName 要追加的资源名。前导斜杠可选。
     * @return 构建好的资源路径
     * @see ClassLoader#getResource
     * @see Class#getResource
     */
    public static String addResourcePathToPackagePath(Class<?> clazz, String resourceName) {
        GutilAssert.notNull(resourceName, () -> "Resource name must not be null");
        if (!resourceName.startsWith("/")) {
            return classPackageAsResourcePath(clazz) + '/' + resourceName;
        }
        return classPackageAsResourcePath(clazz) + resourceName;
    }

    /**
     * 给定一个输入类对象，返回由该类包名组成的路径字符串，即所有点（'.'）替换为斜杠（'/'）。 不添加前导或尾随斜杠。结果可以拼上斜杠和资源名直接用于{@code
     * ClassLoader.getResource()}； 如果要用于{@code Class.getResource}，则还需要在返回值前加前导斜杠。
     *
     * @param clazz 输入类。{@code null}值或默认（空）包将返回空字符串（""）。
     * @return 表示包名的路径
     * @see ClassLoader#getResource
     * @see Class#getResource
     */
    public static String classPackageAsResourcePath(Class<?> clazz) {
        if (clazz == null) {
            return "";
        }
        String className = clazz.getName();
        int packageEndIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
        if (packageEndIndex == -1) {
            return "";
        }
        String packageName = className.substring(0, packageEndIndex);
        return packageName.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
    }

    /**
     * 构建由给定数组中类的名称组成的字符串。
     *
     * <p>基本上类似于{@code AbstractCollection.toString()}，但会在每个类名前去掉 "class "/"interface "前缀。
     *
     * @param classes Class对象数组
     * @return 形如"[com.foo.Bar, com.foo.Baz]"的字符串
     * @see java.util.AbstractCollection#toString()
     */
    public static String classNamesToString(Class<?>... classes) {
        return classNamesToString(Arrays.asList(classes));
    }

    /**
     * 构建由给定集合中类的名称组成的字符串。
     *
     * <p>基本上类似于{@code AbstractCollection.toString()}，但会在每个类名前去掉 "class "/"interface "前缀。
     *
     * @param classes Class对象集合（可以为{@code null}）
     * @return 形如"[com.foo.Bar, com.foo.Baz]"的字符串
     * @see java.util.AbstractCollection#toString()
     */
    public static String classNamesToString(Collection<Class<?>> classes) {
        if (GutilCollection.isEmpty(classes)) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (Iterator<Class<?>> it = classes.iterator(); it.hasNext(); ) {
            Class<?> clazz = it.next();
            sb.append(clazz.getName());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 将给定的{@code Collection}复制为{@code Class}数组。
     *
     * <p>{@code Collection}中只能包含{@code Class}元素。
     *
     * @param collection 要复制的{@code Collection}
     * @return {@code Class}数组
     * @since 3.1
     * @see StringUtils#toStringArray
     */
    public static Class<?>[] toClassArray(Collection<Class<?>> collection) {
        return collection.toArray(new Class<?>[0]);
    }

    /**
     * 返回给定实例实现的所有接口（数组形式），包括其父类实现的接口。
     *
     * @param instance 要分析接口的实例
     * @return 给定实例实现的所有接口（数组形式）
     */
    public static Class<?>[] getAllInterfaces(Object instance) {
        GutilAssert.notNull(instance, () -> "Instance must not be null");
        return getAllInterfacesForClass(instance.getClass());
    }

    /**
     * 返回给定类实现的所有接口（数组形式），包括其父类实现的接口。
     *
     * <p>如果类本身是接口，则将其作为唯一接口返回。
     *
     * @param clazz 待分析接口的类
     * @return 给定类实现的所有接口（数组形式）
     */
    public static Class<?>[] getAllInterfacesForClass(Class<?> clazz) {
        return getAllInterfacesForClass(clazz, null);
    }

    /**
     * 返回给定类实现的所有接口（数组形式），包括其父类实现的接口。
     *
     * <p>如果类本身是接口，则将其作为唯一接口返回。
     *
     * @param clazz 待分析接口的类
     * @param classLoader 接口需要可见的ClassLoader（{@code null}表示接受所有声明的接口）
     * @return 给定类实现的所有接口（数组形式）
     */
    public static Class<?>[] getAllInterfacesForClass(Class<?> clazz, ClassLoader classLoader) {
        return toClassArray(getAllInterfacesForClassAsSet(clazz, classLoader));
    }

    /**
     * 返回给定实例实现的所有接口（Set形式），包括其父类实现的接口。
     *
     * @param instance 待分析接口的实例
     * @return 给定实例实现的所有接口（Set形式）
     */
    public static Set<Class<?>> getAllInterfacesAsSet(Object instance) {
        GutilAssert.notNull(instance, () -> "Instance must not be null");
        return getAllInterfacesForClassAsSet(instance.getClass());
    }

    /**
     * 返回给定类实现的所有接口（Set形式），包括其父类实现的接口。
     *
     * <p>如果类本身是接口，则将其作为唯一接口返回。
     *
     * <p>注意：此方法每次调用都会沿类继承层次全量遍历接口，无缓存。
     *
     * @param clazz 待分析接口的类
     * @return 给定对象实现的所有接口（Set形式）
     */
    public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz) {
        return getAllInterfacesForClassAsSet(clazz, null);
    }

    /**
     * 返回给定类实现的所有接口（Set形式），包括其父类实现的接口。
     *
     * <p>如果类本身是接口，则将其作为唯一接口返回。
     *
     * <p>注意：此方法每次调用都会沿类继承层次全量遍历接口，无缓存。
     *
     * @param clazz 待分析接口的类
     * @param classLoader 接口需要可见的ClassLoader（{@code null}表示接受所有声明的接口）
     * @return 给定对象实现的所有接口（Set形式）
     */
    public static Set<Class<?>> getAllInterfacesForClassAsSet(
            Class<?> clazz, ClassLoader classLoader) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        if (clazz.isInterface() && isVisible(clazz, classLoader)) {
            return Collections.singleton(clazz);
        }
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        Class<?> current = clazz;
        while (current != null) {
            Class<?>[] ifcs = current.getInterfaces();
            for (Class<?> ifc : ifcs) {
                if (isVisible(ifc, classLoader)) {
                    interfaces.add(ifc);
                }
            }
            current = current.getSuperclass();
        }
        return interfaces;
    }

    /**
     * 为给定接口创建复合接口Class，在一个Class中实现给定接口。
     *
     * <p>此实现为给定接口构建JDK代理类。
     *
     * @param interfaces 要合并的接口
     * @param classLoader 在其中创建复合Class的ClassLoader
     * @return 合并后的接口Class
     * @throws IllegalArgumentException 如果指定接口暴露了冲突的方法签名（或类似的约束被违反）
     * @see java.lang.reflect.Proxy#getProxyClass
     */
    @SuppressWarnings("deprecation")
    public static Class<?> createCompositeInterface(
            Class<?>[] interfaces, ClassLoader classLoader) {
        GutilAssert.notEmpty(interfaces, () -> "Interfaces must not be empty");
        return Proxy.getProxyClass(classLoader, interfaces);
    }

    /**
     * 确定给定类的公共祖先（如果存在）。
     *
     * @param clazz1 要内省的类
     * @param clazz2 要内省的另一个类
     * @return 公共祖先（即公共父类，或一个接口继承另一个接口），如果未找到返回{@code null}。 如果任一给定类为{@code null}，则返回另一个类。
     * @since 3.2.6
     */
    public static Class<?> determineCommonAncestor(Class<?> clazz1, Class<?> clazz2) {
        if (clazz1 == null) {
            return clazz2;
        }
        if (clazz2 == null) {
            return clazz1;
        }
        if (clazz1.isAssignableFrom(clazz2)) {
            return clazz1;
        }
        if (clazz2.isAssignableFrom(clazz1)) {
            return clazz2;
        }
        Class<?> ancestor = clazz1;
        do {
            ancestor = ancestor.getSuperclass();
            if (ancestor == null || Object.class == ancestor) {
                return null;
            }
        } while (!ancestor.isAssignableFrom(clazz2));
        return ancestor;
    }

    /**
     * 判断给定接口是否为常见的Java语言接口：{@link Serializable}、{@link Externalizable}、 {@link Closeable}、{@link
     * AutoCloseable}、{@link Cloneable}、{@link Comparable}——
     * 在查找'主'用户级接口时可以忽略这些接口。共同特征：无服务级操作、无bean属性方法、无默认方法。
     *
     * @param ifc 要检查的接口
     * @since 5.0.3
     */
    public static boolean isJavaLanguageInterface(Class<?> ifc) {
        return javaLanguageInterfaces.contains(ifc);
    }

    /**
     * 判断给定类是否为<em>内部类</em>，即外部类的非静态成员。
     *
     * @return {@code true} 如果给定类是内部类
     * @since 5.0.5
     * @see Class#isMemberClass()
     */
    public static boolean isInnerClass(Class<?> clazz) {
        return (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers()));
    }

    /**
     * 检查给定对象是否为CGLIB代理。
     *
     * @param object 要检查的对象
     * @see #isCglibProxyClass(Class)
     * @see org.springframework.aop.support.AopUtils#isCglibProxy(Object)
     */
    public static boolean isCglibProxy(Object object) {
        return isCglibProxyClass(object.getClass());
    }

    /**
     * 检查指定类是否为CGLIB生成的类。
     *
     * @param clazz 要检查的类
     * @see #isCglibProxyClassName(String)
     */
    public static boolean isCglibProxyClass(Class<?> clazz) {
        return (clazz != null && isCglibProxyClassName(clazz.getName()));
    }

    /**
     * 检查指定类名是否为CGLIB生成的类名。
     *
     * @param className 要检查的类名
     */
    public static boolean isCglibProxyClassName(String className) {
        return (className != null && className.contains(CGLIB_CLASS_SEPARATOR));
    }

    /**
     * 返回给定实例的用户定义类：通常就是该实例的类，但对于CGLIB生成的子类则返回原始类。
     *
     * @param instance 要检查的实例
     * @return 用户定义类
     */
    public static Class<?> getUserClass(Object instance) {
        GutilAssert.notNull(instance, () -> "Instance must not be null");
        return getUserClass(instance.getClass());
    }

    /**
     * 返回给定类的用户定义类：通常就是给定类，但对于CGLIB生成的子类则返回原始类。
     *
     * @param clazz 要检查的类
     * @return 用户定义类
     */
    public static Class<?> getUserClass(Class<?> clazz) {
        if (clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return superclass;
            }
        }
        return clazz;
    }

    /**
     * 返回给定对象类型的描述性名称：通常就是类名，但对于数组是组件类型类名加"[]"， 对于JDK代理则是追加其实现接口列表。
     *
     * @param value 要内省的值
     * @return 类的限定名
     */
    public static String getDescriptiveType(Object value) {
        if (value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if (Proxy.isProxyClass(clazz)) {
            StringBuilder result = new StringBuilder(clazz.getName());
            result.append(" implementing ");
            Class<?>[] ifcs = clazz.getInterfaces();
            for (int i = 0; i < ifcs.length; i++) {
                result.append(ifcs[i].getName());
                if (i < ifcs.length - 1) {
                    result.append(',');
                }
            }
            return result.toString();
        } else {
            return clazz.getTypeName();
        }
    }

    /**
     * 检查给定类是否匹配用户指定的类型名。
     *
     * @param clazz 要检查的类
     * @param typeName 要匹配的类型名
     */
    public static boolean matchesTypeName(Class<?> clazz, String typeName) {
        return (typeName != null
                && (typeName.equals(clazz.getTypeName())
                        || typeName.equals(clazz.getSimpleName())));
    }

    /**
     * 获取不带限定包名的类名。
     *
     * @param className 要获取短名的类名
     * @return 不带包名的类名
     * @throws IllegalArgumentException 如果类名为空
     */
    public static String getShortName(String className) {
        GutilAssert.hasText(className, () -> "Class name must not be empty");
        int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
        int nameEndIndex = className.indexOf(CGLIB_CLASS_SEPARATOR);
        if (nameEndIndex == -1) {
            nameEndIndex = className.length();
        }
        String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
        shortName = shortName.replace(INNER_CLASS_SEPARATOR, PACKAGE_SEPARATOR);
        return shortName;
    }

    /**
     * 获取不带限定包名的类名。
     *
     * @param clazz 要获取短名的类
     * @return 不带包名的类名
     */
    public static String getShortName(Class<?> clazz) {
        return getShortName(getQualifiedName(clazz));
    }

    /**
     * 以小写开头的JavaBeans属性格式返回Java类的短字符串名。如果是内部类，则去掉外部类名。
     *
     * @param clazz 类
     * @return 以标准JavaBeans属性格式呈现的短名
     * @see java.beans.Introspector#decapitalize(String)
     */
    public static String getShortNameAsProperty(Class<?> clazz) {
        String shortName = getShortName(clazz);
        int dotIndex = shortName.lastIndexOf(PACKAGE_SEPARATOR);
        shortName = (dotIndex != -1 ? shortName.substring(dotIndex + 1) : shortName);
        return Introspector.decapitalize(shortName);
    }

    /**
     * 确定类文件的名称，相对于包含包：例如"String.class"。
     *
     * @param clazz 类
     * @return ".class"文件的文件名
     */
    public static String getClassFileName(Class<?> clazz) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        String className = clazz.getName();
        int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
        return className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX;
    }

    /**
     * 确定给定类的包名，例如{@code java.lang.String}类的包名为"java.lang"。
     *
     * @param clazz 类
     * @return 包名，如果类定义在默认包中则返回空字符串
     */
    public static String getPackageName(Class<?> clazz) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        return getPackageName(clazz.getName());
    }

    /**
     * 确定给定全限定类名的包名，例如{@code java.lang.String}类名的包名为"java.lang"。
     *
     * @param fqClassName 全限定类名
     * @return 包名，如果类名没有包名则返回空字符串
     */
    public static String getPackageName(String fqClassName) {
        GutilAssert.notNull(fqClassName, () -> "Class name must not be null");
        int lastDotIndex = fqClassName.lastIndexOf(PACKAGE_SEPARATOR);
        return (lastDotIndex != -1 ? fqClassName.substring(0, lastDotIndex) : "");
    }

    /**
     * 返回给定类的限定名：通常就是类名，但对于数组是组件类型类名加"[]"。
     *
     * @param clazz 类
     * @return 类的限定名
     */
    public static String getQualifiedName(Class<?> clazz) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        return clazz.getTypeName();
    }

    /**
     * 返回给定方法的限定名，由全限定接口/类名 + "." + 方法名组成。
     *
     * @param method 方法
     * @return 方法的限定名
     */
    public static String getQualifiedMethodName(Method method) {
        return getQualifiedMethodName(method, null);
    }

    /**
     * 返回给定方法的限定名，由全限定接口/类名 + "." + 方法名组成。
     *
     * @param method 方法
     * @param clazz 调用该方法的类（可以为{@code null}，表示使用方法的声明类）
     * @return 方法的限定名
     * @since 4.3.4
     */
    public static String getQualifiedMethodName(Method method, Class<?> clazz) {
        GutilAssert.notNull(method, () -> "Method must not be null");
        return (clazz != null ? clazz : method.getDeclaringClass()).getName()
                + '.'
                + method.getName();
    }

    /**
     * 判断给定类是否具有给定签名的公共构造方法。
     *
     * <p>本质上将{@code NoSuchMethodException}转换为"false"。
     *
     * @param clazz 要分析的类
     * @param paramTypes 构造方法的参数类型
     * @return 类是否具有对应的构造方法
     * @see Class#getMethod
     */
    public static boolean hasConstructor(Class<?> clazz, Class<?>... paramTypes) {
        return (getConstructorIfAvailable(clazz, paramTypes) != null);
    }

    /**
     * 判断给定类是否具有给定签名的公共构造方法，如果有则返回（否则返回{@code null}）。
     *
     * <p>本质上将{@code NoSuchMethodException}转换为{@code null}。
     *
     * @param clazz 要分析的类
     * @param paramTypes 构造方法的参数类型
     * @return 构造方法，如果未找到返回{@code null}
     * @see Class#getConstructor
     */
    public static <T> Constructor<T> getConstructorIfAvailable(
            Class<T> clazz, Class<?>... paramTypes) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        try {
            return clazz.getConstructor(paramTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * 判断给定类是否具有给定签名的公共方法。
     *
     * <p>本质上将{@code NoSuchMethodException}转换为"false"。
     *
     * @param clazz 要分析的类
     * @param methodName 方法名
     * @param paramTypes 方法的参数类型
     * @return 类是否具有对应的方法
     * @see Class#getMethod
     */
    public static boolean hasMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        return (getMethodIfAvailable(clazz, methodName, paramTypes) != null);
    }

    /**
     * 判断给定类是否具有给定签名的公共方法，如果有则返回（否则抛出{@code IllegalStateException}）。
     *
     * <p>如果指定了任意签名，则仅在存在唯一候选时才返回该方法， 即具有指定名称的唯一公共方法。
     *
     * <p>本质上将{@code NoSuchMethodException}转换为{@code IllegalStateException}。
     *
     * @param clazz 要分析的类
     * @param methodName 方法名
     * @param paramTypes 方法的参数类型（可以为{@code null}表示任意签名）
     * @return 方法（永远不会是{@code null}）
     * @throws IllegalStateException 如果未找到方法
     * @see Class#getMethod
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        GutilAssert.notNull(methodName, () -> "Method name must not be null");
        if (paramTypes != null) {
            try {
                return clazz.getMethod(methodName, paramTypes);
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException("Expected method not found: " + ex);
            }
        } else {
            Set<Method> candidates = new HashSet<>(1);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (methodName.equals(method.getName())) {
                    candidates.add(method);
                }
            }
            if (candidates.size() == 1) {
                return candidates.iterator().next();
            } else if (candidates.isEmpty()) {
                throw new IllegalStateException(
                        "Expected method not found: " + clazz.getName() + '.' + methodName);
            } else {
                throw new IllegalStateException(
                        "No unique method found: " + clazz.getName() + '.' + methodName);
            }
        }
    }

    /**
     * 判断给定类是否具有给定签名的公共方法，如果有则返回（否则返回{@code null}）。
     *
     * <p>如果指定了任意签名，则仅在存在唯一候选时才返回该方法， 即具有指定名称的唯一公共方法。
     *
     * <p>本质上将{@code NoSuchMethodException}转换为{@code null}。
     *
     * @param clazz 要分析的类
     * @param methodName 方法名
     * @param paramTypes 方法的参数类型（可以为{@code null}表示任意签名）
     * @return 方法，如果未找到返回{@code null}
     * @see Class#getMethod
     */
    public static Method getMethodIfAvailable(
            Class<?> clazz, String methodName, Class<?>... paramTypes) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        GutilAssert.notNull(methodName, () -> "Method name must not be null");
        if (paramTypes != null) {
            try {
                return clazz.getMethod(methodName, paramTypes);
            } catch (NoSuchMethodException ex) {
                return null;
            }
        } else {
            Set<Method> candidates = new HashSet<>(1);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (methodName.equals(method.getName())) {
                    candidates.add(method);
                }
            }
            if (candidates.size() == 1) {
                return candidates.iterator().next();
            }
            return null;
        }
    }

    /**
     * 返回给定类及其父类中具有给定名称（任意参数类型）的方法数量。包括非公共方法。
     *
     * @param clazz 要检查的类
     * @param methodName 方法名
     * @return 具有给定名称的方法数量
     */
    public static int getMethodCountForName(Class<?> clazz, String methodName) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        GutilAssert.notNull(methodName, () -> "Method name must not be null");
        int count = 0;
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (methodName.equals(method.getName())) {
                count++;
            }
        }
        Class<?>[] ifcs = clazz.getInterfaces();
        for (Class<?> ifc : ifcs) {
            count += getMethodCountForName(ifc, methodName);
        }
        if (clazz.getSuperclass() != null) {
            count += getMethodCountForName(clazz.getSuperclass(), methodName);
        }
        return count;
    }

    /**
     * 给定类或其某个父类是否至少具有一个或多个具有给定名称（任意参数类型）的方法？包括非公共方法。
     *
     * @param clazz 要检查的类
     * @param methodName 方法名
     * @return 是否至少有一个具有给定名称的方法
     */
    public static boolean hasAtLeastOneMethodWithName(Class<?> clazz, String methodName) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        GutilAssert.notNull(methodName, () -> "Method name must not be null");
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (method.getName().equals(methodName)) {
                return true;
            }
        }
        Class<?>[] ifcs = clazz.getInterfaces();
        for (Class<?> ifc : ifcs) {
            if (hasAtLeastOneMethodWithName(ifc, methodName)) {
                return true;
            }
        }
        return (clazz.getSuperclass() != null
                && hasAtLeastOneMethodWithName(clazz.getSuperclass(), methodName));
    }

    /**
     * 给定一个可能来自接口的方法，以及当前反射调用中使用的目标类，找到对应的目标方法（如果存在）。 例如方法可能是{@code IFoo.bar()}，目标类可能是{@code
     * DefaultFoo}。在这种情况下， 方法可能是{@code DefaultFoo.bar()}。这样可以找到该方法上的属性。
     *
     * <p><b>注意：</b>与{@link org.springframework.aop.support.AopUtils#getMostSpecificMethod}
     * 相反，此方法<i>不</i>自动解析Java 5桥接方法。如果需要桥接方法解析 （例如获取原始方法定义的元数据），请调用{@link
     * org.springframework.core.BridgeMethodResolver#findBridgedMethod}。
     *
     * <p><b>注意：</b>自Spring 3.1.1起，如果Java安全设置不允许反射访问 （例如调用{@code
     * Class#getDeclaredMethods}等），此实现将回退到返回原始方法。
     *
     * @param method 要调用的方法，可能来自接口
     * @param targetClass 当前调用的目标类（可以为{@code null}，或者甚至不实现该方法）
     * @return 特定的目标方法，如果{@code targetClass}未实现该方法则返回原始方法
     */
    public static Method getMostSpecificMethod(Method method, Class<?> targetClass) {
        if (targetClass != null
                && targetClass != method.getDeclaringClass()
                && isOverridable(method, targetClass)) {
            try {
                if (Modifier.isPublic(method.getModifiers())) {
                    try {
                        return targetClass.getMethod(method.getName(), method.getParameterTypes());
                    } catch (NoSuchMethodException ex) {
                        return method;
                    }
                } else {
                    Method specificMethod =
                            GutilReflection.findMethod(
                                    targetClass, method.getName(), method.getParameterTypes());
                    return (specificMethod != null ? specificMethod : method);
                }
            } catch (SecurityException ex) {
                // 安全设置不允许反射访问；回退到下面的'method'
            }
        }
        return method;
    }

    /**
     * 如果可能，为给定的方法句柄确定对应的接口方法。
     *
     * <p>这对于获得Jigsaw上可公开导出的类型特别有用，可以在没有非法访问警告的情况下被反射调用。
     *
     * @param method 要调用的方法，可能来自实现类
     * @return 对应的接口方法，如果未找到则返回原始方法
     * @since 5.1
     * @see #getMostSpecificMethod
     */
    public static Method getInterfaceMethodIfPossible(Method method) {
        if (!Modifier.isPublic(method.getModifiers()) || method.getDeclaringClass().isInterface()) {
            return method;
        }
        return interfaceMethodCache.computeIfAbsent(
                method,
                key -> {
                    Class<?> current = key.getDeclaringClass();
                    while (current != null && current != Object.class) {
                        Class<?>[] ifcs = current.getInterfaces();
                        for (Class<?> ifc : ifcs) {
                            try {
                                return ifc.getMethod(key.getName(), key.getParameterTypes());
                            } catch (NoSuchMethodException ex) {
                                // 忽略
                            }
                        }
                        current = current.getSuperclass();
                    }
                    return key;
                });
    }

    /**
     * 判断给定方法是由用户声明的，或者至少指向用户声明的方法。
     *
     * <p>检查{@link Method#isSynthetic()}（针对实现方法）以及{@code GroovyObject}接口 （针对接口方法；在实现类上，{@code
     * GroovyObject}方法的实现无论如何都会被标记为合成方法）。 注意，尽管是合成的，桥接方法（{@link Method#isBridge()}）仍被视为用户级方法，
     * 因为它们最终指向用户声明的泛型方法。
     *
     * @param method 要检查的方法
     * @return {@code true} 如果该方法可被视为用户声明的；否则为{@code false}
     */
    public static boolean isUserLevelMethod(Method method) {
        GutilAssert.notNull(method, () -> "Method must not be null");
        return (method.isBridge() || (!method.isSynthetic() && !isGroovyObjectMethod(method)));
    }

    private static boolean isGroovyObjectMethod(Method method) {
        return method.getDeclaringClass().getName().equals("groovy.lang.GroovyObject");
    }

    /**
     * 判断给定方法在给定目标类中是否可被重写。
     *
     * @param method 要检查的方法
     * @param targetClass 要检查的目标类
     */
    private static boolean isOverridable(Method method, Class<?> targetClass) {
        if (Modifier.isPrivate(method.getModifiers())) {
            return false;
        }
        if (Modifier.isPublic(method.getModifiers())
                || Modifier.isProtected(method.getModifiers())) {
            return true;
        }
        return (targetClass == null
                || getPackageName(method.getDeclaringClass()).equals(getPackageName(targetClass)));
    }

    /**
     * 返回类的公共静态方法。
     *
     * @param clazz 定义该方法的类
     * @param methodName 静态方法名
     * @param args 方法的参数类型
     * @return 静态方法，如果未找到静态方法则返回{@code null}
     * @throws IllegalArgumentException 如果方法名为空或clazz为null
     */
    public static Method getStaticMethod(Class<?> clazz, String methodName, Class<?>... args) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        GutilAssert.notNull(methodName, () -> "Method name must not be null");
        try {
            Method method = clazz.getMethod(methodName, args);
            return Modifier.isStatic(method.getModifiers()) ? method : null;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * {@code null}安全的获取对象类型
     *
     * @param <T> 对象类型
     * @param obj 对象，如果为{@code null} 返回{@code null}
     * @return 对象类型，提供对象如果为{@code null} 返回{@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClass(T obj) {
        return ((null == obj) ? null : (Class<T>) obj.getClass());
    }

    /**
     * 获得外围类<br>
     * 返回定义此类或匿名类所在的类，如果类本身是在包中定义的，返回{@code null}
     *
     * @param clazz 类
     * @return 外围类
     * @since 4.5.7
     */
    public static Class<?> getEnclosingClass(Class<?> clazz) {
        return null == clazz ? null : clazz.getEnclosingClass();
    }

    /**
     * 是否为顶层类，即定义在包中的类，而非定义在类中的内部类
     *
     * @param clazz 类
     * @return 是否为顶层类
     * @since 4.5.7
     */
    public static boolean isTopLevelClass(Class<?> clazz) {
        if (null == clazz) {
            return false;
        }
        return null == getEnclosingClass(clazz);
    }

    /**
     * 获取类名
     *
     * @param obj 获取类名对象
     * @param isSimple 是否简单类名，如果为true，返回不带包名的类名
     * @return 类名
     * @since 3.0.7
     */
    public static String getClassName(Object obj, boolean isSimple) {
        if (null == obj) {
            return null;
        }
        final Class<?> clazz = obj.getClass();
        return getClassName(clazz, isSimple);
    }

    /**
     * 获取类名<br>
     * 类名并不包含“.class”这个扩展名<br>
     * 例如：ClassUtil这个类<br>
     *
     * <pre>
     * isSimple为false: "com.xiaoleilu.hutool.util.ClassUtil"
     * isSimple为true: "ClassUtil"
     * </pre>
     *
     * @param clazz 类
     * @param isSimple 是否简单类名，如果为true，返回不带包名的类名
     * @return 类名
     * @since 3.0.7
     */
    public static String getClassName(Class<?> clazz, boolean isSimple) {
        if (null == clazz) {
            return null;
        }
        return isSimple ? clazz.getSimpleName() : clazz.getName();
    }

    /**
     * 获得对象数组的类数组
     *
     * @param objects 对象数组，如果数组中存在{@code null}元素，则此元素被认为是Object类型
     * @return 类数组
     */
    public static Class<?>[] getClasses(Object... objects) {
        Class<?>[] classes = new Class<?>[objects.length];
        Object obj;
        for (int i = 0; i < objects.length; i++) {
            obj = objects[i];
            if (obj instanceof GkNullWrapperBean) {
                // 自定义null值的参数类型
                classes[i] = ((GkNullWrapperBean<?>) obj).getWrappedClass();
            } else if (null == obj) {
                classes[i] = Object.class;
            } else {
                classes[i] = obj.getClass();
            }
        }
        return classes;
    }

    /**
     * 指定类是否与给定的类名相同
     *
     * @param clazz 类
     * @param className 类名，可以是全类名（包含包名），也可以是简单类名（不包含包名）
     * @param ignoreCase 是否忽略大小写
     * @return 指定类是否与给定的类名相同
     * @since 3.0.7
     */
    public static boolean equals(Class<?> clazz, String className, boolean ignoreCase) {
        if (null == clazz || GutilStr.isBlank(className)) {
            return false;
        }
        if (ignoreCase) {
            return className.equalsIgnoreCase(clazz.getName())
                    || className.equalsIgnoreCase(clazz.getSimpleName());
        } else {
            return className.equals(clazz.getName()) || className.equals(clazz.getSimpleName());
        }
    }

    // -----------------------------------------------------------------------------------------
    // 方法

    // -----------------------------------------------------------------------------------------
    // 字段

    /**
     * 查找指定类中的所有字段（包括非public字段）， 字段不存在则返回<code>null</code>
     *
     * @param clazz 被查找字段的类
     * @param fieldName 字段名
     * @return 字段
     * @throws SecurityException 安全异常
     */
    public static Field getDeclaredField(Class<?> clazz, String fieldName)
            throws SecurityException {
        if (null == clazz || GutilStr.isBlank(fieldName)) {
            return null;
        }
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // 未找到字段，忽略
        }
        return null;
    }

    /**
     * 查找指定类中的所有字段（包括非public字段)
     *
     * @param clazz 被查找字段的类
     * @return 字段
     * @throws SecurityException 安全异常
     */
    public static Field[] getDeclaredFields(Class<?> clazz) throws SecurityException {
        if (null == clazz) {
            return null;
        }
        return clazz.getDeclaredFields();
    }

    // -----------------------------------------------------------------------------------------
    // Classpath

    /** @return 获得Java ClassPath路径，不包括 jre */
    public static String[] getJavaClassPaths() {
        return System.getProperty("java.class.path").split(System.getProperty("path.separator"));
    }

    /**
     * 比较判断types1和types2两组类，如果types1中所有的类都与types2对应位置的类相同，或者是其父类或接口，则返回<code>true</code>
     *
     * <p>数组中的{@code null}元素按{@link Object}类型处理，即null参数可以匹配任意类型。
     *
     * @param types1 类组1
     * @param types2 类组2
     * @return 是否相同、父类或接口
     */
    public static boolean isAllAssignableFrom(Class<?>[] types1, Class<?>[] types2) {
        if (GutilArray.isEmpty(types1) && GutilArray.isEmpty(types2)) {
            return true;
        }
        if (null == types1 || null == types2) {
            // 任何一个为null不相等（之前已判断两个都为null的情况）
            return false;
        }
        if (types1.length != types2.length) {
            return false;
        }

        Class<?> type1;
        Class<?> type2;
        for (int i = 0; i < types1.length; i++) {
            // 数组元素为null时按Object类型处理，避免空指针
            type1 = (null == types1[i]) ? Object.class : types1[i];
            type2 = (null == types2[i]) ? Object.class : types2[i];
            if (isBasicType(type1) && isBasicType(type2)) {
                // 原始类型和包装类型存在不一致情况
                if (GkBasicType.unWrap(type1) != GkBasicType.unWrap(type2)) {
                    return false;
                }
            } else if (false == type1.isAssignableFrom(type2)) {
                return false;
            }
        }
        return true;
    }

    // ----------------------------------------------------------------------------------------------------
    // 调用相关工具方法结束

    /**
     * 是否为基本类型（包括包装类和原始类）
     *
     * @param clazz 类
     * @return 是否为基本类型
     */
    public static boolean isBasicType(Class<?> clazz) {
        if (null == clazz) {
            return false;
        }
        return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
    }

    /**
     * 是否简单值类型或简单值类型的数组<br>
     * 包括：原始类型,、String、other CharSequence, a Number, a Date, a URI, a URL, a Locale or a Class及其数组
     *
     * @param clazz 属性类
     * @return 是否简单值类型或简单值类型的数组
     */
    public static boolean isSimpleTypeOrArray(Class<?> clazz) {
        if (null == clazz) {
            return false;
        }
        return isSimpleValueType(clazz)
                || (clazz.isArray() && isSimpleValueType(clazz.getComponentType()));
    }

    /**
     * 是否为简单值类型<br>
     * 包括：
     *
     * <pre>
     *     原始类型
     *     String、other CharSequence
     *     Number
     *     Date
     *     URI
     *     URL
     *     Locale
     *     Class
     * </pre>
     *
     * @param clazz 类
     * @return 是否为简单值类型
     */
    public static boolean isSimpleValueType(Class<?> clazz) {
        return isBasicType(clazz) //
                || clazz.isEnum() //
                || CharSequence.class.isAssignableFrom(clazz) //
                || Number.class.isAssignableFrom(clazz) //
                || Date.class.isAssignableFrom(clazz) //
                || clazz.equals(URI.class) //
                || clazz.equals(URL.class) //
                || clazz.equals(Locale.class) //
                || clazz.equals(Class.class) //
                // jdk8日期对象
                || TemporalAccessor.class.isAssignableFrom(clazz); //
    }

    /**
     * 检查目标类是否可以从原类转化（历史版本说明）<br>
     * 转化包括：<br>
     * 1、原类是对象，目标类型是原类型实现的接口<br>
     * 2、目标类型是原类型的父类<br>
     * 3、两者是原始类型或者包装类型（相互转换）
     *
     * @param targetType 目标类型
     * @param sourceType 原类型
     * @return 是否可转化
     */

    /**
     * 指定类是否为Public
     *
     * @param clazz 类
     * @return 是否为public
     */
    public static boolean isPublic(Class<?> clazz) {
        if (null == clazz) {
            throw new NullPointerException("Class to provided is null.");
        }
        return Modifier.isPublic(clazz.getModifiers());
    }

    /**
     * 指定方法是否为Public
     *
     * @param method 方法
     * @return 是否为public
     */
    public static boolean isPublic(Method method) {
        GutilAssert.notNull(method, "Method to provided is null.");
        return Modifier.isPublic(method.getModifiers());
    }

    /**
     * 指定类是否为非public
     *
     * @param clazz 类
     * @return 是否为非public
     */
    public static boolean isNotPublic(Class<?> clazz) {
        return false == isPublic(clazz);
    }

    /**
     * 指定方法是否为非public
     *
     * @param method 方法
     * @return 是否为非public
     */
    public static boolean isNotPublic(Method method) {
        return false == isPublic(method);
    }

    /**
     * 是否为静态方法
     *
     * @param method 方法
     * @return 是否为静态方法
     */
    public static boolean isStatic(Method method) {
        GutilAssert.notNull(method, "Method to provided is null.");
        return Modifier.isStatic(method.getModifiers());
    }

    /**
     * 设置方法为可访问
     *
     * @param method 方法
     * @return 方法
     */
    public static Method setAccessible(Method method) {
        if (null != method && false == method.isAccessible()) {
            method.setAccessible(true);
        }
        return method;
    }

    /**
     * 是否为抽象类
     *
     * @param clazz 类
     * @return 是否为抽象类
     */
    public static boolean isAbstract(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    /**
     * 是否为标准的类<br>
     * 这个类必须：
     *
     * <pre>
     * 1、非接口
     * 2、非抽象类
     * 3、非Enum枚举
     * 4、非数组
     * 5、非注解
     * 6、非原始类型（int, long等）
     * </pre>
     *
     * @param clazz 类
     * @return 是否为标准类
     */
    public static boolean isNormalClass(Class<?> clazz) {
        return null != clazz //
                && false == clazz.isInterface() //
                && false == isAbstract(clazz) //
                && false == clazz.isEnum() //
                && false == clazz.isArray() //
                && false == clazz.isAnnotation() //
                && false == clazz.isSynthetic() //
                && false == clazz.isPrimitive(); //
    }

    /**
     * 判断类是否为枚举类型
     *
     * @param clazz 类
     * @return 是否为枚举类型
     * @since 3.2.0
     */
    public static boolean isEnum(Class<?> clazz) {
        return null != clazz && clazz.isEnum();
    }

    /**
     * 获得给定类的第一个泛型参数
     *
     * @param clazz 被检查的类，必须是已经确定泛型类型的类
     * @return {@link Class}
     */
    public static Class<?> getTypeArgument(Class<?> clazz) {
        return getTypeArgument(clazz, 0);
    }

    /**
     * 获得给定类的泛型参数
     *
     * @param clazz 被检查的类，必须是已经确定泛型类型的类
     * @param index 泛型类型的索引号，即第几个泛型类型
     * @return {@link Class}
     */
    public static Class<?> getTypeArgument(Class<?> clazz, int index) {
        final Type argumentType = GutilType.getTypeArgument(clazz, index);
        return GutilType.getClass(argumentType);
    }

    /**
     * 获得给定类所在包的名称<br>
     * 例如：<br>
     * com.xiaoleilu.hutool.util.ClassUtil =》 com.xiaoleilu.hutool.util
     *
     * @param clazz 类
     * @return 包名
     */
    public static String getPackage(Class<?> clazz) {
        if (clazz == null) {
            return GutilStr.EMPTY;
        }
        final String className = clazz.getName();
        int packageEndIndex = className.lastIndexOf(GutilStr.DOT);
        if (packageEndIndex == -1) {
            return GutilStr.EMPTY;
        }
        return className.substring(0, packageEndIndex);
    }

    /**
     * 获得给定类所在包的路径<br>
     * 例如：<br>
     * com.xiaoleilu.hutool.util.ClassUtil =》 com/xiaoleilu/hutool/util
     *
     * @param clazz 类
     * @return 包名
     */
    public static String getPackagePath(Class<?> clazz) {
        return getPackage(clazz).replace(GutilStr.C_DOT, GutilStr.C_SLASH);
    }

    /**
     * 获取指定类型分的默认值<br>
     * 默认值规则为：
     *
     * <pre>
     * 1、如果为原始类型，返回0
     * 2、非原始类型返回{@code null}
     * </pre>
     *
     * @param clazz 类
     * @return 默认值
     * @since 3.0.8
     */
    public static Object getDefaultValue(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            if (long.class == clazz) {
                return 0L;
            } else if (int.class == clazz) {
                return 0;
            } else if (short.class == clazz) {
                return (short) 0;
            } else if (char.class == clazz) {
                return (char) 0;
            } else if (byte.class == clazz) {
                return (byte) 0;
            } else if (double.class == clazz) {
                return 0D;
            } else if (float.class == clazz) {
                return 0f;
            } else if (boolean.class == clazz) {
                return false;
            }
        }

        return null;
    }

    /**
     * 获得默认值列表
     *
     * @param classes 值类型
     * @return 默认值列表
     * @since 3.0.9
     */
    public static Object[] getDefaultValues(Class<?>... classes) {
        final Object[] values = new Object[classes.length];
        for (int i = 0; i < classes.length; i++) {
            values[i] = getDefaultValue(classes[i]);
        }
        return values;
    }

    /**
     * 是否为JDK中定义的类或接口，判断依据：
     *
     * <pre>
     * 1、以java.、javax.开头的包名
     * 2、ClassLoader为null
     * </pre>
     *
     * @param clazz 被检查的类
     * @return 是否为JDK中定义的类或接口
     * @since 4.6.5
     */
    public static boolean isJdkClass(Class<?> clazz) {
        final Package objectPackage = clazz.getPackage();
        if (null == objectPackage) {
            return false;
        }
        final String objectPackageName = objectPackage.getName();
        return objectPackageName.startsWith("java.") //
                || objectPackageName.startsWith("javax.") //
                || clazz.getClassLoader() == null;
    }

    /**
     * 获取class类路径URL, 不管是否在jar包中都会返回文件夹的路径<br>
     * 类在jar包中返回jar所在文件夹,类不在jar中返回文件夹目录<br>
     * jdk中的类不能使用此方法
     *
     * <p>如果类的保护域或其代码源为{@code null}（例如JDK内置类、bootstrap类），返回{@code null}。
     *
     * @param clazz 类
     * @return URL，无法获取时返回{@code null}
     * @since 5.2.4
     */
    public static URL getLocation(Class<?> clazz) {
        if (null == clazz) {
            return null;
        }
        final ProtectionDomain protectionDomain = clazz.getProtectionDomain();
        if (null == protectionDomain || null == protectionDomain.getCodeSource()) {
            return null;
        }
        return protectionDomain.getCodeSource().getLocation();
    }

    /**
     * 获取class类路径, 不管是否在jar包中都会返回文件夹的路径<br>
     * 类在jar包中返回jar所在文件夹,类不在jar中返回文件夹目录<br>
     * jdk中的类不能使用此方法
     *
     * @param clazz 类
     * @return class路径，无法获取时返回{@code null}
     * @since 5.2.4
     */
    public static String getLocationPath(Class<?> clazz) {
        final URL location = getLocation(clazz);
        if (null == location) {
            return null;
        }
        return location.getPath();
    }
}
