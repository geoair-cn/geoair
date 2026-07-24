package cn.geoair.map.dynamic.file.csv;

import cn.geoair.base.data.page.support.GirPageParam;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.exception.GeoFileReadException;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.locationtech.jts.geom.Geometry;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

public class CsvGeoFileReader implements GeoFileReader {

    private CsvLinkInfo linkInfo;

    private List<String> headers = new ArrayList<>();

    private BufferedReader reader;

    private SimpleFeatureType featureType;

    private final AtomicLong currentRow = new AtomicLong(0);

    private long totalCount = 0;

    @Override
    public void setLinkInfo(LinkInfo linkInfo) {
        if (!(linkInfo instanceof CsvLinkInfo)) {
            throw new IllegalArgumentException("链接信息必须是 CsvLinkInfo 类型");
        }
        this.linkInfo = (CsvLinkInfo) linkInfo;
        this.linkInfo.checkLinkInfo();
        initReader();
    }

    @Override
    public long getFeatureCount() {
        return totalCount;
    }

    private void initReader() {
        try {
            close();
            reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(linkInfo.getCsvFilePath()),
                                    Charset.forName(linkInfo.getCharset())));
            if (linkInfo.isHasHeader()) {
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new GeoFileReadException("CSV 文件为空");
                }
                headers =
                        CsvSchemaSupport.resolveHeaders(
                                CsvSchemaSupport.parseLine(headerLine, linkInfo.getDelimiter()));
            }
            if (headers.isEmpty()) {
                throw new GeoFileReadException("CSV 未提供表头，无法构建结构");
            }
            featureType = CsvSchemaSupport.buildFeatureType(headers, linkInfo);
            totalCount = countRemainingRows();
            resetReaderBody();
        } catch (Exception e) {
            throw new GeoFileReadException("初始化 CSV 读取器失败", e);
        }
    }

    @Override
    public SimpleFeatureType readHeader(ExceptionConsumer exceptionConsumer) {
        return featureType;
    }

    @Override
    public GirAdvOneRow readOneRow(ExceptionConsumer exceptionConsumer) {
        try {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }
            List<String> values = CsvSchemaSupport.parseLine(line, linkInfo.getDelimiter());
            GirAdvOneRow row = GirAdvOneRow.ofByMap(new LinkedHashMap<>());
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                String value = i < values.size() ? values.get(i) : null;
                row.put(header, value);
            }
            Geometry geometry =
                    CsvGeometrySupport.readGeometry(
                            linkInfo, headers, values.toArray(new String[0]));
            if (geometry != null) {
                row.put(linkInfo.getGeometryColumnName(), geometry);
            }
            currentRow.incrementAndGet();
            return row;
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("读取 CSV 单行数据失败", e);
        }
    }

    @Override
    public Iterator<GirAdvOneRow> readRowIterator(ExceptionConsumer exceptionConsumer) {
        resetReaderBody();
        return new Iterator<GirAdvOneRow>() {
            private boolean closed = false;

            @Override
            public boolean hasNext() {
                if (closed) {
                    return false;
                }
                try {
                    reader.mark(1);
                    int c = reader.read();
                    if (c < 0) {
                        closeIterator();
                        return false;
                    }
                    reader.reset();
                    return true;
                } catch (Exception e) {
                    notifyException(exceptionConsumer, e);
                    closeIterator();
                    throw new GeoFileReadException("检查 CSV 是否有下一条数据失败", e);
                }
            }

            @Override
            public GirAdvOneRow next() {
                if (closed) {
                    throw new NoSuchElementException("迭代器已关闭");
                }
                if (!hasNext()) {
                    closeIterator();
                    throw new NoSuchElementException("无更多数据");
                }
                return readOneRow(exceptionConsumer);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("CSV 只读");
            }

            private void closeIterator() {
                if (!closed) {
                    closed = true;
                    CsvGeoFileReader.this.close();
                }
            }
        };
    }

    @Override
    public GirPager<GirAdvOneRow> readRowPage(
            GirPageParam girPageParam, ExceptionConsumer exceptionConsumer) {
        GirPager<GirAdvOneRow> pager = new GirPager<>();
        try {
            if (girPageParam == null) {
                throw new IllegalArgumentException("分页参数不能为空");
            }
            int pageNum = girPageParam.getPageNum();
            int pageSize = girPageParam.getPageSize();
            int start = (pageNum - 1) * pageSize;
            int end = start + pageSize;

            resetReaderBody();
            List<GirAdvOneRow> list = new ArrayList<>();
            int index = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (index++ < start) {
                    continue;
                }
                if (index > end) {
                    break;
                }
                List<String> values = CsvSchemaSupport.parseLine(line, linkInfo.getDelimiter());
                GirAdvOneRow row = GirAdvOneRow.ofByMap(new LinkedHashMap<>());
                for (int i = 0; i < headers.size(); i++) {
                    row.put(headers.get(i), i < values.size() ? values.get(i) : null);
                }
                Geometry geometry =
                        CsvGeometrySupport.readGeometry(
                                linkInfo, headers, values.toArray(new String[0]));
                if (geometry != null) {
                    row.put(linkInfo.getGeometryColumnName(), geometry);
                }
                list.add(row);
            }
            pager.put(list, totalCount, girPageParam);
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("读取 CSV 分页数据失败", e);
        }
        return pager;
    }

    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (Exception ignored) {
            }
        }
        reader = null;
    }

    private void resetReaderBody() {
        try {
            if (reader != null) {
                reader.close();
            }
            reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(linkInfo.getCsvFilePath()),
                                    Charset.forName(linkInfo.getCharset())));
            if (linkInfo.isHasHeader()) {
                reader.readLine();
            }
            currentRow.set(0);
        } catch (Exception e) {
            throw new GeoFileReadException("重置 CSV 读取器失败", e);
        }
    }

    private long countRemainingRows() throws Exception {
        long count = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void notifyException(ExceptionConsumer exceptionConsumer, Exception e) {
        if (exceptionConsumer != null) {
            exceptionConsumer.accept(e);
        }
    }
}
