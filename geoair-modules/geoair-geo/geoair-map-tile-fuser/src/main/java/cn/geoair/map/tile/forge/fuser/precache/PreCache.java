package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;

import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 预缓存逻辑
 *
 * @author 张俊
 * @date Created in 2026/6/15 11:54
 */
@Slf4j
public class PreCache {

    private final ExecutorService executorService;

    public static PreCache getInstance() {
        return new PreCache();
    }

    public PreCache() {
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread t = new Thread(r, "precache-" + System.currentTimeMillis());
                    t.setDaemon(true);
                    return t;
                }
        );
    }

    public PreCache(int threadCount) {
        this.executorService = Executors.newFixedThreadPool(threadCount);
    }

    /**
     * 执行预缓存
     *
     * @param config        图层配置
     * @param wkt4326String WKT几何范围
     * @param minZoom       最小层级
     * @param maxZoom       最大层级
     */
    public void execute(PxyLayerInfo config, String wkt4326String, int minZoom, int maxZoom) {
        executePreCache(config, wkt4326String, minZoom, maxZoom, null);
    }

    public void executePreCache(PxyLayerInfo config, String wkt4326String, int minZoom, int maxZoom, ImageMime format) {
        execute(config, wkt4326String, minZoom, maxZoom, format, false);
    }

    public void executePreCheck(PxyLayerInfo config, String wkt4326String, int minZoom, int maxZoom) {
        executePreCheck(config, wkt4326String, minZoom, maxZoom, null);
    }

    public void executePreCheck(PxyLayerInfo config, String wkt4326String, int minZoom, int maxZoom, ImageMime format) {
        execute(config, wkt4326String, minZoom, maxZoom, format, true);
    }


    public void execute(PxyLayerInfo config, String wkt4326String, int minZoom, int maxZoom, ImageMime format, boolean isPreCheck) {
        if (!isCacheEnabled(config)) {
            log.warn("缓存未启用，跳过预缓存: {}", config.getLayerName());
            return;
        }

        if (format == null) {
            format = ImageMime.png;
        }
        Geometry geometry = GirAdvTools.getFormatOpt().wktToJtsGeometry(wkt4326String);


        int zoomCount = maxZoom - minZoom + 1;
        CountDownLatch latch = new CountDownLatch(zoomCount);
        AtomicInteger totalCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger checkedCount = new AtomicInteger(0);

        log.info("开始预缓存 - 图层: {}, 层级范围: {}-{}", config.getLayerName(), minZoom, maxZoom);

        for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
            Runnable task;
            if (isPreCheck) {
                task = new TileCheckAndRepairTask(
                        config.getLayerName(), zoom, geometry,
                        latch, totalCount, checkedCount, successCount, failCount, format
                );
            } else {
                task = new ZoomPreCacheTask(
                        config.getLayerName(), zoom, geometry,
                        latch, totalCount, successCount, failCount, format
                );
            }

            executorService.submit(task);
        }

        try {
            latch.await();
            if (isPreCheck) {
                log.info("预检查完成 - 图层: {}, 总瓦片: {}, 成功: {}, 失败: {},检查瓦片：{}",
                        config.getLayerName(), totalCount.get(), successCount.get(), failCount.get(), checkedCount.get());
            } else {
                log.info("预缓存完成 - 图层: {}, 总瓦片: {}, 成功: {}, 失败: {}",
                        config.getLayerName(), totalCount.get(), successCount.get(), failCount.get());
            }

        } catch (InterruptedException e) {
            log.error("预缓存被中断 - 图层: {}", config.getLayerName(), e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 检查缓存是否启用
     */
    private boolean isCacheEnabled(PxyLayerInfo config) {
        return "true".equalsIgnoreCase(config.getEnableCache())
                || "1".equals(config.getEnableCache());
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
