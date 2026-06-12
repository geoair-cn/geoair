package cn.geoair.map.dynamic.file.shp;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;

import org.geotools.api.data.Transaction;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShpGeoFileWriter implements GeoFileWriter {

    private static final GiLogger log = GirLoggerFactory.getLogger(ShpGeoFileWriter.class);

    private ShpLinkInfo linkInfo;
    private WriteConfig writeConfig;
    private SimpleFeatureType featureType;
    private DefaultFeatureCollection featureCollection;
    private SimpleFeatureBuilder featureBuilder;
    private boolean headerWritten = false;

    @Override
    public void setLinkInfo(LinkInfo linkInfo) {
        if (!(linkInfo instanceof ShpLinkInfo)) {
            throw new IllegalArgumentException("链接信息必须是 ShpLinkInfo");
        }
        this.linkInfo = (ShpLinkInfo) linkInfo;
        this.linkInfo.checkLinkInfo();
        this.featureCollection = new DefaultFeatureCollection();
    }

    @Override
    public void setWriteConfig(WriteConfig writeConfig) {
        this.writeConfig = writeConfig;
    }

    @Override
    public GeoFileWriter writeHeader(SimpleFeatureType featureType, ExceptionConsumer consumer) {
        try {
            if (headerWritten) return this;

            this.featureType = featureType;

            if (writeConfig != null && writeConfig.getOutPutSrid() > 0) {
                CoordinateReferenceSystem targetCrs =
                        cn.geoair.map.dynamic.tools.GirGeoTools.defaultInstance()
                                .getSridOpt()
                                .getCRS(writeConfig.getOutPutSrid());

                org.geotools.feature.simple.SimpleFeatureTypeBuilder tb =
                        new org.geotools.feature.simple.SimpleFeatureTypeBuilder();
                tb.init(featureType);
                tb.setCRS(targetCrs);
                this.featureType = tb.buildFeatureType();
            }

            this.featureBuilder = new SimpleFeatureBuilder(this.featureType);
            this.headerWritten = true;
            log.info("Shapefile 写入器初始化完成：{}", featureType.getName());
        } catch (Exception e) {
            if (consumer != null) consumer.accept(e);
        }
        return this;
    }

    @Override
    public GeoFileWriter writeOneRow(GirAdvOneRow row, ExceptionConsumer consumer) {
        try {
            if (!headerWritten || featureType == null) {
                throw new IllegalStateException("请先调用 writeHeader");
            }
            if (row == null || row.isEmpty()) return this;

            featureBuilder.reset();
            row.forEach((k, v) -> featureBuilder.set(k, v));

            SimpleFeature feature = featureBuilder.buildFeature(UUID.randomUUID().toString());
            featureCollection.add(feature);
        } catch (Exception e) {
            if (consumer != null) consumer.accept(e);
        }
        return this;
    }

    @Override
    public void close() throws MalformedURLException {
        if (!headerWritten || featureCollection == null || featureCollection.isEmpty()) {
            return;
        }

        File shpFile = new File(linkInfo.getShpFilePath());
        Map<String, Object> params = new HashMap<>();
        params.put(ShapefileDataStoreFactory.URLP.key, shpFile.toURI().toURL());
        params.put(
                ShapefileDataStoreFactory.DBFCHARSET.key, Charset.forName(linkInfo.getCharset()));
        params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);

        ShapefileDataStore dataStore = null;
        Transaction transaction = null;

        try {
            dataStore =
                    (ShapefileDataStore) org.geotools.api.data.DataStoreFinder.getDataStore(params);
            dataStore.createSchema(featureType);
            dataStore.forceSchemaCRS(featureType.getCoordinateReferenceSystem());

            transaction = new DefaultTransaction("shp-write");
            String typeName = dataStore.getTypeNames()[0];

            try (org.geotools.api.data.FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
                    dataStore.getFeatureWriterAppend(typeName, transaction)) {

                for (SimpleFeature source : (Iterable<SimpleFeature>) featureCollection) {
                    SimpleFeature target = writer.next();
                    for (Property prop : source.getProperties()) {
                        String name = prop.getName().getLocalPart();
                        Object value = prop.getValue();
                        target.setAttribute(name, value);
                    }
                    writer.write();
                }
            }

            transaction.commit();
            log.info(
                    "Shapefile 写入完成，共 {} 条，路径：{}",
                    featureCollection.size(),
                    shpFile.getAbsolutePath());

        } catch (Exception e) {
            if (transaction != null) {
                try {
                    transaction.rollback();
                } catch (Exception ex) {
                    log.error("shp 回滚失败", ex);
                }
            }
            throw new RuntimeException("shp 文件写入失败", e);
        } finally {
            if (transaction != null)
                try {
                    transaction.close();
                } catch (Exception ignored) {
                }
            if (dataStore != null) dataStore.dispose();
        }
    }
}
