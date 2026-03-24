package cn.geoair.map.dynamic.mvt;

import org.locationtech.jts.geom.Envelope;

import cn.geoair.map.dynamic.mvt.consumer.VectorTileBuilderConsumer;
import cn.geoair.map.dynamic.mvt.consumer.VectorTileBuilderConsumerByJts;
import cn.geoair.map.dynamic.mvt.dto.TileGlobalConfig;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/2/3 10:29 @description： TODO
 */
public class DefaultRealMvtHelper implements GirRealMvtHelper {

	public VectorTileBuilderConsumer getVectorTileBuilderConsumer(Envelope envelope, String layerName, int outGridSrid,
			TileGlobalConfig tileGlobalConfig) {
		return VectorTileBuilderConsumerByJts.create(envelope, layerName, outGridSrid, tileGlobalConfig);
	}

}
