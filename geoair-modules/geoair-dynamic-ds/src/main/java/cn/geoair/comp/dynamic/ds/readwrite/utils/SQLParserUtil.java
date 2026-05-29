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

        // 使用 JSqlParser 解析
        return parseWithJSqlParser(sql);
    }


    /**
     * 使用 JSqlParser 解析 SQL
     *
     * @param sql SQL语句
     * @return SQL操作类型
     */
    public static SQLType parseWithJSqlParser(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            SQLType type = getTypeFromStatement(statement);
            if (debugEnabled) {
                log.debug("JSqlParser解析成功，SQL类型: {}, SQL: {}", type, sql);
            }
            return type;
        } catch (JSQLParserException e) {
            if (debugEnabled) {
                log.debug("JSqlParser解析失败，降级使用关键词匹配。SQL: {}", sql);
            }
            return fastMatch(sql);
        }
    }

    /**
     * 快速关键词匹配
     */
    private static SQLType fastMatch(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return SQLType.UNKNOWN;
        }

        String trimmed = sql.trim();
        String upperSql = trimmed.toUpperCase();

        // 读操作关键词
        if (upperSql.startsWith("SELECT") ||
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

        // WITH 语句特殊处理
        if (upperSql.startsWith("WITH")) {
            return parseWithStatementType(trimmed);
        }

        return SQLType.UNKNOWN;
    }


    /**
     * 解析 WITH 语句的类型
     */
    private static SQLType parseWithStatementType(String sql) {
        // 移除 WITH 关键字（支持 WITH RECURSIVE）
        String afterWith = sql.substring(4).trim();
        if (afterWith.toUpperCase().startsWith("RECURSIVE")) {
            afterWith = afterWith.substring(9).trim();
        }

        try {
            // 方法1：先检查 CTE 中是否包含写操作
            if (containsWriteInCTE(sql)) {
                return SQLType.WRITE;
            }

            // 方法2：查找主查询（找到所有 CTE 结束后的第一个关键词）
            String mainQuery = extractMainQuery(sql);
            if (mainQuery != null) {
                String upperMain = mainQuery.trim().toUpperCase();

                if (upperMain.startsWith("SELECT")) {
                    return SQLType.READ;
                }
                if (upperMain.startsWith("INSERT") ||
                        upperMain.startsWith("UPDATE") ||
                        upperMain.startsWith("DELETE") ||
                        upperMain.startsWith("REPLACE") ||
                        upperMain.startsWith("TRUNCATE")) {
                    return SQLType.WRITE;
                }
            }

            // 方法3：检查整个 SQL 中是否包含写操作关键词（在 CTE 之后）
            if (containsWriteAfterCTE(sql)) {
                return SQLType.WRITE;
            }

            // 默认当作读操作
            return SQLType.READ;

        } catch (Exception e) {
            if (debugEnabled) {
                log.debug("WITH 语句解析异常: {}", e.getMessage());
            }
            // 降级：检查是否包含写操作关键词
            String upperSql = sql.toUpperCase();
            if (upperSql.contains(" INSERT ") ||
                    upperSql.contains(" UPDATE ") ||
                    upperSql.contains(" DELETE ") ||
                    upperSql.matches(".*\\b(INSERT|UPDATE|DELETE)\\b.*")) {
                return SQLType.WRITE;
            }
            return SQLType.READ;
        }
    }

    /**
     * 提取主查询（所有 CTE 结束后的 SQL 部分）
     */
    private static String extractMainQuery(String sql) {
        // 移除 WITH 关键字
        String remaining = sql.substring(4).trim();
        if (remaining.toUpperCase().startsWith("RECURSIVE")) {
            remaining = remaining.substring(9).trim();
        }

        // 找到所有 CTE 结束的位置
        int endOfCTEs = findEndOfCTEs(remaining);
        if (endOfCTEs > 0 && endOfCTEs < remaining.length()) {
            return remaining.substring(endOfCTEs).trim();
        }

        return null;
    }

    /**
     * 找到所有 CTE 定义结束的位置
     */
    private static int findEndOfCTEs(String sql) {
        int index = 0;
        int length = sql.length();

        while (index < length) {
            // 找到 AS ( 或 AS(
            int asIndex = findASIndex(sql, index);
            if (asIndex == -1) {
                break;
            }

            // 找到 CTE 内容的开始和结束
            int startParen = sql.indexOf('(', asIndex);
            if (startParen == -1) {
                break;
            }

            int endParen = findMatchingParen(sql, startParen);
            if (endParen == -1) {
                break;
            }

            // 移动到 CTE 结束后的位置
            index = endParen + 1;

            // 跳过空格
            while (index < length && sql.charAt(index) == ' ') {
                index++;
            }

            // 检查是否有逗号（多个 CTE）
            if (index < length && sql.charAt(index) == ',') {
                index++;
                continue;
            }

            // 没有逗号，说明 CTE 定义结束，返回当前位置
            return index;
        }

        return -1;
    }

    /**
     * 查找匹配的括号位置
     */
    private static int findMatchingParen(String str, int startPos) {
        int count = 1;
        for (int i = startPos + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '(') {
                count++;
            } else if (c == ')') {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 查找 AS ( 或 AS( 的位置
     */
    private static int findASIndex(String sql, int start) {
        int index = start;
        while (index < sql.length()) {
            int asPos = sql.toUpperCase().indexOf("AS", index);
            if (asPos == -1) {
                return -1;
            }

            // 检查 AS 后面是否是 ( 或空格+(
            int afterAs = asPos + 2;
            while (afterAs < sql.length() && sql.charAt(afterAs) == ' ') {
                afterAs++;
            }

            if (afterAs < sql.length() && sql.charAt(afterAs) == '(') {
                return asPos;
            }

            index = asPos + 2;
        }
        return -1;
    }

    /**
     * 检查 CTE 之后是否包含写操作
     */
    private static boolean containsWriteAfterCTE(String sql) {
        String remaining = sql.substring(4).trim();
        if (remaining.toUpperCase().startsWith("RECURSIVE")) {
            remaining = remaining.substring(9).trim();
        }

        int endOfCTEs = findEndOfCTEs(remaining);
        if (endOfCTEs > 0 && endOfCTEs < remaining.length()) {
            String afterCTE = remaining.substring(endOfCTEs).trim().toUpperCase();
            if (afterCTE.startsWith("INSERT") ||
                    afterCTE.startsWith("UPDATE") ||
                    afterCTE.startsWith("DELETE") ||
                    afterCTE.startsWith("REPLACE") ||
                    afterCTE.startsWith("TRUNCATE")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查 CTE 中是否包含写操作
     */
    private static boolean containsWriteInCTE(String sql) {
        // 移除 WITH 关键字
        String afterWith = sql.substring(4).trim();
        if (afterWith.toUpperCase().startsWith("RECURSIVE")) {
            afterWith = afterWith.substring(9).trim();
        }

        // 遍历每个 CTE
        int index = 0;
        while (index < afterWith.length()) {
            // 找到 AS ( 或 AS(
            int asIndex = findASIndex(afterWith, index);
            if (asIndex == -1) {
                break;
            }

            // 找到 CTE 内容
            int startParen = afterWith.indexOf('(', asIndex);
            if (startParen == -1) {
                break;
            }

            int endParen = findMatchingParen(afterWith, startParen);
            if (endParen == -1) {
                break;
            }

            // 检查 CTE 内容
            String cteContent = afterWith.substring(startParen + 1, endParen).trim().toUpperCase();
            if (cteContent.startsWith("INSERT") ||
                    cteContent.startsWith("UPDATE") ||
                    cteContent.startsWith("DELETE")) {
                return true;
            }

            // 移动到下一个 CTE
            index = endParen + 1;

            // 跳过空格
            while (index < afterWith.length() && afterWith.charAt(index) == ' ') {
                index++;
            }

            // 检查是否有逗号
            if (index < afterWith.length() && afterWith.charAt(index) == ',') {
                index++;
                continue;
            }

            // 没有逗号，CTE 结束
            break;
        }

        return false;
    }


    /**
     * 从 Statement 对象获取 SQL 类型
     *
     * @param statement JSqlParser 解析后的 Statement 对象
     * @return SQL操作类型
     */
    private static SQLType getTypeFromStatement(Statement statement) {
        if (statement instanceof Select) {
            return SQLType.READ;
        }

        if (statement instanceof Insert ||
                statement instanceof Update ||
                statement instanceof Delete) {
            return SQLType.WRITE;
        }

        // 其他类型（DDL、DCL等）也归类为写操作
        return SQLType.WRITE;
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
     * 设置是否启用调试日志
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }
}
