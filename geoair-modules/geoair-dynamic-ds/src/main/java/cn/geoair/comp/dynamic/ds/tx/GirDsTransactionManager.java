package cn.geoair.comp.dynamic.ds.tx;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.base.IDsConnectionOpt;
import cn.geoair.comp.dynamic.ds.base.IDsDataSourceOpt;
import cn.geoair.comp.dynamic.ds.base.RealConnectionOpt;
import cn.geoair.comp.dynamic.ds.tx.enums.IsolationLevel;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;
import javax.sql.DataSource;

/**
 * 事务管理器
 *
 * @author ：zhangjun
 * @date ：Created in 2025/10/9 10:38
 */
public class GirDsTransactionManager implements IDataSourceGetter {

    private static final GiLogger log = GirLoggerFactory.getLogger();

    IDsConnectionOpt connectionOpt;
    IDsDataSourceOpt dsDataSourceOpt;
    IDsTransactionTemplate dsTransactionTemplate;

    public GirDsTransactionManager(
            IDsConnectionOpt connectionOpt, IDsDataSourceOpt dsDataSourceOpt) {
        this.connectionOpt = connectionOpt;
        this.dsDataSourceOpt = dsDataSourceOpt;
        this.dsTransactionTemplate = new GirDsTransactionTemplate(connectionOpt);
    }

    public GirDsTransactionManager(IDsDataSourceOpt dsDataSourceOpt) {
        this.connectionOpt = new RealConnectionOpt(dsDataSourceOpt);
        this.dsDataSourceOpt = dsDataSourceOpt;
        this.dsTransactionTemplate = new GirDsTransactionTemplate(connectionOpt);
    }

    public GirDsTransactionManager(
            IDsConnectionOpt connectionOpt,
            IDsDataSourceOpt dsDataSourceOpt,
            IDsTransactionTemplate dsTransactionTemplate) {
        this.connectionOpt = connectionOpt;
        this.dsDataSourceOpt = dsDataSourceOpt;
        this.dsTransactionTemplate = dsTransactionTemplate;
    }

    @Override
    public void initByDataSourceApo(DataSourceApo dataSourceApo) {
        dsDataSourceOpt.initByDataSourceApo(dataSourceApo);
    }

    @Override
    public void initByDataSource(DataSource dataSource) {
        dsDataSourceOpt.initByDataSource(dataSource);
    }

    @Override
    public void initByDataSource(DataSource dataSource, String dataSourceName) {
        dsDataSourceOpt.initByDataSource(dataSource, dataSourceName);
    }

    @Override
    public void initByConnection(Connection connection) {
        dsDataSourceOpt.initByConnection(connection);
    }

    @Override
    public String getSchemaName() {
        return dsDataSourceOpt.getSchemaName();
    }

    @Override
    public String getDatabaseName() {
        return dsDataSourceOpt.getDatabaseName();
    }

    @Override
    public void setSchemaNameGetterFunction(Supplier<String> schemaNameGetterFunction) {
        dsDataSourceOpt.setSchemaNameGetterFunction(schemaNameGetterFunction);
    }

    @Override
    public void setDatabaseNameGetterFunction(Supplier<String> databaseNameGetterFunction) {
        dsDataSourceOpt.setDatabaseNameGetterFunction(databaseNameGetterFunction);
    }

    @Override
    public String getDataSourceId() {
        return dsDataSourceOpt.getDataSourceId();
    }

    @Override
    public DataSourceApo getDataSourceApo() {
        return dsDataSourceOpt.getDataSourceApo();
    }

    @Override
    public DataSource getDataSource() {
        return dsDataSourceOpt.getDataSource();
    }

    @Override
    public void setTransactionConnectionHolder(
            IDsTransactionConnectionHolder transactionConnectionHolder) {
        dsTransactionTemplate.setTransactionConnectionHolder(transactionConnectionHolder);
    }

    @Override
    public IDsTransactionConnectionHolder getTransactionConnectionHolder() {
        return dsTransactionTemplate.getTransactionConnectionHolder();
    }

    @Override
    public Connection getCurrentConnection() throws SQLException {
        return dsTransactionTemplate.getCurrentConnection();
    }

    @Override
    public void tx(TxActionNp action) {
        dsTransactionTemplate.tx(action);
    }

    @Override
    public void tx(IsolationLevel level, TxActionNp action) {
        dsTransactionTemplate.tx(level, action);
    }

    @Override
    public <T> T txReturn(TxFuncNp<T> txFuncNp) {
        return dsTransactionTemplate.txReturn(txFuncNp);
    }

    @Override
    public <T> T txReturn(IsolationLevel level, TxFuncNp<T> txFuncNp) {
        return dsTransactionTemplate.txReturn(level, txFuncNp);
    }

    @Override
    public <P> void tx(TxAction<P> action, P param) {
        dsTransactionTemplate.tx(action, param);
    }

    @Override
    public <P> void tx(IsolationLevel level, TxAction<P> action, P param) {
        dsTransactionTemplate.tx(action, param);
    }

    @Override
    public <P, R> R txReturn(TxFunc<P, R> func, P param) {
        return dsTransactionTemplate.txReturn(func, param);
    }

    @Override
    public <P, R> R txReturn(IsolationLevel level, TxFunc<P, R> func, P param) {
        return dsTransactionTemplate.txReturn(level, func, param);
    }

    @Override
    public GirDsJdbcTxBuilder txBuilder() {
        return dsTransactionTemplate.txBuilder();
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
        // 这里调用的是事务的关闭连接
        dsTransactionTemplate.connectionClose(connection);
    }

    @Override
    public void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        connectionOpt.closeResources(rs, stmt, conn);
    }
}
