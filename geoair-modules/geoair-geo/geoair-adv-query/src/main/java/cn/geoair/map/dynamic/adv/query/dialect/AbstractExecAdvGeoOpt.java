package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;
import cn.geoair.map.dynamic.adv.query.apo.*;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvQueryCommonUtils;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 空间操作抽象基类 封装通用参数校验、结果处理等逻辑，子类只需实现数据库方言相关逻辑 */
public abstract class AbstractExecAdvGeoOpt implements IAdvGeoPreOpt {
    public static GiLogger log = GirLoggerFactory.getLogger();
    protected final IDataSourceGetter dataSourceGetter;

    protected final DialectTableNameProcessor dialectTableNameProcessor;

    public AbstractExecAdvGeoOpt(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
        this.dialectTableNameProcessor = getDialectTableNameProcessor();
    }

    /** 获取方言专属的表名处理器 */
    protected abstract DialectTableNameProcessor getDialectTableNameProcessor();

    /** 获取抽象查询对象 */
    protected abstract IAdvBaseOpt getAdvBaseOpt();

    /** 抽象查询对象 */
    protected abstract IAdvDDLOpt getAdvDDLOpt();

    /** 将数据库原生几何类型转换为通用枚举 */
    protected abstract AdvEnumsTypeGeom getTypeGeomEnum(String nativeGeomType);

    /** 获取查询相交的SQL */
    public abstract String getQueryIntersectsSql(
            String qualifiedTableName, String geomFieldName, String geometry, int srid);

    /** 获取查询边界框内数据的SQL */
    public abstract String getQueryWithinBBoxSql(
            String qualifiedTableName, String geomFieldName, String bboxWkt, int srid);

    /** 获取计算距离的SQL */
    public abstract String getCalculateDistanceSql(
            String geomFieldName,
            String geometry,
            int srid,
            String distanceAlias,
            String qualifiedTableName);

    /** 获取计算中心点的SQL */
    public abstract String getCentroidSql(
            String geomFieldName, String centerAlias, String qualifiedTableName);

    /** 获取验证几何体的SQL */
    public abstract String getValidateGeometriesSql(
            String qualifiedTableName, String geomFieldName);

    /** 获取修复几何体的SQL */
    public abstract String getRepairGeometriesSql(String qualifiedTableName, String geomFieldName);

    /** 获取获取范围的SQL */
    public abstract String getGetExtentSql(
            String geomFieldName, String qualifiedTableName, int srid);

