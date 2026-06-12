package cn.geoair.map.dynamic.adv.query.dialect.oracle;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecDialectTableUtil;
import cn.hutool.core.util.StrUtil;

/**
 * Oracle方言表名处理器
 *
 * <p>实现Oracle专属的差异化逻辑，复用父类所有通用逻辑
 *
 * <p>注意：Oracle使用双引号作为标识符引用符，默认Schema通常为用户名
 *
 * @author zhangjun
 */
public class OracleDialectTableNameUtil extends AbstractExecDialectTableUtil {

    // 单例实例
    private static final OracleDialectTableNameUtil INSTANCE = new OracleDialectTableNameUtil();

    // Oracle专属常量
    private static final String ORACLE_DEFAULT_SCHEMA = null; // Oracle默认使用当前用户Schema

    private static final String ORACLE_QUOTE_CHAR = "\"";

    private static final String FIELD_QUOTE_PREFIX = "\"";

    private static final String FIELD_QUOTE_SUFFIX = "\"";

    public static OracleDialectTableNameUtil getInstance() {
        return INSTANCE;
    }

    // ========== 实现差异化抽象方法 ==========

    @Override
    protected String getQuoteChar() {
        return ORACLE_QUOTE_CHAR;
    }

    @Override
    protected String getDefaultSchemaName() {
        return ORACLE_DEFAULT_SCHEMA;
    }

    @Override
    public String tbQuoteFieldName(String fieldName) {
        if (StrUtil.isEmpty(fieldName)) {
            return fieldName;
        }
        // 如果已经包含引号，直接返回
        if (fieldName.startsWith(FIELD_QUOTE_PREFIX) && fieldName.endsWith(FIELD_QUOTE_SUFFIX)) {
            return fieldName;
        }
        // Oracle字段名默认转为大写（不加引号时），加双引号则保持原样
        return FIELD_QUOTE_PREFIX + fieldName + FIELD_QUOTE_SUFFIX;
    }

    @Override
    public String tbBuildAsTable(String startFragment, String aliasTableName) {
        return startFragment + "  " + aliasTableName;
    }

    /**
     * Oracle分页SQL（使用ROWNUM方式）
     *
     * <p>Oracle 9i+ 兼容写法
     *
     * <p>生成的SQL结构：
     *
     * <pre>
     * SELECT * FROM (
     *     SELECT t.*, ROWNUM rn FROM (
     *         原始SQL
     *     ) t WHERE ROWNUM <= {pageSize}
     * ) WHERE rn > {offset}
     * </pre>
     *
     * @param noPageSql 原始SQL
     * @param pageSize 每页条数
     * @param offset 偏移量
     * @return 分页SQL
     */
    @Override
    public String tbBuildPageSql(String noPageSql, int pageSize, long offset) {
        // 计算 ROWNUM 的上限（起始行 + 每页条数）
        long endRow = offset + pageSize;
        long startRow = offset;
        return StrUtil.format(
                "SELECT * FROM (SELECT t.*, ROWNUM rn_temp FROM ({}) t WHERE ROWNUM <= {}) WHERE rn_temp > {}",
                noPageSql,
                endRow,
                startRow);
    }

    /**
     * Oracle分页SQL（使用ROWNUM方式，带占位符）
     *
     * <p>Oracle 12c+ 也支持 OFFSET FETCH 语法，这里使用兼容性更好的 ROWNUM 方式
     *
     * @param noPageSql 原始SQL
     * @return 分页SQL（带?占位符）
     */
    @Override
    public String tbBuildPageSql(String noPageSql) {
        // 使用 ROWNUM 实现分页（兼容 Oracle 9i+）
        // 三层嵌套：内层排序，中层限制最大行数，外层过滤起始行
        String sql =
                StrUtil.format(
                        "SELECT * FROM (SELECT t.*, ROWNUM rn_temp FROM ({}) t WHERE ROWNUM <= ?) WHERE rn_temp > ?",
                        noPageSql);
        return sql;
    }

    /**
     * Oracle 12c+ 推荐的分页方式（使用 OFFSET FETCH） 如果确认使用 Oracle 12c+，可以使用此方法
     *
     * @param noPageSql 原始SQL
     * @return 分页SQL（带?占位符）
     */
    public String tbBuildPageSqlWithOffsetFetch(String noPageSql) {
        return StrUtil.format("{} OFFSET ? ROWS FETCH NEXT ? ROWS ONLY", noPageSql);
    }
}
