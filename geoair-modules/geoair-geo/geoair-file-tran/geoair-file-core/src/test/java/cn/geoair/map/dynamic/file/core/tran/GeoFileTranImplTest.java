package cn.geoair.map.dynamic.file.core.tran;

import cn.geoair.map.dynamic.file.core.enums.TranStatus;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.exception.GeoFileReadException;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import cn.geoair.map.dynamic.file.core.tran.model.TranContext;
import cn.geoair.map.dynamic.file.core.tran.model.TranResult;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeatureType;

import static org.junit.jupiter.api.Assertions.*;

class GeoFileTranImplTest {

    @Test
    void readFailureIsNotSuccess() {
        GeoFileReader reader = new GeoFileReader() {
            @Override
            public void setLinkInfo(cn.geoair.map.dynamic.file.core.link.LinkInfo linkInfo) {}

            @Override
            public long getFeatureCount() {
                return 1;
            }

            @Override
            public SimpleFeatureType readHeader(ExceptionConsumer exceptionConsumer) {
                SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
                builder.setName("t");
                builder.add("name", String.class);
                return builder.buildFeatureType();
            }

            @Override
            public cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow readNextRow(ExceptionConsumer exceptionConsumer) {
                throw new GeoFileReadException("boom");
            }

            @Override
            public Iterator<cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow> readRowIterator(ExceptionConsumer exceptionConsumer) {
                return null;
            }

            @Override
            public cn.geoair.base.data.page.support.GirPager<cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow> readRowPage(cn.geoair.base.data.page.support.GirPageParam girPageParam, ExceptionConsumer exceptionConsumer) {
                return null;
            }

            @Override
            public void close() {}
        };

        GeoFileWriter writer = new NoopWriter();
        GeoFileTran tran = new GeoFileTranImpl();
        TranResult result = tran.transform(reader, writer, new TranContext().setSkipErrorRecord(false));

        assertEquals(TranStatus.FAILED, result.getStatus());
        assertTrue(result.getFailCount() >= 1);
        assertFalse(result.getExceptions().isEmpty());
    }

    @Test
    void timeoutKeepsTimeoutStatus() {
        GeoFileReader reader = new InfiniteReader();
        GeoFileWriter writer = new NoopWriter();
        GeoFileTran tran = new GeoFileTranImpl();
        TranResult result = tran.transform(reader, writer, new TranContext().setTimeout(1).setSkipErrorRecord(true));

        assertEquals(TranStatus.TIMEOUT, result.getStatus());
    }

    private static class NoopWriter implements GeoFileWriter {
        @Override
        public void setLinkInfo(cn.geoair.map.dynamic.file.core.link.LinkInfo linkInfo) {}

        @Override
        public void setWriteConfig(cn.geoair.map.dynamic.file.core.write.config.WriteConfig writeConfig) {}

        @Override
        public GeoFileWriter writeHeader(SimpleFeatureType featureType, ExceptionConsumer exceptionConsumer) {
            return this;
        }

        @Override
        public GeoFileWriter writeOneRow(cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow girAdvOneRow, ExceptionConsumer exceptionConsumer) {
            return this;
        }

        @Override
        public void close() throws IOException {}
    }

    private static class InfiniteReader implements GeoFileReader {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public void setLinkInfo(cn.geoair.map.dynamic.file.core.link.LinkInfo linkInfo) {}

        @Override
        public long getFeatureCount() {
            return 1;
        }

        @Override
        public SimpleFeatureType readHeader(ExceptionConsumer exceptionConsumer) {
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName("t");
            builder.add("name", String.class);
            return builder.buildFeatureType();
        }

        @Override
        public cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow readNextRow(ExceptionConsumer exceptionConsumer) {
            if (counter.incrementAndGet() > 1000) {
                return null;
            }
            try {
                Thread.sleep(2L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("name", "x");
            return cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow.ofByMap(map);
        }

        @Override
        public Iterator<cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow> readRowIterator(ExceptionConsumer exceptionConsumer) {
            return null;
        }

        @Override
        public cn.geoair.base.data.page.support.GirPager<cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow> readRowPage(cn.geoair.base.data.page.support.GirPageParam girPageParam, ExceptionConsumer exceptionConsumer) {
            return null;
        }

        @Override
        public void close() {}
    }
}
