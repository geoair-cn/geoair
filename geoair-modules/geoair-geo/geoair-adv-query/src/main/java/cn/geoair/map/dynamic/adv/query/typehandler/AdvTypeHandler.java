package cn.geoair.map.dynamic.adv.query.typehandler;

import cn.geoair.base.sp.annotation.GkSP;
import cn.geoair.base.sp.support.GirJdkSpLoader;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 高级查询器的类型处理器
 */
@GkSP(loader = GirJdkSpLoader.class)
public interface AdvTypeHandler<T> {

    boolean supports(Class<?> javaType, Object value);

    T convertForRead(Object value, Class<?> javaType, AdvTypeHandlerContext context);

    Object convertForWrite(T value, Class<?> javaType, AdvTypeHandlerContext context);

    /**
     * 返回 SQL 占位符表达式。
     *
     * <p>返回 {@code null} 表示使用默认 {@code ?}，值放入参数列表。
     * 返回非空时，{@link SqlPlaceholder#getSql()} 直接拼入 SQL，
     * {@link SqlPlaceholder#getParam()} 替换原始值放入参数列表（为 null 则不放）。</p>
     */
    default SqlPlaceholder getSqlPlaceholder(Object value) {
        return null;
    }
}
