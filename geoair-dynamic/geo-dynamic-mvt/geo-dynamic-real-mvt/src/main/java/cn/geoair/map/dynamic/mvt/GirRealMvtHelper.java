package cn.geoair.map.dynamic.mvt;


import cn.geoair.gtc.base.bean.GirBeanHelper;
import cn.geoair.map.dynamic.mvt.dto.TileGlobalConfig;
import cn.geoair.map.dynamic.mvt.consumer.VectorTileBuilderConsumer;
import org.locationtech.jts.geom.Envelope;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/2/3 10:12
 * @description： 与上游的钩子
 */
public interface GirRealMvtHelper {

    static GirRealMvtHelper getInstance() {
        try {
            return GirBeanHelper.getProvider().getBean(GirRealMvtHelper.class);
        } catch (Exception e) {
            return new DefaultRealMvtHelper();
        }

    }

    VectorTileBuilderConsumer getVectorTileBuilderConsumer(Envelope envelope, String layerName, int outGridSrid, TileGlobalConfig tileGlobalConfig);


}
