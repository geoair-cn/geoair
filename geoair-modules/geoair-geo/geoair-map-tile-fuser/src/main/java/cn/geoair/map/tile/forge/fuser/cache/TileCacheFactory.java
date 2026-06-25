package cn.geoair.map.tile.forge.fuser.cache;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;

/**
 * 缓存工厂类
 *
 * @author 张俊
 * @date Created in 2023/12/4
 * @description 用于创建和管理缓存实例
 */

public class TileCacheFactory {
    private static GiLogger log = GirLoggerFactory.getLogger( );
    private static volatile TileCache defaultCache;


    public static TileCache getDefaultCache() {
        if (defaultCache == null) {
            synchronized (TileCacheFactory.class) {
                if (defaultCache == null) {
                    defaultCache = new FileTileCache();
                    log.info("初始化默认文件缓存");
                }
            }
        }
        return defaultCache;
    }


    public static TileCache getFileCache(String cacheRoot, long expireTime, boolean enabled) {
        return new FileTileCache(cacheRoot, expireTime, enabled);
    }


    public static void setDefaultCache(TileCache cache) {
        if (cache != null) {
            defaultCache = cache;
            log.info("切换默认缓存实现: {}", cache.getClass().getSimpleName());
        }
    }
}
