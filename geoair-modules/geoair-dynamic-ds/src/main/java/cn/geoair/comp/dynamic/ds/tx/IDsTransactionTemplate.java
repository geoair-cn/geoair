package cn.geoair.comp.dynamic.ds.tx;


import cn.geoair.comp.dynamic.ds.tx.enums.IsolationLevel;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * JDBC事务操作标准接口
 */
public interface IDsTransactionTemplate {

    void setTransactionConnectionHolder(IDsTransactionConnectionHolder transactionConnectionHolder);

    IDsTransactionConnectionHolder getTransactionConnectionHolder();

    /**
     * 获取当前的数据库链接
     *
     * @return
     * @throws SQLException
     */
    Connection getCurrentConnection() throws SQLException;

    /**
     * 关闭连接 ，事务中调用关闭的话，会直接跳过
     *
     * @param connection
     */
    void connectionClose(Connection connection);

    /**
     * 打开一个事务方法
     *
     * @param action 方法体
     */
    void tx(TxActionNp action);

    /**
     * 打开一个事务方法
     *
     * @param action 方法体
     */
    void tx(IsolationLevel level, TxActionNp action);

    /**
     * 打开一个事务方法
     *
     * @param action 方法体
     */
    <T> T txReturn(TxFuncNp<T> action);

    /**
     * 打开一个事务方法
     *
     * @param level  事务隔离级别
     * @param action 方法体
     */
    <T> T txReturn(IsolationLevel level, TxFuncNp<T> action);

    /**
     * 打开一个事务方法
     *
     * @param action 方法体
     * @param param  参数
     */
    <P> void tx(TxAction<P> action, P param);

    /**
     * 打开一个事务方法
     *
     * @param level  事务隔离级别
     * @param action 方法体
     * @param param  参数
     */
    <P> void tx(IsolationLevel level, TxAction<P> action, P param);

    /**
     * 打开一个事务方法
     *
     * @param action 方法体
     * @param param  参数
     */
    <P, R> R txReturn(TxFunc<P, R> action, P param);

    /**
     * 打开一个事务方法
     *
     * @param level  事务隔离级别
     * @param action 带参数与返回字的方法体
     * @param param  参数
     */
    <P, R> R txReturn(IsolationLevel level, TxFunc<P, R> action, P param);


    // 获取构造器
    GirDsJdbcTxBuilder txBuilder();
}
