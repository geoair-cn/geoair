package cn.geoair.map.tile.forge.fuser.precache;

import static cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils.ORIGINAL_GRID_SUFFIX;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.GirFuser;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.fuser.CacheTileFuserExec;
import cn.geoair.map.tile.forge.fuser.fuser.GirFuserExecFactory;
import cn.geoair.map.tile.forge.fuser.provider.CachedTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.TileGetterFactory;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import cn.geoair.map.tile.forge.fuser.utils.LargeBlankCheck;
import cn.geoair.map.tile.forge.fuser.utils.TileBlankDetector;
import cn.geoair.web.mime.GirImageMime;

import org.locationtech.jts.geom.Geometry;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 瓦片任务执行器 提供生产者-消费者模式的公共实现
 *
 * @author 张俊
 */
public class TileTaskExecutor {

    private static final GiLogger log = GirLoggerFactory.getLogger();
    private static final TileCoordinate POISON_PILL = new TileCoordinate(-1, -1, -1);

    private final TileTaskConfig config;
    private final PxyLayerInfo pxyLayerInfo;
    private final boolean googleGridIs;
    private final String layerName;
    private final int zoom;
    private final Geometry geometry4326;
    private final GirImageMime format;

    // 任务类型
    private final TaskType taskType;

    public TileTaskExecutor(TileTaskConfig config) {
        this.config = config;
        this.layerName = config.getLayerName();
        this.zoom = config.getZoom();
        this.geometry4326 = config.getGeometry4326();
        this.format = config.getFormat();

        this.pxyLayerInfo = GirFuser.getPxyLayerInfo(layerName);
        if (this.pxyLayerInfo == null) {
            log.error("图层不存在: {}", layerName);
            throw new RuntimeException("图层不存在: " + layerName);
        }
        this.googleGridIs = pxyLayerInfo.isGoogleGrid();

        // 根据配置判断任务类型
        this.taskType = config.getTaskType();
    }

    /** 创建预缓存任务执行器 */
    public static TileTaskExecutor forPreCache(TileTaskConfig config) {
        return new TileTaskExecutor(config);
    }

    /** 创建检查修复任务执行器 */
    public static TileTaskExecutor forCheckAndRepair(TileTaskConfig config) {
        return new TileTaskExecutor(config);
    }

    /** 创建原始网格检查修复任务执行器 */
    public static TileTaskExecutor forOriginalCheckAndRepair(TileTaskConfig config) {
        return new TileTaskExecutor(config);
    }

    /** 创建原始网格预缓存任务执行器 */
    public static TileTaskExecutor forOriginalPreCache(TileTaskConfig config) {
        return new TileTaskExecutor(config);
    }

