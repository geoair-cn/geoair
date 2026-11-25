package cn.geoair.map.dynamic.mvt.exec;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.mvt.tools.param.TileExecParams;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.locationtech.jts.geom.Envelope;

/**
 * PostGIS 矢量瓦片执行器
 *
 * <p>使用 PostGIS 方言函数：ST_MakeEnvelope、ST_Intersects、ST_AsBinary 等
 */
public class PostgisVectorTileExecutor extends AbstractVectorTileExecutor {

    public PostgisVectorTileExecutor(
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
        return StrUtil.format(
                "public.ST_MakeEnvelope({}, {}, {}, {}, {})",
                xmin,
                ymin,
                xmax,
                ymax,
                sourceDataSrid);
    }

    @Override
    protected String getGeomExportExpr(String tableAlias, String geomFieldName) {
        return StrUtil.format(
                "encode(public.ST_AsBinary(public.ST_Force2D({}.{})), 'base64')",
                tableAlias,
                geomFieldName);
    }

    @Override
    protected String getIntersectsWhereExpr(String geomFieldExpr, String withQueryAlias) {
        return StrUtil.format(
                "public.ST_Intersects({}, {}.{})", geomFieldExpr, withQueryAlias, geomBox);
    }

    @Override
    protected String getGeomFieldWithSrid(String tableAlias, String geomFieldName, String srid) {
        if (ObjectUtil.equals(srid, "0")) {
            return "public.ST_SetSRID("
                    + tableAlias
                    + "."
                    + geomFieldName
                    + ","
                    + sourceDataSrid
                    + ")";
        }
        return tableAlias + "." + geomFieldName;
    }
}
