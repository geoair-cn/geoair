package cn.geoair.comp.jdbc.url.impl;

import cn.geoair.comp.jdbc.url.JdbcUrlDialect;
import cn.geoair.comp.jdbc.url.beans.JdbcEndpoint;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.beans.JdbcUrlProperty;
import cn.geoair.comp.jdbc.url.enums.DatabaseType;
import java.util.Collections;

/**
 * SQL Server JDBC URL 方言。
 *
 * <p>SQL Server 使用分号参数区，例如 {@code ;databaseName=db;encrypt=true}， 与使用 {@code ?key=value} 的
 * PostgreSQL/MySQL 明确区分。
 *
 * @author 张逢吉
 */
public class SqlServerJdbcUrlDialect implements JdbcUrlDialect {
    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.SQLSERVER;
    }

    @Override
    public String getSchemaPropertyName() {
        return "schemaName";
    }

    @Override
    public boolean supports(String jdbcUrl) {
        return jdbcUrl.regionMatches(true, 0, "jdbc:sqlserver://", 0, 17);
    }

    @Override
    public JdbcUrl parse(String jdbcUrl) {
        JdbcUrlDialectSupport.ParsedTail tail =
                JdbcUrlDialectSupport.splitTail(jdbcUrl, JdbcUrl.PropertyStyle.SEMICOLON);
        String authority = tail.coreUrl.substring("jdbc:sqlserver://".length());
        String databaseName = null;
        for (JdbcUrlProperty property : tail.properties) {
            if ("databaseName".equalsIgnoreCase(property.getName())) {
                databaseName = property.getValue();
                break;
            }
        }
        return new JdbcUrl(
                jdbcUrl,
                DatabaseType.SQLSERVER,
                "sqlserver",
                null,
                tail.coreUrl,
                JdbcUrlDialectSupport.parseEndpoints(authority),
                databaseName,
                tail.style,
                tail.properties);
    }

    @Override
    public JdbcUrl create(String host, Integer port, String databaseName) {
        String core = "jdbc:sqlserver://" + JdbcUrlDialectSupport.endpoint(host, port);
        return new JdbcUrl(
                core,
                DatabaseType.SQLSERVER,
                "sqlserver",
                null,
                core,
                Collections.singletonList(new JdbcEndpoint(host, port)),
                databaseName,
                JdbcUrl.PropertyStyle.SEMICOLON,
                Collections.singletonList(new JdbcUrlProperty("databaseName", databaseName, true)));
    }
}
