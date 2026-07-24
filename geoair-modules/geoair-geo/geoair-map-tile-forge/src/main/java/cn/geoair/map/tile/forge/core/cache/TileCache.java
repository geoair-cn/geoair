package cn.geoair.map.tile.forge.core.cache;

import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.bygwc.config.CacheInfo;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;

/** 瓦片缓存提供者接口 */
public interface TileCache {

    /**
     * 构建瓦片缓存键
     *
     * @param layerName 图层名称
     * @param z 缩放级别
     * @param y Y坐标
     * @param x X坐标
     * @return 格式化的缓存键字符串，格式为 "tile_{layerName}/{z}/{x}/{y}"
     */
    default String buildTileCacheKey(String layerName, String z, String y, String x) {
        StringBuilder cacheKey = new StringBuilder(layerName).append("/");
        cacheKey.append(z).append("/");
        if (x != null) {
            cacheKey.append(x).append("/");
        }
        if (y != null) {
            cacheKey.append(y);
        }

        return cacheKey.toString();
    }

    /** 获取瓦片缓存（包含不存在状态） */
    TileRequest getTile(String cacheKey, String fileFormat);

    /** 存储瓦片缓存（支持存储不存在状态） */
    void putTile(String cacheKey, TileRequest tileRequest, String fileFormat);

    /** 获取Capabilities缓存 */
    String getCapabilities(String cacheKey);

    /** 存储Capabilities缓存 */
    void putCapabilities(String cacheKey, String capabilities);

    /** 获取BoundingBox缓存 */
    BoundingBox getBoundingBox(String cacheKey);

    /** 存储BoundingBox缓存 */
    void putBoundingBox(String cacheKey, BoundingBox boundingBox);

    /** 获取CacheInfo缓存 */
    CacheInfo getCacheInfo(String cacheKey);

    /** 存储CacheInfo缓存 */
    void putCacheInfo(String cacheKey, CacheInfo cacheInfo);

    /** 清理指定图层的瓦片缓存 */
    void clearTileCache(String layerName);
}
