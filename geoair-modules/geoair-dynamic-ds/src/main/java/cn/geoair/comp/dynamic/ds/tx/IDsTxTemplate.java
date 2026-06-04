package cn.geoair.comp.dynamic.ds.tx;


import cn.geoair.comp.dynamic.ds.tx.enums.IsolationLevel;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * JDBC事务操作标准接口
 */
public interface IDsTxTemplate {

    void setJdbcTxHolder(IDsTxHolder jdbcTxHolder);

    IDsTxHolder getJdbcTxHolder();

    Connection getCurrentConnection() throws SQLException;

    void tx(Runnable action);

    void tx(IsolationLevel level, Runnable action);

    <T> T txReturn(Supplier<T> supplier);

    <T> T txReturn(IsolationLevel level, Supplier<T> supplier);


    <P> void tx(TxAction<P> action, P param);

    <P> void tx(IsolationLevel level, TxAction<P> action, P param);

    <P, R> R txReturn(TxFunc<P, R> func, P param);

    <P, R> R txReturn(IsolationLevel level, TxFunc<P, R> func, P param);


    // 获取构造器（链式配置）
    GirDsJdbcTxBuilder builder();
}
