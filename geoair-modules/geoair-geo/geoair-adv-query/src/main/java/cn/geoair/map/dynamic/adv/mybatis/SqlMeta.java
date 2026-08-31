package cn.geoair.map.dynamic.adv.mybatis;

import java.util.Collections;
import java.util.List;

/**
 * 动态 SQL 引擎的解析结果，包含最终的 SQL 文本和对应的 JDBC 参数值列表。
 *
 * <p>SQL 中的 {@code #{}} 已被替换为 {@code ?} 占位符，{@code jdbcParamValues} 按顺序保存每个 占位符对应的参数值，可直接用于 {@link
 * java.sql.PreparedStatement}。
 *
 * @author zhangjun
 */
public class SqlMeta {

    private String sql;
    private List<Object> jdbcParamValues;

    public SqlMeta(String sql, List<Object> jdbcParamValues) {
        this.sql = sql;
        this.jdbcParamValues = jdbcParamValues;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    /**
     * 获取 JDBC 参数值列表（不可变视图）。
     *
     * @return 参数值列表
     */
    public List<Object> getJdbcParamValues() {
        return Collections.unmodifiableList(jdbcParamValues);
    }

    public void setJdbcParamValues(List<Object> jdbcParamValues) {
        this.jdbcParamValues = jdbcParamValues;
    }

    @Override
    public String toString() {
        return "SqlMeta{sql='" + sql + "', params=" + jdbcParamValues + "}";
    }
}
