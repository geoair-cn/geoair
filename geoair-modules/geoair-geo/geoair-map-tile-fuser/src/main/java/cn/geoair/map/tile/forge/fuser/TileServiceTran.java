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
import jakarta.servlet.http.HttpServletResponse;
import cn.hutool.core.util.URLUtil;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;


/**
 * XYZ 瓦片图层叠加服务转换类
 * <p>
 * 提供不同坐标系之间的瓦片请求转换和缓存管理功能
 * </p>
 *
 * @author 张俊
 * @date Created in 2023/12/4 15:47
 */

public class TileServiceTran implements TileServiceTranResponseProvider {
    private static GiLogger log = GirLoggerFactory.getLogger();

    /**
     * 默认瓦片大小
     */
    private static final int DEFAULT_TILE_SIZE = 256;

    /**
     * 默认输出格式
     */
    private static final String DEFAULT_OUTPUT_FORMAT = "image/png";

    /**
     * 默认坐标系 SRID
     */
    private static final int DEFAULT_SRID = 3857;

    /** 手工 URI 中表示 Google 网格转 4326 网格的操作名。 */
    public static final String GOOGLE_TO_4326_OPERATION = "google-to-4326";

    /** 手工 URI 中表示 Grid4490 网格转 3857 网格的操作名。 */
    public static final String GRID4490_TO_3857_OPERATION = "grid4490-to-3857";

    private static final String URI_PATH_PREFIX = "tile-fuser";

    /**
     * 构建可直接传入 {@link #getTileResponse(String)} 的标准 URI。
     *
     * <p>格式：{@code /tile-fuser/{operation}/{layer}/{z}/{x}/{y}?format=image%2Fpng&deleteCache=false}</p>
     */
    public static String buildTileRequestUri(String operation, String layerName, Integer z, Integer x, Integer y,
                                             String outputFormat, boolean deleteCache) {
        if (!GOOGLE_TO_4326_OPERATION.equals(operation) && !GRID4490_TO_3857_OPERATION.equals(operation)) {
            throw new IllegalArgumentException("Unsupported tile-fuser operation: " + operation);
        }
        if (StrUtil.isBlank(layerName) || z == null || x == null || y == null) {
            throw new IllegalArgumentException("layerName, z, x and y must not be empty");
        }
        String format = StrUtil.isBlank(outputFormat) ? DEFAULT_OUTPUT_FORMAT : outputFormat;
        return "/" + URI_PATH_PREFIX + "/" + operation + "/" + urlEncode(layerName)
                + "/" + z + "/" + x + "/" + y
                + "?format=" + urlEncode(format) + "&deleteCache=" + deleteCache;
    }

    /**
     * 解析由 {@link #buildTileRequestUri(String, String, Integer, Integer, Integer, String, boolean)}
     * 构建的 URI 或完整 URL，并生成瓦片响应。
     */
    @Override
    public TileResponse getTileResponse(String requestUri) {
        try {
            ParsedTileRequest parsed = ParsedTileRequest.parse(requestUri);
            if (parsed == null) {
                return TileResponse.notFound()
                        .setHttpCode(HttpServletResponse.SC_NOT_FOUND)
                        .setErrorMessage("Invalid tile-fuser URI: " + requestUri);
            }
            if (GOOGLE_TO_4326_OPERATION.equals(parsed.operation)) {
                return buildConvertedTileResponse(parsed.layerName, parsed.z, parsed.x, parsed.y,
                        parsed.outputFormat, parsed.deleteCache, true);
            }
            return buildConvertedTileResponse(parsed.layerName, parsed.z, parsed.x, parsed.y,
                    parsed.outputFormat, parsed.deleteCache, false);
        } catch (Exception e) {
            log.error("解析 tile-fuser URI 失败: {}", requestUri, e);
            return TileResponse.error("Failed to parse tile-fuser URI: " + e.getMessage());
        }
    }


    // ==================== 公开方法 - Google 服务 ====================

    /**
     * Google 服务转 4326 请求（默认 PNG 格式）
     *
     * @param layerName 图层名称
     * @param z         zoom 等级
     * @param x         x 坐标
     * @param y         y 坐标
     */
    public void googleServiceTo4326Request(String layerName, Integer z, Integer x, Integer y) {
        writeTileResponse(googleServiceTo4326RequestForTileResponse(layerName, z, x, y));
    }

