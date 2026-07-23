package cn.geoair.map.dynamic.file.postgis;

import cn.geoair.base.Gir;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.utils.AdvJdbcUrlUtil;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.exception.GeoFileWriteException;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.data.*;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

public class PostgisGeoFileWriter implements GeoFileWriter {

    private static final Logger LOGGER = Logger.getLogger(PostgisGeoFileWriter.class.getName());

    private PostgisWriterLinkInfo linkInfo;

    private WriteConfig writeConfig = new WriteConfig();

    private DataStore postgisDataStore;

    private SimpleFeatureType featureType;

    private SimpleFeatureBuilder featureBuilder;

    private FeatureStore<SimpleFeatureType, SimpleFeature> featureStore;

    private DefaultFeatureCollection featureCollection;

    private int batchSize = 1000;

    private int batchCount = 0;

    @Override
    public void setLinkInfo(LinkInfo linkInfo) {
        if (!(linkInfo instanceof PostgisWriterLinkInfo)) {
            throw new IllegalArgumentException("链接信息必须是 PostgisLinkInfo 类型");
        }
        this.linkInfo = (PostgisWriterLinkInfo) linkInfo;
        this.linkInfo.checkLinkInfo();
        initPostgisDataStore();
    }

    @Override
    public void setWriteConfig(WriteConfig writeConfig) {
        this.writeConfig = writeConfig == null ? new WriteConfig() : writeConfig;
    }

    @Override
    public GeoFileWriter writeHeader(
            SimpleFeatureType featureType, ExceptionConsumer exceptionConsumer) {
        try {
            this.featureType = featureType;
            CoordinateReferenceSystem crs =
                    GirGeoTools.defaultInstance().getSridOpt().getCRS(writeConfig.getOutPutSrid());
            org.geotools.feature.simple.SimpleFeatureTypeBuilder typeBuilder =
                    new org.geotools.feature.simple.SimpleFeatureTypeBuilder();
            typeBuilder.init(featureType);
            typeBuilder.setCRS(crs);
            typeBuilder.setName(linkInfo.getTableName());
            this.featureType = typeBuilder.buildFeatureType();

            if (postgisDataStore.getSchema(linkInfo.getTableName()) != null) {
                if (!writeConfig.isOverwrite()) {
                    throw new GeoFileWriteException("目标表已存在且未开启覆盖：" + linkInfo.getTableName());
                }
                postgisDataStore.removeSchema(linkInfo.getTableName());
                LOGGER.info("已删除原有表：" + linkInfo.getTableName());
            }

            postgisDataStore.createSchema(this.featureType);
            LOGGER.info("自动创建 PostGIS 表 " + linkInfo.getTableName() + " 成功");

            this.featureStore =
                    (FeatureStore<SimpleFeatureType, SimpleFeature>)
                            postgisDataStore.getFeatureSource(linkInfo.getTableName());
            this.featureBuilder = new SimpleFeatureBuilder(this.featureType);
            this.featureCollection = new DefaultFeatureCollection(null, this.featureType);
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileWriteException("基于 SimpleFeatureType 自动建表失败", e);
        }
        return this;
    }

    @Override
    public GeoFileWriter writeOneRow(
            GirAdvOneRow girAdvOneRow, ExceptionConsumer exceptionConsumer) {
        try {
            if (featureBuilder == null || girAdvOneRow == null || girAdvOneRow.isEmpty()) {
                return this;
            }

            featureBuilder.reset();
            for (Map.Entry<String, Object> entry : girAdvOneRow.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Geometry) {
                    Geometry geom = (Geometry) value;
                    int srid = geom.getSRID();
                    Geometry convert =
                            GirGeoTools.defaultInstance().getSridOpt()
                                    .convert(geom, srid, writeConfig.getOutPutSrid());
                    if (convert == null) {
                        Gir.log.info("转换失败");
                    }
                    featureBuilder.set(fieldName, convert);
                } else {
                    featureBuilder.set(fieldName, value);
                }
            }

            SimpleFeature feature = featureBuilder.buildFeature(UUID.randomUUID().toString());
            featureCollection.add(feature);
            batchCount++;

            if (batchCount >= batchSize) {
                writeBatchFeatures(exceptionConsumer);
            }
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileWriteException("写入 PostGIS 单行数据失败", e);
        }
        return this;
    }

    private void writeBatchFeatures(ExceptionConsumer exceptionConsumer) {
        Transaction transaction = new DefaultTransaction("batch-write");
        try {
            featureStore.setTransaction(transaction);
            featureStore.addFeatures(featureCollection);
            transaction.commit();
            LOGGER.info("批量写入 " + featureCollection.size() + " 条要素成功");

            featureCollection.clear();
            batchCount = 0;
        } catch (Exception e) {
            try {
                transaction.rollback();
            } catch (IOException ex) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(ex);
                }
            }
            if (exceptionConsumer != null) {
                exceptionConsumer.accept(e);
            }
            throw new GeoFileWriteException("PostGIS 批量写入失败", e);
        } finally {
            try {
                transaction.close();
            } catch (IOException e) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(e);
                }
            }
        }
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

            this.postgisDataStore = DataStoreFinder.getDataStore(params);
            if (postgisDataStore == null) {
                throw new GeoFileWriteException("初始化 PostGIS DataStore 失败");
            }
        } catch (Exception e) {
            throw new GeoFileWriteException("初始化 PostGIS DataStore 失败", e);
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

    @Override
    public void close() {
        try {
            if (batchCount > 0) {
                writeBatchFeatures(null);
            }
            if (postgisDataStore != null) {
                postgisDataStore.dispose();
            }
            LOGGER.info("PostGIS 写入器资源已释放，剩余批次条数 " + batchCount);
        } catch (Exception e) {
            LOGGER.severe("关闭 PostGIS 写入器失败：" + e.getMessage());
            throw new GeoFileWriteException("关闭 PostGIS 写入器失败", e);
        }
    }

    private void notifyException(ExceptionConsumer exceptionConsumer, Exception e) {
        if (exceptionConsumer != null) {
            exceptionConsumer.accept(e);
        }
    }
}
