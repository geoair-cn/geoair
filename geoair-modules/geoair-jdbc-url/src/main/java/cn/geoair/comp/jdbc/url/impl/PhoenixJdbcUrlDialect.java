package cn.geoair.comp.jdbc.url.impl;

import cn.geoair.comp.jdbc.url.enums.DatabaseType;
import cn.geoair.comp.jdbc.url.beans.JdbcEndpoint;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.JdbcUrlDialect;
import cn.geoair.comp.jdbc.url.beans.JdbcUrlProperty;
import java.util.Collections;
import java.util.List;

/**
 * Phoenix JDBC URL 方言。
 *
 * <p>Phoenix 使用 {@code jdbc:phoenix:host:port}，没有通用数据库名或 schema 参数区。</p>
 *
 * @author 张逢吉
 */
public class PhoenixJdbcUrlDialect implements JdbcUrlDialect {
    @Override public DatabaseType getDatabaseType() { return DatabaseType.PHOENIX; }
    @Override public String getSchemaPropertyName() { return null; }
    @Override public boolean supports(String jdbcUrl) { return jdbcUrl.regionMatches(true, 0, "jdbc:phoenix:", 0, 13); }

    @Override
    public JdbcUrl parse(String jdbcUrl) {
        String target = jdbcUrl.substring("jdbc:phoenix:".length());
        String[] parts = target.split(":", 2);
        List<JdbcEndpoint> endpoints = parts.length == 0 || JdbcUrlDialectSupport.isBlank(parts[0])
                ? Collections.<JdbcEndpoint>emptyList()
                : Collections.singletonList(new JdbcEndpoint(parts[0],
                        parts.length > 1 ? JdbcUrlDialectSupport.parsePort(parts[1]) : null));
        return new JdbcUrl(jdbcUrl, DatabaseType.PHOENIX, "phoenix", null, jdbcUrl,
                endpoints, null, JdbcUrl.PropertyStyle.NONE, Collections.<JdbcUrlProperty>emptyList());
    }

    @Override
    public JdbcUrl create(String host, Integer port, String databaseName) {
        String core = "jdbc:phoenix:" + JdbcUrlDialectSupport.endpoint(host, port);
        return new JdbcUrl(core, DatabaseType.PHOENIX, "phoenix", null, core,
                Collections.singletonList(new JdbcEndpoint(host, port)), null,
                JdbcUrl.PropertyStyle.NONE, Collections.<JdbcUrlProperty>emptyList());
    }
}
