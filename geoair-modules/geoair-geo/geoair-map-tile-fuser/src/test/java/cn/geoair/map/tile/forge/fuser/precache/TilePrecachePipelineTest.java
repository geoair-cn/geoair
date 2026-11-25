package cn.geoair.map.tile.forge.fuser.precache;

import static org.junit.Assert.*;

import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.fuser.cache.FileTileCache;
import cn.geoair.map.tile.forge.fuser.cache.MBTilesTileCache;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.cache.TileCacheQuery;
import cn.geoair.map.tile.forge.fuser.encoding.EncodedTileCache;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingContext;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingPolicy;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingScope;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingSnapshot;
import cn.geoair.map.tile.forge.fuser.encoding.TileImageCodec;
import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;
import cn.geoair.map.tile.forge.fuser.fuser.TileRenderPlan;
import cn.geoair.map.tile.forge.fuser.image.*;
import cn.geoair.map.tile.forge.fuser.image.watermark.GoogleWatermarkProcessor;
import cn.geoair.map.tile.forge.fuser.support.ConstantTileImageProcessor;
import cn.geoair.map.tile.forge.fuser.support.RasterTestImages;
import cn.geoair.map.tile.forge.fuser.utils.TileImageUtils;
import cn.geoair.web.mime.GiMimeType;
import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

