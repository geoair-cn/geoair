package cn.geoair.comp.dynamic.ds.readwrite.utils;

import cn.geoair.comp.dynamic.ds.readwrite.enums.SQLType;

/**
 * SQL解析工具类测试
 * 包含 PostgreSQL、MySQL 等主流数据库语法
 *
 * @author 张俊
 * @date 2026/5/28
 */
public class SQLParserUtilTest {

    public static void main(String[] args) {
        // 关闭调试日志
        SQLParserUtil.setDebugEnabled(false);

        System.out.println("========== SQL解析工具类测试（支持PostgreSQL） ==========\n");

        // 测试读操作
        testReadOperations();

        // 测试写操作
        testWriteOperations();

        // 测试PostgreSQL特有语法
        testPostgreSQLSpecific();

        // 测试复杂SQL
        testComplexSQL();

        // 测试边界情况
        testEdgeCases();

        // 批量测试统计
        testBatchStatistics();

        System.out.println("\n========== 测试完成 ==========");
    }

    /**
     * 测试读操作
     */
    private static void testReadOperations() {
        System.out.println("【读操作测试 - 基础】");

        String[] readSQLs = {
                "SELECT * FROM user",
                "SELECT id, name FROM user WHERE id = 1",
                "SELECT u.*, o.order_no FROM user u LEFT JOIN order o ON u.id = o.user_id",
                "SHOW TABLES",
                "SHOW CREATE TABLE user",
                "DESC user",
                "DESCRIBE user",
                "EXPLAIN SELECT * FROM user",
                "select * from user",  // 小写
                "  SELECT * FROM user  "  // 带空格
        };

        for (String sql : readSQLs) {
            boolean isRead = SQLParserUtil.isReadOperation(sql);
            SQLType type = SQLParserUtil.getSQLType(sql);
            System.out.printf("%-50s -> %s (%s)\n", sql, isRead ? "读操作 ✓" : "非读操作 ✗", type);
        }
        System.out.println();
    }

    /**
     * 测试写操作
     */
    private static void testWriteOperations() {
        System.out.println("【写操作测试 - 基础】");

        String[] writeSQLs = {
                "INSERT INTO user VALUES (1, 'test')",
                "INSERT INTO user (id, name) VALUES (1, 'test')",
                "UPDATE user SET name = 'new' WHERE id = 1",
                "DELETE FROM user WHERE id = 1",
                "REPLACE INTO user VALUES (1, 'test')",
                "TRUNCATE TABLE user",
                "CREATE TABLE test (id INT)",
                "ALTER TABLE user ADD COLUMN age INT",
                "DROP TABLE test",
                "GRANT SELECT ON user TO 'test'",
                "REVOKE SELECT ON user FROM 'test'",
                "insert into user values (1)",  // 小写
                "  UPDATE user SET name = 'test'  "  // 带空格
        };

        for (String sql : writeSQLs) {
            boolean isWrite = SQLParserUtil.isWriteOperation(sql);
            SQLType type = SQLParserUtil.getSQLType(sql);
            System.out.printf("%-50s -> %s (%s)\n", sql, isWrite ? "写操作 ✓" : "非写操作 ✗", type);
        }
        System.out.println();
    }

    /**
     * 测试PostgreSQL特有语法
     */
    private static void testPostgreSQLSpecific() {
        System.out.println("【PostgreSQL 特有语法测试】");

        // PostgreSQL WITH 语句 (CTE)
        String[] withSQLs = {
//                "WITH temp AS (SELECT id FROM user) SELECT * FROM temp",
//                "WITH recursive cte AS (SELECT 1 UNION ALL SELECT id+1 FROM cte WHERE id<10) SELECT * FROM cte",
//                "WITH user_cte AS (SELECT id, name FROM user WHERE age > 18) SELECT * FROM user_cte",
                "WITH moved_rows AS (SELECT FROM users WHERE age < 18 RETURNING *)  INSERT INTO users_archive SELECT * FROM moved_rows;",
//                "WITH orders AS (SELECT user_id, COUNT(*) as cnt FROM orders GROUP BY user_id) SELECT u.name, o.cnt FROM users u JOIN orders o ON u.id = o.user_id"
        };

        for (String sql : withSQLs) {
            boolean isRead = SQLParserUtil.isReadOperation(sql);
            SQLType type = SQLParserUtil.getSQLType(sql);
            SQLType jSqlParser = SQLParserUtil.parseWithJSqlParser(sql);
            System.out.println(sql+":"+jSqlParser);
            System.out.printf("%-70s -> %s (%s)\n",
                    sql.length() > 68 ? sql.substring(0, 65) + "..." : sql,
                    isRead ? "读操作 ✓" : "非读操作 ✗", type);
        }

        // PostgreSQL DO 语句
        System.out.println();
        String[] doSQLs = {
                "DO $$ BEGIN RAISE NOTICE 'Hello'; END $$",
                "DO $$ DECLARE i integer; BEGIN FOR i IN 1..10 LOOP INSERT INTO log VALUES(i); END LOOP; END $$",
                "DO LANGUAGE plpgsql $$ BEGIN PERFORM 1; END $$"
        };

        for (String sql : doSQLs) {
            boolean isWrite = SQLParserUtil.isWriteOperation(sql);
            SQLType type = SQLParserUtil.getSQLType(sql);
            System.out.printf("%-70s -> %s (%s)\n",
                    sql.length() > 68 ? sql.substring(0, 65) + "..." : sql,
                    isWrite ? "写操作 ✓" : "非写操作 ✗", type);
        }

        // PostgreSQL RETURNING 子句（INSERT/UPDATE/DELETE 带 RETURNING）
        System.out.println();
        String[] returningSQLs = {
                "INSERT INTO user (name) VALUES ('test') RETURNING id",
                "UPDATE user SET name = 'new' WHERE id = 1 RETURNING id, name",
                "DELETE FROM user WHERE id = 1 RETURNING *"
        };

        for (String sql : returningSQLs) {
            SQLType type = SQLParserUtil.getSQLType(sql);
            System.out.printf("%-70s -> %s\n", sql, type);
        }

        System.out.println();
    }

