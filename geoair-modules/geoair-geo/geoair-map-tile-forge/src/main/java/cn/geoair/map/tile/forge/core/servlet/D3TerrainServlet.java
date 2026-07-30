package cn.geoair.map.tile.forge.core.servlet;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.simple.GirTileResponseUtil;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.TileRequest;


import cn.geoair.map.tile.forge.core.service.GirMapTileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import cn.hutool.core.util.URLUtil;


import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class D3TerrainServlet extends D3TilesServlet {

    public static GiLogger log = GirLoggerFactory.getLogger();

    public D3TerrainServlet(GirMapTileService mapTileService) {
        super(mapTileService);
    }

    public Pattern getPattern() {
        return Pattern.compile("/3dTerrainService/([^/]+)/([^/]+)/([^/]+)/([^/]+(?:/[^/]+/[^/]+)?\\.\\w+)");
    }


    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestURI = request.getRequestURI(); // 示例：/geospatial-api/3dTilesService/12345/myPrefix/tileset.json
        requestURI= URLUtil.decode(requestURI);
        // 解析请求URI
        TileParseResult parseResult = parseRequest(requestURI);
        if (parseResult == null) {
            log.warn("无法解析请求URI: {}", requestURI);
            GirTileResponseUtil.buildFromException(
                    new IllegalArgumentException("Invalid request URI: " + requestURI),
                    response
            );
            return;
        }
        GirLayerConfigContext layerConfigContext = null;
        try {
            layerConfigContext
                    = getGirLayerConfigContext(parseResult.getFileId(), parseResult.getFileName(), parseResult.getServiceName());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            GirTileResponseUtil.buildFromException(e, response);
            return;
        }

        layerConfigContext.setFormat(parseResult.getFormat());
        try {
            TileRequest layerTile = mapTileService.getLayerTile(
                    layerConfigContext,
                    parseResult.getZ(),
                    parseResult.getY(),
                    parseResult.getX()
            );
            toHttpResponse(layerTile, response, parseResult);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            GirTileResponseUtil.buildFromException(e, response);
        }
    }

    public GirLayerConfigContext getGirLayerConfigContext(String fileId, String fileName, String layerName) {
        GirLayerConfigContext config = GirLayerConfigContextHelper.getInstance().getGirLayerConfigContext(
                        GirMapTileType.TERRAIN_3D, layerName, fileId, fileName
                )
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

        TileParseResult result = TileParseResult.of()
                .setRequestURI(requestURI)
                .setFileId(matcher.group(1))        // FileId
                .setFileName(matcher.group(2))      // 文件名称
                .setServiceName(matcher.group(3))   // 服务名称
                .setFullPath(matcher.group(4));     // 第3段及以后的部分

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
