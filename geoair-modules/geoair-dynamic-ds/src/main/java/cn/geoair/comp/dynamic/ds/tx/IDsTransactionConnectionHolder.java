package cn.geoair.comp.dynamic.ds.tx;

import java.sql.Connection;

/**
 * 线程事务连接持有器接口
 * 用于管理当前线程的事务连接，支持连接挂起和恢复
 */
public interface IDsTransactionConnectionHolder {

    /**
     * 绑定连接到当前线程
     *
     * @param conn 数据库连接，不能为null
     * @throws IllegalArgumentException 当conn为null时抛出
     */
    void bind(Connection conn);

    /**
     * 获取当前线程的顶部连接（不移除）
     *
     * @return 当前连接的Connection，如果没有则返回null
     */
    Connection get();

    /**
     * 弹出当前线程的顶部连接（移除）
     *
     * @return 弹出的Connection，如果没有则返回null
     */
    Connection pop();

    /**
     * 移除当前线程的所有连接并清理资源
     */
    void remove();

    /**
     * 判断当前线程是否在事务中
     *
     * @return true: 有事务连接, false: 无事务连接
     */
    boolean isInTx();

    /**
     * 获取当前事务栈深度（用于调试）
     *
     * @return 栈深度，如果没有事务则返回0
     */
    int depth();
}
