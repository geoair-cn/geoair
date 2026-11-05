package cn.geoair.map.tile.forge.fuser.precache;

import static org.junit.Assert.*;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;

import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

public class TileRegionMaskTest {
    @Test
    public void fullyCoveredBlockDoesNotVisitEachTileAndCanBeStopped() throws Exception {
        Geometry region =
                new WKTReader().read("POLYGON((-180 -90,180 -90,180 90,-180 90,-180 -90))");
        Geometry interior = new WKTReader().read("POLYGON((-20 -20,20 -20,20 20,-20 20,-20 -20))");
        RangeApo range = GirAdvTools.getTileGrid4326Opt().tileRangeByGeom(8, interior);
        long rows = range.getMaxY() - range.getMinY() + 1;
        long total = rows * (range.getMaxX() - range.getMinX() + 1);
        AtomicInteger checks = new AtomicInteger();
        BitSet found =
                new TileRegionMask(
                                region,
                                8,
                                GirAdvTools.getTileGrid4326Opt(),
                                TileYAxis.XYZ,
                                checks::incrementAndGet)
                        .classify(range, rows, 0, total);
        assertEquals(total, found.cardinality());
        assertTrue("内部完整列块应直接判断，不逐瓦片访问", checks.get() < total / 4);
        assertThrows(
                IllegalStateException.class,
                () ->
                        new TileRegionMask(
                                        region,
                                        8,
                                        GirAdvTools.getTileGrid4326Opt(),
                                        TileYAxis.XYZ,
                                        () -> {
                                            throw new IllegalStateException("停止");
                                        })
                                .classify(range, rows, 0, total));
    }

    @Test
    public void blockClassificationMatchesEveryTileForHolesAndMultiplePolygons() throws Exception {
        String[] samples = {
            "POLYGON((-50 -50,50 -50,50 50,-50 50,-50 -50),(-10 -10,-10 10,10 10,10 -10,-10 -10))",
            "MULTIPOLYGON(((-80 -40,-20 -40,-50 40,-80 -40)),((20 -40,80 -40,80 40,20 -40)))"
        };
        for (boolean mercator : new boolean[] {true, false})
            for (String wkt : samples) {
                Geometry geometry = new WKTReader().read(wkt);
                int z = 5;
                RangeApo range =
                        mercator
                                ? GirAdvTools.getTileGrid4326Opt().tileRangeByGeom(z, geometry)
                                : GirAdvTools.getTileGrid3857Opt()
                                        .tileRangeByGeom(
                                                z,
                                                GirAdvTools.getSridOpt()
                                                        .convert(geometry, 4326, 3857));
                long rows = range.getMaxY() - range.getMinY() + 1,
                        total = rows * (range.getMaxX() - range.getMinX() + 1);
                for (long from = 0; from < total; from += 37) {
                    long to = Math.min(total, from + 37);
                    BitSet mask =
                            new TileRegionMask(
                                            geometry,
                                            z,
                                            mercator
                                                    ? GirAdvTools.getTileGrid4326Opt()
                                                    : GirAdvTools.getTileGrid3857Opt(),
                                            TileYAxis.XYZ)
                                    .classify(range, rows, from, to);
                    for (long i = from; i < to; i++) {
                        int x = (int) (range.getMinX() + i / rows),
                                y = (int) (range.getMinY() + i % rows);
                        BoxReferencedEnvelope box =
                                mercator
                                        ? GirAdvTools.getTileGrid4326Opt()
                                                .xyzToTileBox(z, x, y, TileYAxis.XYZ, 3857)
                                        : GirAdvTools.getTileGrid3857Opt()
                                                .xyzToTileBox(z, x, y, TileYAxis.XYZ, 4326);
                        assertEquals(
                                geometry.intersects(new WKTReader().read(box.getWktString(4326))),
                                mask.get((int) (i - from)));
                    }
                }
            }
    }
}
