package cn.geoair.map.tile.forge.core.servlet;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.simple.GirTileResponseUtil;
import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.service.GirMapTileService;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.hutool.core.util.URLUtil;


import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class D3TilesServlet extends HttpServlet implements TileResponseProvider {

    public static GiLogger log = GirLoggerFactory.getLogger();

    protected GirMapTileService mapTileService;

    public D3TilesServlet(GirMapTileService mapTileService) {
        this.mapTileService = mapTileService;
    }


    public Pattern getPattern() {
        return Pattern.compile("/3dTilesService/([^/]+)/([^/]+)/([^/]+)(/.*)?");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        GirTileResponseUtil.buildFromTileResponse(getTileResponse(getRequestUri(request)), response);
    }

    @Override
    public TileResponse getTileResponse(String requestUri) {
        if (requestUri == null || requestUri.trim().isEmpty()) {
            return TileResponse.error("Request URI must not be blank");
        }
        String requestPath = getRequestPath(requestUri);

        // 解析请求
        TileParseResult parseResult = parseRequest(requestPath);
        if (parseResult == null || !parseResult.isValid()) {
            log.warn("无法解析请求URI: {}", requestUri);
            return TileResponse.error("Invalid request URI: " + requestUri);
        }
        parseResult.setRequestURI(requestUri);
        try {
            GirLayerConfigContext layerConfigContext = getGirLayerConfigContext(
                    parseResult.getFileId(),
                    parseResult.getFileName(),
                    parseResult.getServiceName()
            );
            TileRequest layerTile = mapTileService.getLayerTile(
                    layerConfigContext,
                    parseResult.getContentAfterPrefix(),
                    "",
                    ""
            );
            return createTileResponse(layerTile, parseResult, requestUri);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return TileResponse.error(e.getMessage());
        }
    }

    /**
     * 从 URI 或完整 URL 中提取不含查询参数、片段标识的请求路径。
     */
    protected String getRequestPath(String requestUri) {
        int queryIndex = requestUri.indexOf('?');
        int fragmentIndex = requestUri.indexOf('#');
        int endIndex = requestUri.length();
        if (queryIndex >= 0) {
            endIndex = queryIndex;
        }
        if (fragmentIndex >= 0) {
            endIndex = Math.min(endIndex, fragmentIndex);
        }
        return requestUri.substring(0, endIndex);
    }

    /**
     * 获取 Web 请求的 URI；子类可按协议需要返回完整 URL。
     */
    protected String getRequestUri(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();
        return queryString == null || queryString.isEmpty() ? requestUri : requestUri + "?" + queryString;
    }

    /**
     * 从 URI 查询参数中读取请求参数，以支持非 Web 的手工 URL 调用。
     */
    protected String getRequestParameter(String requestUri, String parameterName) {
        int queryIndex = requestUri.indexOf('?');
        if (queryIndex < 0 || queryIndex == requestUri.length() - 1) {
            return null;
        }
        String query = requestUri.substring(queryIndex + 1);
        int fragmentIndex = query.indexOf('#');
        if (fragmentIndex >= 0) {
            query = query.substring(0, fragmentIndex);
        }
        for (String item : query.split("&")) {
            int separatorIndex = item.indexOf('=');
            String name = separatorIndex >= 0 ? item.substring(0, separatorIndex) : item;
            if (parameterName.equals(URLUtil.decode(name))) {
                String value = separatorIndex >= 0 ? item.substring(separatorIndex + 1) : "";
                return URLUtil.decode(value);
            }
        }
        return null;
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    public GirLayerConfigContext getGirLayerConfigContext(String fileId, String fileName, String layerName) {
        GirLayerConfigContext config = GirLayerConfigContextHelper.getInstance().getGirLayerConfigContext(
                        GirMapTileType.TILE_3D, layerName, fileId, fileName
                )
                .orElseThrow(() -> new RuntimeException("图层[" + layerName + "]配置不存在"));
        return config;
    }

    /**
     * 解析请求URI
     *
     * @param requestURI 请求URI
     * @return TileParseResult 对象
     */
    public TileParseResult parseRequest(String requestURI) {
        Matcher matcher = getPattern().matcher(requestURI);

        if (!matcher.find()) {
            return null;
        }

        String contentAfterPrefix = matcher.group(4);
        // 如果contentAfterPrefix不为空且不以'/'开头，则去掉第一个'/'
        if (contentAfterPrefix != null && !contentAfterPrefix.isEmpty()) {
            contentAfterPrefix = contentAfterPrefix.substring(1);
        }

        return TileParseResult.of()
                .setRequestURI(requestURI)
                .setFileId(matcher.group(1))        // FileId
                .setFileName(matcher.group(2))      // 文件名称
                .setServiceName(matcher.group(3))   // 服务名称
                .setContentAfterPrefix(contentAfterPrefix)
                .setFullPath(matcher.group(4));     // 完整路径（带前缀的）
    }


    /**
     * 将服务返回的瓦片结果转换为统一响应，子类可在此补充协议特有的处理。
     */
    protected TileResponse createTileResponse(TileRequest tileRequest,
                                                TileParseResult tileParseResult,
                                                String requestUri) {
        return tileRequest.toTileResponse();
    }

    /**
     * @deprecated 使用 {@link #getTileResponse(String)} 获取业务响应，
     * 由 servlet 统一写出 HTTP 响应。
     */
    @Deprecated
    public void toHttpResponse(TileRequest tileRequest,
                               HttpServletResponse response,
                               TileParseResult tileParseResult) {
        GirTileResponseUtil.buildFromTileResponse(
                createTileResponse(tileRequest, tileParseResult, tileParseResult.getRequestURI()), response);
    }
}

