package cn.geoair.base.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数字工具类，提供精确的算术运算、数字解析、进制转换、随机数生成等能力。
 *
 * @author
 */
public abstract class GutilNumber {

    private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);

    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    /** 标准数字类型（均为不可变类型）：Byte、Short、Integer、Long、BigInteger、Float、Double、BigDecimal。 */
    public static final Set<Class<?>> STANDARD_NUMBER_TYPES;

    static {
        Set<Class<?>> numberTypes = new HashSet<>(8);
        numberTypes.add(Byte.class);
        numberTypes.add(Short.class);
        numberTypes.add(Integer.class);
        numberTypes.add(Long.class);
        numberTypes.add(BigInteger.class);
        numberTypes.add(Float.class);
        numberTypes.add(Double.class);
        numberTypes.add(BigDecimal.class);
        STANDARD_NUMBER_TYPES = Collections.unmodifiableSet(numberTypes);
    }

    /**
     * 将给定的数字转换为指定目标类的实例。
     *
     * @param number 要转换的数字
     * @param targetClass 转换的目标类
     * @return 转换后的数字
     * @throws IllegalArgumentException 如果不支持目标类（即不是 JDK 内置的标准 Number 子类）
     * @see java.lang.Byte
     * @see java.lang.Short
     * @see java.lang.Integer
     * @see java.lang.Long
     * @see java.math.BigInteger
     * @see java.lang.Float
     * @see java.lang.Double
     * @see java.math.BigDecimal
     */
    @SuppressWarnings("unchecked")
    public static <T extends Number> T convertNumberToTargetClass(
            Number number, Class<T> targetClass) throws IllegalArgumentException {

        GutilAssert.notNull(number, "Number must not be null");
        GutilAssert.notNull(targetClass, "Target class must not be null");

        if (targetClass.isInstance(number)) {
            return (T) number;
        } else if (Byte.class == targetClass) {
            long value = checkedLongValue(number, targetClass);
            if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
                raiseOverflowException(number, targetClass);
            }
            return (T) Byte.valueOf(number.byteValue());
        } else if (Short.class == targetClass) {
            long value = checkedLongValue(number, targetClass);
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                raiseOverflowException(number, targetClass);
            }
            return (T) Short.valueOf(number.shortValue());
        } else if (Integer.class == targetClass) {
            long value = checkedLongValue(number, targetClass);
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                raiseOverflowException(number, targetClass);
            }
            return (T) Integer.valueOf(number.intValue());
        } else if (Long.class == targetClass) {
            long value = checkedLongValue(number, targetClass);
            return (T) Long.valueOf(value);
        } else if (BigInteger.class == targetClass) {
            if (number instanceof BigDecimal) {
                // 不丢失精度 - 使用 BigDecimal 自身的转换
                return (T) ((BigDecimal) number).toBigInteger();
            } else {
                // 原值不是 Big* 数字 - 使用标准的 long 转换
                return (T) BigInteger.valueOf(number.longValue());
            }
        } else if (Float.class == targetClass) {
            return (T) Float.valueOf(number.floatValue());
        } else if (Double.class == targetClass) {
            return (T) Double.valueOf(number.doubleValue());
        } else if (BigDecimal.class == targetClass) {
            // 这里始终使用 BigDecimal(String) 以避免 BigDecimal(double) 的不可预测性
            // （详见 BigDecimal javadoc）
            return (T) new BigDecimal(number.toString());
        } else {
            throw new IllegalArgumentException(
                    "Could not convert number ["
                            + number
                            + "] of type ["
                            + number.getClass().getName()
                            + "] to unsupported target class ["
                            + targetClass.getName()
                            + "]");
        }
    }

    /**
     * 在将给定数字以 long 值返回之前，检查 {@code BigInteger}/{@code BigDecimal} 是否存在 long 溢出。
     *
     * @param number 要转换的数字
     * @param targetClass 转换的目标类
     * @return 无溢出时可转换的 long 值
     * @throws IllegalArgumentException 如果存在溢出
     * @see #raiseOverflowException
     */
    private static long checkedLongValue(Number number, Class<? extends Number> targetClass) {
        BigInteger bigInt = null;
        if (number instanceof BigInteger) {
            bigInt = (BigInteger) number;
        } else if (number instanceof BigDecimal) {
            bigInt = ((BigDecimal) number).toBigInteger();
        }
        // 效果等同于 JDK 8 的 BigInteger.longValueExact()
        if (bigInt != null && (bigInt.compareTo(LONG_MIN) < 0 || bigInt.compareTo(LONG_MAX) > 0)) {
            raiseOverflowException(number, targetClass);
        }
        return number.longValue();
    }

    /**
     * 为给定的数字和目标类抛出 <em>溢出</em> 异常。
     *
     * @param number 我们尝试转换的数字
     * @param targetClass 我们尝试转换的目标类
     * @throws IllegalArgumentException 如果存在溢出
     */
    private static void raiseOverflowException(Number number, Class<?> targetClass) {
        throw new IllegalArgumentException(
                "Could not convert number ["
                        + number
                        + "] of type ["
                        + number.getClass().getName()
                        + "] to target class ["
                        + targetClass.getName()
                        + "]: overflow");
    }

    /**
     * 将给定的 {@code text} 解析为给定目标类的 {@link Number} 实例，使用对应的 {@code decode} / {@code valueOf} 方法。
     *
     * <p>在尝试解析数字之前，会去除输入 {@code String} 中的所有空白字符（首部、尾部和字符之间）。
     *
     * <p>同样支持十六进制格式的数字（以 "0x"、"0X" 或 "#" 开头）。
     *
     * @param text 要转换的文本
     * @param targetClass 解析的目标类
     * @return 解析后的数字
     * @throws IllegalArgumentException 如果不支持目标类（即不是 JDK 内置的标准 Number 子类）
     * @see Byte#decode
     * @see Short#decode
     * @see Integer#decode
     * @see Long#decode
     * @see #decodeBigInteger(String)
     * @see Float#valueOf
     * @see Double#valueOf
     * @see java.math.BigDecimal#BigDecimal(String)
     */
    @SuppressWarnings("unchecked")
    public static <T extends Number> T parseNumber(String text, Class<T> targetClass) {
        GutilAssert.notNull(text, "Text must not be null");
        GutilAssert.notNull(targetClass, "Target class must not be null");
        String trimmed = GutilStr.trimAllWhitespace(text);

        if (Byte.class == targetClass) {
            return (T) (isHexNumber(trimmed) ? Byte.decode(trimmed) : Byte.valueOf(trimmed));
        } else if (Short.class == targetClass) {
            return (T) (isHexNumber(trimmed) ? Short.decode(trimmed) : Short.valueOf(trimmed));
        } else if (Integer.class == targetClass) {
            return (T) (isHexNumber(trimmed) ? Integer.decode(trimmed) : Integer.valueOf(trimmed));
        } else if (Long.class == targetClass) {
            return (T) (isHexNumber(trimmed) ? Long.decode(trimmed) : Long.valueOf(trimmed));
        } else if (BigInteger.class == targetClass) {
            return (T) (isHexNumber(trimmed) ? decodeBigInteger(trimmed) : new BigInteger(trimmed));
        } else if (Float.class == targetClass) {
            return (T) Float.valueOf(trimmed);
        } else if (Double.class == targetClass) {
            return (T) Double.valueOf(trimmed);
        } else if (BigDecimal.class == targetClass || Number.class == targetClass) {
            return (T) new BigDecimal(trimmed);
        } else {
            throw new IllegalArgumentException(
                    "Cannot convert String ["
                            + text
                            + "] to target class ["
                            + targetClass.getName()
                            + "]");
        }
    }

    /**
     * 使用提供的 {@link NumberFormat} 将给定的 {@code text} 解析为给定目标类的 {@link Number} 实例。
     *
     * <p>在尝试解析数字之前，会去除输入 {@code String} 中的空白字符。
     *
     * @param text 要转换的文本
     * @param targetClass 解析的目标类
     * @param numberFormat 用于解析的 {@code NumberFormat}（如果为 {@code null}， 本方法回退到 {@link
     *     #parseNumber(String, Class)}）
     * @return 解析后的数字
     * @throws IllegalArgumentException 如果不支持目标类（即不是 JDK 内置的标准 Number 子类）
     * @see java.text.NumberFormat#parse
     * @see #convertNumberToTargetClass
     * @see #parseNumber(String, Class)
     */
    public static <T extends Number> T parseNumber(
            String text, Class<T> targetClass, NumberFormat numberFormat) {

        if (numberFormat != null) {
            GutilAssert.notNull(text, "Text must not be null");
            GutilAssert.notNull(targetClass, "Target class must not be null");
            NumberFormat parseFormat = numberFormat;
            if (numberFormat instanceof DecimalFormat) {
                // 拷贝一份再操作，避免修改调用方传入的DecimalFormat
                DecimalFormat decimalFormat =
                        (DecimalFormat) ((DecimalFormat) numberFormat).clone();
                if (BigDecimal.class == targetClass && !decimalFormat.isParseBigDecimal()) {
                    decimalFormat.setParseBigDecimal(true);
                }
                parseFormat = decimalFormat;
            }
            try {
                Number number = parseFormat.parse(GutilStr.trimAllWhitespace(text));
                return convertNumberToTargetClass(number, targetClass);
            } catch (ParseException ex) {
                throw new IllegalArgumentException("Could not parse number: " + ex.getMessage());
            }
        } else {
            return parseNumber(text, targetClass);
        }
    }

    /**
     * 判断给定的 {@code value} 字符串是否表示十六进制数字，即需要传入 {@code Integer.decode} 而不是 {@code Integer.valueOf} 等。
     */
    private static boolean isHexNumber(String value) {
        int index = (value.startsWith("-") ? 1 : 0);
        return (value.startsWith("0x", index)
                || value.startsWith("0X", index)
                || value.startsWith("#", index));
    }

    /**
     * 从给定的 {@link String} 值解码 {@link java.math.BigInteger}。
     *
     * <p>支持十进制、十六进制和八进制表示法。
     *
     * @see BigInteger#BigInteger(String, int)
     */
    private static BigInteger decodeBigInteger(String value) {
        int radix = 10;
        int index = 0;
        boolean negative = false;

        // 处理负号（如果存在）。
        if (value.startsWith("-")) {
            negative = true;
            index++;
        }

        // 处理进制前缀（如果存在）。
        if (value.startsWith("0x", index) || value.startsWith("0X", index)) {
            index += 2;
            radix = 16;
        } else if (value.startsWith("#", index)) {
            index++;
            radix = 16;
        } else if (value.startsWith("0", index) && value.length() > 1 + index) {
            index++;
            radix = 8;
        }

        BigInteger result = new BigInteger(value.substring(index), radix);
        return (negative ? result.negate() : result);
    }

    /** 默认除法运算精度 */
    private static final int DEFAUT_DIV_SCALE = 10;

    /** 0-20对应的阶乘，超过20的阶乘会超过Long.MAX_VALUE */
    private static final long[] FACTORIALS =
            new long[] {
                1L,
                1L,
                2L,
                6L,
                24L,
                120L,
                720L,
                5040L,
                40320L,
                362880L,
                3628800L,
                39916800L,
                479001600L,
                6227020800L,
                87178291200L,
                1307674368000L,
                20922789888000L,
                355687428096000L,
                6402373705728000L,
                121645100408832000L,
                2432902008176640000L
            };

    /**
     * 提供精确的加法运算
     *
     * @param v1 被加数
     * @param v2 加数
     * @return 和
     */
    public static double add(float v1, float v2) {
        return add(Float.toString(v1), Float.toString(v2)).doubleValue();
    }

    /**
     * 提供精确的加法运算
     *
     * @param v1 被加数
     * @param v2 加数
     * @return 和
     */
    public static double add(float v1, double v2) {
        return add(Float.toString(v1), Double.toString(v2)).doubleValue();
    }

    /**
     * 提供精确的加法运算
     *
     * @param v1 被加数
     * @param v2 加数
     * @return 和
     */
    public static double add(double v1, float v2) {
        return add(Double.toString(v1), Float.toString(v2)).doubleValue();
    }

    /**
     * 提供精确的加法运算
     *
     * @param v1 被加数
     * @param v2 加数
     * @return 和
     */
    public static double add(double v1, double v2) {
        return add(Double.toString(v1), Double.toString(v2)).doubleValue();
    }

    /**
     * 提供精确的加法运算
     *
     * @param v1 被加数
     * @param v2 加数
     * @return 和
     * @since 3.1.1
     */
    public static double add(Double v1, Double v2) {
        // noinspection RedundantCast
        return add((Number) v1, (Number) v2).doubleValue();
    }

    /**
     * 提供精确的加法运算<br>
     * 如果传入多个值为null或者空，则返回0
     *
     * @param v1 被加数
     * @param v2 加数
     * @return 和
     */
    public static BigDecimal add(Number v1, Number v2) {
        return add(new Number[] {v1, v2});
    }

    /**
     * 提供精确的加法运算<br>
     * 如果传入多个值为null或者空，则返回0
     *
     * @param values 多个被加值
     * @return 和
     * @since 4.0.0
     */
    public static BigDecimal add(Number... values) {
        if (GutilArray.isEmpty(values)) {
            return BigDecimal.ZERO;
        }

        Number value = values[0];
        BigDecimal result = null == value ? BigDecimal.ZERO : new BigDecimal(value.toString());
        for (int i = 1; i < values.length; i++) {
            value = values[i];
            if (null != value) {
                result = result.add(new BigDecimal(value.toString()));
            }
        }
        return result;
    }

    /**
     * 提供精确的加法运算<br>
     * 如果传入多个值为null或者空，则返回0
     *
     * @param values 多个被加值
     * @return 和
     * @since 4.0.0
     */
    public static BigDecimal add(String... values) {
        if (GutilArray.isEmpty(values)) {
            return BigDecimal.ZERO;
        }

        String value = values[0];
        BigDecimal result = null == value ? BigDecimal.ZERO : new BigDecimal(value);
        for (int i = 1; i < values.length; i++) {
            value = values[i];
            if (null != value) {
                result = result.add(new BigDecimal(value));
            }
        }
        return result;
    }

    /**
     * 提供精确的加法运算<br>
     * 如果传入多个值为null或者空，则返回0
     *
     * @param values 多个被加值
     * @return 和
     * @since 4.0.0
     */
    public static BigDecimal add(BigDecimal... values) {
        if (GutilArray.isEmpty(values)) {
            return BigDecimal.ZERO;
        }

        BigDecimal value = values[0];
        BigDecimal result = null == value ? BigDecimal.ZERO : value;
        for (int i = 1; i < values.length; i++) {
            value = values[i];
            if (null != value) {
                result = result.add(value);
            }
        }
        return result;
    }

    /**
     * 提供精确的减法运算
     *
     * @param v1 被减数
     * @param v2 减数
     * @return 差
     */
    public static double sub(float v1, float v2) {
        return sub(Float.toString(v1), Float.toString(v2)).doubleValue();
    }

    /**
     * 提供精确的减法运算
     *
     * @param v1 被减数
     * @param v2 减数
     * @return 差
     */
    public static double sub(float v1, double v2) {
        return sub(Float.toString(v1), Double.toString(v2)).doubleValue();
    }

    /**
     * 提供精确的减法运算
     *
     * @param v1 被减数
     * @param v2 减数
     * @return 差
     */
    public static double sub(double v1, float v2) {
        return sub(Double.toString(v1), Float.toString(v2)).doubleValue();
    }

    /**
     * 提供精确的减法运算
     *
     * @param v1 被减数
     * @param v2 减数
     * @return 差
     */
    public static double sub(double v1, double v2) {
        return sub(Double.toString(v1), Double.toString(v2)).doubleValue();
    }

    /**
     * 提供精确的减法运算
     *
     * @param v1 被减数
     * @param v2 减数
     * @return 差
     */
    public static double sub(Double v1, Double v2) {
        // noinspection RedundantCast
        return sub((Number) v1, (Number) v2).doubleValue();
    }

    /**
     * 提供精确的减法运算<br>
     * 如果传入多个值为null或者空，则返回0
     *
     * @param v1 被减数
     * @param v2 减数
     * @return 差
     */
    public static BigDecimal sub(Number v1, Number v2) {
        return sub(new Number[] {v1, v2});
    }

    /**
     * 提供精确的减法运算<br>
     * 如果传入多个值为null或者空，则返回0
     *
     * @param values 多个被减值
     * @return 差
     * @since 4.0.0
     */
    public static BigDecimal sub(Number... values) {
        if (GutilArray.isEmpty(values)) {
            return BigDecimal.ZERO;
        }

        Number value = values[0];
        BigDecimal result = null == value ? BigDecimal.ZERO : new BigDecimal(value.toString());
        for (int i = 1; i < values.length; i++) {
            value = values[i];
            if (null != value) {
                result = result.subtract(new BigDecimal(value.toString()));
            }
        }
        return result;
    }

    /**
     * 提供精确的减法运算<br>
     * 如果传入多个值为null或者空，则返回0
     *
     * @param values 多个被减值
     * @return 差
     * @since 4.0.0
     */
    public static BigDecimal sub(String... values) {
        if (GutilArray.isEmpty(values)) {
            return BigDecimal.ZERO;
        }

        String value = values[0];
        BigDecimal result = null == value ? BigDecimal.ZERO : new BigDecimal(value);
        for (int i = 1; i < values.length; i++) {
            value = values[i];
            if (null != value) {
                result = result.subtract(new BigDecimal(value));
            }
        }
        return result;
    }

    /**
     * 提供精确的减法运算<br>
     * 如果传入多个值为null或者空，则返回0
     *
     * @param values 多个被减值
     * @return 差
     * @since 4.0.0
     */
    public static BigDecimal sub(BigDecimal... values) {
        if (GutilArray.isEmpty(values)) {
            return BigDecimal.ZERO;
        }

        BigDecimal value = values[0];
        BigDecimal result = null == value ? BigDecimal.ZERO : value;
        for (int i = 1; i < values.length; i++) {
            value = values[i];
            if (null != value) {
                result = result.subtract(value);
            }
        }
        return result;
    }

    /**
     * 提供精确的乘法运算
     *
     * @param v1 被乘数
     * @param v2 乘数
     * @return 积
     */
    public static double mul(float v1, float v2) {
        return mul(Float.toString(v1), Float.toString(v2)).doubleValue();
    }

    /**
     * 提供精确的乘法运算
     *
     * @param v1 被乘数
     * @param v2 乘数
     * @return 积
     */
    public static double mul(float v1, double v2) {
        return mul(Float.toString(v1), Double.toString(v2)).doubleValue();
    }

    /**
     * 提供精确的乘法运算
     *
     * @param v1 被乘数
     * @param v2 乘数
     * @return 积
     */
    public static double mul(double v1, float v2) {
        return mul(Double.toString(v1), Float.toString(v2)).doubleValue();
    }

    /**
     * 提供精确的乘法运算
     *
     * @param v1 被乘数
     * @param v2 乘数
     * @return 积
     */
    public static double mul(double v1, double v2) {
        return mul(Double.toString(v1), Double.toString(v2)).doubleValue();
    }

    /**
     * 提供精确的乘法运算<br>
     * 如果传入多个值为null或者空，则返回0
     *
     * @param v1 被乘数
     * @param v2 乘数
     * @return 积
     */
    public static double mul(Double v1, Double v2) {
        // noinspection RedundantCast
        return mul((Number) v1, (Number) v2).doubleValue();
    }

    /**
     * 提供精确的乘法运算<br>
     * 如果传入多个值为null或者空，则返回0
     *
     * @param v1 被乘数
     * @param v2 乘数
     * @return 积
     */
    public static BigDecimal mul(Number v1, Number v2) {
        return mul(new Number[] {v1, v2});
    }

    /**
     * 提供精确的乘法运算<br>
     * 如果传入多个值为null或者空，则返回0
     *
     * @param values 多个被乘值
     * @return 积
     * @since 4.0.0
     */
    public static BigDecimal mul(Number... values) {
        if (GutilArray.isEmpty(values) || GutilArray.hasNull(values)) {
            return BigDecimal.ZERO;
        }

        Number value = values[0];
        BigDecimal result = new BigDecimal(value.toString());
        for (int i = 1; i < values.length; i++) {
            value = values[i];
            result = result.multiply(new BigDecimal(value.toString()));
        }
        return result;
    }

    /**
     * 提供精确的乘法运算
     *
     * @param v1 被乘数
     * @param v2 乘数
     * @return 积
     * @since 3.0.8
     */
    public static BigDecimal mul(String v1, String v2) {
        return mul(new BigDecimal(v1), new BigDecimal(v2));
    }

    /**
     * 提供精确的乘法运算<br>
     * 如果传入多个值为null或者空，则返回0
     *
     * @param values 多个被乘值
     * @return 积
     * @since 4.0.0
     */
    public static BigDecimal mul(String... values) {
        if (GutilArray.isEmpty(values) || GutilArray.hasNull(values)) {
            return BigDecimal.ZERO;
        }

        BigDecimal result = new BigDecimal(values[0]);
        for (int i = 1; i < values.length; i++) {
            result = result.multiply(new BigDecimal(values[i]));
        }

        return result;
    }

    /**
     * 提供精确的乘法运算<br>
     * 如果传入多个值为null或者空，则返回0
     *
     * @param values 多个被乘值
     * @return 积
     * @since 4.0.0
     */
    public static BigDecimal mul(BigDecimal... values) {
        if (GutilArray.isEmpty(values) || GutilArray.hasNull(values)) {
            return BigDecimal.ZERO;
        }

        BigDecimal result = values[0];
        for (int i = 1; i < values.length; i++) {
            result = result.multiply(values[i]);
        }
        return result;
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况的时候,精确到小数点后10位,后面的四舍五入
     *
     * @param v1 被除数
     * @param v2 除数
     * @return 两个参数的商
     */
    public static double div(float v1, float v2) {
        return div(v1, v2, DEFAUT_DIV_SCALE);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况的时候,精确到小数点后10位,后面的四舍五入
     *
     * @param v1 被除数
     * @param v2 除数
     * @return 两个参数的商
     */
    public static double div(float v1, double v2) {
        return div(v1, v2, DEFAUT_DIV_SCALE);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况的时候,精确到小数点后10位,后面的四舍五入
     *
     * @param v1 被除数
     * @param v2 除数
     * @return 两个参数的商
     */
    public static double div(double v1, float v2) {
        return div(v1, v2, DEFAUT_DIV_SCALE);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况的时候,精确到小数点后10位,后面的四舍五入
     *
     * @param v1 被除数
     * @param v2 除数
     * @return 两个参数的商
     */
    public static double div(double v1, double v2) {
        return div(v1, v2, DEFAUT_DIV_SCALE);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况的时候,精确到小数点后10位,后面的四舍五入<br>
     * 如果v1或v2为null则返回0
     *
     * @param v1 被除数
     * @param v2 除数
     * @return 两个参数的商
     */
    public static double div(Double v1, Double v2) {
        return div(v1, v2, DEFAUT_DIV_SCALE);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况的时候,精确到小数点后10位,后面的四舍五入<br>
     * 如果v1或v2为null则返回0
     *
     * @param v1 被除数
     * @param v2 除数
     * @return 两个参数的商
     * @since 3.1.0
     */
    public static BigDecimal div(Number v1, Number v2) {
        return div(v1, v2, DEFAUT_DIV_SCALE);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况的时候,精确到小数点后10位,后面的四舍五入
     *
     * @param v1 被除数
     * @param v2 除数
     * @return 两个参数的商
     */
    public static BigDecimal div(String v1, String v2) {
        return div(v1, v2, DEFAUT_DIV_SCALE);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度,后面的四舍五入
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @return 两个参数的商
     */
    public static double div(float v1, float v2, int scale) {
        return div(v1, v2, scale, RoundingMode.HALF_UP);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度,后面的四舍五入
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @return 两个参数的商
     */
    public static double div(float v1, double v2, int scale) {
        return div(v1, v2, scale, RoundingMode.HALF_UP);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度,后面的四舍五入
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @return 两个参数的商
     */
    public static double div(double v1, float v2, int scale) {
        return div(v1, v2, scale, RoundingMode.HALF_UP);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度,后面的四舍五入
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @return 两个参数的商
     */
    public static double div(double v1, double v2, int scale) {
        return div(v1, v2, scale, RoundingMode.HALF_UP);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度,后面的四舍五入
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @return 两个参数的商
     */
    public static double div(Double v1, Double v2, int scale) {
        return div(v1, v2, scale, RoundingMode.HALF_UP);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度,后面的四舍五入<br>
     * 如果v1或v2为null则返回0
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @return 两个参数的商
     * @since 3.1.0
     */
    public static BigDecimal div(Number v1, Number v2, int scale) {
        return div(v1, v2, scale, RoundingMode.HALF_UP);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度,后面的四舍五入
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @return 两个参数的商
     */
    public static BigDecimal div(String v1, String v2, int scale) {
        return div(v1, v2, scale, RoundingMode.HALF_UP);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @param roundingMode 保留小数的模式 {@link RoundingMode}
     * @return 两个参数的商
     */
    public static double div(float v1, float v2, int scale, RoundingMode roundingMode) {
        return div(Float.toString(v1), Float.toString(v2), scale, roundingMode).doubleValue();
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @param roundingMode 保留小数的模式 {@link RoundingMode}
     * @return 两个参数的商
     */
    public static double div(float v1, double v2, int scale, RoundingMode roundingMode) {
        return div(Float.toString(v1), Double.toString(v2), scale, roundingMode).doubleValue();
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @param roundingMode 保留小数的模式 {@link RoundingMode}
     * @return 两个参数的商
     */
    public static double div(double v1, float v2, int scale, RoundingMode roundingMode) {
        return div(Double.toString(v1), Float.toString(v2), scale, roundingMode).doubleValue();
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @param roundingMode 保留小数的模式 {@link RoundingMode}
     * @return 两个参数的商
     */
    public static double div(double v1, double v2, int scale, RoundingMode roundingMode) {
        return div(Double.toString(v1), Double.toString(v2), scale, roundingMode).doubleValue();
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度<br>
     * 如果v1或v2为null则返回0
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @param roundingMode 保留小数的模式 {@link RoundingMode}
     * @return 两个参数的商
     */
    public static double div(Double v1, Double v2, int scale, RoundingMode roundingMode) {
        // noinspection RedundantCast
        return div((Number) v1, (Number) v2, scale, roundingMode).doubleValue();
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度<br>
     * 如果v1或v2为null则返回0
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @param roundingMode 保留小数的模式 {@link RoundingMode}
     * @return 两个参数的商
     * @since 3.1.0
     */
    public static BigDecimal div(Number v1, Number v2, int scale, RoundingMode roundingMode) {
        if (null == v1 || null == v2) {
            return BigDecimal.ZERO;
        }
        return div(v1.toString(), v2.toString(), scale, roundingMode);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @param roundingMode 保留小数的模式 {@link RoundingMode}
     * @return 两个参数的商
     */
    public static BigDecimal div(String v1, String v2, int scale, RoundingMode roundingMode) {
        return div(new BigDecimal(v1), new BigDecimal(v2), scale, roundingMode);
    }

    /**
     * 提供(相对)精确的除法运算,当发生除不尽的情况时,由scale指定精确度<br>
     * 如果v1或v2为null则返回0
     *
     * @param v1 被除数
     * @param v2 除数
     * @param scale 精确度，如果为负值，取绝对值
     * @param roundingMode 保留小数的模式 {@link RoundingMode}
     * @return 两个参数的商
     * @since 3.0.9
     */
    public static BigDecimal div(
            BigDecimal v1, BigDecimal v2, int scale, RoundingMode roundingMode) {
        if (null == v1 || null == v2) {
            return BigDecimal.ZERO;
        }
        if (scale < 0) {
            scale = -scale;
        }
        return v1.divide(v2, scale, roundingMode);
    }

    /**
     * 补充Math.ceilDiv() JDK8中添加了和Math.floorDiv()但却没有ceilDiv()<br>
     * 使用纯整数运算实现数学上的上取整（ceil），避免double精度问题<br>
     * 例如：ceilDiv(7, 3) = 3，ceilDiv(-7, 3) = -2，ceilDiv(7, -3) = -2
     *
     * @param v1 被除数
     * @param v2 除数
     * @return 两个参数的商向上取整
     * @throws ArithmeticException 除数为0时抛出
     * @since 5.3.3
     */
    public static int ceilDiv(int v1, int v2) {
        if (v2 == 0) {
            throw new ArithmeticException("Division by zero!");
        }
        int result = v1 / v2;
        // 余数不为0且同号时，向零截断的结果比数学上的上取整小1
        if ((v1 % v2) != 0 && ((v1 ^ v2) >= 0)) {
            result++;
        }
        return result;
    }

    // -------------------------------------------------------------------------------------------
    // round 四舍五入

    /**
     * 保留固定位数小数<br>
     * 采用四舍五入策略 {@link RoundingMode#HALF_UP}<br>
     * 例如保留2位小数：123.456789 =》 123.46
     *
     * @param v 值
     * @param scale 保留小数位数
     * @return 新值
     */
    public static BigDecimal round(double v, int scale) {
        return round(v, scale, RoundingMode.HALF_UP);
    }

    /**
     * 保留固定位数小数<br>
     * 采用四舍五入策略 {@link RoundingMode#HALF_UP}<br>
     * 例如保留2位小数：123.456789 =》 123.46
     *
     * @param v 值
     * @param scale 保留小数位数
     * @return 新值
     */
    public static String roundStr(double v, int scale) {
        return round(v, scale).toString();
    }

    /**
     * 保留固定位数小数<br>
     * 采用四舍五入策略 {@link RoundingMode#HALF_UP}<br>
     * 例如保留2位小数：123.456789 =》 123.46
     *
     * @param numberStr 数字值的字符串表现形式
     * @param scale 保留小数位数
     * @return 新值
     */
    public static BigDecimal round(String numberStr, int scale) {
        return round(numberStr, scale, RoundingMode.HALF_UP);
    }

    /**
     * 保留固定位数小数<br>
     * 采用四舍五入策略 {@link RoundingMode#HALF_UP}<br>
     * 例如保留2位小数：123.456789 =》 123.46
     *
     * @param number 数字值
     * @param scale 保留小数位数
     * @return 新值
     * @since 4.1.0
     */
    public static BigDecimal round(BigDecimal number, int scale) {
        return round(number, scale, RoundingMode.HALF_UP);
    }

    /**
     * 保留固定位数小数<br>
     * 采用四舍五入策略 {@link RoundingMode#HALF_UP}<br>
     * 例如保留2位小数：123.456789 =》 123.46
     *
     * @param numberStr 数字值的字符串表现形式
     * @param scale 保留小数位数
     * @return 新值
     * @since 3.2.2
     */
    public static String roundStr(String numberStr, int scale) {
        return round(numberStr, scale).toString();
    }

    /**
     * 保留固定位数小数<br>
     * 例如保留四位小数：123.456789 =》 123.4567
     *
     * @param v 值
     * @param scale 保留小数位数
     * @param roundingMode 保留小数的模式 {@link RoundingMode}
     * @return 新值
     */
    public static BigDecimal round(double v, int scale, RoundingMode roundingMode) {
        return round(Double.toString(v), scale, roundingMode);
    }

    /**
     * 保留固定位数小数<br>
     * 例如保留四位小数：123.456789 =》 123.4567
     *
     * @param v 值
     * @param scale 保留小数位数
     * @param roundingMode 保留小数的模式 {@link RoundingMode}
     * @return 新值
     * @since 3.2.2
     */
    public static String roundStr(double v, int scale, RoundingMode roundingMode) {
        return round(v, scale, roundingMode).toString();
    }

    /**
     * 保留固定位数小数<br>
     * 例如保留四位小数：123.456789 =》 123.4567
     *
     * @param numberStr 数字值的字符串表现形式
     * @param scale 保留小数位数，如果传入小于0，则默认0
     * @param roundingMode 保留小数的模式 {@link RoundingMode}，如果传入null则默认四舍五入
     * @return 新值
     */
    public static BigDecimal round(String numberStr, int scale, RoundingMode roundingMode) {
        GutilAssert.hasLength(numberStr, "numberStr is notEmpty");
        if (scale < 0) {
            scale = 0;
        }
        return round(toBigDecimal(numberStr), scale, roundingMode);
    }

    /**
     * 保留固定位数小数<br>
     * 例如保留四位小数：123.456789 =》 123.4567
     *
     * @param number 数字值
     * @param scale 保留小数位数，如果传入小于0，则默认0
     * @param roundingMode 保留小数的模式 {@link RoundingMode}，如果传入null则默认四舍五入
     * @return 新值
     */
    public static BigDecimal round(BigDecimal number, int scale, RoundingMode roundingMode) {
        if (null == number) {
            number = BigDecimal.ZERO;
        }
        if (scale < 0) {
            scale = 0;
        }
        if (null == roundingMode) {
            roundingMode = RoundingMode.HALF_UP;
        }

        return number.setScale(scale, roundingMode);
    }

    /**
     * 保留固定位数小数<br>
     * 例如保留四位小数：123.456789 =》 123.4567
     *
     * @param numberStr 数字值的字符串表现形式
     * @param scale 保留小数位数
     * @param roundingMode 保留小数的模式 {@link RoundingMode}
     * @return 新值
     * @since 3.2.2
     */
    public static String roundStr(String numberStr, int scale, RoundingMode roundingMode) {
        return round(numberStr, scale, roundingMode).toString();
    }

    /**
     * 四舍六入五成双计算法
     *
     * <p>四舍六入五成双是一种比较精确比较科学的计数保留法，是一种数字修约规则。
     *
     * <pre>
     * 算法规则:
     * 四舍六入五考虑，
     * 五后非零就进一，
     * 五后皆零看奇偶，
     * 五前为偶应舍去，
     * 五前为奇要进一。
     * </pre>
     *
     * @param number 需要科学计算的数据
     * @param scale 保留的小数位
     * @return 结果
     * @since 4.1.0
     */
    public static BigDecimal roundHalfEven(Number number, int scale) {
        return roundHalfEven(toBigDecimal(number), scale);
    }

    /**
     * 四舍六入五成双计算法
     *
     * <p>四舍六入五成双是一种比较精确比较科学的计数保留法，是一种数字修约规则。
     *
     * <pre>
     * 算法规则:
     * 四舍六入五考虑，
     * 五后非零就进一，
     * 五后皆零看奇偶，
     * 五前为偶应舍去，
     * 五前为奇要进一。
     * </pre>
     *
     * @param value 需要科学计算的数据
     * @param scale 保留的小数位
     * @return 结果
     * @since 4.1.0
     */
    public static BigDecimal roundHalfEven(BigDecimal value, int scale) {
        return round(value, scale, RoundingMode.HALF_EVEN);
    }

    /**
     * 保留固定小数位数，舍去多余位数
     *
     * @param number 需要科学计算的数据
     * @param scale 保留的小数位
     * @return 结果
     * @since 4.1.0
     */
    public static BigDecimal roundDown(Number number, int scale) {
        return roundDown(toBigDecimal(number), scale);
    }

    /**
     * 保留固定小数位数，舍去多余位数
     *
     * @param value 需要科学计算的数据
     * @param scale 保留的小数位
     * @return 结果
     * @since 4.1.0
     */
    public static BigDecimal roundDown(BigDecimal value, int scale) {
        return round(value, scale, RoundingMode.DOWN);
    }

    // -------------------------------------------------------------------------------------------
    // decimalFormat 格式化

    /**
     * 格式化double<br>
     * 对 {@link DecimalFormat} 做封装<br>
     * 使用{@link Locale#ROOT}区域设置，不受默认区域设置影响，例如小数点符号固定为"."<br>
     *
     * @param pattern 格式 格式中主要以 # 和 0 两种占位符号来指定数字长度。0 表示如果位数不足则以 0 填充，# 表示只要有可能就把数字拉上这个位置。<br>
     *     <ul>
     *       <li>0 =》 取一位整数
     *       <li>0.00 =》 取一位整数和两位小数
     *       <li>00.000 =》 取两位整数和三位小数
     *       <li># =》 取所有整数部分
     *       <li>#.##% =》 以百分比方式计数，并取两位小数
     *       <li>#.#####E0 =》 显示为科学计数法，并取五位小数
     *       <li>,### =》 每三位以逗号进行分隔，例如：299,792,458
     *       <li>光速大小为每秒,###米 =》 将格式嵌入文本
     *     </ul>
     *
     * @param value 值
     * @return 格式化后的值
     * @see Locale#ROOT
     */
    public static String decimalFormat(String pattern, double value) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.ROOT))
                .format(value);
    }

    /**
     * 格式化double<br>
     * 对 {@link DecimalFormat} 做封装<br>
     * 使用{@link Locale#ROOT}区域设置，不受默认区域设置影响，例如小数点符号固定为"."<br>
     *
     * @param pattern 格式 格式中主要以 # 和 0 两种占位符号来指定数字长度。0 表示如果位数不足则以 0 填充，# 表示只要有可能就把数字拉上这个位置。<br>
     *     <ul>
     *       <li>0 =》 取一位整数
     *       <li>0.00 =》 取一位整数和两位小数
     *       <li>00.000 =》 取两位整数和三位小数
     *       <li># =》 取所有整数部分
     *       <li>#.##% =》 以百分比方式计数，并取两位小数
     *       <li>#.#####E0 =》 显示为科学计数法，并取五位小数
     *       <li>,### =》 每三位以逗号进行分隔，例如：299,792,458
     *       <li>光速大小为每秒,###米 =》 将格式嵌入文本
     *     </ul>
     *
     * @param value 值
     * @return 格式化后的值
     * @since 3.0.5
     */
    public static String decimalFormat(String pattern, long value) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.ROOT))
                .format(value);
    }

    /**
     * 格式化double<br>
     * 对 {@link DecimalFormat} 做封装<br>
     * 使用{@link Locale#ROOT}区域设置，不受默认区域设置影响，例如小数点符号固定为"."<br>
     *
     * @param pattern 格式 格式中主要以 # 和 0 两种占位符号来指定数字长度。0 表示如果位数不足则以 0 填充，# 表示只要有可能就把数字拉上这个位置。<br>
     *     <ul>
     *       <li>0 =》 取一位整数
     *       <li>0.00 =》 取一位整数和两位小数
     *       <li>00.000 =》 取两位整数和三位小数
     *       <li># =》 取所有整数部分
     *       <li>#.##% =》 以百分比方式计数，并取两位小数
     *       <li>#.#####E0 =》 显示为科学计数法，并取五位小数
     *       <li>,### =》 每三位以逗号进行分隔，例如：299,792,458
     *       <li>光速大小为每秒,###米 =》 将格式嵌入文本
     *     </ul>
     *
     * @param value 值，支持BigDecimal、BigInteger、Number等类型
     * @return 格式化后的值
     * @since 5.1.6
     */
    public static String decimalFormat(String pattern, Object value) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.ROOT))
                .format(value);
    }

    /**
     * 格式化金额输出，每三位用逗号分隔
     *
     * @param value 金额
     * @return 格式化后的值
     * @since 3.0.9
     */
    public static String decimalFormatMoney(double value) {
        return decimalFormat(",##0.00", value);
    }

    /**
     * 格式化百分比，小数采用四舍五入方式
     *
     * @param number 值
     * @param scale 保留小数位数
     * @return 百分比
     * @since 3.2.3
     */
    public static String formatPercent(double number, int scale) {
        final NumberFormat format = NumberFormat.getPercentInstance();
        format.setMaximumFractionDigits(scale);
        return format.format(number);
    }

    // -------------------------------------------------------------------------------------------
    // isXXX 判断

    /**
     * 是否为数字，支持包括：
     *
     * <pre>
     * 1、10进制
     * 2、16进制数字（0x开头）
     * 3、科学计数法形式（1234E3）
     * 4、类型标识形式（123D）
     * 5、正负数标识形式（+123、-234）
     * </pre>
     *
     * @param str 字符串值
     * @return 是否为数字
     */
    public static boolean isNumber(CharSequence str) {
        if (GutilStr.isBlank(str)) {
            return false;
        }
        char[] chars = str.toString().toCharArray();
        int sz = chars.length;
        boolean hasExp = false;
        boolean hasDecPoint = false;
        boolean allowSigns = false;
        boolean foundDigit = false;
        // 预先处理可能出现的正负号
        int start = (chars[0] == '-' || chars[0] == '+') ? 1 : 0;
        if (sz > start + 1) {
            if (chars[start] == '0' && (chars[start + 1] == 'x' || chars[start + 1] == 'X')) {
                int i = start + 2;
                if (i == sz) {
                    return false; // str == "0x"
                }
                // 检查十六进制（不可能是其它格式）
                for (; i < chars.length; i++) {
                    if ((chars[i] < '0' || chars[i] > '9')
                            && (chars[i] < 'a' || chars[i] > 'f')
                            && (chars[i] < 'A' || chars[i] > 'F')) {
                        return false;
                    }
                }
                return true;
            }
        }
        sz--; // 不想循环到最后一个字符，稍后再检查
        // 用于类型标识
        int i = start;
        // 循环到倒数第二个字符，或者当需要再一个数字才能构成合法数字时循环到最后一个字符
        // （例如 chars[0..5] = "1234E"）
        while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                foundDigit = true;
                allowSigns = false;

            } else if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // 两个小数点或指数中带小数点
                    return false;
                }
                hasDecPoint = true;
            } else if (chars[i] == 'e' || chars[i] == 'E') {
                // 十六进制的情况已处理过
                if (hasExp) {
                    // 两个 E
                    return false;
                }
                if (false == foundDigit) {
                    return false;
                }
                hasExp = true;
                allowSigns = true;
            } else if (chars[i] == '+' || chars[i] == '-') {
                if (!allowSigns) {
                    return false;
                }
                allowSigns = false;
                foundDigit = false; // E 后面需要一个数字
            } else {
                return false;
            }
            i++;
        }
        if (i < chars.length) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                // 无类型标识，合法
                return true;
            }
            if (chars[i] == 'e' || chars[i] == 'E') {
                // 最后一个字节不能是 E
                return false;
            }
            if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    // 两个小数点或指数中带小数点
                    return false;
                }
                // 非指数形式末尾的单个小数点合法
                return foundDigit;
            }
            if (!allowSigns
                    && (chars[i] == 'd' || chars[i] == 'D' || chars[i] == 'f' || chars[i] == 'F')) {
                return foundDigit;
            }
            if (chars[i] == 'l' || chars[i] == 'L') {
                // 不允许带指数的 L
                return foundDigit && !hasExp;
            }
            // 最后一个字符非法
            return false;
        }
        // allowSigns 为 true 当且仅当以 'E' 结尾
        // foundDigit 用于排除 '.' 和 '1E-' 之类的奇怪情况
        return false == allowSigns && foundDigit;
    }

    /**
     * 判断String是否是整数<br>
     * 支持10进制
     *
     * @param s String
     * @return 是否为整数
     */
    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * 判断字符串是否是Long类型<br>
     * 支持10进制
     *
     * @param s String
     * @return 是否为{@link Long}类型
     * @since 4.0.0
     */
    public static boolean isLong(String s) {
        try {
            Long.parseLong(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * 判断字符串是否可解析为浮点数<br>
     * 支持科学计数法（如1e5、1E-3），整数形式（如123）同样返回true
     *
     * @param s String
     * @return 是否可解析为{@link Double}类型
     */
    public static boolean isDouble(String s) {
        if (s == null) {
            return false;
        }
        try {
            Double.parseDouble(s);
        } catch (NumberFormatException ignore) {
            return false;
        }
        return true;
    }

    /**
     * 是否是质数（素数）<br>
     * 质数表的质数又称素数。指整数在一个大于1的自然数中,除了1和此整数自身外,没法被其他自然数整除的数。<br>
     * n小于等于1（包括0、1和负数）时返回false
     *
     * @param n 数字
     * @return 是否是质数
     */
    public static boolean isPrimes(int n) {
        if (n <= 1) {
            return false;
        }
        int sqrt = (int) Math.sqrt(n);
        for (int i = 2; i <= sqrt; i++) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------------------------
    // generateXXX 生成

    /**
     * 生成不重复随机数 根据给定的最小数字和最大数字，以及随机数的个数，产生指定的不重复的数组<br>
     * 使用拒绝采样直接生成，不构造begin到end的中间数组，避免大区间时内存溢出（OOM）
     *
     * @param begin 最小数字（包含该数）
     * @param end 最大数字（不包含该数）
     * @param size 指定产生随机数的个数
     * @return 随机int数组
     * @throws IllegalArgumentException size大于区间范围或区间过大时抛出
     */
    public static int[] generateRandomNumber(int begin, int end, int size) {
        if (begin > end) {
            int temp = begin;
            begin = end;
            end = temp;
        }
        GutilAssert.isTrue(size >= 0, "Size must be >= 0!");
        long range = (long) end - begin;
        if (range < size) {
            throw new IllegalArgumentException("Size is larger than range between begin and end!");
        }
        if (range > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Range between begin and end is too large: " + range);
        }
        if (size == 0) {
            return new int[0];
        }

        final Random random = ThreadLocalRandom.current();
        final Set<Integer> set = new HashSet<>(size);
        while (set.size() < size) {
            set.add(begin + random.nextInt((int) range));
        }

        final int[] ranArr = new int[size];
        int index = 0;
        for (Integer value : set) {
            ranArr[index++] = value;
        }
        return ranArr;
    }

    /**
     * 生成不重复随机数 根据给定的最小数字和最大数字，以及随机数的个数，产生指定的不重复的数组<br>
     * 方法内部会拷贝传入的seed数组，不会修改调用方传入的数组
     *
     * @param begin 最小数字（包含该数）
     * @param end 最大数字（不包含该数）
     * @param size 指定产生随机数的个数
     * @param seed 种子，用于取随机数的int池
     * @return 随机int数组
     * @throws IllegalArgumentException 参数不合法（size大于区间或seed长度等）时抛出
     * @since 5.4.5
     */
    public static int[] generateRandomNumber(int begin, int end, int size, int[] seed) {
        if (begin > end) {
            int temp = begin;
            begin = end;
            end = temp;
        }
        // 加入逻辑判断，确保begin<end并且size不能大于该表示范围
        GutilAssert.notNull(seed, "Seed must not be null!");
        GutilAssert.isTrue(size >= 0, "Size must be >= 0!");
        GutilAssert.isTrue(
                ((long) end - begin) > size, "Size is larger than range between begin and end!");
        GutilAssert.isTrue(seed.length > size, "Size is larger than seed size!");

        // 拷贝种子数组，避免改写调用方传入的数组
        final int[] ranSeed = seed.clone();
        final int[] ranArr = new int[size];
        // 数量你可以自己定义。
        for (int i = 0; i < size; i++) {
            // 得到一个位置
            int j = ThreadLocalRandom.current().nextInt(ranSeed.length - i);
            // 得到那个位置的数值
            ranArr[i] = ranSeed[j];
            // 将最后一个未用的数字放到这里
            ranSeed[j] = ranSeed[ranSeed.length - 1 - i];
        }
        return ranArr;
    }

    /**
     * 生成不重复随机数 根据给定的最小数字和最大数字，以及随机数的个数，产生指定的不重复的数组
     *
     * @param begin 最小数字（包含该数）
     * @param end 最大数字（不包含该数）
     * @param size 指定产生随机数的个数
     * @return 随机int数组
     * @throws IllegalArgumentException size小于0、size大于区间范围或区间过大时抛出
     */
    public static Integer[] generateBySet(int begin, int end, int size) {
        if (begin > end) {
            int temp = begin;
            begin = end;
            end = temp;
        }
        // 加入逻辑判断，确保begin<end并且size不能大于该表示范围
        if (size < 0) {
            throw new IllegalArgumentException("Size must be >= 0!");
        }
        long range = (long) end - begin;
        if (range < size) {
            throw new IllegalArgumentException("Size is larger than range between begin and end!");
        }
        if (range > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Range between begin and end is too large: " + range);
        }

        Random ran = new Random();
        Set<Integer> set = new HashSet<>();
        while (set.size() < size) {
            set.add(begin + ran.nextInt((int) range));
        }

        return set.toArray(new Integer[size]);
    }

    // -------------------------------------------------------------------------------------------
    // range 范围

    /**
     * 从0开始给定范围内的整数列表，步进为1
     *
     * @param stop 结束（包含）
     * @return 整数列表
     * @since 3.3.1
     */
    public static int[] range(int stop) {
        return range(0, stop);
    }

    /**
     * 给定范围内的整数列表，步进为1
     *
     * @param start 开始（包含）
     * @param stop 结束（包含）
     * @return 整数列表
     */
    public static int[] range(int start, int stop) {
        return range(start, stop, 1);
    }

    /**
     * 给定范围内的整数列表<br>
     * 步进的符号自动适配方向：start &lt; stop时按正步进递增，start &gt; stop时按负步进递减，因此负数步进同样支持（取其绝对值）
     *
     * @param start 开始（包含）
     * @param stop 结束（包含）
     * @param step 步进，不能为0
     * @return 整数列表
     * @throws IllegalArgumentException 步进为0时抛出
     */
    public static int[] range(int start, int stop, int step) {
        if (step == 0) {
            throw new IllegalArgumentException("Step must not be zero!");
        }
        if (start < stop) {
            step = Math.abs(step);
        } else if (start > stop) {
            step = -Math.abs(step);
        } else { // start == end 起点等于终点
            return new int[] {start};
        }

        // 使用long计算差值，避免stop - start溢出
        long size = Math.abs(((long) stop - start) / step) + 1;
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Range between start and stop is too large: " + size);
        }
        int[] values = new int[(int) size];
        int index = 0;
        // 使用long循环变量，避免i += step时int溢出
        for (long i = start; (step > 0) ? i <= stop : i >= stop; i += step) {
            values[index] = (int) i;
            index++;
        }
        return values;
    }

    /**
     * 将给定范围内的整数添加到已有集合中，步进为1
     *
     * @param start 开始（包含）
     * @param stop 结束（包含）
     * @param values 集合
     * @return 集合
     */
    public static Collection<Integer> appendRange(int start, int stop, Collection<Integer> values) {
        return appendRange(start, stop, 1, values);
    }

    /**
     * 将给定范围内的整数添加到已有集合中<br>
     * 步进的符号自动适配方向：start &lt; stop时按正步进递增，start &gt; stop时按负步进递减，因此负数步进同样支持（取其绝对值）
     *
     * @param start 开始（包含）
     * @param stop 结束（包含）
     * @param step 步进，不能为0
     * @param values 集合
     * @return 集合
     * @throws IllegalArgumentException 步进为0或集合为null时抛出
     */
    public static Collection<Integer> appendRange(
            int start, int stop, int step, Collection<Integer> values) {
        GutilAssert.notNull(values, "Collection must not be null!");
        if (step == 0) {
            throw new IllegalArgumentException("Step must not be zero!");
        }
        if (start < stop) {
            step = Math.abs(step);
        } else if (start > stop) {
            step = -Math.abs(step);
        } else { // start == end 起点等于终点
            values.add(start);
            return values;
        }

        for (int i = start; (step > 0) ? i <= stop : i >= stop; i += step) {
            values.add(i);
        }
        return values;
    }

    // -------------------------------------------------------------------------------------------
    // others 其它

    /**
     * 计算范围阶乘
     *
     * <p>factorial(start, end) = start * (start - 1) * ... * (end - 1)
     *
     * @param start 阶乘起始（包含）
     * @param end 阶乘结束，必须小于起始（不包括）
     * @return 结果
     * @since 4.1.0
     */
    public static long factorial(long start, long end) {
        // 负数没有阶乘
        if (start < 0 || end < 0) {
            throw new IllegalArgumentException(
                    GutilStr.format(
                            "Factorial start and end both must be >= 0, but got start={}, end={}",
                            start,
                            end));
        }
        if (0L == start || start == end) {
            return 1L;
        }
        if (start < end) {
            return 0L;
        }
        return factorialMultiplyAndCheck(start, factorial(start - 1, end));
    }

    /**
     * 计算范围阶乘中校验中间的计算是否存在溢出，factorial提前做了负数和0的校验，因此这里没有校验数字的正负
     *
     * @param a 乘数
     * @param b 被乘数
     * @return 如果 a * b的结果没有溢出直接返回，否则抛出异常
     */
    private static long factorialMultiplyAndCheck(long a, long b) {
        if (a <= Long.MAX_VALUE / b) {
            return a * b;
        }
        throw new IllegalArgumentException(
                GutilStr.format("Overflow in multiplication: {} * {}", a, b));
    }

    /**
     * 计算阶乘
     *
     * <p>n! = n * (n-1) * ... * 2 * 1
     *
     * @param n 阶乘起始
     * @return 结果
     */
    public static long factorial(long n) {
        if (n < 0 || n > 20) {
            throw new IllegalArgumentException(
                    GutilStr.format(
                            "Factorial must have n >= 0 and n <= 20 for n!, but got n = {}", n));
        }
        return FACTORIALS[(int) n];
    }

    /**
     * 平方根算法<br>
     * 推荐使用 {@link Math#sqrt(double)}
     *
     * @param x 值，必须大于等于0
     * @return 平方根
     * @throws IllegalArgumentException x为负数时抛出
     */
    public static long sqrt(long x) {
        if (x < 0) {
            throw new IllegalArgumentException("Value must be >= 0, but got " + x);
        }
        long y = 0;
        long b = (~Long.MAX_VALUE) >>> 1;
        while (b > 0) {
            if (x >= y + b) {
                x -= y + b;
                y >>= 1;
                y += b;
            } else {
                y >>= 1;
            }
            b >>= 2;
        }
        return y;
    }

    /**
     * 可以用于计算双色球、大乐透注数的方法<br>
     * 比如大乐透35选5可以这样调用processMultiple(7,5); 就是数学中的：C75=7*6/2*1<br>
     * 即计算组合数C(selectNum, minNum)，中间过程使用long计算避免int乘法溢出
     *
     * @param selectNum 选中小球个数
     * @param minNum 最少要选中多少个小球
     * @return 注数，当minNum大于selectNum时返回0
     * @throws IllegalArgumentException selectNum或minNum为负数时抛出
     * @throws ArithmeticException 计算结果超出int范围时抛出
     */
    public static int processMultiple(int selectNum, int minNum) {
        GutilAssert.isTrue(selectNum >= 0, "selectNum must be >= 0!");
        GutilAssert.isTrue(minNum >= 0, "minNum must be >= 0!");
        if (minNum > selectNum) {
            return 0;
        }
        // C(selectNum, minNum) = C(selectNum, selectNum - minNum)，取较小一侧计算
        int k = Math.min(minNum, selectNum - minNum);
        long result = 1;
        // 使用连乘除的方式逐步计算，每一步中间结果均为整数，避免分子溢出
        for (int i = 1; i <= k; i++) {
            result = result * (selectNum - k + i) / i;
        }
        if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
            throw new ArithmeticException(
                    "Overflow in processMultiple(" + selectNum + ", " + minNum + ")");
        }
        return (int) result;
    }

    /**
     * 最大公约数<br>
     * 返回非负的最大公约数，负数参数取绝对值计算
     *
     * @param m 第一个值
     * @param n 第二个值
     * @return 最大公约数（非负）
     */
    public static int divisor(int m, int n) {
        m = Math.abs(m);
        n = Math.abs(n);
        while (m % n != 0) {
            int temp = m % n;
            m = n;
            n = temp;
        }
        return n;
    }

    /**
     * 最小公倍数<br>
     * 使用long做中间计算，避免m * n先乘后除的int溢出
     *
     * @param m 第一个值
     * @param n 第二个值
     * @return 最小公倍数
     * @throws ArithmeticException 计算结果超出int范围时抛出
     */
    public static int multiple(int m, int n) {
        long result = Math.multiplyExact((long) m, (long) n) / divisor(m, n);
        if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE) {
            throw new ArithmeticException("Overflow in multiple(" + m + ", " + n + ")");
        }
        return (int) result;
    }

    /**
     * 获得数字对应的二进制字符串
     *
     * @param number 数字
     * @return 二进制字符串
     */
    public static String getBinaryStr(Number number) {
        if (number instanceof Long) {
            return Long.toBinaryString((Long) number);
        } else if (number instanceof Integer) {
            return Integer.toBinaryString((Integer) number);
        } else {
            return Long.toBinaryString(number.longValue());
        }
    }

    /**
     * 二进制转int
     *
     * @param binaryStr 二进制字符串
     * @return int
     */
    public static int binaryToInt(String binaryStr) {
        return Integer.parseInt(binaryStr, 2);
    }

    /**
     * 二进制转long
     *
     * @param binaryStr 二进制字符串
     * @return long
     */
    public static long binaryToLong(String binaryStr) {
        return Long.parseLong(binaryStr, 2);
    }

    // -------------------------------------------------------------------------------------------
    // compare 比较

    /**
     * 比较两个值的大小<br>
     * 返回两个字符的差值（x - y），与{@link Character#compare(char, char)}一致：<br>
     * x==y返回0，x&lt;y返回负数，x&gt;y返回正数
     *
     * @param x 第一个值
     * @param y 第二个值
     * @return x - y的差值
     * @see Character#compare(char, char)
     * @since 3.0.1
     */
    public static int compare(char x, char y) {
        return x - y;
    }

    /**
     * 比较两个值的大小
     *
     * @param x 第一个值
     * @param y 第二个值
     * @return x==y返回0，x&lt;y返回-1，x&gt;y返回1
     * @see Double#compare(double, double)
     * @since 3.0.1
     */
    public static int compare(double x, double y) {
        return Double.compare(x, y);
    }

    /**
     * 比较两个值的大小
     *
     * @param x 第一个值
     * @param y 第二个值
     * @return x==y返回0，x&lt;y返回-1，x&gt;y返回1
     * @see Integer#compare(int, int)
     * @since 3.0.1
     */
    public static int compare(int x, int y) {
        return Integer.compare(x, y);
    }

    /**
     * 比较两个值的大小
     *
     * @param x 第一个值
     * @param y 第二个值
     * @return x==y返回0，x&lt;y返回-1，x&gt;y返回1
     * @see Long#compare(long, long)
     * @since 3.0.1
     */
    public static int compare(long x, long y) {
        return Long.compare(x, y);
    }

    /**
     * 比较两个值的大小
     *
     * @param x 第一个值
     * @param y 第二个值
     * @return x==y返回0，x&lt;y返回-1，x&gt;y返回1
     * @see Short#compare(short, short)
     * @since 3.0.1
     */
    public static int compare(short x, short y) {
        return Short.compare(x, y);
    }

    /**
     * 比较两个值的大小
     *
     * @param x 第一个值
     * @param y 第二个值
     * @return x==y返回0，x&lt;y返回-1，x&gt;y返回1
     * @see Byte#compare(byte, byte)
     * @since 3.0.1
     */
    public static int compare(byte x, byte y) {
        return Byte.compare(x, y);
    }

    /**
     * 比较大小，参数1 &gt; 参数2 返回true
     *
     * @param bigNum1 数字1
     * @param bigNum2 数字2
     * @return 是否大于
     * @since 3, 0.9
     */
    public static boolean isGreater(BigDecimal bigNum1, BigDecimal bigNum2) {
        GutilAssert.notNull(bigNum1, "bigNum1 is not Null");
        GutilAssert.notNull(bigNum2, "bigNum2 is not Null");
        return bigNum1.compareTo(bigNum2) > 0;
    }

    /**
     * 比较大小，参数1 &gt;= 参数2 返回true
     *
     * @param bigNum1 数字1
     * @param bigNum2 数字2
     * @return 是否大于等于
     * @since 3, 0.9
     */
    public static boolean isGreaterOrEqual(BigDecimal bigNum1, BigDecimal bigNum2) {
        GutilAssert.notNull(bigNum1, "bigNum1 is not Null");
        GutilAssert.notNull(bigNum2, "bigNum2 is not Null");
        return bigNum1.compareTo(bigNum2) >= 0;
    }

    /**
     * 比较大小，参数1 &lt; 参数2 返回true
     *
     * @param bigNum1 数字1
     * @param bigNum2 数字2
     * @return 是否小于
     * @since 3, 0.9
     */
    public static boolean isLess(BigDecimal bigNum1, BigDecimal bigNum2) {
        GutilAssert.notNull(bigNum1, "bigNum1 is not Null");
        GutilAssert.notNull(bigNum2, "bigNum2 is not Null");
        return bigNum1.compareTo(bigNum2) < 0;
    }

    /**
     * 比较大小，参数1&lt;=参数2 返回true
     *
     * @param bigNum1 数字1
     * @param bigNum2 数字2
     * @return 是否小于等于
     * @since 3, 0.9
     */
    public static boolean isLessOrEqual(BigDecimal bigNum1, BigDecimal bigNum2) {
        GutilAssert.notNull(bigNum1, "bigNum1 is not Null");
        GutilAssert.notNull(bigNum2, "bigNum2 is not Null");
        return bigNum1.compareTo(bigNum2) <= 0;
    }

    /**
     * 比较大小，值相等 返回true<br>
     * 使用{@link Double#compare(double, double)}的语义进行精确比较：<br>
     * NaN与NaN视为相等；0.0与-0.0视为不相等（与==运算符不同，==认为它们相等）<br>
     * 注意：此为精确比较，不忽略精度，例如0.1+0.2的结果不等于0.3
     *
     * @param num1 数字1
     * @param num2 数字2
     * @return 是否相等
     * @since 5.4.2
     */
    public static boolean equals(double num1, double num2) {
        return Double.compare(num1, num2) == 0;
    }

    /**
     * 比较大小，值相等 返回true<br>
     * 使用{@link Float#compare(float, float)}的语义进行精确比较：<br>
     * NaN与NaN视为相等；0.0f与-0.0f视为不相等（与==运算符不同，==认为它们相等）<br>
     * 注意：此为精确比较，不忽略精度，例如0.1f+0.2f的结果不等于0.3f
     *
     * @param num1 数字1
     * @param num2 数字2
     * @return 是否相等
     * @since 5.4.5
     */
    public static boolean equals(float num1, float num2) {
        return Float.compare(num1, num2) == 0;
    }

    /**
     * 比较大小，值相等 返回true<br>
     * 此方法通过调用{@link BigDecimal#compareTo(BigDecimal)}方法来判断是否相等<br>
     * 此方法判断值相等时忽略精度的，即0.00 == 0
     *
     * @param bigNum1 数字1
     * @param bigNum2 数字2
     * @return 是否相等
     */
    public static boolean equals(BigDecimal bigNum1, BigDecimal bigNum2) {
        // noinspection NumberEquality
        if (bigNum1 == bigNum2) {
            // 如果用户传入同一对象，省略compareTo以提高性能。
            return true;
        }
        if (bigNum1 == null || bigNum2 == null) {
            return false;
        }
        return 0 == bigNum1.compareTo(bigNum2);
    }

    /**
     * 比较两个字符是否相同
     *
     * @param c1 字符1
     * @param c2 字符2
     * @param ignoreCase 是否忽略大小写
     * @return 是否相同
     * @see GutilChar#equals(char, char, boolean)
     * @since 3.2.1
     */
    public static boolean equals(char c1, char c2, boolean ignoreCase) {
        return GutilChar.equals(c1, c2, ignoreCase);
    }

    /**
     * 取最小值
     *
     * @param <T> 元素类型
     * @param numberArray 数字数组
     * @return 最小值
     * @see GutilArray#min(Comparable[])
     * @since 4.0.7
     */
    public static <T extends Comparable<? super T>> T min(T[] numberArray) {
        return GutilArray.min(numberArray);
    }

    /**
     * 取最小值
     *
     * @param numberArray 数字数组
     * @return 最小值
     * @see GutilArray#min(long...)
     * @since 4.0.7
     */
    public static long min(long... numberArray) {
        return GutilArray.min(numberArray);
    }

    /**
     * 取最小值
     *
     * @param numberArray 数字数组
     * @return 最小值
     * @see GutilArray#min(int...)
     * @since 4.0.7
     */
    public static int min(int... numberArray) {
        return GutilArray.min(numberArray);
    }

    /**
     * 取最小值
     *
     * @param numberArray 数字数组
     * @return 最小值
     * @see GutilArray#min(short...)
     * @since 4.0.7
     */
    public static short min(short... numberArray) {
        return GutilArray.min(numberArray);
    }

    /**
     * 取最小值
     *
     * @param numberArray 数字数组
     * @return 最小值
     * @see GutilArray#min(double...)
     * @since 4.0.7
     */
    public static double min(double... numberArray) {
        return GutilArray.min(numberArray);
    }

    /**
     * 取最小值
     *
     * @param numberArray 数字数组
     * @return 最小值
     * @see GutilArray#min(float...)
     * @since 4.0.7
     */
    public static float min(float... numberArray) {
        return GutilArray.min(numberArray);
    }

    /**
     * 取最小值
     *
     * @param numberArray 数字数组
     * @return 最小值
     * @see GutilArray#min(Comparable[])
     * @since 5.0.8
     */
    public static BigDecimal min(BigDecimal... numberArray) {
        return GutilArray.min(numberArray);
    }

    /**
     * 取最大值
     *
     * @param <T> 元素类型
     * @param numberArray 数字数组
     * @return 最大值
     * @see GutilArray#max(Comparable[])
     * @since 4.0.7
     */
    public static <T extends Comparable<? super T>> T max(T[] numberArray) {
        return GutilArray.max(numberArray);
    }

    /**
     * 取最大值
     *
     * @param numberArray 数字数组
     * @return 最大值
     * @see GutilArray#max(long...)
     * @since 4.0.7
     */
    public static long max(long... numberArray) {
        return GutilArray.max(numberArray);
    }

    /**
     * 取最大值
     *
     * @param numberArray 数字数组
     * @return 最大值
     * @see GutilArray#max(int...)
     * @since 4.0.7
     */
    public static int max(int... numberArray) {
        return GutilArray.max(numberArray);
    }

    /**
     * 取最大值
     *
     * @param numberArray 数字数组
     * @return 最大值
     * @see GutilArray#max(short...)
     * @since 4.0.7
     */
    public static short max(short... numberArray) {
        return GutilArray.max(numberArray);
    }

    /**
     * 取最大值
     *
     * @param numberArray 数字数组
     * @return 最大值
     * @see GutilArray#max(double...)
     * @since 4.0.7
     */
    public static double max(double... numberArray) {
        return GutilArray.max(numberArray);
    }

    /**
     * 取最大值
     *
     * @param numberArray 数字数组
     * @return 最大值
     * @see GutilArray#max(float...)
     * @since 4.0.7
     */
    public static float max(float... numberArray) {
        return GutilArray.max(numberArray);
    }

    /**
     * 取最大值
     *
     * @param numberArray 数字数组
     * @return 最大值
     * @see GutilArray#max(Comparable[])
     * @since 5.0.8
     */
    public static BigDecimal max(BigDecimal... numberArray) {
        return GutilArray.max(numberArray);
    }

    /**
     * 数字转字符串<br>
     * 调用{@link Number#toString()}，并去除尾小数点儿后多余的0
     *
     * @param number 数字
     * @param defaultValue 如果number参数为{@code null}，返回此默认值
     * @return 字符串
     * @since 3.0.9
     */
    public static String toStr(Number number, String defaultValue) {
        return (null == number) ? defaultValue : toStr(number);
    }

    /**
     * 数字转字符串<br>
     * 调用{@link Number#toString()}或 {@link BigDecimal#toPlainString()}，并去除尾小数点儿后多余的0
     *
     * @param number 数字
     * @return 字符串
     */
    public static String toStr(Number number) {
        GutilAssert.notNull(number, "Number is null !");

        // BigDecimal单独处理，使用非科学计数法
        if (number instanceof BigDecimal) {
            return toStr((BigDecimal) number);
        }

        GutilAssert.isTrue(isValidNumber(number), "Number is non-finite!");
        // 去掉小数点儿后多余的0
        String string = number.toString();
        if (string.indexOf('.') > 0 && string.indexOf('e') < 0 && string.indexOf('E') < 0) {
            while (string.endsWith("0")) {
                string = string.substring(0, string.length() - 1);
            }
            if (string.endsWith(".")) {
                string = string.substring(0, string.length() - 1);
            }
        }
        return string;
    }

    /**
     * {@link BigDecimal}数字转字符串<br>
     * 调用{@link BigDecimal#toPlainString()}，并去除尾小数点儿后多余的0
     *
     * @param bigDecimal 数字
     * @return 字符串
     * @since 5.4.6
     */
    public static String toStr(BigDecimal bigDecimal) {
        GutilAssert.notNull(bigDecimal, "BigDecimal is null !");
        return bigDecimal.stripTrailingZeros().toPlainString();
    }

    /**
     * 数字转{@link BigDecimal}<br>
     * Float、Double等有精度问题，转换为字符串后再转换<br>
     * null转换为0
     *
     * @param number 数字
     * @return {@link BigDecimal}
     * @since 4.0.9
     */
    public static BigDecimal toBigDecimal(Number number) {
        if (null == number) {
            return BigDecimal.ZERO;
        }

        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        } else if (number instanceof Long) {
            return new BigDecimal((Long) number);
        } else if (number instanceof Integer) {
            return new BigDecimal((Integer) number);
        } else if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        }

        // Float、Double等有精度问题，转换为字符串后再转换
        return toBigDecimal(number.toString());
    }

    /**
     * 数字转{@link BigDecimal}<br>
     * null或""或空白符转换为0
     *
     * @param number 数字字符串
     * @return {@link BigDecimal}
     * @since 4.0.9
     */
    public static BigDecimal toBigDecimal(String number) {
        return GutilStr.isBlank(number) ? BigDecimal.ZERO : new BigDecimal(number);
    }

    /**
     * 数字转{@link BigInteger}<br>
     * null转换为0<br>
     * BigDecimal使用其自身的{@link BigDecimal#toBigInteger()}转换，不丢失精度；<br>
     * Double、Float通过{@link BigDecimal#valueOf(double)}转换，避免longValue()截断或饱和
     *
     * @param number 数字
     * @return {@link BigInteger}
     * @since 5.4.5
     */
    public static BigInteger toBigInteger(Number number) {
        if (null == number) {
            return BigInteger.ZERO;
        }

        if (number instanceof BigInteger) {
            return (BigInteger) number;
        } else if (number instanceof BigDecimal) {
            // 使用BigDecimal自身的转换，避免longValue()截断丢失精度
            return ((BigDecimal) number).toBigInteger();
        } else if (number instanceof Double || number instanceof Float) {
            // 使用BigDecimal.valueOf转换，避免longValue()截断与饱和
            return BigDecimal.valueOf(number.doubleValue()).toBigInteger();
        }

        return BigInteger.valueOf(number.longValue());
    }

    /**
     * 数字转{@link BigInteger}<br>
     * null或""或空白符转换为0
     *
     * @param number 数字字符串
     * @return {@link BigInteger}
     * @since 5.4.5
     */
    public static BigInteger toBigInteger(String number) {
        return GutilStr.isBlank(number) ? BigInteger.ZERO : new BigInteger(number);
    }

    /**
     * 是否空白符<br>
     * 空白符包括空格、制表符、全角空格和不间断空格<br>
     *
     * @param c 字符
     * @return 是否空白符
     * @see Character#isWhitespace(int)
     * @see Character#isSpaceChar(int)
     * @since 3.0.6
     * @deprecated 请使用{@link GutilChar#isBlankChar(char)}
     */
    @Deprecated
    public static boolean isBlankChar(char c) {
        return isBlankChar((int) c);
    }

    /**
     * 是否空白符<br>
     * 空白符包括空格、制表符、全角空格和不间断空格<br>
     *
     * @param c 字符
     * @return 是否空白符
     * @see Character#isWhitespace(int)
     * @see Character#isSpaceChar(int)
     * @since 3.0.6
     * @deprecated 请使用{@link GutilChar#isBlankChar(int)}
     */
    @Deprecated
    public static boolean isBlankChar(int c) {
        return Character.isWhitespace(c)
                || Character.isSpaceChar(c)
                || c == '\ufeff'
                || c == '\u202a';
    }

    /**
     * 计算等份个数
     *
     * @param total 总数
     * @param part 每份的个数
     * @return 分成了几份
     * @since 3.0.6
     */
    public static int count(int total, int part) {
        return (total % part == 0) ? (total / part) : (total / part + 1);
    }

    /**
     * 空转0
     *
     * @param decimal {@link BigDecimal}，可以为{@code null}
     * @return {@link BigDecimal}参数为空时返回0的值
     * @since 3.0.9
     */
    public static BigDecimal null2Zero(BigDecimal decimal) {

        return decimal == null ? BigDecimal.ZERO : decimal;
    }

    /**
     * 如果给定值为0，返回1，否则返回原值
     *
     * @param value 值
     * @return 1或非0值
     * @since 3.1.2
     */
    public static int zero2One(int value) {
        return 0 == value ? 1 : value;
    }

    /**
     * 创建{@link BigInteger}，支持16进制、10进制和8进制，如果传入空白串返回null<br>
     * from Apache Common Lang
     *
     * @param str 数字字符串
     * @return {@link BigInteger}
     * @since 3.2.1
     */
    public static BigInteger newBigInteger(String str) {
        str = GutilStr.trimToNull(str);
        if (null == str) {
            return null;
        }

        int pos = 0; // 数字字符串位置
        int radix = 10;
        boolean negate = false; // 负数与否
        if (str.startsWith("-")) {
            negate = true;
            pos = 1;
        }
        if (str.startsWith("0x", pos) || str.startsWith("0X", pos)) {
            // 十六进制
            radix = 16;
            pos += 2;
        } else if (str.startsWith("#", pos)) {
            // 十六进制的另一种写法（Long/Integer 允许）
            radix = 16;
            pos++;
        } else if (str.startsWith("0", pos) && str.length() > pos + 1) {
            // 八进制；只要后面还有数字
            radix = 8;
            pos++;
        } // 默认按十进制处理

        if (pos > 0) {
            str = str.substring(pos);
        }
        final BigInteger value = new BigInteger(str, radix);
        return negate ? value.negate() : value;
    }

    /**
     * 判断两个数字是否相邻，例如1和2相邻，1和3不相邻<br>
     * 判断方法为做差取绝对值判断是否为1<br>
     * 使用{@link Math#subtractExact(long, long)}做差，减法溢出时返回false，避免溢出导致误判
     *
     * @param number1 数字1
     * @param number2 数字2
     * @return 是否相邻
     * @since 4.0.7
     */
    public static boolean isBeside(long number1, long number2) {
        try {
            return Math.abs(Math.subtractExact(number1, number2)) == 1;
        } catch (ArithmeticException e) {
            // 减法溢出，两个数必然不相邻
            return false;
        }
    }

    /**
     * 判断两个数字是否相邻，例如1和2相邻，1和3不相邻<br>
     * 判断方法为做差取绝对值判断是否为1<br>
     * 先转为long做差，避免int减法溢出导致误判
     *
     * @param number1 数字1
     * @param number2 数字2
     * @return 是否相邻
     * @since 4.0.7
     */
    public static boolean isBeside(int number1, int number2) {
        return Math.abs((long) number1 - number2) == 1;
    }

    /**
     * 把给定的总数平均分成N份，返回每份的个数<br>
     * 当除以分数有余数时每份+1
     *
     * @param total 总数
     * @param partCount 份数
     * @return 每份的个数
     * @since 4.0.7
     */
    public static int partValue(int total, int partCount) {
        return partValue(total, partCount, true);
    }

    /**
     * 把给定的总数平均分成N份，返回每份的个数<br>
     * 如果isPlusOneWhenHasRem为true，则当除以分数有余数时每份+1，否则丢弃余数部分
     *
     * @param total 总数
     * @param partCount 份数
     * @param isPlusOneWhenHasRem 在有余数时是否每份+1
     * @return 每份的个数
     * @since 4.0.7
     */
    public static int partValue(int total, int partCount, boolean isPlusOneWhenHasRem) {
        int partValue = total / partCount;
        if (isPlusOneWhenHasRem && total % partCount > 0) {
            partValue++;
        }
        return partValue;
    }

    /**
     * 提供精确的幂运算
     *
     * @param number 底数，为null时返回0
     * @param n 指数，必须大于等于0
     * @return 幂的积
     * @throws IllegalArgumentException 指数为负数时抛出
     * @since 4.1.0
     */
    public static BigDecimal pow(Number number, int n) {
        return pow(toBigDecimal(number), n);
    }

    /**
     * 提供精确的幂运算<br>
     * 底层调用{@link BigDecimal#pow(int)}，指数必须大于等于0
     *
     * @param number 底数，为null时返回0
     * @param n 指数，必须大于等于0
     * @return 幂的积
     * @throws IllegalArgumentException 指数为负数时抛出
     * @since 4.1.0
     */
    public static BigDecimal pow(BigDecimal number, int n) {
        if (null == number) {
            return BigDecimal.ZERO;
        }
        if (n < 0) {
            throw new IllegalArgumentException("Exponent must be >= 0, but got " + n);
        }
        return number.pow(n);
    }

    /**
     * 判断一个整数是否是2的幂
     *
     * @param n 待验证的整数
     * @return 如果n是2的幂返回true, 反之返回false
     */
    public static boolean isPowerOfTwo(long n) {
        return (n > 0) && ((n & (n - 1)) == 0);
    }

    /**
     * 解析转换数字字符串为int型数字，规则如下：
     *
     * <pre>
     * 1、0x/0X开头的视为16进制数字
     * 2、其它情况按照10进制转换（注意：不支持8进制，如"010"按10进制解析为10）
     * 3、空串返回0
     * 4、.123形式返回0（按照小于0的小数对待）
     * 5、123.56截取小数点之前的数字，忽略小数部分
     * 6、自动去除尾部的类型标识（D、F、L）
     * </pre>
     *
     * @param number 数字，支持0x/0X开头和普通十进制
     * @return int
     * @throws NumberFormatException 数字格式异常
     * @since 4.1.4
     */
    public static int parseInt(String number) throws NumberFormatException {
        if (GutilStr.isBlank(number)) {
            return 0;
        }

        // 对于带小数转换为整数采取去掉小数的策略
        number = GutilStr.subBefore(number, GutilChar.DOT, false);
        if (GutilStr.isEmpty(number)) {
            return 0;
        }

        if (GutilStr.startWithIgnoreCase(number, "0x")) {
            // 0x04表示16进制数
            return Integer.parseInt(number.substring(2), 16);
        }

        return Integer.parseInt(removeNumberFlag(number));
    }

    /**
     * 解析转换数字字符串为long型数字，规则如下：
     *
     * <pre>
     * 1、0x/0X开头的视为16进制数字
     * 2、其它情况按照10进制转换（注意：不支持8进制，如"010"按10进制解析为10）
     * 3、空串返回0
     * 4、.123形式返回0（按照小于0的小数对待）
     * 5、123.56截取小数点之前的数字，忽略小数部分
     * 6、自动去除尾部的类型标识（D、F、L）
     * </pre>
     *
     * @param number 数字，支持0x/0X开头和普通十进制
     * @return long
     * @throws NumberFormatException 数字格式异常
     * @since 4.1.4
     */
    public static long parseLong(String number) throws NumberFormatException {
        if (GutilStr.isBlank(number)) {
            return 0;
        }

        // 对于带小数转换为整数采取去掉小数的策略
        number = GutilStr.subBefore(number, GutilChar.DOT, false);
        if (GutilStr.isEmpty(number)) {
            return 0;
        }

        if (GutilStr.startWithIgnoreCase(number, "0x")) {
            // 0x04表示16进制数
            return Long.parseLong(number.substring(2), 16);
        }

        return Long.parseLong(removeNumberFlag(number));
    }

    /**
     * 将指定字符串转换为{@link Number} 对象<br>
     * 自动去除千位分隔符与尾部的类型标识（D、F、L）<br>
     * 空串或空白串返回0（Long类型）
     *
     * @param numberStr Number字符串
     * @return Number对象
     * @throws IllegalStateException 字符串格式不完整、无法解析时抛出（包装{@link java.text.ParseException}）
     * @since 4.1.15
     */
    public static Number parseNumber(String numberStr) {
        if (GutilStr.isBlank(numberStr)) {
            return 0L;
        }
        numberStr = removeNumberFlag(numberStr);
        try {
            return NumberFormat.getInstance().parse(numberStr);
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * int值转byte数组，使用大端字节序（高位字节在前，低位字节在后）<br>
     * 见：http://www.ruanyifeng.com/blog/2016/11/byte-order.html
     *
     * @param value 值
     * @return byte数组
     * @since 4.4.5
     */
    public static byte[] toBytes(int value) {
        final byte[] result = new byte[4];

        result[0] = (byte) (value >> 24);
        result[1] = (byte) (value >> 16);
        result[2] = (byte) (value >> 8);
        result[3] = (byte) (value /* >> 0 */);

        return result;
    }

    /**
     * byte数组转int，使用大端字节序（高位字节在前，低位字节在后）<br>
     * 见：http://www.ruanyifeng.com/blog/2016/11/byte-order.html
     *
     * @param bytes byte数组，长度必须大于等于4
     * @return int
     * @throws IllegalArgumentException bytes为null或长度小于4时抛出
     * @since 4.4.5
     */
    public static int toInt(byte[] bytes) {
        if (null == bytes || bytes.length < 4) {
            throw new IllegalArgumentException(
                    "Bytes length must be >= 4, but got "
                            + (null == bytes ? "null" : String.valueOf(bytes.length)));
        }
        return (bytes[0] & 0xff) << 24 //
                | (bytes[1] & 0xff) << 16 //
                | (bytes[2] & 0xff) << 8 //
                | (bytes[3] & 0xff);
    }

    /**
     * 以无符号字节数组的形式返回传入值。<br>
     * 去除{@link BigInteger#toByteArray()}结果中的符号字节（最高位为0的字节），返回无符号表示
     *
     * @param value 需要转换的值
     * @return 无符号bytes
     * @throws IllegalArgumentException value为null时抛出
     * @since 4.5.0
     */
    public static byte[] toUnsignedByteArray(BigInteger value) {
        if (null == value) {
            throw new IllegalArgumentException("Value must not be null!");
        }
        byte[] bytes = value.toByteArray();

        if (bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);

            return tmp;
        }

        return bytes;
    }

    /**
     * 以无符号字节数组的形式返回传入值。<br>
     * 与无length版本语义一致：去除符号字节后得到无符号表示，再左补0填充至指定长度
     *
     * @param length bytes长度，必须大于等于0
     * @param value 需要转换的值
     * @return 无符号bytes
     * @throws IllegalArgumentException value为null、length为负数或值的无符号表示超出指定长度时抛出
     * @since 4.5.0
     */
    public static byte[] toUnsignedByteArray(int length, BigInteger value) {
        if (null == value) {
            throw new IllegalArgumentException("Value must not be null!");
        }
        if (length < 0) {
            throw new IllegalArgumentException("Length must be >= 0, but got " + length);
        }
        byte[] bytes = value.toByteArray();

        int start = bytes[0] == 0 ? 1 : 0;
        int count = bytes.length - start;

        if (count > length) {
            throw new IllegalArgumentException("standard length exceeded for value");
        }

        byte[] tmp = new byte[length];
        System.arraycopy(bytes, start, tmp, tmp.length - count, count);
        return tmp;
    }

    /**
     * 无符号bytes转{@link BigInteger}
     *
     * @param buf buf 无符号bytes
     * @return {@link BigInteger}
     * @throws IllegalArgumentException buf为null时抛出
     * @since 4.5.0
     */
    public static BigInteger fromUnsignedByteArray(byte[] buf) {
        if (null == buf) {
            throw new IllegalArgumentException("Buffer must not be null!");
        }
        return new BigInteger(1, buf);
    }

    /**
     * 无符号bytes转{@link BigInteger}
     *
     * @param buf 无符号bytes
     * @param off 起始位置，必须大于等于0
     * @param length 长度，必须大于等于0
     * @return {@link BigInteger}
     * @throws IllegalArgumentException buf为null时抛出
     * @throws IndexOutOfBoundsException off、length为负数或off + length超出buf范围时抛出
     */
    public static BigInteger fromUnsignedByteArray(byte[] buf, int off, int length) {
        if (null == buf) {
            throw new IllegalArgumentException("Buffer must not be null!");
        }
        if (off < 0 || length < 0 || (long) off + length > buf.length) {
            throw new IndexOutOfBoundsException(
                    "Invalid offset/length: off="
                            + off
                            + ", length="
                            + length
                            + ", buf.length="
                            + buf.length);
        }
        byte[] mag = buf;
        if (off != 0 || length != buf.length) {
            mag = new byte[length];
            System.arraycopy(buf, off, mag, 0, length);
        }
        return new BigInteger(1, mag);
    }

    /**
     * 检查是否为有效的数字<br>
     * 检查Double和Float是否为无限大，或者Not a Number<br>
     * 非数字类型和Null将返回true
     *
     * @param number 被检查类型
     * @return 检查结果，非数字类型和Null将返回true
     * @since 4.6.7
     */
    public static boolean isValidNumber(Number number) {
        if (number instanceof Double) {
            return (false == ((Double) number).isInfinite())
                    && (false == ((Double) number).isNaN());
        } else if (number instanceof Float) {
            return (false == ((Float) number).isInfinite()) && (false == ((Float) number).isNaN());
        }
        return true;
    }

    // -------------------------------------------------------------------------------------------
    // Private method start 私有方法开始

    /**
     * 计算排列的一部分：selectNum * (selectNum - 1) * ... * minNum<br>
     * 递归终止条件：selectNum小于minNum时无解返回0，selectNum等于minNum时返回1，避免无限递归
     *
     * @param selectNum 选中小球个数
     * @param minNum 最少要选中多少个小球
     * @return 乘积，selectNum小于minNum时返回0
     */
    private static long mathSubnode(int selectNum, int minNum) {
        // 递归终止条件：selectNum小于minNum时无解，返回0，避免无限递归
        if (selectNum < minNum) {
            return 0;
        }
        if (selectNum == minNum) {
            return 1;
        } else {
            return (long) selectNum * mathSubnode(selectNum - 1, minNum);
        }
    }

    /**
     * 计算阶乘：selectNum!<br>
     * 递归终止条件：selectNum小于等于0时返回1，避免负数导致无限递归
     *
     * @param selectNum 数字
     * @return 阶乘结果
     */
    private static long mathNode(int selectNum) {
        if (selectNum <= 0) {
            return 1;
        } else {
            return (long) selectNum * mathNode(selectNum - 1);
        }
    }

    /**
     * 去掉数字尾部的数字标识，例如12D，44.0F，22L中的最后一个字母
     *
     * @param number 数字字符串
     * @return 去掉标识的字符串
     */
    private static String removeNumberFlag(String number) {
        // 去掉千位分隔符
        if (GutilStr.contains(number, GutilChar.COMMA)) {
            number = GutilStr.removeAll(number, GutilChar.COMMA);
        }
        // 去掉类型标识的结尾
        final int lastPos = number.length() - 1;
        final char lastCharUpper = Character.toUpperCase(number.charAt(lastPos));
        if ('D' == lastCharUpper || 'L' == lastCharUpper || 'F' == lastCharUpper) {
            number = GutilStr.subPre(number, lastPos);
        }
        return number;
    }
    // -------------------------------------------------------------------------------------------
    // Private method end 私有方法结束

}
