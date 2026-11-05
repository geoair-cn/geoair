package cn.geoair.web.mime;

import static org.junit.Assert.*;

import cn.geoair.base.log.GiLogger;
import cn.geoair.web.util.GutilMimeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/** 在真实 SPI 提供者上阻塞首次加载，验证完整快照发布及并发重载的读取边界。 */
public class SpiMimeLoaderConcurrencyTest {
    private Field snapshots;
    private Object previousSnapshot;
    private GiLogger previousLogger;

    @Before
    public void resetLoader() throws Exception {
        snapshots = SpiMimeLoader.class.getDeclaredField("iMimeGetters");
        snapshots.setAccessible(true);
        previousSnapshot = snapshots.get(null);
        previousLogger = SpiMimeLoader.log;
        snapshots.set(null, null);
    }

    @After
    public void restoreLoader() throws Exception {
        SpiMimeLoader.log = previousLogger;
        snapshots.set(null, previousSnapshot);
    }

    @Test
    public void concurrentFirstReadersWaitForTheCompleteSnapshot() throws Exception {
        CountDownLatch loading = new CountDownLatch(1), release = new CountDownLatch(1);
        SpiMimeLoader.log = blockingLogger(loading, release);
        ExecutorService threads = Executors.newFixedThreadPool(33);
        try {
            Future<List<IMimeTypeGetter>> first = threads.submit(SpiMimeLoader::getMimeTypeGetters);
            assertTrue(loading.await(5, TimeUnit.SECONDS));
            CountDownLatch started = new CountDownLatch(32);
            List<Future<List<IMimeTypeGetter>>> readers = new ArrayList<>();
            for (int index = 0; index < 32; index++) readers.add(threads.submit(() -> {
                started.countDown();
                return SpiMimeLoader.getMimeTypeGetters();
            }));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertThrows("首次加载结束前不能返回半初始化列表", TimeoutException.class,
                    () -> readers.get(0).get(100, TimeUnit.MILLISECONDS));
            release.countDown();
            List<IMimeTypeGetter> complete = first.get(5, TimeUnit.SECONDS);
            assertFalse(complete.isEmpty());
            for (Future<List<IMimeTypeGetter>> reader : readers)
                assertSame(complete, reader.get(5, TimeUnit.SECONDS));
            assertKnownTypes();
        } finally { release.countDown(); stop(threads); }
    }

