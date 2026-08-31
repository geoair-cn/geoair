package cn.geoair.comp.jdbc.url.impl;

import cn.geoair.comp.jdbc.url.JdbcUrlDialect;
import cn.geoair.comp.jdbc.url.beans.JdbcEndpoint;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.beans.JdbcUrlProperty;
import cn.geoair.comp.jdbc.url.enums.DatabaseType;
import java.util.Collections;

/**
 * 嵌入式或本地文件 JDBC URL 方言。
 *
 * <p>用于 H2、SQLite 等不必拥有 host/port 的连接。H2 的配置项采用分号分隔， SQLite 的常见参数采用查询参数分隔。
 *
 * @author 张逢吉
 */
public class LocalJdbcUrlDialect implements JdbcUrlDialect {
    /** 该方言对应的统一数据库类型。 */
    private final DatabaseType databaseType;
    /** URL 内 jdbc: 后使用的驱动协议名。 */
    private final String driverName;
    /** 该驱动声明当前 schema 时采用的参数名；不支持时为 null。 */
    private final String schemaPropertyName;

    public LocalJdbcUrlDialect(
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
                true, 0, "jdbc:" + driverName + ":", 0, driverName.length() + 6);
    }

    @Override
    public JdbcUrl parse(String jdbcUrl) {
        JdbcUrl.PropertyStyle preferredStyle =
                databaseType == DatabaseType.H2
                        ? JdbcUrl.PropertyStyle.SEMICOLON
                        : JdbcUrl.PropertyStyle.QUERY;
        JdbcUrlDialectSupport.ParsedTail tail =
                JdbcUrlDialectSupport.splitTail(jdbcUrl, preferredStyle);
        String databaseName = tail.coreUrl.substring(("jdbc:" + driverName + ":").length());
        return new JdbcUrl(
                jdbcUrl,
                databaseType,
                driverName,
                null,
                tail.coreUrl,
                Collections.<JdbcEndpoint>emptyList(),
                databaseName,
                tail.style,
                tail.properties);
    }

    @Override
    public JdbcUrl create(String host, Integer port, String databaseName) {
        String localDatabase = databaseName == null ? "" : databaseName;
        // 保留 DataSourceApo 原有 H2 文件模式：jdbc:h2:{address}/{dbName}。
        if (databaseType == DatabaseType.H2 && !JdbcUrlDialectSupport.isBlank(host)) {
            localDatabase = host + "/" + localDatabase;
        }
        String core = "jdbc:" + driverName + ":" + localDatabase;
        return new JdbcUrl(
                core,
                databaseType,
                driverName,
                null,
                core,
                Collections.<JdbcEndpoint>emptyList(),
                localDatabase,
                databaseType == DatabaseType.H2
                        ? JdbcUrl.PropertyStyle.SEMICOLON
                        : JdbcUrl.PropertyStyle.QUERY,
                Collections.<JdbcUrlProperty>emptyList());
    }
}
