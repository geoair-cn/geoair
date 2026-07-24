package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.web.mime.GirImageMime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.locationtech.jts.geom.Geometry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 瓦片任务配置 使用链式调用构建
 *
 * @author 张俊
 */
@Getter
@Setter
@Accessors(chain = true)
@Builder
public class TileTaskConfig {
    // 必填参数
    private String layerName;
    private int zoom;
    private Geometry geometry4326;
    private GirImageMime format;
    private TaskType taskType;
    // 可选参数（原始网格检查需要）
    private String originalCacheName;

    // 统计计数器
    private CountDownLatch latch;
    @Builder.Default private AtomicLong totalCount = new AtomicLong(0);
    @Builder.Default private AtomicLong successCount = new AtomicLong(0);
    @Builder.Default private AtomicLong failCount = new AtomicLong(0);
    @Builder.Default private AtomicLong checkedCount = new AtomicLong(0);
    @Builder.Default private AtomicLong repairedCount = new AtomicLong(0);

    /** 快速创建预缓存任务配置 */
    public static TileTaskConfig forPreCache(
            String layerName, int zoom, Geometry geometry4326, GirImageMime format) {
        return TileTaskConfig.builder()
                .layerName(layerName)
                .zoom(zoom)
                .geometry4326(geometry4326)
                .format(format)
                .taskType(TaskType.PRE_CACHE)
                .build();
    }

    /** 快速创建检查修复任务配置 */
    public static TileTaskConfig forCheckAndRepair(
            String layerName, int zoom, Geometry geometry4326, GirImageMime format) {
        return TileTaskConfig.builder()
                .layerName(layerName)
                .zoom(zoom)
                .geometry4326(geometry4326)
                .format(format)
                .taskType(TaskType.CHECK_REPAIR)
                .build();
    }

    /** 快速创建原始网格检查修复任务配置 */
    public static TileTaskConfig forOriginalCheckAndRepair(
            String layerName,
            String originalCacheName,
            int zoom,
            Geometry geometry4326,
            GirImageMime format) {
        return TileTaskConfig.builder()
                .layerName(layerName)
                .zoom(zoom)
                .geometry4326(geometry4326)
                .format(format)
                .originalCacheName(originalCacheName)
                .taskType(TaskType.ORIGINAL_CHECK_REPAIR)
                .build();
    }

    /** 快速创建原始网格预缓存任务配置 */
    public static TileTaskConfig forOriginalPreCache(
            String layerName,
            String originalCacheName,
            int zoom,
            Geometry geometry4326,
            GirImageMime format) {
        return TileTaskConfig.builder()
                .layerName(layerName)
                .zoom(zoom)
                .geometry4326(geometry4326)
                .format(format)
                .originalCacheName(originalCacheName)
                .taskType(TaskType.ORIGINAL_PRE_CACHE)
                .build();
    }
}
