package cn.geoair.gtc.base.util;

import cn.geoair.gtc.base.exception.GirException;
import cn.geoair.gtc.base.def.GkFilter;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 来着spring ReflectionUtil
 */
public abstract class GutilReflection {

	/**
	 * Pre-built MethodFilter that matches all non-bridge non-synthetic methods which are
	 * not declared on {@code java.lang.Object}.
	 * @since 3.0.5
	 */
	public static final MethodFilter USER_DECLARED_METHODS = (method -> !method.isBridge() && !method.isSynthetic());

	/**
	 * Pre-built FieldFilter that matches all non-static, non-final fields.
	 */
	public static final FieldFilter COPYABLE_FIELDS = (field -> !(Modifier.isStatic(field.getModifiers())
			|| Modifier.isFinal(field.getModifiers())));

	/**
	 * Naming prefix for CGLIB-renamed methods.
	 * @see #isCglibRenamedMethod
	 */
	private static final String CGLIB_RENAMED_METHOD_PREFIX = "CGLIB$";

	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

	private static final Method[] EMPTY_METHOD_ARRAY = new Method[0];

	private static final Field[] EMPTY_FIELD_ARRAY = new Field[0];

	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

	/**
	 * Cache for {@link Class#getDeclaredMethods()} plus equivalent default methods from
	 * Java 8 based interfaces, allowing for fast iteration.
	 */
	private static final Map<Class<?>, Method[]> declaredMethodsCache = new ConcurrentHashMap<>(256);

	/**
	 * Cache for {@link Class#getDeclaredFields()}, allowing for fast iteration.
	 */
	private static final Map<Class<?>, Field[]> declaredFieldsCache = new ConcurrentHashMap<>(256);

	// Exception handling

	/**
	 * Handle the given reflection exception.
	 * <p>
	 * Should only be called if no checked exception is expected to be thrown by a target
	 * method, or if an error occurs while accessing a method or field.
	 * <p>
	 * Throws the underlying RuntimeException or Error in case of an
	 * InvocationTargetException with such a root cause. Throws an IllegalStateException
	 * with an appropriate message or UndeclaredThrowableException otherwise.
	 * @param ex the reflection exception to handle
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
	 * Handle the given invocation target exception. Should only be called if no checked
	 * exception is expected to be thrown by the target method.
	 * <p>
	 * Throws the underlying RuntimeException or Error in case of such a root cause.
	 * Throws an UndeclaredThrowableException otherwise.
	 * @param ex the invocation target exception to handle
	 */
	public static void handleInvocationTargetException(InvocationTargetException ex) {
		rethrowRuntimeException(ex.getTargetException());
	}

	/**
	 * Rethrow the given {@link Throwable exception}, which is presumably the <em>target
	 * exception</em> of an {@link InvocationTargetException}. Should only be called if no
	 * checked exception is expected to be thrown by the target method.
	 * <p>
	 * Rethrows the underlying exception cast to a {@link RuntimeException} or
	 * {@link Error} if appropriate; otherwise, throws an
	 * {@link UndeclaredThrowableException}.
	 * @param ex the exception to rethrow
	 * @throws RuntimeException the rethrown exception
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
	 * Rethrow the given {@link Throwable exception}, which is presumably the <em>target
	 * exception</em> of an {@link InvocationTargetException}. Should only be called if no
	 * checked exception is expected to be thrown by the target method.
	 * <p>
	 * Rethrows the underlying exception cast to an {@link Exception} or {@link Error} if
	 * appropriate; otherwise, throws an {@link UndeclaredThrowableException}.
	 * @param ex the exception to rethrow
	 * @throws Exception the rethrown exception (in case of a checked exception)
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

	// Constructor handling

	/**
	 * Obtain an accessible constructor for the given class and parameters.
	 * @param clazz the clazz to check
	 * @param parameterTypes the parameter types of the desired constructor
	 * @return the constructor reference
	 * @throws NoSuchMethodException if no such constructor exists
	 * @since 5.0
	 */
	public static <T> Constructor<T> accessibleConstructor(Class<T> clazz, Class<?>... parameterTypes)
			throws NoSuchMethodException {

		Constructor<T> ctor = clazz.getDeclaredConstructor(parameterTypes);
		makeAccessible(ctor);
		return ctor;
	}

	/**
	 * Make the given constructor accessible, explicitly setting it accessible if
	 * necessary. The {@code setAccessible(true)} method is only called when actually
	 * necessary, to avoid unnecessary conflicts with a JVM SecurityManager (if active).
	 * @param ctor the constructor to make accessible
	 * @see java.lang.reflect.Constructor#setAccessible
	 */
	// on JDK 9
	public static void makeAccessible(Constructor<?> ctor) {
		if ((!Modifier.isPublic(ctor.getModifiers()) || !Modifier.isPublic(ctor.getDeclaringClass().getModifiers()))
				&& !ctor.isAccessible()) {
			ctor.setAccessible(true);
		}
	}

	// Method handling

	/**
	 * Attempt to find a {@link Method} on the supplied class with the supplied name and
	 * no parameters. Searches all superclasses up to {@code Object}.
	 * <p>
	 * Returns {@code null} if no {@link Method} can be found.
	 * @param clazz the class to introspect
	 * @param name the name of the method
	 * @return the Method object, or {@code null} if none found
	 */

	public static Method findMethod(Class<?> clazz, String name) {
		return findMethod(clazz, name, EMPTY_CLASS_ARRAY);
	}

	/**
	 * Attempt to find a {@link Method} on the supplied class with the supplied name and
	 * parameter types. Searches all superclasses up to {@code Object}.
	 * <p>
	 * Returns {@code null} if no {@link Method} can be found.
	 * @param clazz the class to introspect
	 * @param name the name of the method
	 * @param paramTypes the parameter types of the method (may be {@code null} to
	 * indicate any signature)
	 * @return the Method object, or {@code null} if none found
	 */

