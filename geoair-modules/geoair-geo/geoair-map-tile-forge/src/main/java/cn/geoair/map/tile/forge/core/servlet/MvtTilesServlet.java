package cn.geoair.map.tile.forge.core.servlet;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.simple.GirTileResponseUtil;
import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.service.GirMapTileService;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MvtTilesServlet extends D3TilesServlet {

    public static GiLogger log = GirLoggerFactory.getLogger();

    public MvtTilesServlet(GirMapTileService mapTileService) {
        super(mapTileService);
    }

    public Pattern getPattern() {
        return Pattern.compile("/mvtTilesService/([^/]+)/([^/]+)/([^/]+)(/.*)?");
    }


    public GirLayerConfigContext getGirLayerConfigContext(String fileId, String fileName, String layerName) {
        GirLayerConfigContext config = GirLayerConfigContextHelper.getInstance().getGirLayerConfigContext(
                        GirMapTileType.MVT_TILES, layerName, fileId, fileName
                )
                .orElseThrow(() -> new RuntimeException("图层[" + layerName + "]配置不存在"));
        return config;
    }

    @Override
    protected TileResponse createTileResponse(TileRequest tileRequest,
                                                TileParseResult tileParseResult,
                                                String requestUri) {
        String requestURI = tileParseResult.getRequestURI();

        if (requestURI.contains("style.json")) {
            byte[] bytes = tileRequest.getBytes();
            String jsonContent = new String(bytes, StandardCharsets.UTF_8);
            String replace = getMvtBasePath(requestUri);
            jsonContent = jsonContent.replace("{BASE_URL}", replace);
            byte[] responseBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
            tileRequest.setBytes(responseBytes).setSize(responseBytes.length);
        }
        return tileRequest.toTileResponse();
    }

    private String getMvtBasePath(String requestUri) {
        String requestPath = getRequestPath(requestUri);
        int protocolIndex = requestPath.indexOf("://");
        if (protocolIndex >= 0) {
            int pathStart = requestPath.indexOf('/', protocolIndex + 3);
            requestPath = pathStart >= 0 ? requestPath.substring(pathStart) : "/";
        }
        return requestPath.endsWith("/style.json")
                ? requestPath.substring(0, requestPath.length() - "/style.json".length())
                : requestPath;
    }

    /**
     * @deprecated 使用 {@link #getTileResponse(String)}。
     */
    @Deprecated
    @Override
    public void toHttpResponse(TileRequest tileRequest,
                               HttpServletResponse response,
                               TileParseResult tileParseResult) {
        GirTileResponseUtil.buildFromTileResponse(
                createTileResponse(tileRequest, tileParseResult, tileParseResult.getRequestURI()), response);
    }

}
