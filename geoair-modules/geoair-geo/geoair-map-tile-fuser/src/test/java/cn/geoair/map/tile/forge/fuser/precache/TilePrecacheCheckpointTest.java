package cn.geoair.map.tile.forge.fuser.precache;

import static org.junit.Assert.*;

import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.fuser.cache.FileTileCache;
import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;
import cn.geoair.web.mime.GirImageMime;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TilePrecacheCheckpointTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void resumesCommittedBatchesAndRetriesOnlyPersistedFailureAfterReopen()
            throws Exception {
        Path root = folder.getRoot().toPath(), file = root.resolve("检查点.sqlite");
        FileTileCache cache = new FileTileCache(root.resolve("cache").toString(), 0, true);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        TilePrecacheOptions options = options(2);
        TilePrecacheBatchGate gate = new TilePrecacheBatchGate(1);
        List<TilePrecacheLevel> plan = Collections.singletonList(plan());
        TileCancellationToken stopped = new TileCancellationToken();
        List<Integer> requested = new ArrayList<>();
        try {
            try (TilePrecacheCheckpoint checkpoint = new TilePrecacheCheckpoint(file)) {
                TilePrecacheProgress state =
                        SegmentedTilePrecache.executeResumable(
                                        "tiles",
                                        plan,
                                        GirImageMime.png,
                                        cache,
                                        (z, x, y, context) -> {
                                            requested.add(y);
                                            return y == 1 ? null : new byte[] {1};
                                        },
                                        null,
                                        workers,
                                        stopped,
                                        options,
                                        p -> {
                                            if (p.getProcessed() == 2) stopped.cancel();
                                        },
                                        gate,
                                        "source-v1/cache-v1",
                                        checkpoint,
                                        false)
                                .get(0);
                assertEquals(Arrays.asList(0, 1), requested);
                assertEquals(2, state.getProcessed());
                assertEquals(
                        TileOperationStatus.MISSING,
                        checkpoint.failures(2, -1, 10).get(0).getStatus());
                assertTrue(checkpoint.failures(2, -1, 10, TileOperationStatus.TIMED_OUT).isEmpty());
            }
            try (TilePrecacheCheckpoint checkpoint = new TilePrecacheCheckpoint(file)) {
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                SegmentedTilePrecache.executeResumable(
                                        "tiles",
                                        plan,
                                        GirImageMime.png,
                                        cache,
                                        (z, x, y, c) -> null,
                                        null,
                                        workers,
                                        new TileCancellationToken(),
                                        options,
                                        null,
                                        gate,
                                        "source-v1/cache-v2",
                                        checkpoint,
                                        false));

                requested.clear();
                TileContentLoader available =
                        (z, x, y, context) -> {
                            requested.add(y);
                            return new byte[] {1};
                        };
                TilePrecacheProgress retry =
                        SegmentedTilePrecache.executeResumable(
                                        "tiles",
                                        plan,
                                        GirImageMime.png,
                                        cache,
                                        available,
                                        null,
                                        workers,
                                        new TileCancellationToken(),
                                        options,
                                        null,
                                        gate,
                                        "source-v1/cache-v1",
                                        checkpoint,
                                        true)
                                .get(0);
                assertEquals(Collections.singletonList(1), requested);
                assertEquals(2, retry.getProcessed());
                assertTrue(checkpoint.failures(2, -1, 10).isEmpty());
                assertEquals(Long.valueOf(0), retry.getCounts().get(TileOperationStatus.MISSING));

                requested.clear();
                TilePrecacheProgress complete =
                        SegmentedTilePrecache.executeResumable(
                                        "tiles",
                                        plan,
                                        GirImageMime.png,
                                        cache,
                                        available,
                                        null,
                                        workers,
                                        new TileCancellationToken(),
                                        options,
                                        null,
                                        gate,
                                        "source-v1/cache-v1",
                                        checkpoint,
                                        false)
                                .get(0);
                assertEquals(Arrays.asList(2, 3), requested);
                assertEquals(4, complete.getProcessed());
                assertEquals(
                        Long.valueOf(4), complete.getCounts().get(TileOperationStatus.DOWNLOADED));
            }
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    public void outOfOrderCompletionSkipsSavedRangesAfterChangingBatchSize() throws Exception {
        Path root = folder.getRoot().toPath();
        FileTileCache cache = new FileTileCache(root.resolve("cache").toString(), 0, true);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch secondSaved = new CountDownLatch(1);
        List<TilePrecacheLevel> plan = Collections.singletonList(plan());
        TileCancellationToken stopped = new TileCancellationToken();
        TilePrecacheBatchGate gate = new TilePrecacheBatchGate(2);
        try (TilePrecacheCheckpoint checkpoint =
                new TilePrecacheCheckpoint(root.resolve("out-of-order.db"))) {
            TilePrecacheProgress partial =
                    SegmentedTilePrecache.executeResumable(
                                    "tiles",
                                    plan,
                                    GirImageMime.png,
                                    cache,
                                    (z, x, y, context) -> {
                                        if (y == 0)
                                            assertTrue(secondSaved.await(4, TimeUnit.SECONDS));
                                        return new byte[] {1};
                                    },
                                    null,
                                    workers,
                                    stopped,
                                    options(2),
                                    p -> {
                                        if (p.getProcessed() == 2) {
                                            stopped.cancel();
                                            secondSaved.countDown();
                                        }
                                    },
                                    gate,
                                    "source/cache",
                                    checkpoint,
                                    false)
                            .get(0);
            assertEquals(2, partial.getProcessed());
            assertEquals(0, checkpoint.completedTo(2, 0));
            assertEquals(4, checkpoint.completedTo(2, 2));

            List<Integer> resumed = new ArrayList<>();
            TilePrecacheProgress complete =
                    SegmentedTilePrecache.executeResumable(
                                    "tiles",
                                    plan,
                                    GirImageMime.png,
                                    cache,
                                    (z, x, y, context) -> {
                                        resumed.add(y);
                                        return new byte[] {2};
                                    },
                                    null,
                                    workers,
                                    new TileCancellationToken(),
                                    options(4),
                                    null,
                                    gate,
                                    "source/cache",
                                    checkpoint,
                                    false)
                            .get(0);
            assertEquals(Arrays.asList(0, 1), resumed);
            assertEquals(4, complete.getProcessed());
            assertEquals(4, checkpoint.completedTo(2, 0));
        } finally {
            secondSaved.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    public void theSameCheckpointAllowsOnlyOneExecutor() throws Exception {
        Path file = folder.getRoot().toPath().resolve("shared.db");
        try (TilePrecacheCheckpoint first = new TilePrecacheCheckpoint(file);
                TilePrecacheCheckpoint second = new TilePrecacheCheckpoint(file)) {
            first.begin("plan", false);
            try {
                assertThrows(IllegalStateException.class, () -> second.begin("plan", false));
            } finally {
                first.end();
            }
            second.begin("plan", false);
            second.end();
        }
    }

    @Test
    public void twoTasksSharingOneWorkerGetRoundRobinBatchSlots() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(1);
        ExecutorService coordinators = Executors.newFixedThreadPool(2);
        TilePrecacheBatchGate gate = new TilePrecacheBatchGate(1);
        FileTileCache cache = new FileTileCache(folder.getRoot().toString(), 0, true);
        List<String> actual = new ArrayList<>();
        CountDownLatch firstEntered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicReference<Thread> secondCoordinator = new AtomicReference<>();
        try {
            Future<?> a =
                    coordinators.submit(
                            () ->
                                    SegmentedTilePrecache.executeResumable(
                                            "a",
                                            Collections.singletonList(plan()),
                                            GirImageMime.png,
                                            cache,
                                            (z, x, y, context) -> {
                                                actual.add("a" + y);
                                                if (y == 0) {
                                                    firstEntered.countDown();
                                                    assertTrue(release.await(4, TimeUnit.SECONDS));
                                                }
                                                return new byte[] {1};
                                            },
                                            null,
                                            workers,
                                            new TileCancellationToken(),
                                            options(1),
                                            null,
                                            gate,
                                            "a",
                                            null,
                                            false));
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            Future<?> b =
                    coordinators.submit(
                            () -> {
                                secondCoordinator.set(Thread.currentThread());
                                return SegmentedTilePrecache.executeResumable(
                                        "b",
                                        Collections.singletonList(plan()),
                                        GirImageMime.png,
                                        cache,
                                        (z, x, y, context) -> {
                                            actual.add("b" + y);
                                            return new byte[] {2};
                                        },
                                        null,
                                        workers,
                                        new TileCancellationToken(),
                                        options(1),
                                        null,
                                        gate,
                                        "b",
                                        null,
                                        false);
                            });
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while ((secondCoordinator.get() == null
                            || secondCoordinator.get().getState() != Thread.State.TIMED_WAITING)
                    && System.nanoTime() < deadline) Thread.sleep(5);
            assertNotNull(secondCoordinator.get());
            assertEquals(Thread.State.TIMED_WAITING, secondCoordinator.get().getState());
            release.countDown();
            a.get(5, TimeUnit.SECONDS);
            b.get(5, TimeUnit.SECONDS);
            assertEquals(Arrays.asList("a0", "b0"), actual.subList(0, 2));
            assertEquals(8, actual.size());
        } finally {
            release.countDown();
            coordinators.shutdownNow();
            workers.shutdownNow();
        }
    }

    private TilePrecacheOptions options(int batch) {
        TilePrecacheOptions options = new TilePrecacheOptions();
        options.setBatchSize(batch);
        options.setPendingSegments(8);
        return options;
    }

    private TilePrecacheLevel plan() {
        return new TilePrecacheLevel(2, new RangeApo(0, 0, 0, 3, 2), null);
    }
}
