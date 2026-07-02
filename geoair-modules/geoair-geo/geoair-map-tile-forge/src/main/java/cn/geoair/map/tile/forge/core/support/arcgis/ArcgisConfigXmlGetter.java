package cn.geoair.map.tile.forge.core.support.arcgis;

import cn.geoair.map.tile.forge.core.bygwc.config.CacheInfo;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;

/**
 * @author ：zhangjun
 * @date ：Created in 2026/7/2 10:53
 * @description： arcgis的config的XML的获取器
 */
public interface ArcgisConfigXmlGetter {

    /**
     * 获取描述瓦片的格式的xml
     *
     * @param layerConfigContext 瓦片的配置信息
     * @return 描述瓦片的格式的xml
     * @throws Exception 获取过程中可能出现的异常，如网络错误、文件读取错误等
     */
    String getConfigXml(GirLayerConfigContext layerConfigContext) throws Exception;

    /**
     * 获取瓦片的 capabilities 文件
     */
    String getCapabilities(GirLayerConfigContext layerConfigContext) throws Exception;
    /**
     * 获取描述边界的文件描述
     *
     * @param layerConfigContext 瓦片的配置信息
     * @return 描述边界的文件描述
     * @throws Exception 获取过程中可能出现的异常，如网络错误、文件读取错误等
     */
    String getConfigCdi(GirLayerConfigContext layerConfigContext) throws Exception;

    /**
     * 获取瓦片的缓存信息
     */
    CacheInfo getCacheInfo(GirLayerConfigContext layerConfigContext) throws Exception;
}
