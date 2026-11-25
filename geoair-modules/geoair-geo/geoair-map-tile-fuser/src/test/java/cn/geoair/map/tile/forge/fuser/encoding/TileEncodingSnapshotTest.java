package cn.geoair.map.tile.forge.fuser.encoding;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** 快照与线程池作用域测试，不依赖业务图层名、Spring容器或控制库。 */
public class TileEncodingSnapshotTest {
    @Test
    public void snapshotCopiesDefaultsOverridesAndReturnedValues() {
        TileEncodingPolicy original = new TileEncodingPolicy("JPEG", 65);
        TileEncodingSnapshot snapshot =
                new TileEncodingSnapshot(original, new TileEncodingPolicy("PNG", 75));
        original.setQuality(95);
        assertEquals(65, snapshot.getOriginal().getQuality());
        snapshot.getOriginal().setQuality(1);
        assertEquals(65, snapshot.policy(TileEncodingTarget.ORIGINAL).getQuality());
        TileEncodingSnapshot overridden =
                snapshot.override(TileEncodingTarget.OUTPUT, new TileEncodingPolicy("JPEG", 40));
        assertEquals(65, overridden.getOriginal().getQuality());
        assertEquals(40, overridden.getOutput().getQuality());
        assertEquals("PNG", snapshot.getOutput().getMode());
        assertEquals("LEGACY", new TileEncodingSnapshot(null, null).getOriginal().getMode());
        assertThrows(IllegalArgumentException.class, () -> snapshot.override(null, original));
        assertThrows(IllegalArgumentException.class, () -> snapshot.bindings("same", "same"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        snapshot.override(
                                TileEncodingTarget.OUTPUT, new TileEncodingPolicy("JPEG", 101)));
    }

    @Test
    public void nestedScopesRestorePreviousValuesOnException() {
        TileEncodingContext outer =
                TileEncodingContext.empty()
                        .with("explicit-name", new TileEncodingPolicy("PNG", 75));
        assertNull(TileEncodingContext.capture().policy("explicit-name"));
        try (TileEncodingScope first = outer.bind()) {
            assertEquals("PNG", TileEncodingContext.capture().policy("explicit-name").getMode());
            try {
                try (TileEncodingScope second =
                        TileEncodingContext.empty()
                                .with("explicit-name", new TileEncodingPolicy("JPEG", 50))
                                .bind()) {
                    assertEquals(
                            50, TileEncodingContext.capture().policy("explicit-name").getQuality());
                    throw new IllegalStateException("模拟接入回调失败");
                }
            } catch (IllegalStateException expected) {
            }
            assertEquals("PNG", TileEncodingContext.capture().policy("explicit-name").getMode());
        }
        assertNull(TileEncodingContext.capture().policy("explicit-name"));
    }

    @Test
    public void capturedContextCrossesWorkersAndDoesNotLeakIntoNextWorkItem() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            TileEncodingContext captured;
            try (TileEncodingScope ignored =
                    TileEncodingContext.empty()
                            .with("src", new TileEncodingPolicy("JPEG", 60))
                            .bind()) {
                captured = TileEncodingContext.capture();
                assertNull(pool.submit(() -> TileEncodingContext.capture().policy("src")).get());
            }
            assertEquals(
                    60,
                    (int)
                            pool.submit(
                                            captured.wrap(
                                                    () ->
                                                            TileEncodingContext.capture()
                                                                    .policy("src")
                                                                    .getQuality()))
                                    .get());
            Future<?> failed =
                    pool.submit(
                            captured.wrap(
                                    (Runnable)
                                            () -> {
                                                throw new IllegalStateException("模拟工作失败");
                                            }));
            assertThrows(ExecutionException.class, failed::get);
            assertNull(pool.submit(() -> TileEncodingContext.capture().policy("src")).get());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void scopesRejectWrongThreadAndOutOfOrderClose() throws Exception {
        TileEncodingScope first =
                TileEncodingContext.empty().with("src", new TileEncodingPolicy()).bind();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            ExecutionException error =
                    assertThrows(
                            ExecutionException.class, () -> pool.submit(() -> first.close()).get());
            assertTrue(error.getCause() instanceof IllegalStateException);
            TileEncodingScope second = TileEncodingContext.empty().bind();
            try {
                assertThrows(IllegalStateException.class, first::close);
            } finally {
                second.close();
            }
        } finally {
            first.close();
            first.close();
            pool.shutdownNow();
        }
        assertNull(TileEncodingContext.capture().policy("src"));
    }
}
