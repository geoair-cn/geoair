package cn.geoair.comp.dynamic.ds.tx;


import cn.geoair.comp.dynamic.ds.tx.enums.IsolationLevel;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * JDBC事务操作标准接口
 */
public interface IDsTransactionTemplate {

    void setTxHolder(IDsTransactionConnectionHolder jdbcTxHolder);

    IDsTransactionConnectionHolder getTxHolder();

    Connection getCurrentConnection() throws SQLException;

    void connectionClose(Connection connection);

    void tx(TxActionNp action);

    void tx(IsolationLevel level, TxActionNp action);

    <T> T txReturn(TxFuncNp<T> txFuncNp);

    <T> T txReturn(IsolationLevel level, TxFuncNp<T> txFuncNp);


    <P> void tx(TxAction<P> action, P param);

    <P> void tx(IsolationLevel level, TxAction<P> action, P param);

    <P, R> R txReturn(TxFunc<P, R> func, P param);

    <P, R> R txReturn(IsolationLevel level, TxFunc<P, R> func, P param);


    // 获取构造器（链式配置）
    GirDsJdbcTxBuilder builder();
}
