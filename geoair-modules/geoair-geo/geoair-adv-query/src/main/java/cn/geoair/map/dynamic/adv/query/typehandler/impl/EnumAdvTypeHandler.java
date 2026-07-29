package cn.geoair.map.dynamic.adv.query.typehandler.impl;

import cn.geoair.map.dynamic.adv.query.typehandler.AdvBaseTypeHandler;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 枚举类型处理器
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class EnumAdvTypeHandler extends AdvBaseTypeHandler<Enum> {

    @Override
    public boolean supports(Class<?> javaType, Object value) {
        return javaType != null && javaType.isEnum();
    }

    @Override
    protected Enum convertNonNullForRead(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        if (javaType.isInstance(value)) {
            return (Enum) value;
        }
        String text = String.valueOf(value).trim();
        Object[] constants = javaType.getEnumConstants();
        if (constants == null) {
            return null;
        }
        for (Object constant : constants) {
            Enum enumValue = (Enum) constant;
            if (enumValue.name().equalsIgnoreCase(text)) {
                return enumValue;
            }
        }
        try {
            int ordinal = Integer.parseInt(text);
            if (ordinal >= 0 && ordinal < constants.length) {
                return (Enum) constants[ordinal];
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        return Enum.valueOf((Class) javaType, text);
    }

    @Override
    protected Object convertNonNullForWrite(
            Enum value, Class<?> javaType, AdvTypeHandlerContext context) {
        return value.name();
    }
}
