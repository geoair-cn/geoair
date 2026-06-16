package cn.geoair.map.tile.forge.core.cache.impl;

import cn.geoair.map.tile.forge.core.caches.CacheProvider;
import cn.geoair.map.tile.forge.core.caches.NoOpCacheProvider;

/**
 * 空缓存提供者
 */
public class TileNoOpCache extends AbstractTileCache {

    CacheProvider cacheProvider;

    @Override
    public CacheProvider getCacheProvider() {
        if (cacheProvider != null) {
            return cacheProvider;
        } else {
            cacheProvider = new NoOpCacheProvider();
        }
        return cacheProvider;
    }


}
