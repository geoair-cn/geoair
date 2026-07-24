package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.web.mime.GirImageMime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import org.locationtech.jts.geom.Geometry;

/**
 * 单层级融合预缓存任务（简化版）
 *
 * @author 张俊
 */
public class TileFuserPreCacheTask implements Runnable {

    private final TileTaskExecutor executor;

    public TileFuserPreCacheTask(
            String layerName,
            int zoom,
            Geometry geometry4326,
            CountDownLatch latch,
            AtomicLong totalCount,
            AtomicLong successCount,
            AtomicLong failCount,
            GirImageMime format) {
        TileTaskConfig config =
                TileTaskConfig.forPreCache(layerName, zoom, geometry4326, format)
                        .setLatch(latch)
                        .setTotalCount(totalCount)
                        .setSuccessCount(successCount)
                        .setFailCount(failCount);
        this.executor = TileTaskExecutor.forPreCache(config);
    }

    /** 便捷创建方法 */
    public static TileFuserPreCacheTask of(
            String layerName, int zoom, Geometry geometry4326, GirImageMime format) {
        return new TileFuserPreCacheTask(
                layerName, zoom, geometry4326, null, null, null, null, format);
    }

    /** 链式构建 */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void run() {
        executor.execute();
    }

    public static class Builder {
        private String layerName;
        private int zoom;
        private Geometry geometry4326;
        private GirImageMime format;
        private CountDownLatch latch;
        private AtomicLong totalCount;
        private AtomicLong successCount;
        private AtomicLong failCount;

        public Builder layerName(String layerName) {
            this.layerName = layerName;
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

        public Builder successCount(AtomicLong successCount) {
            this.successCount = successCount;
            return this;
        }

        public Builder failCount(AtomicLong failCount) {
            this.failCount = failCount;
            return this;
        }

        public TileFuserPreCacheTask build() {
            return new TileFuserPreCacheTask(
                    layerName,
                    zoom,
                    geometry4326,
                    latch,
                    totalCount,
                    successCount,
                    failCount,
                    format);
        }
    }
}
