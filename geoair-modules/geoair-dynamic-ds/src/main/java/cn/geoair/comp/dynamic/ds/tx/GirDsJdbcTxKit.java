package cn.geoair.comp.dynamic.ds.tx;

import cn.geoair.comp.dynamic.ds.base.IDsConnectionOpt;
import cn.geoair.comp.dynamic.ds.tx.enums.IsolationLevel;

import java.util.function.Supplier;

/**
 * 事务静态调用入口
 */
public final class GirDsJdbcTxKit {


    IDsConnectionOpt iDsConnectionManager;

    private final GirDefaultIDsTxTemplate TEMPLATE;

    public GirDsJdbcTxKit(IDsConnectionOpt iDsConnectionManager) {
        this.iDsConnectionManager = iDsConnectionManager;
        this.TEMPLATE = new GirDefaultIDsTxTemplate(iDsConnectionManager);
    }

    /**
     * 获取事务工具实例
     */
    public static GirDsJdbcTxKit getInstance(IDsConnectionOpt iDsConnectionManager) {
        return new GirDsJdbcTxKit(iDsConnectionManager);
    }

    /**
     * 执行事务（无返回值）
     */
    public void tx(Runnable action) {
        TEMPLATE.tx(action);
    }

    /**
     * 执行事务（无返回值，指定隔离级别）
     */
    public void tx(IsolationLevel level, Runnable action) {
        TEMPLATE.tx(level, action);
    }

    /**
     * 执行事务（带返回值）
     */
    public <T> T txReturn(Supplier<T> supplier) {
        return TEMPLATE.txReturn(supplier);
    }

    /**
     * 执行事务（带返回值，指定隔离级别）
     */
    public <T> T txReturn(IsolationLevel level, Supplier<T> supplier) {
        return TEMPLATE.txReturn(level, supplier);
    }

    /**
     * 执行事务（带参数，无返回值）
     */
    public <P> void tx(TxAction<P> action, P param) {
        TEMPLATE.tx(action, param);
    }

    /**
     * 执行事务（带参数，无返回值，指定隔离级别）
     */
    public <P> void tx(IsolationLevel level, TxAction<P> action, P param) {
        TEMPLATE.tx(level, action, param);
    }

    /**
     * 执行事务（带参数，带返回值）
     */
    public <P, R> R txReturn(TxFunc<P, R> func, P param) {
        return TEMPLATE.txReturn(func, param);
    }

    /**
     * 执行事务（带参数，带返回值，指定隔离级别）
     */
    public <P, R> R txReturn(IsolationLevel level, TxFunc<P, R> func, P param) {
        return TEMPLATE.txReturn(level, func, param);
    }

    /**
     * 获取事务构建器（链式调用）
     */
    public GirDsJdbcTxBuilder builder() {
        return TEMPLATE.builder();
    }
}
