package cn.geoair.map.dynamic.adv.query;

import cn.geoair.map.dynamic.adv.query.apo.*;

import java.util.List;

/**
 * DDL操作接口 约定：所有的方法都以 d 开头
 */
public interface IAdvDDLOpt extends IAdvConfigOpt {

    /**
     * 执行DDL语句的通用方法
     *
     * @param sql       待执行的sql
     * @param tableName 表名
     * @param operation 用于日志记录
     */
    int dExecuteDDL(String sql, String tableName, String operation);

    /**
     * 执行DDL语句的通用方法
     *
     * @param sqlStatement 需要解析mybatis占位符的sql
     * @param sqlParam     参数列表
     * @param tableName    表名
     * @param operation    用于日志记录
     */
    int dExecuteDDL(String sqlStatement, SqlParamMap sqlParam, String tableName, String operation);

    /**
     * 删除表中所有数据
     *
     * @param tableNameWithSchema 表名
     */
    void dDelTable(String tableNameWithSchema);

    /**
     * 根据源表名复制表结构（可选是否同步数据）
     *
     * @param dstTableName 目标表名（支持schema限定，如：public.target_table）
     * @param srcTableName 源表名（支持schema限定，如：public.source_table）
     * @param dataSync 是否同步数据
     *                 - true: 复制表结构及所有数据
     *                 - false: 仅复制表结构（不含数据）
     *
     * 注意事项：
     * 1. 如果目标表已存在，操作会失败（除非使用 IF NOT EXISTS）
     * 2. 复制表结构时使用 INCLUDING ALL，会包含：索引、约束、默认值、注释等
     * 3. 主键约束不会被复制，需要单独处理
     * 4. 跨schema复制时，需要确保源表可访问、目标schema存在
     */
    void dCopyTableByTableName(String dstTableName, String srcTableName, boolean dataSync);

    /**
     * 根据自定义SQL查询结果复制表结构（可选是否同步数据）
     *
     * @param dstTableName 目标表名（支持schema限定，如：public.target_table）
     * @param sql 源数据查询SQL（用于定义表结构和数据来源）
     *            例如：SELECT id, name, created_time FROM users WHERE status = 'active'
     * @param dataSync 是否同步数据
     *                 - true: 创建表并插入查询结果数据
     *                 - false: 仅根据查询结果创建表结构（不含数据）
     *
     * 注意事项：
     * 1. 表结构由查询SQL的返回字段决定
     * 2. 目标表会自动创建，但不会包含源表的索引、约束等信息
     * 3. 可以通过SQL的WHERE条件筛选需要同步的数据
     * 4. 如果dataSync=false，只会创建空表结构
     * 5. 目标表已存在时会失败，建议先检查或先删除
     *
     * 使用示例：
     * // 复制活跃用户数据
     * copyTableBySql("public.active_users",
     *                "SELECT * FROM users WHERE status = 'active'",
     *                true);
     *
     * // 仅创建表结构
     * copyTableBySql("public.user_backup",
     *                "SELECT * FROM users  ",
     *                false);
     */
    void dCopyTableBySql(String dstTableName, String sql, boolean dataSync);

    /**
     * 清空表数据（通常比DELETE效率高）
     *
     * @param tableNameWithSchema 表名
     */
    void dTruncateTable(String tableNameWithSchema);

    /**
     * 删除表（包括表结构）
     *
     * @param tableNameWithSchema 表名
     */
    void dDropTable(String tableNameWithSchema);

    /**
     * 从数据库获取当前模式名称
     *
     * @return 模式名称
     */
    String dGetCurrentSchema();

    /**
     * 获取当前数据库名称
     *
     * @return 数据库名称
     */
    String dGetCurrentDataBase();

    /**
     * 查询所有数据库模式（schema）
     *
     * @return 模式名称列表
     */
    List<String> dGetAllSchemas();

    String dGetTableComment(String tableName);

    /**
     * 查询表的字段信息
     *
     * @param tableName 表名
     * @return 字段信息对象
     */
    DataFieldsApo dGetColumnsByTable(String tableName);

    /**
     * 通过SQL视图查询字段信息
     *
     * @param sqlView SQL视图语句
     * @return 字段信息对象
     */
    DataFieldsApo dGetColumnsBySQL(String sqlView);


    DataFieldsApo dGetColumnsBySQL(String sqlStatement, GirSqlParam sqlParam);

    /**
     * 通过SQL视图查询字段信息
     *
     * @param tbNameOrSql 表名或SQL视图语句
     * @return 字段信息对象
     */
    DataFieldsApo dGetColumnsBySQLOrTable(String tbNameOrSql);

    /**
     * 创建表
     *
     * @param tableName  表名
     * @param fields     字段定义列表
     * @param primaryKey 主键字段名
     */
    void dCreateTable(String tableName, List<FieldBySchemaApo> fields, String primaryKey);

    /**
     * 重命名表
     *
     * @param oldTableName 原表名
     * @param newTableName 新表名
     */
    void dRenameTable(String oldTableName, String newTableName);

    /**
     * 添加字段
     *
     * @param tableName 表名
     * @param field     要添加的字段信息
     */
    void dAddColumn(String tableName, FieldBySchemaApo field);

    /**
     * 修改字段
     *
     * @param tableName     表名
     * @param oldColumnName 原字段名
     * @param newField      新字段信息
     */
    void dAlterColumn(String tableName, String oldColumnName, FieldBySchemaApo newField);

    /**
     * 删除字段
     *
     * @param tableName  表名
     * @param columnName 字段名
     */
    void dDropColumn(String tableName, String columnName);

