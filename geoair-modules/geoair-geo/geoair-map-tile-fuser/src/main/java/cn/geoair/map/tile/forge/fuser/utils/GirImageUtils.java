package cn.geoair.map.tile.forge.fuser.utils;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.hutool.core.img.Img;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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

    public static void compressImage(File inputFile, File outputFile, float quality) throws IOException {

        if (inputFile == null || !inputFile.exists()) {
            throw new IllegalArgumentException("输入文件不存在或为空");
        }

        byte[] inputBytes = FileUtil.readBytes(inputFile);

        byte[] compressedBytes = compressImage(inputBytes, quality);

        FileUtil.writeBytes(compressedBytes, outputFile);
    }

    /**
     * 压缩 JPEG 图片字节数组（使用 Hutool）
     *
     * @param inputBytes 原始图片字节数组
     * @param quality    压缩质量，范围 0.0f ~ 1.0f
     *                   0.0f 表示最高压缩比（文件最小，质量最差）
     *                   1.0f 表示最高质量（文件最大，质量最好）
     * @return 压缩后的 JPEG 图片字节数组
     * @throws IOException IO异常
     */
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

    public static void main(String[] args) throws IOException {

        File input = new File("H:\\tmp\\gwc_fuser\\tile_cache\\osm_original_grid\\4\\10/16.jpg");
        File output = new File("H:\\tmp\\gwc_fuser\\tile_cache\\osm_original_grid\\4\\10/result2.jpg");
        compressImage(input, output, 0.1f);

        System.out.println("压缩完成！");
    }
}
