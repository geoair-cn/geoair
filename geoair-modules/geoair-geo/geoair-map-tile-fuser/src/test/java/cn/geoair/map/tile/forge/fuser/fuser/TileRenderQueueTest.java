package cn.geoair.map.tile.forge.fuser.fuser;

import static org.junit.Assert.*;

import cn.geoair.map.tile.forge.fuser.cache.TileCacheGeneration;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TileRenderQueueTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void coalescesSameTileAndCancellationReleasesOnlyItsSubscriber() throws Exception {
        CountDownLatch hold = new CountDownLatch(1), started = new CountDownLatch(1);
        AtomicInteger renders = new AtomicInteger();
        try (TileRenderQueue<String, Integer> queue =
                new TileRenderQueue<>(1, 1, 2, Executors.defaultThreadFactory())) {
            CompletableFuture<Integer> first =
                    queue.submit(
                            "tile",
                            () -> {
                                renders.incrementAndGet();
                                started.countDown();
                                assertTrue(hold.await(4, TimeUnit.SECONDS));
                                return 7;
                            });
            assertTrue(started.await(2, TimeUnit.SECONDS));
            CompletableFuture<Integer> second = queue.submit("tile", renders::incrementAndGet);
            ExecutionException rejected =
                    assertThrows(
                            ExecutionException.class,
                            () -> queue.submit("other", () -> 8).get(2, TimeUnit.SECONDS));
            assertTrue(rejected.getCause() instanceof RejectedExecutionException);
            for (int i = 0; i < 100; i++) {
                assertTrue(second.cancel(false));
                second = queue.submit("tile", renders::incrementAndGet);
            }
            hold.countDown();
            assertEquals(Integer.valueOf(7), first.get(2, TimeUnit.SECONDS));
            assertEquals(Integer.valueOf(7), second.get(2, TimeUnit.SECONDS));
            assertEquals(1, renders.get());
        } finally {
            hold.countDown();
        }
    }

    @Test
    public void failedKeyCanRetryAndOldGenerationCannotWriteAfterClear() throws Exception {
        Path pointer = folder.getRoot().toPath().resolve("current-generation");
        TileCacheGeneration cache = new TileCacheGeneration(pointer);
        long oldGeneration = cache.current();
        AtomicInteger writes = new AtomicInteger();
        CountDownLatch waitToWrite = new CountDownLatch(1);
        try (TileRenderQueue<String, Boolean> queue =
                new TileRenderQueue<>(2, 2, 4, Executors.defaultThreadFactory())) {
            CompletableFuture<Boolean> late =
                    queue.submit(
                            oldGeneration + "/zxy",
                            () -> {
                                assertTrue(waitToWrite.await(4, TimeUnit.SECONDS));
                                return cache.writeIfCurrent(oldGeneration, writes::incrementAndGet);
                            });
            long newGeneration = cache.advance();
            waitToWrite.countDown();
            assertFalse(late.get(2, TimeUnit.SECONDS));
            String key = newGeneration + "/zxy";
            assertThrows(
                    ExecutionException.class,
                    () ->
                            queue.submit(
                                            key,
                                            () -> {
                                                throw new IllegalStateException("读取失败");
                                            })
                                    .get(2, TimeUnit.SECONDS));
            assertTrue(
                    queue.submit(
                                    key,
                                    () ->
                                            cache.writeIfCurrent(
                                                    newGeneration, writes::incrementAndGet))
                            .get(2, TimeUnit.SECONDS));
            assertEquals(1, writes.get());
            assertEquals(newGeneration, new TileCacheGeneration(pointer).current());
        } finally {
            waitToWrite.countDown();
        }
    }
}
