package cn.geoair.map.tile.forge.fuser.image.watermark;

import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingContext;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingResult;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingState;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessor;
import cn.geoair.map.tile.forge.fuser.utils.TileImageUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.imageio.ImageIO;

/** 固定版本 Google 文字模板定位与局部调和修补，全部在本机运行。 */
public final class GoogleWatermarkProcessor implements TileImageProcessor {
    @Override
    public String getRuleVersion() {
        return RULE_VERSION;
    }

    /** 显式选用的规则；不注册为全局默认处理器。 */
    @Override
    public TileImageProcessingResult process(byte[] bytes, TileImageProcessingContext context)
            throws IOException, InterruptedException {
        if (context == null) throw new IllegalArgumentException("图像处理上下文不能为空");
        WatermarkRepairResult result = repair(bytes, context);
        TileImageProcessingState state;
        switch (result.getState()) {
            case REPAIRED:
                state = TileImageProcessingState.PROCESSED;
                break;
            case UNCHANGED:
                state = TileImageProcessingState.UNCHANGED;
                break;
            default:
                state = TileImageProcessingState.REVIEW;
        }
        return new TileImageProcessingResult(
                state,
                RULE_VERSION,
                result.getScore(),
                result.getRegions(),
                result.getPng(),
                result.getReason());
    }

    public static final String RULE_VERSION = "google-2026-v2";
    private static final String TEMPLATE_VERSION = "google-2026-v1";
    private static final int TILE_SIZE = 256;
    private static final double REPAIR_SCORE = 0.62;
    private static final double REVIEW_SCORE = 0.35;
    private static final int MAX_REGIONS = 3;
    private static final int INPAINT_STEPS = 240;
    private final int width;
    private final int height;
    private final double[] template;
    private final double[] gray;
    private final boolean[] mask;
    private final double power;

