package cn.geoair.map.dynamic.mvt;

import cn.geoair.map.dynamic.mvt.dto.TileGlobalConfig;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.mvt.consumer.VectorTileBuilderConsumer;
import cn.geoair.map.dynamic.mvt.consumer.VectorTileBuilderConsumerByJts;
import org.locationtech.jts.geom.Envelope;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/2/3 10:29
 * @description： TODO
 */
public class DefaultRealMvtHelper implements GirRealMvtHelper {

    public VectorTileBuilderConsumer getVectorTileBuilderConsumer(Envelope envelope,
                                                                  String layerName,
                                                                  int outGridSrid,
                                                                  TileGlobalConfig tileGlobalConfig) {
        return VectorTileBuilderConsumerByJts.create(envelope, layerName, outGridSrid, tileGlobalConfig);
    }
}
