package cn.geoair.map.dynamic.file.geojson;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilReflection;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.exception.GeoFileWriteException;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;
import cn.geoair.map.dynamic.tools.GirGeoTools;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.geojson.GeoJSONWriter;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.UUID;

public class GeoJsonGeoFileWriter implements GeoFileWriter {

    private static GiLogger log = GirLoggerFactory.getLogger(GeoJsonGeoFileWriter.class);

    private GeoJsonLinkInfo linkInfo;

    private WriteConfig writeConfig = new WriteConfig();

    private SimpleFeatureType featureType;

    private DefaultFeatureCollection featureCollection;

    private SimpleFeatureBuilder featureBuilder;

    private boolean headerWritten = false;

    @Override
    public void setLinkInfo(LinkInfo linkInfo) {
        if (!(linkInfo instanceof GeoJsonLinkInfo)) {
            throw new IllegalArgumentException("链接信息必须是 GeoJsonLinkInfo 类型");
        }
        this.linkInfo = (GeoJsonLinkInfo) linkInfo;
        this.linkInfo.checkLinkInfo();
        this.featureCollection = new DefaultFeatureCollection();
    }

    @Override
    public void setWriteConfig(WriteConfig writeConfig) {
        this.writeConfig = writeConfig == null ? new WriteConfig() : writeConfig;
    }

    @Override
    public GeoFileWriter writeHeader(
            SimpleFeatureType featureType, ExceptionConsumer exceptionConsumer) {
        try {
            if (headerWritten) {
                return this;
            }

            this.featureType = featureType;

            if (writeConfig != null && writeConfig.getOutPutSrid() > 0) {
                CoordinateReferenceSystem targetCrs =
                        GirGeoTools.defaultInstance()
                                .getSridOpt()
                                .getCRS(writeConfig.getOutPutSrid());
                org.geotools.feature.simple.SimpleFeatureTypeBuilder typeBuilder =
                        new org.geotools.feature.simple.SimpleFeatureTypeBuilder();
                typeBuilder.init(featureType);
                typeBuilder.setCRS(targetCrs);
                this.featureType = typeBuilder.buildFeatureType();
            }

            this.featureBuilder = new SimpleFeatureBuilder(this.featureType);
            this.headerWritten = true;
            log.info("GeoJSON 写入器初始化完成，使用外部传入的 SimpleFeatureType：{}", featureType.getName());
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileWriteException("初始化 GeoJSON 写入表头失败", e);
        }
        return this;
    }

    @Override
    public GeoFileWriter writeOneRow(
            GirAdvOneRow girAdvOneRow, ExceptionConsumer exceptionConsumer) {
        try {
            if (!headerWritten || featureType == null) {
                throw new IllegalStateException(
                        "请先调用 writeHeader(SimpleFeatureType, ExceptionConsumer) 初始化表头");
            }
            if (girAdvOneRow == null || girAdvOneRow.isEmpty()) {
                return this;
            }

            featureBuilder.reset();
            for (Map.Entry<String, Object> entry : girAdvOneRow.entrySet()) {
                featureBuilder.set(entry.getKey(), entry.getValue());
            }

            SimpleFeature feature = featureBuilder.buildFeature(UUID.randomUUID().toString());
            featureCollection.add(feature);
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileWriteException("写入 GeoJSON 单行数据失败", e);
        }
        return this;
    }

    @Override
    public void close() {
        if (featureCollection == null || featureCollection.isEmpty() || !headerWritten) {
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(new File(linkInfo.getGeoJsonFilePath()))) {
            GeoJSONWriter geoJsonWriter = new GeoJSONWriter(fos);
            if (writeConfig != null && writeConfig.getOutPutSrid() > 0) {
                CoordinateReferenceSystem crs =
                        GirGeoTools.defaultInstance()
                                .getSridOpt()
                                .getCRS(writeConfig.getOutPutSrid());
                try {
                    GutilReflection.setFieldValue(geoJsonWriter, "outCRS", crs);
                } catch (Exception reflectionException) {
                    throw new GeoFileWriteException("设置 GeoJSON 输出坐标系失败", reflectionException);
                }
            }
            geoJsonWriter.setEncodeFeatureCollectionCRS(false);
            geoJsonWriter.setEncodeFeatureBounds(false);
            geoJsonWriter.writeFeatureCollection(featureCollection);
            geoJsonWriter.close();

            log.info("GeoJSON 文件写入完成，共写入 {} 条要素", featureCollection.size());
        } catch (Exception e) {
            throw new GeoFileWriteException("关闭 GeoJSON 写入器并写入文件失败", e);
        }
    }

    private void notifyException(ExceptionConsumer exceptionConsumer, Exception e) {
        if (exceptionConsumer != null) {
            exceptionConsumer.accept(e);
        }
    }
}
