package cn.geoair.comp.dynamic.ds.apo;

import cn.geoair.comp.dynamic.ds.utils.AdvJdbcUrlUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.db.dialect.DialectName;
import cn.hutool.db.dialect.DriverNamePool;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import lombok.Data;

/**
 * 数据源的Api传递对象(Application Persistence Object)
 *
 * <p>用于在应用程序各层之间传递数据源相关信息的数据载体， 包含数据库连接所需的各类配置参数及元数据信息
 */
@Data
public class DataSourceApo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据源唯一标识ID */
    private String id;

    /** 数据库驱动类名 默认使用PostgreSQL驱动，取值于Hutool工具类的DriverNamePool */
    private String driver = DriverNamePool.DRIVER_POSTGRESQL;

    /** 数据库连接URL */
    private String jdbcUrl;

    /** 数据源名称（通常用于显示和标识） */
    private String name;

    /** 数据库服务器地址（IP或域名） */
    private String address;

    /** 数据库服务端口号 */
    private Integer port;

    /** 数据库实例名称 */
    private String dbName;

    /** 数据库模式名称（Schema） */
    private String schemaName;

    /** 数据库登录用户名 */
    private String username;

    /** 数据库登录密码 */
    private String password;

    /** 数据源创建时间 */
    private Date createTime;

    /** 数据源最后更新时间 */
    private Date updateTime;

    // ======================数据源调优参数=============================
    private int initialSize = 2; // 初始连接数

    private int maxActive = 10; // 最大活跃连接数

    private int minIdle = 1; // 最小闲置连接数

    private long maxWait = 10000; // 获取连接的超时等待

    private Integer queryTimeout = 15; // 查询的超时时间 单位秒

    private Integer removeAbandonedTimeout = 1800; // 回收连接的超时时间

    private Integer connectionErrorRetryAttempts = 3; // 链接获取失败的时候重试次数

    private String validationQuery; // 验证链接的SQL

    public void setJdbcUrl(String jdbcUrl) {
        AdvJdbcUrlUtil splitter = AdvJdbcUrlUtil.splitter(jdbcUrl);
        setDbName(splitter.getDatabase());
        if (getPort() == null) {
            setPort(splitter.port == null ? null : Integer.parseInt(splitter.getPort()));
        }
        setAddress(splitter.getHost());
        if (getSchemaName() == null) {
            Map<String, String> params = splitter.getParams();
            if (!MapUtil.isEmpty(params)) {
                setSchemaName(params.get("currentSchema"));
            }
        }
        this.jdbcUrl = jdbcUrl;
    }

    public String getJdbcUrl() {
        if (jdbcUrl == null) {
            jdbcUrl = buildJdbcUrl(this);
        }

        return jdbcUrl;
    }

    /**
     * 判断当前数据源与传入的连接参数是否匹配
     *
     * <p>比较规则：数据库地址、端口、数据库名、用户名完全一致则认为匹配
     *
     * @param address 数据库地址
     * @param port 端口号
     * @param dbName 数据库名
     * @param user 用户名
     * @return true-匹配，false-不匹配
     * @throws NullPointerException 当任一参数为null时可能抛出空指针异常
     */
    public boolean equals(String address, Integer port, String dbName, String user) {
        return this.address.equals(address)
                && this.port.equals(port)
                && this.dbName.equals(dbName)
                && this.username.equals(user);
    }

    /**
     * 根据驱动类名获取对应的数据库类型（DialectName枚举） 核心逻辑：通过驱动类名匹配对应的数据库方言类型
     *
     * @return 数据库类型枚举值，默认返回POSTGRESQL
     */
    public DialectName getDbType() {
        if (driver == null) {
            return DialectName.POSTGRESQL;
        }
        // 按驱动类名匹配数据库类型
        if (driver.contains(DriverNamePool.DRIVER_MYSQL)) {
            return DialectName.MYSQL;
        } else if (driver.contains(DriverNamePool.DRIVER_ORACLE)) {
            return DialectName.ORACLE;
        } else if (driver.contains(DriverNamePool.DRIVER_POSTGRESQL)) {
            return DialectName.POSTGRESQL;
        } else if (driver.contains(DriverNamePool.DRIVER_SQLSERVER)) {
            return DialectName.SQLSERVER;
        } else if (driver.contains(DriverNamePool.DRIVER_SQLLITE3)) {
            return DialectName.SQLITE3;
        } else if (driver.contains(DriverNamePool.DRIVER_H2)) {
            return DialectName.H2;
        } else if (driver.contains(DriverNamePool.DRIVER_DM7)) {
            return DialectName.DM;
        } else if (driver.contains(DriverNamePool.DRIVER_HANA)) {
            return DialectName.HANA;
        } else if (driver.contains(DriverNamePool.DRIVER_PHOENIX)) {
            return DialectName.PHOENIX;
        } else {
            // 默认返回PostgreSQL
            return DialectName.POSTGRESQL;
        }
    }

    /**
     * 根据数据库类型动态构建JDBC URL
     *
     * @param dataSourceApo 数据源配置
     * @return 对应数据库的JDBC URL
     */
    /**
     * 根据数据库类型动态构建JDBC URL（兼容Java 8）
     *
     * @param dataSourceApo 数据源配置
     * @return 对应数据库的JDBC URL
     */
    public static String buildJdbcUrl(DataSourceApo dataSourceApo) {
        DialectName dbType = dataSourceApo.getDbType();
        String address = dataSourceApo.getAddress();
        Integer port = dataSourceApo.getPort();
        String dbName = dataSourceApo.getDbName();
        String schemaName = dataSourceApo.getSchemaName();

        // Java 8兼容的switch语句（替换Switch表达式）
        String jdbcUrl;
        switch (dbType) {
            case MYSQL:
                jdbcUrl =
                        String.format(
                                "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC",
                                address, port, dbName);
                break;
            case ORACLE:
                jdbcUrl = String.format("jdbc:oracle:thin:@%s:%d:%s", address, port, dbName);
                break;
            case POSTGRESQL:
                jdbcUrl =
                        String.format(
                                "jdbc:postgresql://%s:%d/%s%s",
                                address,
                                port,
                                dbName,
                                schemaName != null ? "?currentSchema=" + schemaName : "");
                break;
            case SQLSERVER:
                jdbcUrl =
                        String.format(
                                "jdbc:sqlserver://%s:%d;databaseName=%s", address, port, dbName);
                break;
            case SQLITE3:
                jdbcUrl = String.format("jdbc:sqlite:%s", dbName); // SQLite无需地址端口，dbName为文件路径
                break;
            case H2:
                jdbcUrl = String.format("jdbc:h2:%s/%s", address, dbName); // H2支持多种模式，此处为文件模式示例
                break;
            case DM:
                jdbcUrl = String.format("jdbc:dm://%s:%d/%s", address, port, dbName); // 达梦数据库
                break;
            case HANA:
                jdbcUrl =
                        String.format(
                                "jdbc:sap://%s:%d/?databaseName=%s",
                                address, port, dbName); // 华为HANA
                break;
            case PHOENIX:
                jdbcUrl =
                        String.format(
                                "jdbc:phoenix:%s:%d", address, port); // Phoenix无dbName，端口默认2181
                break;
            default:
                jdbcUrl =
                        String.format(
                                "jdbc:postgresql://%s:%d/%s",
                                address, port, dbName); // 默认PostgreSQL
                break;
        }
        return jdbcUrl;
    }

    /**
     * 根据数据库类型获取对应的连接校验SQL 不同数据库的校验SQL不同，避免校验失败
     *
     * @param dbType 数据库类型
     * @return 对应数据库的校验SQL
     */
    /**
     * 根据数据库类型获取对应的连接校验SQL（兼容Java 8） 不同数据库的校验SQL不同，避免校验失败
     *
     * @param dbType 数据库类型
     * @return 对应数据库的校验SQL
     */
    public static String getValidationQuery(DialectName dbType) {
        String validationQuery;
        switch (dbType) {
            case MYSQL:
            case POSTGRESQL:
            case H2:
                validationQuery = "SELECT 1";
                break;
            case ORACLE:
                validationQuery = "SELECT 1 FROM DUAL";
                break;
            case SQLSERVER:
                validationQuery = "SELECT 1";
                break;
            case SQLITE3:
                validationQuery = "SELECT 1";
                break;
            case DM:
                validationQuery = "SELECT 1 FROM DUAL";
                break;
            case HANA:
                validationQuery = "SELECT 1 FROM DUMMY";
                break;
            case PHOENIX:
                validationQuery = "SELECT 1 FROM SYSTEM.CATALOG LIMIT 1";
                break;
            default:
                validationQuery = "SELECT 1";
                break;
        }
        return validationQuery;
    }
}
