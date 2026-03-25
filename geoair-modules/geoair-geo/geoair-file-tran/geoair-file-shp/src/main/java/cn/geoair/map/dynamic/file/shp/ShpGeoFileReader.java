package cn.geoair.map.dynamic.file.shp;

import cn.geoair.base.data.page.support.GirPageParam;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;
import java.util.Map;

public class ShpGeoFileReader implements GeoFileReader {

    private ShpLinkInfo linkInfo;

    private DataStore dataStore;
    private FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
    private FeatureIterator<SimpleFeature> featureIterator;
    private SimpleFeatureType featureType;

    private final AtomicInteger currentRow = new AtomicInteger(0);

    @Override
    public void setLinkInfo(LinkInfo linkInfo) {
        if (!(linkInfo instanceof ShpLinkInfo)) {
            throw new IllegalArgumentException("链接信息必须是 ShpLinkInfo 类型");
        }
        this.linkInfo = (ShpLinkInfo) linkInfo;
        this.linkInfo.checkLinkInfo();
        initShpReader();
    }

    /**
     * 初始化 Shapefile 读取器
     */
    private void initShpReader() {
        try {
            File shpFile = new File(linkInfo.getShpFilePath());
            Map<String, Object> params = new HashMap<>();
            params.put(ShapefileDataStoreFactory.URLP.key, shpFile.toURI().toURL());
            params.put(ShapefileDataStoreFactory.DBFCHARSET.key, Charset.forName(linkInfo.getCharset()));

            dataStore = DataStoreFinder.getDataStore(params);
            if (dataStore == null) {
                throw new RuntimeException("无法读取 Shapefile，检查文件是否损坏或缺失配套文件(shx/dbf)");
            }

            String typeName = dataStore.getTypeNames()[0];
            featureSource = dataStore.getFeatureSource(typeName);
            featureCollection = featureSource.getFeatures();
            featureType = featureCollection.getSchema();
            featureIterator = featureCollection.features();

        } catch (Exception e) {
            close();
            throw new RuntimeException("初始化 Shapefile 读取器失败", e);
        }
    }

    /**
     * 读取表头 = SimpleFeatureType
     */
    @Override
    public SimpleFeatureType readHeader(ExceptionConsumer exceptionConsumer) {
        try {
            return featureType;
        } catch (Exception e) {
            if (exceptionConsumer != null) {
                exceptionConsumer.accept(e);
                return null;
            }
            throw new RuntimeException("读取 shp 表头失败", e);
        }
    }

    /**
     * 读取单行（和 GeoJSON 逻辑完全一致）
     */
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
                String name = property.getName().getLocalPart();
                Object value = property.getValue();

                if (value instanceof Geometry) {
                    Geometry geom = (Geometry) value;
                    geom.setSRID(linkInfo.getSrid());
                    attributes.put(name, geom);
                } else {
                    attributes.put(name, value);
                }
            }
            return GirAdvOneRow.ofByMap(attributes);

        } catch (Exception e) {
            if (exceptionConsumer != null) {
                exceptionConsumer.accept(e);
                return null;
            }
            throw new RuntimeException("读取 shp 单行数据失败", e);
        }
    }

    /**
     * 行迭代器（完全复用原逻辑）
     */
    @Override
    public Iterator<GirAdvOneRow> readRowIterator(ExceptionConsumer exceptionConsumer) {
        resetIterator();

        return new Iterator<GirAdvOneRow>() {
            private boolean closed = false;

            @Override
            public boolean hasNext() {
                if (closed) return false;
                try {
                    return featureIterator != null && featureIterator.hasNext();
                } catch (Exception e) {
                    if (exceptionConsumer != null) exceptionConsumer.accept(e);
                    return false;
                }
            }

            @Override
            public GirAdvOneRow next() {
                if (closed) throw new NoSuchElementException("迭代器已关闭");
                try {
                    GirAdvOneRow row = readOneRow(exceptionConsumer);
                    if (row == null) {
                        closeIterator();
                        throw new NoSuchElementException("无更多数据");
                    }
                    return row;
                } catch (Exception e) {
                    closeIterator();
                    throw new RuntimeException("读取 shp 数据失败", e);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("shp 只读");
            }

            private void closeIterator() {
                if (!closed && featureIterator != null) {
                    featureIterator.close();
                    closed = true;
                    currentRow.set(0);
                }
            }

            @Override
            protected void finalize() throws Throwable {
                closeIterator();
                super.finalize();
            }
        };
    }

    /**
     * 分页读取（逻辑不变）
     */
    @Override
    public GirPager<GirAdvOneRow> readRowPage(GirPageParam param, ExceptionConsumer consumer) {
        GirPager<GirAdvOneRow> pager = new GirPager<>();
        try {
            if (param == null) throw new IllegalArgumentException("分页参数不能为空");
            int pageNum = param.getPageNum();
            int pageSize = param.getPageSize();
            int start = (pageNum - 1) * pageSize;
            int end = start + pageSize;

            resetIterator();
            List<GirAdvOneRow> list = new ArrayList<>();
            AtomicInteger index = new AtomicInteger(0);

            while (featureIterator.hasNext()) {
                int i = index.getAndIncrement();
                if (i < start) {
                    featureIterator.next();
                    continue;
                }
                if (i >= end) break;

                GirAdvOneRow row = readOneRow(consumer);
                if (row != null) list.add(row);
            }

            pager.put(list, featureCollection.size(), param);
        } catch (Exception e) {
            if (consumer != null) consumer.accept(e);
        }
        return pager;
    }

    /**
     * 重置迭代器
     */
    private void resetIterator() {
        if (featureIterator != null) featureIterator.close();
        initShpReader();
        currentRow.set(0);
    }

    /**
     * 关闭资源
     */
    @Override
    public void close() {
        if (featureIterator != null) featureIterator.close();
        if (dataStore != null) dataStore.dispose();
        featureCollection = null;
        featureType = null;
    }
}
