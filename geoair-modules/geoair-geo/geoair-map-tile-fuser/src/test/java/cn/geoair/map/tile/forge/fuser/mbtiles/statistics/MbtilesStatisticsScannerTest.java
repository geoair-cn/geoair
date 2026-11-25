package cn.geoair.map.tile.forge.fuser.mbtiles.statistics;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

public class MbtilesStatisticsScannerTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Path root;

    @Before
    public void configureDirectory() {
        root = temporary.getRoot().toPath();
    }

    @Test
    public void rejectsViewAndShadowRowIdInsteadOfReportingIncompleteScanAsComplete()
            throws Exception {
        Path source = database(2);
        MbtilesStatisticsScanner dao = new MbtilesStatisticsScanner();
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + source);
                Statement s = c.createStatement()) {
            s.execute("ALTER TABLE tiles RENAME TO physical_tiles");
            s.execute("CREATE VIEW tiles AS SELECT * FROM physical_tiles");
            assertThrows(
                    java.sql.SQLException.class,
                    () -> dao.scan(source, root.resolve("view-stats.sqlite"), null));
            s.execute("DROP VIEW tiles");
            s.execute("ALTER TABLE physical_tiles RENAME TO tiles");
            s.execute("ALTER TABLE tiles ADD COLUMN rowid INTEGER");
            assertThrows(
                    java.sql.SQLException.class,
                    () -> dao.scan(source, root.resolve("shadow-stats.sqlite"), null));
        }
    }

    private Path database(int count) throws Exception {
        Path source = root.resolve("source.mbtiles");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + source)) {
            try (Statement s = connection.createStatement()) {
                s.execute(
                        "CREATE TABLE tiles(zoom_level INTEGER,tile_column INTEGER,tile_row INTEGER,tile_data BLOB,PRIMARY KEY(zoom_level,tile_column,tile_row))");
            }
            connection.setAutoCommit(false);
            try (PreparedStatement insert =
                    connection.prepareStatement("INSERT INTO tiles VALUES(?,?,?,?)")) {
                for (int i = 0; i < count; i++) {
                    insert.setInt(1, i % 2 + 1);
                    insert.setInt(2, i);
                    insert.setInt(3, i + 1);
                    insert.setBytes(4, new byte[] {1, 2, 3});
                    insert.addBatch();
                }
                insert.executeBatch();
                connection.commit();
            }
        }
        return source;
    }

    @Test
    public void persistsProgressAndDetectsLaterChange() throws Exception {
        Path source = database(10002), sidecar = root.resolve("stats.sqlite");
        MbtilesStatisticsScanner dao = new MbtilesStatisticsScanner();
        AtomicBoolean reported = new AtomicBoolean();
        AtomicBoolean overviewFirst = new AtomicBoolean();
        MbtilesScanProgress result =
                dao.scan(
                        source,
                        sidecar,
                        p -> {
                            if (p.getScannedCount() >= 10000) reported.set(true);
                            if (p.getCoordinateOverview() != null && p.getScannedCount() == 0) {
                                overviewFirst.set(true);
                                assertTrue(
                                        p.getCoordinateOverview().stream()
                                                .allMatch(level -> level.getSize() == -1));
                            }
                        });
        assertTrue("精确字节扫描前应发布坐标索引结果", overviewFirst.get());
        assertEquals(5001, result.getCoordinateOverview().get(0).getTileCount());
        assertEquals(10000, result.getCoordinateOverview().get(0).getMaxX().intValue());
        assertEquals("COMPLETED", result.getState());
        assertEquals(10002, result.getScannedCount());
        assertTrue(reported.get());
        assertEquals(2, result.getLevels().size());
        assertEquals(5001, result.getLevels().get(0).getTileCount());
        assertEquals(15003, result.getLevels().get(0).getSize());
        assertEquals("COMPLETED", dao.read(source, sidecar).getState());
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + source);
                Statement s = c.createStatement()) {
            s.execute("DELETE FROM tiles WHERE tile_column=1");
        }
        assertEquals("STALE", dao.read(source, sidecar).getState());
    }

    @Test
    public void skipsOverviewWithoutCompatibleIndexAndUsesCoveringIndexWhenPresent()
            throws Exception {
        Path source = database(5);
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + source);
                Statement s = c.createStatement()) {
            try (java.sql.ResultSet plan =
                    s.executeQuery(
                            "EXPLAIN QUERY PLAN SELECT zoom_level,tile_column,tile_row FROM tiles INDEXED BY sqlite_autoindex_tiles_1 WHERE (zoom_level,tile_column,tile_row)>(1,0,0) ORDER BY zoom_level,tile_column,tile_row LIMIT 10000")) {
                assertTrue(plan.next());
                assertTrue(plan.getString("detail").contains("COVERING INDEX"));
            }
            s.execute("DROP TABLE tiles");
            s.execute(
                    "CREATE TABLE tiles(zoom_level INTEGER,tile_column INTEGER,tile_row INTEGER,tile_data BLOB)");
            assertNull(new MbtilesStatisticsScanner().coordinateOverview(c, null));
        }
    }

    @Test
    public void concurrentWritesDoNotReturnCompleted() throws Exception {
        Path source = database(11000), sidecar = root.resolve("stats.sqlite");
        MbtilesStatisticsScanner dao = new MbtilesStatisticsScanner();
        AtomicBoolean changed = new AtomicBoolean();
        MbtilesScanProgress result =
                dao.scan(
                        source,
                        sidecar,
                        p -> {
                            if (p.getScannedCount() == 10000
                                    && changed.compareAndSet(false, true)) {
                                try (Connection c =
                                                DriverManager.getConnection(
                                                        "jdbc:sqlite:" + source);
                                        Statement s = c.createStatement()) {
                                    s.execute("DELETE FROM tiles WHERE tile_column=1");
                                } catch (Exception e) {
                                    throw new IllegalStateException(e);
                                }
                            }
                        });
        assertTrue(changed.get());
        assertEquals("STALE", result.getState());
        assertEquals("STALE", dao.read(source, sidecar).getState());
    }

    @Test
    public void stopSavesInterruptedAndCanBeRead() throws Exception {
        Path source = database(1), sidecar = root.resolve("stats.sqlite");
        MbtilesStatisticsScanner dao = new MbtilesStatisticsScanner();
        try {
            Thread.currentThread().interrupt();
            assertEquals("INTERRUPTED", dao.scan(source, sidecar, null).getState());
        } finally {
            Thread.interrupted();
        }
        assertEquals("INTERRUPTED", dao.read(source, sidecar).getState());
    }

    @Test
    public void rejectsSourceAsSidecarAndCountsLowestRowid() throws Exception {
        Path source = database(0), sidecar = root.resolve("stats.sqlite");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + source);
                PreparedStatement insert =
                        c.prepareStatement(
                                "INSERT INTO tiles(rowid,zoom_level,tile_column,tile_row,tile_data) VALUES(?,?,?,?,?)")) {
            insert.setLong(1, Long.MIN_VALUE);
            insert.setInt(2, 1);
            insert.setInt(3, 1);
            insert.setInt(4, 1);
            insert.setBytes(5, new byte[] {1});
            insert.executeUpdate();
        }
        MbtilesStatisticsScanner dao = new MbtilesStatisticsScanner();
        assertThrows(java.io.IOException.class, () -> dao.scan(source, source, null));
        MbtilesScanProgress result = dao.scan(source, sidecar, null);
        assertEquals("COMPLETED", result.getState());
        assertEquals(1, result.getScannedCount());
    }

    @Test
    public void readOnlyScanDoesNotResumeAnInstalledIncrementalIndex() throws Exception {
        Path source = root.resolve("installed.sqlite");
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + source.toUri());
                Statement sql = db.createStatement()) {
            sql.execute(
                    "CREATE TABLE tiles(zoom_level INTEGER NOT NULL,tile_column INTEGER NOT NULL,tile_row INTEGER NOT NULL,tile_data BLOB NOT NULL,PRIMARY KEY(zoom_level,tile_column,tile_row))");
            sql.execute("INSERT INTO tiles VALUES(1,0,0,X'01')");
            new TransactionalMbtilesStatistics().install(db);
        }
        assertEquals(
                MbtilesScanState.COMPLETED,
                new MbtilesStatisticsScanner().scan(source, null, null).getScanState());
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + source.toUri());
                Statement sql = db.createStatement();
                ResultSet state =
                        sql.executeQuery("SELECT started,scanned,done FROM tf_stats_state")) {
            state.next();
            assertEquals(0, state.getInt(1));
            assertEquals(0, state.getInt(2));
            assertEquals(0, state.getInt(3));
        }
    }
}
