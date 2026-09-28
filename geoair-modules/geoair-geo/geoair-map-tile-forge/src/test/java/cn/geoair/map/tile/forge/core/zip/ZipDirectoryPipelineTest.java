package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.EocdInfo;
import org.junit.Test;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class ZipDirectoryPipelineTest {
    private static final EocdInfo EOCD = new EocdInfo(0, 0, 2000, 2000, 0, 0, 0);

    @Test(timeout = 10000)
    public void overlapsProductionWithOrderedConsumptionAndWaitsForCompletion() throws Exception {
        Producer handler = new Producer();
        Thread caller = Thread.currentThread();
        final long[] count = {0};
        handler.scanAllEntries(EOCD, "test", (entry, total, index) -> {
            assertSame(caller, Thread.currentThread());
            assertNotSame(caller, handler.worker);
            assertEquals(count[0]++, index.longValue());
            assertEquals(index.toString(), entry.getName());
            if (index == 0) await(handler.queueFull);
            return true;
        });
        assertEquals(2000, count[0]);
        assertFalse(handler.worker.isAlive());
    }

    @Test(timeout = 10000)
    public void cancelsProducerBlockedOnFullQueueWhenConsumerStops() throws Exception {
        Producer handler = new Producer();
        handler.scanAllEntries(EOCD, "test", (entry, total, index) -> {
            await(handler.queueFull);
            return false;
        });
        assertFalse(handler.worker.isAlive());
    }

    @Test(timeout = 10000)
    public void propagatesConsumerFailureAndStopsProducer() throws Exception {
        Producer handler = new Producer();
        try {
            handler.scanAllEntries(EOCD, "test", (entry, total, index) -> {
                await(handler.queueFull);
                throw new IllegalStateException("insert failed");
            });
            fail();
        } catch (IllegalStateException expected) {
            assertEquals("insert failed", expected.getMessage());
        }
        assertFalse(handler.worker.isAlive());
    }

    @Test(timeout = 10000)
    public void preservesCallerInterruptionAndStopsProducer() throws Exception {
        Producer handler = new Producer();
        try {
            handler.scanAllEntries(EOCD, "test", (entry, total, index) -> {
                Thread.currentThread().interrupt();
                return true;
            });
            fail();
        } catch (java.io.InterruptedIOException expected) {
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        assertFalse(handler.worker.isAlive());
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue("Producer must progress while callback is running", latch.await(3, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static class Producer extends AbstractZipCompressionHandler {
        final CountDownLatch queueFull = new CountDownLatch(1);
        volatile Thread worker;
        @Override
        void scanDirectoryEntries(EocdInfo e, String source, TerminatingConsumer<CentralDirectoryModel> consumer) {
            worker = Thread.currentThread();
            for (long i = 0; i < 2000; i++) {
                if (i == 513) queueFull.countDown();
                CentralDirectoryModel entry = new CentralDirectoryModel();
                entry.setName(Long.toString(i));
                if (!consumer.accept(entry, 2000L, i)) return;
            }
        }
        public long getFileSize(String source) { return 0; }
        protected byte[] readRange(String source, long start, long end) throws IOException { throw new IOException("unused"); }
    }
}
