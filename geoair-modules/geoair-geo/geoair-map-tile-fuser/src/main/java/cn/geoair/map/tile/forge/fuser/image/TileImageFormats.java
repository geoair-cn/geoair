package cn.geoair.map.tile.forge.fuser.image;

import cn.geoair.map.tile.forge.fuser.utils.TileImageUtils;
import cn.geoair.map.tile.forge.fuser.utils.TileResourceLimits;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** 按真实图片内容识别格式；受限解码和编码复用库内统一资源限制。 */
public final class TileImageFormats {
    private TileImageFormats() {}

    public static String detect(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) throw new IOException("图片内容为空");
        if (bytes.length > TileResourceLimits.getMaxTileBytes())
            throw new IOException("图片数据超过大小限制");
        try (ImageInputStream input =
                ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("无法识别图片格式");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                TileResourceLimits.validateImageDimensions(reader.getWidth(0), reader.getHeight(0));
                return canonical(reader.getFormatName());
            } finally {
                reader.dispose();
            }
        }
    }

    /** JPEG别名归一化，服务响应格式与磁盘扩展名可独立维护。 */
    public static String canonical(String format) {
        String value =
                format == null ? "png" : format.toLowerCase(Locale.ROOT).replace("image/", "");
        return "jpg".equals(value) ? "jpeg" : value;
    }

    public static byte[] toPng(byte[] bytes) throws IOException {
        // 即使文件头是 PNG 也经过尺寸校验，避免仅检查魔数绕过解码限制。
        BufferedImage image = TileImageUtils.readImage(bytes);
        if (image == null) throw new IOException("瓦片不是可读取的图片");
        try {
            if ("png".equals(detect(bytes))) return bytes.clone();
            return TileImageUtils.writeImage(image, "png");
        } finally {
            image.flush();
        }
    }
}
