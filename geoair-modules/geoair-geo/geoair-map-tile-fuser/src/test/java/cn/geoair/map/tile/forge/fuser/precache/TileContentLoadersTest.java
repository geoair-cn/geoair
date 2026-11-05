package cn.geoair.map.tile.forge.fuser.precache;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.fuser.execution.*;

import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

public class TileContentLoadersTest {
    private TileOperationContext context() {
        return new TileOperationContext(new TileCancellationToken(), 10000);
    }

    @Test
    public void missingSourceCannotBecomeTransparentFusedSuccess() {
        StubTileGetter getter = new StubTileGetter(null);
        TileContentLoader loader =
                TileContentLoaders.fused(
                        getter,
                        ImageMime.png,
                        (z, x, y) ->
                                getter.getSrcGridSubset().boundsFromIndex(new long[] {x, y, z}),
                        256,
                        256);
        assertEquals(
                TileOperationStatus.MISSING,
                assertThrows(TileOperationException.class, () -> loader.load(2, 1, 1, context()))
                        .getStatus());
        assertNull(TileOperationContext.current());
    }

    @Test
    public void controlledFusionStillProducesDecodedImage() throws Exception {
        BufferedImage input = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = input.createGraphics();
        graphics.setColor(java.awt.Color.RED);
        graphics.fillRect(0, 0, 256, 256);
        graphics.dispose();
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        ImageIO.write(input, "png", encoded);
        StubTileGetter getter = new StubTileGetter(encoded.toByteArray());
        TileContentLoader loader =
                TileContentLoaders.fused(
                        getter,
                        ImageMime.png,
                        (z, x, y) ->
                                getter.getSrcGridSubset().boundsFromIndex(new long[] {x, y, z}),
                        256,
                        256);
        byte[] result = loader.load(2, 1, 1, context());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result));
        assertNotNull(decoded);
        assertEquals(256, decoded.getWidth());
        assertEquals(0xFF0000, decoded.getRGB(128, 128) & 0xFFFFFF);
        assertNull(TileOperationContext.current());
    }
}
