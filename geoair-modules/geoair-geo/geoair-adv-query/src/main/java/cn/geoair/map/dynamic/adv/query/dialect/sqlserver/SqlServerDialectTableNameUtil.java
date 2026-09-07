package cn.geoair.map.dynamic.adv.query.dialect.sqlserver;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecDialectTableUtil;
import cn.hutool.core.util.StrUtil;

/**
 * SQL Server 标识符与分页语法处理器。
 * <p>
 * SQL Server 使用方括号引用标识符，且 OFFSET/FETCH 必须带有 ORDER BY。
 * 当调用方没有提供稳定排序时，仅使用 {@code ORDER BY (SELECT 0)} 使语法可执行；
 * 这不保证跨页顺序稳定，业务分页查询应自行提供唯一排序字段。
 *
 * @author 张逢吉
 */
public final class SqlServerDialectTableNameUtil extends AbstractExecDialectTableUtil {

    private static final SqlServerDialectTableNameUtil INSTANCE = new SqlServerDialectTableNameUtil();

    private SqlServerDialectTableNameUtil() {
    }

    public static SqlServerDialectTableNameUtil getInstance() {
        return INSTANCE;
    }

    @Override
    protected String getQuoteChar() {
        return "[";
    }

    @Override
    protected String getDefaultSchemaName() {
        return "dbo";
    }

    @Override
    public String tbQuoteTableName(String tableName) {
        return quoteIdentifier(tableName);
    }

    @Override
    public String tbQuoteSchemaName(String schemaName) {
        return quoteIdentifier(schemaName);
    }

    @Override
    public String tbQuoteFieldName(String fieldName) {
        return quoteIdentifier(fieldName);
    }

    @Override
    public String tbUnquoteTableName(String quotedTableName) {
        return unquoteIdentifier(quotedTableName);
    }

    @Override
    public String tbUnquoteSchemaName(String quotedSchemaName) {
        return unquoteIdentifier(quotedSchemaName);
    }

    /**
     * SQL Server 常见的 {@code [schema].[table]} 不能交给父类的通用正则解析，
     * 因为方括号内允许使用 {@code ]]} 转义字符。
     */
    @Override
    public String tbGetTableNameNotSchema(String fullTableName) {
        String normalized = StrUtil.trim(fullTableName);
        int separator = findSchemaSeparator(normalized);
        return unquoteIdentifier(separator < 0 ? normalized : normalized.substring(separator + 1));
    }

    @Override
    public String tbExtractSchemaName(String fullTableName) {
        String normalized = StrUtil.trim(fullTableName);
        int separator = findSchemaSeparator(normalized);
        return separator < 0 ? null : unquoteIdentifier(normalized.substring(0, separator));
    }

    @Override
    public String tbBuildPageSql(String noPageSql, int pageSize, long offset) {
        String orderedSql = containsOrderBy(noPageSql) ? noPageSql : noPageSql + " ORDER BY (SELECT 0)";
        return StrUtil.format("{} OFFSET {} ROWS FETCH NEXT {} ROWS ONLY", orderedSql, offset, pageSize);
    }

    @Override
    public String tbBuildPageSql(String noPageSql) {
        String orderedSql = containsOrderBy(noPageSql) ? noPageSql : noPageSql + " ORDER BY (SELECT 0)";
        return orderedSql + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
    }

    private String quoteIdentifier(String value) {
        if (StrUtil.isEmpty(value) || (value.startsWith("[") && value.endsWith("]"))) {
            return value;
        }
        return "[" + value.replace("]", "]]" ) + "]";
    }

    private String unquoteIdentifier(String value) {
        if (StrUtil.isEmpty(value)) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).replace("]]", "]");
        }
        return super.tbUnquoteTableName(trimmed);
    }

    private boolean containsOrderBy(String sql) {
        return sql != null && sql.toLowerCase(java.util.Locale.ROOT).matches("(?s).*\\border\\s+by\\b.*");
    }

    /** 查找不位于方括号标识符内部的首个分隔点。 */
    private int findSchemaSeparator(String value) {
        if (StrUtil.isBlank(value)) {
            return -1;
        }
        boolean inBracket = false;
        String text = value.trim();
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '[') {
                inBracket = true;
            } else if (current == ']' && inBracket) {
                if (i + 1 < text.length() && text.charAt(i + 1) == ']') {
                    i++;
                } else {
                    inBracket = false;
                }
            } else if (current == '.' && !inBracket) {
                return i;
            }
        }
        return -1;
    }
}
