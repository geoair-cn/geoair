package cn.geoair.map.dynamic.adv.query.dialect.mysql;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecDialectTableUtil;
import cn.hutool.core.util.StrUtil;

/** MySQL方言表名处理器 仅实现MySQL专属的差异化逻辑 */
public class MysqlDialectTableNameUtil extends AbstractExecDialectTableUtil {

    // 单例实例
    private static final MysqlDialectTableNameUtil INSTANCE = new MysqlDialectTableNameUtil();

    // MySQL专属常量
    private static final String MYSQL_DEFAULT_DATABASE = ""; // MySQL无默认库名

    private static final String MYSQL_QUOTE_CHAR = "`";

    private static final String FIELD_QUOTE_PREFIX = "`";

    private static final String FIELD_QUOTE_SUFFIX = "`";

    public static MysqlDialectTableNameUtil getInstance() {
        return INSTANCE;
    }

    // ========== 实现差异化抽象方法 ==========
    @Override
    protected String getQuoteChar() {
        return MYSQL_QUOTE_CHAR;
    }

    @Override
    protected String getDefaultSchemaName() {
        return MYSQL_DEFAULT_DATABASE;
    }

    @Override
    public String tbQuoteFieldName(String fieldName) {
        if (StrUtil.isEmpty(fieldName)) {
            return fieldName;
        }
        String identifier = fieldName;
        if (identifier.startsWith(FIELD_QUOTE_PREFIX) && identifier.endsWith(FIELD_QUOTE_SUFFIX)
                && identifier.length() >= 2) {
            identifier = identifier.substring(1, identifier.length() - 1).replace("``", "`");
        }
        return FIELD_QUOTE_PREFIX + identifier.replace("`", "``") + FIELD_QUOTE_SUFFIX;
    }

    @Override
    public String tbBuildPageSql(String noPageSql, int pageSize, long offset) {
        return StrUtil.format("{} LIMIT {}, {}", noPageSql, offset, pageSize);
    }


    @Override
    public String tbBuildPageSql(String noPageSql) {
        return StrUtil.format("{} LIMIT {}, {}", noPageSql, "?", "?");
    }
}
