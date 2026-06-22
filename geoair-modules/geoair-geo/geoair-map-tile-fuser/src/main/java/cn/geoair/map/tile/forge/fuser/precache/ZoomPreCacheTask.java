package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.dynamic.tools.GirAdvTools;

import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.fuser.fuser.CacheTileFuserExec;
import cn.geoair.map.tile.forge.fuser.fuser.GirFuserExecFactory;
import lombok.extern.slf4j.Slf4j;

import org.locationtech.jts.geom.Geometry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单层级预缓存任务
 *
 * @author 张俊
 * @date Created in 2026/6/15
 */
@Slf4j
public class ZoomPreCacheTask implements Runnable {

    private final String layerName;


    private final int zoom;
    private final Geometry geometry4326;
    private final CountDownLatch latch;
    private final AtomicInteger totalCount;
    private final AtomicInteger successCount;
    private final AtomicInteger failCount;
    private final ImageMime format;

    public ZoomPreCacheTask(String layerName,
                            int zoom, Geometry geometry4326, CountDownLatch latch,
                            AtomicInteger totalCount, AtomicInteger successCount,
                            AtomicInteger failCount, ImageMime format) {
        this.layerName = layerName;

        this.zoom = zoom;
        this.geometry4326 = geometry4326;
        this.latch = latch;
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.failCount = failCount;
        this.format = format;
    }

    @Override
    public void run() {
        try {
            // 计算当前层级的瓦片范围
            RangeApo rangeApo = GirAdvTools.getTileGrid4326Opt().tileRangeByGeom(zoom, geometry4326);
            int minX = rangeApo.getMinX();
            int maxX = rangeApo.getMaxX();
            int minY = rangeApo.getMinY();
            int maxY = rangeApo.getMaxY();

            int totalTiles = (maxX - minX + 1) * (maxY - minY + 1);
            totalCount.addAndGet(totalTiles);

            log.info("预缓存层级: {}, X范围: [{}, {}], Y范围: [{}, {}], 瓦片数: {}",
                    zoom, minX, maxX, minY, maxY, totalTiles);

            int zoomSuccess = 0;
            int zoomFail = 0;

            // 遍历所有瓦片
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    try {
                        BoxReferencedEnvelope box = GirAdvTools.getTileGrid4326Opt().xyzToTileBox(zoom, x, y, 3857);
                        String wktString = box.getWktString(4326);
                        Geometry geometryByBox = GirAdvTools.getFormatOpt().wktToJtsGeometry(wktString);
                        if (!geometry4326.intersects(geometryByBox)) { // 包含或者相交都算
                            continue;
                        }
                        BoundingBox bounds = new BoundingBox(box.getMinX(), box.getMinY(), box.getMaxX(), box.getMaxY());
                        CacheTileFuserExec cacheTileFuser = GirFuserExecFactory.createCachedFuser(layerName, zoom, x, y, bounds, 256, 256, ImageMime.png);
                        // 检查缓存是否已存在
                        if (cacheTileFuser.getTileCache().exists(layerName, zoom, x, y, format)) {
                            zoomSuccess++;
                            successCount.incrementAndGet();
                            continue;
                        }
                        byte[] imageBytes = cacheTileFuser.toImageBytes();
                        if (imageBytes != null) {
                            zoomSuccess++;
                            successCount.incrementAndGet();
                        } else {
                            zoomFail++;
                            failCount.incrementAndGet();
                            log.warn("预缓存瓦片失败（资源为空）: {}-({},{},{})", layerName, zoom, x, y);
                        }
                    } catch (Exception e) {
                        zoomFail++;
                        failCount.incrementAndGet();
                        log.error("预缓存瓦片异常: {}-({},{},{})", layerName, zoom, x, y, e);
                    }
                }
            }

            log.info("层级预缓存完成: {}, 成功: {}, 失败: {}", zoom, zoomSuccess, zoomFail);
        } catch (Exception e) {
            log.error("预缓存层级失败: {}, 错误: {}", zoom, e.getMessage(), e);
        } finally {
            if (latch != null) {
                latch.countDown();
            }
        }
    }
}
