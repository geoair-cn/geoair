package cn.geoair.map.tile.forge.fuser.utils;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import java.awt.image.BufferedImage;

/** 瓦片空白检测工具类 用于检测瓦片图像中是否存在大面积空白矩形区域 */
public class TileBlankDetector {
    private static GiLogger log = GirLoggerFactory.getLogger();
    // 配置参数
    private static int MIN_TILE_SIZE = 50; // 最小瓦片尺寸
    private static int SAMPLE_DIVISOR = 10; // 采样步长除数（调小以提高精度）
    private static double MIN_BLANK_RECT_RATIO = 0.15; // 最小空白矩形占比（15%）
    private static double ROW_BLANK_THRESHOLD = 0.3; // 行空白率阈值（放宽到70%）
    private static double COL_BLANK_THRESHOLD = 0.3; // 列空白率阈值（放宽到70%）
    private static double WINDOW_BLANK_THRESHOLD = 0.75; // 窗口空白率阈值

    /**
     * 快速检测瓦片是否存在大面积空白矩形区域（推荐使用）
     *
     * @param imageBytes 图片字节数组
     * @param format 图片格式
     * @return true表示存在大面积空白矩形区域
     */
    public static LargeBlankCheck hasLargeBlankRect(byte[] imageBytes, String format) {
        return hasLargeBlankRect(imageBytes, format, null);
    }

    /**
     * 检测瓦片是否存在大面积空白矩形区域（带日志记录）
     *
     * @param imageBytes 图片字节数组
     * @param format 图片格式
     * @param tileInfo 瓦片信息（用于日志记录）
     * @return true表示存在大面积空白矩形区域
     */
    public static LargeBlankCheck hasLargeBlankRect(
            byte[] imageBytes, String format, String tileInfo) {
        if (imageBytes == null || imageBytes.length == 0) {
            if (tileInfo != null) {
                log.debug("瓦片数据为空: {}", tileInfo);
            }
            return LargeBlankCheck.of().setBlankIs(true);
        }

        try {
            BufferedImage image = TileImageUtils.readImage(imageBytes);

            if (image == null) {
                if (tileInfo != null) {
                    log.debug("无法解析瓦片图像: {}", tileInfo);
                }
                return LargeBlankCheck.of().setBlankIs(true).setImage(image);
            }

            int width = image.getWidth();
            int height = image.getHeight();
            int tileSize = Math.min(width, height);

            // 瓦片尺寸太小，直接判定为空白
            if (tileSize < MIN_TILE_SIZE) {
                if (tileInfo != null) {
                    log.debug("瓦片尺寸过小: {}x{}, {}", width, height, tileInfo);
                }
                return LargeBlankCheck.of().setBlankIs(true).setImage(image);
            }

            // 1. 快速检测：如果瓦片完全空白，直接返回true
            if (isCompletelyBlank(image, format)) {
                if (tileInfo != null) {
                    log.debug("瓦片完全空白: {}", tileInfo);
                }
                return LargeBlankCheck.of().setBlankIs(true).setImage(image);
            }

            // 2. 采样分析检测空白矩形区域
            boolean result = detectBlankRectBySampling(image, width, height, format);

            if (result && tileInfo != null) {
                log.debug("检测到空白矩形瓦片: {}", tileInfo);
            }
            return LargeBlankCheck.of().setBlankIs(result).setImage(image);

        } catch (Exception e) {
            log.warn("检测空白矩形失败: {}", e.getMessage());
            return LargeBlankCheck.of().setBlankIs(false);
        }
    }

