package cn.geoair.comp.dynamic.ds.tx;

import cn.geoair.comp.dynamic.ds.base.IDsConnectionOpt;
import cn.geoair.comp.dynamic.ds.tx.enums.IsolationLevel;
import cn.geoair.comp.dynamic.ds.tx.enums.Propagation;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 事务操作实现 */
public class GirDsTransactionTemplate implements IDsTransactionTemplate {

    private static final Logger log = LoggerFactory.getLogger(GirDsTransactionTemplate.class);

    IDsConnectionOpt connectionOpt;
    IDsTransactionConnectionHolder transactionConnectionHolder;

    public GirDsTransactionTemplate(IDsConnectionOpt connectionOpt) {
        this.connectionOpt = connectionOpt;
        this.transactionConnectionHolder = GirDsDefaultJdbcTxHolder.getInstance();
    }

    public GirDsTransactionTemplate(
            IDsConnectionOpt connectionOpt, IDsTransactionConnectionHolder jdbcTxHolder) {
        this.connectionOpt = connectionOpt;
        this.transactionConnectionHolder = jdbcTxHolder;
    }

    @Override
    public void setTransactionConnectionHolder(
            IDsTransactionConnectionHolder transactionConnectionHolder) {
        this.transactionConnectionHolder = transactionConnectionHolder;
    }

    @Override
    public IDsTransactionConnectionHolder getTransactionConnectionHolder() {
        return this.transactionConnectionHolder;
    }

    @Override
    public void tx(TxActionNp action) {
        txBuilder().run(action);
    }

    @Override
    public void tx(IsolationLevel level, TxActionNp action) {
        txBuilder().isolation(level).run(action);
    }

    @Override
    public <T> T txReturn(TxFuncNp<T> txFuncNp) {
        return txBuilder().call(txFuncNp);
    }

    @Override
    public <T> T txReturn(IsolationLevel level, TxFuncNp<T> txFuncNp) {
        return txBuilder().isolation(level).call(txFuncNp);
    }

    @Override
    public <P> void tx(TxAction<P> action, P param) {
        txBuilder().run(action, param);
    }

    @Override
    public <P> void tx(IsolationLevel level, TxAction<P> action, P param) {
        txBuilder().isolation(level).run(action, param);
    }

    @Override
    public <P, R> R txReturn(TxFunc<P, R> func, P param) {
        return txBuilder().call(func, param);
    }

    @Override
    public <P, R> R txReturn(IsolationLevel level, TxFunc<P, R> func, P param) {
        return txBuilder().isolation(level).call(func, param);
    }

    @Override
    public GirDsJdbcTxBuilder txBuilder() {
        return new GirDsJdbcTxBuilder(this);
    }

