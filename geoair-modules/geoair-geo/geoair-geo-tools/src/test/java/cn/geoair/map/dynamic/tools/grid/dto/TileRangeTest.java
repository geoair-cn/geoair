package cn.geoair.map.dynamic.tools.grid.dto;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.GirTileConverterOpt;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/** TileRange 闭区间语义测试。 */
public class TileRangeTest {

    @Test
    public void shouldConvertGeoToolsExclusiveMaximumToClosedRange() {
        RangeApo legacyRange = new RangeApo(10, 13, 20, 22, 8);

        TileRange range = TileRange.fromGeoToolsExclusiveMax(legacyRange);

        Assert.assertEquals(10, range.getMinX());
        Assert.assertEquals(12, range.getMaxX());
        Assert.assertEquals(20, range.getMinY());
        Assert.assertEquals(21, range.getMaxY());
        Assert.assertEquals(6L, range.getTileCount());
        Assert.assertEquals(TileYAxis.XYZ, range.getYAxis());
        Assert.assertEquals(13, legacyRange.getMaxX());
        Assert.assertEquals(22, legacyRange.getMaxY());
    }

    @Test
    public void shouldConvertTmsYUsingActualTileRowCount() {
        GirTileConverterOpt tileConverter = GirGeoTools.defaultInstance().getTileGrid3857Opt();

        Assert.assertEquals(7, tileConverter.convertY(3, 0, TileYAxis.TMS, TileYAxis.XYZ));
        Assert.assertEquals(0, tileConverter.convertY(3, 7, TileYAxis.XYZ, TileYAxis.TMS));
        Assert.assertEquals(tileConverter.reverseY(7, 3),
                tileConverter.convertY(3, 7, TileYAxis.XYZ, TileYAxis.TMS));
        Assert.assertEquals(tileConverter.xyzToTileBox(3, 2, 7, 3857).getMinY(),
                tileConverter.xyzToTileBox(3, 2, 7, TileYAxis.XYZ, 3857).getMinY(), 0D);
        Assert.assertEquals(tileConverter.xyzToTileBox(3, 2, 7, TileYAxis.XYZ, 3857).getMinY(),
                tileConverter.xyzToTileBox(3, 2, 0, TileYAxis.TMS, 3857).getMinY(), 0D);
        Assert.assertEquals(tileConverter.xyzToTileBox(3, 2, 7, TileYAxis.XYZ, 3857).getMaxY(),
                tileConverter.xyzToTileBox(3, 2, 0, TileYAxis.TMS, 3857).getMaxY(), 0D);
        Assert.assertEquals(TileYAxis.TMS,
                tileConverter.tileRangeClosedByBox(3, tileConverter.xyzToTileBox(
                        3, 2, 0, TileYAxis.TMS, 3857), TileYAxis.TMS).getYAxis());

        GirTileConverterOpt separateAxisConverter = GirGeoTools.defaultInstance().getTileGrid4326SeparateOpt();
        Assert.assertEquals(1, separateAxisConverter.getTileLevelMetadata(0).getNumTilesHigh());
        Assert.assertEquals(0, separateAxisConverter.convertY(0, 0, TileYAxis.XYZ, TileYAxis.TMS));
        Assert.assertEquals(separateAxisConverter.reverseY(30, 8),
                separateAxisConverter.convertY(8, 30, TileYAxis.XYZ, TileYAxis.TMS));
    }

    @Test
    public void shouldCalculateBoundsForClosedTileRange() {
        GirTileConverterOpt tileConverter = GirGeoTools.defaultInstance().getTileGrid3857Opt();
        BoxReferencedEnvelope expected = tileConverter.xyzToTileBox(3, 2, 7, 3857);
        TileRange tileRange = TileRange.closed(3, 2, 2, 7, 7, TileYAxis.XYZ);

        BoxReferencedEnvelope actual = tileConverter.boundsFromTileRange(tileRange, 3857);
        BoxReferencedEnvelope fromTiles = tileConverter.boundsFromTileZxyApos(
                Collections.singleton(new TileZxyApo(3, 2, 7)), 3857);

        Assert.assertEquals(expected.getMinX(), actual.getMinX(), 0D);
        Assert.assertEquals(expected.getMaxX(), actual.getMaxX(), 0D);
        Assert.assertEquals(expected.getMinY(), actual.getMinY(), 0D);
        Assert.assertEquals(expected.getMaxY(), actual.getMaxY(), 0D);
        Assert.assertEquals(expected.getMinX(), fromTiles.getMinX(), 0D);
        Assert.assertEquals(expected.getMaxX(), fromTiles.getMaxX(), 0D);
        Assert.assertEquals(expected.getMinY(), fromTiles.getMinY(), 0D);
        Assert.assertEquals(expected.getMaxY(), fromTiles.getMaxY(), 0D);
    }
}
