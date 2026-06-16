package com.tc.tools.geowebcache.fuser.provider;

import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import com.tc.tools.geowebcache.fuser.cache.TileCache;
import com.tc.tools.geowebcache.fuser.cache.TileCacheFactory;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import lombok.extern.slf4j.Slf4j;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.*;

/**
 * 带缓存功能的瓦片获取器代理类
 * 通过代理模式为真实的瓦片获取器添加缓存功能
 *
 * @author 张俊
 * @date Created in 2026/6/15
 */
@Slf4j
public class CachedTileGetterProxy implements LayerTileGetter {

    private final LayerTileGetter target;
    private final TileCache tileCache;
    private final String layerCachePreFix;
    private final boolean cacheEnabled;

    /**
     * 构造函数
     *
     * @param target           真实的瓦片获取器
     * @param layerCachePreFix 图层名称（用于缓存key）
     */
    public CachedTileGetterProxy(LayerTileGetter target, String layerCachePreFix) {
        this(target, layerCachePreFix, null);
    }

    /**
     * 构造函数
     *
     * @param target           真实的瓦片获取器
     * @param layerCachePreFix 图层名称（用于缓存key）
     * @param tileCache        缓存实现，如果为null则使用默认缓存
     */
    public CachedTileGetterProxy(LayerTileGetter target, String layerCachePreFix, TileCache tileCache) {
        this.target = target;
        this.layerCachePreFix = layerCachePreFix;
        this.tileCache = tileCache != null ? tileCache : TileCacheFactory.getDefaultCache();
        this.cacheEnabled = this.tileCache.isEnabled();
        log.debug("初始化缓存代理 - layerName: {}, cacheEnabled: {}", layerCachePreFix, cacheEnabled);
    }

    @Override
    public Resource getTileResource(int z, int x, int y) {
        // 先尝试从缓存读取
        if (cacheEnabled) {
            try {
                byte[] cachedData = tileCache.get(layerCachePreFix, z, x, y);
                if (cachedData != null && cachedData.length > 0) {
                    log.debug("从缓存获取瓦片成功: {} - ({},{},{})", layerCachePreFix, z, x, y);
                    return new ByteArrayResource(cachedData);
                }
            } catch (Exception e) {
                log.error("从缓存读取瓦片失败: {} - ({},{},{})", layerCachePreFix, z, x, y, e);
            }
        }

        // 缓存未命中，调用真实获取器
        Resource resource = target.getTileResource(z, x, y);

        // 保存到缓存
        if (resource != null && cacheEnabled) {
            try {
                byte[] data = resource.getByteData();
                if (data != null && data.length > 0) {
                    tileCache.put(layerCachePreFix, z, x, y, data);
                    log.debug("瓦片已保存到缓存: {} - ({},{},{})", layerCachePreFix, z, x, y);
                }
            } catch (Exception e) {
                log.error("保存瓦片到缓存失败: {} - ({},{},{})", layerCachePreFix, z, x, y, e);
            }
        }

        return resource;
    }

    @Override
    public ImageMime getSrcFormat() {
        return target.getSrcFormat();
    }

    @Override
    public GridSubset getSrcGridSubset() {
        return target.getSrcGridSubset();
    }

    /**
     * 清除当前图层的所有缓存
     */
    public boolean clearCache() {
        if (cacheEnabled) {
            return tileCache.deleteLayerCache(layerCachePreFix);
        }
        return false;
    }

    /**
     * 清除指定瓦片的缓存
     */
    public boolean clearTileCache(int z, int x, int y) {
        if (cacheEnabled) {
            return tileCache.delete(layerCachePreFix, z, x, y);
        }
        return false;
    }

    /**
     * 获取原始目标对象
     */
    public LayerTileGetter getTarget() {
        return target;
    }

    /**
     * 检查缓存是否启用
     */
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
}
