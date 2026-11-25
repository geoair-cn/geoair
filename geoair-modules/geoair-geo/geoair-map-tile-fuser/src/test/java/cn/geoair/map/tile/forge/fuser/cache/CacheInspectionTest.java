package cn.geoair.map.tile.forge.fuser.cache;

import static org.junit.Assert.*;

import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.fuser.execution.*;
import cn.geoair.web.mime.GirImageMime;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class CacheInspectionTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private TileOperationContext context() {
        return new TileOperationContext(new TileCancellationToken(), 10000);
    }

    @Test
    public void fileBatchMatchesPointReadsAndCountsPhysicalBytes() throws Exception {
        FileTileCache cache = new FileTileCache(folder.getRoot().toString(), 0, true);
        cache.put("layer", 2, 0, 1, new byte[] {1, 2, 3}, GirImageMime.png);
        cache.put("layer", 2, 1, 2, new byte[] {4, 5}, GirImageMime.png);
        TileCacheQuery query =
                new TileCacheQuery("layer", 2, new RangeApo(0, 1, 0, 3, 2), 1, 7, GirImageMime.png);
        BitSet found = cache.findExisting(query, context());
        assertEquals(2, found.cardinality());
        for (long i = query.getFrom(); i < query.getTo(); i++)
            assertEquals(
                    cache.get("layer", 2, query.x(i), query.y(i), GirImageMime.png) != null,
                    found.get((int) (i - query.getFrom())));
        TileCacheStatistics stats = cache.statistics("layer", context());
        assertEquals(2, stats.getTileCount());
        assertEquals(5, stats.getStorageBytes());
        assertEquals(5, stats.getTileBytes());
        assertEquals(CacheStorageKind.FILE, stats.getDriver());
        assertEquals(2, stats.getLevels().get(0).getZoom());
    }

    @Test
    public void expiredAndZeroFilesAreNotBatchHits() throws Exception {
        FileTileCache writer = new FileTileCache(folder.getRoot().toString(), 0, true);
        writer.put("layer", 2, 0, 1, new byte[] {1}, GirImageMime.png);
        try (java.util.stream.Stream<Path> paths = Files.walk(folder.getRoot().toPath())) {
            for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator)
                Files.setLastModifiedTime(path, java.nio.file.attribute.FileTime.fromMillis(1));
        }
        FileTileCache reader = new FileTileCache(folder.getRoot().toString(), 100, true);
        assertTrue(
                reader.findExisting(
                                new TileCacheQuery(
                                        "layer",
                                        2,
                                        new RangeApo(0, 0, 0, 3, 2),
                                        0,
                                        4,
                                        GirImageMime.png),
                                context())
                        .isEmpty());
    }

    @Test
    public void mbtilesReadOnlyDoesNotCreateDatabaseAndBatchRespectsOffsets() throws Exception {
        MBTilesTileCache cache = new MBTilesTileCache(folder.getRoot().toString(), true);
        try {
            TileCacheQuery absent =
                    new TileCacheQuery(
                            "absent", 2, new RangeApo(0, 0, 0, 3, 2), 0, 4, GirImageMime.png);
            assertTrue(cache.findExisting(absent, context()).isEmpty());
            assertEquals(0, folder.getRoot().list().length);
            assertTrue(cache.put("layer", 2, 0, 1, new byte[] {1, 2, 3}, GirImageMime.png));
            assertTrue(cache.put("layer", 2, 1, 2, new byte[] {4}, GirImageMime.png));
            TileCacheQuery query =
                    new TileCacheQuery(
                            "layer", 2, new RangeApo(0, 1, 0, 3, 2), 1, 7, GirImageMime.png);
            BitSet found = cache.findExisting(query, context());
            assertEquals(2, found.cardinality());
            for (long i = query.getFrom(); i < query.getTo(); i++)
                assertEquals(
                        cache.get("layer", 2, query.x(i), query.y(i), GirImageMime.png) != null,
                        found.get((int) (i - query.getFrom())));
            TileCacheStatistics stats = cache.statistics("layer", context());
            assertEquals(2, stats.getTileCount());
            assertEquals(4, stats.getTileBytes());
            assertTrue(stats.getStorageBytes() >= 4);
            assertEquals(CacheStorageKind.MBTILES, stats.getDriver());
        } finally {
            cache.close();
        }
    }

    @Test
    public void cancelledInspectionFailsWithoutWalking() {
        TileCancellationToken token = new TileCancellationToken();
        token.cancel();
        FileTileCache cache = new FileTileCache(folder.getRoot().toString(), 0, true);
        assertEquals(
                TileOperationStatus.CANCELLED,
                assertThrows(
                                TileOperationException.class,
                                () -> cache.statistics("x", new TileOperationContext(token, 1000)))
                        .getStatus());
    }
}
