package cn.geoair.map.tile.forge.fuser.converter;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.page.PageConditionDef;
import cn.geoair.map.dynamic.tools.page.PageConfig;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesUtils;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import lombok.Getter;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/24 17:05
 * @description： MBTiles 层级导入工具，支持从一个 MBTiles 导入指定层级到另一个 MBTiles
 * <p>
 * 功能：
 * 1. 导入单个层级
 * 2. 导入多个层级
 * 3. 导入所有层级
 * 4. 支持覆盖或跳过已存在的瓦片
 * 5. 支持图层名称映射
 */

public class MbtilesLayerImporter {
    private static GiLogger log = GirLoggerFactory.getLogger(MbtilesLayerImporter.class);

    /**
     * 导入配置
     */
    @Getter
    public static class ImportConfig {
        private String sourceMbtiles;           // 源 MBTiles 文件路径
        private String sourceLayerName;         // 源图层名称（如果为空则使用第一个图层）
        private String targetMbtiles;           // 目标 MBTiles 文件路径
        private String targetLayerName;         // 目标图层名称（如果为空则使用源图层名称）
        private List<Integer> zoomLevels;       // 要导入的层级列表（为空则导入所有层级）
        private boolean overwrite = false;      // 是否覆盖已存在的瓦片
        private int batchSize = 1000;           // 批量插入大小
        private int maxPoolSize = 20;           // 连接池大小
        private int minIdle = 2;                // 最小空闲连接数
        private boolean copyMetadata = true;    // 是否复制元数据

        public ImportConfig setSourceMbtiles(String sourceMbtiles) {
            this.sourceMbtiles = sourceMbtiles;
            return this;
        }

        public ImportConfig setSourceLayerName(String sourceLayerName) {
            this.sourceLayerName = sourceLayerName;
            return this;
        }

        public ImportConfig setTargetMbtiles(String targetMbtiles) {
            this.targetMbtiles = targetMbtiles;
            return this;
        }

        public ImportConfig setTargetLayerName(String targetLayerName) {
            this.targetLayerName = targetLayerName;
            return this;
        }

        public ImportConfig setZoomLevels(List<Integer> zoomLevels) {
            this.zoomLevels = zoomLevels;
            return this;
        }

        public ImportConfig setOverwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return this;
        }

        public ImportConfig setBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public ImportConfig setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public ImportConfig setMinIdle(int minIdle) {
            this.minIdle = minIdle;
            return this;
        }

