package cn.geoair.comp.dynamic.ds.readwrite.proxy;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.readwrite.GirGroupSource;
import cn.geoair.comp.dynamic.ds.readwrite.GirReadWriteDataSource;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * 读写分离 Connection 代理
 * 职责：拦截 Statement 创建，管理事务上下文
 *
 * @author 张俊
 * @date Created in 2026/5/28
 */
public class ReadWriteSplitConnection implements Connection {

    private static final GiLogger log = GirLogger.getLoger(ReadWriteSplitConnection.class);

    private final GirReadWriteDataSource dataSource;
    private Connection currentConnection;
    private String username;
    private String password;
    private boolean isClosed = false;
    private boolean autoCommit = true;

    // 事务内使用的数据源类型（事务内不能切换）
    private volatile Boolean transactionUsedMaster = null;

    // 保存当前SQL，用于延迟路由
    private String pendingSql = null;

    public ReadWriteSplitConnection(DataSource masterDataSource, GirGroupSource slaveGroup) {
        this.dataSource = new GirReadWriteDataSource(masterDataSource, slaveGroup);
    }

    public ReadWriteSplitConnection(DataSource masterDataSource, GirGroupSource slaveGroup,
                                    String username, String password) {
        this(masterDataSource, slaveGroup);
        this.username = username;
        this.password = password;
    }

    /**
     * 根据SQL获取或创建连接
     */
    protected Connection getConnection(String sql) {
        this.pendingSql = sql;

        // 事务内不能切换数据源
        if (transactionUsedMaster != null) {
            if (transactionUsedMaster) {
                return getMasterConnection();
            } else {
                return getSlaveConnection();
            }
        }

        // 根据SQL类型选择数据源
        DataSource targetDs = dataSource.getDataSourceBySQL(sql);

        if (targetDs instanceof GirGroupSource) {
            return getSlaveConnection();
        } else {
            return getMasterConnection();
        }
    }

    /**
     * 获取当前连接（不切换）
     */
    private Connection getCurrentConnection() {
        if (currentConnection == null) {
            // 默认使用主库
            return getMasterConnection();
        }
        return currentConnection;
    }

    private Connection getMasterConnection() {
        try {
            if (currentConnection != null && !isMasterConnection()) {
                closeCurrentConnection();
            }

            if (currentConnection == null) {
                if (username != null) {
                    currentConnection = dataSource.getMasterConnection(username, password);
                } else {
                    currentConnection = dataSource.getMasterConnection();
                }
                // 设置事务状态
                currentConnection.setAutoCommit(autoCommit);
                log.trace("创建主库连接成功");
            }
            return currentConnection;
        } catch (SQLException e) {
            log.error("获取主库连接失败", e);
            throw new RuntimeException(e);
        }
    }

    private Connection getSlaveConnection() {
        try {
            if (currentConnection != null && isMasterConnection()) {
                // 当前是主库连接，不能切换到从库（防止事务内主从切换）
                log.debug("当前事务已使用主库，继续使用主库");
                return currentConnection;
            }

            if (currentConnection == null) {
                if (username != null) {
                    currentConnection = dataSource.getSlaveConnection(username, password);
                } else {
                    currentConnection = dataSource.getSlaveConnection();
                }
                currentConnection.setAutoCommit(autoCommit);
                log.trace("创建从库连接成功");
            }
            return currentConnection;
        } catch (SQLException e) {
            log.error("获取从库连接失败", e);
            throw new RuntimeException(e);
        }
    }

    private boolean isMasterConnection() {
        if (currentConnection != null) {
            if (currentConnection instanceof ReadWritePxyConnection) {
                ReadWritePxyConnection pxy = (ReadWritePxyConnection) currentConnection;
                return !pxy.slaveIs;
            }
        }
        return true;
    }

    private void closeCurrentConnection() {
        try {
            if (currentConnection != null && !currentConnection.isClosed()) {
                currentConnection.close();
            }
        } catch (SQLException e) {
            log.warn("关闭连接失败", e);
        } finally {
            currentConnection = null;
        }
    }


    /**
     * 标记事务结束
     */
    private void markTransactionEnd() {
        transactionUsedMaster = null;
    }

    // ==================== Connection 接口实现 ====================

