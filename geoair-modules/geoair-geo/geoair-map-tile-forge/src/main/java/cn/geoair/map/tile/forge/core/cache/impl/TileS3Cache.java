package cn.geoair.map.tile.forge.core.cache.impl;

import cn.geoair.map.tile.forge.core.caches.CacheProvider;
import cn.geoair.map.tile.forge.core.caches.S3CacheProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * S3缓存提供者
 */
@Slf4j
public class TileS3Cache extends AbstractTileCache {

    CacheProvider cacheProvider;

    @Override
    public CacheProvider getCacheProvider() {
        if (cacheProvider != null) {
            return cacheProvider;
        } else {
            cacheProvider = new S3CacheProvider("tileCache");
        }
        return cacheProvider;
    }
}
