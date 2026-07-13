package cn.geoair.map.tile.forge.core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 瓦片路径解析工具类（支持XYZ/LCR/ArcGIS紧凑型切片）
 */
public class TilePathParser {

    // ------------------------------ XYZ格式解析 ------------------------------

    /**
     * 解析XYZ格式瓦片路径（如："15/1234/5678.png" 或 "prefix/15/1234/5678.jpg"）
     */
    public static XyzTileInfo parseXyzPath(String fullPath, String tilePathPrefix) {
        if (fullPath == null || tilePathPrefix == null) {
            throw new IllegalArgumentException("路径或前缀不能为空");
        }
        String relativePath = fullPath.replace(tilePathPrefix, "");
        return parseXyzPath(relativePath);
    }

    /**
     * 解析XYZ格式瓦片相对路径（重载方法）
     */
    public static XyzTileInfo parseXyzPath(String relativePath) {
        Pattern tilePattern = Pattern.compile("(^|.*/)(\\d+)/(\\d+)/(\\d+)(/[^/.]+)?\\.[^/]+$");
        Matcher matcher1 = tilePattern.matcher(relativePath);
        if (matcher1.find()) {
            long z = Long.parseLong(matcher1.group(2));
            long x = Long.parseLong(matcher1.group(3));
            long y = Long.parseLong(matcher1.group(4));
            String fileName = extractFileName(relativePath);
            return new XyzTileInfo(z, x, y, fileName, relativePath);
        }
        return null; // 解析失败返回null
    }

    // ------------------------------ ArcGIS LCR格式解析 ------------------------------

    /**
     * 解析ArcGIS LCR格式瓦片路径（如："L01/C11/R11.png" 或 "prefix/L01/C11/R11.png"）
     */
    public static LcrTileInfo parseLcrPath(String fullPath, String tilePathPrefix) {
        if (fullPath == null || tilePathPrefix == null) {
            throw new IllegalArgumentException("路径或前缀不能为空");
        }
        String relativePath = fullPath.replace(tilePathPrefix, "");
        return parseLcrPath(relativePath);
    }

    /**
     * 解析ArcGIS LCR格式瓦片相对路径（重载方法）
     */
    public static LcrTileInfo parseLcrPath(String relativePath) {
        // 匹配路径末尾的 L{数字}/C{数字}/R{数字}.后缀 结构
        Pattern lcrPattern = Pattern.compile("L(\\d+)/C(\\d+)/R(\\d+)\\.[^/]+$");
        Matcher matcher = lcrPattern.matcher(relativePath);

        long level = 0, column = 0, row = 0;
        if (matcher.find()) {
            level = Long.parseLong(matcher.group(1));
            column = Long.parseLong(matcher.group(2));
            row = Long.parseLong(matcher.group(3));
            String fileName = extractFileName(relativePath);
            return new LcrTileInfo(level, column, row, fileName, relativePath);
        } else {
            return null;
        }


    }

    // ------------------------------ ArcGIS紧凑型切片解析 ------------------------------

    /**
     * 解析ArcGIS紧凑型切片（.bundle文件）路径及瓦片索引
     * 格式示例：_alllayers/L01/R00000000/C00000000.bundle 或 L01/R0/C0.bundle
     *
     * @param bundlePath        bundle文件完整路径
     * @param tileIndexInBundle 瓦片在bundle中的索引（0~16383，对应bundle内256x256切片矩阵）
     * @return 紧凑型切片信息
     */
    public static CompactCacheTileInfo parseCompactCachePath(String bundlePath, int tileIndexInBundle) {
        if (bundlePath == null || !bundlePath.endsWith(".bundle")) {
            throw new IllegalArgumentException("无效的bundle文件路径");
        }
        if (tileIndexInBundle < 0 || tileIndexInBundle > 16383) {
            throw new IllegalArgumentException("瓦片索引需在0~16383范围内（256x256矩阵）");
        }

// 匹配bundle路径末尾的 L{level}/R{bundleRow}/C{bundleCol}.bundle 结构
        Pattern bundlePattern = Pattern.compile("L(\\d+)/R(\\d+)/C(\\d+)\\.bundle$");
        Matcher bundleMatcher = bundlePattern.matcher(bundlePath);

        long level = 0, bundleRow = 0, bundleCol = 0;
        if (bundleMatcher.find()) {
            level = Long.parseLong(bundleMatcher.group(1));
            bundleRow = Long.parseLong(bundleMatcher.group(2));
            bundleCol = Long.parseLong(bundleMatcher.group(3));
        }

// 计算bundle内瓦片的实际行列号（每个bundle包含256x256个瓦片）
        long tileRowInBundle = tileIndexInBundle / 256;
        long tileColInBundle = tileIndexInBundle % 256;
        long actualTileRow = bundleRow * 256 + tileRowInBundle;
        long actualTileCol = bundleCol * 256 + tileColInBundle;

        String bundleFileName = extractFileName(bundlePath);
        return new CompactCacheTileInfo(
                level, bundleRow, bundleCol,
                tileIndexInBundle, actualTileRow, actualTileCol,
                bundleFileName, bundlePath
        );
    }

