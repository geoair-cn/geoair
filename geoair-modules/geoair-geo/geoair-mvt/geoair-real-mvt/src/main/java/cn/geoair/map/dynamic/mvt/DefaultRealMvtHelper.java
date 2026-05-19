package cn.geoair.map.dynamic.mvt;

import cn.geoair.map.dynamic.mvt.consumer.VectorTileBuilderConsumer;
import cn.geoair.map.dynamic.mvt.consumer.VectorTileBuilderConsumerByJts;
import cn.geoair.map.dynamic.mvt.dto.ParamCheckResult;
import cn.geoair.map.dynamic.mvt.dto.TileGlobalConfig;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import org.locationtech.jts.geom.Envelope;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/2/3 10:29 @description： 获取实时矢量瓦片的生成器
 */
public class DefaultRealMvtHelper implements GirRealMvtHelper {

    public VectorTileBuilderConsumer getVectorTileBuilderConsumer(
            Envelope envelope,
            String layerName,
            int outGridSrid,
            TileGlobalConfig tileGlobalConfig) {
        return VectorTileBuilderConsumerByJts.create(
                envelope, layerName, outGridSrid, tileGlobalConfig);
    }

    @Override
    public ParamCheckResult checkTileRequestParams(
            TileRequestParams tileRequestParams, String layerName) {
        return ParamCheckResult.of(true);
    }
}
