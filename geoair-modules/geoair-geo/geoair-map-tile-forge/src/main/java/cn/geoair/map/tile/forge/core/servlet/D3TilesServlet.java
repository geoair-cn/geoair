package cn.geoair.map.tile.forge.core.servlet;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.simple.GirTileResponseUtil;
import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.dynamic.tools.simple.response.TileResponseProvider;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.service.GirMapTileService;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.hutool.core.util.URLUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import java.io.IOException;
import java.net.URLDecoder;
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
        GirTileResponseUtil.buildFromTileResponse(getTileResponse(getRequestUri(request), getRequestHost(request)), response);
    }


    @Override
    public TileResponse getTileResponse(String requestUri) {
        return getTileResponse(requestUri, null);
    }

    @Override
    public TileResponse getTileResponse(String requestUri, String requestHost) {
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
        if (!isSafeRelativePath(parseResult.getContentAfterPrefix())) {
            log.warn("请求路径包含非法片段: {}", requestUri);
            return TileResponse.error("Invalid tile path");
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
            parseResult.setRequestHost(requestHost);
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
     * 校验归档内路径或本地缓存相对路径，禁止绝对路径与目录回退。
     */
    protected boolean isSafeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return false;
        }
        String normalized = relativePath.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.indexOf('\u0000') >= 0) {
            return false;
        }
        for (String segment : normalized.split("/")) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 校验地形瓦片坐标或单个元数据文件名，避免其参与本地路径拼接时越界。
     */
    protected boolean isSafeTerrainRequest(TileParseResult parseResult) {
        if (parseResult.isTile()) {
            return isNonNegativeInteger(parseResult.getZ())
                   && isNonNegativeInteger(parseResult.getX())
                   && isNonNegativeInteger(parseResult.getY())
                   && parseResult.getFormat() != null
                   && parseResult.getFormat().matches("[A-Za-z0-9]+$");
        }
        return parseResult.getZ() != null && parseResult.getZ().matches("[A-Za-z0-9._-]+$")
               && !".".equals(parseResult.getZ()) && !"..".equals(parseResult.getZ());
    }

    private boolean isNonNegativeInteger(String value) {
        return value != null && value.matches("[0-9]+");
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
     * 从当前 Servlet 请求构建对外访问源。
     *
     * <p>优先使用反向代理透传的协议、主机和端口；没有代理头时使用 Servlet 请求本身。
     * 调用方应只在受信任的反向代理环境中接受 {@code X-Forwarded-*} 请求头。</p>
     */
    protected String getRequestHost(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String scheme = firstHeaderValue(request.getHeader("X-Forwarded-Proto"));
        if (scheme == null) {
            scheme = request.getScheme();
        }
        String host = firstHeaderValue(request.getHeader("X-Forwarded-Host"));
        if (host == null) {
            host = firstHeaderValue(request.getHeader("Host"));
        }
        String forwardedPort = firstHeaderValue(request.getHeader("X-Forwarded-Port"));
        if (host == null) {
            host = request.getServerName();
        }
        int port = parsePort(forwardedPort, request.getServerPort());
        host = appendPortIfNecessary(host, port, scheme);
        return scheme + "://" + host;
    }

    private String firstHeaderValue(String value) {
        if (value == null) {
            return null;
        }
        int separator = value.indexOf(',');
        String firstValue = (separator >= 0 ? value.substring(0, separator) : value).trim();
        return firstValue.isEmpty() ? null : firstValue;
    }

    private int parsePort(String portValue, int defaultPort) {
        if (portValue == null) {
            return defaultPort;
        }
        try {
            return Integer.parseInt(portValue);
        } catch (NumberFormatException ignored) {
            return defaultPort;
        }
    }

    private String appendPortIfNecessary(String host, int port, String scheme) {
        if (host == null || host.isEmpty() || port <= 0 || hasExplicitPort(host)
            || ("http".equalsIgnoreCase(scheme) && port == 80)
            || ("https".equalsIgnoreCase(scheme) && port == 443)) {
            return host;
        }
        if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
            return "[" + host + "]:" + port;
        }
        return host + ":" + port;
    }

    private boolean hasExplicitPort(String host) {
        if (host.startsWith("[")) {
            return host.indexOf("]:") > 0;
        }
        return host.indexOf(':') >= 0;
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
        requestURI = URLDecoder.decode(requestURI);
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
