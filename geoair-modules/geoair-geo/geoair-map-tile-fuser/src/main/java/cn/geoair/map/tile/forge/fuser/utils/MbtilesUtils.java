package cn.geoair.map.tile.forge.fuser.utils;

import cn.geoair.comp.dynamic.ds.utils.DataSourceDruidFastCreate;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.sql.*;
import java.util.Properties;

/**
 * MBTiles 工具类
 * <p>
 * 提供 MBTiles 相关的公共方法，包括：
 * - 数据源创建
 * - 数据库初始化
 * - 瓦片 CRUD 操作
 * - 元数据管理
 * </p>
 *
 * @author 张俊
 * @date Created in 2026/6/23 09:09
 */
@Slf4j
public class MbtilesUtils {

    // ==================== SQL 语句常量 ====================

    /**
     * 创建 tiles 表的 SQL
     */
    public static final String CREATE_TILES_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS tiles (" +
                    "  zoom_level INTEGER NOT NULL," +
                    "  tile_column INTEGER NOT NULL," +
                    "  tile_row INTEGER NOT NULL," +
                    "  tile_data BLOB NOT NULL," +
                    "  PRIMARY KEY (zoom_level, tile_column, tile_row)" +
                    ")";

    /**
     * 创建 metadata 表的 SQL
     */
    public static final String CREATE_METADATA_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS metadata (" +
                    "  name TEXT NOT NULL," +
                    "  value TEXT," +
                    "  PRIMARY KEY (name)" +
                    ")";

    /**
     * 创建 tiles 索引的 SQL
     */
    public static final String CREATE_TILES_INDEX_SQL =
            "CREATE INDEX IF NOT EXISTS idx_tiles_zoom ON tiles(zoom_level)";

    /**
     * 查询瓦片的 SQL
     */
    public static final String SELECT_TILE_SQL =
            "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";

    /**
     * 检查瓦片是否存在的 SQL
     */
    public static final String EXISTS_TILE_SQL =
            "SELECT 1 FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";

    /**
     * 插入或替换瓦片的 SQL
     */
    public static final String INSERT_OR_REPLACE_TILE_SQL =
            "INSERT OR REPLACE INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)";

    /**
     * 删除瓦片的 SQL
     */
    public static final String DELETE_TILE_SQL =
            "DELETE FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";

    /**
     * 按层级删除瓦片的 SQL
     */
    public static final String DELETE_BY_ZOOM_SQL =
            "DELETE FROM tiles WHERE zoom_level = ?";

    /**
     * 按层级和列删除瓦片的 SQL
     */
    public static final String DELETE_BY_ZOOM_AND_X_SQL =
            "DELETE FROM tiles WHERE zoom_level = ? AND tile_column = ?";

    /**
     * 清空所有瓦片的 SQL
     */
    public static final String TRUNCATE_TILES_SQL =
            "DELETE FROM tiles";

    /**
     * 统计瓦片总数的 SQL
     */
    public static final String COUNT_TILES_SQL =
            "SELECT COUNT(*) FROM tiles";

    /**
     * 按层级统计瓦片数量的 SQL
     */
    public static final String COUNT_TILES_BY_ZOOM_SQL =
            "SELECT COUNT(*) FROM tiles WHERE zoom_level = ?";

    /**
     * 检查表是否存在的 SQL
     */
    public static final String CHECK_TABLE_EXISTS_SQL =
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?";

    // ==================== 数据源创建方法 ====================

    /**
     * 创建 MBTiles 数据源（可读写）
     *
     * @param dbPath    数据库文件路径
     * @param maxActive 最大连接数
     * @param minIdle   最小空闲连接数
     * @return DruidDataSource
     */
    public static DruidDataSource createDataSource(String dbPath, int maxActive, int minIdle) {
        return createDataSource(dbPath, false, maxActive, minIdle);
    }

    /**
     * 创建 MBTiles 数据源
     *
     * @param dbPath    数据库文件路径
     * @param readOnly  是否只读
     * @param maxActive 最大连接数
     * @param minIdle   最小空闲连接数
     * @return DruidDataSource
     */
    public static DruidDataSource createDataSource(String dbPath, boolean readOnly, int maxActive, int minIdle) {
        DataSourceDruidFastCreate dataSourceDruidFastCreate = new DataSourceDruidFastCreate();
        dataSourceDruidFastCreate.setUrl("jdbc:sqlite:" + dbPath);
        dataSourceDruidFastCreate.setConfigurator(dataSource -> {
            // 连接池大小配置
            dataSource.setMaxActive(maxActive);
            dataSource.setInitialSize(Math.min(minIdle, maxActive));
            dataSource.setMinIdle(minIdle);

            // 连接有效性检测
            dataSource.setValidationQuery("SELECT 1");
            dataSource.setTestWhileIdle(true);
            dataSource.setTimeBetweenEvictionRunsMillis(60000);

            // SQLite 特定配置
            dataSource.setConnectionInitSqls(java.util.Arrays.asList(
                    "PRAGMA journal_mode=WAL",
                    "PRAGMA synchronous=" + (readOnly ? "NORMAL" : "FULL"),
                    "PRAGMA cache_size=10000",
                    "PRAGMA temp_store=MEMORY",
                    "PRAGMA mmap_size=268435456"  // 256MB
            ));

            // 连接属性
            Properties properties = new Properties();
            properties.setProperty("journal_mode", "WAL");
            properties.setProperty("synchronous", readOnly ? "NORMAL" : "FULL");
            properties.setProperty("cache_size", "10000");
            if (readOnly) {
                properties.setProperty("read_only", "true");
                properties.setProperty("read_uncommitted", "true");
            }
            dataSource.setConnectProperties(properties);

            // 监控配置
            dataSource.setName("Druid-MBTiles-" + (readOnly ? "Read" : "Write") + "-" +
                    new File(dbPath).getName());
        });

        log.debug("创建 MBTiles 数据源: {}, readOnly: {}, maxActive: {}", dbPath, readOnly, maxActive);
        return (DruidDataSource) dataSourceDruidFastCreate.toDataSource();
    }

    // ==================== 数据库初始化方法 ====================

    /**
     * 初始化 MBTiles 数据库（创建表和索引）
     *
     * @param dataSource 数据源
     * @return 是否初始化成功
     */
    public static boolean initDatabase(DruidDataSource dataSource) {
        if (dataSource == null || dataSource.isClosed()) {
            log.error("数据源无效，无法初始化数据库");
            return false;
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 创建表
            stmt.execute(CREATE_TILES_TABLE_SQL);
            stmt.execute(CREATE_METADATA_TABLE_SQL);
            stmt.execute(CREATE_TILES_INDEX_SQL);

            log.debug("MBTiles 数据库表结构初始化成功");
            return true;

        } catch (SQLException e) {
            log.error("MBTiles 数据库初始化失败", e);
            return false;
        }
    }

    /**
     * 初始化 MBTiles 数据库并设置元数据
     *
     * @param dataSource 数据源
     * @param metadata   元数据键值对
     * @return 是否初始化成功
     */
    public static boolean initDatabase(DruidDataSource dataSource, String... metadata) {
        if (!initDatabase(dataSource)) {
            return false;
        }

        if (metadata != null && metadata.length > 0) {
            return initMetadata(dataSource, metadata);
        }
        return true;
    }

    /**
     * 检查 tiles 表是否存在
     *
     * @param dataSource 数据源
     * @return 是否存在
     */
    public static boolean checkTilesTableExists(DruidDataSource dataSource) {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(CHECK_TABLE_EXISTS_SQL)) {

            pstmt.setString(1, "tiles");
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            log.error("检查 tiles 表是否存在失败", e);
            return false;
        }
    }

    // ==================== 元数据操作方法 ====================

    /**
     * 初始化元数据
     *
     * @param dataSource 数据源
     * @param metadata   元数据键值对（key1, value1, key2, value2, ...）
     * @return 是否成功
     */
    public static boolean initMetadata(DruidDataSource dataSource, String... metadata) {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }

        if (metadata == null || metadata.length == 0 || metadata.length % 2 != 0) {
            log.warn("元数据参数格式错误，需要成对的 key-value");
            return false;
        }

        String sql = "INSERT OR IGNORE INTO metadata (name, value) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < metadata.length; i += 2) {
                pstmt.setString(1, metadata[i]);
                pstmt.setString(2, metadata[i + 1]);
                pstmt.execute();
            }
            return true;

        } catch (SQLException e) {
            log.error("初始化元数据失败", e);
            return false;
        }
    }

    /**
     * 获取元数据值
     *
     * @param dataSource 数据源
     * @param name       元数据名称
     * @return 元数据值，不存在返回 null
     */
    public static String getMetadata(DruidDataSource dataSource, String name) {
        if (dataSource == null || dataSource.isClosed()) {
            return null;
        }

        String sql = "SELECT value FROM metadata WHERE name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }

        } catch (SQLException e) {
            log.error("获取元数据失败: {}", name, e);
        }
        return null;
    }

    /**
     * 设置元数据值
     *
     * @param dataSource 数据源
     * @param name       元数据名称
     * @param value      元数据值
     * @return 是否成功
     */
    public static boolean setMetadata(DruidDataSource dataSource, String name, String value) {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }

        String sql = "INSERT OR REPLACE INTO metadata (name, value) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, value);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("设置元数据失败: {}={}", name, value, e);
            return false;
        }
    }

    // ==================== 瓦片操作方法 ====================

    /**
     * 读取瓦片数据
     *
     * @param dataSource 数据源
     * @param z          层级
     * @param x          列号
     * @param y          行号（存储格式）
     * @return 瓦片数据，不存在返回 null
     */
    public static byte[] getTile(DruidDataSource dataSource, int z, int x, int y) {
        if (dataSource == null || dataSource.isClosed()) {
            return null;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_TILE_SQL)) {

            pstmt.setInt(1, z);
            pstmt.setInt(2, x);
            pstmt.setInt(3, y);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("tile_data");
                }
            }

        } catch (SQLException e) {
            log.error("读取瓦片失败: z={}, x={}, y={}", z, x, y, e);
        }
        return null;
    }

    /**
     * 保存瓦片数据
     *
     * @param dataSource 数据源
     * @param z          层级
     * @param x          列号
     * @param y          行号（存储格式）
     * @param data       瓦片数据
     * @return 是否成功
     */
    public static boolean putTile(DruidDataSource dataSource, int z, int x, int y, byte[] data) {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }

        if (data == null || data.length == 0) {
            log.warn("瓦片数据为空，忽略保存: z={}, x={}, y={}", z, x, y);
            return false;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_OR_REPLACE_TILE_SQL)) {

            pstmt.setInt(1, z);
            pstmt.setInt(2, x);
            pstmt.setInt(3, y);
            pstmt.setBytes(4, data);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            String url = dataSource.getUrl();
            log.error("保存瓦片失败: z={}, x={}, y={},url:{}", z, x, y, url, e);
            return false;
        }
    }

    /**
     * 删除瓦片
     *
     * @param dataSource 数据源
     * @param z          层级
     * @param x          列号
     * @param y          行号（存储格式）
     * @return 是否成功
     */
    public static boolean deleteTile(DruidDataSource dataSource, int z, int x, int y) {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_TILE_SQL)) {

            pstmt.setInt(1, z);
            pstmt.setInt(2, x);
            pstmt.setInt(3, y);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("删除瓦片失败: z={}, x={}, y={}", z, x, y, e);
            return false;
        }
    }

    /**
     * 检查瓦片是否存在
     *
     * @param dataSource 数据源
     * @param z          层级
     * @param x          列号
     * @param y          行号（存储格式）
     * @return 是否存在
     */
    public static boolean existsTile(DruidDataSource dataSource, int z, int x, int y) {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(EXISTS_TILE_SQL)) {

            pstmt.setInt(1, z);
            pstmt.setInt(2, x);
            pstmt.setInt(3, y);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            log.error("检查瓦片存在失败: z={}, x={}, y={}", z, x, y, e);
            return false;
        }
    }

    /**
     * 按层级删除瓦片
     *
     * @param dataSource 数据源
     * @param z          层级
     * @return 删除的数量
     */
    public static int deleteTilesByZoom(DruidDataSource dataSource, int z) {
        if (dataSource == null || dataSource.isClosed()) {
            return 0;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_BY_ZOOM_SQL)) {

            pstmt.setInt(1, z);
            return pstmt.executeUpdate();

        } catch (SQLException e) {
            log.error("按层级删除瓦片失败: z={}", z, e);
            return 0;
        }
    }

    /**
     * 按层级和列删除瓦片
     *
     * @param dataSource 数据源
     * @param z          层级
     * @param x          列号
     * @return 删除的数量
     */
    public static int deleteTilesByZoomAndX(DruidDataSource dataSource, int z, int x) {
        if (dataSource == null || dataSource.isClosed()) {
            return 0;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_BY_ZOOM_AND_X_SQL)) {

            pstmt.setInt(1, z);
            pstmt.setInt(2, x);
            return pstmt.executeUpdate();

        } catch (SQLException e) {
            log.error("按层级和列删除瓦片失败: z={}, x={}", z, x, e);
            return 0;
        }
    }

    /**
     * 清空所有瓦片
     *
     * @param dataSource 数据源
     * @return 删除的数量
     */
    public static int truncateTiles(DruidDataSource dataSource) {
        if (dataSource == null || dataSource.isClosed()) {
            return 0;
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            return stmt.executeUpdate(TRUNCATE_TILES_SQL);

        } catch (SQLException e) {
            log.error("清空瓦片失败", e);
            return 0;
        }
    }

    // ==================== 统计方法 ====================

    /**
     * 获取瓦片总数
     *
     * @param dataSource 数据源
     * @return 瓦片总数，查询失败返回 -1
     */
    public static long getTileCount(DruidDataSource dataSource) {
        if (dataSource == null || dataSource.isClosed()) {
            return -1;
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(COUNT_TILES_SQL)) {

            if (rs.next()) {
                return rs.getLong(1);
            }

        } catch (SQLException e) {
            log.error("获取瓦片总数失败", e);
        }
        return -1;
    }

    /**
     * 按层级统计瓦片数量
     *
     * @param dataSource 数据源
     * @param zoom       层级
     * @return 瓦片数量，查询失败返回 -1
     */
    public static long getTileCountByZoom(DruidDataSource dataSource, int zoom) {
        if (dataSource == null || dataSource.isClosed()) {
            return -1;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(COUNT_TILES_BY_ZOOM_SQL)) {

            pstmt.setInt(1, zoom);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

        } catch (SQLException e) {
            log.error("按层级统计瓦片数量失败: zoom={}", zoom, e);
        }
        return -1;
    }

    // ==================== 文件操作方法 ====================

    /**
     * 检查 MBTiles 文件是否存在
     *
     * @param dbPath 数据库文件路径
     * @return 是否存在
     */
    public static boolean existsFile(String dbPath) {
        if (dbPath == null || dbPath.trim().isEmpty()) {
            return false;
        }
        File file = new File(dbPath);
        return file.exists() && file.isFile();
    }

    /**
     * 获取 MBTiles 文件大小
     *
     * @param dbPath 数据库文件路径
     * @return 文件大小（字节），文件不存在返回 0
     */
    public static long getFileSize(String dbPath) {
        if (dbPath == null || dbPath.trim().isEmpty()) {
            return 0;
        }
        File file = new File(dbPath);
        return file.exists() ? file.length() : 0;
    }

    /**
     * 删除 MBTiles 文件
     *
     * @param dbPath 数据库文件路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String dbPath) {
        if (dbPath == null || dbPath.trim().isEmpty()) {
            return false;
        }
        File file = new File(dbPath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                log.info("删除 MBTiles 文件成功: {}", dbPath);
            } else {
                log.warn("删除 MBTiles 文件失败: {}", dbPath);
            }
            return deleted;
        }
        log.debug("MBTiles 文件不存在: {}", dbPath);
        return false;
    }

    /**
     * 确保目录存在
     *
     * @param dbPath 数据库文件路径
     * @return 目录是否存在或创建成功
     */
    public static boolean ensureDirectoryExists(String dbPath) {
        if (dbPath == null || dbPath.trim().isEmpty()) {
            return false;
        }
        File file = new File(dbPath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                log.debug("创建目录成功: {}", parentDir.getAbsolutePath());
            }
            return created;
        }
        return parentDir == null || parentDir.exists();
    }

    /**
     * 获取安全的文件名（过滤非法字符）
     *
     * @param layerName 图层名称
     * @return 安全的文件名
     */
    public static String getSafeFileName(String layerName) {
        if (layerName == null || layerName.trim().isEmpty()) {
            return "unknown";
        }
        return layerName.replaceAll("[^a-zA-Z0-9\\-_]", "_");
    }

    // ==================== 连接管理方法 ====================

    /**
     * 关闭数据源
     *
     * @param dataSource 数据源
     */
    public static void closeDataSource(DruidDataSource dataSource) {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.debug("MBTiles 数据源已关闭");
        }
    }

    /**
     * 测试数据源连接
     *
     * @param dataSource 数据源
     * @return 连接是否有效
     */
    public static boolean testConnection(DruidDataSource dataSource) {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            return rs.next();

        } catch (SQLException e) {
            log.error("测试连接失败", e);
            return false;
        }
    }
}
