package cn.geoair.map.tile.forge.fuser.precache.bask;

import static cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils.ORIGINAL_GRID_SUFFIX;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilObject;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.CustomTileCacheHelper;
import cn.geoair.map.tile.forge.fuser.GirFuser;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.precache.TileCoordinate;
import cn.geoair.map.tile.forge.fuser.provider.CachedTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.TileGetterFactory;
import cn.geoair.map.tile.forge.fuser.utils.LargeBlankCheck;
import cn.geoair.map.tile.forge.fuser.utils.TileBlankDetector;

import org.locationtech.jts.geom.Geometry;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/27 13:09
 * @description： 原始网格的预缓存任务
 */
public class TileOriginalCheckAndRepairTask implements Runnable {
    private static GiLogger log = GirLoggerFactory.getLogger();
    // 使用唯一的对象作为 Poison Pill（结束信号）
    private static final cn.geoair.map.tile.forge.fuser.precache.TileCoordinate POISON_PILL =
            new cn.geoair.map.tile.forge.fuser.precache.TileCoordinate(-1, -1, -1);

    private final String originalCacheName;
    private final int zoom;
    private final Geometry geometry4326;
    private final CountDownLatch latch;
    private final AtomicLong totalCount;
    private final AtomicLong checkedCount;
    private final AtomicLong repairedCount;
    private final AtomicLong failCount;
    private final ImageMime format;
    boolean googleGridIs;
    TileCache tileCache;

    CachedTileGetter layerTileGetter;
    PxyLayerInfo pxyLayerInfo;

    public TileOriginalCheckAndRepairTask(
            String layerName,
            String originalCacheName,
            int zoom,
            Geometry geometry4326,
            CountDownLatch latch,
            AtomicLong totalCount,
            AtomicLong checkedCount,
            AtomicLong repairedCount,
            AtomicLong failCount,
            ImageMime format) {
        this.originalCacheName =
                GutilObject.isEmpty(originalCacheName)
                        ? layerName + ORIGINAL_GRID_SUFFIX
                        : originalCacheName;
        this.zoom = zoom;
        this.geometry4326 = geometry4326;
        this.latch = latch;
        this.totalCount = totalCount;
        this.checkedCount = checkedCount;
        this.repairedCount = repairedCount;
        this.failCount = failCount;
        this.format = format;
        pxyLayerInfo = GirFuser.getPxyLayerInfo(layerName);
        if (pxyLayerInfo == null) {
            log.error("图层不存在  {}", layerName);
            throw new RuntimeException("图层不存在");
        }
        googleGridIs = pxyLayerInfo.isGoogleGrid();
        layerTileGetter =
                (CachedTileGetter)
                        TileGetterFactory.create(
                                pxyLayerInfo,
                                CustomTileCacheHelper.getInstance().getTileCache(layerName),
                                originalCacheName);
        tileCache = layerTileGetter.getTileCache();
    }

