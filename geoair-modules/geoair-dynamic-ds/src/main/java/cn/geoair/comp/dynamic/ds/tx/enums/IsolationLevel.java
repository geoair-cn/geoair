package cn.geoair.comp.dynamic.ds.tx.enums;

import java.sql.Connection;

/** JDBC事务隔离级别枚举 */
public enum IsolationLevel {
    /** 读未提交：允许脏读、不可重复读、幻读 */
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    /** 读已提交：避免脏读，存在不可重复读、幻读 */
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    /** 可重复读：MySQL默认，避免脏读、不可重复读，存在幻读 */
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    /** 串行化：最高级别，全部问题都避免，性能最差 */
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    public final int code;

    IsolationLevel(int code) {
        this.code = code;
    }
}
