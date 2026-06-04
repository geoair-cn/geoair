package cn.geoair.comp.dynamic.ds;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.base.IDsDataSourceOpt;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Supplier;

/**
 * @author ：zhangjun
 * @date ： 模拟的DataSourceGetter,用于调试使用
 */
public class MockDataSourceGetter implements IDsDataSourceOpt {

    public static IDsDataSourceOpt getInstance() {
        return new MockDataSourceGetter();
    }

    private static final GiLogger log = GirLogger.getLoger();


    @Override
    public void initByDataSourceApo(DataSourceApo dataSourceApo) {

    }

    @Override
    public void initByDataSource(DataSource dataSource) {

    }

    @Override
    public void initByDataSource(DataSource dataSource, String dataSourceName) {

    }

    @Override
    public void initByConnection(Connection connection) {

    }

    @Override
    public String getSchemaName() {
        return "";
    }

    @Override
    public String getDatabaseName() {
        return "";
    }

    @Override
    public void setSchemaNameGetterFunction(Supplier<String> schemaNameGetterFunction) {

    }

    @Override
    public void setDatabaseNameGetterFunction(Supplier<String> databaseNameGetterFunction) {

    }

    @Override
    public String getDataSourceId() {
        return "";
    }


    @Override
    public DataSourceApo getDataSourceApo() {
        return null;
    }

    @Override
    public DataSource getDataSource() {
        return null;
    }


}
