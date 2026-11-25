package cn.geoair.map.tile.forge.fuser.mbtiles;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MbtilesWriteCoordinatorTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Path directory;

    @Before
    public void configureDirectory() {
        directory = temporary.getRoot().toPath();
    }

    @Test
    public void waitingTileWriterGoesBeforeWaitingStatistics() throws Exception {
        Path file = directory.resolve("priority.mbtiles");
        ExecutorService threads = Executors.newFixedThreadPool(2);
        List<String> order = new CopyOnWriteArrayList<>();
        CountDownLatch statisticsWaiting = new CountDownLatch(1);
        try {
            Future<?> writer, scan;
            try (MbtilesWriteLease initial = MbtilesWriteCoordinator.statistics(file)) {
                scan =
                        threads.submit(
                                () -> {
                                    statisticsWaiting.countDown();
                                    try (MbtilesWriteLease ignored =
                                            MbtilesWriteCoordinator.statistics(file)) {
                                        order.add("statistics");
                                    }
                                    return null;
                                });
                assertTrue(statisticsWaiting.await(2, TimeUnit.SECONDS));
                writer =
                        threads.submit(
                                () -> {
                                    try (MbtilesWriteLease ignored =
                                            MbtilesWriteCoordinator.write(file)) {
                                        order.add("writer");
                                    }
                                    return null;
                                });
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                while (!initial.hasWaitingWrites() && System.nanoTime() < deadline) Thread.yield();
                assertTrue(initial.hasWaitingWrites());
            }
            writer.get(2, TimeUnit.SECONDS);
            scan.get(2, TimeUnit.SECONDS);
            assertEquals("writer", order.get(0));
            assertEquals("statistics", order.get(1));
        } finally {
            threads.shutdownNow();
            assertTrue(threads.awaitTermination(2, TimeUnit.SECONDS));
        }
    }

    @Test
    public void cancellingAQueuedWriterDoesNotLeaveStatisticsWaiting() throws Exception {
        Path file = directory.resolve("cancel.mbtiles");
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            Future<?> writer, scan;
            try (MbtilesWriteLease initial = MbtilesWriteCoordinator.statistics(file)) {
                writer =
                        threads.submit(
                                () -> {
                                    try (MbtilesWriteLease ignored =
                                            MbtilesWriteCoordinator.write(file)) {}
                                    return null;
                                });
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                while (!initial.hasWaitingWrites() && System.nanoTime() < deadline) Thread.yield();
                assertTrue(initial.hasWaitingWrites());
                scan =
                        threads.submit(
                                () -> {
                                    try (MbtilesWriteLease ignored =
                                            MbtilesWriteCoordinator.statistics(file)) {}
                                    return null;
                                });
                assertTrue(writer.cancel(true));
            }
            scan.get(2, TimeUnit.SECONDS);
            try (MbtilesWriteLease ignored =
                    MbtilesWriteCoordinator.write(directory.resolve("other.mbtiles"))) {}
        } finally {
            threads.shutdownNow();
            assertTrue(threads.awaitTermination(2, TimeUnit.SECONDS));
        }
    }
}
