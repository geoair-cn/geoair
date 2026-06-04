package cn.geoair.comp.dynamic.ds;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.tx.IDsTxHolder;
import cn.geoair.comp.dynamic.ds.tx.GirDsJdbcTxBuilder;
import cn.geoair.comp.dynamic.ds.tx.TxAction;
import cn.geoair.comp.dynamic.ds.tx.TxFunc;
import cn.geoair.comp.dynamic.ds.tx.enums.IsolationLevel;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.function.Supplier;

/**
 * @author ：zhangjun
 * @date ： 模拟的DataSourceGetter,用于调试使用
 */
public class MockDataSourceGetter implements IDataSourceGetter {

    public static IDataSourceGetter getInstance() {
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
    public Connection getConnection() {
        return null;
    }

    @Override
    public DataSourceApo getDataSourceApo() {
        return null;
    }

    @Override
    public DataSource getDataSource() {
        return null;
    }

    @Override
    public void connectionClose(Connection connection) {

    }

    @Override
    public void closeResources(ResultSet rs, Statement stmt, Connection conn) {

    }

    @Override
    public void setJdbcTxHolder(IDsTxHolder jdbcTxHolder) {

    }

    @Override
    public void tx(Runnable action) {

    }

    @Override
    public void tx(IsolationLevel level, Runnable action) {

    }

    @Override
    public <T> T txReturn(Supplier<T> supplier) {
        return null;
    }

    @Override
    public <T> T txReturn(IsolationLevel level, Supplier<T> supplier) {
        return null;
    }

    @Override
    public <P> void tx(TxAction<P> action, P param) {

    }

    @Override
    public <P> void tx(IsolationLevel level, TxAction<P> action, P param) {

    }

    @Override
    public <P, R> R txReturn(TxFunc<P, R> func, P param) {
        return null;
    }

    @Override
    public <P, R> R txReturn(IsolationLevel level, TxFunc<P, R> func, P param) {
        return null;
    }

    @Override
    public GirDsJdbcTxBuilder builder() {
        return null;
    }
}