    /**
     * 查询指定模式下的所有表名
     *
     * @param schemaName 模式名称，为null则查询当前模式
     * @return 表名列表
     */
    List<String> dGetTablesBySchema(String schemaName);

    /**
     * 查询当前模式下的所有表名
     *
     * @return 表名列表
     */
    List<String> dGetTablesBySchema();

    /**
     * 查询当前模式下的所有表名,视图名称
     *
     * @return 表名列表
     */
    List<SchemaTableApo> dGetTableAndViewBySchema(String schemaName);

    /**
     * 查询当前模式下的所有表名,视图名称
     *
     * @return 表名列表
     */
    List<SchemaTableApo> dGetTableAndViewBySchema();

    /**
     * 判断表是否存在
     *
     * @param tableName 表名
     * @return 存在返回true，否则返回false
     */
    boolean dIsTableExists(String tableName);

    /**
     * 判断函数是否存在
     *
     * @param functionName 函数名称
     * @return 存在返回true，否则返回false
     */
    boolean dIsFunctionExists(String functionName);

    /**
     * 创建模式（schema）
     *
     * @param schemaName 模式名称
     */
    void dCreateSchema(String schemaName);

    /**
     * 删除模式（schema）
     *
     * @param schemaName 模式名称
     * @param cascade    是否级联删除模式下的对象
     */
    void dDropSchema(String schemaName, boolean cascade);

    /**
     * 添加主键，针对已有的列添加
     *
     * @param tableName      表名
     * @param columnNames    主键字段列表
     * @param constraintName 约束名称，为null则自动生成
     */
    void dAddPrimaryKey(String tableName, List<String> columnNames, String constraintName);

    enum PrimaryKeyType {

        /**
         * 字符串类型主键（自定义前缀/时间戳+序号）
         */
        STRING,
        /**
         * 整数自增主键（SERIAL，PostgreSQL 10-）
         */
        INT_AUTO,
        /**
         * 长整数自增主键（BIGSERIAL/IDENTITY，PostgreSQL 12+ 推荐）
         */
        BIGINT_AUTO,
        /**
         * 普通整数主键（非自增，仅INT类型，需手动填充唯一值）
         */
        INT_NORMAL,
        /**
         * 普通长整数主键（非自增，仅BIGINT类型，需手动填充唯一值）
         */
        BIGINT_NORMAL
    }

    /**
     * 给表添加主键（支持字符串/数值自增类型）
     *
     * @param tableName      表名（不含schema）
     * @param pkColumnName   主键列名（如id）
     * @param constraintName 主键约束名（可为空，为空自动生成）
     * @param pkType         主键类型（STRING/INT_AUTO/BIGINT_AUTO）
     * @param pkColumnLength 字符串主键列长度（仅STRING类型需要，如50）
     * @param pkValuePrefix  字符串主键值前缀（仅STRING类型需要，如file_，为空则用时间戳）
     */
    void dAddPrimaryKey(
            String tableName,
            String pkColumnName,
            String constraintName,
            PrimaryKeyType pkType,
            Integer pkColumnLength,
            String pkValuePrefix);

    /**
     * 简化调用：添加字符串类型主键
     */
    void dAddStringPrimaryKey(
            String tableName,
            String pkColumnName,
            int pkColumnLength,
            String constraintName,
            String pkValuePrefix);

    /**
     * 简化调用：添加整数自增主键
     */
    void dAddIntAutoPrimaryKey(String tableName, String pkColumnName, String constraintName);

    /**
     * 简化调用：添加长整数自增主键（推荐）
     */
    void dAddBigIntAutoPrimaryKey(String tableName, String pkColumnName, String constraintName);

    /**
     * 简化调用：添加普通整数主键（非自增）
     */
    void dAddIntNormalPrimaryKey(String tableName, String pkColumnName, String constraintName);

    /**
     * 简化调用：添加普通长整数主键（非自增）
     */
    void dAddBigIntNormalPrimaryKey(String tableName, String pkColumnName, String constraintName);

    /**
     * 删除主键约束
     *
     * @param tableName      表名
     * @param constraintName 约束名称
     */
    void dDropPrimaryKey(String tableName, String constraintName);

    /**
     * 添加索引
     *
     * @param tableName   表名
     * @param indexName   索引名称
     * @param columnNames 索引字段列表
     * @param isUnique    是否唯一索引
     */
    void dCreateIndex(
            String tableName, String indexName, List<String> columnNames, boolean isUnique);

    /**
     * 删除索引
     *
     * @param tableName 表名
     * @param indexName 索引名称
     */
    void dDropIndex(String tableName, String indexName);

    /**
     * 获取表的主键信息
     *
     * @param tableName 表名
     * @return 主键字段列表，无主键则返回空列表
     */
    List<String> dGetPrimaryKeys(String tableName);

    /**
     * 获取表的所有索引信息
     *
     * @param tableName 表名
     * @return 索引名称到字段列表的映射
     */
    List<IndexApo> dGetIndexes(String tableName);

    /**
     * 获取索引是否存在
     *
     * @param indexName 索引名称
     */
    boolean dIndexesExists(String tableName, String indexName);

    /**
     * 获取 PostgreSQL 表的总大小（含数据、索引、TOAST 数据），返回人类可读格式（如 10 MB、2.5 GB）
     *
     * @param tableName 表名（支持带 Schema，如 "public.a_sql"、test1."TC_ADDRESS_武汉_测试坐标转换"）
     * @return 表总大小（人类可读格式），若表不存在或查询失败抛出异常
     * @throws RuntimeException 表不存在或参数非法异常
     */
    String dGetTableSizeFormat(String tableName);

    Long dGetTableSize(String tableName);
}
