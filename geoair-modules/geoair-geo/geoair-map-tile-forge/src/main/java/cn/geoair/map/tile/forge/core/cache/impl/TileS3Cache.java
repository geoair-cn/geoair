package cn.geoair.map.tile.forge.core.cache.impl;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.caches.CacheProvider;
import cn.geoair.map.tile.forge.core.caches.S3CacheProvider;

/** S3缓存提供者 */
public class TileS3Cache extends AbstractTileCache {
    public static GiLogger log = GirLoggerFactory.getLogger();
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