/** 使用真实HTTP图片、SQLite及缓存驱动，业务任务表和Spring模型不进入库测试。 */
public class TilePrecachePipelineTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private final List<MBTilesTileCache> databases = new ArrayList<>();
    private final ExecutorService workers = Executors.newFixedThreadPool(4);
    private HttpServer server;

    @After
    public void close() throws Exception {
        workers.shutdownNow();
        assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        for (MBTilesTileCache db : databases) db.close();
        if (server != null) server.stop(0);
    }

    private TileCache raw(boolean mbtiles) throws Exception {
        String root = folder.newFolder().toString();
        if (!mbtiles) return new FileTileCache(root, 0, true);
        MBTilesTileCache cache = new MBTilesTileCache(root);
        databases.add(cache);
        return cache;
    }

    private TilePrecacheLevel plan(int count) {
        return new TilePrecacheLevel(3, new RangeApo(0, count - 1, 0, 0, 3), null);
    }

    private ConstantTileImageProcessor rule(byte[] output) {
        return new ConstantTileImageProcessor(
                new TileImageProcessingResult(
                        TileImageProcessingState.PROCESSED, "test-v1", .9, 1, output, "完成"));
    }

    private TilePrecachePipeline pipeline(TileCache cache, TileImageProcessor rule) {
        return new TilePrecachePipeline(
                cache,
                "copy",
                rule,
                (z, x, y) -> TileImageProcessingContext.ORIGINAL,
                new TileEncodingPolicy("JPEG", 60),
                null);
    }

    private TilePrecacheProgress run(
            int count,
            TileCache source,
            TileContentLoader loader,
            TilePrecachePipeline pipeline,
            TileCancellationToken stop,
            TilePrecacheOptions options) {
        return SegmentedTilePrecache.execute(
                        "source",
                        Collections.singletonList(plan(count)),
                        ImageMime.png,
                        source,
                        loader,
                        pipeline,
                        workers,
                        stop,
                        options,
                        null)
                .get(0);
    }

    private byte[] sample(String name) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/watermark/" + name)) {
            assertNotNull(input);
            return TileImageUtils.readAllLimited(input);
        }
    }

    @Test
    public void networkDownloadCacheReuseAndWatermarkProcessingComposeOnBothDrivers()
            throws Exception {
        byte[] marked = sample("google-sea-12-3441-1680.png"),
                clean = sample("google-land-12-3420-1680.png");
        ByteArrayOutputStream bigger = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB), "png", bigger);
        AtomicInteger requests = new AtomicInteger(), rawInputs = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                response -> {
                    requests.incrementAndGet();
                    int x = Integer.parseInt(response.getRequestURI().getPath().substring(1));
                    if (x == 3) {
                        response.sendResponseHeaders(404, -1);
                        response.close();
                        return;
                    }
                    byte[] bytes = x == 0 ? marked : bigger.toByteArray();
                    response.getResponseHeaders().set("Connection", "close");
                    response.sendResponseHeaders(200, bytes.length);
                    try {
                        response.getResponseBody().write(bytes);
                    } finally {
                        response.close();
                    }
                });
        server.start();
        for (boolean mbtiles : new boolean[] {false, true}) {
            TileCache storage = raw(mbtiles), source = new EncodedTileCache(storage, null);
            assertTrue(storage.put("source", 3, 1, 0, clean, ImageMime.png));
            GoogleWatermarkProcessor google = new GoogleWatermarkProcessor();
            TileImageProcessor observesRaw =
                    new TileImageProcessor() {
                        public String getRuleVersion() {
                            return google.getRuleVersion();
                        }

                        public TileImageProcessingResult process(
                                byte[] bytes, TileImageProcessingContext context)
                                throws IOException, InterruptedException {
                            if (Arrays.equals(bytes, marked)) rawInputs.incrementAndGet();
                            return google.process(bytes, context);
                        }
                    };
            List<TilePrecacheTileResult> events = new CopyOnWriteArrayList<>();
            TilePrecachePipeline pipeline =
                    new TilePrecachePipeline(
                            storage,
                            "copy",
                            observesRaw,
                            (z, x, y) -> TileImageProcessingContext.ORIGINAL,
                            new TileEncodingPolicy("JPEG", 65),
                            events::add);
            TilePrecacheProgress progress;
            try (TileEncodingScope scope =
                    new TileEncodingSnapshot(
                                    new TileEncodingPolicy("JPEG", 20), new TileEncodingPolicy())
                            .bindings("source", "unused")
                            .bind()) {
                progress =
                        run(
                                5,
                                source,
                                (z, x, y, context) -> {
                                    if (x == 4) throw new IOException("模拟单张源故障");
                                    HttpURLConnection connection =
                                            (HttpURLConnection)
                                                    new URL(
                                                                    "http://127.0.0.1:"
                                                                            + server.getAddress()
                                                                                    .getPort()
                                                                            + "/"
                                                                            + x)
                                                            .openConnection();
                                    connection.setConnectTimeout(context.remainingMillis());
                                    connection.setReadTimeout(context.remainingMillis());
                                    try {
                                        if (connection.getResponseCode() == 404) return null;
                                        try (InputStream input = connection.getInputStream()) {
                                            return TileImageUtils.readAllLimited(input);
                                        }
                                    } finally {
                                        connection.disconnect();
                                    }
                                },
                                pipeline,
                                new TileCancellationToken(),
                                new TilePrecacheOptions());
            }
            assertEquals(5, progress.getProcessed());
            assertEquals(100, progress.getPercent());
            assertEquals(
                    Long.valueOf(2),
                    progress.getStages().getLoads().get(TileOperationStatus.DOWNLOADED));
            assertEquals(Long.valueOf(1), progress.getCounts().get(TileOperationStatus.CACHED));
            assertEquals(Long.valueOf(1), progress.getCounts().get(TileOperationStatus.MISSING));
            assertEquals(Long.valueOf(1), progress.getCounts().get(TileOperationStatus.FAILED));
            assertEquals(3, progress.getStages().getChecked());
            assertEquals(1, progress.getStages().getStored());
            assertEquals(
                    Long.valueOf(1),
                    progress.getStages().getProcessing().get(TileImageProcessingState.REVIEW));
            assertArrayEquals(clean, storage.get("source", 3, 1, 0, ImageMime.png));
            byte[] copy = storage.get("copy", 3, 0, 0, ImageMime.jpeg);
            assertNotNull(copy);
            assertEquals("jpeg", TileImageCodec.detect(copy));
            assertNull(storage.get("copy", 3, 1, 0, ImageMime.jpeg));
            assertEquals(5, events.size());
            assertThrows(
                    UnsupportedOperationException.class,
                    () -> progress.getStages().getLoads().clear());
        }
        assertEquals(6, requests.get());
        assertEquals(2, rawInputs.get());
    }

    @Test
    public void successfulSourceWriteAndFailedResultWriteDoNotDoubleCountAndCanBeRetried()
            throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        FileTileCache source = new FileTileCache(folder.newFolder().toString(), 0, true);
        FileTileCache output =
                new FileTileCache(folder.newFolder().toString(), 0, true) {
                    @Override
                    public boolean put(
                            String layer, int z, int x, int y, byte[] bytes, GiMimeType format) {
                        return x != 0 && super.put(layer, z, x, y, bytes, format);
                    }
                };
        TilePrecacheProgress progress =
                run(
                        2,
                        source,
                        (z, x, y, c) -> png,
                        pipeline(output, rule(png)),
                        new TileCancellationToken(),
                        new TilePrecacheOptions());
        assertEquals(2, progress.getProcessed());
        assertEquals(100, progress.getPercent());
        assertEquals(
                Long.valueOf(2),
                progress.getStages().getLoads().get(TileOperationStatus.DOWNLOADED));
        assertEquals(Long.valueOf(1), progress.getCounts().get(TileOperationStatus.FAILED));
        assertEquals(
                Long.valueOf(1), progress.getStages().getWrites().get(TileImageWriteState.FAILED));
        assertEquals(1, progress.getStages().getStored());
        assertArrayEquals(png, source.get("source", 3, 0, 0, ImageMime.png));
        AtomicInteger reads = new AtomicInteger();
        TilePrecacheProgress retried =
                run(
                        2,
                        source,
                        (z, x, y, c) -> {
                            reads.incrementAndGet();
                            return png;
                        },
                        pipeline(raw(false), rule(png)),
                        new TileCancellationToken(),
                        new TilePrecacheOptions());
        assertEquals(0, reads.get());
        assertEquals(2, retried.getStages().getStored());
        assertEquals(Long.valueOf(2), retried.getCounts().get(TileOperationStatus.CACHED));
    }

    @Test
    public void sourceWriteFailureNeverProcessesOrPublishesTheUncommittedTile() throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        FileTileCache source =
                new FileTileCache(folder.newFolder().toString(), 0, true) {
                    @Override
                    public boolean put(
                            String layer, int z, int x, int y, byte[] bytes, GiMimeType format) {
                        return x != 0 && super.put(layer, z, x, y, bytes, format);
                    }
                };
        TileCache output = raw(false);
        ConstantTileImageProcessor processor = rule(png);
        TilePrecacheProgress result =
                run(
                        2,
                        source,
                        (z, x, y, c) -> png,
                        pipeline(output, processor),
                        new TileCancellationToken(),
                        new TilePrecacheOptions());
        assertEquals(1, processor.calls.get());
        assertEquals(1, result.getStages().getStored());
        assertNull(output.get("copy", 3, 0, 0, ImageMime.jpeg));
        assertEquals(
                Long.valueOf(1), result.getStages().getLoads().get(TileOperationStatus.FAILED));
    }

    @Test
    public void staleCacheHitFallsBackToDownloading() throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        FileTileCache source =
                new FileTileCache(folder.newFolder().toString(), 0, true) {
                    @Override
                    public BitSet findExisting(TileCacheQuery query, TileOperationContext context) {
                        BitSet found = super.findExisting(query, context);
                        delete("source", 3, 0, 0, ImageMime.png);
                        return found;
                    }
                };
        source.put("source", 3, 0, 0, png, ImageMime.png);
        AtomicInteger reads = new AtomicInteger();
        TilePrecacheProgress result =
                run(
                        1,
                        source,
                        (z, x, y, c) -> {
                            reads.incrementAndGet();
                            return png;
                        },
                        pipeline(raw(false), rule(png)),
                        new TileCancellationToken(),
                        new TilePrecacheOptions());
        assertEquals(1, reads.get());
        assertEquals(Long.valueOf(1), result.getCounts().get(TileOperationStatus.DOWNLOADED));
    }

    @Test
    public void oneTileProcessingTimeoutDoesNotCancelTheOtherTile() throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        AtomicInteger calls = new AtomicInteger();
        TileImageProcessor slow =
                new TileImageProcessor() {
                    public String getRuleVersion() {
                        return "slow-v1";
                    }

                    public TileImageProcessingResult process(
                            byte[] bytes, TileImageProcessingContext context)
                            throws InterruptedException {
                        if (calls.getAndIncrement() == 0) Thread.sleep(750);
                        return rule(png).process(bytes, context);
                    }
                };
        TilePrecacheOptions options = new TilePrecacheOptions();
        options.setTileTimeoutMillis(500);
        TileCancellationToken stop = new TileCancellationToken();
        TilePrecacheProgress result =
                run(2, raw(false), (z, x, y, c) -> png, pipeline(raw(false), slow), stop, options);
        assertFalse(stop.isCancelled());
        assertEquals(Long.valueOf(1), result.getCounts().get(TileOperationStatus.TIMED_OUT));
        assertEquals(1, result.getStages().getStored());
        assertEquals(
                Long.valueOf(2), result.getStages().getLoads().get(TileOperationStatus.DOWNLOADED));
    }

    @Test
    public void stopBetweenProcessingAndWritingRetainsSourceButNeverPublishesCopy()
            throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        TileCancellationToken stop = new TileCancellationToken();
        AtomicInteger reads = new AtomicInteger();
        TileImageProcessor cancels =
                new TileImageProcessor() {
                    public String getRuleVersion() {
                        return "stop-v1";
                    }

                    public TileImageProcessingResult process(
                            byte[] bytes, TileImageProcessingContext context) {
                        stop.cancel();
                        return rule(png).process(bytes, context);
                    }
                };
        TileCache source = raw(false), output = raw(false);
        TilePrecacheProgress result =
                run(
                        2,
                        source,
                        (z, x, y, c) -> {
                            reads.incrementAndGet();
                            return png;
                        },
                        pipeline(output, cancels),
                        stop,
                        new TilePrecacheOptions());
        assertEquals(1, reads.get());
        assertEquals(1, result.getRemaining());
        assertEquals(0, result.getStages().getStored());
        assertArrayEquals(png, source.get("source", 3, 0, 0, ImageMime.png));
        assertNull(output.get("copy", 3, 0, 0, ImageMime.jpeg));
    }

    @Test
    public void actualWriteCommitIsKeptEvenIfCancellationIsObservedImmediatelyAfterwards()
            throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        TileCancellationToken stop = new TileCancellationToken();
        FileTileCache output =
                new FileTileCache(folder.newFolder().toString(), 0, true) {
                    @Override
                    public boolean put(
                            String layer, int z, int x, int y, byte[] bytes, GiMimeType format) {
                        boolean stored = super.put(layer, z, x, y, bytes, format);
                        stop.cancel();
                        return stored;
                    }
                };
        TilePrecacheProgress result =
                run(
                        1,
                        raw(false),
                        (z, x, y, c) -> png,
                        pipeline(output, rule(png)),
                        stop,
                        new TilePrecacheOptions());
        assertEquals(Long.valueOf(1), result.getCounts().get(TileOperationStatus.CANCELLED));
        assertEquals(1, result.getStages().getStored());
        assertNotNull(output.get("copy", 3, 0, 0, ImageMime.jpeg));
    }

    @Test
    public void fusedLoaderCanUseTheSamePipelineAndOldEntryStillSkipsExistingTiles()
            throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        StubTileGetter source = new StubTileGetter(png);
        TileContentLoader loader =
                TileContentLoaders.fused(
                        source,
                        ImageMime.png,
                        (z, x, y) ->
                                source.getSrcGridSubset().boundsFromIndex(new long[] {x, y, z}),
                        256,
                        256);
        TileCache cache = raw(false), output = raw(false);
        ConstantTileImageProcessor processor = rule(png);
        TilePrecacheProgress result =
                run(
                        1,
                        cache,
                        loader,
                        pipeline(output, processor),
                        new TileCancellationToken(),
                        new TilePrecacheOptions());
        assertEquals(1, result.getStages().getStored());
        assertNotNull(cache.get("source", 3, 0, 0, ImageMime.png));
        AtomicInteger reads = new AtomicInteger();
        TilePrecacheProgress legacy =
                SegmentedTilePrecache.execute(
                                "source",
                                Collections.singletonList(plan(1)),
                                ImageMime.png,
                                cache,
                                (z, x, y, c) -> {
                                    reads.incrementAndGet();
                                    return png;
                                },
                                workers,
                                new TileCancellationToken(),
                                new TilePrecacheOptions(),
                                null)
                        .get(0);
        assertEquals(0, reads.get());
        assertEquals(0, legacy.getStages().getChecked());
        assertEquals(1, processor.calls.get());
    }

    @Test
    public void parallelTasksFreezeDifferentPoliciesAndRestoreReusedWorkerThreads()
            throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        TileCache firstRaw = raw(false), secondRaw = raw(false);
        ExecutorService coordinators = Executors.newFixedThreadPool(2);
        CountDownLatch downloading = new CountDownLatch(2);
        TileContentLoader loader =
                (z, x, y, c) -> {
                    downloading.countDown();
                    if (!downloading.await(5, TimeUnit.SECONDS)) throw new IOException("未并行进入");
                    return png;
                };
        try {
            List<Future<TilePrecacheProgress>> tasks = new ArrayList<>();
            int index = 0;
            for (TileCache raw : new TileCache[] {firstRaw, secondRaw}) {
                String selected = index++ == 0 ? "JPEG" : "PNG";
                tasks.add(
                        coordinators.submit(
                                () -> {
                                    TileEncodingPolicy policy =
                                            new TileEncodingPolicy(selected, 60);
                                    TileEncodingSnapshot snapshot =
                                            new TileEncodingSnapshot(
                                                    policy, new TileEncodingPolicy());
                                    policy.setMode("LEGACY");
                                    try (TileEncodingScope scope =
                                            snapshot.bindings("source", "unused").bind()) {
                                        return run(
                                                1,
                                                new EncodedTileCache(raw, null),
                                                loader,
                                                null,
                                                new TileCancellationToken(),
                                                new TilePrecacheOptions());
                                    }
                                }));
            }
            for (Future<TilePrecacheProgress> task : tasks)
                assertEquals(1, task.get(10, TimeUnit.SECONDS).getProcessed());
            assertNotNull(firstRaw.get("source", 3, 0, 0, ImageMime.jpeg));
            assertNull(firstRaw.get("source", 3, 0, 0, ImageMime.png));
            assertArrayEquals(png, secondRaw.get("source", 3, 0, 0, ImageMime.png));
            assertNull(workers.submit(() -> TileEncodingContext.capture().policy("source")).get());
        } finally {
            coordinators.shutdownNow();
        }
    }

    @Test
    public void implicitOriginalCacheWritesInsideLoaderUseTheCapturedTaskSnapshot()
            throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        TileCache storage = raw(false), encoded = new EncodedTileCache(storage, null);
        TileEncodingSnapshot snapshot =
                new TileEncodingSnapshot(
                        new TileEncodingPolicy("JPEG", 35), new TileEncodingPolicy("PNG", 80));
        try (TileEncodingScope scope = snapshot.bindings("rawtiles", "source").bind()) {
            TilePrecacheProgress result =
                    run(
                            1,
                            encoded,
                            (z, x, y, c) -> {
                                assertTrue(encoded.put("rawtiles", z, x, y, png, ImageMime.png));
                                return png;
                            },
                            null,
                            new TileCancellationToken(),
                            new TilePrecacheOptions());
            assertEquals(1, result.getProcessed());
        }
        assertEquals(
                "jpeg", TileImageCodec.detect(storage.get("rawtiles", 3, 0, 0, ImageMime.jpeg)));
        assertArrayEquals(png, storage.get("source", 3, 0, 0, ImageMime.png));
    }

    @Test
    public void convertedProcessingUsesActualRenderPlanScaleFromTheResolver() throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        StubTileGetter getter = new StubTileGetter(png);
        TileRenderPlan plan =
                TileRenderPlan.calculate(
                        getter.getSrcGridSubset(),
                        getter.getSrcGridSubset().boundsFromIndex(new long[] {0, 0, 3}),
                        200,
                        200);
        AtomicInteger seen = new AtomicInteger();
        TileImageProcessor processor =
                new TileImageProcessor() {
                    public String getRuleVersion() {
                        return "scale-v1";
                    }

                    public TileImageProcessingResult process(
                            byte[] bytes, TileImageProcessingContext context) {
                        assertEquals(plan.getScaleX(), context.getScaleX(), 0);
                        assertEquals(plan.getScaleY(), context.getScaleY(), 0);
                        assertNotEquals(1, context.getScaleX(), 0);
                        seen.incrementAndGet();
                        return rule(png).process(bytes, context);
                    }
                };
        TilePrecachePipeline pipeline =
                new TilePrecachePipeline(
                        raw(false),
                        "copy",
                        processor,
                        (z, x, y) -> TileImageProcessingContext.from(plan),
                        new TileEncodingPolicy("PNG", 90),
                        null);
        assertEquals(
                1,
                run(
                                1,
                                raw(false),
                                (z, x, y, c) -> png,
                                pipeline,
                                new TileCancellationToken(),
                                new TilePrecacheOptions())
                        .getStages()
                        .getStored());
        assertEquals(1, seen.get());
    }

    @Test
    public void corruptInputFailsBeforeCallingImageRuleAndDoesNotStopOtherTiles() throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        ConstantTileImageProcessor processor = rule(png);
        TileCache source = raw(false);
        TilePrecacheProgress result =
                run(
                        2,
                        source,
                        (z, x, y, c) -> x == 0 ? new byte[] {1, 2, 3} : png,
                        pipeline(raw(false), processor),
                        new TileCancellationToken(),
                        new TilePrecacheOptions());
        assertEquals(1, processor.calls.get());
        assertEquals(1, result.getStages().getStored());
        assertEquals(Long.valueOf(1), result.getCounts().get(TileOperationStatus.FAILED));
        assertArrayEquals(new byte[] {1, 2, 3}, source.get("source", 3, 0, 0, ImageMime.png));
    }

    @Test
    public void failingBusinessCallbackPropagatesWithItsCauseAfterWorkersDrain() throws Exception {
        byte[] png = RasterTestImages.image("png", false);
        IllegalArgumentException failure = new IllegalArgumentException("任务进度保存失败");
        TileCache output = raw(false);
        TilePrecachePipeline pipeline =
                new TilePrecachePipeline(
                        output,
                        "copy",
                        rule(png),
                        (z, x, y) -> TileImageProcessingContext.ORIGINAL,
                        new TileEncodingPolicy("PNG", 90),
                        result -> {
                            throw failure;
                        });
        IllegalStateException thrown =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                run(
                                        1,
                                        raw(false),
                                        (z, x, y, c) -> png,
                                        pipeline,
                                        new TileCancellationToken(),
                                        new TilePrecacheOptions()));
        assertSame(failure, thrown.getCause());
        assertArrayEquals(png, output.get("copy", 3, 0, 0, ImageMime.png));
    }
}
