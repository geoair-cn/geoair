package cn.geoair.map.dynamic.mvt.exec;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.mvt.tools.param.TileExecParams;
import cn.hutool.core.util.StrUtil;
import org.locationtech.jts.geom.Envelope;

/**
 * Oracle Spatial 矢量瓦片执行器
 * <p>
 * 使用 Oracle Spatial SDO 系列函数：SDO_GEOMETRY、SDO_RELATE、SDO_UTIL.TO_WKTGEOMETRY 等
 * <p>
 * 几何导出使用 WKT 格式（Oracle 无 ST_AsBinary），featuresTransform 中走 WKT 解码路径
 */
public class OracleVectorTileExecutor extends AbstractVectorTileExecutor {

    public OracleVectorTileExecutor(
            TileRequestParams requestParams, String layerName, IAdvExecutor iAdvExecutor) {
        super(requestParams, layerName, iAdvExecutor);
    }

    @Override
    protected String getBufferBboxSqlFunction(TileExecParams tileExecParams) {
        Envelope dataExtentBufferEnvelope = tileExecParams.getDataExtentBufferEnvelope();
        double xmin = dataExtentBufferEnvelope.getMinX();
        double ymin = dataExtentBufferEnvelope.getMinY();
        double xmax = dataExtentBufferEnvelope.getMaxX();
        double ymax = dataExtentBufferEnvelope.getMaxY();
        // Oracle: SDO_GEOMETRY(2003, srid, NULL, SDO_ELEM_INFO_ARRAY(1,1003,3), SDO_ORDINATE_ARRAY(...))
        return StrUtil.format(
                "SDO_GEOMETRY(2003, {}, NULL, SDO_ELEM_INFO_ARRAY(1,1003,3), SDO_ORDINATE_ARRAY({},{},{},{}))",
                sourceDataSrid, xmin, ymin, xmax, ymax);
    }

    @Override
    protected String getGeomExportExpr(String tableAlias, String geomFieldName) {
        return StrUtil.format("SDO_UTIL.TO_WKTGEOMETRY({}.{})",
                tableAlias, geomFieldName);
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
