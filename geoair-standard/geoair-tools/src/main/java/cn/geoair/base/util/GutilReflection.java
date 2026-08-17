package cn.geoair.base.util;

import cn.geoair.base.def.GkFilter;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** 来自spring ReflectionUtil */
public abstract class GutilReflection {

    /**
     * 预置的{@link MethodFilter}，匹配所有非桥接、非合成的、且不是声明在
     * {@code java.lang.Object}上的方法。
     *
     * @since 3.0.5
     */
    public static final MethodFilter USER_DECLARED_METHODS =
            (method -> !method.isBridge() && !method.isSynthetic());

    /** 预置的{@link FieldFilter}，匹配所有非静态、非final的字段。 */
    public static final FieldFilter COPYABLE_FIELDS =
            (field ->
                    !(Modifier.isStatic(field.getModifiers())
                            || Modifier.isFinal(field.getModifiers())));

    /**
     * CGLIB重命名方法的前缀。
     *
     * @see #isCglibRenamedMethod
     */
    private static final String CGLIB_RENAMED_METHOD_PREFIX = "CGLIB$";

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    private static final Method[] EMPTY_METHOD_ARRAY = new Method[0];

    private static final Field[] EMPTY_FIELD_ARRAY = new Field[0];

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * {@link Class#getDeclaredMethods()} 的缓存，同时包含 Java 8 接口默认方法，用于快速遍历<br>
     * 无界强引用缓存，通过 {@link #clearCache()} 可清空
     */
    private static final Map<Class<?>, Method[]> declaredMethodsCache =
            new ConcurrentHashMap<>(256);

    /**
     * {@link Class#getDeclaredFields()} 的缓存，用于快速遍历<br>
     * 无界强引用缓存，通过 {@link #clearCache()} 可清空
     */
    private static final Map<Class<?>, Field[]> declaredFieldsCache = new ConcurrentHashMap<>(256);

    // 异常处理

    /**
     * 处理给定的反射异常。
     *
     * <p>仅在目标方法不预期抛出受检异常，或访问方法/字段出错时调用。
     *
     * <p>如果{@link InvocationTargetException}的根因是运行时异常或错误，则直接抛出该根因；
     * 否则抛出带有相应信息的{@link IllegalStateException}或{@link UndeclaredThrowableException}。
     *
     * @param ex 待处理的反射异常
     */
    public static void handleReflectionException(Exception ex) {
        if (ex instanceof NoSuchMethodException) {
            throw new IllegalStateException("Method not found: " + ex.getMessage());
        }
        if (ex instanceof IllegalAccessException) {
            throw new IllegalStateException("Could not access method or field: " + ex.getMessage());
        }
        if (ex instanceof InvocationTargetException) {
            handleInvocationTargetException((InvocationTargetException) ex);
        }
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        throw new UndeclaredThrowableException(ex);
    }

    /**
     * 处理给定的调用目标异常。仅在目标方法不预期抛出受检异常时调用。
     *
     * <p>如果根因是运行时异常或错误则直接抛出；否则抛出{@link UndeclaredThrowableException}。
     *
     * @param ex 待处理的调用目标异常
     */
    public static void handleInvocationTargetException(InvocationTargetException ex) {
        rethrowRuntimeException(ex.getTargetException());
    }

    /**
     * 重新抛出给定的{@link Throwable}，该异常通常是{@link InvocationTargetException}的
     * <em>目标异常</em>。仅在目标方法不预期抛出受检异常时调用。
     *
     * <p>若异常可转换为{@link RuntimeException}或{@link Error}则直接抛出；
     * 否则抛出{@link UndeclaredThrowableException}。
     *
     * @param ex 待重新抛出的异常
     * @throws RuntimeException 重新抛出的异常
     */
    public static void rethrowRuntimeException(Throwable ex) {
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        if (ex instanceof Error) {
            throw (Error) ex;
        }
        throw new UndeclaredThrowableException(ex);
    }

    /**
     * 重新抛出给定的{@link Throwable}，该异常通常是{@link InvocationTargetException}的
     * <em>目标异常</em>。仅在目标方法不预期抛出受检异常时调用。
     *
     * <p>若异常可转换为{@link Exception}或{@link Error}则直接抛出；
     * 否则抛出{@link UndeclaredThrowableException}。
     *
     * @param ex 待重新抛出的异常
     * @throws Exception 重新抛出的异常（受检异常情况下）
     */
    public static void rethrowException(Throwable ex) throws Exception {
        if (ex instanceof Exception) {
            throw (Exception) ex;
        }
        if (ex instanceof Error) {
            throw (Error) ex;
        }
        throw new UndeclaredThrowableException(ex);
    }

    // 构造方法处理

    /**
     * 获得指定类中指定参数的可访问构造方法。
     *
     * @param clazz 待检查的类
     * @param parameterTypes 目标构造方法的参数类型
     * @return 构造方法引用
     * @throws NoSuchMethodException 如果不存在这样的构造方法
     * @since 5.0
     */
    public static <T> Constructor<T> accessibleConstructor(
            Class<T> clazz, Class<?>... parameterTypes) throws NoSuchMethodException {

        Constructor<T> ctor = clazz.getDeclaredConstructor(parameterTypes);
        makeAccessible(ctor);
        return ctor;
    }

    /**
     * 使给定的构造方法可访问，仅在必要时显式调用{@code setAccessible(true)}，
     * 以避免与JVM安全管理器（如果启用）产生不必要的冲突。
     *
     * @param ctor 需要设置为可访问的构造方法
     * @see java.lang.reflect.Constructor#setAccessible
     */
    // JDK 9 提示
    public static void makeAccessible(Constructor<?> ctor) {
        if ((!Modifier.isPublic(ctor.getModifiers())
                        || !Modifier.isPublic(ctor.getDeclaringClass().getModifiers()))
                && !ctor.isAccessible()) {
            ctor.setAccessible(true);
        }
    }

    // 方法处理

    /**
     * 在指定类上按方法名和空参数查找{@link Method}，会沿继承链向上搜索直到{@code Object}。
     *
     * <p>如果找不到任何{@link Method}则返回{@code null}。
     *
     * @param clazz 待检查的类
     * @param name 方法名
     * @return 方法对象，如果未找到返回{@code null}
     */
    public static Method findMethod(Class<?> clazz, String name) {
        return findMethod(clazz, name, EMPTY_CLASS_ARRAY);
    }

    /**
     * 在指定类上按方法名和参数类型查找{@link Method}，会沿继承链向上搜索直到{@code Object}。
     *
     * <p>如果找不到任何{@link Method}则返回{@code null}。
     *
     * @param clazz 待检查的类
     * @param name 方法名
     * @param paramTypes 方法的参数类型（可以为{@code null}表示任意签名）
     * @return 方法对象，如果未找到返回{@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        GutilAssert.notNull(name, () -> "Method name must not be null");
        Class<?> searchType = clazz;
        while (searchType != null) {
            Method[] methods =
                    (searchType.isInterface()
                            ? searchType.getMethods()
                            : getDeclaredMethods(searchType, false));
            for (Method method : methods) {
                if (name.equals(method.getName())
                        && (paramTypes == null || hasSameParams(method, paramTypes))) {
                    return method;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    private static boolean hasSameParams(Method method, Class<?>[] paramTypes) {
        return (paramTypes.length == method.getParameterCount()
                && Arrays.equals(paramTypes, method.getParameterTypes()));
    }

    /**
     * 在给定目标对象上调用指定的{@link Method}，无参数。目标对象在调用静态方法时可以为{@code null}。
     *
     * <p>抛出的异常通过调用{@link #handleReflectionException}处理。
     *
     * @param method 要调用的方法
     * @param target 要调用该方法的目标对象
     * @return 调用结果（如果有）
     * @see #invokeMethod(java.lang.reflect.Method, Object, Object[])
     */
    public static Object invokeMethod(Method method, Object target) {
        return invokeMethod(method, target, EMPTY_OBJECT_ARRAY);
    }

    /**
     * 在给定目标对象上以给定参数调用指定的{@link Method}。目标对象在调用静态方法时可以为{@code null}。
     *
     * <p>抛出的异常通过调用{@link #handleReflectionException}处理。
     *
     * @param method 要调用的方法
     * @param target 要调用该方法的目标对象
     * @param args 调用参数（可以为{@code null}）
     * @return 调用结果（如果有）
     */
    public static Object invokeMethod(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (Exception ex) {
            handleReflectionException(ex);
        }
        throw new IllegalStateException("Should never get here");
    }

    /**
     * 判断给定的方法是否显式声明了给定异常或其父类，即该异常可以在反射调用中原样传播。
     *
     * @param method 声明该异常的方法
     * @param exceptionType 要抛出的异常类型
     * @return {@code true} 表示该异常可以原样抛出；{@code false} 表示需要包装
     */
    public static boolean declaresException(Method method, Class<?> exceptionType) {
        GutilAssert.notNull(method, () -> "Method must not be null");
        Class<?>[] declaredExceptions = method.getExceptionTypes();
        for (Class<?> declaredException : declaredExceptions) {
            if (declaredException.isAssignableFrom(exceptionType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对给定类中所有本地声明（或等价物，如该类实现的Java 8接口上的默认方法）的匹配方法执行回调操作。
     *
     * @param clazz 待检查的类
     * @param mc 对每个方法执行的回调
     * @throws IllegalStateException 如果内省失败
     * @since 4.2
     * @see #doWithMethods
     */
    public static void doWithLocalMethods(Class<?> clazz, MethodCallback mc) {
        Method[] methods = getDeclaredMethods(clazz, false);
        for (Method method : methods) {
            try {
                mc.doWith(method);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(
                        "Not allowed to access method '" + method.getName() + "': " + ex);
            }
        }
    }

    /**
     * 对给定类及其父类中的所有匹配方法执行回调操作。
     *
     * <p>除非被{@link MethodFilter}排除，否则子类和父类中同名的方法会出现两次。
     *
     * @param clazz 待检查的类
     * @param mc 对每个方法执行的回调
     * @throws IllegalStateException 如果内省失败
     * @see #doWithMethods(Class, MethodCallback, MethodFilter)
     */
    public static void doWithMethods(Class<?> clazz, MethodCallback mc) {
        doWithMethods(clazz, mc, null);
    }

    /**
     * 对给定类及其父类（或给定接口及其父接口）中的所有匹配方法执行回调操作。
     *
     * <p>除非被指定的{@link MethodFilter}排除，否则子类和父类中同名的方法会出现两次。
     *
     * @param clazz 待检查的类
     * @param mc 对每个方法执行的回调
     * @param mf 决定对哪些方法执行回调的过滤器
     * @throws IllegalStateException 如果内省失败
     */
    public static void doWithMethods(Class<?> clazz, MethodCallback mc, MethodFilter mf) {
        // 沿继承层次向上回溯
        Method[] methods = getDeclaredMethods(clazz, false);
        for (Method method : methods) {
            if (mf != null && !mf.matches(method)) {
                continue;
            }
            try {
                mc.doWith(method);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(
                        "Not allowed to access method '" + method.getName() + "': " + ex);
            }
        }
        if (clazz.getSuperclass() != null
                && (mf != USER_DECLARED_METHODS || clazz.getSuperclass() != Object.class)) {
            doWithMethods(clazz.getSuperclass(), mc, mf);
        } else if (clazz.isInterface()) {
            for (Class<?> superIfc : clazz.getInterfaces()) {
                doWithMethods(superIfc, mc, mf);
            }
        }
    }

    /**
     * 获取叶子类及其所有父类上的全部声明方法，叶子类方法排在最前。
     *
     * @param leafClass 待检查的类
     * @throws IllegalStateException 如果内省失败
     */
    public static Method[] getAllDeclaredMethods(Class<?> leafClass) {
        final List<Method> methods = new ArrayList<>(20);
        doWithMethods(leafClass, methods::add);
        return methods.toArray(EMPTY_METHOD_ARRAY);
    }

    /**
     * 获取叶子类及其所有父类上的去重声明方法集合。叶子类方法排在最前，在沿继承链遍历时，
     * 与已收录方法签名相同的方法会被过滤掉。
     *
     * @param leafClass 待检查的类
     * @throws IllegalStateException 如果内省失败
     */
    public static Method[] getUniqueDeclaredMethods(Class<?> leafClass) {
        return getUniqueDeclaredMethods(leafClass, null);
    }

    /**
     * 获取叶子类及其所有父类上的去重声明方法集合。叶子类方法排在最前，在沿继承链遍历时，
     * 与已收录方法签名相同的方法会被过滤掉。
     *
     * @param leafClass 待检查的类
     * @param mf 决定收录哪些方法的过滤器
     * @throws IllegalStateException 如果内省失败
     * @since 5.2
     */
    public static Method[] getUniqueDeclaredMethods(Class<?> leafClass, MethodFilter mf) {
        final List<Method> methods = new ArrayList<>(20);
        doWithMethods(
                leafClass,
                method -> {
                    boolean knownSignature = false;
                    Method methodBeingOverriddenWithCovariantReturnType = null;
                    for (Method existingMethod : methods) {
                        if (method.getName().equals(existingMethod.getName())
                                && method.getParameterCount() == existingMethod.getParameterCount()
                                && Arrays.equals(
                                        method.getParameterTypes(),
                                        existingMethod.getParameterTypes())) {
                            // 是否是协变返回类型的情况？
                            if (existingMethod.getReturnType() != method.getReturnType()
                                    && existingMethod
                                            .getReturnType()
                                            .isAssignableFrom(method.getReturnType())) {
                                methodBeingOverriddenWithCovariantReturnType = existingMethod;
                            } else {
                                knownSignature = true;
                            }
                            break;
                        }
                    }
                    if (methodBeingOverriddenWithCovariantReturnType != null) {
                        methods.remove(methodBeingOverriddenWithCovariantReturnType);
                    }
                    if (!knownSignature && !isCglibRenamedMethod(method)) {
                        methods.add(method);
                    }
                },
                mf);
        return methods.toArray(EMPTY_METHOD_ARRAY);
    }

    /**
     * {@link Class#getDeclaredMethods()}的变体，使用本地缓存以避免JVM安全管理器检查和重复创建
     * Method实例。此外，它还会包含本地实现接口上的Java 8默认方法，因为它们在效果上等同于声明方法。
     *
     * @param clazz 待检查的类
     * @return 缓存的方法数组
     * @throws IllegalStateException 如果内省失败
     * @since 5.2
     * @see Class#getDeclaredMethods()
     */
    public static Method[] getDeclaredMethods(Class<?> clazz) {
        return getDeclaredMethods(clazz, true);
    }

    private static Method[] getDeclaredMethods(Class<?> clazz, boolean defensive) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        Method[] result = declaredMethodsCache.get(clazz);
        if (result == null) {
            try {
                Method[] declaredMethods = clazz.getDeclaredMethods();
                List<Method> defaultMethods = findConcreteMethodsOnInterfaces(clazz);
                if (defaultMethods != null) {
                    result = new Method[declaredMethods.length + defaultMethods.size()];
                    System.arraycopy(declaredMethods, 0, result, 0, declaredMethods.length);
                    int index = declaredMethods.length;
                    for (Method defaultMethod : defaultMethods) {
                        result[index] = defaultMethod;
                        index++;
                    }
                } else {
                    result = declaredMethods;
                }
                declaredMethodsCache.put(clazz, (result.length == 0 ? EMPTY_METHOD_ARRAY : result));
            } catch (Throwable ex) {
                throw new IllegalStateException(
                        "Failed to introspect Class ["
                                + clazz.getName()
                                + "] from ClassLoader ["
                                + clazz.getClassLoader()
                                + "]",
                        ex);
            }
        }
        return (result.length == 0 || !defensive) ? result : result.clone();
    }

    private static List<Method> findConcreteMethodsOnInterfaces(Class<?> clazz) {
        List<Method> result = null;
        for (Class<?> ifc : clazz.getInterfaces()) {
            for (Method ifcMethod : ifc.getMethods()) {
                if (!Modifier.isAbstract(ifcMethod.getModifiers())) {
                    if (result == null) {
                        result = new ArrayList<>();
                    }
                    result.add(ifcMethod);
                }
            }
        }
        return result;
    }

    /**
     * 判断给定方法是否为"equals"方法。
     *
     * @see java.lang.Object#equals(Object)
     */
    public static boolean isEqualsMethod(Method method) {
        if (method == null) {
            return false;
        }
        if (method.getParameterCount() != 1) {
            return false;
        }
        if (!method.getName().equals("equals")) {
            return false;
        }
        return method.getParameterTypes()[0] == Object.class;
    }

    /**
     * 判断给定方法是否为"hashCode"方法。
     *
     * @see java.lang.Object#hashCode()
     */
    public static boolean isHashCodeMethod(Method method) {
        return method != null
                && method.getParameterCount() == 0
                && method.getName().equals("hashCode");
    }

    /**
     * 判断给定方法是否为"toString"方法。
     *
     * @see java.lang.Object#toString()
     */
    public static boolean isToStringMethod(Method method) {
        return (method != null
                && method.getParameterCount() == 0
                && method.getName().equals("toString"));
    }

    /** 判断给定方法是否最初由{@link java.lang.Object}声明。 */
    public static boolean isObjectMethod(Method method) {
        return (method != null
                && (method.getDeclaringClass() == Object.class
                        || isEqualsMethod(method)
                        || isHashCodeMethod(method)
                        || isToStringMethod(method)));
    }

    /**
     * 判断给定方法是否为CGLIB'重命名'方法，符合"CGLIB$methodName$0"模式。
     *
     * @param renamedMethod 待检查的方法
     */
    public static boolean isCglibRenamedMethod(Method renamedMethod) {
        String name = renamedMethod.getName();
        if (name.startsWith(CGLIB_RENAMED_METHOD_PREFIX)) {
            int i = name.length() - 1;
            while (i >= 0 && Character.isDigit(name.charAt(i))) {
                i--;
            }
            return (i > CGLIB_RENAMED_METHOD_PREFIX.length()
                    && (i < name.length() - 1)
                    && name.charAt(i) == '$');
        }
        return false;
    }

    /**
     * 使给定的方法可访问，仅在必要时显式调用{@code setAccessible(true)}，以避免与JVM安全管理器
     * （如果启用）产生不必要的冲突。
     *
     * @param method 需要设置为可访问的方法
     * @see java.lang.reflect.Method#setAccessible
     */
    @SuppressWarnings("deprecation") // JDK 9 提示
    public static void makeAccessible(Method method) {
        if ((!Modifier.isPublic(method.getModifiers())
                        || !Modifier.isPublic(method.getDeclaringClass().getModifiers()))
                && !method.isAccessible()) {
            method.setAccessible(true);
        }
    }

    // 字段处理

    /**
     * 尝试在指定{@link Class}上按指定{@code name}查找{@link Field field}。
     * 会沿继承链向上搜索直到{@link Object}。
     *
     * @param clazz 待检查的类
     * @param name 字段名
     * @return 对应的Field对象，如果未找到返回{@code null}
     */
    public static Field findField(Class<?> clazz, String name) {
        return findField(clazz, name, null);
    }

    /**
     * 尝试在指定{@link Class}上按指定{@code name}和/或{@link Class type}查找{@link Field field}。
     * 会沿继承链向上搜索直到{@link Object}。
     *
     * @param clazz 待检查的类
     * @param name 字段名（如果指定了type可以为{@code null}）
     * @param type 字段类型（如果指定了name可以为{@code null}）
     * @return 对应的Field对象，如果未找到返回{@code null}
     */
    public static Field findField(Class<?> clazz, String name, Class<?> type) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        GutilAssert.isTrue(
                name != null || type != null, "Either name or type of the field must be specified");
        Class<?> searchType = clazz;
        while (Object.class != searchType && searchType != null) {
            Field[] fields = getDeclaredFields(searchType);
            for (Field field : fields) {
                if ((name == null || name.equals(field.getName()))
                        && (type == null || type.equals(field.getType()))) {
                    return field;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    /**
     * 查找Field 将查找当前类和父类 的declaredFields
     *
     * @param clazz
     * @param predicate
     * @return
     */
    public static Field findField(Class<?> clazz, Predicate<Field> predicate) {

        GutilAssert.notNull(clazz, () -> "Class must not be null");
        Class<?> searchType = clazz;
        while (Object.class != searchType && searchType != null) {
            Field[] fields = getDeclaredFields(searchType);
            for (Field field : fields) {
                boolean math = predicate.test(field);
                if (math) {
                    return field;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    /**
     * 将指定{@linkplain Field field对象}表示的字段在指定{@linkplain Object target对象}上设置为指定{@code value}。
     *
     * <p>按照{@link Field#set(Object, Object)}语义，如果底层字段是原始类型，新值会自动拆箱。
     *
     * <p>此方法不支持设置{@code static final}字段。
     *
     * <p>抛出的异常通过调用{@link #handleReflectionException(Exception)}处理。
     *
     * @param field 要设置的字段
     * @param target 要设置字段的目标对象（静态字段时为{@code null}）
     * @param value 要设置的值（可以为{@code null}）
     */
    public static void setField(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException ex) {
            handleReflectionException(ex);
        }
    }

    /**
     * 获取指定{@link Field field对象}表示的字段在指定{@link Object target对象}上的值。
     * 按照{@link Field#get(Object)}语义，如果底层字段是原始类型，返回值会自动装箱。
     *
     * <p>抛出的异常通过调用{@link #handleReflectionException(Exception)}处理。
     *
     * @param field 要获取的字段
     * @param target 从中获取字段的目标对象（静态字段时为{@code null}）
     * @return 字段的当前值
     */
    public static Object getField(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException ex) {
            handleReflectionException(ex);
        }
        throw new IllegalStateException("Should never get here");
    }

    /**
     * 对给定类中所有本地声明的字段执行回调操作。
     *
     * @param clazz 要分析的类
     * @param fc 对每个字段执行的回调
     * @throws IllegalStateException 如果内省失败
     * @since 4.2
     * @see #doWithFields
     */
    public static void doWithLocalFields(Class<?> clazz, FieldCallback fc) {
        for (Field field : getDeclaredFields(clazz)) {
            try {
                fc.doWith(field);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(
                        "Not allowed to access field '" + field.getName() + "': " + ex);
            }
        }
    }

    /**
     * 对目标类中所有字段执行回调操作，会沿类层次向上遍历以获取全部声明字段。
     *
     * @param clazz 要分析的类
     * @param fc 对每个字段执行的回调
     * @throws IllegalStateException 如果内省失败
     */
    public static void doWithFields(Class<?> clazz, FieldCallback fc) {
        doWithFields(clazz, fc, null);
    }

    /**
     * 对目标类中所有字段执行回调操作，会沿类层次向上遍历以获取全部声明字段。
     *
     * @param clazz 要分析的类
     * @param fc 对每个字段执行的回调
     * @param ff 决定对哪些字段执行回调的过滤器
     * @throws IllegalStateException 如果内省失败
     */
    public static void doWithFields(Class<?> clazz, FieldCallback fc, FieldFilter ff) {
        // 沿继承层次向上回溯
        Class<?> targetClass = clazz;
        do {
            Field[] fields = getDeclaredFields(targetClass);
            for (Field field : fields) {
                if (ff != null && !ff.matches(field)) {
                    continue;
                }
                try {
                    fc.doWith(field);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(
                            "Not allowed to access field '" + field.getName() + "': " + ex);
                }
            }
            targetClass = targetClass.getSuperclass();
        } while (targetClass != null && targetClass != Object.class);
    }

    /**
     * 此变体从本地缓存获取{@link Class#getDeclaredFields()}，以避免JVM安全管理器检查和防御性数组拷贝。
     *
     * @param clazz 待检查的类
     * @return 缓存的字段数组
     * @throws IllegalStateException 如果内省失败
     * @see Class#getDeclaredFields()
     */
    private static Field[] getDeclaredFields(Class<?> clazz) {
        GutilAssert.notNull(clazz, () -> "Class must not be null");
        Field[] result = declaredFieldsCache.get(clazz);
        if (result == null) {
            try {
                result = clazz.getDeclaredFields();
                declaredFieldsCache.put(clazz, (result.length == 0 ? EMPTY_FIELD_ARRAY : result));
            } catch (Throwable ex) {
                throw new IllegalStateException(
                        "Failed to introspect Class ["
                                + clazz.getName()
                                + "] from ClassLoader ["
                                + clazz.getClassLoader()
                                + "]",
                        ex);
            }
        }
        return result;
    }

    /**
     * 在源对象和目标对象之间复制所有字段（包括继承字段），要求两者是同一个类或子类关系。
     * 设计用于具有公共无参构造函数的对象。
     *
     * @throws IllegalStateException 如果内省失败
     */
    public static void shallowCopyFieldState(final Object src, final Object dest) {
        GutilAssert.notNull(src, () -> "Source for field copy cannot be null");
        GutilAssert.notNull(dest, () -> "Destination for field copy cannot be null");
        if (!src.getClass().isAssignableFrom(dest.getClass())) {
            throw new IllegalArgumentException(
                    "Destination class ["
                            + dest.getClass().getName()
                            + "] must be same or subclass as source class ["
                            + src.getClass().getName()
                            + "]");
        }
        doWithFields(
                src.getClass(),
                field -> {
                    makeAccessible(field);
                    Object srcValue = field.get(src);
                    field.set(dest, srcValue);
                },
                COPYABLE_FIELDS);
    }

    /**
     * 判断给定字段是否为"public static final"常量。
     *
     * @param field 待检查的字段
     */
    public static boolean isPublicStaticFinal(Field field) {
        int modifiers = field.getModifiers();
        return (Modifier.isPublic(modifiers)
                && Modifier.isStatic(modifiers)
                && Modifier.isFinal(modifiers));
    }

    /**
     * 使给定的字段可访问，仅在必要时显式调用{@code setAccessible(true)}，以避免与JVM安全管理器
     * （如果启用）产生不必要的冲突。
     *
     * @param field 需要设置为可访问的字段
     * @see java.lang.reflect.Field#setAccessible
     */
    @SuppressWarnings("deprecation") // JDK 9 提示
    public static void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers())
                        || !Modifier.isPublic(field.getDeclaringClass().getModifiers())
                        || Modifier.isFinal(field.getModifiers()))
                && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    /**
     * 设置可访问对象的可访问权限为 true
     *
     * @param object 可访问的对象
     * @param <T> 类型
     * @return 返回设置后的对象
     */

    // 缓存处理

    /**
     * 清空内部所有缓存，包括声明方法缓存、声明字段缓存以及构造器、字段、方法三个无界强引用缓存。
     *
     * @since 4.2.4
     */
    public static void clearCache() {
        declaredMethodsCache.clear();
        declaredFieldsCache.clear();
        CONSTRUCTORS_CACHE.clear();
        FIELDS_CACHE.clear();
        METHODS_CACHE.clear();
    }

    /** 对每个方法执行的操作。 */
    @FunctionalInterface
    public interface MethodCallback {

        /**
         * 使用给定的方法执行操作。
         *
         * @param method 要操作的方法
         */
        void doWith(Method method) throws IllegalArgumentException, IllegalAccessException;
    }

    /** 回调接口，可选地用于过滤方法回调要操作的方法。 */
    @FunctionalInterface
    public interface MethodFilter {

        /**
         * 判断给定的方法是否匹配。
         *
         * @param method 待检查的方法
         */
        boolean matches(Method method);

        /**
         * 基于此过滤器<em>和</em>给定过滤器创建复合过滤器。
         *
         * <p>如果此过滤器不匹配，则不会应用下一个过滤器。
         *
         * @param next 下一个{@code MethodFilter}
         * @return 复合的{@code MethodFilter}
         * @throws IllegalArgumentException 如果MethodFilter参数为{@code null}
         * @since 5.3.2
         */
        default MethodFilter and(MethodFilter next) {
            GutilAssert.notNull(next, () -> "Next MethodFilter must not be null");
            return method -> matches(method) && next.matches(method);
        }
    }

    /** 对继承层次中的每个字段执行操作的回调接口。 */
    @FunctionalInterface
    public interface FieldCallback {

        /**
         * 使用给定的字段执行操作。
         *
         * @param field 要操作的字段
         */
        void doWith(Field field) throws IllegalArgumentException, IllegalAccessException;
    }

    /** 回调接口，可选地用于过滤字段回调要操作的字段。 */
    @FunctionalInterface
    public interface FieldFilter {

        /**
         * 判断给定的字段是否匹配。
         *
         * @param field 待检查的字段
         */
        boolean matches(Field field);

        /**
         * 基于此过滤器<em>和</em>给定过滤器创建复合过滤器。
         *
         * <p>如果此过滤器不匹配，则不会应用下一个过滤器。
         *
         * @param next 下一个{@code FieldFilter}
         * @return 复合的{@code FieldFilter}
         * @throws IllegalArgumentException 如果FieldFilter参数为{@code null}
         * @since 5.3.2
         */
        default FieldFilter and(FieldFilter next) {
            GutilAssert.notNull(next, () -> "Next FieldFilter must not be null");
            return field -> matches(field) && next.matches(field);
        }
    }

    /**
     * 构造器缓存，无界强引用缓存，键为类，值为该类所有构造器数组<br>
     * 通过 {@link #clearCache()} 可清空
     */
    private static final Map<Class<?>, Constructor<?>[]> CONSTRUCTORS_CACHE =
            new ConcurrentHashMap<>(256);

    /**
     * 字段缓存，无界强引用缓存，键为类，值为该类及其父类所有字段数组<br>
     * 通过 {@link #clearCache()} 可清空
     */
    private static final Map<Class<?>, Field[]> FIELDS_CACHE = new ConcurrentHashMap<>(256);

    /**
     * 方法缓存，无界强引用缓存，键为类，值为该类及其父类所有方法数组<br>
     * 通过 {@link #clearCache()} 可清空
     */
    private static final Map<Class<?>, Method[]> METHODS_CACHE = new ConcurrentHashMap<>(256);

    // ---------------------------------------------------------------------------------------------------------
    // 构造方法

    /**
     * 查找类中的指定参数的构造方法，如果找到构造方法，会自动设置可访问为true
     *
     * @param <T> 对象类型
     * @param clazz 类
     * @param parameterTypes 参数类型，只要任何一个参数是指定参数的父类或接口或相等即可，此参数可以不传
     * @return 构造方法，如果未找到返回null
     */
    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... parameterTypes) {
        if (null == clazz) {
            return null;
        }

        final Constructor<?>[] constructors = getConstructors(clazz);
        Class<?>[] pts;
        for (Constructor<?> constructor : constructors) {
            pts = constructor.getParameterTypes();
            if (GutilClass.isAllAssignableFrom(pts, parameterTypes)) {
                // 构造可访问
                setAccessible(constructor);
                return (Constructor<T>) constructor;
            }
        }
        return null;
    }

    /**
     * 获得一个类中所有构造列表
     *
     * @param <T> 构造的对象类型
     * @param beanClass 类
     * @return 字段列表
     * @throws SecurityException 安全检查异常
     */
    @SuppressWarnings("unchecked")
    public static <T> Constructor<T>[] getConstructors(Class<T> beanClass)
            throws SecurityException {
        GutilAssert.notNull(beanClass, "");
        Constructor<?>[] constructors = CONSTRUCTORS_CACHE.get(beanClass);
        if (null != constructors) {
            return (Constructor<T>[]) constructors.clone();
        }

        constructors = getConstructorsDirectly(beanClass);
        CONSTRUCTORS_CACHE.put(beanClass, constructors);
        return (Constructor<T>[]) constructors.clone();
    }

    /**
     * 获得一个类中所有构造列表，直接反射获取，无缓存
     *
     * @param beanClass 类
     * @return 字段列表
     * @throws SecurityException 安全检查异常
     */
    public static Constructor<?>[] getConstructorsDirectly(Class<?> beanClass)
            throws SecurityException {
        GutilAssert.notNull(beanClass, "");
        return beanClass.getDeclaredConstructors();
    }

    // ---------------------------------------------------------------------------------------------------------
    // 字段

    /**
     * 查找指定类中是否包含指定名称对应的字段，包括所有字段（包括非public字段），也包括父类和Object类的字段
     *
     * @param beanClass 被查找字段的类,不能为null
     * @param name 字段名
     * @return 是否包含字段
     * @throws SecurityException 安全异常
     * @since 4.1.21
     */
    public static boolean hasField(Class<?> beanClass, String name) throws SecurityException {
        return null != getField(beanClass, name);
    }

    /**
     * 获取字段名，如果存在{@link }注解，读取注解的值作为名称
     *
     * @param field 字段
     * @return 字段名
     * @since 5.1.6
     */
    public static String getFieldName(Field field) {
        return field.getName();
    }

    /**
     * 查找指定类中的指定name的字段（包括非public字段），也包括父类和Object类的字段， 字段不存在则返回<code>null</code>
     *
     * @param beanClass 被查找字段的类,不能为null
     * @param name 字段名
     * @return 字段
     * @throws SecurityException 安全异常
     */
    public static Field getField(Class<?> beanClass, String name) throws SecurityException {
        final Field[] fields = getFields(beanClass);
        return GutilArray.firstMatch((field) -> name.equals(getFieldName(field)), fields);
    }

    /**
     * 获取指定类中字段名和字段对应的有序Map，包括其父类中的字段<br>
     * 如果子类与父类中存在同名字段，则这两个字段同时存在，子类字段在前，父类字段在后。
     *
     * @param beanClass 类
     * @return 字段名和字段对应的Map，有序
     * @since 5.0.7
     */
    public static Map<String, Field> getFieldMap(Class<?> beanClass) {
        final Field[] fields = getFields(beanClass);
        final HashMap<String, Field> map = newHashMap(fields.length, true);
        for (Field field : fields) {
            map.put(field.getName(), field);
        }
        return map;
    }

    private static <K, V> HashMap<K, V> newHashMap(int size, boolean isOrder) {
        int initialCapacity = (int) (size / 0.75f) + 1;
        return isOrder ? new LinkedHashMap<>(initialCapacity) : new HashMap<>(initialCapacity);
    }

    /**
     * 获得一个类中所有字段列表，包括其父类中的字段<br>
     * 如果子类与父类中存在同名字段，则这两个字段同时存在，子类字段在前，父类字段在后。
     *
     * @param beanClass 类
     * @return 字段列表
     * @throws SecurityException 安全检查异常
     */
    public static Field[] getFields(Class<?> beanClass) throws SecurityException {
        Field[] allFields = FIELDS_CACHE.get(beanClass);
        if (null != allFields) {
            return allFields.clone();
        }

        allFields = getFieldsDirectly(beanClass, true);
        FIELDS_CACHE.put(beanClass, allFields);
        return allFields.clone();
    }

    /**
     * 获得一个类中所有字段列表，直接反射获取，无缓存<br>
     * 如果子类与父类中存在同名字段，则这两个字段同时存在，子类字段在前，父类字段在后。
     *
     * @param beanClass 类
     * @param withSuperClassFields 是否包括父类的字段列表
     * @return 字段列表
     * @throws SecurityException 安全检查异常
     */
    public static Field[] getFieldsDirectly(Class<?> beanClass, boolean withSuperClassFields)
            throws SecurityException {
        GutilAssert.notNull(beanClass, "");

        Field[] allFields = null;
        Class<?> searchType = beanClass;
        Field[] declaredFields;
        while (searchType != null) {
            declaredFields = searchType.getDeclaredFields();
            if (null == allFields) {
                allFields = declaredFields;
            } else {
                allFields = GutilArray.append(allFields, declaredFields);
            }
            searchType = withSuperClassFields ? searchType.getSuperclass() : null;
        }

        return allFields;
    }

    /**
     * 获取字段值
     *
     * @param obj 对象，如果static字段，此处为类
     * @param fieldName 字段名
     * @return 字段值
     * @throws IllegalStateException 包装IllegalAccessException异常
     */
    public static Object getFieldValue(Object obj, String fieldName) throws IllegalStateException {
        if (null == obj || GutilStr.isBlank(fieldName)) {
            return null;
        }
        return getFieldValue(
                obj, getField(obj instanceof Class ? (Class<?>) obj : obj.getClass(), fieldName));
    }

    /**
     * 获取静态字段值
     *
     * @param field 字段
     * @return 字段值
     * @throws IllegalStateException 包装IllegalAccessException异常
     * @since 5.1.0
     */
    public static Object getStaticFieldValue(Field field) throws IllegalStateException {
        return getFieldValue(null, field);
    }

    /**
     * 获取字段值
     *
     * @param obj 对象，static字段则此字段为null
     * @param field 字段
     * @return 字段值
     * @throws IllegalStateException 包装IllegalAccessException异常
     */
    public static Object getFieldValue(Object obj, Field field) throws IllegalStateException {
        if (null == field) {
            return null;
        }
        if (obj instanceof Class) {
            // 静态字段获取时对象为null
            obj = null;
        }

        setAccessible(field);
        Object result;
        try {
            result = field.get(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    GutilStr.format(
                            "IllegalAccess for {}.{}", field.getDeclaringClass(), field.getName()),
                    e);
        }
        return result;
    }

    /**
     * 获取所有字段的值
     *
     * @param obj bean对象，如果是static字段，此处为类class
     * @return 字段值数组
     * @since 4.1.17
     */
    public static Object[] getFieldsValue(Object obj) {
        if (null != obj) {
            final Field[] fields =
                    getFields(obj instanceof Class ? (Class<?>) obj : obj.getClass());
            if (null != fields) {
                final Object[] values = new Object[fields.length];
                for (int i = 0; i < fields.length; i++) {
                    values[i] = getFieldValue(obj, fields[i]);
                }
                return values;
            }
        }
        return null;
    }

    /**
     * 设置字段值
     *
     * @param obj 对象,static字段则此处传Class
     * @param fieldName 字段名
     * @param value 值，值类型必须与字段类型匹配，不会自动转换对象类型
     * @throws IllegalStateException 包装IllegalAccessException异常
     */
    public static void setFieldValue(Object obj, String fieldName, Object value)
            throws IllegalStateException {
        GutilAssert.notNull(obj, "");
        GutilAssert.hasText(fieldName, "");

        final Field field =
                getField((obj instanceof Class) ? (Class<?>) obj : obj.getClass(), fieldName);
        GutilAssert.notNull(
                field,
                GutilStr.format(
                        "Field [{}] is not exist in [{}]", fieldName, obj.getClass().getName()));
        setFieldValue(obj, field, value);
    }

    /**
     * 设置字段值
     *
     * @param obj 对象，如果是static字段，此参数为null
     * @param field 字段
     * @param value 值，值类型必须与字段类型匹配，不会自动转换对象类型
     * @throws IllegalStateException 包装IllegalAccessException异常
     */
    public static void setFieldValue(Object obj, Field field, Object value)
            throws IllegalStateException {
        GutilAssert.notNull(field, GutilStr.format("Field in [{}] not exist !", obj));

        final Class<?> fieldType = field.getType();
        if (null == value) {
            // 获取null对应默认值，防止原始类型造成空指针问题
            value = GutilClass.getDefaultValue(fieldType);
        }

        setAccessible(field);
        try {
            field.set(obj instanceof Class ? null : obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    GutilStr.format("IllegalAccess for {}.{}", obj, field.getName()), e);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // 方法

    /**
     * 获得指定类本类及其父类中的Public方法名<br>
     * 去重重载的方法
     *
     * @param clazz 类
     * @return 方法名Set
     */
    public static Set<String> getPublicMethodNames(Class<?> clazz) {
        final HashSet<String> methodSet = new HashSet<>();
        final Method[] methodArray = getPublicMethods(clazz);
        if (GutilArray.isNotEmpty(methodArray)) {
            for (Method method : methodArray) {
                methodSet.add(method.getName());
            }
        }
        return methodSet;
    }

    /**
     * 获得本类及其父类所有Public方法
     *
     * @param clazz 查找方法的类
     * @return 过滤后的方法列表
     */
    public static Method[] getPublicMethods(Class<?> clazz) {
        return null == clazz ? null : clazz.getMethods();
    }

    /**
     * 获得指定类过滤后的Public方法列表
     *
     * @param clazz 查找方法的类
     * @param filter 过滤器
     * @return 过滤后的方法列表
     */

    /**
     * 获得指定类过滤后的Public方法列表
     *
     * @param clazz 查找方法的类
     * @param excludeMethods 不包括的方法
     * @return 过滤后的方法列表
     */

    /**
     * 获得指定类过滤后的Public方法列表
     *
     * @param clazz 查找方法的类
     * @param excludeMethodNames 不包括的方法名列表
     * @return 过滤后的方法列表
     */

    /**
     * 查找指定Public方法 如果找不到对应的方法或方法不为public的则返回<code>null</code>
     *
     * @param clazz 类
     * @param methodName 方法名
     * @param paramTypes 参数类型
     * @return 方法
     * @throws SecurityException 无权访问抛出异常
     */
    public static Method getPublicMethod(Class<?> clazz, String methodName, Class<?>... paramTypes)
            throws SecurityException {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * 查找指定对象中的所有方法（包括非public方法），也包括父对象和Object类的方法
     *
     * <p>此方法为精准获取方法名，即方法名和参数数量和类型必须一致，否则返回<code>null</code>。
     *
     * @param obj 被查找的对象，如果为{@code null}返回{@code null}
     * @param methodName 方法名，如果为空字符串返回{@code null}
     * @param args 参数
     * @return 方法
     * @throws SecurityException 无访问权限抛出异常
     */
    public static Method getMethodOfObj(Object obj, String methodName, Object... args)
            throws SecurityException {
        if (null == obj || GutilStr.isBlank(methodName)) {
            return null;
        }
        return getMethod(obj.getClass(), methodName, GutilClass.getClasses(args));
    }

    /**
     * 忽略大小写查找指定方法，如果找不到对应的方法则返回<code>null</code>
     *
     * <p>此方法为精准获取方法名，即方法名和参数数量和类型必须一致，否则返回<code>null</code>。
     *
     * @param clazz 类，如果为{@code null}返回{@code null}
     * @param methodName 方法名，如果为空字符串返回{@code null}
     * @param paramTypes 参数类型，指定参数类型如果是方法的子类也算
     * @return 方法
     * @throws SecurityException 无权访问抛出异常
     * @since 3.2.0
     */
    public static Method getMethodIgnoreCase(
            Class<?> clazz, String methodName, Class<?>... paramTypes) throws SecurityException {
        return getMethod(clazz, true, methodName, paramTypes);
    }

    /**
     * 查找指定方法 如果找不到对应的方法则返回<code>null</code>
     *
     * <p>此方法为精准获取方法名，即方法名和参数数量和类型必须一致，否则返回<code>null</code>。
     *
     * @param clazz 类，如果为{@code null}返回{@code null}
     * @param methodName 方法名，如果为空字符串返回{@code null}
     * @param paramTypes 参数类型，指定参数类型如果是方法的子类也算
     * @return 方法
     * @throws SecurityException 无权访问抛出异常
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes)
            throws SecurityException {
        return getMethod(clazz, false, methodName, paramTypes);
    }

    /**
     * 查找指定方法 如果找不到对应的方法则返回<code>null</code>
     *
     * <p>此方法为精准获取方法名，即方法名和参数数量和类型必须一致，否则返回<code>null</code>。
     *
     * @param clazz 类，如果为{@code null}返回{@code null}
     * @param ignoreCase 是否忽略大小写
     * @param methodName 方法名，如果为空字符串返回{@code null}
     * @param paramTypes 参数类型，指定参数类型如果是方法的子类也算
     * @return 方法
     * @throws SecurityException 无权访问抛出异常
     * @since 3.2.0
     */
    public static Method getMethod(
            Class<?> clazz, boolean ignoreCase, String methodName, Class<?>... paramTypes)
            throws SecurityException {
        if (null == clazz || GutilStr.isBlank(methodName)) {
            return null;
        }

        final Method[] methods = getMethods(clazz);
        if (GutilArray.isNotEmpty(methods)) {
            for (Method method : methods) {
                if (GutilStr.equals(methodName, method.getName(), ignoreCase)) {
                    if (GutilClass.isAllAssignableFrom(method.getParameterTypes(), paramTypes)) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 按照方法名查找指定方法名的方法，只返回匹配到的第一个方法，如果找不到对应的方法则返回<code>null</code>
     *
     * <p>此方法只检查方法名是否一致，并不检查参数的一致性。
     *
     * @param clazz 类，如果为{@code null}返回{@code null}
     * @param methodName 方法名，如果为空字符串返回{@code null}
     * @return 方法
     * @throws SecurityException 无权访问抛出异常
     * @since 4.3.2
     */
    public static Method getMethodByName(Class<?> clazz, String methodName)
            throws SecurityException {
        return getMethodByName(clazz, false, methodName);
    }

    /**
     * 按照方法名查找指定方法名的方法，只返回匹配到的第一个方法，如果找不到对应的方法则返回<code>null</code>
     *
     * <p>此方法只检查方法名是否一致（忽略大小写），并不检查参数的一致性。
     *
     * @param clazz 类，如果为{@code null}返回{@code null}
     * @param methodName 方法名，如果为空字符串返回{@code null}
     * @return 方法
     * @throws SecurityException 无权访问抛出异常
     * @since 4.3.2
     */
    public static Method getMethodByNameIgnoreCase(Class<?> clazz, String methodName)
            throws SecurityException {
        return getMethodByName(clazz, true, methodName);
    }

    /**
     * 按照方法名查找指定方法名的方法，只返回匹配到的第一个方法，如果找不到对应的方法则返回<code>null</code>
     *
     * <p>此方法只检查方法名是否一致，并不检查参数的一致性。
     *
     * @param clazz 类，如果为{@code null}返回{@code null}
     * @param ignoreCase 是否忽略大小写
     * @param methodName 方法名，如果为空字符串返回{@code null}
     * @return 方法
     * @throws SecurityException 无权访问抛出异常
     * @since 4.3.2
     */
    public static Method getMethodByName(Class<?> clazz, boolean ignoreCase, String methodName)
            throws SecurityException {
        if (null == clazz || GutilStr.isBlank(methodName)) {
            return null;
        }

        final Method[] methods = getMethods(clazz);
        if (GutilArray.isNotEmpty(methods)) {
            for (Method method : methods) {
                if (GutilStr.equals(methodName, method.getName(), ignoreCase)) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * 获得指定类中的Public方法名<br>
     * 去重重载的方法
     *
     * @param clazz 类
     * @return 方法名Set
     * @throws SecurityException 安全异常
     */
    public static Set<String> getMethodNames(Class<?> clazz) throws SecurityException {
        final HashSet<String> methodSet = new HashSet<>();
        final Method[] methods = getMethods(clazz);
        for (Method method : methods) {
            methodSet.add(method.getName());
        }
        return methodSet;
    }

    /**
     * 获得指定类过滤后的Public方法列表
     *
     * @param clazz 查找方法的类
     * @param filter 过滤器
     * @return 过滤后的方法列表
     * @throws SecurityException 安全异常
     */
    public static Method[] getMethods(Class<?> clazz, GkFilter<Method> filter)
            throws SecurityException {
        if (null == clazz) {
            return null;
        }
        return GutilArray.filter(getMethods(clazz), filter);
    }

    /**
     * 获得一个类中所有方法列表，包括其父类中的方法
     *
     * @param beanClass 类
     * @return 方法列表
     * @throws SecurityException 安全检查异常
     */
    public static Method[] getMethods(Class<?> beanClass) throws SecurityException {
        Method[] allMethods = METHODS_CACHE.get(beanClass);
        if (null != allMethods) {
            return allMethods.clone();
        }

        allMethods = getMethodsDirectly(beanClass, true);
        METHODS_CACHE.put(beanClass, allMethods);
        return allMethods.clone();
    }

    /**
     * 获得一个类中所有方法列表，直接反射获取，无缓存
     *
     * @param beanClass 类
     * @param withSuperClassMethods 是否包括父类的方法列表
     * @return 方法列表
     * @throws SecurityException 安全检查异常
     */
    public static Method[] getMethodsDirectly(Class<?> beanClass, boolean withSuperClassMethods)
            throws SecurityException {
        GutilAssert.notNull(beanClass, "");

        Method[] allMethods = null;
        Class<?> searchType = beanClass;
        Method[] declaredMethods;
        while (searchType != null) {
            declaredMethods = searchType.getDeclaredMethods();
            if (null == allMethods) {
                allMethods = declaredMethods;
            } else {
                allMethods = GutilArray.append(allMethods, declaredMethods);
            }
            searchType = withSuperClassMethods ? searchType.getSuperclass() : null;
        }

        return allMethods;
    }

    /**
     * 是否为无参数方法
     *
     * @param method 方法
     * @return 是否为无参数方法
     * @since 5.1.1
     */
    public static boolean isEmptyParam(Method method) {
        return method.getParameterTypes().length == 0;
    }

    // ---------------------------------------------------------------------------------------------------------
    // 实例化

    /**
     * 实例化对象
     *
     * <p>类加载时优先使用线程上下文类加载器，线程上下文类加载器不可用时回退到本类类加载器。
     *
     * @param <T> 对象类型
     * @param clazz 类名
     * @return 对象
     * @throws IllegalStateException 包装各类异常
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(String clazz) throws IllegalStateException {
        try {
            return (T) GutilClass.forName(clazz, null).newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    GutilStr.format("Instance class [{}] error!", clazz), e);
        }
    }

    /**
     * 实例化对象
     *
     * @param <T> 对象类型
     * @param clazz 类
     * @param params 构造函数参数
     * @return 对象
     * @throws IllegalStateException 包装各类异常
     */
    public static <T> T newInstance(Class<T> clazz, Object... params) throws IllegalStateException {
        if (GutilArray.isEmpty(params)) {
            try { // 更新
                return clazz.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(
                        GutilStr.format("Instance class [{}] error!", clazz), e);
            }
        }

        final Class<?>[] paramTypes = GutilClass.getClasses(params);
        final Constructor<T> constructor = getConstructor(clazz, paramTypes);
        if (null == constructor) {
            throw new IllegalStateException(
                    GutilStr.format(
                            "No Constructor matched for parameter types: [{}]",
                            new Object[] {paramTypes}));
        }
        try {
            return constructor.newInstance(params);
        } catch (Exception e) {
            throw new IllegalStateException(
                    GutilStr.format("Instance class [{}] error!", clazz), e);
        }
    }

    /**
     * 尝试遍历并调用此类的所有构造方法，直到构造成功并返回
     *
     * <p>对于某些特殊的接口，按照其默认实现实例化，例如：
     *
     * <pre>
     *     Map       -》 HashMap
     *     Collction -》 ArrayList
     *     List      -》 ArrayList
     *     Set       -》 HashSet
     * </pre>
     *
     * <p>如果默认构造和所有其它构造均创建失败，返回{@code null}。
     *
     * @param <T> 对象类型
     * @param beanClass 被构造的类
     * @return 构造后的对象，创建失败返回{@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstanceIfPossible(Class<T> beanClass) {
        GutilAssert.notNull(beanClass, "");

        // 某些特殊接口的实例化按照默认实现进行
        if (AbstractMap.class.isAssignableFrom(beanClass)) {
            beanClass = (Class<T>) HashMap.class;
        } else if (beanClass.isAssignableFrom(List.class)) {
            beanClass = (Class<T>) ArrayList.class;
        } else if (beanClass.isAssignableFrom(Set.class)) {
            beanClass = (Class<T>) HashSet.class;
        }

        try {
            return newInstance(beanClass);
        } catch (Exception e) {
            // 默认构造不存在的情况下查找其它构造
        }

        final Constructor<T>[] constructors = getConstructors(beanClass);
        Class<?>[] parameterTypes;
        for (Constructor<T> constructor : constructors) {
            parameterTypes = constructor.getParameterTypes();
            if (0 == parameterTypes.length) {
                continue;
            }
            setAccessible(constructor);
            try {
                return constructor.newInstance(GutilClass.getDefaultValues(parameterTypes));
            } catch (Exception ignore) {
                // 构造出错时继续尝试下一种构造方式
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------------------
    // 调用

    /**
     * 执行静态方法（历史版本说明）
     *
     * <p>被调用方法为静态方法时，目标对象传{@code null}即可。
     *
     * @param <T> 对象类型
     * @param method 方法（对象方法或static方法都可）
     * @param args 参数对象
     * @return 结果
     */

    /**
     * 执行方法，执行前会检查给定参数（历史版本说明）
     *
     * <pre>
     * 1. 参数个数是否与方法参数个数一致
     * 2. 如果某个参数为null但是方法这个位置的参数为原始类型，则赋予原始类型默认值
     * </pre>
     *
     * @param <T> 返回对象类型
     * @param obj 对象，如果执行静态方法，此值为<code>null</code>
     * @param method 方法（对象方法或static方法都可）
     * @param args 参数对象
     * @return 结果
     */

    /**
     * 执行方法（历史版本说明）
     *
     * <p>对于用户传入参数会做必要检查，包括：
     *
     * <pre>
     *     1、忽略多余的参数
     *     2、参数不够补齐默认值
     *     3、传入参数为null，但是目标参数类型为原始类型，做转换
     * </pre>
     *
     * @param <T> 返回对象类型
     * @param obj 对象，如果执行静态方法，此值为<code>null</code>
     * @param method 方法（对象方法或static方法都可）
     * @param args 参数对象
     * @return 结果
     */

    /**
     * 执行对象中指定方法（历史版本说明）
     *
     * <p>如果需要传递的参数为null，请使用NullWrapperBean来传递，不然会丢失类型信息。
     *
     * @param <T> 返回对象类型
     * @param obj 方法所在对象
     * @param methodName 方法名
     * @param args 参数列表
     * @return 执行结果
     * @see NullWrapperBean
     * @since 3.1.2
     */

    /**
     * 设置方法为可访问（私有方法可以被外部调用）
     *
     * <p>在 JDK 9+ 模块系统下，如果目标对象所属模块未对调用方开放（模块强封装），
     * {@code setAccessible(true)} 会抛出运行时异常（如 {@code InaccessibleObjectException}），
     * 此方法会将此类异常包装为 {@link IllegalStateException} 抛出。
     *
     * @param <T> AccessibleObject的子类，比如Class、Method、Field等
     * @param accessibleObject 可设置访问权限的对象，比如Class、Method、Field等
     * @return 被设置可访问的对象
     * @throws IllegalStateException 模块强封装导致无法设置为可访问时抛出
     * @since 4.6.8
     */
    public static <T extends AccessibleObject> T setAccessible(T accessibleObject) {
        if (null != accessibleObject && false == accessibleObject.isAccessible()) {
            try {
                accessibleObject.setAccessible(true);
            } catch (RuntimeException e) {
                // JDK 9+ 模块强封装等场景下无法设置可访问权限，包装后抛出
                throw new IllegalStateException(
                        GutilStr.format("Unable to make {} accessible!", accessibleObject), e);
            }
        }
        return accessibleObject;
    }
}
