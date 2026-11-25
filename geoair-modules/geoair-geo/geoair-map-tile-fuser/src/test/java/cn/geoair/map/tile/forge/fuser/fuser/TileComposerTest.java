package cn.geoair.map.tile.forge.fuser.fuser;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.fuser.support.TileTestImages;
import cn.geoair.map.tile.forge.fuser.utils.GridInitUtils;
import cn.geoair.map.tile.forge.fuser.utils.TileImageUtils;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

public class TileComposerTest {
    @Test
    public void clipsWithHolesAndOutsideMaskAndSkipsIneligibleSources() throws Exception {
        Geometry area =
                new WKTReader()
                        .read(
                                "POLYGON ((0 -60, 180 -60, 180 60, 0 60, 0 -60), "
                                        + "(20 -10, 20 10, 40 10, 40 -10, 20 -10))");
        byte[] red = TileTestImages.solid(0xff0000, "png"),
                blue = TileTestImages.solid(0x0000ff, "png");
        AtomicInteger skipped = new AtomicInteger();
        TileCompositionMember outside = member(area, true, blue);
        TileCompositionMember inside = member(area, false, red);
        TileCompositionMember higherOnly =
                new TileCompositionMember(
                        1,
                        5,
                        0,
                        5,
                        0,
                        null,
                        false,
                        false,
                        zxy -> {
                            skipped.incrementAndGet();
                            return TileFusionResult.legacy(blue);
                        });
        TileCompositionResult result =
                TileComposer.compose(
                        GridInitUtils.getWorldGrid3857(),
                        0,
                        0,
                        0,
                        256,
                        Arrays.asList(outside, inside, higherOnly),
                        false);
        BufferedImage image = TileImageUtils.readImage(result.getPng());
        try {
            assertEquals(TileFusionState.COMPLETE, result.getState());
            assertEquals(0, skipped.get());
            assertEquals(0xffff0000, image.getRGB(200, 128));
            assertEquals(0xff0000ff, image.getRGB(149, 128));
            assertEquals(0xff0000ff, image.getRGB(64, 128));
        } finally {
            image.flush();
        }
    }

    @Test
    public void fallbackUsesActualGeographicParentAndPartialResultsAreNotCacheable()
            throws Exception {
        byte[] green = TileTestImages.solid(0x00ff00, "png");
        AtomicInteger parentReads = new AtomicInteger();
        TileCompositionMember parent =
                new TileCompositionMember(
                        1,
                        2,
                        0,
                        0,
                        1,
                        null,
                        false,
                        false,
                        tile -> {
                            parentReads.incrementAndGet();
                            assertEquals(0, tile.getZ());
                            assertEquals(0, tile.getY());
                            return TileFusionResult.legacy(green);
                        });
        TileCompositionMember broken =
                new TileCompositionMember(
                        1,
                        2,
                        1,
                        1,
                        0,
                        null,
                        false,
                        false,
                        tile -> {
                            throw new IllegalStateException("上游读失败");
                        });
        TileCompositionResult result =
                TileComposer.compose(
                        GridInitUtils.getTdtGrid4490(),
                        1,
                        1,
                        0,
                        256,
                        Arrays.asList(parent, broken),
                        true);
        assertEquals(1, parentReads.get());
        assertEquals(0, result.getReadZoom());
        assertEquals(TileFusionState.PARTIAL, result.getState());
        assertFalse(result.isCacheable());

        TileCompositionResult direct =
                TileComposer.compose(
                        GridInitUtils.getTdtGrid4490(),
                        1,
                        1,
                        0,
                        256,
                        Arrays.asList(parent, member(null, false, green)),
                        true);
        assertEquals(1, direct.getReadZoom());
        assertEquals(1, parentReads.get());
        assertTrue(direct.isCacheable());
        assertEquals(
                TileFusionState.NO_COVERAGE,
                TileComposer.compose(
                                GridInitUtils.getTdtGrid4490(),
                                1,
                                1,
                                0,
                                256,
                                Collections.emptyList(),
                                true)
                        .getState());
    }

    private TileCompositionMember member(Geometry coverage, boolean outside, byte[] png) {
        return new TileCompositionMember(
                0, 5, 0, 5, 0, coverage, outside, true, zxy -> TileFusionResult.legacy(png));
    }
}
