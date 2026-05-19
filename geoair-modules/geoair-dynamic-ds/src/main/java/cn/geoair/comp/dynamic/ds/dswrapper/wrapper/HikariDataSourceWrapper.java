package cn.geoair.comp.dynamic.ds.dswrapper.wrapper;

import cn.geoair.base.Gir;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/** HikariCP数据源包装器 */
public class HikariDataSourceWrapper extends GirAbstractDataSourceWrapper {

    public HikariDataSourceWrapper(DataSource targetDataSource) {
        super(targetDataSource);
    }

    static Boolean canInit = null;

    public static boolean canInit() {
        if (canInit != null) {
            return canInit;
        }
        try {
            Class.forName("com.zaxxer.hikari.HikariDataSource");
            canInit = true;
        } catch (ClassNotFoundException e) {
            return false;
        }
        return canInit;
    }

    @Override
    public boolean close() {
        HikariDataSource dataSource = (HikariDataSource) targetDataSource;
        try {
            dataSource.close();
        } catch (Exception e) {
            Gir.log.error(e);
        }
        return true;
    }

    @Override
    protected Class<? extends DataSource> getTargetDataSourceClass() {
        return HikariDataSource.class;
    }

    @Override
    public String getSimpleDataSourceName() {
        HikariDataSource dataSource = (HikariDataSource) targetDataSource;
        return dataSource.getJdbcUrl();
    }

    @Override
    public String getJdbcUrl() {
        HikariDataSource dataSource = (HikariDataSource) targetDataSource;
        return dataSource.getJdbcUrl();
    }

    /** 获取Hikari数据源的特有配置（可选扩展） */
    public HikariDataSource getHikariDataSource() {
        if (isSupport()) {
            return (HikariDataSource) super.targetDataSource;
        }
        throw new IllegalArgumentException("当前数据源不是Hikari数据源");
    }
}
