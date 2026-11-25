package cn.geoair.comp.jdbc.url.impl;

import cn.geoair.comp.jdbc.url.JdbcUrlDialect;
import cn.geoair.comp.jdbc.url.beans.JdbcEndpoint;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.beans.JdbcUrlProperty;
import cn.geoair.comp.jdbc.url.enums.DatabaseType;
import java.util.Collections;
import java.util.List;

/**
 * Oracle Thin JDBC URL 方言。
 *
 * <p>支持 SID 格式 {@code @host:port:SID} 和 Service Name 格式 {@code @//host:port/service}。 TNS
 * descriptor 属于驱动私有嵌套语法，保留为不透明连接主体而不进行危险重组。
 *
 * @author 张逢吉
 */
public class OracleJdbcUrlDialect implements JdbcUrlDialect {
    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.ORACLE;
    }

    @Override
    public String getSchemaPropertyName() {
        return "defaultSchema";
    }

    @Override
    public boolean supports(String jdbcUrl) {
        return jdbcUrl.regionMatches(true, 0, "jdbc:oracle:", 0, 12);
    }

    @Override
    public JdbcUrl parse(String jdbcUrl) {
        JdbcUrlDialectSupport.ParsedTail tail =
                JdbcUrlDialectSupport.splitTail(jdbcUrl, JdbcUrl.PropertyStyle.QUERY);
        String remaining = tail.coreUrl.substring("jdbc:oracle:".length());
        int marker = remaining.indexOf(":@");
        if (marker < 0) {
            return new JdbcUrl(
                    jdbcUrl,
                    DatabaseType.ORACLE,
                    "oracle",
                    null,
                    tail.coreUrl,
                    Collections.<JdbcEndpoint>emptyList(),
                    null,
                    tail.style,
                    tail.properties);
        }
        String subProtocol = remaining.substring(0, marker);
        String target = remaining.substring(marker + 2);
        if (target.startsWith("//")) {
            String network = target.substring(2);
            int slash = network.indexOf('/');
            if (slash >= 0) {
                return new JdbcUrl(
                        jdbcUrl,
                        DatabaseType.ORACLE,
                        "oracle",
                        subProtocol,
                        tail.coreUrl,
                        JdbcUrlDialectSupport.parseEndpoints(network.substring(0, slash)),
                        network.substring(slash + 1),
                        tail.style,
                        tail.properties);
            }
        } else {
            String[] parts = target.split(":", 3);
            if (parts.length == 3) {
                List<JdbcEndpoint> endpoints =
                        Collections.singletonList(
                                new JdbcEndpoint(
                                        parts[0], JdbcUrlDialectSupport.parsePort(parts[1])));
                return new JdbcUrl(
                        jdbcUrl,
                        DatabaseType.ORACLE,
                        "oracle",
                        subProtocol,
                        tail.coreUrl,
                        endpoints,
                        parts[2],
                        tail.style,
                        tail.properties);
            }
        }
        return new JdbcUrl(
                jdbcUrl,
                DatabaseType.ORACLE,
                "oracle",
                subProtocol,
                tail.coreUrl,
                Collections.<JdbcEndpoint>emptyList(),
                null,
                tail.style,
                tail.properties);
    }

    @Override
    public JdbcUrl create(String host, Integer port, String databaseName) {
        String core =
                "jdbc:oracle:thin:@"
                        + JdbcUrlDialectSupport.endpoint(host, port)
                        + ":"
                        + (databaseName == null ? "" : databaseName);
        return new JdbcUrl(
                core,
                DatabaseType.ORACLE,
                "oracle",
                "thin",
                core,
                Collections.singletonList(new JdbcEndpoint(host, port)),
                databaseName,
                JdbcUrl.PropertyStyle.QUERY,
                Collections.<JdbcUrlProperty>emptyList());
    }
}
