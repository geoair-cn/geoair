package cn.geoair.comp.dynamic.ds.tx;

import cn.geoair.comp.dynamic.ds.IDsConnectionOpt;
import cn.geoair.comp.dynamic.ds.tx.enums.IsolationLevel;
import cn.geoair.comp.dynamic.ds.tx.enums.Propagation;

import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * 事务操作实现
 */
public class GirDefaultIDsTxTemplate implements IDsTxTemplate {

    IDsConnectionOpt connectionManager;

    IDsTxHolder jdbcTxHolder;

    public GirDefaultIDsTxTemplate(IDsConnectionOpt connectionManager) {
        this.connectionManager = connectionManager;
        this.jdbcTxHolder = GirDsDefaultJdbcTxHolder.getInstance();
    }

    public GirDefaultIDsTxTemplate(IDsConnectionOpt connectionManager, IDsTxHolder jdbcTxHolder) {
        this.connectionManager = connectionManager;
        this.jdbcTxHolder = jdbcTxHolder;
    }

    @Override
    public void setTxHolder(IDsTxHolder jdbcTxHolder) {
        this.jdbcTxHolder = jdbcTxHolder;
    }

    @Override
    public IDsTxHolder getTxHolder() {
        return this.jdbcTxHolder;
    }

    @Override
    public void tx(Runnable action) {
        builder().run(action);
    }

    @Override
    public void tx(IsolationLevel level, Runnable action) {
        builder().isolation(level).run(action);
    }

    @Override
    public <T> T txReturn(Supplier<T> supplier) {
        return builder().call(supplier);
    }

    @Override
    public <T> T txReturn(IsolationLevel level, Supplier<T> supplier) {
        return builder().isolation(level).call(supplier);
    }

    @Override
    public <P> void tx(TxAction<P> action, P param) {
        builder().run(action, param);
    }

    @Override
    public <P> void tx(IsolationLevel level, TxAction<P> action, P param) {
        builder().isolation(level).run(action, param);
    }

    @Override
    public <P, R> R txReturn(TxFunc<P, R> func, P param) {
        return builder().call(func, param);
    }

    @Override
    public <P, R> R txReturn(IsolationLevel level, TxFunc<P, R> func, P param) {
        return builder().isolation(level).call(func, param);
    }

    @Override
    public GirDsJdbcTxBuilder builder() {
        return new GirDsJdbcTxBuilder(this);
    }

