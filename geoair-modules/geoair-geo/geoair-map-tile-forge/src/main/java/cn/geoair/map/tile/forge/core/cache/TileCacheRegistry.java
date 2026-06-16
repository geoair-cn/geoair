package cn.geoair.map.tile.forge.core.cache;

import cn.geoair.map.tile.forge.core.cache.impl.TileNoOpCache;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存提供者注册器
 */
@Slf4j
public class TileCacheRegistry {

    /**
     * 存储类型与缓存提供者的映射（key：ITileStorageSupport实现类的Class.getName()）
     */
    private static final Map<String, TileCache> REGISTRY = new ConcurrentHashMap<>();

    /**
     * 默认缓存提供者
     */
    private static TileCache DEFAULT_TILE_CACHE = new TileNoOpCache();

    /**
     * 注册缓存提供者
     */
    public static void register(Class<? extends ITileStorageSupport> storageClass, TileCache tileCache) {
        REGISTRY.put(storageClass.getName(), tileCache);
        log.info("注册缓存提供者：{} -> {}", storageClass.getName(), tileCache.getClass().getSimpleName());
    }

    /**
     * 获取缓存提供者
     */
    public static TileCache getTileCache(ITileStorageSupport storage) {
        String key = storage.getClass().getName();
        return REGISTRY.getOrDefault(key, DEFAULT_TILE_CACHE);
    }

    public static TileCache getDefaultTileCache() {
        return DEFAULT_TILE_CACHE;
    }

    /**
     * 设置默认缓存提供者
     */
    public static void setDefaultTileCache(TileCache tileCache) {
        DEFAULT_TILE_CACHE = tileCache;
    }

    /**
     * 清除注册器
     */
    public static void clear() {
        REGISTRY.clear();
    }
}
