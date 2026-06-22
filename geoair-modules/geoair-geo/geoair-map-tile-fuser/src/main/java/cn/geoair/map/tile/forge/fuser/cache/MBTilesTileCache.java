package cn.geoair.map.tile.forge.fuser.cache;

import cn.geoair.comp.dynamic.ds.utils.DataSourceDruidFastCreate;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.fuser.cache.utils.FuserCacheUtils;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MBTiles 瓦片缓存实现
 * <p>
 * MBTiles 规范：https://github.com/mapbox/mbtiles-spec
 * 每个图层独立一个 .mbtiles 文件
 * </p>
 * <p>
 * 目录结构: cacheRoot/layerName.mbtiles
 * </p>
 *
 * @author 张逢吉
 * @date Created in 2026/06/22
 * @description 基于 MBTiles 规范的瓦片缓存实现，使用 Druid 连接池，读写分离
 */
@Slf4j
public class MBTilesTileCache implements TileCache {

    private final String cacheRoot;
    private final boolean enabled;
    private final ConcurrentHashMap<String, LayerCacheHolder> layerCaches = new ConcurrentHashMap<>();

    private int maxReadPoolSize = 20;

    private int maxWritePoolSize = 5;

    private int minIdle = 2;

    public MBTilesTileCache setMaxReadPoolSize(int maxReadPoolSize) {
        this.maxReadPoolSize = maxReadPoolSize;
        return this;
    }

    public MBTilesTileCache setMaxWritePoolSize(int maxWritePoolSize) {
        this.maxWritePoolSize = maxWritePoolSize;
        return this;
    }

    public MBTilesTileCache setMinIdle(int minIdle) {
        this.minIdle = minIdle;
        return this;
    }

