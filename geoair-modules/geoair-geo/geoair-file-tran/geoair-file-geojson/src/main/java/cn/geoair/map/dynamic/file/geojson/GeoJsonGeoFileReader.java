package cn.geoair.map.dynamic.file.geojson;

import cn.geoair.base.data.page.support.GirPageParam;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

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

    /** 初始化 GeoJSON 读取器 */
    private void initGeoJsonReader() {
        try (FileInputStream fis = new FileInputStream(linkInfo.getGeoJsonFilePath())) {
            GeoJSONReader geoJsonReader = new GeoJSONReader(fis);
            this.featureCollection = geoJsonReader.getFeatures();
            this.featureType = featureCollection.getSchema(); // 获取完整的 SimpleFeatureType
            this.featureIterator = featureCollection.features();
        } catch (Exception e) {
            throw new RuntimeException("初始化 GeoJSON 读取器失败", e);
        }
    }

    /** 改造核心：返回 SimpleFeatureType（替代原 List<Pair>） */
    @Override
    public SimpleFeatureType readHeader(ExceptionConsumer exceptionConsumer) {
        try {
            return this.featureType;
        } catch (Exception e) {
            if (exceptionConsumer != null) {
                exceptionConsumer.accept(e);
                return null;
            } else {
                throw new RuntimeException("读取 GeoJSON 表头（SimpleFeatureType）失败", e);
            }
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
            if (exceptionConsumer != null) {
                exceptionConsumer.accept(e);
                return null;
            } else {
                throw new RuntimeException("读取 GeoJSON 单行数据失败", e);
            }
        }
    }

    @Override
    public Iterator<GirAdvOneRow> readRowIterator(ExceptionConsumer exceptionConsumer) {
        // 重置迭代器，确保每次获取迭代器都是从第一条数据开始
        resetIterator();

        return new Iterator<GirAdvOneRow>() {
            // 标记迭代器是否已关闭，防止重复关闭
            private boolean closed = false;

            @Override
            public boolean hasNext() {
                // 已关闭则直接返回false
                if (closed) {
                    return false;
                }
                try {
                    return featureIterator != null && featureIterator.hasNext();
                } catch (Exception e) {
                    if (exceptionConsumer != null) {
                        exceptionConsumer.accept(e);
                    } else {
                        throw new RuntimeException("检查GeoJSON是否有下一条数据失败", e);
                    }
                    return false;
                }
            }

            @Override
            public GirAdvOneRow next() {
                if (closed) {
                    throw new NoSuchElementException("迭代器已关闭，无法获取下一条数据");
                }
                try {
                    // 复用已有的readOneRow逻辑，保证数据处理逻辑一致
                    GirAdvOneRow oneRow = readOneRow(exceptionConsumer);
                    // 如果没有下一条数据了，自动关闭迭代器
                    if (oneRow == null) {
                        closeIterator();
                        throw new NoSuchElementException("已无更多GeoJSON数据");
                    }
                    return oneRow;
                } catch (NoSuchElementException e) {
                    // 正常的无数据异常直接抛出
                    throw e;
                } catch (Exception e) {
                    if (exceptionConsumer != null) {
                        exceptionConsumer.accept(e);
                        // 异常时关闭迭代器
                        closeIterator();
                        return null;
                    } else {
                        closeIterator();
                        throw new RuntimeException("读取GeoJSON下一条数据失败", e);
                    }
                }
            }

            // 重写remove方法，GeoJSON只读，不支持删除
            @Override
            public void remove() {
                throw new UnsupportedOperationException("GeoJSON文件为只读模式，不支持删除操作");
            }

            // 迭代器结束时关闭资源
            @Override
            protected void finalize() throws Throwable {
                closeIterator();
                super.finalize();
            }

            // 关闭迭代器的私有方法
            private void closeIterator() {
                if (!closed && featureIterator != null) {
                    featureIterator.close();
                    closed = true;
                    currentRow.set(0);
                }
            }
        };
    }

    /** 读取分页数据（逻辑不变） */
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

                GirAdvOneRow oneRow = readOneRow(exceptionConsumer);
                if (oneRow != null) {
                    rowList.add(oneRow);
                }
            }

            pager.put(rowList, featureCollection.size(), girPageParam);
        } catch (Exception e) {
            if (exceptionConsumer != null) {
                exceptionConsumer.accept(e);
            } else {
                throw new RuntimeException("读取 GeoJSON 分页数据失败", e);
            }
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
}
