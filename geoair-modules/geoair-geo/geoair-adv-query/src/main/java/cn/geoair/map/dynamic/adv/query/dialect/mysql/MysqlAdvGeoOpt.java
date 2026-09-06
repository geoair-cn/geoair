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
                    .append(dialectTableNameProcessor.tbQuoteFieldName(field))
                    .append(") AS ")
                    .append(field)
                    .append("_type");
            whereSql.append(dialectTableNameProcessor.tbQuoteFieldName(field)).append(" IS NOT NULL");
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
                    .append(dialectTableNameProcessor.tbQuoteFieldName(field))
                    .append(") AS ")
                    .append(field)
                    .append("_type");
            whereSql.append(dialectTableNameProcessor.tbQuoteFieldName(field)).append(" IS NOT NULL");
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
        String schemaName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        if (StrUtil.isBlank(schemaName)) {
            schemaName = dataSourceGetter.getSchemaName();
        }
        if (StrUtil.isBlank(schemaName)) {
            throw new IllegalStateException("无法确定 MySQL 表所属数据库：" + tableName);
        }

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
                        "SELECT ST_SRID({}) AS srid FROM {} where {} is not null  LIMIT 1;",
                        dialectTableNameProcessor.tbQuoteFieldName(geomFieldName),
                        qualifiedName,
                        dialectTableNameProcessor.tbQuoteFieldName(geomFieldName));

        GirAdvOneRow row = baseOpt.bSelectOne(sql);
        return row != null ? row.getInt("srid", 0) : 0;
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
            sridSelect.append(StrUtil.format(
                    "IFNULL(ST_SRID({}), -1) AS {}",
                    dialectTableNameProcessor.tbQuoteFieldName(field),
                    dialectTableNameProcessor.tbQuoteFieldName(field + "_srid")));
            where.append(dialectTableNameProcessor.tbQuoteFieldName(field)).append(" IS NOT NULL");
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
        if (hasColumn(tableName, geomFieldName)) {
            throw new IllegalArgumentException("字段已存在，无法添加空间字段：" + geomFieldName);
        }

        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        // MySQL空间字段定义语法：GEOMETRY/SRID 或 具体类型（如POINT）
        String indexName = StrUtil.format("idx_{}_{}", tableName, geomFieldName);
        String quotedGeomField = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        try {
            ddlOpt.dExecuteStatements(Arrays.asList(
                            StrUtil.format("ALTER TABLE {} ADD COLUMN {} {} SRID {}",
                                    qualifiedTableName, quotedGeomField, geomType.getCode().toUpperCase(), srid),
                            StrUtil.format("CREATE SPATIAL INDEX {} ON {} ({})",
                                    dialectTableNameProcessor.tbQuoteFieldName(indexName), qualifiedTableName, quotedGeomField)),
                    tableName,
                    "添加MySQL空间字段及索引[" + geomFieldName + "]");
        } catch (RuntimeException e) {
            // MySQL DDL 无法整体回滚：索引创建失败时，尽量删除本次刚创建的字段。
            compensateAddGeomColumnFailure(tableName, qualifiedTableName, geomFieldName, indexName);
            throw new RuntimeException("添加MySQL空间字段或索引失败；已尝试清理新增字段", e);
        }
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
                StrUtil.format("ALTER TABLE {} DROP COLUMN {};", qualifiedTableName, dialectTableNameProcessor.tbQuoteFieldName(geomFieldName));
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

        String oldGeomFieldBack = geomFieldName + "_old_" + IdUtil.simpleUUID().substring(0, 8);
        String quotedTempGeomField = dialectTableNameProcessor.tbQuoteFieldName(tempGeomField);
        String quotedGeomFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        String quotedOldGeomField = dialectTableNameProcessor.tbQuoteFieldName(oldGeomFieldBack);
        List<String> statements = Arrays.asList(
                // 必须使用 tempGeomField，历史实现误用了 geomFieldName，会立即触发重复列错误。
                StrUtil.format("ALTER TABLE {} ADD COLUMN {} {} SRID 0",
                        qualifiedTableName, quotedTempGeomField, geomType.getCode().toUpperCase()),
                StrUtil.format("UPDATE {} SET {} = ST_Transform(ST_SetSRID({}, {}), {})",
                        qualifiedTableName, quotedTempGeomField, quotedGeomFieldName, oldSrid, targetSrid),
                StrUtil.format("ALTER TABLE {} MODIFY COLUMN {} {} SRID {}",
                        qualifiedTableName, quotedTempGeomField, geomType.getCode().toUpperCase(), targetSrid),
                StrUtil.format("ALTER TABLE {} RENAME COLUMN {} TO {}",
                        qualifiedTableName, quotedGeomFieldName, quotedOldGeomField),
                StrUtil.format("ALTER TABLE {} RENAME COLUMN {} TO {}",
                        qualifiedTableName, quotedTempGeomField, quotedGeomFieldName),
                StrUtil.format("ALTER TABLE {} DROP COLUMN {}", qualifiedTableName, quotedOldGeomField));
        try {
            // MySQL DDL 会隐式提交；同一连接只能保证执行顺序，不能提供事务性回滚。
            ddlOpt.dExecuteStatements(statements, tableName, "MySQL SRID转换为" + targetSrid);
        } catch (RuntimeException e) {
            compensateSridTransformFailure(tableName, qualifiedTableName, geomFieldName, tempGeomField, oldGeomFieldBack);
            throw new RuntimeException("MySQL SRID转换失败；已尝试恢复原字段或清理临时字段，请检查异常日志确认最终状态", e);
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
                        dialectTableNameProcessor.tbQuoteFieldName(indexName),
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
        String sql = StrUtil.format(
                "ALTER TABLE {} DROP INDEX {};", qualifiedTableName, dialectTableNameProcessor.tbQuoteFieldName(indexName));
        ddlOpt.dExecuteDDL(sql, tableName, "删除MySQL空间索引[" + indexName + "]");
    }

    @Override
    public String getQueryIntersectsSql(
            String qualifiedTableName, String geomFieldName, String geometry, int srid) {
        // MySQL: ST_Intersects + ST_GeomFromText
        return StrUtil.format(
                "SELECT * FROM {} WHERE ST_Intersects({}, ST_GeomFromText('{}', {},'axis-order=long-lat'));",
                qualifiedTableName,
                dialectTableNameProcessor.tbQuoteFieldName(geomFieldName),
                escapeSqlLiteral(geometry),
                srid);
    }

    @Override
    public String getQueryWithinBBoxSql(
            String qualifiedTableName, String geomFieldName, String bboxWkt, int srid) {
        // MySQL: ST_Within
        return StrUtil.format(
                "SELECT * FROM {} WHERE ST_Within({}, ST_GeomFromText('{}', {},'axis-order=long-lat'));",
                qualifiedTableName,
                dialectTableNameProcessor.tbQuoteFieldName(geomFieldName),
                escapeSqlLiteral(bboxWkt),
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
                dialectTableNameProcessor.tbQuoteFieldName(geomFieldName),
                escapeSqlLiteral(geometry),
                srid,
                dialectTableNameProcessor.tbQuoteFieldName(distanceAlias),
                qualifiedTableName);
    }

    @Override
    public String getCentroidSql(
            String geomFieldName, String centerAlias, String qualifiedTableName) {
        // MySQL: ST_Centroid
        return StrUtil.format(
                "SELECT *, ST_Centroid({}) AS {} FROM {};",
                dialectTableNameProcessor.tbQuoteFieldName(geomFieldName),
                dialectTableNameProcessor.tbQuoteFieldName(centerAlias),
                qualifiedTableName);
    }

    @Override
    public String getValidateGeometriesSql(String qualifiedTableName, String geomFieldName) {
        return StrUtil.format(
                "SELECT id FROM {} WHERE NOT ST_IsValid({});",
                qualifiedTableName,
                dialectTableNameProcessor.tbQuoteFieldName(geomFieldName));
    }

    @Override
    protected String buildValidateGeometriesByPrimaryKeysSql(
            String qualifiedTableName, String geomFieldName, List<String> primaryKeys) {
        String selectedKeys = primaryKeys.stream()
                .map(dialectTableNameProcessor::tbQuoteFieldName)
                .collect(java.util.stream.Collectors.joining(", "));
        return StrUtil.format(
                "SELECT {} FROM {} WHERE NOT ST_IsValid({});",
                selectedKeys,
                qualifiedTableName,
                dialectTableNameProcessor.tbQuoteFieldName(geomFieldName));
    }

    @Override
    public String getRepairGeometriesSql(String qualifiedTableName, String geomFieldName) {
        // ST_MakeValid修复无效几何体（8.0.13+支持）
        return StrUtil.format(
                "UPDATE {} SET {} = ST_MakeValid({}) WHERE NOT ST_IsValid({});",
                qualifiedTableName,
                dialectTableNameProcessor.tbQuoteFieldName(geomFieldName),
                dialectTableNameProcessor.tbQuoteFieldName(geomFieldName),
                dialectTableNameProcessor.tbQuoteFieldName(geomFieldName));
    }

    @Override
    public String getGetExtentSql(String geomFieldName, String qualifiedTableName, int bboxSrid) {
        String quoteFieldName = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
        String swappedGeom = String.format("ST_GeomFromWKB(ST_AsWKB(%s, 'axis-order=long-lat'))", quoteFieldName);

        // 提取几何坐标的表达式（兼容所有类型）- 使用交换后的几何
        String xExpr = String.format(
                "CASE " +
                "    WHEN ST_GeometryType(%s) IN ('POINT', 'MULTIPOINT') THEN ST_X(%s) " +
                "    WHEN ST_GeometryType(%s) IN ('LINESTRING', 'MULTILINESTRING') THEN ST_X(ST_PointN(%s, 1)) " +
                "    WHEN ST_GeometryType(%s) IN ('POLYGON', 'MULTIPOLYGON') THEN ST_X(ST_PointN(ST_ExteriorRing(ST_GeometryN(%s, 1)), 1)) " +
                "    ELSE NULL " +
                "END",
                quoteFieldName, swappedGeom,
                quoteFieldName, swappedGeom,
                quoteFieldName, swappedGeom
        );

        String yExpr = String.format(
                "CASE " +
                "    WHEN ST_GeometryType(%s) IN ('POINT', 'MULTIPOINT') THEN ST_Y(%s) " +
                "    WHEN ST_GeometryType(%s) IN ('LINESTRING', 'MULTILINESTRING') THEN ST_Y(ST_PointN(%s, 1)) " +
                "    WHEN ST_GeometryType(%s) IN ('POLYGON', 'MULTIPOLYGON') THEN ST_Y(ST_PointN(ST_ExteriorRing(ST_GeometryN(%s, 1)), 1)) " +
                "    ELSE NULL " +
                "END",
                quoteFieldName, swappedGeom,
                quoteFieldName, swappedGeom,
                quoteFieldName, swappedGeom
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
                qualifiedTableName, quoteFieldName
        );

        // 如果需要坐标转换（从其他坐标系转到 4326）
        if (bboxSrid != 4326) {
            // 先设置 SRID，再转换到 4326，最后交换轴顺序确保 X=经度
            String swappedGeomTransform = String.format(
                    "ST_GeomFromWKB(ST_AsWKB(ST_Transform(ST_SRID(%s, %d), 4326), 'axis-order=long-lat'))",
                    quoteFieldName, bboxSrid
            );

            String xExprTransform = String.format(
                    "CASE " +
                    "    WHEN ST_GeometryType(%s) IN ('POINT', 'MULTIPOINT') THEN ST_X(%s) " +
                    "    WHEN ST_GeometryType(%s) IN ('LINESTRING', 'MULTILINESTRING') THEN ST_X(ST_PointN(%s, 1)) " +
                    "    WHEN ST_GeometryType(%s) IN ('POLYGON', 'MULTIPOLYGON') THEN ST_X(ST_PointN(ST_ExteriorRing(ST_GeometryN(%s, 1)), 1)) " +
                    "    ELSE NULL " +
                    "END",
                    quoteFieldName, swappedGeomTransform,
                    quoteFieldName, swappedGeomTransform,
                    quoteFieldName, swappedGeomTransform
            );

            String yExprTransform = String.format(
                    "CASE " +
                    "    WHEN ST_GeometryType(%s) IN ('POINT', 'MULTIPOINT') THEN ST_Y(%s) " +
                    "    WHEN ST_GeometryType(%s) IN ('LINESTRING', 'MULTILINESTRING') THEN ST_Y(ST_PointN(%s, 1)) " +
                    "    WHEN ST_GeometryType(%s) IN ('POLYGON', 'MULTIPOLYGON') THEN ST_Y(ST_PointN(ST_ExteriorRing(ST_GeometryN(%s, 1)), 1)) " +
                    "    ELSE NULL " +
                    "END",
                    quoteFieldName, swappedGeomTransform,
                    quoteFieldName, swappedGeomTransform,
                    quoteFieldName, swappedGeomTransform
            );

            String transformSql = String.format(
                    "SELECT " +
                    "MIN(%s) AS minx_gs, " +
                    "MIN(%s) AS miny_gs, " +
                    "MAX(%s) AS maxx_gs, " +
                    "MAX(%s) AS maxy_gs " +
                    "FROM %s WHERE %s IS NOT NULL",
                    xExprTransform, yExprTransform, xExprTransform, yExprTransform,
                    qualifiedTableName, quoteFieldName
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
                    .append( dialectTableNameProcessor.tbQuoteFieldName(field))
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

    /**
     * MySQL DDL 无法依靠 JDBC 回滚。失败后按字段实际状态进行保守补偿：优先恢复旧字段，
     * 再删除未启用的临时字段。补偿本身也可能受索引、权限或连接中断影响，因此会保留原始异常。
     */
    private void compensateSridTransformFailure(
            String tableName,
            String qualifiedTableName,
            String geomFieldName,
            String tempGeomField,
            String oldGeomFieldBack) {
        try {
            boolean hasOriginal = hasColumn(tableName, geomFieldName);
            boolean hasTemporary = hasColumn(tableName, tempGeomField);
            boolean hasBackup = hasColumn(tableName, oldGeomFieldBack);
            String quotedOriginal = dialectTableNameProcessor.tbQuoteFieldName(geomFieldName);
            String quotedTemporary = dialectTableNameProcessor.tbQuoteFieldName(tempGeomField);
            String quotedBackup = dialectTableNameProcessor.tbQuoteFieldName(oldGeomFieldBack);

            if (hasBackup) {
                // 临时字段已替换为原字段名时，先移除转换后的字段，再还原旧字段。
                if (hasOriginal) {
                    ddlOpt.dExecuteDDL(
                            StrUtil.format("ALTER TABLE {} DROP COLUMN {}", qualifiedTableName, quotedOriginal),
                            tableName,
                            "回退MySQL SRID转换后的空间字段");
                }
                ddlOpt.dExecuteDDL(
                        StrUtil.format("ALTER TABLE {} RENAME COLUMN {} TO {}",
                                qualifiedTableName, quotedBackup, quotedOriginal),
                        tableName,
                        "回退MySQL原空间字段名称");
            }
            if (hasTemporary && hasColumn(tableName, tempGeomField)) {
                ddlOpt.dExecuteDDL(
                        StrUtil.format("ALTER TABLE {} DROP COLUMN {}", qualifiedTableName, quotedTemporary),
                        tableName,
                        "清理MySQL SRID转换临时字段");
            }
        } catch (Exception compensationError) {
            log.error("MySQL SRID转换失败后的补偿未完成，表={}, 原字段={}, 临时字段={}, 备份字段={}",
                    tableName, geomFieldName, tempGeomField, oldGeomFieldBack, compensationError);
        }
    }

    private boolean hasColumn(String tableName, String columnName) {
        return ddlOpt.dGetColumnsByTable(tableName)
                .findField(field -> columnName.equalsIgnoreCase(field.getColumnName()))
                .isPresent();
    }

    /** MySQL 建索引失败后，清理本次新建且尚未交付使用的空间字段。 */
    private void compensateAddGeomColumnFailure(
            String tableName, String qualifiedTableName, String geomFieldName, String indexName) {
        try {
            if (ddlOpt.dIndexesExists(tableName, indexName)) {
                ddlOpt.dExecuteDDL(
                        StrUtil.format("ALTER TABLE {} DROP INDEX {}", qualifiedTableName,
                                dialectTableNameProcessor.tbQuoteFieldName(indexName)),
                        tableName,
                        "回退MySQL空间索引");
            }
            if (hasColumn(tableName, geomFieldName)) {
                ddlOpt.dExecuteDDL(
                        StrUtil.format("ALTER TABLE {} DROP COLUMN {}", qualifiedTableName,
                                dialectTableNameProcessor.tbQuoteFieldName(geomFieldName)),
                        tableName,
                        "回退MySQL空间字段");
            }
        } catch (Exception compensationError) {
            log.error("MySQL 添加空间字段失败后的补偿未完成，表={}, 字段={}, 索引={}",
                    tableName, geomFieldName, indexName, compensationError);
        }
    }

    /** 将 WKT 等外部文本安全嵌入 SQL 字符串字面量。 */
    private static String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }


}
