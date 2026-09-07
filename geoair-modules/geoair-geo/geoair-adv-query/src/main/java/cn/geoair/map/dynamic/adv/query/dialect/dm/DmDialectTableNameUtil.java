package cn.geoair.map.dynamic.adv.query.dialect.dm;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecDialectTableUtil;
import cn.hutool.core.util.StrUtil;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 达梦方言表名处理器
 */
public class DmDialectTableNameUtil extends AbstractExecDialectTableUtil {

    private static final DmDialectTableNameUtil INSTANCE = new DmDialectTableNameUtil();

    private static final String DM_DEFAULT_SCHEMA = null;

    private static final String DM_QUOTE_CHAR = "\"";

    private static final String FIELD_QUOTE_PREFIX = "\"";

    private static final String FIELD_QUOTE_SUFFIX = "\"";

    public static DmDialectTableNameUtil getInstance() {
        return INSTANCE;
    }

    @Override
    protected String getQuoteChar() {
        return DM_QUOTE_CHAR;
    }

    @Override
    protected String getDefaultSchemaName() {
        return DM_DEFAULT_SCHEMA;
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
    public String tbBuildAsTable(String startFragment, String aliasTableName) {
        return startFragment + "  " + aliasTableName;
    }

    @Override
    public String tbBuildPageSql(String noPageSql, int pageSize, long offset) {
        long endRow = offset + pageSize;
        long startRow = offset;
        return StrUtil.format(
                "SELECT * FROM (SELECT t.*, ROWNUM rn FROM ({}) t WHERE ROWNUM <= {}) WHERE rn > {}",
                noPageSql, endRow, startRow);
    }

    @Override
    public String tbBuildPageSql(String noPageSql) {
        return StrUtil.format(
                "SELECT * FROM (SELECT t.*, ROWNUM rn FROM ({}) t WHERE ROWNUM <= ?) WHERE rn > ?",
                noPageSql);
    }
}
