package cn.geoair.map.tile.forge.fuser.converter;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.runtime.GutilShutdownHook;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesInfo;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesUtils;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author 张俊
 * @date 2026/6/26
 * @description ZIP 瓦片导入工具，支持从 ZIP 包导入瓦片到 MBTiles
 */
public class MbtilesFromLocalZipConverter {

    private static GiLogger log = GirLoggerFactory.getLogger(MbtilesFromLocalZipConverter.class);
    private static final int BUF_SIZE = 8192;
    private static final int DEFAULT_BATCH_SIZE = 5000;

    /** 导入配置 */
    @Data
    @Accessors(chain = true)
    public static class ImportConfig {
        private String zipPath; // ZIP 文件路径
        private String targetMbtiles; // 目标 MBTiles 文件路径
        private String layerName; // 图层名称
        private String format = "png"; // 图片格式
        Boolean overwrite = false; // 存在是否覆盖
        private int batchSize = DEFAULT_BATCH_SIZE; // 批量插入大小
        private int maxPoolSize = 20; // 连接池大小
        private int minIdle = 2; // 最小空闲连接数
        private TileNameProcessor processor = TileNameProcessor.DEFAULT_PROCESSOR; // 文件名处理器
        private List<String> extensions = ListUtil.of(".png", ".jpg"); // 允许的扩展名

        /** ZIP 内单个瓦片的最大解压后字节数。 */
        private long maxEntryBytes = 10L * 1024 * 1024;

        /** ZIP 文件本身允许的最大字节数。 */
        private long maxZipFileBytes = 4L * 1024 * 1024 * 1024;

        /** ZIP 导入期间允许读取的最大解压后总字节数。 */
        private long maxTotalUncompressedBytes = 4L * 1024 * 1024 * 1024;

        /** ZIP 中允许的最大条目数（包含目录）。 */
        private int maxEntries = 1_000_000;

        /** 已知压缩大小时允许的最大压缩比。 */
        private int maxCompressionRatio = 1_000;
    }

    /** 导入结果 */
    @Data
    public static class ImportResult {
        private String zipPath;
        private String targetMbtiles;
        private String layerName;
        private long totalTiles; // 总瓦片数（有效）
        private long failedTiles; // 读取失败的瓦片数
        private long skipDir; // 跳过的目录数
        private long costTime; // 耗时（毫秒）
    }

    // ==================== 便捷方法 ====================

    /** 便捷方法：使用默认配置导入 */
    public static ImportResult importZip(String zipPath, String targetMbtiles, String layerName) {
        ImportConfig config =
                new ImportConfig()
                        .setZipPath(zipPath)
                        .setTargetMbtiles(targetMbtiles)
                        .setLayerName(layerName);
        return importZip(config);
    }

    /** 便捷方法：自定义处理器导入 */
    public static ImportResult importZip(
            String zipPath, String targetMbtiles, String layerName, TileNameProcessor processor) {
        ImportConfig config =
                new ImportConfig()
                        .setZipPath(zipPath)
                        .setTargetMbtiles(targetMbtiles)
                        .setLayerName(layerName)
                        .setProcessor(processor);
        return importZip(config);
    }

    /** 便捷方法：自定义批量大小 */
    public static ImportResult importZip(
            String zipPath, String targetMbtiles, String layerName, int batchSize) {
        ImportConfig config =
                new ImportConfig()
                        .setZipPath(zipPath)
                        .setTargetMbtiles(targetMbtiles)
                        .setLayerName(layerName)
                        .setBatchSize(batchSize);
        return importZip(config);
    }

    // ==================== 核心方法 ====================

