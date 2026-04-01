package cn.geoair.comp.dynamic.ds.dswrapper.wrapper;

import cn.geoair.comp.dynamic.ds.simple.AdvSimpleDataSource;
import javax.sql.DataSource;

/** HikariCP数据源包装器 */
public class AdvSimpleDataSourceWrapper extends AbstractDataSourceWrapper {

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

        return null;
    }

    @Override
    public String getJdbcUrl() {

        return null;
    }
}
