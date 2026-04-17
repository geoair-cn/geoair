package cn.geoair.map.dynamic.adv.query.dialect.oracle;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.apo.*;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvSchemaTableTypeOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Oracle DDL操作实现类
 *
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

    // ========== 表操作差异化实现 ==========

    @Override
    protected String buildTruncateTableSql(String qualifiedTableName) {
        // Oracle TRUNCATE 不支持 RESTART IDENTITY
        return StrUtil.format("TRUNCATE TABLE {}", qualifiedTableName);
    }

    @Override
    protected String buildDropTableSql(String qualifiedTableName) {
        // Oracle 使用 PURGE 彻底删除（不进回收站）
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

        // Oracle 使用 USER_TABLES 查询
        String sql = StrUtil.format(
                "SELECT COUNT(*) AS cnt FROM USER_TABLES WHERE TABLE_NAME = UPPER('{}')",
                nameNotSchema);

        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null && row.getInt("CNT") > 0;
    }

    // ========== 字段操作差异化实现 ==========

    @Override
    public DataFieldsApo dGetColumnsByTable(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            return null;
        }

        String schemaName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String owner = (schemaName != null) ? schemaName.toUpperCase() : dataSourceGetter.getSchemaName().toUpperCase();
        String tableNameUpper = notSchemaTableName.toUpperCase();

        // Oracle 使用 USER_TAB_COLUMNS 查询字段信息
        String sql = StrUtil.format(
                "SELECT " +
                        "  COLUMN_NAME AS column_name, " +
                        "  DATA_TYPE AS udt_name, " +
                        "  DATA_LENGTH AS character_maximum_length, " +
                        "  DATA_PRECISION AS numeric_precision, " +
                        "  DATA_SCALE AS numeric_precision_radix, " +
                        "  NULLABLE AS is_nullable, " +
                        "  DATA_DEFAULT AS column_default " +
                        "FROM USER_TAB_COLUMNS " +
                        "WHERE TABLE_NAME = '{}' " +
                        "ORDER BY COLUMN_ID",
                tableNameUpper);

        List<FieldBySchemaApo> fields = getAdvBaseOpt().bSelectObjList(sql, FieldBySchemaApo.class);

        // 获取主键信息
        List<String> primaryKeys = dGetPrimaryKeys(tableName);

        for (FieldBySchemaApo field : fields) {
            field.setOriginalColumnName(field.getColumnName());
            // 设置主键标识
            if (primaryKeys.contains(field.getColumnName())) {
                field.setPrimaryKeyIs(true);
            } else {
                field.setPrimaryKeyIs(false);
            }
            // 设置是否可为空
            if ("Y".equals(field.getIsNullable())) {
                field.setIsNullable("YES");
            } else {
                field.setIsNullable("NO");
            }
        }

        DataFieldsApo dataFieldsApo = new DataFieldsApo();
        dataFieldsApo.setDataFieldList(fields);
        return dataFieldsApo;
    }

    @Override
    protected String buildAlterColumnSql(
            String qualifiedTableName, String oldColumnName, FieldBySchemaApo newField) {
        StringBuilder sqlBuilder = new StringBuilder();
        String finalColumnName = StrUtil.isEmpty(newField.getColumnName()) ? oldColumnName : newField.getColumnName();

        // Oracle 重命名字段
        if (!oldColumnName.equals(finalColumnName)) {
            sqlBuilder.append(StrUtil.format(
                    "ALTER TABLE {} RENAME COLUMN {} TO {}",
                    qualifiedTableName, oldColumnName, finalColumnName));
        }

        // Oracle 修改字段类型
        if (StrUtil.isNotEmpty(newField.getUdtName())) {
            if (sqlBuilder.length() > 0) {
                sqlBuilder.append("; ");
            }
            String dataType = newField.getUdtName();

            // 处理长度/精度
            if (StrUtil.isNotEmpty(newField.getCharacterMaximumLength()) &&
                    (dataType.contains("CHAR") || dataType.contains("VARCHAR2"))) {
                dataType = StrUtil.format("{}({})", dataType, newField.getCharacterMaximumLength());
            } else if (StrUtil.isNotEmpty(newField.getNumericPrecision()) &&
                    (dataType.contains("NUMBER") || dataType.contains("DECIMAL"))) {
                if (StrUtil.isNotEmpty(newField.getNumericPrecisionRadix())) {
                    dataType = StrUtil.format("{}({}, {})", dataType,
                            newField.getNumericPrecision(), newField.getNumericPrecisionRadix());
                } else {
                    dataType = StrUtil.format("{}({})", dataType, newField.getNumericPrecision());
                }
            }

            sqlBuilder.append(StrUtil.format(
                    "ALTER TABLE {} MODIFY {} {}", qualifiedTableName, finalColumnName, dataType));
        }

        // Oracle 修改非空约束
        if (StrUtil.isNotEmpty(newField.getIsNullable())) {
            if (sqlBuilder.length() > 0) {
                sqlBuilder.append("; ");
            }
            if ("NO".equals(newField.getIsNullable())) {
                sqlBuilder.append(StrUtil.format(
                        "ALTER TABLE {} MODIFY {} NOT NULL", qualifiedTableName, finalColumnName));
            } else {
                sqlBuilder.append(StrUtil.format(
                        "ALTER TABLE {} MODIFY {} NULL", qualifiedTableName, finalColumnName));
            }
        }

        // Oracle 修改默认值
        if (newField.getColumnDefault() != null) {
            if (sqlBuilder.length() > 0) {
                sqlBuilder.append("; ");
            }
            if ("null".equalsIgnoreCase(newField.getColumnDefault()) ||
                    "NULL".equalsIgnoreCase(newField.getColumnDefault())) {
                sqlBuilder.append(StrUtil.format(
                        "ALTER TABLE {} MODIFY {} DEFAULT NULL", qualifiedTableName, finalColumnName));
            } else {
                sqlBuilder.append(StrUtil.format(
                        "ALTER TABLE {} MODIFY {} DEFAULT {}",
                        qualifiedTableName, finalColumnName, newField.getColumnDefault()));
            }
        }

        return sqlBuilder.toString();
    }

    @Override
    protected String buildDropColumnSql(String qualifiedTableName, String columnName) {
        return StrUtil.format("ALTER TABLE {} DROP COLUMN {}", qualifiedTableName, columnName);
    }

    // ========== 主键/索引差异化实现 ==========

    @Override
    public List<String> dGetPrimaryKeys(String tableName) {
        if (StrUtil.isEmpty(tableName) || !dIsTableExists(tableName)) {
            return new ArrayList<>();
        }

        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);

        // Oracle 使用 USER_CONSTRAINTS 和 USER_CONS_COLUMNS 查询主键
        String sql = StrUtil.format(
                "SELECT COLUMN_NAME FROM USER_CONS_COLUMNS " +
                        "WHERE CONSTRAINT_NAME = ( " +
                        "  SELECT CONSTRAINT_NAME FROM USER_CONSTRAINTS " +
                        "  WHERE TABLE_NAME = UPPER('{}') AND CONSTRAINT_TYPE = 'P' " +
                        ") ORDER BY POSITION",
                notSchemaTableName);

        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<String> pks = new ArrayList<>();
        rows.forEach(row -> pks.add(row.getStr("COLUMN_NAME")));
        return pks;
    }

    @Override
    protected boolean checkConstraintExists(String tableName, String constraintName, String constraintType) {
        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        // Oracle 约束类型：P=主键, U=唯一, R=外键, C=检查
        String type = "PRIMARY KEY".equals(constraintType) ? "P" : constraintType;

        String sql = StrUtil.format(
                "SELECT CONSTRAINT_NAME FROM USER_CONSTRAINTS " +
                        "WHERE TABLE_NAME = UPPER('{}') AND CONSTRAINT_TYPE = '{}' AND CONSTRAINT_NAME = UPPER('{}')",
                notSchemaTableName, type, constraintName);

        List<GirAdvOneRow> result = getAdvBaseOpt().bSelectList(sql);
        return ObjectUtil.isNotEmpty(result);
    }

    @Override
    protected String buildAddPrimaryKeySql(String qualifiedTableName, String constraintName, String columns) {
        return StrUtil.format(
                "ALTER TABLE {} ADD CONSTRAINT {} PRIMARY KEY ({})",
                qualifiedTableName, constraintName, columns);
    }

    @Override
    protected String buildDropPrimaryKeySql(String qualifiedTableName, String constraintName) {
        return StrUtil.format("ALTER TABLE {} DROP CONSTRAINT {}", qualifiedTableName, constraintName);
    }

    @Override
    protected String buildCreateIndexSql(
            String qualifiedTableName, String indexName, String columns, boolean isUnique) {
        // Oracle 索引名需要唯一，建议加前缀
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

        // Oracle 查询索引
        String sql = StrUtil.format(
                "SELECT INDEX_NAME as indexname, TABLE_NAME as tablename, UNIQUENESS as indexdef " +
                        "FROM USER_INDEXES WHERE TABLE_NAME = UPPER('{}')",
                notSchemaTableName);

        return getAdvBaseOpt().bSelectObjList(sql, IndexApo.class);
    }

    @Override
    public boolean dIndexesExists(String tableName, String indexName) {
        List<IndexApo> indexes = dGetIndexes(tableName);
        return indexes.stream().anyMatch(idx -> idx.getIndexname().equalsIgnoreCase(indexName));
    }

    @Override
    public String dGetCurrentSchema() {
        // Oracle 获取当前用户/Schema
        String sql = "SELECT USER AS schema_name FROM DUAL";
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null ? row.getStr("schema_name") : null;
    }

    @Override
    public String dGetCurrentDataBase() {
        // Oracle 获取数据库名（需要特定权限）
        String sql = "SELECT SYS_CONTEXT('USERENV', 'DB_NAME') AS database_name FROM DUAL";
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null ? row.getStr("database_name") : null;
    }

    // ========== Schema/模式差异化实现 ==========

    @Override
    public List<String> dGetAllSchemas() {
        // Oracle 查询所有用户（Schema）
        String sql = "SELECT USERNAME AS schema_name FROM ALL_USERS ORDER BY USERNAME";
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<String> schemas = new ArrayList<>();
        rows.forEach(row -> schemas.add(row.getStr("schema_name")));
        return schemas;
    }

    @Override
    public String dGetTableComment(String tableName) {
        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        // Oracle 查询表注释
        String sql = StrUtil.format(
                "SELECT COMMENTS FROM USER_TAB_COMMENTS WHERE TABLE_NAME = UPPER('{}')",
                notSchemaTableName);
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row == null ? "" : row.getStr("COMMENTS");
    }

    @Override
    public List<String> dGetTablesBySchema(String schemaName) {
        String owner = (schemaName != null) ? schemaName.toUpperCase() : dataSourceGetter.getSchemaName().toUpperCase();

        String sql = StrUtil.format(
                "SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = '{}' ORDER BY TABLE_NAME",
                owner);

        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<String> tables = new ArrayList<>();
        rows.forEach(row -> tables.add(row.getStr("TABLE_NAME")));
        return tables;
    }

    @Override
    public List<String> dGetTablesBySchema() {
        return dGetTablesBySchema(null);
    }

    @Override
    public List<SchemaTableApo> dGetTableAndViewBySchema(String schemaName) {
        String owner = (schemaName != null) ? schemaName.toUpperCase() : dataSourceGetter.getSchemaName().toUpperCase();

        // Oracle 查询表和视图
        String sql = StrUtil.format(
                "SELECT OBJECT_NAME, OBJECT_TYPE FROM ALL_OBJECTS " +
                        "WHERE OWNER = '{}' AND OBJECT_TYPE IN ('TABLE', 'VIEW') " +
                        "ORDER BY OBJECT_NAME",
                owner);

        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<SchemaTableApo> result = new ArrayList<>();

        for (GirAdvOneRow row : rows) {
            SchemaTableApo apo = new SchemaTableApo();
            apo.setName(row.getStr("OBJECT_NAME"));
            apo.setSchema(owner);
            String objectType = row.getStr("OBJECT_TYPE");
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
                "SELECT USERNAME FROM ALL_USERS WHERE USERNAME = UPPER('{}')",
                schemaName);
        return ObjectUtil.isNotEmpty(getAdvBaseOpt().bSelectList(sql));
    }

    @Override
    protected String buildCreateSchemaSql(String schemaName) {
        // Oracle 创建用户即创建Schema
        return StrUtil.format("CREATE USER {} IDENTIFIED BY {}",
                schemaName, schemaName + "_pwd");
    }

    @Override
    protected String buildDropSchemaSql(String schemaName, boolean cascade) {
        return StrUtil.format("DROP USER {} {}", schemaName, cascade ? "CASCADE" : "");
    }

    // ========== 表大小差异化实现 ==========

    @Override
    public Long dGetTableSize(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            return null;
        }

        String schemaName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String owner = (schemaName != null) ? schemaName.toUpperCase() : dataSourceGetter.getSchemaName().toUpperCase();
        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName).toUpperCase();

        // Oracle 查询表大小（字节）
        String sql = StrUtil.format(
                "SELECT SUM(BYTES) AS table_size FROM USER_SEGMENTS " +
                        "WHERE SEGMENT_NAME = UPPER('{}') AND SEGMENT_TYPE = 'TABLE'",
                notSchemaTableName);

        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null ? row.getLong("TABLE_SIZE") : null;
    }

    // ========== 元数据差异化实现 ==========

    @Override
    protected String buildMetadataQuerySql(String sqlView) {
        // Oracle 使用 ROWNUM 0 获取元数据
        return StrUtil.format("SELECT * FROM ({}) WHERE ROWNUM = 0", sqlView);
    }

    @Override
    protected String getBaseColumnName(ResultSetMetaData metaData, int columnIndex)
            throws SQLException {
        // Oracle 直接返回列名
        return metaData.getColumnName(columnIndex);
    }

    @Override
    protected String getColumnTypeName(ResultSetMetaData metaData, int columnIndex)
            throws SQLException {
        return metaData.getColumnTypeName(columnIndex);
    }

    @Override
    protected void setFieldLengthInfo(
            ResultSetMetaData metaData, int columnIndex, FieldBySchemaApo field)
            throws SQLException {
        String columnTypeName = field.getUdtName();
        if (columnTypeName == null) {
            return;
        }

        if (columnTypeName.contains("CHAR") || columnTypeName.contains("VARCHAR2")) {
            field.setCharacterMaximumLength(String.valueOf(metaData.getColumnDisplaySize(columnIndex)));
        } else if (columnTypeName.contains("NUMBER")) {
            field.setNumericPrecision(String.valueOf(metaData.getPrecision(columnIndex)));
            field.setNumericPrecisionRadix(String.valueOf(metaData.getScale(columnIndex)));
        }
    }

    @Override
    public boolean dIsFunctionExists(String functionName) {
        if (StrUtil.isEmpty(functionName)) {
            return false;
        }

        String nameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(functionName);
        String schemaName = dialectTableNameProcessor.tbExtractSchemaName(functionName);
        String owner = (schemaName != null) ? schemaName.toUpperCase() : dataSourceGetter.getSchemaName().toUpperCase();

        // Oracle 查询函数是否存在
        String sql = StrUtil.format(
                "SELECT COUNT(*) AS cnt FROM ALL_OBJECTS " +
                        "WHERE OBJECT_NAME = UPPER('{}') AND OWNER = '{}' AND OBJECT_TYPE = 'FUNCTION'",
                nameNotSchema, owner);

        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null && row.getInt("CNT") > 0;
    }

    // ========== 添加主键方法（Oracle 使用 SEQUENCE） ==========

    @Override
    public void dAddPrimaryKey(
            String tableName,
            String pkColumnName,
            String constraintName,
            PrimaryKeyType pkType,
            Integer pkColumnLength,
            String pkValuePrefix) {

        // 基础参数校验
        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(pkColumnName) || pkType == null) {
            throw new IllegalArgumentException("表名、主键列名、主键类型不能为空");
        }
        if (!dIsTableExists(tableName)) {
            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法添加主键", tableName));
        }

        // 检查是否已存在主键
        List<String> existingPk = dGetPrimaryKeys(tableName);
        if (ObjectUtil.isNotEmpty(existingPk)) {
            throw new RuntimeException(StrUtil.format("表[{}]已存在主键[{}]，无法重复添加", tableName, String.join(",", existingPk)));
        }

        // 生成约束名
        String pkConstraintName = StrUtil.isEmpty(constraintName)
                ? StrUtil.format("PK_{}_{}", tableName, System.currentTimeMillis())
                : constraintName;

        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sequenceName = StrUtil.format("SEQ_{}", tableName.toUpperCase());

        try {
            // 字符串类型主键
            if (PrimaryKeyType.STRING.equals(pkType)) {
                if (pkColumnLength == null) {
                    throw new IllegalArgumentException("字符串主键必须指定列长度");
                }
                // 新增字符串列
                String addColumnSql = StrUtil.format(
                        "ALTER TABLE {} ADD {} VARCHAR2({})",
                        qualifiedTableName, pkColumnName, pkColumnLength);
                dExecuteDDL(addColumnSql, tableName, "新增字符串主键列[" + pkColumnName + "]");

                // 添加主键约束
                String addPkSql = buildAddPrimaryKeySql(qualifiedTableName, pkConstraintName, pkColumnName);
                dExecuteDDL(addPkSql, tableName, "添加字符串主键约束[" + pkConstraintName + "]");
            }
            // 整数自增主键（Oracle 使用 SEQUENCE + TRIGGER）
            else if (PrimaryKeyType.INT_AUTO.equals(pkType) || PrimaryKeyType.BIGINT_AUTO.equals(pkType)) {
                String dataType = PrimaryKeyType.INT_AUTO.equals(pkType) ? "NUMBER(10)" : "NUMBER(19)";

                // 1. 新增主键列
                String addColumnSql = StrUtil.format(
                        "ALTER TABLE {} ADD {} {} NOT NULL",
                        qualifiedTableName, pkColumnName, dataType);
                dExecuteDDL(addColumnSql, tableName, "新增主键列[" + pkColumnName + "]");

                // 2. 创建序列
                String createSequenceSql = StrUtil.format(
                        "CREATE SEQUENCE {} START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE",
                        sequenceName);
                dExecuteDDL(createSequenceSql, tableName, "创建序列[" + sequenceName + "]");

                // 3. 添加主键约束
                String addPkSql = buildAddPrimaryKeySql(qualifiedTableName, pkConstraintName, pkColumnName);
                dExecuteDDL(addPkSql, tableName, "添加主键约束[" + pkConstraintName + "]");

                // 4. 创建触发器自动填充主键值
                String triggerName = StrUtil.format("TRG_{}", tableName.toUpperCase());
                String createTriggerSql = StrUtil.format(
                        "CREATE OR REPLACE TRIGGER {} \n" +
                                "BEFORE INSERT ON {} \n" +
                                "FOR EACH ROW \n" +
                                "BEGIN \n" +
                                "  IF :NEW.{} IS NULL THEN \n" +
                                "    SELECT {}.NEXTVAL INTO :NEW.{} FROM DUAL; \n" +
                                "  END IF; \n" +
                                "END;",
                        triggerName, qualifiedTableName, pkColumnName, sequenceName, pkColumnName);
                dExecuteDDL(createTriggerSql, tableName, "创建触发器[" + triggerName + "]");
            }
            // 普通整数主键（非自增）
            else if (PrimaryKeyType.INT_NORMAL.equals(pkType) || PrimaryKeyType.BIGINT_NORMAL.equals(pkType)) {
                String dataType = PrimaryKeyType.INT_NORMAL.equals(pkType) ? "NUMBER(10)" : "NUMBER(19)";

                // 1. 新增主键列
                String addColumnSql = StrUtil.format(
                        "ALTER TABLE {} ADD {} {} NOT NULL",
                        qualifiedTableName, pkColumnName, dataType);
                dExecuteDDL(addColumnSql, tableName, "新增主键列[" + pkColumnName + "]");

                // 2. 添加主键约束
                String addPkSql = buildAddPrimaryKeySql(qualifiedTableName, pkConstraintName, pkColumnName);
                dExecuteDDL(addPkSql, tableName, "添加主键约束[" + pkConstraintName + "]");
            }
        } catch (Exception e) {
            throw new RuntimeException(StrUtil.format("给表[{}]添加主键失败：{}", tableName, e.getMessage()), e);
        }
    }
}
