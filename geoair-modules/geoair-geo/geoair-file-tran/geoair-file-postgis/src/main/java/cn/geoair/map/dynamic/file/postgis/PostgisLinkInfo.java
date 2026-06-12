package cn.geoair.map.dynamic.file.postgis;

import cn.geoair.map.dynamic.file.core.link.LinkInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** PostGIS 链接信息类 包含数据库连接、排序字段、几何字段等核心配置 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Slf4j
public abstract class PostgisLinkInfo extends LinkInfo {

    // 数据库基础配置
    protected String jdbcUrl; // PostGIS JDBC地址，如 jdbc:postgresql://localhost:5432/geo_db

    protected String username; // 数据库用户名

    protected String password; // 数据库密码

    protected String schema;

    // 几何配置
    protected int srid = 4326; // 几何字段SRID，默认 4326

    // 连接池配置（基础）
    protected int maxConn = 10; // 最大连接数

    protected int connTimeout = 30000; // 连接超时时间（ms）

    /** 获取数据库连接（带 PostGIS 扩展） */
    public Connection getConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        props.setProperty("connectTimeout", String.valueOf(connTimeout));
        props.setProperty("ApplicationName", "GeoFileReader/Writer");

        // 加载 PostGIS 驱动
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("加载 PostGIS 驱动失败", e);
        }

        return DriverManager.getConnection(jdbcUrl, props);
    }

    /** 获取纯 PostgreSQL 连接（用于注册类型） */
    private Connection getPostgresConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
