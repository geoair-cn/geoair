package cn.geoair.map.dynamic.adv.query.dialect.dm;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.apo.IndexApo;
import cn.geoair.map.dynamic.adv.query.apo.SchemaTableApo;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.OracleAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvSchemaTableTypeOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.dialect.DialectName;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 达梦DDL实现类（第一版复用Oracle实现骨架）
 */
public class DmAdvDDLOpt extends OracleAdvDDLOpt {

    public DmAdvDDLOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt) {
        super(dataSourceGetter, baseOpt);
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return DmDialectTableNameUtil.getInstance();
    }

    @Override
    protected DialectName getDialectName() {
        return DialectName.DM;
    }

    @Override
    public boolean dIsTableExists(String tableName) {
        if (StrUtil.isEmpty(tableName) || dialectTableNameProcessor.tbTableIsSqlView(tableName)) {
            return false;
        }

        String nameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaName = getOwnerName(tableName);
        String sql =
                StrUtil.format(
                        "SELECT COUNT(*) AS \"cnt\" FROM ALL_TABLES WHERE TABLE_NAME = UPPER('{}'){}",
                        nameNotSchema,
                        buildOwnerClause(schemaName));
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null && row.getInt("cnt") > 0;
    }

    @Override
    public DataFieldsApo dGetColumnsByTable(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            return null;
        }

        String owner = getOwnerName(tableName);
        String tableNameUpper = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String sql =
                StrUtil.format(
                        "SELECT "
                                + "  col.COLUMN_NAME AS \"column_name\", "
                                + "  col.DATA_TYPE AS \"udt_name\", "
                                + "  col.DATA_TYPE AS \"data_type\", "
                                + "  col.DATA_LENGTH AS \"character_maximum_length\", "
                                + "  col.DATA_PRECISION AS \"numeric_precision\", "
                                + "  col.DATA_SCALE AS \"numeric_scale\", "
                                + "  col.NULLABLE AS \"is_nullable\", "
                                + "  col.DATA_DEFAULT AS \"column_default\", "
                                + "  comm.COMMENTS AS \"column_comment\" "
                                + "FROM ALL_TAB_COLUMNS col "
                                + "LEFT JOIN ALL_COL_COMMENTS comm "
                                + "  ON col.OWNER = comm.OWNER "
                                + "  AND col.TABLE_NAME = comm.TABLE_NAME "
                                + "  AND col.COLUMN_NAME = comm.COLUMN_NAME "
                                + "WHERE col.TABLE_NAME = UPPER('{}'){} "
                                + "ORDER BY col.COLUMN_ID",
                        tableNameUpper,
                        buildOwnerClause(owner, "col.OWNER"));

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
    public List<String> dGetPrimaryKeys(String tableName) {
        if (StrUtil.isEmpty(tableName) || !dIsTableExists(tableName)) {
            return new ArrayList<>();
        }

        String owner = getOwnerName(tableName);
        String tableNameUpper = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String ownerClause = buildOwnerClause(owner);
        String sql =
                StrUtil.format(
                        "SELECT COLUMN_NAME AS \"column_name\" FROM ALL_CONS_COLUMNS "
                                + "WHERE TABLE_NAME = UPPER('{}'){} "
                                + "  AND CONSTRAINT_NAME = ( "
                                + "      SELECT CONSTRAINT_NAME FROM ALL_CONSTRAINTS "
                                + "      WHERE TABLE_NAME = UPPER('{}'){} "
                                + "        AND CONSTRAINT_TYPE = 'P' "
                                + "  ) ORDER BY POSITION",
                        tableNameUpper,
                        ownerClause,
                        tableNameUpper,
                        ownerClause);

        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<String> pks = new ArrayList<>();
        rows.forEach(row -> pks.add(row.getStr("column_name")));
        return pks;
    }

    @Override
    public List<IndexApo> dGetIndexes(String tableName) {
        if (StrUtil.isEmpty(tableName) || !dIsTableExists(tableName)) {
            return new ArrayList<>();
        }

        String owner = getOwnerName(tableName);
        String tableNameUpper = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String sql =
                StrUtil.format(
                        "SELECT "
                                + "INDEX_NAME AS \"indexname\", "
                                + "TABLE_NAME AS \"tablename\", "
                                + "UNIQUENESS AS \"indexdef\" "
                                + "FROM ALL_INDEXES WHERE TABLE_NAME = UPPER('{}'){}",
                        tableNameUpper,
                        buildOwnerClause(owner));
        return getAdvBaseOpt().bSelectObjList(sql, IndexApo.class);
    }

    @Override
    public String dGetCurrentSchema() {
        String sql = "SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AS \"schema_name\" FROM DUAL";
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null ? row.getStr("schema_name") : null;
    }

    @Override
    public String dGetCurrentDataBase() {
        GirAdvOneRow row =
                getAdvBaseOpt()
                        .bSelectOne(
                                "SELECT SYS_CONTEXT('USERENV', 'DB_NAME') AS \"database_name\" FROM DUAL");
        if (row != null) {
            String dbName = row.getStr("database_name");
            if (StrUtil.isNotEmpty(dbName)) {
                return dbName;
            }
        }
        return dGetCurrentSchema();
    }

    @Override
    public List<String> dGetAllSchemas() {
        List<GirAdvOneRow> rows =
                getAdvBaseOpt()
                        .bSelectList(
                                "SELECT USERNAME AS \"schema_name\" FROM ALL_USERS ORDER BY USERNAME");
        List<String> schemas = new ArrayList<>();
        rows.forEach(row -> schemas.add(row.getStr("schema_name")));
        return schemas;
    }

    @Override
    public String dGetTableComment(String tableName) {
        String tableNameUpper = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String owner = getOwnerName(tableName);
        String sql =
                StrUtil.format(
                        "SELECT COMMENTS AS \"comments\" FROM ALL_TAB_COMMENTS WHERE TABLE_NAME = UPPER('{}'){}",
                        tableNameUpper,
                        buildOwnerClause(owner));
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row == null ? "" : row.getStr("comments");
    }

    @Override
    public List<String> dGetTablesBySchema(String schemaName) {
        String owner = normalizeOwner(schemaName);
        String sql =
                StrUtil.format(
                        "SELECT TABLE_NAME AS \"table_name\" FROM ALL_TABLES{} ORDER BY TABLE_NAME",
                        buildOwnerClause(owner));
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
        String owner = normalizeOwner(schemaName);
        String sql =
                StrUtil.format(
                        "SELECT OBJECT_NAME AS \"object_name\", OBJECT_TYPE AS \"object_type\", OWNER AS \"owner\" "
                                + "FROM ALL_OBJECTS WHERE OBJECT_TYPE IN ('TABLE', 'VIEW'){} ORDER BY OBJECT_NAME",
                        buildOwnerClause(owner));
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<SchemaTableApo> result = new ArrayList<>();
        for (GirAdvOneRow row : rows) {
            SchemaTableApo apo = new SchemaTableApo();
            apo.setName(row.getStr("object_name"));
            apo.setSchema(row.getStr("owner"));
            apo.setDatabaseName(dGetCurrentDataBase());
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
    public Long dGetTableSize(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            return null;
        }
        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String owner = getOwnerName(tableName);
        String sql =
                StrUtil.format(
                        "SELECT SUM(BYTES) AS \"table_size\" FROM ALL_SEGMENTS WHERE SEGMENT_NAME = UPPER('{}') AND SEGMENT_TYPE = 'TABLE'{}",
                        notSchemaTableName,
                        buildOwnerClause(owner));
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null ? row.getLong("table_size") : null;
    }

    @Override
    public boolean dIsFunctionExists(String functionName) {
        if (StrUtil.isEmpty(functionName)) {
            return false;
        }
        String nameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(functionName);
        String owner = getOwnerName(functionName);
        String sql =
                StrUtil.format(
                        "SELECT COUNT(*) AS \"cnt\" FROM ALL_OBJECTS WHERE OBJECT_NAME = UPPER('{}') AND OBJECT_TYPE = 'FUNCTION'{}",
                        nameNotSchema,
                        buildOwnerClause(owner));
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null && row.getInt("cnt") > 0;
    }

    private String getOwnerName(String tableName) {
        return normalizeOwner(dialectTableNameProcessor.tbExtractSchemaName(tableName));
    }

    private String normalizeOwner(String schemaName) {
        if (StrUtil.isNotEmpty(schemaName)) {
            return schemaName.toUpperCase();
        }
        String currentSchema = dataSourceGetter.getSchemaName();
        if (StrUtil.isNotEmpty(currentSchema)) {
            return currentSchema.toUpperCase();
        }
        String detectedSchema = dGetCurrentSchema();
        return StrUtil.isEmpty(detectedSchema) ? null : detectedSchema.toUpperCase();
    }

    private String buildOwnerClause(String owner) {
        return buildOwnerClause(owner, "OWNER");
    }

    private String buildOwnerClause(String owner, String ownerFieldName) {
        if (StrUtil.isEmpty(owner)) {
            return "";
        }
        return StrUtil.format(" AND {} = '{}'", ownerFieldName, owner);
    }
}
