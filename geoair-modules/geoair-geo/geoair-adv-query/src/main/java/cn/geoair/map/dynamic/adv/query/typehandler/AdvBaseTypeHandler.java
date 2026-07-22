package cn.geoair.map.dynamic.adv.query.typehandler;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 高级查询类型处理器基类
 */
public abstract class AdvBaseTypeHandler<T> implements AdvTypeHandler<T> {

    @Override
    public T convertForRead(Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        if (value == null) {
            return null;
        }
        return convertNonNullForRead(value, javaType, context);
    }

    @Override
    public Object convertForWrite(T value, Class<?> javaType, AdvTypeHandlerContext context) {
        if (value == null) {
            return null;
        }
        return convertNonNullForWrite(value, javaType, context);
    }

    protected abstract T convertNonNullForRead(
            Object value, Class<?> javaType, AdvTypeHandlerContext context);

    protected Object convertNonNullForWrite(
            T value, Class<?> javaType, AdvTypeHandlerContext context) {
        return value;
    }
}
