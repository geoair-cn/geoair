package cn.geoair.comp.dynamic.ds.dswrapper.wrapper;

import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;

import javax.sql.DataSource;

/** 数据源包装器抽象基类 实现通用逻辑，子类只需实现特定方法 */
public abstract class GirAbstractDataSourceWrapper implements AdvDataSourceWrapper {

    // 被包装的原始数据源
    protected DataSource targetDataSource;

    @Override
    public String toString() {
        return "AbstractDataSourceWrapper{" + "targetDataSource=" + targetDataSource + '}';
    }

    public GirAbstractDataSourceWrapper(DataSource targetDataSource) {
        this.targetDataSource = targetDataSource;
    }

    /** 获取包装的原始数据源 */
    @Override
    public DataSource getTargetDataSource() {
        return targetDataSource;
    }

    /** 默认实现：判断当前数据源是否是目标类型 */
    @Override
    public boolean isSupport() {
        return getTargetDataSourceClass().isInstance(targetDataSource);
    }

    /** 子类需实现：返回目标数据源的Class类型 */
    protected abstract Class<? extends DataSource> getTargetDataSourceClass();

    /** 数据源通用方法转发（实现DataSource接口的默认转发） */
    @Override
    public java.sql.Connection getConnection() throws java.sql.SQLException {
        return targetDataSource.getConnection();
    }

    @Override
    public java.sql.Connection getConnection(String username, String password)
            throws java.sql.SQLException {
        return targetDataSource.getConnection(username, password);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws java.sql.SQLException {
        return targetDataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws java.sql.SQLException {
        return targetDataSource.isWrapperFor(iface);
    }

    @Override
    public int getLoginTimeout() throws java.sql.SQLException {
        return targetDataSource.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int seconds) throws java.sql.SQLException {
        targetDataSource.setLoginTimeout(seconds);
    }

    @Override
    public java.io.PrintWriter getLogWriter() throws java.sql.SQLException {
        return targetDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(java.io.PrintWriter out) throws java.sql.SQLException {
        targetDataSource.setLogWriter(out);
    }

    @Override
    public java.util.logging.Logger getParentLogger()
            throws java.sql.SQLFeatureNotSupportedException {
        return targetDataSource.getParentLogger();
    }
}
