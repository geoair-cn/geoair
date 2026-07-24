package cn.geoair.comp.dynamic.ds.tx;

/**
 * 带入参、有返回值事务执行器
 *
 * @param <P> 入参类型
 * @param <R> 返回值类型
 */
@FunctionalInterface
public interface TxFunc<P, R> {
    R apply(P param);
}
