package cn.geoair.map.tile.forge.core.cache.impl;

import cn.geoair.map.tile.forge.core.bygwc.config.CacheInfo;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.cache.TileCache;
import cn.geoair.map.tile.forge.core.caches.CacheProvider;
import cn.geoair.map.tile.forge.core.vo.TileRequest;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 瓦片缓存提供者抽象基类
 */
public abstract class AbstractTileCache implements TileCache {

    public abstract CacheProvider getCacheProvider();

    public TileRequest getTile(String cacheKey, String fileFormat) {
        String byteKey = getByteKey(cacheKey, fileFormat);
        byte[] bytes = getCacheProvider().getByte(byteKey);
        if (bytes != null && bytes.length > 0) {
            TileRequest requestMeta = getCacheProvider().get(cacheKey + "_meta.json", TileRequest.class);
            requestMeta.setBytes(bytes);
            return requestMeta;
        }
        return null;
    }


    public void putTile(String cacheKey, TileRequest tileRequest, String fileFormat) {
        String byteKey = getByteKey(cacheKey, fileFormat);
        getCacheProvider().put(byteKey, tileRequest.getBytes(), -1);
        TileRequest tileRequestCacheMeta = new TileRequest();
        BeanUtil.copyProperties(tileRequest, tileRequestCacheMeta, "bytes");
        getCacheProvider().put(cacheKey + "_meta.json", tileRequestCacheMeta, -1);
    }

    private static String getByteKey(String cacheKey, String fileFormat) {
        String temp = cacheKey;
        while (StrUtil.endWith(temp, "/")) {
            temp = StrUtil.replaceLast(temp, "/", "");
        }
        String suffix = FileNameUtil.getSuffix(temp);
        String byteKey = null;
        if (StrUtil.isEmpty(suffix)) {
            if (StrUtil.isEmpty(fileFormat)) {
                fileFormat = "png";
            }
            byteKey = temp + "." + fileFormat;
        } else {
            byteKey = cacheKey;
        }
        return byteKey;
    }

    /**
     * 获取Capabilities缓存
     */
    public String getCapabilities(String cacheKey) {
        return getCacheProvider().get(cacheKey, String.class);
    }

    /**
     * 存储Capabilities缓存
     */
    public void putCapabilities(String cacheKey, String capabilities) {
        getCacheProvider().put(cacheKey, capabilities);
    }

    /**
     * 获取BoundingBox缓存
     */
    public BoundingBox getBoundingBox(String cacheKey) {
        return getCacheProvider().get(cacheKey, BoundingBox.class);
    }

    /**
     * 存储BoundingBox缓存
     */
    public void putBoundingBox(String cacheKey, BoundingBox boundingBox) {
        getCacheProvider().put(cacheKey, boundingBox);
    }

    /**
     * 获取CacheInfo缓存
     */
    public CacheInfo getCacheInfo(String cacheKey) {
        return getCacheProvider().get(cacheKey, CacheInfo.class);
    }

    /**
     * 存储CacheInfo缓存
     */
    public void putCacheInfo(String cacheKey, CacheInfo cacheInfo) {
        getCacheProvider().put(cacheKey, cacheInfo);
    }

    @Override
    public void clearTileCache(String layerName) {

    }
}
