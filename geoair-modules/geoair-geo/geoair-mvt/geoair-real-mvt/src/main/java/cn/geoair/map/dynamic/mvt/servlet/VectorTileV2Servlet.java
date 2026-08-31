package cn.geoair.map.dynamic.mvt.servlet;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.mvt.GirRealMvtHelper;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.dynamic.tools.simple.response.TileResponseByByte;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.URLUtil;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.locationtech.jts.geom.Geometry;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@GaApi(
        text = "矢量瓦片服务",
        tags = {"矢量瓦片服务"})
public class VectorTileV2Servlet extends TileCommonServlet {
    public static GiLogger log = GirLoggerFactory.getLogger();

    public VectorTileV2Servlet() {
        log.info("初始化矢量瓦片 Servlet 完成");
    }

    String mockPbfUrl = "vectorTileService/v2/real/preview/1/2/3.pbf?paramTile=XXXX";
    String mockDebugUrl = "vectorTileService/v2/debug/preview/1/2/3.pbf?paramTile=XXXX";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String requestUri = request.getRequestURI();
            if (request.getQueryString() != null) {
                requestUri += "?" + request.getQueryString();
            }
            writeTileResponse(getTileResponse(requestUri), response);
        } catch (Exception e) {
            log.error("瓦片请求异常", e);
            response.sendError(500);
        }
    }

    /** 根据 URI 或完整 URL 构建 MVT 响应，不依赖 HttpServletRequest。 */
    @Override
    public TileResponse getTileResponse(String requestUri, String requestHost) {
        JSONObject re = new JSONObject();
        try {
            ParsedTileUri parsed = ParsedTileUri.parse(requestUri);
            if (parsed == null) {
                return TileResponse.notFound()
                        .setHttpCode(HttpServletResponse.SC_NOT_FOUND)
                        .setErrorMessage("Invalid vector tile URI: " + requestUri);
            }
            Map<String, String> queryParams = parseQueryParams(parsed.query);
            if ("real".equals(parsed.mode)) {
                return getRealTileResponse(parsed, queryParams);
            }
            return getDebugTileResponse(parsed, queryParams, re);
        } catch (Exception e) {
            log.error("解析矢量瓦片 URI 失败: {}", requestUri, e);
            return TileResponse.error("Failed to parse vector tile URI: " + e.getMessage());
        }
    }

    private TileResponse getRealTileResponse(ParsedTileUri parsed, Map<String, String> queryParams)
            throws Exception {
        TileRequestParams params;
        String paramTile = queryParams.get("paramTile");
        if (ObjectUtil.isNotEmpty(paramTile)) {
            params = TileRequestParams.fromBase32(paramTile);
        } else {
            params = GirRealMvtHelper.getInstance().getTileRequestParams(parsed.layerName);
        }
        if (ObjectUtil.isEmpty(params)) {
            return TileResponse.error("参数错误:" + parsed.layerName);
        }

        params = params.copy();
        String minZoom = queryParams.get("minZoom");
        if (minZoom != null) {
            params.setMinZoom(Integer.parseInt(minZoom));
        }
        String isGeo = queryParams.get("isGeo");
        if (isGeo != null) {
            params.setGeoIs(Boolean.parseBoolean(isGeo));
        }
        return buildMvtTileResponse(parsed.layerName, params, parsed.z, parsed.x, parsed.y);
    }

    private TileResponse getDebugTileResponse(
            ParsedTileUri parsed, Map<String, String> queryParams, JSONObject re) {
        String paramTile = queryParams.get("paramTile");
        if (ObjectUtil.isEmpty(paramTile)) {
            return TileResponse.error("参数错误:" + parsed.layerName);
        }

        TileRequestParams params = TileRequestParams.fromBase32(paramTile);
        String minZoom = queryParams.get("minZoom");
        if (minZoom != null) {
            params.setMinZoom(Integer.parseInt(minZoom));
        }

        re.put("params", params);
        re.put("z", parsed.z);
        re.put("x", parsed.x);
        re.put("y", parsed.y);
        try {
            BoxReferencedEnvelope boxReferencedEnvelope;
            int gridSrid = params.isGeoIs() ? 4326 : 3857;
            if (params.isGeoIs()) {
                boxReferencedEnvelope =
                        GirGeoTools.defaultInstance()
                                .getTileGrid4326Opt()
                                .xyzToTileBox(parsed.z, parsed.x, parsed.y, TileYAxis.XYZ, 4326);
            } else {
                boxReferencedEnvelope =
                        GirGeoTools.defaultInstance()
                                .getTileGrid3857Opt()
                                .xyzToTileBox(parsed.z, parsed.x, parsed.y, TileYAxis.XYZ, 3857);
            }
            Geometry geometry =
                    GirGeoTools.defaultInstance().getSridOpt().convertToGeom(boxReferencedEnvelope);
            re.put("bbox", geometry.toText());
            re.put(
                    "bbox4326",
                    GirGeoTools.defaultInstance()
                            .getSridOpt()
                            .convert(geometry, gridSrid, 4326)
                            .toText());
        } catch (Exception ignored) {
            // 调试信息中的 bbox 为附加信息，计算失败不影响主响应。
        }

        return TileResponseByByte.of()
                .setBytesAndUpdateSize(
                        JSON.toJSONString(re, JSONWriter.Feature.PrettyFormat)
                                .getBytes(StandardCharsets.UTF_8))
                .setMimeType(cn.geoair.web.mime.GirApplicationMime.json)
                .setDataSource("real-mvt-debug")
                .setCoordinate(
                        cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo.of()
                                .setZ(parsed.z)
                                .setX(parsed.x)
                                .setY(parsed.y))
                .setGridEpsgStr(params.isGeoIs() ? "EPSG:4490" : "EPSG:3857");
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return result;
        }
        for (String pair : query.split("&")) {
            int separator = pair.indexOf('=');
            String key = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
            result.put(URLUtil.decode(key), URLUtil.decode(value));
        }
        return result;
    }

    private static class ParsedTileUri {
        private final String mode;
        private final String layerName;
        private final int z;
        private final int x;
        private final int y;
        private final String query;

        private ParsedTileUri(String mode, String layerName, int z, int x, int y, String query) {
            this.mode = mode;
            this.layerName = layerName;
            this.z = z;
            this.x = x;
            this.y = y;
            this.query = query;
        }

        private static ParsedTileUri parse(String requestUri) throws Exception {
            if (requestUri == null || requestUri.trim().isEmpty()) {
                return null;
            }
            URI uri = new URI(requestUri.trim());
            String path = uri.getPath();
            if (path == null) {
                return null;
            }
            String[] parts = path.split("/");
            for (int i = 0; i + 6 < parts.length; i++) {
                if (!"vectorTileService".equals(parts[i]) || !"v2".equals(parts[i + 1])) {
                    continue;
                }
                String mode = parts[i + 2];
                if (!"real".equals(mode) && !"debug".equals(mode)) {
                    return null;
                }
                String yPart = parts[i + 6];
                if (!yPart.endsWith(".pbf")) {
                    return null;
                }
                int z = parseCoordinate(parts[i + 4]);
                int x = parseCoordinate(parts[i + 5]);
                int y = parseCoordinate(yPart.substring(0, yPart.length() - 4));
                return new ParsedTileUri(mode, parts[i + 3], z, x, y, uri.getRawQuery());
            }
            return null;
        }

        private static int parseCoordinate(String value) {
            int coordinate = Integer.parseInt(value);
            if (coordinate < 0) {
                throw new IllegalArgumentException("Tile coordinate must not be negative");
            }
            return coordinate;
        }
    }
}
