package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.comp.dynamic.ds.IDsDataSourceManger;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库方言表名处理器抽象父类 封装所有数据库通用的表名/字段名处理逻辑，差异化逻辑由子类实现
 */
public abstract class AbstractExecDialectTableUtil implements DialectTableNameProcessor {

    protected static final int TEMP_TABLE_ALIAS_LENGTH = 8;

    // ========== 通用逻辑：子类无需重写 ==========
    @Override
    public String tbGetTableNameWithSchema(IDsDataSourceManger dataSourceGetter, String tableName) {
        return tbGetTableNameWithSchema(
                dataSourceGetter, tableName, dataSourceGetter.getSchemaName());
    }

    @Override
    public String tbGetTableNameWithSchema(
            IDsDataSourceManger dataSourceGetter, String tableName, String schemaName) {
        // Step1：确定最终Schema/库名（表名提取 > 传入库名 > 默认值）
        String extractedSchema = tbExtractSchemaName(tableName);
        if (StrUtil.isNotEmpty(extractedSchema)) {
            schemaName = extractedSchema;
        } else if (ObjectUtil.isEmpty(schemaName)) {
            schemaName = tbGetSchemaNameForSql(dataSourceGetter);
        }

        // Step2：处理Schema/库名引号
        String quotedSchema = tbQuoteSchemaName(schemaName);
        // Step3：处理表名（去Schema + 加引号）
        String pureTableName = tbGetTableNameNotSchema(tableName);
        String quotedTableName = tbQuoteTableName(pureTableName);

        // Step4：组合完整表名（避免重复拼接）
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


        Pattern pattern = Pattern.compile(
                "^([\"'`]?[a-zA-Z0-9_\\u4e00-\\u9fa5.-]+[\"'`]?)\\." +
                        "([\"'`]?[a-zA-Z0-9_\\u4e00-\\u9fa5.-]+[\"'`]?)$"
        );

        Matcher matcher = pattern.matcher(processedName);


        if (matcher.matches()) {
            return tbUnquoteTableName(matcher.group(2));
        }
        return tbUnquoteTableName(processedName);
    }

    @Override
    public String tbExtractSchemaName(String fullTableName) {
        if (StrUtil.isEmpty(fullTableName)) {
            return null;
        }


        String processedName = fullTableName.trim();


        Pattern pattern = Pattern.compile(
                "^([\"'`]?[a-zA-Z0-9_\\u4e00-\\u9fa5.-]+[\"'`]?)\\." +
                        "([\"'`]?[a-zA-Z0-9_\\u4e00-\\u9fa5.-]+[\"'`]?)$"
        );

        Matcher matcher = pattern.matcher(processedName);

        if (matcher.matches()) {
            return tbUnquoteSchemaName(matcher.group(1));
        }
        return null;
    }

    @Override
    public String tbQuoteTableName(String tableName) {
        // 通用加引号逻辑：避免重复加引号
        if (StrUtil.isEmpty(tableName)
                || (tableName.startsWith(getQuoteChar()) && tableName.endsWith(getQuoteChar()))) {
            return tableName;
        }
        return StrUtil.wrap(tableName, getQuoteChar());
    }

    @Override
    public String tbQuoteSchemaName(String schemaName) {
        // Schema/库名加引号：复用表名逻辑
        return tbQuoteTableName(schemaName);
    }

    @Override
    public String tbUnquoteTableName(String quotedTableName) {
        // 通用去引号逻辑：移除双引号、单引号、反引号
        if (StrUtil.isEmpty(quotedTableName)) {
            return quotedTableName;
        }
        return quotedTableName.replaceAll("^[\"'`]|[\"'`]$", "");
    }

    @Override
    public String tbUnquoteSchemaName(String quotedSchemaName) {
        // Schema/库名去引号：复用表名逻辑
        return tbUnquoteTableName(quotedSchemaName);
    }

    @Override
    public boolean tbTableIsSqlView(String tableName) {
        // 1. 空值 → 肯定不是 SQL，是表名
        if (StrUtil.isBlank(tableName)) {
            return false;
        }
        // 2. 去除首尾空格
        String trim = tableName.trim();
        // 3. 包含空格 → 一定是 SQL 表达式，不是表名
        if (trim.contains(" ")) {
            return true;
        }
        // 4. 不包含空格 → 认为是表名
        return false;
    }

    @Override
    public String tbGetTempAliasTableName() {
        return "t_" + IdUtil.simpleUUID().substring(0, TEMP_TABLE_ALIAS_LENGTH);
    }

    @Override
    public String tbRemoveSqlSpaces(String sqlView) {
        if (StrUtil.isEmpty(sqlView)) {
            return sqlView;
        }
        String cleanedSql = sqlView.replaceAll("\\s*;\\s*$", "").trim();
        // 2. 移除行内注释（-- 开头的注释），避免语法错误
        cleanedSql = cleanedSql.replaceAll("--[^\\n]*\\n", "\n");
        // 3. 移除/* */块注释（可选，根据业务需求）
        cleanedSql = cleanedSql.replaceAll("/\\*[\\s\\S]*?\\*/", " ");
        return cleanedSql;
    }

    @Override
    public  String tbBuildAsTable(String startFragment, String aliasTableName){
        return  startFragment + " as " + aliasTableName;
    }

    /**
     * 获取数据库专属的引号字符（PG："，MySQL：`）
     */
    protected abstract String getQuoteChar();

    /**
     * 获取数据库默认的Schema/库名（PG：public，MySQL：空）
     */
    protected abstract String getDefaultSchemaName();

    /**
     * 获取数据源对应的Schema/库名（适配PG/MySQL语义）
     */
    @Override
    public String tbGetSchemaNameForSql(IDsDataSourceManger dataSourceGetter) {
        return ObjectUtil.isEmpty(dataSourceGetter.getSchemaName())
                ? getDefaultSchemaName()
                : dataSourceGetter.getSchemaName();
    }
}