    // MBTiles 标准表结构
    private static final String CREATE_TILES_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS tiles (" +
                    "  zoom_level INTEGER NOT NULL," +
                    "  tile_column INTEGER NOT NULL," +
                    "  tile_row INTEGER NOT NULL," +
                    "  tile_data BLOB NOT NULL," +
                    "  PRIMARY KEY (zoom_level, tile_column, tile_row)" +
                    ")";

    private static final String CREATE_METADATA_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS metadata (" +
                    "  name TEXT NOT NULL," +
                    "  value TEXT," +
                    "  PRIMARY KEY (name)" +
                    ")";

    private static final String CREATE_TILES_INDEX_SQL =
            "CREATE INDEX IF NOT EXISTS idx_tiles_zoom ON tiles(zoom_level)";

    /**
     * 构造函数（使用默认缓存目录）
     */
    public MBTilesTileCache() {
        this(System.getProperty("java.io.tmpdir") + "/mbtiles_cache/", true);
    }

    /**
     * 构造函数
     *
     * @param cacheRoot 缓存根目录
     */
    public MBTilesTileCache(String cacheRoot) {
        this(cacheRoot, true);
    }

    /**
     * 构造函数
     *
     * @param cacheRoot 缓存根目录
     * @param enabled   是否启用缓存
     */
    public MBTilesTileCache(String cacheRoot, boolean enabled) {
        this.cacheRoot = cacheRoot.endsWith("/") ? cacheRoot : cacheRoot + "/";
        this.enabled = enabled;

        // 初始化缓存目录
        if (enabled) {
            try {
                java.io.File dir = new java.io.File(this.cacheRoot);
                if (!dir.exists()) {
                    if (dir.mkdirs()) {
                        log.info("创建缓存根目录: {}", this.cacheRoot);
                    }
                }
                log.info("Druid MBTiles 缓存初始化完成，根目录: {}", this.cacheRoot);
            } catch (Exception e) {
                log.error("创建缓存根目录失败: {}", this.cacheRoot, e);
            }
        }
    }

    /**
     * 获取图层对应的 MBTiles 文件路径
     *
     * @param layerName 图层名称
     * @return 文件路径
     */
    private String getDbPath(String layerName) {
        // 过滤非法字符，保证文件名安全
        String safeName = layerName.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        return cacheRoot + safeName + ".mbtiles";
    }

    /**
     * 获取或创建图层的缓存持有者
     */
    private LayerCacheHolder getOrCreateHolder(String layerName) {
        if (!enabled) {
            return null;
        }

        try {

            return layerCaches.computeIfAbsent(layerName, key -> {
                try {
                    String dbPath = getDbPath(key);
                    log.debug("创建图层缓存: {}", key);
                    return new LayerCacheHolder(
                            dbPath,
                            FuserCacheUtils.isNeedReverseY(key),
                            maxReadPoolSize,
                            maxWritePoolSize,
                            minIdle
                    );
                } catch (Exception e) {
                    log.error("创建图层缓存失败: {}", key, e);
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("获取或创建图层缓存异常: {}", layerName, e);
            return null;
        }
    }

    @Override
    public byte[] get(String layerName, int z, int x, int y, ImageMime format) {
        if (!enabled) {
            return null;
        }

        LayerCacheHolder holder = getOrCreateHolder(layerName);
        if (holder == null) {
            return null;
        }

        long startTime = System.currentTimeMillis();
        byte[] data = holder.get(z, x, y);
        if (data != null && log.isDebugEnabled()) {
            log.debug("MBTiles 缓存命中: {} ({},{},{}) 耗时: {}ms",
                    layerName, z, x, y, System.currentTimeMillis() - startTime);
        }
        return data;
    }

    @Override
    public boolean put(String layerName, int z, int x, int y, byte[] data, ImageMime format) {
        if (!enabled || data == null || data.length == 0) {
            return false;
        }

        LayerCacheHolder holder = getOrCreateHolder(layerName);
        if (holder == null) {
            return false;
        }

        boolean result = holder.put(z, x, y, data);
        if (result && log.isDebugEnabled()) {
            log.debug("MBTiles 缓存保存: {} ({},{},{}) 大小: {} bytes",
                    layerName, z, x, y, data.length);
        }
        return result;
    }

    @Override
    public boolean deleteLayerCache(String layerName) {
        if (!enabled) {
            return false;
        }

        // 关闭并移除连接
        LayerCacheHolder holder = layerCaches.remove(layerName);
        if (holder != null) {
            holder.close();
        }

        // 删除文件
        String dbPath = getDbPath(layerName);
        java.io.File file = new java.io.File(dbPath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                log.info("删除 MBTiles 缓存文件成功: {}", dbPath);
            } else {
                log.warn("删除 MBTiles 缓存文件失败: {}", dbPath);
            }
            return deleted;
        }

        log.debug("MBTiles 缓存文件不存在: {}", dbPath);
        return false;
    }

    @Override
    public boolean delete(String layerName, Integer z, Integer x) {
        if (!enabled) {
            return false;
        }

        LayerCacheHolder holder = layerCaches.get(layerName);
        if (holder == null) {
            log.debug("图层缓存不存在: {}", layerName);
            return false;
        }

        if (z == null) {
            // 删除整个图层
            return deleteLayerCache(layerName);
        } else if (x == null) {
            // 删除指定层级的所有瓦片
            return holder.deleteByZoom(z);
        } else {
            // 删除指定 x 目录下的所有瓦片
            return holder.deleteByZoomAndX(z, x);
        }
    }

    @Override
    public boolean delete(String layerName, int z, int x, int y, ImageMime format) {
        if (!enabled) {
            return false;
        }

        LayerCacheHolder holder = layerCaches.get(layerName);
        if (holder == null) {
            return false;
        }

        return holder.delete(z, x, y);
    }

    @Override
    public void clearAll() {
        if (!enabled) {
            return;
        }

        // 关闭所有连接
        for (LayerCacheHolder holder : layerCaches.values()) {
            holder.close();
        }
        layerCaches.clear();

        // 删除所有 .mbtiles 文件
        try {
            java.io.File dir = new java.io.File(cacheRoot);
            if (dir.exists() && dir.isDirectory()) {
                java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".mbtiles"));
                if (files != null) {
                    for (java.io.File file : files) {
                        if (file.delete()) {
                            log.debug("删除 MBTiles 文件: {}", file.getName());
                        }
                    }
                }
                log.info("清空所有 MBTiles 缓存完成");
            }
        } catch (Exception e) {
            log.error("清空 MBTiles 缓存失败", e);
        }
    }

    @Override
    public long getTotalSize() {
        if (!enabled) {
            return 0;
        }

        long totalSize = 0;
        try {
            java.io.File dir = new java.io.File(cacheRoot);
            if (dir.exists() && dir.isDirectory()) {
                java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".mbtiles"));
                if (files != null) {
                    for (java.io.File file : files) {
                        totalSize += file.length();
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取 MBTiles 缓存总大小失败", e);
        }
        return totalSize;
    }

    @Override
    public boolean exists(String layerName, int z, int x, int y, ImageMime format) {
        if (!enabled) {
            return false;
        }

        LayerCacheHolder holder = layerCaches.get(layerName);
        if (holder == null) {
            return false;
        }

        return holder.exists(z, x, y);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 关闭所有缓存连接
     */
    public void close() {
        for (LayerCacheHolder holder : layerCaches.values()) {
            holder.close();
        }
        layerCaches.clear();
        log.info("所有 MBTiles 缓存连接已关闭");
    }

    /**
     * 获取指定图层的瓦片数量
     */
    public long getTileCount(String layerName) {
        LayerCacheHolder holder = layerCaches.get(layerName);
        if (holder == null) {
            return 0;
        }
        return holder.getTileCount();
    }

    /**
     * 获取指定图层指定层级的瓦片数量
     */
    public long getTileCountByZoom(String layerName, int zoom) {
        LayerCacheHolder holder = layerCaches.get(layerName);
        if (holder == null) {
            return 0;
        }
        return holder.getTileCountByZoom(zoom);
    }

    // ==================== 内部类 ====================

    /**
     * 单个图层的缓存持有者（Druid 连接池 - 读写分离）
     */
    @Slf4j
    private static class LayerCacheHolder {
        private final String dbPath;
        private final boolean needReverseY;
        private final DruidDataSource readDataSource;
        private final DruidDataSource writeDataSource;
        private int maxReadPoolSize = 20;
        private int maxWritePoolSize = 5;
        private int minIdle = 2;
        private final AtomicBoolean initialized = new AtomicBoolean(false);

        public LayerCacheHolder(String dbPath, boolean needReverseY, int maxReadPoolSize, int maxWritePoolSize, int minIdle) {
            this.dbPath = dbPath;
            this.needReverseY = needReverseY;
            this.maxReadPoolSize = maxReadPoolSize;
            this.maxWritePoolSize = maxWritePoolSize;
            this.minIdle = minIdle;
            this.readDataSource = createDataSource(true);
            this.writeDataSource = createDataSource(false);
            init();
        }

        /**
         * 创建 Druid 数据源
         *
         * @param readOnly 是否只读
         * @return DruidDataSource
         */
        private DruidDataSource createDataSource(boolean readOnly) {
            DataSourceDruidFastCreate dataSourceDruidFastCreate = new DataSourceDruidFastCreate();
            dataSourceDruidFastCreate.setUrl("jdbc:sqlite:" + dbPath);
            dataSourceDruidFastCreate.setConfigurator(dataSource -> {
                // 连接池大小配置
                if (readOnly) {
                    dataSource.setMaxActive(maxReadPoolSize);
                    dataSource.setInitialSize(minIdle);
                } else {
                    dataSource.setMaxActive(maxWritePoolSize);
                    dataSource.setInitialSize(1);
                }
                dataSource.setMinIdle(minIdle);
                // 连接有效性检测
                dataSource.setValidationQuery("SELECT 1");

                // SQLite 特定配置
                dataSource.setConnectionInitSqls(java.util.Arrays.asList(
                        "PRAGMA journal_mode=WAL",
                        "PRAGMA synchronous=" + (readOnly ? "NORMAL" : "FULL"),
                        "PRAGMA cache_size=10000",
                        "PRAGMA temp_store=MEMORY",
                        "PRAGMA mmap_size=268435456"  // 256MB
                ));

                // 连接属性
                java.util.Properties properties = new java.util.Properties();
                properties.setProperty("journal_mode", "WAL");
                properties.setProperty("synchronous", readOnly ? "NORMAL" : "FULL");
                properties.setProperty("cache_size", "10000");
                dataSource.setConnectProperties(properties);

                // 监控配置
                dataSource.setName("Druid-MBTiles-" + (readOnly ? "Read" : "Write") + "-" +
                        new java.io.File(dbPath).getName());
            });


            log.debug("创建   数据源: {}, readOnly: {}, maxActive: {}",
                    dbPath, readOnly, readOnly ? maxReadPoolSize : maxWritePoolSize);

            return (DruidDataSource) dataSourceDruidFastCreate.toDataSource();
        }

        /**
         * 初始化数据库
         */
        private void init() {
            if (!initialized.compareAndSet(false, true)) {
                return;
            }

            // 使用写连接初始化表结构
            try (Connection conn = writeDataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // 创建表
                stmt.execute(CREATE_TILES_TABLE_SQL);
                stmt.execute(CREATE_METADATA_TABLE_SQL);
                stmt.execute(CREATE_TILES_INDEX_SQL);

                // 初始化元数据
                initMetadata(conn);

                log.info("MBTiles 数据库初始化成功: {}", dbPath);

            } catch (SQLException e) {
                log.error("MBTiles 数据库初始化失败: {}", dbPath, e);
                initialized.set(false);
                throw new RuntimeException("MBTiles 初始化失败: " + dbPath, e);
            }
        }

        /**
         * 初始化元数据
         */
        private void initMetadata(Connection conn) {
            String[] metadata = {
                    "name", new java.io.File(dbPath).getName(),
                    "format", "png",
                    "version", "1.0",
                    "type", "overlay"
            };

            String sql = "INSERT OR IGNORE INTO metadata (name, value) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < metadata.length; i += 2) {
                    pstmt.setString(1, metadata[i]);
                    pstmt.setString(2, metadata[i + 1]);
                    pstmt.execute();
                }
            } catch (SQLException e) {
                log.warn("初始化元数据失败: {}", e.getMessage());
            }
        }

        /**
         * 检查初始化状态
         */
        private void checkInitialized() {
            if (!initialized.get()) {
                throw new IllegalStateException("MBTiles 未初始化: " + dbPath);
            }
        }

        /**
         * 读取瓦片数据（使用读连接池）
         */
        public byte[] get(int z, int x, int y) {
            checkInitialized();
            String sql = "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";

            try (Connection conn = readDataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, z);
                pstmt.setInt(2, x);
                pstmt.setInt(3, FuserCacheUtils.getStoreY(z, y, needReverseY));

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBytes("tile_data");
                    }
                }
            } catch (SQLException e) {
                log.error("读取瓦片失败: z={}, x={}, y={}, db={}", z, x, y, dbPath, e);
            }
            return null;
        }

