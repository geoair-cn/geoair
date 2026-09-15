package cn.geoair.map.dynamic.adv.query.dialect.sqlite;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecDialectTableUtil;
import cn.geoair.map.dynamic.adv.query.enums.AdvOperatorEnums;
import cn.hutool.core.util.StrUtil;

/** SQLite 标识符及分页语法处理器。 */
public final class SqliteDialectTableNameUtil extends AbstractExecDialectTableUtil {

    private static final SqliteDialectTableNameUtil INSTANCE =
            new SqliteDialectTableNameUtil();

    private SqliteDialectTableNameUtil() {}

    public static SqliteDialectTableNameUtil getInstance() {
        return INSTANCE;
    }

    @Override
    protected String getQuoteChar() {
        return "\"";
    }

    @Override
    protected String getDefaultSchemaName() {
        return "main";
    }

    @Override
    public String tbQuoteFieldName(String fieldName) {
        if (StrUtil.isEmpty(fieldName)) {
            return fieldName;
        }
        String identifier = fieldName;
        if (identifier.startsWith("\"") && identifier.endsWith("\"")
                && identifier.length() >= 2) {
            identifier = identifier.substring(1, identifier.length() - 1).replace("\"\"", "\"");
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String tbBuildPageSql(String noPageSql, int pageSize, long offset) {
        return StrUtil.format("{} LIMIT {} OFFSET {}", noPageSql, pageSize, offset);
    }

    @Override
    public String tbBuildPageSql(String noPageSql) {
        return StrUtil.format("{} LIMIT ? OFFSET ?", noPageSql);
    }

    @Override
    public String tbGetOperatorSql(AdvOperatorEnums operator) {
        if (operator == AdvOperatorEnums.EQUAL_NULL_SAFE) {
            return "IS";
        }
        if (operator == AdvOperatorEnums.ILIKE_LEFT
                || operator == AdvOperatorEnums.ILIKE_RIGHT
                || operator == AdvOperatorEnums.ILIKE_ALL
                || operator == AdvOperatorEnums.ANY
                || operator == AdvOperatorEnums.ALL) {
            throw new UnsupportedOperationException(
                    "SQLite Core 条件构建不支持操作符：" + operator.getSqlValue());
        }
        return operator.getSqlValue();
    }
}
