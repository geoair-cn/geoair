package cn.geoair.map.tile.forge.fuser.mbtiles.statistics;

import org.junit.Test;
import org.junit.Rule;
import org.junit.Before;
import org.junit.rules.TemporaryFolder;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesUtils;
import cn.geoair.map.tile.forge.fuser.cache.MBTilesTileCache;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import static org.junit.Assert.*;

/** 临时 SQLite 验证冲突动作、事务回滚和重启，不访问实际缓存。 */
public class TransactionalMbtilesStatisticsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Path directory;
    @Before public void configureDirectory() { directory = temporary.getRoot().toPath(); }
    private static final String SCHEMA="CREATE TABLE tiles(zoom_level INTEGER NOT NULL,tile_column INTEGER NOT NULL,tile_row INTEGER NOT NULL,tile_data BLOB NOT NULL,PRIMARY KEY(zoom_level,tile_column,tile_row))";
    private Connection open(Path path,int recursive) throws Exception {
        Connection c=DriverManager.getConnection("jdbc:sqlite:"+path);
        try(Statement s=c.createStatement()){s.execute("PRAGMA recursive_triggers="+recursive);}
        return c;
    }
    private void sql(Connection c,String sql) throws Exception {try(Statement s=c.createStatement()){s.execute(sql);}}

    @Test public void yieldsAtARowBoundaryAndPreservesThePrefix() throws Exception {
        Path file = directory.resolve("yield.sqlite");
        TransactionalMbtilesStatistics stats = new TransactionalMbtilesStatistics();
        try (Connection c = open(file, 0)) {
            sql(c, SCHEMA);
            sql(c, "INSERT INTO tiles VALUES(1,0,0,X'01'),(1,1,0,X'0203'),(1,2,0,X'040506')");
            stats.install(c);
            TransactionalStatsSnapshot first = stats.scanBatch(c, 100, TimeUnit.SECONDS.toNanos(1), () -> true);
            assertEquals(1, first.getScanned());
            assertFalse(first.isComplete());
            assertMatching(c, stats);
            sql(c, "INSERT OR REPLACE INTO tiles VALUES(1,0,0,X'FF'),(1,3,0,X'FFFF')");
            while (!stats.scanBatch(c, 100, 1, () -> false).isComplete()) assertMatching(c, stats);
            assertMatching(c, stats);
        }
    }

    @Test public void boundsRecalculationYieldsWithoutPublishingPartialBounds() throws Exception {
        Path file = directory.resolve("bounds-yield.sqlite");
        TransactionalMbtilesStatistics stats = new TransactionalMbtilesStatistics();
        try (Connection c = open(file, 0)) {
            sql(c, SCHEMA);
            sql(c, "WITH RECURSIVE seq(x) AS (SELECT 0 UNION ALL SELECT x+1 FROM seq WHERE x<1999)"
                    + " INSERT INTO tiles SELECT 1,x,0,X'01' FROM seq");
            stats.install(c);
            while (!stats.scanBatch(c, 10000).isComplete()) { }
            sql(c, "DELETE FROM tiles WHERE tile_column=0");
            assertTrue(stats.read(c).getLevels().get(0).isBoundsDirty());
            assertThrows(SQLException.class, () -> stats.recalculateBounds(c, 1, () -> true));
            assertTrue(stats.read(c).getLevels().get(0).isBoundsDirty());
            stats.recalculateBounds(c, 1, () -> false);
            assertFalse(stats.read(c).getLevels().get(0).isBoundsDirty());
            assertMatching(c, stats);
        }
    }
    private void assertMatching(Connection c,TransactionalMbtilesStatistics stats) throws Exception {
        TransactionalStatsSnapshot snapshot=stats.read(c);
        Map<Integer,String> expected=new TreeMap<>(), bounds=new TreeMap<>();
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT t.zoom_level,COUNT(*),SUM(LENGTH(t.tile_data)),MIN(tile_column),MAX(tile_column),MIN(tile_row),MAX(tile_row) FROM tiles t WHERE (SELECT done=1 OR (started=1 AND t.rowid<=cursor) FROM tf_stats_state WHERE id=1) GROUP BY t.zoom_level")) {
            while(r.next()) {
                expected.put(r.getInt(1),r.getLong(2)+":"+r.getLong(3));
                bounds.put(r.getInt(1),r.getInt(4)+":"+r.getInt(5)+":"+r.getInt(6)+":"+r.getInt(7));
            }
        }
        Map<Integer,String> actual=new TreeMap<>();
        for(TransactionalStatsLevel level:snapshot.getLevels()) {
            actual.put(level.getZoom(),level.getCount()+":"+level.getBytes());
            if(!level.isBoundsDirty()) assertEquals(bounds.get(level.getZoom()),level.getMinX()+":"+level.getMaxX()+":"+level.getMinY()+":"+level.getMaxY());
            else { assertNull(level.getMinX());assertNull(level.getMaxX()); }
        }
        assertEquals(expected,actual);
    }
    @Test public void tracksConflictsAndRestartForBothRecursiveSettings() throws Exception {
        for(int mode:new int[]{0,1}) {
            Path file=directory.resolve("mode"+mode+".sqlite");
            TransactionalMbtilesStatistics stats=new TransactionalMbtilesStatistics();
            try(Connection c=open(file,mode)) {
                sql(c,SCHEMA);stats.install(c);
                sql(c,"INSERT INTO tiles VALUES(1,1,1,X'0102'),(1,2,1,X'030405'),(2,3,1,X'06'),(2,4,1,X'07')");
                stats.scanBatch(c,2);assertMatching(c,stats);
                String[] mutations={
                    "INSERT OR IGNORE INTO tiles VALUES(1,1,1,X'AAAA')",
                    "INSERT OR REPLACE INTO tiles VALUES(1,1,1,X'AAAAAA')",
                    "INSERT OR REPLACE INTO tiles VALUES(2,3,1,X'BBBB')",
                    "INSERT INTO tiles VALUES(1,2,1,X'11111111') ON CONFLICT(zoom_level,tile_column,tile_row) DO UPDATE SET tile_data=excluded.tile_data",
                    "UPDATE OR REPLACE tiles SET zoom_level=2,tile_column=4,tile_data=X'AA' WHERE zoom_level=1 AND tile_column=2",
                    "DELETE FROM tiles WHERE zoom_level=2 AND tile_column=4"};
                for(String mutation:mutations){sql(c,mutation);assertMatching(c,stats);}
                sql(c,"BEGIN");sql(c,"INSERT INTO tiles VALUES(3,9,9,X'AAAA')");sql(c,"ROLLBACK");
                assertMatching(c,stats);
            }
            try(Connection c=open(file,mode)) {
                assertMatching(c,stats);
                while(!stats.scanBatch(c,2).isComplete()) assertMatching(c,stats);
                assertMatching(c,stats);
                sql(c,"INSERT OR REPLACE INTO tiles VALUES(1,1,1,X'01020304')");assertMatching(c,stats);
                stats.recalculateBounds(c,1);
                TransactionalStatsLevel level=stats.read(c).getLevels().stream().filter(l->l.getZoom()==1).findFirst().get();
                assertFalse(level.isBoundsDirty());assertEquals(Integer.valueOf(1),level.getMinX());
                sql(c,"DELETE FROM tiles WHERE zoom_level=1 AND tile_column=1");assertMatching(c,stats);
                sql(c,"INSERT INTO tiles VALUES(3,10,1,X'01'),(3,11,1,X'0203'),(3,12,1,X'030405')");
                sql(c,"UPDATE OR REPLACE tiles SET rowid=(SELECT rowid FROM tiles WHERE tile_column=11),tile_column=12 WHERE tile_column=10");
                assertMatching(c,stats);
                sql(c,"INSERT INTO tiles VALUES(4,20,1,X'01'),(4,21,1,X'0203')");
                sql(c,"INSERT OR REPLACE INTO tiles(rowid,zoom_level,tile_column,tile_row,tile_data) SELECT rowid,4,21,1,X'FF' FROM tiles WHERE zoom_level=4 AND tile_column=20");
                assertMatching(c,stats);
            }
        }
    }
    @Test public void rejectsUnsupportedSchemaAndAlteredTriggers() throws Exception {
        Path file=directory.resolve("bad.sqlite");
        TransactionalMbtilesStatistics stats=new TransactionalMbtilesStatistics();
        try(Connection c=open(file,0)) {
            sql(c,SCHEMA);sql(c,"CREATE UNIQUE INDEX other_unique ON tiles(tile_column,tile_row)");
            assertThrows(SQLException.class,()->stats.install(c));
            sql(c,"DROP INDEX other_unique");stats.install(c);
            sql(c,"DROP TRIGGER tf_stats_ai");
            assertThrows(SQLException.class,()->stats.read(c));
        }
    }
    @Test public void preservesNegativeRowidWhenAutoInsertDoesNotConflict() throws Exception {
        Path file=directory.resolve("negative.sqlite");
        TransactionalMbtilesStatistics stats=new TransactionalMbtilesStatistics();
        try(Connection c=open(file,0)) {
            sql(c,SCHEMA);sql(c,"INSERT INTO tiles(rowid,zoom_level,tile_column,tile_row,tile_data) VALUES(-1,1,1,1,X'01')");
            stats.install(c);
            while(!stats.scanBatch(c,10).isComplete()) { }
            sql(c,"INSERT INTO tiles VALUES(1,2,1,X'0102')");
            assertMatching(c,stats);
        }
    }
    @Test public void randomizedMutationsMatchSourceDuringAndAfterScan() throws Exception {
        for(int mode:new int[]{0,1}) {
            Path file=directory.resolve("random"+mode+".sqlite");
            TransactionalMbtilesStatistics stats=new TransactionalMbtilesStatistics();
            try(Connection c=open(file,mode)) {
                sql(c,MbtilesUtils.CREATE_TILES_TABLE_SQL);
                sql(c,MbtilesUtils.CREATE_TILES_INDEX_SQL);
                Random random=new Random(4189);
                for(int i=0;i<200;i++) sql(c,"INSERT OR REPLACE INTO tiles VALUES("+(i%4)+","+i+",1,X'0102')");
                stats.install(c);
                for(int i=0;i<650;i++) {
                    int z=random.nextInt(4), x=random.nextInt(240), op=random.nextInt(5);
                    if(op==0) sql(c,"DELETE FROM tiles WHERE zoom_level="+z+" AND tile_column="+x);
                    else if(op==1) sql(c,"INSERT OR IGNORE INTO tiles VALUES("+z+","+x+",1,X'010203')");
                    else if(op==2) sql(c,"INSERT OR REPLACE INTO tiles VALUES("+z+","+x+",1,X'01020304')");
                    else if(op==3) sql(c,"UPDATE OR REPLACE tiles SET tile_column="+random.nextInt(240)+",tile_data=X'FF' WHERE zoom_level="+z+" AND tile_column="+x);
                    else sql(c,"INSERT INTO tiles VALUES("+z+","+x+",1,X'0102') ON CONFLICT(zoom_level,tile_column,tile_row) DO UPDATE SET tile_data=excluded.tile_data");
                    if(i%11==0 && !stats.read(c).isComplete()) stats.scanBatch(c,7);
                    if(i%13==0) assertMatching(c,stats);
                }
                while(!stats.scanBatch(c,50).isComplete()) assertMatching(c,stats);
                assertMatching(c,stats);
            }
        }
    }
    @Test public void tracksActualPublishedR2CacheWriterAndDelete() throws Exception {
        Path file=directory.resolve("writer.mbtiles");
        TransactionalMbtilesStatistics stats=new TransactionalMbtilesStatistics();
        try(Connection c=open(file,0)) {
            sql(c,MbtilesUtils.CREATE_TILES_TABLE_SQL);sql(c,MbtilesUtils.CREATE_TILES_INDEX_SQL);
            MBTilesTileCache cache=new MBTilesTileCache(directory.toString(),true);
            try {
                assertTrue(cache.put("writer",3,1,2,new byte[]{1,2,3},ImageMime.png));
                stats.install(c);
                while(!stats.scanBatch(c,10).isComplete()) { }
                assertMatching(c,stats);
                assertTrue(cache.put("writer",3,1,2,new byte[]{1,2,3,4},ImageMime.png));
                assertMatching(c,stats);
                assertTrue(cache.delete("writer",3,1,2,ImageMime.png));
                assertMatching(c,stats);
            } finally {cache.close();}
        }
    }
    @Test public void scanningAndWritingDifferentConnectionsAreSerializedSafely() throws Exception {
        Path file=directory.resolve("concurrent.sqlite");
        TransactionalMbtilesStatistics stats=new TransactionalMbtilesStatistics();
        try(Connection c=open(file,0)) {
            sql(c,SCHEMA);sql(c,"PRAGMA journal_mode=WAL");sql(c,"PRAGMA busy_timeout=10000");
            assertFalse(stats.isInstalled(c));stats.install(c);assertTrue(stats.isInstalled(c));
            sql(c,"BEGIN");
            for(int i=0;i<500;i++) sql(c,"INSERT INTO tiles VALUES(1,"+i+",1,X'01')");
            sql(c,"COMMIT");
            ExecutorService executor=Executors.newSingleThreadExecutor();
            try {
                Future<?> writer=executor.submit(()->{
                    try(Connection w=open(file,1)) {
                        sql(w,"PRAGMA busy_timeout=10000");
                        for(int i=0;i<250;i++) sql(w,"INSERT OR REPLACE INTO tiles VALUES(1,"+(i%125)+",1,X'010203')");
                    } catch(Exception e) {throw new IllegalStateException(e);}
                });
                while(!stats.scanBatch(c,20).isComplete()) { }
                writer.get(30,TimeUnit.SECONDS);
                assertMatching(c,stats);
            } finally {executor.shutdownNow();}
            stats.uninstall(c);assertFalse(stats.isInstalled(c));
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM tiles")) {r.next();assertEquals(500,r.getInt(1));}
        }
    }
    @Test public void processCrashRollsBackTileAndStatisticsTogether() throws Exception {
        Path file=directory.resolve("crash.sqlite");
        TransactionalMbtilesStatistics stats=new TransactionalMbtilesStatistics();
        try(Connection c=open(file,0)) {
            sql(c,SCHEMA);stats.install(c);sql(c,"INSERT INTO tiles VALUES(1,1,1,X'01')");
            stats.scanBatch(c,1);assertMatching(c,stats);
        }
        String javaBinary=java.nio.file.Paths.get(System.getProperty("java.home"),"bin","java").toString();
        String classpath=System.getProperty("surefire.test.class.path",System.getProperty("java.class.path"));
        Process process=new ProcessBuilder(javaBinary,"-cp",classpath,CrashDuringStatsTransaction.class.getName(),file.toString())
                .redirectErrorStream(true).start();
        assertTrue(process.waitFor(20,TimeUnit.SECONDS));
        assertEquals(23,process.exitValue());
        try(Connection c=open(file,0)) {
            assertMatching(c,stats);
            assertFalse(stats.read(c).isComplete());
            while(!stats.scanBatch(c,10).isComplete()) { }
            assertMatching(c,stats);
        }
    }
}
