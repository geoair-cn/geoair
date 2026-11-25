package cn.geoair.map.tile.forge.fuser;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.geoair.map.dynamic.tools.simple.GirTileResponseUtil;
import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.dynamic.tools.simple.response.TileResponseByByte;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.enums.TileServiceOperation;
import cn.geoair.map.tile.forge.fuser.fuser.CacheTileFuserExec;
import cn.geoair.map.tile.forge.fuser.fuser.FuserExec;
import cn.geoair.map.tile.forge.fuser.fuser.GirFuserExec;
import cn.geoair.map.tile.forge.fuser.fuser.GirFuserExecFactory;
import cn.geoair.map.tile.forge.fuser.provider.CachedTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.SameGridTileReader;
import cn.geoair.map.tile.forge.fuser.provider.TileGetterFactory;
import cn.geoair.map.tile.forge.fuser.request.TileServiceRequest;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import cn.geoair.web.mime.GiMimeType;
import cn.geoair.web.mime.GirImageMime;
import cn.geoair.web.util.GirHttpServletHelper;
import cn.geoair.web.util.GutilMimeType;
import cn.hutool.core.util.StrUtil;

import jakarta.servlet.http.HttpServletResponse;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * XYZ 瓦片图层叠加服务转换类
 *
 * <p>提供不同坐标系之间的瓦片请求转换和缓存管理功能
 *
 * @author 张俊
 * @date Created in 2023/12/4 15:47
 */
public class TileServiceTran implements TileServiceTranResponseProvider {
    private static GiLogger log = GirLoggerFactory.getLogger();

    /** 默认瓦片大小 */
    private static final int DEFAULT_TILE_SIZE = 256;

    /** 默认输出格式 */
    private static final String DEFAULT_OUTPUT_FORMAT = "image/png";

    /** 默认坐标系 SRID */
    private static final int DEFAULT_SRID = 3857;

    private static final String URI_PATH_PREFIX = "tile-fuser";

    /**
     * 构建可直接传入 {@link #getTileResponse(String)} 的标准 URI。
     *
     * <p>格式：{@code
     * /tile-fuser/{operation}/{layer}/{z}/{x}/{y}?format=image%2Fpng&deleteCache=false}
     */
    public static String buildTileRequestUri(
            TileServiceOperation operation,
            String layerName,
            Integer z,
            Integer x,
            Integer y,
            String outputFormat,
            boolean deleteCache) {
        if (operation == null) {
            throw new IllegalArgumentException("tile-fuser operation must not be null");
        }
        if (StrUtil.isBlank(layerName) || z == null || x == null || y == null) {
            throw new IllegalArgumentException("layerName, z, x and y must not be empty");
        }
        String format = StrUtil.isBlank(outputFormat) ? DEFAULT_OUTPUT_FORMAT : outputFormat;
        return "/"
                + URI_PATH_PREFIX
                + "/"
                + operation.getCode()
                + "/"
                + urlEncode(layerName)
                + "/"
                + z
                + "/"
                + x
                + "/"
                + y
                + "?format="
                + urlEncode(format)
                + "&deleteCache="
                + deleteCache;
    }

    /** 兼容直接传入操作编码的调用。 */
    public static String buildTileRequestUri(
            String operation,
            String layerName,
            Integer z,
            Integer x,
            Integer y,
            String outputFormat,
            boolean deleteCache) {
        return buildTileRequestUri(
                TileServiceOperation.requireFromCode(operation),
                layerName,
                z,
                x,
                y,
                outputFormat,
                deleteCache);
    }

    /**
     * 解析由 {@link #buildTileRequestUri(String, String, Integer, Integer, Integer, String, boolean)}
     * 构建的 URI 或完整 URL，并生成瓦片响应。
     */
    @Override
    public TileResponse getTileResponse(String requestUri) {
        return getTileResponse(requestUri, null);
    }

