package cn.geoair.map.tile.forge.fuser.precache.bask;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.fuser.GirFuser;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.fuser.CacheTileFuserExec;
import cn.geoair.map.tile.forge.fuser.fuser.GirFuserExecFactory;
import cn.geoair.map.tile.forge.fuser.precache.TileCoordinate;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.locationtech.jts.geom.Geometry;

/**
 * 单层级融合预缓存任务
 *
 * @author 张俊
 * @date Created in 2026/6/15
 */
public class TileFuserPreCacheTask implements Runnable {

    private static GiLogger log = GirLoggerFactory.getLogger();
    private static final cn.geoair.map.tile.forge.fuser.precache.TileCoordinate POISON_PILL =
            new cn.geoair.map.tile.forge.fuser.precache.TileCoordinate(-1, -1, -1);

    private final String layerName;
    private final int zoom;
    private final Geometry geometry4326;
    private final CountDownLatch latch;
    private final AtomicLong totalCount;
    private final AtomicLong successCount;
    private final AtomicLong failCount;
    private final ImageMime format;
    boolean googleGridIs;

    public TileFuserPreCacheTask(
            String layerName,
            int zoom,
            Geometry geometry4326,
            CountDownLatch latch,
            AtomicLong totalCount,
            AtomicLong successCount,
            AtomicLong failCount,
            ImageMime format) {
        this.layerName = layerName;
        this.zoom = zoom;
        this.geometry4326 = geometry4326;
        this.latch = latch;
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.failCount = failCount;
        this.format = format;
        PxyLayerInfo pxyLayerInfo = GirFuser.getPxyLayerInfo(layerName);

        if (pxyLayerInfo == null) {
            log.error("图层不存在  {}", layerName);
            throw new RuntimeException("图层不存在");
        }

        googleGridIs = pxyLayerInfo.isGoogleGrid();
    }

