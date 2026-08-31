package cn.geoair.comp.jdbc.url.impl;

import cn.geoair.comp.jdbc.url.JdbcUrlDialect;
import cn.geoair.comp.jdbc.url.beans.JdbcEndpoint;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.beans.JdbcUrlProperty;
import cn.geoair.comp.jdbc.url.enums.DatabaseType;
import java.util.Collections;

/**
 * 解析和构建 {@code jdbc:driver://host:port/database?key=value} 形式的网络 JDBC URL。
 *
 * <p>用于 PostgreSQL、MySQL 与达梦。它支持逗号分隔的多主机地址，并保留查询参数原始顺序。
 *
 * @author 张逢吉
 */
public class NetworkJdbcUrlDialect implements JdbcUrlDialect {
    /** 该方言对应的统一数据库类型。 */
    private final DatabaseType databaseType;
    /** URL 内 jdbc: 后使用的驱动协议名。 */
    private final String driverName;
    /** 该驱动声明当前 schema 时采用的参数名；不支持时为 null。 */
    private final String schemaPropertyName;

    public NetworkJdbcUrlDialect(
            DatabaseType databaseType, String driverName, String schemaPropertyName) {
        this.databaseType = databaseType;
        this.driverName = driverName;
        this.schemaPropertyName = schemaPropertyName;
    }

    @Override
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    @Override
    public String getSchemaPropertyName() {
        return schemaPropertyName;
    }

    @Override
    public boolean supports(String jdbcUrl) {
        return jdbcUrl.regionMatches(
                true, 0, "jdbc:" + driverName + "://", 0, driverName.length() + 8);
    }

    @Override
    public JdbcUrl parse(String jdbcUrl) {
        JdbcUrlDialectSupport.ParsedTail tail =
                JdbcUrlDialectSupport.splitTail(jdbcUrl, JdbcUrl.PropertyStyle.QUERY);
        String prefix = "jdbc:" + driverName + "://";
        String connection = tail.coreUrl.substring(prefix.length());
        int slash = connection.indexOf('/');
        if (slash < 0) {
            throw new IllegalArgumentException("无效的 " + driverName + " JDBC URL：缺少数据库名");
        }
        String databaseName = connection.substring(slash + 1);
        return new JdbcUrl(
                jdbcUrl,
                databaseType,
                driverName,
                null,
                tail.coreUrl,
                JdbcUrlDialectSupport.parseEndpoints(connection.substring(0, slash)),
                databaseName,
                tail.style,
                tail.properties);
    }

    @Override
    public JdbcUrl create(String host, Integer port, String databaseName) {
        String core =
                "jdbc:"
                        + driverName
                        + "://"
                        + JdbcUrlDialectSupport.endpoint(host, port)
                        + "/"
                        + (databaseName == null ? "" : databaseName);
        return new JdbcUrl(
                core,
                databaseType,
                driverName,
                null,
                core,
                Collections.singletonList(new JdbcEndpoint(host, port)),
                databaseName,
                JdbcUrl.PropertyStyle.QUERY,
                Collections.<JdbcUrlProperty>emptyList());
    }
}
