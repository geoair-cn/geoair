package cn.geoair.comp.dynamic.ds.dswrapper.wrapper;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;

import org.apache.commons.dbcp.BasicDataSource;

import java.sql.SQLException;

import javax.sql.DataSource;

/** Apache DBCP2 数据源包装器 */
public class DBCP2DataSourceWrapper extends GirAbstractDataSourceWrapper {
    public static GiLogger log = GirLoggerFactory.getLogger();
    private static Boolean canInit = null;

    public DBCP2DataSourceWrapper(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    protected Class<? extends DataSource> getTargetDataSourceClass() {
        return BasicDataSource.class;
    }

    public static boolean canInit() {
        if (canInit != null) {
            return canInit;
        }
        try {
            Class.forName("org.apache.commons.dbcp.BasicDataSource");
            canInit = true;
        } catch (ClassNotFoundException e) {
            canInit = false;
        }
        return canInit;
    }

    @Override
    public boolean close() {
        try {
            getDBCP2DataSource().close();
        } catch (SQLException e) {
        }
        return true;
    }

    @Override
    public String getSimpleDataSourceName() {
        BasicDataSource basicDataSource = getDBCP2DataSource();
        // DBCP2的name属性是可选的，兜底返回固定标识
        return basicDataSource.getUrl() != null
                ? basicDataSource.getUrl()
                : targetDataSource.getClass().getSimpleName() + "@" + targetDataSource.hashCode();
    }

    @Override
    public String getJdbcUrl() {
        BasicDataSource basicDataSource = getDBCP2DataSource();
        return basicDataSource.getUrl();
    }

    @Override
    public Integer getActiveCount() {
        BasicDataSource basicDataSource = getDBCP2DataSource();
        if (basicDataSource == null) {
            return null; // 数据源不存在，返回null
        }

        try {
            // DBCP2 获取活跃连接数的方式
            // 总连接数 - 空闲连接数 = 活跃连接数
            int totalConnections = basicDataSource.getNumActive(); // 活跃连接数（正在使用的）
            // 注意：DBCP2 的 getNumActive() 直接返回的就是活跃连接数

            return totalConnections;
        } catch (Exception e) {
            log.error("获取DBCP2活跃连接数失败", e);
            return null; // 异常时返回null
        }
    }

    public BasicDataSource getDBCP2DataSource() {
        if (isSupport()) {
            return (BasicDataSource) targetDataSource;
        }
        throw new IllegalArgumentException("当前数据源不是DBCP2数据源");
    }
}
