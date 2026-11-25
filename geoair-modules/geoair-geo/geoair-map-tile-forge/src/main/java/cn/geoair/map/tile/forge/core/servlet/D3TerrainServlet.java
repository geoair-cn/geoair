package cn.geoair.map.tile.forge.core.servlet;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.service.GirMapTileService;
import cn.hutool.core.util.URLUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class D3TerrainServlet extends D3TilesServlet {

    public static GiLogger log = GirLoggerFactory.getLogger();

    public D3TerrainServlet(GirMapTileService mapTileService) {
        super(mapTileService);
    }

    public Pattern getPattern() {
        return Pattern.compile(
                "/3dTerrainService/([^/]+)/([^/]+)/([^/]+)/([^/]+(?:/[^/]+/[^/]+)?\\.\\w+)");
    }

    @Override
    public TileResponse getTileResponse(String requestUri, String requestHost) {
        if (requestUri == null || requestUri.trim().isEmpty()) {
            return TileResponse.error("Request URI must not be blank");
        }
        String requestPath = URLUtil.decode(getRequestPath(requestUri));
        // 解析请求URI
        TileParseResult parseResult = parseRequest(requestPath);
        if (parseResult == null) {
            log.warn("无法解析请求URI: {}", requestUri);
            return TileResponse.error("Invalid request URI: " + requestUri);
        }
        if (!isSafeTerrainRequest(parseResult)) {
            log.warn("地形瓦片请求包含非法坐标或路径: {}", requestUri);
            return TileResponse.error("Invalid terrain tile path");
        }
        parseResult.setRequestURI(requestUri);
        try {
            GirLayerConfigContext layerConfigContext =
                    getGirLayerConfigContext(
                            parseResult.getFileId(),
                            parseResult.getFileName(),
                            parseResult.getServiceName());
            layerConfigContext.setFormat(parseResult.getFormat());
            TileRequest layerTile =
                    mapTileService.getLayerTile(
                            layerConfigContext,
                            parseResult.getZ(),
                            parseResult.getY(),
                            parseResult.getX());
            return createTileResponse(layerTile, parseResult, requestUri);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return TileResponse.error(e.getMessage());
        }
    }

    public GirLayerConfigContext getGirLayerConfigContext(
            String fileId, String fileName, String layerName) {
        GirLayerConfigContext config =
                GirLayerConfigContextHelper.getInstance()
                        .getGirLayerConfigContext(
                                GirMapTileType.TERRAIN_3D, layerName, fileId, fileName)
                        .orElseThrow(() -> new RuntimeException("图层[" + layerName + "]配置不存在"));
        return config;
    }

    /**
     * 解析请求URI
     *
     * @param requestURI 请求URI
     * @return TileParseResult 对象，解析失败返回null
     */
    public TileParseResult parseRequest(String requestURI) {
        Matcher matcher = getPattern().matcher(requestURI);
        if (!matcher.find()) {
            return null;
        }

        TileParseResult result =
                TileParseResult.of()
                        .setRequestURI(requestURI)
                        .setFileId(matcher.group(1)) // FileId
                        .setFileName(matcher.group(2)) // 文件名称
                        .setServiceName(matcher.group(3)) // 服务名称
                        .setFullPath(matcher.group(4)); // 第3段及以后的部分

        String pathPart = matcher.group(4);

        if (pathPart.contains("/")) {
            String[] zxyParts = pathPart.split("/");
            if (zxyParts.length >= 3) {
                String[] yAndFormat = zxyParts[2].split("\\.");

                result.setZ(zxyParts[0])
                        .setX(zxyParts[1])
                        .setY(yAndFormat.length > 0 ? yAndFormat[0] : null)
                        .setFormat(yAndFormat.length > 1 ? yAndFormat[1] : null);
            } else {
                log.warn("瓦片路径格式不正确，期望 z/x/y.format，实际: {}", pathPart);
            }
        } else {
            // 处理 layer.json 或其他文件
            result.setZ(pathPart);
        }

        return result;
    }
}
