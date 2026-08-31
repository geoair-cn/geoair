package cn.geoair.map.dynamic.mvt.exec;

import cn.geoair.base.util.GutilObject;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.mvt.tools.param.TileExecParams;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Oracle Spatial 矢量瓦片执行器
 *
 * <p>Oracle 使用完全自包含的 SQL：SDO_GEOMETRY 包围盒直接写在 SELECT 中，
 *
 * <p>
 */
public class OracleVectorTileExecutor extends AbstractVectorTileExecutor {

    public OracleVectorTileExecutor(
            TileRequestParams requestParams, String layerName, IAdvExecutor iAdvExecutor) {
        super(requestParams, layerName, iAdvExecutor);
    }

    @Override
    public String getExecSql(TileExecParams tileExecParams) {
        String tableAlias = "root_tt";
        // 构建表名
        String tbNameOrSql = requestParams.getTbNameOrSql();
        String schema = requestParams.getSchemaName();
        boolean isSqlView = iAdvExecutor.tbTableIsSqlView(tbNameOrSql);
        String finalTbName;
        if (isSqlView) {
            String tbRemoveSqlSpaces = iAdvExecutor.tbRemoveSqlSpaces(tbNameOrSql);
            finalTbName =
                    iAdvExecutor.tbBuildAsTable(
                            StrUtil.format("({})", tbRemoveSqlSpaces), tableAlias);
        } else {
            String tbGetTableNameWithSchema =
                    iAdvExecutor.tbGetTableNameWithSchema(tbNameOrSql, schema);
            finalTbName =
                    iAdvExecutor.tbBuildAsTable(
                            StrUtil.format("( select * from  {})", tbGetTableNameWithSchema),
                            tableAlias);
        }

        // 校验必要参数
        Envelope dataExtent = tileExecParams.getDataExtent();
        if (dataExtent == null
                || requestParams.getSrid() == null
                || requestParams.getGeomFieldName() == null
                || requestParams.getDsId() == null) {
            log.error("缺少必要参数，无法查询瓦片数据，当前参数：{}", requestParams.toString());
            return null;
        }

        String geomFieldName = requestParams.getGeomFieldName();
        String srid = requestParams.getSrid();
        String geomField = tableAlias + "." + geomFieldName;

        // Oracle 不用 WITH CTE，直接把 SDO_GEOMETRY 构造为内联子查询
        Envelope bufEnv = tileExecParams.getDataExtentBufferEnvelope();
        String bboxExpr =
                StrUtil.format(
                        "SDO_GEOMETRY(2003, {}, NULL, SDO_ELEM_INFO_ARRAY(1,1003,3), SDO_ORDINATE_ARRAY({},{},{},{}))",
                        sourceDataSrid,
                        bufEnv.getMinX(),
                        bufEnv.getMinY(),
                        bufEnv.getMaxX(),
                        bufEnv.getMaxY());

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        if (ObjectUtil.isEmpty(keepFieldList) && !requestParams.isKeepFieldAll()) {
            sql.append(StrUtil.format("{}.{}", tableAlias, geomFieldName))
                    .append(" as ")
                    .append(GEOM_FIELD_ALIAS_IN_SQL);
        } else if (requestParams.isKeepFieldAll()) {
            sql.append(tableAlias)
                    .append(".*")
                    .append(", ")
                    .append(StrUtil.format("{}.{}", tableAlias, geomFieldName))
                    .append(" as ")
                    .append(GEOM_FIELD_ALIAS_IN_SQL);
        } else {
            List<String> aliasFields = new ArrayList<>();
            for (String field : keepFieldList) {
                if (StrUtil.isNotBlank(field)) {
                    aliasFields.add(tableAlias + "." + StrUtil.wrap(field, "\""));
                }
            }
            sql.append(String.join(", ", aliasFields))
                    .append(", ")
                    .append(StrUtil.format("{}.{}", tableAlias, geomFieldName))
                    .append(" as ")
                    .append(GEOM_FIELD_ALIAS_IN_SQL);
        }

        sql.append(" FROM ")
                .append(finalTbName)
                .append(" WHERE SDO_RELATE(")
                .append(geomField)
                .append(", ")
                .append(bboxExpr)
                .append(", 'MASK=ANYINTERACT') = 'TRUE'")
                .append(" AND ")
                .append(geomField)
                .append(" IS NOT NULL");

        return sql.toString();
    }

    @Override
    protected String getBufferBboxSqlFunction(TileExecParams tileExecParams) {
        // Oracle 走重写的 getExecSql，不调用此方法
        return "";
    }

    @Override
    protected String getGeomExportExpr(String tableAlias, String geomFieldName) {
        // Oracle 走重写的 getExecSql，不调用此方法
        return "";
    }

    @Override
    protected String getIntersectsWhereExpr(String geomFieldExpr, String withQueryAlias) {
        // Oracle 走重写的 getExecSql，不调用此方法
        return "";
    }

    public void featuresTransform(GirAdvOneRow oneRow) {
        try {
            String upperCase = GEOM_FIELD_ALIAS_IN_SQL.toUpperCase();

            Object o = oneRow.get(upperCase);
            if (GutilObject.isEmpty(o)) {
                oneRow.remove(upperCase);
                return;
            }
            Geometry geometry = oneRow.getGeometry(upperCase);
            if (!ObjectUtil.equals(gridSrid, sourceDataSrid)) {
                geometry =
                        GirGeoTools.defaultInstance()
                                .getSridOpt()
                                .convert(geometry, sourceDataSrid, gridSrid);
            }
            Geometry finalGeometry = geometry;
            if (geometry != null) {
                try {
                    Geometry gridExtentBufferBoxGeom =
                            this.tileExecParams.getGridExtentBufferBoxGeom();
                    finalGeometry = gridExtentBufferBoxGeom.intersection(geometry);
                } catch (Exception e) {
                    // 裁剪失败时忽略，后续 PipelineBuilder 会再裁剪
                }
            }
            if (finalGeometry != null && !finalGeometry.isEmpty()) {
                oneRow.put(GEOM_FIELD_ALIAS_IN_TRAN, finalGeometry);
            }
            oneRow.remove(upperCase);
        } catch (Exception e) {
            log.error("featuresTransform异常", e);
            throw new RuntimeException("几何转换错误");
        }
    }
}
