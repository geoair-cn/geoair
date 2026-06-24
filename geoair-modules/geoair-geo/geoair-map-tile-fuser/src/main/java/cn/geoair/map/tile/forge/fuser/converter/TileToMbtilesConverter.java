package cn.geoair.map.tile.forge.fuser.converter;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.runtime.GutilShutdownHook;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import cn.geoair.map.tile.forge.fuser.utils.MbtilesUtils;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.Data;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/24 17:05
 * @description： 本地的散列文件瓦片转换成Mbtile工具类
 * <p>
 * 支持路径模板：{z}/{y}/{x}.png 或 {z}/{x}/{y}.png 等任意格式
 * 支持追加到已有 MBTiles 文件
 */
public class TileToMbtilesConverter {

    private static GiLogger log = GirLoggerFactory.getLogger(TileToMbtilesConverter.class);

    /**
     * 路径模板占位符
     */
    private static final String PLACEHOLDER_Z = "{z}";
    private static final String PLACEHOLDER_X = "{x}";
    private static final String PLACEHOLDER_Y = "{y}";

    /**
     * 转换配置
     */
    @Getter
    public static class ConvertConfig {
        private String sourceRoot;              // 源文件根路径，如 D:\mapcache\quangguoyingxiang0-14\
        private String pathTemplate;            // 路径模板，如 {z}/{y}/{x}.png
        private String mbtilesPath;             // MBTiles 文件完整路径
        private String layerName;               // MBTiles 中的图层名称（如果文件已存在且包含该图层则使用已有，否则创建）
        private boolean needReverseY = false;   // 是否需要 Y 轴翻转（Google坐标系 ↔ TMS坐标系）
        private int batchSize = 1000;           // 批量插入大小
        private int maxPoolSize = 20;           // 连接池大小
        private int minIdle = 2;                // 最小空闲连接数
        private boolean overwrite = false;      // 如果瓦片已存在是否覆盖
        private List<Integer> zoomLevels;       // 指定要转换的层级，为空则自动扫描
        private boolean deleteSourceAfterConvert = false; // 转换后是否删除源文件

        public ConvertConfig setSourceRoot(String sourceRoot) {
            this.sourceRoot = sourceRoot.endsWith(File.separator) ? sourceRoot : sourceRoot + File.separator;
            return this;
        }

        public ConvertConfig setPathTemplate(String pathTemplate) {
            this.pathTemplate = pathTemplate;
            return this;
        }

        public ConvertConfig setMbtilesPath(String mbtilesPath) {
            this.mbtilesPath = mbtilesPath;
            return this;
        }

        public ConvertConfig setLayerName(String layerName) {
            this.layerName = layerName;
            return this;
        }

        public ConvertConfig setNeedReverseY(boolean needReverseY) {
            this.needReverseY = needReverseY;
            return this;
        }

        public ConvertConfig setBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public ConvertConfig setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public ConvertConfig setMinIdle(int minIdle) {
            this.minIdle = minIdle;
            return this;
        }

        public ConvertConfig setOverwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return this;
        }

        public ConvertConfig setZoomLevels(List<Integer> zoomLevels) {
            this.zoomLevels = zoomLevels;
            return this;
        }

