package com.tc.tools.geowebcache.fuser.precache;

import cn.geoair.map.dynamic.tools.GirAdvTools;

import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import com.tc.tools.geowebcache.fuser.fuser.CacheTileFuserExec;
import com.tc.tools.geowebcache.fuser.fuser.GtcFuserExecFactory;
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
    private final Geometry geometry;
    private final CountDownLatch latch;
    private final AtomicInteger totalCount;
    private final AtomicInteger successCount;
    private final AtomicInteger failCount;

    public ZoomPreCacheTask(String layerName,
                            int zoom, Geometry geometry, CountDownLatch latch,
                            AtomicInteger totalCount, AtomicInteger successCount,
                            AtomicInteger failCount) {
        this.layerName = layerName;

        this.zoom = zoom;
        this.geometry = geometry;
        this.latch = latch;
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.failCount = failCount;
    }

    @Override
    public void run() {
        try {
            // 计算当前层级的瓦片范围
            RangeApo rangeApo = GirAdvTools.getTileGrid4326Opt().tileRangeByGeom(zoom, geometry);
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
                        BoundingBox bounds = new BoundingBox(box.getMinX(), box.getMinY(), box.getMaxX(), box.getMaxY());
                        CacheTileFuserExec cacheTileFuser = GtcFuserExecFactory.createCachedFuser(layerName, zoom, x, y, bounds, 256, 256, ImageMime.png);
                        // 检查缓存是否已存在
                        if (cacheTileFuser.getTileCache().exists(layerName, zoom, x, y)) {
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
