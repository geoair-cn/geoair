package cn.geoair.comp.dynamic.ds.utils;

import com.alibaba.druid.pool.DruidDataSource;
import java.util.function.Consumer;
import javax.sql.DataSource;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DataSourceDruidFastCreate {

    private String url;
    private String username;
    private String password;

    // Druid 可选配置
    private Integer initialSize = 1;
    private Integer queryTimeout = 120;
    private Integer minIdle = 5;
    private Integer maxActive = 20;
    private Long maxWait = 60000L;
    private Long timeBetweenEvictionRunsMillis = 60000L;
    private Long minEvictableIdleTimeMillis = 1800000L;

    private Boolean testWhileIdle = true;
    private Boolean testOnBorrow = false;
    private Boolean testOnReturn = false;
    private Boolean poolPreparedStatements = true;
    private Integer maxPoolPreparedStatementPerConnectionSize = 20;

    private Integer removeAbandonedTimeout = 300; // 连接泄漏回收超时（秒）
    private Integer connectionErrorRetryAttempts = 3; // 连接错误重试次数
    private String validationQuery; // 验证查询SQL
    private Integer numTestsPerEvictionRun = -1; // 每次检测的连接数

    // ==================== 静态工厂方法 ====================

    /**
     * 快速创建数据源（仅使用必需参数）
     *
     * @param url 数据库连接URL
     * @param username 用户名
     * @param password 密码
     * @return Druid 数据源
     */
    public static DataSource create(String url, String username, String password) {
        return new DataSourceDruidFastCreate()
                .setUrl(url)
                .setUsername(username)
                .setPassword(password)
                .toDataSource();
    }

    /**
     * 使用 Consumer 配置模式创建数据源
     *
     * <p>使用示例：
     *
     * <pre>
     * DataSource ds = DataSourceDruidFastCreate.create(builder -> builder
     *     .setUrl("jdbc:mysql://localhost:3306/test")
     *     .setUsername("root")
     *     .setPassword("123456")
     *     .setMaxActive(20)
     *     .setMaxWait(30000L)
     * );
     * </pre>
     *
     * @param configurer 配置函数，用于设置连接参数
     * @return Druid 数据源
     */
    public static DataSource create(Consumer<DataSourceDruidFastCreate> configurer) {
        DataSourceDruidFastCreate builder = new DataSourceDruidFastCreate();
        configurer.accept(builder);
        return builder.toDataSource();
    }

    /**
     * 创建 Druid 数据源（支持自定义配置扩展）
     *
     * <p>使用示例：
     *
     * <pre>
     * DataSource ds = DataSourceDruidFastCreate.create(
     *     "jdbc:mysql://localhost:3306/test",
     *     "root",
     *     "123456",
     *     config -> {
     *         config.setMaxActive(30);
     *         config.setMaxWait(10000L);
     *         config.setRemoveAbandonedTimeout(600);
     *     }
     * );
     * </pre>
     *
     * @param url 数据库连接URL
     * @param username 用户名
     * @param password 密码
     * @param configurer 额外配置函数
     * @return Druid 数据源
     */
    public static DataSource create(
            String url,
            String username,
            String password,
            Consumer<DataSourceDruidFastCreate> configurer) {
        DataSourceDruidFastCreate builder =
                new DataSourceDruidFastCreate()
                        .setUrl(url)
                        .setUsername(username)
                        .setPassword(password);
        if (configurer != null) {
            configurer.accept(builder);
        }
        return builder.toDataSource();
    }

    /**
     * 快速创建数据源并返回 DruidDataSource 对象（支持更详细的配置）
     *
     * @param url 数据库连接URL
     * @param username 用户名
     * @param password 密码
     * @return DruidDataSource 对象
     */
    public static DruidDataSource createDruid(String url, String username, String password) {
        return (DruidDataSource) create(url, username, password);
    }

    /**
     * 使用 Consumer 配置模式创建 DruidDataSource 对象
     *
     * @param configurer 配置函数
     * @return DruidDataSource 对象
     */
    public static DruidDataSource createDruid(Consumer<DataSourceDruidFastCreate> configurer) {
        return (DruidDataSource) create(configurer);
    }

    /** 创建 Druid 数据源 */
    public DataSource toDataSource() {
        // 参数校验
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("url must not be null or empty");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username must not be null or empty");
        }
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 连接池配置
        if (initialSize != null) {
            dataSource.setInitialSize(initialSize);
        }
        if (minIdle != null) {
            dataSource.setMinIdle(minIdle);
        }
        if (maxActive != null) {
            dataSource.setMaxActive(maxActive);
        }
        if (maxWait != null) {
            dataSource.setMaxWait(maxWait);
        }
        if (timeBetweenEvictionRunsMillis != null) {
            dataSource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        }
        if (minEvictableIdleTimeMillis != null) {
            dataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        }

        if (testWhileIdle != null) {
            dataSource.setTestWhileIdle(testWhileIdle);
        }
        if (testOnBorrow != null) {
            dataSource.setTestOnBorrow(testOnBorrow);
        }
        if (testOnReturn != null) {
            dataSource.setTestOnReturn(testOnReturn);
        }
        if (poolPreparedStatements != null) {
            dataSource.setPoolPreparedStatements(poolPreparedStatements);
        }
        if (queryTimeout != null) {
            dataSource.setQueryTimeout(queryTimeout);
        }
        if (maxPoolPreparedStatementPerConnectionSize != null) {
            dataSource.setMaxPoolPreparedStatementPerConnectionSize(
                    maxPoolPreparedStatementPerConnectionSize);
        }
        if (removeAbandonedTimeout != null) {
            dataSource.setRemoveAbandonedTimeout(removeAbandonedTimeout);
        }
        if (connectionErrorRetryAttempts != null) {
            dataSource.setBreakAfterAcquireFailure(true);
            dataSource.setConnectionErrorRetryAttempts(connectionErrorRetryAttempts);
        }
        if (validationQuery != null) {
            dataSource.setValidationQuery(validationQuery);
        }
        if (numTestsPerEvictionRun != null) {
            dataSource.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        }

        // 强制开启连接泄漏检测
        dataSource.setBreakAfterAcquireFailure(true);
        dataSource.setRemoveAbandoned(true);
        dataSource.setLogAbandoned(true);

        return dataSource;
    }
}
