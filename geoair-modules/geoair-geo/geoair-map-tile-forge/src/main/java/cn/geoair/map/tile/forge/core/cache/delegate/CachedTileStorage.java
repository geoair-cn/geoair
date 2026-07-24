package cn.geoair.map.tile.forge.core.cache.delegate;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.cache.TileCache;
import cn.geoair.map.tile.forge.core.cache.TileCacheRegistry;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.hutool.bloomfilter.BloomFilter;
import cn.hutool.bloomfilter.BloomFilterUtil;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** 带缓存的瓦片存储委派包装类 */
public class CachedTileStorage implements ITileStorageSupport {
    public static GiLogger log = GirLoggerFactory.getLogger();

    private final ITileStorageSupport delegate;
    private final TileCache tileCache;

    private static final ExecutorService executorService =
            Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors() * 2,
                    r -> {
                        Thread t = new Thread(r);
                        t.setName("tile-cache-thread-" + t.getId());
                        t.setDaemon(true); // 守护线程，避免应用退出时阻塞
                        return t;
                    });

    private static final int EXPECTED_TILE_COUNT = 100000;

    private static final int MAX_LAYER_COUNT = 100;

    private final Map<String, BloomFilter> layerBloomFilters = new ConcurrentHashMap<>();

    public CachedTileStorage(ITileStorageSupport delegate) {
        this.delegate = Objects.requireNonNull(delegate, "瓦片存储实现不能为空");
        this.tileCache = TileCacheRegistry.getTileCache(delegate);
        // 初始化时注册JVM关闭钩子，释放资源
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public static CachedTileStorage getInstance(ITileStorageSupport delegate) {
        return new CachedTileStorage(delegate);
    }

    /** 获取指定图层的布隆过滤器（懒加载，首次使用时创建） */
    private BloomFilter getBloomFilterByLayer(String layerName) {
        // 双重检查锁定：避免重复创建过滤器
        if (!layerBloomFilters.containsKey(layerName)) {
            synchronized (layerBloomFilters) {
                if (!layerBloomFilters.containsKey(layerName)) {
                    // 校验图层数上限，避免内存溢出
                    if (layerBloomFilters.size() >= MAX_LAYER_COUNT) {
                        log.warn(
                                "已达最大图层数({})，新图层[{}]的布隆过滤器创建失败，可能导致缓存穿透",
                                MAX_LAYER_COUNT,
                                layerName);
                        return null;
                    }
                    // 创建布隆过滤器：LongMap支持大容量，MurmurHash3哈希算法（默认）
                    BloomFilter filter = BloomFilterUtil.createBitMap(EXPECTED_TILE_COUNT);
                    layerBloomFilters.put(layerName, filter);
                }
            }
        }
        return layerBloomFilters.get(layerName);
    }

    @Override
    public TileRequest getTileData(
            GirLayerConfigContext layerConfigContext, String z, String x, String y)
            throws Exception {
        Objects.requireNonNull(layerConfigContext, "图层配置不能为空");
        String layerName = layerConfigContext.getDataId();
        Objects.requireNonNull(layerName, "图层名不能为空");
        String fileFormat = layerConfigContext.getFormat();
        // 1. 构建瓦片唯一标识（缓存Key和布隆过滤器Key共用）
        String tileKey = tileCache.buildTileCacheKey(layerName, z, y, x);

        // 2. 先查布隆过滤器：过滤「确定不存在」的瓦片，直接返回null（避免穿透到存储层）
        BloomFilter bloomFilter = getBloomFilterByLayer(layerName);
        if (bloomFilter != null && bloomFilter.contains(tileKey)) {
            log.trace("布隆过滤器命中不存在瓦片：layer={}, z={}, x={}, y={}", layerName, z, x, y);
            return null; // 直接返回，无需查询缓存和存储层
        }

        // 3. 查本地缓存：缓存命中则直接返回
        TileRequest cachedTile = tileCache.getTile(tileKey, fileFormat);
        if (cachedTile != null) {
            // 补全必要字段（避免字段缺失导致业务异常）
            fillTileRequestFields(cachedTile, layerConfigContext);
            cachedTile.setLastModified(System.currentTimeMillis());
            log.trace("缓存命中瓦片：{}", tileKey);
            return cachedTile;
        }

        // 4. 查原始存储层（ delegate ）
        TileRequest tileData = delegate.getTileData(layerConfigContext, z, x, y);

        // 5. 处理瓦片数据：分「存在」和「不存在」两种情况
        if (tileData != null && tileData.isExists() && tileData.getBytes() != null) {
            // 5.1 瓦片存在：异步写入缓存（避免阻塞主线程）
            asyncPutTileToCache(layerName, tileKey, tileData, fileFormat);
        } else {
            // 5.2 瓦片不存在：写入布隆过滤器（下次直接过滤）
            if (bloomFilter != null) {
                bloomFilter.add(tileKey);
                log.trace("布隆过滤器记录不存在瓦片：{}", tileKey);
            }
            // 不存在的瓦片直接返回null（或构造空的TileRequest，根据业务需求调整）
            return null;
        }

        return tileData;
    }

    /** 补全TileRequest的必要字段（避免缓存中字段缺失） */
    private void fillTileRequestFields(TileRequest tile, GirLayerConfigContext layerConfigContext) {
        if (tile.getLayerName() == null) {
            tile.setLayerName(layerConfigContext.getLayerName());
        }
        if (tile.getStorageType() == null) {
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

            // 提交异步任务
            executorService.submit(
                    () -> {
                        try {
                            tileCache.putTile(tileKey, cacheTile, fileFormat);
                            log.trace("异步缓存瓦片成功：{}", tileKey);
                        } catch (Exception e) {
                            log.error("异步缓存瓦片失败：{}", tileKey, e);
                        }
                    });
        } catch (Exception e) {
            log.error("构建缓存瓦片数据失败：{}", tileKey, e);
            // 异常时不影响主线程返回原始数据
        }
    }

    /** 关闭资源（线程池+布隆过滤器） */
    private void shutdown() {
        log.info("开始关闭CachedTileStorage资源...");
        // 关闭线程池（等待已提交任务执行完成）
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                log.warn("线程池强制关闭，可能有未完成的缓存任务");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        // 清空布隆过滤器（释放内存）
        layerBloomFilters.clear();
        log.info("CachedTileStorage资源关闭完成");
    }

    @Override
    public void preCacheTiles(
            GirLayerConfigContext layerConfigContext,
            TileCache tileCache,
            ProgressConsumer progressConsumer) {
        delegate.preCacheTiles(layerConfigContext, tileCache, progressConsumer);
    }
}
