package cn.geoair.map.dynamic.file.postgis;

import cn.geoair.base.Gir;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.utils.AdvJdbcUrlUtil;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;

import java.io.IOException;
import java.util.*;
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
        this.writeConfig = writeConfig;
    }

    @Override
    public GeoFileWriter writeHeader(
            SimpleFeatureType featureType, ExceptionConsumer exceptionConsumer) {
        try {
            this.featureType = featureType;
            CoordinateReferenceSystem crs =
                    GirGeoTools.me().getSridOpt().getCRS(writeConfig.getOutPutSrid());
            org.geotools.feature.simple.SimpleFeatureTypeBuilder typeBuilder =
                    new org.geotools.feature.simple.SimpleFeatureTypeBuilder();
            typeBuilder.init(featureType);
            typeBuilder.setCRS(crs);
            typeBuilder.setName(linkInfo.getTableName());
            this.featureType = typeBuilder.buildFeatureType();

            try {
                if (postgisDataStore.getSchema(linkInfo.getTableName()) != null) {
                    postgisDataStore.removeSchema(linkInfo.getTableName());
                    LOGGER.info("已删除原有表：" + linkInfo.getTableName());
                }
            } catch (Exception e) {

            }

            postgisDataStore.createSchema(this.featureType);
            LOGGER.info("自动创建 PostGIS 表 " + linkInfo.getTableName() + " 成功");

            // 初始化要素写入器
            this.featureStore =
                    (FeatureStore<SimpleFeatureType, SimpleFeature>)
                            postgisDataStore.getFeatureSource(linkInfo.getTableName());
            this.featureBuilder = new SimpleFeatureBuilder(this.featureType);
            this.featureCollection = new DefaultFeatureCollection(null, this.featureType);

        } catch (Exception e) {
            if (exceptionConsumer != null) {
                exceptionConsumer.accept(e);
            } else {
                throw new RuntimeException("基于 SimpleFeatureType 自动建表失败", e);
            }
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
                // 处理几何字段的 SRID 转换
                if (value instanceof Geometry) {
                    Geometry geom = (Geometry) value;
                    int srid = geom.getSRID();
                    Geometry convert =
                            GirGeoTools.me().getSridOpt()
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

            // 批量写入
            if (batchCount >= batchSize) {
                writeBatchFeatures(exceptionConsumer);
            }

        } catch (Exception e) {
            if (exceptionConsumer != null) {
                exceptionConsumer.accept(e);
            } else {
                throw new RuntimeException("写入 PostGIS 单行数据失败", e);
            }
        }
        return this;
    }

    /** 批量写入要素 */
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
                exceptionConsumer.accept(ex);
            }
            if (exceptionConsumer != null) {
                exceptionConsumer.accept(e);
            } else {
                throw new RuntimeException("PostGIS 批量写入失败", e);
            }
        } finally {
            try {
                transaction.close();
            } catch (IOException e) {
                exceptionConsumer.accept(e);
            }
        }
    }

    /** 初始化 PostGIS DataStore */
    private void initPostgisDataStore() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(PostgisNGDataStoreFactory.DBTYPE.key, "postgis");
            params.put(
                    PostgisNGDataStoreFactory.HOST.key,
                    extractHostFromJdbcUrl(linkInfo.getJdbcUrl()));
            params.put(
                    PostgisNGDataStoreFactory.PORT.key,
                    extractPortFromJdbcUrl(linkInfo.getJdbcUrl()));
            params.put(
                    PostgisNGDataStoreFactory.DATABASE.key,
                    extractDbNameFromJdbcUrl(linkInfo.getJdbcUrl()));
            params.put(PostgisNGDataStoreFactory.USER.key, linkInfo.getUsername());
            params.put(PostgisNGDataStoreFactory.PASSWD.key, linkInfo.getPassword());
            params.put(PostgisNGDataStoreFactory.SCHEMA.key, linkInfo.getSchema());

            this.postgisDataStore = DataStoreFinder.getDataStore(params);
            if (postgisDataStore == null) {
                throw new RuntimeException("初始化 PostGIS DataStore 失败");
            }
        } catch (Exception e) {
            throw new RuntimeException("初始化 PostGIS DataStore 失败", e);
        }
    }

    /** 解析 JDBC URL 工具方法 */
    private String extractHostFromJdbcUrl(String jdbcUrl) {

        return AdvJdbcUrlUtil.splitter(jdbcUrl).host;
    }

    private Integer extractPortFromJdbcUrl(String jdbcUrl) {
        String port = AdvJdbcUrlUtil.splitter(jdbcUrl).port;
        return  port!=null?Integer.parseInt(port):5432;
    }

    private String extractDbNameFromJdbcUrl(String jdbcUrl) {
        return AdvJdbcUrlUtil.splitter(jdbcUrl).database;
    }

    @Override
    public void close() {
        try {
            // 写入剩余要素
            if (batchCount > 0) {
                writeBatchFeatures(null);
            }
            // 释放 DataStore
            if (postgisDataStore != null) {
                postgisDataStore.dispose();
            }
            LOGGER.info("PostGIS 写入器资源已释放，总处理 " + batchCount + " 条记录");
        } catch (Exception e) {
            LOGGER.severe("关闭 PostGIS 写入器失败：" + e.getMessage());
        }
    }
}
