package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.hutool.core.io.IoUtil;
import cn.hutool.db.dialect.DialectName;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;


/** 高级查询执行器工厂，根据数据源类型自动创建对应执行器 */

public class AdvExecutorFactory {
    public static GiLogger log = GirLoggerFactory.getLogger();

    /** 以稳定方言标识索引的执行器提供者。 */
    private static final Map<String, AdvDialectProvider> DIALECT_PROVIDERS = new ConcurrentHashMap<>();

    /** 兼容既有 Hutool DialectName API 的默认提供者索引。 */
    private static final Map<DialectName, AdvDialectProvider> HUTOOL_DIALECT_PROVIDERS = new ConcurrentHashMap<>();

    /**
     * JDBC 产品名匹配顺序。ConcurrentHashMap 的 values() 没有稳定迭代顺序，不能直接用于
     * 包含匹配；这里保留注册顺序，使出现关键字重叠时的结果可预测。
     */
    private static final CopyOnWriteArrayList<AdvDialectProvider> PRODUCT_NAME_PROVIDERS = new CopyOnWriteArrayList<>();

    static {
        registerProvider(new MysqlAdvDialectProvider());
        registerProvider(new MariaDbAdvDialectProvider());
        registerProvider(new PostgresqlAdvDialectProvider());
        registerProvider(new KingbaseAdvDialectProvider());
        registerProvider(new OpenGaussAdvDialectProvider());
        registerProvider(new SqlServerAdvDialectProvider());
        registerProvider(new OracleAdvDialectProvider());
        registerProvider(new DmAdvDialectProvider());
    }

    /**
     * 注册一个数据库方言提供者。
     * <p>同一方言标识只能注册一次，避免某个扩展模块在运行期悄悄覆盖已有数据库实现。
     */
    public static void registerProvider(AdvDialectProvider provider) {
        if (provider == null || isBlank(provider.getDialectId())) {
            throw new IllegalArgumentException("AdvDialectProvider 和 dialectId 不能为空");
        }
        String dialectId = normalizeDialectId(provider.getDialectId());
        AdvDialectProvider previous = DIALECT_PROVIDERS.putIfAbsent(dialectId, provider);
        if (previous != null && previous.getClass() != provider.getClass()) {
            throw new IllegalStateException("AdvQuery 方言已注册：" + dialectId);
        }
        if (previous == null) {
            PRODUCT_NAME_PROVIDERS.add(provider);
        }
        if (provider.getDialectName() != null) {
            HUTOOL_DIALECT_PROVIDERS.putIfAbsent(provider.getDialectName(), provider);
        }
    }

    /** 返回已注册方言的只读视图，供启动诊断或文档展示使用。 */
    public static Collection<AdvDialectProvider> getRegisteredProviders() {
        return Collections.unmodifiableCollection(DIALECT_PROVIDERS.values());
    }

    /** 按方言标识获取提供者；未注册时返回 {@code null}。 */
    public static AdvDialectProvider getProvider(String dialectId) {
        return findProviderByDialectId(dialectId);
    }

    /**
     * 将 JDBC 产品名称解析为已注册的方言标识。
     * <p>该方法不获取连接，适合在数据源初始化前做配置校验。
     */
    public static String resolveDialectIdByProductName(String databaseProductName) {
        AdvDialectProvider provider = findProviderByProductName(databaseProductName);
        return provider == null ? null : provider.getDialectId();
    }

    /** 按稳定方言标识创建执行器，不会触发 JDBC 元数据探测。 */
    public static IAdvExecutor getAdvExecutorByDialectId(
            String dialectId, DataSource dataSource, String dataSourceName) {
        AdvDialectProvider provider = findProviderByDialectId(dialectId);
        if (provider == null) {
            throw new UnsupportedOperationException("不支持的数据库方言标识：" + dialectId);
        }
        return provider.create(dataSource, dataSourceName);
    }


    public static IAdvExecutor getAdvExecutorByDataSource(DataSource dataSource) {
        return getAdvExecutorByDataSource(dataSource, null);
    }

    public static IAdvExecutor getAdvExecutorByDataSource(
            DataSource dataSource, String dataSourceName) {

        String databaseProductName = getDatabaseProductName(dataSource);
        AdvDialectProvider provider = findProviderByProductName(databaseProductName);
        if (provider == null) {
            throw new UnsupportedOperationException("无法识别或未注册的数据库类型：" + databaseProductName);
        }
        log.trace("检测到{}数据源，使用{}方言创建Executor执行器", databaseProductName, provider.getDialectId());
        return provider.create(dataSource, dataSourceName);
    }

    /**
     * 根据方言名称直接创建执行器（跳过 JDBC 连接探测，性能更高）
     *
     * <p>适用于调用方已经明确知道数据库类型的场景，避免 {@link #getAdvExecutorByDataSource(DataSource)}
     * 中通过 {@code DatabaseMetaData.getDatabaseProductName()} 探测数据库类型带来的额外连接开销。</p>
     *
     * @param dialectName    数据库方言
     * @param dataSource     数据源对象
     * @param dataSourceName 数据源名称（可为 null）
     * @return 匹配的 IAdvExecutor 实现类
     */
    public static IAdvExecutor getAdvExecutorByDialect(
            DialectName dialectName, DataSource dataSource, String dataSourceName) {

        AdvDialectProvider provider = findProviderByDialectName(dialectName);
        if (provider == null) {
            throw new UnsupportedOperationException("不支持的数据库方言：" + dialectName);
        }
        return provider.create(dataSource, dataSourceName);
    }

    private static AdvDialectProvider findProviderByDialectId(String dialectId) {
        if (isBlank(dialectId)) {
            return null;
        }
        return DIALECT_PROVIDERS.get(normalizeDialectId(dialectId));
    }

    private static AdvDialectProvider findProviderByDialectName(DialectName dialectName) {
        if (dialectName == null) {
            throw new IllegalArgumentException("dialectName 不能为空，请指定数据库方言");
        }
        return HUTOOL_DIALECT_PROVIDERS.get(dialectName);
    }

    private static AdvDialectProvider findProviderByProductName(String databaseProductName) {
        for (AdvDialectProvider provider : PRODUCT_NAME_PROVIDERS) {
            if (provider.supportsProductName(databaseProductName)) {
                return provider;
            }
        }
        return null;
    }

    /** 从数据源读取 JDBC 产品名称。 */
    private static String getDatabaseProductName(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource 不能为空");
        }
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            return metaData.getDatabaseProductName();
        } catch (SQLException e) {
            log.error("解析数据源类型失败", e);
            throw new RuntimeException("获取数据库类型失败", e);
        } finally {
            if (conn != null) {
                IoUtil.close(conn);
            }
        }
    }

    private static String normalizeDialectId(String dialectId) {
        return dialectId.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
