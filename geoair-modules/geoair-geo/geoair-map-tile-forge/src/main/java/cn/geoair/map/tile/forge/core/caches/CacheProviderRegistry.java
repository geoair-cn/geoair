package cn.geoair.map.tile.forge.core.caches;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
 

/**
 * 缓存提供者注册器
 */
 
public class CacheProviderRegistry {
    public static GiLogger log = GirLoggerFactory.getLogger();

    /**
     * 默认缓存提供者
     */
    private static CacheProvider DEFAULT_PROVIDER = new NoOpCacheProvider();


    public static CacheProvider getDefaultCacheProvider() {
        return DEFAULT_PROVIDER;
    }

    /**
     * 设置默认缓存提供者
     */
    public static void setDefaultCacheProvider(CacheProvider cacheProvider) {
        DEFAULT_PROVIDER = cacheProvider;
    }


}
