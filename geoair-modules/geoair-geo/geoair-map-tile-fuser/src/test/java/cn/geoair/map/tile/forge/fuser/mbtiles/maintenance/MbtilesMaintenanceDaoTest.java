package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesAccessCoordinator;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesAccessLease;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/** 对真实SQLite验证CAS、事务恢复点、WAL和完整回收，不使用模拟SQL成功。 */
public class MbtilesMaintenanceDaoTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private Path directory;

    @Before
    public void prepare() {
        directory = folder.getRoot().toPath();
    }

    private final MbtilesMaintenanceDao dao = new MbtilesMaintenanceDao();

    private Path database(String name) throws Exception {
        Path file = Files.createDirectories(directory.resolve("中文 缓存 #1")).resolve(name);
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                Statement sql = db.createStatement()) {
            sql.execute("PRAGMA journal_mode=WAL");
            sql.execute(
                    "CREATE TABLE tiles(zoom_level INTEGER,tile_column INTEGER,tile_row INTEGER,tile_data BLOB,PRIMARY KEY(zoom_level,tile_column,tile_row))");
            sql.execute("CREATE TABLE metadata(name TEXT PRIMARY KEY,value TEXT)");
            sql.execute("INSERT INTO metadata VALUES('format','png')");
            sql.execute("INSERT INTO tiles VALUES(3,2,1,x'0102')");
        }
        return file;
    }

    @Test
    public void commitsImageAndCursorAtomicallyAndDoesNotOverwriteConcurrentChange()
            throws Exception {
        Path file = database("cache.mbtiles");
        MbtilesTileKey key = new MbtilesTileKey(3, 2, 1, 2);
        dao.validateTranscode(file);
        dao.commit(
                file,
                key,
                new byte[] {1, 2},
                new byte[] {3, 4},
                "cache_job_1",
                "cursor1",
                () -> false);
        assertArrayEquals(new byte[] {3, 4}, dao.readTile(file, key, () -> false));
        assertEquals("cursor1", dao.readCheckpoint(file, "cache_job_1"));
        assertThrows(
                MbtilesTileConflictException.class,
                () ->
                        dao.commit(
                                file,
                                key,
                                new byte[] {1, 2},
                                new byte[] {5},
                                "cache_job_1",
                                "cursor2",
                                () -> false));
        assertArrayEquals(new byte[] {3, 4}, dao.readTile(file, key, () -> false));
        assertEquals("cursor1", dao.readCheckpoint(file, "cache_job_1"));
        assertThrows(
                InterruptedException.class,
                () ->
                        dao.commit(
                                file,
                                key,
                                new byte[] {3, 4},
                                new byte[] {6},
                                "cache_job_1",
                                "cursor3",
                                () -> true));
        assertEquals("cursor1", dao.readCheckpoint(file, "cache_job_1"));
    }

    @Test
    public void vacuumRequiresExclusiveLeaseAndActuallyReclaimsFreedPages() throws Exception {
        Path file = database("vacuum.mbtiles");
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                Statement sql = db.createStatement()) {
            sql.execute("CREATE TABLE filler(data BLOB)");
            for (int i = 0; i < 20; i++) sql.execute("INSERT INTO filler VALUES(zeroblob(100000))");
            sql.execute("DELETE FROM filler");
            sql.execute("PRAGMA wal_checkpoint(TRUNCATE)");
        }
        MbtilesFileInfo before = dao.inspect(file);
        assertTrue(before.getFreePages() > 0);
        assertThrows(IllegalStateException.class, () -> dao.vacuum(file, () -> false));
        try (MbtilesAccessLease ignored = MbtilesAccessCoordinator.maintenance(file, () -> false)) {
            dao.vacuum(file, () -> false);
            int[] checkpoint = dao.checkpoint(file, MbtilesCheckpointMode.TRUNCATE, () -> false);
            assertEquals(0, checkpoint[0]);
        }
        MbtilesFileInfo after = dao.inspect(file);
        assertEquals(0, after.getFreePages());
        assertTrue(after.getMainBytes() < before.getMainBytes());
        assertArrayEquals(
                new byte[] {1, 2}, dao.readTile(file, new MbtilesTileKey(3, 2, 1, 2), () -> false));
    }

    @Test
    public void passiveCheckpointReportsBlockedReadersInsteadOfPretendingComplete()
            throws Exception {
        Path file = database("wal.mbtiles");
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
            int[] checkpoint = dao.checkpoint(file, MbtilesCheckpointMode.PASSIVE, () -> false);
            assertTrue(checkpoint[1] > checkpoint[2]);
            read.execute("ROLLBACK");
            checkpoint = dao.checkpoint(file, MbtilesCheckpointMode.TRUNCATE, () -> false);
            assertEquals(0, checkpoint[0]);
            assertEquals(0, Files.size(file.resolveSibling(file.getFileName() + "-wal")));
        }
    }

    @Test
    public void incrementalConsumesAllRequestedPageStepsInOneBatch() throws Exception {
        Path file = directory.resolve("incremental.mbtiles");
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                Statement sql = db.createStatement()) {
            sql.execute("PRAGMA auto_vacuum=INCREMENTAL");
            sql.execute("CREATE TABLE filler(data BLOB)");
            sql.execute("INSERT INTO filler VALUES(zeroblob(1000000))");
            sql.execute("DELETE FROM filler");
        }
        long before = dao.inspect(file).getFreePages();
        assertTrue(before > 128);
        dao.incremental(file, 128, () -> false);
        assertTrue("每批必须释放所请求的页数，不能退化为一次只回收一页", before - dao.inspect(file).getFreePages() >= 128);
    }

    @Test
    public void cancellationBeforeCommitRollsBackBothImageAndRecoveryPoint() throws Exception {
        Path file = database("rollback.mbtiles");
        AtomicInteger checks = new AtomicInteger();
        assertThrows(
                InterruptedException.class,
                () ->
                        dao.commit(
                                file,
                                new MbtilesTileKey(3, 2, 1, 2),
                                new byte[] {1, 2},
                                new byte[] {3, 4},
                                "cancel",
                                "never-committed",
                                () -> checks.incrementAndGet() >= 2));
        assertArrayEquals(
                new byte[] {1, 2}, dao.readTile(file, new MbtilesTileKey(3, 2, 1, 2), () -> false));
        assertNull(dao.readCheckpoint(file, "cancel"));
    }
}
