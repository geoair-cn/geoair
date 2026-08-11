package cn.geoair.map.dynamic.adv.query.dialect.mysql;

import cn.geoair.base.Gir;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.apo.IndexApo;
import cn.geoair.map.dynamic.adv.query.apo.SchemaTableApo;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvSchemaTableTypeOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.dialect.DialectName;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL DDL操作实现类 仅实现MySQL专属的差异化逻辑，复用抽象父类的所有通用DDL逻辑
 */
public class MysqlAdvDDLOpt extends AbstractExecAdvDDLOpt {


    public MysqlAdvDDLOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt) {
        super(dataSourceGetter, baseOpt);

    }

    @Override
    public DialectTableNameProcessor getDialectTableNameProcessor() {
        return MysqlDialectTableNameUtil.getInstance();
    }

    @Override
    protected DialectName getDialectName() {
        return DialectName.MYSQL;
    }

    // ========== 表操作差异化实现 ==========
    @Override
    public String buildTruncateTableSql(String qualifiedTableName) {
        // MySQL专属：TRUNCATE语法（无RESTART IDENTITY，自动重置自增）
        return StrUtil.format("TRUNCATE TABLE {}", qualifiedTableName);
    }

    @Override
    public String buildDropTableSql(String qualifiedTableName) {
        return StrUtil.format("DROP TABLE IF EXISTS {}", qualifiedTableName);
    }

    @Override
    public String buildRenameTableSql(String oldQualifiedName, String newQualifiedName) {
        // MySQL专属：RENAME TABLE语法
        return StrUtil.format("RENAME TABLE {} TO {}", oldQualifiedName, newQualifiedName);
    }

    @Override
    public boolean dIsTableExists(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            return false;
        }
        if (dialectTableNameProcessor.tbTableIsSqlView(tableName)) {
            return false;
        }

        String nameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        schemaName = schemaName == null ? dataSourceGetter.getSchemaName() : schemaName;

        // MySQL专属：表存在性检查（INFORMATION_SCHEMA.TABLES）
        String sql =
                StrUtil.format(
                        "SELECT COUNT(*) AS cnt FROM information_schema.tables "
                                + "WHERE table_name = '{}' AND table_type = 'BASE TABLE'",
                        nameNotSchema);
        if (StrUtil.isNotEmpty(schemaName)) {
            sql += StrUtil.format(" AND table_schema = '{}'", schemaName);
        }

        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null && row.getInt("cnt") > 0;
    }

    // ========== 字段操作差异化实现 ==========
    @Override
    public DataFieldsApo dGetColumnsByTable(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            return null;
        }
        String schemaNameBySQL = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String schemaName =
                ObjectUtil.isNotEmpty(schemaNameBySQL)
                        ? schemaNameBySQL
                        : dataSourceGetter.getSchemaName();
        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);

        // MySQL专属：字段元数据查询（INFORMATION_SCHEMA.COLUMNS）
        String sql =
                StrUtil.format(
                        "SELECT "
                                + "c.*, "
                                + "c.data_type as udt_name, "
                                + "COLUMN_COMMENT AS column_comment, "
                                + "CASE WHEN kcu.column_name IS NOT NULL THEN 't' ELSE 'f' END AS primary_key_is "
                                + "FROM information_schema.columns c "
                                + "LEFT JOIN information_schema.key_column_usage kcu "
                                + "ON c.table_schema = kcu.table_schema "
                                + "AND c.table_name = kcu.table_name "
                                + "AND c.column_name = kcu.column_name "
                                + "AND kcu.constraint_name = 'PRIMARY' "
                                + "WHERE c.table_name = '{}'",
                        notSchemaTableName);
        if (StrUtil.isNotEmpty(schemaName)) {
            sql += StrUtil.format(" AND c.table_schema = '{}'", schemaName);
        }

        List<FieldBySchemaApo> fields = getAdvBaseOpt().bSelectObjList(sql, FieldBySchemaApo.class);
        fields.forEach(f -> {
            f.setDialectName(getDialectName());
            f.setOriginalColumnName(f.getColumnName());
        });

        DataFieldsApo dataFieldsApo = new DataFieldsApo();
        dataFieldsApo.setDataFieldList(fields);
        return dataFieldsApo;
    }

    @Override
    public String buildAlterColumnSql(
            String qualifiedTableName, String oldColumnName, FieldBySchemaApo newField) {
        StringBuilder sqlBuilder = new StringBuilder();
        String finalColumnName =
                StrUtil.isEmpty(newField.getColumnName())
                        ? oldColumnName
                        : newField.getColumnName();
        String quotedFinalColumnName = dialectTableNameProcessor.tbQuoteFieldName(finalColumnName);

        // MySQL专属：修改字段语法（ALTER COLUMN → MODIFY COLUMN）
        StringBuilder alterDef = new StringBuilder();
        alterDef.append(
                StrUtil.format(
                        "ALTER TABLE {} MODIFY COLUMN {} {}",
                        qualifiedTableName,
                        quotedFinalColumnName,
                        newField.getUdtName()));

        // 处理长度/精度
        if (newField.getCharacterMaximumLength() != null
                && (newField.getUdtName().contains("char")
                || newField.getUdtName().contains("varchar"))) {
            alterDef.append(StrUtil.format("({})", newField.getCharacterMaximumLength()));
        } else if (newField.getNumericPrecision() != null
                && newField.getNumericScale() != null
                && (newField.getUdtName().contains("numeric")
                || newField.getUdtName().contains("decimal"))) {
            alterDef.append(
                    StrUtil.format(
                            "({}, {})",
                            newField.getNumericPrecision(),
                            newField.getNumericScale()));
        }

        // 处理非空
        if ("NO".equals(newField.getIsNullable())) {
            alterDef.append(" NOT NULL");
        } else {
            alterDef.append(" NULL");
        }

        // 处理默认值
        if (StrUtil.isNotEmpty(newField.getColumnDefault())) {
            alterDef.append(" DEFAULT ").append(newField.getColumnDefault());
        } else {
            alterDef.append(" DEFAULT NULL");
        }

        // MySQL专属：重名字段（单独语句）
        if (!oldColumnName.equals(finalColumnName)) {
            sqlBuilder.append(
                    StrUtil.format(
                            "ALTER TABLE {} RENAME COLUMN {} TO {};",
                            qualifiedTableName,
                            oldColumnName,
                            quotedFinalColumnName));
        }

        sqlBuilder.append(alterDef).append(";");
        return sqlBuilder.toString();
    }

    @Override
    public String buildDropColumnSql(String qualifiedTableName, String columnName) {
        return StrUtil.format(
                "ALTER TABLE {} DROP COLUMN IF EXISTS {}", qualifiedTableName, columnName);
    }

    // ========== 主键/索引差异化实现 ==========
    @Override
    public List<String> dGetPrimaryKeys(String tableName) {
        if (StrUtil.isEmpty(tableName) || !dIsTableExists(tableName)) {
            return new ArrayList<>();
        }
        String schemaName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        schemaName = schemaName == null ? dataSourceGetter.getSchemaName() : schemaName;
        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);

        // MySQL专属：主键查询
        String sql =
                StrUtil.format(
                        "SELECT COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE "
                                + "WHERE TABLE_SCHEMA = '{}' AND TABLE_NAME = '{}' AND CONSTRAINT_NAME = 'PRIMARY'",
                        schemaName,
                        notSchemaTableName);

        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<String> pks = new ArrayList<>();
        rows.forEach(row -> pks.add(row.getStr("COLUMN_NAME")));
        return pks;
    }

    @Override
    public boolean checkConstraintExists(
            String tableName, String constraintName, String constraintType) {
        String schemaName = dataSourceGetter.getSchemaName();
        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);

        // MySQL专属：约束存在性检查
        String sql =
                StrUtil.format(
                        "SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS "
                                + "WHERE TABLE_SCHEMA = '{}' AND TABLE_NAME = '{}' AND CONSTRAINT_TYPE = '{}' AND CONSTRAINT_NAME = '{}'",
                        schemaName,
                        notSchemaTableName,
                        constraintType,
                        constraintName);
        return ObjectUtil.isNotEmpty(getAdvBaseOpt().bSelectList(sql));
    }

    /**
     * MySQL 版本：给表添加主键（支持字符串/数值自增/数值非自增）
     *
     * @param tableName      表名（不含库名）
     * @param pkColumnName   主键列名（如id）
     * @param constraintName 主键约束名（MySQL 中主键约束名可省略，为空自动生成）
     * @param pkType         主键类型（STRING/INT_AUTO/BIGINT_AUTO/INT_NORMAL/BIGINT_NORMAL）
     * @param pkColumnLength 字符串主键列长度（仅STRING类型需要，如50）
     * @param pkValuePrefix  字符串主键值前缀（仅STRING类型需要，如file_，为空则用时间戳）
     */
    @Override
    public void dAddPrimaryKey(
            String tableName,
            String pkColumnName,
            String constraintName,
            PrimaryKeyType pkType,
            Integer pkColumnLength,
            String pkValuePrefix) {
        // 1. 基础参数校验
        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(pkColumnName) || pkType == null) {
            throw new IllegalArgumentException("表名、主键列名、主键类型不能为空");
        }
        if (!dIsTableExists(tableName)) {
            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法添加主键", tableName));
        }
        // 检查是否已存在主键
        List<String> existingPk = dGetPrimaryKeys(tableName);
        if (ObjectUtil.isNotEmpty(existingPk)) {
            throw new RuntimeException(
                    StrUtil.format(
                            "表[{}]已存在主键[{}]，无法重复添加", tableName, String.join(",", existingPk)));
        }

        // 2. 生成约束名（MySQL 主键约束名可选，建议统一命名）
        String pkConstraintName =
                StrUtil.isEmpty(constraintName)
                        ? StrUtil.format("pk_{}_{}", tableName, System.currentTimeMillis())
                        : constraintName;
        // 获取带库名的表名（适配MySQL多库场景，如db_name.table_name）
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);

        String quotedPkColumnName = dialectTableNameProcessor.tbQuoteFieldName(pkColumnName);

        try {
            // ========== 分支1：字符串类型主键 ==========
            if (PrimaryKeyType.STRING.equals(pkType)) {
                if (pkColumnLength == null) {
                    throw new IllegalArgumentException("字符串主键必须指定列长度");
                }
                // 步骤1：新增字符串列（VARCHAR，非空）
                String addColumnSql =
                        StrUtil.format(
                                "ALTER TABLE {} ADD COLUMN {} VARCHAR({})  ",
                                qualifiedTableName,
                                quotedPkColumnName,
                                pkColumnLength);
                dExecuteDDL(addColumnSql, tableName, "新增字符串主键列[" + pkColumnName + "]");

                // 步骤2：填充唯一值（MySQL 用@变量替代ctid，生成连续序号）
                // 初始化序号变量
                String initVarSql = "SET @row_num = 0;";
                dExecuteDDL(initVarSql, tableName, "初始化序号变量");
                // 拼接值前缀（MySQL 用CONCAT替代||）
                String valuePrefix =
                        StrUtil.isEmpty(pkValuePrefix)
                                ? "CONCAT(DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), '_')" // 时间戳前缀
                                : "CONCAT('" + pkValuePrefix + "', '')"; // 自定义前缀
                // 填充值SQL
                String updateSql =
                        StrUtil.format(
                                "UPDATE {} SET {} = CONCAT({}, (@row_num := @row_num + 1))",
                                qualifiedTableName,
                                quotedPkColumnName,
                                valuePrefix);
                dExecuteDDL(updateSql, tableName, "填充字符串主键值[" + pkColumnName + "]");

                // 步骤3：添加主键约束
                String addPkSql =
                        buildAddPrimaryKeySql(qualifiedTableName, pkConstraintName, pkColumnName);
                dExecuteDDL(addPkSql, tableName, "添加字符串主键约束[" + pkConstraintName + "]");
            }

            // ========== 分支2：整数自增主键（MySQL  AUTO_INCREMENT） ==========
            else if (PrimaryKeyType.INT_AUTO.equals(pkType)) {
                // MySQL 自增主键：INT + AUTO_INCREMENT + 主键（一步到位）
                String addColumnSql =
                        StrUtil.format(
                                "ALTER TABLE {} ADD COLUMN {} INT NOT NULL AUTO_INCREMENT PRIMARY KEY",
                                qualifiedTableName,
                                quotedPkColumnName);
                dExecuteDDL(addColumnSql, tableName, "新增整数自增主键列[" + pkColumnName + "]");
            }

            // ========== 分支3：长整数自增主键（MySQL 推荐） ==========
            else if (PrimaryKeyType.BIGINT_AUTO.equals(pkType)) {
                // MySQL 长整数自增：BIGINT + AUTO_INCREMENT + 主键
                String addColumnSql =
                        StrUtil.format(
                                "ALTER TABLE {} ADD COLUMN {} BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY",
                                qualifiedTableName,
                                quotedPkColumnName);
                dExecuteDDL(addColumnSql, tableName, "新增长整数自增主键列[" + pkColumnName + "]");
            }

            // ========== 分支4：普通整数主键（非自增） ==========
            else if (PrimaryKeyType.INT_NORMAL.equals(pkType)) {
                // 步骤1：新增普通INT列（非空）
                String addColumnSql =
                        StrUtil.format(
                                "ALTER TABLE {} ADD COLUMN {} INT  ",
                                qualifiedTableName,
                                quotedPkColumnName);
                dExecuteDDL(addColumnSql, tableName, "新增普通整数列[" + pkColumnName + "]");

                // 步骤2：填充连续唯一值（MySQL @变量方式）
                String initVarSql = "SET @row_num = 0;";
                dExecuteDDL(initVarSql, tableName, "初始化序号变量");
                String updateSql =
                        StrUtil.format(
                                "UPDATE {} SET {} = (@row_num := @row_num + 1)",
                                qualifiedTableName,
                                quotedPkColumnName);
                dExecuteDDL(updateSql, tableName, "填充普通整数主键值[" + pkColumnName + "]");

                // 步骤3：添加主键约束
                String addPkSql =
                        buildAddPrimaryKeySql(qualifiedTableName, pkConstraintName, pkColumnName);
                dExecuteDDL(addPkSql, tableName, "添加普通整数主键约束[" + pkConstraintName + "]");
            }

            // ========== 分支5：普通长整数主键（非自增） ==========
            else if (PrimaryKeyType.BIGINT_NORMAL.equals(pkType)) {
                // 步骤1：新增普通BIGINT列（非空）
                String addColumnSql =
                        StrUtil.format(
                                "ALTER TABLE {} ADD COLUMN {} BIGINT  ",
                                qualifiedTableName,
                                quotedPkColumnName);
                dExecuteDDL(addColumnSql, tableName, "新增普通长整数列[" + pkColumnName + "]");

                // 步骤2：填充连续唯一值
                String initVarSql = "SET @row_num = 0;";
                dExecuteDDL(initVarSql, tableName, "初始化序号变量");
                String updateSql =
                        StrUtil.format(
                                "UPDATE {} SET {} = (@row_num := @row_num + 1)",
                                qualifiedTableName,
                                quotedPkColumnName);
                dExecuteDDL(updateSql, tableName, "填充普通长整数主键值[" + pkColumnName + "]");

                // 步骤3：添加主键约束
                String addPkSql =
                        buildAddPrimaryKeySql(qualifiedTableName, pkConstraintName, pkColumnName);
                dExecuteDDL(addPkSql, tableName, "添加普通长整数主键约束[" + pkConstraintName + "]");
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format("给MySQL表[{}]添加主键失败：{}", tableName, e.getMessage()), e);
        }
    }

    @Override
    public String buildAddPrimaryKeySql(
            String qualifiedTableName, String constraintName, String columns) {
        String quotedColumns = dialectTableNameProcessor.tbQuoteFieldName(columns);
        // MySQL专属：添加主键（约束名可选）
        return StrUtil.format(
                "ALTER TABLE {} ADD CONSTRAINT {} PRIMARY KEY ({})",
                qualifiedTableName,
                constraintName,
                quotedColumns);
    }

    @Override
    public String buildDropPrimaryKeySql(String qualifiedTableName, String constraintName) {
        // MySQL专属：删除主键（直接DROP PRIMARY KEY）
        return StrUtil.format("ALTER TABLE {} DROP PRIMARY KEY", qualifiedTableName);
    }

    @Override
    public String buildCreateIndexSql(
            String qualifiedTableName, String indexName, String columns, boolean isUnique) {
        return StrUtil.format(
                "CREATE {} INDEX {} ON {} ({})",
                isUnique ? "UNIQUE" : "",
                indexName,
                qualifiedTableName,
                columns);
    }

    @Override
    public String buildDropIndexSql(String tableName, String indexName) {
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        // MySQL专属：删除索引（ALTER TABLE DROP INDEX）
        return StrUtil.format("ALTER TABLE {} DROP INDEX {}", qualifiedTableName, indexName);
    }

    @Override
    public List<IndexApo> dGetIndexes(String tableName) {
        if (StrUtil.isEmpty(tableName) || !dIsTableExists(tableName)) {
            return ListUtil.empty();
        }
        String schemaName = dataSourceGetter.getSchemaName();
        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);

        // MySQL专属：索引查询（SHOW INDEX）
        String sql =
                StrUtil.format(
                        "SHOW INDEX FROM {} WHERE TABLE_SCHEMA = '{}'",
                        notSchemaTableName,
                        schemaName);
        return getAdvBaseOpt().bSelectObjList(sql, IndexApo.class);
    }

    @Override
    public boolean dIndexesExists(String tableName, String indexName) {
        List<IndexApo> indexes = dGetIndexes(tableName);
        return indexes.stream().anyMatch(idx -> idx.getIndexname().equals(indexName));
    }

    @Override
    public String dGetCurrentSchema() {
        String sql = "SELECT DATABASE() as ds ";
        AdvQueryGlobalConfig config = getConfig();
        boolean enableQueryLog = config.isEnableQueryLog();
        if (enableQueryLog) {
            config.setEnableQueryLog(false);  //  如果不关闭，就会死循环
        }
        GirAdvOneRow girAdvOneRow = getAdvBaseOpt().bSelectOne(sql);
        config.setEnableQueryLog(enableQueryLog);
        String schema = girAdvOneRow.getStr("ds");
        Gir.log.info("从数据库获取到的schema为：【{}】", schema);
        return schema;
    }

    @Override
    public String dGetCurrentDataBase() {
        return dGetCurrentSchema();
    }

    // ========== Schema/模式差异化实现 ==========
    @Override
    public List<String> dGetAllSchemas() {
        // MySQL：Schema = Database
        String sql =
                "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys') ORDER BY SCHEMA_NAME";
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<String> schemas = new ArrayList<>();
        rows.forEach(row -> schemas.add(row.getStr("SCHEMA_NAME")));
        return schemas;
    }

    @Override
    public String dGetTableComment(String tableName) {
        String schemaName = dataSourceGetter.getSchemaName();
        String sql =
                StrUtil.format(
                        "SELECT TABLE_COMMENT as tc FROM information_schema.TABLES "
                                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '{}'",
                        schemaName,
                        tableName);
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        if (ObjectUtil.isNotEmpty(rows)) {
            return rows.get(0).getStr("tc");
        }
        return "";
    }

    @Override
    public List<String> dGetTablesBySchema(String schemaName) {
        String actualSchema =
                ObjectUtil.isEmpty(schemaName) ? dataSourceGetter.getSchemaName() : schemaName;
        String sql =
                StrUtil.format(
                        "SELECT table_name FROM information_schema.tables "
                                + "WHERE table_type = 'BASE TABLE' AND table_schema = '{}' ORDER BY table_name",
                        actualSchema);
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<String> tables = new ArrayList<>();
        rows.forEach(row -> tables.add(row.getStr("table_name")));
        return tables;
    }

    @Override
    public List<String> dGetTablesBySchema() {
        return dGetTablesBySchema(null);
    }

    @Override
    public List<SchemaTableApo> dGetTableAndViewBySchema(String schemaName) {
        String actualSchema =
                ObjectUtil.isEmpty(schemaName) ? dataSourceGetter.getSchemaName() : schemaName;
        String sql;
        String fields = "table_type,table_name,table_catalog,table_schema";
        if (StrUtil.isEmpty(actualSchema)) {
            sql = "SELECT " + fields + " FROM information_schema.tables " + "ORDER BY table_name";
        } else {
            sql =
                    StrUtil.format(
                            "SELECT "
                                    + fields
                                    + " FROM information_schema.tables "
                                    + "WHERE   table_schema = '{}' ORDER BY table_name",
                            actualSchema);
        }
        List<SchemaTableApo> result = new ArrayList<>();
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);

        rows.forEach(
                row -> {
                    SchemaTableApo schemaTableApo = new SchemaTableApo();
                    schemaTableApo.setDatabaseName(row.getStr("TABLE_SCHEMA"));
                    schemaTableApo.setSchema(row.getStr("TABLE_SCHEMA"));
                    schemaTableApo.setName(row.getStr("TABLE_NAME"));
                    String tableType = row.getStr("TABLE_TYPE");
                    if (tableType.equals("BASE TABLE")) {
                        schemaTableApo.setType(AdvSchemaTableTypeOpt.表);
                    } else if (tableType.equals("VIEW")) {
                        schemaTableApo.setType(AdvSchemaTableTypeOpt.视图);
                    } else {
                        schemaTableApo.setType(AdvSchemaTableTypeOpt.未知);
                    }
                    result.add(schemaTableApo);
                });
        return result;
    }

    @Override
    public List<SchemaTableApo> dGetTableAndViewBySchema() {
        return dGetTableAndViewBySchema(null);
    }

    @Override
    public boolean checkSchemaExists(String schemaName) {
        // MySQL：Schema = Database
        String sql =
                StrUtil.format(
                        "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '{}'",
                        schemaName);
        return ObjectUtil.isNotEmpty(getAdvBaseOpt().bSelectList(sql));
    }

    @Override
    public String buildCreateSchemaSql(String schemaName) {
        // MySQL：CREATE SCHEMA = CREATE DATABASE
        return StrUtil.format("CREATE DATABASE IF NOT EXISTS {}", schemaName);
    }

    @Override
    public String buildDropSchemaSql(String schemaName, boolean cascade) {
        // MySQL：DROP SCHEMA = DROP DATABASE
        return StrUtil.format("DROP DATABASE IF EXISTS {}", schemaName);
    }

    // ========== 表大小差异化实现 ==========
    @Override
    public Long dGetTableSize(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            return null;
        }
        String schemaName = dataSourceGetter.getSchemaName();
        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);

        // MySQL专属：表大小查询（DATA_LENGTH + INDEX_LENGTH）
        String sql =
                StrUtil.format(
                        "SELECT (DATA_LENGTH + INDEX_LENGTH) AS table_size "
                                + "FROM information_schema.TABLES WHERE TABLE_SCHEMA = '{}' AND TABLE_NAME = '{}'",
                        schemaName,
                        notSchemaTableName);
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row.getLong("table_size");
    }

    // ========== 元数据差异化实现 ==========
    @Override
    public String buildMetadataQuerySql(String sqlView) {
        // MySQL专属：LIMIT 0获取元数据
        return StrUtil.format("SELECT * FROM ({}) AS temp_table LIMIT 0", sqlView);
    }

    @Override
    public String getBaseColumnName(ResultSetMetaData metaData, int columnIndex)
            throws SQLException {
        // MySQL：直接返回列名
        return metaData.getColumnName(columnIndex);
    }

    @Override
    public String getColumnTypeName(ResultSetMetaData metaData, int columnIndex)
            throws SQLException {
        // MySQL：返回列类型名
        return metaData.getColumnTypeName(columnIndex);
    }

    @Override
    public void setFieldLengthInfo(
            ResultSetMetaData metaData, int columnIndex, FieldBySchemaApo field)
            throws SQLException {
        String columnTypeName = field.getUdtName();
        if (columnTypeName == null) {
            return;
        }

        // MySQL专属：字段长度处理
        if (columnTypeName.contains("char")
                || columnTypeName.contains("varchar")
                || columnTypeName.contains("text")) {
            field.setCharacterMaximumLength(metaData.getColumnDisplaySize(columnIndex));
        } else if (columnTypeName.contains("int")
                || columnTypeName.contains("decimal")
                || columnTypeName.contains("float")
                || columnTypeName.contains("double")) {
            field.setNumericPrecision(metaData.getPrecision(columnIndex));
            field.setNumericScale(metaData.getScale(columnIndex));
        }
    }

    @Override
    public boolean dIsFunctionExists(String functionName) {
        if (StrUtil.isEmpty(functionName)) {
            return false;
        }

        String nameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(functionName);
        String schemaName = dialectTableNameProcessor.tbExtractSchemaName(functionName);
        schemaName = schemaName == null ? dataSourceGetter.getSchemaName() : schemaName;

        // MySQL专属：函数存在性检查
        String sql =
                StrUtil.format(
                        "SELECT COUNT(*) AS cnt FROM information_schema.ROUTINES "
                                + "WHERE ROUTINE_NAME = '{}' AND ROUTINE_TYPE = 'FUNCTION'",
                        nameNotSchema);
        if (StrUtil.isNotEmpty(schemaName)) {
            sql += StrUtil.format(" AND ROUTINE_SCHEMA = '{}'", schemaName);
        }

        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null && row.getInt("cnt") > 0;
    }
    @Override
    protected String buildCreateTableFromTableSql(String dstTableName, String srcTableName) {
        // MySQL: CREATE TABLE IF NOT EXISTS target SELECT * FROM source
        return StrUtil.format("CREATE TABLE IF NOT EXISTS {} SELECT * FROM {}",
                dstTableName, srcTableName);
    }

    @Override
    protected String buildCreateTableLikeSql(String dstTableName, String srcTableName) {
        // MySQL: CREATE TABLE IF NOT EXISTS target LIKE source
        return StrUtil.format("CREATE TABLE IF NOT EXISTS {} LIKE {}",
                dstTableName, srcTableName);
    }

    @Override
    protected String buildCreateTableFromSqlSql(String dstTableName, String sql) {
        // MySQL: CREATE TABLE IF NOT EXISTS target AS (SELECT ...)
        return StrUtil.format("CREATE TABLE IF NOT EXISTS {} AS ({})",
                dstTableName, sql);
    }

    @Override
    protected String buildCreateTableFromSqlWithNoDataSql(String dstTableName, String sql) {
        // MySQL: CREATE TABLE IF NOT EXISTS target AS (SELECT ...) LIMIT 0
        return StrUtil.format("CREATE TABLE IF NOT EXISTS {} AS ({}) LIMIT 0",
                dstTableName, sql);
    }
}
