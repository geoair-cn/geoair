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

    /** 当前迭代器已消费的要素数，用于顺序分页时判断是否需要跳过或重置 */
    private int consumedCount = 0;

    @Override
    public void setLinkInfo(LinkInfo linkInfo) {
        if (!(linkInfo instanceof ShpLinkInfo)) {
            throw new IllegalArgumentException("链接信息必须是 ShpLinkInfo 类型");
        }
        this.linkInfo = (ShpLinkInfo) linkInfo;
        this.linkInfo.checkLinkInfo();
        initShpReader();
    }

    public boolean supportParallelPageRead() {
        return false;
    }

    @Override
    public long getFeatureCount() {
        if (this.featureCollection == null) {
            return 0;
        }
        return this.featureCollection.size();
    }

    /**
     * 初始化 Shapefile 读取器。
     * 仅创建 DataStore / FeatureSource / FeatureCollection / FeatureType，
     * 不打开 FeatureIterator——迭代器由读取方法按需懒初始化。
     */
    private void initShpReader() {
        try {
            File shpFile = new File(linkInfo.getShpFilePath());
            Map<String, Object> params = new HashMap<>();
            params.put(ShapefileDataStoreFactory.URLP.key, shpFile.toURI().toURL());
            params.put(ShapefileDataStoreFactory.DBFCHARSET.key, Charset.forName(linkInfo.getCharset()));

            dataStore = DataStoreFinder.getDataStore(params);
            if (dataStore == null) {
                throw new GeoFileReadException("无法读取 Shapefile，检查文件是否损坏或缺失配套文件(shx/dbf)");
            }

            String typeName = dataStore.getTypeNames()[0];
            featureSource = dataStore.getFeatureSource(typeName);
            featureCollection = featureSource.getFeatures();
            featureType = featureCollection.getSchema();
        } catch (Exception e) {
            close();
            throw new GeoFileReadException("初始化 Shapefile 读取器失败", e);
        }
    }

    /**
     * 懒初始化迭代器：首次调用时打开，后续复用
     */
    private FeatureIterator<SimpleFeature> getOrCreateIterator() {
        if (featureIterator == null) {
            featureIterator = featureCollection.features();
            consumedCount = 0;
        }
        return featureIterator;
    }

    /**
     * 关闭实例级迭代器并重置消费计数（不 dispose DataStore）
     */
    private void closeIterator() {
        if (featureIterator != null) {
            featureIterator.close();
            featureIterator = null;
            consumedCount = 0;
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
    public GirAdvOneRow readNextRow(ExceptionConsumer exceptionConsumer) {
        try {
            FeatureIterator<SimpleFeature> iter = getOrCreateIterator();
            if (!iter.hasNext()) {
                return null;
            }
            consumedCount++;
            return featureToRow(iter.next());
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("读取 shp 单行数据失败", e);
        }
    }

    @Override
    public Iterator<GirAdvOneRow> readRowIterator(ExceptionConsumer exceptionConsumer) {
        return new Iterator<GirAdvOneRow>() {
            private final FeatureIterator<SimpleFeature> iterator = featureCollection.features();
            private boolean closed = false;

            @Override
            public boolean hasNext() {
                if (closed) {
                    return false;
                }
                try {
                    return iterator.hasNext();
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
                return featureToRow(iterator.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("shp 只读");
            }

            private void closeIterator() {
                if (!closed) {
                    iterator.close();
                    closed = true;
                }
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

            // 回翻时重置迭代器（从头重新打开）
            if (start < consumedCount) {
                closeIterator();
            }

            FeatureIterator<SimpleFeature> iter = getOrCreateIterator();

            // 跳到 start 位置；顺序读取时 consumedCount == start，无需跳过
            while (consumedCount < start && iter.hasNext()) {
                iter.next();
                consumedCount++;
            }

            // 读取当前页数据
            List<GirAdvOneRow> list = new ArrayList<>();
            while (consumedCount < end && iter.hasNext()) {
                consumedCount++;
                list.add(featureToRow(iter.next()));
            }

            pager.put(list, featureCollection.size(), param);
        } catch (Exception e) {
            notifyException(consumer, e);
            throw new GeoFileReadException("读取 shp 分页数据失败", e);
        }
        return pager;
    }

    /**
     * 将 SimpleFeature 转换为 GirAdvOneRow
     */
    private GirAdvOneRow featureToRow(SimpleFeature feature) {
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
    }

    /**
     * 关闭资源
     */
    @Override
    public void close() {
        closeIterator();
        if (dataStore != null) {
            dataStore.dispose();
            dataStore = null;
        }
        featureCollection = null;
        featureSource = null;
        featureType = null;
    }

    private void notifyException(ExceptionConsumer exceptionConsumer, Exception e) {
        if (exceptionConsumer != null) {
            exceptionConsumer.accept(e);
        }
    }
}