    /** 执行 ZIP 导入 */
    public static ImportResult importZip(ImportConfig config) {
        long startTime = System.currentTimeMillis();

        // 参数校验
        if (!validateConfig(config)) {
            ImportResult errorResult = new ImportResult();
            errorResult.failedTiles = -1;
            errorResult.costTime = System.currentTimeMillis() - startTime;
            return errorResult;
        }
        if (FileUtil.size(new File(config.getZipPath())) > config.getMaxZipFileBytes()) {
            log.error("ZIP 文件超过大小限制: {}", config.getZipPath());
            ImportResult errorResult = new ImportResult();
            errorResult.failedTiles = -1;
            errorResult.costTime = System.currentTimeMillis() - startTime;
            return errorResult;
        }

        ImportResult result = new ImportResult();
        result.zipPath = config.getZipPath();
        result.targetMbtiles = config.getTargetMbtiles();
        result.layerName = config.getLayerName();

        DruidDataSource dataSource = null;
        try {
            // 确保目标目录存在
            MbtilesUtils.ensureDirectoryExists(config.getTargetMbtiles());

            // 创建数据源
            dataSource =
                    MbtilesUtils.createDataSource(
                            config.getTargetMbtiles(),
                            false,
                            config.getMaxPoolSize(),
                            config.getMinIdle());
            GutilShutdownHook.getInstance().registerTask(dataSource::close);
            // 初始化数据库
            if (!MbtilesUtils.initDatabase(dataSource)) {
                System.err.println("初始化目标数据库失败: " + config.getTargetMbtiles());
                result.failedTiles = -1;
                result.costTime = System.currentTimeMillis() - startTime;
                return result;
            }

            // 初始化元数据
            MbtilesUtils.initMetadata(
                    dataSource,
                    "name",
                    config.getLayerName(),
                    "format",
                    config.getFormat(),
                    "version",
                    "1.0",
                    "type",
                    "overlay");
            // 处理 ZIP 文件
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(config.getZipPath());
            } catch (IOException e) {
                log.error("ZIP 文件读取失败: " + config.getZipPath(), e);
                result.failedTiles = -1;
                result.costTime = System.currentTimeMillis() - startTime;
                return result;
            }
            if (zipFile.size() > config.getMaxEntries()) {
                log.error("ZIP 条目数超过限制: {} > {}", zipFile.size(), config.getMaxEntries());
                result.failedTiles = -1;
                result.costTime = System.currentTimeMillis() - startTime;
                IoUtil.close(zipFile);
                return result;
            }
            MbtilesInfoBatchPutConsumer mbtilesInfoBatchPutConsumer =
                    new MbtilesInfoBatchPutConsumer(
                            false,
                            config.getOverwrite(),
                            config.batchSize,
                            dataSource,
                            0,
                            zipFile.size());
            // 处理 ZIP 文件
            try {
                Enumeration<? extends ZipEntry> entryEnum = zipFile.entries();
                long totalReadBytes = 0;
                while (entryEnum.hasMoreElements()) {
                    ZipEntry entry = entryEnum.nextElement();
                    String entryName = entry.getName();

                    // 跳过目录
                    if (entry.isDirectory()) {
                        result.skipDir++;
                        continue;
                    }

                    // 过滤图片后缀
                    if (!isAllowedExtension(entryName, config.getExtensions())) {
                        continue;
                    }

                    if (!isEntryWithinLimits(entry, config)) {
                        result.failedTiles++;
                        log.warn("ZIP 瓦片条目超过安全限制，跳过: {}", entryName);
                        continue;
                    }

                    result.totalTiles++;

                    // 打印进度
                    if (result.totalTiles % 100000 == 0) {
                        log.info("已读取瓦片：{} | 当前瓦片路径：{}  ", result.totalTiles, entryName);
                    }
                    // 读取瓦片数据
                    byte[] tileBytes = null;
                    try {
                        tileBytes = readEntryBytes(zipFile, entry, config.getMaxEntryBytes());
                    } catch (Exception e) {
                        result.failedTiles++;
                        log.warn("读取瓦片二进制失败，跳过: {}，原因: {}", entryName, e.getMessage());
                        continue;
                    }
                    if (tileBytes.length > config.getMaxTotalUncompressedBytes() - totalReadBytes) {
                        log.error("ZIP 解压后总数据量超过限制: {}", config.getMaxTotalUncompressedBytes());
                        result.failedTiles = -1;
                        break;
                    }
                    totalReadBytes += tileBytes.length;
                    // 使用处理器解析瓦片信息
                    MbtilesInfo tileInfo = null;
                    try {
                        tileInfo = config.getProcessor().apply(entryName, tileBytes);
                    } catch (Exception e) {
                        result.failedTiles++;
                        log.warn("瓦片坐标解析失败，跳过: {}，原因: {}", entryName, e.getMessage());
                        continue;
                    }
                    if (tileInfo == null) {
                        result.failedTiles++;
                        log.warn("瓦片坐标解析结果为空，跳过: {}", entryName);
                        continue;
                    }
                    mbtilesInfoBatchPutConsumer.accept(tileInfo);
                }
                mbtilesInfoBatchPutConsumer.close();
                if (result.failedTiles == -1) {
                    result.costTime = System.currentTimeMillis() - startTime;
                    return result;
                }
                ConvertStats stats = mbtilesInfoBatchPutConsumer.getStats();
                result.costTime = System.currentTimeMillis() - startTime;

                log.info("==== 全部处理完成 ====");
                log.info("目录总数：" + result.skipDir);
                log.info("有效瓦片总数：" + result.totalTiles);
                log.info("成功导入瓦片：" + stats.success);
                log.info("跳过瓦片数：" + stats.skipped);
                log.info("失败瓦片数：" + stats.failed);
                log.info("总耗时：" + result.costTime + "ms");

                return result;

            } catch (Exception e) {
                log.error("ZIP 文件读取失败: " + config.getZipPath(), e);
                result.failedTiles = -1;
                result.costTime = System.currentTimeMillis() - startTime;
                return result;
            } finally {
                IoUtil.close(zipFile);
            }

        } catch (Exception e) {

            log.error(e);
            result.failedTiles = -1;
            result.costTime = System.currentTimeMillis() - startTime;
            return result;
        } finally {
            if (dataSource != null) {
                dataSource.close();
            }
        }
    }

    // ==================== 私有方法 ====================

    /** 验证配置 */
    private static boolean validateConfig(ImportConfig config) {
        if (config == null) {
            System.err.println("ZIP 导入配置不能为空");
            return false;
        }
        if (StrUtil.isBlank(config.getZipPath())) {
            System.err.println("ZIP 文件路径不能为空");
            return false;
        }
        if (!FileUtil.exist(config.getZipPath())) {
            System.err.println("ZIP 文件不存在: " + config.getZipPath());
            return false;
        }
        if (StrUtil.isBlank(config.getTargetMbtiles())) {
            System.err.println("目标 MBTiles 路径不能为空");
            return false;
        }
        if (StrUtil.isBlank(config.getLayerName())) {
            System.err.println("图层名称不能为空");
            return false;
        }
        if (config.getProcessor() == null) {
            System.err.println("瓦片处理器不能为空");
            return false;
        }
        if (config.getMaxEntryBytes() <= 0
                || config.getMaxZipFileBytes() <= 0
                || config.getMaxTotalUncompressedBytes() <= 0
                || config.getMaxEntries() <= 0
                || config.getMaxCompressionRatio() <= 0) {
            System.err.println("ZIP 安全限制必须大于 0");
            return false;
        }
        return true;
    }

    /** 检查文件扩展名是否允许 */
    private static boolean isAllowedExtension(String fileName, List<String> extensions) {
        for (String ext : extensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /** 读取 ZipEntry 完整二进制 */
    private static byte[] readEntryBytes(ZipFile zipFile, ZipEntry entry, long maxEntryBytes)
            throws IOException {
        long expectedSize = entry.getSize();
        if (expectedSize > maxEntryBytes) {
            throw new IOException("ZIP 条目超过大小限制: " + expectedSize);
        }
        try (InputStream is = zipFile.getInputStream(entry);
                ByteArrayOutputStream bos =
                        new ByteArrayOutputStream(
                                (int) Math.min(Math.max(expectedSize, 0), BUF_SIZE))) {

            byte[] buf = new byte[BUF_SIZE];
            int len;
            long total = 0;
            while ((len = is.read(buf)) != -1) {
                total += len;
                if (total > maxEntryBytes) {
                    throw new IOException("ZIP 条目超过大小限制: " + total);
                }
                bos.write(buf, 0, len);
            }
            return bos.toByteArray();
        }
    }

    private static boolean isEntryWithinLimits(ZipEntry entry, ImportConfig config) {
        long uncompressedSize = entry.getSize();
        if (uncompressedSize > config.getMaxEntryBytes()) {
            return false;
        }
        long compressedSize = entry.getCompressedSize();
        if (uncompressedSize < 0 || compressedSize < 0) {
            return true;
        }
        if (compressedSize == 0) {
            return uncompressedSize == 0;
        }
        return uncompressedSize / compressedSize <= config.getMaxCompressionRatio();
    }

    /** 批量插入 */
    private static void batchInsert(DruidDataSource dataSource, List<MbtilesInfo> batchList) {
        if (batchList.isEmpty()) {
            return;
        }
        log.info("执行批量插入，本次条数：{}", batchList.size());
        MbtilesUtils.putTileBatch(dataSource, true, batchList);
    }

    // ==================== main 测试 ====================

    public static void main(String[] args) {
        // ==================== 1. 最简单的用法（使用默认处理器） ====================
        ImportResult result1 =
                MbtilesFromLocalZipConverter.importZip(
                        "D:\\mapcache\\xj\\16.zip",
                        "G:\\GTC_CACHE_DIR\\gwc_fuser\\tile_cache\\arcgis16_2023.mbtiles",
                        "arcgis16_2023");
        log.info("导入结果: " + result1);

        // ==================== 2. 使用自定义处理器 ====================
        ImportResult result2 =
                MbtilesFromLocalZipConverter.importZip(
                        "D:\\mapcache\\xj\\16.zip",
                        "G:\\GTC_CACHE_DIR\\gwc_fuser\\tile_cache\\arcgis16_2023.mbtiles",
                        "arcgis16_2023",
                        (entryName, tileData) -> {
                            // 自定义解析逻辑
                            String[] pathArr = entryName.split("/");
                            if (pathArr.length != 3) {
                                return null;
                            }
                            // 自定义解析...
                            return null;
                        });
        log.info("导入结果: " + result2);

        // ==================== 3. 完整配置（推荐） ====================
        ImportConfig config =
                new ImportConfig()
                        .setZipPath("D:\\mapcache\\xj\\16.zip")
                        .setTargetMbtiles(
                                "G:\\GTC_CACHE_DIR\\gwc_fuser\\tile_cache\\arcgis16_2023.mbtiles")
                        .setLayerName("arcgis16_2023")
                        .setFormat("png")
                        .setBatchSize(5000)
                        .setMaxPoolSize(20)
                        .setMinIdle(2)
                        .setProcessor(
                                (entryName, tileData) -> {
                                    // 自定义处理器：可以在这里添加额外的逻辑
                                    // 例如：校验数据大小、过滤特定范围的瓦片等
                                    String[] pathArr = entryName.split("/");
                                    if (pathArr.length != 3) {
                                        return null;
                                    }
                                    try {
                                        int z = Integer.parseInt(pathArr[0]);
                                        int y = Integer.parseInt(pathArr[1]);
                                        String yFile = pathArr[2];
                                        int x =
                                                Integer.parseInt(
                                                        yFile.replaceAll("\\.(png|jpg)$", ""));
                                        int reverseY =
                                                GirAdvTools.getTileGrid3857Opt()
                                                        .convertY(
                                                                z, y, TileYAxis.XYZ, TileYAxis.TMS);

                                        // 可以在这里添加额外过滤逻辑
                                        // if (x < 0 || x > 100) return null;

                                        return MbtilesInfo.of()
                                                .setX(x)
                                                .setZoomLevel(z)
                                                .setY(reverseY)
                                                .setTileData(tileData);
                                    } catch (NumberFormatException e) {
                                        return null;
                                    }
                                });

        ImportResult result3 = MbtilesFromLocalZipConverter.importZip(config);
        log.info("导入结果: " + result3);

        // ==================== 4. 使用预定义的处理器 ====================
        // 使用 z/x/y 格式（注意顺序）
        ImportResult result4 =
                MbtilesFromLocalZipConverter.importZip(
                        "D:\\mapcache\\xj\\16.zip",
                        "G:\\GTC_CACHE_DIR\\gwc_fuser\\tile_cache\\arcgis16_2023.mbtiles",
                        "arcgis16_2023",
                        TileNameProcessor.PROCESSOR_ZXY);
        log.info("导入结果: " + result4);

        // 使用不翻转 Y 的处理器
        ImportResult result5 =
                MbtilesFromLocalZipConverter.importZip(
                        "D:\\mapcache\\xj\\16.zip",
                        "G:\\GTC_CACHE_DIR\\gwc_fuser\\tile_cache\\arcgis16_2023.mbtiles",
                        "arcgis16_2023",
                        TileNameProcessor.PROCESSOR_ZYX_NO_REVERSE);
        log.info("导入结果: " + result5);
    }
}
