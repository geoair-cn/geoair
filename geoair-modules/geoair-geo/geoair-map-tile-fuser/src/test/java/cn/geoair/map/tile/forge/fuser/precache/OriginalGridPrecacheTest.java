package cn.geoair.map.tile.forge.fuser.precache;

import static org.junit.Assert.*;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.fuser.cache.FileTileCache;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.execution.*;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesSourceReader;
import cn.geoair.map.tile.forge.fuser.provider.impl.MBTilesTileGetter;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.locationtech.jts.geom.*;

import java.nio.file.Path;
import java.sql.*;
import java.util.List;
import java.util.concurrent.*;

/** 同一地物分别使用两种网格、两种行原点，验证只写原始缓存及重复执行复用。 */
public class OriginalGridPrecacheTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void sourceLevelsAndRowsRemainCorrectForBothGridsAndOrigins() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            for (int srid : new int[] {3857, 4326})
                for (boolean topLeft : new boolean[] {false, true}) {
                    Path file =
                            folder.getRoot().toPath().resolve(srid + "_" + topLeft + ".mbtiles");
                    int row = FuserCacheUtils.getStoreY(3, 1, topLeft, srid);
                    try (Connection db =
                                    DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                            Statement sql = db.createStatement()) {
                        sql.execute(
                                "CREATE TABLE tiles(zoom_level INTEGER,tile_column INTEGER,tile_row INTEGER,tile_data BLOB,PRIMARY KEY(zoom_level,tile_column,tile_row))");
                        sql.execute("INSERT INTO tiles VALUES(3,2," + row + ",X'010203')");
                    }
                    PxyLayerInfo layer =
                            new PxyLayerInfo()
                                    .setLayerName("source_" + srid + "_" + topLeft)
                                    .setPath(file.toString())
                                    .setGridSrid(srid)
                                    .setTileRowOrigin(topLeft ? "top-left" : "bottom-left");
                    FileTileCache cache =
                            new FileTileCache(
                                    folder.getRoot().toPath().resolve("cache").toString(), 0, true);
                    try (MbtilesSourceReader reader = new MbtilesSourceReader();
                            MBTilesTileGetter getter = new MBTilesTileGetter(layer, reader)) {
                        BoundingBox box =
                                getter.getSrcGridSubset().boundsFromIndex(new long[] {2, 1, 3});
                        double dx = (box.getMaxX() - box.getMinX()) / 4,
                                dy = (box.getMaxY() - box.getMinY()) / 4;
                        Geometry region =
                                new GeometryFactory()
                                        .toGeometry(
                                                new Envelope(
                                                        box.getMinX() + dx,
                                                        box.getMaxX() - dx,
                                                        box.getMinY() + dy,
                                                        box.getMaxY() - dy));
                        if (srid == 3857)
                            region = GirAdvTools.getSridOpt().convert(region, 3857, 4326);
                        OriginalGridPrecache precache =
                                new OriginalGridPrecache(layer.getLayerName(), getter, cache);
                        List<TilePrecacheLevel> plan = precache.plan(region, 3, 3);
                        assertEquals(1, plan.size());
                        assertEquals(3, plan.get(0).getZoom());
                        assertEquals(1, plan.get(0).getTotal());
                        List<TilePrecacheProgress> first =
                                precache.execute(
                                        region,
                                        3,
                                        3,
                                        workers,
                                        new TileCancellationToken(),
                                        new TilePrecacheOptions(),
                                        null);
                        assertEquals(
                                Long.valueOf(1),
                                first.get(0).getCounts().get(TileOperationStatus.DOWNLOADED));
                        assertArrayEquals(
                                new byte[] {1, 2, 3},
                                cache.get(precache.getCacheName(), 3, 2, 1, getter.getSrcFormat()));
                        List<TilePrecacheProgress> second =
                                precache.execute(
                                        region,
                                        3,
                                        3,
                                        workers,
                                        new TileCancellationToken(),
                                        new TilePrecacheOptions(),
                                        null);
                        assertEquals(
                                Long.valueOf(1),
                                second.get(0).getCounts().get(TileOperationStatus.CACHED));
                    }
                }
        } finally {
            workers.shutdownNow();
        }
    }
}
