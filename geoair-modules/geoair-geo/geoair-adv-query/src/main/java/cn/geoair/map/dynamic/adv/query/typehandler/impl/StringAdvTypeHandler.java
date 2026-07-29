package cn.geoair.map.dynamic.adv.query.typehandler.impl;

import cn.geoair.map.dynamic.adv.query.typehandler.AdvBaseTypeHandler;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 字符串类型处理器
 */
public class StringAdvTypeHandler extends AdvBaseTypeHandler<String> {

    @Override
    public boolean supports(Class<?> javaType, Object value) {
        return javaType != null && CharSequence.class.isAssignableFrom(javaType);
    }

    @Override
    protected String convertNonNullForRead(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        return String.valueOf(value);
    }

    @Override
    protected Object convertNonNullForWrite(
            String value, Class<?> javaType, AdvTypeHandlerContext context) {
        return value;
    }
}
