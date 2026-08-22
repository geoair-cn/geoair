package cn.geoair.map.tile.forge.fuser.converter;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.runtime.GutilShutdownHook;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesInfo;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesUtils;
import cn.geoair.map.tile.forge.fuser.utils.TileResourceLimits;
import cn.hutool.core.io.unit.DataSizeUtil;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/24 17:05
 * @description： 本地的散列文件瓦片转换成Mbtile工具类
 * <p>
 * 功能：
 * 1. 从本地目录扫描瓦片文件
 * 2. 支持自定义路径解析器（从文件路径中提取 z/x/y）
 * 3. 批量导入到 MBTiles
 * 4. 支持 Y 轴翻转、覆盖、删除源文件等
 * </p>
 */
public class MbtilesFromLocalFileConverter {

    private static GiLogger log = GirLoggerFactory.getLogger(MbtilesFromLocalFileConverter.class);
    private static final ExecutorService DELETE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "tile-convert-delete");
        thread.setDaemon(true);
        return thread;
    });


    /**
     * 自定义正则表达式解析器构建器
     */
    public static TilePathParser parserByRegex(String regex, int zGroup, int xGroup, int yGroup) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        return (file, relativePath, config) -> {
            try {
                java.util.regex.Matcher matcher = pattern.matcher(relativePath);
                if (matcher.matches()) {
                    TileInfo tile = new TileInfo();
                    tile.path = file.toPath();
                    tile.z = Integer.parseInt(matcher.group(zGroup));
                    tile.x = Integer.parseInt(matcher.group(xGroup));
                    tile.y = Integer.parseInt(matcher.group(yGroup));
                    return tile;
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        };
    }

    // ==================== 转换配置 ====================

    /**
     * 转换配置
     */
    @Data
    @Accessors(chain = true)
    public static class ConvertConfig {
        private String sourceRoot;              // 源文件根路径
        private String mbtilesPath;             // MBTiles 文件完整路径
        private String layerName;               // MBTiles 中的图层名称
        private boolean needReverseY = false;   // 是否需要 Y 轴翻转（Google坐标系 ↔ TMS坐标系）
        private int batchSize = 5000;           // 批量插入大小
        private int maxPoolSize = 20;           // 连接池大小
        private int minIdle = 2;                // 最小空闲连接数
        private boolean overwrite = false;      // 如果瓦片已存在是否覆盖
        private List<Integer> zoomLevels;       // 指定要转换的层级，为空则自动扫描
        private boolean deleteSourceAfterConvert = false; // 转换后是否删除源文件
        private TilePathParser pathParser = TilePathParser.DEFAULT_ZYX_PARSER; // 路径解析器


    }

    /**
     * 转换结果
     */
    @Data
    public static class ConvertResult {
        private String sourceRoot;
        private String mbtilesPath;
        private String layerName;
        private long totalTiles;
        private long successTiles;
        private long failedTiles;
        private long skippedTiles;      // 因解析失败而跳过的瓦片数
        private long totalSize;
        private long costTime;
        private List<Integer> processedZoomLevels = new ArrayList<>();
        private boolean deletedSource;

        @Override
        public String toString() {
            return String.format("ConvertResult{source='%s', target='%s', layer='%s', " +
                                 "totalTiles=%d, successTiles=%d, skippedTiles=%d, failedTiles=%d, " +
                                 "totalSize=%s, costTime=%dms, deletedSource=%s}",
                    sourceRoot, mbtilesPath, layerName,
                    totalTiles, successTiles, skippedTiles, failedTiles,
                    DataSizeUtil.format(totalSize), costTime, deletedSource);
        }
    }

    /**
     * 瓦片信息
     */
    public static class TileInfo {
        int z;
        int x;
        int y;
        Path path;

        public int getZ() {
            return z;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public Path getPath() {
            return path;
        }
    }


    /**
     * 瓦片信息消费者 - 负责将解析出的瓦片批量入库
     */
    public static class TileInfoConsumer implements Consumer<TileInfo> {
        private final MbtilesInfoBatchPutConsumer mbtilesInfoBatchPutConsumer;
        @Getter
        private final ConvertStats stats = new ConvertStats();

        public TileInfoConsumer(boolean needReverseY, boolean overwrite, int batchSize,
                                DruidDataSource dataSource, Integer zoom) {
            this.mbtilesInfoBatchPutConsumer = new MbtilesInfoBatchPutConsumer(
                    needReverseY, overwrite, batchSize, dataSource, zoom, 0
            );
        }

        @Override
        public void accept(TileInfo tile) {
            try {
                if (Files.size(tile.path) > TileResourceLimits.getMaxTileBytes()) {
                    stats.failed++;
                    log.warn("瓦片文件超过大小限制，跳过: {}", tile.path);
                    return;
                }
                byte[] data = Files.readAllBytes(tile.path);
                stats.total++;
                stats.totalSize += data.length;

                mbtilesInfoBatchPutConsumer.accept(
                        MbtilesInfo.of()
                                .setX(tile.x)
                                .setY(tile.y)
                                .setZoomLevel(tile.z)
                                .setTileData(data)
                );
                stats.success++;
            } catch (Exception e) {
                stats.failed++;
                log.error("读取瓦片数据失败: {}", tile.path, e);
            }
        }

        public void doImportEnd() {
            mbtilesInfoBatchPutConsumer.close();
            // 合并统计
            ConvertStats batchStats = mbtilesInfoBatchPutConsumer.getStats();
            // 注意：success/failed 已经在 accept 中统计了，这里只需要补充 skipped
            // 但实际上 skipped 是在 MbtilesInfoBatchPutConsumer 中统计的（覆盖时跳过）
            // 所以我们用 batchStats 的 skipped 覆盖
            this.stats.skipped = batchStats.skipped;
        }

    }

    // ==================== 核心转换方法 ====================

    /**
     * 执行转换（便捷方法）
     */
    public static ConvertResult convert(String sourceRoot, String mbtilesPath,
                                        String layerName, boolean needReverseY) {
        ConvertConfig config = new ConvertConfig()
                .setSourceRoot(sourceRoot)
                .setMbtilesPath(mbtilesPath)
                .setLayerName(layerName)
                .setNeedReverseY(needReverseY);
        return convert(config);
    }

    /**
     * 执行转换（带配置回调）
     */
    public static ConvertResult convert(String sourceRoot, String mbtilesPath,
                                        String layerName, Consumer<ConvertConfig> consumer) {
        ConvertConfig config = new ConvertConfig()
                .setSourceRoot(sourceRoot)
                .setMbtilesPath(mbtilesPath)
                .setLayerName(layerName);
        consumer.accept(config);
        return convert(config);
    }

    /**
     * 执行转换（配置回调）
     */
    public static ConvertResult convert(Consumer<ConvertConfig> consumer) {
        ConvertConfig config = new ConvertConfig();
        consumer.accept(config);
        return convert(config);
    }

    /**
     * 执行转换（完整配置）
     */
    public static ConvertResult convert(ConvertConfig config) {
        long startTime = System.currentTimeMillis();

        // 参数校验
        if (!validateConfig(config)) {
            return null;
        }

        ConvertResult result = new ConvertResult();
        result.sourceRoot = config.getSourceRoot();
        result.mbtilesPath = config.getMbtilesPath();
        result.layerName = config.getLayerName();

        // 确保 MBTiles 目录存在
        MbtilesUtils.ensureDirectoryExists(config.getMbtilesPath());

        // 创建数据源
        DruidDataSource dataSource = MbtilesUtils.createDataSource(
                config.getMbtilesPath(),
                false,
                config.getMaxPoolSize(),
                config.getMinIdle()
        );
        GutilShutdownHook.getInstance().registerTask(dataSource::close);

        try {
            // 初始化数据库
            if (!MbtilesUtils.initDatabase(dataSource)) {
                log.error("初始化数据库失败: {}", config.getMbtilesPath());
                result.failedTiles = -1;
                return result;
            }

            // 初始化图层元数据
            initLayerMetadata(dataSource, config);

            // 获取要处理的层级列表
            List<Integer> zoomLevels = config.getZoomLevels();
            if (zoomLevels == null || zoomLevels.isEmpty()) {
                zoomLevels = scanZoomLevels(config);
            }

            if (zoomLevels.isEmpty()) {
                log.warn("未找到任何瓦片数据: {}", config.getSourceRoot());
                result.costTime = System.currentTimeMillis() - startTime;
                return result;
            }

            log.info("开始转换瓦片，层级: {}, 图层: {}", zoomLevels, config.getLayerName());

            // 执行转换
            ConvertStats stats = convertTiles(config, dataSource, zoomLevels);

            // 填充结果
            result.totalTiles = stats.total;
            result.successTiles = stats.success;
            result.failedTiles = stats.failed;
            result.skippedTiles = stats.skipped;
            result.totalSize = stats.totalSize;
            result.processedZoomLevels = zoomLevels;

            // 可选：删除源文件
            if (config.isDeleteSourceAfterConvert()) {
                result.deletedSource = deleteSourceFiles(config);
            }

            result.costTime = System.currentTimeMillis() - startTime;

            log.info("转换完成: {}", result);
            return result;

        } catch (Exception e) {
            log.error("转换失败", e);
            result.failedTiles = -1;
            return result;
        } finally {
            MbtilesUtils.closeDataSource(dataSource);
        }
    }

    // ==================== 瓦片转换核心逻辑 ====================

    /**
     * 转换瓦片
     */
    private static ConvertStats convertTiles(ConvertConfig config, DruidDataSource dataSource,
                                             List<Integer> zoomLevels) {
        ConvertStats totalStats = new ConvertStats();
        try {
            for (int z : zoomLevels) {
                log.info("处理层级: z={}", z);

                // 构建层级目录路径


                String zDirPath = concatPath(normalizePath(config.getSourceRoot()), normalizePath(z + ""));

                File zDir = new File(zDirPath);

                if (!zDir.exists() || !zDir.isDirectory()) {
                    log.warn("层级目录不存在: {}", zDirPath);
                    continue;
                }

                // 创建消费者
                TileInfoConsumer consumer = new TileInfoConsumer(
                        config.isNeedReverseY(),
                        config.isOverwrite(),
                        config.getBatchSize(),
                        dataSource,
                        z

                );

                // 递归遍历层级目录下的所有文件
                scanDirectory(zDir, z, "", consumer, config);

                // 完成批量插入
                consumer.doImportEnd();

                // 汇总统计
                totalStats.add(consumer.getStats());

                log.info("层级 z={} 处理完成: 总数={}, 成功={}, 跳过={}, 失败={}",
                        z,
                        consumer.getStats().total,
                        consumer.getStats().success,
                        consumer.getStats().skipped,
                        consumer.getStats().failed
                );
            }

            log.info("所有层级转换完成: 总数={}, 成功={}, 跳过={}, 失败={}, 总大小={}",
                    totalStats.total, totalStats.success, totalStats.skipped,
                    totalStats.failed, DataSizeUtil.format(totalStats.totalSize));

        } catch (Exception e) {
            log.error("转换过程异常", e);
        }

        return totalStats;
    }

    /**
     * 递归扫描目录，使用 TilePathParser 解析每个文件
     */
    private static void scanDirectory(File dir, int z, String relativePath,
                                      TileInfoConsumer consumer, ConvertConfig config) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归子目录
                String subPath = relativePath + file.getName() + File.separator;
                scanDirectory(file, z, subPath, consumer, config);
            } else if (file.isFile()) {
                // 解析瓦片文件
                String fullRelativePath = z + File.separator + relativePath + file.getName();
                parseAndAddTile(file, fullRelativePath, consumer, config);
            }
        }
    }

    /**
     * 解析并添加瓦片（使用自定义解析器）
     */
    private static void parseAndAddTile(File file, String relativePath,
                                        TileInfoConsumer consumer, ConvertConfig config) {
        try {
            TilePathParser parser = config.getPathParser();
            if (parser == null) {
                parser = TilePathParser.DEFAULT_ZYX_PARSER;
            }

            TileInfo tile = parser.parse(file, relativePath, config);
            if (tile != null) {
                consumer.accept(tile);
            } else {
                log.debug("解析瓦片失败，跳过: {} (路径: {})", file.getPath(), relativePath);
            }
        } catch (Exception e) {
            log.debug("解析瓦片异常: {}", file.getPath(), e);
        }
    }


    /**
     * 标准化路径
     * - 统一使用系统文件分隔符
     * - 处理多余的斜杠
     */
    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        // 统一使用系统文件分隔符
        String normalized = path.replace('\\', File.separatorChar)
                .replace('/', File.separatorChar).replace("\\", File.separator);

        // 处理多个连续分隔符
        String doubleSeparator = File.separator + File.separator;
        while (normalized.contains(doubleSeparator)) {
            normalized = normalized.replace(doubleSeparator, File.separator);
        }

        return normalized;
    }

    /**
     * 拼接路径
     * 自动处理头尾分隔符，确保中间只有一个分隔符
     */
    private static String concatPath(String parent, String child) {
        if (parent == null || parent.isEmpty()) {
            return child;
        }
        if (child == null || child.isEmpty()) {
            return parent;
        }

        // 移除 parent 尾部分隔符
        String p = parent;
        while (p.endsWith(File.separator)) {
            p = p.substring(0, p.length() - 1);
        }

        // 移除 child 头部分隔符
        String c = child;
        while (c.startsWith(File.separator)) {
            c = c.substring(1);
        }

        return p + File.separator + c;
    }
    // ==================== 辅助方法 ====================

    /**
     * 验证配置
     */
    private static boolean validateConfig(ConvertConfig config) {
        if (config.getSourceRoot() == null || config.getSourceRoot().isEmpty()) {
            log.error("源根路径不能为空");
            return false;
        }

        File sourceDir = new File(config.getSourceRoot());
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            log.error("源根路径不存在或不是目录: {}", config.getSourceRoot());
            return false;
        }

        if (config.getMbtilesPath() == null || config.getMbtilesPath().isEmpty()) {
            log.error("MBTiles 路径不能为空");
            return false;
        }

        if (config.getLayerName() == null || config.getLayerName().isEmpty()) {
            log.error("图层名称不能为空");
            return false;
        }

        if (config.getPathParser() == null) {
            log.warn("未设置路径解析器，使用默认解析器");
        }

        return true;
    }

    /**
     * 初始化图层元数据
     */
    private static boolean initLayerMetadata(DruidDataSource dataSource, ConvertConfig config) {
        String layerName = config.getLayerName();

        if (MbtilesUtils.layerExists(dataSource, layerName)) {
            log.info("图层已存在，将使用已有配置: {}", layerName);
            return true;
        }

        return MbtilesUtils.initMetadata(dataSource,
                "name", layerName,
                "format", detectImageFormat(config),
                "version", "1.0",
                "type", "overlay"
        );
    }

    /**
     * 检测图片格式（从文件扩展名推断）
     */
    private static String detectImageFormat(ConvertConfig config) {
        // 尝试扫描一个文件来判断格式
        File sourceDir = new File(config.getSourceRoot());
        if (sourceDir.exists()) {
            String format = scanFileExtension(sourceDir);
            if (format != null) {
                return format;
            }
        }
        return "png"; // 默认
    }

    private static String scanFileExtension(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                String result = scanFileExtension(file);
                if (result != null) {
                    return result;
                }
            } else if (file.isFile()) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".png")) return "png";
                if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "jpg";
                if (name.endsWith(".webp")) return "webp";
                if (name.endsWith(".gif")) return "gif";
            }
        }
        return null;
    }

    /**
     * 扫描所有层级（从源根目录下的一级子目录中识别数字）
     */
    private static List<Integer> scanZoomLevels(ConvertConfig config) {
        List<Integer> zoomLevels = new ArrayList<>();
        File sourceDir = new File(config.getSourceRoot());

        File[] files = sourceDir.listFiles(File::isDirectory);
        if (files != null) {
            for (File file : files) {
                try {
                    int zoom = Integer.parseInt(file.getName());
                    zoomLevels.add(zoom);
                } catch (NumberFormatException e) {
                    // 跳过非数字目录
                }
            }
        }

        zoomLevels.sort(Integer::compareTo);
        log.info("扫描到 {} 个层级: {}", zoomLevels.size(), zoomLevels);
        return zoomLevels;
    }

    /**
     * 删除源文件
     */
    private static boolean deleteSourceFiles(ConvertConfig config) {
        try {
            File sourceDir = new File(config.getSourceRoot());
            String tempDirName = sourceDir.getName() + "_deleting_" + System.currentTimeMillis();
            Path tempPath = sourceDir.toPath().resolveSibling(tempDirName);
            Files.move(sourceDir.toPath(), tempPath, StandardCopyOption.ATOMIC_MOVE);
            asyncDeleteDirectory(tempPath);
            log.info("源文件已标记删除: {}", config.getSourceRoot());
            return true;
        } catch (IOException e) {
            log.error("删除源文件失败: {}", config.getSourceRoot(), e);
            return false;
        }
    }

    /**
     * 异步删除目录
     */
    private static void asyncDeleteDirectory(Path path) {
        DELETE_EXECUTOR.execute(() -> {
            try {
                try (Stream<Path> paths = Files.walk(path)) {
                    paths.sorted((a, b) -> -a.compareTo(b))
                            .forEach(p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException e) {
                                    log.error("删除文件/目录失败: {}", p, e);
                                }
                            });
                }
                log.info("异步删除临时目录成功: {}", path);
            } catch (IOException e) {
                log.error("异步删除临时目录失败: {}", path, e);
            }
        });
    }

    // ==================== 便捷方法 ====================

    /**
     * 转换指定层级
     */
    public static ConvertResult convertWithZoomLevels(String sourceRoot, String mbtilesPath,
                                                      String layerName, List<Integer> zoomLevels) {
        ConvertConfig config = new ConvertConfig()
                .setSourceRoot(sourceRoot)
                .setMbtilesPath(mbtilesPath)
                .setLayerName(layerName)
                .setZoomLevels(zoomLevels);
        return convert(config);
    }

    /**
     * 转换并追加到已有 MBTiles（不覆盖）
     */
    public static ConvertResult convertAppend(String sourceRoot, String mbtilesPath,
                                              String layerName) {
        ConvertConfig config = new ConvertConfig()
                .setSourceRoot(sourceRoot)
                .setMbtilesPath(mbtilesPath)
                .setLayerName(layerName)
                .setOverwrite(false);
        return convert(config);
    }

    /**
     * 转换并覆盖已有瓦片
     */
    public static ConvertResult convertOverwrite(String sourceRoot, String mbtilesPath,
                                                 String layerName) {
        ConvertConfig config = new ConvertConfig()
                .setSourceRoot(sourceRoot)
                .setMbtilesPath(mbtilesPath)
                .setLayerName(layerName)
                .setOverwrite(true);
        return convert(config);
    }

    /**
     * 转换并删除源文件
     */
    public static ConvertResult convertAndDelete(String sourceRoot, String mbtilesPath,
                                                 String layerName) {
        ConvertConfig config = new ConvertConfig()
                .setSourceRoot(sourceRoot)
                .setMbtilesPath(mbtilesPath)
                .setLayerName(layerName)
                .setDeleteSourceAfterConvert(true);
        return convert(config);
    }

    // ==================== main 测试 ====================

    public static void main(String[] args) {
        // ==================== 1. 基本用法（默认解析器） ====================
        ConvertResult result1 = MbtilesFromLocalFileConverter.convert(
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1-13",
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1_13.mbtiles",
                "1_13",
                true  // needReverseY
        );
        System.out.println("结果1: " + result1);


        // ==================== 4. 使用正则表达式解析器 ====================
        // 路径格式: z/x/y.png
        ConvertResult result4 = MbtilesFromLocalFileConverter.convert(
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1-13",
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1_13_regex.mbtiles",
                "1_13_regex",
                config -> {
                    config.setNeedReverseY(true);
                    config.setPathParser(MbtilesFromLocalFileConverter.parserByRegex(
                            "(\\d+)[/\\\\](\\d+)[/\\\\](\\d+)\\.png",  // 正则
                            1,  // z 组
                            2,  // x 组
                            3   // y 组
                    ));
                }
        );
        System.out.println("结果4: " + result4);

        // ==================== 5. 完全自定义解析器 ====================
        ConvertResult result5 = MbtilesFromLocalFileConverter.convert(
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1-13",
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1_13_custom.mbtiles",
                "1_13_custom",
                config -> {
                    config.setNeedReverseY(true);
                    config.setPathParser((file, relativePath, cfg) -> {
                        // 自定义解析逻辑
                        // 例如：从路径中提取 z, x, y
                        String[] parts = relativePath.split("[/\\\\]");
                        if (parts.length < 3) {
                            return null;
                        }

                        TileInfo tile = new TileInfo();
                        tile.path = file.toPath();

                        try {
                            // 假设格式: z/y/x.png
                            tile.z = Integer.parseInt(parts[0]);
                            tile.y = Integer.parseInt(parts[parts.length - 2]);
                            String fileName = parts[parts.length - 1];
                            int dotIndex = fileName.lastIndexOf('.');
                            tile.x = Integer.parseInt(dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName);
                            return tile;
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    });
                }
        );
        System.out.println("结果5: " + result5);

        // ==================== 6. 指定层级 + 覆盖 ====================
        List<Integer> zoomLevels = java.util.Arrays.asList(0, 1, 2, 3, 4, 5);
        ConvertResult result6 = MbtilesFromLocalFileConverter.convert(
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1-13",
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1_13_partial.mbtiles",
                "1_13_partial",
                config -> {
                    config.setNeedReverseY(true);
                    config.setZoomLevels(zoomLevels);
                    config.setOverwrite(true);
                    config.setBatchSize(3000);
                }
        );
        System.out.println("结果6: " + result6);

        // ==================== 7. 转换并删除源文件 ====================
        ConvertResult result7 = MbtilesFromLocalFileConverter.convertAndDelete(
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1-13",
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1_13_final.mbtiles",
                "1_13_final"
        );
        System.out.println("结果7: " + result7);
    }
}
