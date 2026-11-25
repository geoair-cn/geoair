package cn.geoair.map.tile.forge.fuser.provider;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSet;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSetFactory;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubsetFactory;
import cn.geoair.map.tile.forge.core.bygwc.grid.SRS;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.fuser.fuser.TileFusionState;
import cn.geoair.map.tile.forge.fuser.support.FusionTestSource;
import cn.geoair.map.tile.forge.fuser.utils.GridInitUtils;
import org.junit.Test;

public class SameGridTileReaderTest {
    @Test
    public void geographicAliasesKeepLevelAndConvertXyzToTmsExactlyOnce() throws Exception {
        GridSubset grid = GridInitUtils.getTdtGrid4490();
        FusionTestSource source =
                new FusionTestSource(grid, (x, y) -> new ByteArrayResource(new byte[] {1}));
        for (int level : new int[] {0, 1, 5, 16}) {
            assertEquals(
                    TileFusionState.COMPLETE,
                    SameGridTileReader.read(
                                    source, 4326, 4490, level, 0, 0, MissingTileMeaning.NO_COVERAGE)
                            .getState());
            assertEquals(level, source.lastZ);
            assertEquals(grid.getGridSet().getGrid(level).getNumTilesHigh() - 1, source.lastY);
            assertEquals(0, source.lastX);
        }
        assertEquals(4, source.calls.get());
    }

    @Test
    public void mercatorAliasDoesNotChangeColumnsOrLevels() throws Exception {
        FusionTestSource source =
                new FusionTestSource(GridInitUtils.getWorldGrid3857(), (x, y) -> null);
        assertEquals(
                TileFusionState.NO_COVERAGE,
                SameGridTileReader.read(
                                source, 900913, 3857, 3, 5, 2, MissingTileMeaning.NO_COVERAGE)
                        .getState());
        assertEquals(3, source.lastZ);
        assertEquals(5, source.lastX);
        assertEquals(5, source.lastY);
        assertFalse(SameGridTileReader.equivalentSrid(3857, 4490));
    }

    @Test
    public void rejectsDifferentGridAndInvalidRowBeforeReadingSource() throws Exception {
        FusionTestSource source =
                new FusionTestSource(GridInitUtils.getWorldGrid3857(), (x, y) -> null);
        try {
            SameGridTileReader.read(source, 3857, 4490, 2, 1, 1, MissingTileMeaning.UNAVAILABLE);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try {
            SameGridTileReader.read(source, 3857, 3857, 2, 1, 4, MissingTileMeaning.UNAVAILABLE);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(0, source.calls.get());
    }

    @Test
    public void sameSridWithDifferentMatrixCannotSilentlyUseDirectRead() throws Exception {
        GridSet custom =
                GridSetFactory.createGridSet(
                        "custom512",
                        SRS.getEPSG3857(),
                        BoundingBox.WORLD3857,
                        false,
                        5,
                        null,
                        GridSetFactory.DEFAULT_PIXEL_SIZE_METER,
                        512,
                        512,
                        false);
        GridSubset grid = GridSubsetFactory.createGridSubSet(custom, BoundingBox.WORLD3857, 0, 4);
        FusionTestSource source = new FusionTestSource(grid, (x, y) -> null);
        try {
            SameGridTileReader.read(source, 3857, 3857, 2, 1, 1, MissingTileMeaning.NO_COVERAGE);
            fail("瓦片矩阵不同不能仅凭 SRID 判定同网格");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(0, source.calls.get());
    }
}
