package cn.geoair.map.dynamic.file.geojson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;

import org.geotools.data.geojson.GeoJSONWriter;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import cn.geoair.base.Gir;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.base.util.GutilReflection;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;
import cn.geoair.map.dynamic.tools.GirAdvTools;

public class GeoJsonGeoFileWriter implements GeoFileWriter {

	private static GiLogger log = GirLogger.getLoger(GeoJsonGeoFileWriter.class);

	private GeoJsonLinkInfo linkInfo;

	private WriteConfig writeConfig;

	private SimpleFeatureType featureType; // 外部传入的要素类型

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
		this.writeConfig = writeConfig;
	}

	/** 改造核心：入参为 SimpleFeatureType（替代原空参/ExceptionConsumer 入参） */
	@Override
	public GeoFileWriter writeHeader(SimpleFeatureType featureType, ExceptionConsumer exceptionConsumer) {
		try {
			if (headerWritten) {
				return this;
			}

			this.featureType = featureType;

			// 覆盖坐标系（如果配置了目标 SRID）
			if (writeConfig != null && writeConfig.getOutPutSrid() > 0) {
				CoordinateReferenceSystem targetCrs = GirAdvTools.getSridOpt().getCRS(writeConfig.getOutPutSrid());
				// 重建要素类型，替换 CRS
				org.geotools.feature.simple.SimpleFeatureTypeBuilder typeBuilder = new org.geotools.feature.simple.SimpleFeatureTypeBuilder();
				typeBuilder.init(featureType);
				typeBuilder.setCRS(targetCrs);
				this.featureType = typeBuilder.buildFeatureType();
			}

			this.featureBuilder = new SimpleFeatureBuilder(this.featureType);
			this.headerWritten = true;
			log.info("GeoJSON 写入器初始化完成，使用外部传入的 SimpleFeatureType：{}", featureType.getName());

		}
		catch (Exception e) {
			if (exceptionConsumer != null) {
				exceptionConsumer.accept(e);
			}
			else {
				throw new RuntimeException("初始化 GeoJSON 写入表头失败", e);
			}
		}
		return this;
	}

	/** 写入单行数据（逻辑不变，基于外部传入的 SimpleFeatureType 构建要素） */
	@Override
	public GeoFileWriter writeOneRow(GirAdvOneRow girAdvOneRow, ExceptionConsumer exceptionConsumer) {
		try {
			if (!headerWritten || featureType == null) {
				throw new IllegalStateException("请先调用 writeHeader(SimpleFeatureType, ExceptionConsumer) 初始化表头");
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

		}
		catch (Exception e) {
			if (exceptionConsumer != null) {
				exceptionConsumer.accept(e);
			}
			else {
				throw new RuntimeException("写入 GeoJSON 单行数据失败", e);
			}
		}
		return this;
	}

	/** 关闭资源（最终写入文件） */
	@Override
	public void close() {
		if (featureCollection == null || featureCollection.isEmpty() || !headerWritten) {
			return;
		}

		try (FileOutputStream fos = new FileOutputStream(new File(linkInfo.getGeoJsonFilePath()));
				OutputStreamWriter osw = new OutputStreamWriter(fos, Charset.forName(linkInfo.getCharset()))) {
			/** geojson的Writer实在是无解，没办法生成3857的geojson */
			GeoJSONWriter geoJsonWriter = new GeoJSONWriter(fos);
			int outPutSrid = writeConfig.getOutPutSrid();
			CoordinateReferenceSystem crs = GirAdvTools.getSridOpt().getCRS(outPutSrid);
			GutilReflection.setFieldValue(geoJsonWriter, "outCRS", crs);
			geoJsonWriter.setEncodeFeatureCollectionCRS(false);
			geoJsonWriter.setEncodeFeatureBounds(false);
			geoJsonWriter.writeFeatureCollection(featureCollection);
			geoJsonWriter.close();

			log.info("GeoJSON 文件写入完成，共写入 {} 条要素", featureCollection.size());

		}
		catch (Exception e) {
			throw new RuntimeException("关闭 GeoJSON 写入器并写入文件失败", e);
		}
	}

	// 可选：添加日志（如果项目有 slf4j 依赖）
	private void log(String msg) {
		Gir.log.info("[GeoJsonGeoFileWriter] " + msg);
	}

	private void log(String msg, Object... args) {
		Gir.log.info(String.format("[GeoJsonGeoFileWriter] " + msg, args));
	}

}
