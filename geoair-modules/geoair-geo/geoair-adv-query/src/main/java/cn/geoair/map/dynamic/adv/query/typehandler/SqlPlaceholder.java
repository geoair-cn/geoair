package cn.geoair.map.dynamic.adv.query.typehandler;

/**
 * SQL 占位符结果，由 {@link AdvTypeHandler#getSqlPlaceholder(Object)} 返回。
 *
 * <p>返回 {@code null} 表示使用默认 {@code ?}，值放入参数列表。
 * 返回非空时，{@link #getSql()} 直接拼入 SQL，
 * {@link #getParam()} 替换原始值放入参数列表（为 null 则不放）。</p>
 *
 * @author zhangjun
 */
public class SqlPlaceholder {
    private final String sql;
    private final Object param;

    public SqlPlaceholder(String sql, Object param) {
        this.sql = sql;
        this.param = param;
    }

    /** @return SQL 表达式，替代 {@code ?} 的位置 */
    public String getSql() { return sql; }

    /** @return 要绑定的参数（null 表示值已内嵌在 sql 中，不占用参数位） */
    public Object getParam() { return param; }
}