    /** 执行任务 */
    public void execute() {
        try {
            // 计算瓦片范围
            RangeApo rangeApo = calculateTileRange();
            int minX = rangeApo.getMinX();
            int maxX = rangeApo.getMaxX();
            int minY = rangeApo.getMinY();
            int maxY = rangeApo.getMaxY();

            int totalTiles = (maxX - minX + 1) * (maxY - minY + 1);
            config.getTotalCount().addAndGet(totalTiles);

            log.info(
                    "{} - 层级: {}, X范围: [{}, {}], Y范围: [{}, {}], 总瓦片数: {}",
                    taskType.getDescription(),
                    zoom,
                    minX,
                    maxX,
                    minY,
                    maxY,
                    totalTiles);

            if (totalTiles == 0) {
                log.warn("层级 {} 没有瓦片需要处理", zoom);
                return;
            }

            // 计算线程池大小
            int batchSize = 10000;
            int totalBatches = (totalTiles + batchSize - 1) / batchSize;
            int threadPoolSize =
                    Math.min(totalBatches, Runtime.getRuntime().availableProcessors() * 2);
            threadPoolSize = Math.max(1, threadPoolSize);

            log.info("层级 {} 启动消费者线程数量：{}", zoom, threadPoolSize);

            ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
            CountDownLatch consumerLatch = new CountDownLatch(threadPoolSize);

            // 统计计数器
            AtomicLong zoomSuccess = new AtomicLong(0);
            AtomicLong zoomFail = new AtomicLong(0);
            AtomicLong zoomChecked = new AtomicLong(0);
            AtomicLong zoomRepaired = new AtomicLong(0);
            AtomicLong zoomSkipped = new AtomicLong(0);
            AtomicLong totalValidTiles = new AtomicLong(0);
            AtomicLong processedCount = new AtomicLong(0);

            int progressInterval = Math.max(100, totalTiles / 100);
            AtomicBoolean shutdownSignalSent = new AtomicBoolean(false);
            BlockingQueue<TileCoordinate> taskQueue = new LinkedBlockingQueue<>(batchSize * 2);

            // 启动生产者
            Thread producerThread =
                    startProducer(
                            minX,
                            maxX,
                            minY,
                            maxY,
                            taskQueue,
                            totalValidTiles,
                            shutdownSignalSent,
                            threadPoolSize);

            // 启动消费者
            startConsumers(
                    executorService,
                    threadPoolSize,
                    consumerLatch,
                    taskQueue,
                    zoomSuccess,
                    zoomFail,
                    zoomChecked,
                    zoomRepaired,
                    zoomSkipped,
                    processedCount,
                    totalValidTiles,
                    progressInterval);

            // 等待完成
            waitForCompletion(executorService, consumerLatch, producerThread);

            // 更新全局计数器
            updateGlobalCounters(
                    zoomSuccess, zoomFail, zoomChecked, zoomRepaired, zoomSkipped, totalValidTiles);

        } catch (Exception e) {
            log.error("{} 失败 - 层级: {}, 错误: {}", taskType.getDescription(), zoom, e.getMessage(), e);
        } finally {
            if (config.getLatch() != null) {
                config.getLatch().countDown();
            }
        }
    }

    /** 计算瓦片范围 */
    private RangeApo calculateTileRange() {
        if (taskType == TaskType.ORIGINAL_CHECK_REPAIR || taskType == TaskType.ORIGINAL_PRE_CACHE) {
            // 原始网格使用不同的坐标计算
            if (googleGridIs) {
                Geometry convert = GirAdvTools.getSridOpt().convert(geometry4326, 4326, 3857);
                return GirAdvTools.getTileGrid3857Opt().tileRangeByGeom(zoom, convert);
            } else {
                return GirAdvTools.getTileGrid4326Opt().tileRangeByGeom(zoom, geometry4326);
            }
        } else {
            // 预缓存和普通检查修复
            if (googleGridIs) {
                return GirAdvTools.getTileGrid4326Opt().tileRangeByGeom(zoom, geometry4326);
            } else {
                Geometry convert = GirAdvTools.getSridOpt().convert(geometry4326, 4326, 3857);
                return GirAdvTools.getTileGrid3857Opt().tileRangeByGeom(zoom, convert);
            }
        }
    }

    /** 判断瓦片是否与几何相交 */
    private boolean isTileIntersects(int x, int y) {
        try {
            BoxReferencedEnvelope box;
            if (taskType == TaskType.ORIGINAL_CHECK_REPAIR
                    || taskType == TaskType.ORIGINAL_PRE_CACHE) {
                // 原始网格使用相反的坐标计算
                if (googleGridIs) {
                    box = GirAdvTools.getTileGrid3857Opt().xyzToTileBox(zoom, x, y, 4326);
                } else {
                    box = GirAdvTools.getTileGrid4326Opt().xyzToTileBox(zoom, x, y, 3857);
                }
            } else {
                if (googleGridIs) {
                    box = GirAdvTools.getTileGrid4326Opt().xyzToTileBox(zoom, x, y, 3857);
                } else {
                    box = GirAdvTools.getTileGrid3857Opt().xyzToTileBox(zoom, x, y, 4326);
                }
            }

            String wktString = box.getWktString(4326);
            Geometry geometryByBox = GirAdvTools.getFormatOpt().wktToJtsGeometry(wktString);
            return geometry4326.intersects(geometryByBox);
        } catch (Exception e) {
            log.error("判断瓦片相交异常: {}-({},{},{})", layerName, zoom, x, y, e);
            return false;
        }
    }

