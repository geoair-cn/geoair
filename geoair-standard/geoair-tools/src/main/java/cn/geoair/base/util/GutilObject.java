package cn.geoair.base.util;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 对象工具 来自 spring ObjectUtil + hutool ObjectUtil
 *
 * @author
 */
public abstract class GutilObject {

    private static final int INITIAL_HASH = 7;

    private static final int MULTIPLIER = 31;

    private static final String EMPTY_STRING = "";

    private static final String NULL_STRING = "null";

    private static final String ARRAY_START = "{";

    private static final String ARRAY_END = "}";

    private static final String EMPTY_ARRAY = ARRAY_START + ARRAY_END;

    private static final String ARRAY_ELEMENT_SEPARATOR = ", ";

    /**
     * 判断给定的 throwable 是否为受检异常：即既不是 RuntimeException 也不是 Error。
     *
     * @param ex 要检查的 throwable
     * @return 该 throwable 是否为受检异常
     * @see java.lang.Exception
     * @see java.lang.RuntimeException
     * @see java.lang.Error
     */
    public static boolean isCheckedException(Throwable ex) {
        return !(ex instanceof RuntimeException || ex instanceof Error);
    }

    /**
     * 检查给定的异常是否与 throws 子句中声明的异常类型兼容。
     *
     * @param ex 要检查的异常
     * @param declaredExceptions throws 子句中声明的异常类型
     * @return 给定的异常是否兼容
     */
    public static boolean isCompatibleWithThrowsClause(
            Throwable ex, Class<?>... declaredExceptions) {
        if (!isCheckedException(ex)) {
            return true;
        }
        if (declaredExceptions != null) {
            for (Class<?> declaredException : declaredExceptions) {
                if (declaredException.isInstance(ex)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断给定的对象是否为数组：对象数组或基本类型数组均可。
     *
     * @param obj 要检查的对象
     */
    public static boolean isArray(Object obj) {
        return (obj != null && obj.getClass().isArray());
    }

    /**
     * 判断给定的数组是否为空：即 {@code null} 或长度为 0。
     *
     * @param array 要检查的数组
     * @see #isEmpty(Object)
     */
    public static boolean isEmpty(Object[] array) {
        return (array == null || array.length == 0);
    }

    /**
     * 判断给定的对象是否为空。
     *
     * <p>本方法支持以下对象类型。
     *
     * <ul>
     *   <li>{@code Optional}：为 {@link Optional#empty()} 时视为空
     *   <li>{@code Array}：长度为 0 时视为空
     *   <li>{@link CharSequence}：长度为 0 时视为空
     *   <li>{@link String}：为 {@code null} 或仅包含空白字符时视为空（即去除首尾空白后长度为 0）
     *   <li>{@link Collection}：委托 {@link Collection#isEmpty()}
     *   <li>{@link Map}：委托 {@link Map#isEmpty()}
     * </ul>
     *
     * <p>如果给定的对象非 {@code null} 且不属于上述支持类型，本方法返回 {@code false}。
     *
     * @param obj 要检查的对象
     * @return 对象为 {@code null} 或 <em>空</em> 时返回 {@code true}
     * @since 4.2
     * @see Optional#isPresent()
     * @see #isEmpty(Object[])
     */
    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        }
        if (obj instanceof String) {
            return ((String) obj).trim().length() == 0;
        }
        if (obj instanceof Optional) {
            return !((Optional) obj).isPresent();
        }
        if (obj instanceof CharSequence) {
            return ((CharSequence) obj).length() == 0;
        }
        if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        }
        if (obj instanceof Collection) {
            return ((Collection) obj).isEmpty();
        }
        if (obj instanceof Map) {
            return ((Map) obj).isEmpty();
        }

        // 其他类型
        return false;
    }

    /**
     * 解包可能为 {@link java.util.Optional} 的给定对象。
     *
     * @param obj 候选对象
     * @return {@code Optional} 内部持有的值；{@code Optional} 为空时返回 {@code null}；
     *     否则原样返回给定对象
     * @since 5.0
     */
    public static Object unwrapOptional(Object obj) {
        if (obj instanceof Optional) {
            Optional<?> optional = (Optional<?>) obj;
            if (!optional.isPresent()) {
                return null;
            }
            Object result = optional.get();
            GutilAssert.isTrue(
                    !(result instanceof Optional), "Multi-level Optional usage not supported");
            return result;
        }
        return obj;
    }

    /**
     * 检查给定的数组是否包含给定的元素。
     *
     * @param array 要检查的数组（可以为 {@code null}，此时返回值恒为 {@code false}）
     * @param element 要查找的元素
     * @return 是否在给定数组中找到该元素
     */
    public static boolean containsElement(Object[] array, Object element) {
        if (array == null) {
            return false;
        }
        for (Object arrayEle : array) {
            if (nullSafeEquals(arrayEle, element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查给定的枚举常量数组中是否存在指定名称的常量（判断匹配时忽略大小写）。
     *
     * @param enumValues 要检查的枚举值，通常通过 {@code MyEnum.values()} 获取
     * @param constant 要查找的常量名称（不能为 null 或空字符串）
     * @return 是否在给定数组中找到该常量
     */
    public static boolean containsConstant(Enum<?>[] enumValues, String constant) {
        return containsConstant(enumValues, constant, false);
    }

    /**
     * 检查给定的枚举常量数组中是否存在指定名称的常量。
     *
     * @param enumValues 要检查的枚举值，通常通过 {@code MyEnum.values()} 获取
     * @param constant 要查找的常量名称（不能为 null 或空字符串）
     * @param caseSensitive 匹配时是否区分大小写
     * @return 是否在给定数组中找到该常量；{@code enumValues} 为 {@code null} 时返回 {@code false}
     */
    public static boolean containsConstant(
            Enum<?>[] enumValues, String constant, boolean caseSensitive) {
        if (enumValues == null) {
            return false;
        }
        for (Enum<?> candidate : enumValues) {
            if (caseSensitive
                    ? candidate.toString().equals(constant)
                    : candidate.toString().equalsIgnoreCase(constant)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@link Enum#valueOf(Class, String)} 的不区分大小写替代方案。
     *
     * @param <E> 具体枚举类型
     * @param enumValues 所有枚举常量的数组，通常按 {@code Enum.values()} 获取
     * @param constant 要获取枚举值的常量名
     * @return 匹配的枚举常量；{@code enumValues} 为 {@code null} 或未找到给定常量时返回 {@code null}
     * @throws IllegalArgumentException 如果在给定枚举数组中未找到给定常量。可先用
     *     {@link #containsConstant(Enum[], String)} 作为守卫以避免该异常。
     */
    public static <E extends Enum<?>> E caseInsensitiveValueOf(E[] enumValues, String constant) {
        if (enumValues == null) {
            return null;
        }
        for (E candidate : enumValues) {
            if (candidate.toString().equalsIgnoreCase(constant)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException(
                "Constant ["
                        + constant
                        + "] does not exist in enum type "
                        + enumValues.getClass().getComponentType().getName());
    }

    /**
     * 将给定对象追加到给定数组中，返回由输入数组内容加给定对象组成的新数组。
     *
     * @param array 要追加的数组（可以为 {@code null}）
     * @param obj 要追加的对象
     * @return 新数组（组件类型相同；永不为 {@code null}）
     * @throws IllegalArgumentException 如果 {@code obj} 不能赋值给 {@code array} 的组件类型
     */
    public static <A, O extends A> A[] addObjectToArray(A[] array, O obj) {
        Class<?> compType = Object.class;
        if (array != null) {
            compType = array.getClass().getComponentType();
        } else if (obj != null) {
            compType = obj.getClass();
        }
        if (obj != null && array != null && !compType.isInstance(obj)) {
            throw new IllegalArgumentException(
                    "Cannot append object of type ["
                            + obj.getClass().getName()
                            + "] to an array of component type ["
                            + compType.getName()
                            + "]");
        }
        int newArrLength = (array != null ? array.length + 1 : 1);
        @SuppressWarnings("unchecked")
        A[] newArr = (A[]) Array.newInstance(compType, newArrLength);
        if (array != null) {
            System.arraycopy(array, 0, newArr, 0, array.length);
        }
        newArr[newArr.length - 1] = obj;
        return newArr;
    }

    /**
     * 将给定数组（可能是基本类型数组）转换为对象数组（必要时转换为基本类型包装对象）。
     *
     * <p>{@code null} 源值将被转换为空的对象数组。
     *
     * @param source 待转换的（可能是基本类型的）数组
     * @return 对应的对象数组（永不为 {@code null}）
     * @throws IllegalArgumentException 如果参数不是数组
     */
    public static Object[] toObjectArray(Object source) {
        if (source instanceof Object[]) {
            return (Object[]) source;
        }
        if (source == null) {
            return new Object[0];
        }
        if (!source.getClass().isArray()) {
            throw new IllegalArgumentException("Source is not an array: " + source);
        }
        int length = Array.getLength(source);
        if (length == 0) {
            return new Object[0];
        }
        Class<?> wrapperType = Array.get(source, 0).getClass();
        Object[] newArray = (Object[]) Array.newInstance(wrapperType, length);
        for (int i = 0; i < length; i++) {
            newArray[i] = Array.get(source, i);
        }
        return newArray;
    }

    // ---------------------------------------------------------------------
    // 基于内容的相等性/哈希码处理便捷方法
    // ---------------------------------------------------------------------

    /**
     * 判断给定对象是否相等：两者均为 {@code null} 时返回 {@code true}，仅一方为 {@code null} 时返回
     * {@code false}。
     *
     * <p>使用 {@code Arrays.equals} 比较数组，即基于数组元素而非数组引用进行相等性判断。
     *
     * @param o1 第一个要比较的对象
     * @param o2 第二个要比较的对象
     * @return 给定的对象是否相等
     * @see Object#equals(Object)
     * @see java.util.Arrays#equals
     */
    public static boolean nullSafeEquals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        if (o1.equals(o2)) {
            return true;
        }
        if (o1.getClass().isArray() && o2.getClass().isArray()) {
            return arrayEquals(o1, o2);
        }
        return false;
    }

    /**
     * 使用 {@code Arrays.equals} 比较给定数组，即基于数组元素而非数组引用进行相等性判断。
     *
     * @param o1 第一个要比较的数组
     * @param o2 第二个要比较的数组
     * @return 给定的对象是否相等
     * @see # SafeEquals(Object, Object)
     * @see java.util.Arrays#equals
     */
    private static boolean arrayEquals(Object o1, Object o2) {
        if (o1 instanceof Object[] && o2 instanceof Object[]) {
            return Arrays.equals((Object[]) o1, (Object[]) o2);
        }
        if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
            return Arrays.equals((boolean[]) o1, (boolean[]) o2);
        }
        if (o1 instanceof byte[] && o2 instanceof byte[]) {
            return Arrays.equals((byte[]) o1, (byte[]) o2);
        }
        if (o1 instanceof char[] && o2 instanceof char[]) {
            return Arrays.equals((char[]) o1, (char[]) o2);
        }
        if (o1 instanceof double[] && o2 instanceof double[]) {
            return Arrays.equals((double[]) o1, (double[]) o2);
        }
        if (o1 instanceof float[] && o2 instanceof float[]) {
            return Arrays.equals((float[]) o1, (float[]) o2);
        }
        if (o1 instanceof int[] && o2 instanceof int[]) {
            return Arrays.equals((int[]) o1, (int[]) o2);
        }
        if (o1 instanceof long[] && o2 instanceof long[]) {
            return Arrays.equals((long[]) o1, (long[]) o2);
        }
        if (o1 instanceof short[] && o2 instanceof short[]) {
            return Arrays.equals((short[]) o1, (short[]) o2);
        }
        return false;
    }

    /**
     * 返回给定对象的哈希码，通常为 {@code Object#hashCode()} 的值。如果对象是数组，
     * 本方法将委托给本类中针对数组的任一 {@code nullSafeHashCode} 方法。如果对象为
     * {@code null}，本方法返回 0。
     *
     * @see Object#hashCode()
     * @see # SafeHashCode(Object[])
     * @see # SafeHashCode(boolean[])
     * @see # SafeHashCode(byte[])
     * @see # SafeHashCode(char[])
     * @see # SafeHashCode(double[])
     * @see # SafeHashCode(float[])
     * @see # SafeHashCode(int[])
     * @see # SafeHashCode(long[])
     * @see # SafeHashCode(short[])
     */
    public static int nullSafeHashCode(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (obj.getClass().isArray()) {
            if (obj instanceof Object[]) {
                return nullSafeHashCode((Object[]) obj);
            }
            if (obj instanceof boolean[]) {
                return nullSafeHashCode((boolean[]) obj);
            }
            if (obj instanceof byte[]) {
                return nullSafeHashCode((byte[]) obj);
            }
            if (obj instanceof char[]) {
                return nullSafeHashCode((char[]) obj);
            }
            if (obj instanceof double[]) {
                return nullSafeHashCode((double[]) obj);
            }
            if (obj instanceof float[]) {
                return nullSafeHashCode((float[]) obj);
            }
            if (obj instanceof int[]) {
                return nullSafeHashCode((int[]) obj);
            }
            if (obj instanceof long[]) {
                return nullSafeHashCode((long[]) obj);
            }
            if (obj instanceof short[]) {
                return nullSafeHashCode((short[]) obj);
            }
        }
        return obj.hashCode();
    }

    /**
     * 根据指定数组的内容返回哈希码。如果 {@code array} 为 {@code null}，本方法返回 0。
     */
    public static int nullSafeHashCode(Object[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (Object element : array) {
            hash = MULTIPLIER * hash + nullSafeHashCode(element);
        }
        return hash;
    }

    /**
     * 根据指定数组的内容返回哈希码。如果 {@code array} 为 {@code null}，本方法返回 0。
     */
    public static int nullSafeHashCode(boolean[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (boolean element : array) {
            hash = MULTIPLIER * hash + Boolean.hashCode(element);
        }
        return hash;
    }

    /**
     * 根据指定数组的内容返回哈希码。如果 {@code array} 为 {@code null}，本方法返回 0。
     */
    public static int nullSafeHashCode(byte[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (byte element : array) {
            hash = MULTIPLIER * hash + element;
        }
        return hash;
    }

    /**
     * 根据指定数组的内容返回哈希码。如果 {@code array} 为 {@code null}，本方法返回 0。
     */
    public static int nullSafeHashCode(char[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (char element : array) {
            hash = MULTIPLIER * hash + element;
        }
        return hash;
    }

    /**
     * 根据指定数组的内容返回哈希码。如果 {@code array} 为 {@code null}，本方法返回 0。
     */
    public static int nullSafeHashCode(double[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (double element : array) {
            hash = MULTIPLIER * hash + Double.hashCode(element);
        }
        return hash;
    }

    /**
     * 根据指定数组的内容返回哈希码。如果 {@code array} 为 {@code null}，本方法返回 0。
     */
    public static int nullSafeHashCode(float[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (float element : array) {
            hash = MULTIPLIER * hash + Float.hashCode(element);
        }
        return hash;
    }

    /**
     * 根据指定数组的内容返回哈希码。如果 {@code array} 为 {@code null}，本方法返回 0。
     */
    public static int nullSafeHashCode(int[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (int element : array) {
            hash = MULTIPLIER * hash + element;
        }
        return hash;
    }

    /**
     * 根据指定数组的内容返回哈希码。如果 {@code array} 为 {@code null}，本方法返回 0。
     */
    public static int nullSafeHashCode(long[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (long element : array) {
            hash = MULTIPLIER * hash + Long.hashCode(element);
        }
        return hash;
    }

    /**
     * 根据指定数组的内容返回哈希码。如果 {@code array} 为 {@code null}，本方法返回 0。
     */
    public static int nullSafeHashCode(short[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (short element : array) {
            hash = MULTIPLIER * hash + element;
        }
        return hash;
    }

    /**
     * 返回与 {@link Boolean#hashCode(boolean)} 相同的值。
     *
     * @deprecated 自 Spring Framework 5.0 起已弃用，请使用 JDK 8 原生变体
     */
    @Deprecated
    public static int hashCode(boolean bool) {
        return Boolean.hashCode(bool);
    }

    /**
     * 返回与 {@link Double#hashCode(double)} 相同的值。
     *
     * @deprecated 自 Spring Framework 5.0 起已弃用，请使用 JDK 8 原生变体
     */
    @Deprecated
    public static int hashCode(double dbl) {
        return Double.hashCode(dbl);
    }

    /**
     * 返回与 {@link Float#hashCode(float)} 相同的值。
     *
     * @deprecated 自 Spring Framework 5.0 起已弃用，请使用 JDK 8 原生变体
     */
    @Deprecated
    public static int hashCode(float flt) {
        return Float.hashCode(flt);
    }

    /**
     * 返回与 {@link Long#hashCode(long)} 相同的值。
     *
     * @deprecated 自 Spring Framework 5.0 起已弃用，请使用 JDK 8 原生变体
     */
    @Deprecated
    public static int hashCode(long lng) {
        return Long.hashCode(lng);
    }

    // ---------------------------------------------------------------------
    // toString 输出便捷方法
    // ---------------------------------------------------------------------

    /**
     * 返回对象整体身份的字符串表示。
     *
     * @param obj 对象（可以为 {@code null}）
     * @return 对象的身份字符串表示；对象为 {@code null} 时返回空字符串
     */
    public static String identityToString(Object obj) {
        if (obj == null) {
            return EMPTY_STRING;
        }
        return obj.getClass().getName() + "@" + getIdentityHexString(obj);
    }

    /**
     * 返回对象身份哈希码的十六进制字符串形式。
     *
     * @param obj 对象
     * @return 对象的身份哈希码的十六进制表示
     */
    public static String getIdentityHexString(Object obj) {
        return Integer.toHexString(System.identityHashCode(obj));
    }

    /**
     * 如果 {@code obj} 不为 {@code null}，返回基于内容的字符串表示；否则返回空字符串。
     *
     * <p>与 {@link #SafeToString(Object)} 不同之处在于：对于 {@code null} 值，本方法返回空字符串
     * 而非 "null"。
     *
     * @param obj 要构建显示字符串的对象
     * @return {@code obj} 的显示字符串表示
     * @see #SafeToString(Object)
     */
    public static String getDisplayString(Object obj) {
        if (obj == null) {
            return EMPTY_STRING;
        }
        return nullSafeToString(obj);
    }

    /**
     * 确定给定对象的类名。
     *
     * <p>如果 {@code obj} 为 {@code null}，返回 {@code "null"}。
     *
     * @param obj 要内省的对象（可以为 {@code null}）
     * @return 对应的类名
     */
    public static String nullSafeClassName(Object obj) {
        return (obj != null ? obj.getClass().getName() : NULL_STRING);
    }

    /**
     * 返回指定对象的字符串表示。
     *
     * <p>对于数组，构建其内容的字符串表示。如果 {@code obj} 为 {@code null}，返回 {@code "null"}。
     *
     * @param obj 要构建字符串表示的对象
     * @return {@code obj} 的字符串表示
     */
    public static String nullSafeToString(Object obj) {
        if (obj == null) {
            return NULL_STRING;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof Object[]) {
            return nullSafeToString((Object[]) obj);
        }
        if (obj instanceof boolean[]) {
            return nullSafeToString((boolean[]) obj);
        }
        if (obj instanceof byte[]) {
            return nullSafeToString((byte[]) obj);
        }
        if (obj instanceof char[]) {
            return nullSafeToString((char[]) obj);
        }
        if (obj instanceof double[]) {
            return nullSafeToString((double[]) obj);
        }
        if (obj instanceof float[]) {
            return nullSafeToString((float[]) obj);
        }
        if (obj instanceof int[]) {
            return nullSafeToString((int[]) obj);
        }
        if (obj instanceof long[]) {
            return nullSafeToString((long[]) obj);
        }
        if (obj instanceof short[]) {
            return nullSafeToString((short[]) obj);
        }
        String str = obj.toString();
        return (str != null ? str : EMPTY_STRING);
    }

    /**
     * 返回指定数组内容的字符串表示。
     *
     * <p>字符串表示由数组元素列表组成，外层用花括号（{@code "{}"}）包围。相邻元素以
     * {@code ", "}（逗号加空格）分隔。如果 {@code array} 为 {@code null}，返回 {@code "null"}。
     *
     * @param array 要构建字符串表示的数组
     * @return {@code array} 的字符串表示
     */
    public static String nullSafeToString(Object[] array) {
        if (array == null) {
            return NULL_STRING;
        }
        int length = array.length;
        if (length == 0) {
            return EMPTY_ARRAY;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i == 0) {
                sb.append(ARRAY_START);
            } else {
                sb.append(ARRAY_ELEMENT_SEPARATOR);
            }
            sb.append(String.valueOf(array[i]));
        }
        sb.append(ARRAY_END);
        return sb.toString();
    }

    /**
     * 返回指定数组内容的字符串表示。
     *
     * <p>字符串表示由数组元素列表组成，外层用花括号（{@code "{}"}）包围。相邻元素以
     * {@code ", "}（逗号加空格）分隔。如果 {@code array} 为 {@code null}，返回 {@code "null"}。
     *
     * @param array 要构建字符串表示的数组
     * @return {@code array} 的字符串表示
     */
    public static String nullSafeToString(boolean[] array) {
        if (array == null) {
            return NULL_STRING;
        }
        int length = array.length;
        if (length == 0) {
            return EMPTY_ARRAY;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i == 0) {
                sb.append(ARRAY_START);
            } else {
                sb.append(ARRAY_ELEMENT_SEPARATOR);
            }

            sb.append(array[i]);
        }
        sb.append(ARRAY_END);
        return sb.toString();
    }

    /**
     * 返回指定数组内容的字符串表示。
     *
     * <p>字符串表示由数组元素列表组成，外层用花括号（{@code "{}"}）包围。相邻元素以
     * {@code ", "}（逗号加空格）分隔。如果 {@code array} 为 {@code null}，返回 {@code "null"}。
     *
     * @param array 要构建字符串表示的数组
     * @return {@code array} 的字符串表示
     */
    public static String nullSafeToString(byte[] array) {
        if (array == null) {
            return NULL_STRING;
        }
        int length = array.length;
        if (length == 0) {
            return EMPTY_ARRAY;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i == 0) {
                sb.append(ARRAY_START);
            } else {
                sb.append(ARRAY_ELEMENT_SEPARATOR);
            }
            sb.append(array[i]);
        }
        sb.append(ARRAY_END);
        return sb.toString();
    }

    /**
     * 返回指定数组内容的字符串表示。
     *
     * <p>字符串表示由数组元素列表组成，外层用花括号（{@code "{}"}）包围。相邻元素以
     * {@code ", "}（逗号加空格）分隔。如果 {@code array} 为 {@code null}，返回 {@code "null"}。
     *
     * @param array 要构建字符串表示的数组
     * @return {@code array} 的字符串表示
     */
    public static String nullSafeToString(char[] array) {
        if (array == null) {
            return NULL_STRING;
        }
        int length = array.length;
        if (length == 0) {
            return EMPTY_ARRAY;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i == 0) {
                sb.append(ARRAY_START);
            } else {
                sb.append(ARRAY_ELEMENT_SEPARATOR);
            }
            sb.append("'").append(array[i]).append("'");
        }
        sb.append(ARRAY_END);
        return sb.toString();
    }

    /**
     * 返回指定数组内容的字符串表示。
     *
     * <p>字符串表示由数组元素列表组成，外层用花括号（{@code "{}"}）包围。相邻元素以
     * {@code ", "}（逗号加空格）分隔。如果 {@code array} 为 {@code null}，返回 {@code "null"}。
     *
     * @param array 要构建字符串表示的数组
     * @return {@code array} 的字符串表示
     */
    public static String nullSafeToString(double[] array) {
        if (array == null) {
            return NULL_STRING;
        }
        int length = array.length;
        if (length == 0) {
            return EMPTY_ARRAY;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i == 0) {
                sb.append(ARRAY_START);
            } else {
                sb.append(ARRAY_ELEMENT_SEPARATOR);
            }

            sb.append(array[i]);
        }
        sb.append(ARRAY_END);
        return sb.toString();
    }

    /**
     * 返回指定数组内容的字符串表示。
     *
     * <p>字符串表示由数组元素列表组成，外层用花括号（{@code "{}"}）包围。相邻元素以
     * {@code ", "}（逗号加空格）分隔。如果 {@code array} 为 {@code null}，返回 {@code "null"}。
     *
     * @param array 要构建字符串表示的数组
     * @return {@code array} 的字符串表示
     */
    public static String nullSafeToString(float[] array) {
        if (array == null) {
            return NULL_STRING;
        }
        int length = array.length;
        if (length == 0) {
            return EMPTY_ARRAY;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i == 0) {
                sb.append(ARRAY_START);
            } else {
                sb.append(ARRAY_ELEMENT_SEPARATOR);
            }

            sb.append(array[i]);
        }
        sb.append(ARRAY_END);
        return sb.toString();
    }

    /**
     * 返回指定数组内容的字符串表示。
     *
     * <p>字符串表示由数组元素列表组成，外层用花括号（{@code "{}"}）包围。相邻元素以
     * {@code ", "}（逗号加空格）分隔。如果 {@code array} 为 {@code null}，返回 {@code "null"}。
     *
     * @param array 要构建字符串表示的数组
     * @return {@code array} 的字符串表示
     */
    public static String nullSafeToString(int[] array) {
        if (array == null) {
            return NULL_STRING;
        }
        int length = array.length;
        if (length == 0) {
            return EMPTY_ARRAY;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i == 0) {
                sb.append(ARRAY_START);
            } else {
                sb.append(ARRAY_ELEMENT_SEPARATOR);
            }
            sb.append(array[i]);
        }
        sb.append(ARRAY_END);
        return sb.toString();
    }

    /**
     * 返回指定数组内容的字符串表示。
     *
     * <p>字符串表示由数组元素列表组成，外层用花括号（{@code "{}"}）包围。相邻元素以
     * {@code ", "}（逗号加空格）分隔。如果 {@code array} 为 {@code null}，返回 {@code "null"}。
     *
     * @param array 要构建字符串表示的数组
     * @return {@code array} 的字符串表示
     */
    public static String nullSafeToString(long[] array) {
        if (array == null) {
            return NULL_STRING;
        }
        int length = array.length;
        if (length == 0) {
            return EMPTY_ARRAY;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i == 0) {
                sb.append(ARRAY_START);
            } else {
                sb.append(ARRAY_ELEMENT_SEPARATOR);
            }
            sb.append(array[i]);
        }
        sb.append(ARRAY_END);
        return sb.toString();
    }

    /**
     * 返回指定数组内容的字符串表示。
     *
     * <p>字符串表示由数组元素列表组成，外层用花括号（{@code "{}"}）包围。相邻元素以
     * {@code ", "}（逗号加空格）分隔。如果 {@code array} 为 {@code null}，返回 {@code "null"}。
     *
     * @param array 要构建字符串表示的数组
     * @return {@code array} 的字符串表示
     */
    public static String nullSafeToString(short[] array) {
        if (array == null) {
            return NULL_STRING;
        }
        int length = array.length;
        if (length == 0) {
            return EMPTY_ARRAY;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i == 0) {
                sb.append(ARRAY_START);
            } else {
                sb.append(ARRAY_ELEMENT_SEPARATOR);
            }
            sb.append(array[i]);
        }
        sb.append(ARRAY_END);
        return sb.toString();
    }

    /**
     * 比较两个对象是否相等，此方法是 {@link #equal(Object, Object)}的别名方法。<br>
     * 比较策略与{@link #equal(Object, Object)}完全一致，包括BigDecimal数值比较与数组内容深度比较。<br>
     *
     * @param obj1 对象1
     * @param obj2 对象2
     * @return 是否相等
     * @see #equal(Object, Object)
     * @since 5.4.3
     */
    public static boolean equals(Object obj1, Object obj2) {
        return equal(obj1, obj2);
    }

    /**
     * 比较两个对象是否相等。<br>
     * 相同的条件有以下几个，满足其一即可：<br>
     *
     * <ol>
     *   <li>obj1 == null &amp;&amp; obj2 == null
     *   <li>obj1.equals(obj2)
     *   <li>任一对象为BigDecimal时，按数值比较（{@code compareTo}结果为0即相等，忽略精度，如0.00 == 0）；
     *       另一侧为{@link Number}时按数值转换后比较，否则视为不相等
     *   <li>两侧均为数组时，按元素内容深度比较（委托 {@link #nullSafeEquals(Object, Object)}）
     * </ol>
     *
     * @param obj1 对象1
     * @param obj2 对象2
     * @return 是否相等
     * @see Objects#equals(Object, Object)
     */
    public static boolean equal(Object obj1, Object obj2) {
        if (obj1 instanceof BigDecimal || obj2 instanceof BigDecimal) {
            if (obj1 instanceof BigDecimal && obj2 instanceof BigDecimal) {
                return GutilNumber.equals((BigDecimal) obj1, (BigDecimal) obj2);
            }
            BigDecimal bigDecimal = (BigDecimal) (obj1 instanceof BigDecimal ? obj1 : obj2);
            Object other = obj1 instanceof BigDecimal ? obj2 : obj1;
            if (other instanceof Number) {
                return bigDecimal.compareTo(new BigDecimal(other.toString())) == 0;
            }
            return false;
        }
        if (obj1 != null && obj2 != null && obj1.getClass().isArray() && obj2.getClass().isArray()) {
            return arrayEquals(obj1, obj2);
        }
        return Objects.equals(obj1, obj2);
    }

    /**
     * 比较两个对象是否不相等。<br>
     *
     * @param obj1 对象1
     * @param obj2 对象2
     * @return 是否不等
     * @since 3.0.7
     */
    public static boolean notEqual(Object obj1, Object obj2) {
        return false == equal(obj1, obj2);
    }

    /**
     * 计算对象长度，如果是字符串调用其length函数，集合类调用其size函数，数组调用其length属性，其他可遍历对象遍历计算长度<br>
     * 支持的类型包括：
     *
     * <ul>
     *   <li>CharSequence
     *   <li>Map
     *   <li>Iterator
     *   <li>Enumeration
     *   <li>Array
     * </ul>
     *
     * @param obj 被计算长度的对象
     * @return 长度，不支持的类型的返回 -1
     * @apiNote 当对象为 {@link Iterator} 或 {@link Enumeration} 时，必须遍历才能计数，因此本方法会<b>消费（耗尽）</b>
     *     传入的迭代器/枚举：调用后该迭代器/枚举已无剩余元素，不可复用。如有复用需求请先拷贝或改用
     *     {@link Collection#size()}。
     */
    public static int length(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (obj instanceof CharSequence) {
            return ((CharSequence) obj).length();
        }
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).size();
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).size();
        }

        int count;
        if (obj instanceof Iterator) {
            Iterator<?> iter = (Iterator<?>) obj;
            count = 0;
            while (iter.hasNext()) {
                count++;
                iter.next();
            }
            return count;
        }
        if (obj instanceof Enumeration) {
            Enumeration<?> enumeration = (Enumeration<?>) obj;
            count = 0;
            while (enumeration.hasMoreElements()) {
                count++;
                enumeration.nextElement();
            }
            return count;
        }
        if (obj.getClass().isArray() == true) {
            return Array.getLength(obj);
        }
        return -1;
    }

    /**
     * 对象中是否包含元素<br>
     * 支持的对象类型包括：
     *
     * <ul>
     *   <li>String
     *   <li>Collection
     *   <li>Map
     *   <li>Iterator
     *   <li>Enumeration
     *   <li>Array
     * </ul>
     *
     * @param obj 对象
     * @param element 元素
     * @return 是否包含
     * @apiNote 当对象为 {@link Map} 时，本方法判断的是<b>值</b>（{@code containsValue}）而非键；
     *     当对象为 {@link Iterator} 或 {@link Enumeration} 时，本方法会<b>消费（耗尽）</b>传入的迭代器/枚举，
     *     调用后其剩余元素不可再取。
     */
    public static boolean contains(Object obj, Object element) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof String) {
            if (element == null) {
                return false;
            }
            return ((String) obj).contains(element.toString());
        }
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).contains(element);
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).containsValue(element);
        }

        if (obj instanceof Iterator) {
            Iterator<?> iter = (Iterator<?>) obj;
            while (iter.hasNext()) {
                Object o = iter.next();
                if (equal(o, element)) {
                    return true;
                }
            }
            return false;
        }
        if (obj instanceof Enumeration) {
            Enumeration<?> enumeration = (Enumeration<?>) obj;
            while (enumeration.hasMoreElements()) {
                Object o = enumeration.nextElement();
                if (equal(o, element)) {
                    return true;
                }
            }
            return false;
        }
        if (obj.getClass().isArray() == true) {
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                Object o = Array.get(obj, i);
                if (equal(o, element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查对象是否为null<br>
     * 判断标准为 {@code obj == null}。注意：不再调用 {@code equals(null)} 判断，
     * 因为对equals非null安全的类（如{@code List}、{@code Map}）会抛出NPE。
     *
     * @param obj 对象
     * @return 是否为null
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }

    /**
     * 检查对象是否不为null，即 {@code obj != null}。
     *
     * @param obj 对象
     * @return 是否不为null
     */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    /**
     * 判断指定对象是否为空，支持： 使用spring 的isEmpty
     *
     * <pre>
     * 1. CharSequence
     * 2. Map
     * 3. Iterable
     * 4. Iterator
     * 5. Array
     * </pre>
     *
     * @param obj 被判断的对象
     * @return 是否为空，如果类型不支持，返回false
     * @since 4.5.7
     */

    /**
     * 判断指定对象是否为非空，支持：
     *
     * <pre>
     * 1. CharSequence
     * 2. Map
     * 3. Iterable
     * 4. Iterator
     * 5. Array
     * </pre>
     *
     * @param obj 被判断的对象
     * @return 是否为空，如果类型不支持，返回true
     * @since 4.5.7
     */
    public static boolean isNotEmpty(Object obj) {
        return false == isEmpty(obj);
    }

    /**
     * 如果给定对象为{@code null}返回默认值
     *
     * <pre>
     * ObjectUtil.defaultIfNull(null, null)      = null
     * ObjectUtil.defaultIfNull(null, "")        = ""
     * ObjectUtil.defaultIfNull(null, "zz")      = "zz"
     * ObjectUtil.defaultIfNull("abc", *)        = "abc"
     * ObjectUtil.defaultIfNull(Boolean.TRUE, *) = Boolean.TRUE
     * </pre>
     *
     * @param <T> 对象类型
     * @param object 被检查对象，可能为{@code null}
     * @param defaultValue 被检查对象为{@code null}返回的默认值，可以为{@code null}
     * @return 被检查对象为{@code null}返回默认值，否则返回原值
     * @since 3.0.7
     */
    public static <T> T defaultIfNull(final T object, final T defaultValue) {
        return (null != object) ? object : defaultValue;
    }

    /**
     * 如果给定对象为{@code null} 返回默认值, 如果不为null 返回自定义handle处理后的返回值
     *
     * @param source Object 类型对象
     * @param handle 自定义的处理方法，可以为{@code null}；为{@code null}时视为未提供处理逻辑，
     *     直接返回{@code defaultValue}
     * @param defaultValue 默认为空的返回值
     * @param <T> 被检查对象为{@code null}返回默认值，否则返回自定义handle处理后的返回值
     * @return 处理后的返回值
     * @since 5.4.6
     */
    public static <T> T defaultIfNull(
            Object source, Supplier<? extends T> handle, final T defaultValue) {
        if (handle == null || Objects.isNull(source)) {
            return defaultValue;
        }
        return handle.get();
    }

    /**
     * 如果给定对象为{@code null}或者""返回默认值, 否则返回自定义handle处理后的返回值
     *
     * @param str String 类型
     * @param handle 自定义的处理方法，可以为{@code null}；为{@code null}时视为未提供处理逻辑，
     *     直接返回{@code defaultValue}
     * @param defaultValue 默认为空的返回值
     * @param <T> 被检查对象为{@code null}或者 ""返回默认值，否则返回自定义handle处理后的返回值
     * @return 处理后的返回值
     * @since 5.4.6
     */
    public static <T> T defaultIfEmpty(
            String str, Supplier<? extends T> handle, final T defaultValue) {
        if (handle == null || GutilStr.isEmpty(str)) {
            return defaultValue;
        }
        return handle.get();
    }

    /**
     * 如果给定对象为{@code null}或者 "" 返回默认值
     *
     * <pre>
     * ObjectUtil.defaultIfEmpty(null, null)      = null
     * ObjectUtil.defaultIfEmpty(null, "")        = ""
     * ObjectUtil.defaultIfEmpty("", "zz")      = "zz"
     * ObjectUtil.defaultIfEmpty(" ", "zz")      = " "
     * ObjectUtil.defaultIfEmpty("abc", *)        = "abc"
     * </pre>
     *
     * @param <T> 对象类型（必须实现CharSequence接口）
     * @param str 被检查对象，可能为{@code null}
     * @param defaultValue 被检查对象为{@code null}或者 ""返回的默认值，可以为{@code null}或者 ""
     * @return 被检查对象为{@code null}或者 ""返回默认值，否则返回原值
     * @since 5.0.4
     */
    public static <T extends CharSequence> T defaultIfEmpty(final T str, final T defaultValue) {
        return GutilStr.isEmpty(str) ? defaultValue : str;
    }

    /**
     * 如果给定对象为{@code null}或者""或者空白符返回默认值
     *
     * <pre>
     * ObjectUtil.defaultIfEmpty(null, null)      = null
     * ObjectUtil.defaultIfEmpty(null, "")        = ""
     * ObjectUtil.defaultIfEmpty("", "zz")      = "zz"
     * ObjectUtil.defaultIfEmpty(" ", "zz")      = "zz"
     * ObjectUtil.defaultIfEmpty("abc", *)        = "abc"
     * </pre>
     *
     * @param <T> 对象类型（必须实现CharSequence接口）
     * @param str 被检查对象，可能为{@code null}
     * @param defaultValue 被检查对象为{@code null}或者 ""或者空白符返回的默认值，可以为{@code null}或者 ""或者空白符
     * @return 被检查对象为{@code null}或者 ""或者空白符返回默认值，否则返回原值
     * @since 5.0.4
     */
    public static <T extends CharSequence> T defaultIfBlank(final T str, final T defaultValue) {
        return GutilStr.isBlank(str) ? defaultValue : str;
    }

    /**
     * 检查是否为有效的数字<br>
     * 检查Double和Float是否为无限大，或者Not a Number<br>
     * 非数字类型和Null将返回true
     *
     * @param obj 被检查类型
     * @return 检查结果，非数字类型和Null将返回true
     */
    public static boolean isValidIfNumber(Object obj) {
        if (obj instanceof Number) {
            return GutilNumber.isValidNumber((Number) obj);
        }
        return true;
    }
}
