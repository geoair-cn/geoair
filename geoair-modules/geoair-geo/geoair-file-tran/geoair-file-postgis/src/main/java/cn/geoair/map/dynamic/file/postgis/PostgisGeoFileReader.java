package cn.geoair.map.dynamic.file.postgis;

import cn.geoair.base.data.page.support.GirPageParam;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.dynamic.ds.utils.AdvJdbcUrlUtil;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;

import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.exception.GeoFileReadException;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.hutool.core.util.IdUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** 改造后的 PostGIS 读取器 适配新接口：readHeader 返回 SimpleFeatureType，补全核心读取逻辑 基于 GeoTools 实现，统一要素类型标准 */
public class PostgisGeoFileReader implements GeoFileReader {
    public static GiLogger log = GirLoggerFactory.getLogger();
    private PostgisReadLinkInfo linkInfo;

    private DataStore postgisDataStore;

    private SimpleFeatureType featureType;

    private FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;

    private FeatureIterator<SimpleFeature> featureIterator;

    private long totalCount;

    @Override
    public void setLinkInfo(LinkInfo linkInfo) {
        if (!(linkInfo instanceof PostgisReadLinkInfo)) {
            throw new IllegalArgumentException("链接信息必须是 PostgisReadLinkInfo 类型");
        }
        this.linkInfo = (PostgisReadLinkInfo) linkInfo;
        this.linkInfo.checkLinkInfo();
        initPostgisDataStore();
        initFeatureSource();
        initFeatureType();
        calculateTotalCount();
    }

    @Override
    public long getFeatureCount() {
        return this.totalCount;
    }

    private void initPostgisDataStore() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
            params.put(PostgisNGDataStoreFactory.HOST.key, extractHostFromJdbcUrl(linkInfo.getJdbcUrl()));
            params.put(PostgisNGDataStoreFactory.PORT.key, extractPortFromJdbcUrl(linkInfo.getJdbcUrl()));
            params.put(PostgisNGDataStoreFactory.DATABASE.key, extractDbNameFromJdbcUrl(linkInfo.getJdbcUrl()));
            params.put(PostgisNGDataStoreFactory.USER.key, linkInfo.getUsername());
            params.put(PostgisNGDataStoreFactory.PASSWD.key, linkInfo.getPassword());
            params.put(PostgisNGDataStoreFactory.SCHEMA.key, linkInfo.getSchema());
            params.put(PostgisNGDataStoreFactory.PREPARED_STATEMENTS.key, true);

