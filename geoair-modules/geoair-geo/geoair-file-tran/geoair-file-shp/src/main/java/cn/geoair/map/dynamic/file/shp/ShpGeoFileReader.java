package cn.geoair.map.dynamic.file.shp;

import cn.geoair.base.data.page.support.GirPageParam;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.exception.GeoFileReadException;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Override
    public long getFeatureCount() {
        if (this.featureCollection == null) {
            return 0;
        }
        return this.featureCollection.size();
    }

    /** 初始化 Shapefile 读取器 */
    private void initShpReader() {
        try {
            File shpFile = new File(linkInfo.getShpFilePath());
            Map<String, Object> params = new HashMap<>();
            params.put(ShapefileDataStoreFactory.URLP.key, shpFile.toURI().toURL());
            params.put(
                    ShapefileDataStoreFactory.DBFCHARSET.key,
                    Charset.forName(linkInfo.getCharset()));

            dataStore = DataStoreFinder.getDataStore(params);
            if (dataStore == null) {
                throw new GeoFileReadException("无法读取 Shapefile，检查文件是否损坏或缺失配套文件(shx/dbf)");
            }

            String typeName = dataStore.getTypeNames()[0];
            featureSource = dataStore.getFeatureSource(typeName);
            featureCollection = featureSource.getFeatures();
            featureType = featureCollection.getSchema();
            featureIterator = featureCollection.features();
        } catch (Exception e) {
            close();
            throw new GeoFileReadException("初始化 Shapefile 读取器失败", e);
        }
    }

    @Override
    public SimpleFeatureType readHeader(ExceptionConsumer exceptionConsumer) {
        try {
            return featureType;
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("读取 shp 表头失败", e);
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
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("读取 shp 单行数据失败", e);
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
                    throw new GeoFileReadException("检查 shp 是否有下一条数据失败", e);
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

    @Override
    public GirPager<GirAdvOneRow> readRowPage(GirPageParam param, ExceptionConsumer consumer) {
        GirPager<GirAdvOneRow> pager = new GirPager<>();
        try {
            if (param == null) {
                throw new IllegalArgumentException("分页参数不能为空");
            }
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
                if (i >= end) {
                    break;
                }

                list.add(readOneRow(consumer));
            }

            pager.put(list, featureCollection.size(), param);
        } catch (Exception e) {
            notifyException(consumer, e);
            throw new GeoFileReadException("读取 shp 分页数据失败", e);
        }
        return pager;
    }

    /** 重置迭代器 */
    private void resetIterator() {
        if (featureIterator != null) {
            featureIterator.close();
        }
        initShpReader();
        currentRow.set(0);
    }

    /** 关闭资源 */
    @Override
    public void close() {
        if (featureIterator != null) {
            featureIterator.close();
        }
        if (dataStore != null) {
            dataStore.dispose();
        }
        featureCollection = null;
        featureType = null;
    }

    private void notifyException(ExceptionConsumer exceptionConsumer, Exception e) {
        if (exceptionConsumer != null) {
            exceptionConsumer.accept(e);
        }
    }
}
