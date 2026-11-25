package cn.geoair.comp.dynamic.ds.utils;

import cn.geoair.comp.jdbc.url.GirJdbcUrlCodecs;
import cn.geoair.comp.jdbc.url.beans.JdbcEndpoint;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.beans.JdbcUrlProperty;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JDBC URL 旧版兼容工具。
 *
 * @deprecated 请直接使用 {@link cn.geoair.comp.jdbc.url.JdbcUrlCodec} 与 {@link
 *     GirJdbcUrlCodecs#defaultCodec()}。该类仅保留已有二进制和源码 API。
 */
@Deprecated
public class AdvJdbcUrlUtil {

    /**
     * @deprecated 请使用 JdbcUrlCodec#rewriteSchema。
     */
    @Deprecated
    public static String appendSchema(String jdbcUrl, String schema) {
        return GirJdbcUrlCodecs.defaultCodec().rewriteSchema(jdbcUrl, schema);
    }

    /**
     * @deprecated 请使用 JdbcUrlCodec#parse。
     */
    @Deprecated
    public static AdvJdbcUrlUtil splitter(String jdbcUrl) {
        return new AdvJdbcUrlUtil(jdbcUrl);
    }

    @Getter public String driverName;
    @Getter public String subProtocol;
    @Getter public boolean oracleServiceNameFormat;
    @Getter public String host;
    @Getter public String port;
    @Getter public String database;
    @Getter public Map<String, String> params;

    /**
     * @deprecated 请使用 JdbcUrlCodec#parse。
     */
    @Deprecated
    public AdvJdbcUrlUtil(String jdbcUrl) {
        JdbcUrl parsed = GirJdbcUrlCodecs.defaultCodec().parse(jdbcUrl);
        this.driverName = parsed.getDriverName();
        this.subProtocol = parsed.getSubProtocol();
        this.oracleServiceNameFormat =
                jdbcUrl.regionMatches(true, 0, "jdbc:oracle:", 0, 12) && jdbcUrl.contains(":@//");
        JdbcEndpoint endpoint = parsed.getPrimaryEndpoint();
        this.host = endpoint == null ? null : endpoint.getHost();
        this.port =
                endpoint == null || endpoint.getPort() == null
                        ? null
                        : String.valueOf(endpoint.getPort());
        this.database = parsed.getDatabaseName();
        this.params = new LinkedHashMap<String, String>();
        for (JdbcUrlProperty property : parsed.getProperties()) {
            this.params.put(
                    property.getName(), property.getValue() == null ? "" : property.getValue());
        }
    }

    /**
     * @deprecated 请使用 JdbcUrlCodec#withoutProperties。
     */
    @Deprecated
    public String getJdbcUrlWithoutParams() {
        StringBuilder jdbcUrl = new StringBuilder("jdbc:").append(driverName).append(':');
        if (subProtocol != null) {
            jdbcUrl.append(subProtocol).append(':');
        }
        if (host == null) {
            return jdbcUrl.append(database == null ? "" : database).toString();
        }
        if ("oracle".equalsIgnoreCase(driverName)) {
            jdbcUrl.append('@');
            if (oracleServiceNameFormat) {
                jdbcUrl.append("//");
            }
            jdbcUrl.append(host);
            if (port != null) {
                jdbcUrl.append(':').append(port);
            }
            jdbcUrl.append(oracleServiceNameFormat ? '/' : ':')
                    .append(database == null ? "" : database);
            return jdbcUrl.toString();
        }
        return jdbcUrl.append("//")
                .append(host)
                .append(port == null ? "" : ":" + port)
                .append(database == null ? "" : "/" + database)
                .toString();
    }
}
