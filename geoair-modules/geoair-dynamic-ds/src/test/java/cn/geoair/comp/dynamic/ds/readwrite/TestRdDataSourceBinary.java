package cn.geoair.comp.dynamic.ds.readwrite;

import static org.junit.jupiter.api.Assertions.*;

import cn.geoair.comp.dynamic.ds.utils.DataSourceDruidFastCreate;

import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.*;
import java.util.Arrays;
import java.util.Map;

/**
 * 读写分离数据源测试用例 - 包含二进制字段测试
 *
 * @author 张俊
 * @date Created in 2026/6/26
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRdDataSourceBinary {

    private GirReadWriteDataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    private static final String TEST_TABLE = "test_user_binary";

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
                        GirGroupSource.builder().dataSources(read.toDataSource()).build());

        jdbcTemplate = new JdbcTemplate(dataSource);

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
        } catch (Exception e) {
            // 表可能不存在，忽略
        }
    }

    /** 初始化数据库 - DDL */
    private void initDatabase() {
        try {
            // 创建包含二进制字段的表
            String createTable =
                    "CREATE TABLE IF NOT EXISTS "
                            + TEST_TABLE
                            + " ("
                            + "id SERIAL PRIMARY KEY, "
                            + "username VARCHAR(50) NOT NULL UNIQUE, "
                            + "email VARCHAR(100) NOT NULL, "
                            + "age INTEGER, "
                            + "avatar BYTEA, "
                            + // 二进制字段（PostgreSQL）
                            "file_data BYTEA, "
                            + // 二进制字段（PostgreSQL）
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                            + ")";
            jdbcTemplate.execute(createTable);

            // 创建索引
            String createIndex =
                    "CREATE INDEX IF NOT EXISTS idx_username_binary ON "
                            + TEST_TABLE
                            + " (username)";
            jdbcTemplate.execute(createIndex);

            System.out.println("✅ 数据库初始化成功（包含二进制字段）");
        } catch (Exception e) {
            System.err.println("❌ 数据库初始化失败: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /** 清理数据库 */
    private void dropTables() {
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + TEST_TABLE + " CASCADE");
            System.out.println("✅ 清理测试表成功");
        } catch (Exception e) {
            System.err.println("⚠️ 清理测试表失败: " + e.getMessage());
        }
    }

    // ==================== 二进制字段测试用例 ====================

    /** 测试1: 使用 setBytes 插入二进制数据 */
    @Test
    @DisplayName("测试 setBytes - 插入二进制数据")
    void testSetBytesInsert() throws SQLException {
        String sql =
                "INSERT INTO "
                        + TEST_TABLE
                        + " (username, email, age, avatar, file_data) VALUES (?, ?, ?, ?, ?)";

        // 准备二进制数据
        byte[] avatarData = createTestImageData(100, 100);
        byte[] fileData = createTestFileData(1024);

        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        try {
            ps.setString(1, "setbytes_user");
            ps.setString(2, "setbytes@example.com");
            ps.setInt(3, 25);
            ps.setBytes(4, avatarData); // 使用 setBytes
            ps.setBytes(5, fileData); // 使用 setBytes

            int affected = ps.executeUpdate();
            assertEquals(1, affected, "应该影响1行");

            // 获取生成的主键
            ResultSet rs = ps.getGeneratedKeys();
            assertTrue(rs.next(), "应该生成主键");
            Long id = rs.getLong(1);
            assertNotNull(id, "主键不应该为空");

            System.out.println(
                    "✅ setBytes插入测试通过，生成ID: "
                            + id
                            + ", avatar大小: "
                            + avatarData.length
                            + ", file大小: "
                            + fileData.length);
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试2: 使用 setObject 插入二进制数据 */
    @Test
    @DisplayName("测试 setObject - 插入二进制数据")
    void testSetObjectInsert() throws SQLException {
        String sql =
                "INSERT INTO "
                        + TEST_TABLE
                        + " (username, email, age, avatar, file_data) VALUES (?, ?, ?, ?, ?)";

        // 准备二进制数据
        byte[] avatarData = createTestImageData(200, 200);
        byte[] fileData = createTestFileData(2048);

        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        try {
            ps.setString(1, "setobject_user");
            ps.setString(2, "setobject@example.com");
            ps.setInt(3, 30);
            ps.setObject(4, avatarData); // 使用 setObject
            ps.setObject(5, fileData); // 使用 setObject

            int affected = ps.executeUpdate();
            assertEquals(1, affected, "应该影响1行");

            // 获取生成的主键
            ResultSet rs = ps.getGeneratedKeys();
            assertTrue(rs.next(), "应该生成主键");
            Long id = rs.getLong(1);
            assertNotNull(id, "主键不应该为空");

            System.out.println(
                    "✅ setObject插入测试通过，生成ID: "
                            + id
                            + ", avatar大小: "
                            + avatarData.length
                            + ", file大小: "
                            + fileData.length);
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试3: 使用 setBytes 读取二进制数据 */
    @Test
    @DisplayName("测试 setBytes - 读取二进制数据")
    void testSetBytesRead() throws SQLException {
        // 先插入数据
        byte[] originalData = createTestImageData(150, 150);
        insertUserWithBinary("readbytes_user", "readbytes@example.com", 28, originalData, null);

        // 读取数据
        String sql = "SELECT id, username, avatar FROM " + TEST_TABLE + " WHERE username = ?";
        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, "readbytes_user");

            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "应该有查询结果");

            // 使用 getBytes 读取
            byte[] readData = rs.getBytes("avatar");
            assertNotNull(readData, "读取的数据不应该为null");
            assertEquals(originalData.length, readData.length, "数据长度应该一致");
            assertArrayEquals(originalData, readData, "数据内容应该一致");

            System.out.println("✅ setBytes读取测试通过，数据大小: " + readData.length);
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试4: 使用 setObject 读取二进制数据 */
    @Test
    @DisplayName("测试 setObject - 读取二进制数据")
    void testSetObjectRead() throws SQLException {
        // 先插入数据
        byte[] originalData = createTestFileData(3072);
        insertUserWithBinary("readobject_user", "readobject@example.com", 32, null, originalData);

        // 读取数据
        String sql = "SELECT id, username, file_data FROM " + TEST_TABLE + " WHERE username = ?";
        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, "readobject_user");

            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "应该有查询结果");

            // 使用 getObject 读取
            Object readObject = rs.getObject("file_data");
            assertNotNull(readObject, "读取的对象不应该为null");
            assertTrue(readObject instanceof byte[], "应该是byte[]类型");

            byte[] readData = (byte[]) readObject;
            assertEquals(originalData.length, readData.length, "数据长度应该一致");
            assertArrayEquals(originalData, readData, "数据内容应该一致");

            System.out.println("✅ setObject读取测试通过，数据大小: " + readData.length);
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试5: 混合使用 setBytes 和 setObject */
    @Test
    @DisplayName("测试混合使用 setBytes 和 setObject")
    void testMixedSetBytesAndObject() throws SQLException {
        String sql =
                "INSERT INTO "
                        + TEST_TABLE
                        + " (username, email, age, avatar, file_data) VALUES (?, ?, ?, ?, ?)";

        byte[] avatarData = createTestImageData(64, 64);
        byte[] fileData = createTestFileData(512);

        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        try {
            ps.setString(1, "mixed_user");
            ps.setString(2, "mixed@example.com");
            ps.setInt(3, 27);
            ps.setBytes(4, avatarData); // 使用 setBytes
            ps.setObject(5, fileData); // 使用 setObject

            int affected = ps.executeUpdate();
            assertEquals(1, affected);

            // 读取验证
            ResultSet rs = ps.getGeneratedKeys();
            assertTrue(rs.next());
            Long id = rs.getLong(1);

            // 查询验证
            String querySql = "SELECT avatar, file_data FROM " + TEST_TABLE + " WHERE id = ?";
            PreparedStatement queryPs = conn.prepareStatement(querySql);
            try {
                queryPs.setLong(1, id);
                ResultSet queryRs = queryPs.executeQuery();
                assertTrue(queryRs.next());

                byte[] readAvatar = queryRs.getBytes("avatar");
                byte[] readFile = queryRs.getBytes("file_data");

                assertArrayEquals(avatarData, readAvatar, "Avatar数据应该一致");
                assertArrayEquals(fileData, readFile, "File数据应该一致");

                System.out.println("✅ 混合使用测试通过，ID: " + id);
            } finally {
                queryPs.close();
            }
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试6: 空二进制数据（NULL） */
    @Test
    @DisplayName("测试空二进制数据（NULL）")
    void testNullBinaryData() throws SQLException {
        String sql =
                "INSERT INTO "
                        + TEST_TABLE
                        + " (username, email, age, avatar, file_data) VALUES (?, ?, ?, ?, ?)";

        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, "nullbinary_user");
            ps.setString(2, "nullbinary@example.com");
            ps.setInt(3, 25);
            ps.setNull(4, Types.BINARY); // 设置NULL
            ps.setNull(5, Types.BINARY); // 设置NULL

            int affected = ps.executeUpdate();
            assertEquals(1, affected);

            // 验证
            String querySql =
                    "SELECT avatar, file_data FROM "
                            + TEST_TABLE
                            + " WHERE username = 'nullbinary_user'";
            Map<String, Object> result = jdbcTemplate.queryForMap(querySql);
            assertNull(result.get("avatar"), "avatar应该为NULL");
            assertNull(result.get("file_data"), "file_data应该为NULL");

            System.out.println("✅ 空二进制数据测试通过");
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试7: 大数据量二进制数据 */
    @Test
    @DisplayName("测试大数据量二进制数据")
    void testLargeBinaryData() throws SQLException {
        String sql =
                "INSERT INTO "
                        + TEST_TABLE
                        + " (username, email, age, avatar, file_data) VALUES (?, ?, ?, ?, ?)";

        // 创建1MB的数据
        int size = 1024 * 1024;
        byte[] largeData = new byte[size];
        for (int i = 0; i < size; i++) {
            largeData[i] = (byte) (i % 256);
        }

        long startTime = System.currentTimeMillis();

        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, "largebinary_user");
            ps.setString(2, "largebinary@example.com");
            ps.setInt(3, 25);
            ps.setBytes(4, largeData); // 使用 setBytes
            ps.setObject(5, largeData); // 使用 setObject

            int affected = ps.executeUpdate();
            assertEquals(1, affected);

            long endTime = System.currentTimeMillis();
            System.out.println(
                    "✅ 大数据量测试通过，数据大小: "
                            + size
                            + " bytes ("
                            + (size / 1024)
                            + " KB), 耗时: "
                            + (endTime - startTime)
                            + "ms");
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试8: 批量插入二进制数据（使用 setBytes） */
    @Test
    @DisplayName("测试批量插入二进制数据 - setBytes")
    void testBatchInsertWithSetBytes() throws SQLException {
        String sql =
                "INSERT INTO " + TEST_TABLE + " (username, email, age, avatar) VALUES (?, ?, ?, ?)";

        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            for (int i = 1; i <= 10; i++) {
                byte[] avatarData = createTestImageData(50 + i, 50 + i);
                ps.setString(1, "batch_bytes_" + i);
                ps.setString(2, "batch" + i + "@example.com");
                ps.setInt(3, 20 + i);
                ps.setBytes(4, avatarData); // 使用 setBytes
                ps.addBatch();
            }

            int[] results = ps.executeBatch();
            assertEquals(10, results.length);
            for (int result : results) {
                assertEquals(1, result);
            }

            // 验证
            String countSql =
                    "SELECT COUNT(*) FROM " + TEST_TABLE + " WHERE username LIKE 'batch_bytes_%'";
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
            assertEquals(10, count);

            System.out.println("✅ 批量插入(setBytes)测试通过");
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试9: 批量插入二进制数据（使用 setObject） */
    @Test
    @DisplayName("测试批量插入二进制数据 - setObject")
    void testBatchInsertWithSetObject() throws SQLException {
        String sql =
                "INSERT INTO "
                        + TEST_TABLE
                        + " (username, email, age, file_data) VALUES (?, ?, ?, ?)";

        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            for (int i = 1; i <= 10; i++) {
                byte[] fileData = createTestFileData(256 * i);
                ps.setString(1, "batch_object_" + i);
                ps.setString(2, "batch" + i + "@example.com");
                ps.setInt(3, 20 + i);
                ps.setObject(4, fileData); // 使用 setObject
                ps.addBatch();
            }

            int[] results = ps.executeBatch();
            assertEquals(10, results.length);
            for (int result : results) {
                assertEquals(1, result);
            }

            // 验证
            String countSql =
                    "SELECT COUNT(*) FROM " + TEST_TABLE + " WHERE username LIKE 'batch_object_%'";
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
            assertEquals(10, count);

            System.out.println("✅ 批量插入(setObject)测试通过");
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试10: 更新二进制数据（setBytes vs setObject 对比） */
    @Test
    @DisplayName("测试更新二进制数据 - setBytes vs setObject")
    void testUpdateBinaryData() throws SQLException {
        // 先插入初始数据
        byte[] initialAvatar = createTestImageData(100, 100);
        insertUserWithBinary("update_user", "update@example.com", 25, initialAvatar, null);

        // 使用 setBytes 更新
        byte[] newAvatar1 = createTestImageData(200, 200);
        String sql1 = "UPDATE " + TEST_TABLE + " SET avatar = ? WHERE username = ?";
        Connection conn = dataSource.getConnection();
        PreparedStatement ps1 = conn.prepareStatement(sql1);
        try {
            ps1.setBytes(1, newAvatar1);
            ps1.setString(2, "update_user");
            int affected = ps1.executeUpdate();
            assertEquals(1, affected);

            // 验证
            String querySql =
                    "SELECT avatar FROM " + TEST_TABLE + " WHERE username = 'update_user'";
            byte[] readData = jdbcTemplate.queryForObject(querySql, byte[].class);
            assertArrayEquals(newAvatar1, readData);
            System.out.println("✅ setBytes更新测试通过");
        } finally {
            ps1.close();
            conn.close();
        }

        // 使用 setObject 更新
        byte[] newAvatar2 = createTestImageData(300, 300);
        String sql2 = "UPDATE " + TEST_TABLE + " SET avatar = ? WHERE username = ?";
        Connection conn2 = dataSource.getConnection();
        PreparedStatement ps2 = conn2.prepareStatement(sql2);
        try {
            ps2.setObject(1, newAvatar2);
            ps2.setString(2, "update_user");
            int affected = ps2.executeUpdate();
            assertEquals(1, affected);

            // 验证
            String querySql =
                    "SELECT avatar FROM " + TEST_TABLE + " WHERE username = 'update_user'";
            byte[] readData = jdbcTemplate.queryForObject(querySql, byte[].class);
            assertArrayEquals(newAvatar2, readData);
            System.out.println("✅ setObject更新测试通过");
        } finally {
            ps2.close();
            conn2.close();
        }
    }

    /** 测试11: 使用 setObject 指定 SQL 类型 */
    @Test
    @DisplayName("测试 setObject 指定 SQL 类型")
    void testSetObjectWithSqlType() throws SQLException {
        String sql =
                "INSERT INTO " + TEST_TABLE + " (username, email, age, avatar) VALUES (?, ?, ?, ?)";

        byte[] avatarData = createTestImageData(128, 128);

        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, "setobject_type_user");
            ps.setString(2, "setobject_type@example.com");
            ps.setInt(3, 25);
            ps.setObject(4, avatarData, Types.BINARY); // 指定 SQL 类型

            int affected = ps.executeUpdate();
            assertEquals(1, affected);

            // 验证
            String querySql =
                    "SELECT avatar FROM " + TEST_TABLE + " WHERE username = 'setobject_type_user'";
            byte[] readData = jdbcTemplate.queryForObject(querySql, byte[].class);
            assertArrayEquals(avatarData, readData);

            System.out.println("✅ setObject指定类型测试通过");
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 测试12: 二进制数据完整性验证 */
    @Test
    @DisplayName("测试二进制数据完整性")
    void testBinaryDataIntegrity() throws SQLException {
        String sql =
                "INSERT INTO "
                        + TEST_TABLE
                        + " (username, email, age, avatar, file_data) VALUES (?, ?, ?, ?, ?)";

        // 创建特定的测试数据
        byte[] avatarData = createTestImageData(80, 80);
        byte[] fileData = createTestFileData(1536);

        // 修改一些特定字节以便验证
        avatarData[0] = (byte) 0x01;
        avatarData[avatarData.length - 1] = (byte) 0xFF;
        fileData[100] = (byte) 0x5A;
        fileData[200] = (byte) 0xA5;

        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, "integrity_user");
            ps.setString(2, "integrity@example.com");
            ps.setInt(3, 25);
            ps.setBytes(4, avatarData);
            ps.setObject(5, fileData);

            ps.executeUpdate();

            // 读取并验证完整性
            String querySql =
                    "SELECT avatar, file_data FROM "
                            + TEST_TABLE
                            + " WHERE username = 'integrity_user'";
            PreparedStatement queryPs = conn.prepareStatement(querySql);
            try {
                ResultSet rs = queryPs.executeQuery();
                assertTrue(rs.next());

                byte[] readAvatar = rs.getBytes("avatar");
                byte[] readFile = rs.getBytes("file_data");

                // 逐字节验证
                assertArrayEquals(avatarData, readAvatar, "Avatar数据不完整");
                assertArrayEquals(fileData, readFile, "File数据不完整");

                // 验证特定字节
                assertEquals(0x01, readAvatar[0]);
                assertEquals(0xFF, readAvatar[readAvatar.length - 1]);
                assertEquals(0x5A, readFile[100]);
                assertEquals(0xA5, readFile[200]);

                System.out.println("✅ 数据完整性验证通过");
            } finally {
                queryPs.close();
            }
        } finally {
            ps.close();
            conn.close();
        }
    }

    // ==================== 辅助方法 ====================

    /** 创建模拟的图片数据 */
    private byte[] createTestImageData(int width, int height) {
        // 模拟图片数据：RGB 数据
        int size = width * height * 3;
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (i % 256);
        }
        return data;
    }

    /** 创建模拟的文件数据 */
    private byte[] createTestFileData(int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (i % 256);
        }
        return data;
    }

    /** 插入包含二进制数据的用户 */
    private void insertUserWithBinary(
            String username, String email, int age, byte[] avatar, byte[] fileData)
            throws SQLException {
        String sql =
                "INSERT INTO "
                        + TEST_TABLE
                        + " (username, email, age, avatar, file_data) VALUES (?, ?, ?, ?, ?)";
        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setInt(3, age);
            if (avatar != null) {
                ps.setBytes(4, avatar);
            } else {
                ps.setNull(4, Types.BINARY);
            }
            if (fileData != null) {
                ps.setBytes(5, fileData);
            } else {
                ps.setNull(5, Types.BINARY);
            }
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** 打印二进制数据信息（用于调试） */
    private void printBinaryInfo(String label, byte[] data) {
        if (data == null) {
            System.out.println(label + ": null");
            return;
        }
        System.out.println(
                label
                        + ": size="
                        + data.length
                        + " bytes, first10="
                        + Arrays.toString(Arrays.copyOf(data, Math.min(10, data.length))));
    }

    public static void main(String[] args) {
        // 可以运行特定测试
        System.out.println("请使用 JUnit 运行测试");
    }
}
