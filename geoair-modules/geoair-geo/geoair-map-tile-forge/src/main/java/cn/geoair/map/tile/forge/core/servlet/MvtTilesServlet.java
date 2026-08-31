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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;

public class MvtTilesServlet extends D3TilesServlet {

    public static GiLogger log = GirLoggerFactory.getLogger();

    public MvtTilesServlet(GirMapTileService mapTileService) {
        super(mapTileService);
    }

    public Pattern getPattern() {
        return Pattern.compile("/mvtTilesService/([^/]+)/([^/]+)/([^/]+)(/.*)?");
    }

    public GirLayerConfigContext getGirLayerConfigContext(
            String fileId, String fileName, String layerName) {
        GirLayerConfigContext config =
                GirLayerConfigContextHelper.getInstance()
                        .getGirLayerConfigContext(
                                GirMapTileType.MVT_TILES, layerName, fileId, fileName)
                        .orElseThrow(() -> new RuntimeException("图层[" + layerName + "]配置不存在"));
        return config;
    }

    @Override
    protected TileResponse createTileResponse(
            TileRequest tileRequest, TileParseResult tileParseResult, String requestUri) {
        String requestURI = tileParseResult.getRequestURI();
        if (isStyleJsonRequest(requestURI)) {
            byte[] bytes = tileRequest.getBytes();
            String jsonContent = new String(bytes, StandardCharsets.UTF_8);
            String baseUrl = getMvtBaseUrl(requestURI, tileParseResult.getRequestHost());
            jsonContent = jsonContent.replace("{BASE_URL}", baseUrl);
            byte[] responseBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
            tileRequest.setBytes(responseBytes).setSize(responseBytes.length);
        }
        return tileRequest.toTileResponse();
    }

    /**
     * 解析请求URI
     *
     * @param requestURI 请求URI
     * @return TileParseResult 对象
     */
    public TileParseResult parseRequest(String requestURI) {
        requestURI = URLDecoder.decode(requestURI);
        return super.parseRequest(requestURI);
    }

    /**
     * 生成 style.json 中的瓦片基础地址。
     *
     * <p>请求 URI 只负责定位资源路径；只有显式传入请求源时才生成绝对地址。 未传入请求源时保持相对地址，兼容历史单参数 API。
     */
    private String getMvtBaseUrl(String requestUri, String requestHost) {
        String basePath = getMvtBasePath(requestUri);
        String origin = normalizeOrigin(requestHost);
        return origin == null ? basePath : origin + basePath;
    }

    /** 获取不带查询参数、协议和主机的 MVT 请求路径。 */
    private String getMvtRequestPath(String requestUri) {
        String requestPath = getRequestPath(requestUri);
        int protocolIndex = requestPath.indexOf("://");
        if (protocolIndex >= 0) {
            int pathStart = requestPath.indexOf('/', protocolIndex + 3);
            return pathStart >= 0 ? requestPath.substring(pathStart) : "/";
        }
        return requestPath;
    }

    private boolean isStyleJsonRequest(String requestUri) {
        return getMvtRequestPath(requestUri).endsWith("/style.json");
    }

    private String getMvtBasePath(String requestUri) {
        String requestPath = getMvtRequestPath(requestUri);
        return requestPath.endsWith("/style.json")
                ? requestPath.substring(0, requestPath.length() - "/style.json".length())
                : requestPath;
    }

    private String normalizeOrigin(String requestHost) {
        if (requestHost == null || requestHost.trim().isEmpty()) {
            return null;
        }
        String origin = requestHost.trim();
        while (origin.endsWith("/")) {
            origin = origin.substring(0, origin.length() - 1);
        }
        return origin.isEmpty() ? null : origin;
    }

    /** @deprecated 使用 {@link #getTileResponse(String)}。 */
    @Deprecated
    @Override
    public void toHttpResponse(
            TileRequest tileRequest,
            HttpServletResponse response,
            TileParseResult tileParseResult) {
        GirTileResponseUtil.buildFromTileResponse(
                createTileResponse(tileRequest, tileParseResult, tileParseResult.getRequestURI()),
                response);
    }
}
