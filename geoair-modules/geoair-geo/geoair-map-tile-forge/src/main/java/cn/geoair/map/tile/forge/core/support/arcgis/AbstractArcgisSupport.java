package cn.geoair.map.tile.forge.core.support.arcgis;

import cn.geoair.map.tile.forge.core.bygwc.config.CacheInfo;
import cn.geoair.map.tile.forge.core.bygwc.config.CacheInfoPersister;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.layer.ArcGISCacheLayer;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/2 10:59
 * @description：
 */
public abstract class AbstractArcgisSupport implements ArcgisConfigXmlGetter {
    /**
     * 获取瓦片的缓存信息
     *
     * @param layerConfigContext 瓦片的配置信息
     * @return 瓦片的缓存信息
     * @throws Exception 获取过程中可能出现的异常，如网络错误、文件读取错误等
     */
    public CacheInfo getCacheInfo(GirLayerConfigContext layerConfigContext) throws Exception {
        CacheInfoPersister instance = CacheInfoPersister.getInstance();
        String configXml = getConfigXml(layerConfigContext);
        return instance.load(configXml);
    }


    public ArcGISCacheLayer getGwcArcGISCacheLayer(GirLayerConfigContext layerConfigContext) throws Exception {
        CacheInfo cacheInfo = getCacheInfo(layerConfigContext);
        BoundingBox boundingBox = getBoundingBox(layerConfigContext);
        String layerName = layerConfigContext.getLayerName();
        return new ArcGISCacheLayer(layerName, cacheInfo, boundingBox);
    }

    /**
     * 获取瓦片的边界信息
     *
     * @param layerConfigContext 瓦片的配置信息
     * @return 瓦片的边界信息
     * @throws Exception 获取过程中可能出现的异常，如网络错误、文件读取错误等
     */
    public BoundingBox getBoundingBox(GirLayerConfigContext layerConfigContext) throws Exception {
        String configCdi = getConfigCdi(layerConfigContext);
        CacheInfoPersister instance = CacheInfoPersister.getInstance();
        return instance.parseLayerBounds(configCdi);
    }
}
