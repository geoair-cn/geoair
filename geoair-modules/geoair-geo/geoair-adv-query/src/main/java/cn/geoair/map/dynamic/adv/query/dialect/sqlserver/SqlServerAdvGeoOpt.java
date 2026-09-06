package cn.geoair.map.dynamic.adv.query.dialect.sqlserver;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvGeoOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL Server {@code geometry} 空间操作实现。
 *
 * <p>该实现有意以平面 {@code geometry} 为默认类型。SQL Server 原生没有 ST_Transform，
 * 因此 {@link #eTransformSrid(String, String, int)} 明确拒绝服务端转换，防止仅重标 SRID
 * 而造成坐标值与坐标系不一致。
 *
 * @author 张逢吉
 */
public class SqlServerAdvGeoOpt extends AbstractExecAdvGeoOpt {
    private final IAdvBaseOpt baseOpt;
    private final IAdvDDLOpt ddlOpt;

    public SqlServerAdvGeoOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt, IAdvDDLOpt ddlOpt) {
        super(dataSourceGetter);
        this.baseOpt = baseOpt;
        this.ddlOpt = ddlOpt;
    }

    @Override protected DialectTableNameProcessor getDialectTableNameProcessor() { return SqlServerDialectTableNameUtil.getInstance(); }
    @Override protected IAdvBaseOpt getAdvBaseOpt() { return baseOpt; }
    @Override protected IAdvDDLOpt getAdvDDLOpt() { return ddlOpt; }

    @Override
    protected AdvEnumsTypeGeom getTypeGeomEnum(String nativeGeomType) {
        if (StrUtil.isBlank(nativeGeomType)) return null;
        String type = nativeGeomType.toLowerCase(java.util.Locale.ROOT).replace("st_", "");
        if ("point".equals(type)) return AdvEnumsTypeGeom.Point;
        if ("multipoint".equals(type)) return AdvEnumsTypeGeom.MultiPoint;
        if ("linestring".equals(type)) return AdvEnumsTypeGeom.LineString;
        if ("multilinestring".equals(type)) return AdvEnumsTypeGeom.MultiLineString;
        if ("polygon".equals(type)) return AdvEnumsTypeGeom.Polygon;
        if ("multipolygon".equals(type)) return AdvEnumsTypeGeom.MultiPolygon;
        return AdvEnumsTypeGeom.Geometry;
    }

    @Override
    public List<String> eGetAllGeoLayerName() {
        String sql = "SELECT DISTINCT t.name AS table_name FROM sys.columns c JOIN sys.tables t ON t.object_id=c.object_id "
                + "JOIN sys.types ty ON ty.user_type_id=c.user_type_id JOIN sys.schemas s ON s.schema_id=t.schema_id "
                + "WHERE ty.name = 'geometry' AND s.name=#{schema} ORDER BY t.name";
        SqlParamMap params = new SqlParamMap();
        params.put("schema", StrUtil.blankToDefault(dataSourceGetter.getSchemaName(), "dbo"));
        List<String> names = new ArrayList<>();
        for (GirAdvOneRow row : baseOpt.bSelectList(sql, params)) names.add(row.getStr("table_name"));
        return names;
    }

    @Override
    public List<String> eGetGeoLayerNameByKeyword(String keyword) {
        if (StrUtil.isBlank(keyword)) return eGetAllGeoLayerName();
        String sql = "SELECT DISTINCT t.name AS table_name FROM sys.columns c JOIN sys.tables t ON t.object_id=c.object_id "
                + "JOIN sys.types ty ON ty.user_type_id=c.user_type_id JOIN sys.schemas s ON s.schema_id=t.schema_id "
                + "WHERE ty.name = 'geometry' AND s.name=#{schema} AND t.name LIKE '%' + #{keyword} + '%' ORDER BY t.name";
        SqlParamMap params = new SqlParamMap();
        params.put("schema", StrUtil.blankToDefault(dataSourceGetter.getSchemaName(), "dbo"));
        params.put("keyword", keyword);
        List<String> names = new ArrayList<>();
        for (GirAdvOneRow row : baseOpt.bSelectList(sql, params)) names.add(row.getStr("table_name"));
        return names;
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeByTable(String tableName, List<String> fields) {
        if (CollectionUtil.isEmpty(fields)) return MapUtil.empty();
        String table = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        StringBuilder selected = new StringBuilder();
        StringBuilder filter = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            String field = dialectTableNameProcessor.tbQuoteFieldName(fields.get(i));
            if (i > 0) { selected.append(", "); filter.append(" OR "); }
            selected.append(field).append(".STGeometryType() AS [").append(fields.get(i)).append("_type]");
            filter.append(field).append(" IS NOT NULL");
        }
        GirAdvOneRow row = baseOpt.bSelectOne("SELECT TOP 1 " + selected + " FROM " + table + " WHERE " + filter);
        Map<String, AdvEnumsTypeGeom> result = new LinkedHashMap<>();
        if (row != null) for (String field : fields) {
            AdvEnumsTypeGeom type = getTypeGeomEnum(row.getStr(field + "_type"));
            if (type != null) result.put(field, type);
        }
        return result;
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(String sqlView, List<String> fields) {
        if (StrUtil.isBlank(sqlView) || CollectionUtil.isEmpty(fields)) return MapUtil.empty();
        StringBuilder selected = new StringBuilder();
        StringBuilder filter = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            String field = dialectTableNameProcessor.tbQuoteFieldName(fields.get(i));
            if (i > 0) { selected.append(", "); filter.append(" OR "); }
            selected.append(field).append(".STGeometryType() AS [").append(fields.get(i)).append("_type]");
            filter.append(field).append(" IS NOT NULL");
        }
        GirAdvOneRow row = baseOpt.bSelectOne("SELECT TOP 1 " + selected + " FROM (" + sqlView + ") AS temp WHERE " + filter);
        Map<String, AdvEnumsTypeGeom> result = new LinkedHashMap<>();
        if (row != null) for (String field : fields) {
            AdvEnumsTypeGeom type = getTypeGeomEnum(row.getStr(field + "_type"));
            if (type != null) result.put(field, type);
        }
        return result;
    }

    @Override
    public Integer eGetSrid(String tableNameOrSqlView, String geomFieldName) {
        if (StrUtil.isBlank(geomFieldName)) return 0;
        String source = ddlOpt.dIsTableExists(tableNameOrSqlView)
                ? dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameOrSqlView)
                : "(" + tableNameOrSqlView + ") AS temp";
        String field = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        GirAdvOneRow row = baseOpt.bSelectOne("SELECT TOP 1 " + field + ".STSrid AS srid FROM " + source + " WHERE " + field + " IS NOT NULL");
        return row == null ? 0 : row.getInt("srid");
    }

    /** SQL Server 当前实现只暴露平面 geometry 字段，避免 geography 与 geometry 构造器混用。 */
    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListByTable(String tableName) {
        List<FieldBySchemaApo> fields = ddlOpt.dGetColumnsByTable(tableName).geomFields();
        List<FieldBySchemaApo> geometryFields = new ArrayList<>();
        for (FieldBySchemaApo field : fields) {
            if ("geometry".equalsIgnoreCase(field.getUdtName())) {
                geometryFields.add(field);
            }
        }
        return geometryFields;
    }

    @Override
    public List<String> eGetGeomColumnNameListByTable(String tableName) {
        List<String> names = new ArrayList<>();
        for (FieldBySchemaApo field : eGetGeomColumnListByTable(tableName)) {
            names.add(field.getColumnName());
        }
        return names;
    }

    @Override
    public String eGetGeomColumnNameBySql(String dynamicSql, GirSqlParam sqlParam) {
        FieldBySchemaApo field = eGetGeomColumnBySql(dynamicSql, sqlParam);
        return field == null ? null : field.getColumnName();
    }

    @Override
    public String eGetGeomColumnNameBySql(String sqlView) {
        FieldBySchemaApo field = eGetGeomColumnBySql(sqlView);
        return field == null ? null : field.getColumnName();
    }

    @Override
    public List<String> eGetGeomColumnNameListBySql(String sqlView) {
        List<String> names = new ArrayList<>();
        for (FieldBySchemaApo field : eGetGeomColumnListBySql(sqlView)) names.add(field.getColumnName());
        return names;
    }

    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListBySql(String sqlView) {
        return geometryOnly(ddlOpt.dGetColumnsBySQL(sqlView).geomFields());
    }

    @Override
    public FieldBySchemaApo eGetGeomColumnBySql(String sqlView) {
        List<FieldBySchemaApo> fields = eGetGeomColumnListBySql(sqlView);
        return fields.isEmpty() ? null : fields.get(0);
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(
            String dynamicSql, GirSqlParam sqlParam, List<String> geomFieldNames) {
        if (CollectionUtil.isEmpty(geomFieldNames)) return MapUtil.empty();
        DataFieldsApo fields = ddlOpt.dGetColumnsBySQL(dynamicSql, sqlParam);
        Map<String, AdvEnumsTypeGeom> result = new LinkedHashMap<>();
        for (String name : geomFieldNames) {
            FieldBySchemaApo field = fields.findField(
                    candidate -> name.equalsIgnoreCase(candidate.getColumnName())).orElse(null);
            if (field != null && "geometry".equalsIgnoreCase(field.getUdtName())) {
                result.put(name, AdvEnumsTypeGeom.Geometry);
            }
        }
        return result;
    }

    @Override
    public boolean eIsGeomBySql(String dynamicSql, GirSqlParam sqlParam) {
        return !eGetGeomColumnListBySql(dynamicSql, sqlParam).isEmpty();
    }

    @Override
    public List<String> eGetGeomColumnNameListBySql(String dynamicSql, GirSqlParam sqlParam) {
        List<String> names = new ArrayList<>();
        for (FieldBySchemaApo field : eGetGeomColumnListBySql(dynamicSql, sqlParam)) names.add(field.getColumnName());
        return names;
    }

    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListBySql(String dynamicSql, GirSqlParam sqlParam) {
        return geometryOnly(ddlOpt.dGetColumnsBySQL(dynamicSql, sqlParam).geomFields());
    }

    @Override
    public FieldBySchemaApo eGetGeomColumnBySql(String dynamicSql, GirSqlParam sqlParam) {
        List<FieldBySchemaApo> fields = eGetGeomColumnListBySql(dynamicSql, sqlParam);
        return fields.isEmpty() ? null : fields.get(0);
    }

    @Override
    public Map<String, Integer> eGetSrid(String tableNameOrSqlView, List<String> geomFieldNames) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (CollectionUtil.isEmpty(geomFieldNames)) return result;
        for (String field : geomFieldNames) result.put(field, eGetSrid(tableNameOrSqlView, field));
        return result;
    }

    @Override
    public void eAddGeomColumn(String tableName, String field, AdvEnumsTypeGeom geomType, int srid) {
        validateTableName(tableName); validateGeomFieldName(field); validateSrid(srid);
        if (!ddlOpt.dIsTableExists(tableName)) throw new IllegalArgumentException("表不存在：" + tableName);
        if (ddlOpt.dGetColumnsByTable(tableName).findField(column -> field.equalsIgnoreCase(column.getColumnName())).isPresent()) {
            throw new IllegalArgumentException("字段已存在，无法添加 geometry 字段：" + field);
        }
        String table = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String quotedField = dialectTableNameProcessor.tbQuoteFieldName(field);
        List<String> statements = new ArrayList<>();
        statements.add("ALTER TABLE " + table + " ADD " + quotedField + " geometry NULL");
        String sridConstraint = buildGeomConstraintName(tableName, field, "srid");
        statements.add("ALTER TABLE " + table + " ADD CONSTRAINT "
                        + dialectTableNameProcessor.tbQuoteFieldName(sridConstraint)
                        + " CHECK (" + quotedField + " IS NULL OR " + quotedField + ".STSrid = " + srid + ")");
        if (geomType != null && geomType != AdvEnumsTypeGeom.Geometry && geomType != AdvEnumsTypeGeom.unknown) {
            String nativeType = geomType.getCode().toUpperCase(java.util.Locale.ROOT);
            String typeConstraint = buildGeomConstraintName(tableName, field, "type");
            statements.add("ALTER TABLE " + table + " ADD CONSTRAINT "
                            + dialectTableNameProcessor.tbQuoteFieldName(typeConstraint)
                            + " CHECK (" + quotedField + " IS NULL OR UPPER(" + quotedField + ".STGeometryType()) = '" + nativeType + "')");
        }
        ddlOpt.dExecuteDDL(String.join("; ", statements), tableName, "添加 SQL Server geometry 字段及约束");
    }

    @Override
    public void eDropGeomColumn(String tableName, String field) {
        validateTableName(tableName); validateGeomFieldName(field);
        assertGeometryField(tableName, field);
        String table = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        List<String> statements = new ArrayList<>();
        for (String indexName : getSpatialIndexNames(table, field)) {
            statements.add("DROP INDEX " + dialectTableNameProcessor.tbQuoteFieldName(indexName) + " ON " + table);
        }
        statements.add(buildDropGeomConstraintIfExistsSql(table, tableName, field, "type"));
        statements.add(buildDropGeomConstraintIfExistsSql(table, tableName, field, "srid"));
        statements.add("ALTER TABLE " + table + " DROP COLUMN " + dialectTableNameProcessor.tbQuoteFieldName(field));
        ddlOpt.dExecuteDDL(String.join("; ", statements), tableName, "删除 SQL Server geometry 字段及其空间索引");
    }

    /** 使用实际主键代替历史实现中硬编码的 id 字段。 */
    @Override
    public List<Object> eValidateGeometries(String tableName, String geomFieldName) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        List<String> primaryKeys = ddlOpt.dGetPrimaryKeys(tableName);
        if (primaryKeys.isEmpty()) {
            throw new IllegalStateException("无法返回无效几何记录：表未定义主键：" + tableName);
        }
        String table = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String selectedKeys = primaryKeys.stream().map(dialectTableNameProcessor::tbQuoteFieldName)
                .collect(java.util.stream.Collectors.joining(", "));
        String sql = "SELECT " + selectedKeys + " FROM " + table + " WHERE " + SqlServerSpatialSql.isValid(geomFieldName);
        List<Object> results = new ArrayList<>();
        for (GirAdvOneRow row : baseOpt.bSelectList(sql)) {
            if (primaryKeys.size() == 1) {
                results.add(row.get(primaryKeys.get(0)));
            } else {
                Map<String, Object> key = new LinkedHashMap<>();
                for (String primaryKey : primaryKeys) key.put(primaryKey, row.get(primaryKey));
                results.add(key);
            }
        }
        return results;
    }

    @Override
    public void eTransformSrid(String tableName, String field, int targetSrid) {
        throw new UnsupportedOperationException("SQL Server geometry 不提供坐标转换函数；请先使用 GirGeoTools 转换坐标，再写回 geometry 字段。targetSrid=" + targetSrid);
    }

    @Override
    public void eCreateSpatialIndex(String tableName, String field, String indexName) {
        validateTableName(tableName); validateGeomFieldName(field);
        if (StrUtil.isBlank(indexName)) throw new IllegalArgumentException("空间索引名不能为空");
        assertGeometryField(tableName, field);
        if (!hasClusteredPrimaryKey(tableName)) {
            throw new IllegalStateException("SQL Server 创建空间索引要求表存在聚簇主键，请先为表添加主键：" + tableName);
        }
        if (ddlOpt.dIndexesExists(tableName, indexName)) {
            String indexedField = getSpatialIndexField(tableName, indexName);
            if (indexedField != null && indexedField.equalsIgnoreCase(field)) {
                return;
            }
            if (indexedField != null) {
                throw new IllegalStateException("同名空间索引已绑定其他 geometry 字段：" + indexName + " -> " + indexedField);
            }
            throw new IllegalStateException("同名索引已存在但不是空间索引：" + indexName);
        }
        String table = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = "CREATE SPATIAL INDEX " + dialectTableNameProcessor.tbQuoteFieldName(indexName) + " ON " + table
                + " (" + dialectTableNameProcessor.tbQuoteFieldName(field) + ") USING GEOMETRY_AUTO_GRID";
        ddlOpt.dExecuteDDL(sql, tableName, "创建 SQL Server 空间索引");
    }

    @Override
    public void eDropSpatialIndex(String tableName, String indexName) {
        validateTableName(tableName);
        if (StrUtil.isBlank(indexName)) throw new IllegalArgumentException("空间索引名不能为空");
        if (!isSpatialIndex(tableName, indexName)) {
            throw new IllegalArgumentException("指定索引不存在或不是 SQL Server 空间索引：" + indexName);
        }
        String table = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        ddlOpt.dExecuteDDL("DROP INDEX " + dialectTableNameProcessor.tbQuoteFieldName(indexName) + " ON " + table, tableName, "删除 SQL Server 空间索引");
    }

    @Override public String getQueryIntersectsSql(String table, String field, String wkt, int srid) { return "SELECT * FROM " + table + " WHERE " + SqlServerSpatialSql.intersects(field, wkt, srid); }
    @Override public String getQueryWithinBBoxSql(String table, String field, String bbox, int srid) { return "SELECT * FROM " + table + " WHERE " + SqlServerSpatialSql.within(field, bbox, srid); }
    @Override public String getCalculateDistanceSql(String field, String wkt, int srid, String alias, String table) { return "SELECT *, " + SqlServerSpatialSql.distance(field, wkt, srid) + " AS " + dialectTableNameProcessor.tbQuoteFieldName(alias) + " FROM " + table; }
    @Override public String getCentroidSql(String field, String alias, String table) { return "SELECT *, " + SqlServerSpatialSql.centroid(field) + " AS " + dialectTableNameProcessor.tbQuoteFieldName(alias) + " FROM " + table; }
    @Override
    public String getValidateGeometriesSql(String table, String field) {
        List<String> primaryKeys = ddlOpt.dGetPrimaryKeys(table);
        if (primaryKeys.isEmpty()) {
            throw new IllegalStateException("无法构造无效几何查询：表未定义主键：" + table);
        }
        String selectedKeys = primaryKeys.stream().map(dialectTableNameProcessor::tbQuoteFieldName)
                .collect(java.util.stream.Collectors.joining(", "));
        return "SELECT " + selectedKeys + " FROM " + table + " WHERE " + SqlServerSpatialSql.isValid(field);
    }

    @Override
    protected String buildValidateGeometriesByPrimaryKeysSql(
            String qualifiedTableName, String geomFieldName, List<String> primaryKeys) {
        String selectedKeys = primaryKeys.stream()
                .map(dialectTableNameProcessor::tbQuoteFieldName)
                .collect(java.util.stream.Collectors.joining(", "));
        return "SELECT " + selectedKeys + " FROM " + qualifiedTableName
                + " WHERE " + SqlServerSpatialSql.isValid(geomFieldName);
    }
    @Override public String getRepairGeometriesSql(String table, String field) { String quoted = dialectTableNameProcessor.tbQuoteFieldName(field); return "UPDATE " + table + " SET " + quoted + "=" + SqlServerSpatialSql.makeValid(field) + " WHERE " + quoted + ".STIsValid()=0"; }
    @Override public String getGetExtentSql(String field, String table, int srid) { return "SELECT geometry::EnvelopeAggregate(" + dialectTableNameProcessor.tbQuoteFieldName(field) + ").STAsText() AS extent FROM " + table; }

    private List<FieldBySchemaApo> geometryOnly(List<FieldBySchemaApo> fields) {
        List<FieldBySchemaApo> result = new ArrayList<>();
        for (FieldBySchemaApo field : fields) {
            if ("geometry".equalsIgnoreCase(field.getUdtName())) {
                result.add(field);
            }
        }
        return result;
    }

    private String buildGeomConstraintName(String tableName, String fieldName, String suffix) {
        String base = "ck_" + dialectTableNameProcessor.tbGetTableNameNotSchema(tableName) + "_" + fieldName + "_" + suffix;
        if (base.length() <= 128) {
            return base;
        }
        String hash = Integer.toHexString(base.hashCode());
        return base.substring(0, 128 - hash.length() - 1) + "_" + hash;
    }

    private String buildDropGeomConstraintIfExistsSql(String qualifiedTable, String tableName, String fieldName, String suffix) {
        String constraint = buildGeomConstraintName(tableName, fieldName, suffix);
        return "IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'"
                + qualifiedTable.replace("'", "''") + "') AND name = N'" + constraint.replace("'", "''") + "') "
                + "ALTER TABLE " + qualifiedTable + " DROP CONSTRAINT " + dialectTableNameProcessor.tbQuoteFieldName(constraint);
    }

    /** SQL Server 空间索引依赖 geometry 字段，删除字段前必须先删除这些索引。 */
    private List<String> getSpatialIndexNames(String qualifiedTable, String fieldName) {
        String sql = "SELECT i.name AS index_name FROM sys.indexes i JOIN sys.index_columns ic "
                + "ON i.object_id=ic.object_id AND i.index_id=ic.index_id JOIN sys.columns c "
                + "ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE i.type=4 "
                + "AND i.object_id=OBJECT_ID(N'" + qualifiedTable.replace("'", "''") + "') AND c.name=N'"
                + fieldName.replace("'", "''") + "'";
        List<String> result = new ArrayList<>();
        for (GirAdvOneRow row : baseOpt.bSelectList(sql)) {
            result.add(row.getStr("index_name"));
        }
        return result;
    }

    /** SQL Server 的 geometry 空间索引要求同表存在聚簇主键。 */
    private boolean hasClusteredPrimaryKey(String tableName) {
        String table = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = "SELECT COUNT(1) AS cnt FROM sys.indexes WHERE object_id=OBJECT_ID(N'"
                + table.replace("'", "''") + "') AND is_primary_key=1 AND type=1";
        GirAdvOneRow row = baseOpt.bSelectOne(sql);
        return row != null && row.getInt("cnt") > 0;
    }

    /** 防止空间 API 因调用方误传字段名而删除或索引普通业务字段。 */
    private void assertGeometryField(String tableName, String fieldName) {
        boolean geometryField = eGetGeomColumnNameListByTable(tableName).stream()
                .anyMatch(column -> column.equalsIgnoreCase(fieldName));
        if (!geometryField) {
            throw new IllegalArgumentException("字段不是 SQL Server geometry 类型：" + fieldName);
        }
    }

    /** 仅将 sys.indexes.type=4 认定为 SQL Server 空间索引。 */
    private boolean isSpatialIndex(String tableName, String indexName) {
        return getSpatialIndexField(tableName, indexName) != null;
    }

    /** 查询空间索引绑定的 geometry 字段；不存在或非空间索引时返回 {@code null}。 */
    private String getSpatialIndexField(String tableName, String indexName) {
        String table = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = "SELECT c.name AS field_name FROM sys.indexes i JOIN sys.index_columns ic "
                + "ON i.object_id=ic.object_id AND i.index_id=ic.index_id JOIN sys.columns c "
                + "ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE i.object_id=OBJECT_ID(N'"
                + table.replace("'", "''") + "') AND i.type=4 AND i.name=N'" + indexName.replace("'", "''") + "'";
        GirAdvOneRow row = baseOpt.bSelectOne(sql);
        return row == null ? null : row.getStr("field_name");
    }
}
