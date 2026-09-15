package cn.geoair.map.dynamic.adv.query.dialect.sqlite;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.apo.IndexApo;
import cn.geoair.map.dynamic.adv.query.apo.SchemaTableApo;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvSchemaTableTypeOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.dialect.DialectName;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** SQLite Core 的 DDL 与元数据实现。 */
public class SqliteAdvDDLOpt extends AbstractExecAdvDDLOpt {

    private static final String DEFAULT_SCHEMA = "main";
    private static final String ALTER_COLUMN_MESSAGE =
            "SQLite Core 不支持直接修改列定义或事后增删主键；请显式执行建新表、复制数据、替换旧表的迁移流程";
    private static final String SCHEMA_MESSAGE =
            "SQLite 没有 CREATE/DROP SCHEMA；main、temp 和 ATTACH 数据库属于连接级命名空间";
    private static final Pattern TYPE_SIZE = Pattern.compile("\\((\\d+)(?:\\s*,\\s*(\\d+))?\\)");

    public SqliteAdvDDLOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt) {
        super(dataSourceGetter, baseOpt);
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return SqliteDialectTableNameUtil.getInstance();
    }

    @Override
    protected DialectName getDialectName() {
        return DialectName.SQLITE3;
    }

    @Override
    protected String buildTruncateTableSql(String qualifiedTableName) {
        // SQLite 没有 TRUNCATE；这里只保证删除全部行，不承诺重置 ROWID/sqlite_sequence。
        return StrUtil.format("DELETE FROM {}", qualifiedTableName);
    }

    @Override
    protected String buildDropTableSql(String qualifiedTableName) {
        return StrUtil.format("DROP TABLE IF EXISTS {}", qualifiedTableName);
    }

    @Override
    protected String buildRenameTableSql(String oldQualifiedName, String newQualifiedName) {
        return StrUtil.format("ALTER TABLE {} RENAME TO {}", oldQualifiedName, newQualifiedName);
    }

    @Override
    public boolean dIsTableExists(String tableName) {
        if (StrUtil.isBlank(tableName)
                || dialectTableNameProcessor.tbTableIsSqlView(tableName)) {
            return false;
        }
        String schema = resolveSchema(tableName);
        String pureTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String sql = StrUtil.format(
                "SELECT COUNT(*) AS cnt FROM {} WHERE type = 'table' AND name = ?",
                schemaCatalog(schema));
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql, SqlParamList.of(pureTableName));
        return row != null && row.getInt("cnt") > 0;
    }

    @Override
    public DataFieldsApo dGetColumnsByTable(String tableName) {
        if (StrUtil.isBlank(tableName)) {
            return new DataFieldsApo();
        }
        String schema = resolveSchema(tableName);
        String pureTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String sql = "SELECT cid, name, type, \"notnull\" AS not_null, dflt_value, pk, hidden "
                + "FROM pragma_table_xinfo(?, ?) ORDER BY cid";
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(
                sql, SqlParamList.of(pureTableName, schema));
        List<FieldBySchemaApo> fields = new ArrayList<>();
        for (GirAdvOneRow row : rows) {
            FieldBySchemaApo field = new FieldBySchemaApo();
            String columnName = row.getStr("name");
            String declaredType = row.getStr("type");
            field.setDialectName(DialectName.SQLITE3);
            field.setColumnName(columnName);
            field.setOriginalColumnName(columnName);
            field.setOrdinalPosition(row.getInt("cid") + 1);
            field.setColumnDefault(row.getStr("dflt_value"));
            field.setIsNullable(row.getInt("not_null") == 1 ? "NO" : "YES");
            field.setPrimaryKeyIs(row.getInt("pk") > 0);
            field.setUdtName(StrUtil.isBlank(declaredType) ? "BLOB" : declaredType);
            field.setDataType(field.getUdtName());
            field.setTableSchema(schema);
            field.setTableName(pureTableName);
            field.setGeometryFieldIs(false);
            setDeclaredTypeLength(field);
            fields.add(field);
        }
        return new DataFieldsApo(fields);
    }

    @Override
    public void dAlterColumn(
            String tableName, String oldColumnName, FieldBySchemaApo newField) {
        throw new UnsupportedOperationException(ALTER_COLUMN_MESSAGE);
    }

    @Override
    protected String buildAlterColumnSql(
            String qualifiedTableName, String oldColumnName, FieldBySchemaApo newField) {
        throw new UnsupportedOperationException(ALTER_COLUMN_MESSAGE);
    }

    @Override
    protected String buildDropColumnSql(String qualifiedTableName, String columnName) {
        return StrUtil.format("ALTER TABLE {} DROP COLUMN {}", qualifiedTableName, columnName);
    }

    @Override
    public List<String> dGetPrimaryKeys(String tableName) {
        if (StrUtil.isBlank(tableName) || !dIsTableExists(tableName)) {
            return Collections.emptyList();
        }
        String schema = resolveSchema(tableName);
        String pureTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String sql = "SELECT name FROM pragma_table_xinfo(?, ?) WHERE pk > 0 ORDER BY pk";
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(
                sql, SqlParamList.of(pureTableName, schema));
        List<String> primaryKeys = new ArrayList<>();
        for (GirAdvOneRow row : rows) {
            primaryKeys.add(row.getStr("name"));
        }
        return primaryKeys;
    }

    @Override
    protected boolean checkConstraintExists(
            String tableName, String constraintName, String constraintType) {
        return "PRIMARY KEY".equalsIgnoreCase(constraintType)
                && !dGetPrimaryKeys(tableName).isEmpty();
    }

    @Override
    public void dAddPrimaryKey(
            String tableName, List<String> columnNames, String constraintName) {
        throw new UnsupportedOperationException(ALTER_COLUMN_MESSAGE);
    }

    @Override
    public void dAddPrimaryKey(
            String tableName,
            String pkColumnName,
            String constraintName,
            PrimaryKeyType pkType,
            Integer pkColumnLength,
            String pkValuePrefix) {
        throw new UnsupportedOperationException(ALTER_COLUMN_MESSAGE);
    }

    @Override
    public void dDropPrimaryKey(String tableName, String constraintName) {
        throw new UnsupportedOperationException(ALTER_COLUMN_MESSAGE);
    }

    @Override
    protected String buildAddPrimaryKeySql(
            String qualifiedTableName, String constraintName, String columns) {
        throw new UnsupportedOperationException(ALTER_COLUMN_MESSAGE);
    }

    @Override
    protected String buildDropPrimaryKeySql(
            String qualifiedTableName, String constraintName) {
        throw new UnsupportedOperationException(ALTER_COLUMN_MESSAGE);
    }

    @Override
    protected String buildCreateIndexSql(
            String qualifiedTableName,
            String indexName,
            String columns,
            boolean isUnique) {
        String schema = dialectTableNameProcessor.tbExtractSchemaName(qualifiedTableName);
        if (StrUtil.isBlank(schema)) {
            schema = DEFAULT_SCHEMA;
        }
        String table = dialectTableNameProcessor.tbGetTableNameNotSchema(qualifiedTableName);
        String qualifiedIndex = dialectTableNameProcessor.tbQuoteSchemaName(schema)
                + "." + dialectTableNameProcessor.tbQuoteFieldName(indexName);
        return StrUtil.format(
                "CREATE {}INDEX {} ON {} ({})",
                isUnique ? "UNIQUE " : "",
                qualifiedIndex,
                dialectTableNameProcessor.tbQuoteTableName(table),
                columns);
    }

    @Override
    protected String buildDropIndexSql(String tableName, String indexName) {
        String schema = resolveSchema(tableName);
        return StrUtil.format(
                "DROP INDEX IF EXISTS {}.{}",
                dialectTableNameProcessor.tbQuoteSchemaName(schema),
                dialectTableNameProcessor.tbQuoteFieldName(indexName));
    }

    @Override
    public List<IndexApo> dGetIndexes(String tableName) {
        if (StrUtil.isBlank(tableName) || !dIsTableExists(tableName)) {
            return Collections.emptyList();
        }
        String schema = resolveSchema(tableName);
        String pureTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(
                "SELECT name FROM pragma_index_list(?, ?) ORDER BY seq",
                SqlParamList.of(pureTableName, schema));
        List<IndexApo> result = new ArrayList<>();
        for (GirAdvOneRow row : rows) {
            String indexName = row.getStr("name");
            String definitionSql = StrUtil.format(
                    "SELECT sql FROM {} WHERE type = 'index' AND name = ?",
                    schemaCatalog(schema));
            GirAdvOneRow definition = getAdvBaseOpt().bSelectOne(
                    definitionSql, SqlParamList.of(indexName));
            IndexApo index = new IndexApo();
            index.setSchemaname(schema);
            index.setTablename(pureTableName);
            index.setIndexname(indexName);
            index.setIndexdef(definition == null ? null : definition.getStr("sql"));
            result.add(index);
        }
        return result;
    }

    @Override
    public boolean dIndexesExists(String tableName, String indexName) {
        if (StrUtil.isBlank(indexName)) {
            return false;
        }
        for (IndexApo index : dGetIndexes(tableName)) {
            if (indexName.equalsIgnoreCase(index.getIndexname())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String dGetCurrentSchema() {
        return DEFAULT_SCHEMA;
    }

    @Override
    public String dGetCurrentDataBase() {
        // 这里不能走 AdvBaseOpt：SQL 日志在执行前会反查数据库名，进而再次进入本方法。
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = dataSourceGetter.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery("PRAGMA database_list");
            while (resultSet.next()) {
                if (DEFAULT_SCHEMA.equalsIgnoreCase(resultSet.getString("name"))) {
                    String file = resultSet.getString("file");
                    return StrUtil.isBlank(file) ? ":memory:" : file;
                }
            }
            return null;
        } catch (SQLException e) {
            throw new IllegalStateException("读取 SQLite 当前数据库失败: " + e.getMessage(), e);
        } finally {
            dataSourceGetter.closeResources(resultSet, statement, connection);
        }
    }

    @Override
    public List<String> dGetAllSchemas() {
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList("PRAGMA database_list");
        List<String> schemas = new ArrayList<>();
        for (GirAdvOneRow row : rows) {
            schemas.add(row.getStr("name"));
        }
        return schemas;
    }

    @Override
    public String dGetTableComment(String tableName) {
        // SQLite 没有原生表注释元数据。
        return "";
    }

    @Override
    public List<String> dGetTablesBySchema(String schemaName) {
        String schema = StrUtil.isBlank(schemaName) ? DEFAULT_SCHEMA : schemaName;
        String sql = StrUtil.format(
                "SELECT name FROM {} WHERE type = 'table' "
                        + "AND name NOT LIKE 'sqlite_%' ORDER BY name",
                schemaCatalog(schema));
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<String> tables = new ArrayList<>();
        for (GirAdvOneRow row : rows) {
            tables.add(row.getStr("name"));
        }
        return tables;
    }

    @Override
    public List<String> dGetTablesBySchema() {
        return dGetTablesBySchema(DEFAULT_SCHEMA);
    }

    @Override
    public List<SchemaTableApo> dGetTableAndViewBySchema(String schemaName) {
        String schema = StrUtil.isBlank(schemaName) ? DEFAULT_SCHEMA : schemaName;
        String sql = StrUtil.format(
                "SELECT name, type FROM {} WHERE type IN ('table', 'view') "
                        + "AND name NOT LIKE 'sqlite_%' ORDER BY name",
                schemaCatalog(schema));
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<SchemaTableApo> result = new ArrayList<>();
        String databaseName = dGetCurrentDataBase();
        for (GirAdvOneRow row : rows) {
            String type = row.getStr("type");
            result.add(new SchemaTableApo()
                    .setDatabaseName(databaseName)
                    .setSchema(schema)
                    .setName(row.getStr("name"))
                    .setType("table".equalsIgnoreCase(type)
                            ? AdvSchemaTableTypeOpt.表
                            : AdvSchemaTableTypeOpt.视图));
        }
        return result;
    }

    @Override
    public List<SchemaTableApo> dGetTableAndViewBySchema() {
        return dGetTableAndViewBySchema(DEFAULT_SCHEMA);
    }

    @Override
    protected boolean checkSchemaExists(String schemaName) {
        for (String schema : dGetAllSchemas()) {
            if (schema.equalsIgnoreCase(schemaName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void dCreateSchema(String schemaName) {
        throw new UnsupportedOperationException(SCHEMA_MESSAGE);
    }

    @Override
    public void dDropSchema(String schemaName, boolean cascade) {
        throw new UnsupportedOperationException(SCHEMA_MESSAGE);
    }

    @Override
    protected String buildCreateSchemaSql(String schemaName) {
        throw new UnsupportedOperationException(SCHEMA_MESSAGE);
    }

    @Override
    protected String buildDropSchemaSql(String schemaName, boolean cascade) {
        throw new UnsupportedOperationException(SCHEMA_MESSAGE);
    }

    @Override
    public Long dGetTableSize(String tableName) {
        // SQLite 不提供稳定的逐表物理大小；dbstat 并非所有构建都可用。
        return null;
    }

    @Override
    protected String buildMetadataQuerySql(String sqlView) {
        return StrUtil.format("SELECT * FROM ({}) AS \"_geoair_meta\" LIMIT 0", sqlView);
    }

    @Override
    protected String getBaseColumnName(ResultSetMetaData metaData, int columnIndex)
            throws SQLException {
        String columnName = metaData.getColumnName(columnIndex);
        return StrUtil.isBlank(columnName) ? metaData.getColumnLabel(columnIndex) : columnName;
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
        String typeName = field.getUdtName();
        if (typeName == null) {
            return;
        }
        String normalized = typeName.toUpperCase(Locale.ROOT);
        if (normalized.contains("CHAR") || normalized.contains("CLOB")
                || normalized.contains("TEXT")) {
            int displaySize = metaData.getColumnDisplaySize(columnIndex);
            if (displaySize > 0) {
                field.setCharacterMaximumLength(displaySize);
            }
        } else if (!normalized.contains("BLOB")) {
            int precision = metaData.getPrecision(columnIndex);
            int scale = metaData.getScale(columnIndex);
            if (precision > 0) {
                field.setNumericPrecision(precision);
                field.setNumericScale(scale);
            }
        }
    }

    @Override
    public boolean dIsFunctionExists(String functionName) {
        if (StrUtil.isBlank(functionName)) {
            return false;
        }
        String pureName = dialectTableNameProcessor.tbGetTableNameNotSchema(functionName);
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(
                "SELECT COUNT(*) AS cnt FROM pragma_function_list WHERE lower(name) = lower(?)",
                SqlParamList.of(pureName));
        return row != null && row.getInt("cnt") > 0;
    }

    @Override
    protected String buildCreateTableFromTableSql(String dstTableName, String srcTableName) {
        return StrUtil.format("INSERT INTO {} SELECT * FROM {}", dstTableName, srcTableName);
    }

    @Override
    protected String buildCreateTableLikeSql(String dstTableName, String srcTableName) {
        return StrUtil.format("CREATE TABLE {} AS SELECT * FROM {} WHERE 0", dstTableName, srcTableName);
    }

    @Override
    protected String buildCreateTableFromSqlSql(String dstTableName, String sql) {
        return StrUtil.format("CREATE TABLE {} AS {}", dstTableName, sql);
    }

    @Override
    protected String buildCreateTableFromSqlWithNoDataSql(String dstTableName, String sql) {
        return StrUtil.format(
                "CREATE TABLE {} AS SELECT * FROM ({}) AS \"_geoair_src\" WHERE 0",
                dstTableName, sql);
    }

    private String resolveSchema(String tableName) {
        String schema = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        if (StrUtil.isBlank(schema)) {
            schema = dataSourceGetter.getSchemaName();
        }
        return StrUtil.isBlank(schema) ? DEFAULT_SCHEMA : schema;
    }

    private String schemaCatalog(String schema) {
        return dialectTableNameProcessor.tbQuoteSchemaName(schema) + ".sqlite_schema";
    }

    private void setDeclaredTypeLength(FieldBySchemaApo field) {
        String declaredType = field.getUdtName();
        if (StrUtil.isBlank(declaredType)) {
            return;
        }
        Matcher matcher = TYPE_SIZE.matcher(declaredType);
        if (!matcher.find()) {
            return;
        }
        String normalized = declaredType.toUpperCase(Locale.ROOT);
        int first = Integer.parseInt(matcher.group(1));
        if (normalized.contains("CHAR") || normalized.contains("CLOB")
                || normalized.contains("TEXT")) {
            field.setCharacterMaximumLength(first);
            return;
        }
        field.setNumericPrecision(first);
        if (matcher.group(2) != null) {
            field.setNumericScale(Integer.parseInt(matcher.group(2)));
        }
    }
}
