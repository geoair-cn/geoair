package cn.geoair.map.tile.forge.fuser;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.geoair.map.dynamic.tools.simple.GirTileResponseUtil;
import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.dynamic.tools.simple.response.TileResponseByByte;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.fuser.fuser.CacheTileFuserExec;
import cn.geoair.map.tile.forge.fuser.fuser.FuserExec;
import cn.geoair.map.tile.forge.fuser.fuser.GirFuserExecFactory;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import cn.geoair.web.mime.GiMimeType;
import cn.geoair.web.mime.GirImageMime;
import cn.geoair.web.util.GirHttpServletHelper;
import cn.geoair.web.util.GutilMimeType;
import cn.hutool.core.util.StrUtil;
import javax.servlet.http.HttpServletResponse;

/**
 * XYZ 瓦片图层叠加服务转换类
 *
 * <p>提供不同坐标系之间的瓦片请求转换和缓存管理功能
 *
 * @author 张俊
 * @date Created in 2023/12/4 15:47
 */
public class TileServiceTran {
    private static GiLogger log = GirLoggerFactory.getLogger();

    /** 默认瓦片大小 */
    private static final int DEFAULT_TILE_SIZE = 256;

    /** 默认输出格式 */
    private static final String DEFAULT_OUTPUT_FORMAT = "image/png";

    /** 默认坐标系 SRID */
    private static final int DEFAULT_SRID = 3857;

    // ==================== 公开方法 - Google 服务 ====================

    /**
     * Google 服务转 4326 请求（默认 PNG 格式）
     *
     * @param layerName 图层名称
     * @param z zoom 等级
     * @param x x 坐标
     * @param y y 坐标
     */
    public void googleServiceTo4326Request(String layerName, Integer z, Integer x, Integer y) {
        googleServiceTo4326Request(layerName, z, x, y, DEFAULT_OUTPUT_FORMAT);
    }

    /**
     * Google 服务转 4326 请求
     *
     * @param layerName 图层名称
     * @param z zoom 等级
     * @param x x 坐标
     * @param y y 坐标
     * @param outputFormat 输出格式（如 image/png）
     */
    public void googleServiceTo4326Request(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        log.debug(
                "Google服务转4326请求: layer={}, z={}, x={}, y={}, format={}",
                layerName,
                z,
                x,
                y,
                outputFormat);
        BoxReferencedEnvelope box =
                GirAdvTools.getTileGrid4326Opt().xyzToTileBox(z, x, y, DEFAULT_SRID);
        BoundingBox bounds = buildBoundingBox(box);
        processTileRequest(layerName, z, x, y, bounds, outputFormat, false, 4326);
    }

    /**
     * Google 服务转 4326 请求（删除缓存）
     *
     * @param layerName 图层名称
     * @param z zoom 等级
     * @param x x 坐标
     * @param y y 坐标
     * @param outputFormat 输出格式
     */
    public void googleServiceTo4326RequestDelCache(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        log.debug(
                "Google服务转4326请求并删除缓存: layer={}, z={}, x={}, y={}, format={}",
                layerName,
                z,
                x,
                y,
                outputFormat);
        BoxReferencedEnvelope box =
                GirAdvTools.getTileGrid4326Opt().xyzToTileBox(z, x, y, DEFAULT_SRID);
        BoundingBox bounds = buildBoundingBox(box);
        processTileRequest(layerName, z, x, y, bounds, outputFormat, true, 4326);
    }

    // ==================== 公开方法 - Grid4490 服务 ====================

    /**
     * Grid4490 服务转 3857 请求（默认 PNG 格式）
     *
     * @param layerName 图层名称
     * @param z zoom 等级
     * @param x x 坐标
     * @param y y 坐标
     */
    public void grid4490ServiceTo3857Request(String layerName, Integer z, Integer x, Integer y) {
        grid4490ServiceTo3857Request(layerName, z, x, y, DEFAULT_OUTPUT_FORMAT);
    }

    /**
     * Grid4490 服务转 3857 请求
     *
     * @param layerName 图层名称
     * @param z zoom 等级
     * @param x x 坐标
     * @param y y 坐标
     * @param outputFormat 输出格式
     */
    public void grid4490ServiceTo3857Request(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        log.debug(
                "Grid4490服务转3857请求: layer={}, z={}, x={}, y={}, format={}",
                layerName,
                z,
                x,
                y,
                outputFormat);

        BoxReferencedEnvelope box = GirAdvTools.getTileGrid3857Opt().xyzToTileBox(z, x, y, 4326);
        BoundingBox bounds =
                new BoundingBox(box.getMinX(), box.getMinY(), box.getMaxX(), box.getMaxY());
        processTileRequest(layerName, z, x, y, bounds, outputFormat, false, 3857);
    }

    /**
     * Grid4490 服务转 3857 请求（删除缓存）
     *
     * @param layerName 图层名称
     * @param z zoom 等级
     * @param x x 坐标
     * @param y y 坐标
     * @param outputFormat 输出格式
     */
    public void grid4490ServiceTo3857RequestDelCache(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        log.debug(
                "Grid4490服务转3857请求并删除缓存: layer={}, z={}, x={}, y={}, format={}",
                layerName,
                z,
                x,
                y,
                outputFormat);
        BoxReferencedEnvelope box = GirAdvTools.getTileGrid3857Opt().xyzToTileBox(z, x, y, 4326);
        BoundingBox bounds =
                new BoundingBox(box.getMinX(), box.getMinY(), box.getMaxX(), box.getMaxY());
        processTileRequest(layerName, z, x, y, bounds, outputFormat, true, 3857);
    }

    // ==================== 核心处理方法 ====================