    /** 快速检测瓦片是否完全空白 */
    private static boolean isCompletelyBlank(BufferedImage image, String format) {
        int width = image.getWidth();
        int height = image.getHeight();

        int step = Math.max(5, Math.min(width, height) / 10);
        int totalSamples = 0;
        int blankSamples = 0;

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                totalSamples++;
                int rgb = image.getRGB(x, y);
                if (isBlankPixel(rgb, format)) {
                    blankSamples++;
                }
            }
        }

        return totalSamples > 0 && (double) blankSamples / totalSamples >= 0.90;
    }

    /** 通过采样检测空白矩形区域（优化版） */
    private static boolean detectBlankRectBySampling(
            BufferedImage image, int width, int height, String format) {
        // 采样检测，步长根据瓦片大小动态调整（调小步长提高精度）
        int step = Math.max(1, Math.min(width, height) / SAMPLE_DIVISOR);

        // 计算采样矩阵尺寸
        int totalRows = height / step;
        int totalCols = width / step;

        if (totalRows < 4 || totalCols < 4) {
            step = Math.max(1, step / 2);
            totalRows = height / step;
            totalCols = width / step;
        }

        // 统计每行和每列的空白密度
        int[] rowBlankCount = new int[totalRows];
        int[] colBlankCount = new int[totalCols];
        int totalBlank = 0;

        for (int y = 0, ry = 0; y < height && ry < totalRows; y += step, ry++) {
            for (int x = 0, cx = 0; x < width && cx < totalCols; x += step, cx++) {
                int rgb = image.getRGB(x, y);
                boolean isBlank = isBlankPixel(rgb, format);
                if (isBlank) {
                    rowBlankCount[ry]++;
                    colBlankCount[cx]++;
                    totalBlank++;
                }
            }
        }

        int totalSamples = totalRows * totalCols;

        // 如果总空白像素太少（少于10%），直接返回false
        if (totalSamples == 0 || (double) totalBlank / totalSamples < 0.05) {
            return false;
        }

        // 计算每行的空白率
        double[] rowBlankRatio = new double[totalRows];
        for (int i = 0; i < totalRows; i++) {
            rowBlankRatio[i] = (double) rowBlankCount[i] / totalCols;
        }

        // 计算每列的空白率
        double[] colBlankRatio = new double[totalCols];
        for (int i = 0; i < totalCols; i++) {
            colBlankRatio[i] = (double) colBlankCount[i] / totalRows;
        }

        // 方法1：检测连续空白行和列
        if (detectByRowCol(rowBlankRatio, colBlankRatio, totalRows, totalCols)) {
            return true;
        }

        // 方法2：检测大块矩形区域（专门针对下半部分空白的情况）
        if (detectLargeRectRegion(rowBlankRatio, colBlankRatio, totalRows, totalCols)) {
            return true;
        }

        // 方法3：滑动窗口检测
        return detectBlankWindow(rowBlankRatio, colBlankRatio, totalRows, totalCols);
    }

    /** 方法1：检测连续空白行和列 */
    private static boolean detectByRowCol(
            double[] rowBlankRatio, double[] colBlankRatio, int totalRows, int totalCols) {
        // 查找连续的空白行（空白率 >= 阈值）
        int maxBlankRows = findMaxConsecutive(rowBlankRatio, ROW_BLANK_THRESHOLD);
        int maxBlankCols = findMaxConsecutive(colBlankRatio, COL_BLANK_THRESHOLD);

        int minBlankRows = Math.max(2, (int) (totalRows * MIN_BLANK_RECT_RATIO));
        int minBlankCols = Math.max(2, (int) (totalCols * MIN_BLANK_RECT_RATIO));

        // 如果存在足够大的连续空白行和列，判定为空白矩形
        if (maxBlankRows >= minBlankRows && maxBlankCols >= minBlankCols) {
            return true;
        }

        // 如果行或列其中一个特别大（超过50%），也判定为空白矩形
        if (maxBlankRows >= totalRows * 0.2 && maxBlankCols >= totalCols * 0.15) {
            return true;
        }
        if (maxBlankCols >= totalCols * 0.2 && maxBlankRows >= totalRows * 0.15) {
            return true;
        }

        return false;
    }

    /** 方法2：检测大块矩形区域（专门针对下半部分空白、右上角空白等情况） */
    private static boolean detectLargeRectRegion(
            double[] rowBlankRatio, double[] colBlankRatio, int totalRows, int totalCols) {
        // 检测是否存在一个矩形区域，其空白率很高

        // 1. 检测底部大块空白（下半部分空白）
        int halfRows = totalRows / 2;
        if (halfRows >= 3) {
            // 计算下半部分的平均空白率
            double bottomHalfAvg = 0;
            for (int i = halfRows; i < totalRows; i++) {
                bottomHalfAvg += rowBlankRatio[i];
            }
            bottomHalfAvg /= (totalRows - halfRows);

            // 如果下半部分空白率超过75%，且下半部分高度至少占25%
            if (bottomHalfAvg >= 0.75 && (totalRows - halfRows) >= totalRows * 0.25) {
                // 检查是否有足够的列也是空白的
                int blankColsInBottom = 0;
                for (int i = 0; i < totalCols; i++) {
                    if (colBlankRatio[i] >= 0.5) {
                        blankColsInBottom++;
                    }
                }
                if (blankColsInBottom >= totalCols * 0.3) {
                    return true;
                }
            }
        }

        // 2. 检测顶部大块空白
        int topRows = totalRows / 3;
        if (topRows >= 3) {
            double topAvg = 0;
            for (int i = 0; i < topRows; i++) {
                topAvg += rowBlankRatio[i];
            }
            topAvg /= topRows;
            if (topAvg >= 0.75 && topRows >= totalRows * 0.2) {
                int blankColsInTop = 0;
                for (int i = 0; i < totalCols; i++) {
                    if (colBlankRatio[i] >= 0.5) {
                        blankColsInTop++;
                    }
                }
                if (blankColsInTop >= totalCols * 0.3) {
                    return true;
                }
            }
        }

        // 3. 检测左侧大块空白
        int leftCols = totalCols / 3;
        if (leftCols >= 3) {
            double leftAvg = 0;
            for (int i = 0; i < leftCols; i++) {
                leftAvg += colBlankRatio[i];
            }
            leftAvg /= leftCols;
            if (leftAvg >= 0.75 && leftCols >= totalCols * 0.2) {
                int blankRowsInLeft = 0;
                for (int i = 0; i < totalRows; i++) {
                    if (rowBlankRatio[i] >= 0.5) {
                        blankRowsInLeft++;
                    }
                }
                if (blankRowsInLeft >= totalRows * 0.3) {
                    return true;
                }
            }
        }

        // 4. 检测右侧大块空白
        int rightCols = totalCols / 3;
        if (rightCols >= 3) {
            double rightAvg = 0;
            for (int i = totalCols - rightCols; i < totalCols; i++) {
                rightAvg += colBlankRatio[i];
            }
            rightAvg /= rightCols;
            if (rightAvg >= 0.75 && rightCols >= totalCols * 0.2) {
                int blankRowsInRight = 0;
                for (int i = 0; i < totalRows; i++) {
                    if (rowBlankRatio[i] >= 0.5) {
                        blankRowsInRight++;
                    }
                }
                if (blankRowsInRight >= totalRows * 0.3) {
                    return true;
                }
            }
        }

        // 5. 检测任意位置的大块矩形（使用滑动窗口）
        return detectLargeWindow(rowBlankRatio, colBlankRatio, totalRows, totalCols);
    }

    /** 检测大窗口空白区域（针对大面积矩形） */
    private static boolean detectLargeWindow(
            double[] rowBlankRatio, double[] colBlankRatio, int totalRows, int totalCols) {
        // 窗口大小：至少20%的尺寸
        int windowRows = Math.max(2, (int) (totalRows * 0.2));
        int windowCols = Math.max(2, (int) (totalCols * 0.2));

        if (windowRows > totalRows / 2) {
            windowRows = totalRows / 2;
        }
        if (windowCols > totalCols / 2) {
            windowCols = totalCols / 2;
        }

        // 步长：每次移动窗口的步长
        int stepRows = Math.max(1, windowRows / 4);
        int stepCols = Math.max(1, windowCols / 4);

        for (int y = 0; y <= totalRows - windowRows; y += stepRows) {
            for (int x = 0; x <= totalCols - windowCols; x += stepCols) {
                // 计算窗口内行空白率平均值
                double windowRowAvg = 0;
                int highBlankRows = 0;
                for (int dy = 0; dy < windowRows; dy++) {
                    double rowRatio = rowBlankRatio[y + dy];
                    windowRowAvg += rowRatio;
                    if (rowRatio >= ROW_BLANK_THRESHOLD) {
                        highBlankRows++;
                    }
                }
                windowRowAvg /= windowRows;

                // 计算窗口内列空白率平均值
                double windowColAvg = 0;
                int highBlankCols = 0;
                for (int dx = 0; dx < windowCols; dx++) {
                    double colRatio = colBlankRatio[x + dx];
                    windowColAvg += colRatio;
                    if (colRatio >= COL_BLANK_THRESHOLD) {
                        highBlankCols++;
                    }
                }
                windowColAvg /= windowCols;

                // 判断条件：行和列的平均空白率都达标，且大部分行列都满足条件
                if (windowRowAvg >= WINDOW_BLANK_THRESHOLD
                        && windowColAvg >= WINDOW_BLANK_THRESHOLD
                        && highBlankRows >= windowRows * 0.6
                        && highBlankCols >= windowCols * 0.6) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 查找连续超过阈值的最大长度 */
    private static int findMaxConsecutive(double[] ratios, double threshold) {
        int maxLength = 0;
        int currentLength = 0;

        for (double ratio : ratios) {
            if (ratio >= threshold) {
                currentLength++;
                maxLength = Math.max(maxLength, currentLength);
            } else {
                currentLength = 0;
            }
        }
        return maxLength;
    }

    /** 使用滑动窗口检测空白矩形区域（保留原方法） */
    private static boolean detectBlankWindow(
            double[] rowBlankRatio, double[] colBlankRatio, int totalRows, int totalCols) {
        int windowRows = Math.max(2, (int) (totalRows * 0.15));
        int windowCols = Math.max(2, (int) (totalCols * 0.15));

        if (windowRows > totalRows / 2) {
            windowRows = totalRows / 2;
        }
        if (windowCols > totalCols / 2) {
            windowCols = totalCols / 2;
        }

        int stepRows = Math.max(1, windowRows / 3);
        int stepCols = Math.max(1, windowCols / 3);

        for (int y = 0; y <= totalRows - windowRows; y += stepRows) {
            for (int x = 0; x <= totalCols - windowCols; x += stepCols) {
                double windowBlankRatio = 0;
                int rowCount = 0;
                int colCount = 0;

                for (int dy = 0; dy < windowRows; dy++) {
                    windowBlankRatio += rowBlankRatio[y + dy];
                    if (rowBlankRatio[y + dy] >= ROW_BLANK_THRESHOLD) {
                        rowCount++;
                    }
                }
                windowBlankRatio = windowBlankRatio / windowRows;

                for (int dx = 0; dx < windowCols; dx++) {
                    if (colBlankRatio[x + dx] >= COL_BLANK_THRESHOLD) {
                        colCount++;
                    }
                }

                if (windowBlankRatio >= WINDOW_BLANK_THRESHOLD
                        && rowCount >= windowRows * 0.6
                        && colCount >= windowCols * 0.6) {
                    return true;
                }
            }
        }
        return false;
    }

    //    /**
    //     * 检测像素是否为空白
    //     */
    //    private static boolean isBlankPixel(int rgb, String format) {
    //        int alpha = (rgb >> 24) & 0xFF;
    //
    //        // 完全透明
    //        if (alpha == 0) {
    //            return true;
    //        }
    //
    //        int red = (rgb >> 16) & 0xFF;
    //        int green = (rgb >> 8) & 0xFF;
    //        int blue = rgb & 0xFF;
    //
    //        // 检测是否为纯白色或接近白色
    //        boolean isWhite = red >= WHITE_THRESHOLD && green >= WHITE_THRESHOLD && blue >=
    // WHITE_THRESHOLD;
    //
    //        // PNG格式：透明或半透明也算空白
    //        if (format != null && format.toLowerCase().contains("png")) {
    //            return isWhite || alpha < 50;
    //        }
    //
    //        return isWhite;
    //    }

    /** 检测像素是否为空白（只检测透明像素） */
    private static boolean isBlankPixel(int rgb, String format) {
        int alpha = (rgb >> 24) & 0xFF;

        // 完全透明
        if (alpha == 0) {
            return true;
        }

        // PNG格式：半透明也算空白（alpha < 50 视为接近透明）
        if (format != null && format.toLowerCase().contains("png")) {
            return alpha < 50;
        }

        // 非PNG格式，只有完全透明才算空白
        return false;
    }
}
