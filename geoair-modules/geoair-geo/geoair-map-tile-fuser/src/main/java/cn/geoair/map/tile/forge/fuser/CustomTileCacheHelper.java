package cn.geoair.map.tile.forge.fuser;

import cn.geoair.map.dynamic.tools.GirService;
import cn.geoair.map.tile.forge.fuser.cache.DefaultCustomTileCacheHelper;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/22 13:15
 * @description： 客户端自定义的缓存器获取
 */
public interface CustomTileCacheHelper {

    static CustomTileCacheHelper getInstance() {
        try {
            return GirService.getPxyBeanC(CustomTileCacheHelper.class);
        } catch (Exception e) {
            return DefaultCustomTileCacheHelper.getInstance();
        }
    }

    TileCache getTileCache(String layerName);
}
