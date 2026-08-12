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
     * 返回 SQL 占位符表达式，用于替代 INSERT 语句中的普通 {@code ?}。
     *
     * <p>例如 MySQL 几何类型返回 {@code ST_GeomFromText(?, 4326, 'axis-order=long-lat')}。
     * 返回 {@code null} 表示使用默认的 {@code ?}。</p>
     *
     * @param value 要写入的值
     * @return SQL 占位符表达式，null 表示用默认 ?
     */
    default String getSqlPlaceholder(Object value) {
        return null;
    }
}
