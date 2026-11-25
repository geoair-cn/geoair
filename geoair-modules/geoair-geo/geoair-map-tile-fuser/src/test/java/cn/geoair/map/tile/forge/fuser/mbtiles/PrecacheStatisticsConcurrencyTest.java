package cn.geoair.map.tile.forge.fuser.mbtiles;

import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.fuser.cache.MBTilesTileCache;
import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;
import cn.geoair.map.tile.forge.fuser.precache.*;
import cn.geoair.map.tile.forge.fuser.mbtiles.statistics.*;
import cn.geoair.web.mime.GiMimeType;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.file.Path;
import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 迁移应用并发验收，用库的正常缓存与分段入口验证统计时下载仍推进。 */
public class PrecacheStatisticsConcurrencyTest {
    @Rule public TemporaryFolder folder=new TemporaryFolder();

    @Test(timeout=30000) public void incrementalStatisticsYieldsToRealPrecacheWrites() throws Exception { run(false); }
    @Test(timeout=30000) public void readOnlyStatisticsDoesNotInstallSourceTablesWhileDownloading() throws Exception { run(true); }

    private void run(boolean readOnly) throws Exception {
        Path root=folder.newFolder().toPath();
        String layer="source_original_grid";
        Path file=root.resolve(MbtilesUtils.getSafeFileName(layer)+".mbtiles");
        Path source=root.resolve("upstream.sqlite");
        AtomicBoolean scanning=new AtomicBoolean();
        AtomicInteger writes=new AtomicInteger();
        AtomicInteger batchesAfterWrite=new AtomicInteger();
        MBTilesTileCache cache=new MBTilesTileCache(root.toString()) {
            @Override public boolean put(String name,int z,int x,int y,byte[] data,GiMimeType format) {
                boolean result=super.put(name,z,x,y,data,format);
                if(result && scanning.get()) writes.incrementAndGet();
                return result;
            }
        };
        ExecutorService statistics=Executors.newSingleThreadExecutor();
        ExecutorService downloads=Executors.newFixedThreadPool(4);
        CountDownLatch firstBatch=new CountDownLatch(1);
        CountDownLatch submitted=new CountDownLatch(1);
        MbtilesSourceReader reader=new MbtilesSourceReader();
        try {
            try(Connection db=open(source);Statement sql=db.createStatement()) {
                sql.execute("CREATE TABLE tiles(zoom_level INTEGER,tile_column INTEGER,tile_row INTEGER,tile_data BLOB,PRIMARY KEY(zoom_level,tile_column,tile_row))");
                sql.execute("WITH RECURSIVE seq(x) AS (SELECT 0 UNION ALL SELECT x+1 FROM seq WHERE x<1023) "
                        + "INSERT INTO tiles SELECT 5,x/32,x%32,X'01' FROM seq");
            }
            assertArrayEquals(new byte[]{1},reader.readTile(source.toString(),5,0,0));
            assertTrue(cache.put(layer,17,0,0,new byte[]{1},ImageMime.png));
            try(Connection db=open(file);Statement sql=db.createStatement()) {
                sql.execute("WITH RECURSIVE seq(x) AS (SELECT 0 UNION ALL SELECT x+1 FROM seq WHERE x<19999) "
                        + "INSERT OR REPLACE INTO tiles SELECT 17,x,0,X'01' FROM seq");
            }
            Future<?> scan=statistics.submit(()->{
                scanning.set(true);
                try {
                    if(readOnly) new MbtilesStatisticsScanner().scan(file,root.resolve("stats.sqlite"),progress->{
                        firstBatch.countDown();await(submitted);
                    });
                    else new MbtilesIncrementalStatistics().scan(file,progress->{
                        firstBatch.countDown();await(submitted);
                        if(writes.get()>0) batchesAfterWrite.incrementAndGet();
                    });
                } catch(Exception e) { throw new IllegalStateException(e); }
                finally { scanning.set(false); }
            });
            assertTrue(firstBatch.await(5,TimeUnit.SECONDS));
            submitted.countDown();
            List<TilePrecacheProgress> result=SegmentedTilePrecache.execute(layer,
                    Collections.singletonList(new TilePrecacheLevel(5,new RangeApo(0,31,0,31,5),null)),ImageMime.png,
                    cache,(z,x,y,context)->reader.readTile(source.toString(),z,x,y),downloads,new TileCancellationToken(),new TilePrecacheOptions(),null);
            assertEquals(Long.valueOf(1024),result.get(0).getCounts().get(TileOperationStatus.DOWNLOADED));
            scan.get(20,TimeUnit.SECONDS);
            assertTrue("统计执行期间应有真实缓存写入",writes.get()>0);
            try(Connection db=open(file);Statement sql=db.createStatement();ResultSet rows=sql.executeQuery("SELECT COUNT(*),SUM(LENGTH(tile_data)) FROM tiles")) {
                rows.next();assertEquals(21024,rows.getLong(1));
                if(readOnly) {
                    try(Statement schema=db.createStatement();ResultSet objects=schema.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name GLOB 'tf_stats_*'")) {
                        objects.next();assertEquals(0,objects.getInt(1));
                    }
                } else {
                    assertTrue(batchesAfterWrite.get()>0);
                    TransactionalStatsSnapshot current=new MbtilesIncrementalStatistics().read(file);
                    assertTrue(current.isComplete());
                    assertEquals(rows.getLong(1),current.getLevels().stream().mapToLong(TransactionalStatsLevel::getCount).sum());
                    assertEquals(rows.getLong(2),current.getLevels().stream().mapToLong(TransactionalStatsLevel::getBytes).sum());
                }
            }
        } finally {
            submitted.countDown();statistics.shutdownNow();downloads.shutdownNow();
            assertTrue(statistics.awaitTermination(5,TimeUnit.SECONDS));
            assertTrue(downloads.awaitTermination(5,TimeUnit.SECONDS));
            reader.close();
            cache.close();
        }
    }
    private Connection open(Path file) throws SQLException { return DriverManager.getConnection("jdbc:sqlite:"+file.toUri()); }
    private void await(CountDownLatch latch) {
        try { if(!latch.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("测试协调超时"); }
        catch(InterruptedException e) { Thread.currentThread().interrupt();throw new IllegalStateException(e); }
    }
}
