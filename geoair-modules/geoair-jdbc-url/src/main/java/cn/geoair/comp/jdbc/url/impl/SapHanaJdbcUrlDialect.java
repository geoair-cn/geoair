package cn.geoair.comp.jdbc.url.impl;

import cn.geoair.comp.jdbc.url.JdbcUrlDialect;
import cn.geoair.comp.jdbc.url.beans.JdbcEndpoint;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.beans.JdbcUrlProperty;
import cn.geoair.comp.jdbc.url.enums.DatabaseType;

import java.util.Collections;

/**
 * SAP HANA JDBC URL 方言。
 *
 * <p>遵循项目原有格式 {@code jdbc:sap://host:port/?databaseName=db}；数据库名位于查询参数而不是路径。
 *
 * @author 张逢吉
 */
public class SapHanaJdbcUrlDialect implements JdbcUrlDialect {
    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.SAP_HANA;
    }

    @Override
    public String getSchemaPropertyName() {
        return null;
    }

    @Override
    public boolean supports(String jdbcUrl) {
        return jdbcUrl.regionMatches(true, 0, "jdbc:sap://", 0, 11);
    }

    @Override
    public JdbcUrl parse(String jdbcUrl) {
        JdbcUrlDialectSupport.ParsedTail tail =
                JdbcUrlDialectSupport.splitTail(jdbcUrl, JdbcUrl.PropertyStyle.QUERY);
        String target = tail.coreUrl.substring("jdbc:sap://".length());
        int slash = target.indexOf('/');
        String authority = slash < 0 ? target : target.substring(0, slash);
        String databaseName = null;
        for (JdbcUrlProperty property : tail.properties) {
            if ("databaseName".equalsIgnoreCase(property.getName())) {
                databaseName = property.getValue();
                break;
            }
        }
        return new JdbcUrl(
                jdbcUrl,
                DatabaseType.SAP_HANA,
                "sap",
                null,
                tail.coreUrl,
                JdbcUrlDialectSupport.parseEndpoints(authority),
                databaseName,
                tail.style,
                tail.properties);
    }

    @Override
    public JdbcUrl create(String host, Integer port, String databaseName) {
        String core = "jdbc:sap://" + JdbcUrlDialectSupport.endpoint(host, port) + "/";
        return new JdbcUrl(
                core,
                DatabaseType.SAP_HANA,
                "sap",
                null,
                core,
                Collections.singletonList(new JdbcEndpoint(host, port)),
                databaseName,
                JdbcUrl.PropertyStyle.QUERY,
                Collections.singletonList(new JdbcUrlProperty("databaseName", databaseName, true)));
    }
}
