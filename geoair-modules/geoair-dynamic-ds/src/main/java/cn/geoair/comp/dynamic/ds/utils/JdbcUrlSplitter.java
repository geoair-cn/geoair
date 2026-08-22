package cn.geoair.comp.dynamic.ds.utils;

import cn.geoair.comp.jdbc.url.beans.JdbcEndpoint;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.GirJdbcUrlCodecs;

/**
 * JDBC URL 旧版拆分器。
 *
 * @deprecated 请使用 {@link cn.geoair.comp.jdbc.url.JdbcUrlCodec}；该类仅为旧调用方保留。
 */
@Deprecated
public class JdbcUrlSplitter {

    public String driverName, host, port, database, params;

    public JdbcUrlSplitter(String jdbcUrl) {
        JdbcUrl parsed = GirJdbcUrlCodecs.defaultCodec().parse(jdbcUrl);
        driverName = parsed.getDriverName();
        database = parsed.getDatabaseName();
        JdbcEndpoint endpoint = parsed.getPrimaryEndpoint();
        host = endpoint == null ? null : endpoint.getHost();
        port = endpoint == null || endpoint.getPort() == null ? null : String.valueOf(endpoint.getPort());

        String normalized = GirJdbcUrlCodecs.defaultCodec().format(parsed);
        String core = parsed.getCoreUrl();
        params = normalized.length() > core.length() ? normalized.substring(core.length() + 1) : null;
    }
}