        /**
         * 保存瓦片数据（使用写连接池）
         */
        public boolean put(int z, int x, int y, byte[] data) {
            checkInitialized();
            String sql = "INSERT OR REPLACE INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)";

            try (Connection conn = writeDataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, z);
                pstmt.setInt(2, x);
                pstmt.setInt(3, FuserCacheUtils.getStoreY(z, y, needReverseY));
                pstmt.setBytes(4, data);

                return pstmt.executeUpdate() > 0;

            } catch (SQLException e) {
                log.error("保存瓦片失败: z={}, x={}, y={}, db={}", z, x, y, dbPath, e);
                return false;
            }
        }


        /**
         * 删除瓦片（使用写连接池）
         */
        public boolean delete(int z, int x, int y) {
            checkInitialized();
            String sql = "DELETE FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";

            try (Connection conn = writeDataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, z);
                pstmt.setInt(2, x);
                pstmt.setInt(3, FuserCacheUtils.getStoreY(z, y, needReverseY));

                boolean result = pstmt.executeUpdate() > 0;
                if (result && log.isDebugEnabled()) {
                    log.debug("删除瓦片成功: z={}, x={}, y={}", z, x, y);
                }
                return result;

            } catch (SQLException e) {
                log.error("删除瓦片失败: z={}, x={}, y={}, db={}", z, x, y, dbPath, e);
                return false;
            }
        }

        /**
         * 按层级删除瓦片（使用写连接池）
         */
        public boolean deleteByZoom(int z) {
            checkInitialized();
            String sql = "DELETE FROM tiles WHERE zoom_level = ?";

            try (Connection conn = writeDataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, z);
                int count = pstmt.executeUpdate();
                log.debug("删除层级 {} 瓦片: {} 个, db={}", z, count, dbPath);
                return count > 0;

            } catch (SQLException e) {
                log.error("删除层级失败: z={}, db={}", z, dbPath, e);
                return false;
            }
        }

        /**
         * 按层级和列删除瓦片（使用写连接池）
         */
        public boolean deleteByZoomAndX(int z, int x) {
            checkInitialized();
            String sql = "DELETE FROM tiles WHERE zoom_level = ? AND tile_column = ?";

            try (Connection conn = writeDataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, z);
                pstmt.setInt(2, x);
                int count = pstmt.executeUpdate();
                log.debug("删除瓦片: z={}, x={}, 数量: {}, db={}", z, x, count, dbPath);
                return count > 0;

            } catch (SQLException e) {
                log.error("删除瓦片失败: z={}, x={}, db={}", z, x, dbPath, e);
                return false;
            }
        }

        /**
         * 检查瓦片是否存在（使用读连接池）
         */
        public boolean exists(int z, int x, int y) {
            checkInitialized();
            String sql = "SELECT 1 FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";

            try (Connection conn = readDataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, z);
                pstmt.setInt(2, x);
                pstmt.setInt(3, FuserCacheUtils.getStoreY(z, y, needReverseY));

                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next();
                }

            } catch (SQLException e) {
                log.error("检查瓦片存在失败: z={}, x={}, y={}, db={}", z, x, y, dbPath, e);
                return false;
            }
        }

        /**
         * 获取瓦片总数（使用读连接池）
         */
        public long getTileCount() {
            checkInitialized();
            String sql = "SELECT COUNT(*) FROM tiles";

            try (Connection conn = readDataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                if (rs.next()) {
                    return rs.getLong(1);
                }

            } catch (SQLException e) {
                log.error("获取瓦片数量失败: db={}", dbPath, e);
            }
            return 0;
        }

        /**
         * 获取指定层级的瓦片数量（使用读连接池）
         */
        public long getTileCountByZoom(int zoom) {
            checkInitialized();
            String sql = "SELECT COUNT(*) FROM tiles WHERE zoom_level = ?";

            try (Connection conn = readDataSource.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, zoom);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }

            } catch (SQLException e) {
                log.error("获取层级瓦片数量失败: z={}, db={}", zoom, dbPath, e);
            }
            return 0;
        }

        /**
         * 关闭连接池
         */
        public void close() {
            if (readDataSource != null && !readDataSource.isClosed()) {
                readDataSource.close();
                log.debug("读连接池已关闭: {}", dbPath);
            }
            if (writeDataSource != null && !writeDataSource.isClosed()) {
                writeDataSource.close();
                log.debug("写连接池已关闭: {}", dbPath);
            }
        }


    }
}
