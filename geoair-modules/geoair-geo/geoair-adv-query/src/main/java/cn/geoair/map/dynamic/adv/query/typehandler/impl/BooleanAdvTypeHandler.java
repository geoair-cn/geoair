package cn.geoair.map.dynamic.adv.query.typehandler.impl;

import cn.geoair.map.dynamic.adv.query.typehandler.AdvBaseTypeHandler;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;

import cn.hutool.core.util.BooleanUtil;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 布尔类型处理器
 */
public class BooleanAdvTypeHandler extends AdvBaseTypeHandler<Boolean> {

    @Override
    public boolean supports(Class<?> javaType, Object value) {
        return javaType == Boolean.class || javaType == boolean.class;
    }

    @Override
    protected Boolean convertNonNullForRead(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = String.valueOf(value).trim();
        if ("1".equals(text) || "Y".equalsIgnoreCase(text) || "YES".equalsIgnoreCase(text)) {
            return true;
        }
        if ("0".equals(text) || "N".equalsIgnoreCase(text) || "NO".equalsIgnoreCase(text)) {
            return false;
        }
        return BooleanUtil.toBoolean(text);
    }
}
