package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.fuser.CacheTileFuserExec;
import cn.geoair.map.tile.forge.fuser.fuser.GirFuserExecFactory;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;

import cn.geoair.map.tile.forge.fuser.utils.TileBlankDetector;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 瓦片异常检查与修复任务
 * 只检查已存在的瓦片，发现异常（如空白矩形）则重新切片
 *
 * @author 张俊
 * @date Created in 2026/6/23
 */
@Slf4j
public class TileCheckAndRepairTask implements Runnable {

    private final String layerName;
    private final int zoom;
    private final Geometry geometry4326;
    private final CountDownLatch latch;
    private final AtomicInteger totalCount;
    private final AtomicInteger checkedCount;
    private final AtomicInteger repairedCount;
    private final AtomicInteger failCount;
    private final ImageMime format;

    public TileCheckAndRepairTask(String layerName,
                                  int zoom,
                                  Geometry geometry4326,
                                  CountDownLatch latch,
                                  AtomicInteger totalCount,
                                  AtomicInteger checkedCount,
                                  AtomicInteger repairedCount,
                                  AtomicInteger failCount,
                                  ImageMime format) {
        this.layerName = layerName;
        this.zoom = zoom;
        this.geometry4326 = geometry4326;
        this.latch = latch;
        this.totalCount = totalCount;
        this.checkedCount = checkedCount;
        this.repairedCount = repairedCount;
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

            log.info("瓦片检查任务开始 - 层级: {}, X范围: [{}, {}], Y范围: [{}, {}], 总瓦片数: {}",
                    zoom, minX, maxX, minY, maxY, totalTiles);

            int zoomChecked = 0;
            int zoomRepaired = 0;
            int zoomFail = 0;
            int zoomSkipped = 0;

            // 遍历所有瓦片
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    try {
                        // 1. 检查瓦片是否在几何范围内
                        BoxReferencedEnvelope box = GirAdvTools.getTileGrid4326Opt().xyzToTileBox(zoom, x, y, 3857);
                        String wktString = box.getWktString(4326);
                        Geometry geometryByBox = GirAdvTools.getFormatOpt().wktToJtsGeometry(wktString);
                        if (!geometry4326.intersects(geometryByBox)) {
                            continue;
                        }

                        // 2. 检查瓦片缓存是否存在
                        BoundingBox bounds = new BoundingBox(box.getMinX(), box.getMinY(), box.getMaxX(), box.getMaxY());
                        CacheTileFuserExec cacheTileFuser = GirFuserExecFactory.createCachedFuser(
                                layerName, zoom, x, y, bounds, 256, 256, ImageMime.png);
                        TileCache tileCache = cacheTileFuser.getTileCache();

                        // 3. 只检查已存在的瓦片
                        if (!tileCache.exists(layerName, zoom, x, y, format)) {
                            zoomSkipped++;
                            continue;
                        }

                        // 4. 获取缓存瓦片并进行检查
                        byte[] bytes = tileCache.get(layerName, zoom, x, y, format);
                        zoomChecked++;
                        checkedCount.incrementAndGet();

                        // 5. 检测是否为异常瓦片（空白矩形）
                        if (TileBlankDetector.hasLargeBlankRect(bytes, format.getInternalName())) {
                            log.info("检测到异常瓦片（空白矩形）: z={}, x={}, y={}, 开始重新切片", zoom, x, y);

                            // 6. 删除异常缓存
                            FuserCacheUtils.deleteCacheByRequestGrid(layerName, zoom, x, y, cacheTileFuser, format);

                            // 7. 重新生成瓦片
                            byte[] newImageBytes = cacheTileFuser.toImageBytes();
                            if (newImageBytes != null && newImageBytes.length > 0) {
                                zoomRepaired++;
                                repairedCount.incrementAndGet();
                                log.info("瓦片重新切片成功: z={}, x={}, y={}", zoom, x, y);
                            } else {
                                zoomFail++;
                                failCount.incrementAndGet();
                                log.warn("瓦片重新切片失败（生成空数据）: z={}, x={}, y={}", zoom, x, y);
                            }
                        } else {
                            // 瓦片正常，无需处理
                            if (log.isDebugEnabled()) {
                                log.debug("瓦片正常: z={}, x={}, y={}", zoom, x, y);
                            }
                        }

                    } catch (Exception e) {
                        zoomFail++;
                        failCount.incrementAndGet();
                        log.error("检查瓦片异常: z={}, x={}, y={}, 错误: {}", zoom, x, y, e.getMessage(), e);
                    }
                }
            }

            log.info("瓦片检查任务完成 - 层级: {}, 已检查: {}, 已修复: {}, 失败: {}, 跳过(不存在): {}",
                    zoom, zoomChecked, zoomRepaired, zoomFail, zoomSkipped);

        } catch (Exception e) {
            log.error("瓦片检查任务失败 - 层级: {}, 错误: {}", zoom, e.getMessage(), e);
        } finally {
            if (latch != null) {
                latch.countDown();
            }
        }
    }
}
