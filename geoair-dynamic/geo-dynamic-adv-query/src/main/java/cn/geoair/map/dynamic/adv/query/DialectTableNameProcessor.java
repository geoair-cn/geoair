package cn.geoair.map.dynamic.adv.query;

import cn.geoair.map.dynamic.ds.IDataSourceGetter;

/**
 * 数据库方言表名处理器接口
 * 定义表名、Schema相关的通用操作规范，适配不同数据库方言实现
 * 约定 ：以tb开头
 *
 * @author 张逢吉
 * @date 2025/10/15 10:44
 */
public interface DialectTableNameProcessor {

    int TEMP_TABLE_ALIAS_LENGTH = 8;

    /**
     * 获取用于SQL的Schema名（含默认值与基础校验）
     *
     * @return 处理后的Schema名
     */
    String tbGetSchemaNameForSql(IDataSourceGetter dataSourceGetter);

    /**
     * （默认Schema）获取带Schema的完整表名
     *
     * @param tableName 原始表名（可含Schema，也可不含）
     * @return 带Schema的标准表名（如 "public"."user"）
     */
    String tbGetTableNameWithSchema(IDataSourceGetter dataSourceGetter, String tableName);

    /**
     * （指定Schema）获取带Schema的完整表名
     *
     * @param tableName  原始表名（可含Schema，也可不含）
     * @param schemaName 指定的Schema名（优先级低于表名中提取的Schema）
     * @return 带Schema的标准表名
     */
    String tbGetTableNameWithSchema(IDataSourceGetter dataSourceGetter, String tableName, String schemaName);

    /**
     * 从完整表名中提取纯表名（去除Schema与多余引号）
     *
     * @param fullTableName 含Schema的完整表名（如 "public"."user" 或 public.user）
     * @return 纯表名（如 user）
     */
    String tbGetTableNameNotSchema(String fullTableName);

    /**
     * 从完整表名中提取Schema名（去除多余引号）
     *
     * @param fullTableName 含Schema的完整表名（如 "public"."user" 或 public.user）
     * @return 提取的Schema名；无Schema时返回null
     */
    String tbExtractSchemaName(String fullTableName);

    /**
     * 给表名添加数据库方言对应的引号（如PostgreSQL的双引号）
     *
     * @param tableName 原始表名（不含引号）
     * @return 带引号的表名（如 "user"）
     */
    String tbQuoteTableName(String tableName);

    /**
     * 给Schema名添加数据库方言对应的引号（如PostgreSQL的双引号）
     *
     * @param schemaName 原始Schema名（不含引号）
     * @return 带引号的Schema名（如 "public"）
     */
    String tbQuoteSchemaName(String schemaName);

    /**
     * 移除表名中的引号（兼容不同引号格式）
     *
     * @param quotedTableName 带引号的表名（如 "user" 或 `user`）
     * @return 无引号的纯表名（如 user）
     */
    String tbUnquoteTableName(String quotedTableName);

    /**
     * 移除Schema名中的引号（兼容不同引号格式）
     *
     * @param quotedSchemaName 带引号的Schema名（如 "public" 或 `public`）
     * @return 无引号的纯Schema名（如 public）
     */
    String tbUnquoteSchemaName(String quotedSchemaName);

    /**
     * 判断表名是否对应SQL视图（基于SQL语句特征）
     *
     * @param tableName 表名或SQL视图语句
     * @return 如果是SQL视图返回true，否则返回false
     */
    boolean tbTableIsSqlView(String tableName);


    /**
     * 获取临时的别名表
     *
     * @return
     */
    String tbGetTempAliasTableName();

    /**
     * 移除sql的分号无关信息
     *
     * @param sqlView
     * @return
     */
    String tbRemoveSqlSpaces(String sqlView);
}
