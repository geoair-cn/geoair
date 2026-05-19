package cn.geoair.comp.dynamic.ds.dswrapper.wrapper;

import cn.geoair.base.Gir;
import cn.geoair.comp.dynamic.ds.simple.DriverManagerDataSource;

import javax.sql.DataSource;

public class DiverManagerSourceWrapper extends GirAbstractDataSourceWrapper {

    public DiverManagerSourceWrapper(DataSource targetDataSource) {
        super(targetDataSource);
    }

    public static boolean canInit() {
        return true;
    }

    @Override
    public boolean close() {
        DriverManagerDataSource dataSource = (DriverManagerDataSource) targetDataSource;
        try {
            dataSource.close();
        } catch (Exception e) {
            Gir.log.error(e);
        }
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
