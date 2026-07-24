package cn.geoair.comp.dynamic.ds.tx;

/** 带入参、无返回值事务执行器 */
@FunctionalInterface
public interface TxActionNp {
    void run();
}