    /**
     * 测试复杂SQL
     */
    private static void testComplexSQL() {
        System.out.println("【复杂SQL测试】");

        String[] complexSQLs = {
                // 子查询
                "SELECT * FROM user WHERE id IN (SELECT user_id FROM orders WHERE amount > 100)",
                // 联合查询
                "SELECT * FROM user UNION SELECT * FROM admin",
                // 窗口函数
                "SELECT id, name, ROW_NUMBER() OVER (PARTITION BY age ORDER BY id) as rn FROM user",
                // 批量插入
                "INSERT INTO user (id, name) VALUES (1, 'a'), (2, 'b'), (3, 'c')",
                // 条件更新
                "UPDATE user SET age = CASE WHEN id = 1 THEN 20 ELSE 30 END WHERE id IN (1,2,3)",
                // 关联删除
                "DELETE FROM user USING orders WHERE user.id = orders.user_id",
                // 带注释的SQL
                "/* 查询用户 */ SELECT * FROM user WHERE id = 1",
                "-- 查询用户\nSELECT * FROM user WHERE id = 1",
                // 事务语句
                "BEGIN",
                "COMMIT",
                "ROLLBACK"
        };

        for (String sql : complexSQLs) {
            SQLType type = SQLParserUtil.getSQLType(sql);
            String displaySql = sql.length() > 60 ? sql.substring(0, 57) + "..." : sql;
            System.out.printf("%-60s -> %s\n", displaySql, type);
        }
        System.out.println();
    }

    /**
     * 测试边界情况
     */
    private static void testEdgeCases() {
        System.out.println("【边界测试】");

        // 空值和无效SQL
        Object[][] edgeCases = {
                {"null", SQLParserUtil.getSQLType(null)},
                {"空字符串", SQLParserUtil.getSQLType("")},
                {"空格", SQLParserUtil.getSQLType("   ")},
                {"无效SQL", SQLParserUtil.getSQLType("invalid sql")},
                {"只有注释", SQLParserUtil.getSQLType("/* only comment */")},
                {"纯数字", SQLParserUtil.getSQLType("12345")}
        };

        for (Object[] item : edgeCases) {
            System.out.printf("%-15s -> %s\n", item[0], item[1]);
        }
        System.out.println();
    }

    /**
     * 批量测试统计
     */
    private static void testBatchStatistics() {
        System.out.println("【批量测试统计】");

        String[] allSQLs = {
                // 读操作
                "SELECT * FROM user",
                "WITH temp AS (SELECT 1) SELECT * FROM temp",
                "SHOW TABLES",
                "DESC user",
                "EXPLAIN SELECT * FROM user",
                // 写操作
                "INSERT INTO user VALUES (1)",
                "UPDATE user SET name = 'test'",
                "DELETE FROM user",
                "CREATE TABLE test (id INT)",
                "DROP TABLE test",
                "DO $$ BEGIN END $$",
                // 未知
                "invalid sql",
                ""
        };

        int readCount = 0;
        int writeCount = 0;
        int unknownCount = 0;

        System.out.println("\n详细结果:");
        for (String sql : allSQLs) {
            SQLType type = SQLParserUtil.getSQLType(sql);
            System.out.printf("  %-45s -> %s\n",
                    sql.length() > 43 ? sql.substring(0, 40) + "..." : sql,
                    type);

            switch (type) {
                case READ: readCount++; break;
                case WRITE: writeCount++; break;
                case UNKNOWN: unknownCount++; break;
            }
        }

        System.out.println("\n统计结果:");
        System.out.println("  总测试数: " + allSQLs.length);
        System.out.println("  读操作: " + readCount);
        System.out.println("  写操作: " + writeCount);
        System.out.println("  未知: " + unknownCount);

        // 计算识别率
        int recognized = readCount + writeCount;
        double recognitionRate = (double) recognized / allSQLs.length * 100;
        System.out.printf("  识别率: %.2f%%\n", recognitionRate);
    }
}