    /** 启动生产者线程 */
    private Thread startProducer(
            int minX,
            int maxX,
            int minY,
            int maxY,
            BlockingQueue<TileCoordinate> taskQueue,
            AtomicLong totalValidTiles,
            AtomicBoolean shutdownSignalSent,
            int threadPoolSize) {
        Thread producerThread =
                new Thread(
                        () -> {
                            try {
                                int validTileCount = 0;
                                for (int x = minX; x <= maxX; x++) {
                                    for (int y = minY; y <= maxY; y++) {
                                        if (isTileIntersects(x, y)) {
                                            taskQueue.put(new TileCoordinate(zoom, x, y));
                                            validTileCount++;
                                        }
                                    }
                                }
                                totalValidTiles.set(validTileCount);
                                log.info("生产者完成，有效瓦片数: {}", validTileCount);
                            } catch (Exception e) {
                                Thread.currentThread().interrupt();
                                log.error("生产者线程被中断", e);
                            } finally {
                                if (shutdownSignalSent.compareAndSet(false, true)) {
                                    log.info("生产者发送结束信号，消费者数量: {}", threadPoolSize);
                                    for (int i = 0; i < threadPoolSize; i++) {
                                        try {
                                            taskQueue.put(POISON_PILL);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                            log.error("发送结束信号失败", e);
                                            break;
                                        }
                                    }
                                }
                            }
                        },
                        "Producer-" + taskType.getPrefix() + "-" + zoom);

        producerThread.start();
        return producerThread;
    }

