package cn.geoair.comp.dynamic.ds.readwrite.test;

import cn.geoair.comp.dynamic.ds.readwrite.enums.SQLType;
import cn.geoair.comp.dynamic.ds.readwrite.utils.SQLParserUtil;

/**
 * WITH 语句（CTE）测试用例 测试 PostgreSQL 等各种 WITH 语法
 *
 * @author 张俊
 * @date 2026/5/29
 */
public class WithStatementTest {

    public static void main(String[] args) {

        System.out.println("========== WITH 语句测试用例 ==========\n");

        // 1. 读操作 - 简单 WITH + SELECT
        testReadOperations();

        // 2. 写操作 - WITH + INSERT/UPDATE/DELETE
        testWriteOperations();

        // 3. 复杂场景 - 递归CTE、多个CTE
        testComplexScenarios();

        // 4. 边界情况
        testEdgeCases();

        System.out.println("\n========== 测试完成 ==========");
    }

    /** 测试读操作类型的 WITH 语句 */
    private static void testReadOperations() {
        System.out.println("【读操作测试 - 应该识别为 READ】");

        String[] readSQLs = {
            // 基础 WITH + SELECT
            "WITH temp AS (SELECT id, name FROM users) SELECT * FROM temp",

            // 多个 CTE
            "WITH cte1 AS (SELECT id FROM users), cte2 AS (SELECT user_id FROM orders) SELECT * FROM cte1 JOIN cte2 ON cte1.id = cte2.user_id",

            // 带 WHERE 条件
            "WITH active_users AS (SELECT * FROM users WHERE status = 'active') SELECT * FROM active_users WHERE age > 18",

            // 带 JOIN
            "WITH user_orders AS (SELECT u.id, u.name, o.order_no FROM users u JOIN orders o ON u.id = o.user_id) SELECT * FROM user_orders",

            // 带聚合函数
            "WITH order_stats AS (SELECT user_id, COUNT(*) as order_count FROM orders GROUP BY user_id) SELECT u.name, os.order_count FROM users u JOIN order_stats os ON u.id = os.user_id",

            // PostgreSQL 递归 CTE
            "WITH RECURSIVE cte AS (SELECT 1 as n UNION ALL SELECT n + 1 FROM cte WHERE n < 10) SELECT * FROM cte",

            // 递归 CTE 获取树形结构
            "WITH RECURSIVE org_tree AS (SELECT id, name, parent_id FROM departments WHERE parent_id IS NULL UNION ALL SELECT d.id, d.name, d.parent_id FROM departments d INNER JOIN org_tree ot ON d.parent_id = ot.id) SELECT * FROM org_tree",

            // 带 DISTINCT
            "WITH unique_users AS (SELECT DISTINCT user_id FROM orders) SELECT * FROM unique_users",

            // 带 ORDER BY
            "WITH sorted AS (SELECT * FROM users ORDER BY created_at DESC) SELECT * FROM sorted LIMIT 10",

            // 带 LIMIT
            "WITH top_users AS (SELECT * FROM users ORDER BY points DESC LIMIT 5) SELECT * FROM top_users"
        };

        for (String sql : readSQLs) {
            SQLType type = SQLParserUtil.getSQLType(sql);
            boolean isRead = SQLParserUtil.isReadOperation(sql);
            System.out.printf("%-80s -> %s (%s)\n", truncate(sql, 78), type, isRead ? "✓" : "✗");
        }
        System.out.println();
    }

    /** 测试写操作类型的 WITH 语句 */
    private static void testWriteOperations() {
        System.out.println("【写操作测试 - 应该识别为 WRITE】");

        String[] writeSQLs = {
            // WITH + INSERT
            "WITH new_users AS (SELECT 'test' as name, 20 as age) INSERT INTO users (name, age) SELECT name, age FROM new_users",

            // WITH + UPDATE
            "WITH inactive_users AS (SELECT id FROM users WHERE last_login < '2024-01-01') UPDATE users SET status = 'inactive' WHERE id IN (SELECT id FROM inactive_users)",

            // WITH + DELETE
            "WITH old_orders AS (SELECT id FROM orders WHERE created_at < '2023-01-01') DELETE FROM orders WHERE id IN (SELECT id FROM old_orders)",

            // WITH + INSERT ... RETURNING
            "WITH inserted AS (INSERT INTO users (name, age) VALUES ('test', 20) RETURNING id) SELECT * FROM inserted",

            // WITH + UPDATE ... RETURNING
            "WITH updated AS (UPDATE users SET status = 'active' WHERE id = 1 RETURNING *) SELECT * FROM updated",

            // WITH + DELETE ... RETURNING
            "WITH deleted AS (DELETE FROM users WHERE id = 1 RETURNING *) SELECT * FROM deleted",

            // 多个 CTE + INSERT
            "WITH cte1 AS (SELECT 1 as id, 'test' as name), cte2 AS (SELECT 2 as id, 'test2' as name) INSERT INTO users (id, name) SELECT id, name FROM cte1 UNION SELECT id, name FROM cte2",

            // CTE 中包含写操作的复杂场景
            "WITH moved_rows AS (DELETE FROM users WHERE status = 'deleted' RETURNING *) INSERT INTO users_archive SELECT * FROM moved_rows",

            // 批量插入
            "WITH data AS (SELECT * FROM (VALUES (1,'a'), (2,'b'), (3,'c')) AS t(id,name)) INSERT INTO users SELECT * FROM data",

            // WITH + REPLACE
            "WITH new_data AS (SELECT 1 as id, 'test' as name) REPLACE INTO users SELECT * FROM new_data"
        };

        for (String sql : writeSQLs) {
            SQLType type = SQLParserUtil.getSQLType(sql);
            boolean isWrite = SQLParserUtil.isWriteOperation(sql);
            System.out.printf("%-80s -> %s (%s)\n", truncate(sql, 78), type, isWrite ? "✓" : "✗");
        }
        System.out.println();
    }

