package cn.geoair.comp.dynamic.ds.readwrite.proxy;

import cn.geoair.comp.dynamic.ds.readwrite.GirGroupSource;
import cn.geoair.comp.dynamic.ds.readwrite.GirReadWriteDataSource;
import cn.geoair.comp.dynamic.ds.readwrite.log.RdLog;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

/**
 * 读写分离 Connection 代理 职责：拦截 Statement 创建，管理事务上下文
 *
 * @author 张俊
 * @date Created in 2026/5/28
 */
public class ReadWriteSplitConnection implements Connection {

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
        RdLog.getInstance()
                .trace(
                        "创建 ReadWriteSplitConnection 实例，主库: {}, 从库组: {}",
                        masterDataSource,
                        slaveGroup);
    }

    public ReadWriteSplitConnection(
            DataSource masterDataSource,
            GirGroupSource slaveGroup,
            String username,
            String password) {
        this(masterDataSource, slaveGroup);
        this.username = username;
        this.password = password;
        RdLog.getInstance().trace("使用用户名创建连接实例，username: {}", username);
    }

    /** 根据SQL获取或创建连接 */
    protected Connection getConnection(String sql) {
        RdLog.getInstance()
                .trace(
                        "getConnection 被调用，SQL: {}, 当前事务状态: transactionUsedMaster={}",
                        sql,
                        transactionUsedMaster);

        this.pendingSql = sql;

        // 事务内不能切换数据源
        if (transactionUsedMaster != null) {
            RdLog.getInstance().trace("事务内调用，事务使用数据源类型: {}", transactionUsedMaster ? "主库" : "从库");
            if (transactionUsedMaster) {
                return getMasterConnection();
            } else {
                return getSlaveConnection();
            }
        }

        // 根据SQL类型选择数据源
        DataSource targetDs = dataSource.getDataSourceBySQL(sql);
        RdLog.getInstance()
                .trace(
                        "根据SQL路由选择数据源类型: {}",
                        targetDs instanceof GirGroupSource ? "从库(GirGroupSource)" : "主库");

        if (targetDs instanceof GirGroupSource) {
            return getSlaveConnection();
        } else {
            return getMasterConnection();
        }
    }

    /** 获取当前连接（不切换） */
    private Connection getCurrentConnection() {
        if (currentConnection == null) {
            RdLog.getInstance().trace("当前连接为空，默认使用主库连接");
            return getMasterConnection();
        }
        RdLog.getInstance().trace("返回当前已存在的连接: {}", currentConnection);
        return currentConnection;
    }

    private Connection getMasterConnection() {
        RdLog.getInstance()
                .trace(
                        "准备获取主库连接，当前连接状态: currentConnection={}, autoCommit={}",
                        currentConnection,
                        autoCommit);

        try {
            if (currentConnection != null && !isMasterConnection()) {
                RdLog.getInstance().trace("当前连接不是主库连接，关闭当前连接并重新获取主库");
                closeCurrentConnection();
            }

            if (currentConnection == null) {
                RdLog.getInstance().trace("创建新的主库连接");
                if (username != null) {
                    currentConnection = dataSource.getMasterConnection(username, password);
                    RdLog.getInstance().trace("使用用户名/密码创建主库连接成功");
                } else {
                    currentConnection = dataSource.getMasterConnection();
                    RdLog.getInstance().trace("使用默认方式创建主库连接成功");
                }
                // 设置事务状态
                currentConnection.setAutoCommit(autoCommit);
                RdLog.getInstance().trace("主库连接已设置 autoCommit={}", autoCommit);
                RdLog.getInstance().debug("创建主库连接成功");
            } else {
                RdLog.getInstance().trace("复用已存在的主库连接");
            }
            return currentConnection;
        } catch (SQLException e) {
            RdLog.getInstance().error("获取主库连接失败", e);
            throw new RuntimeException(e);
        }
    }

    private Connection getSlaveConnection() {
        RdLog.getInstance()
                .trace(
                        "准备获取从库连接，当前连接状态: currentConnection={}, autoCommit={}",
                        currentConnection,
                        autoCommit);

        try {
            if (currentConnection != null && isMasterConnection()) {
                RdLog.getInstance().trace("当前连接是主库连接，事务内不能切换到从库，继续使用主库");
                RdLog.getInstance().debug("当前事务已使用主库，继续使用主库");
                return currentConnection;
            }

            if (currentConnection == null) {
                RdLog.getInstance().trace("创建新的从库连接");
                if (username != null) {
                    currentConnection = dataSource.getSlaveConnection(username, password);
                    RdLog.getInstance().trace("使用用户名/密码创建从库连接成功");
                } else {
                    currentConnection = dataSource.getSlaveConnection();
                    RdLog.getInstance().trace("使用默认方式创建从库连接成功");
                }
                currentConnection.setAutoCommit(autoCommit);
                RdLog.getInstance().trace("从库连接已设置 autoCommit={}", autoCommit);
                RdLog.getInstance().debug("创建从库连接成功");
            } else {
                RdLog.getInstance().trace("复用已存在的从库连接");
            }
            return currentConnection;
        } catch (SQLException e) {
            RdLog.getInstance().error("获取从库连接失败", e);
            throw new RuntimeException(e);
        }
    }

    private boolean isMasterConnection() {
        boolean result = true;
        if (currentConnection != null) {
            if (currentConnection instanceof ReadWritePxyConnection) {
                ReadWritePxyConnection pxy = (ReadWritePxyConnection) currentConnection;
                result = !pxy.slaveIs;
                RdLog.getInstance()
                        .trace("判断连接类型: 当前连接是 {}, 是否主库: {}", pxy.slaveIs ? "从库" : "主库", result);
            } else {
                RdLog.getInstance().trace("当前连接不是 ReadWritePxyConnection 类型，默认为主库");
            }
        } else {
            RdLog.getInstance().trace("当前连接为空，默认返回主库");
        }
        return result;
    }

    private void closeCurrentConnection() {
        RdLog.getInstance().trace("准备关闭当前连接: {}", currentConnection);
        try {
            if (currentConnection != null && !currentConnection.isClosed()) {
                currentConnection.close();
                RdLog.getInstance().trace("当前连接已关闭");
            } else {
                RdLog.getInstance().trace("当前连接为空或已关闭，无需关闭");
            }
        } catch (SQLException e) {
            RdLog.getInstance().warn("关闭连接失败", e);
        } finally {
            currentConnection = null;
            RdLog.getInstance().trace("当前连接引用已清空");
        }
    }

    /** 标记事务结束 */
    private void markTransactionEnd() {
        RdLog.getInstance().trace("标记事务结束，transactionUsedMaster 原值: {}", transactionUsedMaster);
        transactionUsedMaster = null;
        RdLog.getInstance().trace("事务已标记结束");
    }

    // ==================== Connection 接口实现 ====================

    @Override
    public Statement createStatement() throws SQLException {
        RdLog.getInstance().trace("createStatement 被调用");
        return new ReadWriteSplitStatement(this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        RdLog.getInstance().trace("prepareStatement 被调用，SQL: {}", sql);
        return new ReadWriteSplitPreparedStatement(this, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        RdLog.getInstance().trace("prepareCall 被调用，SQL: {}，存储过程默认使用主库", sql);
        Connection conn = getMasterConnection();
        RdLog.getInstance().trace("获取主库连接用于 prepareCall");
        return conn.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        RdLog.getInstance().trace("nativeSQL 被调用，SQL: {}", sql);
        String result = getCurrentConnection().nativeSQL(sql);
        RdLog.getInstance().trace("nativeSQL 转换结果: {}", result);
        return result;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        RdLog.getInstance()
                .trace(
                        "setAutoCommit 被调用，参数 autoCommit={}，当前 autoCommit={}",
                        autoCommit,
                        this.autoCommit);

        this.autoCommit = autoCommit;
        if (autoCommit) {
            // 关闭事务
            if (transactionUsedMaster != null) {
                RdLog.getInstance().trace("setAutoCommit(true) 检测到活跃事务，准备结束事务");
                transactionUsedMaster = null;
                RdLog.getInstance().debug("事务结束（setAutoCommit(true)）");
            } else {
                RdLog.getInstance().trace("setAutoCommit(true) 但无活跃事务");
            }
        } else {
            // 开启事务，等待第一个SQL确定数据源
            if (transactionUsedMaster == null) {
                RdLog.getInstance().trace("setAutoCommit(false) 开启新事务，等待第一个SQL确定数据源");
                RdLog.getInstance().trace("事务开始（setAutoCommit(false)），等待第一个SQL确定数据源");
            } else {
                RdLog.getInstance()
                        .trace(
                                "setAutoCommit(false) 但在事务中，保持当前数据源类型: {}",
                                transactionUsedMaster ? "主库" : "从库");
            }
        }

        if (currentConnection != null) {
            RdLog.getInstance().trace("将 autoCommit={} 设置到底层连接", autoCommit);
            currentConnection.setAutoCommit(autoCommit);
        } else {
            RdLog.getInstance().trace("当前连接为空，暂不设置底层 autoCommit");
        }
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        RdLog.getInstance().trace("getAutoCommit 被调用，返回: {}", autoCommit);
        return autoCommit;
    }

    @Override
    public void commit() throws SQLException {
        RdLog.getInstance()
                .trace("commit 被调用，当前事务状态: transactionUsedMaster={}", transactionUsedMaster);
        if (currentConnection != null && !currentConnection.isClosed()) {
            RdLog.getInstance().trace("对底层连接执行 commit");
            currentConnection.commit();
            RdLog.getInstance().trace("commit 执行完成");
        } else {
            RdLog.getInstance().trace("当前连接为空或已关闭，跳过 commit");
        }
        markTransactionEnd();
    }

    @Override
    public void rollback() throws SQLException {
        RdLog.getInstance()
                .trace("rollback 被调用，当前事务状态: transactionUsedMaster={}", transactionUsedMaster);
        if (currentConnection != null && !currentConnection.isClosed()) {
            RdLog.getInstance().trace("对底层连接执行 rollback");
            currentConnection.rollback();
            RdLog.getInstance().trace("rollback 执行完成");
        } else {
            RdLog.getInstance().trace("当前连接为空或已关闭，跳过 rollback");
        }
        markTransactionEnd();
    }

    @Override
    public void close() throws SQLException {
        RdLog.getInstance().trace("close 被调用，当前连接: {}, isClosed={}", currentConnection, isClosed);
        if (currentConnection != null && !currentConnection.isClosed()) {
            RdLog.getInstance().trace("关闭底层连接");
            currentConnection.close();
            RdLog.getInstance().trace("底层连接已关闭");
        }
        isClosed = true;
        transactionUsedMaster = null;
        pendingSql = null;
        RdLog.getInstance().trace("ReadWriteSplitConnection 已关闭，事务状态和SQL已清空");
    }

    @Override
    public boolean isClosed() throws SQLException {
        boolean result = isClosed || (currentConnection != null && currentConnection.isClosed());
        RdLog.getInstance().trace("isClosed 被调用，返回: {}", result);
        return result;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        RdLog.getInstance().trace("getMetaData 被调用");
        DatabaseMetaData metaData = getCurrentConnection().getMetaData();
        return metaData;
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        RdLog.getInstance()
                .trace(
                        "setReadOnly 被调用，readOnly={}, transactionUsedMaster={}",
                        readOnly,
                        transactionUsedMaster);

        // 如果是读操作，可以设置只读（从库）
        if (readOnly && transactionUsedMaster == null) {
            RdLog.getInstance().trace("设置为只读模式且无事务，将延迟到获取连接时设置");
            // 延迟到获取连接时设置
        }
        if (currentConnection != null) {
            RdLog.getInstance().trace("将 readOnly={} 设置到底层连接", readOnly);
            currentConnection.setReadOnly(readOnly);
        } else {
            RdLog.getInstance().trace("当前连接为空，暂不设置底层 readOnly");
        }
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        boolean result = currentConnection != null && currentConnection.isReadOnly();
        RdLog.getInstance().trace("isReadOnly 被调用，返回: {}", result);
        return result;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        RdLog.getInstance().trace("setCatalog 被调用，catalog: {}", catalog);
        if (currentConnection != null) {
            currentConnection.setCatalog(catalog);
            RdLog.getInstance().trace("已设置 catalog 到底层连接");
        } else {
            RdLog.getInstance().trace("当前连接为空，暂不设置 catalog");
        }
    }

    @Override
    public String getCatalog() throws SQLException {
        String result = currentConnection != null ? currentConnection.getCatalog() : null;
        RdLog.getInstance().trace("getCatalog 被调用，返回: {}", result);
        return result;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        RdLog.getInstance().trace("setTransactionIsolation 被调用，level: {}", level);
        if (currentConnection != null) {
            currentConnection.setTransactionIsolation(level);
            RdLog.getInstance().trace("已设置事务隔离级别到底层连接");
        } else {
            RdLog.getInstance().trace("当前连接为空，暂不设置事务隔离级别");
        }
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        int result =
                currentConnection != null
                        ? currentConnection.getTransactionIsolation()
                        : Connection.TRANSACTION_NONE;
        RdLog.getInstance().trace("getTransactionIsolation 被调用，返回: {}", result);
        return result;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        SQLWarning result = currentConnection != null ? currentConnection.getWarnings() : null;
        if (result != null) {
            RdLog.getInstance().trace("getWarnings 被调用，存在警告: {}", result);
        } else {
            RdLog.getInstance().trace("getWarnings 被调用，无警告");
        }
        return result;
    }

    @Override
    public void clearWarnings() throws SQLException {
        RdLog.getInstance().trace("clearWarnings 被调用");
        if (currentConnection != null) {
            currentConnection.clearWarnings();
            RdLog.getInstance().trace("已清除底层连接的警告");
        } else {
            RdLog.getInstance().trace("当前连接为空，跳过清除警告");
        }
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        RdLog.getInstance()
                .trace(
                        "createStatement 被调用，resultSetType={}, resultSetConcurrency={}",
                        resultSetType,
                        resultSetConcurrency);
        return new ReadWriteSplitStatement(this, resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(
            String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        RdLog.getInstance()
                .trace(
                        "prepareStatement 被调用，SQL: {}, resultSetType={}, resultSetConcurrency={}",
                        sql,
                        resultSetType,
                        resultSetConcurrency);
        return new ReadWriteSplitPreparedStatement(this, sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        RdLog.getInstance().trace("prepareCall 被调用，SQL: {}，存储过程默认使用主库", sql);
        Connection conn = getMasterConnection();
        RdLog.getInstance().trace("获取主库连接用于 prepareCall");
        return conn.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        RdLog.getInstance().trace("getTypeMap 被调用");
        Map<String, Class<?>> typeMap = getCurrentConnection().getTypeMap();
        RdLog.getInstance().trace("getTypeMap 返回，大小: {}", typeMap != null ? typeMap.size() : 0);
        return typeMap;
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        RdLog.getInstance().trace("setTypeMap 被调用，map 大小: {}", map != null ? map.size() : 0);
        getCurrentConnection().setTypeMap(map);
        RdLog.getInstance().trace("已设置 TypeMap");
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        RdLog.getInstance().trace("setHoldability 被调用，holdability: {}", holdability);
        if (currentConnection != null) {
            currentConnection.setHoldability(holdability);
            RdLog.getInstance().trace("已设置 holdability 到底层连接");
        } else {
            RdLog.getInstance().trace("当前连接为空，暂不设置 holdability");
        }
    }

    @Override
    public int getHoldability() throws SQLException {
        int result =
                currentConnection != null
                        ? currentConnection.getHoldability()
                        : ResultSet.HOLD_CURSORS_OVER_COMMIT;
        RdLog.getInstance().trace("getHoldability 被调用，返回: {}", result);
        return result;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        RdLog.getInstance().trace("setSavepoint 被调用，保存点只能在主库");
        Connection conn = getMasterConnection();
        Savepoint savepoint = conn.setSavepoint();
        RdLog.getInstance().trace("创建保存点成功: {}", savepoint);
        return savepoint;
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        RdLog.getInstance().trace("setSavepoint 被调用，name: {}，保存点只能在主库", name);
        Connection conn = getMasterConnection();
        Savepoint savepoint = conn.setSavepoint(name);
        RdLog.getInstance().trace("创建命名保存点成功: {}", savepoint);
        return savepoint;
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        RdLog.getInstance().trace("rollback(Savepoint) 被调用，savepoint: {}", savepoint);
        if (currentConnection != null) {
            currentConnection.rollback(savepoint);
            RdLog.getInstance().trace("已回滚到指定保存点");
        } else {
            RdLog.getInstance().trace("当前连接为空，跳过回滚到保存点");
        }
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        RdLog.getInstance().trace("releaseSavepoint 被调用，savepoint: {}", savepoint);
        if (currentConnection != null) {
            currentConnection.releaseSavepoint(savepoint);
            RdLog.getInstance().trace("已释放保存点");
        } else {
            RdLog.getInstance().trace("当前连接为空，跳过释放保存点");
        }
    }

    @Override
    public Statement createStatement(
            int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        RdLog.getInstance()
                .trace(
                        "createStatement 被调用，resultSetType={}, resultSetConcurrency={}, resultSetHoldability={}",
                        resultSetType,
                        resultSetConcurrency,
                        resultSetHoldability);
        return new ReadWriteSplitStatement(
                this, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(
            String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        RdLog.getInstance()
                .trace(
                        "prepareStatement 被调用，SQL: {}, resultSetType={}, resultSetConcurrency={}, resultSetHoldability={}",
                        sql,
                        resultSetType,
                        resultSetConcurrency,
                        resultSetHoldability);
        return new ReadWriteSplitPreparedStatement(
                this, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(
            String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        RdLog.getInstance().trace("prepareCall 被调用，SQL: {}，存储过程默认使用主库", sql);
        Connection conn = getMasterConnection();
        RdLog.getInstance().trace("获取主库连接用于 prepareCall");
        return conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        RdLog.getInstance()
                .trace(
                        "prepareStatement 被调用，SQL: {}, autoGeneratedKeys: {}",
                        sql,
                        autoGeneratedKeys);
        return new ReadWriteSplitPreparedStatement(this, sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        RdLog.getInstance()
                .trace(
                        "prepareStatement 被调用，SQL: {}, columnIndexes 长度: {}",
                        sql,
                        columnIndexes != null ? columnIndexes.length : 0);
        return new ReadWriteSplitPreparedStatement(this, sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        RdLog.getInstance()
                .trace(
                        "prepareStatement 被调用，SQL: {}, columnNames 长度: {}",
                        sql,
                        columnNames != null ? columnNames.length : 0);
        return new ReadWriteSplitPreparedStatement(this, sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException {
        RdLog.getInstance().trace("createClob 被调用");
        Clob clob = getCurrentConnection().createClob();
        RdLog.getInstance().trace("创建 Clob 成功");
        return clob;
    }

    @Override
    public Blob createBlob() throws SQLException {
        RdLog.getInstance().trace("createBlob 被调用");
        Blob blob = getCurrentConnection().createBlob();
        RdLog.getInstance().trace("创建 Blob 成功");
        return blob;
    }

    @Override
    public NClob createNClob() throws SQLException {
        RdLog.getInstance().trace("createNClob 被调用");
        NClob nClob = getCurrentConnection().createNClob();
        RdLog.getInstance().trace("创建 NClob 成功");
        return nClob;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        RdLog.getInstance().trace("createSQLXML 被调用");
        SQLXML sqlxml = getCurrentConnection().createSQLXML();
        RdLog.getInstance().trace("创建 SQLXML 成功");
        return sqlxml;
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        boolean result = currentConnection != null && currentConnection.isValid(timeout);
        RdLog.getInstance().trace("isValid 被调用，timeout: {}，返回: {}", timeout, result);
        return result;
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        RdLog.getInstance().trace("setClientInfo 被调用，name: {}, value: {}", name, value);
        if (currentConnection != null) {
            currentConnection.setClientInfo(name, value);
            RdLog.getInstance().trace("已设置 ClientInfo 到底层连接");
        } else {
            RdLog.getInstance().trace("当前连接为空，跳过设置 ClientInfo");
        }
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        RdLog.getInstance()
                .trace(
                        "setClientInfo 被调用，properties 大小: {}",
                        properties != null ? properties.size() : 0);
        if (currentConnection != null) {
            currentConnection.setClientInfo(properties);
            RdLog.getInstance().trace("已设置 ClientInfo 到底层连接");
        } else {
            RdLog.getInstance().trace("当前连接为空，跳过设置 ClientInfo");
        }
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        String result = currentConnection != null ? currentConnection.getClientInfo(name) : null;
        RdLog.getInstance().trace("getClientInfo 被调用，name: {}，返回: {}", name, result);
        return result;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        Properties result = currentConnection != null ? currentConnection.getClientInfo() : null;
        RdLog.getInstance()
                .trace(
                        "getClientInfo 被调用，返回 properties 大小: {}",
                        result != null ? result.size() : 0);
        return result;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        RdLog.getInstance()
                .trace(
                        "createArrayOf 被调用，typeName: {}, elements 长度: {}",
                        typeName,
                        elements != null ? elements.length : 0);
        Array array = getCurrentConnection().createArrayOf(typeName, elements);
        RdLog.getInstance().trace("创建 Array 成功");
        return array;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        RdLog.getInstance()
                .trace(
                        "createStruct 被调用，typeName: {}, attributes 长度: {}",
                        typeName,
                        attributes != null ? attributes.length : 0);
        Struct struct = getCurrentConnection().createStruct(typeName, attributes);
        RdLog.getInstance().trace("创建 Struct 成功");
        return struct;
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        RdLog.getInstance().trace("setSchema 被调用，schema: {}", schema);
        if (currentConnection != null) {
            currentConnection.setSchema(schema);
            RdLog.getInstance().trace("已设置 schema 到底层连接");
        } else {
            RdLog.getInstance().trace("当前连接为空，暂不设置 schema");
        }
    }

    @Override
    public String getSchema() throws SQLException {
        String result = currentConnection != null ? currentConnection.getSchema() : null;
        RdLog.getInstance().trace("getSchema 被调用，返回: {}", result);
        return result;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        RdLog.getInstance().trace("abort 被调用，executor: {}", executor);
        if (currentConnection != null) {
            currentConnection.abort(executor);
            RdLog.getInstance().trace("已中止底层连接");
        } else {
            RdLog.getInstance().trace("当前连接为空，跳过中止操作");
        }
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        RdLog.getInstance().trace("setNetworkTimeout 被调用，milliseconds: {}", milliseconds);
        if (currentConnection != null) {
            currentConnection.setNetworkTimeout(executor, milliseconds);
            RdLog.getInstance().trace("已设置网络超时到底层连接");
        } else {
            RdLog.getInstance().trace("当前连接为空，暂不设置网络超时");
        }
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        int result = currentConnection != null ? currentConnection.getNetworkTimeout() : 0;
        RdLog.getInstance().trace("getNetworkTimeout 被调用，返回: {}", result);
        return result;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        RdLog.getInstance().trace("unwrap 被调用，iface: {}", iface);
        if (iface.isInstance(this)) {
            RdLog.getInstance().trace("unwrap 返回当前实例");
            return iface.cast(this);
        }
        T result = getCurrentConnection().unwrap(iface);
        RdLog.getInstance().trace("unwrap 返回底层连接的解包结果");
        return result;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        boolean result = iface.isInstance(this) || getCurrentConnection().isWrapperFor(iface);
        RdLog.getInstance().trace("isWrapperFor 被调用，iface: {}，返回: {}", iface, result);
        return result;
    }

    // ==================== 内部方法 ====================

    public GirReadWriteDataSource getDataSource() {
        RdLog.getInstance().trace("getDataSource 被调用");
        return dataSource;
    }

    public String getPendingSql() {
        RdLog.getInstance().trace("getPendingSql 被调用，返回: {}", pendingSql);
        return pendingSql;
    }

    public void setPendingSql(String sql) {
        RdLog.getInstance().trace("setPendingSql 被调用，SQL: {}", sql);
        this.pendingSql = sql;
    }

    public void markTransactionStart(boolean useMaster) {
        RdLog.getInstance()
                .trace(
                        "markTransactionStart 被调用，useMaster: {}，当前事务状态: transactionUsedMaster={}",
                        useMaster,
                        transactionUsedMaster);

        if (transactionUsedMaster == null) {
            transactionUsedMaster = useMaster;
            RdLog.getInstance().trace("事务状态已设置: {}", useMaster ? "主库" : "从库");
            RdLog.getInstance().debug("事务开始，使用数据源类型: {}", useMaster ? "主库" : "从库");
        } else {
            RdLog.getInstance()
                    .trace("事务已开始，状态为 {}，忽略新的事务标记请求", transactionUsedMaster ? "主库" : "从库");
        }
    }

    public boolean isTransactionStarted() {
        boolean result = transactionUsedMaster != null;
        RdLog.getInstance().trace("isTransactionStarted 被调用，返回: {}", result);
        return result;
    }

    public boolean isTransactionUseMaster() {
        boolean result = transactionUsedMaster != null && transactionUsedMaster;
        RdLog.getInstance().trace("isTransactionUseMaster 被调用，返回: {}", result);
        return result;
    }
}
