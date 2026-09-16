package cn.geoair.map.tile.forge.fuser.request;

import cn.geoair.map.tile.forge.fuser.enums.TileServiceOperation;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 从 tile-fuser URI 解析出的瓦片请求。
 */
public final class TileServiceRequest {

    private final TileServiceOperation operation;
    private final String layerName;
    private final Integer z;
    private final Integer x;
    private final Integer y;
    private final String outputFormat;
    private final boolean deleteCache;

    private TileServiceRequest(
            TileServiceOperation operation, String layerName, Integer z, Integer x, Integer y,
            String outputFormat, boolean deleteCache) {
        this.operation = operation;
        this.layerName = layerName;
        this.z = z;
        this.x = x;
        this.y = y;
        this.outputFormat = outputFormat;
        this.deleteCache = deleteCache;
    }

    public static TileServiceRequest parse(
            String requestUri, String pathPrefix, String defaultOutputFormat) throws Exception {
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
            if (!pathPrefix.equals(parts[i])) {
                continue;
            }

            TileServiceOperation operation = TileServiceOperation.fromCode(parts[i + 1]);
            if (operation == null) {
                return null;
            }
            String layerName = URLUtil.decode(parts[i + 2]);
            if (StrUtil.isBlank(layerName)) {
                return null;
            }

            Map<String, String> query = parseQuery(uri.getRawQuery());
            String outputFormat = query.get("format");
            return new TileServiceRequest(
                    operation,
                    layerName,
                    parseCoordinate(parts[i + 3]),
                    parseCoordinate(parts[i + 4]),
                    parseCoordinate(parts[i + 5]),
                    StrUtil.isBlank(outputFormat) ? defaultOutputFormat : outputFormat,
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

    public TileServiceOperation getOperation() {
        return operation;
    }

    public String getLayerName() {
        return layerName;
    }

    public Integer getZ() {
        return z;
    }

    public Integer getX() {
        return x;
    }

    public Integer getY() {
        return y;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public boolean isDeleteCache() {
        return deleteCache;
    }
}
