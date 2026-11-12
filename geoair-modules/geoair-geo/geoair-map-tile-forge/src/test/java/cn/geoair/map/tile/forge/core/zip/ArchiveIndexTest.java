package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.local.LocalZipXYZTileStorageSupport;
import cn.geoair.map.tile.forge.core.support.local.LocalZip3DTileStorageSupport;
import cn.geoair.map.tile.forge.core.zip.cache.*;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import java.util.zip.*;
import static org.junit.Assert.*;

/** 从应用迁移的真实归档测试，验证完整提交、重试与三维相对资源。 */
public class ArchiveIndexTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private Path root;
    private String previous;
    @Before public void setup() {
        root = folder.getRoot().toPath();
        previous = TileTempPathConfig.getInstance().getTempPath();
        TileTempPathConfig.getInstance().setTempPath(root.toString());
    }
    @After public void restore() { TileTempPathConfig.getInstance().setTempPath(previous); }

    private GirLayerConfigContext context(int entries) throws Exception {
        Path source = root.resolve("中文归档.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(source))) {
            for (int x=1; x<=entries; x++) {
                zip.putNextEntry(new ZipEntry("13/"+x+"/2.png")); zip.write(new byte[]{1,2,3,4}); zip.closeEntry();
            }
        }
        GirLayerConfigContext context = new GirLayerConfigContext();
        context.setDataId("archive"); context.setObjectKey(source.toString());
        context.setStorageType(GirStorageType.LOCAL_ZIP); context.setMapTileType(GirMapTileType.XYZ);
        return context;
    }
    private GirLayerConfigContextHelper helper() {
        return new GirLayerConfigContextHelper() {
            public Optional<GirLayerConfigContext> getByLayerName(String name) { return Optional.empty(); }
            public Optional<GirLayerConfigContext> getGirLayerConfigContext(GirMapTileType type, String name, String id, String file) { return Optional.empty(); }
            public LayerPerFileDao getLayerPerFileDao(GirLayerConfigContext context) { return new SQLiteLayerPerFileDao(context.getLayerName()); }
            public Long getLayerPerCacheBatchSize(GirLayerConfigContext context) { return 2000L; }
        };
    }
    private long count(SQLiteLayerPerFileDao dao) throws Exception {
        try (Connection db=DriverManager.getConnection("jdbc:sqlite:"+Paths.get(dao.getTableName()).toUri());
             Statement sql=db.createStatement(); ResultSet rows=sql.executeQuery("SELECT COUNT(*) FROM tile_central_directory")) {
            rows.next(); return rows.getLong(1);
        }
    }
    @Test public void commitsAllBatchesBeforeSuccessAndReadsOriginalOffsets() throws Exception {
        GirLayerConfigContext context=context(4501);
        LocalZipXYZTileStorageSupport support=new LocalZipXYZTileStorageSupport(helper());
        support.preCacheTiles(context, (all, done)->{});
        try(SQLiteLayerPerFileDao dao=new SQLiteLayerPerFileDao(context.getLayerName())) {
            assertEquals(4501,count(dao));
            assertEquals(SQLiteLayerPerFileDao.CacheStatus.CACHED,dao.getCacheStatus());
            assertArrayEquals(new byte[]{1,2,3,4},support.getICompressionHandler().readAndDecompressEntry(
                    dao.findByXyz("4501","2","13"),context.getObjectKey()));
        }
    }
    @Test public void propagatesFailureAndRetryDoesNotDuplicateCommittedPrefix() throws Exception {
        GirLayerConfigContext context=context(4501);
        AtomicInteger processed=new AtomicInteger();
        LocalZipXYZTileStorageSupport broken=new LocalZipXYZTileStorageSupport(helper()) {
            @Override public TileCentralDirectoryModel tranToTileModel(CentralDirectoryModel entry) {
                if(processed.incrementAndGet()==2100) throw new IllegalStateException("模拟解析失败");
                return super.tranToTileModel(entry);
            }
        };
        assertThrows(IllegalStateException.class,()->broken.preCacheTiles(context,(all,done)->{}));
        try(SQLiteLayerPerFileDao dao=new SQLiteLayerPerFileDao(context.getLayerName())) {
            assertEquals(2000,count(dao)); assertEquals(SQLiteLayerPerFileDao.CacheStatus.CACHE_FAILED,dao.getCacheStatus());
        }
        new LocalZipXYZTileStorageSupport(helper()).preCacheTiles(context,(all,done)->{});
        try(SQLiteLayerPerFileDao dao=new SQLiteLayerPerFileDao(context.getLayerName())) { assertEquals(4501,count(dao)); }
    }
    @Test public void batchRollbackKeepsEarlierRowsOutOfTheIndex() throws Exception {
        GirLayerConfigContext context=context(1);
        try(SQLiteLayerPerFileDao dao=new SQLiteLayerPerFileDao(context.getLayerName())) {
            dao.doPreCacheStart();
            try(Connection db=DriverManager.getConnection("jdbc:sqlite:"+Paths.get(dao.getTableName()).toUri()); Statement sql=db.createStatement()) {
                sql.execute("CREATE TRIGGER reject_bad BEFORE INSERT ON tile_central_directory WHEN NEW.name='bad' BEGIN SELECT RAISE(ABORT,'fail'); END");
            }
            TileCentralDirectoryModel good=model("good"),bad=model("bad");
            assertThrows(SQLException.class,()->dao.batchInsert(Arrays.asList(good,bad)));
            assertEquals(0,count(dao));
        }
    }
    private TileCentralDirectoryModel model(String name) {
        TileCentralDirectoryModel entry=new TileCentralDirectoryModel();
        entry.setLocalHeaderOffset(0L); entry.setCompressionMethod(0L); entry.setCompressedSize(4L);
        entry.setUncompressedSize(4L); entry.setEntrySize(46); entry.setName(name); return entry;
    }
    @Test public void concurrentBuildsOfOneArchiveDoNotDuplicateRows() throws Exception {
        GirLayerConfigContext context=context(4501);
        ExecutorService workers=Executors.newFixedThreadPool(2);
        try {
            Future<?> first=workers.submit(()->new LocalZipXYZTileStorageSupport(helper()).preCacheTiles(context,(all,done)->{}));
            Future<?> second=workers.submit(()->new LocalZipXYZTileStorageSupport(helper()).preCacheTiles(context,(all,done)->{}));
            first.get(10,TimeUnit.SECONDS);second.get(10,TimeUnit.SECONDS);
            try(SQLiteLayerPerFileDao dao=new SQLiteLayerPerFileDao(context.getLayerName())) { assertEquals(4501,count(dao)); }
        } finally {workers.shutdownNow();}
    }
    @Test public void preserves3dAndS3mMetadataAndAssociatedResources() throws Exception {
        for(GirMapTileType type:Arrays.asList(GirMapTileType.TILE_3D,GirMapTileType.S3M)) {
            GirLayerConfigContext context=context(1); context.setDataId(type.name()); context.setMapTileType(type);
            String rootName=type==GirMapTileType.S3M?"root.scp":"root.json";
            try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(Paths.get(context.getObjectKey())))) {
                for(String name:Arrays.asList("container/"+rootName,"container/parts/model.bin","unrelated.txt")) {
                    zip.putNextEntry(new ZipEntry(name));zip.write(new byte[]{5,6,7});zip.closeEntry();
                }
            }
            LocalZip3DTileStorageSupport support=new LocalZip3DTileStorageSupport(helper());
            support.preCacheTiles(context,(all,done)->{});
            try(SQLiteLayerPerFileDao dao=new SQLiteLayerPerFileDao(context.getLayerName())) {
                assertEquals(2,count(dao));
                assertNotNull(dao.findByFileName(type==GirMapTileType.S3M?"tilesetS3MB.scp":"tileset.json"));
                assertArrayEquals(new byte[]{5,6,7},support.getICompressionHandler().readAndDecompressEntry(
                        dao.findByFileName("parts/model.bin"),context.getObjectKey()));
            }
        }
    }
}
