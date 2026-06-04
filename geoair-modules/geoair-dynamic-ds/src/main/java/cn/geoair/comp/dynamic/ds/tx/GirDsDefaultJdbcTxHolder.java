package cn.geoair.comp.dynamic.ds.tx;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 线程事务连接持有器实现：Deque结构用于REQUIRES_NEW挂起上层连接
 */
public class GirDsDefaultJdbcTxHolder implements IDsTxHolder {

    public static GirDsDefaultJdbcTxHolder getInstance() {
        return new GirDsDefaultJdbcTxHolder();
    }

    private static final ThreadLocal<Deque<Connection>> CONN_HOLDER = new ThreadLocal<>();

    private Deque<Connection> getDeque() {
        Deque<Connection> deque = CONN_HOLDER.get();
        if (deque == null) {
            deque = new ArrayDeque<>();
            CONN_HOLDER.set(deque);
        }
        return deque;
    }

    @Override
    public void bind(Connection conn) {
        if (conn == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        getDeque().push(conn);
    }

    @Override
    public Connection get() {
        Deque<Connection> deque = CONN_HOLDER.get();
        if (deque == null || deque.isEmpty()) {
            return null;
        }
        Connection conn = deque.peek();
        if (conn != null) {
            try {
                if (conn.isClosed()) {
                    deque.pop();
                    return get();
                }
            } catch (SQLException e) {
                return null;
            }
        }
        return conn;
    }

    @Override
    public Connection pop() {
        Deque<Connection> deque = CONN_HOLDER.get();
        if (deque == null || deque.isEmpty()) {
            return null;
        }
        return deque.pop();
    }

    @Override
    public void remove() {
        Deque<Connection> deque = CONN_HOLDER.get();
        if (deque != null) {
            for (Connection conn : deque) {
                if (conn != null) {
                    try {
                        if (!conn.isClosed()) {
                            conn.close();
                        }
                    } catch (SQLException ignore) {
                        // 忽略关闭异常
                    }
                }
            }
            deque.clear();
        }
        CONN_HOLDER.remove();
    }

    @Override
    public boolean isInTx() {
        return get() != null;
    }

    @Override
    public int depth() {
        Deque<Connection> deque = CONN_HOLDER.get();
        return deque == null ? 0 : deque.size();
    }
}