            this.postgisDataStore = DataStoreFinder.getDataStore(params);
            if (postgisDataStore == null) {
                throw new GeoFileReadException("初始化 GeoTools PostGIS DataStore 失败");
            }
            log.info("PostGIS DataStore 初始化成功，查询SQL：{}", linkInfo.getQuerySqlByOutPut());
        } catch (Exception e) {
            throw new GeoFileReadException("初始化 PostGIS 连接失败", e);
        }
    }

    private void initFeatureSource() {
        try {
            String viewName = IdUtil.fastSimpleUUID();
            VirtualTable virtualTable = new VirtualTable(viewName, linkInfo.getQuerySqlByOutPut());
            ((JDBCDataStore) postgisDataStore).createVirtualTable(virtualTable);
            this.featureSource = postgisDataStore.getFeatureSource(viewName);
            this.featureIterator = featureSource.getFeatures().features();
        } catch (Exception e) {
            throw new GeoFileReadException("初始化 PostGIS 要素源失败", e);
        }
    }

    private void initFeatureType() {
        FeatureIterator<SimpleFeature> features = null;
        try {
            Query query = new Query();
            query.setMaxFeatures(1);
            features = featureSource.getFeatures(query).features();
            if (features.hasNext()) {
                SimpleFeature feature = features.next();
                featureType = feature.getFeatureType();
            }
        } catch (Exception e) {
            throw new GeoFileReadException("初始化 PostGIS 要素类型失败", e);
        } finally {
            if (features != null) {
                features.close();
            }
        }
    }

    private void calculateTotalCount() {
        try {
            Query countQuery = new Query();
            this.totalCount = featureSource.getCount(countQuery);
            log.info("PostGIS 查询语句 {} 总记录数：{}", linkInfo.getQuerySqlByOutPut(), totalCount);
        } catch (IOException e) {
            log.warn("计算 PostGIS 总记录数失败，将返回 0", e);
            this.totalCount = 0;
        }
    }

    private void resetFeatureIterator() {
        try {
            if (featureIterator != null) {
                featureIterator.close();
            }
            String viewName = IdUtil.fastSimpleUUID();
            VirtualTable virtualTable = new VirtualTable(viewName, linkInfo.getQuerySqlByOutPut());
            ((JDBCDataStore) postgisDataStore).createVirtualTable(virtualTable);
            this.featureSource = postgisDataStore.getFeatureSource(viewName);
            this.featureIterator = featureSource.getFeatures().features();
        } catch (Exception e) {
            throw new GeoFileReadException("重置 PostGIS 要素迭代器失败", e);
        }
    }

    @Override
    public SimpleFeatureType readHeader(ExceptionConsumer exceptionConsumer) {
        try {
            if (linkInfo.getSrid() > 0 && featureType != null) {
                SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
                typeBuilder.init(featureType);
                CoordinateReferenceSystem crs = GirGeoTools.defaultInstance().getSridOpt().getCRS(linkInfo.getSrid());
                typeBuilder.setCRS(crs);
                this.featureType = typeBuilder.buildFeatureType();
            }
            return this.featureType;
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("读取 PostGIS 表头失败", e);
        }
    }

    @Override
    public GirAdvOneRow readNextRow(ExceptionConsumer exceptionConsumer) {
        try {
            if (featureIterator == null || !featureIterator.hasNext()) {
                return null;
            }
            SimpleFeature feature = featureIterator.next();
            Map<String, Object> attributes = new HashMap<>();
            for (Property property : feature.getProperties()) {
                String propName = property.getName().getLocalPart();
                Object propValue = property.getValue();
                if (propValue instanceof Geometry) {
                    attributes.put(propName, propValue);
                } else {
                    attributes.put(propName, propValue);
                }
            }
            return GirAdvOneRow.ofByMap(attributes);
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("读取 PostGIS 单行数据失败", e);
        }
    }

    @Override
    public Iterator<GirAdvOneRow> readRowIterator(ExceptionConsumer exceptionConsumer) {
        resetFeatureIterator();

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
                    throw new GeoFileReadException("检查 PostGIS 是否有下一条数据失败", e);
                }
            }

            @Override
            public GirAdvOneRow next() {
                if (closed) {
                    throw new NoSuchElementException("迭代器已关闭，无法获取下一条数据");
                }
                if (!hasNext()) {
                    closeIterator();
                    throw new NoSuchElementException("已无更多 PostGIS 数据");
                }
                return readNextRow(exceptionConsumer);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("PostGIS数据为只读模式，不支持删除操作");
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

            Query pageQuery = new Query();
            pageQuery.setStartIndex(startIndex);
            pageQuery.setMaxFeatures(pageSize);

            FeatureCollection<SimpleFeatureType, SimpleFeature> pageFeatures =
                    featureSource.getFeatures(pageQuery);
            List<GirAdvOneRow> rowList = new ArrayList<>();

            try (FeatureIterator<SimpleFeature> pageIterator = pageFeatures.features()) {
                while (pageIterator.hasNext()) {
                    SimpleFeature feature = pageIterator.next();
                    Map<String, Object> attributes = new HashMap<>();

                    for (Property property : feature.getProperties()) {
                        String propName = property.getName().getLocalPart();
                        Object propValue = property.getValue();
                        attributes.put(propName, propValue);
                    }

                    rowList.add(GirAdvOneRow.ofByMap(attributes));
                }
            }

            pager.put(rowList, totalCount, girPageParam);
            log.info(
                    "读取 PostGIS 分页数据：第{}页，每页{}条，本次返回{}条，总记录数{}",
                    pageNum,
                    pageSize,
                    rowList.size(),
                    totalCount);
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("读取 PostGIS 分页数据失败", e);
        }
        return pager;
    }

    @Override
    public void close() {
        try {
            if (featureIterator != null) {
                featureIterator.close();
            }
            if (postgisDataStore != null) {
                postgisDataStore.dispose();
            }
            log.info("PostGIS 读取器资源已释放");
        } catch (Exception e) {
            throw new GeoFileReadException("关闭 PostGIS 读取器资源失败", e);
        }
    }

    private String extractHostFromJdbcUrl(String jdbcUrl) {
        return AdvJdbcUrlUtil.splitter(jdbcUrl).host;
    }

    private Integer extractPortFromJdbcUrl(String jdbcUrl) {
        String port = AdvJdbcUrlUtil.splitter(jdbcUrl).port;
        return port != null ? Integer.parseInt(port) : 5432;
    }

    private String extractDbNameFromJdbcUrl(String jdbcUrl) {
        return AdvJdbcUrlUtil.splitter(jdbcUrl).database;
    }

    private void notifyException(ExceptionConsumer exceptionConsumer, Exception e) {
        if (exceptionConsumer != null) {
            exceptionConsumer.accept(e);
        }
    }
}
