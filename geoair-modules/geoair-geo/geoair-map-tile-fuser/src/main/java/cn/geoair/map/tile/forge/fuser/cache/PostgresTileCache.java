package cn.geoair.map.tile.forge.fuser.cache;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.adv.GirAdvQuery;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import cn.geoair.web.mime.GiMimeType;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

/**
 * PostgreSQL 瓦片缓存实现
 *
 * <p>每个图层独立一张表，表名格式: tile_cache_fuser_{layerName} 缓存的结果，全部转换成wmts原点
 *
 * @author 张逢吉
 * @date Created in 2026/06/22
 * @description 基于 PostgreSQL 的瓦片缓存实现
 */
public class PostgresTileCache implements TileCache {
    private static GiLogger log = GirLoggerFactory.getLogger();
    private final DataSource dataSource;
    private final boolean enabled;
    private final String tablePrefix;
    private final ConcurrentHashMap<String, TableCacheHolder> layerCaches =
            new ConcurrentHashMap<>();

    // 表结构
    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS %s ("
                    + "  z INTEGER NOT NULL,"
                    + "  x INTEGER NOT NULL,"
                    + "  y INTEGER NOT NULL,"
                    + "  tile_data BYTEA NOT NULL,"
                    + "  format VARCHAR(50),"
                    + "  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "  PRIMARY KEY (z, x, y)"
                    + ")";

    // 索引
    private static final String CREATE_INDEX_ZOOM_SQL =
            "CREATE INDEX IF NOT EXISTS idx_%s_zoom ON %s(z)";

    private static final String CREATE_INDEX_UPDATE_TIME_SQL =
            "CREATE INDEX IF NOT EXISTS idx_%s_update_time ON %s(update_time)";

    /**
     * 构造函数
     *
     * @param dataSource 数据源
     */
    public PostgresTileCache(DataSource dataSource) {
        this(dataSource, "tile_cache_fuser_", true);
    }

    /**
     * 构造函数
     *
     * @param dataSource 数据源
     * @param enabled 是否启用缓存
     */
    public PostgresTileCache(DataSource dataSource, boolean enabled) {
        this(dataSource, "tile_cache_fuser_", enabled);
    }

    /**
     * 构造函数
     *
     * @param dataSource 数据源
     * @param tablePrefix 表前缀
     * @param enabled 是否启用缓存
     */
    public PostgresTileCache(DataSource dataSource, String tablePrefix, boolean enabled) {
        this.dataSource = dataSource;
        this.tablePrefix = tablePrefix;
        this.enabled = enabled;
        log.info("PostgreSQL 瓦片缓存初始化完成，表前缀: {}", tablePrefix);
    }

    /** 获取图层对应的表名 */
    private String getTableName(String layerName) {
        // 过滤非法字符，保证表名安全
        String safeName = layerName.replaceAll("[^a-zA-Z0-9_]", "_");
        // 如果以数字开头，加上前缀
        if (safeName.matches("^[0-9].*")) {
            safeName = "t_" + safeName;
        }
        return tablePrefix + safeName;
    }

    /** 获取或创建图层的缓存持有者 */
    private TableCacheHolder getOrCreateHolder(String layerName) {
        if (!enabled) {
            return null;
        }

        return layerCaches.computeIfAbsent(
                layerName,
                k -> {
                    try {
                        String tableName = getTableName(k);
                        boolean needReverse = FuserCacheUtils.fileCheckIsNeedReverseY(k);
                        return new TableCacheHolder(
                                dataSource,
                                tableName,
                                needReverse,
                                FuserCacheUtils.getCacheGridSrid(k));
                    } catch (Exception e) {
                        log.error("创建图层缓存失败: {}", layerName, e);
                        return null;
                    }
                });
    }

    @Override
    public byte[] get(String layerName, int z, int x, int y, GiMimeType format) {
        if (!enabled) {
            return null;
        }

        TableCacheHolder holder = getOrCreateHolder(layerName);
        if (holder == null) {
            return null;
        }

        long startTime = System.currentTimeMillis();
        byte[] data = holder.get(z, x, y);
        if (data != null && log.isDebugEnabled()) {
            log.debug(
                    "PostgreSQL 缓存命中: {} ({},{},{}) 耗时: {}ms",
                    layerName,
                    z,
                    x,
                    y,
                    System.currentTimeMillis() - startTime);
        }
        return data;
    }

    @Override
    public boolean put(String layerName, int z, int x, int y, byte[] data, GiMimeType format) {
        if (!enabled || data == null || data.length == 0) {
            return false;
        }

        TableCacheHolder holder = getOrCreateHolder(layerName);
        if (holder == null) {
            return false;
        }

        boolean result = holder.put(z, x, y, data, format.getFormat());
        if (result && log.isDebugEnabled()) {
            log.debug(
                    "PostgreSQL 缓存保存: {} ({},{},{}) 大小: {} bytes", layerName, z, x, y, data.length);
        }
        return result;
    }

    @Override
    public boolean deleteLayerCache(String layerName) {
        if (!enabled) {
            return false;
        }

        TableCacheHolder holder = layerCaches.remove(layerName);
        if (holder != null) {
            holder.dropTable();
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(String layerName, Integer z, Integer x) {
        if (!enabled) {
            return false;
        }

        TableCacheHolder holder = layerCaches.get(layerName);
        if (holder == null) {
            log.debug("图层缓存不存在: {}", layerName);
            return false;
        }

        if (z == null) {
            // 删除整个图层
            holder.truncateTable();
            return true;
        } else if (x == null) {
            // 删除指定层级的所有瓦片
            return holder.deleteByZoom(z);
        } else {
            // 删除指定 x 的所有瓦片
            return holder.deleteByZoomAndX(z, x);
        }
    }

    @Override
    public boolean delete(String layerName, int z, int x, int y, GiMimeType format) {
        if (!enabled) {
            return false;
        }

        TableCacheHolder holder = layerCaches.get(layerName);
        if (holder == null) {
            return false;
        }

        return holder.delete(z, x, y);
    }

    @Override
    public void clearAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getTotalSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(String layerName, int z, int x, int y, GiMimeType format) {
        if (!enabled) {
            return false;
        }

        TableCacheHolder holder = layerCaches.get(layerName);
        if (holder == null) {
            return false;
        }

        return holder.exists(z, x, y);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** 获取指定图层的瓦片数量 */
    public long getTileCount(String layerName) {
        TableCacheHolder holder = layerCaches.get(layerName);
        if (holder == null) {
            return 0;
        }
        return holder.getTileCount();
    }

    /** 获取指定图层指定层级的瓦片数量 */
    public long getTileCountByZoom(String layerName, int zoom) {
        TableCacheHolder holder = layerCaches.get(layerName);
        if (holder == null) {
            return 0;
        }
        return holder.getTileCountByZoom(zoom);
    }

    // ==================== 内部类 ====================

    /** 单个图层的缓存持有者 */
    private static class TableCacheHolder {
        private static GiLogger log = GirLoggerFactory.getLogger();
        private final IAdvExecutor iAdvExecutor;
        private final String tableName;
        private final boolean needReverseY;
        private final int gridSrid;
        private volatile boolean initialized = false;

        public TableCacheHolder(
                DataSource dataSource, String tableName, boolean needReverseY, int gridSrid) {
            this.iAdvExecutor = GirAdvQuery.getIAdvExecutor(dataSource);
            this.tableName = tableName;
            this.needReverseY = needReverseY;
            this.gridSrid = gridSrid;
            init();
        }

        private synchronized void init() {
            if (initialized) {
                return;
            }
            // 创建表
            if (!iAdvExecutor.dIsTableExists(tableName)) {
                String createSql = String.format(CREATE_TABLE_SQL, tableName);
                iAdvExecutor.dExecuteDDL(createSql, tableName, "创建表");

                // 创建索引
                String idxZoomSql = String.format(CREATE_INDEX_ZOOM_SQL, tableName, tableName);
                iAdvExecutor.dExecuteDDL(idxZoomSql, tableName, "创建索引");

                String idxUpdateSql =
                        String.format(CREATE_INDEX_UPDATE_TIME_SQL, tableName, tableName);
                iAdvExecutor.dExecuteDDL(idxUpdateSql, tableName, "创建索引");
            }
            initialized = true;
            log.debug("PostgreSQL 缓存表初始化完成: {}, needReverseY: {}", tableName, needReverseY);
        }

        public byte[] get(int z, int x, int y) {
            int storeY = FuserCacheUtils.getStoreY(z, y, needReverseY, gridSrid);
            GirAdvOneRow girAdvOneRow =
                    iAdvExecutor.bSelectOne(
                            "SELECT tile_data FROM "
                                    + tableName
                                    + " WHERE z = ? AND x = ? AND y = ?",
                            SqlParamList.of(z, x, storeY));
            return girAdvOneRow.getPrimitiveByteArray("tile_data");
        }

        public boolean put(int z, int x, int y, byte[] data, String format) {
            int storeY = FuserCacheUtils.getStoreY(z, y, needReverseY, gridSrid);

            String sql =
                    "INSERT INTO "
                            + tableName
                            + " (z, x, y, tile_data, format, update_time) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP) "
                            + "ON CONFLICT (z, x, y) DO UPDATE SET tile_data = EXCLUDED.tile_data, format = EXCLUDED.format, update_time = CURRENT_TIMESTAMP";

            return iAdvExecutor.bInsertBySql(sql, SqlParamList.of(z, x, storeY, data, format)) > 0;
        }

        public boolean delete(int z, int x, int y) {
            int storeY = FuserCacheUtils.getStoreY(z, y, needReverseY, gridSrid);
            String sql = "DELETE FROM " + tableName + " WHERE z = ? AND x = ? AND y = ?";
            boolean b = iAdvExecutor.bInsertBySql(sql, SqlParamList.of(z, x, storeY)) > 0;
            log.debug("删除瓦片成功: z={}, x={}, y={}", z, x, y);
            return b;
        }

        public boolean deleteByZoom(int z) {
            String sql = "DELETE FROM " + tableName + " WHERE z = ?";
            int count = iAdvExecutor.bDeleteBySql(sql, SqlParamList.of(z));
            log.debug("删除层级 {} 瓦片: {} 个", z, count);
            return count > 0;
        }

        public boolean deleteByZoomAndX(int z, int x) {
            String sql = "DELETE FROM " + tableName + " WHERE z = ? AND x = ?";
            int count = iAdvExecutor.bDeleteBySql(sql, SqlParamList.of(z, x));
            log.debug("删除瓦片: z={}, x={}, 数量: {}", z, x, count);
            return count > 0;
        }

        public boolean exists(int z, int x, int y) {
            int storeY = FuserCacheUtils.getStoreY(z, y, needReverseY, gridSrid);
            String sql =
                    "SELECT count(1) as count FROM "
                            + tableName
                            + " WHERE z = ? AND x = ? AND y = ?";
            GirAdvOneRow dvOneRow = iAdvExecutor.bSelectOne(sql, SqlParamList.of(z, x, storeY));
            Long count = dvOneRow.getLong("count");
            return count != null && count > 0;
        }

        public long getTileCount() {
            String sql = "SELECT count(1) as count FROM " + tableName;
            GirAdvOneRow dvOneRow = iAdvExecutor.bSelectOne(sql);
            return dvOneRow.getLong("count");
        }

        public long getTileCountByZoom(int zoom) {
            String sql = "SELECT count(1) as count FROM " + tableName + " WHERE z = ?";
            GirAdvOneRow dvOneRow = iAdvExecutor.bSelectOne(sql, SqlParamList.of(zoom));
            Long count = dvOneRow.getLong("count");
            return count != null ? count : 0;
        }

        public void truncateTable() {
            iAdvExecutor.dTruncateTable(tableName);
            initialized = false;
        }

        public void dropTable() {
            iAdvExecutor.dDropTable(tableName);
            initialized = false;
        }
    }
}