    @Override
    public void run() {
        try {

            RangeApo rangeApo = null;
            if (googleGridIs) {
                // 计算当前层级的瓦片范围
                rangeApo = GirAdvTools.getTileGrid4326Opt().tileRangeByGeom(zoom, geometry4326);
            } else {
                Geometry convert = GirAdvTools.getSridOpt().convert(geometry4326, 4326, 3857);
                rangeApo = GirAdvTools.getTileGrid3857Opt().tileRangeByGeom(zoom, convert);
            }

            int minX = rangeApo.getMinX();
            int maxX = rangeApo.getMaxX();
            int minY = rangeApo.getMinY();
            int maxY = rangeApo.getMaxY();

            int totalTiles = (maxX - minX + 1) * (maxY - minY + 1);
            totalCount.addAndGet(totalTiles);

            log.info(
                    "预缓存层级: {}, X范围: [{}, {}], Y范围: [{}, {}], 瓦片数: {}",
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
            threadPoolSize = Math.max(1, threadPoolSize); // 至少启动一个线程

            log.info("层级 {} 启动消费者线程数量：{}", zoom, threadPoolSize);

            ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

            // 消费者完成计数器
            CountDownLatch consumerLatch = new CountDownLatch(threadPoolSize);

            AtomicLong zoomSuccess = new AtomicLong(0);
            AtomicLong zoomFail = new AtomicLong(0);
            AtomicLong totalValidTiles = new AtomicLong(0);

            // 进度计数器
            AtomicLong processedCount = new AtomicLong(0);
            // 进度打印间隔（每处理多少个瓦片打印一次进度）
            int progressInterval = Math.max(100, totalTiles / 100); // 默认每1%打印一次，最少100个

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
                                                // 先过滤不相交的瓦片
                                                if (googleGridIs) {
                                                    BoxReferencedEnvelope box =
                                                            GirAdvTools.getTileGrid4326Opt()
                                                                    .xyzToTileBox(
                                                                            zoom,
                                                                            x,
                                                                            y,
                                                                            TileYAxis.XYZ,
                                                                            3857);
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
                                                    BoxReferencedEnvelope box =
                                                            GirAdvTools.getTileGrid3857Opt()
                                                                    .xyzToTileBox(
                                                                            zoom,
                                                                            x,
                                                                            y,
                                                                            TileYAxis.XYZ,
                                                                            4326);
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
                                                        layerName,
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
                                        for (int i = 0; i < finalThreadPoolSize * 2; i++) {
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
                            "Producer-" + zoom);

            // ============ 启动消费者线程 ============
            for (int i = 0; i < threadPoolSize; i++) {
                final int consumerId = i;
                executorService.submit(
                        () -> {
                            String threadName =
                                    "Consumer-" + zoom + "-" + layerName + "-" + consumerId;
                            Thread.currentThread().setName(threadName);

                            int localProcessed = 0;

                            try {
                                log.info("消费者线程 {} 启动", threadName);

                                while (true) {
                                    cn.geoair.map.tile.forge.fuser.precache.TileCoordinate coord =
                                            taskQueue.take();

                                    // 检查结束标志 - 使用 == 比较对象引用
                                    if (coord == POISON_PILL) {
                                        log.debug(
                                                "消费者线程 {} 收到结束信号，共处理 {} 个瓦片",
                                                threadName,
                                                localProcessed);
                                        break;
                                    }

                                    // 直接处理单个瓦片
                                    processSingleTile(coord, zoomSuccess, zoomFail);
                                    localProcessed++;

                                    // 更新全局处理计数
                                    long totalProcessed = processedCount.incrementAndGet();
                                    long totalValid = totalValidTiles.get();

                                    // 打印进度
                                    if (totalValid > 0) {
                                        if (totalProcessed % progressInterval == 0
                                                || totalProcessed == totalValid) {
                                            long success = zoomSuccess.get();
                                            long fail = zoomFail.get();
                                            double percent =
                                                    (double) totalProcessed / totalValid * 100;
                                            log.info(
                                                    "层级 {} 进度: {}/{} ({}%), 成功: {}, 失败: {}",
                                                    zoom,
                                                    totalProcessed,
                                                    totalValid,
                                                    percent,
                                                    success,
                                                    fail);
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
                                log.error("消费者线程 {} 被中断，已处理 {} 个瓦片", threadName, localProcessed, e);
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
                    log.info("所有消费者线程已完成:{}", zoom);
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
            long finalSuccess = zoomSuccess.get();
            long finalFail = zoomFail.get();
            successCount.addAndGet(finalSuccess);
            failCount.addAndGet(finalFail);

            log.info(
                    "层级预缓存完成: {}, 有效瓦片: {}, 成功: {}, 失败: {}",
                    zoom,
                    totalValidTiles.get(),
                    finalSuccess,
                    finalFail);

        } catch (Exception e) {
            log.error("预缓存层级失败: {}, 错误: {}", zoom, e.getMessage(), e);
        } finally {
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    /** 处理单个瓦片 */
    private void processSingleTile(TileCoordinate coord, AtomicLong success, AtomicLong fail) {
        int z = coord.getZoom();
        int x = coord.getX();
        int y = coord.getY();

        try {
            // 获取瓦片的边界框
            BoxReferencedEnvelope box = null;
            if (googleGridIs) {
                box = GirAdvTools.getTileGrid4326Opt().xyzToTileBox(z, x, y, TileYAxis.XYZ, 3857);
            } else {
                box = GirAdvTools.getTileGrid3857Opt().xyzToTileBox(z, x, y, TileYAxis.XYZ, 4326);
            }
            // 创建缓存融合器
            BoundingBox bounds =
                    new BoundingBox(box.getMinX(), box.getMinY(), box.getMaxX(), box.getMaxY());
            CacheTileFuserExec cacheTileFuser =
                    GirFuserExecFactory.createCachedFuser(
                            layerName, z, x, y, bounds, 256, 256, ImageMime.png);

            // 检查缓存是否存在
            TileCache tileCache = cacheTileFuser.getTileCache();
            if (tileCache.exists(layerName, z, x, y, format)) {
                log.debug("缓存存在, layerName:{} ,z：{}，x：{}，y：{}", layerName, z, x, y);
            }

            // 生成新的瓦片
            byte[] imageBytes = cacheTileFuser.toImageBytes();
            if (imageBytes != null) {
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
}
