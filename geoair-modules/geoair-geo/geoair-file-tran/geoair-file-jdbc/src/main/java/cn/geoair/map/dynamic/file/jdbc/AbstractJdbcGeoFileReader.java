package cn.geoair.map.dynamic.file.jdbc;

import cn.geoair.base.data.page.support.GirPageParam;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.exception.GeoFileReadException;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoReadLinkInfo;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.hutool.core.util.IdUtil;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.locationtech.jts.geom.Geometry;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 基于 GeoTools JDBC DataStore 的通用空间读取器。
 *
 * @author 张逢吉
 */
public abstract class AbstractJdbcGeoFileReader implements GeoFileReader {

    private final JdbcGeoDialect dialect;
    protected JdbcGeoReadLinkInfo linkInfo;
    protected DataStore dataStore;
    protected FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
    protected SimpleFeatureType featureType;
    protected long totalCount = -1L;
    private FeatureIterator<SimpleFeature> sequentialIterator;

    protected AbstractJdbcGeoFileReader(JdbcGeoDialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public final void setLinkInfo(LinkInfo linkInfo) {
        if (!(linkInfo instanceof JdbcGeoReadLinkInfo)) {
            throw new IllegalArgumentException("链接信息必须继承 JdbcGeoReadLinkInfo");
        }
        closeQuietly();
        this.linkInfo = (JdbcGeoReadLinkInfo) linkInfo;
        this.linkInfo.checkLinkInfo();
        try {
            this.dataStore = dialect.createDataStore(this.linkInfo);
            if (!(dataStore instanceof JDBCDataStore)) {
                throw new GeoFileReadException(dialect.getName() + " DataStore 必须为 JDBCDataStore");
            }
            resetFeatureSource();
            this.featureType = resolveFeatureType();
            this.totalCount = resolveFeatureCount();
        } catch (Exception e) {
            closeQuietly();
            throw new GeoFileReadException("初始化 " + dialect.getName() + " 读取器失败", e);
        }
    }

    @Override
    public long getFeatureCount() {
        return totalCount;
    }

    @Override
    public SimpleFeatureType readHeader(ExceptionConsumer exceptionConsumer) {
        try {
            if (featureType == null) {
                throw new GeoFileReadException("尚未初始化要素类型");
            }
            if (linkInfo.getSrid() <= 0) {
                return featureType;
            }
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.init(featureType);
            CoordinateReferenceSystem crs = GirGeoTools.defaultInstance().getSridOpt().getCRS(linkInfo.getSrid());
            builder.setCRS(crs);
            return builder.buildFeatureType();
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("读取 " + dialect.getName() + " 表头失败", e);
        }
    }

    @Override
    public GirAdvOneRow readNextRow(ExceptionConsumer exceptionConsumer) {
        try {
            if (sequentialIterator == null) {
                sequentialIterator = featureSource.getFeatures().features();
            }
            if (!sequentialIterator.hasNext()) {
                closeSequentialIterator();
                return null;
            }
            return toRow(sequentialIterator.next());
        } catch (Exception e) {
            closeSequentialIterator();
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("顺序读取 " + dialect.getName() + " 数据失败", e);
        }
    }

    @Override
    public Iterator<GirAdvOneRow> readRowIterator(ExceptionConsumer exceptionConsumer) {
        try {
            final FeatureIterator<SimpleFeature> iterator = featureSource.getFeatures().features();
            return new Iterator<GirAdvOneRow>() {
                private boolean closed;

                @Override
                public boolean hasNext() {
                    if (closed) {
                        return false;
                    }
                    try {
                        boolean hasNext = iterator.hasNext();
                        if (!hasNext) {
                            close();
                        }
                        return hasNext;
                    } catch (Exception e) {
                        close();
                        notifyException(exceptionConsumer, e);
                        throw new GeoFileReadException("迭代读取 " + dialect.getName() + " 数据失败", e);
                    }
                }

                @Override
                public GirAdvOneRow next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException("没有更多空间要素");
                    }
                    return toRow(iterator.next());
                }

                private void close() {
                    if (!closed) {
                        iterator.close();
                        closed = true;
                    }
                }
            };
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("创建 " + dialect.getName() + " 数据迭代器失败", e);
        }
    }

    @Override
    public GirPager<GirAdvOneRow> readRowPage(GirPageParam pageParam, ExceptionConsumer exceptionConsumer) {
        if (pageParam == null) {
            throw new IllegalArgumentException("pageParam 不能为空");
        }
        try {
            int pageNum = pageParam.getPageNum();
            int pageSize = pageParam.getPageSize();
            Query query = new Query();
            query.setStartIndex(Math.max(0, (pageNum - 1) * pageSize));
            query.setMaxFeatures(pageSize);
            FeatureCollection<SimpleFeatureType, SimpleFeature> features = featureSource.getFeatures(query);
            java.util.List<GirAdvOneRow> rows = new java.util.ArrayList<>();
            try (FeatureIterator<SimpleFeature> iterator = features.features()) {
                while (iterator.hasNext()) {
                    rows.add(toRow(iterator.next()));
                }
            }
            GirPager<GirAdvOneRow> pager = new GirPager<>();
            pager.put(rows, Math.max(0, totalCount), pageParam);
            return pager;
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileReadException("分页读取 " + dialect.getName() + " 数据失败", e);
        }
    }

    @Override
    public boolean supportParallelPageRead() {
        // GeoTools JDBCDataStore 的连接和 VirtualTable 由实例共享，默认按顺序分页更安全。
        return false;
    }

    @Override
    public void close() {
        closeQuietly();
    }

    private void resetFeatureSource() throws Exception {
        String viewName = "gir_" + IdUtil.fastSimpleUUID();
        String sql = trimTrailingSemicolon(linkInfo.getQuerySql());
        String orderedSql = "SELECT * FROM (" + sql + ") gir_source ORDER BY " + linkInfo.getOrderBy();
        VirtualTable virtualTable = new VirtualTable(viewName, orderedSql);
        ((JDBCDataStore) dataStore).createVirtualTable(virtualTable);
        this.featureSource = dataStore.getFeatureSource(viewName);
    }

    private SimpleFeatureType resolveFeatureType() throws IOException {
        Query query = new Query();
        query.setMaxFeatures(1);
        try (FeatureIterator<SimpleFeature> iterator = featureSource.getFeatures(query).features()) {
            return iterator.hasNext() ? iterator.next().getFeatureType() : featureSource.getSchema();
        }
    }

    private long resolveFeatureCount() {
        try {
            int count = featureSource.getCount(new Query());
            return count >= 0 ? count : -1L;
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private GirAdvOneRow toRow(SimpleFeature feature) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Property property : feature.getProperties()) {
            Object value = property.getValue();
            values.put(property.getName().getLocalPart(), value instanceof Geometry ? value : value);
        }
        return GirAdvOneRow.ofByMap(values);
    }

    private void closeQuietly() {
        closeSequentialIterator();
        if (dataStore != null) {
            dataStore.dispose();
            dataStore = null;
        }
        featureSource = null;
        featureType = null;
    }

    private void closeSequentialIterator() {
        if (sequentialIterator != null) {
            sequentialIterator.close();
            sequentialIterator = null;
        }
    }

    private String trimTrailingSemicolon(String sql) {
        String trimmed = sql.trim();
        return trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private void notifyException(ExceptionConsumer consumer, Exception e) {
        if (consumer != null) {
            consumer.accept(e);
        }
    }
}
