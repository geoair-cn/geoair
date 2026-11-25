package cn.geoair.map.tile.forge.fuser.precache;

import static cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils.ORIGINAL_GRID_SUFFIX;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.web.mime.GirImageMime;

import org.locationtech.jts.geom.Geometry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 预缓存逻辑
 *
 * @author 张俊
 * @date Created in 2026/6/15 11:54
 */
public class PreCache {
    private static GiLogger log = GirLoggerFactory.getLogger();
    private static final int MAX_ZOOM_TASKS_PER_EXECUTION = 64;
    private final ExecutorService executorService;
    private final int maxConcurrentTiles;

    public static PreCache getInstance() {
        return new PreCache();
    }

    public PreCache() {
        this(Math.max(1, Runtime.getRuntime().availableProcessors()));
    }

    /**
     * @param maxConcurrentTiles 同一时刻最多处理的瓦片数；层级会顺序调度，避免嵌套线程池放大并发。
     */
    public PreCache(int maxConcurrentTiles) {
        if (maxConcurrentTiles <= 0) {
            throw new IllegalArgumentException("maxConcurrentTiles 必须大于 0");
        }
        this.maxConcurrentTiles = maxConcurrentTiles;
        this.executorService =
                Executors.newSingleThreadExecutor(
                        r -> {
                            Thread thread = new Thread(r, "precache-coordinator");
                            thread.setDaemon(true);
                            return thread;
                        });
    }

    // ==================== 普通预缓存 ====================

    /**
     * 执行预缓存
     *
     * @param config 图层配置
     * @param wkt4326String WKT几何范围
     * @param minZoom 最小层级
     * @param maxZoom 最大层级
     */
    public void execute(PxyLayerInfo config, String wkt4326String, int minZoom, int maxZoom) {
        executePreCache(config, wkt4326String, minZoom, maxZoom, null);
    }

    public void executePreCache(
            PxyLayerInfo config,
            String wkt4326String,
            int minZoom,
            int maxZoom,
            GirImageMime format) {
        execute(config, wkt4326String, minZoom, maxZoom, format, false, false, null);
    }

    public void executePreCheck(
            PxyLayerInfo config, String wkt4326String, int minZoom, int maxZoom) {
        executePreCheck(config, wkt4326String, minZoom, maxZoom, null);
    }

    public void executePreCheck(
            PxyLayerInfo config,
            String wkt4326String,
            int minZoom,
            int maxZoom,
            GirImageMime format) {
        execute(config, wkt4326String, minZoom, maxZoom, format, true, false, null);
    }

    // ==================== 原始网格预缓存 ====================

    /**
     * 执行原始网格预缓存
     *
     * @param config 图层配置
     * @param wkt4326String WKT几何范围
     * @param minZoom 最小层级
     * @param maxZoom 最大层级
     */
    public void executeOriginalPreCache(
            PxyLayerInfo config, String wkt4326String, int minZoom, int maxZoom) {
        executeOriginalPreCache(config, wkt4326String, minZoom, maxZoom, null);
    }

    public void executeOriginalPreCache(
            PxyLayerInfo config,
            String wkt4326String,
            int minZoom,
            int maxZoom,
            GirImageMime format) {
        executeOriginalPreCache(config, wkt4326String, minZoom, maxZoom, format, null);
    }

    public void executeOriginalPreCache(
            PxyLayerInfo config,
            String wkt4326String,
            int minZoom,
            int maxZoom,
            GirImageMime format,
            String originalCacheName) {
        execute(config, wkt4326String, minZoom, maxZoom, format, false, true, originalCacheName);
    }

    /**
     * 执行原始网格预检查
     *
     * @param config 图层配置
     * @param wkt4326String WKT几何范围
     * @param minZoom 最小层级
     * @param maxZoom 最大层级
     */
    public void executeOriginalPreCheck(
            PxyLayerInfo config, String wkt4326String, int minZoom, int maxZoom) {
        executeOriginalPreCheck(config, wkt4326String, minZoom, maxZoom, null);
    }

    public void executeOriginalPreCheck(
            PxyLayerInfo config,
            String wkt4326String,
            int minZoom,
            int maxZoom,
            GirImageMime format) {
        executeOriginalPreCheck(config, wkt4326String, minZoom, maxZoom, format, null);
    }

    public void executeOriginalPreCheck(
            PxyLayerInfo config,
            String wkt4326String,
            int minZoom,
            int maxZoom,
            GirImageMime format,
            String originalCacheName) {
        execute(config, wkt4326String, minZoom, maxZoom, format, true, true, originalCacheName);
    }

    // ==================== 统一执行方法 ====================

