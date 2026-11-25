package cn.geoair.map.tile.forge.fuser.support;

import cn.geoair.map.tile.forge.fuser.utils.TileImageUtils;
import java.awt.image.BufferedImage;
import java.io.IOException;

/** 不依赖外网的真实编码测试图片。 */
public final class TileTestImages {
    private TileTestImages() { }
    public static byte[] solid(int rgb, String format) throws IOException {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        try {
            for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++) image.setRGB(x, y, rgb);
            return TileImageUtils.writeImage(image, format);
        } finally { image.flush(); }
    }
}
