package cn.geoair.map.dynamic.adv.query.dialect.oracle;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.apo.*;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvGeoOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;


import java.sql.*;
import java.util.*;

/**
 * Oracle Spatial 空间操作实现类
 * 已修复：所有查询字段增加双引号，强制小写别名
 *
 * @author zhangjun
 */

public class OracleAdvGeoOpt extends AbstractExecAdvGeoOpt {
    public static GiLogger log = GirLoggerFactory.getLogger();
    private final IAdvBaseOpt baseOpt;
    private final IAdvDDLOpt ddlOpt;

    public OracleAdvGeoOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt, IAdvDDLOpt ddlOpt) {
        super(dataSourceGetter);
        this.baseOpt = baseOpt;
        this.ddlOpt = ddlOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return OracleDialectTableNameUtil.getInstance();
    }

    @Override
    protected IAdvBaseOpt getAdvBaseOpt() {
        return baseOpt;
    }

    @Override
    protected IAdvDDLOpt getAdvDDLOpt() {
        return ddlOpt;
    }

    @Override
    protected AdvEnumsTypeGeom getTypeGeomEnum(String nativeGeomType) {
        if (StrUtil.isEmpty(nativeGeomType)) {
            return null;
        }
        String typeName = nativeGeomType.toUpperCase();
        if (typeName.contains("POINT")) {
            return AdvEnumsTypeGeom.Point;
        } else if (typeName.contains("MULTIPOINT")) {
            return AdvEnumsTypeGeom.MultiPoint;
        } else if (typeName.contains("LINESTRING")) {
            return AdvEnumsTypeGeom.LineString;
        } else if (typeName.contains("MULTILINESTRING")) {
            return AdvEnumsTypeGeom.MultiLineString;
        } else if (typeName.contains("POLYGON")) {
            return AdvEnumsTypeGeom.Polygon;
        } else if (typeName.contains("MULTIPOLYGON")) {
            return AdvEnumsTypeGeom.MultiPolygon;
        }
        return AdvEnumsTypeGeom.Geometry;
    }

    /**
     * 检查 Oracle Spatial 是否可用
     */
    private boolean isOracleSpatialAvailable() {
        try {
            baseOpt.bSelectOne("SELECT MDSYS.SDO_GEOMETRY FROM DUAL WHERE ROWNUM = 0");
            return true;
        } catch (Exception e) {
            log.warn("Oracle Spatial 不可用: {}", e.getMessage());
            return false;
        }
    }

    // ===================== 几何类型查询方法 =====================

    @Override
    public List<String> eGetAllGeoLayerName() {
        String schemaName = resolveOwner(null);
        String sql = StrUtil.format(
                "SELECT TABLE_NAME AS \"table_name\" FROM ALL_TAB_COLUMNS " +
                "WHERE DATA_TYPE = 'SDO_GEOMETRY' AND OWNER = UPPER('{}') " +
                "GROUP BY TABLE_NAME",
                schemaName);

        List<GirAdvOneRow> result = baseOpt.bSelectList(sql);
        List<String> layerNames = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(result)) {
            result.forEach(row -> layerNames.add(row.getStr("table_name")));
        }
        return layerNames;
    }

    @Override
    public List<String> eGetGeoLayerNameByKeyword(String layerNameKeyword) {
        if (StrUtil.isEmpty(layerNameKeyword)) {
            return eGetAllGeoLayerName();
        }
        String schemaName = resolveOwner(null);
        // 转义单引号防止 SQL 注入
        String safeKeyword = layerNameKeyword.replace("'", "''");
        String sql = StrUtil.format(
                "SELECT TABLE_NAME AS \"table_name\" FROM ALL_TAB_COLUMNS " +
                "WHERE DATA_TYPE = 'SDO_GEOMETRY' AND OWNER = UPPER('{}') " +
                "AND TABLE_NAME LIKE '%{}%' " +
                "GROUP BY TABLE_NAME",
                schemaName, safeKeyword);

        List<GirAdvOneRow> result = baseOpt.bSelectList(sql);
        List<String> layerNames = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(result)) {
            result.forEach(row -> layerNames.add(row.getStr("table_name")));
        }
        return layerNames;
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeByTable(String tableName, List<String> geomFieldNames) {
        validateTableName(tableName);
        if (CollectionUtil.isEmpty(geomFieldNames)) {
            return MapUtil.empty();
        }
        Map<String, AdvEnumsTypeGeom> resultMap = new HashMap<>();
        // 遍历每个几何字段，直接从表中解析 SDO_GTYPE 获取真实类型
        for (String field : geomFieldNames) {
            try {
                AdvEnumsTypeGeom advEnumsTypeGeom = inferGeometryType(tableName, field);
                resultMap.put(field, advEnumsTypeGeom);
            } catch (Exception e) {
                // 异常时跳过，不影响整体
                log.warn("解析几何类型失败：表={}, 字段={}", tableName, field);
            }
        }

        return resultMap;
    }

    /**
     * 从数据推断几何类型
     */
    private AdvEnumsTypeGeom inferGeometryType(String tableNameOrSqlView, String geomFieldName) {

        boolean b = dialectTableNameProcessor.tbTableIsSqlView(tableNameOrSqlView);

        String tableNameS = "";
        if (b) {
            tableNameS =  StrUtil.wrap(tableNameOrSqlView, "( ", " )");
            String aliasTableName = dialectTableNameProcessor.tbGetTempAliasTableName();
            tableNameS =   dialectTableNameProcessor.tbBuildAsTable(tableNameS,aliasTableName);
        } else {
            String tableNameWithSchema = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameOrSqlView);
            tableNameS = StrUtil.wrap(tableNameWithSchema, "( ", " )");
        }

        String quotedField = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        String sql = StrUtil.format(
                "SELECT CASE " +
                "  WHEN SDO_GEOMETRY.GET_GTYPE({}) = 1 THEN 'POINT' " +
                "  WHEN SDO_GEOMETRY.GET_GTYPE({}) = 2 THEN 'LINESTRING' " +
                "  WHEN SDO_GEOMETRY.GET_GTYPE({}) = 3 THEN 'POLYGON' " +
                "  WHEN SDO_GEOMETRY.GET_GTYPE({}) = 4 THEN 'COLLECTION' " +
                "  WHEN SDO_GEOMETRY.GET_GTYPE({}) = 5 THEN 'MULTIPOINT' " +
                "  WHEN SDO_GEOMETRY.GET_GTYPE({}) = 6 THEN 'MULTILINESTRING' " +
                "  WHEN SDO_GEOMETRY.GET_GTYPE({}) = 7 THEN 'MULTIPOLYGON' " +
                "  ELSE 'GEOMETRY' END AS \"geom_type\" " +
                "FROM {} WHERE {} IS NOT NULL AND ROWNUM = 1",
                quotedField, quotedField, quotedField, quotedField,
                quotedField, quotedField, quotedField,
                tableNameS, quotedField);

        GirAdvOneRow row = baseOpt.bSelectOne(sql);
        if (row != null) {
            return getTypeGeomEnum(row.getStr("geom_type"));
        }
        return AdvEnumsTypeGeom.Geometry;
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(String sqlView, List<String> geomFieldNames) {
        return eGetGeoTypeByTable(sqlView, geomFieldNames);
    }

    @Override
    public List<String> eGetGeomColumnNameListByTable(String tableName) {
        validateTableName(tableName);
        String nameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String owner = resolveOracleOwner(tableName);

        String sql = StrUtil.format(
                "SELECT COLUMN_NAME AS \"column_name\" FROM ALL_TAB_COLUMNS " +
                "WHERE OWNER = UPPER('{}') AND TABLE_NAME = UPPER('{}') AND DATA_TYPE = 'SDO_GEOMETRY'",
                owner,
                nameNotSchema);

        List<GirAdvOneRow> rows = baseOpt.bSelectList(sql);
        List<String> names = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(rows)) {
            rows.forEach(row -> names.add(row.getStr("column_name")));
        }
        return names;
    }

    @Override
    public String eGetGeomColumnNameBySql(String sqlView) {
        if (StrUtil.isEmpty(sqlView)) {
            return null;
        }

        String wrapperSql = StrUtil.format("SELECT * FROM ({}) WHERE ROWNUM = 0", sqlView);
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = dataSourceGetter.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(wrapperSql);
            ResultSetMetaData metaData = rs.getMetaData();

            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String colType = metaData.getColumnTypeName(i);
                if (colType.contains("SDO_GEOMETRY")) {  //还有可能是MDSYS.SDO_GEOMETRY
                    return metaData.getColumnName(i);
                }
            }
        } catch (SQLException e) {
            log.error("查询SQL空间字段失败: {}", e.getMessage(), e);
        } finally {
            dataSourceGetter.closeResources(rs, stmt, conn);
        }
        return null;
    }

    @Override
    public Integer eGetSrid(String tableNameOrSqlView, String geomFieldName) {
        if (StrUtil.isEmpty(tableNameOrSqlView) || StrUtil.isEmpty(geomFieldName)) {
            return 0;
        }

        Integer sridFromMeta = getSridFromMetadata(tableNameOrSqlView, geomFieldName);
        if (sridFromMeta != null && sridFromMeta > 0) {
            return sridFromMeta;
        }
        String qualifiedName = null;
        String aliasTableName = dialectTableNameProcessor.tbGetTempAliasTableName();
        if (isTableOrViewName(tableNameOrSqlView)) {
            qualifiedName = StrUtil.format(
                    "({}) {}",
                    dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameOrSqlView), aliasTableName);
        } else {
            qualifiedName = StrUtil.format(
                    "({}) {}",
                    dialectTableNameProcessor.tbRemoveSqlSpaces(tableNameOrSqlView), aliasTableName
            );
        }

        String quotedGeomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        String sql = StrUtil.format(
                "SELECT {}.{}.SDO_SRID AS \"srid\" FROM {} WHERE {} IS NOT NULL AND ROWNUM = 1",
                aliasTableName,
                quotedGeomFieldName,
                qualifiedName,
                quotedGeomFieldName);
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        return row != null ? row.getInt("srid", 0) : 0;
    }

    @Override
    public Map<String, Integer> eGetSrid(String tableNameOrSqlView, List<String> geomFieldNames) {
        if (StrUtil.isEmpty(tableNameOrSqlView) || CollectionUtil.isEmpty(geomFieldNames)) {
            return MapUtil.empty();
        }
        Map<String, Integer> sridMap = new HashMap<>();
        for (String geomFieldName : geomFieldNames) {
            sridMap.put(geomFieldName, eGetSrid(tableNameOrSqlView, geomFieldName));
        }
        return sridMap;
    }

    /**
     * 从 Oracle 空间元数据表中获取 SRID
     */
    private Integer getSridFromMetadata(String tableNameOrSqlView, String geomFieldName) {
        String owner = resolveOwner(tableNameOrSqlView);
        if (StrUtil.isEmpty(owner)) {
            return null;
        }

        String tableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableNameOrSqlView);
        String columnName = dialectTableNameProcessor.tbUnquoteTableName(geomFieldName);

        try {
            String allMetaSql = StrUtil.format(
                    "SELECT SRID AS \"srid\" FROM ALL_SDO_GEOM_METADATA WHERE OWNER = UPPER('{}') AND TABLE_NAME = UPPER('{}') AND COLUMN_NAME = UPPER('{}')",
                    owner,
                    tableName,
                    columnName);
            GirAdvOneRow row = getAdvBaseOpt().bSelectOne(allMetaSql);
            if (row != null) {
                return row.getInt("srid", 0);
            }
        } catch (Exception ignored) {
        }

        try {
            String userMetaSql = StrUtil.format(
                    "SELECT SRID AS \"srid\" FROM USER_SDO_GEOM_METADATA WHERE TABLE_NAME = UPPER('{}') AND COLUMN_NAME = UPPER('{}')",
                    tableName,
                    columnName);
            GirAdvOneRow row = getAdvBaseOpt().bSelectOne(userMetaSql);
            if (row != null) {
                return row.getInt("srid", 0);
            }
        } catch (Exception ignored) {
        }
        return null;
    }



    /**
     * 判断是否为实际存在的表名或视图名
     */
    private boolean isTableOrViewName(String tableNameOrSqlView) {
        if (StrUtil.isEmpty(tableNameOrSqlView) || dialectTableNameProcessor.tbTableIsSqlView(tableNameOrSqlView)) {
            return false;
        }
        String name = dialectTableNameProcessor.tbGetTableNameNotSchema(tableNameOrSqlView);
        String schema = dialectTableNameProcessor.tbExtractSchemaName(tableNameOrSqlView);
        List<SchemaTableApo> objects = StrUtil.isNotEmpty(schema)
                ? getAdvDDLOpt().dGetTableAndViewBySchema(schema)
                : getAdvDDLOpt().dGetTableAndViewBySchema();
        if (CollectionUtil.isEmpty(objects)) {
            return false;
        }
        for (SchemaTableApo object : objects) {
            if (object != null && StrUtil.equalsIgnoreCase(name, object.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析表的 owner（schema）
     */
    private String resolveOwner(String tableNameOrSqlView) {
        String schemaName = dialectTableNameProcessor.tbExtractSchemaName(tableNameOrSqlView);
        if (StrUtil.isNotEmpty(schemaName)) {
            return schemaName.toUpperCase();
        }

        String currentSchema = dataSourceGetter.getSchemaName();
        if (StrUtil.isNotEmpty(currentSchema)) {
            return currentSchema.toUpperCase();
        }

        String detectedSchema = getAdvDDLOpt().dGetCurrentSchema();
        return StrUtil.isEmpty(detectedSchema) ? null : detectedSchema.toUpperCase();
    }

    // ===================== DDL 操作 =====================

    @Override
    public void eAddGeomColumn(String tableName, String geomFieldName, AdvEnumsTypeGeom geomType, int srid) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        validateSrid(srid);
        if (!ddlOpt.dIsTableExists(tableName)) {
            throw new IllegalArgumentException("表不存在：" + tableName);
        }
        if (ddlOpt.dGetColumnsByTable(tableName)
                .findField(field -> geomFieldName.equalsIgnoreCase(field.getColumnName())).isPresent()) {
            throw new IllegalArgumentException("字段已存在，无法添加空间字段：" + geomFieldName);
        }

        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String metadataTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String indexName = buildDefaultSpatialIndexName(tableName, geomFieldName);

        String quotedGeomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        String sql = StrUtil.format(
                "ALTER TABLE {} ADD {} SDO_GEOMETRY",
                qualifiedTableName, quotedGeomFieldName);
        String insertMetaSql = StrUtil.format(
                "INSERT INTO USER_SDO_GEOM_METADATA (TABLE_NAME, COLUMN_NAME, DIMINFO, SRID) " +
                "VALUES (UPPER('{}'), UPPER('{}'), " +
                "SDO_DIM_ARRAY(SDO_DIM_ELEMENT('X', -180, 180, 0.005), " +
                "SDO_DIM_ELEMENT('Y', -90, 90, 0.005)), {})",
                metadataTableName, geomFieldName, srid);

        boolean fieldCreated = false;
        try {
            // Oracle 的 DDL 会隐式提交，因此后续步骤失败时只能进行补偿清理。
            ddlOpt.dExecuteDDL(sql, tableName, "添加空间字段[" + geomFieldName + "]");
            fieldCreated = true;
            ddlOpt.dExecuteDDL(insertMetaSql, tableName, "插入空间元数据");
            eCreateSpatialIndex(tableName, geomFieldName, indexName);
        } catch (Exception e) {
            if (fieldCreated) {
                compensateAddGeomColumnFailure(tableName, qualifiedTableName, metadataTableName, geomFieldName, indexName);
            }
            throw new RuntimeException("添加 Oracle 空间字段、元数据或索引失败；已尝试补偿清理", e);
        }
    }

    @Override
    public void eDropGeomColumn(String tableName, String geomFieldName) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        if (!eGetGeomColumnNameListByTable(tableName).stream()
                .anyMatch(column -> geomFieldName.equalsIgnoreCase(column))) {
            throw new IllegalArgumentException("指定字段不是 Oracle SDO_GEOMETRY 字段：" + geomFieldName);
        }

        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String metadataTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String quotedGeomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        for (String indexName : findSpatialIndexNames(tableName, geomFieldName)) {
            ddlOpt.dExecuteDDL(
                    StrUtil.format("DROP INDEX {}", dialectTableNameProcessor.tbQuoteFieldName(indexName)),
                    tableName,
                    "删除空间字段关联索引[" + indexName + "]");
        }
        String sql = StrUtil.format("ALTER TABLE {} DROP COLUMN {}", qualifiedTableName, quotedGeomFieldName);
        ddlOpt.dExecuteDDL(sql, tableName, "删除空间字段[" + geomFieldName + "]");
        String deleteMetaSql = StrUtil.format(
                "DELETE FROM USER_SDO_GEOM_METADATA WHERE TABLE_NAME = UPPER('{}') AND COLUMN_NAME = UPPER('{}')",
                metadataTableName, geomFieldName);
        try {
            ddlOpt.dExecuteDDL(deleteMetaSql, tableName, "删除空间元数据");
        } catch (Exception e) {
            throw new IllegalStateException("Oracle 空间字段已删除，但空间元数据清理失败；请手动清理 USER_SDO_GEOM_METADATA，表="
                    + metadataTableName + "，字段=" + geomFieldName, e);
        }
    }

    @Override
    public void eTransformSrid(String tableName, String geomFieldName, int targetSrid) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        validateSrid(targetSrid);
        if (!ddlOpt.dIsTableExists(tableName)) {
            throw new IllegalArgumentException("表不存在：" + tableName);
        }

        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String metadataTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);

        String quotedGeomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        String sql = StrUtil.format(
                "UPDATE {} SET {} = SDO_CS.TRANSFORM({}, {}) WHERE {} IS NOT NULL",
                qualifiedTableName, quotedGeomFieldName, quotedGeomFieldName, targetSrid, quotedGeomFieldName);

        String updateMetaSql = StrUtil.format(
                "UPDATE USER_SDO_GEOM_METADATA SET SRID = {} WHERE TABLE_NAME = UPPER('{}') AND COLUMN_NAME = UPPER('{}')",
                targetSrid, metadataTableName, geomFieldName);
        // 两条均为 DML；放在同一连接中，避免数据已转换而元数据仍指向旧 SRID。
        ddlOpt.dExecuteStatements(
                Arrays.asList(sql, updateMetaSql), tableName, "Oracle Spatial SRID转换及元数据同步为" + targetSrid);
    }

    @Override
    public void eCreateSpatialIndex(String tableName, String geomFieldName, String indexName) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        if (StrUtil.isBlank(indexName)) {
            throw new IllegalArgumentException("空间索引名不能为空");
        }
        if (!eGetGeomColumnNameListByTable(tableName).stream()
                .anyMatch(column -> geomFieldName.equalsIgnoreCase(column))) {
            throw new IllegalArgumentException("指定字段不是 Oracle SDO_GEOMETRY 字段：" + geomFieldName);
        }

        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String quotedGeomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        String sql = StrUtil.format(
                "CREATE INDEX {} ON {} ({}) INDEXTYPE IS MDSYS.SPATIAL_INDEX",
                dialectTableNameProcessor.tbQuoteFieldName(indexName), qualifiedTableName, quotedGeomFieldName);

        ddlOpt.dExecuteDDL(sql, tableName, "创建空间索引[" + indexName + "]");
    }

    @Override
    public void eDropSpatialIndex(String tableName, String indexName) {
        validateTableName(tableName);
        if (StrUtil.isBlank(indexName)) {
            throw new IllegalArgumentException("索引名不能为空");
        }
        String sql = StrUtil.format("DROP INDEX {}", dialectTableNameProcessor.tbQuoteFieldName(indexName));
        ddlOpt.dExecuteDDL(sql, tableName, "删除空间索引[" + indexName + "]");
    }

    // ===================== 空间查询 SQL 构建 =====================

    @Override
    public String getQueryIntersectsSql(String qualifiedTableName, String geomFieldName, String geometry, int srid) {
        String quotedGeomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        return StrUtil.format(
                "SELECT * FROM {} WHERE SDO_RELATE({}, SDO_GEOMETRY('{}', {}), 'MASK=ANYINTERACT') = 'TRUE'",
                qualifiedTableName, quotedGeomFieldName, escapeSqlLiteral(geometry), srid);
    }

    @Override
    public String getQueryWithinBBoxSql(String qualifiedTableName, String geomFieldName, String bboxWkt, int srid) {
        String quotedGeomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        return StrUtil.format(
                "SELECT * FROM {} WHERE SDO_RELATE({}, SDO_GEOMETRY('{}', {}), 'MASK=INSIDE') = 'TRUE'",
                qualifiedTableName, quotedGeomFieldName, escapeSqlLiteral(bboxWkt), srid);
    }

    @Override
    public String getCalculateDistanceSql(String geomFieldName, String geometry, int srid,
                                          String distanceAlias, String qualifiedTableName) {
        String quotedGeomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        return StrUtil.format(
                "SELECT *, SDO_GEOM.SDO_DISTANCE({}, SDO_GEOMETRY('{}', {}), 0.005) AS {} FROM {}",
                quotedGeomFieldName, escapeSqlLiteral(geometry), srid,
                dialectTableNameProcessor.tbQuoteFieldName(distanceAlias), qualifiedTableName);
    }

    @Override
    public String getCentroidSql(String geomFieldName, String centerAlias, String qualifiedTableName) {
        String quotedGeomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        return StrUtil.format(
                "SELECT {}, SDO_GEOM.SDO_CENTROID({}, 0.005) AS {} FROM {}",
                "*", quotedGeomFieldName,
                dialectTableNameProcessor.tbQuoteFieldName(centerAlias), qualifiedTableName);
    }

    @Override
    public String getValidateGeometriesSql(String qualifiedTableName, String geomFieldName) {
        String quotedGeomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        return StrUtil.format(
                "SELECT id FROM {} WHERE SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT({}, 0.005) <> 'TRUE'",
                qualifiedTableName, quotedGeomFieldName);
    }

    @Override
    protected String buildValidateGeometriesByPrimaryKeysSql(
            String qualifiedTableName, String geomFieldName, List<String> primaryKeys) {
        String selectedKeys = primaryKeys.stream()
                .map(dialectTableNameProcessor::tbQuoteFieldName)
                .collect(java.util.stream.Collectors.joining(", "));
        return StrUtil.format(
                "SELECT {} FROM {} WHERE SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT({}, 0.005) <> 'TRUE'",
                selectedKeys,
                qualifiedTableName,
                dialectTableNameProcessor.tbQuoteFieldName(geomFieldName));
    }

    @Override
    public String getRepairGeometriesSql(String qualifiedTableName, String geomFieldName) {
        String quotedGeomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        return StrUtil.format(
                "UPDATE {} SET {} = SDO_UTIL.RECTIFY_GEOMETRY({}, 0.005) WHERE SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT({}, 0.005) <> 'TRUE'",
                qualifiedTableName, quotedGeomFieldName, quotedGeomFieldName, quotedGeomFieldName);
    }

    @Override
    public String getGetExtentSql(String geomFieldName, String qualifiedTableName, int srid) {
        String quotedGeomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        return StrUtil.format(
                "SELECT "
                + "SDO_GEOM.SDO_MIN_MBR_ORDINATE(extent, 1) AS \"minx\","
                + "SDO_GEOM.SDO_MIN_MBR_ORDINATE(extent, 2) AS \"miny\","
                + "SDO_GEOM.SDO_MAX_MBR_ORDINATE(extent, 1) AS \"maxx\","
                + "SDO_GEOM.SDO_MAX_MBR_ORDINATE(extent, 2) AS \"maxy\","
                + "SDO_GEOM.SDO_MIN_MBR_ORDINATE(SDO_CS.TRANSFORM(extent, 4326), 1) AS \"minx_gs\","
                + "SDO_GEOM.SDO_MIN_MBR_ORDINATE(SDO_CS.TRANSFORM(extent, 4326), 2) AS \"miny_gs\","
                + "SDO_GEOM.SDO_MAX_MBR_ORDINATE(SDO_CS.TRANSFORM(extent, 4326), 1) AS \"maxx_gs\","
                + "SDO_GEOM.SDO_MAX_MBR_ORDINATE(SDO_CS.TRANSFORM(extent, 4326), 2) AS \"maxy_gs\" "
                + "FROM ( SELECT SDO_AGGR_MBR({}) AS extent FROM {} )",
                quotedGeomFieldName,
                qualifiedTableName);
    }

    // ===================== 带参数的查询方法 =====================

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(String dynamicSql, GirSqlParam sqlParam,
                                                          List<String> geomFieldNames) {
        Map<String, AdvEnumsTypeGeom> resultMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(geomFieldNames)) {
            for (String field : geomFieldNames) {
                resultMap.put(field, AdvEnumsTypeGeom.Geometry);
            }
        }
        return resultMap;
    }

    @Override
    public String eGetGeomColumnNameBySql(String dynamicSql, GirSqlParam sqlParam) {
        return eGetGeomColumnNameBySql(dynamicSql);
    }

    /**
     * Oracle DDL 隐式提交，新增字段后的元数据或索引步骤失败时无法通过 rollback 恢复。
     * 此处仅回退本次生成的索引、元数据和字段，所有补偿失败都会保留在日志中供人工处理。
     */
    private void compensateAddGeomColumnFailure(
            String tableName,
            String qualifiedTableName,
            String metadataTableName,
            String geomFieldName,
            String indexName) {
        try {
            if (ddlOpt.dIndexesExists(tableName, indexName)) {
                ddlOpt.dExecuteDDL(
                        StrUtil.format("DROP INDEX {}", dialectTableNameProcessor.tbQuoteFieldName(indexName)),
                        tableName,
                        "回退Oracle空间索引");
            }
            ddlOpt.dExecuteDDL(
                    StrUtil.format("DELETE FROM USER_SDO_GEOM_METADATA WHERE TABLE_NAME = UPPER('{}') AND COLUMN_NAME = UPPER('{}')",
                            metadataTableName, geomFieldName),
                    tableName,
                    "回退Oracle空间元数据");
            if (ddlOpt.dGetColumnsByTable(tableName)
                    .findField(field -> geomFieldName.equalsIgnoreCase(field.getColumnName())).isPresent()) {
                ddlOpt.dExecuteDDL(
                        StrUtil.format("ALTER TABLE {} DROP COLUMN {}", qualifiedTableName,
                                dialectTableNameProcessor.tbQuoteFieldName(geomFieldName)),
                        tableName,
                        "回退Oracle空间字段");
            }
        } catch (Exception compensationError) {
            log.error("Oracle 添加空间字段失败后的补偿未完成，表={}, 字段={}, 索引={}",
                    tableName, geomFieldName, indexName, compensationError);
        }
    }

    /** 将 WKT 等外部文本安全嵌入 SQL 字符串字面量。 */
    private static String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    /**
     * 获取目标表所属的 Oracle Schema，避免 {@code ALL_*} 数据字典视图跨 Schema 混入同名表。
     */
    private String resolveOracleOwner(String tableName) {
        String owner = resolveOwner(tableName);
        if (StrUtil.isBlank(owner)) {
            throw new IllegalStateException("无法确定 Oracle 表所属 Schema：" + tableName);
        }
        return owner;
    }

    /**
     * 查询绑定到指定空间字段的 domain 索引。删除列前先显式删除这些索引，避免不同 Oracle
     * 版本对 Spatial 索引依赖关系的处理差异导致 DDL 失败或留下无效索引。
     */
    private List<String> findSpatialIndexNames(String tableName, String geomFieldName) {
        String owner = resolveOracleOwner(tableName);
        String plainTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String sql = StrUtil.format(
                "SELECT c.INDEX_NAME AS \"index_name\" "
                        + "FROM ALL_IND_COLUMNS c "
                        + "JOIN ALL_INDEXES i ON i.OWNER = c.INDEX_OWNER AND i.INDEX_NAME = c.INDEX_NAME "
                        + "WHERE c.TABLE_OWNER = UPPER('{}') AND c.TABLE_NAME = UPPER('{}') "
                        + "AND c.COLUMN_NAME = UPPER('{}') AND i.INDEX_TYPE = 'DOMAIN'",
                owner,
                plainTableName,
                geomFieldName);
        List<String> indexNames = new ArrayList<>();
        for (GirAdvOneRow row : baseOpt.bSelectList(sql)) {
            String indexName = row.getStr("index_name");
            if (StrUtil.isNotBlank(indexName)) {
                indexNames.add(indexName);
            }
        }
        return indexNames;
    }

    /**
     * 生成同时兼容 Oracle 12.1 及更高版本的空间索引名。Oracle 12.1 的标识符上限为 30
     * 个字符；截断部分追加稳定哈希，避免长表名、字段名组合发生碰撞。
     */
    private String buildDefaultSpatialIndexName(String tableName, String geomFieldName) {
        String source = "IDX_" + dialectTableNameProcessor.tbGetTableNameNotSchema(tableName)
                + "_" + geomFieldName;
        String normalized = source.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_$#]", "_");
        if (normalized.length() <= 30) {
            return normalized;
        }
        String suffix = "_" + Integer.toHexString(normalized.hashCode()).toUpperCase(Locale.ROOT);
        return normalized.substring(0, 30 - suffix.length()) + suffix;
    }
}
