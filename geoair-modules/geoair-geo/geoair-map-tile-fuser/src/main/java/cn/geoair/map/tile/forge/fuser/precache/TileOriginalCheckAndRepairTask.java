package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.web.mime.GirImageMime;
import org.locationtech.jts.geom.Geometry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 原始网格预缓存任务（简化版）
 *
 * @author 张俊
 */
public class TileOriginalCheckAndRepairTask implements Runnable {

    private final TileTaskExecutor executor;

    public TileOriginalCheckAndRepairTask(String layerName, String originalCacheName,
                                           int zoom, Geometry geometry4326,
                                           CountDownLatch latch, AtomicLong totalCount,
                                           AtomicLong checkedCount, AtomicLong repairedCount,
                                           AtomicLong failCount, GirImageMime format) {
        TileTaskConfig config = TileTaskConfig.forOriginalCheckAndRepair(layerName, originalCacheName, zoom, geometry4326, format)
                .setLatch(latch)
                .setTotalCount(totalCount)
                .setCheckedCount(checkedCount)
                .setRepairedCount(repairedCount)
                .setFailCount(failCount);
        this.executor = TileTaskExecutor.forOriginalCheckAndRepair(config);
    }

    /**
     * 便捷创建方法
     */
    public static TileOriginalCheckAndRepairTask of(String layerName, String originalCacheName,
                                                     int zoom, Geometry geometry4326, GirImageMime format) {
        return new TileOriginalCheckAndRepairTask(layerName, originalCacheName, zoom, geometry4326,
                null, null, null, null, null, format);
    }

    /**
     * 便捷创建方法（使用默认缓存名）
     */
    public static TileOriginalCheckAndRepairTask of(String layerName, int zoom, Geometry geometry4326, GirImageMime format) {
        return new TileOriginalCheckAndRepairTask(layerName, null, zoom, geometry4326,
                null, null, null, null, null, format);
    }

    /**
     * 链式构建
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void run() {
        executor.execute();
    }

    public static class Builder {
        private String layerName;
        private String originalCacheName;
        private int zoom;
        private Geometry geometry4326;
        private GirImageMime format;
        private CountDownLatch latch;
        private AtomicLong totalCount;
        private AtomicLong checkedCount;
        private AtomicLong repairedCount;
        private AtomicLong failCount;

        public Builder layerName(String layerName) {
            this.layerName = layerName;
            return this;
        }

        public Builder originalCacheName(String originalCacheName) {
            this.originalCacheName = originalCacheName;
            return this;
        }

        public Builder zoom(int zoom) {
            this.zoom = zoom;
            return this;
        }

        public Builder geometry4326(Geometry geometry4326) {
            this.geometry4326 = geometry4326;
            return this;
        }

        public Builder format(GirImageMime format) {
            this.format = format;
            return this;
        }

        public Builder latch(CountDownLatch latch) {
            this.latch = latch;
            return this;
        }

        public Builder totalCount(AtomicLong totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public Builder checkedCount(AtomicLong checkedCount) {
            this.checkedCount = checkedCount;
            return this;
        }

        public Builder repairedCount(AtomicLong repairedCount) {
            this.repairedCount = repairedCount;
            return this;
        }

        public Builder failCount(AtomicLong failCount) {
            this.failCount = failCount;
            return this;
        }

        public TileOriginalCheckAndRepairTask build() {
            return new TileOriginalCheckAndRepairTask(layerName, originalCacheName, zoom, geometry4326,
                    latch, totalCount, checkedCount, repairedCount, failCount, format);
        }
    }
}
