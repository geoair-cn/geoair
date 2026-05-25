package cn.geoair.map.dynamic.adv.query.dialect.pg;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.apo.IndexApo;
import cn.geoair.map.dynamic.adv.query.apo.SchemaTableApo;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractPxyAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvSchemaTableTypeOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.postgresql.jdbc.PgResultSetMetaData;

/**
 * PostgreSQL DDL操作实现类
 */
public class PgAdvDDLOpt extends AbstractExecAdvDDLOpt {

    public PgAdvDDLOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt) {
        super(dataSourceGetter, baseOpt);
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return PgDialectTableNameUtil.getInstance();
    }

    // ========== 表操作差异化实现 ==========
    @Override
    protected String buildTruncateTableSql(String qualifiedTableName) {
        return StrUtil.format("TRUNCATE TABLE {} RESTART IDENTITY CASCADE", qualifiedTableName);
    }

    @Override
    protected String buildDropTableSql(String qualifiedTableName) {
        return StrUtil.format("DROP TABLE IF EXISTS {} CASCADE", qualifiedTableName);
    }

    @Override
    protected String buildRenameTableSql(String oldQualifiedName, String newQualifiedName) {
        return StrUtil.format("ALTER TABLE {} RENAME TO {}", oldQualifiedName, newQualifiedName);
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

        String sql =
                StrUtil.format(
                        "SELECT COUNT(*) AS cnt FROM information_schema.tables "
                                + "WHERE table_name = '{}' AND table_type = 'BASE TABLE'",
                        nameNotSchema);
        if (StrUtil.isNotEmpty(schemaName)) {
            sql += StrUtil.format(" AND table_schema = '{}'", schemaName);
        } else {
            sql += StrUtil.format(" AND table_schema = {}", "current_schema()");
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

        // PG专属字段元数据查询SQL
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder
                .append("SELECT ")
                .append("c.*, ")
                .append("temp.column_comment, ")
                .append("CASE WHEN pk.attnum IS NOT NULL THEN 't' ELSE 'f' END AS primary_key_is ")
                .append("FROM ")
                .append("information_schema.columns AS c ")
                .append("JOIN pg_class AS cl ON c.table_name = cl.relname ")
                .append(
                        "JOIN pg_namespace AS ns ON cl.relnamespace = ns.oid AND ns.nspname = c.table_schema ")
                .append(
                        "JOIN pg_attribute AS a ON cl.oid = a.attrelid AND c.column_name = a.attname ")
                .append("JOIN pg_type AS t ON a.atttypid = t.oid ")
                .append("LEFT JOIN ( ")
                .append("    SELECT A.attname AS COLUMN_NAME, d.description AS column_comment ")
                .append("    FROM pg_class C ")
                .append("    JOIN pg_namespace ns ON C.relnamespace = ns.oid ");
        if (ObjectUtil.isNotEmpty(schemaName)) {
            sqlBuilder.append("    AND ns.nspname = '").append(schemaName).append("' ");
        }
        sqlBuilder
                .append("    JOIN pg_attribute A ON C.OID = A.attrelid ")
                .append(
                        "    LEFT JOIN pg_description d ON C.OID = d.objoid AND A.attnum = d.objsubid ")
                .append("    WHERE C.relname = '")
                .append(notSchemaTableName)
                .append("' ")
                .append(") AS temp ON temp.COLUMN_NAME = c.column_name ")
                .append("LEFT JOIN ( ")
                .append("    SELECT unnest(conkey) AS attnum ")
                .append("    FROM pg_constraint ")
                .append("    WHERE conrelid = (SELECT oid FROM pg_class WHERE relname = '")
                .append(notSchemaTableName)
                .append("' ");
        if (ObjectUtil.isNotEmpty(schemaName)) {
            sqlBuilder
                    .append(
                            "    AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = '")
                    .append(schemaName)
                    .append("') ");
        }
        sqlBuilder
                .append(") AND contype = 'p' ")
                .append(") AS pk ON a.attnum = pk.attnum ")
                .append("WHERE c.table_name = '")
                .append(notSchemaTableName)
                .append("' ");
        if (ObjectUtil.isNotEmpty(schemaName)) {
            sqlBuilder.append("AND c.\"table_schema\" = '").append(schemaName).append("' ");
        }

        List<FieldBySchemaApo> fields =
                getAdvBaseOpt().bSelectObjList(sqlBuilder.toString(), FieldBySchemaApo.class);
        fields.forEach(f -> f.setOriginalColumnName(f.getColumnName()));

        DataFieldsApo dataFieldsApo = new DataFieldsApo();
        dataFieldsApo.setDataFieldList(fields);
        return dataFieldsApo;
    }

    @Override
    protected String buildAlterColumnSql(
            String qualifiedTableName, String oldColumnName, FieldBySchemaApo newField) {
        StringBuilder sqlBuilder = new StringBuilder();
        String finalColumnName =
                StrUtil.isEmpty(newField.getColumnName())
                        ? oldColumnName
                        : newField.getColumnName();

        // PG专属：重名字段
        if (!oldColumnName.equals(finalColumnName)) {
            sqlBuilder.append(
                    StrUtil.format(
                            "ALTER TABLE {} RENAME COLUMN {} TO {};",
                            qualifiedTableName,
                            oldColumnName,
                            finalColumnName));
        }

        // PG专属：修改字段类型/约束
        StringBuilder alterDef = new StringBuilder();
        alterDef.append(
                StrUtil.format(
                        "ALTER TABLE {} ALTER COLUMN {} TYPE {}",
                        qualifiedTableName,
                        finalColumnName,
                        newField.getUdtName()));

        // 处理长度/精度
        if (StrUtil.isNotEmpty(newField.getCharacterMaximumLength())
                && (newField.getUdtName().contains("char")
                || newField.getUdtName().contains("varchar"))) {
            alterDef.append(StrUtil.format("({})", newField.getCharacterMaximumLength()));
        } else if (StrUtil.isNotEmpty(newField.getNumericPrecision())
                && StrUtil.isNotEmpty(newField.getNumericPrecisionRadix())
                && (newField.getUdtName().contains("numeric")
                || newField.getUdtName().contains("decimal"))) {
            alterDef.append(
                    StrUtil.format(
                            "({}, {})",
                            newField.getNumericPrecision(),
                            newField.getNumericPrecisionRadix()));
        }

        // 处理非空
        if ("NO".equals(newField.getIsNullable())) {
            alterDef.append(" NOT NULL");
        } else {
            alterDef.append(" DROP NOT NULL");
        }

        // 处理默认值
        if (StrUtil.isNotEmpty(newField.getColumnDefault())) {
            alterDef.append(" SET DEFAULT ").append(newField.getColumnDefault());
        } else {
            alterDef.append(" DROP DEFAULT");
        }

        sqlBuilder.append(alterDef).append(";");
        return sqlBuilder.toString();
    }

    @Override
    protected String buildDropColumnSql(String qualifiedTableName, String columnName) {
        return StrUtil.format(
                "ALTER TABLE {} DROP COLUMN IF EXISTS {} CASCADE", qualifiedTableName, columnName);
    }

    // ========== 主键/索引差异化实现 ==========
    @Override
    public List<String> dGetPrimaryKeys(String tableName) {
        if (StrUtil.isEmpty(tableName) || !dIsTableExists(tableName)) {
            return new ArrayList<>();
        }
        tableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);

        String sql =
                StrUtil.format(
                        "SELECT kcu.column_name "
                                + "FROM information_schema.table_constraints tco "
                                + "JOIN information_schema.key_column_usage kcu "
                                + "ON tco.constraint_name = kcu.constraint_name "
                                + "WHERE tco.table_name ='{}' AND tco.constraint_type = 'PRIMARY KEY' "
                                + "ORDER BY kcu.ordinal_position",
                        tableName);
        if (StrUtil.isNotEmpty(dataSourceGetter.getSchemaName())) {
            sql += StrUtil.format(" AND tco.table_schema = '{}'", dataSourceGetter.getSchemaName());
        }

        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<String> pks = new ArrayList<>();
        rows.forEach(row -> pks.add(row.getStr("column_name")));
        return pks;
    }

    @Override
    protected boolean checkConstraintExists(
            String tableName, String constraintName, String constraintType) {
        String sql =
                StrUtil.format(
                        "SELECT constraint_name FROM information_schema.table_constraints "
                                + "WHERE table_name = '{}' AND constraint_type = '{}' AND constraint_name = '{}'",
                        tableName,
                        constraintType,
                        constraintName);
        return ObjectUtil.isNotEmpty(getAdvBaseOpt().bSelectList(sql));
    }

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

        // 2. 生成约束名
        String pkConstraintName =
                StrUtil.isEmpty(constraintName)
                        ? StrUtil.format("pk_{}_{}", tableName, System.currentTimeMillis())
                        : constraintName;
        // 获取带schema的表名
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);

        try {
            // ========== 分支1：字符串类型主键 ==========
            if (PrimaryKeyType.STRING.equals(pkType)) {
                // 字符串类型必须指定长度
                if (pkColumnLength == null) {
                    throw new IllegalArgumentException("字符串主键必须指定列长度");
                }
                // 新增字符串列
                String addColumnSql =
                        StrUtil.format(
                                "ALTER TABLE {} ADD COLUMN {} VARCHAR({})  ",
                                qualifiedTableName,
                                pkColumnName,
                                pkColumnLength);
                dExecuteDDL(addColumnSql, tableName, "新增字符串主键列[" + pkColumnName + "]");

                // 填充唯一值（自定义前缀/时间戳）
                String valuePrefix =
                        StrUtil.isEmpty(pkValuePrefix)
                                ? "to_char(now(), 'YYYYMMDDHH24MISS') || '_'"
                                : "'" + pkValuePrefix + "' || ";
                String updateSql =
                        StrUtil.format(
                                "WITH numbered_rows AS (SELECT ctid, row_number() OVER () AS rn FROM {}) "
                                        + "UPDATE {} f1 SET {} = {} f1.rn "
                                        + "FROM numbered_rows nr WHERE f1.ctid = nr.ctid",
                                qualifiedTableName,
                                qualifiedTableName,
                                pkColumnName,
                                valuePrefix);
                dExecuteDDL(updateSql, tableName, "填充字符串主键值[" + pkColumnName + "]");

                // 添加主键约束
                String addPkSql =
                        buildAddPrimaryKeySql(qualifiedTableName, pkConstraintName, pkColumnName);
                dExecuteDDL(addPkSql, tableName, "添加字符串主键约束[" + pkConstraintName + "]");
            }

            // ========== 分支2：整数自增主键（SERIAL） ==========
            else if (PrimaryKeyType.INT_AUTO.equals(pkType)) {
                // PostgreSQL中SERIAL本身就是自增+主键的简化写法（自动创建序列）
                String addColumnSql =
                        StrUtil.format(
                                "ALTER TABLE {} ADD COLUMN {} SERIAL PRIMARY KEY",
                                qualifiedTableName,
                                pkColumnName);
                dExecuteDDL(addColumnSql, tableName, "新增整数自增主键列[" + pkColumnName + "]");
            }

            // ========== 分支3：长整数自增主键（PostgreSQL 12+ 推荐） ==========
            else if (PrimaryKeyType.BIGINT_AUTO.equals(pkType)) {
                // PostgreSQL 12+ 推荐用 GENERATED ALWAYS AS IDENTITY（替代BIGSERIAL）
                String addColumnSql =
                        StrUtil.format(
                                "ALTER TABLE {} ADD COLUMN {} BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY",
                                qualifiedTableName,
                                pkColumnName);
                dExecuteDDL(addColumnSql, tableName, "新增长整数自增主键列[" + pkColumnName + "]");
            }

            // ========== 分支4：普通整数主键（非自增） ==========
            else if (PrimaryKeyType.INT_NORMAL.equals(pkType)) {
                // 步骤1：新增普通INT列（非空）
                String addColumnSql =
                        StrUtil.format(
                                "ALTER TABLE {} ADD COLUMN {} INT  ",
                                qualifiedTableName,
                                pkColumnName);
                dExecuteDDL(addColumnSql, tableName, "新增普通整数列[" + pkColumnName + "]");

                // 步骤2：填充唯一整数值（从1开始连续序号）
                String updateSql =
                        StrUtil.format(
                                "WITH numbered_rows AS (SELECT ctid, row_number() OVER () AS rn FROM {}) "
                                        + "UPDATE {} f1 SET {} = nr.rn "
                                        + "FROM numbered_rows nr WHERE f1.ctid = nr.ctid",
                                qualifiedTableName,
                                qualifiedTableName,
                                pkColumnName);
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
                                pkColumnName);
                dExecuteDDL(addColumnSql, tableName, "新增普通长整数列[" + pkColumnName + "]");

                // 步骤2：填充唯一长整数值（从1开始连续序号）
                String updateSql =
                        StrUtil.format(
                                "WITH numbered_rows AS (SELECT ctid, row_number() OVER () AS rn FROM {}) "
                                        + "UPDATE {} f1 SET {} = nr.rn "
                                        + "FROM numbered_rows nr WHERE f1.ctid = nr.ctid",
                                qualifiedTableName,
                                qualifiedTableName,
                                pkColumnName);
                dExecuteDDL(updateSql, tableName, "填充普通长整数主键值[" + pkColumnName + "]");

                // 步骤3：添加主键约束
                String addPkSql =
                        buildAddPrimaryKeySql(qualifiedTableName, pkConstraintName, pkColumnName);
                dExecuteDDL(addPkSql, tableName, "添加普通长整数主键约束[" + pkConstraintName + "]");
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format("给表[{}]添加主键失败：{}", tableName, e.getMessage()), e);
        }
    }

    @Override
    protected String buildAddPrimaryKeySql(
            String qualifiedTableName, String constraintName, String columns) {
        return StrUtil.format(
                "ALTER TABLE {} ADD CONSTRAINT {} PRIMARY KEY ({})",
                qualifiedTableName,
                constraintName,
                columns);
    }

    @Override
    protected String buildDropPrimaryKeySql(String qualifiedTableName, String constraintName) {
        return StrUtil.format(
                "ALTER TABLE {} DROP CONSTRAINT {}", qualifiedTableName, constraintName);
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
        return StrUtil.format(
                "DROP INDEX IF EXISTS {}.{}",
                dialectTableNameProcessor.tbGetSchemaNameForSql(dataSourceGetter),
                indexName);
    }

    @Override
    public List<IndexApo> dGetIndexes(String tableName) {
        if (StrUtil.isEmpty(tableName) || !dIsTableExists(tableName)) {
            return ListUtil.empty();
        }
        String sql =
                StrUtil.format(
                        " SELECT * FROM pg_indexes WHERE tablename = '{}' {}",
                        tableName,
                        StrUtil.isEmpty(dataSourceGetter.getSchemaName())
                                ? ""
                                : StrUtil.format(
                                "AND schemaname = '{}'", dataSourceGetter.getSchemaName()));
        return getAdvBaseOpt().bSelectObjList(sql, IndexApo.class);
    }

    @Override
    public boolean dIndexesExists(String tableName, String indexName) {
        List<IndexApo> indexes = dGetIndexes(tableName);
        return indexes.stream().anyMatch(idx -> idx.getIndexname().equals(indexName));
    }

    @Override
    public String dGetCurrentSchema() {
        String sql = "SELECT current_schema()  as schema  ";
        GirAdvOneRow girAdvOneRow = getAdvBaseOpt().bSelectOne(sql);
        return girAdvOneRow.getStr("schema");
    }

    @Override
    public String dGetCurrentDataBase() {
        String sql = "SELECT current_database() AS database_name ";
        GirAdvOneRow girAdvOneRow = getAdvBaseOpt().bSelectOne(sql);
        return girAdvOneRow.getStr("database_name");
    }

    // ========== Schema/模式差异化实现 ==========
    @Override
    public List<String> dGetAllSchemas() {
        String sql =
                "SELECT schema_name FROM information_schema.schemata WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast') ORDER BY schema_name";
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<String> schemas = new ArrayList<>();
        rows.forEach(row -> schemas.add(row.getStr("schema_name")));
        return schemas;
    }

    @Override
    public String dGetTableComment(String tableName) {
        String sql = StrUtil.format("SELECT obj_description('{}'::regclass)", tableName);
        GirAdvOneRow girAdvOneRow = getAdvBaseOpt().bSelectOne(sql);
        return girAdvOneRow == null ? "" : girAdvOneRow.getStr("obj_description");
    }

    @Override
    public List<String> dGetTablesBySchema(String schemaName) {
        String actualSchema =
                ObjectUtil.isEmpty(schemaName) ? dataSourceGetter.getSchemaName() : schemaName;
        String sql;

        if (StrUtil.isEmpty(actualSchema)) {
            sql =
                    "SELECT table_name FROM information_schema.tables "
                            + "WHERE table_type = 'BASE TABLE' AND table_schema NOT IN ('information_schema', 'pg_catalog') "
                            + "ORDER BY table_name";
        } else {
            sql =
                    StrUtil.format(
                            "SELECT table_name FROM information_schema.tables "
                                    + "WHERE table_type = 'BASE TABLE' AND table_schema = '{}' ORDER BY table_name",
                            actualSchema);
        }

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
        String fields = "table_name,table_type,table_catalog,table_schema";
        if (StrUtil.isEmpty(actualSchema)) {
            sql =
                    "SELECT "
                            + fields
                            + " FROM information_schema.tables "
                            + "WHERE   table_schema NOT IN ('information_schema', 'pg_catalog') "
                            + "ORDER BY table_name";
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
                    schemaTableApo.setDatabaseName(row.getStr("table_catalog"));
                    schemaTableApo.setSchema(row.getStr("table_schema"));
                    schemaTableApo.setName(row.getStr("table_name"));
                    String tableType = row.getStr("table_type");
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
    protected boolean checkSchemaExists(String schemaName) {
        String sql =
                StrUtil.format(
                        "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '{}'",
                        schemaName);
        return ObjectUtil.isNotEmpty(getAdvBaseOpt().bSelectList(sql));
    }

    @Override
    protected String buildCreateSchemaSql(String schemaName) {
        return StrUtil.format("CREATE SCHEMA {}", schemaName);
    }

    @Override
    protected String buildDropSchemaSql(String schemaName, boolean cascade) {
        return StrUtil.format("DROP SCHEMA {} {}", schemaName, cascade ? "CASCADE" : "RESTRICT");
    }

    // ========== 表大小差异化实现 ==========
    @Override
    public Long dGetTableSize(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            return null;
        }
        String schemaTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql =
                StrUtil.format(
                        "SELECT pg_total_relation_size('{}') AS table_size;", schemaTableName);
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row.getLong("table_size");
    }

    // ========== 元数据差异化实现 ==========
    @Override
    protected String buildMetadataQuerySql(String sqlView) {
        return StrUtil.format("SELECT * FROM ({}) AS temp_table LIMIT 0", sqlView);
    }

    @Override
    protected String getBaseColumnName(ResultSetMetaData metaData, int columnIndex)
            throws SQLException {
        if (metaData instanceof PgResultSetMetaData) {
            return ((PgResultSetMetaData) metaData).getBaseColumnName(columnIndex);
        }
        return metaData.getColumnName(columnIndex);
    }

    @Override
    protected String getColumnTypeName(ResultSetMetaData metaData, int columnIndex)
            throws SQLException {
        if (metaData instanceof PgResultSetMetaData) {
            return ((PgResultSetMetaData) metaData).getColumnTypeName(columnIndex);
        }
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

        // PG专属：字段长度处理
        if (columnTypeName.contains("char") || columnTypeName.contains("varchar")) {
            field.setCharacterMaximumLength(
                    String.valueOf(metaData.getColumnDisplaySize(columnIndex)));
        } else if (columnTypeName.contains("int")
                || columnTypeName.contains("numeric")
                || columnTypeName.contains("decimal")
                || columnTypeName.contains("float")) {
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
        schemaName = schemaName == null ? dataSourceGetter.getSchemaName() : schemaName;

        String sql =
                StrUtil.format(
                        "SELECT COUNT(*) AS cnt FROM information_schema.routines "
                                + "WHERE routine_name = '{}' AND routine_type = 'FUNCTION'",
                        nameNotSchema);
        if (StrUtil.isNotEmpty(schemaName)) {
            sql += StrUtil.format(" AND specific_schema = '{}'", schemaName);
        } else {
            sql += StrUtil.format(" AND specific_schema = {}", "current_schema()");
        }

        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null && row.getInt("cnt") > 0;
    }

    @Override
    protected String buildCreateTableFromTableSql(String dstTableName, String srcTableName) {
        // PostgreSQL: CREATE TABLE IF NOT EXISTS target AS SELECT * FROM source
        return StrUtil.format("CREATE TABLE IF NOT EXISTS {} AS SELECT * FROM {}",
                dstTableName, srcTableName);
    }

    @Override
    protected String buildCreateTableLikeSql(String dstTableName, String srcTableName) {
        // PostgreSQL: CREATE TABLE IF NOT EXISTS target (LIKE source INCLUDING ALL)
        return StrUtil.format("CREATE TABLE IF NOT EXISTS {} (LIKE {} INCLUDING ALL)",
                dstTableName, srcTableName);
    }

    @Override
    protected String buildCreateTableFromSqlSql(String dstTableName, String sql) {
        // PostgreSQL: CREATE TABLE IF NOT EXISTS target AS (SELECT ...)
        return StrUtil.format("CREATE TABLE IF NOT EXISTS {} AS ({})",
                dstTableName, sql);
    }

    @Override
    protected String buildCreateTableFromSqlWithNoDataSql(String dstTableName, String sql) {
        // PostgreSQL: CREATE TABLE IF NOT EXISTS target AS (SELECT ...) WITH NO DATA
        return StrUtil.format("CREATE TABLE IF NOT EXISTS {} AS ({}) WITH NO DATA",
                dstTableName, sql);
    }
}
