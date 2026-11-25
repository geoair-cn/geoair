package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.fuser.encoding.TileImageCodec;
import cn.geoair.map.tile.forge.fuser.support.RasterTestImages;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MbtilesInPlaceTranscoderTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private final MbtilesMaintenanceDao dao = new MbtilesMaintenanceDao();

    private Path database(String name) throws Exception {
        Path file = folder.getRoot().toPath().resolve(name);
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                Statement sql = db.createStatement()) {
            sql.execute("PRAGMA journal_mode=WAL");
            sql.execute(
                    "CREATE TABLE tiles(zoom_level INTEGER,tile_column INTEGER,tile_row INTEGER,tile_data BLOB,PRIMARY KEY(zoom_level,tile_column,tile_row))");
            sql.execute("CREATE TABLE metadata(name TEXT PRIMARY KEY,value TEXT)");
            sql.execute("INSERT INTO metadata VALUES('format','png')");
        }
        return file;
    }

    private void put(Path file, int x, byte[] bytes) throws Exception {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri());
                PreparedStatement sql = db.prepareStatement("INSERT INTO tiles VALUES(3,?,0,?)")) {
            sql.setInt(1, x);
            sql.setBytes(2, bytes);
            sql.executeUpdate();
        }
    }

    private MbtilesTranscodeOptions options(String id) {
        MbtilesTranscodeOptions options = new MbtilesTranscodeOptions();
        options.setCheckpointId(id);
        options.setOnlyIfSmaller(false);
        return options;
    }

    @Test
    public void mixedFormatsAndBadTileDoNotAbortOthersOrChangeFileName() throws Exception {
        Path file = database("中文 缓存 #1.mbtiles");
        byte[] png = RasterTestImages.image("png", false),
                jpeg = RasterTestImages.image("jpeg", false),
                alpha = RasterTestImages.image("png", true);
        put(file, 0, png);
        put(file, 1, jpeg);
        put(file, 2, alpha);
        put(file, 3, new byte[] {1, 2, 3});
        MbtilesTranscodeProgress result =
                new MbtilesInPlaceTranscoder()
                        .execute(file, options("test"), null, () -> false, null);
        assertTrue(result.isComplete());
        assertEquals(4, result.getProcessed());
        assertEquals(1, result.getConverted());
        assertEquals(1, result.getFailed());
        assertEquals(1, result.getTransparent());
        assertEquals(2, result.getSkipped());
        assertEquals(
                "jpeg",
                TileImageCodec.detect(
                        dao.readTile(file, new MbtilesTileKey(3, 0, 0, 0), () -> false)));
        assertArrayEquals(jpeg, dao.readTile(file, new MbtilesTileKey(3, 1, 0, 0), () -> false));
        assertArrayEquals(alpha, dao.readTile(file, new MbtilesTileKey(3, 2, 0, 0), () -> false));
        assertTrue(Files.exists(file));
        assertEquals(4, result.getLevels().get(3).getProcessed());
    }

    @Test
    public void newEngineResumesAfterStopAndVacuumWithoutRowIdOrDoubleLossyEncoding()
            throws Exception {
        Path file = database("resume.mbtiles");
        for (int x = 0; x < 3; x++) put(file, x, RasterTestImages.image("png", false));
        AtomicBoolean stopped = new AtomicBoolean();
        MbtilesTranscodeOptions options = options("resume");
        options.setExpectedFileIdentity(MbtilesInPlaceTranscoder.fileIdentity(file));
        assertThrows(
                InterruptedException.class,
                () ->
                        new MbtilesInPlaceTranscoder()
                                .execute(
                                        file,
                                        options,
                                        null,
                                        stopped::get,
                                        progress -> {
                                            if (progress.getProcessed() == 1) stopped.set(true);
                                        }));
        byte[] first = dao.readTile(file, new MbtilesTileKey(3, 0, 0, 0), () -> false);
        assertEquals("jpeg", TileImageCodec.detect(first));
        assertEquals(
                MbtilesMaintenanceStatus.COMPLETED,
                new MbtilesMaintenance()
                        .reclaim(file, MbtilesReclaimMode.VACUUM, () -> false, null)
                        .getStatus());
        stopped.set(false);
        MbtilesTranscodeProgress result =
                new MbtilesInPlaceTranscoder().execute(file, options, null, stopped::get, null);
        assertEquals(3, result.getConverted());
        assertEquals(3, result.getProcessed());
        assertArrayEquals(first, dao.readTile(file, new MbtilesTileKey(3, 0, 0, 0), () -> false));
        assertEquals(
                3,
                new MbtilesInPlaceTranscoder()
                        .execute(file, options, null, () -> false, null)
                        .getProcessed());
        options.getEncoding().setQuality(20);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MbtilesInPlaceTranscoder()
                                .execute(file, options, null, () -> false, null));
    }

    @Test
    public void rangeFilterNeedsAnIdentityAndDoesNotExpandItsScope() throws Exception {
        Path file = database("filter.mbtiles");
        put(file, 0, RasterTestImages.image("png", false));
        put(file, 1, RasterTestImages.image("png", false));
        MbtilesTranscodeOptions options = options("region");
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MbtilesInPlaceTranscoder()
                                .execute(file, options, key -> key.getX() == 0, () -> false, null));
        options.setScopeId("region-v1");
        MbtilesTranscodeProgress result =
                new MbtilesInPlaceTranscoder()
                        .execute(file, options, key -> key.getX() == 0, () -> false, null);
        assertEquals(1, result.getConverted());
        assertEquals(1, result.getSkipped());
        assertEquals(
                "png",
                TileImageCodec.detect(
                        dao.readTile(file, new MbtilesTileKey(3, 1, 0, 0), () -> false)));
    }

    @Test
    public void staleFileIdentityIsRejectedEvenIfNewFileHasNoOldCheckpoint() throws Exception {
        Path first = database("first.mbtiles"), second = database("second.mbtiles");
        MbtilesTranscodeOptions options = options("stale");
        options.setExpectedFileIdentity(MbtilesInPlaceTranscoder.fileIdentity(first));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new MbtilesInPlaceTranscoder()
                                .execute(second, options, null, () -> false, null));
        assertNull(dao.readCheckpoint(second, "stale"));
    }

    @Test
    public void concurrentChangedTileIsNotOverwrittenAndConflictIsCounted() throws Exception {
        Path file = database("conflict.mbtiles");
        put(file, 0, RasterTestImages.image("png", false));
        byte[] changed = RasterTestImages.image("png", true);
        AtomicBoolean intervened = new AtomicBoolean();
        MbtilesMaintenanceDao competing =
                new MbtilesMaintenanceDao() {
                    @Override
                    public void commit(
                            Path path,
                            MbtilesTileKey key,
                            byte[] expected,
                            byte[] replacement,
                            String id,
                            String checkpoint,
                            BooleanSupplier cancelled)
                            throws Exception {
                        if (replacement != null && intervened.compareAndSet(false, true))
                            try (Connection db =
                                            DriverManager.getConnection(
                                                    "jdbc:sqlite:" + file.toUri());
                                    PreparedStatement sql =
                                            db.prepareStatement("UPDATE tiles SET tile_data=?")) {
                                sql.setBytes(1, changed);
                                sql.executeUpdate();
                            }
                        super.commit(path, key, expected, replacement, id, checkpoint, cancelled);
                    }
                };
        MbtilesTranscodeProgress result =
                new MbtilesInPlaceTranscoder(competing)
                        .execute(file, options("conflict"), null, () -> false, null);
        assertEquals(1, result.getConflicts());
        assertEquals(0, result.getConverted());
        assertEquals(1, result.getSkipped());
        assertArrayEquals(changed, dao.readTile(file, new MbtilesTileKey(3, 0, 0, 0), () -> false));
    }
}