    /**
     * Google 服务转 4326 请求，并返回统一瓦片响应（默认 PNG 格式）。
     */
    @Override
    public TileResponse googleServiceTo4326RequestForTileResponse(String layerName, Integer z, Integer x, Integer y) {
        return googleServiceTo4326RequestForTileResponse(layerName, z, x, y, DEFAULT_OUTPUT_FORMAT);
    }

    /**
     * Google 服务转 4326 请求
     *
     * @param layerName    图层名称
     * @param z            zoom 等级
     * @param x            x 坐标
     * @param y            y 坐标
     * @param outputFormat 输出格式（如 image/png）
     */
    public void googleServiceTo4326Request(String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        writeTileResponse(googleServiceTo4326RequestForTileResponse(layerName, z, x, y, outputFormat));
    }

    /**
     * Google 服务转 4326 请求，并返回统一瓦片响应。
     */
    @Override
    public TileResponse googleServiceTo4326RequestForTileResponse(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        log.debug("Google服务转4326请求: layer={}, z={}, x={}, y={}, format={}",
                layerName, z, x, y, outputFormat);
        return buildConvertedTileResponse(layerName, z, x, y, outputFormat, false, true);
    }

    /**
     * Google 服务转 4326 请求（删除缓存）
     *
     * @param layerName    图层名称
     * @param z            zoom 等级
     * @param x            x 坐标
     * @param y            y 坐标
     * @param outputFormat 输出格式
     */
    public void googleServiceTo4326RequestDelCache(String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        writeTileResponse(googleServiceTo4326RequestDelCacheForTileResponse(layerName, z, x, y, outputFormat));
    }

    /**
     * Google 服务转 4326 请求、删除缓存后返回统一瓦片响应。
     */
    @Override
    public TileResponse googleServiceTo4326RequestDelCacheForTileResponse(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        log.debug("Google服务转4326请求并删除缓存: layer={}, z={}, x={}, y={}, format={}",
                layerName, z, x, y, outputFormat);
        return buildConvertedTileResponse(layerName, z, x, y, outputFormat, true, true);
    }

    // ==================== 公开方法 - Grid4490 服务 ====================

    /**
     * Grid4490 服务转 3857 请求（默认 PNG 格式）
     *
     * @param layerName 图层名称
     * @param z         zoom 等级
     * @param x         x 坐标
     * @param y         y 坐标
     */
    public void grid4490ServiceTo3857Request(String layerName, Integer z, Integer x, Integer y) {
        writeTileResponse(grid4490ServiceTo3857RequestForTileResponse(layerName, z, x, y));
    }

    /**
     * Grid4490 服务转 3857 请求，并返回统一瓦片响应（默认 PNG 格式）。
     */
    @Override
    public TileResponse grid4490ServiceTo3857RequestForTileResponse(String layerName, Integer z, Integer x, Integer y) {
        return grid4490ServiceTo3857RequestForTileResponse(layerName, z, x, y, DEFAULT_OUTPUT_FORMAT);
    }

    /**
     * Grid4490 服务转 3857 请求
     *
     * @param layerName    图层名称
     * @param z            zoom 等级
     * @param x            x 坐标
     * @param y            y 坐标
     * @param outputFormat 输出格式
     */
    public void grid4490ServiceTo3857Request(String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        writeTileResponse(grid4490ServiceTo3857RequestForTileResponse(layerName, z, x, y, outputFormat));
    }

    /**
     * Grid4490 服务转 3857 请求，并返回统一瓦片响应。
     */
    @Override
    public TileResponse grid4490ServiceTo3857RequestForTileResponse(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        log.debug("Grid4490服务转3857请求: layer={}, z={}, x={}, y={}, format={}",
                layerName, z, x, y, outputFormat);
        return buildConvertedTileResponse(layerName, z, x, y, outputFormat, false, false);
    }

    /**
     * Grid4490 服务转 3857 请求（删除缓存）
     *
     * @param layerName    图层名称
     * @param z            zoom 等级
     * @param x            x 坐标
     * @param y            y 坐标
     * @param outputFormat 输出格式
     */
    public void grid4490ServiceTo3857RequestDelCache(String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        writeTileResponse(grid4490ServiceTo3857RequestDelCacheForTileResponse(layerName, z, x, y, outputFormat));
    }

    /**
     * Grid4490 服务转 3857 请求、删除缓存后返回统一瓦片响应。
     */
    @Override
    public TileResponse grid4490ServiceTo3857RequestDelCacheForTileResponse(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        log.debug("Grid4490服务转3857请求并删除缓存: layer={}, z={}, x={}, y={}, format={}",
                layerName, z, x, y, outputFormat);
        return buildConvertedTileResponse(layerName, z, x, y, outputFormat, true, false);
    }

