package cn.geoair.comp.dynamic.ds.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.function.Consumer;
import javax.sql.DataSource;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * HikariCP 数据源快速创建工具类
 *
 * <p>注意：HikariCP 官方推荐 minimumIdle = maximumPoolSize 以获得最佳性能
 */
@Data
@Accessors(chain = true)
public class DataSourceHikariFastCreate {

    // ==================== 必须配置 ====================
    private String jdbcUrl; // 对应 Druid 的 url
    private String username;
    private String password;

    // ==================== 连接池核心配置 ====================
    /**
     * 最大连接数（对应 Druid 的 maxActive） 官方建议：根据 ((core_count * 2) + effective_spindle_count) 公式计算 默认值：10
     */
    private Integer maximumPoolSize = 10;

    /** 最小空闲连接数（对应 Druid 的 minIdle） HikariCP 官方建议设为与 maximumPoolSize 相同，避免动态扩缩容开销 */
    private Integer minimumIdle = 10;

    /** 获取连接的超时时间（毫秒）（对应 Druid 的 maxWait） 默认值：30000 (30秒) 建议调小至 1000~3000，让业务快速失败 */
    private Long connectionTimeout = 30000L;

    /** 连接空闲超时时间（毫秒）（对应 Druid 的 minEvictableIdleTimeMillis） 默认值：600000 (10分钟) 注意：必须小于 maxLifetime */
    private Long idleTimeout = 600000L;

    /** 连接最大存活时间（毫秒） 默认值：1800000 (30分钟) 必须小于数据库的 wait_timeout（如 MySQL 默认 8 小时） */
    private Long maxLifetime = 1800000L;

    // ==================== 连接健康检查 ====================
    /** 连接池名称（便于监控定位） */
    private String poolName = "HikariPool";

    /**
     * 验证查询 SQL（对应 Druid 的 validationQuery） 对于支持 JDBC4 的驱动（MySQL 5+、PostgreSQL 8+），通常无需设置
     * 若必须设置，推荐：SELECT 1
     */
    private String connectionTestQuery;

    /** 验证超时时间（毫秒） 默认值：5000 (5秒) */
    private Long validationTimeout = 5000L;

    /** 连接泄漏检测阈值（毫秒） 当连接持有时间超过此值，会打印堆栈日志（0 表示关闭） 推荐开发/测试环境设置为 30000~60000，生产环境谨慎开启 */
    private Long leakDetectionThreshold;

    /** 是否自动提交事务 默认值：true */
    private Boolean autoCommit = true;

    /** 连接初始化 SQL（连接创建后立即执行） 例如：SET NAMES utf8mb4 或 SET time_zone = '+8:00' */
    private String connectionInitSql;

    /** 是否允许在池中暂停连接（Druid 无此概念，保留为可选） */
    private Boolean allowPoolSuspension = false;

    /** 数据源缓存相关配置 */
    private Long catalog;

    private Long schema;
    private Boolean readOnly; // 是否只读数据源

    private Consumer<HikariConfig> configurator = t -> {};

    // ==================== 静态工厂方法 ====================

    /**
     * 快速创建数据源（仅使用必需参数）
     *
     * @param jdbcUrl 数据库连接URL
     * @param username 用户名
     * @param password 密码
     * @return HikariCP 数据源
     */
    public static DataSource create(String jdbcUrl, String username, String password) {
        return new DataSourceHikariFastCreate()
                .setJdbcUrl(jdbcUrl)
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
     * DataSource ds = DataSourceHikariFastCreate.create(builder -> builder
     *     .setJdbcUrl("jdbc:mysql://localhost:3306/test")
     *     .setUsername("root")
     *     .setPassword("123456")
     *     .setMaximumPoolSize(20)
     *     .setConnectionTimeout(5000L)
     * );
     * </pre>
     *
     * @param configurer 配置函数，用于设置连接参数
     * @return HikariCP 数据源
     */
    public static DataSource create(Consumer<DataSourceHikariFastCreate> configurer) {
        DataSourceHikariFastCreate builder = new DataSourceHikariFastCreate();
        configurer.accept(builder);
        return builder.toDataSource();
    }

    /**
     * 创建 HikariCP 数据源（支持自定义配置扩展）
     *
     * <p>使用示例：
     *
     * <pre>
     * DataSource ds = DataSourceHikariFastCreate.create(
     *     "jdbc:mysql://localhost:3306/test",
     *     "root",
     *     "123456",
     *     config -> {
     *         config.setMaximumPoolSize(30);
     *         config.setConnectionTimeout(10000L);
     *         config.setLeakDetectionThreshold(60000L);
     *     }
     * );
     * </pre>
     *
     * @param jdbcUrl 数据库连接URL
     * @param username 用户名
     * @param password 密码
     * @param configurer 额外配置函数
     * @return HikariCP 数据源
     */
    public static DataSource create(
            String jdbcUrl,
            String username,
            String password,
            Consumer<DataSourceHikariFastCreate> configurer) {
        DataSourceHikariFastCreate builder =
                new DataSourceHikariFastCreate()
                        .setJdbcUrl(jdbcUrl)
                        .setUsername(username)
                        .setPassword(password);
        if (configurer != null) {
            configurer.accept(builder);
        }
        return builder.toDataSource();
    }

    /** 创建 HikariCP 数据源 */
    public DataSource toDataSource() {
        HikariConfig config = new HikariConfig();

        // 必须配置项
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("jdbcUrl must not be null or empty");
        }
        if (!jdbcUrl.contains("sqlite")) {
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("username must not be null or empty");
            }
            if (password == null) {
                throw new IllegalArgumentException("password must not be null");
            }
        }
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        // 连接池大小配置
        if (maximumPoolSize != null) {
            config.setMaximumPoolSize(maximumPoolSize);
        }
        if (minimumIdle != null) {
            config.setMinimumIdle(minimumIdle);
        }

        // 超时配置
        if (connectionTimeout != null) {
            config.setConnectionTimeout(connectionTimeout);
        }
        if (idleTimeout != null) {
            config.setIdleTimeout(idleTimeout);
        }
        if (maxLifetime != null) {
            config.setMaxLifetime(maxLifetime);
        }

        // 连接名称与健康检查
        if (poolName != null) {
            config.setPoolName(poolName);
        }
        if (connectionTestQuery != null && !connectionTestQuery.trim().isEmpty()) {
            config.setConnectionTestQuery(connectionTestQuery);
        }
        if (validationTimeout != null) {
            config.setValidationTimeout(validationTimeout);
        }
        if (leakDetectionThreshold != null) {
            config.setLeakDetectionThreshold(leakDetectionThreshold);
        }
        if (autoCommit != null) {
            config.setAutoCommit(autoCommit);
        }
        if (connectionInitSql != null && !connectionInitSql.trim().isEmpty()) {
            config.setConnectionInitSql(connectionInitSql);
        }
        if (allowPoolSuspension != null) {
            config.setAllowPoolSuspension(allowPoolSuspension);
        }
        if (readOnly != null) {
            config.setReadOnly(readOnly);
        }
        // 额外优化：注册 JMX 监控 Bean（便于通过 JConsole 等工具监控）
        config.setRegisterMbeans(true);
        if (configurator != null) {
            configurator.accept(config);
        }
        return new HikariDataSource(config);
    }
}
