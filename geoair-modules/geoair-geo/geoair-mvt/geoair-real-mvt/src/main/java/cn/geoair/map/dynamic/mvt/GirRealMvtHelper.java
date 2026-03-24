package cn.geoair.map.dynamic.mvt;

import org.locationtech.jts.geom.Envelope;

import cn.geoair.base.bean.GirBeanHelper;
import cn.geoair.map.dynamic.mvt.consumer.VectorTileBuilderConsumer;
import cn.geoair.map.dynamic.mvt.dto.TileGlobalConfig;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/2/3 10:12 @description： 与上游的钩子
 */
public interface GirRealMvtHelper {

	static GirRealMvtHelper getInstance() {
		try {
			return GirBeanHelper.getProvider().getBean(GirRealMvtHelper.class);
		}
		catch (Exception e) {
			return new DefaultRealMvtHelper();
		}

	}

	VectorTileBuilderConsumer getVectorTileBuilderConsumer(Envelope envelope, String layerName, int outGridSrid,
			TileGlobalConfig tileGlobalConfig);

}
