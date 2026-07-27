package cn.geoair.map.dynamic.adv.query.typehandler;

import cn.hutool.core.convert.Convert;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 默认对象类型处理器
 */
public class ObjectAdvTypeHandler extends AdvBaseTypeHandler<Object> {

    @Override
    public boolean supports(Class<?> javaType, Object value) {
        return true;
    }

    @Override
    protected Object convertNonNullForRead(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        try {
            return Convert.convert(javaType, value);
        } catch (Exception e) {
            return value;
        }

    }
}