    @Override
    public void run() {
        try {
            // 计算当前层级的瓦片范围
            RangeApo rangeApo = null;
            if (googleGridIs) {
                Geometry convert = GirAdvTools.getSridOpt().convert(geometry4326, 4326, 3857);
                rangeApo = GirAdvTools.getTileGrid3857Opt().tileRangeByGeom(zoom, convert);
            } else {
                // 计算当前层级的瓦片范围
                rangeApo = GirAdvTools.getTileGrid4326Opt().tileRangeByGeom(zoom, geometry4326);
            }
            int minX = rangeApo.getMinX();
            int maxX = rangeApo.getMaxX();
            int minY = rangeApo.getMinY();
            int maxY = rangeApo.getMaxY();

            int totalTiles = (maxX - minX + 1) * (maxY - minY + 1);
            totalCount.addAndGet(totalTiles);

            log.info(
                    "瓦片检查任务开始 - 层级: {}, X范围: [{}, {}], Y范围: [{}, {}], 总瓦片数: {}",
                    zoom,
                    minX,
                    maxX,
                    minY,
                    maxY,
                    totalTiles);

            // 如果总瓦片数为0，直接返回
            if (totalTiles == 0) {
                log.warn("层级 {} 没有瓦片需要处理", zoom);
                return;
            }

            // 计算线程数
            int batchSize = 10000;
            int totalBatches = (totalTiles + batchSize - 1) / batchSize;
            int threadPoolSize =
                    Math.min(totalBatches, Runtime.getRuntime().availableProcessors() * 2);
            threadPoolSize = Math.max(1, threadPoolSize);

            log.info("层级 {} 启动消费者线程数量：{}", zoom, threadPoolSize);

            ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

            // 消费者完成计数器
            CountDownLatch consumerLatch = new CountDownLatch(threadPoolSize);

            // 统计计数器
            AtomicLong zoomChecked = new AtomicLong(0);
            AtomicLong zoomRepaired = new AtomicLong(0);
            AtomicLong zoomFail = new AtomicLong(0);
            AtomicLong zoomSkipped = new AtomicLong(0);
            AtomicLong totalValidTiles = new AtomicLong(0);

            // 进度计数器
            AtomicLong processedCount = new AtomicLong(0);
            int progressInterval = Math.max(100, totalTiles / 100); // 每1%打印一次，最少100个

            // 控制结束信号只发送一次
            AtomicBoolean shutdownSignalSent = new AtomicBoolean(false);

            // 使用阻塞队列作为任务队列
            BlockingQueue<cn.geoair.map.tile.forge.fuser.precache.TileCoordinate> taskQueue =
                    new LinkedBlockingQueue<>(batchSize * 2);

            // ============ 启动生产者线程 ============
            int finalThreadPoolSize = threadPoolSize;
            Thread producerThread =
                    new Thread(
                            () -> {
                                try {
                                    int validTileCount = 0;
                                    for (int x = minX; x <= maxX; x++) {
                                        for (int y = minY; y <= maxY; y++) {
                                            try {
                                                if (!googleGridIs) {
                                                    // 先过滤不相交的瓦片
                                                    BoxReferencedEnvelope box =
                                                            GirAdvTools.getTileGrid4326Opt()
                                                                    .xyzToTileBox(zoom, x, y, 3857);
                                                    String wktString = box.getWktString(4326);
                                                    Geometry geometryByBox =
                                                            GirAdvTools.getFormatOpt()
                                                                    .wktToJtsGeometry(wktString);
                                                    if (geometry4326.intersects(geometryByBox)) {
                                                        taskQueue.put(
                                                                new cn.geoair.map.tile.forge.fuser
                                                                        .precache.TileCoordinate(
                                                                        zoom, x, y));
                                                        validTileCount++;
                                                    }
                                                } else {
                                                    // 先过滤不相交的瓦片
                                                    BoxReferencedEnvelope box =
                                                            GirAdvTools.getTileGrid3857Opt()
                                                                    .xyzToTileBox(zoom, x, y, 4326);
                                                    String wktString = box.getWktString(4326);
                                                    Geometry geometryByBox =
                                                            GirAdvTools.getFormatOpt()
                                                                    .wktToJtsGeometry(wktString);
                                                    if (geometry4326.intersects(geometryByBox)) {
                                                        taskQueue.put(
                                                                new cn.geoair.map.tile.forge.fuser
                                                                        .precache.TileCoordinate(
                                                                        zoom, x, y));
                                                        validTileCount++;
                                                    }
                                                }

                                            } catch (Exception e) {
                                                log.error(
                                                        "准备瓦片任务异常: {}-({},{},{})",
                                                        originalCacheName,
                                                        zoom,
                                                        x,
                                                        y,
                                                        e);
                                            }
                                        }
                                    }
                                    totalValidTiles.set(validTileCount);
                                    log.info("生产者完成，有效瓦片数: {}", validTileCount);
                                } catch (Exception e) {
                                    Thread.currentThread().interrupt();
                                    log.error("生产者线程被中断", e);
                                } finally {
                                    // 只发送一次结束信号，每个消费者一个
                                    if (shutdownSignalSent.compareAndSet(false, true)) {
                                        log.info("生产者发送结束信号，消费者数量: {}", finalThreadPoolSize);
                                        for (int i = 0; i < finalThreadPoolSize; i++) {
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
                            "Producer-Check-" + zoom);

            // ============ 启动消费者线程 ============
            for (int i = 0; i < threadPoolSize; i++) {
                final int consumerId = i;
                executorService.submit(
                        () -> {
                            String threadName =
                                    "Consumer-Check-"
                                            + zoom
                                            + "-"
                                            + originalCacheName
                                            + "-"
                                            + consumerId;
                            Thread.currentThread().setName(threadName);

                            try {
                                log.debug("消费者线程 {} 启动", threadName);

                                while (true) {
                                    cn.geoair.map.tile.forge.fuser.precache.TileCoordinate coord =
                                            taskQueue.take();

                                    // 检查结束标志
                                    if (coord == POISON_PILL) {
                                        log.debug("消费者线程 {} 收到结束信号", threadName);
                                        break;
                                    }

                                    // 处理单个瓦片并获取结果
                                    RepairResult result = checkAndRepairSingleTile(coord);

                                    // 更新统计计数器
                                    if (result.isRepaired()) {
                                        zoomRepaired.incrementAndGet();
                                    } else if (result.isFailed()) {
                                        zoomFail.incrementAndGet();
                                    } else if (result.isSkipped()) {
                                        zoomSkipped.incrementAndGet();
                                    }
                                    // 所有处理过的瓦片都计入已检查
                                    zoomChecked.incrementAndGet();

                                    // 更新进度
                                    long totalProcessed = processedCount.incrementAndGet();
                                    long totalValid = totalValidTiles.get();

                                    // 打印进度
                                    if (totalValid > 0) {
                                        if (totalProcessed % progressInterval == 0
                                                || totalProcessed == totalValid) {
                                            long checked = zoomChecked.get();
                                            long repaired = zoomRepaired.get();
                                            long fail = zoomFail.get();
                                            long skipped = zoomSkipped.get();
                                            double percent =
                                                    (double) totalProcessed / totalValid * 100;

                                            log.info(
                                                    "层级 {} 检查进度: {}/{} ({}%), 已修复: {}, 失败: {}, 跳过(不存在): {}",
                                                    zoom,
                                                    totalProcessed,
                                                    totalValid,
                                                    percent,
                                                    repaired,
                                                    fail,
                                                    skipped);
                                        }
                                    } else {
                                        // 如果有效瓦片数为0，每1000个打印一次
                                        if (totalProcessed % 1000 == 0) {
                                            log.info("层级 {} 已处理: {} 个瓦片", zoom, totalProcessed);
                                        }
                                    }
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.error("消费者线程 {} 被中断", threadName, e);
                            } finally {
                                consumerLatch.countDown();
                                log.debug(
                                        "消费者线程 {} 结束，剩余消费者: {}",
                                        threadName,
                                        consumerLatch.getCount());
                            }
                        });
            }

            // ============ 启动生产者 ============
            producerThread.start();

            // ============ 等待所有消费者完成 ============
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

            // ============ 等待生产者线程结束 ============
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

            // ============ 关闭线程池 ============
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

            // ============ 更新全局计数器 ============
            long finalChecked = zoomChecked.get();
            long finalRepaired = zoomRepaired.get();
            long finalFail = zoomFail.get();
            long finalSkipped = zoomSkipped.get();

            checkedCount.addAndGet(finalChecked);
            repairedCount.addAndGet(finalRepaired);
            failCount.addAndGet(finalFail);

            log.info(
                    "瓦片检查任务完成 - 层级: {}, 有效瓦片: {}, 已检查: {}, 已修复: {}, 失败: {}, 跳过(不存在): {}",
                    zoom,
                    totalValidTiles.get(),
                    finalChecked,
                    finalRepaired,
                    finalFail,
                    finalSkipped);

        } catch (Exception e) {
            log.error("瓦片检查任务失败 - 层级: {}, 错误: {}", zoom, e.getMessage(), e);
        } finally {
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    /**
     * 检查并修复单个瓦片
     *
     * @param coord 瓦片坐标
     * @return 修复结果
     */
    private RepairResult checkAndRepairSingleTile(TileCoordinate coord) {
        int z = coord.getZoom();
        int x = coord.getX();
        int y = coord.getY();
        // 这里的坐标是谷歌原点
        try {
            // 1. 获取瓦片的边界框
            y = GirAdvTools.getTileGrid3857Opt().reverseY(y, z);

            // 4. 只检查已存在的瓦片
            if (!tileCache.exists(originalCacheName, z, x, y, format)) {
                return RepairResult.skipped();
            }

            // 5. 获取缓存瓦片并进行检查
            byte[] bytes = tileCache.get(originalCacheName, z, x, y, format);
            if (bytes == null || bytes.length == 0) {
                log.warn("瓦片数据为空: z={}, x={}, y={}", z, x, y);
                return RepairResult.failed();
            }

            // 6. 检测是否为异常瓦片（空白矩形）
            LargeBlankCheck largeBlankCheck =
                    TileBlankDetector.hasLargeBlankRect(bytes, format.getInternalName());
            if (largeBlankCheck.getBlankIs()) {
                log.info("检测到异常瓦片（空白矩形）: z={}, x={}, y={}, 开始重新切片", z, x, y);

                // 7. 删除异常缓存
                tileCache.delete(originalCacheName, z, x, y, format);
                Resource tileResource = layerTileGetter.getTileResource(z, x, y);
                // 8. 重新生成瓦片
                byte[] newImageBytes = tileResource.getByteData();
                if (newImageBytes != null && newImageBytes.length > 0) {
                    log.debug("瓦片重新切片成功: z={}, x={}, y={}", z, x, y);
                    return RepairResult.repaired();
                } else {
                    log.warn("瓦片重新切片失败（生成空数据）: z={}, x={}, y={}", z, x, y);
                    return RepairResult.failed();
                }
            }

            // 瓦片正常
            return RepairResult.normal();

        } catch (Exception e) {
            log.error("检查瓦片异常: z={}, x={}, y={}, 错误: {}", z, x, y, e.getMessage(), e);
            return RepairResult.failed();
        }
    }

    /** 修复结果枚举 */
    private enum RepairResult {
        /** 已修复 */
        REPAIRED,
        /** 失败 */
        FAILED,
        /** 跳过（瓦片不存在） */
        SKIPPED,
        /** 正常（瓦片存在且正常） */
        NORMAL;

        public boolean isRepaired() {
            return this == REPAIRED;
        }

        public boolean isFailed() {
            return this == FAILED;
        }

        public boolean isSkipped() {
            return this == SKIPPED;
        }

        public boolean isNormal() {
            return this == NORMAL;
        }

        public static RepairResult repaired() {
            return REPAIRED;
        }

        public static RepairResult failed() {
            return FAILED;
        }

        public static RepairResult skipped() {
            return SKIPPED;
        }

        public static RepairResult normal() {
            return NORMAL;
        }
    }
}
