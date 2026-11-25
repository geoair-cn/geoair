package cn.geoair.map.tile.forge.fuser.cache;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.fuser.execution.*;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesUtils;
import cn.geoair.map.tile.forge.fuser.mbtiles.statistics.MbtilesScanState;
import cn.geoair.web.mime.GirImageMime;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class CacheConcurrencyTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void independentFileCacheInstancesCannotReadTruncatedTiles() throws Exception {
        FileTileCache writer = new FileTileCache(folder.getRoot().toString(), 0, true);
        FileTileCache reader = new FileTileCache(folder.getRoot().toString(), 0, true);
        byte[] first = new byte[65536], second = new byte[32768];
        Arrays.fill(first, (byte) 1);
        Arrays.fill(second, (byte) 2);
        assertTrue(writer.put("file", 3, 2, 1, first, GirImageMime.png));
        ExecutorService workers = Executors.newFixedThreadPool(3);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> put =
                    workers.submit(
                            () -> {
                                await(start);
                                for (int i = 0; i < 100; i++)
                                    assertTrue(
                                            writer.put(
                                                    "file",
                                                    3,
                                                    2,
                                                    1,
                                                    i % 2 == 0 ? first : second,
                                                    GirImageMime.png));
                            });
            List<Future<?>> reads = new ArrayList<>();
            for (int thread = 0; thread < 2; thread++)
                reads.add(
                        workers.submit(
                                () -> {
                                    await(start);
                                    for (int i = 0; i < 100; i++) {
                                        byte[] bytes =
                                                reader.get("file", 3, 2, 1, GirImageMime.png);
                                        assertTrue(
                                                Arrays.equals(first, bytes)
                                                        || Arrays.equals(second, bytes));
                                    }
                                }));
            start.countDown();
            put.get(10, TimeUnit.SECONDS);
            for (Future<?> read : reads) read.get(10, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    public void normalAndProgressStatisticsKeepEmptyTileAndReadOnlySemantics() throws Exception {
        MBTilesTileCache cache = new MBTilesTileCache(folder.getRoot().toString());
        try {
            assertTrue(cache.put("stats", 3, 0, 0, new byte[] {1, 2}, GirImageMime.png));
            Path file =
                    folder.getRoot()
                            .toPath()
                            .resolve(MbtilesUtils.getSafeFileName("stats") + ".mbtiles");
            try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                    Statement sql = db.createStatement()) {
                sql.execute(
                        "WITH RECURSIVE seq(x) AS (SELECT 1 UNION ALL SELECT x+1 FROM seq WHERE x<10001) "
                                + "INSERT INTO tiles SELECT 3,x,0,X'' FROM seq");
            }
            TileCacheStatistics summary =
                    cache.statistics(
                            "stats", new TileOperationContext(new TileCancellationToken(), 10000));
            assertEquals(1, summary.getTileCount());
            assertEquals(2, summary.getTileBytes());
            assertEquals(
                    MbtilesScanState.COMPLETED,
                    cache.scanStatistics("stats", null, null).getScanState());
            try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                    Statement sql = db.createStatement();
                    ResultSet rows =
                            sql.executeQuery(
                                    "SELECT COUNT(*) FROM sqlite_master WHERE name GLOB 'tf_stats_*'")) {
                rows.next();
                assertEquals(0, rows.getInt(1));
            }
        } finally {
            cache.close();
        }
    }

    private void await(CountDownLatch start) {
        try {
            assertTrue(start.await(3, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
