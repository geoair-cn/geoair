package cn.geoair.map.dynamic.file.test.gj2pg;

import static cn.geoair.base.Gir.log;

import cn.geoair.map.dynamic.file.core.enums.TranStatus;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import cn.geoair.map.dynamic.file.core.tran.GeoFileTran;
import cn.geoair.map.dynamic.file.core.tran.GeoFileTranImpl;
import cn.geoair.map.dynamic.file.core.tran.model.TranContext;
import cn.geoair.map.dynamic.file.core.tran.model.TranResult;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;
import cn.geoair.map.dynamic.file.geojson.GeoJsonGeoFileWriter;
import cn.geoair.map.dynamic.file.geojson.GeoJsonLinkInfo;
import cn.geoair.map.dynamic.file.postgis.*;

import java.io.IOException;

public class PgToGeoJson {

	public static void main(String[] args) throws IOException {

		LinkInfo writeLink = new GeoJsonLinkInfo()
				.setGeoJsonFilePath("C:\\Users\\Administrator\\Documents\\6666.geojson").setCharset("UTF-8");
		GeoFileWriter geoFileWriter = new GeoJsonGeoFileWriter();
		geoFileWriter.setLinkInfo(writeLink);

		WriteConfig writeConfig = new WriteConfig().setOutPutSrid(3857);
		geoFileWriter.setWriteConfig(writeConfig);

		PostgisLinkInfo postgisLinkInfo = new PostgisReadLinkInfo().setQuerySqlByOutPut("select * from geo_tran_demo ")
				.setJdbcUrl("jdbc:postgresql://192.168.0.110:5432/kashi_dth").setUsername("postgres")
				.setPassword("tcsd2019").setSchema("public");
		GeoFileReader geoFileReader = new PostgisGeoFileReader();
		geoFileReader.setLinkInfo(postgisLinkInfo);

		TranContext context = new TranContext().setBatchLogThreshold(500).setSkipErrorRecord(true)
				.setTimeout(60 * 60 * 1000)
				// 预处理：校验表是否存在
				.setPreProcessor((reader, writer, ctx) -> {
					log.info("执行预处理：校验 PostGIS 表是否存在");
					// 自定义校验逻辑...
					return true; // 继续执行
				})
				// 后处理：归档结果
				.setPostProcessor((result, ctx) -> {
					log.info("执行后处理：转换完成，归档结果");
					// 自定义归档逻辑（如写入日志表、发送通知）
					log.info("转换结果：{}", result);
				})
				// 自定义扩展参数
				.putExtParam("tranId", "TRAN_20260209_001").putExtParam("operator", "admin");

		// 3. 构建转换处理器
		GeoFileTran tran = new GeoFileTranImpl()
				// 全局异常处理器
				.setExceptionConsumer(e -> {
					log.error("全局异常：{}", e.getMessage());
				})
				// 进度监听器（实时回调）
				.setProgressListener(progress -> {
					log.info(String.format("进度更新：已处理 %d 条，成功率 %.2f%%，状态 %s", progress.getTotalCount(),
							progress.getSuccessRate(), progress.getStatus()));
				});

		// 4. 执行转换
		TranResult result = tran.transform(geoFileReader, geoFileWriter, context);

		// 5. 处理结果
		if (result.getStatus() == TranStatus.SUCCESS) {
			log.info("转换成功！总条数：{}，成功率：{:.2f}%，耗时：{}ms", result.getTotalCount(), result.getSuccessRate(),
					result.getElapsedTime());
		}
		else {
			log.error("转换失败！错误信息：{}，异常列表：{}", result.getErrorMsg(), result.getExceptions());
		}

		// 6. 关闭资源
		tran.close();
	}

}