    @Override
    public TileResponse getTileResponse(String requestUri, String requestHost) {
        try {
            TileServiceRequest parsed =
                    TileServiceRequest.parse(requestUri, URI_PATH_PREFIX, DEFAULT_OUTPUT_FORMAT);
            if (parsed == null) {
                return TileResponse.notFound()
                        .setHttpCode(HttpServletResponse.SC_NOT_FOUND)
                        .setErrorMessage("Invalid tile-fuser URI: " + requestUri);
            }
            if (parsed.getOperation() == TileServiceOperation.GOOGLE_TO_4326) {
                return buildConvertedTileResponse(
                        parsed.getLayerName(),
                        parsed.getZ(),
                        parsed.getX(),
                        parsed.getY(),
                        parsed.getOutputFormat(),
                        parsed.isDeleteCache(),
                        true);
            }
            if (parsed.getOperation() == TileServiceOperation.SAME_GRID) {
                return buildSameGridTileResponse(
                        parsed.getLayerName(),
                        parsed.getZ(),
                        parsed.getX(),
                        parsed.getY(),
                        parsed.getOutputFormat(),
                        parsed.isDeleteCache());
            }
            return buildConvertedTileResponse(
                    parsed.getLayerName(),
                    parsed.getZ(),
                    parsed.getX(),
                    parsed.getY(),
                    parsed.getOutputFormat(),
                    parsed.isDeleteCache(),
                    false);
        } catch (Exception e) {
            log.error("解析 tile-fuser URI 失败: {}", requestUri, e);
            return TileResponse.error("Failed to parse tile-fuser URI: " + e.getMessage());
        }
    }

    // ==================== 公开方法 - 同网格直出 ====================

    /** 源网格与请求网格一致时，瓦片直出并缓存（默认 PNG 格式）。 */
    public void sameGridRequest(String layerName, Integer z, Integer x, Integer y) {
        writeTileResponse(sameGridRequestForTileResponse(layerName, z, x, y));
    }

    /** 源网格与请求网格一致时，瓦片直出并缓存（默认 PNG 格式）。 */
    @Override
    public TileResponse sameGridRequestForTileResponse(
            String layerName, Integer z, Integer x, Integer y) {
        return sameGridRequestForTileResponse(layerName, z, x, y, DEFAULT_OUTPUT_FORMAT);
    }

    /** 源网格与请求网格一致时，瓦片直出并缓存。 */
    public void sameGridRequest(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        writeTileResponse(sameGridRequestForTileResponse(layerName, z, x, y, outputFormat));
    }

    /**
     * 源网格与请求网格一致时，瓦片直出并缓存。
     *
     * <p>该入口是严格直出接口：源网格、瓦片矩阵、输出格式或缓存配置不满足条件时， 返回明确错误，不会转入融合流程。
     */
    @Override
    public TileResponse sameGridRequestForTileResponse(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        return buildSameGridTileResponse(layerName, z, x, y, outputFormat, false);
    }

    /** 删除对应原始网格缓存后，重新获取并直出同网格瓦片。 */
    public void sameGridRequestDelCache(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        writeTileResponse(sameGridRequestDelCacheForTileResponse(layerName, z, x, y, outputFormat));
    }