    /** 测试复杂场景 */
    private static void testComplexScenarios() {
        System.out.println("【复杂场景测试】");

        // 混合场景数组
        Object[][] scenarios = {
            // 场景名称, SQL, 期望类型
            {
                "递归 CTE 生成数字序列",
                "WITH RECURSIVE numbers(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM numbers WHERE n < 100) SELECT * FROM numbers",
                SQLType.READ
            },
            {
                "递归 CTE 获取所有下级",
                "WITH RECURSIVE subordinates AS (SELECT id, name, manager_id FROM employees WHERE manager_id IS NULL UNION ALL SELECT e.id, e.name, e.manager_id FROM employees e JOIN subordinates s ON e.manager_id = s.id) SELECT * FROM subordinates",
                SQLType.READ
            },
            {
                "多个 CTE 相互引用",
                "WITH cte1 AS (SELECT id, name FROM users), cte2 AS (SELECT user_id, amount FROM orders), cte3 AS (SELECT cte1.name, SUM(cte2.amount) as total FROM cte1 JOIN cte2 ON cte1.id = cte2.user_id GROUP BY cte1.name) SELECT * FROM cte3 ORDER BY total DESC",
                SQLType.READ
            },
            {
                "CTE + 窗口函数",
                "WITH ranked AS (SELECT id, name, points, ROW_NUMBER() OVER (ORDER BY points DESC) as rank FROM users) SELECT * FROM ranked WHERE rank <= 10",
                SQLType.READ
            },
            {
                "CTE + 数据迁移",
                "WITH archive AS (DELETE FROM logs WHERE created_at < '2024-01-01' RETURNING *) INSERT INTO logs_archive SELECT * FROM archive",
                SQLType.WRITE
            },
            {
                "CTE + 批量更新",
                "WITH to_update AS (SELECT id, status FROM users WHERE status = 'pending' LIMIT 100) UPDATE users SET status = 'processed' WHERE id IN (SELECT id FROM to_update)",
                SQLType.WRITE
            },
            {
                "CTE + 软删除",
                "WITH to_delete AS (SELECT id FROM products WHERE stock = 0) UPDATE products SET deleted_at = NOW() WHERE id IN (SELECT id FROM to_delete)",
                SQLType.WRITE
            }
        };

        for (Object[] scenario : scenarios) {
            String name = (String) scenario[0];
            String sql = (String) scenario[1];
            SQLType expected = (SQLType) scenario[2];
            SQLType actual = SQLParserUtil.getSQLType(sql);

            boolean passed = actual == expected;
            System.out.printf(
                    "%-35s -> 期望: %s, 实际: %s %s\n", name, expected, actual, passed ? "✓" : "✗");
            if (!passed) {
                System.out.println("  SQL: " + truncate(sql, 70));
            }
        }
        System.out.println();
    }

    /** 测试边界情况 */
    private static void testEdgeCases() {
        System.out.println("【边界情况测试】");

        String[] edgeCases = {
            // 空 WITH（不应该出现，但测试健壮性）
            "WITH",
            "WITH ",
            "WITH RECURSIVE",

            // 不完整的 WITH
            "WITH temp AS (SELECT",
            "WITH temp AS () SELECT * FROM temp",

            // 带注释的 WITH
            "/* comment */ WITH temp AS (SELECT * FROM users) SELECT * FROM temp",
            "-- comment\nWITH temp AS (SELECT * FROM users) SELECT * FROM temp",

            // WITH 中包含子查询
            "WITH filtered AS (SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)) SELECT * FROM filtered",

            // 大小写混合
            "with temp as (select * from users) select * from temp",
            "WiTh ReCuRsIvE cte AS (SELECT 1) SELECT * FROM cte",

            // 带换行和空格
            "WITH\n\ttemp AS (\n\t\tSELECT * FROM users\n\t)\nSELECT * FROM temp",

            // PostgreSQL 特有语法
            "WITH data AS (SELECT 'test'::varchar as name) SELECT * FROM data",
            "WITH data AS (SELECT 1::int as id) INSERT INTO users (id) SELECT id FROM data"
        };

        for (String sql : edgeCases) {
            SQLType type = SQLParserUtil.getSQLType(sql);
            System.out.printf("%-60s -> %s\n", truncate(sql, 58), type);
        }
        System.out.println();
    }

    /** 截断过长的字符串用于显示 */
    private static String truncate(String str, int maxLength) {
        if (str == null) {
            return "null";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}
