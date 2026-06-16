package cn.geoair.map.tile.forge.core.caches;

import lombok.extern.slf4j.Slf4j;

/**
 * 缓存提供者注册器
 */
@Slf4j
public class CacheProviderRegistry {


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
