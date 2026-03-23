package cn.geoair.comp.dynamic.ds.datasource.wrapper;

import cn.geoair.comp.dynamic.ds.simple.DriverManagerDataSource;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;


public class DiverManagerSourceWrapper extends AbstractDataSourceWrapper {


    public DiverManagerSourceWrapper(DataSource targetDataSource) {
        super(targetDataSource);
    }


    public static boolean canInit() {
        return true;
    }

    @Override
    protected Class<? extends DataSource> getTargetDataSourceClass() {
        return DriverManagerDataSource.class;
    }

    @Override
    public String getSimpleDataSourceName() {
        DriverManagerDataSource dataSource = (DriverManagerDataSource) targetDataSource;
        return dataSource.getUrl();
    }

    @Override
    public String getJdbcUrl() {
        DriverManagerDataSource dataSource = (DriverManagerDataSource) targetDataSource;
        return dataSource.getUrl();
    }


}
