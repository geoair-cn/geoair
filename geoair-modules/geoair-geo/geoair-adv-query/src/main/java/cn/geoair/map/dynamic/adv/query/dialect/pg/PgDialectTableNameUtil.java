package cn.geoair.map.dynamic.adv.query.dialect.pg;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecDialectTableUtil;
import cn.hutool.core.util.StrUtil;

/**
 * PostgreSQL方言表名处理器 仅实现PG专属的差异化逻辑，复用父类所有通用逻辑
 */
public class PgDialectTableNameUtil extends AbstractExecDialectTableUtil {

    // 单例实例
    private static final PgDialectTableNameUtil INSTANCE = new PgDialectTableNameUtil();

    // PG专属常量
    private static final String POSTGRESQL_DEFAULT_SCHEMA = "public";

    private static final String POSTGRESQL_QUOTE_CHAR = "\"";

    private static final String FIELD_QUOTE_PREFIX = "\"";

    private static final String FIELD_QUOTE_SUFFIX = "\"";

    public static PgDialectTableNameUtil getInstance() {
        return INSTANCE;
    }

    // ========== 实现差异化抽象方法 ==========
    @Override
    protected String getQuoteChar() {
        return POSTGRESQL_QUOTE_CHAR;
    }

    @Override
    protected String getDefaultSchemaName() {
        return POSTGRESQL_DEFAULT_SCHEMA;
    }

    @Override
    public String tbQuoteFieldName(String fieldName) {
        if (StrUtil.isEmpty(fieldName)) {
            return fieldName;
        }
        String identifier = fieldName;
        if (identifier.startsWith(FIELD_QUOTE_PREFIX) && identifier.endsWith(FIELD_QUOTE_SUFFIX)
                && identifier.length() >= 2) {
            identifier = identifier.substring(1, identifier.length() - 1).replace("\"\"", "\"");
        }
        return FIELD_QUOTE_PREFIX + identifier.replace("\"", "\"\"") + FIELD_QUOTE_SUFFIX;
    }

    @Override
    public String tbBuildPageSql(String noPageSql, int pageSize, long offset) {
        return StrUtil.format("{} LIMIT {} OFFSET {}", noPageSql, pageSize, offset);
    }

    @Override
    public String tbBuildPageSql(String noPageSql) {
        return StrUtil.format("{} LIMIT {} OFFSET {}", noPageSql, "?", "?");
    }
}
