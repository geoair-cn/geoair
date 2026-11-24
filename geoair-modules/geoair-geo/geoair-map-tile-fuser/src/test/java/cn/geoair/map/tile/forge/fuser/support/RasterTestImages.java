package cn.geoair.map.tile.forge.fuser.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import javax.imageio.ImageIO;

/** 可复现的实际栅格字节，避免断言模拟编码成功。 */
public final class RasterTestImages {
    private RasterTestImages() {}

    public static byte[] image(String format, boolean alpha) throws Exception {
        BufferedImage image =
                new BufferedImage(
                        256, 256, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42);
        try {
            for (int y = 0; y < 256; y++)
                for (int x = 0; x < 256; x++)
                    image.setRGB(
                            x, y, (alpha ? 0x80000000 : 0xff000000) | random.nextInt(0xffffff));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (!ImageIO.write(image, format, bytes)) throw new IllegalStateException("图片编码器不可用");
            return bytes.toByteArray();
        } finally {
            image.flush();
        }
    }
}