    /**
     * 统一核心事务入口
     */
    @SuppressWarnings("unchecked")
    <T, P> T doTx(Propagation propagation,
                  IsolationLevel level,
                  boolean readOnly,
                  Class<? extends Throwable>[] rollFor,
                  Class<? extends Throwable>[] noRollFor,
                  Object execObj, P param) {

        Connection suspendConn = null;
        Connection currConn = null;
        Savepoint savepoint = null;

        try {
            // 传播行为处理
            switch (propagation) {
                case REQUIRED:
                    if (jdbcTxHolder.isInTx()) {
                        return exec(execObj, param);
                    }
                    // 无事务，开启新事务
                    break;
                case REQUIRES_NEW:
                    if (jdbcTxHolder.isInTx()) {
                        suspendConn = jdbcTxHolder.pop();
                    }
                    // 开启新事务
                    break;
                case SUPPORTS:
                case MANDATORY:
                case NOT_SUPPORTED:
                case NEVER:
                    // 这些分支不需要开启新事务，直接执行
                    if (propagation == Propagation.MANDATORY && !jdbcTxHolder.isInTx()) {
                        throw new GirDsJdbcTxException("MANDATORY传播：当前无可用事务，禁止执行");
                    }
                    if (propagation == Propagation.NEVER && jdbcTxHolder.isInTx()) {
                        throw new GirDsJdbcTxException("NEVER传播：禁止在事务内部执行");
                    }
                    if (propagation == Propagation.NOT_SUPPORTED && jdbcTxHolder.isInTx()) {
                        suspendConn = jdbcTxHolder.pop();
                    }
                    return exec(execObj, param);
                case NESTED:
                    if (jdbcTxHolder.isInTx()) {
                        // 有外部事务，使用保存点
                        currConn = jdbcTxHolder.get();
                        savepoint = currConn.setSavepoint();
                        try {
                            T result = exec(execObj, param);
                            return result;
                        } catch (Throwable e) {
                            if (savepoint != null) {
                                currConn.rollback(savepoint);
                            }
                            throw e;
                        }
                    }
                    // 无事务，开启新事务（同 REQUIRED）
                    break;
                default:
                    throw new GirDsJdbcTxException("不支持的传播类型：" + propagation);
            }

            // 开启新事务（只有 REQUIRED、REQUIRES_NEW、NESTED无事务时 能走到这里）
            currConn = getCurrentConnection();
            if (level != null) {
                currConn.setTransactionIsolation(level.code);
            }
            currConn.setReadOnly(readOnly);
            currConn.setAutoCommit(false);
            jdbcTxHolder.bind(currConn);

            T result = exec(execObj, param);
            currConn.commit();

            return result;

        } catch (Throwable e) {
            // 判断是否需要回滚
            boolean needRoll = shouldRollback(e, rollFor, noRollFor);

            // 只有当前方法开启了事务才需要处理提交/回滚
            if (currConn != null && savepoint == null) {
                try {
                    if (needRoll) {
                        currConn.rollback();
                    } else {
                        currConn.commit();
                    }
                } catch (SQLException ex) {
                    throw new GirDsJdbcTxException("事务提交/回滚异常", ex);
                }
            }

            // 重新抛出原始异常
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else if (e instanceof Error) {
                throw (Error) e;
            } else {
                throw new GirDsJdbcTxException("事务执行异常", e);
            }

        } finally {
            // 清理当前方法开启的事务（有 savepoint 说明是嵌套事务，不在此关闭）
            if (currConn != null && savepoint == null) {
                jdbcTxHolder.pop();
                closeConnection(currConn);
            }

            // 恢复挂起的事务
            if (suspendConn != null) {
                jdbcTxHolder.bind(suspendConn);
            }
        }
    }

    /**
     * 统一执行 Runnable / Supplier / TxAction / TxFunc
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T, P> T exec(Object execObj, P param) {
        if (execObj instanceof Runnable) {
            ((Runnable) execObj).run();
            return null;
        } else if (execObj instanceof Supplier) {
            return (T) ((Supplier<?>) execObj).get();
        } else if (execObj instanceof TxAction) {
            ((TxAction<P>) execObj).run(param);
            return null;
        } else if (execObj instanceof TxFunc) {
            return ((TxFunc<P, T>) execObj).apply(param);
        }
        throw new GirDsJdbcTxException("不支持的执行对象类型：" + execObj.getClass());
    }

    /**
     * 回滚判定规则
     */
    private boolean shouldRollback(Throwable e,
                                   Class<? extends Throwable>[] rollFor,
                                   Class<? extends Throwable>[] noRollFor) {
        // noRollFor 优先
        if (noRollFor != null) {
            for (Class<?> clz : noRollFor) {
                if (clz.isInstance(e)) {
                    return false;
                }
            }
        }
        // rollFor 命中则回滚
        if (rollFor != null) {
            for (Class<?> clz : rollFor) {
                if (clz.isInstance(e)) {
                    return true;
                }
            }
        }
        // 默认：RuntimeException 和 Error 回滚
        return e instanceof RuntimeException || e instanceof Error;
    }

    /**
     * 获取数据库连接（优先从当前事务获取）
     */
    public Connection getCurrentConnection() throws SQLException {
        Connection txConn = jdbcTxHolder.get();
        if (txConn != null && !txConn.isClosed()) {
            return txConn;
        }
        return connectionManager.getConnection();
    }

    /**
     * 关闭数据库连接
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new GirDsJdbcTxException("关闭连接异常", e);
            }
        }
    }
}
