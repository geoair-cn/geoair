package cn.geoair.map.dynamic.adv.query.dialect.mysql;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.apo.BBoxApo;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvGeoOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

import java.sql.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MySQL（Spatial）空间操作实现类 基于MySQL Spatial扩展实现通用空间操作接口 适配MySQL 5.7+/8.0+ Spatial语法特性
 */
public class MysqlAdvGeoOpt extends AbstractExecAdvGeoOpt {

    private static final Logger log = LoggerFactory.getLogger(MysqlAdvGeoOpt.class);

    private IAdvBaseOpt baseOpt;

    private IAdvDDLOpt ddlOpt;

    public MysqlAdvGeoOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt, IAdvDDLOpt ddlOpt) {
        super(dataSourceGetter);
        this.baseOpt = baseOpt;
        this.ddlOpt = ddlOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return MysqlDialectTableNameUtil.getInstance();
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
        // MySQL几何类型：POINT, MULTIPOINT, LINESTRING, MULTILINESTRING, POLYGON,
        // MULTIPOLYGON, GEOMETRY
        String typeName = nativeGeomType.toLowerCase();
        if ("point".equals(typeName)) {
            return AdvEnumsTypeGeom.Point;
        } else if ("multipoint".equals(typeName)) {
            return AdvEnumsTypeGeom.MultiPoint;
        } else if ("linestring".equals(typeName)) {
            return AdvEnumsTypeGeom.LineString;
        } else if ("multilinestring".equals(typeName)) {
            return AdvEnumsTypeGeom.MultiLineString;
        } else if ("polygon".equals(typeName)) {
            return AdvEnumsTypeGeom.Polygon;
        } else if ("multipolygon".equals(typeName)) {
            return AdvEnumsTypeGeom.MultiPolygon;
        } else {
            return AdvEnumsTypeGeom.Geometry;
        }
    }

    @Override
    public List<String> eGetAllGeoLayerName() {
        String sql =
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE DATA_TYPE IN ('geometry','point','linestring','polygon','multipoint','multilinestring','multipolygon') "
                + "AND TABLE_SCHEMA = #{schema} "
                + "GROUP BY TABLE_NAME;";

        SqlParamMap paramMap = new SqlParamMap();
        paramMap.put("schema", dataSourceGetter.getSchemaName());

        List<GirAdvOneRow> result = baseOpt.bSelectList(sql, paramMap);
        List<String> layerNames = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(result)) {
            result.forEach(row -> layerNames.add(row.getStr("TABLE_NAME")));
        }
        return layerNames;
    }

    @Override
    public List<String> eGetGeoLayerNameByKeyword(String layerNameKeyword) {
        if (StrUtil.isEmpty(layerNameKeyword)) {
            return eGetAllGeoLayerName();
        }
        String sql =
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE DATA_TYPE IN ('geometry','point','linestring','polygon','multipoint','multilinestring','multipolygon') "
                + "AND TABLE_SCHEMA = #{schema} "
                + "AND TABLE_NAME LIKE CONCAT('%', #{keyword}, '%') "
                + "GROUP BY TABLE_NAME;";

        SqlParamMap paramMap = new SqlParamMap();
        paramMap.put("schema", dataSourceGetter.getSchemaName());
        paramMap.put("keyword", layerNameKeyword);

        List<GirAdvOneRow> result = baseOpt.bSelectList(sql, paramMap);
        List<String> layerNames = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(result)) {
            result.forEach(row -> layerNames.add(row.getStr("TABLE_NAME")));
        }
        return layerNames;
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeByTable(
            String tableName, List<String> geomFieldNames) {
        validateTableName(tableName);
        if (CollectionUtil.isEmpty(geomFieldNames)) {
            return MapUtil.empty();
        }

        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        StringBuilder fieldsSql = new StringBuilder();
        StringBuilder whereSql = new StringBuilder();

        for (int i = 0; i < geomFieldNames.size(); i++) {
            String field = geomFieldNames.get(i);
            // MySQL: ST_GeometryType返回几何类型（如POINT, POLYGON）
            fieldsSql
                    .append("ST_GeometryType(")
                    .append(field)
                    .append(") AS ")
                    .append(field)
                    .append("_type");
            whereSql.append(field).append(" IS NOT NULL");
            if (i != geomFieldNames.size() - 1) {
                fieldsSql.append(", ");
                whereSql.append(" OR ");
            }
        }

        // MySQL查询几何类型
        String sql =
                StrUtil.format(
                        "SELECT {} FROM {} WHERE {} LIMIT 1;",
                        fieldsSql.toString(),
                        qualifiedTableName,
                        whereSql.toString());

        GirAdvOneRow row = baseOpt.bSelectOne(sql);
        Map<String, AdvEnumsTypeGeom> resultMap = new HashMap<>(geomFieldNames.size());
        if (row != null) {
            for (String field : geomFieldNames) {
                String geomType = row.getStr(field + "_type");
                if (StrUtil.isNotEmpty(geomType)) {
                    resultMap.put(field, getTypeGeomEnum(geomType));
                }
            }
        }
        return resultMap;
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(
            String sqlView, List<String> geomFieldNames) {
        if (StrUtil.isEmpty(sqlView) || CollectionUtil.isEmpty(geomFieldNames)) {
            return MapUtil.empty();
        }

        sqlView = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlView);
        StringBuilder fieldsSql = new StringBuilder();
        StringBuilder whereSql = new StringBuilder();

        for (int i = 0; i < geomFieldNames.size(); i++) {
            String field = geomFieldNames.get(i);
            fieldsSql
                    .append("ST_GeometryType(")
                    .append(field)
                    .append(") AS ")
                    .append(field)
                    .append("_type");
            whereSql.append(field).append(" IS NOT NULL");
            if (i != geomFieldNames.size() - 1) {
                fieldsSql.append(", ");
                whereSql.append(" OR ");
            }
        }

        String sql =
                StrUtil.format(
                        "SELECT {} FROM ({}) AS temp WHERE {} LIMIT 1;",
                        fieldsSql.toString(),
                        sqlView,
                        whereSql.toString());

        GirAdvOneRow row = baseOpt.bSelectOne(sql);
        Map<String, AdvEnumsTypeGeom> resultMap = new HashMap<>(geomFieldNames.size());
        if (row != null) {
            for (String field : geomFieldNames) {
                String geomType = row.getStr(field + "_type");
                if (StrUtil.isNotEmpty(geomType)) {
                    resultMap.put(field, getTypeGeomEnum(geomType));
                }
            }
        }
        return resultMap;
    }

    @Override
    public List<String> eGetGeomColumnNameListByTable(String tableName) {
        validateTableName(tableName);
        String schemaName = dataSourceGetter.getSchemaName();

        String sql =
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_NAME = #{tableName} "
                + "AND TABLE_SCHEMA = #{schemaName} "
                + "AND DATA_TYPE IN ('geometry','point','linestring','polygon','multipoint','multilinestring','multipolygon');";
        SqlParamMap paramMap = new SqlParamMap();
        paramMap.put("tableName", dialectTableNameProcessor.tbGetTableNameNotSchema(tableName));
        paramMap.put("schemaName", schemaName);

        List<GirAdvOneRow> rows = baseOpt.bSelectList(sql, paramMap);
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
        sqlView = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlView);
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String alias = dialectTableNameProcessor.tbGetTempAliasTableName();

        try {
            String fieldQuerySql =
                    StrUtil.format("SELECT * FROM ({}) AS {} LIMIT 0", sqlView, alias);
            conn = dataSourceGetter.getConnection();
            if (conn == null) {
                throw new IllegalStateException("无法获取MySQL数据库连接");
            }
            stmt = conn.prepareStatement(fieldQuerySql);
            rs = stmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();

            if (metaData != null) {
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String colName = metaData.getColumnName(i);
                    String colType = metaData.getColumnTypeName(i);
                    // 判断MySQL空间类型
                    if (Arrays.asList(
                                    "GEOMETRY",
                                    "POINT",
                                    "LINESTRING",
                                    "POLYGON",
                                    "MULTIPOINT",
                                    "MULTILINESTRING",
                                    "MULTIPOLYGON")
                            .contains(colType.toUpperCase())) {
                        return colName;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("查询MySQL SQL视图空间字段失败: {}", e.getMessage(), e);
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

        String qualifiedName =
                ddlOpt.dIsTableExists(tableNameOrSqlView)
                        ? dialectTableNameProcessor.tbGetTableNameWithSchema(
                        dataSourceGetter, tableNameOrSqlView)
                        : StrUtil.format(
                        "({}) as {}",
                        dialectTableNameProcessor.tbRemoveSqlSpaces(tableNameOrSqlView),
                        dialectTableNameProcessor.tbGetTempAliasTableName());

        // MySQL: ST_SRID获取空间参考系ID
        String sql =
                StrUtil.format(
                        "SELECT ST_SRID({}) AS srid FROM {} LIMIT 1;",
                        geomFieldName,
                        qualifiedName);

        GirAdvOneRow row = baseOpt.bSelectOne(sql);
        return row != null ? row.getInt("srid") : 0;
    }

    @Override
    public Map<String, Integer> eGetSrid(String tableNameOrSqlView, List<String> geomFieldNames) {
        if (StrUtil.isEmpty(tableNameOrSqlView) || CollectionUtil.isEmpty(geomFieldNames)) {
            return new HashMap<>();
        }

        String qualifiedName =
                ddlOpt.dIsTableExists(tableNameOrSqlView)
                        ? dialectTableNameProcessor.tbGetTableNameWithSchema(
                        dataSourceGetter, tableNameOrSqlView)
                        : StrUtil.format(
                        "({}) as {}",
                        dialectTableNameProcessor.tbRemoveSqlSpaces(tableNameOrSqlView),
                        dialectTableNameProcessor.tbGetTempAliasTableName());

        StringBuilder sridSelect = new StringBuilder();
        StringBuilder where = new StringBuilder("WHERE ");
        for (int i = 0; i < geomFieldNames.size(); i++) {
            String field = geomFieldNames.get(i);
            sridSelect.append(StrUtil.format("IFNULL(ST_SRID({}), -1) AS {}_srid", field, field));
            where.append(field).append(" IS NOT NULL");
            if (i != geomFieldNames.size() - 1) {
                sridSelect.append(", ");
                where.append(" OR ");
            }
        }

        String sql =
                StrUtil.format(
                        "SELECT {} FROM {} {} LIMIT 1;",
                        sridSelect.toString(),
                        qualifiedName,
                        where);

        GirAdvOneRow row = baseOpt.bSelectOne(sql);
        Map<String, Integer> sridMap = new HashMap<>();
        if (row != null) {
            for (String field : geomFieldNames) {
                int srid = row.getInt(field + "_srid");
                sridMap.put(field, srid == -1 ? 0 : srid);
            }
        }
        return sridMap;
    }

    // ===================== 空间字段DDL操作实现 =====================
    @Override
    public void eAddGeomColumn(
            String tableName, String geomFieldName, AdvEnumsTypeGeom geomType, int srid) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        validateSrid(srid);
        if (!ddlOpt.dIsTableExists(tableName)) {
            throw new RuntimeException("表[" + tableName + "]不存在，无法添加空间字段");
        }
        if (StrUtil.isNotEmpty(eGetGeomColumnNameByTable(tableName))) {
            throw new RuntimeException("表[" + tableName + "]已存在空间字段，MySQL暂不支持多空间字段");
        }

        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        // MySQL空间字段定义语法：GEOMETRY/SRID 或 具体类型（如POINT）
        String sql =
                StrUtil.format(
                        "ALTER TABLE {} ADD COLUMN {} {} SRID {};",
                        qualifiedTableName,
                        geomFieldName,
                        geomType.getCode().toUpperCase(),
                        srid);
        ddlOpt.dExecuteDDL(sql, tableName, "添加MySQL空间字段[" + geomFieldName + "]");

        // 创建MySQL空间索引（SPATIAL INDEX）
        String indexName = StrUtil.format("idx_{}_{}", tableName, geomFieldName);
        eCreateSpatialIndex(tableName, geomFieldName, indexName);
    }

    @Override
    public void eDropGeomColumn(String tableName, String geomFieldName) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        if (!ddlOpt.dIsTableExists(tableName)) {
            throw new RuntimeException("表[" + tableName + "]不存在");
        }

        String existingGeomField = eGetGeomColumnNameByTable(tableName);
        if (StrUtil.isEmpty(existingGeomField) || !existingGeomField.equals(geomFieldName)) {
            log.warn("表[{}]中不存在空间字段[{}]，无需删除", tableName, geomFieldName);
            return;
        }

        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql =
                StrUtil.format("ALTER TABLE {} DROP COLUMN {};", qualifiedTableName, geomFieldName);
        ddlOpt.dExecuteDDL(sql, tableName, "删除MySQL空间字段[" + geomFieldName + "]");
    }

    @Override
    public void eTransformSrid(String tableName, String geomFieldName, int targetSrid) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        validateSrid(targetSrid);
        if (!ddlOpt.dIsTableExists(tableName)) {
            throw new RuntimeException("表[" + tableName + "]不存在");
        }

        String existingGeomField = eGetGeomColumnNameByTable(tableName);
        if (StrUtil.isEmpty(existingGeomField) || !existingGeomField.equals(geomFieldName)) {
            throw new RuntimeException("表[" + tableName + "]中不存在空间字段[" + geomFieldName + "]");
        }

        AdvEnumsTypeGeom geomType = eGetGeoTypeByTable(tableName, geomFieldName);
        Integer oldSrid = eGetSrid(tableName, geomFieldName);
        oldSrid = oldSrid == 0 ? 4326 : oldSrid;

        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String tempGeomField = "geom_" + IdUtil.simpleUUID().substring(0, 8);

        try {
            // 1. 新建临时字段
            String createTempSql =
                    StrUtil.format(
                            "ALTER TABLE {} ADD COLUMN {} {} SRID 0;",
                            qualifiedTableName,
                            tempGeomField,
                            geomType.getCode().toUpperCase());
            ddlOpt.dExecuteDDL(createTempSql, tableName, "新建MySQL临时空间字段[" + tempGeomField + "]");

            // 2. 拷贝并转换SRID（MySQL: ST_Transform）
            String copySql =
                    StrUtil.format(
                            "UPDATE {} SET {} = ST_Transform(ST_SetSRID({}, {}), {});",
                            qualifiedTableName,
                            tempGeomField,
                            geomFieldName,
                            oldSrid,
                            targetSrid);
            ddlOpt.dExecuteDDL(copySql, tableName, "拷贝并转换SRID");

            // 3. 修改临时字段SRID
            String alterSridSql =
                    StrUtil.format(
                            "ALTER TABLE {} MODIFY COLUMN {} {} SRID {};",
                            qualifiedTableName,
                            tempGeomField,
                            geomType.getCode().toUpperCase(),
                            targetSrid);
            ddlOpt.dExecuteDDL(alterSridSql, tableName, "修改临时字段SRID为" + targetSrid);

            // 4. 重命名原字段
            String renameOldSql =
                    StrUtil.format(
                            "ALTER TABLE {} RENAME COLUMN {} TO {};",
                            qualifiedTableName,
                            geomFieldName,
                            geomFieldName + "_old_" + IdUtil.simpleUUID().substring(0, 8));
            ddlOpt.dExecuteDDL(renameOldSql, tableName, "重命名原空间字段");

            // 5. 重命名临时字段
            String renameTempSql =
                    StrUtil.format(
                            "ALTER TABLE {} RENAME COLUMN {} TO {};",
                            qualifiedTableName,
                            tempGeomField,
                            geomFieldName);
            ddlOpt.dExecuteDDL(renameTempSql, tableName, "重命名临时字段为原字段名");

            // 6. 删除旧字段
            String dropOldSql =
                    StrUtil.format(
                            "ALTER TABLE {} DROP COLUMN {};",
                            qualifiedTableName,
                            geomFieldName + "_old_" + IdUtil.simpleUUID().substring(0, 8));
            ddlOpt.dExecuteDDL(dropOldSql, tableName, "删除旧空间字段");
        } catch (Exception e) {
            throw new RuntimeException("MySQL SRID转换失败", e);
        }
    }

    @Override
    public void eCreateSpatialIndex(String tableName, String geomFieldName, String indexName) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        if (StrUtil.isEmpty(indexName)) {
            throw new IllegalArgumentException("索引名不能为空");
        }
        if (!ddlOpt.dIsTableExists(tableName)) {
            throw new RuntimeException("表[" + tableName + "]不存在");
        }

        String existingGeomField = eGetGeomColumnNameByTable(tableName);
        if (StrUtil.isEmpty(existingGeomField) || !existingGeomField.equals(geomFieldName)) {
            throw new RuntimeException("表[" + tableName + "]中不存在空间字段[" + geomFieldName + "]");
        }

        if (ddlOpt.dIndexesExists(tableName, indexName)) {
            log.warn("MySQL空间索引[{}]已存在，无需重复创建", indexName);
            return;
        }

        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        // MySQL空间索引语法：SPATIAL INDEX
        geomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        String sql =
                StrUtil.format(
                        "CREATE SPATIAL INDEX {} ON {} ({});",
                        indexName,
                        qualifiedTableName,
                        geomFieldName);
        ddlOpt.dExecuteDDL(sql, tableName, "创建MySQL空间索引[" + indexName + "]");
    }

    @Override
    public void eDropSpatialIndex(String tableName, String indexName) {
        validateTableName(tableName);
        if (StrUtil.isEmpty(indexName)) {
            throw new IllegalArgumentException("索引名不能为空");
        }

        if (!ddlOpt.dIndexesExists(tableName, indexName)) {
            log.warn("MySQL空间索引[{}]不存在，无需删除", indexName);
            return;
        }
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = StrUtil.format("ALTER TABLE {} DROP INDEX {};", qualifiedTableName, indexName);
        ddlOpt.dExecuteDDL(sql, tableName, "删除MySQL空间索引[" + indexName + "]");
    }

    @Override
    public String getQueryIntersectsSql(
            String qualifiedTableName, String geomFieldName, String geometry, int srid) {
        // MySQL: ST_Intersects + ST_GeomFromText
        return StrUtil.format(
                "SELECT * FROM {} WHERE ST_Intersects({}, ST_GeomFromText('{}', {},'axis-order=long-lat'));",
                qualifiedTableName,
                geomFieldName,
                geometry,
                srid);
    }

    @Override
    public String getQueryWithinBBoxSql(
            String qualifiedTableName, String geomFieldName, String bboxWkt, int srid) {
        // MySQL: ST_Within
        return StrUtil.format(
                "SELECT * FROM {} WHERE ST_Within({}, ST_GeomFromText('{}', {},'axis-order=long-lat'));",
                qualifiedTableName,
                geomFieldName,
                bboxWkt,
                srid);
    }

    @Override
    public String getCalculateDistanceSql(
            String geomFieldName,
            String geometry,
            int srid,
            String distanceAlias,
            String qualifiedTableName) {
        // MySQL: ST_Distance (注意：MySQL 8.0+支持ST_Distance，5.7需用ST_Distance_Sphere)
        return StrUtil.format(
                "SELECT *, ST_Distance({}, ST_GeomFromText('{}', {},'axis-order=long-lat')) AS {} FROM {};",
                geomFieldName,
                geometry,
                srid,
                distanceAlias,
                qualifiedTableName);
    }

    @Override
    public String getCentroidSql(
            String geomFieldName, String centerAlias, String qualifiedTableName) {
        // MySQL: ST_Centroid
        return StrUtil.format(
                "SELECT *, ST_Centroid({}) AS {} FROM {};",
                geomFieldName,
                centerAlias,
                qualifiedTableName);
    }

    @Override
    public String getValidateGeometriesSql(String qualifiedTableName, String geomFieldName) {
        return StrUtil.format(
                "SELECT id FROM {} WHERE NOT ST_IsValid({});", qualifiedTableName, geomFieldName);
    }

    @Override
    public String getRepairGeometriesSql(String qualifiedTableName, String geomFieldName) {
        // ST_MakeValid修复无效几何体（8.0.13+支持）
        return StrUtil.format(
                "UPDATE {} SET {} = ST_MakeValid({}) WHERE NOT ST_IsValid({});",
                qualifiedTableName,
                geomFieldName,
                geomFieldName,
                geomFieldName);
    }

    @Override
    public String getGetExtentSql(String geomFieldName, String qualifiedTableName, int bboxSrid) {

        String swappedGeom = String.format("ST_GeomFromWKB(ST_AsWKB(%s, 'axis-order=long-lat'))", geomFieldName);

        // 提取几何坐标的表达式（兼容所有类型）- 使用交换后的几何
        String xExpr = String.format(
                "CASE " +
                "    WHEN ST_GeometryType(%s) IN ('POINT', 'MULTIPOINT') THEN ST_X(%s) " +
                "    WHEN ST_GeometryType(%s) IN ('LINESTRING', 'MULTILINESTRING') THEN ST_X(ST_PointN(%s, 1)) " +
                "    WHEN ST_GeometryType(%s) IN ('POLYGON', 'MULTIPOLYGON') THEN ST_X(ST_PointN(ST_ExteriorRing(ST_GeometryN(%s, 1)), 1)) " +
                "    ELSE NULL " +
                "END",
                geomFieldName, swappedGeom,
                geomFieldName, swappedGeom,
                geomFieldName, swappedGeom
        );

        String yExpr = String.format(
                "CASE " +
                "    WHEN ST_GeometryType(%s) IN ('POINT', 'MULTIPOINT') THEN ST_Y(%s) " +
                "    WHEN ST_GeometryType(%s) IN ('LINESTRING', 'MULTILINESTRING') THEN ST_Y(ST_PointN(%s, 1)) " +
                "    WHEN ST_GeometryType(%s) IN ('POLYGON', 'MULTIPOLYGON') THEN ST_Y(ST_PointN(ST_ExteriorRing(ST_GeometryN(%s, 1)), 1)) " +
                "    ELSE NULL " +
                "END",
                geomFieldName, swappedGeom,
                geomFieldName, swappedGeom,
                geomFieldName, swappedGeom
        );

        // 基础查询（long-lat 轴顺序）
        String baseSql = String.format(
                "SELECT " +
                "MIN(%s) AS minx, " +
                "MIN(%s) AS miny, " +
                "MAX(%s) AS maxx, " +
                "MAX(%s) AS maxy " +
                "FROM %s WHERE %s IS NOT NULL",
                xExpr, yExpr, xExpr, yExpr,
                qualifiedTableName, geomFieldName
        );

        // 如果需要坐标转换（从其他坐标系转到 4326）
        if (bboxSrid != 4326) {
            // 先设置 SRID，再转换到 4326，最后交换轴顺序确保 X=经度
            String swappedGeomTransform = String.format(
                    "ST_GeomFromWKB(ST_AsWKB(ST_Transform(ST_SRID(%s, %d), 4326), 'axis-order=long-lat'))",
                    geomFieldName, bboxSrid
            );

            String xExprTransform = String.format(
                    "CASE " +
                    "    WHEN ST_GeometryType(%s) IN ('POINT', 'MULTIPOINT') THEN ST_X(%s) " +
                    "    WHEN ST_GeometryType(%s) IN ('LINESTRING', 'MULTILINESTRING') THEN ST_X(ST_PointN(%s, 1)) " +
                    "    WHEN ST_GeometryType(%s) IN ('POLYGON', 'MULTIPOLYGON') THEN ST_X(ST_PointN(ST_ExteriorRing(ST_GeometryN(%s, 1)), 1)) " +
                    "    ELSE NULL " +
                    "END",
                    geomFieldName, swappedGeomTransform,
                    geomFieldName, swappedGeomTransform,
                    geomFieldName, swappedGeomTransform
            );

            String yExprTransform = String.format(
                    "CASE " +
                    "    WHEN ST_GeometryType(%s) IN ('POINT', 'MULTIPOINT') THEN ST_Y(%s) " +
                    "    WHEN ST_GeometryType(%s) IN ('LINESTRING', 'MULTILINESTRING') THEN ST_Y(ST_PointN(%s, 1)) " +
                    "    WHEN ST_GeometryType(%s) IN ('POLYGON', 'MULTIPOLYGON') THEN ST_Y(ST_PointN(ST_ExteriorRing(ST_GeometryN(%s, 1)), 1)) " +
                    "    ELSE NULL " +
                    "END",
                    geomFieldName, swappedGeomTransform,
                    geomFieldName, swappedGeomTransform,
                    geomFieldName, swappedGeomTransform
            );

            String transformSql = String.format(
                    "SELECT " +
                    "MIN(%s) AS minx_gs, " +
                    "MIN(%s) AS miny_gs, " +
                    "MAX(%s) AS maxx_gs, " +
                    "MAX(%s) AS maxy_gs " +
                    "FROM %s WHERE %s IS NOT NULL",
                    xExprTransform, yExprTransform, xExprTransform, yExprTransform,
                    qualifiedTableName, geomFieldName
            );

            return String.format("SELECT t1.*, t2.* FROM (%s) t1 CROSS JOIN (%s) t2", baseSql, transformSql);
        }

        return baseSql;
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(
            String dynamicSql, GirSqlParam sqlParam, List<String> geomFieldNames) {
        if (StrUtil.isEmpty(dynamicSql) || CollectionUtil.isEmpty(geomFieldNames)) {
            return MapUtil.empty();
        }

        dynamicSql = dialectTableNameProcessor.tbRemoveSqlSpaces(dynamicSql);
        StringBuilder fieldsSql = new StringBuilder();
        StringBuilder whereSql = new StringBuilder();

        for (int i = 0; i < geomFieldNames.size(); i++) {
            String field = geomFieldNames.get(i);
            fieldsSql
                    .append("ST_GeometryType(")
                    .append(field)
                    .append(") AS ")
                    .append(field)
                    .append("_type");
            whereSql.append(field).append(" IS NOT NULL");
            if (i != geomFieldNames.size() - 1) {
                fieldsSql.append(", ");
                whereSql.append(" OR ");
            }
        }

        String sql =
                StrUtil.format(
                        "SELECT {} FROM ({}) AS temp WHERE {} LIMIT 1;",
                        fieldsSql.toString(),
                        dynamicSql,
                        whereSql.toString());

        GirAdvOneRow row = baseOpt.bSelectOne(sql, sqlParam);
        Map<String, AdvEnumsTypeGeom> resultMap = new HashMap<>(geomFieldNames.size());
        if (row != null) {
            for (String field : geomFieldNames) {
                String geomType = row.getStr(field + "_type");
                if (StrUtil.isNotEmpty(geomType)) {
                    resultMap.put(field, getTypeGeomEnum(geomType));
                }
            }
        }
        return resultMap;
    }

    @Override
    public String eGetGeomColumnNameBySql(String dynamicSql, GirSqlParam sqlParam) {
        if (StrUtil.isEmpty(dynamicSql)) {
            return null;
        }
        dynamicSql = dialectTableNameProcessor.tbRemoveSqlSpaces(dynamicSql);
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String alias = dialectTableNameProcessor.tbGetTempAliasTableName();

        try {
            String fieldQuerySql =
                    StrUtil.format("SELECT * FROM ({}) AS {} LIMIT 0", dynamicSql, alias);
            // 解析带参数SQL
            SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(fieldQuerySql, sqlParam, dialectTableNameProcessor);
            conn = dataSourceGetter.getConnection();
            if (conn == null) {
                throw new IllegalStateException("无法获取MySQL数据库连接");
            }
            stmt = conn.prepareStatement(sqlMeta.getSql());
            List<Object> params = sqlMeta.getJdbcParamValues();
            for (int i = 1; i <= params.size(); i++) {
                stmt.setObject(i, params.get(i - 1));
            }

            rs = stmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            if (metaData != null) {
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String colName = metaData.getColumnName(i);
                    String colType = metaData.getColumnTypeName(i);
                    if (Arrays.asList(
                                    "GEOMETRY",
                                    "POINT",
                                    "LINESTRING",
                                    "POLYGON",
                                    "MULTIPOINT",
                                    "MULTILINESTRING",
                                    "MULTIPOLYGON")
                            .contains(colType.toUpperCase())) {
                        return colName;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("带参数查询MySQL SQL视图空间字段失败: {}", e.getMessage(), e);
        } finally {
            dataSourceGetter.closeResources(rs, stmt, conn);
        }
        return null;
    }


}