    @Override
    public DataFieldsApo dGetColumnsByTable(String tableName) {
        validateTableName(tableName);
        DataFieldsApo dataFieldsApo = getAdvDDLOpt().dGetColumnsByTable(tableName);
        try {
            Map<String, AdvEnumsTypeGeom> typeMaps =
                    eGetGeoTypeByTable(tableName, dataFieldsApo.getGeomFieldNameList());
            return fillGeomType(dataFieldsApo, typeMaps);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return dataFieldsApo;
    }

    @Override
    public AdvQueryGlobalConfig getConfig() {
        return getAdvBaseOpt().getConfig();
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySqlOrTable(String sqlViewOrTableName) {
        boolean sqlViewIs = dialectTableNameProcessor.tbTableIsSqlView(sqlViewOrTableName);
        AdvEnumsTypeGeom advEnumsTypeGeom = null;
        if (sqlViewIs) {
            advEnumsTypeGeom = eGetGeoTypeBySql(sqlViewOrTableName);
        } else {
            advEnumsTypeGeom = eGetGeoTypeByTable(sqlViewOrTableName);
        }
        return advEnumsTypeGeom;
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySqlOrTable(
            String sqlViewOrTableName, String geomFieldName) {
        boolean sqlViewIs = dialectTableNameProcessor.tbTableIsSqlView(sqlViewOrTableName);
        AdvEnumsTypeGeom advEnumsTypeGeom = null;
        if (sqlViewIs) {
            advEnumsTypeGeom = eGetGeoTypeBySql(sqlViewOrTableName, geomFieldName);
        } else {
            advEnumsTypeGeom = eGetGeoTypeByTable(sqlViewOrTableName, geomFieldName);
        }
        return advEnumsTypeGeom;
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySqlOrTable(
            String sqlViewOrTableName, List<String> geomFieldNames) {
        boolean sqlViewIs = dialectTableNameProcessor.tbTableIsSqlView(sqlViewOrTableName);
        Map<String, AdvEnumsTypeGeom> advEnumsTypeGeomMap = null;
        if (sqlViewIs) {
            advEnumsTypeGeomMap = eGetGeoTypeBySql(sqlViewOrTableName, geomFieldNames);
        } else {
            advEnumsTypeGeomMap = eGetGeoTypeByTable(sqlViewOrTableName, geomFieldNames);
        }
        return advEnumsTypeGeomMap;
    }

    @Override
    public boolean eIsGeomByTable(String tableName) {
        validateTableName(tableName);
        return StrUtil.isNotEmpty(eGetGeomColumnNameByTable(tableName));
    }

    @Override
    public boolean eIsGeomBySql(String sqlView) {
        return StrUtil.isNotEmpty(eGetGeomColumnNameBySql(sqlView));
    }

    @Override
    public boolean eIsGeomBySqlOrTable(String sqlViewOrTableName) {
        boolean sqlViewIs = dialectTableNameProcessor.tbTableIsSqlView(sqlViewOrTableName);
        boolean geomIs = false;
        if (sqlViewIs) {
            geomIs = eIsGeomBySql(sqlViewOrTableName);
        } else {
            geomIs = eIsGeomByTable(sqlViewOrTableName);
        }
        return geomIs;
    }

    @Override
    public boolean eIsGeomBySql(String dynamicSql, GirSqlParam sqlParam) {
        return StrUtil.isNotEmpty(eGetGeomColumnNameBySql(dynamicSql, sqlParam));
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeByTable(String tableName) {
        return eGetGeoTypeByTable(tableName, eGetGeomColumnNameByTable(tableName));
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeByTable(String tableName, String geomFieldName) {
        Map<String, AdvEnumsTypeGeom> map =
                eGetGeoTypeByTable(tableName, ListUtil.of(geomFieldName));
        return MapUtil.isEmpty(map) ? null : map.get(geomFieldName);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlView) {
        return eGetGeoTypeBySql(sqlView, eGetGeomColumnNameBySql(sqlView));
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlView, String geomFieldName) {
        Map<String, AdvEnumsTypeGeom> map = eGetGeoTypeBySql(sqlView, ListUtil.of(geomFieldName));
        return MapUtil.isEmpty(map) ? null : map.get(geomFieldName);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(String dynamicSql, GirSqlParam sqlParam) {
        return eGetGeoTypeBySql(
                dynamicSql, sqlParam, eGetGeomColumnNameBySql(dynamicSql, sqlParam));
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(
            String dynamicSql, GirSqlParam sqlParam, String geomFieldName) {
        Map<String, AdvEnumsTypeGeom> map =
                eGetGeoTypeBySql(dynamicSql, sqlParam, ListUtil.of(geomFieldName));
        return MapUtil.isEmpty(map) ? null : map.get(geomFieldName);
    }

    @Override
    public String eGetGeomColumnNameByTable(String tableName) {
        validateTableName(tableName);
        List<String> list = eGetGeomColumnNameListByTable(tableName);
        return CollectionUtil.isNotEmpty(list) ? list.get(0) : null;
    }

    @Override
    public List<String> eGetGeomColumnNameListByTable(String tableName) {
        DataFieldsApo dataFieldsApo = dGetColumnsByTable(tableName);
        return dataFieldsApo.getGeomFieldNameList();
    }

    @Override
    public List<String> eGetGeomColumnNameListBySql(String sqlView) {
        List<FieldBySchemaApo> fields = eGetGeomColumnListBySql(sqlView);
        DataFieldsApo dataFieldsApo = new DataFieldsApo();
        dataFieldsApo.setDataFieldList(fields);
        return dataFieldsApo.getFieldNameList();
    }

    @Override
    public List<String> eGetGeomColumnNameListBySqlOrTable(String sqlViewOrTableName) {
        boolean sqlViewIs = dialectTableNameProcessor.tbTableIsSqlView(sqlViewOrTableName);
        List<String> geomFieldNameList = null;
        if (sqlViewIs) {
            geomFieldNameList = eGetGeomColumnNameListBySql(sqlViewOrTableName);
        } else {
            geomFieldNameList = eGetGeomColumnNameListByTable(sqlViewOrTableName);
        }
        return geomFieldNameList;
    }

    @Override
    public List<String> eGetGeomColumnNameListBySql(String dynamicSql, GirSqlParam sqlParam) {
        List<FieldBySchemaApo> fields = eGetGeomColumnListBySql(dynamicSql, sqlParam);
        DataFieldsApo dataFieldsApo = new DataFieldsApo();
        dataFieldsApo.setDataFieldList(fields);
        return dataFieldsApo.getFieldNameList();
    }

    @Override
    public FieldBySchemaApo eGetGeomColumnByTable(String tableName) {
        List<FieldBySchemaApo> fields = eGetGeomColumnListByTable(tableName);
        return CollectionUtil.isNotEmpty(fields) ? fields.get(0) : null;
    }

    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListByTable(String tableName) {
        DataFieldsApo dataFieldsApo = dGetColumnsByTable(tableName);
        return dataFieldsApo.getGeomFields();
    }

    @Override
    public FieldBySchemaApo eGetGeomColumnBySql(String sqlView) {
        List<FieldBySchemaApo> fields = eGetGeomColumnListBySql(sqlView);
        return CollectionUtil.isNotEmpty(fields) ? fields.get(0) : null;
    }

    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListBySql(String sqlView) {
        DataFieldsApo dataFieldsApo = getAdvDDLOpt().dGetColumnsBySQL(sqlView);
        return dataFieldsApo.getGeomFields();
    }

    @Override
    public FieldBySchemaApo eGetGeomColumnBySql(String dynamicSql, GirSqlParam sqlParam) {
        List<FieldBySchemaApo> fields = eGetGeomColumnListBySql(dynamicSql, sqlParam);
        return CollectionUtil.isNotEmpty(fields) ? fields.get(0) : null;
    }

    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListBySql(String dynamicSql, GirSqlParam sqlParam) {
        DataFieldsApo dataFieldsApo = getAdvDDLOpt().dGetColumnsBySQL(dynamicSql, sqlParam);
        return dataFieldsApo.getGeomFields();
    }

    @Override
    public List<GirAdvOneRow> eQueryIntersects(
            String tableName, String geomFieldName, String geometry, int srid) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        validateSrid(srid);
        if (StrUtil.isEmpty(geometry)) {
            throw new IllegalArgumentException("几何体WKT不能为空");
        }

        String qualifiedTableName =
                getAdvDDLOpt().dIsTableExists(tableName)
                        ? dialectTableNameProcessor.tbGetTableNameWithSchema(
                                dataSourceGetter, tableName)
                        : StrUtil.format(
                                "({}) as {}",
                                tableName,
                                dialectTableNameProcessor.tbGetTempAliasTableName());

        String sql = getQueryIntersectsSql(qualifiedTableName, geomFieldName, geometry, srid);
        return getAdvBaseOpt().bSelectList(sql);
    }

    @Override
    public List<GirAdvOneRow> eQueryIntersects(String tableName, String geometry, int srid) {
        validateTableName(tableName);
        String geomFieldName = eGetGeomColumnNameByTable(tableName);
        return eQueryIntersects(tableName, geomFieldName, geometry, srid);
    }

    @Override
    public List<GirAdvOneRow> eQueryWithinBBox(
            String tableName, String geomFieldName, double[] bbox, int srid) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        validateBbox(bbox);
        validateSrid(srid);
        String qualifiedTableName =
                getAdvDDLOpt().dIsTableExists(tableName)
                        ? dialectTableNameProcessor.tbGetTableNameWithSchema(
                                dataSourceGetter, tableName)
                        : StrUtil.format(
                                "({}) as {}",
                                tableName,
                                dialectTableNameProcessor.tbGetTempAliasTableName());
        String bboxWkt =
                StrUtil.format(
                        "POLYGON(({} {}, {} {}, {} {}, {} {}, {} {}))",
                        bbox[0],
                        bbox[1],
                        bbox[0],
                        bbox[3],
                        bbox[2],
                        bbox[3],
                        bbox[2],
                        bbox[1],
                        bbox[0],
                        bbox[1]);
        String sql = getQueryWithinBBoxSql(qualifiedTableName, geomFieldName, bboxWkt, srid);
        return getAdvBaseOpt().bSelectList(sql);
    }

    @Override
    public List<GirAdvOneRow> eQueryWithinBBox(String tableName, double[] bbox, int srid) {
        validateTableName(tableName);
        validateBbox(bbox);
        validateSrid(srid);
        String geomFieldName = eGetGeomColumnNameByTable(tableName);
        return eQueryWithinBBox(tableName, geomFieldName, bbox, srid);
    }

    @Override
    public Integer eGetSrid(String tableNameOrSqlView) {
        boolean isSqlView = dialectTableNameProcessor.tbTableIsSqlView(tableNameOrSqlView);
        String geomFieldName =
                isSqlView
                        ? eGetGeomColumnNameBySql(tableNameOrSqlView)
                        : eGetGeomColumnNameByTable(tableNameOrSqlView);
        return eGetSrid(tableNameOrSqlView, geomFieldName);
    }

    @Override
    public Integer eGetSrid(String tableNameOrSqlView, String geomFieldName) {
        // 实现获取SRID的逻辑
        return 0; // 占位实现
    }

    @Override
    public List<GirAdvOneRow> eCalculateDistance(
            String tableName,
            String geomFieldName,
            String geometry,
            int srid,
            String distanceAlias) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        validateSrid(srid);
        if (StrUtil.isEmpty(geometry) || StrUtil.isEmpty(distanceAlias)) {
            throw new IllegalArgumentException("几何体WKT和距离字段别名不能为空");
        }

        String qualifiedTableName =
                getAdvDDLOpt().dIsTableExists(tableName)
                        ? dialectTableNameProcessor.tbGetTableNameWithSchema(
                                dataSourceGetter, tableName)
                        : StrUtil.format(
                                "({}) as {}",
                                tableName,
                                dialectTableNameProcessor.tbGetTempAliasTableName());

        String sql =
                getCalculateDistanceSql(
                        geomFieldName, geometry, srid, distanceAlias, qualifiedTableName);
        return getAdvBaseOpt().bSelectList(sql);
    }

    @Override
    public List<GirAdvOneRow> eCalculateDistance(
            String tableName, String geometry, int srid, String distanceAlias) {
        validateTableName(tableName);
        validateSrid(srid);
        String geomFieldName = eGetGeomColumnNameByTable(tableName);
        return eCalculateDistance(tableName, geomFieldName, geometry, srid, distanceAlias);
    }

    @Override
    public List<GirAdvOneRow> eGetCentroid(
            String tableNameOrSqlView, String geomFieldName, String centerAlias) {
        if (StrUtil.isEmpty(tableNameOrSqlView)
                || StrUtil.isEmpty(geomFieldName)
                || StrUtil.isEmpty(centerAlias)) {
            throw new IllegalArgumentException("表名、空间字段名和中心点字段别名不能为空");
        }
        String qualifiedTableName =
                getAdvDDLOpt().dIsTableExists(tableNameOrSqlView)
                        ? dialectTableNameProcessor.tbGetTableNameWithSchema(
                                dataSourceGetter, tableNameOrSqlView)
                        : StrUtil.format(
                                "({}) as {}",
                                dialectTableNameProcessor.tbRemoveSqlSpaces(tableNameOrSqlView),
                                dialectTableNameProcessor.tbGetTempAliasTableName());

        String sql = getCentroidSql(geomFieldName, centerAlias, qualifiedTableName);
        return getAdvBaseOpt().bSelectList(sql);
    }

    @Override
    public List<GirAdvOneRow> eGetCentroid(String tableNameOrSqlView, String centerAlias) {
        String geomFieldName =
                dialectTableNameProcessor.tbTableIsSqlView(tableNameOrSqlView)
                        ? eGetGeomColumnNameBySql(tableNameOrSqlView)
                        : eGetGeomColumnNameByTable(tableNameOrSqlView);
        return eGetCentroid(tableNameOrSqlView, geomFieldName, centerAlias);
    }

    @Override
    public List<Object> eValidateGeometries(String tableName, String geomFieldName) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        if (!getAdvDDLOpt().dIsTableExists(tableName)) {
            throw new RuntimeException("表[" + tableName + "]不存在");
        }

        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = getValidateGeometriesSql(qualifiedTableName, geomFieldName);

        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        List<Object> invalidIds = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(rows)) {
            rows.forEach(row -> invalidIds.add(row.get("id")));
        }
        return invalidIds;
    }

    @Override
    public List<Object> eValidateGeometries(String tableName) {
        validateTableName(tableName);
        String geomFieldName = eGetGeomColumnNameByTable(tableName);
        return eValidateGeometries(tableName, geomFieldName);
    }

    @Override
    public int eRepairGeometries(String tableName, String geomFieldName) {
        validateTableName(tableName);
        validateGeomFieldName(geomFieldName);
        if (!getAdvDDLOpt().dIsTableExists(tableName)) {
            throw new RuntimeException("表[" + tableName + "]不存在");
        }

        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = getRepairGeometriesSql(qualifiedTableName, geomFieldName);

        Connection conn = null;
        Statement stmt = null;
        int updatedCount = 0;
        try {
            conn = dataSourceGetter.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            updatedCount = stmt.executeUpdate(sql);
            conn.commit();
            log.debug("修复表[{}]中的无效几何体，共修复{}条记录", tableName, updatedCount);
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                log.warn("修复几何体回滚失败", ex);
            }
            log.error("修复表[{}]中的无效几何体失败", tableName, e);
            throw new RuntimeException("修复几何体失败: " + e.getMessage(), e);
        } finally {
            dataSourceGetter.closeResources(null, stmt, conn);
        }
        return updatedCount;
    }

    @Override
    public int eRepairGeometries(String tableName) {
        validateTableName(tableName);
        String geomFieldName = eGetGeomColumnNameByTable(tableName);
        return eRepairGeometries(tableName, geomFieldName);
    }

    @Override
    public BBoxApo eGetExtent(String tableNameOrSqlView, String geomFieldName) {
        if (StrUtil.isEmpty(tableNameOrSqlView) || StrUtil.isEmpty(geomFieldName)) {
            return new BBoxApo(new double[4], new double[4], 0);
        }

        String qualifiedTableName =
                getAdvDDLOpt().dIsTableExists(tableNameOrSqlView)
                        ? dialectTableNameProcessor.tbGetTableNameWithSchema(
                                dataSourceGetter, tableNameOrSqlView)
                        : StrUtil.format(
                                "({}) as {}",
                                dialectTableNameProcessor.tbRemoveSqlSpaces(tableNameOrSqlView),
                                dialectTableNameProcessor.tbGetTempAliasTableName());

        Integer srid = eGetSrid(tableNameOrSqlView, geomFieldName);

        String sql = getGetExtentSql(geomFieldName, qualifiedTableName, srid);
        sql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);

        if (row == null) {
            return new BBoxApo(new double[4], new double[4], 0);
        }

        double[] bbox = {
            row.getDouble("minx", 0.0),
            row.getDouble("miny", 0.0),
            row.getDouble("maxx", 0.0),
            row.getDouble("maxy", 0.0)
        };
        double[] bbox4326 = {
            row.getDouble("minx_gs", 0.0),
            row.getDouble("miny_gs", 0.0),
            row.getDouble("maxx_gs", 0.0),
            row.getDouble("maxy_gs", 0.0)
        };
        return new BBoxApo(bbox, bbox4326, srid);
    }

    @Override
    public BBoxApo eGetExtent(String tableNameOrSqlView) {
        boolean b = dialectTableNameProcessor.tbTableIsSqlView(tableNameOrSqlView);
        String geomFieldName = null;
        if (b) {
            geomFieldName = eGetGeomColumnNameBySql(tableNameOrSqlView);
        } else {
            geomFieldName = eGetGeomColumnNameByTable(tableNameOrSqlView);
        }
        return eGetExtent(tableNameOrSqlView, geomFieldName);
    }

    @Override
    public GirAdvOneRow eSelectOne(String sql, AdvEnumsGeomOpt advEnumsGeomOpt) {
        List<String> geomFieldNameList = new ArrayList<>();
        boolean isNotOpt =
                advEnumsGeomOpt == null || advEnumsGeomOpt.equals(AdvEnumsGeomOpt.不做任何操作);
        if (!isNotOpt) {
            geomFieldNameList = eGetGeomColumnNameListBySql(sql);
        }
        return eSelectOne(sql, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public GirAdvOneRow eSelectOne(
            String sql, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList) {
        GirAdvOneRow row = getAdvBaseOpt().bSelectOne(sql);
        if (ObjectUtil.isNotNull(geomFieldNameList) && ObjectUtil.isNotNull(row)) {
            GirAdvQueryCommonUtils.transGeometryField(
                    ListUtil.of(row), advEnumsGeomOpt, geomFieldNameList);
        }
        return row;
    }

    @Override
    public GirAdvOneRow eSelectOne(
            String sql, AdvEnumsGeomOpt advEnumsGeomOpt, String geomFieldName) {
        return eSelectOne(sql, advEnumsGeomOpt, ListUtil.of(geomFieldName));
    }

    @Override
    public GirAdvOneRow eSelectOne(
            String dynamicSql, GirSqlParam sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt) {
        List<String> geomFieldNameList = new ArrayList<>();
        boolean isNotOpt =
                advEnumsGeomOpt == null || advEnumsGeomOpt.equals(AdvEnumsGeomOpt.不做任何操作);
        if (!isNotOpt) {
            geomFieldNameList = eGetGeomColumnNameListBySql(dynamicSql, sqlParam);
        }
        return eSelectOne(dynamicSql, sqlParam, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public GirAdvOneRow eSelectOne(
            String sqlStatement,
            GirSqlParam sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            String geomFieldName) {
        return eSelectOne(sqlStatement, sqlParam, advEnumsGeomOpt, ListUtil.of(geomFieldName));
    }

    @Override
    public GirAdvOneRow eSelectOne(
            String dynamicSql,
            GirSqlParam sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            List<String> geomFieldNameList) {
        GirAdvOneRow row = null;
        row = getAdvBaseOpt().bSelectOne(dynamicSql, sqlParam);
        if (ObjectUtil.isNotNull(geomFieldNameList) && ObjectUtil.isNotNull(row)) {
            GirAdvQueryCommonUtils.transGeometryField(
                    ListUtil.of(row), advEnumsGeomOpt, geomFieldNameList);
        }
        return row;
    }

    @Override
    public List<GirAdvOneRow> eSelectList(String sql, AdvEnumsGeomOpt advEnumsGeomOpt) {
        List<String> geomFieldNameList = new ArrayList<>();
        boolean isNotOpt =
                advEnumsGeomOpt == null || advEnumsGeomOpt.equals(AdvEnumsGeomOpt.不做任何操作);
        if (!isNotOpt) {
            geomFieldNameList = eGetGeomColumnNameListBySql(sql);
        }
        return eSelectList(sql, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String sql, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList) {
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(sql);
        if (ObjectUtil.isNotNull(geomFieldNameList) && CollectionUtil.isNotEmpty(rows)) {
            GirAdvQueryCommonUtils.transGeometryField(rows, advEnumsGeomOpt, geomFieldNameList);
        }
        return rows;
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String sql, AdvEnumsGeomOpt advEnumsGeomOpt, String geomFieldName) {
        return eSelectList(sql, advEnumsGeomOpt, ListUtil.of(geomFieldName));
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String dynamicSql, GirSqlParam sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt) {
        List<String> geomFieldNameList = new ArrayList<>();
        boolean isNotOpt =
                advEnumsGeomOpt == null || advEnumsGeomOpt.equals(AdvEnumsGeomOpt.不做任何操作);
        if (!isNotOpt) {
            geomFieldNameList = eGetGeomColumnNameListBySql(dynamicSql, sqlParam);
        }
        return eSelectList(dynamicSql, sqlParam, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String sqlStatement,
            GirSqlParam sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            String geomFieldName) {
        return eSelectList(sqlStatement, sqlParam, advEnumsGeomOpt, ListUtil.of(geomFieldName));
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String dynamicSql,
            GirSqlParam sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            List<String> geomFieldNameList) {
        List<GirAdvOneRow> rows = getAdvBaseOpt().bSelectList(dynamicSql, sqlParam);
        if (ObjectUtil.isNotNull(geomFieldNameList) && CollectionUtil.isNotEmpty(rows)) {
            GirAdvQueryCommonUtils.transGeometryField(rows, advEnumsGeomOpt, geomFieldNameList);
        }
        return rows;
    }

    @Override
    public boolean eIsPointTable(String tableName) {
        validateTableName(tableName);
        AdvEnumsTypeGeom geomType = eGetGeoTypeByTable(tableName);
        return AdvEnumsTypeGeom.Point.equals(geomType)
                || AdvEnumsTypeGeom.MultiPoint.equals(geomType);
    }

    @Override
    public boolean eIsPolygonTable(String tableName) {
        validateTableName(tableName);
        AdvEnumsTypeGeom geomType = eGetGeoTypeByTable(tableName);
        return AdvEnumsTypeGeom.Polygon.equals(geomType)
                || AdvEnumsTypeGeom.MultiPolygon.equals(geomType);
    }

    @Override
    public boolean eIsLineStringTable(String tableName) {
        validateTableName(tableName);
        AdvEnumsTypeGeom geomType = eGetGeoTypeByTable(tableName);
        return AdvEnumsTypeGeom.LineString.equals(geomType)
                || AdvEnumsTypeGeom.MultiLineString.equals(geomType);
    }

    @Override
    public void eDropGeomColumn(String tableName) {
        validateTableName(tableName);
        String geomFieldName = eGetGeomColumnNameByTable(tableName);
        eDropGeomColumn(tableName, geomFieldName);
    }

    @Override
    public void eTransformSrid(String tableName, int targetSrid) {
        validateTableName(tableName);
        validateSrid(targetSrid);
        String geomFieldName = eGetGeomColumnNameByTable(tableName);
        eTransformSrid(tableName, geomFieldName, targetSrid);
    }

    @Override
    public void eCreateSpatialIndex(String tableName, String indexName) {
        validateTableName(tableName);
        String geomFieldName = eGetGeomColumnNameByTable(tableName);
        eCreateSpatialIndex(tableName, geomFieldName, indexName);
    }

    // ===================== 校验方法 =====================

    /** 校验表名非空 */
    protected void validateTableName(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }
    }

    /** 校验空间字段名非空 */
    protected void validateGeomFieldName(String geomFieldName) {
        if (StrUtil.isEmpty(geomFieldName)) {
            throw new IllegalArgumentException("空间字段名不能为空");
        }
    }

    /** 校验SRID合法性 */
    protected void validateSrid(int srid) {
        if (srid <= 0) {
            throw new IllegalArgumentException("SRID必须为正数");
        }
    }

    /** 校验边界框合法性 */
    protected void validateBbox(double[] bbox) {
        if (bbox == null || bbox.length != 4) {
            throw new IllegalArgumentException("边界框不能为空，且必须包含4个元素[minx, miny, maxx, maxy]");
        }
    }

    /** 填充空间类型到DataFieldsApo */
    protected DataFieldsApo fillGeomType(
            DataFieldsApo dataFieldsApo, Map<String, AdvEnumsTypeGeom> typeMaps) {
        if (CollectionUtil.isEmpty(typeMaps)
                || CollectionUtil.isEmpty(dataFieldsApo.getDataFieldList())) {
            return dataFieldsApo;
        }
        for (FieldBySchemaApo field : dataFieldsApo.getDataFieldList()) {
            AdvEnumsTypeGeom geomType = typeMaps.get(field.getColumnName());
            if (ObjectUtil.isNotNull(geomType)) {
                field.setGeomType(geomType);
            }
        }
        return dataFieldsApo;
    }
}
