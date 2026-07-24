package cn.geoair.comp.dynamic.ds.readwrite;

import cn.geoair.comp.dynamic.ds.AdvDynamicDataSourceStorage;
import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;
import cn.geoair.comp.dynamic.ds.readwrite.log.RdLog;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * 延迟加载的数据源包装器
 *
 * @author 张俊
 * @date Created in 2026/5/28
 */
public class LazyDataSourceWrapper implements DataSource {

    private final String dataSourceId;
    private volatile AdvDataSourceWrapper realDataSource;
    private final ReentrantLock lock = new ReentrantLock();

    /** 当数据源初始化的时候，进行调用这个消费者对外进行回调 */
    BiConsumer<String, AdvDataSourceWrapper> dataSourceWrapperConsumer =
            (dataSourceId, dataSourceWrapper) -> {};

    public LazyDataSourceWrapper(String dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public LazyDataSourceWrapper(
            String dataSourceId,
            BiConsumer<String, AdvDataSourceWrapper> dataSourceWrapperConsumer) {
        this.dataSourceId = dataSourceId;
        this.dataSourceWrapperConsumer = dataSourceWrapperConsumer;
    }

    /**
     * 直接进行初始化真实数据源，这一步非必要不用执行，因为数据源真正使用的时候会进行懒加载的。
     *
     * @return 获取真实数据源的包装对象
     */
    public LazyDataSourceWrapper init() {
        getRealDataSource();
        return this;
    }

    public LazyDataSourceWrapper setDataSourceWrapperConsumer(
            BiConsumer<String, AdvDataSourceWrapper> dataSourceWrapperConsumer) {
        this.dataSourceWrapperConsumer = dataSourceWrapperConsumer;
        return this;
    }

    /** 获取真实的数据源（延迟加载） */
    private AdvDataSourceWrapper getRealDataSource() {
        if (realDataSource == null) {
            lock.lock();
            try {
                if (realDataSource == null) {
                    RdLog.getInstance().debug("延迟加载数据源: {}", dataSourceId);
                    realDataSource =
                            AdvDynamicDataSourceStorage.getInstance()
                                    .getOrCreateDataSource(dataSourceId);
                    dataSourceWrapperConsumer.accept(dataSourceId, realDataSource);
                    if (realDataSource == null) {
                        throw new IllegalStateException("数据源不存在: " + dataSourceId);
                    }
                    RdLog.getInstance().debug("数据源加载完成: {}", dataSourceId);
                }
            } finally {
                lock.unlock();
            }
        }
        return realDataSource;
    }

    /** 检查数据源是否已加载 */
    public boolean isLoaded() {
        return realDataSource != null;
    }

    /** 获取数据源ID */
    public String getDataSourceId() {
        return dataSourceId;
    }

    // ==================== DataSource 接口实现 ====================

    @Override
    public Connection getConnection() throws SQLException {
        return getRealDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getRealDataSource().getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return getRealDataSource().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        getRealDataSource().setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        getRealDataSource().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return getRealDataSource().getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return getRealDataSource().getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return getRealDataSource().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return getRealDataSource().isWrapperFor(iface);
    }

    /** 获取JDBC URL（用于调试） */
    public String getJdbcUrl() {
        if (realDataSource != null) {
            return realDataSource.getJdbcUrl();
        }
        return "lazy:" + dataSourceId;
    }

    /** 获取活跃连接数 */
    public Integer getActiveCount() {
        if (realDataSource != null) {
            return realDataSource.getActiveCount();
        }
        return null; // 未加载时认为0
    }
}
