package cn.geoair.map.tile.forge.fuser.utils;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/1 13:15
 * @description： 简单的图像转换方法
 */

public class GirImageUtils {

    private static GiLogger log = GirLoggerFactory.getLogger();

    public static byte[] imageToBytes(BufferedImage image, String formatName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // formatName 可以是 "png", "jpg", "bmp" 等
            ImageIO.write(image, formatName, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }


    public static BufferedImage bytesToImage(byte[] tileData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(tileData)) {
            return ImageIO.read(bais);
        }
    }
}
