package cn.geoair.map.dynamic.adv.query.dialect.oracle;

import cn.geoair.base.Gir;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.apo.*;
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
 * Oracle DDL操作实现类
 * @author zhangjun
 */
public class OracleAdvDDLOpt extends AbstractExecAdvDDLOpt {

    public OracleAdvDDLOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt) {
        super(dataSourceGetter, baseOpt);
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return OracleDialectTableNameUtil.getInstance();
    }

    @Override
    protected DialectName getDialectName() {
        return DialectName.ORACLE;
    }

    // ========== 表操作 ==========

    @Override
    protected String buildTruncateTableSql(String qualifiedTableName) {
        return StrUtil.format("TRUNCATE TABLE {}", qualifiedTableName);
    }

    @Override
    protected String buildDropTableSql(String qualifiedTableName) {
        return StrUtil.format("DROP TABLE {} PURGE", qualifiedTableName);
    }

    @Override
    protected String buildRenameTableSql(String oldQualifiedName, String newQualifiedName) {
        return StrUtil.format("RENAME {} TO {}", oldQualifiedName, newQualifiedName);
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
        schemaName = (schemaName == null) ? dataSourceGetter.getSchemaName() : schemaName;


        String sql = StrUtil.format(
                "SELECT COUNT(*) AS \"cnt\" FROM ALL_TABLES WHERE OWNER = UPPER('{}') AND TABLE_NAME = '{}'",
                schemaName, nameNotSchema);

        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null && row.getInt("cnt") > 0;
    }

    // ========== 字段操作 ==========

    @Override
    public DataFieldsApo dGetColumnsByTable(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            return null;
        }

        String schemaName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String owner = (schemaName != null) ? schemaName.toUpperCase() : dataSourceGetter.getSchemaName().toUpperCase();
        String tableNameUpper = notSchemaTableName ;

        // 关键修复：关联 ALL_COL_COMMENTS 查询字段注释
        String sql = StrUtil.format(
                "SELECT " +
                        "  col.COLUMN_NAME AS \"column_name\", " +
                        "  col.DATA_TYPE AS \"udt_name\", " +
                        "  col.DATA_TYPE AS \"data_type\", " +
                        "  col.DATA_LENGTH AS \"character_maximum_length\", " +
                        "  col.DATA_PRECISION AS \"numeric_precision\", " +
                        "  col.DATA_SCALE AS \"numeric_scale\", " +
                        "  col.NULLABLE AS \"is_nullable\", " +
                        "  col.DATA_DEFAULT AS \"column_default\", " +
                        "  comm.COMMENTS AS \"column_comment\" " +  // 字段注释
                        "FROM ALL_TAB_COLUMNS col " +
                        "LEFT JOIN ALL_COL_COMMENTS comm " +
                        "  ON col.OWNER = comm.OWNER " +
                        "  AND col.TABLE_NAME = comm.TABLE_NAME " +
                        "  AND col.COLUMN_NAME = comm.COLUMN_NAME " +
                        "WHERE col.OWNER = '{}' AND col.TABLE_NAME = '{}' " +
                        "ORDER BY col.COLUMN_ID",
                owner, tableNameUpper);

        List<FieldBySchemaApo> fields = getAdvBaseOpt().bSelectObjList(sql, FieldBySchemaApo.class);
        List<String> primaryKeys = dGetPrimaryKeys(tableName);

        for (FieldBySchemaApo field : fields) {
            field.setDialectName(getDialectName());
            field.setOriginalColumnName(field.getColumnName());
            field.determineGeometryFieldIs();
            field.setPrimaryKeyIs(primaryKeys.contains(field.getColumnName()));
            field.setIsNullable("Y".equals(field.getIsNullable()) ? "YES" : "NO");
        }

        DataFieldsApo dataFieldsApo = new DataFieldsApo(fields);
        return dataFieldsApo;
    }

    @Override
    protected String buildAlterColumnSql(
            String qualifiedTableName, String oldColumnName, FieldBySchemaApo newField) {
        StringBuilder sqlBuilder = new StringBuilder();
        String finalColumnName = StrUtil.isEmpty(newField.getColumnName()) ? oldColumnName : newField.getColumnName();
        String quotedFinalColumnName = dialectTableNameProcessor.tbQuoteFieldName(finalColumnName);

        if (!oldColumnName.equals(finalColumnName)) {
            sqlBuilder.append(StrUtil.format(
                    "ALTER TABLE {} RENAME COLUMN {} TO {}",
                    qualifiedTableName, oldColumnName, quotedFinalColumnName));
        }

        if (StrUtil.isNotEmpty(newField.getUdtName())) {
            if (sqlBuilder.length() > 0) sqlBuilder.append("; ");
            String dataType = newField.getUdtName();

            if (newField.getCharacterMaximumLength() != null &&
                    (dataType.contains("CHAR") || dataType.contains("VARCHAR2"))) {
                dataType = StrUtil.format("{}({})", dataType, newField.getCharacterMaximumLength());
            } else if (newField.getNumericPrecision() != null &&
                    (dataType.contains("NUMBER") || dataType.contains("DECIMAL"))) {
                if (newField.getNumericScale() != null) {
                    dataType = StrUtil.format("{}({}, {})", dataType,
                            newField.getNumericPrecision(), newField.getNumericScale());
                } else {
                    dataType = StrUtil.format("{}({})", dataType, newField.getNumericPrecision());
                }
            }

            sqlBuilder.append(StrUtil.format(
                    "ALTER TABLE {} MODIFY {} {}", qualifiedTableName, quotedFinalColumnName, dataType));
        }

        if (StrUtil.isNotEmpty(newField.getIsNullable())) {
            if (sqlBuilder.length() > 0) sqlBuilder.append("; ");
            if ("NO".equals(newField.getIsNullable())) {
                sqlBuilder.append(StrUtil.format(
                        "ALTER TABLE {} MODIFY {} NOT NULL", qualifiedTableName, quotedFinalColumnName));
            } else {
                sqlBuilder.append(StrUtil.format(
                        "ALTER TABLE {} MODIFY {} NULL", qualifiedTableName, quotedFinalColumnName));
            }
        }

        if (newField.getColumnDefault() != null) {
            if (sqlBuilder.length() > 0) sqlBuilder.append("; ");
            if ("null".equalsIgnoreCase(newField.getColumnDefault())) {
                sqlBuilder.append(StrUtil.format(
                        "ALTER TABLE {} MODIFY {} DEFAULT NULL", qualifiedTableName, quotedFinalColumnName));
            } else {
                sqlBuilder.append(StrUtil.format(
                        "ALTER TABLE {} MODIFY {} DEFAULT {}",
                        qualifiedTableName, quotedFinalColumnName, newField.getColumnDefault()));
            }
        }

        return sqlBuilder.toString();
    }

    @Override
    protected String buildDropColumnSql(String qualifiedTableName, String columnName) {
        return StrUtil.format("ALTER TABLE {} DROP COLUMN {}", qualifiedTableName, columnName);
    }

    // ========== 主键/索引 ==========

    @Override
    public List<String> dGetPrimaryKeys(String tableName) {
        if (StrUtil.isEmpty(tableName) || !dIsTableExists(tableName)) {
            return new ArrayList<>();
        }

        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String owner = (schemaName != null) ? schemaName.toUpperCase() : dataSourceGetter.getSchemaName().toUpperCase();
        String tableNameUpper = notSchemaTableName ;

        // 修复：使用 ALL_* 视图 + 指定 OWNER，跨Schema也能查到主键
        String sql = StrUtil.format(
                "SELECT COLUMN_NAME AS \"column_name\" FROM ALL_CONS_COLUMNS " +
                        "WHERE OWNER = '{}' " +
                        "  AND TABLE_NAME = '{}' " +
                        "  AND CONSTRAINT_NAME = ( " +
                        "      SELECT CONSTRAINT_NAME FROM ALL_CONSTRAINTS " +
                        "      WHERE OWNER = '{}' " +
                        "        AND TABLE_NAME = '{}' " +
                        "        AND CONSTRAINT_TYPE = 'P' " +
                        ") ORDER BY POSITION",
                owner, tableNameUpper,
                owner, tableNameUpper);

        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<String> pks = new ArrayList<>();
        rows.forEach(row -> pks.add(row.getStr("column_name")));
        return pks;
    }

    @Override
    protected boolean checkConstraintExists(String tableName, String constraintName, String constraintType) {
        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String owner = (schemaName != null) ? schemaName.toUpperCase() : dataSourceGetter.getSchemaName().toUpperCase();
        String tableNameUpper = notSchemaTableName ;
        String type = "PRIMARY KEY".equals(constraintType) ? "P" : constraintType;

        String sql = StrUtil.format(
                "SELECT CONSTRAINT_NAME AS \"constraint_name\" FROM ALL_CONSTRAINTS " +
                        "WHERE OWNER = '{}' AND TABLE_NAME = '{}' AND CONSTRAINT_TYPE = '{}' AND CONSTRAINT_NAME = '{}'",
                owner, tableNameUpper, type, constraintName);

        List<GirAdvOneRow> result = getAdvBaseOpt().bSelectList(sql);
        return ObjectUtil.isNotEmpty(result);
    }

    @Override
    protected String buildAddPrimaryKeySql(String qualifiedTableName, String constraintName, String columns) {
        String quotedColumns = dialectTableNameProcessor.tbQuoteFieldName(columns);
        return StrUtil.format(
                "ALTER TABLE {} ADD CONSTRAINT {} PRIMARY KEY ({})",
                qualifiedTableName, constraintName, quotedColumns);
    }

    @Override
    protected String buildDropPrimaryKeySql(String qualifiedTableName, String constraintName) {
        return StrUtil.format("ALTER TABLE {} DROP CONSTRAINT {}", qualifiedTableName, constraintName);
    }

    @Override
    protected String buildCreateIndexSql(
            String qualifiedTableName, String indexName, String columns, boolean isUnique) {
        return StrUtil.format(
                "CREATE {} INDEX {} ON {} ({})",
                isUnique ? "UNIQUE" : "",
                indexName,
                qualifiedTableName,
                columns);
    }

    @Override
    protected String buildDropIndexSql(String tableName, String indexName) {
        return StrUtil.format("DROP INDEX {}", indexName);
    }

    @Override
    public List<IndexApo> dGetIndexes(String tableName) {
        if (StrUtil.isEmpty(tableName) || !dIsTableExists(tableName)) {
            return ListUtil.empty();
        }

        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String owner = (schemaName != null) ? schemaName.toUpperCase() : dataSourceGetter.getSchemaName().toUpperCase();
        String tableNameUpper = notSchemaTableName ;

        // 修复：USER_INDEXES → ALL_INDEXES + 增加 OWNER 查询
        String sql = StrUtil.format(
                "SELECT " +
                        "INDEX_NAME AS \"indexname\", " +
                        "TABLE_NAME AS \"tablename\", " +
                        "UNIQUENESS AS \"indexdef\" " +
                        "FROM ALL_INDEXES " +
                        "WHERE OWNER = '{}' AND TABLE_NAME = '{}'",
                owner, tableNameUpper);

        return getAdvBaseOpt().bSelectObjList(sql, IndexApo.class);
    }

    @Override
    public boolean dIndexesExists(String tableName, String indexName) {
        List<IndexApo> indexes = dGetIndexes(tableName);
        return indexes.stream().anyMatch(idx -> idx.getIndexname().equalsIgnoreCase(indexName));
    }

    // ===================== 你重点关心的：已加双引号 =====================
    @Override
    public String dGetCurrentSchema() {
        // 双引号强制小写
        String sql = "SELECT USER AS \"schema_name\" FROM DUAL";
        AdvQueryGlobalConfig config = getConfig();
        boolean enableQueryLog = config.isEnableQueryLog();
        if (enableQueryLog) {
            config.setEnableQueryLog(false);  //  如果不关闭，就会死循环
        }
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        config.setEnableQueryLog(enableQueryLog);
        String schemaName = row.getStr("schema_name");
        Gir.log.info("从数据库获取到的schema为：【{}】", schemaName);
        return schemaName;
    }

    @Override
    public String dGetCurrentDataBase() {
        String sql = "SELECT SYS_CONTEXT('USERENV', 'DB_NAME') AS \"database_name\" FROM DUAL";
        AdvQueryGlobalConfig config = getConfig();
        boolean enableQueryLog = config.isEnableQueryLog();
        if (enableQueryLog) {
            config.setEnableQueryLog(false);  //  如果不关闭，就会死循环
        }
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        config.setEnableQueryLog(enableQueryLog);
        String databaseName = row.getStr("database_name");
        Gir.log.info("从数据库获取到的databaseName为：【{}】", databaseName);
        return databaseName;
    }

    // ========== Schema 相关 ==========

    @Override
    public List<String> dGetAllSchemas() {
        String sql = "SELECT USERNAME AS \"schema_name\" FROM ALL_USERS ORDER BY USERNAME";
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<String> schemas = new ArrayList<>();
        rows.forEach(row -> schemas.add(row.getStr("schema_name")));
        return schemas;
    }

    @Override
    public String dGetTableComment(String tableName) {
        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String sql = StrUtil.format(
                "SELECT COMMENTS AS \"comments\" FROM USER_TAB_COMMENTS WHERE TABLE_NAME = '{}'",
                notSchemaTableName);
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row == null ? "" : row.getStr("comments");
    }

    @Override
    public List<String> dGetTablesBySchema(String schemaName) {
        String owner = (schemaName != null) ? schemaName.toUpperCase() : dataSourceGetter.getSchemaName().toUpperCase();

        String sql = StrUtil.format(
                "SELECT TABLE_NAME AS \"table_name\" FROM ALL_TABLES WHERE OWNER = '{}' ORDER BY TABLE_NAME",
                owner);

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
        String owner = (schemaName != null) ? schemaName.toUpperCase() : dataSourceGetter.getSchemaName().toUpperCase();

        String sql = StrUtil.format(
                "SELECT " +
                        "OBJECT_NAME AS \"object_name\", " +
                        "OBJECT_TYPE AS \"object_type\" " +
                        "FROM ALL_OBJECTS " +
                        "WHERE OWNER = '{}' AND OBJECT_TYPE IN ('TABLE', 'VIEW') " +
                        "ORDER BY OBJECT_NAME",
                owner);

        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<SchemaTableApo> result = new ArrayList<>();

        for (GirAdvOneRow row : rows) {
            SchemaTableApo apo = new SchemaTableApo();
            apo.setName(row.getStr("object_name"));
            apo.setSchema(owner);
            String objectType = row.getStr("object_type");
            if ("TABLE".equals(objectType)) {
                apo.setType(AdvSchemaTableTypeOpt.表);
            } else if ("VIEW".equals(objectType)) {
                apo.setType(AdvSchemaTableTypeOpt.视图);
            } else {
                apo.setType(AdvSchemaTableTypeOpt.未知);
            }
            result.add(apo);
        }
        return result;
    }

    @Override
    public List<SchemaTableApo> dGetTableAndViewBySchema() {
        return dGetTableAndViewBySchema(null);
    }

    @Override
    protected boolean checkSchemaExists(String schemaName) {
        String sql = StrUtil.format(
                "SELECT USERNAME AS \"username\" FROM ALL_USERS WHERE USERNAME = UPPER('{}')",
                schemaName);
        return ObjectUtil.isNotEmpty(getAdvBaseOpt().bSelectList(sql));
    }

    @Override
    protected String buildCreateSchemaSql(String schemaName) {
        return StrUtil.format("CREATE USER {} IDENTIFIED BY {}",
                schemaName, schemaName + "_pwd");
    }

    @Override
    protected String buildDropSchemaSql(String schemaName, boolean cascade) {
        return StrUtil.format("DROP USER {} {}", schemaName, cascade ? "CASCADE" : "");
    }

    // ========== 表大小 ==========

    @Override
    public Long dGetTableSize(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            return null;
        }
        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName) ;

        String sql = StrUtil.format(
                "SELECT SUM(BYTES) AS \"table_size\" FROM USER_SEGMENTS " +
                        "WHERE SEGMENT_NAME = '{}' AND SEGMENT_TYPE = 'TABLE'",
                notSchemaTableName);

        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null ? row.getLong("table_size") : null;
    }

    // ========== 元数据 ==========

    @Override
    protected String buildMetadataQuerySql(String sqlView) {
        return StrUtil.format("SELECT * FROM ({}) WHERE ROWNUM = 0", sqlView);
    }

    @Override
    protected String getBaseColumnName(ResultSetMetaData metaData, int columnIndex) throws SQLException {
        return metaData.getColumnName(columnIndex);
    }

    @Override
    protected String getColumnTypeName(ResultSetMetaData metaData, int columnIndex) throws SQLException {
        return metaData.getColumnTypeName(columnIndex);
    }

    @Override
    protected void setFieldLengthInfo(
            ResultSetMetaData metaData, int columnIndex, FieldBySchemaApo field) throws SQLException {
        String columnTypeName = field.getUdtName();
        if (columnTypeName == null) return;

        if (columnTypeName.contains("CHAR") || columnTypeName.contains("VARCHAR2")) {
            field.setCharacterMaximumLength(metaData.getColumnDisplaySize(columnIndex));
        } else if (columnTypeName.contains("NUMBER")) {
            field.setNumericPrecision(metaData.getPrecision(columnIndex));
            field.setNumericScale(metaData.getScale(columnIndex));
        }
    }

    // ========== 函数是否存在 ==========

    @Override
    public boolean dIsFunctionExists(String functionName) {
        if (StrUtil.isEmpty(functionName)) return false;

        String nameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(functionName);
        String schemaName = dialectTableNameProcessor.tbExtractSchemaName(functionName);
        String owner = (schemaName != null) ? schemaName.toUpperCase() : dataSourceGetter.getSchemaName().toUpperCase();

        String sql = StrUtil.format(
                "SELECT COUNT(*) AS \"cnt\" FROM ALL_OBJECTS " +
                        "WHERE OBJECT_NAME = '{}' AND OWNER = '{}' AND OBJECT_TYPE = 'FUNCTION'",
                nameNotSchema, owner);

        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null && row.getInt("cnt") > 0;
    }

    // ========== 主键添加 ==========

    @Override
    public void dAddPrimaryKey(
            String tableName,
            String pkColumnName,
            String constraintName,
            PrimaryKeyType pkType,
            Integer pkColumnLength,
            String pkValuePrefix) {

        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(pkColumnName) || pkType == null) {
            throw new IllegalArgumentException("表名、主键列名、主键类型不能为空");
        }
        if (!dIsTableExists(tableName)) {
            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法添加主键", tableName));
        }

        List<String> existingPk = dGetPrimaryKeys(tableName);
        if (ObjectUtil.isNotEmpty(existingPk)) {
            throw new RuntimeException(StrUtil.format("表[{}]已存在主键[{}]，无法重复添加", tableName, String.join(",", existingPk)));
        }

        String pkConstraintName = StrUtil.isEmpty(constraintName)
                ? StrUtil.format("PK_{}_{}", tableName, System.currentTimeMillis())
                : constraintName;

        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sequenceName = StrUtil.format("SEQ_{}", tableName.toUpperCase());
        String quotedPkColumnName = dialectTableNameProcessor.tbQuoteFieldName(pkColumnName);

        try {
            if (PrimaryKeyType.STRING.equals(pkType)) {
                if (pkColumnLength == null) {
                    throw new IllegalArgumentException("字符串主键必须指定列长度");
                }
                String addColumnSql = StrUtil.format(
                        "ALTER TABLE {} ADD {} VARCHAR2({})",
                        qualifiedTableName, quotedPkColumnName, pkColumnLength);
                dExecuteDDL(addColumnSql, tableName, "新增字符串主键列[" + pkColumnName + "]");

                String addPkSql = buildAddPrimaryKeySql(qualifiedTableName, pkConstraintName, pkColumnName);
                dExecuteDDL(addPkSql, tableName, "添加字符串主键约束[" + pkConstraintName + "]");
            }
            else if (PrimaryKeyType.INT_AUTO.equals(pkType) || PrimaryKeyType.BIGINT_AUTO.equals(pkType)) {
                String dataType = PrimaryKeyType.INT_AUTO.equals(pkType) ? "NUMBER(10)" : "NUMBER(19)";

                String addColumnSql = StrUtil.format(
                        "ALTER TABLE {} ADD {} {} NOT NULL",
                        qualifiedTableName, quotedPkColumnName, dataType);
                dExecuteDDL(addColumnSql, tableName, "新增主键列[" + pkColumnName + "]");

                String createSequenceSql = StrUtil.format(
                        "CREATE SEQUENCE {} START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE",
                        sequenceName);
                dExecuteDDL(createSequenceSql, tableName, "创建序列[" + sequenceName + "]");

                String addPkSql = buildAddPrimaryKeySql(qualifiedTableName, pkConstraintName, pkColumnName);
                dExecuteDDL(addPkSql, tableName, "添加主键约束[" + pkConstraintName + "]");

                String triggerName = StrUtil.format("TRG_{}", tableName.toUpperCase());
                String createTriggerSql = StrUtil.format(
                        "CREATE OR REPLACE TRIGGER {} " +
                                "BEFORE INSERT ON {} " +
                                "FOR EACH ROW " +
                                "BEGIN " +
                                "  IF :NEW.{} IS NULL THEN " +
                                "    SELECT {}.NEXTVAL INTO :NEW.{} FROM DUAL; " +
                                "  END IF; " +
                                "END;",
                        triggerName, qualifiedTableName, quotedPkColumnName, sequenceName, quotedPkColumnName);
                dExecuteDDL(createTriggerSql, tableName, "创建触发器[" + triggerName + "]");
            }
            else if (PrimaryKeyType.INT_NORMAL.equals(pkType) || PrimaryKeyType.BIGINT_NORMAL.equals(pkType)) {
                String dataType = PrimaryKeyType.INT_NORMAL.equals(pkType) ? "NUMBER(10)" : "NUMBER(19)";

                String addColumnSql = StrUtil.format(
                        "ALTER TABLE {} ADD {} {} NOT NULL",
                        qualifiedTableName, quotedPkColumnName, dataType);
                dExecuteDDL(addColumnSql, tableName, "新增主键列[" + pkColumnName + "]");

                String addPkSql = buildAddPrimaryKeySql(qualifiedTableName, pkConstraintName, pkColumnName);
                dExecuteDDL(addPkSql, tableName, "添加主键约束[" + pkConstraintName + "]");
            }
        } catch (Exception e) {
            throw new RuntimeException(StrUtil.format("给表[{}]添加主键失败：{}", tableName, e.getMessage()), e);
        }
    }
    @Override
    protected String buildCreateTableFromTableSql(String dstTableName, String srcTableName) {
        // Oracle: CREATE TABLE target AS SELECT * FROM source
        return StrUtil.format("CREATE TABLE {} AS SELECT * FROM {}",
                dstTableName, srcTableName);
    }

    @Override
    protected String buildCreateTableLikeSql(String dstTableName, String srcTableName) {
        // Oracle: CREATE TABLE target AS SELECT * FROM source WHERE 1=0
        return StrUtil.format("CREATE TABLE {} AS SELECT * FROM {} WHERE 1=0",
                dstTableName, srcTableName);
    }

    @Override
    protected String buildCreateTableFromSqlSql(String dstTableName, String sql) {
        // Oracle: CREATE TABLE target AS (SELECT ...)
        return StrUtil.format("CREATE TABLE {} AS ({})",
                dstTableName, sql);
    }

    @Override
    protected String buildCreateTableFromSqlWithNoDataSql(String dstTableName, String sql) {
        // Oracle: CREATE TABLE target AS (SELECT ...) WHERE 1=0
        return StrUtil.format("CREATE TABLE {} AS ({}) WHERE 1=0",
                dstTableName, sql);
    }
}
