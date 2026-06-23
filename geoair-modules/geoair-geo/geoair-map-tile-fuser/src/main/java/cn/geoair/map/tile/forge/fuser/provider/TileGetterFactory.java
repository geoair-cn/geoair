package cn.geoair.map.tile.forge.fuser.provider;

import cn.geoair.map.tile.forge.fuser.CustomTileGetterHelper;
import cn.geoair.map.tile.forge.fuser.GirFuser;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.constant.Constant;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.enums.PxyType;
import cn.geoair.map.tile.forge.fuser.provider.impl.MBTilesTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.impl.google.GoogleLocalFileTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.impl.google.GoogleWebTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.impl.grid4490.Grid4490LocalFileTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.impl.grid4490.Grid4490WebTileGetter;

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
    public static LayerTileGetter create(PxyLayerInfo pxyLayerInfo) {
        return create(pxyLayerInfo, null);
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
     * @param pxyLayerInfo    配置信息
     * @param tileCache 自定义缓存（可选）
     */
    public static LayerTileGetter create(PxyLayerInfo pxyLayerInfo, TileCache tileCache) {

        LayerTileGetter realGetter = createRealGetter(pxyLayerInfo);
        boolean enableCache = "true".equalsIgnoreCase(pxyLayerInfo.getEnableCache())
                || "1".equals(pxyLayerInfo.getEnableCache());
        if (!enableCache) {
            return realGetter;
        }
        // 生成图层标识
        String layerName = pxyLayerInfo.getLayerName();
        String layerCachePreFix = layerName + Constant._original_grid_name_suffix;
        return new CachedTileGetterProxy(realGetter, layerCachePreFix, tileCache);
    }

    public static LayerTileGetter create(String layerName, TileCache tileCache) {
        PxyLayerInfo pxyLayerInfo = GirFuser.getPxyLayerInfo(layerName);
        return create(pxyLayerInfo, tileCache);
    }

    /**
     * 创建真实的获取器（不带缓存）
     */
    private static LayerTileGetter createRealGetter(PxyLayerInfo pxyLayerInfo) {
        String type = pxyLayerInfo.getSrcType();
        PxyType pxyType = PxyType.fromCode(type);
        Integer gridSrid = pxyLayerInfo.getGridSrid();
        if (pxyType.isCustom()) {
            CustomTileGetterHelper instance = CustomTileGetterHelper.getInstance();
            return instance.getTileGetterByPxyLayerInfo(pxyLayerInfo);
        }
        if (pxyType.isMbtiles()) {
            return new MBTilesTileGetter(pxyLayerInfo);
        }
        if (pxyType.isLocal()) {
            if (gridSrid.equals(3857)) {
                return new GoogleLocalFileTileGetter(pxyLayerInfo);
            }
            return new Grid4490LocalFileTileGetter(pxyLayerInfo);
        } else {
            if (gridSrid.equals(3857)) {
                return new GoogleWebTileGetter(pxyLayerInfo);
            }
            return new Grid4490WebTileGetter(pxyLayerInfo);
        }
    }
}