    public GoogleWatermarkProcessor() {
        Properties values = new Properties();
        try (InputStream input =
                getClass()
                        .getResourceAsStream(
                                "/tile-image-rules/google/" + TEMPLATE_VERSION + ".properties")) {
            if (input == null) throw new IOException("水印模板不存在");
            values.load(input);
        } catch (IOException error) {
            throw new IllegalStateException("无法加载水印检测模板", error);
        }
        width = Integer.parseInt(values.getProperty("width"));
        height = Integer.parseInt(values.getProperty("height"));
        String[] samples = values.getProperty("values").split(",");
        String[] grayscale = values.getProperty("gray").split(",");
        String bits = values.getProperty("mask");
        if (samples.length != width * height
                || bits.length() != samples.length
                || grayscale.length != samples.length) throw new IllegalStateException("水印模板损坏");
        template = new double[samples.length];
        gray = new double[samples.length];
        mask = new boolean[samples.length];
        double norm = 0;
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                gray[i] = Double.parseDouble(grayscale[i]);
                // 模板裁切边缘的局部均值不参与匹配，避免背景边缘伪造命中。
                template[i] =
                        y < 2 || x < 2 || y >= height - 2 || x >= width - 2
                                ? 0
                                : Double.parseDouble(samples[i]);
                mask[i] = bits.charAt(i) == '1';
                norm += template[i] * template[i];
            }
        power = norm;
    }

    /** 转换缓存按融合器实际横纵比例变换模板，不把拉伸后的水印当作原尺寸文字。 */
    private GoogleWatermarkProcessor(GoogleWatermarkProcessor source, int width, int height) {
        this.width = width;
        this.height = height;
        template = new double[width * height];
        gray = new double[template.length];
        mask = new boolean[template.length];
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                double sx = (x + 0.5) * source.width / width - 0.5,
                        sy = (y + 0.5) * source.height / height - 0.5;
                int x0 = Math.max(0, (int) Math.floor(sx)), y0 = Math.max(0, (int) Math.floor(sy));
                int x1 = Math.min(source.width - 1, x0 + 1),
                        y1 = Math.min(source.height - 1, y0 + 1);
                double dx = Math.max(0, sx - x0), dy = Math.max(0, sy - y0);
                int i = y * width + x;
                gray[i] =
                        source.gray[y0 * source.width + x0] * (1 - dx) * (1 - dy)
                                + source.gray[y0 * source.width + x1] * dx * (1 - dy)
                                + source.gray[y1 * source.width + x0] * (1 - dx) * dy
                                + source.gray[y1 * source.width + x1] * dx * dy;
                // 双线性缩放会把笔画影响到相邻像素，掩膜覆盖其采样邻域。
                mask[i] =
                        source.mask[y0 * source.width + x0]
                                || source.mask[y0 * source.width + x1]
                                || source.mask[y1 * source.width + x0]
                                || source.mask[y1 * source.width + x1];
            }
        // 先缩放原灰度图再提取高频。直接缩放旧高频值会在半尺寸文字上丢失笔画特征。
        double norm = 0;
        for (int y = 2; y < height - 2; y++)
            for (int x = 2; x < width - 2; x++) {
                double mean = 0;
                for (int yy = y - 2; yy <= y + 2; yy++)
                    for (int xx = x - 2; xx <= x + 2; xx++) mean += gray[yy * width + xx];
                int i = y * width + x;
                template[i] = gray[i] - mean / 25;
                norm += template[i] * template[i];
            }
        power = norm;
    }

    public WatermarkRepairResult repair(byte[] bytes, TileImageProcessingContext scale)
            throws IOException, InterruptedException {
        GoogleWatermarkProcessor processor = scaled(scale);
        if (processor == null)
            return new WatermarkRepairResult(
                    WatermarkRepairState.REVIEW, 0, 0, null, "转换后文字尺寸超出当前检测范围，保留原图");
        return processor.repair(bytes);
    }

    public WatermarkMatch detect(byte[] bytes, TileImageProcessingContext scale)
            throws IOException, InterruptedException {
        GoogleWatermarkProcessor processor = scaled(scale);
        if (processor == null) throw new IllegalArgumentException("转换后文字尺寸超出当前检测范围");
        return processor.detect(bytes);
    }

    private GoogleWatermarkProcessor scaled(TileImageProcessingContext scale) {
        if (scale == null
                || !Double.isFinite(scale.getScaleX())
                || !Double.isFinite(scale.getScaleY())) return null;
        double scaledWidth = width * scale.getScaleX(), scaledHeight = height * scale.getScaleY();
        if (scaledWidth < 16
                || scaledHeight < 8
                || scaledWidth > TILE_SIZE
                || scaledHeight > TILE_SIZE) return null;
        int w = (int) Math.round(scaledWidth), h = (int) Math.round(scaledHeight);
        return w == width && h == height ? this : new GoogleWatermarkProcessor(this, w, h);
    }

    public WatermarkRepairResult repair(byte[] bytes) throws IOException, InterruptedException {
        BufferedImage image = TileImageUtils.readImage(bytes);
        if (image == null) throw new IOException("瓦片不是可读取的图片");
        try {
            if (image.getWidth() != TILE_SIZE || image.getHeight() != TILE_SIZE)
                return new WatermarkRepairResult(
                        WatermarkRepairState.REVIEW, 0, 0, null, "首版仅支持256×256瓦片");
            int[] pixels = image.getRGB(0, 0, TILE_SIZE, TILE_SIZE, null, 0, TILE_SIZE);
            double firstScore = 0;
            int regions = 0;
            for (; regions < MAX_REGIONS; regions++) {
                checkCancelled();
                WatermarkMatch match = find(pixels);
                firstScore = Math.max(firstScore, match.getScore());
                if (match.getScore() < REVIEW_SCORE) break;
                if (match.getScore() < REPAIR_SCORE)
                    return new WatermarkRepairResult(
                            WatermarkRepairState.REVIEW,
                            firstScore,
                            regions,
                            null,
                            "文字匹配置信度不足，保留原图");
                inpaint(pixels, match.getX(), match.getY());
            }
            if (regions == 0)
                return new WatermarkRepairResult(
                        WatermarkRepairState.UNCHANGED, firstScore, 0, null, "未检测到匹配水印");
            if (find(pixels).getScore() >= REVIEW_SCORE)
                return new WatermarkRepairResult(
                        WatermarkRepairState.REVIEW, firstScore, regions, null, "修补后仍有疑似水印，保留原图");
            // 索引色 PNG 的旧调色板可能无法表达修补后的颜色，统一写入真彩色副本。
            BufferedImage repaired =
                    new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            repaired.setRGB(0, 0, TILE_SIZE, TILE_SIZE, pixels, 0, TILE_SIZE);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (!ImageIO.write(repaired, "png", output)) throw new IOException("PNG编码不可用");
                return new WatermarkRepairResult(
                        WatermarkRepairState.REPAIRED,
                        firstScore,
                        regions,
                        output.toByteArray(),
                        "已局部修补");
            } finally {
                repaired.flush();
            }
        } finally {
            image.flush();
        }
    }

    public WatermarkMatch detect(byte[] bytes) throws IOException, InterruptedException {
        BufferedImage image = TileImageUtils.readImage(bytes);
        if (image == null || image.getWidth() != TILE_SIZE || image.getHeight() != TILE_SIZE) {
            if (image != null) image.flush();
            throw new IOException("检测需要256×256图片");
        }
        try {
            return find(image.getRGB(0, 0, TILE_SIZE, TILE_SIZE, null, 0, TILE_SIZE));
        } finally {
            image.flush();
        }
    }

    private WatermarkMatch find(int[] pixels) throws InterruptedException {
        double[] data = highPass(pixels);
        double[][] square = new double[TILE_SIZE + 1][TILE_SIZE + 1];
        for (int y = 1; y <= TILE_SIZE; y++)
            for (int x = 1; x <= TILE_SIZE; x++) {
                double v = data[(y - 1) * TILE_SIZE + x - 1];
                square[y][x] = v * v + square[y - 1][x] + square[y][x - 1] - square[y - 1][x - 1];
            }
        double best = 0;
        int bx = 0, by = 0;
        for (int y = 0; y + height <= TILE_SIZE; y++) {
            if (y % 16 == 0) checkCancelled();
            for (int x = 0; x + width <= TILE_SIZE; x++) {
                double norm =
                        square[y + height][x + width]
                                - square[y][x + width]
                                - square[y + height][x]
                                + square[y][x];
                if (norm < 1) continue;
                double dot = 0;
                for (int yy = 2; yy < height - 2; yy++) {
                    int imageRow = (y + yy) * TILE_SIZE + x, templateRow = yy * width;
                    for (int xx = 2; xx < width - 2; xx++)
                        dot += data[imageRow + xx] * template[templateRow + xx];
                }
                double score = dot / Math.sqrt(power * norm);
                if (score > best) {
                    best = score;
                    bx = x;
                    by = y;
                }
            }
        }
        return new WatermarkMatch(bx, by, Math.min(1, best));
    }

    private double[] highPass(int[] pixels) {
        double[] gray = new double[pixels.length], result = new double[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            gray[i] = (((c >> 16) & 255) + ((c >> 8) & 255) + (c & 255)) / 3.0;
        }
        for (int y = 0; y < TILE_SIZE; y++)
            for (int x = 0; x < TILE_SIZE; x++) {
                double total = 0;
                int count = 0;
                for (int yy = Math.max(0, y - 2); yy <= Math.min(TILE_SIZE - 1, y + 2); yy++)
                    for (int xx = Math.max(0, x - 2); xx <= Math.min(TILE_SIZE - 1, x + 2); xx++) {
                        total += gray[yy * TILE_SIZE + xx];
                        count++;
                    }
                result[y * TILE_SIZE + x] = gray[y * TILE_SIZE + x] - total / count;
            }
        return result;
    }

    private void inpaint(int[] pixels, int left, int top) throws InterruptedException {
        // 掩膜只覆盖文字及紧邻光晕；掩膜外像素逐位保留，不能整块涂抹背景。
        double[][] channels = new double[3][width * height];
        for (int c = 0; c < 3; c++)
            for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                    channels[c][y * width + x] =
                            (pixels[(top + y) * TILE_SIZE + left + x] >> (16 - 8 * c)) & 255;
        for (int step = 0; step < INPAINT_STEPS; step++) {
            if (step % 16 == 0) checkCancelled();
            for (int y = 1; y < height - 1; y++)
                for (int x = 1; x < width - 1; x++) {
                    int i = y * width + x;
                    if (!mask[i]) continue;
                    for (int c = 0; c < 3; c++)
                        channels[c][i] =
                                (channels[c][i - 1]
                                                + channels[c][i + 1]
                                                + channels[c][i - width]
                                                + channels[c][i + width])
                                        / 4;
                }
        }
        for (int y = 1; y < height - 1; y++)
            for (int x = 1; x < width - 1; x++) {
                int i = y * width + x, p = (top + y) * TILE_SIZE + left + x;
                if (mask[i])
                    pixels[p] =
                            (pixels[p] & 0xff000000)
                                    | ((int) Math.round(channels[0][i]) << 16)
                                    | ((int) Math.round(channels[1][i]) << 8)
                                    | (int) Math.round(channels[2][i]);
            }
    }

    private static void checkCancelled() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("去水印已中止");
    }
}
