package cn.geoair.comp.dynamic.ds.apo;

import cn.geoair.comp.jdbc.url.enums.DatabaseType;
import cn.geoair.comp.jdbc.url.beans.JdbcEndpoint;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.JdbcUrlCodec;
import cn.geoair.comp.jdbc.url.GirJdbcUrlCodecs;
import cn.hutool.db.dialect.DialectName;
import cn.hutool.db.dialect.DriverNamePool;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 数据源的Api传递对象(Application Persistence Object)
 *
 * <p>用于在应用程序各层之间传递数据源相关信息的数据载体， 包含数据库连接所需的各类配置参数及元数据信息
 */
@Data
public class DataSourceApo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据源唯一标识ID
     */
    private String id;

    /**
     * 数据库驱动类名 默认使用PostgreSQL驱动，取值于Hutool工具类的DriverNamePool
     */
    private String driver = DriverNamePool.DRIVER_POSTGRESQL;

    /**
     * 数据库连接URL
     */
    private String jdbcUrl;

    /**
     * 数据源名称（通常用于显示和标识）
     */
    private String name;

    /**
     * 数据库服务器地址（IP或域名）
     */
    private String address;

    /**
     * 数据库服务端口号
     */
    private Integer port;

    /**
     * 数据库实例名称
     */
    private String dbName;

    /**
     * 数据库模式名称（Schema）
     */
    private String schemaName;

    /**
     * 数据库登录用户名
     */
    private String username;

    /**
     * 数据库登录密码
     */
    private String password;

    /**
     * 数据源创建时间
     */
    private Date createTime;

    /**
     * 数据源最后更新时间
     */
    private Date updateTime;


    // ======================数据源调优参数=============================
    private int initialSize = 2; // 初始连接数

    private int maxActive = 10; // 最大活跃连接数

    private int minIdle = 1; // 最小闲置连接数

    private long maxWait = 10000; // 获取连接的超时等待

    private Integer queryTimeout = 15; // 查询的超时时间 单位秒

    private Integer removeAbandonedTimeout = 1800; // 回收连接的超时时间


    private Integer connectionErrorRetryAttempts = 3;  // 链接获取失败的时候重试次数

    private String validationQuery; // 验证链接的SQL

    public void setJdbcUrl(String jdbcUrl) {
        JdbcUrl parsed = GirJdbcUrlCodecs.defaultCodec().parse(jdbcUrl);
        setDbName(parsed.getDatabaseName());
        JdbcEndpoint endpoint = parsed.getPrimaryEndpoint();
        if (getPort() == null && endpoint != null) {
            setPort(endpoint.getPort());
        }
        setAddress(endpoint == null ? null : endpoint.getHost());
        if (getSchemaName() == null) {
            setSchemaName(GirJdbcUrlCodecs.defaultCodec().getSchema(jdbcUrl));
        }
        this.jdbcUrl = jdbcUrl;
    }

    public String getJdbcUrl() {
        if (jdbcUrl == null) {
            jdbcUrl = createJdbcUrl(this);
        }

        return jdbcUrl;
    }

    /**
     * 判断当前数据源与传入的连接参数是否匹配
     *
     * <p>比较规则：数据库地址、端口、数据库名、用户名完全一致则认为匹配
     *
     * @param address 数据库地址
     * @param port    端口号
     * @param dbName  数据库名
     * @param user    用户名
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
        DatabaseType databaseType = DatabaseType.fromDriverClassName(driver);
        return databaseType == DatabaseType.UNKNOWN ? DialectName.POSTGRESQL : databaseType.getDialectName();
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
    public static String createJdbcUrl(DataSourceApo dataSourceApo) {
        DialectName dbType = dataSourceApo.getDbType();
        String address = dataSourceApo.getAddress();
        Integer port = dataSourceApo.getPort();
        String dbName = dataSourceApo.getDbName();
        String schemaName = dataSourceApo.getSchemaName();

        DatabaseType databaseType = DatabaseType.fromDialectName(dbType);
        if (databaseType == DatabaseType.UNKNOWN) {
            databaseType = DatabaseType.POSTGRESQL;
        }
        JdbcUrlCodec codec = GirJdbcUrlCodecs.defaultCodec();
        JdbcUrl jdbcUrl = codec.create(databaseType, address, port, dbName);
        if (databaseType == DatabaseType.MYSQL) {
            jdbcUrl = codec.withProperty(jdbcUrl, "useUnicode", "true");
            jdbcUrl = codec.withProperty(jdbcUrl, "characterEncoding", "utf8");
            jdbcUrl = codec.withProperty(jdbcUrl, "useSSL", "false");
            jdbcUrl = codec.withProperty(jdbcUrl, "serverTimezone", "UTC");
        }
        String result = codec.format(jdbcUrl);
        return databaseType == DatabaseType.POSTGRESQL && schemaName != null
                ? codec.rewriteSchema(result, schemaName) : result;
    }

    /**
     * @deprecated 请使用 {@link #createJdbcUrl(DataSourceApo)}，新实现由 geoair-jdbc-url 模块负责。
     */
    @Deprecated
    public static String buildJdbcUrl(DataSourceApo dataSourceApo) {
        return createJdbcUrl(dataSourceApo);
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