    // ==================== 核心处理方法 ====================

    private TileResponse buildConvertedTileResponse(String layerName, Integer z, Integer x, Integer y,
                                                     String outputFormat, boolean deleteCache, boolean googleTo4326) {
        int requestGridSrid = googleTo4326 ? 4326 : 3857;
        try {
            BoxReferencedEnvelope box = googleTo4326
                    ? GirAdvTools.getTileGrid4326Opt().xyzToTileBox(z, x, y, DEFAULT_SRID)
                    : GirAdvTools.getTileGrid3857Opt().xyzToTileBox(z, x, y, 4326);
            return buildTileResponse(layerName, z, x, y, buildBoundingBox(box), outputFormat,
                    deleteCache, requestGridSrid);
        } catch (Exception e) {
            return buildErrorTileResponse(layerName, z, x, y, requestGridSrid, e);
        }
    }

    /**
     * 处理瓦片请求的核心方法
     *
     * @param layerName    图层名称
     * @param z            zoom 等级
     * @param x            x 坐标
     * @param y            y 坐标
     * @param bounds       边界框
     * @param outputFormat 输出格式
     * @param deleteCache  是否删除缓存
     */
    private TileResponse buildTileResponse(String layerName, Integer z, Integer x, Integer y,
                                           BoundingBox bounds, String outputFormat, boolean deleteCache, int requestGridSrid) {
        try {
            GiMimeType fromFormat = GutilMimeType.fromFormat(outputFormat);

            // 创建融合执行器
            FuserExec cacheTileFuser = GirFuserExecFactory.createCachedFuser(
                    layerName,
                    z,
                    x,
                    y,
                    bounds,
                    DEFAULT_TILE_SIZE,
                    DEFAULT_TILE_SIZE,
                    (ImageMime) ImageMime.createFromFormat(fromFormat.getFormat())
            );

            // 如果需要删除缓存
            if (deleteCache) {
                deleteCacheForTile(layerName, z, x, y, cacheTileFuser, (GirImageMime) fromFormat);
                // 删除缓存后，重新生成瓦片
                cacheTileFuser = GirFuserExecFactory.createCachedFuser(
                        layerName,
                        z,
                        x,
                        y,
                        bounds,
                        DEFAULT_TILE_SIZE,
                        DEFAULT_TILE_SIZE,
                        (ImageMime) ImageMime.createFromFormat(fromFormat.getFormat())
                );
            }

            // 生成瓦片
            byte[] imageBytes = cacheTileFuser.toImageBytes();


            return TileResponseByByte.of()
                    .setBytesAndUpdateSize(imageBytes)
                    .setLastModified(System.currentTimeMillis())
                    .setSuccess(true)
                    .setMimeType(fromFormat)
                    .setDataSource("fuser").setCoordinate(TileZxyApo.of().setZ(z).setX(x).setY(y))
                    .setGridEpsgStr("EPSG:" + requestGridSrid);
        } catch (Exception e) {
            return buildErrorTileResponse(layerName, z, x, y, requestGridSrid, e);
        }
    }

    private TileResponse buildErrorTileResponse(
            String layerName, Integer z, Integer x, Integer y, int requestGridSrid, Exception e) {
        String errorMsg = StrUtil.format("生成瓦片失败: layerName={}, z={}, x={}, y={}", layerName, z, x, y);
        log.error(errorMsg, e);
        return TileResponse.error(errorMsg + "，异常信息：" + e.getMessage())
                .setDataSource("fuser")
                .setCoordinate(TileZxyApo.of().setZ(z).setX(x).setY(y))
                .setGridEpsgStr("EPSG:" + requestGridSrid);
    }

    private void writeTileResponse(TileResponse tileResponse) {
        HttpServletResponse response = GirHttpServletHelper.getResponse();
        GirTileResponseUtil.buildFromTileResponse(tileResponse, response);
    }


    // ==================== 缓存管理方法 ====================

