package cn.geoair.map.dynamic.adv.query.dialect.pg;

import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.ds.IDataSourceGetter;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostgreSQL方言表名处理器
 * 实现PostgreSQL数据库下的表名、Schema处理逻辑，遵循DialectTableNameProcessor接口规范
 *
 * @author 张逢吉
 * @date 2025/10/15 10:44
 */
public class PgDialectTableNameUtil implements DialectTableNameProcessor {

    static PgDialectTableNameUtil dialectTableNameProcessor = new PgDialectTableNameUtil();

    public static DialectTableNameProcessor getInstance() {
        return dialectTableNameProcessor;
    }

    // PostgreSQL默认Schema（固定值）
    private static final String POSTGRESQL_DEFAULT_SCHEMA = "public";
    // PostgreSQL标准引号（双引号，避免与SQL关键字冲突）
    private static final String POSTGRESQL_QUOTE_CHAR = "\"";

    @Override
    public String tbGetSchemaNameForSql(IDataSourceGetter dataSourceGetter) {
        // 优先级：已有Schema名 > 默认Schema（public）
        return ObjectUtil.isEmpty(dataSourceGetter.getSchemaName()) ? POSTGRESQL_DEFAULT_SCHEMA : dataSourceGetter.getSchemaName();
    }

    @Override
    public String tbGetTableNameWithSchema(IDataSourceGetter dataSourceGetter, String tableName) {
        // 复用“指定Schema”的方法，传入当前类的默认Schema
        return tbGetTableNameWithSchema(dataSourceGetter, tableName, dataSourceGetter.getSchemaName());
    }

    @Override
    public String tbGetTableNameWithSchema(IDataSourceGetter dataSourceGetter, String tableName, String schemaName) {
        // Step1：确定最终Schema（表名提取 > 传入Schema > 默认Schema）
        String extractedSchema = tbExtractSchemaName(tableName);
        if (StrUtil.isNotEmpty(extractedSchema)) {
            schemaName = extractedSchema;
        } else if (ObjectUtil.isEmpty(schemaName)) {
            schemaName = tbGetSchemaNameForSql(dataSourceGetter);
        }

        // Step2：处理Schema引号（确保统一用PostgreSQL双引号）
        String quotedSchema = tbQuoteSchemaName(schemaName);
        // Step3：处理表名（去Schema + 加引号）
        String pureTableName = tbGetTableNameNotSchema(tableName);
        String quotedTableName = tbQuoteTableName(pureTableName);

        // Step4：组合完整表名（避免重复拼接，如已含“.”则直接返回）
        if (StrUtil.isNotEmpty(quotedSchema) && !quotedTableName.contains(".")) {
            return String.format("%s.%s", quotedSchema, quotedTableName);
        }
        return quotedTableName;
    }

    @Override
    public String tbGetTableNameNotSchema(String fullTableName) {
        if (StrUtil.isEmpty(fullTableName)) {
            return fullTableName;
        }

        // 预处理：去空格
        String processedName = fullTableName.trim();
        // 正则：匹配“Schema.表名”格式（支持带引号/不带引号）
        Pattern pattern = Pattern.compile(
                "(?:[\"'`][^\"'`]+[\"'`]|[a-zA-Z0-9_]+)\\." +  // Schema部分（不变）
                        "([\"'`][^\"'`]+[\"'`]|[a-zA-Z0-9_\\u4e00-\\u9fa5]+)"  // 表名部分增加中文范围 \u4e00-\u9fa5
        );
        Matcher matcher = pattern.matcher(processedName);

        // 匹配成功：提取表名部分并去引号；失败：直接去引号返回
        if (matcher.matches()) {
            return tbUnquoteTableName(matcher.group(1));
        }
        return tbUnquoteTableName(processedName);
    }

    @Override
    public String tbExtractSchemaName(String fullTableName) {
        if (StrUtil.isEmpty(fullTableName)) {
            return null;
        }

        // 预处理：去空格
        String processedName = fullTableName.trim();
        // 正则：支持Schema名称包含中文、字母、数字、下划线及部分特殊字符（根据实际需求调整）
        Pattern pattern = Pattern.compile(
                "(?:([\"'`][^\"'`]+[\"'`]|[a-zA-Z0-9_\\u4e00-\\u9fa5-]+))\\." +  // Schema部分增加中文和横线
                        "(?:[\"'`][^\"'`]+[\"'`]|[a-zA-Z0-9_\\u4e00-\\u9fa5]+)"       // 表名部分也同步支持中文（如果需要）
        );
        Matcher matcher = pattern.matcher(processedName);

        // 匹配成功：提取Schema部分并去引号；失败：返回null（无Schema）
        if (matcher.matches()) {
            return tbUnquoteSchemaName(matcher.group(1));
        }
        return null;
    }

    @Override
    public String tbQuoteTableName(String tableName) {
        // 避免重复加引号：已带双引号则直接返回
        if (StrUtil.isEmpty(tableName) || (tableName.startsWith(POSTGRESQL_QUOTE_CHAR) && tableName.endsWith(POSTGRESQL_QUOTE_CHAR))) {
            return tableName;
        }
        return StrUtil.wrap(tableName, POSTGRESQL_QUOTE_CHAR);
    }

    @Override
    public String tbQuoteSchemaName(String schemaName) {
        // 逻辑同表名引号处理（复用规则）
        if (StrUtil.isEmpty(schemaName) || (schemaName.startsWith(POSTGRESQL_QUOTE_CHAR) && schemaName.endsWith(POSTGRESQL_QUOTE_CHAR))) {
            return schemaName;
        }
        return StrUtil.wrap(schemaName, POSTGRESQL_QUOTE_CHAR);
    }

    @Override
    public String tbUnquoteTableName(String quotedTableName) {
        // 移除双引号、反引号、单引号（兼容多种输入格式）
        if (StrUtil.isEmpty(quotedTableName)) {
            return quotedTableName;
        }
        return quotedTableName.replaceAll("^[\"'`]|[\"'`]$", "");
    }

    @Override
    public String tbUnquoteSchemaName(String quotedSchemaName) {
        // 逻辑同表名去引号（复用规则）
        if (StrUtil.isEmpty(quotedSchemaName)) {
            return quotedSchemaName;
        }
        return quotedSchemaName.replaceAll("^[\"'`]|[\"'`]$", "");
    }


    @Override
    public boolean tbTableIsSqlView(String tableName) {
        String trim = StrUtil.trim(tableName);
        String lowerCase = trim.toLowerCase();
        if (lowerCase.startsWith("select") || lowerCase.startsWith("with")
        ) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String tbGetTempAliasTableName() {
        return "t_" + IdUtil.simpleUUID().substring(0, TEMP_TABLE_ALIAS_LENGTH);
    }

    /**
     * 移除sql的分号无关信息
     *
     * @param sqlView
     * @return
     */
    @Override
    public String tbRemoveSqlSpaces(String sqlView) {
        if (StrUtil.isEmpty(sqlView)) {
            return sqlView;
        }
        // 正则：匹配末尾的分号（可能带空格/换行），替换为空
        return sqlView.replaceAll("\\s*;\\s*$", "").trim();
    }

}