        public ConvertConfig setDeleteSourceAfterConvert(boolean deleteSourceAfterConvert) {
            this.deleteSourceAfterConvert = deleteSourceAfterConvert;
            return this;
        }
    }

    /**
     * 转换结果
     */
    @Data
    public static class ConvertResult {
        private String sourceRoot;
        private String pathTemplate;
        private String mbtilesPath;
        private String layerName;
        private long totalTiles;
        private long successTiles;
        private long failedTiles;
        private long skippedTiles;      // 因已存在而跳过的瓦片数
        private long totalSize;
        private long costTime;
        private List<Integer> processedZoomLevels = new ArrayList<>();
        private boolean deletedSource;
    }

    /**
     * 执行转换（便捷方法）
     *
     * @param sourceRoot   源文件根路径
     * @param pathTemplate 路径模板
     * @param mbtilesPath  MBTiles 文件路径
     * @param layerName    图层名称
     * @return 转换结果
     */
    public static ConvertResult convert(String sourceRoot, String pathTemplate, String mbtilesPath, String layerName, boolean needReverseY) {
        ConvertConfig config = new ConvertConfig()
                .setSourceRoot(sourceRoot)
                .setPathTemplate(pathTemplate)
                .setMbtilesPath(mbtilesPath)
                .setLayerName(layerName)
                .setNeedReverseY(needReverseY);
        return convert(config);
    }

    public static ConvertResult convert(String sourceRoot, String pathTemplate, String mbtilesPath, String layerName, Consumer<ConvertConfig> consumer) {
        ConvertConfig config = new ConvertConfig()
                .setSourceRoot(sourceRoot)
                .setPathTemplate(pathTemplate)
                .setMbtilesPath(mbtilesPath)
                .setLayerName(layerName);
        consumer.accept(config);
        return convert(config);
    }

    /**
     * 执行转换（完整配置）
     *
     * @param consumer 转换配置
     * @return 转换结果
     */
    public static ConvertResult convert(Consumer<ConvertConfig> consumer) {
        ConvertConfig config = new ConvertConfig();
        consumer.accept(config);
        return convert(config);
    }

    /**
     * 执行转换（完整配置）
     *
     * @param config 转换配置
     * @return 转换结果
     */
    public static ConvertResult convert(ConvertConfig config) {
        long startTime = System.currentTimeMillis();

        // 参数校验
        if (!validateConfig(config)) {
            return null;
        }

        ConvertResult result = new ConvertResult();
        result.sourceRoot = config.getSourceRoot();
        result.pathTemplate = config.getPathTemplate();
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
            // 初始化数据库（如果文件不存在则创建）
            if (!MbtilesUtils.initDatabase(dataSource)) {
                log.error("初始化数据库失败: {}", config.getMbtilesPath());
                result.failedTiles = -1;
                return result;
            }

            // 检查并创建图层元数据
            boolean metadataInit = initLayerMetadata(dataSource, config);
            if (!metadataInit) {
                log.warn("图层元数据初始化失败，继续执行: {}", config.getLayerName());
            }

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

        if (config.getPathTemplate() == null || config.getPathTemplate().isEmpty()) {
            log.error("路径模板不能为空");
            return false;
        }

        if (!config.getPathTemplate().contains(PLACEHOLDER_Z) ||
                !config.getPathTemplate().contains(PLACEHOLDER_X) ||
                !config.getPathTemplate().contains(PLACEHOLDER_Y)) {
            log.error("路径模板必须包含 {z}, {x}, {y} 占位符");
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

        return true;
    }

    /**
     * 初始化图层元数据
     * 如果 MBTiles 文件已存在且包含该图层，则使用已有配置
     * 否则创建新图层元数据
     */
    private static boolean initLayerMetadata(DruidDataSource dataSource, ConvertConfig config) {
        String layerName = config.getLayerName();

        // 检查是否已存在该图层
        if (layerExists(dataSource, layerName)) {
            log.info("图层已存在，将使用已有配置: {}", layerName);
            return true;
        }

        // 创建新图层元数据
        boolean metadataInit = MbtilesUtils.initMetadata(dataSource,
                "name", layerName,
                "format", detectImageFormat(config),
                "version", "1.0",
                "type", "overlay"
        );

        if (metadataInit) {
            log.info("创建图层元数据成功: {}", layerName);
        } else {
            log.warn("创建图层元数据失败: {}", layerName);
        }

        return metadataInit;
    }

    /**
     * 检查图层是否已存在
     */
    private static boolean layerExists(DruidDataSource dataSource, String layerName) {
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
     * 检测图片格式
     */
    private static String detectImageFormat(ConvertConfig config) {
        String template = config.getPathTemplate().toLowerCase();
        if (template.contains(".png")) {
            return "png";
        } else if (template.contains(".jpg") || template.contains(".jpeg")) {
            return "jpg";
        } else if (template.contains(".webp")) {
            return "webp";
        } else if (template.contains(".gif")) {
            return "gif";
        } else {
            return "png"; // 默认
        }
    }

    /**
     * 扫描所有层级
     */
    private static List<Integer> scanZoomLevels(ConvertConfig config) {
        List<Integer> zoomLevels = new ArrayList<>();
        File sourceDir = new File(config.getSourceRoot());

        // 尝试扫描数字目录
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
     * 转换统计
     */
    private static class ConvertStats {
        long total = 0;
        long success = 0;
        long failed = 0;
        long skipped = 0;
        long totalSize = 0;
    }

    /**
     * 转换瓦片
     */
    private static ConvertStats convertTiles(ConvertConfig config, DruidDataSource dataSource, List<Integer> zoomLevels) {
        ConvertStats stats = new ConvertStats();

        String insertSql = config.isOverwrite()
                ? "INSERT OR REPLACE INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)"
                : "INSERT OR IGNORE INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)";

        List<Object[]> batchArgs = new ArrayList<>(config.getBatchSize());

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            for (int z : zoomLevels) {
                log.info("处理层级: z={}", z);
                long layerStartTime = System.currentTimeMillis();
                long layerCount = 0;

                // 扫描该层级下的所有瓦片
                List<TileInfo> tiles = scanTiles(config, z);
                stats.total += tiles.size();
                log.info("层级 z={} 扫描到 {} 个瓦片", z, tiles.size());

                for (TileInfo tile : tiles) {
                    try {
                        // 读取瓦片数据
                        byte[] data = Files.readAllBytes(tile.path);
                        if (data.length == 0) {
                            stats.failed++;
                            continue;
                        }

                        // 计算存储 Y（根据需要翻转）
                        int storeY = FuserCacheUtils.getStoreY(z, tile.y, config.isNeedReverseY());

                        // 添加到批量
                        batchArgs.add(new Object[]{z, tile.x, storeY, data});
                        stats.totalSize += data.length;

                        if (batchArgs.size() >= config.getBatchSize()) {
                            int[] results = executeBatch(conn, insertSql, batchArgs);
                            stats.success += results[0];
                            stats.skipped += results[1];
                            stats.failed += results[2];
                            batchArgs.clear();
                        }

                        layerCount++;

                    } catch (IOException e) {
                        log.debug("读取瓦片文件失败: {}", tile.path, e);
                        stats.failed++;
                    }
                }

                // 执行剩余的批量插入
                if (!batchArgs.isEmpty()) {
                    int[] results = executeBatch(conn, insertSql, batchArgs);
                    stats.success += results[0];
                    stats.skipped += results[1];
                    stats.failed += results[2];
                    batchArgs.clear();
                }

                conn.commit();

                log.info("层级 z={} 完成: 总数={}, 耗时={}ms",
                        z, layerCount, System.currentTimeMillis() - layerStartTime);
            }

            log.info("所有层级转换完成: 总数={}, 成功={}, 跳过={}, 失败={}",
                    stats.total, stats.success, stats.skipped, stats.failed);

        } catch (SQLException e) {
            log.error("数据库操作失败", e);
        }

        return stats;
    }

    /**
     * 瓦片信息
     */
    private static class TileInfo {
        int z;
        int x;
        int y;
        Path path;
    }

    // ==================== 路径处理方法 ====================


    /**
     * 构建完整的文件路径（用于扫描层级目录）
     */
    private static String buildFullPath(String sourceRoot, String childPath) {
        String normalizedRoot = normalizePath(sourceRoot);
        String normalizedChild = normalizePath(childPath);
        return concatPath(normalizedRoot, normalizedChild);
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


    /**
     * 递归遍历并匹配路径模板
     */
    private static void traverseAndMatch(File dir, String currentPath, String template,
                                         int z, List<TileInfo> tiles, ConvertConfig config) {
        if (template == null || template.isEmpty()) {
            // 没有模板了，当前目录下所有文件都是瓦片
            scanAllFiles(dir, z, currentPath, tiles, config);
            return;
        }

        // 分割模板
        String normalizedTemplate = normalizePath(template);
        int idx = normalizedTemplate.indexOf(File.separatorChar);
        String firstPart = idx > 0 ? normalizedTemplate.substring(0, idx) : normalizedTemplate;
        String restPart = idx > 0 ? normalizedTemplate.substring(idx + 1) : null;

        // 如果第一部分包含 {x} 或 {y}，说明是占位符，需要匹配多个
        if (firstPart.contains(PLACEHOLDER_X) || firstPart.contains(PLACEHOLDER_Y)) {
            // 这是文件名部分，匹配所有文件
            matchFiles(dir, firstPart, z, currentPath, tiles, config);
        } else if (firstPart.contains(PLACEHOLDER_Z)) {
            // 不应该还有 {z}
            log.warn("模板中仍有 {z} 占位符: {}", firstPart);
        } else {
            // 这是具体目录名，直接进入
            File subDir = new File(dir, firstPart);
            if (subDir.exists() && subDir.isDirectory()) {
                String newPath = currentPath + firstPart + File.separator;
                traverseAndMatch(subDir, newPath, restPart, z, tiles, config);
            } else {
                // 尝试作为占位符处理（可能是数字目录）
                matchDirectories(dir, firstPart, restPart, z, currentPath, tiles, config);
            }
        }
    }

    /**
     * 匹配目录（处理占位符情况）
     */
    private static void matchDirectories(File dir, String dirPattern, String restTemplate,
                                         int z, String currentPath,
                                         List<TileInfo> tiles, ConvertConfig config) {
        File[] subDirs = dir.listFiles(File::isDirectory);
        if (subDirs == null) {
            return;
        }

        for (File subDir : subDirs) {
            try {
                // 尝试解析为数字
                Integer.parseInt(subDir.getName());
                String newPath = currentPath + subDir.getName() + File.separator;
                // 递归处理剩余模板
                traverseAndMatch(subDir, newPath, restTemplate, z, tiles, config);
            } catch (NumberFormatException e) {
                // 跳过非数字目录
            }
        }
    }

    /**
     * 匹配文件
     */
    private static void matchFiles(File dir, String filePattern, int z, String currentPath,
                                   List<TileInfo> tiles, ConvertConfig config) {
        File[] files = dir.listFiles(File::isFile);
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (matchesFileNamePattern(file.getName(), filePattern)) {
                parseAndAddTile(file, z, currentPath, tiles, config);
            }
        }
    }

    /**
     * 扫描所有文件
     */
    private static void scanAllFiles(File dir, int z, String currentPath,
                                     List<TileInfo> tiles, ConvertConfig config) {
        File[] files = dir.listFiles(File::isFile);
        if (files == null) {
            return;
        }

        for (File file : files) {
            parseAndAddTile(file, z, currentPath, tiles, config);
        }
    }

    /**
     * 扫描瓦片文件 - 递归遍历目录
     * 根据路径模板递归查找所有瓦片文件
     */
    private static List<TileInfo> scanTiles(ConvertConfig config, int z) {
        List<TileInfo> tiles = new ArrayList<>();

        // 1. 构建 z 层级的路径（只替换 {z}）
        String template = config.getPathTemplate();
        String zPath = template.replace(PLACEHOLDER_Z, String.valueOf(z));


        String zRootPath = getZRootPath(zPath);
        String fullDirPath = buildFullPath(config.getSourceRoot(), zRootPath);

        File zRootDir = new File(fullDirPath);
        if (!zRootDir.exists() || !zRootDir.isDirectory()) {
            log.warn("层级根目录不存在: {}", fullDirPath);
            return tiles;
        }

        // 3. 获取剩余路径模板（用于匹配子目录和文件）
        // 例如：{y}/{x}.png
        String remainingTemplate = getRemainingPath(zPath, zRootPath);

        // 4. 递归扫描目录，匹配路径模板
        scanDirectoryWithTemplate(zRootDir, "", remainingTemplate, z, tiles, config);

        return tiles;
    }

    /**
     * 获取 z 层级的根目录路径（只包含第一层目录）
     * 例如：{z}/{y}/{x}.png -> 替换 z 后为 3/{y}/{x}.png
     * 返回：3/
     */
    private static String getZRootPath(String zPath) {
        String normalized = normalizePath(zPath);
        int firstSeparator = normalized.indexOf(File.separatorChar);
        if (firstSeparator > 0) {
            // 只取第一个目录
            return normalized.substring(0, firstSeparator + 1);
        }
        // 如果没有目录分隔符，说明模板就是 {z}.xxx，直接返回
        return normalized;
    }

    /**
     * 获取剩余路径模板（去掉 z 层级根目录）
     * 例如：3/{y}/{x}.png -> 返回 {y}/{x}.png
     */
    private static String getRemainingPath(String fullPath, String rootPath) {
        String normalized = normalizePath(fullPath);
        String normalizedRoot = normalizePath(rootPath);

        if (normalized.startsWith(normalizedRoot)) {
            String remaining = normalized.substring(normalizedRoot.length());
            // 移除开头的斜杠
            while (remaining.startsWith(File.separator)) {
                remaining = remaining.substring(1);
            }
            return remaining;
        }
        return normalized;
    }

    /**
     * 递归扫描目录，根据路径模板匹配文件和子目录
     */
    private static void scanDirectoryWithTemplate(File dir, String currentPath,
                                                  String template, int z,
                                                  List<TileInfo> tiles,
                                                  ConvertConfig config) {
        if (template == null || template.isEmpty()) {
            // 如果模板为空，说明当前目录下应该直接是瓦片文件
            scanTileFiles(dir, z, tiles);
            return;
        }

        // 解析模板的第一部分
        String normalizedTemplate = normalizePath(template);
        int firstSeparator = normalizedTemplate.indexOf(File.separatorChar);

        String firstPart;
        String remainingTemplate;

        if (firstSeparator > 0) {
            firstPart = normalizedTemplate.substring(0, firstSeparator);
            remainingTemplate = normalizedTemplate.substring(firstSeparator + 1);
        } else {
            firstPart = normalizedTemplate;
            remainingTemplate = null;
        }

        // 判断第一部分是目录还是文件
        boolean isDirectory = remainingTemplate != null && !remainingTemplate.isEmpty();

        if (isDirectory) {
            // 第一部分是目录，需要匹配子目录
            // firstPart 可能是 {y} 或具体的目录名
            if (firstPart.equals(PLACEHOLDER_Y) || firstPart.equals(PLACEHOLDER_X)) {
                // 如果是占位符，遍历所有子目录
                File[] subDirs = dir.listFiles(File::isDirectory);
                if (subDirs != null) {
                    for (File subDir : subDirs) {
                        try {
                            // 尝试解析为数字
                            int value = Integer.parseInt(subDir.getName());
                            String newPath = currentPath + subDir.getName() + File.separator;

                            // 确定这个目录对应的是 x 还是 y
                            // 根据模板中的位置来判断
                            if (firstPart.equals(PLACEHOLDER_Y)) {
                                // 这个目录是 y
                                scanDirectoryWithTemplate(subDir, newPath, remainingTemplate, z, tiles, config);
                            } else {
                                // 这个目录是 x
                                scanDirectoryWithTemplate(subDir, newPath, remainingTemplate, z, tiles, config);
                            }
                        } catch (NumberFormatException e) {
                            // 跳过非数字目录
                            continue;
                        }
                    }
                }
            } else {
                // 如果是具体目录名，直接匹配
                File targetDir = new File(dir, firstPart);
                if (targetDir.exists() && targetDir.isDirectory()) {
                    String newPath = currentPath + firstPart + File.separator;
                    scanDirectoryWithTemplate(targetDir, newPath, remainingTemplate, z, tiles, config);
                }
            }
        } else {
            // 第一部分是文件，匹配瓦片文件
            // firstPart 应该是 {x}.png 或 {y}.png 或具体文件名
            if (firstPart.contains(PLACEHOLDER_X) || firstPart.contains(PLACEHOLDER_Y)) {
                // 匹配文件
                scanTileFilesWithPattern(dir, firstPart, z, currentPath, tiles, config);
            } else {
                // 具体文件名，直接匹配
                File targetFile = new File(dir, firstPart);
                if (targetFile.exists() && targetFile.isFile()) {
                    parseAndAddTile(targetFile, z, currentPath, tiles, config);
                }
            }
        }
    }

    /**
     * 扫描目录下的瓦片文件（匹配文件名模式）
     */
    private static void scanTileFilesWithPattern(File dir, String fileNamePattern,
                                                 int z, String currentPath,
                                                 List<TileInfo> tiles,
                                                 ConvertConfig config) {
        File[] files = dir.listFiles(File::isFile);
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (matchesFileNamePattern(file.getName(), fileNamePattern)) {
                parseAndAddTile(file, z, currentPath, tiles, config);
            }
        }
    }

    /**
     * 扫描目录下的所有瓦片文件（没有模板限制）
     */
    private static void scanTileFiles(File dir, int z, List<TileInfo> tiles) {
        File[] files = dir.listFiles(File::isFile);
        if (files == null) {
            return;
        }

        for (File file : files) {
            TileInfo tile = new TileInfo();
            tile.z = z;
            tile.path = file.toPath();
            // 从文件名和路径中解析 x 和 y
            parseXYFromPath(file, tile);
            if (tile.x >= 0 && tile.y >= 0) {
                tiles.add(tile);
            }
        }
    }

    /**
     * 从文件路径解析 x 和 y
     */
    private static void parseXYFromPath(File file, TileInfo tile) {
        // 从父目录获取 y
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            try {
                tile.y = Integer.parseInt(parentDir.getName());
            } catch (NumberFormatException e) {
                // 尝试从路径中提取
            }
        }

        // 从文件名获取 x
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            try {
                tile.x = Integer.parseInt(fileName.substring(0, dotIndex));
            } catch (NumberFormatException e) {
                tile.x = -1;
            }
        }
    }

    /**
     * 解析并添加瓦片
     */
    private static void parseAndAddTile(File file, int z, String currentPath,
                                        List<TileInfo> tiles, ConvertConfig config) {
        try {
            TileInfo tile = new TileInfo();
            tile.z = z;
            tile.path = file.toPath();

            // 从当前路径和文件名中解析 x 和 y
            String fullRelativePath = tile.z + File.separator + currentPath + file.getName();
            String template = config.getPathTemplate();
            String normalizedTemplate = normalizePath(template);
            String normalizedPath = normalizePath(fullRelativePath);

            // 构建正则表达式提取 x 和 y
            Pattern pattern = buildExtractPattern(normalizedTemplate);
            Matcher matcher = pattern.matcher(normalizedPath);

            if (matcher.matches()) {
                int xGroup = getGroupOrder(template, PLACEHOLDER_X);
                int yGroup = getGroupOrder(template, PLACEHOLDER_Y);

                tile.x = Integer.parseInt(matcher.group(xGroup - 1));
                tile.y = Integer.parseInt(matcher.group(yGroup - 1));
                tiles.add(tile);
            }
        } catch (Exception e) {
            log.debug("解析瓦片失败: {}", file.getPath(), e);
        }
    }


    /**
     * 检查文件名是否匹配模式
     */
    private static boolean matchesFileNamePattern(String fileName, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }

        // 将模式转换为正则表达式
        // 先转义整个 pattern，然后替换转义后的占位符

        String regex = pattern
                .replace("{x}", "\\d+")    // 注意：Pattern.quote("{x}") 的结果是 "\{x\}"
                .replace("{y}", "\\d+")
                .replace("{z}", "\\d+");

        return fileName.matches(regex);
    }

    /**
     * 构建提取 x 和 y 的正则表达式
     */
    private static Pattern buildExtractPattern(String template) {
        // 转义特殊字符，但保留占位符用于替换
        String regex = template
                .replace("\\", "\\\\")  // 转义反斜杠
                .replace(".", "\\.")    // 转义点号
                .replace(PLACEHOLDER_Z, "\\d+")     // {z} 匹配数字但不捕获
                .replace(PLACEHOLDER_X, "(\\d+)")   // {x} 捕获数字
                .replace(PLACEHOLDER_Y, "(\\d+)");  // {y} 捕获数字

        return Pattern.compile(regex);
    }

    /**
     * 计算占位符在模板中的顺序（第几个占位符）
     */
    private static int getGroupOrder(String template, String placeholder) {
        int order = 1;
        int index = template.indexOf(placeholder);

        // 统计在 placeholder 之前有多少个占位符
        String before = template.substring(0, index);
        int count = 0;
        int pos = 0;
        while (true) {
            int start = before.indexOf('{', pos);
            if (start == -1) break;
            int end = before.indexOf('}', start);
            if (end == -1) break;
            count++;
            pos = end + 1;
        }

        return count + 1;
    }

    // ==================== 数据库操作方法 ====================

    /**
     * 执行批量插入
     *
     * @return [success, skipped, failed]
     */
    private static int[] executeBatch(Connection conn, String sql, List<Object[]> batchArgs) throws SQLException {
        if (batchArgs.isEmpty()) {
            return new int[]{0, 0, 0};
        }

        int success = 0;
        int skipped = 0;
        int failed = 0;

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
        } catch (SQLException e) {
            log.error("批量插入失败", e);
            failed = batchArgs.size();
        }

        return new int[]{success, skipped, failed};
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
        Thread deleteThread = new Thread(() -> {
            try {
                Files.walk(path)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                log.error("删除文件/目录失败: {}", p, e);
                            }
                        });
                log.info("异步删除临时目录成功: {}", path);
            } catch (IOException e) {
                log.error("异步删除临时目录失败: {}", path, e);
            }
        });
        deleteThread.setDaemon(true);
        deleteThread.setName("tile-convert-delete-" + System.currentTimeMillis());
        deleteThread.start();
    }

    // ==================== 便捷方法 ====================

    /**
     * 转换指定层级
     */
    public static ConvertResult convertWithZoomLevels(String sourceRoot, String pathTemplate,
                                                      String mbtilesPath, String layerName,
                                                      List<Integer> zoomLevels) {
        ConvertConfig config = new ConvertConfig()
                .setSourceRoot(sourceRoot)
                .setPathTemplate(pathTemplate)
                .setMbtilesPath(mbtilesPath)
                .setLayerName(layerName)
                .setZoomLevels(zoomLevels);
        return convert(config);
    }

    /**
     * 转换并追加到已有 MBTiles
     */
    public static ConvertResult convertAppend(String sourceRoot, String pathTemplate,
                                              String mbtilesPath, String layerName) {
        ConvertConfig config = new ConvertConfig()
                .setSourceRoot(sourceRoot)
                .setPathTemplate(pathTemplate)
                .setMbtilesPath(mbtilesPath)
                .setLayerName(layerName)
                .setOverwrite(false);
        return convert(config);
    }

    /**
     * 转换并覆盖已有瓦片
     */
    public static ConvertResult convertOverwrite(String sourceRoot, String pathTemplate,
                                                 String mbtilesPath, String layerName) {
        ConvertConfig config = new ConvertConfig()
                .setSourceRoot(sourceRoot)
                .setPathTemplate(pathTemplate)
                .setMbtilesPath(mbtilesPath)
                .setLayerName(layerName)
                .setOverwrite(true);
        return convert(config);
    }

    /**
     * 转换并删除源文件
     */
    public static ConvertResult convertAndDelete(String sourceRoot, String pathTemplate,
                                                 String mbtilesPath, String layerName) {
        ConvertConfig config = new ConvertConfig()
                .setSourceRoot(sourceRoot)
                .setPathTemplate(pathTemplate)
                .setMbtilesPath(mbtilesPath)
                .setLayerName(layerName)
                .setDeleteSourceAfterConvert(true);
        return convert(config);
    }

    public static void main(String[] args) {
        // 1. 基本用法 - 转换所有瓦片到 MBTiles
        TileToMbtilesConverter.ConvertResult result = TileToMbtilesConverter.convert(
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1-13",  // 源根路径
                "{z}\\{y}\\{x}.png",                     // 路径模板
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1_13.mbtiles",          // MBTiles 文件路径
                "1_13",// 图层名称
                true
        );
    }
}
