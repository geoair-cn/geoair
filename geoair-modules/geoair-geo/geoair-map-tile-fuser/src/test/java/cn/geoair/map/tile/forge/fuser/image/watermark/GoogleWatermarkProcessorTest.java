package cn.geoair.map.tile.forge.fuser.image.watermark;

import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingContext;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingResult;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingState;
import cn.geoair.map.tile.forge.fuser.support.TileTestImages;
import cn.geoair.map.tile.forge.fuser.utils.TileImageUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/** 从地图服务平台迁入的真实留出样本，不用制作模板的瓦片验收算法。 */
public class GoogleWatermarkProcessorTest {
    private final GoogleWatermarkProcessor processor = new GoogleWatermarkProcessor();

    @Test public void heldOutSamplesPreserveAllPixelsOutsideMatchedArea() throws Exception {
        for (String name : new String[]{"google-sea-12-3441-1680.png", "google-sea-12-3440-1681.png"}) {
            byte[] original = sample(name);
            WatermarkMatch match = processor.detect(original);
            TileImageProcessingResult result = processor.process(original, TileImageProcessingContext.ORIGINAL);
            assertEquals(TileImageProcessingState.PROCESSED, result.getState());
            assertEquals(GoogleWatermarkProcessor.RULE_VERSION, result.getRuleVersion());
            assertTrue(processor.detect(result.getBytes()).getScore() < .35);
            BufferedImage before = ImageIO.read(new ByteArrayInputStream(original));
            BufferedImage after = ImageIO.read(new ByteArrayInputStream(result.getBytes()));
            int changed = 0;
            try {
                for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++) {
                    if (x < match.getX() || x >= match.getX() + 100 || y < match.getY() || y >= match.getY() + 24)
                        assertEquals("匹配区域外像素不得改变", before.getRGB(x, y), after.getRGB(x, y));
                    if (before.getRGB(x, y) != after.getRGB(x, y)) changed++;
                }
                assertTrue(changed > 0 && changed < 2400);
            } finally { before.flush(); after.flush(); }
        }
    }

    @Test public void landTextureRemainsUnchanged() throws Exception {
        TileImageProcessingResult result = processor.process(sample("google-land-12-3420-1680.png"), TileImageProcessingContext.ORIGINAL);
        assertEquals(TileImageProcessingState.UNCHANGED, result.getState()); assertNull(result.getBytes());
    }

    @Test public void uncertainMatchReturnsReviewWithoutPublishingImage() throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(sample("google-sea-12-3441-1680.png")));
        Random random = new Random(7);
        byte[] bytes;
        try {
            for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++) {
                int pixel = image.getRGB(x, y), noise = random.nextInt(7) - 3, value = pixel & 0xff000000;
                for (int channel = 0; channel < 3; channel++) {
                    int shift = 8 * channel;
                    value |= Math.max(0, Math.min(255, ((pixel >> shift) & 255) + noise)) << shift;
                }
                image.setRGB(x, y, value);
            }
            bytes = TileImageUtils.writeImage(image, "png");
        } finally { image.flush(); }
        TileImageProcessingResult result = processor.process(bytes, TileImageProcessingContext.ORIGINAL);
        assertTrue(result.getScore() >= .35 && result.getScore() < .62);
        assertEquals(TileImageProcessingState.REVIEW, result.getState()); assertNull(result.getBytes());
    }

    @Test public void unsupportedScaleIsReviewAndCancellationPropagates() throws Exception {
        assertEquals(TileImageProcessingState.REVIEW,
                processor.process(TileTestImages.solid(0, "png"), new TileImageProcessingContext(.01, .01)).getState());
        Thread.currentThread().interrupt();
        try {
            processor.process(sample("google-sea-12-3441-1680.png"), TileImageProcessingContext.ORIGINAL);
            fail("必须响应停止");
        } catch (InterruptedException expected) { }
        finally { Thread.interrupted(); }
    }

    private byte[] sample(String name) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/watermark/" + name);
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            assertNotNull(input);
            byte[] buffer = new byte[4096]; int n;
            while ((n = input.read(buffer)) != -1) bytes.write(buffer, 0, n);
            return bytes.toByteArray();
        }
    }
}
