package cn.geoair.map.tile.forge.core.support;

import cn.geoair.map.tile.forge.core.cache.TileCache;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.vo.TileRequest;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;

public interface ITileStorageSupport {
    /**
     * 获取瓦片数据
     */
    TileRequest getTileData(GirLayerConfigContext layerConfigContext, String z, String x, String y) throws Exception;

    /**
     * 获取瓦片的 capabilities 文件
     */
    String getCapabilities(GirLayerConfigContext layerConfigContext) throws Exception;

    /**
     * 预缓存指定层级范围的瓦片
     *
     * @param layerConfigContext 图层配置
     * @param tileCache
     * @return 异步任务结果（可用于监听进度）
     */
    default void preCacheTiles(GirLayerConfigContext layerConfigContext, TileCache tileCache) {
        preCacheTiles(layerConfigContext, tileCache, (allCount, currentCount) -> {
        });
    }

    /**
     * 预缓存指定层级范围的瓦片
     *
     * @param layerConfigContext 图层配置
     * @return 异步任务结果（可用于监听进度）
     */
    default void preCacheTiles(GirLayerConfigContext layerConfigContext, ProgressConsumer progressConsumer) {
        preCacheTiles(layerConfigContext, null, progressConsumer);
    }

    /**
     * 预缓存指定层级范围的瓦片
     *
     * @param layerConfigContext 图层配置
     * @param tileCache
     * @return 异步任务结果（可用于监听进度）
     */
    void preCacheTiles(GirLayerConfigContext layerConfigContext, TileCache tileCache, ProgressConsumer progressConsumer);


}
