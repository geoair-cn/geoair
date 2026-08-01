package cn.geoair.map.dynamic.file.postgis;

import cn.geoair.base.Gir;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.dynamic.ds.utils.DataSourceDruidFastCreate;
import cn.geoair.map.dynamic.adv.GirAdvQuery;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import cn.geoair.map.dynamic.tools.simple.collection.map.GirFastStrObjMap;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.StopWatch;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.sql.DataSource;

public class PostgisGeoFileWriter implements GeoFileWriter {

    private static final GiLogger logger = GirLoggerFactory.getLogger();

    private PostgisWriterLinkInfo linkInfo;

    DataSource dataSource = null;

    private WriteConfig writeConfig = new WriteConfig();

    private DataStore postgisDataStore;

    private SimpleFeatureType featureType;

    private SimpleFeatureBuilder featureBuilder;

    private FeatureStore<SimpleFeatureType, SimpleFeature> featureStore;

    private DefaultFeatureCollection featureCollection;

    private int batchSize;

    private int batchCount = 0;

    @Override
    public void setLinkInfo(LinkInfo linkInfo) {

        if (!(linkInfo instanceof PostgisWriterLinkInfo)) {
            throw new IllegalArgumentException("链接信息必须是 PostgisLinkInfo 类型");
        }
        this.linkInfo = (PostgisWriterLinkInfo) linkInfo;
        this.linkInfo.checkLinkInfo();
        batchSize = this.linkInfo.getBatchSize();
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
            SimpleFeatureType schema = null;
            try {
                schema = postgisDataStore.getSchema(linkInfo.getTableName());
            } catch (IOException e) {

            }

            if (schema != null) {
                if (!writeConfig.isOverwrite()) {
                    throw new GeoFileWriteException("目标表已存在且未开启覆盖：" + linkInfo.getTableName());
                }
                postgisDataStore.removeSchema(linkInfo.getTableName());
                logger.info("已删除原有表：" + linkInfo.getTableName());
            }
            postgisDataStore.createSchema(this.featureType);
            logger.info("自动创建 PostGIS 表 " + linkInfo.getTableName() + " 成功");
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
            GirAdvQuery.getIAdvExecutor(dataSource).bInsertIgnore(girAdvOneRow,
                    s -> s.setBatchSize(batchSize).setConflictKeys(ListUtil.of("fid")).setTableName(linkInfo.getTableName()));
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
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Transaction transaction = new DefaultTransaction("batch-write");
        stopWatch.stop();
        try {
            stopWatch.start();
            featureStore.setTransaction(transaction);
            featureStore.addFeatures(featureCollection);
            transaction.commit();
            stopWatch.stop();
            logger.info("批量写入 {} 条要素成功，耗时：{}秒", batchCount, stopWatch.getTotalTimeSeconds());
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
            DataSourceDruidFastCreate fastCreate = new DataSourceDruidFastCreate();
            fastCreate.setUrl(linkInfo.getJdbcUrl());
            fastCreate.setUsername(linkInfo.getUsername());
            fastCreate.setPassword(linkInfo.getPassword());
            fastCreate.setInitialSize(1);
            fastCreate.setMinIdle(1);
            dataSource = fastCreate.toDataSource();
            params.put(PostgisNGDataStoreFactory.DATASOURCE.key, dataSource);
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
            logger.info("PostGIS 写入器资源已释放，剩余批次条数 " + batchCount);
        } catch (Exception e) {
            logger.error("关闭 PostGIS 写入器失败：" + e.getMessage());
            throw new GeoFileWriteException("关闭 PostGIS 写入器失败", e);
        }
    }

    private void notifyException(ExceptionConsumer exceptionConsumer, Exception e) {
        if (exceptionConsumer != null) {
            exceptionConsumer.accept(e);
        }
    }
}