    //    private static final Pattern PATH_PATTERN = Pattern.compile(
//            ".*/(L\\d+/R\\d+C\\d+\\.(?:bundle|bundlx)|conf\\.(?:xml|cdi|json))",Pattern.CASE_INSENSITIVE
//    );
    private static final Pattern PATH_PATTERN = Pattern.compile(".*/(L\\d+/[^/]+\\.(?:bundle|bundlx)|conf\\.(?:xml|cdi|json|properties))", Pattern.CASE_INSENSITIVE);

    public static String getSubBundlePath(String bundlePath) {

        // 空值校验
        if (bundlePath == null || bundlePath.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = PATH_PATTERN.matcher(bundlePath);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    public static void main(String[] args) {
        String[] testPaths = {
                "_alllayers/L04/R0000C0000.bundle",
                "_alllayers/L04/R0000C0f000.bundle",
                "layers/L10/R001C002.bundlx",
                "config/conf.xml",
                "data/conf.cdi",
                "settings/conf.json",
                "invalid/path.txt",
                null,
                "",
                "MIXED/CONF.XML", // 大小写混合
                "root/L05/R123C456.bundle/subpath" // 路径后有多余内容
        };
        for (String path : testPaths) {
            String result = getSubBundlePath(path);


            System.out.printf("输入路径: %-40s | 普通匹配: %-30s",
                    (path == null ? "null" : path),
                    (result == null ? "无匹配" : result))
            ;
        }
    }
    // ------------------------------ 通用工具方法 ------------------------------

    /**
     * 从路径中提取文件名（含后缀）
     */
    private static String extractFileName(String path) {
        if (path == null) return "";
        String[] parts = path.split("[\\\\/]");
        return parts.length > 0 ? parts[parts.length - 1] : "";
    }

    // ------------------------------ 实体类：XYZ格式瓦片信息 ------------------------------
    public static class XyzTileInfo {
        private final long z;          // 层级
        private final long x;          // 横坐标（列）
        private final long y;          // 纵坐标（行）
        private final String fileName; // 文件名（含后缀）
        private final String originalPath; // 原始路径

        public XyzTileInfo(long z, long x, long y, String fileName, String originalPath) {
            this.z = z;
            this.x = x;
            this.y = y;
            this.fileName = fileName;
            this.originalPath = originalPath;
        }

        // Getter方法
        public long getZ() {
            return z;
        }

        public long getX() {
            return x;
        }

        public long getY() {
            return y;
        }

        public String getFileName() {
            return fileName;
        }

        public String getOriginalPath() {
            return originalPath;
        }

        @Override
        public String toString() {
            return String.format("XyzTileInfo{z=%d, x=%d, y=%d, fileName='%s', originalPath='%s'}",
                    z, x, y, fileName, originalPath);
        }
    }

    // ------------------------------ 实体类：ArcGIS LCR格式瓦片信息 ------------------------------
    public static class LcrTileInfo {
        private final long level;      // 层级（对应XYZ的z）
        private final long column;     // 列号（对应XYZ的x）
        private final long row;        // 行号（对应XYZ的y）
        private final String fileName; // 文件名（含后缀）
        private final String originalPath; // 原始路径
        private final String lcrZxy;   // 带LCR前缀的ZXY字符串（如L01/C11/R11）

        public LcrTileInfo(long level, long column, long row, String fileName, String originalPath) {
            this.level = level;
            this.column = column;
            this.row = row;
            this.fileName = fileName;
            this.originalPath = originalPath;
            // 自动生成带LCR前缀的ZXY字符串（补零格式与原始路径一致）
            this.lcrZxy = generateLcrZxy(originalPath, level, column, row);
        }

        private String generateLcrZxy(String originalPath, long level, long column, long row) {
            // 匹配原始路径中的L/C/R后的数字格式（如L01→两位，L1→一位）
            Pattern lPattern = Pattern.compile("L(\\d+)");
            Pattern cPattern = Pattern.compile("C(\\d+)");
            Pattern rPattern = Pattern.compile("R(\\d+)");

            String lPart = "L" + getPaddedNumber(originalPath, lPattern, level);
            String cPart = "C" + getPaddedNumber(originalPath, cPattern, column);
            String rPart = "R" + getPaddedNumber(originalPath, rPattern, row);

            return String.format("%s/%s/%s", lPart, cPart, rPart);
        }

        /**
         * 根据原始路径中的数字格式补零
         */
        private String getPaddedNumber(String path, Pattern pattern, long number) {
            Matcher matcher = pattern.matcher(path);
            if (matcher.find()) {
                String originalNum = matcher.group(1);
                int length = originalNum.length();
                // 按原始长度补零
                return String.format("%0" + length + "d", number);
            }
            // 默认补两位
            return String.format("%02d", number);
        }

        // Getter方法
        public long getLevel() {
            return level;
        }

        public long getColumn() {
            return column;
        }

        public long getRow() {
            return row;
        }

        public String getFileName() {
            return fileName;
        }

        public String getOriginalPath() {
            return originalPath;
        }

        public String getLcrZxy() {
            return lcrZxy;
        }

        // 兼容XYZ格式的映射
        public long getZ() {
            return level;
        }

        public long getX() {
            return column;
        }

        public long getY() {
            return row;
        }

        @Override
        public String toString() {
            return String.format("LcrTileInfo{level=%d, column=%d, row=%d, fileName='%s', originalPath='%s', lcrZxy='%s'}",
                    level, column, row, fileName, originalPath, lcrZxy);
        }
    }

    // ------------------------------ 实体类：ArcGIS紧凑型切片信息 ------------------------------
    public static class CompactCacheTileInfo {
        private final long level;          // 层级（对应XYZ的z）
        private final long bundleRow;      // bundle行号（大区块行）
        private final long bundleCol;      // bundle列号（大区块列）
        private final int tileIndexInBundle; // 瓦片在bundle内的索引（0~16383）
        private final long actualTileRow;  // 瓦片实际行号（对应XYZ的y）
        private final long actualTileCol;  // 瓦片实际列号（对应XYZ的x）
        private final String bundleFileName; // bundle文件名（如C00000000.bundle）
        private final String originalBundlePath; // bundle原始路径

        public CompactCacheTileInfo(long level, long bundleRow, long bundleCol,
                                    int tileIndexInBundle, long actualTileRow, long actualTileCol,
                                    String bundleFileName, String originalBundlePath) {
            this.level = level;
            this.bundleRow = bundleRow;
            this.bundleCol = bundleCol;
            this.tileIndexInBundle = tileIndexInBundle;
            this.actualTileRow = actualTileRow;
            this.actualTileCol = actualTileCol;
            this.bundleFileName = bundleFileName;
            this.originalBundlePath = originalBundlePath;
        }

        // Getter方法（兼容XYZ格式的映射）
        public long getZ() {
            return level;
        }

        public long getX() {
            return actualTileCol;
        }

        public long getY() {
            return actualTileRow;
        }

        public long getLevel() {
            return level;
        }

        public long getBundleRow() {
            return bundleRow;
        }

        public long getBundleCol() {
            return bundleCol;
        }

        public int getTileIndexInBundle() {
            return tileIndexInBundle;
        }

        public long getActualTileRow() {
            return actualTileRow;
        }

        public long getActualTileCol() {
            return actualTileCol;
        }

        public String getBundleFileName() {
            return bundleFileName;
        }

        public String getOriginalBundlePath() {
            return originalBundlePath;
        }

        @Override
        public String toString() {
            return String.format("CompactCacheTileInfo{level=%d, bundleRow=%d, bundleCol=%d, " +
                                 "tileIndexInBundle=%d, actualTileRow=%d, actualTileCol=%d, " +
                                 "bundleFileName='%s', originalBundlePath='%s'}",
                    level, bundleRow, bundleCol, tileIndexInBundle,
                    actualTileRow, actualTileCol, bundleFileName, originalBundlePath);
        }
    }
}
