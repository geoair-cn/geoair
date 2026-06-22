package cn.geoair.map.tile.forge.fuser.provider;

import cn.geoair.map.tile.forge.fuser.CustomTileGetterHelper;
import cn.geoair.map.tile.forge.fuser.GirFuser;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.enums.PxyType;
import cn.geoair.map.tile.forge.fuser.provider.google.GoogleLocalFileTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.google.GoogleWebTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.grid4490.Grid4490LocalFileTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.grid4490.Grid4490WebTileGetter;

/**
 * 瓦片获取器工厂类
 *
 * @author 张俊
 * @date Created in 2026/6/15
 */
public class TileGetterFactory {

    /**
     * 根据配置创建瓦片获取器（带缓存）
     */
    public static LayerTileGetter create(PxyLayerInfo config) {
        return create(config, null);
    }

    /**
     * 根据配置创建瓦片获取器（带缓存）
     */
    public static LayerTileGetter create(String layerName) {
        PxyLayerInfo pxyLayerInfo = GirFuser.getPxyLayerInfo(layerName);
        return create(pxyLayerInfo, null);
    }

    /**
     * 根据配置创建瓦片获取器（带缓存）
     *
     * @param config    配置信息
     * @param tileCache 自定义缓存（可选）
     */
    public static LayerTileGetter create(PxyLayerInfo config, TileCache tileCache) {

        LayerTileGetter realGetter = createRealGetter(config);
        boolean enableCache = "true".equalsIgnoreCase(config.getEnableCache())
                || "1".equals(config.getEnableCache());
        if (!enableCache) {
            return realGetter;
        }
        // 生成图层标识
        String layerName = config.getLayerName();
        String layerCachePreFix = layerName + "_original_grid";
        return new CachedTileGetterProxy(realGetter, layerCachePreFix, tileCache);
    }

    public static LayerTileGetter create(String layerName, TileCache tileCache) {
        PxyLayerInfo pxyLayerInfo = GirFuser.getPxyLayerInfo(layerName);
        return create(pxyLayerInfo, tileCache);
    }

    /**
     * 创建真实的获取器（不带缓存）
     */
    private static LayerTileGetter createRealGetter(PxyLayerInfo config) {
        String type = config.getSrcType();
        PxyType pxyType = PxyType.fromMode(type);
        Integer gridSrid = config.getGridSrid();
        if (pxyType.isCustom()) {
            CustomTileGetterHelper instance = CustomTileGetterHelper.getInstance();
            return instance.getTileGetterByPxyLayerInfo(config);
        }
        if (pxyType.isLocal()) {
            if (gridSrid.equals(3857)) {
                return new GoogleLocalFileTileGetter(config);
            }
            return new Grid4490LocalFileTileGetter(config);
        } else {
            if (gridSrid.equals(3857)) {
                return new GoogleWebTileGetter(config);
            }
            return new Grid4490WebTileGetter(config);
        }
    }
}
