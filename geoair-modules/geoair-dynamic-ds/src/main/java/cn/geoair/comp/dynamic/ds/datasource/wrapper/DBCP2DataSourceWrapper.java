package cn.geoair.comp.dynamic.ds.datasource.wrapper;

import org.apache.commons.dbcp.BasicDataSource;

import javax.sql.DataSource;

/**
 * Apache DBCP2 数据源包装器
 */
public class DBCP2DataSourceWrapper extends AbstractDataSourceWrapper {

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
    public String getSimpleDataSourceName() {
        BasicDataSource basicDataSource = (BasicDataSource) targetDataSource;
        // DBCP2的name属性是可选的，兜底返回固定标识
        return basicDataSource.getUrl() != null ? basicDataSource.getUrl() : null;
    }

    @Override
    public String getJdbcUrl() {
        BasicDataSource basicDataSource = (BasicDataSource) targetDataSource;
        return basicDataSource.getUrl();
    }

    public BasicDataSource getDBCP2DataSource() {
        if (isSupport()) {
            return (BasicDataSource) targetDataSource;
        }
        throw new IllegalArgumentException("当前数据源不是DBCP2数据源");
    }
}
