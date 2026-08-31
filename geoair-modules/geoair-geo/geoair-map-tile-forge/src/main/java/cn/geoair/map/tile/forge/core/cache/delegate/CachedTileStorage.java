package cn.geoair.map.tile.forge.core.cache.delegate;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.cache.TileCache;
import cn.geoair.map.tile.forge.core.cache.TileCacheRegistry;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 带缓存的瓦片存储委派包装类 */
public class CachedTileStorage implements ITileStorageSupport {
    public static GiLogger log = GirLoggerFactory.getLogger();

    private final ITileStorageSupport delegate;
    private final TileCache tileCache;

    private static final int CACHE_QUEUE_CAPACITY = 1024;

    private static final ThreadPoolExecutor executorService =
            new ThreadPoolExecutor(
                    Runtime.getRuntime().availableProcessors() * 2,
                    Runtime.getRuntime().availableProcessors() * 2,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(CACHE_QUEUE_CAPACITY),
                    r -> {
                        Thread t = new Thread(r);
                        t.setName("tile-cache-thread-" + t.getId());
                        t.setDaemon(true);
                        return t;
                    },
                    new ThreadPoolExecutor.AbortPolicy());

    public CachedTileStorage(ITileStorageSupport delegate) {
        this.delegate = Objects.requireNonNull(delegate, "瓦片存储实现不能为空");
        this.tileCache = TileCacheRegistry.getTileCache(delegate);
    }

    public static CachedTileStorage getInstance(ITileStorageSupport delegate) {
        return new CachedTileStorage(delegate);
    }

    @Override
    public TileRequest getTileData(
            GirLayerConfigContext layerConfigContext, String z, String x, String y)
            throws Exception {
        Objects.requireNonNull(layerConfigContext, "图层配置不能为空");
        String layerName = layerConfigContext.getDataId();
        Objects.requireNonNull(layerName, "图层名不能为空");
        String fileFormat = layerConfigContext.getFormat();
        // 1. 构建瓦片唯一标识。
        String tileKey = tileCache.buildTileCacheKey(layerName, z, y, x);

        // 2. 查缓存。不能用 BloomFilter 记录“不存在瓦片”，因为假阳性会误伤真实瓦片。
        TileRequest cachedTile = tileCache.getTile(tileKey, fileFormat);
        if (cachedTile != null) {
            // 补全必要字段（避免字段缺失导致业务异常）
            fillTileRequestFields(cachedTile, layerConfigContext);
            cachedTile.setLastModified(System.currentTimeMillis());
            log.trace("缓存命中瓦片：{}", tileKey);
            return cachedTile;
        }

        // 3. 查原始存储层（ delegate ）
        TileRequest tileData = delegate.getTileData(layerConfigContext, z, x, y);

        // 4. 处理瓦片数据：分「存在」和「不存在」两种情况
        if (tileData != null && tileData.isExists() && tileData.getBytes() != null) {
            // 4.1 瓦片存在：异步写入缓存（避免阻塞主线程）
            asyncPutTileToCache(layerName, tileKey, tileData, fileFormat);
        } else {
            // 4.2 保持 TileRequest 合约，避免上层转换响应时产生空指针异常。
            return TileRequest.emptyByContext(layerConfigContext);
        }

        return tileData;
    }

    /** 补全TileRequest的必要字段（避免缓存中字段缺失） */
    private void fillTileRequestFields(TileRequest tile, GirLayerConfigContext layerConfigContext) {
        if (tile.getLayerName() == null) {
            tile.setLayerName(layerConfigContext.getLayerName());
        }
        if (tile.getMapTileType() == null) {
            tile.setMapTileType(layerConfigContext.getMapTileType());
        }
        if (tile.getStorageType() == null) {
            tile.setStorageType(layerConfigContext.getStorageType());
        }
    }

    /** 异步将瓦片写入缓存（避免阻塞查询主线程） */
    private void asyncPutTileToCache(
            String layerName, String tileKey, TileRequest originalTile, String fileFormat) {
        try {
            // 拷贝字节数组（避免原始流被关闭导致数据丢失）
            byte[] tileBytes = originalTile.getBytes().clone();

            // 构建缓存用的TileRequest（仅保留必要字段）
            TileRequest cacheTile = new TileRequest();
            cacheTile.setLayerName(layerName);
            cacheTile.setBytes(tileBytes);
            cacheTile.setExists(true);
            cacheTile.setSize(tileBytes.length);
            cacheTile.setLastModified(System.currentTimeMillis());
            cacheTile.setMimeType(originalTile.getMimeType());
            cacheTile.setMapTileType(originalTile.getMapTileType());
            cacheTile.setStorageType(originalTile.getStorageType());

            try {
                executorService.submit(
                        () -> {
                            try {
                                tileCache.putTile(tileKey, cacheTile, fileFormat);
                                log.trace("异步缓存瓦片成功：{}", tileKey);
                            } catch (Exception e) {
                                log.error("异步缓存瓦片失败：{}", tileKey, e);
                            }
                        });
            } catch (RejectedExecutionException e) {
                log.warn("缓存队列已满，跳过当前瓦片缓存: {}", tileKey);
            }
        } catch (Exception e) {
            log.error("构建缓存瓦片数据失败：{}", tileKey, e);
            // 异常时不影响主线程返回原始数据
        }
    }

    @Override
    public void preCacheTiles(
            GirLayerConfigContext layerConfigContext,
            TileCache tileCache,
            ProgressConsumer progressConsumer) {
        delegate.preCacheTiles(layerConfigContext, tileCache, progressConsumer);
    }
}
