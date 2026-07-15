package cn.geoair.map.dynamic.tools.simple;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.hutool.core.img.Img;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/1 13:15
 * @description： 图像处理工具类
 */

public class GirImageUtil extends ImgUtil {

    private static GiLogger log = GirLoggerFactory.getLogger();

    // ========== 基础方法 ==========

    public static byte[] imageToBytes(BufferedImage image, String formatName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, formatName, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("图片转字节数组失败", e);
            return new byte[0];
        }
    }

    public static BufferedImage bytesToImage(byte[] tileData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(tileData)) {
            return ImageIO.read(bais);
        }
    }

    public static void compressImage(File inputFile, File outputFile, float quality) throws IOException {
        if (inputFile == null || !inputFile.exists()) {
            throw new IllegalArgumentException("输入文件不存在或为空");
        }
        byte[] inputBytes = FileUtil.readBytes(inputFile);
        byte[] compressedBytes = compressImage(inputBytes, quality);
        FileUtil.writeBytes(compressedBytes, outputFile);
    }

    public static byte[] compressImage(byte[] inputBytes, float quality) throws IOException {
        BufferedImage image = ImgUtil.toImage(inputBytes);
        if (image == null) {
            throw new IllegalArgumentException("无法解析图片数据，请确认输入为有效图片");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Img.from(image)
                .setQuality(quality)
                .write(outputStream);
        return outputStream.toByteArray();
    }


    // ========== 各等级增强实现 ==========


    // ========== 1. USM 锐化 ==========

    /**
     * USM 锐化（Unsharp Mask）
     * 原理：原图 - 高斯模糊图 = 边缘细节，再把细节加回原图
     *
     * @param src       原始图片
     * @param radius    模糊半径（推荐 1.0 - 3.0）
     * @param amount    锐化强度（推荐 0.5 - 2.0）
     * @param threshold 亮度阈值（推荐 0 - 10，忽略微小变化）
     * @return 锐化后的 BufferedImage
     */
    public static BufferedImage unSharpMask(BufferedImage src, float radius, float amount, int threshold) {
        BufferedImage blurred = gaussianBlur(src, radius);

        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage result = new BufferedImage(width, height, src.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgbSrc = src.getRGB(x, y);
                int rgbBlur = blurred.getRGB(x, y);

                int r = enhanceChannel((rgbSrc >> 16) & 0xFF, (rgbBlur >> 16) & 0xFF, amount, threshold);
                int g = enhanceChannel((rgbSrc >> 8) & 0xFF, (rgbBlur >> 8) & 0xFF, amount, threshold);
                int b = enhanceChannel(rgbSrc & 0xFF, rgbBlur & 0xFF, amount, threshold);

                int a = (rgbSrc >> 24) & 0xFF;
                result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }

    public static int enhanceChannel(int src, int blur, float amount, int threshold) {
        int diff = src - blur;
        if (Math.abs(diff) < threshold) {
            return clamp(src);
        }
        return clamp((int) (src + diff * amount));
    }

    public static BufferedImage gaussianBlur(BufferedImage src, float radius) {
        float[] matrix = {
                1 / 16f, 2 / 16f, 1 / 16f,
                2 / 16f, 4 / 16f, 2 / 16f,
                1 / 16f, 2 / 16f, 1 / 16f
        };
        Kernel kernel = new Kernel(3, 3, matrix);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return op.filter(src, null);
    }


    // ========== 3. 饱和度增强 ==========

    public static BufferedImage enhanceSaturation(BufferedImage src, float factor) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage result = new BufferedImage(width, height, src.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = src.getRGB(x, y);
                float[] hsv = rgbToHsv(rgb);

                hsv[1] = Math.min(1.0f, hsv[1] * factor);

                int newRgb = hsvToRgb(hsv);
                int a = (rgb >> 24) & 0xFF;
                result.setRGB(x, y, (a << 24) | (newRgb & 0xFFFFFF));
            }
        }
        return result;
    }

    public static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float h, s, v = max;

        float delta = max - min;
        if (max == 0) {
            s = 0;
            h = 0;
        } else {
            s = delta / max;
            if (r == max) h = (g - b) / delta;
            else if (g == max) h = 2 + (b - r) / delta;
            else h = 4 + (r - g) / delta;
            h *= 60;
            if (h < 0) h += 360;
        }
        return new float[]{h, s, v};
    }

    public static int hsvToRgb(float[] hsv) {
        float h = hsv[0], s = hsv[1], v = hsv[2];
        int hi = (int) (h / 60) % 6;
        float f = h / 60 - hi;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);

        float r, g, b;
        switch (hi) {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            default:
                r = v;
                g = p;
                b = q;
                break;
        }
        return (clamp((int) (r * 255)) << 16) |
               (clamp((int) (g * 255)) << 8) |
               clamp((int) (b * 255));
    }

    // ========== 4. Gamma 校正 ==========

    public static BufferedImage gammaCorrection(BufferedImage src, float gamma) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage result = new BufferedImage(width, height, src.getType());

        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            lut[i] = clamp((int) (255 * Math.pow(i / 255f, gamma)));
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = src.getRGB(x, y);
                int r = lut[(rgb >> 16) & 0xFF];
                int g = lut[(rgb >> 8) & 0xFF];
                int b = lut[rgb & 0xFF];
                int a = (rgb >> 24) & 0xFF;
                result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }

    // ========== 5. 中值滤波去噪 ==========

    public static BufferedImage medianFilter(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage result = new BufferedImage(width, height, src.getType());

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int[] rArr = new int[9];
                int[] gArr = new int[9];
                int[] bArr = new int[9];

                int idx = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int rgb = src.getRGB(x + dx, y + dy);
                        rArr[idx] = (rgb >> 16) & 0xFF;
                        gArr[idx] = (rgb >> 8) & 0xFF;
                        bArr[idx] = rgb & 0xFF;
                        idx++;
                    }
                }

                Arrays.sort(rArr);
                Arrays.sort(gArr);
                Arrays.sort(bArr);

                int a = (src.getRGB(x, y) >> 24) & 0xFF;
                result.setRGB(x, y, (a << 24) | (rArr[4] << 16) | (gArr[4] << 8) | bArr[4]);
            }
        }
        return result;
    }

    // ========== 6. 局部直方图均衡化 ==========

    public static BufferedImage localHistogramEqualization(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage result = new BufferedImage(width, height, src.getType());

        int windowSize = 8;
        int half = windowSize / 2;

        for (int y = half; y < height - half; y++) {
            for (int x = half; x < width - half; x++) {
                int[] hist = new int[256];
                for (int dy = -half; dy < half; dy++) {
                    for (int dx = -half; dx < half; dx++) {
                        int rgb = src.getRGB(x + dx, y + dy);
                        int gray = (int) (0.299 * ((rgb >> 16) & 0xFF) +
                                          0.587 * ((rgb >> 8) & 0xFF) +
                                          0.114 * (rgb & 0xFF));
                        hist[gray]++;
                    }
                }

                int[] cdf = new int[256];
                cdf[0] = hist[0];
                for (int i = 1; i < 256; i++) {
                    cdf[i] = cdf[i - 1] + hist[i];
                }

                int total = windowSize * windowSize;
                int rgb = src.getRGB(x, y);
                int r = equalizeChannel((rgb >> 16) & 0xFF, cdf, total);
                int g = equalizeChannel((rgb >> 8) & 0xFF, cdf, total);
                int b = equalizeChannel(rgb & 0xFF, cdf, total);

                int a = (rgb >> 24) & 0xFF;
                result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }

    private static int equalizeChannel(int value, int[] cdf, int total) {
        return clamp((int) (cdf[value] * 255f / total));
    }

    // ========== 7. 色阶调整 ==========

    public static BufferedImage levelsAdjust(BufferedImage src, int blackPoint, int whitePoint) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage result = new BufferedImage(width, height, src.getType());

        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            if (i <= blackPoint) {
                lut[i] = 0;
            } else if (i >= whitePoint) {
                lut[i] = 255;
            } else {
                lut[i] = clamp((i - blackPoint) * 255 / (whitePoint - blackPoint));
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = src.getRGB(x, y);
                int r = lut[(rgb >> 16) & 0xFF];
                int g = lut[(rgb >> 8) & 0xFF];
                int b = lut[rgb & 0xFF];
                int a = (rgb >> 24) & 0xFF;
                result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }

    // ========== 8. 工具方法 ==========

    private static int clamp(int value) {
        return Math.min(255, Math.max(0, value));
    }

    /**
     * 检测图片格式
     */
    public static String detectImageFormat(byte[] bytes) {
        if (bytes.length < 4) {
            return "png";
        }
        // PNG: 89 50 4E 47
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "png";
        }
        // JPEG: FF D8
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "jpg";
        }
        // WebP: 52 49 46 46
        if (bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46) {
            return "webp";
        }
        return "png";
    }

    /**
     * 保存 BufferedImage 到文件
     */
    private static void saveImage(BufferedImage image, String basePath, String name, String ext) {
        try {
            File output = new File(basePath, name + ext);
            // 判断格式：如果扩展名是 .jpg 或 .jpeg，使用 JPEG 格式
            String format = "png";
            if (ext.toLowerCase().endsWith("jpg") || ext.toLowerCase().endsWith("jpeg")) {
                format = "jpg";
            }
            ImageIO.write(image, format, output);
        } catch (IOException e) {
            System.err.println("保存图片失败: " + name + " - " + e.getMessage());
        }
    }

    /**
     * 统计生成的文件数量
     */
    private static int countFiles(String basePath, String fileName) {
        File dir = new File(basePath);
        File[] files = dir.listFiles((d, name) -> name.startsWith(fileName));
        return files != null ? files.length : 0;
    }

    public static void main(String[] args) throws IOException {

        File testFile = new File("H:\\tmp\\gwc_fuser\\tile_cache\\osm_original_grid\\4\\10/2101.png");
        if (!testFile.exists()) {
            System.out.println("测试文件不存在，请修改路径: " + testFile.getAbsolutePath());
            return;
        }

        byte[] originalBytes = FileUtil.readBytes(testFile);
        String basePath = testFile.getParent();
        String fileName = testFile.getName().substring(0, testFile.getName().lastIndexOf("."));
        String ext = testFile.getName().substring(testFile.getName().lastIndexOf("."));

        // 加载原图
        BufferedImage originalImage = ImgUtil.toImage(originalBytes);

        System.out.println("========== USM 参数专项测试 ==========");
        System.out.println("原图尺寸: " + originalImage.getWidth() + "x" + originalImage.getHeight());
        System.out.println("原图类型: " + originalImage.getType());
        System.out.println();


        System.out.println("【测试1: radius 参数测试】");
        System.out.println("固定条件: amount=1.2, threshold=5");
        float[] radiusValues = {0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f};
        for (float radius : radiusValues) {
            BufferedImage result = unSharpMask(originalImage, radius, 1.2f, 5);
            saveImage(result, basePath, fileName + "_usm_r" + formatFloat(radius) + "_a12_t5", ext);
            System.out.println("  radius=" + radius + " 完成");
        }
        System.out.println();


        System.out.println("【测试2: amount 参数测试】");
        System.out.println("固定条件: radius=1.5, threshold=5");
        float[] amountValues = {0.3f, 0.5f, 0.8f, 1.0f, 1.2f, 1.5f, 1.8f, 2.0f, 2.5f, 3.0f};
        for (float amount : amountValues) {
            BufferedImage result = unSharpMask(originalImage, 1.5f, amount, 5);
            saveImage(result, basePath, fileName + "_usm_r15_a" + formatFloat(amount) + "_t5", ext);
            System.out.println("  amount=" + amount + " 完成");
        }
        System.out.println();

        // ========================================
        // 测试3: 固定 radius 和 amount，测试不同 threshold
        // ========================================
        System.out.println("【测试3: threshold 参数测试】");
        System.out.println("固定条件: radius=1.5, amount=1.2");
        int[] thresholdValues = {0, 1, 2, 3, 5, 8, 10, 15, 20, 30};
        for (int threshold : thresholdValues) {
            BufferedImage result = unSharpMask(originalImage, 1.5f, 1.2f, threshold);
            saveImage(result, basePath, fileName + "_usm_r15_a12_t" + threshold, ext);
            System.out.println("  threshold=" + threshold + " 完成");
        }
        System.out.println();

        // ========================================
        // 测试4: 组合测试 - 不同场景推荐参数
        // ========================================
        System.out.println("【测试4: 场景组合测试】");

        // 场景1: 轻度锐化（适合已经很清晰的图）
        System.out.println("  场景1: 轻度锐化 (radius=1.0, amount=0.6, threshold=10)");
        BufferedImage scene1 = unSharpMask(originalImage, 1.0f, 0.6f, 10);
        saveImage(scene1, basePath, fileName + "_usm_light", ext);

        // 场景2: 标准锐化（推荐，适合大多数情况）
        System.out.println("  场景2: 标准锐化 (radius=1.5, amount=1.2, threshold=5)");
        BufferedImage scene2 = unSharpMask(originalImage, 1.5f, 1.2f, 5);
        saveImage(scene2, basePath, fileName + "_usm_standard", ext);

        // 场景3: 重度锐化（适合模糊的图）
        System.out.println("  场景3: 重度锐化 (radius=2.0, amount=2.0, threshold=3)");
        BufferedImage scene3 = unSharpMask(originalImage, 2.0f, 2.0f, 3);
        saveImage(scene3, basePath, fileName + "_usm_strong", ext);

        // 场景4: 极致锐化（适合严重模糊，可能有噪点）
        System.out.println("  场景4: 极致锐化 (radius=3.0, amount=3.0, threshold=0)");
        BufferedImage scene4 = unSharpMask(originalImage, 3.0f, 3.0f, 0);
        saveImage(scene4, basePath, fileName + "_usm_extreme", ext);

        // 场景5: 高阈值去噪锐化（适合有噪点的图）
        System.out.println("  场景5: 高阈值去噪锐化 (radius=1.2, amount=1.0, threshold=15)");
        BufferedImage scene5 = unSharpMask(originalImage, 1.2f, 1.0f, 15);
        saveImage(scene5, basePath, fileName + "_usm_denoise", ext);

        // 场景6: 大半径边缘锐化（适合增强轮廓）
        System.out.println("  场景6: 大半径边缘锐化 (radius=3.0, amount=1.0, threshold=5)");
        BufferedImage scene6 = unSharpMask(originalImage, 3.0f, 1.0f, 5);
        saveImage(scene6, basePath, fileName + "_usm_edge", ext);
        System.out.println();

        // ========================================
        // 测试5: 极端参数测试（边界值）
        // ========================================
        System.out.println("【测试5: 极端参数测试】");

        System.out.println("  极端1: 最小半径 (radius=0.1, amount=1.0, threshold=5)");
        BufferedImage extreme1 = unSharpMask(originalImage, 0.1f, 1.0f, 5);
        saveImage(extreme1, basePath, fileName + "_usm_extreme_min_r", ext);

        System.out.println("  极端2: 最大半径 (radius=5.0, amount=1.0, threshold=5)");
        BufferedImage extreme2 = unSharpMask(originalImage, 5.0f, 1.0f, 5);
        saveImage(extreme2, basePath, fileName + "_usm_extreme_max_r", ext);

        System.out.println("  极端3: 最大锐化 (radius=2.0, amount=5.0, threshold=0)");
        BufferedImage extreme3 = unSharpMask(originalImage, 2.0f, 5.0f, 0);
        saveImage(extreme3, basePath, fileName + "_usm_extreme_max_a", ext);

        System.out.println("  极端4: 最大阈值 (radius=2.0, amount=2.0, threshold=50)");
        BufferedImage extreme4 = unSharpMask(originalImage, 2.0f, 2.0f, 50);
        saveImage(extreme4, basePath, fileName + "_usm_extreme_max_t", ext);
        System.out.println();

        // ========================================
        // 测试6: 原图对比保存
        // ========================================
        System.out.println("【测试6: 保存原图参考】");
        File originalOutput = new File(basePath, fileName + "_original" + ext);
        FileUtil.writeBytes(originalBytes, originalOutput);
        System.out.println("  原图已保存: " + originalOutput.getName());
        System.out.println();

        // ========================================
        // 统计信息
        // ========================================
        System.out.println("========== USM 测试完成 ==========");
        System.out.println("输出目录: " + basePath);
        System.out.println("共生成 " + countFiles(basePath, fileName) + " 个测试图片");
        System.out.println();
        System.out.println("【USM 参数选择指南】");
        System.out.println("  ┌─────────────┬──────────┬──────────┬───────────┬─────────────────────┐");
        System.out.println("  │ 场景        │ radius   │ amount   │ threshold │ 说明                │");
        System.out.println("  ├─────────────┼──────────┼──────────┼───────────┼─────────────────────┤");
        System.out.println("  │ 轻度锐化    │ 1.0      │ 0.5-0.8  │ 8-10      │ 已经很清晰的图      │");
        System.out.println("  │ 标准锐化    │ 1.5      │ 1.0-1.5  │ 5-8       │ 大多数场景推荐      │");
        System.out.println("  │ 重度锐化    │ 2.0      │ 1.8-2.5  │ 2-5       │ 模糊的图            │");
        System.out.println("  │ 去噪锐化    │ 1.2      │ 0.8-1.0  │ 10-15     │ 有噪点的图          │");
        System.out.println("  │ 边缘增强    │ 2.5-3.0  │ 0.8-1.2  │ 5-8       │ 增强轮廓            │");
        System.out.println("  │ 极致锐化    │ 2.0-3.0  │ 2.5-3.0  │ 0-2       │ 严重模糊，可能有噪点│");
        System.out.println("  └─────────────┴──────────┴──────────┴───────────┴─────────────────────┘");
        System.out.println();
        System.out.println("【参数说明】");
        System.out.println("  • radius（模糊半径）: 控制边缘检测范围，越大边缘越粗，推荐 1.0-3.0");
        System.out.println("  • amount（锐化强度）: 控制锐化程度，越大越锐利，推荐 0.5-2.0");
        System.out.println("  • threshold（亮度阈值）: 控制噪点抑制，越大越平滑，推荐 0-10");
        System.out.println();
        System.out.println("【推荐测试顺序】");
        System.out.println("  1. 先看场景组合测试 (usm_light, usm_standard, usm_strong)");
        System.out.println("  2. 确定大致场景后，看对应的参数专项测试");
        System.out.println("  3. 最后看极端测试，了解参数极限效果");
    }

    /**
     * 格式化浮点数，去掉多余的小数点
     */
    private static String formatFloat(float value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value).replace(".", "");
    }


}
