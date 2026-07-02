package cn.geoair.map.tile.forge.core.zip.cache;

import cn.geoair.map.tile.forge.core.caches.CacheProvider;
import cn.geoair.map.tile.forge.core.caches.S3CacheProvider;
import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import lombok.extern.slf4j.Slf4j;
import org.sqlite.JDBC;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class SQLiteLayerPerFileDao implements LayerPerFileDao, AutoCloseable {

    private final String baseDir;          // 数据库文件根目录
    private final String layerName;        // 图层名称
    private Connection connection;
    private static final String TABLE_NAME = "tile_central_directory"; // 固定表名
    private static final String CACHE_STATUS_TABLE = "cache_status";    // 缓存状态表名

    // 缓存状态枚举
    public enum CacheStatus {
        NOT_CACHED,    // 未缓存
        CACHING,       // 缓存中
        CACHED,        // 已缓存
        CACHE_FAILED   // 缓存失败
    }

    // 构造函数：传入根目录 + 图层名称（生成独立SQLite文件）
    public SQLiteLayerPerFileDao(String layerName) {
        String tempPath = TileTempPathConfig.getInstance().getTempPath();
        if (!tempPath.endsWith("/") && !tempPath.endsWith("\\")) {
            tempPath += File.separator;
        }
        this.baseDir = tempPath + "sql_lite_meta";
        FileUtil.mkdir(this.baseDir);
        this.layerName = layerName;
    }

    // 获取图层专属SQLite文件路径
    private String getDbFilePath() {
        String format = String.format("%s%s%s_tiles.db", baseDir, File.separator, layerName.replaceAll("[^a-zA-Z0-9_]", "_"));
//        log.info("getDbFilePath: {}", format);
        return format;
    }

    // 初始化：创建图层专属数据库文件 + 表 + 缓存状态表
    public void init() throws SQLException {
        // 连接图层专属SQLite文件（不存在自动创建）
        DriverManager.registerDriver(new JDBC());
        if (connection == null) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + getDbFilePath());
        }
        // 创建瓦片数据表
        String createTableSql = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "local_header_offset BIGINT NOT NULL, " +
                        "data_offset BIGINT, " +
                        "compression_method BIGINT NOT NULL, " +
                        "compressed_size BIGINT NOT NULL, " +
                        "uncompressed_size BIGINT NOT NULL, " +
                        "name TEXT, " +
                        "entry_size INTEGER NOT NULL, " +
                        "directory_is BOOLEAN NOT NULL, " +
                        "xyz_path TEXT, " +
                        "x TEXT, " +
                        "y TEXT, " +
                        "z TEXT, " +
                        "file_name TEXT" +
                        ")", TABLE_NAME
        );

        // 创建缓存状态表
        String createStatusTableSql = String.format(
                "CREATE TABLE IF NOT EXISTS %s (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "layer_name TEXT UNIQUE NOT NULL, " +
                        "cache_status TEXT NOT NULL, " +
                        "cache_time TIMESTAMP, " +
                        "cache_size BIGINT, " +
                        "last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")", CACHE_STATUS_TABLE
        );

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSql);
            stmt.execute(createStatusTableSql);
            // 初始化当前图层的缓存状态（如果不存在）
            initCacheStatus();
        }
        connection.setAutoCommit(false);
    }

    /**
     * 初始化当前图层的缓存状态记录
     */
    private void initCacheStatus() throws SQLException {
        String checkSql = String.format("SELECT 1 FROM %s WHERE layer_name = ?", CACHE_STATUS_TABLE);
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, layerName);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) {
                    // 插入初始状态
                    String insertSql = String.format(
                            "INSERT INTO %s (layer_name, cache_status) VALUES (?, ?)",
                            CACHE_STATUS_TABLE);
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        insertStmt.setString(1, layerName);
                        insertStmt.setString(2, CacheStatus.NOT_CACHED.name());
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }

    /**
     * 更新缓存状态
     */
    public void updateCacheStatus(CacheStatus status) throws SQLException {
        updateCacheStatus(status, 0);
    }

    /**
     * 更新缓存状态（带缓存大小）
     */
    public void updateCacheStatus(CacheStatus status, long cacheSize) throws SQLException {
        init();
        String sql = String.format(
                "UPDATE %s SET cache_status = ?, last_update = CURRENT_TIMESTAMP, " +
                        "cache_time = CASE WHEN ? = 'CACHED' THEN CURRENT_TIMESTAMP ELSE cache_time END, " +
                        "cache_size = CASE WHEN ? = 'CACHED' THEN ? ELSE cache_size END " +
                        "WHERE layer_name = ?", CACHE_STATUS_TABLE);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, CacheStatus.CACHED.name());
            stmt.setString(3, CacheStatus.CACHED.name());
            stmt.setLong(4, cacheSize);
            stmt.setString(5, layerName);

            stmt.executeUpdate();
            connection.commit();
            log.info("图层{}缓存状态更新为:{}", layerName, status.name());
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

    /**
     * 获取当前图层的缓存状态
     */
    public CacheStatus getCacheStatus() throws SQLException {
        init();
        String sql = String.format("SELECT cache_status FROM %s WHERE layer_name = ?", CACHE_STATUS_TABLE);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, layerName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return CacheStatus.valueOf(rs.getString("cache_status"));
                }
                return CacheStatus.NOT_CACHED;
            }
        }
    }

    /**
     * 获取缓存状态详情
     */
    public CacheStatusInfo getCacheStatusInfo() throws SQLException {
        init();
        String sql = String.format(
                "SELECT * FROM %s WHERE layer_name = ?", CACHE_STATUS_TABLE);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, layerName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    CacheStatusInfo info = new CacheStatusInfo();
                    info.setLayerName(rs.getString("layer_name"));
                    info.setCacheStatus(CacheStatus.valueOf(rs.getString("cache_status")));
                    info.setCacheTime(rs.getTimestamp("cache_time"));
                    info.setCacheSize(rs.getLong("cache_size"));
                    info.setLastUpdate(rs.getTimestamp("last_update"));
                    return info;
                }
                return null;
            }
        }
    }

    @Override
    public String getTableName() {
        return getDbFilePath();
    }

    // 插入/批量插入/查询方法保持不变...
    public void insert(TileCentralDirectoryModel entry) throws SQLException {
        init();
        String sql = String.format(
                "INSERT INTO %s (local_header_offset, data_offset, compression_method, compressed_size, " +
                        "uncompressed_size, name, entry_size, directory_is, xyz_path, x, y, z, file_name) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", TABLE_NAME
        );

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, entry);
            stmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

    @Override
    public void batchInsert(List<TileCentralDirectoryModel> entries) throws SQLException {
        init();
        String sql = String.format(
                "INSERT INTO %s (local_header_offset, data_offset, compression_method, compressed_size, " +
                        "uncompressed_size, name, entry_size, directory_is, xyz_path, x, y, z, file_name) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", TABLE_NAME
        );
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (TileCentralDirectoryModel entry : entries) {
                setParameters(stmt, entry);
                stmt.addBatch();
            }
            stmt.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }


    public TileCentralDirectoryModel findByXyzPath(String xyzPath) throws SQLException {
        init();
        String sql = String.format("SELECT * FROM %s WHERE xyz_path = ?", TABLE_NAME);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, xyzPath);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntry(rs);
                }
                return null;
            }
        }
    }

    public TileCentralDirectoryModel findByXyz(String x, String y, String z) throws SQLException {
        init();
        String sql = String.format("SELECT * FROM %s WHERE x = ? and y = ? and z = ?", TABLE_NAME);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, x);
            stmt.setString(2, y);
            stmt.setString(3, z);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntry(rs);
                }
                return null;
            }
        }
    }

    public TileCentralDirectoryModel findByFileName(String fileName) throws SQLException {
        init();

        String sql = String.format("SELECT * FROM %s WHERE file_name  = ?  ", TABLE_NAME);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, fileName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntry(rs);
                }
                return null;
            }
        }
    }

    public TileCentralDirectoryModel findById(Long id) throws SQLException {
        init();
        String sql = String.format("SELECT * FROM %s WHERE id = ?", TABLE_NAME);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntry(rs);
                }
                return null;
            }
        }

    }

    @Override
    public void findBySql(String sql, Consumer<TileCentralDirectoryModel> consumer) throws SQLException {
        init();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            log.info("findBySql sql:{}", sql);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TileCentralDirectoryModel tileCentralDirectoryEntry = mapResultSetToEntry(rs);
                    consumer.accept(tileCentralDirectoryEntry);
                }

            }
        }
    }

    @Override
    public void findAll(Consumer<TileCentralDirectoryModel> consumer) throws SQLException {
        init();
        findBySql(String.format("SELECT * FROM %s  ", TABLE_NAME), consumer);
    }

    @Override
    public boolean cacheEnableIs(GirLayerConfigContext layerConfigContext) {
        String dbFilePath = getDbFilePath();
        boolean exist = FileUtil.exist(dbFilePath);
        if (!exist) {
            CacheProvider cacheProvider = getCacheProvider();
            boolean exists = cacheProvider.exists("sqlLiteCache_" + layerName);
            if (exists) {
                byte[] object = (byte[]) cacheProvider.getObject("sqlLiteCache_" + layerName);
                // 将字节数组写入本地SQLite文件
                FileUtil.writeBytes(object, dbFilePath);
                return true;
            } else {
                return false;
            }
        } else {
            try {
                // 先检查缓存状态
                CacheStatus status = getCacheStatus();
                if (status != CacheStatus.CACHED) {
                    return false;
                } else {
                    return true;
                }
            } catch (SQLException e) {
                log.error("获取缓存状态失败", e);
                return false;
            }
        }
    }

    @Override
    public void doPreCacheEnd() {
        try {
            // 更新状态为缓存中
            updateCacheStatus(CacheStatus.CACHED);
            CacheProvider cacheProvider = getCacheProvider();
            File dbFile = new File(getDbFilePath());
            byte[] dbBytes = FileUtil.readBytes(dbFile);
            log.info("图层{}缓存开始，文件大小:{}字节", layerName, dbFile.length());
            cacheProvider.put("sqlLiteCache_" + layerName, dbBytes, -1);

            log.info("图层{}缓存成功，文件大小:{}字节", layerName, dbFile.length());
        } catch (Exception e) {
            log.error("图层{}缓存失败", layerName, e);
            try {
                // 更新状态为缓存失败
                updateCacheStatus(CacheStatus.CACHE_FAILED);
            } catch (SQLException ex) {
                log.error("更新缓存失败状态失败", ex);
            }
        }
    }

    @Override
    public void doPreCacheStart() {
        try {
            // 更新状态为缓存中
            updateCacheStatus(CacheStatus.CACHING);
            log.info("图层{}缓存开始 ", layerName);
        } catch (Exception e) {
            log.error("图层{}缓存失败", layerName, e);
            try {
                // 更新状态为缓存失败
                updateCacheStatus(CacheStatus.CACHE_FAILED);
            } catch (SQLException ex) {
                log.error("更新缓存失败状态失败", ex);
            }
        }
    }

    @Override
    public void delCache() {
        CacheProvider cacheProvider = getCacheProvider();
        cacheProvider.evict("sqlLiteCache_" + layerName);
        File dbFile = new File(getDbFilePath());
        FileUtil.del(dbFile);
    }

    @Override
    public CacheProvider getCacheProvider() {
        return new S3CacheProvider("tilePreCache");
    }

    // 私有辅助方法
    private void setParameters(PreparedStatement stmt, TileCentralDirectoryModel entry) throws SQLException {
        stmt.setLong(1, entry.getLocalHeaderOffset());
        stmt.setObject(2, entry.getDataOffset());
        stmt.setLong(3, entry.getCompressionMethod());
        stmt.setLong(4, entry.getCompressedSize());
        stmt.setLong(5, entry.getUncompressedSize());
        stmt.setString(6, entry.getName());
        stmt.setInt(7, entry.getEntrySize());
        stmt.setBoolean(8, entry.isDirectoryIs());
        stmt.setString(9, entry.getXyzPath());
        stmt.setString(10, entry.getX());
        stmt.setString(11, entry.getY());
        stmt.setString(12, entry.getZ());
        stmt.setString(13, entry.getFileName());
    }

    private TileCentralDirectoryModel mapResultSetToEntry(ResultSet rs) throws SQLException {
        TileCentralDirectoryModel entry = new TileCentralDirectoryModel(
                rs.getLong("local_header_offset"),
                rs.getObject("data_offset") != null ? rs.getLong("data_offset") : null,
                rs.getLong("compression_method"),
                rs.getLong("compressed_size"),
                rs.getLong("uncompressed_size"),
                rs.getString("name"),
                rs.getInt("entry_size")
        );
        entry.setId(rs.getLong("id"));
        entry.setDirectoryIs(rs.getBoolean("directory_is"));
        entry.setXyzPath(rs.getString("xyz_path"));
        entry.setX(rs.getString("x"));
        entry.setY(rs.getString("y"));
        entry.setZ(rs.getString("z"));
        entry.setFileName(rs.getString("file_name"));
        return entry;
    }

    /**
     * 缓存状态信息类
     */
    public static class CacheStatusInfo {
        private String layerName;
        private CacheStatus cacheStatus;
        private Timestamp cacheTime;
        private long cacheSize;
        private Timestamp lastUpdate;

        // Getters and Setters
        public String getLayerName() {
            return layerName;
        }

        public void setLayerName(String layerName) {
            this.layerName = layerName;
        }

        public CacheStatus getCacheStatus() {
            return cacheStatus;
        }

        public void setCacheStatus(CacheStatus cacheStatus) {
            this.cacheStatus = cacheStatus;
        }

        public Timestamp getCacheTime() {
            return cacheTime;
        }

        public void setCacheTime(Timestamp cacheTime) {
            this.cacheTime = cacheTime;
        }

        public long getCacheSize() {
            return cacheSize;
        }

        public void setCacheSize(long cacheSize) {
            this.cacheSize = cacheSize;
        }

        public Timestamp getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(Timestamp lastUpdate) {
            this.lastUpdate = lastUpdate;
        }
    }

    public void close() {
        IoUtil.close(connection);
    }
}
