package cn.geoair.map.dynamic.adv.query.typehandler.impl;

import cn.geoair.map.dynamic.adv.query.typehandler.AdvBaseTypeHandler;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;
import java.util.Base64;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 字节数组类型处理器
 */
public class ByteArrayAdvTypeHandler extends AdvBaseTypeHandler<Object> {

    @Override
    public boolean supports(Class<?> javaType, Object value) {
        return javaType == byte[].class || javaType == Byte[].class;
    }

    @Override
    protected Object convertNonNullForRead(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        if (javaType == byte[].class) {
            return toPrimitiveBytes(value);
        }
        if (javaType == Byte[].class) {
            return toWrapperBytes(value);
        }
        return value;
    }

    @Override
    protected Object convertNonNullForWrite(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        if (value instanceof Byte[]) {
            return toPrimitiveBytes(value);
        }
        return value;
    }

    public String toBase64(Object value) {
        byte[] bytes = toPrimitiveBytes(value);
        return bytes == null ? null : Base64.getEncoder().encodeToString(bytes);
    }

    private byte[] toPrimitiveBytes(Object value) {
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        if (value instanceof Byte[]) {
            Byte[] source = (Byte[]) value;
            byte[] target = new byte[source.length];
            for (int i = 0; i < source.length; i++) {
                target[i] = source[i] == null ? 0 : source[i];
            }
            return target;
        }
        return null;
    }

    private Byte[] toWrapperBytes(Object value) {
        if (value instanceof Byte[]) {
            return (Byte[]) value;
        }
        if (value instanceof byte[]) {
            byte[] source = (byte[]) value;
            Byte[] target = new Byte[source.length];
            for (int i = 0; i < source.length; i++) {
                target[i] = source[i];
            }
            return target;
        }
        return null;
    }
}