	public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
		GutilAssert.notNull(clazz, () -> "Class must not be null");
		GutilAssert.notNull(name, () -> "Method name must not be null");
		Class<?> searchType = clazz;
		while (searchType != null) {
			Method[] methods = (searchType.isInterface() ? searchType.getMethods()
					: getDeclaredMethods(searchType, false));
			for (Method method : methods) {
				if (name.equals(method.getName()) && (paramTypes == null || hasSameParams(method, paramTypes))) {
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
	 * Invoke the specified {@link Method} against the supplied target object with no
	 * arguments. The target object can be {@code null} when invoking a static
	 * {@link Method}.
	 * <p>
	 * Thrown exceptions are handled via a call to {@link #handleReflectionException}.
	 * @param method the method to invoke
	 * @param target the target object to invoke the method on
	 * @return the invocation result, if any
	 * @see #invokeMethod(java.lang.reflect.Method, Object, Object[])
	 */

	public static Object invokeMethod(Method method, Object target) {
		return invokeMethod(method, target, EMPTY_OBJECT_ARRAY);
	}

	/**
	 * Invoke the specified {@link Method} against the supplied target object with the
	 * supplied arguments. The target object can be {@code null} when invoking a static
	 * {@link Method}.
	 * <p>
	 * Thrown exceptions are handled via a call to {@link #handleReflectionException}.
	 * @param method the method to invoke
	 * @param target the target object to invoke the method on
	 * @param args the invocation arguments (may be {@code null})
	 * @return the invocation result, if any
	 */

	public static Object invokeMethod(Method method, Object target, Object... args) {
		try {
			return method.invoke(target, args);
		}
		catch (Exception ex) {
			handleReflectionException(ex);
		}
		throw new IllegalStateException("Should never get here");
	}

	/**
	 * Determine whether the given method explicitly declares the given exception or one
	 * of its superclasses, which means that an exception of that type can be propagated
	 * as-is within a reflective invocation.
	 * @param method the declaring method
	 * @param exceptionType the exception to throw
	 * @return {@code true} if the exception can be thrown as-is; {@code false} if it
	 * needs to be wrapped
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
	 * Perform the given callback operation on all matching methods of the given class, as
	 * locally declared or equivalent thereof (such as default methods on Java 8 based
	 * interfaces that the given class implements).
	 * @param clazz the class to introspect
	 * @param mc the callback to invoke for each method
	 * @throws IllegalStateException if introspection fails
	 * @since 4.2
	 * @see #doWithMethods
	 */
	public static void doWithLocalMethods(Class<?> clazz, MethodCallback mc) {
		Method[] methods = getDeclaredMethods(clazz, false);
		for (Method method : methods) {
			try {
				mc.doWith(method);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
			}
		}
	}

	/**
	 * Perform the given callback operation on all matching methods of the given class and
	 * superclasses.
	 * <p>
	 * The same named method occurring on subclass and superclass will appear twice,
	 * unless excluded by a {@link MethodFilter}.
	 * @param clazz the class to introspect
	 * @param mc the callback to invoke for each method
	 * @throws IllegalStateException if introspection fails
	 * @see #doWithMethods(Class, MethodCallback, MethodFilter)
	 */
	public static void doWithMethods(Class<?> clazz, MethodCallback mc) {
		doWithMethods(clazz, mc, null);
	}

	/**
	 * Perform the given callback operation on all matching methods of the given class and
	 * superclasses (or given interface and super-interfaces).
	 * <p>
	 * The same named method occurring on subclass and superclass will appear twice,
	 * unless excluded by the specified {@link MethodFilter}.
	 * @param clazz the class to introspect
	 * @param mc the callback to invoke for each method
	 * @param mf the filter that determines the methods to apply the callback to
	 * @throws IllegalStateException if introspection fails
	 */
	public static void doWithMethods(Class<?> clazz, MethodCallback mc, MethodFilter mf) {
		// Keep backing up the inheritance hierarchy.
		Method[] methods = getDeclaredMethods(clazz, false);
		for (Method method : methods) {
			if (mf != null && !mf.matches(method)) {
				continue;
			}
			try {
				mc.doWith(method);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
			}
		}
		if (clazz.getSuperclass() != null && (mf != USER_DECLARED_METHODS || clazz.getSuperclass() != Object.class)) {
			doWithMethods(clazz.getSuperclass(), mc, mf);
		}
		else if (clazz.isInterface()) {
			for (Class<?> superIfc : clazz.getInterfaces()) {
				doWithMethods(superIfc, mc, mf);
			}
		}
	}

	/**
	 * Get all declared methods on the leaf class and all superclasses. Leaf class methods
	 * are included first.
	 * @param leafClass the class to introspect
	 * @throws IllegalStateException if introspection fails
	 */
	public static Method[] getAllDeclaredMethods(Class<?> leafClass) {
		final List<Method> methods = new ArrayList<>(20);
		doWithMethods(leafClass, methods::add);
		return methods.toArray(EMPTY_METHOD_ARRAY);
	}

	/**
	 * Get the unique set of declared methods on the leaf class and all superclasses. Leaf
	 * class methods are included first and while traversing the superclass hierarchy any
	 * methods found with signatures matching a method already included are filtered out.
	 * @param leafClass the class to introspect
	 * @throws IllegalStateException if introspection fails
	 */
	public static Method[] getUniqueDeclaredMethods(Class<?> leafClass) {
		return getUniqueDeclaredMethods(leafClass, null);
	}

	/**
	 * Get the unique set of declared methods on the leaf class and all superclasses. Leaf
	 * class methods are included first and while traversing the superclass hierarchy any
	 * methods found with signatures matching a method already included are filtered out.
	 * @param leafClass the class to introspect
	 * @param mf the filter that determines the methods to take into account
	 * @throws IllegalStateException if introspection fails
	 * @since 5.2
	 */
	public static Method[] getUniqueDeclaredMethods(Class<?> leafClass, MethodFilter mf) {
		final List<Method> methods = new ArrayList<>(20);
		doWithMethods(leafClass, method -> {
			boolean knownSignature = false;
			Method methodBeingOverriddenWithCovariantReturnType = null;
			for (Method existingMethod : methods) {
				if (method.getName().equals(existingMethod.getName())
						&& method.getParameterCount() == existingMethod.getParameterCount()
						&& Arrays.equals(method.getParameterTypes(), existingMethod.getParameterTypes())) {
					// Is this a covariant return type situation?
					if (existingMethod.getReturnType() != method.getReturnType()
							&& existingMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
						methodBeingOverriddenWithCovariantReturnType = existingMethod;
					}
					else {
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
		}, mf);
		return methods.toArray(EMPTY_METHOD_ARRAY);
	}

	/**
	 * Variant of {@link Class#getDeclaredMethods()} that uses a local cache in order to
	 * avoid the JVM's SecurityManager check and new Method instances. In addition, it
	 * also includes Java 8 default methods from locally implemented interfaces, since
	 * those are effectively to be treated just like declared methods.
	 * @param clazz the class to introspect
	 * @return the cached array of methods
	 * @throws IllegalStateException if introspection fails
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
				}
				else {
					result = declaredMethods;
				}
				declaredMethodsCache.put(clazz, (result.length == 0 ? EMPTY_METHOD_ARRAY : result));
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Failed to introspect Class [" + clazz.getName()
						+ "] from ClassLoader [" + clazz.getClassLoader() + "]", ex);
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
	 * Determine whether the given method is an "equals" method.
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
	 * Determine whether the given method is a "hashCode" method.
	 * @see java.lang.Object#hashCode()
	 */
	public static boolean isHashCodeMethod(Method method) {
		return method != null && method.getParameterCount() == 0 && method.getName().equals("hashCode");
	}

	/**
	 * Determine whether the given method is a "toString" method.
	 * @see java.lang.Object#toString()
	 */
	public static boolean isToStringMethod(Method method) {
		return (method != null && method.getParameterCount() == 0 && method.getName().equals("toString"));
	}

	/**
	 * Determine whether the given method is originally declared by
	 * {@link java.lang.Object}.
	 */
	public static boolean isObjectMethod(Method method) {
		return (method != null && (method.getDeclaringClass() == Object.class || isEqualsMethod(method)
				|| isHashCodeMethod(method) || isToStringMethod(method)));
	}

	/**
	 * Determine whether the given method is a CGLIB 'renamed' method, following the
	 * pattern "CGLIB$methodName$0".
	 * @param renamedMethod the method to check
	 */
	public static boolean isCglibRenamedMethod(Method renamedMethod) {
		String name = renamedMethod.getName();
		if (name.startsWith(CGLIB_RENAMED_METHOD_PREFIX)) {
			int i = name.length() - 1;
			while (i >= 0 && Character.isDigit(name.charAt(i))) {
				i--;
			}
			return (i > CGLIB_RENAMED_METHOD_PREFIX.length() && (i < name.length() - 1) && name.charAt(i) == '$');
		}
		return false;
	}

	/**
	 * Make the given method accessible, explicitly setting it accessible if necessary.
	 * The {@code setAccessible(true)} method is only called when actually necessary, to
	 * avoid unnecessary conflicts with a JVM SecurityManager (if active).
	 * @param method the method to make accessible
	 * @see java.lang.reflect.Method#setAccessible
	 */
	@SuppressWarnings("deprecation") // on JDK 9
	public static void makeAccessible(Method method) {
		if ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers()))
				&& !method.isAccessible()) {
			method.setAccessible(true);
		}
	}

	// Field handling

	/**
	 * Attempt to find a {@link Field field} on the supplied {@link Class} with the
	 * supplied {@code name}. Searches all superclasses up to {@link Object}.
	 * @param clazz the class to introspect
	 * @param name the name of the field
	 * @return the corresponding Field object, or {@code null} if not found
	 */

	public static Field findField(Class<?> clazz, String name) {
		return findField(clazz, name, null);
	}

	/**
	 * Attempt to find a {@link Field field} on the supplied {@link Class} with the
	 * supplied {@code name} and/or {@link Class type}. Searches all superclasses up to
	 * {@link Object}.
	 * @param clazz the class to introspect
	 * @param name the name of the field (may be {@code null} if type is specified)
	 * @param type the type of the field (may be {@code null} if name is specified)
	 * @return the corresponding Field object, or {@code null} if not found
	 */

	public static Field findField(Class<?> clazz, String name, Class<?> type) {
		GutilAssert.notNull(clazz, () -> "Class must not be null");
		GutilAssert.isTrue(name != null || type != null, "Either name or type of the field must be specified");
		Class<?> searchType = clazz;
		while (Object.class != searchType && searchType != null) {
			Field[] fields = getDeclaredFields(searchType);
			for (Field field : fields) {
				if ((name == null || name.equals(field.getName())) && (type == null || type.equals(field.getType()))) {
					return field;
				}
			}
			searchType = searchType.getSuperclass();
		}
		return null;
	}

	/**
	 * 查找Field 将查找当前类和父类 的declaredFields
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
	 * Set the field represented by the supplied {@linkplain Field field object} on the
	 * specified {@linkplain Object target object} to the specified {@code value}.
	 * <p>
	 * In accordance with {@link Field#set(Object, Object)} semantics, the new value is
	 * automatically unwrapped if the underlying field has a primitive type.
	 * <p>
	 * This method does not support setting {@code static final} fields.
	 * <p>
	 * Thrown exceptions are handled via a call to
	 * {@link #handleReflectionException(Exception)}.
	 * @param field the field to set
	 * @param target the target object on which to set the field (or {@code null} for a
	 * static field)
	 * @param value the value to set (may be {@code null})
	 */
	public static void setField(Field field, Object target, Object value) {
		try {
			field.set(target, value);
		}
		catch (IllegalAccessException ex) {
			handleReflectionException(ex);
		}
	}

	/**
	 * Get the field represented by the supplied {@link Field field object} on the
	 * specified {@link Object target object}. In accordance with
	 * {@link Field#get(Object)} semantics, the returned value is automatically wrapped if
	 * the underlying field has a primitive type.
	 * <p>
	 * Thrown exceptions are handled via a call to
	 * {@link #handleReflectionException(Exception)}.
	 * @param field the field to get
	 * @param target the target object from which to get the field (or {@code null} for a
	 * static field)
	 * @return the field's current value
	 */

	public static Object getField(Field field, Object target) {
		try {
			return field.get(target);
		}
		catch (IllegalAccessException ex) {
			handleReflectionException(ex);
		}
		throw new IllegalStateException("Should never get here");
	}

	/**
	 * Invoke the given callback on all locally declared fields in the given class.
	 * @param clazz the target class to analyze
	 * @param fc the callback to invoke for each field
	 * @throws IllegalStateException if introspection fails
	 * @since 4.2
	 * @see #doWithFields
	 */
	public static void doWithLocalFields(Class<?> clazz, FieldCallback fc) {
		for (Field field : getDeclaredFields(clazz)) {
			try {
				fc.doWith(field);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException("Not allowed to access field '" + field.getName() + "': " + ex);
			}
		}
	}

	/**
	 * Invoke the given callback on all fields in the target class, going up the class
	 * hierarchy to get all declared fields.
	 * @param clazz the target class to analyze
	 * @param fc the callback to invoke for each field
	 * @throws IllegalStateException if introspection fails
	 */
	public static void doWithFields(Class<?> clazz, FieldCallback fc) {
		doWithFields(clazz, fc, null);
	}

	/**
	 * Invoke the given callback on all fields in the target class, going up the class
	 * hierarchy to get all declared fields.
	 * @param clazz the target class to analyze
	 * @param fc the callback to invoke for each field
	 * @param ff the filter that determines the fields to apply the callback to
	 * @throws IllegalStateException if introspection fails
	 */
	public static void doWithFields(Class<?> clazz, FieldCallback fc, FieldFilter ff) {
		// Keep backing up the inheritance hierarchy.
		Class<?> targetClass = clazz;
		do {
			Field[] fields = getDeclaredFields(targetClass);
			for (Field field : fields) {
				if (ff != null && !ff.matches(field)) {
					continue;
				}
				try {
					fc.doWith(field);
				}
				catch (IllegalAccessException ex) {
					throw new IllegalStateException("Not allowed to access field '" + field.getName() + "': " + ex);
				}
			}
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);
	}

	/**
	 * This variant retrieves {@link Class#getDeclaredFields()} from a local cache in
	 * order to avoid the JVM's SecurityManager check and defensive array copying.
	 * @param clazz the class to introspect
	 * @return the cached array of fields
	 * @throws IllegalStateException if introspection fails
	 * @see Class#getDeclaredFields()
	 */
	private static Field[] getDeclaredFields(Class<?> clazz) {
		GutilAssert.notNull(clazz, () -> "Class must not be null");
		Field[] result = declaredFieldsCache.get(clazz);
		if (result == null) {
			try {
				result = clazz.getDeclaredFields();
				declaredFieldsCache.put(clazz, (result.length == 0 ? EMPTY_FIELD_ARRAY : result));
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Failed to introspect Class [" + clazz.getName()
						+ "] from ClassLoader [" + clazz.getClassLoader() + "]", ex);
			}
		}
		return result;
	}

	/**
	 * Given the source object and the destination, which must be the same class or a
	 * subclass, copy all fields, including inherited fields. Designed to work on objects
	 * with public no-arg constructors.
	 * @throws IllegalStateException if introspection fails
	 */
	public static void shallowCopyFieldState(final Object src, final Object dest) {
		GutilAssert.notNull(src, () -> "Source for field copy cannot be null");
		GutilAssert.notNull(dest, () -> "Destination for field copy cannot be null");
		if (!src.getClass().isAssignableFrom(dest.getClass())) {
			throw new IllegalArgumentException("Destination class [" + dest.getClass().getName()
					+ "] must be same or subclass as source class [" + src.getClass().getName() + "]");
		}
		doWithFields(src.getClass(), field -> {
			makeAccessible(field);
			Object srcValue = field.get(src);
			field.set(dest, srcValue);
		}, COPYABLE_FIELDS);
	}

	/**
	 * Determine whether the given field is a "public static final" constant.
	 * @param field the field to check
	 */
	public static boolean isPublicStaticFinal(Field field) {
		int modifiers = field.getModifiers();
		return (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers));
	}

	/**
	 * Make the given field accessible, explicitly setting it accessible if necessary. The
	 * {@code setAccessible(true)} method is only called when actually necessary, to avoid
	 * unnecessary conflicts with a JVM SecurityManager (if active).
	 * @param field the field to make accessible
	 * @see java.lang.reflect.Field#setAccessible
	 */
	@SuppressWarnings("deprecation") // on JDK 9
	public static void makeAccessible(Field field) {
		if ((!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers())
				|| Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
			field.setAccessible(true);
		}
	}

	/**
	 * 设置可访问对象的可访问权限为 true
	 * @param object 可访问的对象
	 * @param <T> 类型
	 * @return 返回设置后的对象
	 *
	 * public static <T extends AccessibleObject> T setAccessible(T object) { return
	 * AccessController.doPrivileged(new SetAccessibleAction<>(object)); }
	 */

	// Cache handling

	/**
	 * Clear the internal method/field cache.
	 * @since 4.2.4
	 */
	public static void clearCache() {
		declaredMethodsCache.clear();
		declaredFieldsCache.clear();
	}

	/**
	 * Action to take on each method.
	 */
	@FunctionalInterface
	public interface MethodCallback {

		/**
		 * Perform an operation using the given method.
		 * @param method the method to operate on
		 */
		void doWith(Method method) throws IllegalArgumentException, IllegalAccessException;

	}

	/**
	 * Callback optionally used to filter methods to be operated on by a method callback.
	 */
	@FunctionalInterface
	public interface MethodFilter {

		/**
		 * Determine whether the given method matches.
		 * @param method the method to check
		 */
		boolean matches(Method method);

		/**
		 * Create a composite filter based on this filter <em>and</em> the provided
		 * filter.
		 * <p>
		 * If this filter does not match, the next filter will not be applied.
		 * @param next the next {@code MethodFilter}
		 * @return a composite {@code MethodFilter}
		 * @throws IllegalArgumentException if the MethodFilter argument is {@code null}
		 * @since 5.3.2
		 */
		default MethodFilter and(MethodFilter next) {
			GutilAssert.notNull(next, () -> "Next MethodFilter must not be null");
			return method -> matches(method) && next.matches(method);
		}

	}

	/**
	 * Callback interface invoked on each field in the hierarchy.
	 */
	@FunctionalInterface
	public interface FieldCallback {

		/**
		 * Perform an operation using the given field.
		 * @param field the field to operate on
		 */
		void doWith(Field field) throws IllegalArgumentException, IllegalAccessException;

	}

	/**
	 * Callback optionally used to filter fields to be operated on by a field callback.
	 */
	@FunctionalInterface
	public interface FieldFilter {

		/**
		 * Determine whether the given field matches.
		 * @param field the field to check
		 */
		boolean matches(Field field);

		/**
		 * Create a composite filter based on this filter <em>and</em> the provided
		 * filter.
		 * <p>
		 * If this filter does not match, the next filter will not be applied.
		 * @param next the next {@code FieldFilter}
		 * @return a composite {@code FieldFilter}
		 * @throws IllegalArgumentException if the FieldFilter argument is {@code null}
		 * @since 5.3.2
		 */
		default FieldFilter and(FieldFilter next) {
			GutilAssert.notNull(next, () -> "Next FieldFilter must not be null");
			return field -> matches(field) && next.matches(field);
		}

	}

	/**
	 * 构造对象缓存
	 */
	private static final Map<Class<?>, Constructor<?>[]> CONSTRUCTORS_CACHE = new ConcurrentHashMap<>(256);

	/**
	 * 字段缓存
	 */
	private static final Map<Class<?>, Field[]> FIELDS_CACHE = new ConcurrentHashMap<>(256);

	/**
	 * 方法缓存
	 */
	private static final Map<Class<?>, Method[]> METHODS_CACHE = new ConcurrentHashMap<>(256);

	// ---------------------------------------------------------------------------------------------------------
	// Constructor

	/**
	 * 查找类中的指定参数的构造方法，如果找到构造方法，会自动设置可访问为true
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
	 * @param <T> 构造的对象类型
	 * @param beanClass 类
	 * @return 字段列表
	 * @throws SecurityException 安全检查异常
	 */
	@SuppressWarnings("unchecked")
	public static <T> Constructor<T>[] getConstructors(Class<T> beanClass) throws SecurityException {
		GutilAssert.notNull(beanClass, "");
		Constructor<?>[] constructors = CONSTRUCTORS_CACHE.get(beanClass);
		if (null != constructors) {
			return (Constructor<T>[]) constructors;
		}

		constructors = getConstructorsDirectly(beanClass);
		return (Constructor<T>[]) CONSTRUCTORS_CACHE.put(beanClass, constructors);
	}

	/**
	 * 获得一个类中所有构造列表，直接反射获取，无缓存
	 * @param beanClass 类
	 * @return 字段列表
	 * @throws SecurityException 安全检查异常
	 */
	public static Constructor<?>[] getConstructorsDirectly(Class<?> beanClass) throws SecurityException {
		GutilAssert.notNull(beanClass, "");
		return beanClass.getDeclaredConstructors();
	}

	// ---------------------------------------------------------------------------------------------------------
	// Field

	/**
	 * 查找指定类中是否包含指定名称对应的字段，包括所有字段（包括非public字段），也包括父类和Object类的字段
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
	 * @param field 字段
	 * @return 字段名
	 * @since 5.1.6
	 */
	public static String getFieldName(Field field) {
		return field.getName();
	}

	/**
	 * 查找指定类中的指定name的字段（包括非public字段），也包括父类和Object类的字段， 字段不存在则返回<code>null</code>
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
	 * @param beanClass 类
	 * @return 字段列表
	 * @throws SecurityException 安全检查异常
	 */
	public static Field[] getFields(Class<?> beanClass) throws SecurityException {
		Field[] allFields = FIELDS_CACHE.get(beanClass);
		if (null != allFields) {
			return allFields;
		}

		allFields = getFieldsDirectly(beanClass, true);
		FIELDS_CACHE.put(beanClass, allFields);
		return FIELDS_CACHE.get(beanClass);
	}

	/**
	 * 获得一个类中所有字段列表，直接反射获取，无缓存<br>
	 * 如果子类与父类中存在同名字段，则这两个字段同时存在，子类字段在前，父类字段在后。
	 * @param beanClass 类
	 * @param withSuperClassFields 是否包括父类的字段列表
	 * @return 字段列表
	 * @throws SecurityException 安全检查异常
	 */
	public static Field[] getFieldsDirectly(Class<?> beanClass, boolean withSuperClassFields) throws SecurityException {
		GutilAssert.notNull(beanClass, "");

		Field[] allFields = null;
		Class<?> searchType = beanClass;
		Field[] declaredFields;
		while (searchType != null) {
			declaredFields = searchType.getDeclaredFields();
			if (null == allFields) {
				allFields = declaredFields;
			}
			else {
				allFields = GutilArray.append(allFields, declaredFields);
			}
			searchType = withSuperClassFields ? searchType.getSuperclass() : null;
		}

		return allFields;
	}

	/**
	 * 获取字段值
	 * @param obj 对象，如果static字段，此处为类
	 * @param fieldName 字段名
	 * @return 字段值
	 * @throws GirException 包装IllegalAccessException异常
	 */
	public static Object getFieldValue(Object obj, String fieldName) throws IllegalStateException {
		if (null == obj || GutilStr.isBlank(fieldName)) {
			return null;
		}
		return getFieldValue(obj, getField(obj instanceof Class ? (Class<?>) obj : obj.getClass(), fieldName));
	}

	/**
	 * 获取静态字段值
	 * @param field 字段
	 * @return 字段值
	 * @throws GirException 包装IllegalAccessException异常
	 * @since 5.1.0
	 */
	public static Object getStaticFieldValue(Field field) throws IllegalStateException {
		return getFieldValue(null, field);
	}

	/**
	 * 获取字段值
	 * @param obj 对象，static字段则此字段为null
	 * @param field 字段
	 * @return 字段值
	 * @throws GirException 包装IllegalAccessException异常
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
		}
		catch (IllegalAccessException e) {
			throw new IllegalStateException(
					GutilStr.format("IllegalAccess for {}.{}", field.getDeclaringClass(), field.getName()), e);
		}
		return result;
	}

	/**
	 * 获取所有字段的值
	 * @param obj bean对象，如果是static字段，此处为类class
	 * @return 字段值数组
	 * @since 4.1.17
	 */
	public static Object[] getFieldsValue(Object obj) {
		if (null != obj) {
			final Field[] fields = getFields(obj instanceof Class ? (Class<?>) obj : obj.getClass());
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
	 * @param obj 对象,static字段则此处传Class
	 * @param fieldName 字段名
	 * @param value 值，值类型必须与字段类型匹配，不会自动转换对象类型
	 * @throws GirException 包装IllegalAccessException异常
	 */
	public static void setFieldValue(Object obj, String fieldName, Object value) throws IllegalStateException {
		GutilAssert.notNull(obj, "");
		GutilAssert.hasText(fieldName, "");

		final Field field = getField((obj instanceof Class) ? (Class<?>) obj : obj.getClass(), fieldName);
		GutilAssert.notNull(field,
				GutilStr.format("Field [{}] is not exist in [{}]", fieldName, obj.getClass().getName()));
		setFieldValue(obj, field, value);
	}

	/**
	 * 设置字段值
	 * @param obj 对象，如果是static字段，此参数为null
	 * @param field 字段
	 * @param value 值，值类型必须与字段类型匹配，不会自动转换对象类型
	 * @throws GirException gtcException 包装IllegalAccessException异常
	 */
	public static void setFieldValue(Object obj, Field field, Object value) throws IllegalStateException {
		GutilAssert.notNull(field, GutilStr.format("Field in [{}] not exist !", obj));

		final Class<?> fieldType = field.getType();
		if (null != value) {

		}
		else {
			// 获取null对应默认值，防止原始类型造成空指针问题
			value = GutilClass.getDefaultValue(fieldType);
		}

		setAccessible(field);
		try {
			field.set(obj instanceof Class ? null : obj, value);
		}
		catch (IllegalAccessException e) {
			throw new IllegalStateException(GutilStr.format("IllegalAccess for {}.{}", obj, field.getName()), e);
		}
	}

	// ---------------------------------------------------------------------------------------------------------
	// method

	/**
	 * 获得指定类本类及其父类中的Public方法名<br>
	 * 去重重载的方法
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
	 * @param clazz 查找方法的类
	 * @return 过滤后的方法列表
	 */
	public static Method[] getPublicMethods(Class<?> clazz) {
		return null == clazz ? null : clazz.getMethods();
	}

	/**
	 * 获得指定类过滤后的Public方法列表
	 * @param clazz 查找方法的类
	 * @param filter 过滤器
	 * @return 过滤后的方法列表
	 *
	 * public static List<Method> getPublicMethods(Class<?> clazz, Filter<Method> filter)
	 * { if (null == clazz) { return null; }
	 *
	 * final Method[] methods = getPublicMethods(clazz); List<Method> methodList; if (null
	 * != filter) { methodList = new ArrayList<>(); for (Method method : methods) { if
	 * (filter.accept(method)) { methodList.add(method); } } } else { methodList =
	 * gtcArrayUtil.newArrayList(methods); } return methodList; }
	 */

	/**
	 * 获得指定类过滤后的Public方法列表
	 * @param clazz 查找方法的类
	 * @param excludeMethods 不包括的方法
	 * @return 过滤后的方法列表
	 *
	 * public static List<Method> getPublicMethods(Class<?> clazz, Method...
	 * excludeMethods) { final HashSet<Method> excludeMethodSet =
	 * gtcCollectionUtil.newHashSet(excludeMethods); return getPublicMethods(clazz, method
	 * -> false == excludeMethodSet.contains(method)); }
	 */

	/**
	 * 获得指定类过滤后的Public方法列表
	 * @param clazz 查找方法的类
	 * @param excludeMethodNames 不包括的方法名列表
	 * @return 过滤后的方法列表
	 *
	 * public static List<Method> getPublicMethods(Class<?> clazz, String...
	 * excludeMethodNames) { final HashSet<String> excludeMethodNameSet =
	 * gtcCollectionUtil.newHashSet(excludeMethodNames); return getPublicMethods(clazz,
	 * method -> false == excludeMethodNameSet.contains(method.getName())); }
	 */

	/**
	 * 查找指定Public方法 如果找不到对应的方法或方法不为public的则返回<code>null</code>
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
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	/**
	 * 查找指定对象中的所有方法（包括非public方法），也包括父对象和Object类的方法
	 *
	 * <p>
	 * 此方法为精准获取方法名，即方法名和参数数量和类型必须一致，否则返回<code>null</code>。
	 * </p>
	 * @param obj 被查找的对象，如果为{@code null}返回{@code null}
	 * @param methodName 方法名，如果为空字符串返回{@code null}
	 * @param args 参数
	 * @return 方法
	 * @throws SecurityException 无访问权限抛出异常
	 */
	public static Method getMethodOfObj(Object obj, String methodName, Object... args) throws SecurityException {
		if (null == obj || GutilStr.isBlank(methodName)) {
			return null;
		}
		return getMethod(obj.getClass(), methodName, GutilClass.getClasses(args));
	}

	/**
	 * 忽略大小写查找指定方法，如果找不到对应的方法则返回<code>null</code>
	 *
	 * <p>
	 * 此方法为精准获取方法名，即方法名和参数数量和类型必须一致，否则返回<code>null</code>。
	 * </p>
	 * @param clazz 类，如果为{@code null}返回{@code null}
	 * @param methodName 方法名，如果为空字符串返回{@code null}
	 * @param paramTypes 参数类型，指定参数类型如果是方法的子类也算
	 * @return 方法
	 * @throws SecurityException 无权访问抛出异常
	 * @since 3.2.0
	 */
	public static Method getMethodIgnoreCase(Class<?> clazz, String methodName, Class<?>... paramTypes)
			throws SecurityException {
		return getMethod(clazz, true, methodName, paramTypes);
	}

	/**
	 * 查找指定方法 如果找不到对应的方法则返回<code>null</code>
	 *
	 * <p>
	 * 此方法为精准获取方法名，即方法名和参数数量和类型必须一致，否则返回<code>null</code>。
	 * </p>
	 * @param clazz 类，如果为{@code null}返回{@code null}
	 * @param methodName 方法名，如果为空字符串返回{@code null}
	 * @param paramTypes 参数类型，指定参数类型如果是方法的子类也算
	 * @return 方法
	 * @throws SecurityException 无权访问抛出异常
	 */
	public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) throws SecurityException {
		return getMethod(clazz, false, methodName, paramTypes);
	}

	/**
	 * 查找指定方法 如果找不到对应的方法则返回<code>null</code>
	 *
	 * <p>
	 * 此方法为精准获取方法名，即方法名和参数数量和类型必须一致，否则返回<code>null</code>。
	 * </p>
	 * @param clazz 类，如果为{@code null}返回{@code null}
	 * @param ignoreCase 是否忽略大小写
	 * @param methodName 方法名，如果为空字符串返回{@code null}
	 * @param paramTypes 参数类型，指定参数类型如果是方法的子类也算
	 * @return 方法
	 * @throws SecurityException 无权访问抛出异常
	 * @since 3.2.0
	 */
	public static Method getMethod(Class<?> clazz, boolean ignoreCase, String methodName, Class<?>... paramTypes)
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
	 * <p>
	 * 此方法只检查方法名是否一致，并不检查参数的一致性。
	 * </p>
	 * @param clazz 类，如果为{@code null}返回{@code null}
	 * @param methodName 方法名，如果为空字符串返回{@code null}
	 * @return 方法
	 * @throws SecurityException 无权访问抛出异常
	 * @since 4.3.2
	 */
	public static Method getMethodByName(Class<?> clazz, String methodName) throws SecurityException {
		return getMethodByName(clazz, false, methodName);
	}

	/**
	 * 按照方法名查找指定方法名的方法，只返回匹配到的第一个方法，如果找不到对应的方法则返回<code>null</code>
	 *
	 * <p>
	 * 此方法只检查方法名是否一致（忽略大小写），并不检查参数的一致性。
	 * </p>
	 * @param clazz 类，如果为{@code null}返回{@code null}
	 * @param methodName 方法名，如果为空字符串返回{@code null}
	 * @return 方法
	 * @throws SecurityException 无权访问抛出异常
	 * @since 4.3.2
	 */
	public static Method getMethodByNameIgnoreCase(Class<?> clazz, String methodName) throws SecurityException {
		return getMethodByName(clazz, true, methodName);
	}

	/**
	 * 按照方法名查找指定方法名的方法，只返回匹配到的第一个方法，如果找不到对应的方法则返回<code>null</code>
	 *
	 * <p>
	 * 此方法只检查方法名是否一致，并不检查参数的一致性。
	 * </p>
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
	 * @param clazz 查找方法的类
	 * @param filter 过滤器
	 * @return 过滤后的方法列表
	 * @throws SecurityException 安全异常
	 */
	public static Method[] getMethods(Class<?> clazz, GkFilter<Method> filter) throws SecurityException {
		if (null == clazz) {
			return null;
		}
		return GutilArray.filter(getMethods(clazz), filter);
	}

	/**
	 * 获得一个类中所有方法列表，包括其父类中的方法
	 * @param beanClass 类
	 * @return 方法列表
	 * @throws SecurityException 安全检查异常
	 */
	public static Method[] getMethods(Class<?> beanClass) throws SecurityException {
		Method[] allMethods = METHODS_CACHE.get(beanClass);
		if (null != allMethods) {
			return allMethods;
		}

		allMethods = getMethodsDirectly(beanClass, true);
		return METHODS_CACHE.put(beanClass, allMethods);
	}

	/**
	 * 获得一个类中所有方法列表，直接反射获取，无缓存
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
			}
			else {
				allMethods = GutilArray.append(allMethods, declaredMethods);
			}
			searchType = withSuperClassMethods ? searchType.getSuperclass() : null;
		}

		return allMethods;
	}

	/**
	 * 是否为无参数方法
	 * @param method 方法
	 * @return 是否为无参数方法
	 * @since 5.1.1
	 */
	public static boolean isEmptyParam(Method method) {
		return method.getParameterTypes().length == 0;
	}

	// ---------------------------------------------------------------------------------------------------------
	// newInstance

	/**
	 * 实例化对象
	 * @param <T> 对象类型
	 * @param clazz 类名
	 * @return 对象
	 * @throws GirException 包装各类异常
	 */
	@SuppressWarnings("unchecked")
	public static <T> T newInstance(String clazz) throws IllegalStateException {
		try {
			return (T) Class.forName(clazz).newInstance();
		}
		catch (Exception e) {
			throw new IllegalStateException(GutilStr.format("Instance class [{}] error!", clazz), e);
		}
	}

	/**
	 * 实例化对象
	 * @param <T> 对象类型
	 * @param clazz 类
	 * @param params 构造函数参数
	 * @return 对象
	 * @throws GirException 包装各类异常
	 */
	public static <T> T newInstance(Class<T> clazz, Object... params) throws IllegalStateException {
		if (GutilArray.isEmpty(params)) {
			try { // 更新
				return clazz.newInstance();
			}
			catch (Exception e) {
				throw new IllegalStateException(GutilStr.format("Instance class [{}] error!", clazz), e);
			}
		}

		final Class<?>[] paramTypes = GutilClass.getClasses(params);
		final Constructor<T> constructor = getConstructor(clazz, paramTypes);
		if (null == constructor) {
			throw new IllegalStateException(
					GutilStr.format("No Constructor matched for parameter types: [{}]", new Object[] { paramTypes }));
		}
		try {
			return constructor.newInstance(params);
		}
		catch (Exception e) {
			throw new IllegalStateException(GutilStr.format("Instance class [{}] error!", clazz), e);
		}
	}

	/**
	 * 尝试遍历并调用此类的所有构造方法，直到构造成功并返回
	 * <p>
	 * 对于某些特殊的接口，按照其默认实现实例化，例如： <pre>
	 *     Map       -》 HashMap
	 *     Collction -》 ArrayList
	 *     List      -》 ArrayList
	 *     Set       -》 HashSet
	 * </pre>
	 * @param <T> 对象类型
	 * @param beanClass 被构造的类
	 * @return 构造后的对象
	 */
	@SuppressWarnings("unchecked")
	public static <T> T newInstanceIfPossible(Class<T> beanClass) {
		GutilAssert.notNull(beanClass, "");

		// 某些特殊接口的实例化按照默认实现进行
		if (beanClass.isAssignableFrom(AbstractMap.class)) {
			beanClass = (Class<T>) HashMap.class;
		}
		else if (beanClass.isAssignableFrom(List.class)) {
			beanClass = (Class<T>) ArrayList.class;
		}
		else if (beanClass.isAssignableFrom(Set.class)) {
			beanClass = (Class<T>) HashSet.class;
		}

		try {
			return newInstance(beanClass);
		}
		catch (Exception e) {
			// ignore
			// 默认构造不存在的情况下查找其它构造
			e.printStackTrace();
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
			}
			catch (Exception ignore) {
				// 构造出错时继续尝试下一种构造方式
			}
		}
		return null;
	}

	// ---------------------------------------------------------------------------------------------------------
	// invoke

	/**
	 * 执行静态方法
	 * @param <T> 对象类型
	 * @param method 方法（对象方法或static方法都可）
	 * @param args 参数对象
	 * @return 结果
	 * @throws GirException 多种异常包装
	 *
	 * public static <T> T invokeStatic(Method method, Object... args) throws gtcException
	 * { return invoke(null, method, args); }
	 */

	/**
	 * 执行方法<br>
	 * 执行前要检查给定参数：
	 *
	 * <pre>
	 * 1. 参数个数是否与方法参数个数一致
	 * 2. 如果某个参数为null但是方法这个位置的参数为原始类型，则赋予原始类型默认值
	 * </pre>
	 * @param <T> 返回对象类型
	 * @param obj 对象，如果执行静态方法，此值为<code>null</code>
	 * @param method 方法（对象方法或static方法都可）
	 * @param args 参数对象
	 * @return 结果
	 * @throws GirException 一些列异常的包装
	 *
	 * public static <T> T invokeWithCheck(Object obj, Method method, Object... args)
	 * throws gtcException { final Class<?>[] types = method.getParameterTypes(); if (null
	 * != args) { gtcAssert.isTrue(args.length == types.length, gtcStrUtil.format( "Params
	 * length [{}] is not fit for param length [{}] of method !", args.length,
	 * types.length)); Class<?> type; for (int i = 0; i < args.length; i++) { type =
	 * types[i]; if (type.isPrimitive() && null == args[i]) { // 参数是原始类型，而传入参数为null时赋予默认值
	 * args[i] = gtcClassUtil.getDefaultValue(type); } } }
	 *
	 * return invoke(obj, method, args); }
	 */

	/**
	 * 执行方法
	 *
	 * <p>
	 * 对于用户传入参数会做必要检查，包括：
	 *
	 * <pre>
	 *     1、忽略多余的参数
	 *     2、参数不够补齐默认值
	 *     3、传入参数为null，但是目标参数类型为原始类型，做转换
	 * </pre>
	 * @param <T> 返回对象类型
	 * @param obj 对象，如果执行静态方法，此值为<code>null</code>
	 * @param method 方法（对象方法或static方法都可）
	 * @param args 参数对象
	 * @return 结果
	 * @throws GirException 一些列异常的包装
	 *
	 * @SuppressWarnings("unchecked") public static <T> T invoke(Object obj, Method
	 * method, Object... args) throws gtcException { setAccessible(method);
	 *
	 * // 检查用户传入参数： // 1、忽略多余的参数 // 2、参数不够补齐默认值 // 3、通过NullWrapperBean传递的参数,会直接赋值null //
	 * 4、传入参数为null，但是目标参数类型为原始类型，做转换 // 5、传入参数类型不对应，尝试转换类型 final Class<?>[] parameterTypes
	 * = method.getParameterTypes(); final Object[] actualArgs = new
	 * Object[parameterTypes.length]; if (null != args) { for (int i = 0; i <
	 * actualArgs.length; i++) { if (i >= args.length || null == args[i]) { // 越界或者空值
	 * actualArgs[i] = gtcClassUtil.getDefaultValue(parameterTypes[i]); } else if (args[i]
	 * instanceof NullWrapperBean) { //如果是通过NullWrapperBean传递的null参数,直接赋值null
	 * actualArgs[i] = null; } else if (false ==
	 * parameterTypes[i].isAssignableFrom(args[i].getClass())) {
	 * //对于类型不同的字段，尝试转换，转换失败则使用原对象类型 final Object targetValue =
	 * Convert.convert(parameterTypes[i], args[i]); if (null != targetValue) {
	 * actualArgs[i] = targetValue; } } else { actualArgs[i] = args[i]; } } }
	 *
	 * try { return (T) method.invoke( gtcClassUtil.isStatic(method) ? null : obj,
	 * actualArgs); } catch (Exception e) { throw new gtcException(e); } }
	 */

	/**
	 * 执行对象中指定方法 如果需要传递的参数为null,请使用NullWrapperBean来传递,不然会丢失类型信息
	 * @param <T> 返回对象类型
	 * @param obj 方法所在对象
	 * @param methodName 方法名
	 * @param args 参数列表
	 * @return 执行结果
	 * @throws GirException IllegalAccessException包装
	 * @see NullWrapperBean
	 * @since 3.1.2
	 *
	 * public static <T> T invoke(Object obj, String methodName, Object... args) throws
	 * gtcException { final Method method = getMethodOfObj(obj, methodName, args); if
	 * (null == method) { throw new gtcException( gtcStrUtil.format("No such method:
	 * [{}]", methodName)); } return invoke(obj, method, args); }
	 */

	/**
	 * 设置方法为可访问（私有方法可以被外部调用）
	 * @param <T> AccessibleObject的子类，比如Class、Method、Field等
	 * @param accessibleObject 可设置访问权限的对象，比如Class、Method、Field等
	 * @return 被设置可访问的对象
	 * @since 4.6.8
	 */
	public static <T extends AccessibleObject> T setAccessible(T accessibleObject) {
		if (null != accessibleObject && false == accessibleObject.isAccessible()) {
			accessibleObject.setAccessible(true);
		}
		return accessibleObject;
	}

}
