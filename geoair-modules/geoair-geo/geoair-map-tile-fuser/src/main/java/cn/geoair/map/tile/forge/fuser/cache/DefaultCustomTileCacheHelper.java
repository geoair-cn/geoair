package cn.geoair.map.tile.forge.fuser.cache;

import cn.geoair.map.tile.forge.fuser.CustomTileCacheHelper;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/22 13:17
 * @description： 默认的自定义缓存的实现
 */
public class DefaultCustomTileCacheHelper implements CustomTileCacheHelper {
    static DefaultCustomTileCacheHelper INSTANCE = new DefaultCustomTileCacheHelper();

    public static DefaultCustomTileCacheHelper getInstance() {
        return INSTANCE;
    }

    @Override
    public TileCache getTileCache(String layerName) {
        return TileCacheFactory.getDefaultCache();
    }
}
