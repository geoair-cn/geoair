package cn.geoair.map.dynamic.mvt;

import cn.geoair.map.dynamic.mvt.consumer.VectorTileBuilderConsumer;
import cn.geoair.map.dynamic.mvt.dto.ParamCheckResult;
import cn.geoair.map.dynamic.mvt.dto.TileGlobalConfig;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.tools.GirService;
import cn.geoair.web.GirWeb;
import javax.servlet.http.HttpServletRequest;
import org.locationtech.jts.geom.Envelope;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/2/3 10:12 @description： 与上游的钩子
 */
public interface GirRealMvtHelper {

    /**
     * 获取一个帮助类实例
     *
     * @return
     */
    static GirRealMvtHelper getInstance() {
        try {
            return GirService.getPxyBeanC(GirRealMvtHelper.class);
        } catch (Exception e) {
            return new DefaultRealMvtHelper();
        }
    }

    /**
     * 获取一个矢量瓦片构建器的消费者
     *
     * @param envelope
     * @param layerName
     * @param outGridSrid
     * @param tileGlobalConfig
     * @return
     */
    VectorTileBuilderConsumer getVectorTileBuilderConsumer(
            Envelope envelope,
            String layerName,
            int outGridSrid,
            TileGlobalConfig tileGlobalConfig);

    /**
     * 用于检查tileRequestParams 的参数合法性
     *
     * @param tileRequestParams
     * @param layerName
     * @return
     */
    default ParamCheckResult checkTileRequestParams(
            TileRequestParams tileRequestParams, String layerName) {
        return ParamCheckResult.of(true);
    }

    default TileRequestParams getTileRequestParams(String layerName) {
        HttpServletRequest request = GirWeb.getRequest();
        String paramTile = request.getParameter("paramTile");
        return TileRequestParams.fromBase32(paramTile);
    }
}
