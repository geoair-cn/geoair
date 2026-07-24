package cn.geoair.comp.dynamic.ds.readwrite.proxy;

import cn.geoair.comp.dynamic.ds.readwrite.enums.SQLType;
import cn.geoair.comp.dynamic.ds.readwrite.log.RdLog;
import cn.geoair.comp.dynamic.ds.readwrite.utils.SQLParserUtil;
import java.sql.*;

/**
 * 读写分离 Statement 代理 职责：拦截 SQL 执行，动态路由到主库或从库
 *
 * @author 张俊
 * @date Created in 2026/5/28
 */
public class ReadWriteSplitStatement implements Statement {

    private final ReadWriteSplitConnection connection;
    private Statement currentStatement;
    private int resultSetType;
    private int resultSetConcurrency;
    private int resultSetHoldability;

    public ReadWriteSplitStatement(ReadWriteSplitConnection connection) {
        this.connection = connection;
    }

    public ReadWriteSplitStatement(
            ReadWriteSplitConnection connection, int resultSetType, int resultSetConcurrency) {
        this(connection);
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
    }

    public ReadWriteSplitStatement(
            ReadWriteSplitConnection connection,
            int resultSetType,
            int resultSetConcurrency,
            int resultSetHoldability) {
        this(connection);
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.resultSetHoldability = resultSetHoldability;
    }

    /** 获取真实的 Statement */
    private Statement getRealStatement(String sql) throws SQLException {
        Connection realConn = connection.getConnection(sql);

        if (currentStatement != null && currentStatement.getConnection() != realConn) {
            closeCurrentStatement();
        }

        if (currentStatement == null) {
            if (resultSetHoldability > 0) {
                currentStatement =
                        realConn.createStatement(
                                resultSetType, resultSetConcurrency, resultSetHoldability);
            } else if (resultSetType > 0) {
                currentStatement = realConn.createStatement(resultSetType, resultSetConcurrency);
            } else {
                currentStatement = realConn.createStatement();
            }

            // 标记事务开始
            SQLType sqlType = SQLParserUtil.getSQLType(sql);
            connection.markTransactionStart(sqlType == SQLType.WRITE);
        }

        return currentStatement;
    }

    private void closeCurrentStatement() {
        try {
            if (currentStatement != null && !currentStatement.isClosed()) {
                currentStatement.close();
            }
        } catch (SQLException e) {
            RdLog.getInstance().warn("关闭Statement失败", e);
        } finally {
            currentStatement = null;
        }
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        RdLog.getInstance().trace("executeQuery: {}", sql);
        Statement stmt = getRealStatement(sql);
        return stmt.executeQuery(sql);
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        RdLog.getInstance().trace("executeUpdate: {}", sql);
        Statement stmt = getRealStatement(sql);
        return stmt.executeUpdate(sql);
    }

    @Override
    public void close() throws SQLException {
        closeCurrentStatement();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return currentStatement != null ? currentStatement.getMaxFieldSize() : 0;
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        if (currentStatement != null) {
            currentStatement.setMaxFieldSize(max);
        }
    }

    @Override
    public int getMaxRows() throws SQLException {
        return currentStatement != null ? currentStatement.getMaxRows() : 0;
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        if (currentStatement != null) {
            currentStatement.setMaxRows(max);
        }
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        if (currentStatement != null) {
            currentStatement.setEscapeProcessing(enable);
        }
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return currentStatement != null ? currentStatement.getQueryTimeout() : 0;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        if (currentStatement != null) {
            currentStatement.setQueryTimeout(seconds);
        }
    }

    @Override
    public void cancel() throws SQLException {
        if (currentStatement != null) {
            currentStatement.cancel();
        }
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return currentStatement != null ? currentStatement.getWarnings() : null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        if (currentStatement != null) {
            currentStatement.clearWarnings();
        }
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        if (currentStatement != null) {
            currentStatement.setCursorName(name);
        }
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        RdLog.getInstance().debug("execute: {}", sql);
        Statement stmt = getRealStatement(sql);
        return stmt.execute(sql);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return currentStatement != null ? currentStatement.getResultSet() : null;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return currentStatement != null ? currentStatement.getUpdateCount() : -1;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return currentStatement != null && currentStatement.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (currentStatement != null) {
            currentStatement.setFetchDirection(direction);
        }
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return currentStatement != null
                ? currentStatement.getFetchDirection()
                : ResultSet.FETCH_FORWARD;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        if (currentStatement != null) {
            currentStatement.setFetchSize(rows);
        }
    }

    @Override
    public int getFetchSize() throws SQLException {
        return currentStatement != null ? currentStatement.getFetchSize() : 0;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return resultSetConcurrency;
    }

    @Override
    public int getResultSetType() throws SQLException {
        return resultSetType;
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        // 批量操作，使用主库
        Connection realConn = connection.getConnection(sql);
        Statement stmt = realConn.createStatement();
        stmt.addBatch(sql);
        currentStatement = stmt;
        connection.markTransactionStart(true);
    }

    @Override
    public void clearBatch() throws SQLException {
        if (currentStatement != null) {
            currentStatement.clearBatch();
        }
    }

    @Override
    public int[] executeBatch() throws SQLException {
        if (currentStatement != null) {
            return currentStatement.executeBatch();
        }
        return new int[0];
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connection;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return currentStatement != null && currentStatement.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return currentStatement != null ? currentStatement.getGeneratedKeys() : null;
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        RdLog.getInstance().debug("executeUpdate: {}", sql);
        Statement stmt = getRealStatement(sql);
        return stmt.executeUpdate(sql, autoGeneratedKeys);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        RdLog.getInstance().debug("executeUpdate: {}", sql);
        Statement stmt = getRealStatement(sql);
        return stmt.executeUpdate(sql, columnIndexes);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        RdLog.getInstance().debug("executeUpdate: {}", sql);
        Statement stmt = getRealStatement(sql);
        return stmt.executeUpdate(sql, columnNames);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        RdLog.getInstance().debug("execute: {}", sql);
        Statement stmt = getRealStatement(sql);
        return stmt.execute(sql, autoGeneratedKeys);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        RdLog.getInstance().debug("execute: {}", sql);
        Statement stmt = getRealStatement(sql);
        return stmt.execute(sql, columnIndexes);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        RdLog.getInstance().debug("execute: {}", sql);
        Statement stmt = getRealStatement(sql);
        return stmt.execute(sql, columnNames);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return resultSetHoldability;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return currentStatement == null || currentStatement.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        if (currentStatement != null) {
            currentStatement.setPoolable(poolable);
        }
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return currentStatement != null && currentStatement.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        if (currentStatement != null) {
            currentStatement.closeOnCompletion();
        }
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return currentStatement != null && currentStatement.isCloseOnCompletion();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        if (currentStatement != null) {
            return currentStatement.unwrap(iface);
        }
        throw new SQLException("No statement available");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this)
                || (currentStatement != null && currentStatement.isWrapperFor(iface));
    }
}