    /** 启动消费者线程 */
    /** 启动消费者线程 */
    private void startConsumers(
            ExecutorService executorService,
            int threadPoolSize, // 明确传入线程数
            CountDownLatch consumerLatch,
            BlockingQueue<TileCoordinate> taskQueue,
            AtomicLong zoomSuccess,
            AtomicLong zoomFail,
            AtomicLong zoomChecked,
            AtomicLong zoomRepaired,
            AtomicLong zoomSkipped,
            AtomicLong processedCount,
            AtomicLong totalValidTiles,
            int progressInterval) {
        for (int i = 0; i < threadPoolSize; i++) {
            final int consumerId = i;
            executorService.submit(
                    () -> {
                        String threadName =
                                "Consumer-"
                                        + taskType.getPrefix()
                                        + "-"
                                        + zoom
                                        + "-"
                                        + layerName
                                        + "-"
                                        + consumerId;
                        Thread.currentThread().setName(threadName);

                        try {
                            log.debug("消费者线程 {} 启动", threadName);

                            while (true) {
                                TileCoordinate coord = taskQueue.take();

                                if (coord == POISON_PILL) {
                                    log.debug("消费者线程 {} 收到结束信号", threadName);
                                    break;
                                }

                                // 处理瓦片
                                processTile(
                                        coord,
                                        zoomSuccess,
                                        zoomFail,
                                        zoomChecked,
                                        zoomRepaired,
                                        zoomSkipped);

                                // 更新进度
                                updateProgress(
                                        processedCount,
                                        totalValidTiles,
                                        progressInterval,
                                        zoomSuccess,
                                        zoomFail,
                                        zoomChecked,
                                        zoomRepaired,
                                        zoomSkipped);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.error("消费者线程 {} 被中断", threadName, e);
                        } finally {
                            consumerLatch.countDown();
                            log.debug(
                                    "消费者线程 {} 结束，剩余消费者: {}", threadName, consumerLatch.getCount());
                        }
                    });
        }
    }

    /** 处理单个瓦片 */
    private void processTile(
            TileCoordinate coord,
            AtomicLong zoomSuccess,
            AtomicLong zoomFail,
            AtomicLong zoomChecked,
            AtomicLong zoomRepaired,
            AtomicLong zoomSkipped) {
        int z = coord.getZoom();
        int x = coord.getX();
        int y = coord.getY();

        switch (taskType) {
            case PRE_CACHE:
                processPreCacheTile(z, x, y, zoomSuccess, zoomFail);
                break;
            case CHECK_REPAIR:
                processCheckAndRepairTile(
                        z, x, y, zoomChecked, zoomRepaired, zoomFail, zoomSkipped);
                break;
            case ORIGINAL_CHECK_REPAIR:
                processOriginalCheckAndRepairTile(
                        z, x, y, zoomChecked, zoomRepaired, zoomFail, zoomSkipped);
                break;
            case ORIGINAL_PRE_CACHE:
                processOriginalPreCacheTile(z, x, y, zoomSuccess, zoomFail);
                break;
        }
    }

    /** 处理预缓存瓦片 */
    private void processPreCacheTile(int z, int x, int y, AtomicLong success, AtomicLong fail) {
        try {
            BoxReferencedEnvelope box =
                    googleGridIs
                            ? GirAdvTools.getTileGrid4326Opt().xyzToTileBox(z, x, y, 3857)
                            : GirAdvTools.getTileGrid3857Opt().xyzToTileBox(z, x, y, 4326);

            BoundingBox bounds =
                    new BoundingBox(box.getMinX(), box.getMinY(), box.getMaxX(), box.getMaxY());

            CacheTileFuserExec cacheTileFuser =
                    GirFuserExecFactory.createCachedFuser(
                            layerName, z, x, y, bounds, 256, 256, ImageMime.png);

            byte[] imageBytes = cacheTileFuser.toImageBytes();
            if (imageBytes != null && imageBytes.length > 0) {
                success.incrementAndGet();
            } else {
                fail.incrementAndGet();
                log.warn("预缓存瓦片失败（资源为空）: {}-({},{},{})", layerName, z, x, y);
            }
        } catch (Exception e) {
            fail.incrementAndGet();
            log.error("预缓存瓦片异常: {}-({},{},{})", layerName, z, x, y, e);
        }
    }

    /** 处理检查修复瓦片 */
    private void processCheckAndRepairTile(
            int z,
            int x,
            int y,
            AtomicLong checked,
            AtomicLong repaired,
            AtomicLong fail,
            AtomicLong skipped) {
        try {
            BoxReferencedEnvelope box =
                    googleGridIs
                            ? GirAdvTools.getTileGrid4326Opt().xyzToTileBox(z, x, y, 3857)
                            : GirAdvTools.getTileGrid3857Opt().xyzToTileBox(z, x, y, 4326);

            BoundingBox bounds =
                    new BoundingBox(box.getMinX(), box.getMinY(), box.getMaxX(), box.getMaxY());

            CacheTileFuserExec cacheTileFuser =
                    GirFuserExecFactory.createCachedFuser(
                            layerName, z, x, y, bounds, 256, 256, ImageMime.png);

            TileCache tileCache = cacheTileFuser.getTileCache();

            // 只检查已存在的瓦片
            if (!tileCache.exists(layerName, z, x, y, format)) {
                skipped.incrementAndGet();
                checked.incrementAndGet();
                return;
            }

            byte[] bytes = tileCache.get(layerName, z, x, y, format);
            if (bytes == null || bytes.length == 0) {
                log.warn("瓦片数据为空: z={}, x={}, y={}", z, x, y);
                fail.incrementAndGet();
                checked.incrementAndGet();
                return;
            }

            // 检测空白矩形
            LargeBlankCheck largeBlankCheck =
                    TileBlankDetector.hasLargeBlankRect(bytes, format.getInternalName());
            if (largeBlankCheck.getBlankIs()) {
                log.info("检测到异常瓦片（空白矩形）: z={}, x={}, y={}, 开始重新切片", z, x, y);

                // 删除异常缓存
                FuserCacheUtils.deleteCacheByRequestGrid(
                        layerName, z, x, y, cacheTileFuser, format);

                // 重新生成
                byte[] newImageBytes = cacheTileFuser.toImageBytes();
                if (newImageBytes != null && newImageBytes.length > 0) {
                    repaired.incrementAndGet();
                    log.debug("瓦片重新切片成功: z={}, x={}, y={}", z, x, y);
                } else {
                    fail.incrementAndGet();
                    log.warn("瓦片重新切片失败: z={}, x={}, y={}", z, x, y);
                }
            }

            checked.incrementAndGet();

        } catch (Exception e) {
            fail.incrementAndGet();
            log.error("检查瓦片异常: z={}, x={}, y={}, 错误: {}", z, x, y, e.getMessage(), e);
        }
    }

    /** 处理原始网格预缓存瓦片 */
    private void processOriginalPreCacheTile(
            int z, int x, int y, AtomicLong success, AtomicLong fail) {
        String originalCacheName = config.getOriginalCacheName();
        if (originalCacheName == null || originalCacheName.isEmpty()) {
            originalCacheName = layerName + ORIGINAL_GRID_SUFFIX;
        }

        try {
            // 反转Y坐标（原始网格使用谷歌原点）
            int reversedY = GirAdvTools.getTileGrid3857Opt().reverseY(y, z);

            // 获取原始网格的TileGetter
            CachedTileGetter layerTileGetter =
                    (CachedTileGetter)
                            TileGetterFactory.create(pxyLayerInfo, null, originalCacheName);

            // 生成原始网格瓦片
            Resource tileResource = layerTileGetter.getTileResource(z, x, reversedY);
            byte[] imageBytes = tileResource.getByteData();

            if (imageBytes != null && imageBytes.length > 0) {
                success.incrementAndGet();
                log.debug("原始网格预缓存成功: {}-({},{},{})", originalCacheName, z, x, y);
            } else {
                fail.incrementAndGet();
                log.warn("原始网格预缓存失败（资源为空）: {}-({},{},{})", originalCacheName, z, x, y);
            }
        } catch (Exception e) {
            fail.incrementAndGet();
            log.error("原始网格预缓存异常: {}-({},{},{})", originalCacheName, z, x, y, e);
        }
    }

    /** 处理原始网格检查修复瓦片 */
    private void processOriginalCheckAndRepairTile(
            int z,
            int x,
            int y,
            AtomicLong checked,
            AtomicLong repaired,
            AtomicLong fail,
            AtomicLong skipped) {
        String originalCacheName = config.getOriginalCacheName();
        if (originalCacheName == null || originalCacheName.isEmpty()) {
            originalCacheName = layerName + ORIGINAL_GRID_SUFFIX;
        }

        try {
            // 反转Y坐标（原始网格使用谷歌原点）
            int reversedY = GirAdvTools.getTileGrid3857Opt().reverseY(y, z);

            // 获取原始网格的TileCache
            CachedTileGetter layerTileGetter =
                    (CachedTileGetter)
                            TileGetterFactory.create(pxyLayerInfo, null, originalCacheName);
            TileCache tileCache = layerTileGetter.getTileCache();

            // 只检查已存在的瓦片
            if (!tileCache.exists(originalCacheName, z, x, reversedY, format)) {
                skipped.incrementAndGet();
                checked.incrementAndGet();
                return;
            }

            byte[] bytes = tileCache.get(originalCacheName, z, x, reversedY, format);
            if (bytes == null || bytes.length == 0) {
                log.warn("原始网格瓦片数据为空: z={}, x={}, y={}", z, x, y);
                fail.incrementAndGet();
                checked.incrementAndGet();
                return;
            }

            // 检测空白矩形
            LargeBlankCheck largeBlankCheck =
                    TileBlankDetector.hasLargeBlankRect(bytes, format.getInternalName());
            if (largeBlankCheck.getBlankIs()) {
                log.info("检测到原始网格异常瓦片（空白矩形）: z={}, x={}, y={}, 开始重新切片", z, x, y);

                // 删除异常缓存
                tileCache.delete(originalCacheName, z, x, reversedY, format);

                // 重新生成
                Resource tileResource = layerTileGetter.getTileResource(z, x, reversedY);
                byte[] newImageBytes = tileResource.getByteData();
                if (newImageBytes != null && newImageBytes.length > 0) {
                    repaired.incrementAndGet();
                    log.debug("原始网格瓦片重新切片成功: z={}, x={}, y={}", z, x, y);
                } else {
                    fail.incrementAndGet();
                    log.warn("原始网格瓦片重新切片失败: z={}, x={}, y={}", z, x, y);
                }
            }

            checked.incrementAndGet();

        } catch (Exception e) {
            fail.incrementAndGet();
            log.error("检查原始网格瓦片异常: z={}, x={}, y={}, 错误: {}", z, x, y, e.getMessage(), e);
        }
    }

    /** 更新进度 */
    private void updateProgress(
            AtomicLong processedCount,
            AtomicLong totalValidTiles,
            int progressInterval,
            AtomicLong zoomSuccess,
            AtomicLong zoomFail,
            AtomicLong zoomChecked,
            AtomicLong zoomRepaired,
            AtomicLong zoomSkipped) {
        long totalProcessed = processedCount.incrementAndGet();
        long totalValid = totalValidTiles.get();

        if (totalValid > 0) {
            if (totalProcessed % progressInterval == 0 || totalProcessed == totalValid) {
                double percent = (double) totalProcessed / totalValid * 100;

                switch (taskType) {
                    case PRE_CACHE:
                        log.info(
                                "层级 {} 进度: {}/{} ({}%), 成功: {}, 失败: {}",
                                zoom,
                                totalProcessed,
                                totalValid,
                                percent,
                                zoomSuccess.get(),
                                zoomFail.get());
                        break;
                    case CHECK_REPAIR:
                    case ORIGINAL_CHECK_REPAIR:
                        log.info(
                                "层级 {} 检查进度: {}/{} ({}%), 已修复: {}, 失败: {}, 跳过(不存在): {}",
                                zoom,
                                totalProcessed,
                                totalValid,
                                percent,
                                zoomRepaired.get(),
                                zoomFail.get(),
                                zoomSkipped.get());
                        break;
                }
            }
        } else if (totalProcessed % 1000 == 0) {
            log.info("层级 {} 已处理: {} 个瓦片", zoom, totalProcessed);
        }
    }

    /** 等待任务完成 */
    private void waitForCompletion(
            ExecutorService executorService, CountDownLatch consumerLatch, Thread producerThread) {
        // 等待消费者完成
        try {
            boolean completed = consumerLatch.await(30, TimeUnit.DAYS);
            if (!completed) {
                log.warn("消费者线程未能在30分钟内完成，强制关闭");
                executorService.shutdownNow();
            } else {
                log.info("所有消费者线程已完成: {}", zoom);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待消费者完成时被中断", e);
            executorService.shutdownNow();
        }

        // 等待生产者结束
        try {
            producerThread.join(5000);
            if (producerThread.isAlive()) {
                log.warn("生产者线程未能正常结束，强制中断");
                producerThread.interrupt();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待生产者线程结束被中断", e);
            producerThread.interrupt();
        }

        // 关闭线程池
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.DAYS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.DAYS)) {
                    log.error("线程池未能正常关闭");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /** 更新全局计数器 */
    private void updateGlobalCounters(
            AtomicLong zoomSuccess,
            AtomicLong zoomFail,
            AtomicLong zoomChecked,
            AtomicLong zoomRepaired,
            AtomicLong zoomSkipped,
            AtomicLong totalValidTiles) {
        switch (taskType) {
            case PRE_CACHE:
            case ORIGINAL_PRE_CACHE:
                config.getSuccessCount().addAndGet(zoomSuccess.get());
                config.getFailCount().addAndGet(zoomFail.get());
                log.info(
                        "{} 完成 - 层级: {}, 有效瓦片: {}, 成功: {}, 失败: {}",
                        taskType.getDescription(),
                        zoom,
                        totalValidTiles.get(),
                        zoomSuccess.get(),
                        zoomFail.get());
                break;
            case CHECK_REPAIR:
            case ORIGINAL_CHECK_REPAIR:
                config.getCheckedCount().addAndGet(zoomChecked.get());
                config.getRepairedCount().addAndGet(zoomRepaired.get());
                config.getFailCount().addAndGet(zoomFail.get());
                log.info(
                        "{} 完成 - 层级: {}, 有效瓦片: {}, 已检查: {}, 已修复: {}, 失败: {}, 跳过(不存在): {}",
                        taskType.getDescription(),
                        zoom,
                        totalValidTiles.get(),
                        zoomChecked.get(),
                        zoomRepaired.get(),
                        zoomFail.get(),
                        zoomSkipped.get());
                break;
        }
    }
}
