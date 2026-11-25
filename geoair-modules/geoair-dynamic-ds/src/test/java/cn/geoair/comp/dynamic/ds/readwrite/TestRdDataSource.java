package cn.geoair.comp.dynamic.ds.readwrite;

import static org.junit.jupiter.api.Assertions.*;

import cn.geoair.base.log.GemLogLevel;
import cn.geoair.comp.dynamic.ds.readwrite.log.RdLog;
import cn.geoair.comp.dynamic.ds.utils.DataSourceDruidFastCreate;
import cn.hutool.core.thread.ThreadUtil;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 读写分离数据源测试用例 - JDK 8 版本
 *
 * @author 张俊
 * @date Created in 2026/6/26
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRdDataSource {

    private GirReadWriteDataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    private static final String TEST_TABLE = "test_user";
    private static final String TEST_TABLE_BATCH = "test_batch_log";

    @BeforeAll
    void setUp() {
        // 初始化读写分离数据源
        DataSourceDruidFastCreate master = new DataSourceDruidFastCreate();
        master.setUrl("jdbc:postgresql://192.168.0.110:5432/test_read_write");
        master.setUsername("postgres");
        master.setPassword("tcsd2019");
        master.setQueryTimeout(12000);

        DataSourceDruidFastCreate read = new DataSourceDruidFastCreate();
        read.setUrl("jdbc:postgresql://192.168.0.104:5432/test_read_write");
        read.setUsername("postgres");
        read.setPassword("tcsd2019");
        read.setQueryTimeout(12000);

        dataSource =
                GirReadWriteDataSourceBuilder.build(
                        master.toDataSource(),
                        GirGroupSource.builder()
                                .dataSources(read.toDataSource())
                                .groupName("104")
                                .build());

        jdbcTemplate = new JdbcTemplate(dataSource);
        RdLog.minLogLevel = GemLogLevel.TRACE;
        // 执行DDL初始化
        initDatabase();
    }

    @AfterAll
    void tearDown() {
        // 清理测试数据
        dropTables();
    }

    @BeforeEach
    void setUpEach() {
        // 每个测试前清空数据
        try {
            jdbcTemplate.execute("TRUNCATE TABLE " + TEST_TABLE + " RESTART IDENTITY");
            jdbcTemplate.execute("TRUNCATE TABLE " + TEST_TABLE_BATCH + " RESTART IDENTITY");
        } catch (Exception e) {
            // 表可能不存在，忽略
        }
    }

    /** 初始化数据库 - DDL */
    private void initDatabase() {
        try {
            // 创建主表
            String createUserTable =
                    "CREATE TABLE IF NOT EXISTS "
                            + TEST_TABLE
                            + " ("
                            + "id SERIAL PRIMARY KEY, "
                            + "username VARCHAR(50) NOT NULL UNIQUE, "
                            + "email VARCHAR(100) NOT NULL, "
                            + "age INTEGER, "
                            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                            + ")";
            jdbcTemplate.execute(createUserTable);

            // 创建批处理日志表
            String createBatchLogTable =
                    "CREATE TABLE IF NOT EXISTS "
                            + TEST_TABLE_BATCH
                            + " ("
                            + "id SERIAL PRIMARY KEY, "
                            + "batch_id VARCHAR(50) NOT NULL, "
                            + "operation VARCHAR(20) NOT NULL, "
                            + "record_count INTEGER, "
                            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                            + ")";
            jdbcTemplate.execute(createBatchLogTable);

            // 创建索引
            String createIndex =
                    "CREATE INDEX IF NOT EXISTS idx_username ON " + TEST_TABLE + " (username)";
            jdbcTemplate.execute(createIndex);

            // 创建注释
            String createComment = "COMMENT ON TABLE " + TEST_TABLE + " IS '测试用户表'";
            jdbcTemplate.execute(createComment);

            System.out.println("✅ 数据库初始化成功");
        } catch (Exception e) {
            System.err.println("❌ 数据库初始化失败: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /** 清理数据库 */
    private void dropTables() {
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + TEST_TABLE + " CASCADE");
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + TEST_TABLE_BATCH + " CASCADE");
            System.out.println("✅ 清理测试表成功");
        } catch (Exception e) {
            System.err.println("⚠️ 清理测试表失败: " + e.getMessage());
        }
    }

    // ==================== 测试用例 ====================

    /** 测试1: DDL - 创建表和索引 */
    @Test
    @DisplayName("测试DDL - 创建表结构")
    void testDDL() {
        // 验证表是否存在
        String checkTableSql =
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_name = '"
                        + TEST_TABLE
                        + "'";

        Integer count = jdbcTemplate.queryForObject(checkTableSql, Integer.class);
        assertEquals(1, count, "表应该存在");

        // 验证索引是否存在
        String checkIndexSql =
                "SELECT COUNT(*) FROM pg_indexes "
                        + "WHERE tablename = '"
                        + TEST_TABLE
                        + "' AND indexname = 'idx_username'";

        Integer indexCount = jdbcTemplate.queryForObject(checkIndexSql, Integer.class);
        assertEquals(1, indexCount, "索引应该存在");

        System.out.println("✅ DDL测试通过");
    }

    /** 测试2: INSERT - 插入单条数据 */
    @Test
    @DisplayName("测试INSERT - 插入单条数据")
    void testInsert() throws SQLException {
        String sql = "INSERT INTO " + TEST_TABLE + " (username, email, age) VALUES (?, ?, ?)";

        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        try {
            ps.setString(1, "test_user_1");
            ps.setString(2, "test1@example.com");
            ps.setInt(3, 25);

            int affected = ps.executeUpdate();
            assertEquals(1, affected, "应该影响1行");

            // 获取生成的主键
            ResultSet rs = ps.getGeneratedKeys();
            assertTrue(rs.next(), "应该生成主键");
            Long id = rs.getLong(1);
            assertNotNull(id, "主键不应该为空");

            System.out.println("✅ INSERT测试通过，生成ID: " + id);
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试3: INSERT - 批量插入数据 */
    @Test
    @DisplayName("测试INSERT - 批量插入数据")
    void testBatchInsert() throws SQLException {
        String sql = "INSERT INTO " + TEST_TABLE + " (username, email, age) VALUES (?, ?, ?)";

        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            // 添加10条数据
            for (int i = 1; i <= 10; i++) {
                ps.setString(1, "batch_user_" + i);
                ps.setString(2, "batch" + i + "@example.com");
                ps.setInt(3, 20 + i);
                ps.addBatch();
            }

            int[] results = ps.executeBatch();
            assertEquals(10, results.length, "应该执行10条");
            for (int result : results) {
                assertEquals(1, result, "每条应该影响1行");
            }

            // 验证插入数量
            String countSql = "SELECT COUNT(*) FROM " + TEST_TABLE;
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
            assertEquals(10, count, "应该插入10条记录");

            System.out.println("✅ 批量INSERT测试通过，插入 " + count + " 条记录");
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试4: UPDATE - 更新数据 */
    @Test
    @DisplayName("测试UPDATE - 更新数据")
    void testUpdate() throws SQLException {
        // 先插入一条数据
        insertTestUser("update_user", "update@example.com", 30);

        // 更新数据
        String sql = "UPDATE " + TEST_TABLE + " SET age = ?, email = ? WHERE username = ?";
        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, 35);
            ps.setString(2, "updated@example.com");
            ps.setString(3, "update_user");

            int affected = ps.executeUpdate();
            assertEquals(1, affected, "应该影响1行");

            // 验证更新结果
            String querySql =
                    "SELECT age, email FROM " + TEST_TABLE + " WHERE username = 'update_user'";
            Map<String, Object> result = jdbcTemplate.queryForMap(querySql);
            assertEquals(35, result.get("age"));
            assertEquals("updated@example.com", result.get("email"));

            System.out.println("✅ UPDATE测试通过");
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试5: DELETE - 删除数据 */
    @Test
    @DisplayName("测试DELETE - 删除数据")
    void testDelete() throws SQLException {
        // 插入测试数据
        insertTestUser("delete_user", "delete@example.com", 25);

        // 删除数据
        String sql = "DELETE FROM " + TEST_TABLE + " WHERE username = ?";
        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, "delete_user");

            int affected = ps.executeUpdate();
            assertEquals(1, affected, "应该影响1行");

            // 验证删除结果
            String countSql =
                    "SELECT COUNT(*) FROM " + TEST_TABLE + " WHERE username = 'delete_user'";
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
            assertEquals(0, count, "记录应该被删除");

            System.out.println("✅ DELETE测试通过");
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试6: SELECT - 查询数据（读路由） */
    @Test
    @DisplayName("测试SELECT - 查询数据")
    void testSelect() throws SQLException {
        // 插入测试数据
        insertTestUser("select_user", "select@example.com", 28);

        // 查询数据
        String sql = "SELECT * FROM " + TEST_TABLE + " WHERE username = ?";
        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, "select_user");

            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "应该有查询结果");
            assertEquals("select_user", rs.getString("username"));
            assertEquals("select@example.com", rs.getString("email"));
            assertEquals(28, rs.getInt("age"));

            System.out.println("✅ SELECT测试通过");
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试7: 批处理 - 混合操作 */
    @Test
    @DisplayName("测试批处理 - 混合操作")
    void testMixedBatch() throws SQLException {
        String insertSql =
                "INSERT INTO "
                        + TEST_TABLE_BATCH
                        + " (batch_id, operation, record_count) VALUES (?, ?, ?)";

        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(insertSql);
        try {
            // 添加多条记录
            for (int i = 1; i <= 5; i++) {
                ps.setString(1, "BATCH_" + i);
                ps.setString(2, i % 2 == 0 ? "UPDATE" : "INSERT");
                ps.setInt(3, i * 10);
                ps.addBatch();
            }

            int[] results = ps.executeBatch();
            assertEquals(5, results.length);

            // 验证
            String countSql = "SELECT COUNT(*) FROM " + TEST_TABLE_BATCH;
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
            assertEquals(5, count);

            System.out.println("✅ 混合批处理测试通过");
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试8: 事务 - 事务提交和回滚 */
    @Test
    @DisplayName("测试事务 - 提交")
    void testTransaction() throws SQLException {
        Connection conn = dataSource.getConnection();
        conn.setAutoCommit(false);

        try {
            // 插入数据
            String sql1 = "INSERT INTO " + TEST_TABLE + " (username, email, age) VALUES (?, ?, ?)";
            PreparedStatement ps1 = conn.prepareStatement(sql1);
            try {
                ps1.setString(1, "tx_user_1");
                ps1.setString(2, "tx1@example.com");
                ps1.setInt(3, 25);
                ps1.executeUpdate();
            } finally {
                ps1.close();
            }

            // 插入另一条数据
            String sql2 = "INSERT INTO " + TEST_TABLE + " (username, email, age) VALUES (?, ?, ?)";
            PreparedStatement ps2 = conn.prepareStatement(sql2);
            try {
                ps2.setString(1, "tx_user_2");
                ps2.setString(2, "tx2@example.com");
                ps2.setInt(3, 30);
                ps2.executeUpdate();
            } finally {
                ps2.close();
            }

            conn.commit();
            ThreadUtil.sleep(1);
            // 验证
            String countSql = "SELECT COUNT(*) FROM " + TEST_TABLE + " WHERE username LIKE 'tx_%'";
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
            assertEquals(2, count);

            System.out.println("✅ 事务提交测试通过");

        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.close();
        }
    }

    /** 测试9: 事务 - 回滚 */
    @Test
    @DisplayName("测试事务 - 回滚")
    void testTransactionRollback() throws SQLException {
        Connection conn = dataSource.getConnection();
        conn.setAutoCommit(false);

        try {
            // 插入数据
            String sql = "INSERT INTO " + TEST_TABLE + " (username, email, age) VALUES (?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            try {
                ps.setString(1, "rollback_user");
                ps.setString(2, "rollback@example.com");
                ps.setInt(3, 25);
                ps.executeUpdate();
            } finally {
                ps.close();
            }

            // 故意抛出异常触发回滚
            throw new RuntimeException("模拟异常");

        } catch (Exception e) {
            conn.rollback();

            // 验证数据未插入
            String countSql =
                    "SELECT COUNT(*) FROM " + TEST_TABLE + " WHERE username = 'rollback_user'";
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
            assertEquals(0, count, "数据应该被回滚");

            System.out.println("✅ 事务回滚测试通过");
        } finally {
            conn.close();
        }
    }

    /** 测试10: 并发 - 多线程读写测试 */
    @Test
    @DisplayName("测试并发 - 多线程读写")
    void testConcurrentReadWrite() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                for (int j = 0; j < operationsPerThread; j++) {
                                    // 50%读，50%写
                                    if (j % 2 == 0) {
                                        // 读操作
                                        String sql = "SELECT COUNT(*) FROM " + TEST_TABLE;
                                        jdbcTemplate.queryForObject(sql, Integer.class);
                                    } else {
                                        // 写操作
                                        String sql =
                                                "INSERT INTO "
                                                        + TEST_TABLE
                                                        + " (username, email, age) VALUES (?, ?, ?)";
                                        jdbcTemplate.update(
                                                sql,
                                                "concurrent_" + threadId + "_" + j,
                                                "concurrent" + threadId + "@example.com",
                                                20 + j % 50);
                                    }
                                    successCount.incrementAndGet();
                                }
                            } catch (Exception e) {
                                failCount.incrementAndGet();
                                System.err.println("线程 " + threadId + " 执行失败: " + e.getMessage());
                            } finally {
                                latch.countDown();
                            }
                        }
                    });
        }

        latch.await();
        executor.shutdown();

        assertEquals(0, failCount.get(), "并发执行不应该有失败");
        System.out.println("✅ 并发测试通过，成功: " + successCount.get() + "，失败: " + failCount.get());
    }

    /** 测试11: 预编译SQL缓存测试 */
    @Test
    @DisplayName("测试预编译SQL缓存")
    void testPreparedStatementCache() throws SQLException {
        String sql = "SELECT * FROM " + TEST_TABLE + " WHERE username = ? AND age > ?";

        Connection conn = dataSource.getConnection();
        try {
            // 第一次执行
            PreparedStatement ps1 = conn.prepareStatement(sql);
            try {
                ps1.setString(1, "cache_test_1");
                ps1.setInt(2, 18);
                ps1.executeQuery();
            } finally {
                ps1.close();
            }

            // 第二次执行，应该复用缓存的Statement
            PreparedStatement ps2 = conn.prepareStatement(sql);
            try {
                ps2.setString(1, "cache_test_2");
                ps2.setInt(2, 20);
                ps2.executeQuery();
            } finally {
                ps2.close();
            }
        } finally {
            conn.close();
        }

        System.out.println("✅ 预编译SQL缓存测试通过");
    }

    /** 测试12: 大数据量批量插入 */
    @Test
    @DisplayName("测试大数据量批量插入")
    void testLargeBatchInsert() throws SQLException {
        int batchSize = 1000;
        String sql = "INSERT INTO " + TEST_TABLE + " (username, email, age) VALUES (?, ?, ?)";

        long startTime = System.currentTimeMillis();

        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            for (int i = 1; i <= batchSize; i++) {
                ps.setObject(1, "large_batch_" + i);
                ps.setObject(2, "large" + i + "@example.com");
                ps.setObject(3, 20 + i % 50);
                ps.addBatch();

                // 每100条执行一次
                if (i % 100 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
        } finally {
            ps.close();
            conn.close();
        }

        long endTime = System.currentTimeMillis();
        ThreadUtil.sleep(20);
        // 验证
        String countSql =
                "SELECT COUNT(*) FROM " + TEST_TABLE + " WHERE username LIKE 'large_batch_%'";
        Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
        assertEquals(batchSize, count);

        System.out.println(
                "✅ 大数据量批量插入测试通过，插入 " + batchSize + " 条，耗时: " + (endTime - startTime) + "ms");
    }

    /** 测试13: 复杂查询 - JOIN和聚合 */
    @Test
    @DisplayName("测试复杂查询 - JOIN和聚合")
    void testComplexQuery() throws SQLException {
        // 插入测试数据
        for (int i = 1; i <= 20; i++) {
            insertTestUser("complex_" + i, "complex" + i + "@example.com", 20 + i % 30);
        }

        // 执行复杂查询
        String sql =
                "SELECT "
                        + "COUNT(*) as total, "
                        + "AVG(age) as avg_age, "
                        + "MAX(age) as max_age, "
                        + "MIN(age) as min_age "
                        + "FROM "
                        + TEST_TABLE
                        + " WHERE username LIKE 'complex_%'";

        Map<String, Object> result = jdbcTemplate.queryForMap(sql);
        assertEquals(20L, result.get("total"));
        assertNotNull(result.get("avg_age"));
        assertNotNull(result.get("max_age"));
        assertNotNull(result.get("min_age"));

        System.out.println("✅ 复杂查询测试通过");
    }

    /** 测试14: 批量更新 */
    @Test
    @DisplayName("测试批量更新")
    void testBatchUpdate() throws SQLException {
        // 插入初始数据
        for (int i = 1; i <= 10; i++) {
            insertTestUser("batch_update_" + i, "update" + i + "@example.com", 20);
        }

        // 批量更新
        String sql = "UPDATE " + TEST_TABLE + " SET age = age + 1 WHERE username LIKE ?";
        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, "batch_update_%");
            int affected = ps.executeUpdate();
            assertEquals(10, affected);
        } finally {
            ps.close();
            conn.close();
        }

        // 验证更新结果
        String querySql =
                "SELECT AVG(age) FROM " + TEST_TABLE + " WHERE username LIKE 'batch_update_%'";
        Double avgAge = jdbcTemplate.queryForObject(querySql, Double.class);
        assertEquals(21.0, avgAge, 0.1);

        System.out.println("✅ 批量更新测试通过");
    }

    /** 测试15: 边界值测试 */
    @Test
    @DisplayName("测试边界值")
    void testBoundaryValues() throws SQLException {
        // 测试空字符串
        insertTestUser("", "empty@example.com", 0);

        // 测试特殊字符
        insertTestUser("test@#$%", "special@example.com", 100);

        // 测试NULL值（年龄可以为null）
        String sql = "INSERT INTO " + TEST_TABLE + " (username, email, age) VALUES (?, ?, ?)";
        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, "null_age_user");
            ps.setString(2, "null@example.com");
            ps.setNull(3, Types.INTEGER);
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
        }

        // 验证
        String countSql =
                "SELECT COUNT(*) FROM " + TEST_TABLE + " WHERE username = 'null_age_user'";
        Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
        assertEquals(1, count);

        System.out.println("✅ 边界值测试通过");
    }

    // ==================== 辅助方法 ====================

    private void insertTestUser(String username, String email, int age) throws SQLException {
        String sql = "INSERT INTO " + TEST_TABLE + " (username, email, age) VALUES (?, ?, ?)";
        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setInt(3, age);
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
        }
    }

    private void insertTestUser(String username, String email, Integer age) throws SQLException {
        String sql = "INSERT INTO " + TEST_TABLE + " (username, email, age) VALUES (?, ?, ?)";
        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, username);
            ps.setString(2, email);
            if (age != null) {
                ps.setInt(3, age);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
        }
    }

    public static void main(String[] args) {
        // 可以运行特定测试
        System.out.println("请使用 JUnit 运行测试");
    }
}
