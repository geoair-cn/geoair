package cn.geoair.map.tile.forge.fuser.encoding;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.fuser.utils.TileImageUtils;

import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import javax.imageio.ImageIO;

/** 真实编码图片验证内容识别、透明保护和任务参数隔离。 */
public class TileImageCodecTest {
    static byte[] image(boolean transparent) throws Exception {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(8);
        try {
            for (int y = 0; y < 256; y++)
                for (int x = 0; x < 256; x++)
                    image.setRGB(
                            x,
                            y,
                            (transparent && x < 100 ? 0x33000000 : 0xff000000)
                                    | random.nextInt(0x1000000));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bytes);
            return bytes.toByteArray();
        } finally {
            image.flush();
        }
    }

    @Test
    public void sourceModePreservesExactBytesAndJpegQualityChangesSize() throws Exception {
        byte[] png = image(false);
        assertArrayEquals(
                png, TileImageCodec.encode(png, new TileEncodingPolicy("SOURCE", 75)).getBytes());
        byte[] high = TileImageCodec.encode(png, new TileEncodingPolicy("JPEG", 90)).getBytes();
        byte[] low = TileImageCodec.encode(png, new TileEncodingPolicy("JPEG", 60)).getBytes();
        assertEquals("jpeg", TileImageCodec.detect(low));
        assertTrue(low.length < high.length);
        assertTrue(high.length < png.length);
        byte[] normalized = TileImageCodec.normalize(low, "png");
        assertEquals("png", TileImageCodec.detect(normalized));
        BufferedImage decoded = TileImageUtils.readImage(normalized);
        try {
            assertEquals(256, decoded.getWidth());
            assertEquals(256, decoded.getHeight());
        } finally {
            decoded.flush();
        }
    }

    @Test
    public void jpegDoesNotFlattenTransparentPixels() throws Exception {
        byte[] png = image(true);
        EncodedTile encoded = TileImageCodec.encode(png, new TileEncodingPolicy("JPEG", 60));
        assertEquals("png", encoded.getFormat());
        assertTrue(encoded.isTransparent());
        assertArrayEquals(png, encoded.getBytes());
    }
}