    @Test
    public void reloadLeavesTheOldSnapshotReadableUntilTheNewOneIsComplete() throws Exception {
        List<IMimeTypeGetter> old = SpiMimeLoader.getMimeTypeGetters();
        List<IMimeTypeGetter> oldContents = new ArrayList<>(old);
        CountDownLatch loading = new CountDownLatch(1), release = new CountDownLatch(1);
        SpiMimeLoader.log = blockingLogger(loading, release);
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            Future<?> reload = threads.submit(SpiMimeLoader::loadMimes);
            assertTrue(loading.await(5, TimeUnit.SECONDS));
            Future<List<IMimeTypeGetter>> reader = threads.submit(SpiMimeLoader::getMimeTypeGetters);
            assertSame("重载期间读者继续使用完整旧快照", old, reader.get(1, TimeUnit.SECONDS));
            assertEquals(oldContents, old);
            assertKnownTypes();
            release.countDown(); reload.get(5, TimeUnit.SECONDS);
            List<IMimeTypeGetter> replacement = SpiMimeLoader.getMimeTypeGetters();
            assertNotSame(old, replacement);
            assertEquals(providerTypes(oldContents), providerTypes(replacement));
            assertEquals(oldContents, old);
        } finally { release.countDown(); stop(threads); }
    }

    @Test
    public void failedReloadDoesNotReplaceTheWorkingSnapshot() {
        List<IMimeTypeGetter> old = SpiMimeLoader.getMimeTypeGetters();
        SpiMimeLoader.log = failingLogger();
        assertThrows(IllegalStateException.class, SpiMimeLoader::loadMimes);
        SpiMimeLoader.log = previousLogger;
        assertSame(old, SpiMimeLoader.getMimeTypeGetters());
        assertKnownTypes();
    }

    @Test
    public void failedInitialLoadRemainsUnpublishedAndCanBeRetried() throws Exception {
        SpiMimeLoader.log = failingLogger();
        assertThrows(IllegalStateException.class, SpiMimeLoader::getMimeTypeGetters);
        assertNull("失败的首次加载不能留下半初始化快照", snapshots.get(null));
        SpiMimeLoader.log = previousLogger;
        assertFalse(SpiMimeLoader.getMimeTypeGetters().isEmpty());
        assertKnownTypes();
    }

    @Test
    public void publishedSnapshotCannotBeChangedByCallers() {
        List<IMimeTypeGetter> snapshot = SpiMimeLoader.getMimeTypeGetters();
        assertFalse(snapshot.isEmpty());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(snapshot.get(0)));
        assertKnownTypes();
    }

    @Test
    public void concurrentReadersAndReloadersAlwaysRecognizeTheSameTypes() throws Exception {
        SpiMimeLoader.getMimeTypeGetters();
        ExecutorService threads = Executors.newFixedThreadPool(34);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> pending = new ArrayList<>();
        try {
            for (int reader = 0; reader < 32; reader++) pending.add(threads.submit(() -> {
                start.await();
                for (int iteration = 0; iteration < 2000; iteration++) assertKnownTypes();
                return null;
            }));
            for (int loader = 0; loader < 2; loader++) pending.add(threads.submit(() -> {
                start.await();
                for (int iteration = 0; iteration < 200; iteration++) SpiMimeLoader.loadMimes();
                return null;
            }));
            start.countDown();
            for (Future<?> task : pending) task.get(30, TimeUnit.SECONDS);
        } finally { start.countDown(); stop(threads); }
    }

    private List<Class<?>> providerTypes(List<IMimeTypeGetter> providers) {
        List<Class<?>> types = new ArrayList<>();
        for (IMimeTypeGetter provider : providers) types.add(provider.getClass());
        return types;
    }

    private void assertKnownTypes() {
        assertEquals("image/png", GutilMimeType.fromContentType("image/png").getMimeType());
        assertEquals("image/jpeg", GutilMimeType.fromExtension("jpg").getMimeType());
        assertEquals("application/json", GutilMimeType.fromContentType("application/json;charset=UTF-8").getMimeType());
    }

    private GiLogger blockingLogger(CountDownLatch loading, CountDownLatch release) {
        AtomicBoolean first = new AtomicBoolean(true);
        return (GiLogger) Proxy.newProxyInstance(GiLogger.class.getClassLoader(), new Class<?>[] {GiLogger.class},
                (proxy, method, args) -> {
                    if (isProviderTrace(method.getName(), args) && first.compareAndSet(true, false)) {
                        loading.countDown();
                        if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("等待测试释放加载线程超时");
                    }
                    return method.getReturnType() == boolean.class ? Boolean.FALSE : null;
                });
    }

    private GiLogger failingLogger() {
        return (GiLogger) Proxy.newProxyInstance(GiLogger.class.getClassLoader(), new Class<?>[] {GiLogger.class},
                (proxy, method, args) -> {
                    if (isProviderTrace(method.getName(), args)) throw new IllegalStateException("模拟加载中途失败");
                    return method.getReturnType() == boolean.class ? Boolean.FALSE : null;
                });
    }

    private boolean isProviderTrace(String method, Object[] args) {
        return "trace".equals(method) && args != null && args.length > 0
                && String.valueOf(args[0]).startsWith("通过Java SPI");
    }

    private void stop(ExecutorService threads) throws InterruptedException {
        threads.shutdownNow();
        assertTrue(threads.awaitTermination(5, TimeUnit.SECONDS));
    }
}
