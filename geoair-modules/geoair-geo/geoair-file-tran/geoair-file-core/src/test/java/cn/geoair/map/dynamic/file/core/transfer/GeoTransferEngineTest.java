package cn.geoair.map.dynamic.file.core.transfer;

import cn.geoair.base.data.page.support.GirPageParam;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;
import org.geotools.api.feature.simple.SimpleFeatureType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** Core V2 无状态传输引擎测试。 */
public class GeoTransferEngineTest {

    @Test
    public void shouldFallbackToSingleRowWhenBatchWriteFails() {
        FakeReader reader = new FakeReader(rows(1, 2, 3));
        FakeWriter writer = new FakeWriter(2);
        GeoTransferResult result = GeoTransferEngine.create().execute(new GeoTransferRequest()
                .setReader(reader)
                .setWriter(writer)
                .setOptions(new GeoTransferOptions()
                        .setBatchSize(3)
                        .setErrorPolicy(ErrorPolicy.SKIP_RECORD)));

        assertEquals(3, result.getTotalCount());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals(2, writer.rows.size());
    }

    private static List<GirAdvOneRow> rows(int... ids) {
        List<GirAdvOneRow> rows = new ArrayList<>();
        for (int id : ids) {
            java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("id", id);
            rows.add(GirAdvOneRow.ofByMap(row));
        }
        return rows;
    }

    private static class FakeReader implements GeoFileReader {
        private final List<GirAdvOneRow> rows;

        private FakeReader(List<GirAdvOneRow> rows) {
            this.rows = rows;
        }

        @Override public void setLinkInfo(LinkInfo linkInfo) { }
        @Override public long getFeatureCount() { return rows.size(); }
        @Override public SimpleFeatureType readHeader(ExceptionConsumer exceptionConsumer) {
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName("row");
            builder.add("id", Integer.class);
            return builder.buildFeatureType();
        }
        @Override public GirAdvOneRow readNextRow(ExceptionConsumer exceptionConsumer) { return null; }
        @Override public Iterator<GirAdvOneRow> readRowIterator(ExceptionConsumer exceptionConsumer) { return rows.iterator(); }
        @Override public GirPager<GirAdvOneRow> readRowPage(GirPageParam page, ExceptionConsumer exceptionConsumer) {
            int from = Math.min((page.getPageNum() - 1) * page.getPageSize(), rows.size());
            int to = Math.min(from + page.getPageSize(), rows.size());
            GirPager<GirAdvOneRow> pager = new GirPager<>();
            pager.put(rows.subList(from, to), rows.size(), page);
            return pager;
        }
        @Override public void close() { }
    }

    private static class FakeWriter implements GeoFileWriter {
        private final int failingId;
        private final List<GirAdvOneRow> rows = new ArrayList<>();

        private FakeWriter(int failingId) {
            this.failingId = failingId;
        }

        @Override public void setLinkInfo(LinkInfo linkInfo) { }
        @Override public void setWriteConfig(WriteConfig writeConfig) { }
        @Override public GeoFileWriter writeHeader(SimpleFeatureType featureType, ExceptionConsumer exceptionConsumer) { return this; }
        @Override public GeoFileWriter writeOneRow(GirAdvOneRow row, ExceptionConsumer exceptionConsumer) {
            if (row.getInt("id") == failingId) {
                throw new IllegalArgumentException("bad row");
            }
            rows.add(row);
            return this;
        }
        @Override public GeoFileWriter writeRows(List<GirAdvOneRow> rows, ExceptionConsumer exceptionConsumer) {
            throw new IllegalStateException("simulate batch failure");
        }
        @Override public void close() { }
    }
}
