package cn.geoair.comp.dynamic.ds.readwrite.utils;

import cn.geoair.comp.dynamic.ds.readwrite.enums.SQLType;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQL解析工具类
 * 用于识别SQL语句的类型（读/写）

 *
 * @author 张俊
 * @date 2026/5/28
 */
public class SQLParserUtil {

    private static final Logger log = LoggerFactory.getLogger(SQLParserUtil.class);

    /**
     * 是否启用调试日志（生产环境设为false）
     */
    private static boolean debugEnabled = false;

    /**
     * 获取SQL操作类型
     *
     * @param sql SQL语句
     * @return SQL操作类型
     */
    public static SQLType getSQLType(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            if (debugEnabled) {
                log.debug("SQL为空，返回UNKNOWN");
            }
            return SQLType.UNKNOWN;
        }

        // 先尝试关键词快速匹配（提高性能，特别是对于特殊语句）
        SQLType fastType = fastMatch(sql);
        if (fastType != SQLType.UNKNOWN) {
            return fastType;
        }

        try {
            Statement statement = CCJSqlParserUtil.parse(sql);

            if (statement instanceof Select) {
                return SQLType.READ;
            }

            if (statement instanceof Insert ||
                    statement instanceof Update ||
                    statement instanceof Delete) {
                return SQLType.WRITE;
            }

            return SQLType.WRITE;

        } catch (JSQLParserException e) {
            if (debugEnabled) {
                log.debug("JSqlParser解析失败，降级使用关键词匹配。SQL: {}", sql);
            }
            return fallbackGetSQLType(sql);
        }
    }

    /**
     * 快速关键词匹配（优先执行，提高性能）
     *
     * @param sql SQL语句
     * @return SQL操作类型，UNKNOWN表示无法快速判断
     */
    private static SQLType fastMatch(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return SQLType.UNKNOWN;
        }

        String upperSql = sql.trim().toUpperCase();

        // 读操作关键词
        if (upperSql.startsWith("SELECT") ||
                upperSql.startsWith("WITH") ||      // PostgreSQL CTE
                upperSql.startsWith("SHOW") ||
                upperSql.startsWith("DESC") ||
                upperSql.startsWith("DESCRIBE") ||
                upperSql.startsWith("EXPLAIN")) {
            return SQLType.READ;
        }

        // 写操作关键词
        if (upperSql.startsWith("INSERT") ||
                upperSql.startsWith("UPDATE") ||
                upperSql.startsWith("DELETE") ||
                upperSql.startsWith("REPLACE") ||
                upperSql.startsWith("TRUNCATE") ||
                upperSql.startsWith("CREATE") ||
                upperSql.startsWith("ALTER") ||
                upperSql.startsWith("DROP") ||
                upperSql.startsWith("GRANT") ||
                upperSql.startsWith("REVOKE") ||
                upperSql.startsWith("DO")) {        // PostgreSQL DO 语句
            return SQLType.WRITE;
        }

        return SQLType.UNKNOWN;
    }

    /**
     * 判断是否是读操作
     *
     * @param sql SQL语句
     * @return true-读操作，false-非读操作
     */
    public static boolean isReadOperation(String sql) {
        return getSQLType(sql) == SQLType.READ;
    }

    /**
     * 判断是否是写操作
     *
     * @param sql SQL语句
     * @return true-写操作，false-非写操作
     */
    public static boolean isWriteOperation(String sql) {
        SQLType type = getSQLType(sql);
        return type == SQLType.WRITE;
    }

    /**
     * 降级方案：关键词匹配
     *
     * @param sql SQL语句
     * @return SQL操作类型
     */
    private static SQLType fallbackGetSQLType(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return SQLType.UNKNOWN;
        }

        String upperSql = sql.trim().toUpperCase();

        // 读操作关键词
        if (upperSql.startsWith("SELECT") ||
                upperSql.startsWith("WITH") ||
                upperSql.startsWith("SHOW") ||
                upperSql.startsWith("DESC") ||
                upperSql.startsWith("DESCRIBE") ||
                upperSql.startsWith("EXPLAIN")) {
            return SQLType.READ;
        }

        // 写操作关键词
        if (upperSql.startsWith("INSERT") ||
                upperSql.startsWith("UPDATE") ||
                upperSql.startsWith("DELETE") ||
                upperSql.startsWith("REPLACE") ||
                upperSql.startsWith("TRUNCATE") ||
                upperSql.startsWith("CREATE") ||
                upperSql.startsWith("ALTER") ||
                upperSql.startsWith("DROP") ||
                upperSql.startsWith("GRANT") ||
                upperSql.startsWith("REVOKE") ||
                upperSql.startsWith("DO")) {
            return SQLType.WRITE;
        }

        if (debugEnabled) {
            log.debug("无法识别的SQL类型: {}", sql);
        }
        return SQLType.UNKNOWN;
    }

    /**
     * 设置是否启用调试日志
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }
}