    /**
     * 删除瓦片缓存
     *
     * @param layerName   图层名称
     * @param z           zoom 等级
     * @param x           x 坐标
     * @param y           y 坐标
     * @param cacheFuser  缓存融合执行器
     * @param imageFormat 图片格式
     */
    public void deleteCacheForTile(String layerName, Integer z, Integer x, Integer y,
                                   FuserExec cacheFuser, GirImageMime imageFormat) {
        try {
            // 删除当前瓦片缓存
            if (cacheFuser instanceof CacheTileFuserExec) {
                FuserCacheUtils.deleteCacheByRequestGrid(layerName, z, x, y, (CacheTileFuserExec) cacheFuser, imageFormat);
                log.debug("删除当前瓦片缓存: layer={}, z={}, x={}, y={}", layerName, z, x, y);
            }
        } catch (Exception e) {
            log.error("删除瓦片缓存失败: layer={}, z={}, x={}, y={}", layerName, z, x, y, e);
        }
    }


    /**
     * 删除瓦片缓存（公开方法，用于外部调用）
     *
     * @param layerName    图层名称
     * @param z            zoom 等级
     * @param x            x 坐标
     * @param y            y 坐标
     * @param bounds       边界框
     * @param outputFormat 输出格式
     */
    public void delCache(String layerName, Integer z, Integer x, Integer y,
                         BoundingBox bounds, String outputFormat) {
        log.info("删除瓦片缓存: layer={}, z={}, x={}, y={}, format={}",
                layerName, z, x, y, outputFormat);

        try {
            GiMimeType fromFormat = GutilMimeType.fromFormat(outputFormat);
            CacheTileFuserExec cacheTileFuser = GirFuserExecFactory.createCachedFuser(
                    layerName,
                    z,
                    x,
                    y,
                    bounds,
                    DEFAULT_TILE_SIZE,
                    DEFAULT_TILE_SIZE,
                    (ImageMime) fromFormat
            );

            // 删除周边瓦片缓存
            FuserCacheUtils.deleteCacheByRequestGrid(layerName, z, x, y, cacheTileFuser, (GirImageMime) fromFormat);

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

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported", e);
        }
    }

    private static class ParsedTileRequest {
        private final String operation;
        private final String layerName;
        private final Integer z;
        private final Integer x;
        private final Integer y;
        private final String outputFormat;
        private final boolean deleteCache;

        private ParsedTileRequest(String operation, String layerName, Integer z, Integer x, Integer y,
                                  String outputFormat, boolean deleteCache) {
            this.operation = operation;
            this.layerName = layerName;
            this.z = z;
            this.x = x;
            this.y = y;
            this.outputFormat = outputFormat;
            this.deleteCache = deleteCache;
        }

        private static ParsedTileRequest parse(String requestUri) throws Exception {
            if (StrUtil.isBlank(requestUri)) {
                return null;
            }
            URI uri = new URI(requestUri.trim());
            String rawPath = uri.getRawPath();
            if (rawPath == null) {
                return null;
            }
            String[] parts = rawPath.split("/");
            for (int i = 0; i + 5 < parts.length; i++) {
                if (!URI_PATH_PREFIX.equals(parts[i])) {
                    continue;
                }
                String operation = parts[i + 1];
                if (!GOOGLE_TO_4326_OPERATION.equals(operation) && !GRID4490_TO_3857_OPERATION.equals(operation)) {
                    return null;
                }
                String layerName = URLUtil.decode(parts[i + 2]);
                if (StrUtil.isBlank(layerName)) {
                    return null;
                }
                Integer z = parseCoordinate(parts[i + 3]);
                Integer x = parseCoordinate(parts[i + 4]);
                Integer y = parseCoordinate(parts[i + 5]);
                Map<String, String> query = parseQuery(uri.getRawQuery());
                String outputFormat = query.get("format");
                return new ParsedTileRequest(operation, layerName, z, x, y,
                        StrUtil.isBlank(outputFormat) ? DEFAULT_OUTPUT_FORMAT : outputFormat,
                        Boolean.parseBoolean(query.get("deleteCache")));
            }
            return null;
        }

        private static Integer parseCoordinate(String value) {
            int coordinate = Integer.parseInt(value);
            if (coordinate < 0) {
                throw new IllegalArgumentException("Tile coordinate must not be negative");
            }
            return coordinate;
        }

        private static Map<String, String> parseQuery(String rawQuery) {
            Map<String, String> result = new HashMap<>();
            if (StrUtil.isBlank(rawQuery)) {
                return result;
            }
            for (String pair : rawQuery.split("&")) {
                int separator = pair.indexOf('=');
                String key = separator < 0 ? pair : pair.substring(0, separator);
                String value = separator < 0 ? "" : pair.substring(separator + 1);
                result.put(URLUtil.decode(key), URLUtil.decode(value));
            }
            return result;
        }
    }

}
