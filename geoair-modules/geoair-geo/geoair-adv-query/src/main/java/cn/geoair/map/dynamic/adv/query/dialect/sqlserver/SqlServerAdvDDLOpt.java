package cn.geoair.map.dynamic.adv.query.dialect.sqlserver;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.apo.IndexApo;
import cn.geoair.map.dynamic.adv.query.apo.SchemaTableApo;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvSchemaTableTypeOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.dialect.DialectName;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL Server DDL 实现。
 *
 * <p>继承通用 DDL 编排，所有会落到数据库的 SQL Server 差异语法在此覆写；特别是删除表、
 * 重命名、索引、模式和元数据查询均不复用 MySQL 语法。
 *
 * @author 张逢吉
 */
public class SqlServerAdvDDLOpt extends MysqlAdvDDLOpt {

    public SqlServerAdvDDLOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt) {
        super(dataSourceGetter, baseOpt);
    }

    @Override public DialectTableNameProcessor getDialectTableNameProcessor() { return SqlServerDialectTableNameUtil.getInstance(); }
    @Override protected DialectName getDialectName() { return DialectName.SQLSERVER; }
    @Override public String buildTruncateTableSql(String qualifiedTableName) { return "TRUNCATE TABLE " + qualifiedTableName; }
    @Override public String buildDropTableSql(String qualifiedTableName) { return "DROP TABLE IF EXISTS " + qualifiedTableName; }

    @Override
    public String buildRenameTableSql(String oldQualifiedName, String newQualifiedName) {
        String newName = dialectTableNameProcessor.tbGetTableNameNotSchema(newQualifiedName);
        return "EXEC sp_rename N'" + escapeSqlLiteral(oldQualifiedName) + "', N'"
                + escapeSqlLiteral(newName) + "'";
    }

    @Override
    public boolean dIsTableExists(String tableName) {
        if (StrUtil.isBlank(tableName) || dialectTableNameProcessor.tbTableIsSqlView(tableName)) return false;
        String schema = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        if (StrUtil.isBlank(schema)) schema = dataSourceGetter.getSchemaName();
        if (StrUtil.isBlank(schema)) schema = "dbo";
        String name = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String sql = "SELECT COUNT(1) AS cnt FROM sys.tables t JOIN sys.schemas s ON s.schema_id=t.schema_id "
                + "WHERE s.name='" + escapeSqlLiteral(schema) + "' AND t.name='" + escapeSqlLiteral(name) + "'";
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null && row.getInt("cnt") > 0;
    }

    @Override
    public DataFieldsApo dGetColumnsByTable(String tableName) {
        if (StrUtil.isBlank(tableName)) return null;
        String schema = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        if (StrUtil.isBlank(schema)) schema = dataSourceGetter.getSchemaName();
        if (StrUtil.isBlank(schema)) schema = "dbo";
        String name = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String sql = "SELECT c.name AS column_name, c.name AS original_column_name, c.column_id AS ordinal_position, "
                + "ty.name AS udt_name, ty.name AS data_type, s.name AS table_schema, t.name AS table_name, "
                + "CASE WHEN c.is_nullable=1 THEN 'YES' ELSE 'NO' END AS is_nullable, "
                + "OBJECT_DEFINITION(c.default_object_id) AS column_default, "
                + "CASE WHEN ty.name IN ('nvarchar','nchar') THEN c.max_length / 2 ELSE c.max_length END AS character_maximum_length, "
                + "c.precision AS numeric_precision, c.scale AS numeric_scale, "
                + "CASE WHEN pk.column_id IS NULL THEN 0 ELSE 1 END AS primary_key_is "
                + "FROM sys.columns c JOIN sys.tables t ON t.object_id=c.object_id "
                + "JOIN sys.schemas s ON s.schema_id=t.schema_id JOIN sys.types ty ON ty.user_type_id=c.user_type_id "
                + "LEFT JOIN (SELECT ic.object_id, ic.column_id FROM sys.indexes i JOIN sys.index_columns ic "
                + "ON i.object_id=ic.object_id AND i.index_id=ic.index_id WHERE i.is_primary_key=1) pk "
                + "ON pk.object_id=c.object_id AND pk.column_id=c.column_id "
                + "WHERE s.name='" + escapeSqlLiteral(schema) + "' AND t.name='" + escapeSqlLiteral(name) + "' ORDER BY c.column_id";
        List<FieldBySchemaApo> fields = getAdvBaseOpt().bSelectObjList(sql, FieldBySchemaApo.class);
        for (FieldBySchemaApo field : fields) {
            field.setDialectName(DialectName.SQLSERVER);
            field.determineGeometryFieldIs();
        }
        return new DataFieldsApo(fields);
    }

    @Override
    public String buildAlterColumnSql(String qualifiedTableName, String oldColumnName, FieldBySchemaApo newField) {
        String oldColumn = dialectTableNameProcessor.tbUnquoteTableName(oldColumnName);
        String column = StrUtil.blankToDefault(newField.getColumnName(), oldColumn);
        String type = newField.getUdtName();
        if (newField.getCharacterMaximumLength() != null && type != null
                && (type.toLowerCase().contains("char") || type.toLowerCase().contains("binary"))) {
            type += newField.getCharacterMaximumLength() == -1 ? "(MAX)"
                    : "(" + newField.getCharacterMaximumLength() + ")";
        } else if (newField.getNumericPrecision() != null && newField.getNumericScale() != null
                && type != null && (type.equalsIgnoreCase("decimal") || type.equalsIgnoreCase("numeric"))) {
            type += "(" + newField.getNumericPrecision() + "," + newField.getNumericScale() + ")";
        }
        String nullable = "NO".equalsIgnoreCase(newField.getIsNullable()) ? " NOT NULL" : " NULL";
        String alter = "ALTER TABLE " + qualifiedTableName + " ALTER COLUMN "
                + dialectTableNameProcessor.tbQuoteFieldName(column) + " " + type + nullable;
        if (!oldColumn.equalsIgnoreCase(column)) {
            String qualifiedColumn = qualifiedTableName + "." + dialectTableNameProcessor.tbQuoteFieldName(oldColumn);
            return "EXEC sp_rename N'" + escapeSqlLiteral(qualifiedColumn) + "', N'"
                    + escapeSqlLiteral(column) + "', 'COLUMN'; " + alter;
        }
        return alter;
    }

    @Override public String buildDropColumnSql(String qualifiedTableName, String columnName) {
        return "ALTER TABLE " + qualifiedTableName + " DROP COLUMN " + dialectTableNameProcessor.tbQuoteFieldName(columnName);
    }
    @Override public String buildAddPrimaryKeySql(String qualifiedTableName, String constraintName, String columns) {
        return "ALTER TABLE " + qualifiedTableName + " ADD CONSTRAINT " + dialectTableNameProcessor.tbQuoteFieldName(constraintName)
                + " PRIMARY KEY (" + columns + ")";
    }
    @Override public String buildDropPrimaryKeySql(String qualifiedTableName, String constraintName) {
        return "ALTER TABLE " + qualifiedTableName + " DROP CONSTRAINT " + dialectTableNameProcessor.tbQuoteFieldName(constraintName);
    }
    @Override public String buildCreateIndexSql(String qualifiedTableName, String indexName, String columns, boolean unique) {
        return "CREATE " + (unique ? "UNIQUE " : "") + "INDEX " + dialectTableNameProcessor.tbQuoteFieldName(indexName)
                + " ON " + qualifiedTableName + " (" + columns + ")";
    }
    @Override public String buildDropIndexSql(String tableName, String indexName) {
        return "DROP INDEX " + dialectTableNameProcessor.tbQuoteFieldName(indexName) + " ON "
                + dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
    }

    @Override
    public List<String> dGetPrimaryKeys(String tableName) {
        if (!dIsTableExists(tableName)) return new ArrayList<>();
        String schema = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        if (StrUtil.isBlank(schema)) schema = StrUtil.blankToDefault(dataSourceGetter.getSchemaName(), "dbo");
        String name = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String sql = "SELECT c.name AS column_name FROM sys.indexes i JOIN sys.index_columns ic "
                + "ON i.object_id=ic.object_id AND i.index_id=ic.index_id JOIN sys.columns c "
                + "ON c.object_id=ic.object_id AND c.column_id=ic.column_id JOIN sys.tables t ON t.object_id=i.object_id "
                + "JOIN sys.schemas s ON s.schema_id=t.schema_id WHERE i.is_primary_key=1 AND s.name='"
                + escapeSqlLiteral(schema) + "' AND t.name='" + escapeSqlLiteral(name) + "' ORDER BY ic.key_ordinal";
        List<String> result = new ArrayList<>();
        for (GirAdvOneRow row : getAdvBaseOpt().bSelectList(sql)) result.add(row.getStr("column_name"));
        return result;
    }

    /**
     * 为既有表补充主键列的 SQL Server 实现。
     *
     * <p>不能复用父类的 MySQL {@code AUTO_INCREMENT} 实现。数值普通主键和字符串主键
     * 使用窗口序号填充现有数据；序号只保证唯一，不承诺与任何业务排序一致。</p>
     */
    @Override
    public void dAddPrimaryKey(
            String tableName,
            String pkColumnName,
            String constraintName,
            PrimaryKeyType pkType,
            Integer pkColumnLength,
            String pkValuePrefix) {
        if (StrUtil.isBlank(tableName) || StrUtil.isBlank(pkColumnName) || pkType == null) {
            throw new IllegalArgumentException("表名、主键列名和主键类型不能为空");
        }
        if (!dIsTableExists(tableName)) {
            throw new IllegalArgumentException("表不存在，无法添加主键：" + tableName);
        }
        if (!dGetPrimaryKeys(tableName).isEmpty()) {
            throw new IllegalStateException("表已存在主键，不能重复添加：" + tableName);
        }
        String qualifiedTable = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String quotedColumn = dialectTableNameProcessor.tbQuoteFieldName(pkColumnName);
        String pkName = StrUtil.blankToDefault(constraintName,
                "pk_" + dialectTableNameProcessor.tbGetTableNameNotSchema(tableName) + "_" + System.currentTimeMillis());
        try {
            List<String> statements = new ArrayList<>();
            switch (pkType) {
                case INT_AUTO:
                    statements.add("ALTER TABLE " + qualifiedTable + " ADD " + quotedColumn
                            + " INT IDENTITY(1,1) NOT NULL");
                    break;
                case BIGINT_AUTO:
                    statements.add("ALTER TABLE " + qualifiedTable + " ADD " + quotedColumn
                            + " BIGINT IDENTITY(1,1) NOT NULL");
                    break;
                case STRING:
                    if (pkColumnLength == null || pkColumnLength <= 0) {
                        throw new IllegalArgumentException("字符串主键必须指定正数长度");
                    }
                    String prefix = StrUtil.blankToDefault(pkValuePrefix,
                            "gir_" + System.currentTimeMillis() + "_");
                    validateStringPrimaryKeyCapacity(qualifiedTable, prefix, pkColumnLength);
                    statements.add("ALTER TABLE " + qualifiedTable + " ADD " + quotedColumn + " VARCHAR(" + pkColumnLength + ") NULL");
                    statements.add(buildNumberedPrimaryKeySql(qualifiedTable, quotedColumn,
                            "CAST(N'" + escapeSqlLiteral(prefix) + "' + CONVERT(nvarchar(30), rn) AS varchar(" + pkColumnLength + "))"));
                    statements.add("ALTER TABLE " + qualifiedTable + " ALTER COLUMN " + quotedColumn + " VARCHAR(" + pkColumnLength + ") NOT NULL");
                    break;
                case INT_NORMAL:
                    statements.add("ALTER TABLE " + qualifiedTable + " ADD " + quotedColumn + " INT NULL");
                    statements.add(buildNumberedPrimaryKeySql(qualifiedTable, quotedColumn, "CONVERT(int, rn)"));
                    statements.add("ALTER TABLE " + qualifiedTable + " ALTER COLUMN " + quotedColumn + " INT NOT NULL");
                    break;
                case BIGINT_NORMAL:
                    statements.add("ALTER TABLE " + qualifiedTable + " ADD " + quotedColumn + " BIGINT NULL");
                    statements.add(buildNumberedPrimaryKeySql(qualifiedTable, quotedColumn, "CONVERT(bigint, rn)"));
                    statements.add("ALTER TABLE " + qualifiedTable + " ALTER COLUMN " + quotedColumn + " BIGINT NOT NULL");
                    break;
                default:
                    throw new IllegalArgumentException("不支持的 SQL Server 主键类型：" + pkType);
            }
            statements.add(buildAddPrimaryKeySql(qualifiedTable, pkName, quotedColumn));
            dExecuteDDL(String.join("; ", statements), tableName, "添加 SQL Server 主键[" + pkName + "]");
        } catch (RuntimeException ex) {
            throw new RuntimeException("给 SQL Server 表添加主键失败：" + tableName, ex);
        }
    }

    @Override
    public List<IndexApo> dGetIndexes(String tableName) {
        if (!dIsTableExists(tableName)) return new ArrayList<>();
        String schema = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        if (StrUtil.isBlank(schema)) schema = StrUtil.blankToDefault(dataSourceGetter.getSchemaName(), "dbo");
        String name = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String sql = "SELECT i.name AS indexname, i.is_unique AS non_unique FROM sys.indexes i JOIN sys.tables t "
                + "ON t.object_id=i.object_id JOIN sys.schemas s ON s.schema_id=t.schema_id WHERE i.name IS NOT NULL "
                + "AND s.name='" + escapeSqlLiteral(schema) + "' AND t.name='" + escapeSqlLiteral(name) + "'";
        return getAdvBaseOpt().bSelectObjList(sql, IndexApo.class);
    }
    @Override public boolean dIndexesExists(String tableName, String indexName) {
        return dGetIndexes(tableName).stream().anyMatch(index -> indexName.equalsIgnoreCase(index.getIndexname()));
    }
    @Override public boolean checkConstraintExists(String tableName, String constraintName, String constraintType) {
        String sql = "SELECT COUNT(1) AS cnt FROM sys.objects WHERE parent_object_id=OBJECT_ID(N'"
                + escapeSqlLiteral(dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName))
                + "') AND name='" + escapeSqlLiteral(constraintName) + "'"
                + ("PRIMARY KEY".equalsIgnoreCase(constraintType) ? " AND type='PK'" : "");
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null && row.getInt("cnt") > 0;
    }
    @Override public String dGetCurrentSchema() { return getAdvBaseOpt().bSelectOne("SELECT SCHEMA_NAME() AS ds").getStr("ds"); }
    @Override public String dGetCurrentDataBase() { return getAdvBaseOpt().bSelectOne("SELECT DB_NAME() AS ds").getStr("ds"); }
    @Override
    public List<String> dGetAllSchemas() {
        List<String> schemas = new ArrayList<>();
        for (GirAdvOneRow row : getAdvBaseOpt().bSelectList(
                "SELECT name FROM sys.schemas WHERE name NOT IN ('sys','INFORMATION_SCHEMA','guest') ORDER BY name")) {
            schemas.add(row.getStr("name"));
        }
        return schemas;
    }
    @Override
    public String dGetTableComment(String tableName) {
        String schema = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        if (StrUtil.isBlank(schema)) schema = StrUtil.blankToDefault(dataSourceGetter.getSchemaName(), "dbo");
        String name = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String sql = "SELECT CAST(ep.value AS nvarchar(max)) AS comment FROM sys.extended_properties ep "
                + "JOIN sys.tables t ON t.object_id=ep.major_id JOIN sys.schemas s ON s.schema_id=t.schema_id "
                + "WHERE ep.minor_id=0 AND ep.name='MS_Description' AND s.name='" + escapeSqlLiteral(schema)
                + "' AND t.name='" + escapeSqlLiteral(name) + "'";
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row == null ? "" : StrUtil.blankToDefault(row.getStr("comment"), "");
    }
    @Override
    public List<String> dGetTablesBySchema(String schemaName) {
        String schema = StrUtil.blankToDefault(schemaName, StrUtil.blankToDefault(dataSourceGetter.getSchemaName(), "dbo"));
        List<String> tables = new ArrayList<>();
        String sql = "SELECT t.name AS table_name FROM sys.tables t JOIN sys.schemas s ON s.schema_id=t.schema_id "
                + "WHERE s.name='" + escapeSqlLiteral(schema) + "' ORDER BY t.name";
        for (GirAdvOneRow row : getAdvBaseOpt().bSelectList(sql)) tables.add(row.getStr("table_name"));
        return tables;
    }
    @Override public List<String> dGetTablesBySchema() { return dGetTablesBySchema(null); }
    @Override
    public List<SchemaTableApo> dGetTableAndViewBySchema(String schemaName) {
        String schema = StrUtil.blankToDefault(schemaName, dataSourceGetter.getSchemaName());
        String where = StrUtil.isBlank(schema) ? " WHERE 1=1" : " WHERE s.name='" + escapeSqlLiteral(schema) + "'";
        String sql = "SELECT s.name AS schema_name,o.name AS object_name,o.type AS object_type FROM sys.objects o "
                + "JOIN sys.schemas s ON s.schema_id=o.schema_id" + where + " AND o.type IN ('U','V') ORDER BY o.name";
        List<SchemaTableApo> result = new ArrayList<>();
        for (GirAdvOneRow row : getAdvBaseOpt().bSelectList(sql)) {
            result.add(new SchemaTableApo().setDatabaseName(dGetCurrentDataBase()).setSchema(row.getStr("schema_name"))
                    .setName(row.getStr("object_name")).setType("U".equals(row.getStr("object_type"))
                            ? AdvSchemaTableTypeOpt.表 : AdvSchemaTableTypeOpt.视图));
        }
        return result;
    }
    @Override public List<SchemaTableApo> dGetTableAndViewBySchema() { return dGetTableAndViewBySchema(null); }
    @Override
    public Long dGetTableSize(String tableName) {
        if (StrUtil.isBlank(tableName)) return null;
        String schema = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        if (StrUtil.isBlank(schema)) schema = StrUtil.blankToDefault(dataSourceGetter.getSchemaName(), "dbo");
        String name = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String sql = "SELECT SUM(ps.used_page_count) * 8192 AS table_size FROM sys.dm_db_partition_stats ps "
                + "JOIN sys.tables t ON t.object_id=ps.object_id JOIN sys.schemas s ON s.schema_id=t.schema_id "
                + "WHERE s.name='" + escapeSqlLiteral(schema) + "' AND t.name='" + escapeSqlLiteral(name) + "'";
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row == null ? null : row.getLong("table_size");
    }
    @Override
    public boolean dIsFunctionExists(String functionName) {
        if (StrUtil.isBlank(functionName)) return false;
        String schema = dialectTableNameProcessor.tbExtractSchemaName(functionName);
        if (StrUtil.isBlank(schema)) schema = StrUtil.blankToDefault(dataSourceGetter.getSchemaName(), "dbo");
        String name = dialectTableNameProcessor.tbGetTableNameNotSchema(functionName);
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne("SELECT COUNT(1) AS cnt FROM sys.objects o JOIN sys.schemas s "
                + "ON s.schema_id=o.schema_id WHERE o.type IN ('FN','IF','TF','FS','FT') AND s.name='"
                + escapeSqlLiteral(schema) + "' AND o.name='" + escapeSqlLiteral(name) + "'");
        return row != null && row.getInt("cnt") > 0;
    }
    @Override public boolean checkSchemaExists(String schemaName) {
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne("SELECT COUNT(1) AS cnt FROM sys.schemas WHERE name='" + escapeSqlLiteral(schemaName) + "'");
        return row != null && row.getInt("cnt") > 0;
    }
    @Override public String buildCreateSchemaSql(String schemaName) { return "CREATE SCHEMA " + dialectTableNameProcessor.tbQuoteFieldName(schemaName); }
    @Override public String buildDropSchemaSql(String schemaName, boolean cascade) { return "DROP SCHEMA " + dialectTableNameProcessor.tbQuoteFieldName(schemaName); }

    /** SQL Server 不支持 DROP SCHEMA CASCADE，禁止静默忽略调用方意图。 */
    @Override
    public void dDropSchema(String schemaName, boolean cascade) {
        if (cascade) {
            throw new UnsupportedOperationException("SQL Server 不支持 DROP SCHEMA ... CASCADE；请先迁移或删除该 Schema 中的对象：" + schemaName);
        }
        super.dDropSchema(schemaName, false);
    }
    @Override public String buildMetadataQuerySql(String sqlView) { return "SELECT TOP 0 * FROM (" + sqlView + ") AS temp_table"; }
    @Override public String getBaseColumnName(ResultSetMetaData metaData, int index) throws SQLException { return metaData.getColumnName(index); }
    @Override public String getColumnTypeName(ResultSetMetaData metaData, int index) throws SQLException { return metaData.getColumnTypeName(index); }
    @Override public void setFieldLengthInfo(ResultSetMetaData metaData, int index, FieldBySchemaApo field) throws SQLException {
        field.setCharacterMaximumLength(metaData.getColumnDisplaySize(index));
        field.setNumericPrecision(metaData.getPrecision(index));
        field.setNumericScale(metaData.getScale(index));
    }
    @Override public String buildCreateTableFromTableSql(String dst, String src) { return "INSERT INTO " + dst + " SELECT * FROM " + src; }
    @Override public String buildCreateTableLikeSql(String dst, String src) { return "SELECT TOP 0 * INTO " + dst + " FROM " + src; }
    @Override public String buildCreateTableFromSqlSql(String dst, String sql) { return "SELECT * INTO " + dst + " FROM (" + sql + ") AS source_table"; }
    @Override public String buildCreateTableFromSqlWithNoDataSql(String dst, String sql) { return "SELECT TOP 0 * INTO " + dst + " FROM (" + sql + ") AS source_table"; }

    private static String escapeSqlLiteral(String value) { return value == null ? "" : value.replace("'", "''"); }

    private String buildNumberedPrimaryKeySql(String qualifiedTable, String quotedColumn, String valueExpression) {
        return "WITH numbered AS (SELECT " + quotedColumn
                + ", ROW_NUMBER() OVER (ORDER BY (SELECT 0)) AS rn FROM " + qualifiedTable + ") "
                + "UPDATE numbered SET " + quotedColumn + " = " + valueExpression;
    }

    /** 防止字符串主键在显式转换为 varchar 时被截断，进而产生重复值。 */
    private void validateStringPrimaryKeyCapacity(String qualifiedTable, String prefix, int columnLength) {
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne("SELECT COUNT(1) AS cnt FROM " + qualifiedTable);
        long rowCount = row == null ? 0L : row.getLong("cnt");
        if (rowCount > 0 && prefix.length() + String.valueOf(rowCount).length() > columnLength) {
            throw new IllegalArgumentException("字符串主键长度不足：前缀与序号最大长度为"
                    + (prefix.length() + String.valueOf(rowCount).length()) + "，字段长度为" + columnLength);
        }
    }
}