    /** 统一核心事务入口 */
    @SuppressWarnings("unchecked")
    <T, P> T doTx(
            Propagation propagation,
            IsolationLevel level,
            boolean readOnly,
            Class<? extends Throwable>[] rollFor,
            Class<? extends Throwable>[] noRollFor,
            Object execObj,
            P param) {

        Connection suspendConn = null;
        Connection currConn = null;
        Savepoint savepoint = null;

        // 保存原始连接状态，用于恢复
        boolean needRestoreState = false;
        boolean originalAutoCommit = true;
        int originalIsolationLevel = Connection.TRANSACTION_READ_COMMITTED;
        boolean originalReadOnly = false;

        try {
            // 传播行为处理
            switch (propagation) {
                case REQUIRED:
                    if (transactionConnectionHolder.isInTx()) {
                        return exec(execObj, param);
                    }
                    // 无事务，开启新事务
                    break;
                case REQUIRES_NEW:
                    if (transactionConnectionHolder.isInTx()) {
                        suspendConn = transactionConnectionHolder.pop();
                    }
                    // 开启新事务
                    break;
                case SUPPORTS:
                case MANDATORY:
                case NOT_SUPPORTED:
                case NEVER:
                    // 这些分支不需要开启新事务，直接执行
                    if (propagation == Propagation.MANDATORY
                            && !transactionConnectionHolder.isInTx()) {
                        throw new GirDsJdbcTxException("MANDATORY传播：当前无可用事务，禁止执行");
                    }
                    if (propagation == Propagation.NEVER && transactionConnectionHolder.isInTx()) {
                        throw new GirDsJdbcTxException("NEVER传播：禁止在事务内部执行");
                    }
                    if (propagation == Propagation.NOT_SUPPORTED
                            && transactionConnectionHolder.isInTx()) {
                        suspendConn = transactionConnectionHolder.pop();
                    }
                    return exec(execObj, param);
                case NESTED:
                    if (transactionConnectionHolder.isInTx()) {
                        // 有外部事务，使用保存点
                        currConn = transactionConnectionHolder.get();
                        savepoint = currConn.setSavepoint();
                        try {
                            T result = exec(execObj, param);
                            return result;
                        } catch (Throwable e) {
                            if (savepoint != null) {
                                try {
                                    currConn.rollback(savepoint);
                                } catch (SQLException ex) {
                                    log.warn("回滚保存点失败", ex);
                                }
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

            // 保存原始连接状态
            originalAutoCommit = currConn.getAutoCommit();
            originalIsolationLevel = currConn.getTransactionIsolation();
            originalReadOnly = currConn.isReadOnly();
            needRestoreState = true;

            // 设置事务属性
            if (level != null) {
                currConn.setTransactionIsolation(level.code);
            }
            currConn.setReadOnly(readOnly);
            currConn.setAutoCommit(false);

            // 绑定到当前线程
            transactionConnectionHolder.bind(currConn);

            // 执行业务逻辑
            T result = exec(execObj, param);

            // 提交事务
            currConn.commit();

            return result;

        } catch (Throwable e) {
            // 判断是否需要回滚
            boolean needRoll = shouldRollback(e, rollFor, noRollFor);

            // 只有当前方法开启了事务才需要处理回滚
            if (currConn != null && savepoint == null) {
                try {
                    if (needRoll) {
                        currConn.rollback();
                        log.debug("事务回滚成功");
                    } else {
                        currConn.commit();
                        log.debug("事务提交成功（不回滚异常）");
                    }
                } catch (SQLException ex) {
                    log.error("事务提交/回滚异常", ex);
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
            // 恢复连接状态并清理当前方法开启的事务（有 savepoint 说明是嵌套事务，不在此关闭）
            if (currConn != null && savepoint == null) {
                try {
                    // 恢复原始连接状态（重要：防止连接池污染）
                    if (needRestoreState) {
                        currConn.setAutoCommit(originalAutoCommit);
                        currConn.setTransactionIsolation(originalIsolationLevel);
                        currConn.setReadOnly(originalReadOnly);
                    }
                } catch (SQLException e) {
                    log.warn("恢复连接状态失败", e);
                }

                // 从当前线程解绑
                transactionConnectionHolder.pop();

                // 关闭连接（归还连接池）
                connectionOpt.connectionClose(currConn);
            }

            // 恢复挂起的事务
            if (suspendConn != null) {
                try {
                    transactionConnectionHolder.bind(suspendConn);
                } catch (Exception e) {
                    log.warn("恢复挂起事务失败", e);
                }
            }
        }
    }

    /** 统一执行 Runnable / Supplier / TxAction / TxFunc */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T, P> T exec(Object execObj, P param) {
        if (execObj instanceof TxActionNp) {
            ((TxActionNp) execObj).run();
            return null;
        } else if (execObj instanceof TxFuncNp) {
            return (T) ((TxFuncNp<?>) execObj).apply();
        } else if (execObj instanceof TxAction) {
            ((TxAction<P>) execObj).run(param);
            return null;
        } else if (execObj instanceof TxFunc) {
            return ((TxFunc<P, T>) execObj).apply(param);
        }
        throw new GirDsJdbcTxException("不支持的执行对象类型：" + execObj.getClass());
    }

    /** 回滚判定规则 */
    private boolean shouldRollback(
            Throwable e,
            Class<? extends Throwable>[] rollFor,
            Class<? extends Throwable>[] noRollFor) {
        // noRollFor 优先（这些异常不回滚）
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

    /** 获取数据库连接（优先从当前事务获取） */
    public Connection getCurrentConnection() throws SQLException {
        Connection txConn = transactionConnectionHolder.get();
        if (txConn != null && !txConn.isClosed()) {
            return txConn;
        }
        return connectionOpt.getConnection();
    }

    /** 关闭连接（非事务中才真正关闭） */
    public void connectionClose(Connection connection) {
        boolean inTx = transactionConnectionHolder.isInTx();
        if (inTx) {
            return;
        } else {
            connectionOpt.connectionClose(connection);
        }
    }
}
