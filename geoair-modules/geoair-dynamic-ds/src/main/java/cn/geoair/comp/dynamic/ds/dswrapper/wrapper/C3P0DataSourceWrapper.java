package cn.geoair.comp.dynamic.ds.dswrapper.wrapper;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import javax.sql.DataSource;

/** C3P0 数据源包装器 */
public class C3P0DataSourceWrapper extends GirAbstractDataSourceWrapper {

    private static Boolean canInit = null;

    public C3P0DataSourceWrapper(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public boolean close() {
        getC3P0DataSource().close();
        return true;
    }

    @Override
    protected Class<? extends DataSource> getTargetDataSourceClass() {
        return ComboPooledDataSource.class;
    }

    public static boolean canInit() {
        if (canInit != null) {
            return canInit;
        }
        try {
            Class.forName("com.mchange.v2.c3p0.ComboPooledDataSource");
            canInit = true;
        } catch (ClassNotFoundException e) {
            canInit = false;
        }
        return canInit;
    }

    @Override
    public String getSimpleDataSourceName() {
        ComboPooledDataSource c3p0DataSource = getC3P0DataSource() ;
        // C3P0的标识优先用dataSourceName，兜底返回c3p0
        return c3p0DataSource.getDataSourceName() != null
                ? c3p0DataSource.getDataSourceName()
                : "c3p0";
    }

    @Override
    public String getJdbcUrl() {
        ComboPooledDataSource c3p0DataSource =getC3P0DataSource() ;
        try {
            return c3p0DataSource.getJdbcUrl();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Integer getActiveCount() {
        ComboPooledDataSource c3p0 = getC3P0DataSource();
        if (c3p0 == null) {
            return null;
        }

        try {
            // C3P0 获取活跃连接数的正确方式：
            // 总连接数 - 空闲连接数 = 活跃连接数
            int totalConnections = c3p0.getNumConnectionsAllUsers();  // 总连接数
            int idleConnections = c3p0.getNumIdleConnectionsAllUsers();  // 空闲连接数
            int activeCount = totalConnections - idleConnections;

            return Math.max(0, activeCount);
        } catch (Exception e) {
            return null;
        }
    }

    public ComboPooledDataSource getC3P0DataSource() {
        if (isSupport()) {
            return (ComboPooledDataSource) targetDataSource;
        }
        throw new IllegalArgumentException("当前数据源不是C3P0数据源");
    }
}
