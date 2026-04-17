package cn.geoair.map.dynamic.adv.query.dialect.oracle;

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
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

/**
 * Oracle Spatial 空间操作实现类
 * <p>使用 Oracle Spatial 的 MDSYS.SDO_GEOMETRY 类型和相关函数</p>
 *
 * @author zhangjun
 */
@Slf4j
public class OracleAdvGeoOpt extends AbstractExecAdvGeoOpt {

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
        // Oracle Spatial 类型：SDO_GEOMETRY
        // 实际几何类型存储在 METADATA 中
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

    // ===================== Oracle Spatial 特有方法 =====================

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
        String schemaName = dataSourceGetter.getSchemaName();
        String sql = StrUtil.format(
                "SELECT TABLE_NAME FROM USER_TAB_COLUMNS " +
                        "WHERE DATA_TYPE = 'SDO_GEOMETRY' AND OWNER = UPPER('{}') " +
                        "GROUP BY TABLE_NAME",
                schemaName);

        List<GirAdvOneRow> result = baseOpt.bSelectList(sql);
        List<String> layerNames = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(result)) {
            result.forEach(row -> layerNames.add(row.getStr("TABLE_NAME")));
        }
        return layerNames;
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeByTable(String tableName, List<String> geomFieldNames) {
        validateTableName(tableName);
        if (CollectionUtil.isEmpty(geomFieldNames)) {
            return MapUtil.empty();
        }

        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);

        // Oracle 从 USER_SDO_GEOM_METADATA 获取几何类型
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COLUMN_NAME, DIMINFO FROM USER_SDO_GEOM_METADATA WHERE TABLE_NAME = UPPER('");
        sql.append(qualifiedTableName.toUpperCase()).append("')");

        List<GirAdvOneRow> rows = baseOpt.bSelectList(sql.toString());

        Map<String, AdvEnumsTypeGeom> resultMap = new HashMap<>();
        for (String field : geomFieldNames) {
            // 尝试从元数据获取类型
            for (GirAdvOneRow row : rows) {
                if (field.equalsIgnoreCase(row.getStr("COLUMN_NAME"))) {
                    // Oracle 没有直接存储几何类型，需要从数据推断
                    resultMap.put(field, inferGeometryType(tableName, field));
                    break;
                }
            }
        }
        return resultMap;
    }

    /**
     * 从数据推断几何类型
     */
    private AdvEnumsTypeGeom inferGeometryType(String tableName, String geomFieldName) {
        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = StrUtil.format(
                "SELECT CASE " +
                        "  WHEN SDO_GEOMETRY.GET_GTYPE({}) = 1 THEN 'POINT' " +
                        "  WHEN SDO_GEOMETRY.GET_GTYPE({}) = 2 THEN 'LINESTRING' " +
                        "  WHEN SDO_GEOMETRY.GET_GTYPE({}) = 3 THEN 'POLYGON' " +
                        "  WHEN SDO_GEOMETRY.GET_GTYPE({}) = 4 THEN 'COLLECTION' " +
                        "  WHEN SDO_GEOMETRY.GET_GTYPE({}) = 5 THEN 'MULTIPOINT' " +
                        "  WHEN SDO_GEOMETRY.GET_GTYPE({}) = 6 THEN 'MULTILINESTRING' " +
                        "  WHEN SDO_GEOMETRY.GET_GTYPE({}) = 7 THEN 'MULTIPOLYGON' " +
                        "  ELSE 'GEOMETRY' END AS GEOM_TYPE " +
                        "FROM {} WHERE {} IS NOT NULL AND ROWNUM = 1",
                geomFieldName, geomFieldName, geomFieldName, geomFieldName,
                geomFieldName, geomFieldName, geomFieldName,
                qualifiedTableName, geomFieldName);

        GirAdvOneRow row = baseOpt.bSelectOne(sql);
        if (row != null) {
            return getTypeGeomEnum(row.getStr("GEOM_TYPE"));
        }
        return AdvEnumsTypeGeom.Geometry;
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(String sqlView, List<String> geomFieldNames) {
        // 简化实现，从查询结果推断
        Map<String, AdvEnumsTypeGeom> resultMap = new HashMap<>();
        if (CollectionUtil.isNotEmpty(geomFieldNames)) {
            for (String field : geomFieldNames) {
                resultMap.put(field, AdvEnumsTypeGeom.Geometry);
            }
        }
        return resultMap;
    }

    @Override
    public List<String> eGetGeomColumnNameListByTable(String tableName) {
        validateTableName(tableName);
        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);

        String sql = StrUtil.format(
                "SELECT COLUMN_NAME FROM USER_TAB_COLUMNS " +
                        "WHERE TABLE_NAME = UPPER('{}') AND DATA_TYPE = 'SDO_GEOMETRY'",
                qualifiedTableName.toUpperCase());

        List<GirAdvOneRow> rows = baseOpt.bSelectList(sql);
        List<String> names = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(rows)) {
            rows.forEach(row -> names.add(row.getStr("COLUMN_NAME")));
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
                if ("SDO_GEOMETRY".equalsIgnoreCase(colType)) {
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

        String qualifiedName;
        if (ddlOpt.dIsTableExists(tableNameOrSqlView)) {
            qualifiedName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameOrSqlView);
        } else {
            qualifiedName = StrUtil.format("({})", tableNameOrSqlView);
        }

        // Oracle 从 USER_SDO_GEOM_METADATA 获取 SRID
        String sql = StrUtil.format(
                "SELECT DIMINFO FROM USER_SDO_GEOM_METADATA " +
                        "WHERE TABLE_NAME = UPPER('{}') AND COLUMN_NAME = UPPER('{}')",
                qualifiedName.toUpperCase(), geomFieldName.toUpperCase());

        GirAdvOneRow row = baseOpt.bSelectOne(sql);
        // Oracle SRID 存储在 DIMINFO 中，简化返回 4326
        return row != null ? 4326 : 0;
    }

    @Override
    public Map<String, Integer> eGetSrid(String tableNameOrSqlView, List<String> geomFieldNames) {
        Map<String, Integer> sridMap = new HashMap<>();
        for (String field : geomFieldNames) {
            sridMap.put(field, eGetSrid(tableNameOrSqlView, field));
        }
        return sridMap;
    }

    // ===================== DDL 操作 =====================

    @Override
    public void eAddGeomColumn(String tableName, String geomFieldName, AdvEnumsTypeGeom geomType, int srid) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);

        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);

        // Oracle 添加 SDO_GEOMETRY 列
        String sql = StrUtil.format(
                "ALTER TABLE {} ADD {} SDO_GEOMETRY",
                qualifiedTableName, geomFieldName);
        ddlOpt.dExecuteDDL(sql, tableName, "添加空间字段[" + geomFieldName + "]");

        // 插入元数据到 USER_SDO_GEOM_METADATA
        String insertMetaSql = StrUtil.format(
                "INSERT INTO USER_SDO_GEOM_METADATA (TABLE_NAME, COLUMN_NAME, DIMINFO, SRID) " +
                        "VALUES (UPPER('{}'), UPPER('{}'), " +
                        "SDO_DIM_ARRAY(SDO_DIM_ELEMENT('X', -180, 180, 0.005), " +
                        "SDO_DIM_ELEMENT('Y', -90, 90, 0.005)), {})",
                qualifiedTableName, geomFieldName, srid);

        try {
            ddlOpt.dExecuteDDL(insertMetaSql, tableName, "插入空间元数据");
        } catch (Exception e) {
            log.warn("插入空间元数据失败: {}", e.getMessage());
        }

        // 创建空间索引
        String indexName = StrUtil.format("IDX_{}_{}", tableName, geomFieldName).toUpperCase();
        eCreateSpatialIndex(tableName, geomFieldName, indexName);
    }

    @Override
    public void eDropGeomColumn(String tableName, String geomFieldName) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);

        // 删除元数据
        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String deleteMetaSql = StrUtil.format(
                "DELETE FROM USER_SDO_GEOM_METADATA WHERE TABLE_NAME = UPPER('{}') AND COLUMN_NAME = UPPER('{}')",
                qualifiedTableName, geomFieldName);

        try {
            ddlOpt.dExecuteDDL(deleteMetaSql, tableName, "删除空间元数据");
        } catch (Exception e) {
            log.warn("删除空间元数据失败: {}", e.getMessage());
        }

        // 删除列
        String sql = StrUtil.format("ALTER TABLE {} DROP COLUMN {}", qualifiedTableName, geomFieldName);
        ddlOpt.dExecuteDDL(sql, tableName, "删除空间字段[" + geomFieldName + "]");
    }

    @Override
    public void eTransformSrid(String tableName, String geomFieldName, int targetSrid) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);

        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);

        // Oracle 使用 SDO_CS.TRANSFORM 转换 SRID
        String sql = StrUtil.format(
                "UPDATE {} SET {} = SDO_CS.TRANSFORM({}, {}) WHERE {} IS NOT NULL",
                qualifiedTableName, geomFieldName, geomFieldName, targetSrid, geomFieldName);

        ddlOpt.dExecuteDDL(sql, tableName, "转换SRID为" + targetSrid);

        // 更新元数据中的 SRID
        String updateMetaSql = StrUtil.format(
                "UPDATE USER_SDO_GEOM_METADATA SET SRID = {} WHERE TABLE_NAME = UPPER('{}') AND COLUMN_NAME = UPPER('{}')",
                targetSrid, qualifiedTableName, geomFieldName);

        try {
            ddlOpt.dExecuteDDL(updateMetaSql, tableName, "更新空间元数据SRID");
        } catch (Exception e) {
            log.warn("更新空间元数据SRID失败: {}", e.getMessage());
        }
    }

    @Override
    public void eCreateSpatialIndex(String tableName, String geomFieldName, String indexName) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);

        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);

        // Oracle 创建空间索引（R-tree）
        String sql = StrUtil.format(
                "CREATE INDEX {} ON {} ({}) INDEXTYPE IS MDSYS.SPATIAL_INDEX",
                indexName, qualifiedTableName, geomFieldName);

        ddlOpt.dExecuteDDL(sql, tableName, "创建空间索引[" + indexName + "]");
    }

    @Override
    public void eDropSpatialIndex(String tableName, String indexName) {
        String sql = StrUtil.format("DROP INDEX {}", indexName);
        ddlOpt.dExecuteDDL(sql, tableName, "删除空间索引[" + indexName + "]");
    }

    // ===================== 空间查询 SQL 构建 =====================

    @Override
    public String getQueryIntersectsSql(String qualifiedTableName, String geomFieldName, String geometry, int srid) {
        // Oracle 使用 SDO_RELATE 进行空间相交查询
        return StrUtil.format(
                "SELECT * FROM {} WHERE SDO_RELATE({}, SDO_GEOMETRY('{}', {}), 'MASK=ANYINTERACT') = 'TRUE'",
                qualifiedTableName, geomFieldName, geometry, srid);
    }

    @Override
    public String getQueryWithinBBoxSql(String qualifiedTableName, String geomFieldName, String bboxWkt, int srid) {
        // Oracle 使用 SDO_INSIDE 或 SDO_RELATE
        return StrUtil.format(
                "SELECT * FROM {} WHERE SDO_RELATE({}, SDO_GEOMETRY('{}', {}), 'MASK=INSIDE') = 'TRUE'",
                qualifiedTableName, geomFieldName, bboxWkt, srid);
    }

    @Override
    public String getCalculateDistanceSql(String geomFieldName, String geometry, int srid,
                                          String distanceAlias, String qualifiedTableName) {
        // Oracle 使用 SDO_GEOM.SDO_DISTANCE
        return StrUtil.format(
                "SELECT *, SDO_GEOM.SDO_DISTANCE({}, SDO_GEOMETRY('{}', {}), 0.005) AS {} FROM {}",
                geomFieldName, geometry, srid, distanceAlias, qualifiedTableName);
    }

    @Override
    public String getCentroidSql(String geomFieldName, String centerAlias, String qualifiedTableName) {
        // Oracle 使用 SDO_GEOM.SDO_CENTROID
        return StrUtil.format(
                "SELECT {}, SDO_GEOM.SDO_CENTROID({}, 0.005) AS {} FROM {}",
                "*", geomFieldName, centerAlias, qualifiedTableName);
    }

    @Override
    public String getValidateGeometriesSql(String qualifiedTableName, String geomFieldName) {
        // Oracle 使用 SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT
        return StrUtil.format(
                "SELECT id FROM {} WHERE SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT({}, 0.005) <> 'TRUE'",
                qualifiedTableName, geomFieldName);
    }

    @Override
    public String getRepairGeometriesSql(String qualifiedTableName, String geomFieldName) {
        // Oracle 没有直接的修复函数，使用 SDO_UTIL.RECTIFY_GEOMETRY
        return StrUtil.format(
                "UPDATE {} SET {} = SDO_UTIL.RECTIFY_GEOMETRY({}, 0.005) WHERE SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT({}, 0.005) <> 'TRUE'",
                qualifiedTableName, geomFieldName, geomFieldName, geomFieldName);
    }

    @Override
    public String getGetExtentSql(String geomFieldName, String qualifiedTableName, int srid) {
        // Oracle 使用 SDO_AGGR_MBR 获取最小包围矩形
        return StrUtil.format(
                "SELECT SDO_AGGR_MBR({}) AS extent FROM {}",
                geomFieldName, qualifiedTableName);
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
        // 简化实现
        return eGetGeomColumnNameBySql(dynamicSql);
    }
}