    @Override
    public Statement createStatement() throws SQLException {
        return new ReadWriteSplitStatement(this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new ReadWriteSplitPreparedStatement(this, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        // 存储过程默认使用主库
        Connection conn = getMasterConnection();
        return conn.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return getCurrentConnection().nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.autoCommit = autoCommit;
        if (autoCommit) {
            // 关闭事务
            if (transactionUsedMaster != null) {
                transactionUsedMaster = null;
                log.debug("事务结束（setAutoCommit(true)）");
            }
        } else {
            // 开启事务，等待第一个SQL确定数据源
            if (transactionUsedMaster == null) {
                log.trace("事务开始（setAutoCommit(false)），等待第一个SQL确定数据源");
            }
        }

        if (currentConnection != null) {
            currentConnection.setAutoCommit(autoCommit);
        }
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return autoCommit;
    }

    @Override
    public void commit() throws SQLException {
        if (currentConnection != null && !currentConnection.isClosed()) {
            currentConnection.commit();
        }
        markTransactionEnd();
    }

    @Override
    public void rollback() throws SQLException {
        if (currentConnection != null && !currentConnection.isClosed()) {
            currentConnection.rollback();
        }
        markTransactionEnd();
    }

    @Override
    public void close() throws SQLException {
        if (currentConnection != null && !currentConnection.isClosed()) {
            currentConnection.close();
        }
        isClosed = true;
        transactionUsedMaster = null;
        pendingSql = null;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return isClosed || (currentConnection != null && currentConnection.isClosed());
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return getCurrentConnection().getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        // 如果是读操作，可以设置只读（从库）
        if (readOnly && transactionUsedMaster == null) {
            // 延迟到获取连接时设置
        }
        if (currentConnection != null) {
            currentConnection.setReadOnly(readOnly);
        }
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return currentConnection != null && currentConnection.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        if (currentConnection != null) {
            currentConnection.setCatalog(catalog);
        }
    }

    @Override
    public String getCatalog() throws SQLException {
        return currentConnection != null ? currentConnection.getCatalog() : null;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        if (currentConnection != null) {
            currentConnection.setTransactionIsolation(level);
        }
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return currentConnection != null ? currentConnection.getTransactionIsolation() : Connection.TRANSACTION_NONE;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return currentConnection != null ? currentConnection.getWarnings() : null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        if (currentConnection != null) {
            currentConnection.clearWarnings();
        }
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return new ReadWriteSplitStatement(this, resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return new ReadWriteSplitPreparedStatement(this, sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        Connection conn = getMasterConnection();
        return conn.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return getCurrentConnection().getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        getCurrentConnection().setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        if (currentConnection != null) {
            currentConnection.setHoldability(holdability);
        }
    }

    @Override
    public int getHoldability() throws SQLException {
        return currentConnection != null ? currentConnection.getHoldability() : ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        Connection conn = getMasterConnection(); // 保存点只能在主库
        return conn.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        Connection conn = getMasterConnection();
        return conn.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        if (currentConnection != null) {
            currentConnection.rollback(savepoint);
        }
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        if (currentConnection != null) {
            currentConnection.releaseSavepoint(savepoint);
        }
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return new ReadWriteSplitStatement(this, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return new ReadWriteSplitPreparedStatement(this, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        Connection conn = getMasterConnection();
        return conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return new ReadWriteSplitPreparedStatement(this, sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return new ReadWriteSplitPreparedStatement(this, sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return new ReadWriteSplitPreparedStatement(this, sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException {
        return getCurrentConnection().createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return getCurrentConnection().createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return getCurrentConnection().createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return getCurrentConnection().createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return currentConnection != null && currentConnection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        if (currentConnection != null) {
            currentConnection.setClientInfo(name, value);
        }
    }


    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        if (currentConnection != null) {
            currentConnection.setClientInfo(properties);
        }
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return currentConnection != null ? currentConnection.getClientInfo(name) : null;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return currentConnection != null ? currentConnection.getClientInfo() : null;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return getCurrentConnection().createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return getCurrentConnection().createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        if (currentConnection != null) {
            currentConnection.setSchema(schema);
        }
    }

    @Override
    public String getSchema() throws SQLException {
        return currentConnection != null ? currentConnection.getSchema() : null;
    }


    @Override
    public void abort(Executor executor) throws SQLException {
        if (currentConnection != null) {
            currentConnection.abort(executor);
        }
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        if (currentConnection != null) {
            currentConnection.setNetworkTimeout(executor, milliseconds);
        }
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return currentConnection != null ? currentConnection.getNetworkTimeout() : 0;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return getCurrentConnection().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || getCurrentConnection().isWrapperFor(iface);
    }

    // ==================== 内部方法 ====================

    public GirReadWriteDataSource getDataSource() {
        return dataSource;
    }

    public String getPendingSql() {
        return pendingSql;
    }

    public void setPendingSql(String sql) {
        this.pendingSql = sql;
    }

    public void markTransactionStart(boolean useMaster) {
        if (transactionUsedMaster == null) {
            transactionUsedMaster = useMaster;
            log.trace("事务开始，使用数据源类型: {}", useMaster ? "主库" : "从库");
        }
    }

    public boolean isTransactionStarted() {
        return transactionUsedMaster != null;
    }

    public boolean isTransactionUseMaster() {
        return transactionUsedMaster != null && transactionUsedMaster;
    }
}
