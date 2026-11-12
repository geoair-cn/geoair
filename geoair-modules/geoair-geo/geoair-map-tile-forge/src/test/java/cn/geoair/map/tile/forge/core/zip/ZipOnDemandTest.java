package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.config.ZipReadOptions;
import cn.geoair.map.tile.forge.core.utils.ExtractLockUtils;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.local.LocalZip3DTileStorageSupport;
import cn.geoair.map.tile.forge.core.zip.cache.SQLiteLayerPerFileDao;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.Assert.*;

public class ZipOnDemandTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private String previous;
    private Path source;
    private GirLayerConfigContext context;
    private final ZipFixtureHelper helper = new ZipFixtureHelper();
    private final CountingZipHandler handler = new CountingZipHandler();

    @Before public void setup() throws Exception {
        previous = TileTempPathConfig.getInstance().getTempPath();
        TileTempPathConfig.getInstance().setTempPath(folder.getRoot().toString());
        source = folder.getRoot().toPath().resolve("中文 + 空格.zip");
        context = new GirLayerConfigContext().setDataId("source").setStorageType(GirStorageType.LOCAL_ZIP)
                .setMapTileType(GirMapTileType.TILE_3D).setObjectKey(source.toString());
        archive("Data/tileset.json", "Data/模型/a+b 1.glb", "old model");
    }
    @After public void restore() { TileTempPathConfig.getInstance().setTempPath(previous); }

    private void archive(String metadata, String model, String content) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(source))) {
            for (String path : new String[]{metadata, model}) {
                zip.putNextEntry(new ZipEntry(path)); zip.write(content.getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            }
        }
    }

    private LocalZip3DTileStorageSupport support() {
        return new LocalZip3DTileStorageSupport(helper) {
            @Override public ICompressionHandler getICompressionHandler() { return handler; }
        };
    }

    @Test public void noIndexAndUnavailableIndexBothReadTheSource() throws Exception {
        helper.broken = true;
        LocalZip3DTileStorageSupport support = support();
        assertTrue(support.getTileData(context, "tileset.json", "", "").isExists());
        assertArrayEquals("old model".getBytes(StandardCharsets.UTF_8), support.getTileData(context, "模型/a+b 1.glb", "", "").getBytes());
        assertEquals("索引故障短期内只检查一次", 1, helper.lookups.get());
        assertFalse(Files.exists(folder.getRoot().toPath().resolve("sql_lite_meta/source_tiles.sqlite")));
    }

    @Test public void outerRootAndExplicitPrefixKeepRelativeModelPaths() throws Exception {
        archive("tileset.json", "Data/模型/a+b 1.glb", "outside root");
        context.setTilePathPrefix("Data");
        LocalZip3DTileStorageSupport support = support();
        assertEquals("", support.preCheckZipAndGetRoot(context, handler).getRootPath());
        assertTrue(support.getTileData(context, "tileset.json", "", "").isExists());
        assertTrue(support.getTileData(context, "Data/模型/a+b 1.glb", "", "").isExists());
        assertFalse(support.getTileData(context, "模型/a+b 1.glb", "", "").isExists());
    }

    @Test public void concurrentFirstRequestsExtractOnlyOnce() throws Exception {
        LocalZip3DTileStorageSupport support = support();
        ExecutorService pool = Executors.newFixedThreadPool(12);
        try {
            List<Future<TileRequest>> requests = new ArrayList<>();
            for (int i = 0; i < 24; i++) requests.add(pool.submit(() -> support.getTileData(context, "模型/a+b 1.glb", "", "")));
            for (Future<TileRequest> request : requests) assertEquals("old model", new String(request.get(10, TimeUnit.SECONDS).getBytes(), StandardCharsets.UTF_8));
            assertEquals(1, handler.extractions.get());
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS)); }
    }

    @Test public void failedExtractionLeavesNoPartialFileAndCanRetry() throws Exception {
        LocalZip3DTileStorageSupport support = support(); handler.fail = true;
        try { support.getTileData(context, "模型/a+b 1.glb", "", ""); fail("应报告提取失败"); }
        catch (ZipReadException e) { assertEquals(ZipReadFailure.EXTRACTION_FAILED, e.getFailure()); }
        try (Stream<Path> paths = Files.walk(folder.getRoot().toPath().resolve("zip_resources"))) {
            assertFalse(paths.anyMatch(Files::isRegularFile));
        }
        handler.fail = false;
        assertTrue(support.getTileData(context, "模型/a+b 1.glb", "", "").isExists());
        assertEquals(2, handler.extractions.get());
    }

    @Test public void sourceUpdateInvalidatesFilesEntriesAndPersistentIndex() throws Exception {
        LocalZip3DTileStorageSupport support = support();
        support.preCacheCentralDir(context);
        assertTrue(support.getTileData(context, "模型/a+b 1.glb", "", "").isExists());
        String before;
        try (SQLiteLayerPerFileDao index = new SQLiteLayerPerFileDao(context.getDataId())) { before = index.getArchiveVersion(); }
        archive("Data/tileset.json", "Data/模型/a+b 1.glb", "new model"); // 同样长度也须失效。
        Files.setLastModifiedTime(source, FileTime.fromMillis(System.currentTimeMillis() + 10000));
        assertEquals("new model", new String(support.getTileData(context, "模型/a+b 1.glb", "", "").getBytes(), StandardCharsets.UTF_8));
        support.preCacheCentralDir(context);
        try (SQLiteLayerPerFileDao index = new SQLiteLayerPerFileDao(context.getDataId())) { assertNotEquals(before, index.getArchiveVersion()); }
    }

    @Test public void completeVersionedIndexAvoidsDirectorySearch() throws Exception {
        support().preCacheCentralDir(context);
        handler.scans.set(0); handler.searches.set(0);
        assertTrue(support().getTileData(context, "模型/a+b 1.glb", "", "").isExists());
        assertEquals(0, handler.scans.get()); assertEquals(0, handler.searches.get());
    }

    @Test public void missingIsShortCachedButBrokenArchiveIsAnError() throws Exception {
        LocalZip3DTileStorageSupport support = support();
        assertFalse(support.getTileData(context, "missing.glb", "", "").isExists());
        int searches = handler.searches.get();
        assertFalse(support.getTileData(context, "missing.glb", "", "").isExists());
        assertEquals(searches, handler.searches.get());
        Files.write(source, new byte[]{1, 2, 3});
        try { support.getTileData(context, "tileset.json", "", ""); fail("损坏归档不能当成资源不存在"); }
        catch (ZipReadException e) { assertEquals(ZipReadFailure.INVALID_ARCHIVE, e.getFailure()); }
    }

    @Test public void sourceChangedDuringExtractionDoesNotPublish() throws Exception {
        handler.mutate = true;
        try { support().getTileData(context, "tileset.json", "", ""); fail("不应发布旧源的文件"); }
        catch (ZipReadException e) { assertEquals(ZipReadFailure.SOURCE_CHANGED, e.getFailure()); }
        try (Stream<Path> files = Files.walk(folder.getRoot().toPath().resolve("zip_resources"))) { assertFalse(files.anyMatch(Files::isRegularFile)); }
    }

    @Test public void pathTraversalAndWindowsAbsolutePathsAreRejected() throws Exception {
        LocalZip3DTileStorageSupport support = support();
        for (String path : new String[]{"../tileset.json", "a/../../x", "/x", "C:/x", "a\\x"}) {
            try { support.getTileData(context, path, "", ""); fail(path); }
            catch (ZipReadException e) { assertEquals(ZipReadFailure.INVALID_PATH, e.getFailure()); }
        }
    }

    @Test public void malformedArchivePathsCannotBeNormalizedIntoValidResources() throws Exception {
        archive("Data/tileset.json", "Data/../escaped.glb", "unsafe");
        try { support().getTileData(context, "tileset.json", "", ""); fail("非法ZIP条目必须拒绝"); }
        catch (ZipReadException e) { assertEquals(ZipReadFailure.INVALID_ARCHIVE, e.getFailure()); }
    }

    @Test public void multipleSiblingRootsRequireAnExplicitPrefix() throws Exception {
        archive("a/tileset.json", "b/tileset.json", "ambiguous");
        try { support().getTileData(context, "tileset.json", "", ""); fail("不能任意选择同级数据集"); }
        catch (ZipReadException e) { assertEquals(ZipReadFailure.ROOT_NOT_FOUND, e.getFailure()); }
        context.setTilePathPrefix("a");
        assertTrue(support().getTileData(context, "tileset.json", "", "").isExists());
    }

    @Test public void resourceWaitTimesOutAndThreadInterruptionIsPreserved() throws Exception {
        LocalZip3DTileStorageSupport support = support();
        ZipResourceReader resources = new ZipResourceReader(support);
        String key = resources.directory(context).resolve("tileset.json").toString();
        ZipReadOptions options = ZipReadOptions.getInstance(); long previousTimeout = options.getLockTimeoutMillis();
        CountDownLatch held = new CountDownLatch(1), release = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<?> holder = pool.submit(() -> {
            try { ExtractLockUtils.withLock(key, () -> { held.countDown(); release.await(); return null; }); }
            catch (Exception e) { throw new IllegalStateException(e); }
        });
        try {
            assertTrue(held.await(5, TimeUnit.SECONDS)); options.setLockTimeoutMillis(20);
            try { support.getTileData(context, "tileset.json", "", ""); fail("等待应超时"); }
            catch (ZipReadException e) { assertEquals(ZipReadFailure.WAIT_TIMEOUT, e.getFailure()); }
            Thread.currentThread().interrupt();
            try { support.getTileData(context, "tileset.json", "", ""); fail("中断应终止读取"); }
            catch (ZipReadException e) { assertEquals(ZipReadFailure.INTERRUPTED, e.getFailure()); assertTrue(Thread.currentThread().isInterrupted()); }
            finally { Thread.interrupted(); }
        } finally {
            options.setLockTimeoutMillis(previousTimeout); release.countDown(); holder.get(5, TimeUnit.SECONDS);
            pool.shutdownNow(); assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test public void oldIndexIsNotModifiedByReadsAndCanBeRebuiltAutomatically() throws Exception {
        SQLiteLayerPerFileDao index = new SQLiteLayerPerFileDao(context.getDataId());
        Path file = Paths.get(index.getTableName());
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri()); Statement sql = db.createStatement()) {
            sql.execute("CREATE TABLE cache_status(id INTEGER PRIMARY KEY AUTOINCREMENT,layer_name TEXT UNIQUE NOT NULL,cache_status TEXT NOT NULL,cache_time TIMESTAMP,cache_size BIGINT,last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            sql.execute("INSERT INTO cache_status(layer_name,cache_status) VALUES('source','CACHED')");
        }
        assertNull(index.getArchiveVersion());
        assertTrue(support().getTileData(context, "tileset.json", "", "").isExists());
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri()); Statement sql = db.createStatement();
             ResultSet result = sql.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name='archive_source'")) {
            assertTrue(result.next()); assertEquals(0, result.getInt(1));
        }
        support().preCacheCentralDir(context);
        assertNotNull(index.getArchiveVersion());
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file.toUri()); Statement sql = db.createStatement();
             ResultSet result = sql.executeQuery("EXPLAIN QUERY PLAN SELECT * FROM tile_central_directory WHERE file_name='tileset.json'")) {
            boolean indexed = false;
            while (result.next()) indexed |= result.getString(4).contains("idx_tile_directory_file_name");
            assertTrue("文件名查询必须命中索引", indexed);
        }
    }
}
