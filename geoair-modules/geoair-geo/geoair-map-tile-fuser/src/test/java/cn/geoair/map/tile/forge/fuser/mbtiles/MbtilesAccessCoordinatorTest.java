package cn.geoair.map.tile.forge.fuser.mbtiles;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** 不使用长休眠推测时序，协调租约必须在真实收尾后归还。 */
public class MbtilesAccessCoordinatorTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private Path directory;

    @Before
    public void prepare() {
        directory = folder.getRoot().toPath();
    }

    @Test
    public void existingTaskCanFinishWhileMaintenanceWaitsAndOtherFilesAreUnaffected()
            throws Exception {
        Path file = Files.createFile(directory.resolve("cache.mbtiles")),
                other = Files.createFile(directory.resolve("other.mbtiles"));
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        MbtilesAccessLease task = MbtilesAccessCoordinator.task(file, () -> false);
        try {
            Future<?> maintenance =
                    workers.submit(
                            () -> {
                                try (MbtilesAccessLease ignored =
                                        MbtilesAccessCoordinator.maintenance(file, () -> false)) {
                                    entered.countDown();
                                    release.await(5, TimeUnit.SECONDS);
                                } catch (Exception error) {
                                    throw new RuntimeException(error);
                                }
                            });
            try (MbtilesAccessLease operation = MbtilesAccessCoordinator.operation(file);
                    MbtilesAccessLease unaffected = MbtilesAccessCoordinator.operation(other)) {
                assertNotNull(operation);
            }
            assertFalse(entered.await(100, TimeUnit.MILLISECONDS));
            task.close();
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertThrows(
                    MbtilesMaintenanceException.class,
                    () -> MbtilesAccessCoordinator.operation(file));
            release.countDown();
            maintenance.get(5, TimeUnit.SECONDS);
            try (MbtilesAccessLease operation = MbtilesAccessCoordinator.operation(file)) {
                assertNotNull(operation);
            }
        } finally {
            task.close();
            release.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    public void cancelledMaintenanceDoesNotLeaveFilePermanentlyBlocked() throws Exception {
        Path file = Files.createFile(directory.resolve("cache.mbtiles"));
        AtomicBoolean cancelled = new AtomicBoolean();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (MbtilesAccessLease task = MbtilesAccessCoordinator.task(file, () -> false)) {
            Future<?> waiting =
                    worker.submit(
                            () -> {
                                try {
                                    MbtilesAccessCoordinator.maintenance(file, cancelled::get)
                                            .close();
                                    fail("应当取消");
                                } catch (InterruptedException expected) {
                                } catch (Exception error) {
                                    throw new RuntimeException(error);
                                }
                            });
            cancelled.set(true);
            waiting.get(5, TimeUnit.SECONDS);
        } finally {
            worker.shutdownNow();
        }
        try (MbtilesAccessLease operation = MbtilesAccessCoordinator.operation(file)) {
            assertNotNull(operation);
        }
    }
}