    /**
     * 统一执行方法
     *
     * @param config 图层配置
     * @param wkt4326String WKT几何范围
     * @param minZoom 最小层级
     * @param maxZoom 最大层级
     * @param format 图片格式
     * @param isPreCheck 是否为检查修复模式
     * @param isOriginalGrid 是否为原始网格
     * @param originalCacheName 原始网格缓存名（可为null，使用默认）
     */
    public void execute(
            PxyLayerInfo config,
            String wkt4326String,
            int minZoom,
            int maxZoom,
            GirImageMime format,
            boolean isPreCheck,
            boolean isOriginalGrid,
            String originalCacheName) {
        if (config == null
                || minZoom > maxZoom
                || (long) maxZoom - minZoom + 1 > MAX_ZOOM_TASKS_PER_EXECUTION) {
            throw new IllegalArgumentException("预缓存图层配置或层级范围无效");
        }
        if (!isCacheEnabled(config)) {
            log.warn("缓存未启用，跳过预缓存: {}", config.getLayerName());
            return;
        }

        if (format == null) {
            format = GirImageMime.png;
        }

        // 处理原始网格缓存名
        if (isOriginalGrid && (originalCacheName == null || originalCacheName.isEmpty())) {
            originalCacheName = config.getLayerName() + ORIGINAL_GRID_SUFFIX;
        }

        Geometry geometry = GirAdvTools.getFormatOpt().wktToJtsGeometry(wkt4326String);

        int zoomCount = maxZoom - minZoom + 1;
        CountDownLatch latch = new CountDownLatch(zoomCount);
        AtomicLong totalCount = new AtomicLong(0);
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failCount = new AtomicLong(0);
        AtomicLong checkedCount = new AtomicLong(0);
        AtomicLong repairedCount = new AtomicLong(0);

        String taskTypeDesc = buildTaskTypeDesc(isPreCheck, isOriginalGrid);
        log.info(
                "开始{} - 图层: {}, 层级范围: {}-{}",
                taskTypeDesc,
                config.getLayerName(),
                minZoom,
                maxZoom);

        for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
            Runnable task =
                    buildTask(
                            config,
                            zoom,
                            geometry,
                            format,
                            isPreCheck,
                            isOriginalGrid,
                            originalCacheName,
                            latch,
                            totalCount,
                            successCount,
                            failCount,
                            checkedCount,
                            repairedCount,
                            maxConcurrentTiles);
            executorService.submit(task);
        }

        try {
            latch.await();
            logResult(
                    config.getLayerName(),
                    isPreCheck,
                    isOriginalGrid,
                    totalCount,
                    successCount,
                    failCount,
                    checkedCount,
                    repairedCount);
        } catch (InterruptedException e) {
            log.error("{}被中断 - 图层: {}", taskTypeDesc, config.getLayerName(), e);
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 构建任务 ====================

    /** 构建任务 */
    private Runnable buildTask(
            PxyLayerInfo config,
            int zoom,
            Geometry geometry,
            GirImageMime format,
            boolean isPreCheck,
            boolean isOriginalGrid,
            String originalCacheName,
            CountDownLatch latch,
            AtomicLong totalCount,
            AtomicLong successCount,
            AtomicLong failCount,
            AtomicLong checkedCount,
            AtomicLong repairedCount,
            int maxConsumerThreads) {
        String layerName = config.getLayerName();

        if (isOriginalGrid) {
            // 原始网格任务
            if (isPreCheck) {
                return new TileOriginalCheckAndRepairTask(
                        layerName,
                        originalCacheName,
                        zoom,
                        geometry,
                        latch,
                        totalCount,
                        checkedCount,
                        repairedCount,
                        failCount,
                        format,
                        maxConsumerThreads);
            } else {
                return new TileOriginalPreCacheTask(
                        layerName,
                        originalCacheName,
                        zoom,
                        geometry,
                        latch,
                        totalCount,
                        successCount,
                        failCount,
                        format,
                        maxConsumerThreads);
            }
        } else {
            // 普通任务
            if (isPreCheck) {
                return new TileFuserCheckAndRepairTask(
                        layerName,
                        zoom,
                        geometry,
                        latch,
                        totalCount,
                        checkedCount,
                        repairedCount,
                        failCount,
                        format,
                        maxConsumerThreads);
            } else {
                return new TileFuserPreCacheTask(
                        layerName,
                        zoom,
                        geometry,
                        latch,
                        totalCount,
                        successCount,
                        failCount,
                        format,
                        maxConsumerThreads);
            }
        }
    }

    // ==================== 辅助方法 ====================

    /** 构建任务类型描述 */
    private String buildTaskTypeDesc(boolean isPreCheck, boolean isOriginalGrid) {
        if (isOriginalGrid) {
            return isPreCheck ? "原始网格检查修复" : "原始网格预缓存";
        } else {
            return isPreCheck ? "检查修复" : "预缓存";
        }
    }

    /** 记录结果日志 */
    private void logResult(
            String layerName,
            boolean isPreCheck,
            boolean isOriginalGrid,
            AtomicLong totalCount,
            AtomicLong successCount,
            AtomicLong failCount,
            AtomicLong checkedCount,
            AtomicLong repairedCount) {
        String taskType = buildTaskTypeDesc(isPreCheck, isOriginalGrid);

        if (isPreCheck) {
            log.info(
                    "{}完成 - 图层: {}, 总瓦片: {}, 已检查: {}, 已修复: {}, 失败: {}",
                    taskType,
                    layerName,
                    totalCount.get(),
                    checkedCount.get(),
                    repairedCount.get(),
                    failCount.get());
        } else {
            log.info(
                    "{}完成 - 图层: {}, 总瓦片: {}, 成功: {}, 失败: {}",
                    taskType,
                    layerName,
                    totalCount.get(),
                    successCount.get(),
                    failCount.get());
        }
    }

    /** 检查缓存是否启用 */
    private boolean isCacheEnabled(PxyLayerInfo config) {
        return "true".equalsIgnoreCase(config.getEnableCache())
                || "1".equals(config.getEnableCache());
    }

    /** 关闭线程池 */
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
