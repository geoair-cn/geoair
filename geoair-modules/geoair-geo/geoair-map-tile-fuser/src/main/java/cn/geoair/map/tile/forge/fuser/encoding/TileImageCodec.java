package cn.geoair.map.tile.forge.fuser.encoding;

import cn.geoair.map.tile.forge.fuser.image.TileImageFormats;
import cn.geoair.map.tile.forge.fuser.utils.TileImageUtils;
import cn.geoair.map.tile.forge.fuser.utils.TileResourceLimits;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

/** 显式的栅格编码策略，复用读取字节与解码像素限制。 */
public final class TileImageCodec {
    private TileImageCodec() {}

    public static String detect(byte[] bytes) throws IOException {
        return TileImageFormats.detect(bytes);
    }

    public static String canonical(String format) {
        return TileImageFormats.canonical(format);
    }
    /** 缓存驱动也承担历史非图片字节的透明读写；只有已知栅格签名才进入格式适配。 */
    public static byte[] normalizeCache(byte[] bytes, String requested) throws IOException {
        if (bytes == null || bytes.length < 3) return bytes;
        boolean jpeg = bytes[0] == (byte) 255 && bytes[1] == (byte) 216;
        boolean png =
                bytes.length >= 8
                        && bytes[0] == (byte) 137
                        && bytes[1] == 80
                        && bytes[2] == 78
                        && bytes[3] == 71;
        return jpeg || png ? normalize(bytes, requested) : bytes;
    }

    public static byte[] normalize(byte[] bytes, String requested) throws IOException {
        return normalize(bytes, requested, 75, null);
    }

    /** 仅转换响应；JPEG 用指定的不透明底色合成，现有瓦片和缓存编码策略不参与。 */
    public static byte[] encodeForResponse(byte[] bytes, String requested, int jpegQuality, Color jpegBackground)
            throws IOException {
        if (jpegQuality < 1 || jpegQuality > 100 || jpegBackground == null || jpegBackground.getAlpha() != 255)
            throw new IllegalArgumentException("JPEG 质量应为 1 到 100，底色必须不透明");
        return normalize(bytes, requested, jpegQuality, jpegBackground);
    }

    private static byte[] normalize(byte[] bytes, String requested, int quality, Color background) throws IOException {
        if (bytes == null || bytes.length == 0) return bytes;
        String target = canonical(requested);
        if (detect(bytes).equals(target)) return bytes;
        BufferedImage image = TileImageUtils.readImage(bytes);
        if (image == null) throw new IOException("瓦片解码失败");
        try {
            if ("jpeg".equals(target) && background == null && transparent(image))
                throw new IOException("透明图片不能输出JPEG，请使用PNG");
            return encodeImage(image, target, quality, background);
        } finally {
            image.flush();
        }
    }

    public static EncodedTile encode(byte[] bytes, TileEncodingPolicy policy) throws IOException {
        policy.validate();
        String actual = detect(bytes);
        TileEncodingMode mode = TileEncodingMode.parse(policy.getMode());
        if (mode == TileEncodingMode.LEGACY || mode == TileEncodingMode.SOURCE)
            return new EncodedTile(bytes, actual, false);
        BufferedImage image = TileImageUtils.readImage(bytes);
        if (image == null) throw new IOException("瓦片解码失败");
        try {
            boolean alpha = transparent(image);
            String target = mode == TileEncodingMode.PNG || alpha ? "png" : "jpeg";
            if ("png".equals(target) && "png".equals(actual))
                return new EncodedTile(bytes, actual, alpha);
            return new EncodedTile(encodeImage(image, target, policy.getQuality(), null), target, alpha);
        } finally {
            image.flush();
        }
    }

    public static boolean transparent(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) return false;
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++)
                if ((image.getRGB(x, y) >>> 24) != 255) return true;
        return false;
    }

    private static byte[] encodeImage(BufferedImage image, String format, int quality, Color background)
            throws IOException {
        if (!"jpeg".equals(format)) return TileImageUtils.writeImage(image, format);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        BufferedImage rgb = image;
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            rgb =
                    new BufferedImage(
                            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgb.createGraphics();
            try {
                if (background != null) {
                    graphics.setColor(background);
                    graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                }
                graphics.drawImage(image, 0, 0, null);
            } finally {
                graphics.dispose();
            }
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            JPEGImageWriteParam params = new JPEGImageWriteParam(Locale.ROOT);
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(quality / 100F);
            params.setOptimizeHuffmanTables(true);
            writer.setOutput(stream);
            writer.write(null, new IIOImage(rgb, null, null), params);
            stream.flush();
            byte[] encoded = output.toByteArray();
            if (encoded.length > TileResourceLimits.getMaxTileBytes())
                throw new IOException("编码结果超过瓦片大小限制");
            return encoded;
        } finally {
            writer.dispose();
            if (rgb != image) rgb.flush();
        }
    }
}
