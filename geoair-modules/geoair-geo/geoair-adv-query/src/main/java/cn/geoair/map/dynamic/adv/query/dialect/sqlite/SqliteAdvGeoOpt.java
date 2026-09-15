package cn.geoair.map.dynamic.adv.query.dialect.sqlite;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvGeoOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;

import java.util.List;
import java.util.Map;

/**
 * SQLite Core 的空间能力边界。
 *
 * <p>普通 SQLite 没有 geometry_columns、SRID、空间索引及 ST_* 函数。本实现保留统一执行器的
 * 非空间查询通道，但所有空间 API 都明确失败；后续 SpatiaLite 应提供独立方言实现。
 */
public class SqliteAdvGeoOpt extends AbstractExecAdvGeoOpt {

    private static final String MESSAGE =
            "SQLite Core 不支持空间能力；geometry/SRID/空间索引/ST_* 函数需要后续 SpatiaLite 方言";

    private final IAdvBaseOpt baseOpt;
    private final IAdvDDLOpt ddlOpt;

    public SqliteAdvGeoOpt(
            IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt, IAdvDDLOpt ddlOpt) {
        super(dataSourceGetter);
        this.baseOpt = baseOpt;
        this.ddlOpt = ddlOpt;
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException(MESSAGE);
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return SqliteDialectTableNameUtil.getInstance();
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
        throw unsupported();
    }

    @Override
    public List<String> eGetAllGeoLayerName() {
        throw unsupported();
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeByTable(
            String tableName, List<String> geomFieldNames) {
        throw unsupported();
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(
            String sqlView, List<String> geomFieldNames) {
        throw unsupported();
    }

    @Override
    public List<String> eGetGeomColumnNameListByTable(String tableName) {
        throw unsupported();
    }

    @Override
    public String eGetGeomColumnNameBySql(String sqlView) {
        throw unsupported();
    }

    @Override
    public Integer eGetSrid(String tableNameOrSqlView, String geomFieldName) {
        throw unsupported();
    }

    @Override
    public Map<String, Integer> eGetSrid(
            String tableNameOrSqlView, List<String> geomFieldNames) {
        throw unsupported();
    }

    @Override
    public void eAddGeomColumn(
            String tableName, String geomFieldName, AdvEnumsTypeGeom geomType, int srid) {
        throw unsupported();
    }

    @Override
    public void eDropGeomColumn(String tableName, String geomFieldName) {
        throw unsupported();
    }

    @Override
    public void eTransformSrid(String tableName, String geomFieldName, int targetSrid) {
        throw unsupported();
    }

    @Override
    public void eCreateSpatialIndex(String tableName, String geomFieldName, String indexName) {
        throw unsupported();
    }

    @Override
    public void eDropSpatialIndex(String tableName, String indexName) {
        throw unsupported();
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(
            String dynamicSql, GirSqlParam sqlParam, List<String> geomFieldNames) {
        throw unsupported();
    }

    @Override
    public String eGetGeomColumnNameBySql(String dynamicSql, GirSqlParam sqlParam) {
        throw unsupported();
    }

    @Override
    public String getQueryIntersectsSql(
            String qualifiedTableName, String geomFieldName, String geometry, int srid) {
        throw unsupported();
    }

    @Override
    public String getQueryWithinBBoxSql(
            String qualifiedTableName, String geomFieldName, String bboxWkt, int srid) {
        throw unsupported();
    }

    @Override
    public String getCalculateDistanceSql(
            String geomFieldName,
            String geometry,
            int srid,
            String distanceAlias,
            String qualifiedTableName) {
        throw unsupported();
    }

    @Override
    public String getCentroidSql(
            String geomFieldName, String centerAlias, String qualifiedTableName) {
        throw unsupported();
    }

    @Override
    public String getValidateGeometriesSql(
            String qualifiedTableName, String geomFieldName) {
        throw unsupported();
    }

    @Override
    protected String buildValidateGeometriesByPrimaryKeysSql(
            String qualifiedTableName, String geomFieldName, List<String> primaryKeys) {
        throw unsupported();
    }

    @Override
    public String getRepairGeometriesSql(
            String qualifiedTableName, String geomFieldName) {
        throw unsupported();
    }

    @Override
    public String getGetExtentSql(
            String geomFieldName, String qualifiedTableName, int srid) {
        throw unsupported();
    }
}
