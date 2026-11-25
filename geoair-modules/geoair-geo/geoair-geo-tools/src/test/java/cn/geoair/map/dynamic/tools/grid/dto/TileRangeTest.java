package cn.geoair.map.dynamic.tools.grid.dto;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.GirTileConverterOpt;
import cn.geoair.map.dynamic.tools.grid.converter.AbstractWgs84TileConverter;

import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;

import java.util.Collections;
import java.util.Set;

/** TileRange 闭区间语义测试。 */
public class TileRangeTest {

    @Test
    public void shouldTreatRangeApoMaximumAsClosedForGeometryAndTileList() {
        GirTileConverterOpt tileConverter = GirGeoTools.defaultInstance().getTileGrid3857Opt();
        Geometry geometry =
                GirGeoTools.defaultInstance()
                        .getSridOpt()
                        .convertToGeom(tileConverter.xyzToTileBox(3, 2, 4, 3857));

        RangeApo range = tileConverter.tileRangeByGeom(3, geometry);
        Set<TileZxyApo> tiles = tileConverter.zxyListByGeom(geometry, 3857, 3);

        Assert.assertTrue(range.getMinX() <= range.getMaxX());
        Assert.assertTrue(range.getMinY() <= range.getMaxY());
        // 该几何在历史计算中会因边界精度补偿覆盖 3 × 3 个瓦片；语义收敛后保持不变。
        Assert.assertEquals(9, tiles.size());
        Assert.assertTrue(contains(tiles, range.getMinX(), range.getMinY()));
        Assert.assertTrue(contains(tiles, range.getMaxX(), range.getMaxY()));
    }

    @Test
    public void shouldConvertTmsYUsingActualTileRowCount() {
        GirTileConverterOpt tileConverter = GirGeoTools.defaultInstance().getTileGrid3857Opt();

        Assert.assertEquals(7, tileConverter.convertY(3, 0, TileYAxis.TMS, TileYAxis.XYZ));
        Assert.assertEquals(0, tileConverter.convertY(3, 7, TileYAxis.XYZ, TileYAxis.TMS));
        Assert.assertEquals(
                tileConverter.reverseY(7, 3),
                tileConverter.convertY(3, 7, TileYAxis.XYZ, TileYAxis.TMS));
        Assert.assertEquals(
                tileConverter.xyzToTileBox(3, 2, 7, 3857).getMinY(),
                tileConverter.xyzToTileBox(3, 2, 7, TileYAxis.XYZ, 3857).getMinY(),
                0D);
        Assert.assertEquals(
                tileConverter.xyzToTileBox(3, 2, 7, TileYAxis.XYZ, 3857).getMinY(),
                tileConverter.xyzToTileBox(3, 2, 0, TileYAxis.TMS, 3857).getMinY(),
                0D);
        Assert.assertEquals(
                tileConverter.xyzToTileBox(3, 2, 7, TileYAxis.XYZ, 3857).getMaxY(),
                tileConverter.xyzToTileBox(3, 2, 0, TileYAxis.TMS, 3857).getMaxY(),
                0D);
        GirTileConverterOpt separateAxisConverter =
                GirGeoTools.defaultInstance().getTileGrid4326SeparateOpt();
        Assert.assertEquals(1, separateAxisConverter.getTileLevelMetadata(0).getNumTilesHigh());
        Assert.assertEquals(0, separateAxisConverter.convertY(0, 0, TileYAxis.XYZ, TileYAxis.TMS));
        Assert.assertEquals(
                separateAxisConverter.reverseY(30, 8),
                separateAxisConverter.convertY(8, 30, TileYAxis.XYZ, TileYAxis.TMS));
    }

    @Test
    public void shouldCalculateBoundsForClosedTileRange() {
        GirTileConverterOpt tileConverter = GirGeoTools.defaultInstance().getTileGrid3857Opt();
        BoxReferencedEnvelope expected = tileConverter.xyzToTileBox(3, 2, 7, 3857);
        TileRange tileRange = TileRange.closed(3, 2, 2, 7, 7, TileYAxis.XYZ);

        BoxReferencedEnvelope actual = tileConverter.boundsFromTileRange(tileRange, 3857);
        BoxReferencedEnvelope fromTiles =
                tileConverter.boundsFromTileZxyApos(
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

    @Test
    public void shouldUseFourLatitudeRowsForSeparateAxisAtZoomThree() {
        GirTileConverterOpt tileConverter =
                GirGeoTools.defaultInstance().getTileGrid4326SeparateOpt();

        Assert.assertEquals(4, tileConverter.getTileLevelMetadata(3).getNumTilesHigh());
        Assert.assertEquals(45D, tileConverter.xyzToTileBox(3, 0, 0, 4326).getMinY(), 0D);
        Assert.assertEquals(-90D, tileConverter.xyzToTileBox(3, 0, 3, 4326).getMinY(), 0D);
        Assert.assertEquals(-90D, tileConverter.tileYToCoordinateY(4, 3), 0D);
        Assert.assertEquals(
                3,
                tileConverter
                        .tileRangeByBox(
                                3, new org.locationtech.jts.geom.Envelope(-180D, 180D, -90D, 90D))
                        .getMaxY());
    }

    @Test
    public void shouldConvertCurrentSeparateAndEqualAxisYUsingActualRowCount() {
        GirTileConverterOpt separate = GirGeoTools.defaultInstance().getTileGrid4326SeparateOpt();
        GirTileConverterOpt equal = GirGeoTools.defaultInstance().getTileGrid4326Opt();

        Assert.assertEquals(4, separate.getTileRowCount(3));
        Assert.assertEquals(4, equal.getTileRowCount(3));
        for (int y = 0; y < 4; y++) {
            Assert.assertEquals(
                    y,
                    separate.convertSeparateAxisYToEqualAxisY(
                            y, 3, AbstractWgs84TileConverter.RoundingType.FLOOR));
            Assert.assertEquals(
                    y,
                    equal.convertEqualAxisYToSeparateAxisY(
                            y, 3, AbstractWgs84TileConverter.RoundingType.CEIL));
        }

        try {
            separate.convertSeparateAxisYToEqualAxisY(
                    4, 3, AbstractWgs84TileConverter.RoundingType.ROUND);
            Assert.fail("z=3 的 Separate 网格仅有 4 行，Y=4 必须被拒绝");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("合法范围0~3"));
        }
    }

    private boolean contains(Set<TileZxyApo> tiles, int x, int y) {
        for (TileZxyApo tile : tiles) {
            if (tile.getX() == x && tile.getY() == y) {
                return true;
            }
        }
        return false;
    }
}
