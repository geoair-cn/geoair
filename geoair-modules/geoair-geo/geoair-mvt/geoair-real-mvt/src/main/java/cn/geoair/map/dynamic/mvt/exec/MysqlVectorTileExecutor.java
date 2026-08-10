package cn.geoair.map.dynamic.mvt.exec;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.mvt.tools.param.TileExecParams;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.locationtech.jts.geom.Envelope;

/**
 * MySQL Spatial 矢量瓦片执行器
 * <p>
 * 使用 MySQL 空间函数：ST_GeomFromText、ST_Intersects、ST_AsBinary、TO_BASE64 等
 */
public class MysqlVectorTileExecutor extends AbstractVectorTileExecutor {

    public MysqlVectorTileExecutor(
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
                "ST_GeomFromText('POLYGON(({} {}, {} {}, {} {}, {} {}, {} {}))', {})",
                xmin, ymin, xmin, ymax, xmax, ymax, xmax, ymin, xmin, ymin, sourceDataSrid);
    }

    @Override
    protected String getGeomExportExpr(String tableAlias, String geomFieldName) {
        return StrUtil.format("TO_BASE64(ST_AsBinary({}.{}))",
                tableAlias, geomFieldName);
    }

    @Override
    protected String getIntersectsWhereExpr(String geomFieldExpr, String withQueryAlias) {
        return StrUtil.format("ST_Intersects({}, {}.{})",
                geomFieldExpr, withQueryAlias, geomBox);
    }

    @Override
    protected String getGeomEncodingFormat() {
        return "wkb_base64";
    }

    @Override
    protected String getGeomFieldWithSrid(String tableAlias, String geomFieldName, String srid) {
        if (ObjectUtil.equals(srid, "0")) {
            return "ST_SetSRID(" + tableAlias + "." + geomFieldName + "," + srid + ")";
        }
        return tableAlias + "." + geomFieldName;
    }
}
