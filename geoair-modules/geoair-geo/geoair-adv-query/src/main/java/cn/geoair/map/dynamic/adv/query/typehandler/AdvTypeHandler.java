package cn.geoair.map.dynamic.adv.query.typehandler;

import cn.geoair.base.sp.annotation.GkSP;
import cn.geoair.base.sp.support.GirJdkSpLoader;

/**
 * 类型处理器 —— Java 类型与 JDBC 之间的双向转换。
 *
 * <p>实现类通过 SPI 加载，按优先级链式匹配。 读方向：JDBC 原始值 → Java 对象；写方向：Java 对象 → JDBC 可接受值。 同时支持自定义 SQL 占位符表达式（如
 * MySQL 几何类型的 {@code ST_GeomFromText(?, srid)}）。
 *
 * @param <T> 处理的目标 Java 类型
 * @author zhangjun
 */
@GkSP(loader = GirJdkSpLoader.class)
public interface AdvTypeHandler<T> {

    /**
     * 判断当前处理器是否支持该类型和值。
     *
     * @param javaType 目标 Java 类型
     * @param value 原始值（可能为 null）
     * @return true 表示由本处理器处理
     */
    boolean supports(Class<?> javaType, Object value);

    /**
     * 读方向：将 JDBC 原始值转换为目标 Java 类型。
     *
     * @param value JDBC 原始值（PGobject / byte[] / String 等）
     * @param javaType 期望的 Java 类型
     * @param context 类型处理上下文（字段名、列名、Bean 类型等）
     * @return 转换后的 Java 对象
     */
    T convertForRead(Object value, Class<?> javaType, AdvTypeHandlerContext context);

    /**
     * 写方向：将 Java 对象转换为 JDBC 可接受的写入值。
     *
     * @param value Java 对象
     * @param javaType Java 类型
     * @param context 类型处理上下文
     * @return JDBC 写入值（String / byte[] / 数据库特定对象等）
     */
    Object convertForWrite(T value, Class<?> javaType, AdvTypeHandlerContext context);

    /**
     * 返回 SQL 占位符表达式，用于替代 INSERT/UPDATE 语句中的普通 {@code ?}。
     *
     * <p>返回 {@code null} 表示使用默认 {@code ?}，原值放入参数列表。 返回非空时，{@link SqlPlaceholder#getSql()} 直接拼入 SQL
     * 模板， {@link SqlPlaceholder#getParam()} 替换原始值放入参数列表（为 {@code null} 则不放）。
     *
     * <p>典型用法：
     *
     * <pre>{@code
     * // MySQL 几何列：WKT 必须直接嵌入 SQL，不能用 ? 传参
     * return new SqlPlaceholder("ST_GeomFromText('LINESTRING(...)', 4326)", null);
     *
     * // PG 几何列：? 占位符即可，驱动自动处理
     * return null;
     * }</pre>
     *
     * @param value 要写入的 Java 值
     * @return 占位符表达式，{@code null} 表示用默认 {@code ?}
     */
    default SqlPlaceholder getSqlPlaceholder(Object value) {
        return null;
    }
}
