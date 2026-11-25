package cn.geoair.comp.dynamic.ds.dswrapper.wrapper;

import cn.geoair.comp.dynamic.ds.simple.AdvSimpleDataSource;

import javax.sql.DataSource;

public class AdvSimpleDataSourceWrapper extends GirAbstractDataSourceWrapper {

    public AdvSimpleDataSourceWrapper(DataSource targetDataSource) {
        super(targetDataSource);
    }

    public static boolean canInit() {
        return true;
    }

    @Override
    public boolean close() {
        return true;
    }

    protected Class<? extends DataSource> getTargetDataSourceClass() {
        return AdvSimpleDataSource.class;
    }

    @Override
    public String getSimpleDataSourceName() {
        return targetDataSource.getClass().getSimpleName() + "@" + targetDataSource.hashCode();
    }

    @Override
    public String getJdbcUrl() {
        return null;
    }

    @Override
    public Integer getActiveCount() {
        return null;
    }
}
