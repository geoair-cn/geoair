package cn.geoair.comp.dynamic.ds.dswrapper.wrapper;

import cn.geoair.base.Gir;
import com.alibaba.druid.pool.DruidDataSource;

import javax.sql.DataSource;

/**
 * Druid数据源包装器
 */
public class DruidDataSourceWrapper extends GirAbstractDataSourceWrapper {

    public DruidDataSourceWrapper(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    protected Class<? extends DataSource> getTargetDataSourceClass() {
        return DruidDataSource.class;
    }

    static Boolean canInit = null;

    @Override
    public boolean close() {
        DruidDataSource dataSource = (DruidDataSource) targetDataSource;
        try {
            dataSource.close();
        } catch (Exception e) {
            Gir.log.error(e);
        }
        return true;
    }

    public static boolean canInit() {
        if (canInit != null) {
            return canInit;
        }
        try {
            Class.forName("com.alibaba.druid.pool.DruidDataSource");
            canInit = true;
        } catch (ClassNotFoundException e) {
            return false;
        }
        return canInit;
    }

    @Override
    public String getSimpleDataSourceName() {
        DruidDataSource druidDataSource = (DruidDataSource) targetDataSource;
        return druidDataSource.getName() == null ?
                targetDataSource.getClass().getSimpleName() + "@" + targetDataSource.hashCode() :
                druidDataSource.getName();
    }

    @Override
    public String getJdbcUrl() {
        DruidDataSource druidDataSource = (DruidDataSource) targetDataSource;
        return druidDataSource.getRawJdbcUrl();
    }

    @Override
    public Integer getActiveCount() {
        return getDruidDataSource().getActiveCount();
    }


    public DruidDataSource getDruidDataSource() {
        if (isSupport()) {
            return (DruidDataSource) super.targetDataSource;
        }
        throw new IllegalArgumentException("当前数据源不是Druid数据源");
    }
}