    /**
     * 处理瓦片请求的核心方法
     *
     * @param layerName 图层名称
     * @param z zoom 等级
     * @param x x 坐标
     * @param y y 坐标
     * @param bounds 边界框
     * @param outputFormat 输出格式
     * @param deleteCache 是否删除缓存
     */
    private void processTileRequest(
            String layerName,
            Integer z,
            Integer x,
            Integer y,
            BoundingBox bounds,
            String outputFormat,
            boolean deleteCache,
            int requestGridSrid) {
        HttpServletResponse response = GirHttpServletHelper.getResponse();

        try {
            GiMimeType fromFormat = GutilMimeType.fromFormat(outputFormat);

            // 创建融合执行器
            FuserExec cacheTileFuser =
                    GirFuserExecFactory.createCachedFuser(
                            layerName,
                            z,
                            x,
                            y,
                            bounds,
                            DEFAULT_TILE_SIZE,
                            DEFAULT_TILE_SIZE,
                            (ImageMime) ImageMime.createFromFormat(fromFormat.getFormat()));

            // 如果需要删除缓存
            if (deleteCache) {
                deleteCacheForTile(layerName, z, x, y, cacheTileFuser, (GirImageMime) fromFormat);
                // 删除缓存后，重新生成瓦片
                cacheTileFuser =
                        GirFuserExecFactory.createCachedFuser(
                                layerName,
                                z,
                                x,
                                y,
                                bounds,
                                DEFAULT_TILE_SIZE,
                                DEFAULT_TILE_SIZE,
                                (ImageMime) ImageMime.createFromFormat(fromFormat.getFormat()));
            }

            // 生成瓦片
            byte[] imageBytes = cacheTileFuser.toImageBytes();

            TileResponse tileResponse =
                    TileResponseByByte.of()
                            .setBytes(imageBytes)
                            .setLastModified(System.currentTimeMillis())
                            .setSuccess(true)
                            .setMimeType(fromFormat)
                            .setDataSource("fuser")
                            .setCoordinate(TileZxyApo.of().setZ(z).setX(x).setY(y))
                            .setGridEpsgStr("EPSG:" + requestGridSrid);
            GirTileResponseUtil.buildFromTileResponse(tileResponse, response);

        } catch (Exception e) {
            String errorMsg =
                    StrUtil.format("生成瓦片失败: layerName={}, z={}, x={}, y={}", layerName, z, x, y);
            log.error(errorMsg, e);

            String fullErrorMsg = errorMsg + "，异常信息：" + e.getMessage();
            TileResponse error =
                    TileResponse.error(fullErrorMsg)
                            .setDataSource("fuser")
                            .setCoordinate(TileZxyApo.of().setZ(z).setX(x).setY(y))
                            .setGridEpsgStr("EPSG:" + requestGridSrid);
            ;
            GirTileResponseUtil.buildFromTileResponse(error, response);
        }
    }

    // ==================== 缓存管理方法 ====================

    /**
     * 删除瓦片缓存
     *
     * @param layerName 图层名称
     * @param z zoom 等级
     * @param x x 坐标
     * @param y y 坐标
     * @param cacheFuser 缓存融合执行器
     * @param imageFormat 图片格式
     */
    public void deleteCacheForTile(
            String layerName,
            Integer z,
            Integer x,
            Integer y,
            FuserExec cacheFuser,
            GirImageMime imageFormat) {
        try {
            // 删除当前瓦片缓存
            if (cacheFuser instanceof CacheTileFuserExec) {
                FuserCacheUtils.deleteCacheByRequestGrid(
                        layerName, z, x, y, (CacheTileFuserExec) cacheFuser, imageFormat);
                log.debug("删除当前瓦片缓存: layer={}, z={}, x={}, y={}", layerName, z, x, y);
            }
        } catch (Exception e) {
            log.error("删除瓦片缓存失败: layer={}, z={}, x={}, y={}", layerName, z, x, y, e);
        }
    }

    /**
     * 删除瓦片缓存（公开方法，用于外部调用）
     *
     * @param layerName 图层名称
     * @param z zoom 等级
     * @param x x 坐标
     * @param y y 坐标
     * @param bounds 边界框
     * @param outputFormat 输出格式
     */
    public void delCache(
            String layerName,
            Integer z,
            Integer x,
            Integer y,
            BoundingBox bounds,
            String outputFormat) {
        log.info("删除瓦片缓存: layer={}, z={}, x={}, y={}, format={}", layerName, z, x, y, outputFormat);

        try {
            GiMimeType fromFormat = GutilMimeType.fromFormat(outputFormat);
            CacheTileFuserExec cacheTileFuser =
                    GirFuserExecFactory.createCachedFuser(
                            layerName,
                            z,
                            x,
                            y,
                            bounds,
                            DEFAULT_TILE_SIZE,
                            DEFAULT_TILE_SIZE,
                            (ImageMime) fromFormat);

            // 删除周边瓦片缓存
            FuserCacheUtils.deleteCacheByRequestGrid(
                    layerName, z, x, y, cacheTileFuser, (GirImageMime) fromFormat);

            log.info("删除瓦片缓存完成: layer={}, z={}, x={}, y={}", layerName, z, x, y);

        } catch (Exception e) {
            log.error("删除瓦片缓存失败: layer={}, z={}, x={}, y={}", layerName, z, x, y, e);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 构建边界框
     *
     * @param box 坐标盒子
     * @return 边界框
     */
    public BoundingBox buildBoundingBox(BoxReferencedEnvelope box) {
        return new BoundingBox(box.getMinX(), box.getMinY(), box.getMaxX(), box.getMaxY());
    }
}
