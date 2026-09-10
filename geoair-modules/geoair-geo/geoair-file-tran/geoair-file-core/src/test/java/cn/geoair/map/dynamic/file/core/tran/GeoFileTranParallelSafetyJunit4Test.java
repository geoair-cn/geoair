package cn.geoair.map.dynamic.file.core.tran;

import cn.geoair.base.data.page.support.GirPageParam;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.enums.TranStatus;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.exception.GeoFileWriteException;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import cn.geoair.map.dynamic.file.core.tran.model.TranContext;
import cn.geoair.map.dynamic.file.core.tran.model.TranResult;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;
import org.geotools.api.feature.simple.SimpleFeatureType;

import static org.junit.Assert.assertEquals;

/** GeoFileTran 并发读写协商与异常收集的回归测试。 */
public class GeoFileTranParallelSafetyJunit4Test {

    @Test
    public void writerWithoutParallelCapabilityIsExecutedSequentially() {
        ConcurrentDetectWriter writer = new ConcurrentDetectWriter();
        TranResult result = new GeoFileTranImpl().transform(
                new ParallelPagedReader(), writer, new TranContext().setBatchSize(1));

        assertEquals(TranStatus.SUCCESS, result.getStatus());
        assertEquals(1, writer.maxConcurrency.get());
    }

    @Test
    public void concurrentWriteFailuresAreCollectedSafely() {
        TranResult result = new GeoFileTranImpl().transform(
                new ParallelPagedReader(), new FailingParallelWriter(),
                new TranContext().setBatchSize(1).setSkipErrorRecord(true));

        assertEquals(8L, result.getFailCount());
        assertEquals(8, result.getExceptions().size());
    }

    private static class ParallelPagedReader implements GeoFileReader {
        @Override
        public void setLinkInfo(cn.geoair.map.dynamic.file.core.link.LinkInfo linkInfo) {
        }

        @Override
        public boolean supportParallelPageRead() {
            return true;
        }

        @Override
        public long getFeatureCount() {
            return 8;
        }

        @Override
        public SimpleFeatureType readHeader(ExceptionConsumer exceptionConsumer) {
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName("test");
            builder.add("name", String.class);
            return builder.buildFeatureType();
        }

        @Override
        public GirAdvOneRow readNextRow(ExceptionConsumer exceptionConsumer) {
            return null;
        }

        @Override
        public Iterator<GirAdvOneRow> readRowIterator(ExceptionConsumer exceptionConsumer) {
            return null;
        }

        @Override
        public GirPager<GirAdvOneRow> readRowPage(GirPageParam pageParam, ExceptionConsumer exceptionConsumer) {
            GirPager<GirAdvOneRow> pager = new GirPager<>();
            GirAdvOneRow row = GirAdvOneRow.ofByMap(
                    Collections.<String, Object>singletonMap("name", "test"));
            pager.put(Collections.singletonList(row), getFeatureCount(), pageParam);
            return pager;
        }

        @Override
        public void close() {
        }
    }

    private static class ConcurrentDetectWriter implements GeoFileWriter {
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger maxConcurrency = new AtomicInteger();

        @Override
        public void setLinkInfo(cn.geoair.map.dynamic.file.core.link.LinkInfo linkInfo) {
        }

        @Override
        public void setWriteConfig(WriteConfig writeConfig) {
        }

        @Override
        public GeoFileWriter writeHeader(SimpleFeatureType featureType, ExceptionConsumer exceptionConsumer) {
            return this;
        }

        @Override
        public GeoFileWriter writeOneRow(GirAdvOneRow row, ExceptionConsumer exceptionConsumer) {
            return this;
        }

        @Override
        public GeoFileWriter writeRows(List<GirAdvOneRow> rows, ExceptionConsumer exceptionConsumer) {
            int current = active.incrementAndGet();
            maxConcurrency.accumulateAndGet(current, Math::max);
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } finally {
                active.decrementAndGet();
            }
            return this;
        }

        @Override
        public void close() throws IOException {
        }
    }

    private static class FailingParallelWriter extends ConcurrentDetectWriter {
        @Override
        public boolean supportParallelPageWrite() {
            return true;
        }

        @Override
        public GeoFileWriter writeRows(List<GirAdvOneRow> rows, ExceptionConsumer exceptionConsumer) {
            throw new GeoFileWriteException("simulated write failure");
        }
    }
}
