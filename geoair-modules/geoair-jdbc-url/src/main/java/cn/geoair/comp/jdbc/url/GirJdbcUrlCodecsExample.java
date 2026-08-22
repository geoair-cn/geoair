package cn.geoair.comp.jdbc.url;

import cn.geoair.comp.jdbc.url.beans.JdbcEndpoint;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.enums.DatabaseType;

/**
 * {@link GirJdbcUrlCodecs} 的基础用法示例。
 *
 * <p>本类不连接数据库，只演示 JDBC URL 的解析、读取、重写和构建。</p>
 *
 * @author 张逢吉
 */
public final class GirJdbcUrlCodecsExample {

    private GirJdbcUrlCodecsExample() {
    }

    public static void main(String[] args) {
        JdbcUrlCodec codec = GirJdbcUrlCodecs.defaultCodec();

        // 1. 解析已有 URL 并读取结构化字段。
        String postgreSqlUrl = "jdbc:postgresql://127.0.0.1:5432/gis?currentSchema=public&sslmode=disable";
        JdbcUrl parsed = codec.parse(postgreSqlUrl);
        JdbcEndpoint endpoint = parsed.getPrimaryEndpoint();
        System.out.println("数据库类型: " + parsed.getDatabaseType());
        System.out.println("主机: " + endpoint.getHost());
        System.out.println("端口: " + endpoint.getPort());
        System.out.println("数据库: " + parsed.getDatabaseName());
        System.out.println("Schema: " + codec.getSchema(postgreSqlUrl));

        // 2. 使用数据库方言改写 schema；PostgreSQL 会改写 currentSchema 参数。
        String tenantUrl = codec.rewriteSchema(postgreSqlUrl, "tenant_gis");
        System.out.println("租户 URL: " + tenantUrl);

        // 3. 按数据库类型构建 URL，再增加驱动参数。
        JdbcUrl mysqlUrl = codec.create(DatabaseType.MYSQL, "mysql.example.com", 3306, "geoair");
        mysqlUrl = codec.withProperty(mysqlUrl, "useUnicode", "true");
        mysqlUrl = codec.withProperty(mysqlUrl, "characterEncoding", "utf8");
        System.out.println("MySQL URL: " + codec.format(mysqlUrl));

        // 4. SQL Server 自动使用分号参数区。
        JdbcUrl sqlServerUrl = codec.create(DatabaseType.SQLSERVER, "sqlserver.example.com", 1433, "gis");
        sqlServerUrl = codec.withProperty(sqlServerUrl, "encrypt", "true");
        System.out.println("SQL Server URL: " + codec.format(sqlServerUrl));
    }
}
