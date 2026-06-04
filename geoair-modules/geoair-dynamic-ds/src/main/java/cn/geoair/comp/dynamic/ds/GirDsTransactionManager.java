package cn.geoair.comp.dynamic.ds;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.tx.*;
import cn.geoair.comp.dynamic.ds.tx.enums.IsolationLevel;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

/**
 * 事务管理器
 *
 * @author ：zhangjun
 * @date ：Created in 2025/10/9 10:38
 */
public class GirDsTransactionManager implements IDataSourceGetter {

    private static final GiLogger log = GirLoggerFactory.getLogger();

    IDsConnectionOpt connectionManager;
    IDsDataSourceOpt dataSourceManger;
    IDsTxTemplate txTemplate;

    public GirDsTransactionManager(IDsConnectionOpt connectionManager, IDsDataSourceOpt dataSourceManger) {
        this.connectionManager = connectionManager;
        this.dataSourceManger = dataSourceManger;
        this.txTemplate = new GirDefaultIDsTxTemplate(connectionManager);
    }

    public GirDsTransactionManager(IDsDataSourceOpt dataSourceManger) {
        this.connectionManager = new RealConnectionOpt(dataSourceManger);
        this.dataSourceManger = dataSourceManger;
        this.txTemplate = new GirDefaultIDsTxTemplate(connectionManager);
    }

    public GirDsTransactionManager(IDsConnectionOpt connectionManager, IDsDataSourceOpt dataSourceManger, IDsTxTemplate txTemplate) {
        this.connectionManager = connectionManager;
        this.dataSourceManger = dataSourceManger;
        this.txTemplate = txTemplate;
    }

    @Override
    public void initByDataSourceApo(DataSourceApo dataSourceApo) {
        dataSourceManger.initByDataSourceApo(dataSourceApo);
    }

    @Override
    public void initByDataSource(DataSource dataSource) {
        dataSourceManger.initByDataSource(dataSource);
    }

    @Override
    public void initByDataSource(DataSource dataSource, String dataSourceName) {
        dataSourceManger.initByDataSource(dataSource, dataSourceName);
    }

    @Override
    public void initByConnection(Connection connection) {
        dataSourceManger.initByConnection(connection);
    }

    @Override
    public String getSchemaName() {
        return dataSourceManger.getSchemaName();
    }

    @Override
    public String getDatabaseName() {
        return dataSourceManger.getDatabaseName();
    }

    @Override
    public void setSchemaNameGetterFunction(Supplier<String> schemaNameGetterFunction) {
        dataSourceManger.setSchemaNameGetterFunction(schemaNameGetterFunction);
    }

    @Override
    public void setDatabaseNameGetterFunction(Supplier<String> databaseNameGetterFunction) {
        dataSourceManger.setDatabaseNameGetterFunction(databaseNameGetterFunction);
    }

    @Override
    public String getDataSourceId() {
        return dataSourceManger.getDataSourceId();
    }

    @Override
    public DataSourceApo getDataSourceApo() {
        return dataSourceManger.getDataSourceApo();
    }

    @Override
    public DataSource getDataSource() {
        return dataSourceManger.getDataSource();
    }

    @Override
    public void setTxHolder(IDsTxHolder jdbcTxHolder) {
        txTemplate.setTxHolder(jdbcTxHolder);
    }

    @Override
    public IDsTxHolder getTxHolder() {
        return txTemplate.getTxHolder();
    }

    @Override
    public Connection getCurrentConnection() throws SQLException {
        return txTemplate.getCurrentConnection();
    }

    @Override
    public void tx(Runnable action) {
        txTemplate.tx(action);
    }

    @Override
    public void tx(IsolationLevel level, Runnable action) {
        txTemplate.tx(level, action);
    }

    @Override
    public <T> T txReturn(Supplier<T> supplier) {
        return txTemplate.txReturn(supplier);
    }

    @Override
    public <T> T txReturn(IsolationLevel level, Supplier<T> supplier) {
        return txTemplate.txReturn(level, supplier);
    }

    @Override
    public <P> void tx(TxAction<P> action, P param) {
        txTemplate.tx(action, param);
    }

    @Override
    public <P> void tx(IsolationLevel level, TxAction<P> action, P param) {
        txTemplate.tx(action, param);
    }

    @Override
    public <P, R> R txReturn(TxFunc<P, R> func, P param) {
        return txTemplate.txReturn(func, param);
    }

    @Override
    public <P, R> R txReturn(IsolationLevel level, TxFunc<P, R> func, P param) {
        return txTemplate.txReturn(level, func, param);
    }

    @Override
    public GirDsJdbcTxBuilder builder() {
        return txTemplate.builder();
    }

    @Override
    public Connection getConnection() {
        try {
            return getCurrentConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void connectionClose(Connection connection) {
        connectionManager.connectionClose(connection);
    }

    @Override
    public void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        connectionManager.closeResources(rs, stmt, conn);
    }
}
