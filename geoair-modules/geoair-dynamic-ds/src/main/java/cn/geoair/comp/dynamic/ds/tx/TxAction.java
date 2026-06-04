package cn.geoair.comp.dynamic.ds.tx;

/**
 * 带入参、无返回值事务执行器
 *
 * @param <P> 入参类型
 */
@FunctionalInterface
public interface TxAction<P> {
    void run(P param);
}
