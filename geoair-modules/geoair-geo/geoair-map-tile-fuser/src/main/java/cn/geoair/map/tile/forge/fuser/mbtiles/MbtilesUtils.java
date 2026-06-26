package cn.geoair.map.tile.forge.fuser.mbtiles;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.dynamic.ds.simple.DriverManagerDataSource;
import cn.geoair.comp.dynamic.ds.utils.DataSourceDruidFastCreate;
import cn.hutool.core.date.DateUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.File;
import java.sql.*;
import java.util.List;
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

public class MbtilesUtils {
    private static GiLogger log = GirLoggerFactory.getLogger();
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
     * @param dbPath 数据库文件路径
     * @return DruidDataSource
     */
    public static DruidDataSource createDataSource(String dbPath) {
        return createDataSource(dbPath, false, 10, 1);
    }

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

    /**
     * 检查图层是否已存在
     */
    public static boolean layerExists(DruidDataSource dataSource, String layerName) {
        String sql = "SELECT COUNT(*) FROM metadata WHERE name = ? AND value = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "name");
            pstmt.setString(2, layerName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            log.debug("检查图层是否存在失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 获取图层名称
     */
    public static String getLayerName(DruidDataSource dataSource, String layerName) {
        if (layerName != null && !layerName.isEmpty()) {
            // 检查图层是否存在
            if (MbtilesUtils.layerExists(dataSource, layerName)) {
                return layerName;
            }
            log.warn("图层不存在: {}, 将使用第一个可用图层", layerName);
        }

        // 获取第一个图层
        String sql = "SELECT value FROM metadata WHERE name = 'name' LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            log.error("获取图层名称失败", e);
        }
        return null;
    }

    // ==================== 元数据操作方法 ====================

    /**
     * 初始化元数据
     *
     * @param dataSource 数据源
     * @return 是否成功
     */
    public static boolean initMetadata(String layerName, String format, DruidDataSource dataSource) {
        return MbtilesUtils.initMetadata(dataSource,
                "name", layerName,
                "format", format,
                "version", "1.0",
                "type", "overlay"
        );
    }


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

    // ==================== 数据库维护方法 ====================

    /**
     * 执行 VACUUM 清理空间
     * <p>
     * VACUUM 命令会重建数据库文件，回收未使用的空间并整理碎片。
     * 注意：VACUUM 会锁定数据库，执行期间不能进行写操作，建议在低峰期执行。
     * </p>
     *
     * @param dbPath 数据库文件位置
     * @return 是否执行成功
     */
    public static boolean vacuum(String dbPath) {
        if (dbPath == null) {
            log.error("数据源无效，无法执行 VACUUM");
            return false;
        }
        DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:sqlite:" + dbPath, null, null);
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            log.info("开始执行 VACUUM 清理空间...");
            long startTime = System.currentTimeMillis();

            stmt.execute("VACUUM");

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("VACUUM 执行完成，耗时: {} s", elapsed/1000);
            return true;

        } catch (SQLException e) {
            log.error("执行 VACUUM 失败", e);
            return false;
        }
    }

    /**
     * 执行 WAL 日志同步到主文件（Checkpoint）
     * <p>
     * 将 WAL 文件中的内容同步到主数据库文件中，释放 WAL 文件占用的空间。
     * <ul>
     *   <li>PASSIVE: 默认模式，不阻塞其他读写操作</li>
     *   <li>FULL: 阻塞写操作，直到所有 WAL 内容同步完成</li>
     *   <li>RESTART: 与 FULL 类似，但同步后会重置 WAL 文件</li>
     * </ul>
     * </p>
     *
     * @param dataSource 数据源
     * @param mode       检查点模式：PASSIVE、FULL、RESTART
     * @return 是否执行成功
     */
    public static boolean walCheckpoint(DataSource dataSource, String mode) {
        if (dataSource == null) {
            log.error("数据源无效，无法执行 WAL checkpoint");
            return false;
        }

        if (mode == null || mode.trim().isEmpty()) {
            mode = "PASSIVE";
        }

        // 确保模式有效
        String upperMode = mode.toUpperCase();
        if (!"PASSIVE".equals(upperMode) && !"FULL".equals(upperMode) && !"RESTART".equals(upperMode)) {
            log.warn("无效的 checkpoint 模式: {}，使用默认 PASSIVE", mode);
            upperMode = "PASSIVE";
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            log.info("开始执行 WAL checkpoint (模式: {})...", upperMode);
            long startTime = System.currentTimeMillis();

            stmt.execute("PRAGMA wal_checkpoint(" + upperMode + ")");

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("WAL checkpoint 执行完成，耗时: {} s", elapsed/1000);
            return true;

        } catch (SQLException e) {
            log.error("执行 WAL checkpoint 失败 (模式: {})", upperMode, e);
            return false;
        }
    }

    /**
     * 执行 WAL 日志同步到主文件（使用 PASSIVE 模式）
     *
     * @param dataSource 数据源
     * @return 是否执行成功
     */
    public static boolean walCheckpoint(DataSource dataSource) {
        return walCheckpoint(dataSource, "PASSIVE");
    }

    /**
     * 执行完整的 WAL 日志同步（FULL 模式）
     * <p>
     * 注意：此方法会阻塞写操作，直到所有 WAL 内容同步到主文件。
     * 建议在低峰期或关闭数据源前调用。
     * </p>
     *
     * @param dataSource 数据源
     * @return 是否执行成功
     */
    public static boolean walCheckpointFull(DataSource dataSource) {
        return walCheckpoint(dataSource, "FULL");
    }

    /**
     * 执行 WAL 日志同步并重置（RESTART 模式）
     * <p>
     * 与 FULL 模式类似，但同步后会重置 WAL 文件，常用于维护操作。
     * </p>
     *
     * @param dataSource 数据源
     * @return 是否执行成功
     */
    public static boolean walCheckpointRestart(DruidDataSource dataSource) {
        return walCheckpoint(dataSource, "RESTART");
    }

    /**
     * 获取 WAL 文件大小
     * <p>
     * 通过查询 PRAGMA wal_checkpoint 获取 WAL 文件大小信息
     * </p>
     *
     * @param dataSource 数据源
     * @return WAL 文件大小（字节），查询失败返回 -1
     */
    public static long getWalSize(DruidDataSource dataSource) {
        if (dataSource == null || dataSource.isClosed()) {
            return -1;
        }

        // 查询 WAL 文件大小（从数据库连接属性获取）
        String sql = "PRAGMA wal_checkpoint";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // PRAGMA wal_checkpoint 返回三列：busy, log, checkpointed
            // log 列表示 WAL 文件中的页数
            if (rs.next()) {
                // 获取页面大小，计算 WAL 文件大小
                long walPages = rs.getLong(2); // log 列
                if (walPages > 0) {
                    try (Statement stmt2 = conn.createStatement();
                         ResultSet rs2 = stmt2.executeQuery("PRAGMA page_size")) {
                        if (rs2.next()) {
                            int pageSize = rs2.getInt(1);
                            return walPages * pageSize;
                        }
                    }
                }
                return 0; // WAL 为空
            }

        } catch (SQLException e) {
            log.error("获取 WAL 文件大小失败", e);
        }
        return -1;
    }

