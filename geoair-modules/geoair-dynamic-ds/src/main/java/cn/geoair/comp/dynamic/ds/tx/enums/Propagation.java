package cn.geoair.comp.dynamic.ds.tx.enums;

/** 事务传播行为 */
public enum Propagation {
    /** 有事务沿用，无则新建【默认】 */
    REQUIRED,
    /** 无论有无事务，新建独立事务 */
    REQUIRES_NEW,
    /** 嵌套事务，依赖上层事务+保存点 */
    NESTED,
    /** 有事务就用，无事务非事务执行 */
    SUPPORTS,
    /** 必须在已有事务中运行，无事务抛异常 */
    MANDATORY,
    /** 挂起上层事务，以非事务运行 */
    NOT_SUPPORTED,
    /** 禁止在事务中运行，存在事务抛异常 */
    NEVER
}