        public ImportConfig setCopyMetadata(boolean copyMetadata) {
            this.copyMetadata = copyMetadata;
            return this;
        }

    }

    /**
     * 导入结果
     */
    @Getter
    public static class ImportResult {
        private String sourceMbtiles;
        private String sourceLayerName;
        private String targetMbtiles;
        private String targetLayerName;
        private List<Integer> importedZoomLevels = new ArrayList<>();
        private long totalTiles;
        private long successTiles;
        private long skippedTiles;
        private long failedTiles;
        private long costTime;


        @Override
        public String toString() {
            return String.format("ImportResult{source='%s', target='%s', layers=[%s->%s], " +
                            "totalTiles=%d, successTiles=%d, skippedTiles=%d, failedTiles=%d, costTime=%dms}",
                    sourceMbtiles, targetMbtiles, sourceLayerName, targetLayerName,
                    totalTiles, successTiles, skippedTiles, failedTiles, costTime);
        }
    }

    /**
     * 便捷方法：导入单个层级
     */
    public static ImportResult importZoomLevel(String sourceMbtiles, String targetMbtiles,
                                               String layerName, int zoomLevel) {
        ImportConfig config = new ImportConfig()
                .setSourceMbtiles(sourceMbtiles)
                .setTargetMbtiles(targetMbtiles)
                .setSourceLayerName(layerName)
                .setTargetLayerName(layerName)
                .setZoomLevels(ListUtil.of(zoomLevel));
        return importLayers(config);
    }

    /**
     * 便捷方法：导入多个层级
     */
    public static ImportResult importZoomLevels(String sourceMbtiles, String targetMbtiles,
                                                String layerName, List<Integer> zoomLevels) {
        ImportConfig config = new ImportConfig()
                .setSourceMbtiles(sourceMbtiles)
                .setTargetMbtiles(targetMbtiles)
                .setSourceLayerName(layerName)
                .setTargetLayerName(layerName)
                .setZoomLevels(zoomLevels);
        return importLayers(config);
    }

    /**
     * 便捷方法：导入所有层级
     */
    public static ImportResult importAllZoomLevels(String sourceMbtiles, String targetMbtiles,
                                                   String layerName) {
        ImportConfig config = new ImportConfig()
                .setSourceMbtiles(sourceMbtiles)
                .setTargetMbtiles(targetMbtiles)
                .setSourceLayerName(layerName)
                .setTargetLayerName(layerName);
        return importLayers(config);
    }

    /**
     * 便捷方法：导入并覆盖已存在的瓦片
     */
    public static ImportResult importOverwrite(String sourceMbtiles, String targetMbtiles,
                                               String sourceLayerName, String targetLayerName) {
        ImportConfig config = new ImportConfig()
                .setSourceMbtiles(sourceMbtiles)
                .setTargetMbtiles(targetMbtiles)
                .setSourceLayerName(sourceLayerName)
                .setTargetLayerName(targetLayerName)
                .setOverwrite(true);
        return importLayers(config);
    }

    /**
     * 执行导入
     */
    public static ImportResult importLayers(ImportConfig config) {
        long startTime = System.currentTimeMillis();

        // 参数校验
        if (!validateConfig(config)) {
            return null;
        }

        ImportResult result = new ImportResult();
        result.sourceMbtiles = config.getSourceMbtiles();
        result.sourceLayerName = config.getSourceLayerName();
        result.targetMbtiles = config.getTargetMbtiles();
        result.targetLayerName = config.getTargetLayerName();

        // 确保目标 MBTiles 文件所在的目录存在
        MbtilesUtils.ensureDirectoryExists(config.getTargetMbtiles());

        // 创建数据源
        DruidDataSource sourceDataSource = null;
        DruidDataSource targetDataSource = null;

        try {
            // 连接源数据库（只读）
            sourceDataSource = MbtilesUtils.createDataSource(
                    config.getSourceMbtiles(),
                    true,
                    config.getMaxPoolSize(),
                    config.getMinIdle()
            );

            // 连接目标数据库（读写）
            targetDataSource = MbtilesUtils.createDataSource(
                    config.getTargetMbtiles(),
                    false,
                    config.getMaxPoolSize(),
                    config.getMinIdle()
            );

            // 初始化目标数据库（如果文件不存在则创建）
            if (!MbtilesUtils.initDatabase(targetDataSource)) {
                log.error("初始化目标数据库失败: {}", config.getTargetMbtiles());
                result.failedTiles = -1;
                return result;
            }

            // 获取源图层名称
            String sourceLayer = getLayerName(sourceDataSource, config.getSourceLayerName());
            if (sourceLayer == null) {
                log.error("源图层不存在: {}", config.getSourceLayerName());
                result.failedTiles = -1;
                return result;
            }
            result.sourceLayerName = sourceLayer;

            // 确定目标图层名称
            String targetLayer = config.getTargetLayerName();
            if (targetLayer == null || targetLayer.isEmpty()) {
                targetLayer = sourceLayer;
            }
            result.targetLayerName = targetLayer;

            // 复制元数据（可选）
            if (config.isCopyMetadata()) {
                copyMetadata(sourceDataSource, targetDataSource, sourceLayer, targetLayer);
            }

            // 获取要导入的层级列表
            List<Integer> zoomLevels = config.getZoomLevels();
            if (zoomLevels == null || zoomLevels.isEmpty()) {
                zoomLevels = getZoomLevels(sourceDataSource, sourceLayer);
            }

            if (zoomLevels.isEmpty()) {
                log.warn("源图层没有瓦片数据: {}", sourceLayer);
                result.costTime = System.currentTimeMillis() - startTime;
                return result;
            }

            log.info("开始导入层级: {}, 层级列表: {}, 源图层: {}, 目标图层: {}",
                    zoomLevels.size(), zoomLevels, sourceLayer, targetLayer);

            // 执行导入
            ImportStats stats = importTiles(sourceDataSource, targetDataSource,
                    sourceLayer, targetLayer, zoomLevels, config);

            // 填充结果
            result.totalTiles = stats.total;
            result.successTiles = stats.success;
            result.skippedTiles = stats.skipped;
            result.failedTiles = stats.failed;
            result.importedZoomLevels = zoomLevels;
            result.costTime = System.currentTimeMillis() - startTime;

            log.info("导入完成: {}", result);
            return result;

        } catch (Exception e) {
            log.error("导入失败", e);
            result.failedTiles = -1;
            return result;
        } finally {
            MbtilesUtils.closeDataSource(sourceDataSource);
            MbtilesUtils.closeDataSource(targetDataSource);
        }
    }

    /**
     * 验证配置
     */
    private static boolean validateConfig(ImportConfig config) {
        if (config.getSourceMbtiles() == null || config.getSourceMbtiles().isEmpty()) {
            log.error("源 MBTiles 路径不能为空");
            return false;
        }

        if (!FileUtil.exist(config.getSourceMbtiles())) {
            log.error("源 MBTiles 文件不存在: {}", config.getSourceMbtiles());
            return false;
        }

        if (config.getTargetMbtiles() == null || config.getTargetMbtiles().isEmpty()) {
            log.error("目标 MBTiles 路径不能为空");
            return false;
        }

        // 如果源和目标相同，需要检查是否会导致循环导入
        if (config.getSourceMbtiles().equals(config.getTargetMbtiles())) {
            log.warn("源和目标 MBTiles 文件相同，将导入到同一文件的不同图层");
        }

        return true;
    }

    /**
     * 获取图层名称
     */
    private static String getLayerName(DruidDataSource dataSource, String layerName) {
        if (layerName != null && !layerName.isEmpty()) {
            // 检查图层是否存在
            if (layerExists(dataSource, layerName)) {
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

    /**
     * 检查图层是否存在
     */
    private static boolean layerExists(DruidDataSource dataSource, String layerName) {
        String sql = "SELECT COUNT(*) FROM metadata WHERE name = 'name' AND value = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, layerName);
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
     * 获取所有层级
     */
    private static List<Integer> getZoomLevels(DruidDataSource dataSource, String layerName) {
        List<Integer> zoomLevels = new ArrayList<>();
        String sql = "SELECT DISTINCT zoom_level FROM tiles ORDER BY zoom_level";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                zoomLevels.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            log.error("获取层级列表失败", e);
        }
        return zoomLevels;
    }

    /**
     * 复制元数据
     */
    private static void copyMetadata(DruidDataSource sourceDataSource,
                                     DruidDataSource targetDataSource,
                                     String sourceLayer, String targetLayer) {
        // 检查目标图层是否已存在
        if (layerExists(targetDataSource, targetLayer)) {
            log.info("目标图层已存在，跳过元数据复制: {}", targetLayer);
            return;
        }

        String sql = "SELECT name, value FROM metadata WHERE name != 'name'";
        try (Connection sourceConn = sourceDataSource.getConnection();
             PreparedStatement pstmt = sourceConn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            // 先插入图层名称
            MbtilesUtils.initMetadata(targetDataSource,
                    "name", targetLayer,
                    "format", "png",
                    "version", "1.0",
                    "type", "overlay"
            );

            // 复制其他元数据
            String insertSql = "INSERT OR REPLACE INTO metadata (name, value) VALUES (?, ?)";
            try (Connection targetConn = targetDataSource.getConnection();
                 PreparedStatement insertStmt = targetConn.prepareStatement(insertSql)) {
                targetConn.setAutoCommit(false);

                while (rs.next()) {
                    String name = rs.getString(1);
                    String value = rs.getString(2);
                    // 跳过 name，因为我们已经设置了目标图层名称
                    if (!"name".equals(name)) {
                        insertStmt.setString(1, name);
                        insertStmt.setString(2, value);
                        insertStmt.addBatch();
                    }
                }

                insertStmt.executeBatch();
                targetConn.commit();
                log.info("元数据复制完成");
            }

        } catch (SQLException e) {
            log.error("复制元数据失败", e);
        }
    }

    /**
     * 导入统计
     */
    private static class ImportStats {
        long total = 0;
        long success = 0;
        long skipped = 0;
        long failed = 0;
    }

    /**
     * 导入瓦片数据
     */
    private static ImportStats importTiles(DruidDataSource sourceDataSource,
                                           DruidDataSource targetDataSource,
                                           String sourceLayer, String targetLayer,
                                           List<Integer> zoomLevels,
                                           ImportConfig config) {
        ImportStats stats = new ImportStats();

        // 构建查询和插入 SQL
        String selectSql = "SELECT tile_column, tile_row, tile_data FROM tiles WHERE zoom_level = ? LIMIT ? OFFSET ?";

        String insertSql = config.isOverwrite()
                ? "INSERT OR REPLACE INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)"
                : "INSERT OR IGNORE INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)";
        try {
            for (int zoom : zoomLevels) {
                log.info("开始导入层级: z={}", zoom);

                final long[] layerCount = {0};
                List<Object[]> batchArgs = new ArrayList<>(config.getBatchSize());
                long tileCountByZoom = MbtilesUtils.getTileCountByZoom(sourceDataSource, zoom);
                GirAdvTools.getPageActuatorOpt(new PageConditionDef<Object[]>() {
                    @Override
                    public Long getTotalRecordCount() {
                        return tileCountByZoom;
                    }

                    @Override
                    public void setPageConfig(PageConfig pageConfig) {
                        pageConfig.setPageSize((long) config.getBatchSize())
                                .setParallelConsumeRecordIs(true)
                                .setParallelExecPageIs(true)
                                .setPageNumStartByZero(true);
                    }

                    @Override
                    public List<Object[]> getPageRecords(Integer pageNo, Integer pageSize) {
                        Connection sourceConn = null;
                        try {
                            int offset = pageNo * pageSize;
                            sourceConn = sourceDataSource.getConnection();
                            PreparedStatement selectStmt = sourceConn.prepareStatement(selectSql);
                            selectStmt.setInt(1, zoom);
                            selectStmt.setInt(2, pageSize);
                            selectStmt.setInt(3, offset);
                            try (ResultSet rs = selectStmt.executeQuery()) {
                                while (rs.next()) {
                                    int x = rs.getInt(1);
                                    int y = rs.getInt(2);
                                    byte[] data = rs.getBytes(3);
                                    if (data == null || data.length == 0) {
                                        stats.failed++;
                                        continue;
                                    }
                                    batchArgs.add(new Object[]{zoom, x, y, data});
                                    stats.total++;
                                    layerCount[0]++;
                                }
                            }
                            IoUtil.close(sourceConn);
                            int[] results = executeBatch(targetDataSource, insertSql, batchArgs);
                            stats.success += results[0];
                            stats.skipped += results[1];
                            stats.failed += results[2];
                            log.info("导入成功{}条，批次：{},总成功数量：{}",batchArgs.size(),pageNo+1,stats.success);
                            batchArgs.clear();
                            return Collections.emptyList();
                        } catch (Exception e) {
                            log.info(e.getMessage());
                        } finally {
                            if (sourceConn != null) {
                                IoUtil.close(sourceConn);
                            }
                        }
                        return Collections.emptyList();
                    }
                }).execute();
                // 执行剩余的批量插入
                if (!batchArgs.isEmpty()) {
                    int[] results = executeBatch(targetDataSource, insertSql, batchArgs);
                    stats.success += results[0];
                    stats.skipped += results[1];
                    stats.failed += results[2];
                    batchArgs.clear();
                }
            }

        } catch (Exception e) {
            log.error("导入瓦片数据失败", e);
        }

        return stats;
    }

    private static int[] executeBatch(DruidDataSource targetDataSource, String sql, List<Object[]> batchArgs) {
        if (batchArgs.isEmpty()) {
            return new int[]{0, 0, 0};
        }

        int success = 0;
        int skipped = 0;
        int failed = 0;

        try (DruidPooledConnection conn = targetDataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Object[] args : batchArgs) {
                    pstmt.setInt(1, (Integer) args[0]);
                    pstmt.setInt(2, (Integer) args[1]);
                    pstmt.setInt(3, (Integer) args[2]);
                    pstmt.setBytes(4, (byte[]) args[3]);
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
                failed = batchArgs.size();
            }
        } catch (SQLException e) {
            log.error("获取数据库连接失败", e);
            failed = batchArgs.size();
        }

        return new int[]{success, skipped, failed};
    }

    public static void main(String[] args) {
        // ==================== 1. 最简单的用法 ====================
        // 导入单个层级
        MbtilesLayerImporter.ImportResult result1 = MbtilesLayerImporter.importZoomLevel(
                "D:/mbtiles/source.mbtiles",      // 源文件
                "D:/mbtiles/target.mbtiles",      // 目标文件
                "imagery",                         // 图层名称
                5                                  // 层级
        );
        System.out.println("导入结果: " + result1);

        // ==================== 2. 导入多个层级 ====================
        List<Integer> zoomLevels = Arrays.asList(0, 1, 2, 3, 4, 5);
        MbtilesLayerImporter.ImportResult result2 = MbtilesLayerImporter.importZoomLevels(
                "D:/mbtiles/source.mbtiles",
                "D:/mbtiles/target.mbtiles",
                "imagery",
                zoomLevels
        );
        System.out.println("导入结果: " + result2);

        // ==================== 3. 导入所有层级 ====================
        MbtilesLayerImporter.ImportResult result3 = MbtilesLayerImporter.importAllZoomLevels(
                "D:/mbtiles/source.mbtiles",
                "D:/mbtiles/target.mbtiles",
                "imagery"
        );
        System.out.println("导入结果: " + result3);

        // ==================== 4. 导入并覆盖已有瓦片 ====================
        MbtilesLayerImporter.ImportResult result4 = MbtilesLayerImporter.importOverwrite(
                "D:/mbtiles/source.mbtiles",
                "D:/mbtiles/target.mbtiles",
                "source_layer",      // 源图层名
                "target_layer"       // 目标图层名（可以不同）
        );
        System.out.println("导入结果: " + result4);

        // ==================== 5. 完整配置（推荐） ====================
        MbtilesLayerImporter.ImportConfig config = new MbtilesLayerImporter.ImportConfig()
                .setSourceMbtiles("D:/mbtiles/source.mbtiles")
                .setSourceLayerName("imagery")
                .setTargetMbtiles("D:/mbtiles/target.mbtiles")
                .setTargetLayerName("imagery_backup")
                .setZoomLevels(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8))
                .setOverwrite(true)                // 覆盖已存在的瓦片
                .setBatchSize(2000)                // 批量插入大小
                .setCopyMetadata(true)             // 复制元数据
                .setMaxPoolSize(20)                // 连接池大小
                .setMinIdle(2);                    // 最小空闲连接数

        MbtilesLayerImporter.ImportResult result5 = MbtilesLayerImporter.importLayers(config);
        System.out.println("导入结果: " + result5);

        // ==================== 6. 同一文件不同图层之间导入 ====================
        // 从同一个 MBTiles 文件的 layer1 导入到 layer2
        MbtilesLayerImporter.ImportResult result6 = MbtilesLayerImporter.importLayers(
                new MbtilesLayerImporter.ImportConfig()
                        .setSourceMbtiles("D:/mbtiles/merged.mbtiles")
                        .setSourceLayerName("layer1")
                        .setTargetMbtiles("D:/mbtiles/merged.mbtiles")
                        .setTargetLayerName("layer2")
                        .setZoomLevels(Arrays.asList(0, 1, 2, 3))
                        .setOverwrite(false)        // 不覆盖，跳过已存在的
        );
        System.out.println("导入结果: " + result6);
    }
}