    /**
     * 检查当前是否启用 WAL 模式
     *
     * @param dataSource 数据源
     * @return 是否启用 WAL
     */
    public static boolean isWalEnabled(DruidDataSource dataSource) {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA journal_mode")) {

            if (rs.next()) {
                String mode = rs.getString(1);
                return "wal".equalsIgnoreCase(mode);
            }

        } catch (SQLException e) {
            log.error("检查 WAL 模式失败", e);
        }
        return false;
    }

//    /**
//     * 压缩数据库（执行 VACUUM 和 WAL checkpoint 的组合）
//     * <p>
//     * 先执行 FULL 模式的 checkpoint 将 WAL 同步到主文件，
//     * 再执行 VACUUM 回收空间。
//     * 注意：此操作会锁定数据库，建议在低峰期执行。
//     * </p>
//     *
//     * @param dbPath 数据库的位置
//     * @return 是否执行成功
//     */
//    public static boolean compactDatabase(String dbPath) {
//        DruidDataSource dataSource = createDataSource(dbPath);
//        boolean b = compactDatabase(dataSource);
//        dataSource.close();
//        return b;
//    }

    /**
     * 压缩数据库（执行 VACUUM 和 WAL checkpoint 的组合）
     * <p>
     * 先执行 FULL 模式的 checkpoint 将 WAL 同步到主文件，
     * 再执行 VACUUM 回收空间。
     * 注意：此操作会锁定数据库，建议在低峰期执行。
     * </p>
     *
     * @param dbPath 数据源
     * @return 是否执行成功
     */
    public static boolean compactDatabase(String dbPath) {
        if (dbPath == null) {
            log.error("数据源无效，无法压缩数据库");
            return false;
        }

        log.info("开始压缩数据库...");
        long startTime = System.currentTimeMillis();
        DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:sqlite:" + dbPath, null, null);
        if (!walCheckpointFull(dataSource)) {
            log.warn("WAL checkpoint 执行失败，继续执行 VACUUM...");
        }
        // 第二步：执行 VACUUM
        boolean vacuumResult = vacuum(dbPath);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("数据库压缩完成，结果: {}, 耗时: {} ms", vacuumResult, elapsed);

        return vacuumResult;
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
    public static MbtilesInfo getTile(DruidDataSource dataSource, int z, int x, int y) {
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
                    return MbtilesInfo.of().setZoomLevel(z).setX(x).setY(y).setTileData(rs.getBytes("tile_data"));
                }
            }

        } catch (SQLException e) {
            log.error("读取瓦片失败: z={}, x={}, y={}", z, x, y, e);
        }
        return MbtilesInfo.of();
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
     * 批量插入瓦片数据
     *
     * @param targetDataSource 目标数据源
     * @param overwrite        是否覆盖已存在的瓦片
     * @param mbtilesInfos     瓦片信息列表
     * @return int[]{success, skipped, failed}
     */
    public static int[] putTileBatch(DruidDataSource targetDataSource, boolean overwrite, List<MbtilesInfo> mbtilesInfos) {
        if (mbtilesInfos == null || mbtilesInfos.isEmpty()) {
            log.warn("瓦片列表为空，跳过批量插入");
            return new int[]{0, 0, 0};
        }

        if (targetDataSource == null || targetDataSource.isClosed()) {
            log.error("数据源无效或已关闭，无法执行批量插入");
            return new int[]{0, 0, mbtilesInfos.size()};
        }

        String insertSql = overwrite
                ? "INSERT OR REPLACE INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)"
                : "INSERT OR IGNORE INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)";

        int success = 0;
        int skipped = 0;
        int failed = 0;

        long startTime = System.currentTimeMillis();
        int totalCount = mbtilesInfos.size();
        try (DruidPooledConnection conn = targetDataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (MbtilesInfo args : mbtilesInfos) {
                    pstmt.setInt(1, args.getZoomLevel());
                    pstmt.setInt(2, args.getTileColumn());
                    pstmt.setInt(3, args.getTileRow());
                    pstmt.setBytes(4, args.getTileData());
                    pstmt.addBatch();
                }

                int[] results = pstmt.executeBatch();
                for (int result : results) {
                    if (result >= 0 || result == Statement.SUCCESS_NO_INFO) {
                        success++;
                    } else if (result == Statement.EXECUTE_FAILED) {
                        failed++;
                    } else {
                        skipped++;
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                log.error("批量插入失败", e);
                failed = mbtilesInfos.size();
            }
        } catch (SQLException e) {
            log.error("获取数据库连接失败", e);
            failed = mbtilesInfos.size();
        }

        long costTime = System.currentTimeMillis() - startTime;
        log.info("批量插入完成: 总数={}, 成功={}, 跳过={}, 失败={}, 耗时={}s,覆盖模式: {}",
                totalCount, success, skipped, failed, costTime / 1000, overwrite);

        return new int[]{success, skipped, failed};
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
