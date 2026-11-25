package cn.geoair.map.tile.forge.fuser.image;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.fuser.provider.PngLayerTileGetter;
import cn.geoair.map.tile.forge.fuser.support.FusionTestSource;
import cn.geoair.map.tile.forge.fuser.support.TileTestImages;
import cn.geoair.map.tile.forge.fuser.utils.GridInitUtils;
import cn.geoair.map.tile.forge.fuser.utils.TileImageUtils;
import cn.geoair.map.tile.forge.fuser.utils.TileResourceLimits;

import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class TileImageFormatsTest {
    @Test
    public void getterUsesActualJpegContentEvenWhenSourceDeclaresPng() throws Exception {
        byte[] jpeg = TileTestImages.solid(0xff0000, "jpeg");
        assertEquals("jpeg", TileImageFormats.detect(jpeg));
        FusionTestSource source =
                new FusionTestSource(
                        GridInitUtils.getWorldGrid3857(), (x, y) -> new ByteArrayResource(jpeg));
        byte[] output = new PngLayerTileGetter(source).getTileResource(2, 1, 1).getByteData();
        assertEquals("png", TileImageFormats.detect(output));
        BufferedImage image = TileImageUtils.readImage(output);
        try {
            assertTrue((image.getRGB(100, 100) >> 16 & 255) > 250);
        } finally {
            image.flush();
        }
        assertEquals("jpeg", TileImageFormats.detect(jpeg));
    }

    @Test
    public void pngPassThroughPreservesBytesAndRejectsInvalidImage() throws Exception {
        byte[] png = TileTestImages.solid(0x123456, "png");
        byte[] output = TileImageFormats.toPng(png);
        assertArrayEquals(png, output);
        assertNotSame(png, output);
        try {
            TileImageFormats.toPng(new byte[] {1, 2, 3});
            fail();
        } catch (IOException expected) {
        }
    }

    @Test
    public void pngHeaderCannotBypassResourceLimits() throws Exception {
        byte[] png = TileTestImages.solid(0x123456, "png");
        int oldWidth = TileResourceLimits.getMaxImageWidth();
        try {
            TileResourceLimits.setMaxImageWidth(128);
            try {
                TileImageFormats.toPng(png);
                fail("超尺寸图片不能因格式相同而跳过检查");
            } catch (IllegalArgumentException expected) {
            }
        } finally {
            TileResourceLimits.setMaxImageWidth(oldWidth);
        }
    }
}
