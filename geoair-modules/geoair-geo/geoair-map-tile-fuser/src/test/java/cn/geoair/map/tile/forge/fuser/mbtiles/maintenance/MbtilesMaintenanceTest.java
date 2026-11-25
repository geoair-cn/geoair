package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.fuser.cache.MBTilesTileCache;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesAccessCoordinator;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesAccessLease;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesMaintenanceException;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesSourceReader;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sqlite.SQLiteDataSource;

/** 验证门面实际效果、旧入口兼容以及读写端遵守同一物理文件维护租约。 */
public class MbtilesMaintenanceTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private Path database(String name, boolean incremental) throws Exception {
        Path file = folder.getRoot().toPath().resolve(name);
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                Statement sql = db.createStatement()) {
            if (incremental) sql.execute("PRAGMA auto_vacuum=INCREMENTAL");
            sql.execute("PRAGMA journal_mode=WAL");
            sql.execute(
                    "CREATE TABLE tiles(zoom_level INTEGER,tile_column INTEGER,tile_row INTEGER,tile_data BLOB,PRIMARY KEY(zoom_level,tile_column,tile_row))");
            sql.execute("INSERT INTO tiles VALUES(3,2,1,x'0102')");
            sql.execute("CREATE TABLE metadata(name TEXT PRIMARY KEY,value TEXT)");
            sql.execute("CREATE TABLE filler(data BLOB)");
            sql.execute("INSERT INTO filler VALUES(zeroblob(3000000))");
            sql.execute("DELETE FROM filler");
            sql.execute("PRAGMA wal_checkpoint(TRUNCATE)");
        }
        return file;
    }

    @Test
    public void realPartialCheckpointDoesNotReportSuccessAndTruncateCompletesAfterReaderEnds()
            throws Exception {
        Path file = database("wal 中文 #.mbtiles", false);
        MbtilesMaintenance maintenance = new MbtilesMaintenance();
        try (Connection reader = DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                Connection writer = DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                Statement read = reader.createStatement();
                Statement write = writer.createStatement()) {
            write.execute("PRAGMA wal_autocheckpoint=0");
            read.execute("BEGIN");
            try (ResultSet snapshot = read.executeQuery("SELECT tile_data FROM tiles")) {
                assertTrue(snapshot.next());
            }
            write.execute("UPDATE tiles SET tile_data=x'0304'");
            MbtilesMaintenanceResult partial =
                    maintenance.checkpoint(file, MbtilesCheckpointMode.PASSIVE, () -> false);
            assertEquals(MbtilesMaintenanceStatus.PARTIAL, partial.getStatus());
            SQLiteDataSource legacySource = new SQLiteDataSource();
            legacySource.setUrl("jdbc:sqlite:" + file.toUri());
            assertFalse(MbtilesUtils.walCheckpoint(legacySource, "PASSIVE"));
            assertTrue(
                    partial.getCheckpoint().getLogPages()
                            > partial.getCheckpoint().getCheckpointedPages());
            read.execute("ROLLBACK");
            MbtilesMaintenanceResult done =
                    maintenance.checkpoint(file, MbtilesCheckpointMode.TRUNCATE, () -> false);
            assertEquals(MbtilesMaintenanceStatus.COMPLETED, done.getStatus());
            assertEquals(0, Files.size(file.resolveSibling(file.getFileName() + "-wal")));
        }
    }

    @Test
    public void incrementalBatchesDoNotTrustMutatedCallbackAndLegacyCompactActuallyShrinksFile()
            throws Exception {
        Path incremental = database("incremental.mbtiles", true);
        MbtilesMaintenance maintenance = new MbtilesMaintenance();
        MbtilesMaintenanceResult result =
                maintenance.reclaim(
                        incremental,
                        MbtilesReclaimMode.AUTO,
                        () -> false,
                        info -> info.setFreePages(0));
        assertEquals(0, result.getAfter().getFreePages());
        assertTrue(result.getAfter().getMainBytes() < result.getBefore().getMainBytes());
        Path normal = database("normal.mbtiles", false);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        maintenance.reclaim(
                                normal, MbtilesReclaimMode.INCREMENTAL, () -> false, null));
        long before = Files.size(normal);
        assertTrue(MbtilesUtils.compactDatabase(normal.toString()));
        assertTrue(Files.size(normal) < before);
        assertArrayEquals(
                new byte[] {1, 2},
                new MbtilesMaintenanceDao()
                        .readTile(normal, new MbtilesTileKey(3, 2, 1, 2), () -> false));
    }

    @Test
    public void insufficientSpaceAndCancellationNeverStartVacuum() throws Exception {
        Path file = database("space.mbtiles", false);
        AtomicBoolean vacuumStarted = new AtomicBoolean();
        MbtilesMaintenanceDao dao =
                new MbtilesMaintenanceDao() {
                    @Override
                    public MbtilesFileInfo inspect(Path path) throws Exception {
                        MbtilesFileInfo info = super.inspect(path);
                        info.setVacuumSpaceEnough(false);
                        return info;
                    }

                    @Override
                    public void vacuum(Path path, BooleanSupplier cancelled) {
                        vacuumStarted.set(true);
                    }
                };
        MbtilesMaintenance maintenance = new MbtilesMaintenance(dao);
        assertThrows(
                IllegalStateException.class,
                () -> maintenance.reclaim(file, MbtilesReclaimMode.VACUUM, () -> false, null));
        assertThrows(
                InterruptedException.class,
                () -> maintenance.reclaim(file, MbtilesReclaimMode.VACUUM, () -> true, null));
        assertFalse(vacuumStarted.get());
        try (MbtilesAccessLease operation = MbtilesAccessCoordinator.operation(file)) {
            assertNotNull(operation);
        }
    }

    @Test
    public void sourceAndCacheRejectActiveExclusiveMaintenanceThenRecover() throws Exception {
        MBTilesTileCache cache = new MBTilesTileCache(folder.getRoot().toString());
        ExecutorService worker = Executors.newSingleThreadExecutor();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        try (MbtilesSourceReader source = new MbtilesSourceReader()) {
            assertTrue(cache.put("source", 3, 2, 1, new byte[] {1, 2}, ImageMime.png));
            Path file = cache.layerFile("source");
            Future<?> exclusive =
                    worker.submit(
                            () -> {
                                try (MbtilesAccessLease lease =
                                        MbtilesAccessCoordinator.maintenance(file, () -> false)) {
                                    entered.countDown();
                                    release.await(10, TimeUnit.SECONDS);
                                } catch (Exception failure) {
                                    throw new IllegalStateException(failure);
                                }
                            });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertThrows(
                    MbtilesMaintenanceException.class,
                    () -> source.readTile(file.toString(), 3, 2, 1));
            assertThrows(
                    MbtilesMaintenanceException.class,
                    () -> cache.get("source", 3, 2, 1, ImageMime.png));
            assertThrows(
                    MbtilesMaintenanceException.class,
                    () -> cache.put("source", 3, 2, 1, new byte[] {3}, ImageMime.png));
            release.countDown();
            exclusive.get(5, TimeUnit.SECONDS);
            assertArrayEquals(new byte[] {1, 2}, cache.get("source", 3, 2, 1, ImageMime.png));
        } finally {
            release.countDown();
            worker.shutdownNow();
            cache.close();
        }
    }
}
