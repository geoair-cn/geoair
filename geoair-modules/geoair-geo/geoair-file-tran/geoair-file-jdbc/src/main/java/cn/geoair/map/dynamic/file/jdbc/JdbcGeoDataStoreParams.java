package cn.geoair.map.dynamic.file.jdbc;

import cn.geoair.comp.jdbc.url.GirJdbcUrlCodecs;
import cn.geoair.comp.jdbc.url.beans.JdbcEndpoint;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoLinkInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于 {@link GirJdbcUrlCodecs} 解析 JDBC URL 并构建 GeoTools DataStore 参数。
 *
 * @author 张逢吉
 */
public final class JdbcGeoDataStoreParams {

    private JdbcGeoDataStoreParams() {
    }

    /** 创建 GeoTools JDBC DataStore 的公共参数。 */
    public static Map<String, Object> create(String dbType, JdbcGeoLinkInfo linkInfo) {
        JdbcUrl jdbcUrl = GirJdbcUrlCodecs.defaultCodec().parse(linkInfo.getJdbcUrl());
        JdbcEndpoint endpoint = jdbcUrl.getPrimaryEndpoint();
        if (endpoint == null || endpoint.getHost() == null) {
            throw new IllegalArgumentException("JDBC URL 未包含可用于 GeoTools 的主机端点：" + linkInfo.getJdbcUrl());
        }
        Map<String, Object> params = new HashMap<>();
        params.put("dbtype", dbType);
        params.put("host", endpoint.getHost());
        if (endpoint.getPort() != null) {
            params.put("port", endpoint.getPort());
        }
        params.put("database", jdbcUrl.getDatabaseName());
        params.put("user", linkInfo.getUsername());
        params.put("passwd", linkInfo.getPassword());
        if (linkInfo.getSchema() != null && !linkInfo.getSchema().trim().isEmpty()) {
            params.put("schema", linkInfo.getSchema());
        }
        return params;
    }
}
