package cn.geoair.map.tile.forge.fuser.cache;

import cn.geoair.base.runtime.GutilShutdownHook;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;

import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import cn.geoair.map.tile.forge.fuser.utils.MbtilesUtils;
import cn.hutool.core.io.FileUtil;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
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

    LayerCacheHolder NULL_OBJ = new LayerCacheHolder();

    private final ConcurrentHashMap<String, LayerCacheHolder> layerCaches = new ConcurrentHashMap<>();

    private int maxReadPoolSize = 20;
    private int maxWritePoolSize = 20;
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
                File dir = new File(this.cacheRoot);
                if (!dir.exists()) {
                    if (dir.mkdirs()) {
                        log.info("创建缓存根目录: {}", this.cacheRoot);
                    }
                }
                log.info("MBTiles 缓存初始化完成，根目录: {}", this.cacheRoot);
            } catch (Exception e) {
                log.error("创建缓存根目录失败: {}", this.cacheRoot, e);
            }
        }
        GutilShutdownHook.getInstance().registerTask(this::close);
    }

    /**
     * 获取图层对应的 MBTiles 文件路径
     *
     * @param layerName 图层名称
     * @return 文件路径
     */
    private String getDbPath(String layerName) {
        String safeName = MbtilesUtils.getSafeFileName(layerName);
        return cacheRoot + safeName + ".mbtiles";
    }

    /**
     * 获取或创建图层的缓存持有者
     */
    private LayerCacheHolder getOrCreateHolder(String layerName) {
        return getOrCreateHolder(layerName, false);
    }

    /**
     * 获取或创建图层的缓存持有者
     */
    private LayerCacheHolder getOrCreateHolder(String layerName, boolean initIs) {
        if (!enabled) {
            return null;
        }

        try {

            LayerCacheHolder result = layerCaches.compute(layerName, (key, oldValue) -> {
                // 情况1: initIs 为 true 且旧值为 NULL_OBJ，需要重建
                if (initIs && oldValue == NULL_OBJ) {
                    try {
                        String dbPath = getDbPath(key);
                        log.debug("重建图层缓存: {}", key);
                        return new LayerCacheHolder(
                                dbPath,
                                FuserCacheUtils.isNeedReverseY(key),
                                maxReadPoolSize,
                                maxWritePoolSize,
                                minIdle
                        );
                    } catch (Exception e) {
                        log.error("重建图层缓存失败: {}", key, e);
                        return null;
                    }
                }

                // 情况2: 旧值不存在，需要创建
                if (oldValue == null) {
                    try {
                        String dbPath = getDbPath(key);
                        if (FileUtil.exist(dbPath)) {
                            log.debug("创建图层缓存: {}", key);
                            return new LayerCacheHolder(
                                    dbPath,
                                    FuserCacheUtils.isNeedReverseY(key),
                                    maxReadPoolSize,
                                    maxWritePoolSize,
                                    minIdle
                            );
                        } else {
                            log.warn("图层缓存文件不存在: {}", dbPath);
                            return NULL_OBJ;
                        }
                    } catch (Exception e) {
                        log.error("创建图层缓存失败: {}", key, e);
                        return null;
                    }
                }

                // 情况3: 旧值存在，直接返回
                return oldValue;
            });

            // 判断返回结果
            if (result == null || result == NULL_OBJ) {
                return null;
            }
            return result;

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

        LayerCacheHolder holder = getOrCreateHolder(layerName, false);
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

        LayerCacheHolder holder = getOrCreateHolder(layerName, true);
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
        return MbtilesUtils.deleteFile(dbPath);
    }

    @Override
    public boolean delete(String layerName, Integer z, Integer x) {
        if (!enabled) {
            return false;
        }

        LayerCacheHolder holder = getOrCreateHolder(layerName);
        if (holder == null) {
            log.debug("图层缓存不存在: {}", layerName);
            return false;
        }

        if (z == null) {
            // 删除整个图层
            return holder.truncateTable();
        } else if (x == null) {
            // 删除指定层级的所有瓦片
            return holder.deleteByZoom(z) > 0;
        } else {
            // 删除指定 x 目录下的所有瓦片
            return holder.deleteByZoomAndX(z, x) > 0;
        }
    }

    @Override
    public boolean delete(String layerName, int z, int x, int y, ImageMime format) {
        if (!enabled) {
            return false;
        }

        LayerCacheHolder holder = getOrCreateHolder(layerName);
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
            File dir = new File(cacheRoot);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.endsWith(".mbtiles"));
                if (files != null) {
                    for (File file : files) {
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
            File dir = new File(cacheRoot);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.endsWith(".mbtiles"));
                if (files != null) {
                    for (File file : files) {
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

        LayerCacheHolder holder = getOrCreateHolder(layerName);
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
        LayerCacheHolder holder = getOrCreateHolder(layerName);
        if (holder == null) {
            return 0;
        }
        return holder.getTileCount();
    }

    /**
     * 获取指定图层指定层级的瓦片数量
     */
    public long getTileCountByZoom(String layerName, int zoom) {
        LayerCacheHolder holder = getOrCreateHolder(layerName);
        if (holder == null) {
            return 0;
        }
        return holder.getTileCountByZoom(zoom);
    }

    // ==================== 内部类 ====================

    /**
     * 单个图层的缓存持有者（连接池 - 读写分离）
     */
    @Slf4j
    private static class LayerCacheHolder {
        private String dbPath;
        private boolean needReverseY;
        private DruidDataSource readDataSource;
        private DruidDataSource writeDataSource;
        private AtomicBoolean initialized = new AtomicBoolean(false);

        public LayerCacheHolder() {
        }

        public LayerCacheHolder(String dbPath, boolean needReverseY, int maxReadPoolSize, int maxWritePoolSize, int minIdle) {
            this.dbPath = dbPath;
            this.needReverseY = needReverseY;

            // 确保目录存在
            MbtilesUtils.ensureDirectoryExists(dbPath);

            // 创建读写数据源
            this.readDataSource = MbtilesUtils.createDataSource(
                    dbPath,
                    true,   // 只读
                    maxReadPoolSize,
                    minIdle
            );
            this.writeDataSource = MbtilesUtils.createDataSource(
                    dbPath,
                    false,  // 可读写
                    maxWritePoolSize,
                    Math.min(minIdle, maxWritePoolSize)
            );

            init();
        }

        /**
         * 初始化数据库
         */
        private void init() {
            if (!initialized.compareAndSet(false, true)) {
                return;
            }

            // 使用写连接初始化表结构
            if (MbtilesUtils.initDatabase(writeDataSource)) {
                // 初始化元数据
                boolean metadataInit = MbtilesUtils.initMetadata(writeDataSource,
                        "name", new File(dbPath).getName(),
                        "format", "png",
                        "version", "1.0",
                        "type", "overlay"
                );

                if (metadataInit) {
                    log.info("MBTiles 数据库初始化成功: {}", dbPath);
                } else {
                    log.warn("MBTiles 数据库初始化成功，但元数据初始化失败: {}", dbPath);
                }
            } else {
                log.error("MBTiles 数据库初始化失败: {}", dbPath);
                initialized.set(false);
                throw new RuntimeException("MBTiles 初始化失败: " + dbPath);
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
            int storeY = FuserCacheUtils.getStoreY(z, y, needReverseY);
            return MbtilesUtils.getTile(readDataSource, z, x, storeY);
        }

        /**
         * 保存瓦片数据（使用写连接池）
         */
        public boolean put(int z, int x, int y, byte[] data) {
            checkInitialized();
            int storeY = FuserCacheUtils.getStoreY(z, y, needReverseY);
            return MbtilesUtils.putTile(writeDataSource, z, x, storeY, data);
        }

        /**
         * 删除瓦片（使用写连接池）
         */
        public boolean delete(int z, int x, int y) {
            checkInitialized();
            int storeY = FuserCacheUtils.getStoreY(z, y, needReverseY);
            boolean result = MbtilesUtils.deleteTile(writeDataSource, z, x, storeY);
            log.info("删除瓦片成功: z={}, x={}, y={}, db={}", z, x, y, dbPath);
            return result;
        }

        /**
         * 按层级删除瓦片（使用写连接池）
         */
        public int deleteByZoom(int z) {
            checkInitialized();
            int count = MbtilesUtils.deleteTilesByZoom(writeDataSource, z);
            log.info("删除层级 {} 瓦片: {} 个, db={}", z, count, dbPath);
            return count;
        }

        /**
         * 清空所有瓦片
         */
        public boolean truncateTable() {
            checkInitialized();
            int count = MbtilesUtils.truncateTiles(writeDataSource);
            log.info("清空所有瓦片: {} 个, db={}", count, dbPath);
            return count > 0;
        }

        /**
         * 按层级和列删除瓦片（使用写连接池）
         */
        public int deleteByZoomAndX(int z, int x) {
            checkInitialized();
            int count = MbtilesUtils.deleteTilesByZoomAndX(writeDataSource, z, x);
            log.info("删除瓦片: z={}, x={}, 数量: {}, db={}", z, x, count, dbPath);
            return count;
        }

        /**
         * 检查瓦片是否存在（使用读连接池）
         */
        public boolean exists(int z, int x, int y) {
            checkInitialized();
            int storeY = FuserCacheUtils.getStoreY(z, y, needReverseY);
            return MbtilesUtils.existsTile(readDataSource, z, x, storeY);
        }

        /**
         * 获取瓦片总数（使用读连接池）
         */
        public long getTileCount() {
            checkInitialized();
            return MbtilesUtils.getTileCount(readDataSource);
        }

        /**
         * 获取指定层级的瓦片数量（使用读连接池）
         */
        public long getTileCountByZoom(int zoom) {
            checkInitialized();
            return MbtilesUtils.getTileCountByZoom(readDataSource, zoom);
        }

        /**
         * 关闭连接池
         */
        public void close() {
            MbtilesUtils.closeDataSource(readDataSource);
            MbtilesUtils.closeDataSource(writeDataSource);
            log.debug("连接池已关闭: {}", dbPath);
        }
    }
}
