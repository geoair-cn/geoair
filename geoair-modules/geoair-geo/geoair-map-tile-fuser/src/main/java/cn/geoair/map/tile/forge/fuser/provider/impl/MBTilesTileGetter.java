package cn.geoair.map.tile.forge.fuser.provider.impl;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.runtime.GutilShutdownHook;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.enums.OriginType;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesInfo;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesUtils;
import cn.geoair.map.tile.forge.fuser.provider.BaseTileGetter;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import com.alibaba.druid.pool.DruidDataSource;

/**
 * MBTiles 瓦片获取器（从 MBTiles 数据库读取瓦片）
 *
 * @author 张俊
 * @date Created in 2026/06/23
 */
public class MBTilesTileGetter extends BaseTileGetter {
    public static GiLogger log = GirLoggerFactory.getLogger();
    private final String mbtilesFilePath;
    private final DruidDataSource dataSource;
    private final boolean needReverseY;
    private volatile boolean initialized = false;

    public MBTilesTileGetter(PxyLayerInfo layerInfo) {
        super(layerInfo);

        String path = layerInfo.getPath();
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("MBTiles 文件路径不能为空");
        }

        this.mbtilesFilePath = path.trim();
        this.needReverseY = OriginType.fromMode(layerInfo.getOriginType()).isGoogle();

        // 检查文件是否存在
        if (!MbtilesUtils.existsFile(this.mbtilesFilePath)) {
            log.warn("MBTiles 文件不存在: {}", this.mbtilesFilePath);
        }

        // 创建数据源（只读模式）
        this.dataSource =
                MbtilesUtils.createDataSource(
                        this.mbtilesFilePath,
                        true, // 只读
                        10, // 最大连接数
                        2 // 最小空闲连接数
                        );

        // 初始化数据库
        initDatabase();
        log.info("MBTiles 瓦片获取器初始化完成: {}", this.mbtilesFilePath);
        GutilShutdownHook.getInstance().registerTask(this::close);
    }

    private void initDatabase() {
        if (initialized) {
            return;
        }

        synchronized (this) {
            if (initialized) {
                return;
            }

            if (!MbtilesUtils.existsFile(mbtilesFilePath)) {
                log.warn("MBTiles 文件不存在，将无法读取瓦片: {}", mbtilesFilePath);
                initialized = true;
                return;
            }

            // 检查 tiles 表是否存在
            if (MbtilesUtils.checkTilesTableExists(dataSource)) {
                long count = MbtilesUtils.getTileCount(dataSource);
                log.info("MBTiles 数据库已就绪: {}, 瓦片数量: {}", mbtilesFilePath, count);
            } else {
                log.warn("MBTiles 数据库缺少 tiles 表: {}", mbtilesFilePath);
            }

            initialized = true;
        }
    }

    @Override
    public Resource getTileResource(int z, int x, int y) {
        if (dataSource == null || dataSource.isClosed()) {
            log.error("MBTiles 数据源不可用: {}", mbtilesFilePath);
            return null;
        }

        if (!MbtilesUtils.existsFile(mbtilesFilePath)) {
            log.debug("MBTiles 文件不存在: {}", mbtilesFilePath);
            return null;
        }
        int storeY = FuserCacheUtils.getStoreY(z, y, needReverseY);
        long startTime = System.currentTimeMillis();
        MbtilesInfo tile = MbtilesUtils.getTile(dataSource, z, x, storeY);
        byte[] imageBytes = tile.getTileData();
        if (imageBytes != null && imageBytes.length > 0) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.debug(
                    "从 MBTiles 读取瓦片成功: {} ({},{},{}) 大小: {} bytes, 耗时: {}ms",
                    mbtilesFilePath,
                    z,
                    x,
                    y,
                    imageBytes.length,
                    elapsed);
            return new ByteArrayResource(imageBytes);
        }

        if (log.isDebugEnabled()) {
            log.debug("MBTiles 中未找到瓦片: {} ({},{},{}) storeY={}", mbtilesFilePath, z, x, y, storeY);
        }
        return null;
    }

    public void close() {
        MbtilesUtils.closeDataSource(dataSource);
        log.info("MBTiles 数据源已关闭: {}", mbtilesFilePath);
    }
}
