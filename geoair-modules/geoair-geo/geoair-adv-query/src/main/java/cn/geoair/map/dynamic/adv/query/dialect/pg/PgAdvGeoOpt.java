package cn.geoair.map.dynamic.adv.query.dialect.pg;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.mybatis.SqlEngineUtil;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
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

/**
 * PostgreSQL（PostGIS）空间操作实现类 复用你原有PgAdvGeoOpt + PgAdvGeoPreOpt的核心逻辑
 */
public class PgAdvGeoOpt extends AbstractExecAdvGeoOpt {

    private static final GiLogger log = GirLogger.getLoger();

    private boolean _POSTGIS_IS;

    private String _POSTGIS_VERSION;

    private IAdvBaseOpt baseOpt;

    private IAdvDDLOpt pgAdvDDLOpt;

    public PgAdvGeoOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt, IAdvDDLOpt ddlOpt) {
        super(dataSourceGetter);
        this.baseOpt = baseOpt;
        this.pgAdvDDLOpt = ddlOpt;
        // getPostGisVersion();
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return PgDialectTableNameUtil.getInstance();
    }

    @Override
    protected IAdvBaseOpt getAdvBaseOpt() {
        return baseOpt;
    }

    @Override
    protected IAdvDDLOpt getAdvDDLOpt() {
        return pgAdvDDLOpt;
    }

    @Override
    protected AdvEnumsTypeGeom getTypeGeomEnum(String nativeGeomType) {
        if (StrUtil.isEmpty(nativeGeomType)) {
            return null;
        }
        String typeName = nativeGeomType.replace("ST_", "").toLowerCase();
        for (AdvEnumsTypeGeom type : AdvEnumsTypeGeom.values()) {
            if (type.getCode().equalsIgnoreCase(typeName)) {
                return type;
            }
        }
        return AdvEnumsTypeGeom.Geometry;
    }

    // ===================== 原有PgAdvGeoOpt的核心方法实现 =====================
    private void getPostGisVersion() {
        try {
            GirAdvOneRow row = baseOpt.bSelectOne("SELECT public.postgis_version();");
            _POSTGIS_VERSION = row.getStr("postgis_version");
            _POSTGIS_IS = true;
            log.info("当前数据源的POSTGIS版本信息：{}", _POSTGIS_VERSION);
        } catch (Exception e) {
            log.info("当前数据源为非POSTGIS数据源", e);
            _POSTGIS_VERSION = "";
            _POSTGIS_IS = false;
        }
    }

    @Override
    public List<String> eGetAllGeoLayerName() {
        String sqlTemp =
                "SELECT table_name FROM information_schema.columns "
                        + "WHERE udt_name = 'geometry' {} "
                        + "GROUP BY table_name;";
        String schemaFilter =
                StrUtil.isEmpty(dataSourceGetter.getSchemaName())
                        ? ""
                        : StrUtil.format(
                        "AND \"table_schema\" = '{}'", dataSourceGetter.getSchemaName());
        String sql = StrUtil.format(sqlTemp, schemaFilter);

        List<GirAdvOneRow> result = baseOpt.bSelectList(sql);
        List<String> layerNames = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(result)) {
            result.forEach(row -> layerNames.add(row.getStr("table_name")));
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
            fieldsSql
                    .append(" public.ST_GeometryType(")
                    .append(field)
                    .append(") AS ")
                    .append(field)
                    .append("_type");
            whereSql.append(field).append(" IS NOT NULL");
            if (i != geomFieldNames.size() - 1) {
                fieldsSql.append(", ");
                whereSql.append(" AND ");
            }
        }

        String sql =
                StrUtil.format(
                        "SELECT {} FROM {} WHERE {} LIMIT 1;",
                        fieldsSql.toString(),
                        qualifiedTableName,
                        whereSql.toString());
        GirAdvOneRow row = baseOpt.bSelectOne(sql);

        Map<String, AdvEnumsTypeGeom> resultMap = new HashMap<>();
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
                    .append("public.ST_GeometryType(")
                    .append(field)
                    .append(") AS ")
                    .append(field)
                    .append("_type");
            whereSql.append(field).append(" IS NOT NULL");
            if (i != geomFieldNames.size() - 1) {
                fieldsSql.append(", ");
                whereSql.append(" AND ");
            }
        }

        String sql =
                StrUtil.format(
                        "SELECT {} FROM ({}) AS temp WHERE {} LIMIT 1;",
                        fieldsSql.toString(),
                        sqlView,
                        whereSql.toString());
        GirAdvOneRow row = baseOpt.bSelectOne(sql);

        Map<String, AdvEnumsTypeGeom> resultMap = new HashMap<>();
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
        String tableNameWithoutSchema =
                dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        schemaName = StrUtil.isEmpty(schemaName) ? dataSourceGetter.getSchemaName() : schemaName;

        String sqlTemp =
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_name = '{}' AND udt_name = 'geometry' {};";
        String schemaFilter =
                StrUtil.isEmpty(schemaName)
                        ? ""
                        : StrUtil.format("AND \"table_schema\" = '{}'", schemaName);
        String sql = StrUtil.format(sqlTemp, tableNameWithoutSchema, schemaFilter);

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
        sqlView = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlView);
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String alias = dialectTableNameProcessor.tbGetTempAliasTableName();

        try {
            String fieldQuerySql =
                    StrUtil.format("SELECT * FROM ({}) AS {} LIMIT 0", sqlView, alias);
            conn = dataSourceGetter.getConnection();
            if (conn == null) {
                throw new IllegalStateException("无法获取数据库连接");
            }
            stmt = conn.createStatement();
            rs = stmt.executeQuery(fieldQuerySql);
            ResultSetMetaData metaData = rs.getMetaData();

            if (metaData != null) {
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String colName = metaData.getColumnName(i);
                    String colType = metaData.getColumnTypeName(i);
                    if ("geometry".equals(colType)
                            || "geography".equals(colType)
                            || "\"public\".\"geometry\"".equals(colType)) {
                        return colName;
                    }
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

        String qualifiedName =
                pgAdvDDLOpt.dIsTableExists(tableNameOrSqlView)
                        ? dialectTableNameProcessor.tbGetTableNameWithSchema(
                        dataSourceGetter, tableNameOrSqlView)
                        : StrUtil.format(
                        "({}) as {}",
                        dialectTableNameProcessor.tbRemoveSqlSpaces(tableNameOrSqlView),
                        dialectTableNameProcessor.tbGetTempAliasTableName());

        String sql =
                StrUtil.format(
                        "SELECT public.st_srid({}) AS srid FROM {} LIMIT 1;",
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
                pgAdvDDLOpt.dIsTableExists(tableNameOrSqlView)
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
            sridSelect.append(
                    StrUtil.format("COALESCE(public.st_srid({}), -1) AS {}_srid", field, field));
            where.append(field).append(" IS NOT NULL");
            if (i != geomFieldNames.size() - 1) {
                sridSelect.append(", ");
                where.append(" and ");
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

    @Override
    public void eAddGeomColumn(
            String tableName, String geomFieldName, AdvEnumsTypeGeom geomType, int srid) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        validateSrid(srid);
        if (!pgAdvDDLOpt.dIsTableExists(tableName)) {
            throw new RuntimeException("表[" + tableName + "]不存在");
        }
        if (StrUtil.isNotEmpty(eGetGeomColumnNameByTable(tableName))) {
            throw new RuntimeException("表[" + tableName + "]已存在空间字段");
        }

        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql =
                StrUtil.format(
                        "ALTER TABLE {} ADD COLUMN {} geometry({}, {});",
                        qualifiedTableName,
                        geomFieldName,
                        geomType.getCode(),
                        srid);
        getAdvDDLOpt().dExecuteDDL(sql, tableName, "添加空间字段[" + geomFieldName + "]");

        // 创建空间索引
        String indexName = StrUtil.format("idx_{}_{}", tableName, geomFieldName);
        eCreateSpatialIndex(tableName, geomFieldName, indexName);
    }

    @Override
    public void eDropGeomColumn(String tableName, String geomFieldName) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        if (!pgAdvDDLOpt.dIsTableExists(tableName)) {
            throw new RuntimeException("表[" + tableName + "]不存在");
        }

        String existingGeomField = eGetGeomColumnNameByTable(tableName);
        if (StrUtil.isEmpty(existingGeomField) || !existingGeomField.equals(geomFieldName)) {
            log.warn("表[{}]无空间字段[{}]，无需删除", tableName, geomFieldName);
            return;
        }

        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql =
                StrUtil.format("ALTER TABLE {} DROP COLUMN {};", qualifiedTableName, geomFieldName);
        getAdvDDLOpt().dExecuteDDL(sql, tableName, "删除空间字段[" + geomFieldName + "]");
    }

    @Override
    public void eTransformSrid(String tableName, String geomFieldName, int targetSrid) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        validateSrid(targetSrid);
        if (!pgAdvDDLOpt.dIsTableExists(tableName)) {
            throw new RuntimeException("表[" + tableName + "]不存在");
        }

        String existingGeomField = eGetGeomColumnNameByTable(tableName);
        if (StrUtil.isEmpty(existingGeomField) || !existingGeomField.equals(geomFieldName)) {
            throw new RuntimeException("表[" + tableName + "]无空间字段[" + geomFieldName + "]");
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
                            "ALTER TABLE {} ADD COLUMN {} geometry({},0);",
                            qualifiedTableName,
                            tempGeomField,
                            geomType.name().toLowerCase());
            getAdvDDLOpt().dExecuteDDL(createTempSql, tableName, "新建临时空间字段[" + tempGeomField + "]");

            // 2. 拷贝数据
            String copySql =
                    StrUtil.format(
                            "UPDATE {} SET {} =ST_SetSRID({}, {});",
                            qualifiedTableName,
                            tempGeomField,
                            geomFieldName,
                            oldSrid);
            getAdvDDLOpt().dExecuteDDL(copySql, tableName, "拷贝空间数据到临时字段");

            // 3. 转换SRID
            String transformSql =
                    StrUtil.format(
                            "ALTER TABLE {} ALTER COLUMN {} TYPE geometry({}, {}) USING public.ST_Transform({}, {});",
                            qualifiedTableName,
                            tempGeomField,
                            geomType.name().toLowerCase(),
                            targetSrid,
                            tempGeomField,
                            targetSrid);
            getAdvDDLOpt().dExecuteDDL(transformSql, tableName, "转换SRID为" + targetSrid);

            // 4. 重命名原字段
            String renameOldSql =
                    StrUtil.format(
                            "ALTER TABLE {} RENAME COLUMN {} TO {};",
                            qualifiedTableName,
                            geomFieldName,
                            geomFieldName + "_old_" + IdUtil.simpleUUID().substring(0, 8));
            getAdvDDLOpt().dExecuteDDL(renameOldSql, tableName, "重命名原空间字段");

            // 5. 重命名临时字段
            String renameTempSql =
                    StrUtil.format(
                            "ALTER TABLE {} RENAME COLUMN {} TO {};",
                            qualifiedTableName,
                            tempGeomField,
                            geomFieldName);
            getAdvDDLOpt().dExecuteDDL(renameTempSql, tableName, "重命名临时字段为原字段名");

            // 6. 删除旧字段
            String dropOldSql =
                    StrUtil.format(
                            "ALTER TABLE {} DROP COLUMN {};",
                            qualifiedTableName,
                            geomFieldName + "_old_" + IdUtil.simpleUUID().substring(0, 8));
            getAdvDDLOpt().dExecuteDDL(dropOldSql, tableName, "删除旧空间字段");
        } catch (Exception e) {
            throw new RuntimeException("SRID转换失败", e);
        }
    }

    @Override
    public void eCreateSpatialIndex(String tableName, String geomFieldName, String indexName) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        if (StrUtil.isEmpty(indexName)) {
            throw new IllegalArgumentException("索引名不能为空");
        }
        if (!pgAdvDDLOpt.dIsTableExists(tableName)) {
            throw new RuntimeException("表[" + tableName + "]不存在");
        }

        String existingGeomField = eGetGeomColumnNameByTable(tableName);
        if (StrUtil.isEmpty(existingGeomField) || !existingGeomField.equals(geomFieldName)) {
            throw new RuntimeException("表[" + tableName + "]无空间字段[" + geomFieldName + "]");
        }

        if (pgAdvDDLOpt.dIndexesExists(tableName, indexName)) {
            log.warn("索引[{}]已存在，无需创建", indexName);
            return;
        }

        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql =
                StrUtil.format(
                        "CREATE INDEX {} ON {} USING GIST ({});",
                        indexName,
                        qualifiedTableName,
                        geomFieldName);
        getAdvDDLOpt().dExecuteDDL(sql, tableName, "创建空间索引[" + indexName + "]");
    }

    @Override
    public void eDropSpatialIndex(String tableName, String indexName) {
        validateTableName(tableName);
        if (StrUtil.isEmpty(indexName)) {
            throw new IllegalArgumentException("索引名不能为空");
        }

        if (!pgAdvDDLOpt.dIndexesExists(tableName, indexName)) {
            log.warn("索引[{}]不存在，无需删除", indexName);
            return;
        }

        String sql =
                StrUtil.format(
                        "DROP INDEX IF EXISTS {}.{};",
                        dialectTableNameProcessor.tbGetSchemaNameForSql(dataSourceGetter),
                        indexName);
        getAdvDDLOpt().dExecuteDDL(sql, tableName, "删除空间索引[" + indexName + "]");
    }

    @Override
    public String getQueryIntersectsSql(
            String qualifiedTableName, String geomFieldName, String geometry, int srid) {
        return StrUtil.format(
                "SELECT * FROM {} WHERE public.ST_Intersects({}, public.ST_GeomFromText('{}', {}));",
                qualifiedTableName,
                geomFieldName,
                geometry,
                srid);
    }

    @Override
    public String getQueryWithinBBoxSql(
            String qualifiedTableName, String geomFieldName, String bboxWkt, int srid) {
        return StrUtil.format(
                "SELECT * FROM {} WHERE public.ST_Within({}, public.ST_GeomFromText('{}', {}));",
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
        return StrUtil.format(
                "SELECT *, public.ST_Distance({}, public.ST_GeomFromText('{}', {})) AS {} FROM {};",
                geomFieldName,
                geometry,
                srid,
                distanceAlias,
                qualifiedTableName);
    }

    @Override
    public String getCentroidSql(
            String geomFieldName, String centerAlias, String qualifiedTableName) {
        return StrUtil.format(
                "SELECT *, public.ST_Centroid({}) AS {} FROM {};",
                geomFieldName,
                centerAlias,
                qualifiedTableName);
    }

    @Override
    public String getValidateGeometriesSql(String qualifiedTableName, String geomFieldName) {
        return StrUtil.format(
                "SELECT id FROM {} WHERE NOT public.ST_IsValid({});",
                qualifiedTableName,
                geomFieldName);
    }

    @Override
    public String getRepairGeometriesSql(String qualifiedTableName, String geomFieldName) {
        return StrUtil.format(
                "UPDATE {} SET {} = public.ST_MakeValid({}) WHERE NOT public.ST_IsValid({});",
                qualifiedTableName,
                geomFieldName,
                geomFieldName,
                geomFieldName);
    }

    @Override
    public String getGetExtentSql(String geomFieldName, String qualifiedTableName, int srid) {
        return StrUtil.format(
                "SELECT "
                        + "public.ST_XMin ( extent ) AS minx,"
                        + "public.ST_YMin ( extent ) AS miny,"
                        + "public.ST_XMax ( extent ) AS maxx,"
                        + "public.ST_YMax ( extent ) AS maxy,"
                        + "public.ST_XMin ( public.st_transform ( extent, 4326 ) ) AS minx_gs,"
                        + "public.ST_YMin ( public.st_transform ( extent, 4326 ) ) AS miny_gs,"
                        + "public.ST_XMax ( public.st_transform ( extent, 4326 ) ) AS maxx_gs,"
                        + "public.ST_YMax ( public.st_transform ( extent, 4326 ) ) AS maxy_gs "
                        + "FROM ( SELECT public.st_setsrid ( public.ST_Extent ( {} ), {} ) AS extent FROM {} ) AS lpl666;",
                geomFieldName,
                srid,
                qualifiedTableName);
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
                    .append("public.ST_GeometryType(")
                    .append(field)
                    .append(") AS ")
                    .append(field)
                    .append("_type");
            whereSql.append(field).append(" IS NOT NULL");
            if (i != geomFieldNames.size() - 1) {
                fieldsSql.append(", ");
                whereSql.append(" AND ");
            }
        }

        String sql =
                StrUtil.format(
                        "SELECT {} FROM ({}) AS temp WHERE {} LIMIT 1;",
                        fieldsSql.toString(),
                        dynamicSql,
                        whereSql.toString());


        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql, sqlParam);

        Map<String, AdvEnumsTypeGeom> resultMap = new HashMap<>();
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
            SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(fieldQuerySql, sqlParam, dialectTableNameProcessor);

            conn = dataSourceGetter.getConnection();
            if (conn == null) {
                throw new IllegalStateException("无法获取数据库连接");
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
                    if ("geometry".equals(colType)
                            || "geography".equals(colType)
                            || "\"public\".\"geometry\"".equals(colType)) {
                        return colName;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("带参数查询SQL空间字段失败: {}", e.getMessage(), e);
        } finally {
            dataSourceGetter.closeResources(rs, stmt, conn);
        }
        return null;
    }
}
