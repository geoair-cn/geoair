package cn.geoair.map.dynamic.file.geojson;

import cn.geoair.base.data.page.support.GirPageParam;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.exception.GeoFileReadException;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;

import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

public class GeoJsonGeoFileReader implements GeoFileReader {

    private GeoJsonLinkInfo linkInfo;

    private FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;

    private FeatureIterator<SimpleFeature> featureIterator;

    private SimpleFeatureType featureType;

    private AtomicInteger currentRow = new AtomicInteger(0);

    @Override
    public void setLinkInfo(LinkInfo linkInfo) {
        if (!(linkInfo instanceof GeoJsonLinkInfo)) {
            throw new IllegalArgumentException("链接信息必须是 GeoJsonLinkInfo 类型");
        }
        this.linkInfo = (GeoJsonLinkInfo) linkInfo;
        this.linkInfo.checkLinkInfo();
        initGeoJsonReader();
    }

    @Override
    public long getFeatureCount() {
        if (this.featureCollection == null) {
            return 0;
        }
        return this.featureCollection.size();
    }

    /** 初始化 GeoJSON 读取器 */
    private void initGeoJsonReader() {
        try (FileInputStream fis = new FileInputStream(linkInfo.getGeoJsonFilePath())) {
            GeoJSONReader geoJsonReader = new GeoJSONReader(fis);
            this.featureCollection = geoJsonReader.getFeatures();
            this.featureType = featureCollection.getSchema();
            this.featureIterator = featureCollection.features();
        } catch (Exception e) {
            throw new GeoFileReadException("初始化 GeoJSON 读取器失败", e);
        }
    }

    @Override
    public SimpleFeatureType readHeader(ExceptionConsumer exceptionConsumer) {
        try {
            return this.featureType;
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("读取 GeoJSON 表头失败", e);
        }
    }

    @Override
    public GirAdvOneRow readOneRow(ExceptionConsumer exceptionConsumer) {
        try {
            if (featureIterator == null || !featureIterator.hasNext()) {
                return null;
            }
            SimpleFeature feature = featureIterator.next();
            currentRow.incrementAndGet();
            Map<String, Object> attributes = new HashMap<>();
            for (Property property : feature.getProperties()) {
                String propName = property.getName().getLocalPart();
                Object propValue = property.getValue();
                if (propValue instanceof Geometry) {
                    Geometry geometry = (Geometry) propValue;
                    geometry.setSRID(linkInfo.getSrid());
                    attributes.put(propName, geometry);
                } else {
                    attributes.put(propName, propValue);
                }
            }
            return GirAdvOneRow.ofByMap(attributes);
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("读取 GeoJSON 单行数据失败", e);
        }
    }

    @Override
    public Iterator<GirAdvOneRow> readRowIterator(ExceptionConsumer exceptionConsumer) {
        resetIterator();

        return new Iterator<GirAdvOneRow>() {
            private boolean closed = false;

            @Override
            public boolean hasNext() {
                if (closed) {
                    return false;
                }
                try {
                    return featureIterator != null && featureIterator.hasNext();
                } catch (Exception e) {
                    notifyException(exceptionConsumer, e);
                    closeIterator();
                    throw new GeoFileReadException("检查 GeoJSON 是否有下一条数据失败", e);
                }
            }

            @Override
            public GirAdvOneRow next() {
                if (closed) {
                    throw new NoSuchElementException("迭代器已关闭，无法获取下一条数据");
                }
                if (!hasNext()) {
                    closeIterator();
                    throw new NoSuchElementException("已无更多 GeoJSON 数据");
                }
                return readOneRow(exceptionConsumer);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("GeoJSON 文件为只读模式，不支持删除操作");
            }

            @Override
            protected void finalize() throws Throwable {
                closeIterator();
                super.finalize();
            }

            private void closeIterator() {
                if (!closed && featureIterator != null) {
                    featureIterator.close();
                    closed = true;
                    currentRow.set(0);
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
            int startIndex = (pageNum - 1) * pageSize;
            int endIndex = startIndex + pageSize;

            resetIterator();

            List<GirAdvOneRow> rowList = new ArrayList<>();
            AtomicInteger index = new AtomicInteger(0);

            while (featureIterator.hasNext()) {
                int currentIndex = index.getAndIncrement();
                if (currentIndex < startIndex) {
                    featureIterator.next();
                    continue;
                }
                if (currentIndex >= endIndex) {
                    break;
                }

                rowList.add(readOneRow(exceptionConsumer));
            }

            pager.put(rowList, featureCollection.size(), girPageParam);
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("读取 GeoJSON 分页数据失败", e);
        }
        return pager;
    }

    /** 重置迭代器 */
    private void resetIterator() {
        if (featureIterator != null) {
            featureIterator.close();
        }
        initGeoJsonReader();
        currentRow.set(0);
    }

    /** 关闭资源 */
    @Override
    public void close() throws IOException {
        if (featureIterator != null) {
            featureIterator.close();
        }
        this.featureCollection = null;
        this.featureType = null;
    }

    private void notifyException(ExceptionConsumer exceptionConsumer, Exception e) {
        if (exceptionConsumer != null) {
            exceptionConsumer.accept(e);
        }
    }
}