    /** 删除对应原始网格缓存后，重新获取并直出同网格瓦片。 */
    @Override
    public TileResponse sameGridRequestDelCacheForTileResponse(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        return buildSameGridTileResponse(layerName, z, x, y, outputFormat, true);
    }

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
        writeTileResponse(googleServiceTo4326RequestForTileResponse(layerName, z, x, y));
    }

    /** Google 服务转 4326 请求，并返回统一瓦片响应（默认 PNG 格式）。 */
    @Override
    public TileResponse googleServiceTo4326RequestForTileResponse(
            String layerName, Integer z, Integer x, Integer y) {
        return googleServiceTo4326RequestForTileResponse(layerName, z, x, y, DEFAULT_OUTPUT_FORMAT);
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
        writeTileResponse(
                googleServiceTo4326RequestForTileResponse(layerName, z, x, y, outputFormat));
    }

    /** Google 服务转 4326 请求，并返回统一瓦片响应。 */
    @Override
    public TileResponse googleServiceTo4326RequestForTileResponse(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        log.debug(
                "Google服务转4326请求: layer={}, z={}, x={}, y={}, format={}",
                layerName,
                z,
                x,
                y,
                outputFormat);
        return buildConvertedTileResponse(layerName, z, x, y, outputFormat, false, true);
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
        writeTileResponse(
                googleServiceTo4326RequestDelCacheForTileResponse(
                        layerName, z, x, y, outputFormat));
    }

    /** Google 服务转 4326 请求、删除缓存后返回统一瓦片响应。 */
    @Override
    public TileResponse googleServiceTo4326RequestDelCacheForTileResponse(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        log.debug(
                "Google服务转4326请求并删除缓存: layer={}, z={}, x={}, y={}, format={}",
                layerName,
                z,
                x,
                y,
                outputFormat);
        return buildConvertedTileResponse(layerName, z, x, y, outputFormat, true, true);
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
        writeTileResponse(grid4490ServiceTo3857RequestForTileResponse(layerName, z, x, y));
    }

    /** Grid4490 服务转 3857 请求，并返回统一瓦片响应（默认 PNG 格式）。 */
    @Override
    public TileResponse grid4490ServiceTo3857RequestForTileResponse(
            String layerName, Integer z, Integer x, Integer y) {
        return grid4490ServiceTo3857RequestForTileResponse(
                layerName, z, x, y, DEFAULT_OUTPUT_FORMAT);
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
        writeTileResponse(
                grid4490ServiceTo3857RequestForTileResponse(layerName, z, x, y, outputFormat));
    }

    /** Grid4490 服务转 3857 请求，并返回统一瓦片响应。 */
    @Override
    public TileResponse grid4490ServiceTo3857RequestForTileResponse(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        log.debug(
                "Grid4490服务转3857请求: layer={}, z={}, x={}, y={}, format={}",
                layerName,
                z,
                x,
                y,
                outputFormat);
        return buildConvertedTileResponse(layerName, z, x, y, outputFormat, false, false);
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
        writeTileResponse(
                grid4490ServiceTo3857RequestDelCacheForTileResponse(
                        layerName, z, x, y, outputFormat));
    }

    /** Grid4490 服务转 3857 请求、删除缓存后返回统一瓦片响应。 */
    @Override
    public TileResponse grid4490ServiceTo3857RequestDelCacheForTileResponse(
            String layerName, Integer z, Integer x, Integer y, String outputFormat) {
        log.debug(
                "Grid4490服务转3857请求并删除缓存: layer={}, z={}, x={}, y={}, format={}",
                layerName,
                z,
                x,
                y,
                outputFormat);
        return buildConvertedTileResponse(layerName, z, x, y, outputFormat, true, false);
    }

    // ==================== 核心处理方法 ====================

    public TileResponse buildConvertedTileResponse(
            String layerName,
            Integer z,
            Integer x,
            Integer y,
            String outputFormat,
            boolean deleteCache,
            boolean googleTo4326) {
        int requestGridSrid = googleTo4326 ? 4326 : 3857;
        try {
            PxyLayerInfo layerInfo = resolveLayerInfo(layerName);
            int sourceBoundsSrid = resolveSourceBoundsSrid(layerInfo, googleTo4326);
            BoxReferencedEnvelope box =
                    googleTo4326
                            ? GirAdvTools.getTileGrid4326Opt()
                                    .xyzToTileBox(z, x, y, TileYAxis.XYZ, sourceBoundsSrid)
                            : GirAdvTools.getTileGrid3857Opt()
                                    .xyzToTileBox(z, x, y, TileYAxis.XYZ, sourceBoundsSrid);
            return buildTileResponse(
                    layerName,
                    z,
                    x,
                    y,
                    buildBoundingBox(box),
                    outputFormat,
                    deleteCache,
                    requestGridSrid);
        } catch (Exception e) {
            return buildErrorTileResponse(layerName, z, x, y, requestGridSrid, e);
        }
    }

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
    public TileResponse buildTileResponse(
            String layerName,
            Integer z,
            Integer x,
            Integer y,
            BoundingBox bounds,
            String outputFormat,
            boolean deleteCache,
            int requestGridSrid) {
        try {
            PxyLayerInfo layer = resolveLayerInfo(layerName);
            TileResponse sameGridResponse =
                    tryBuildSameGridTileResponse(
                            layer,
                            layerName,
                            z,
                            x,
                            y,
                            bounds,
                            outputFormat,
                            deleteCache,
                            requestGridSrid);
            if (sameGridResponse != null) {
                return sameGridResponse;
            }

            GiMimeType fromFormat = GutilMimeType.fromFormat(outputFormat);
            ImageMime imageFormat = (ImageMime) ImageMime.createFromFormat(fromFormat.getFormat());
            FuserExec fuser;
            if (layer != null && !layer.isCacheEnabled(layer)) {
                // 关闭缓存后，重投影也直接读取源瓦片，刷新请求不触碰历史缓存。
                fuser =
                        new GirFuserExec(
                                createUncachedTileGetter(layer),
                                imageFormat,
                                bounds,
                                DEFAULT_TILE_SIZE,
                                DEFAULT_TILE_SIZE);
            } else {
                fuser =
                        GirFuserExecFactory.createCachedFuser(
                                layerName,
                                z,
                                x,
                                y,
                                bounds,
                                DEFAULT_TILE_SIZE,
                                DEFAULT_TILE_SIZE,
                                imageFormat);
                if (deleteCache) {
                    deleteCacheForTile(layerName, z, x, y, fuser, (GirImageMime) fromFormat);
                    fuser =
                            GirFuserExecFactory.createCachedFuser(
                                    layerName,
                                    z,
                                    x,
                                    y,
                                    bounds,
                                    DEFAULT_TILE_SIZE,
                                    DEFAULT_TILE_SIZE,
                                    imageFormat);
                }
            }

            byte[] imageBytes = fuser.toImageBytes();

            return TileResponseByByte.of()
                    .setBytesAndUpdateSize(imageBytes)
                    .setLastModified(System.currentTimeMillis())
                    .setSuccess(true)
                    .setMimeType(fromFormat)
                    .setDataSource("fuser")
                    .setCoordinate(TileZxyApo.of().setZ(z).setX(x).setY(y))
                    .setGridEpsgStr("EPSG:" + requestGridSrid);
        } catch (Exception e) {
            return buildErrorTileResponse(layerName, z, x, y, requestGridSrid, e);
        }
    }

    private TileResponse tryBuildSameGridTileResponse(
            PxyLayerInfo layerInfo,
            String layerName,
            Integer z,
            Integer x,
            Integer y,
            BoundingBox bounds,
            String outputFormat,
            boolean deleteCache,
            int requestGridSrid) {
        if (z == null || x == null || y == null || bounds == null) {
            return null;
        }

        if (layerInfo == null || !isSameGridSrid(layerInfo.getGridSrid(), requestGridSrid)) {
            return null;
        }

        try {
            LayerTileGetter tileGetter = createDirectTileGetter(layerInfo, layerName);
            int tmsY =
                    validateSameGridAndGetTmsY(
                            tileGetter.getSrcGridSubset(), layerInfo.getGridSrid(), z, x, y);
            BoundingBox sourceTileBounds =
                    tileGetter.getSrcGridSubset().boundsFromIndex(new long[] {x, tmsY, z});
            if (!sameBounds(bounds, sourceTileBounds)) {
                return null;
            }
            return doBuildSameGridTileResponse(
                    layerInfo, tileGetter, layerName, z, x, y, outputFormat, deleteCache);
        } catch (Exception e) {
            // 自动优化不能改变原接口能力；不满足严格直出条件时继续走融合流程。
            log.debug(
                    "同网格直出不可用，回退融合: layer={}, requestGridSrid={}, z={}, x={}, y={}, reason={}",
                    layerName,
                    requestGridSrid,
                    z,
                    x,
                    y,
                    e.getMessage());
            return null;
        }
    }

    private TileResponse buildSameGridTileResponse(
            String layerName,
            Integer z,
            Integer x,
            Integer y,
            String outputFormat,
            boolean deleteCache) {
        int sourceGridSrid = DEFAULT_SRID;
        try {
            PxyLayerInfo layerInfo = resolveLayerInfo(layerName);
            if (layerInfo == null) {
                throw new IllegalArgumentException("图层不存在: layerName=" + layerName);
            }
            if (layerInfo.getGridSrid() == null) {
                throw new IllegalArgumentException("同网格直出要求配置gridSrid: layerName=" + layerName);
            }
            sourceGridSrid = normalizeGridSrid(layerInfo.getGridSrid());
            LayerTileGetter tileGetter = createDirectTileGetter(layerInfo, layerName);
            return doBuildSameGridTileResponse(
                    layerInfo, tileGetter, layerName, z, x, y, outputFormat, deleteCache);
        } catch (Exception e) {
            return buildErrorTileResponse(layerName, z, x, y, sourceGridSrid, e);
        }
    }

    private TileResponse doBuildSameGridTileResponse(
            PxyLayerInfo layerInfo,
            LayerTileGetter tileGetter,
            String layerName,
            Integer z,
            Integer x,
            Integer y,
            String outputFormat,
            boolean deleteCache)
            throws Exception {
        if (z == null || x == null || y == null) {
            throw new IllegalArgumentException("z、x、y不能为空");
        }

        GiMimeType responseFormat = GutilMimeType.fromFormat(outputFormat);
        ImageMime requestedImageFormat =
                (ImageMime) ImageMime.createFromFormat(responseFormat.getFormat());
        if (!isSameImageFormat(tileGetter.getSrcFormat(), requestedImageFormat)) {
            throw new IllegalArgumentException(
                    "同网格直出不支持格式转换: sourceFormat="
                            + tileGetter.getSrcFormat().getFormat()
                            + ", outputFormat="
                            + requestedImageFormat.getFormat());
        }

        int tmsY =
                validateSameGridAndGetTmsY(
                        tileGetter.getSrcGridSubset(), layerInfo.getGridSrid(), z, x, y);
        if (deleteCache && tileGetter instanceof CachedTileGetter) {
            ((CachedTileGetter) tileGetter).clearTileCache(z, x, tmsY);
        }

        Resource tileResource = tileGetter.getTileResource(z, x, tmsY);
        byte[] imageBytes = tileResource == null ? null : tileResource.getByteData();
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalStateException("源瓦片不存在或内容为空");
        }

        return TileResponseByByte.of()
                .setBytesAndUpdateSize(imageBytes)
                .setLastModified(System.currentTimeMillis())
                .setSuccess(true)
                .setMimeType(responseFormat)
                .setDataSource(TileServiceOperation.SAME_GRID.getCode())
                .setCoordinate(TileZxyApo.of().setZ(z).setX(x).setY(y))
                .setGridEpsgStr("EPSG:" + normalizeGridSrid(layerInfo.getGridSrid()));
    }

    /** 获取图层配置，保留为保护方法以便外部实现和测试替换配置来源。 */
    protected PxyLayerInfo resolveLayerInfo(String layerName) {
        return GirFuser.getPxyLayerInfo(layerName);
    }

    /** 创建强类型原始网格缓存 Getter。 */
    protected CachedTileGetter createSameGridTileGetter(PxyLayerInfo layerInfo, String layerName) {
        return TileGetterFactory.createRequiredCached(
                layerInfo, null, layerName + FuserCacheUtils.ORIGINAL_GRID_SUFFIX);
    }

    private LayerTileGetter createDirectTileGetter(PxyLayerInfo layer, String layerName) {
        return layer.isCacheEnabled(layer)
                ? createSameGridTileGetter(layer, layerName)
                : createUncachedTileGetter(layer);
    }

    /** 接入方可以为关闭缓存的图层提供自己的源读取器。 */
    protected LayerTileGetter createUncachedTileGetter(PxyLayerInfo layer) {
        return TileGetterFactory.createUncached(layer);
    }

    private int resolveSourceBoundsSrid(PxyLayerInfo layerInfo, boolean googleTo4326) {
        if (layerInfo == null) {
            // 图层不存在时保持原入口的历史假设，后续由 Getter 创建阶段返回明确错误。
            return googleTo4326 ? DEFAULT_SRID : 4326;
        }
        return layerInfo.isWebMercatorGrid() ? DEFAULT_SRID : 4326;
    }

    private boolean isSameGridSrid(Integer sourceGridSrid, int requestGridSrid) {
        return SameGridTileReader.equivalentSrid(sourceGridSrid, requestGridSrid);
    }

    private int normalizeGridSrid(int gridSrid) {
        return gridSrid == 900913 ? DEFAULT_SRID : gridSrid;
    }

    private int validateSameGridAndGetTmsY(
            GridSubset sourceGrid, Integer configuredGridSrid, int z, int x, int xyzY) {
        if (sourceGrid == null) {
            throw new IllegalArgumentException("源Getter没有提供网格定义");
        }
        if (configuredGridSrid == null) {
            throw new IllegalArgumentException("同网格直出要求配置gridSrid");
        }
        if (sourceGrid.getTileWidth() != DEFAULT_TILE_SIZE
                || sourceGrid.getTileHeight() != DEFAULT_TILE_SIZE)
            throw new IllegalArgumentException("同网格直出要求瓦片大小为256x256");
        return SameGridTileReader.tmsY(sourceGrid, configuredGridSrid, z, x, xyzY);
    }

    private boolean isSameImageFormat(ImageMime sourceFormat, ImageMime outputFormat) {
        if (sourceFormat == null || outputFormat == null) {
            return false;
        }
        return sourceFormat.getFormat().equalsIgnoreCase(outputFormat.getFormat())
                || sourceFormat.getMimeType().equalsIgnoreCase(outputFormat.getMimeType());
    }

    private boolean sameBounds(BoundingBox first, BoundingBox second) {
        double tolerance =
                Math.max(Math.max(Math.abs(second.getWidth()), Math.abs(second.getHeight())), 1D)
                        * 1.0E-9D;
        return Math.abs(first.getMinX() - second.getMinX()) <= tolerance
                && Math.abs(first.getMinY() - second.getMinY()) <= tolerance
                && Math.abs(first.getMaxX() - second.getMaxX()) <= tolerance
                && Math.abs(first.getMaxY() - second.getMaxY()) <= tolerance;
    }

    private TileResponse buildErrorTileResponse(
            String layerName, Integer z, Integer x, Integer y, int requestGridSrid, Exception e) {
        String errorMsg =
                StrUtil.format("生成瓦片失败: layerName={}, z={}, x={}, y={}", layerName, z, x, y);
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

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported", e);
        }
    }
}
