package cn.geoair.map.tile.forge.fuser.precache;

import static org.junit.Assert.*;

import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.fuser.cache.*;
import cn.geoair.map.tile.forge.fuser.execution.*;
import cn.geoair.web.mime.GirImageMime;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class SegmentedTilePrecacheTest {
    @Test
    public void lateLoaderExceptionIsCountedAsTileTimeoutAndOtherTilesContinue() {
        TilePrecacheOptions options = new TilePrecacheOptions();
        options.setTileTimeoutMillis(1);
        List<TilePrecacheProgress> result = SegmentedTilePrecache.execute(
                "x", Collections.singletonList(plan(2)), GirImageMime.png, cache(),
                (z, x, y, context) -> {
                    Thread.sleep(10);
                    throw new IllegalStateException("延迟失败");
                }, workers, new TileCancellationToken(), options, null);
        assertEquals(4, result.get(0).getProcessed());
        assertEquals(Long.valueOf(4), result.get(0).getCounts().get(TileOperationStatus.TIMED_OUT));
    }
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private ExecutorService workers, coordinator;

    @Before
    public void setup() {
        workers = Executors.newFixedThreadPool(4);
        coordinator = Executors.newSingleThreadExecutor();
    }

    @After
    public void cleanup() {
        workers.shutdownNow();
        coordinator.shutdownNow();
    }

    private FileTileCache cache() {
        return new FileTileCache(folder.getRoot().toString(), 0, true);
    }

    private TilePrecacheLevel plan(int z) {
        return new TilePrecacheLevel(z, new RangeApo(0, 0, 0, 3, z), null);
    }

    @Test
    public void countsCachedDownloadedMissingAndFailedWithoutPreScanning() {
        FileTileCache cache = cache();
        cache.put("x", 2, 0, 0, new byte[] {1}, GirImageMime.png);
        AtomicInteger calls = new AtomicInteger();
        List<TilePrecacheProgress> result =
                SegmentedTilePrecache.execute(
                        "x",
                        Collections.singletonList(plan(2)),
                        GirImageMime.png,
                        cache,
                        (z, x, y, c) -> {
                            calls.incrementAndGet();
                            if (y == 2) return null;
                            if (y == 3) throw new IllegalStateException();
                            return new byte[] {1};
                        },
                        workers,
                        new TileCancellationToken(),
                        new TilePrecacheOptions(),
                        null);
        TilePrecacheProgress p = result.get(0);
        assertEquals(4, p.getTotal());
        assertEquals(4, p.getProcessed());
        assertEquals(3, calls.get());
        assertEquals(Long.valueOf(1), p.getCounts().get(TileOperationStatus.CACHED));
        assertEquals(Long.valueOf(1), p.getCounts().get(TileOperationStatus.DOWNLOADED));
        assertEquals(Long.valueOf(1), p.getCounts().get(TileOperationStatus.MISSING));
        assertEquals(Long.valueOf(1), p.getCounts().get(TileOperationStatus.FAILED));
        assertNull(cache.get("x", 2, 0, 2, GirImageMime.png));
    }

    @Test
    public void segmentsFromDifferentLevelsRunTogether() throws Exception {
        CountDownLatch entered = new CountDownLatch(2);
        Set<Integer> levels = ConcurrentHashMap.newKeySet();
        TilePrecacheOptions options = new TilePrecacheOptions();
        options.setSegmentSize(1);
        List<TilePrecacheProgress> result =
                SegmentedTilePrecache.execute(
                        "x",
                        Arrays.asList(plan(2), plan(3)),
                        GirImageMime.png,
                        cache(),
                        (z, x, y, c) -> {
                            levels.add(z);
                            entered.countDown();
                            if (!entered.await(3, TimeUnit.SECONDS))
                                throw new IllegalStateException("串行执行");
                            return new byte[] {1};
                        },
                        workers,
                        new TileCancellationToken(),
                        options,
                        null);
        assertEquals(2, levels.size());
        assertEquals(8, result.stream().mapToLong(TilePrecacheProgress::getProcessed).sum());
        assertEquals(
                8,
                result.stream()
                        .mapToLong(p -> p.getCounts().get(TileOperationStatus.DOWNLOADED))
                        .sum());
    }

    @Test
    public void cancellationWaitsForActualWorkerExitAndPreventsLateWrites() throws Exception {
        TileCancellationToken token = new TileCancellationToken();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        FileTileCache cache = cache();
        Future<List<TilePrecacheProgress>> future =
                coordinator.submit(
                        () ->
                                SegmentedTilePrecache.execute(
                                        "x",
                                        Collections.singletonList(plan(2)),
                                        GirImageMime.png,
                                        cache,
                                        (z, x, y, c) -> {
                                            entered.countDown();
                                            release.await();
                                            return new byte[] {1};
                                        },
                                        workers,
                                        token,
                                        new TilePrecacheOptions(),
                                        null));
        assertTrue(entered.await(3, TimeUnit.SECONDS));
        token.cancel();
        assertFalse(future.isDone());
        release.countDown();
        future.get(3, TimeUnit.SECONDS);
        assertNull(cache.get("x", 2, 0, 0, GirImageMime.png));
        assertNull(TileOperationContext.current());
    }

    @Test
    public void workerErrorAndRejectedSubmissionDoNotRemainRunning() {
        AtomicInteger active = new AtomicInteger();
        TilePrecacheOptions options = new TilePrecacheOptions();
        options.setSegmentSize(1);
        assertThrows(
                AssertionError.class,
                () ->
                        SegmentedTilePrecache.execute(
                                "x",
                                Collections.singletonList(plan(2)),
                                GirImageMime.png,
                                cache(),
                                (z, x, y, c) -> {
                                    active.incrementAndGet();
                                    try {
                                        throw new AssertionError("worker");
                                    } finally {
                                        active.decrementAndGet();
                                    }
                                },
                                workers,
                                new TileCancellationToken(),
                                options,
                                null));
        assertEquals(0, active.get());
        workers.shutdown();
        assertThrows(
                IllegalStateException.class,
                () ->
                        SegmentedTilePrecache.execute(
                                "x",
                                Collections.singletonList(plan(2)),
                                GirImageMime.png,
                                cache(),
                                (z, x, y, c) -> new byte[] {1},
                                workers,
                                new TileCancellationToken(),
                                options,
                                null));
    }
}
