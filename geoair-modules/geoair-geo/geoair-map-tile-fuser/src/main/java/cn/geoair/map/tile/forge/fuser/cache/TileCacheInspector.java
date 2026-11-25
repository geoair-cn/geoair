package cn.geoair.map.tile.forge.fuser.cache;

import cn.geoair.map.tile.forge.fuser.CustomTileCacheHelper;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;

/** 通过已配置的缓存驱动统计，不额外创建数据库或复制缓存布局规则。 */
public final class TileCacheInspector {
    private TileCacheInspector() {}

    public static LayerCacheStatistics inspect(
            String layerName, CustomTileCacheHelper helper, TileOperationContext context) {
        String originalName = layerName + FuserCacheUtils.ORIGINAL_GRID_SUFFIX;
        return new LayerCacheStatistics(
                helper.getTileCache(layerName).statistics(layerName, context),
                helper.getTileCache(originalName).statistics(originalName, context));
    }
}
