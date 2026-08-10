package cn.geoair.map.dynamic.mvt.exec;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.mvt.tools.param.TileExecParams;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.locationtech.jts.geom.Envelope;

import java.util.ArrayList;
import java.util.List;

/**
 * Oracle Spatial 矢量瓦片执行器
 * <p>
 * Oracle 使用完全自包含的 SQL：SDO_GEOMETRY 包围盒直接写在 SELECT 中，
 * 避免 WITH CTE 与 CROSS JOIN 的兼容性问题（Oracle 不兼容 WITH ... CROSS JOIN）。
 * <p>
 * 几何导出使用 WKT 格式，featuresTransform 中走 WKT 解码路径。
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
            finalTbName = iAdvExecutor.tbBuildAsTable(
                    StrUtil.format("({})", tbRemoveSqlSpaces), tableAlias);
        } else {
            String tbGetTableNameWithSchema =
                    iAdvExecutor.tbGetTableNameWithSchema(tbNameOrSql, schema);
            finalTbName = iAdvExecutor.tbBuildAsTable(
                    StrUtil.format("( select * from  {})", tbGetTableNameWithSchema), tableAlias);
        }

        // 校验必要参数
        Envelope dataExtent = tileExecParams.getDataExtent();
        if (dataExtent == null || requestParams.getSrid() == null
                || requestParams.getGeomFieldName() == null || requestParams.getDsId() == null) {
            log.error("缺少必要参数，无法查询瓦片数据，当前参数：{}", requestParams.toString());
            return null;
        }

        String geomFieldName = requestParams.getGeomFieldName();
        String srid = requestParams.getSrid();
        String geomField = tableAlias + "." + geomFieldName;

        // Oracle 不用 WITH CTE，直接把 SDO_GEOMETRY 构造为内联子查询
        Envelope bufEnv = tileExecParams.getDataExtentBufferEnvelope();
        String bboxExpr = StrUtil.format(
                "SDO_GEOMETRY(2003, {}, NULL, SDO_ELEM_INFO_ARRAY(1,1003,3), SDO_ORDINATE_ARRAY({},{},{},{}))",
                sourceDataSrid,
                bufEnv.getMinX(), bufEnv.getMinY(), bufEnv.getMaxX(), bufEnv.getMaxY());

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        if (ObjectUtil.isEmpty(keepFieldList) && !requestParams.isKeepFieldAll()) {
            sql.append(StrUtil.format("SDO_UTIL.TO_WKTGEOMETRY({}.{})", tableAlias, geomFieldName))
               .append(" as ").append(GEOM_FIELD_ALIAS_IN_SQL);
        } else if (requestParams.isKeepFieldAll()) {
            sql.append(tableAlias).append(".*")
               .append(", ").append(StrUtil.format("SDO_UTIL.TO_WKTGEOMETRY({}.{})", tableAlias, geomFieldName))
               .append(" as ").append(GEOM_FIELD_ALIAS_IN_SQL);
        } else {
            List<String> aliasFields = new ArrayList<>();
            for (String field : keepFieldList) {
                if (StrUtil.isNotBlank(field)) {
                    aliasFields.add(tableAlias + "." + StrUtil.wrap(field, "\""));
                }
            }
            sql.append(String.join(", ", aliasFields))
               .append(", ").append(StrUtil.format("SDO_UTIL.TO_WKTGEOMETRY({}.{})", tableAlias, geomFieldName))
               .append(" as ").append(GEOM_FIELD_ALIAS_IN_SQL);
        }

        sql.append(" FROM ").append(finalTbName)
           .append(" WHERE SDO_RELATE(").append(geomField).append(", ").append(bboxExpr)
           .append(", 'MASK=ANYINTERACT') = 'TRUE'")
           .append(" AND ").append(geomField).append(" IS NOT NULL");

        return sql.toString();
    }

    @Override
    protected String getBufferBboxSqlFunction(TileExecParams tileExecParams) {
        // Oracle 走重写的 getExecSql，不调用此方法
        return "";
    }

    @Override
    protected String getGeomExportExpr(String tableAlias, String geomFieldName) {
        return StrUtil.format("SDO_UTIL.TO_WKTGEOMETRY({}.{})", tableAlias, geomFieldName);
    }

    @Override
    protected String getIntersectsWhereExpr(String geomFieldExpr, String withQueryAlias) {
        return StrUtil.format("SDO_RELATE({}, {}.{}, 'MASK=ANYINTERACT') = 'TRUE'",
                geomFieldExpr, withQueryAlias, geomBox);
    }

    @Override
    protected String getGeomEncodingFormat() {
        return "wkt";
    }
}
