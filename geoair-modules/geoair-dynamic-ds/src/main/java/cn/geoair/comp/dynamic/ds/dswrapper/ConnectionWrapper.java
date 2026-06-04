package cn.geoair.comp.dynamic.ds.dswrapper;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * 数据库链接的包装实现
 *
 * @author 张俊
 * @date Created in 2026/5/28
 */
public class ConnectionWrapper implements Connection {

    private static final GiLogger log = GirLoggerFactory.getLogger(ConnectionWrapper.class);

    private Connection pxyConnection;

    public ConnectionWrapper(Connection connection) {
        this.pxyConnection = connection;
    }

    /**
     * 获取真实的连接对象
     *
     * @return
     */
    public Connection getPxyConnection() {
        return pxyConnection;
    }

    // ==================== Connection 接口实现 ====================

    @Override
    public Statement createStatement() throws SQLException {
        return pxyConnection.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return pxyConnection.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return pxyConnection.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return pxyConnection.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        pxyConnection.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return pxyConnection.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        pxyConnection.commit();
    }

    @Override
    public void rollback() throws SQLException {
        pxyConnection.rollback();
    }

    @Override
    public void close() throws SQLException {
        pxyConnection.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return pxyConnection.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return pxyConnection.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        pxyConnection.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return pxyConnection.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        pxyConnection.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        return pxyConnection.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        pxyConnection.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return pxyConnection.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return pxyConnection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        pxyConnection.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return pxyConnection.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return pxyConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return pxyConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return pxyConnection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        pxyConnection.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        pxyConnection.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        return pxyConnection.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return pxyConnection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return pxyConnection.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        pxyConnection.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        pxyConnection.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return pxyConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return pxyConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return pxyConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return pxyConnection.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return pxyConnection.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return pxyConnection.prepareStatement(sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException {
        return pxyConnection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return pxyConnection.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return pxyConnection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return pxyConnection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return pxyConnection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        pxyConnection.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        pxyConnection.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return pxyConnection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return pxyConnection.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return pxyConnection.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return pxyConnection.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        pxyConnection.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        return pxyConnection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        pxyConnection.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        pxyConnection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return pxyConnection.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return pxyConnection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || pxyConnection.isWrapperFor(iface);
    }
}
