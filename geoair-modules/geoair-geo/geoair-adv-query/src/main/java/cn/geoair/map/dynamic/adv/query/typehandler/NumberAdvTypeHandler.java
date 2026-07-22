package cn.geoair.map.dynamic.adv.query.typehandler;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 数值类型处理器
 */
public class NumberAdvTypeHandler extends AdvBaseTypeHandler<Number> {

    @Override
    public boolean supports(Class<?> javaType, Object value) {
        if (javaType == null) {
            return false;
        }
        return Number.class.isAssignableFrom(javaType)
                || javaType == int.class
                || javaType == long.class
                || javaType == short.class
                || javaType == byte.class
                || javaType == double.class
                || javaType == float.class;
    }

    @Override
    protected Number convertNonNullForRead(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        if (value instanceof Number && matchesNumberType((Number) value, javaType)) {
            return (Number) value;
        }
        String text = String.valueOf(value).trim();
        if (javaType == Integer.class || javaType == int.class) {
            return Integer.valueOf(text);
        }
        if (javaType == Long.class || javaType == long.class) {
            return Long.valueOf(text);
        }
        if (javaType == Short.class || javaType == short.class) {
            return Short.valueOf(text);
        }
        if (javaType == Byte.class || javaType == byte.class) {
            return Byte.valueOf(text);
        }
        if (javaType == Double.class || javaType == double.class) {
            return Double.valueOf(text);
        }
        if (javaType == Float.class || javaType == float.class) {
            return Float.valueOf(text);
        }
        if (javaType == BigDecimal.class) {
            return new BigDecimal(text);
        }
        if (javaType == BigInteger.class) {
            return new BigInteger(text);
        }
        return value instanceof Number ? (Number) value : new BigDecimal(text);
    }

    private boolean matchesNumberType(Number value, Class<?> javaType) {
        if (javaType == Integer.class || javaType == int.class) {
            return value instanceof Integer;
        }
        if (javaType == Long.class || javaType == long.class) {
            return value instanceof Long;
        }
        if (javaType == Short.class || javaType == short.class) {
            return value instanceof Short;
        }
        if (javaType == Byte.class || javaType == byte.class) {
            return value instanceof Byte;
        }
        if (javaType == Double.class || javaType == double.class) {
            return value instanceof Double;
        }
        if (javaType == Float.class || javaType == float.class) {
            return value instanceof Float;
        }
        if (javaType == BigDecimal.class) {
            return value instanceof BigDecimal;
        }
        if (javaType == BigInteger.class) {
            return value instanceof BigInteger;
        }
        return false;
    }
}
