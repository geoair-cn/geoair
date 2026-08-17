package cn.geoair.map.dynamic.file.test.gj2pg;

import static cn.geoair.base.Gir.log;

import cn.geoair.base.percent.GiProgressListener;
import cn.geoair.base.percent.GirProgressReporter;
import cn.geoair.base.util.GutilPercent;
import cn.geoair.map.dynamic.file.core.enums.TranStatus;
import cn.geoair.map.dynamic.file.core.tran.GeoFileTran;
import cn.geoair.map.dynamic.file.core.tran.GeoFileTranImpl;
import cn.geoair.map.dynamic.file.core.tran.model.TranContext;
import cn.geoair.map.dynamic.file.core.tran.model.TranResult;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;
import cn.geoair.map.dynamic.file.geojson.GeoJsonGeoFileReader;
import cn.geoair.map.dynamic.file.geojson.GeoJsonLinkInfo;
import cn.geoair.map.dynamic.file.postgis.PostgisGeoFileWriter;
import cn.geoair.map.dynamic.file.postgis.PostgisLinkInfo;
import cn.geoair.map.dynamic.file.postgis.PostgisWriterLinkInfo;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

import java.io.IOException;

public class GeoJsonToPg {

    public static void main(String[] args) throws IOException {
        GeoJsonLinkInfo geoJsonLinkInfo =
                new GeoJsonLinkInfo()
                        .setGeoJsonFilePath("E:\\gis测试数据\\测试数据\\geojson\\poi.geojson")
                        .setCharset("UTF-8");
        GeoJsonGeoFileReader geoJsonReader = new GeoJsonGeoFileReader();
        geoJsonReader.setLinkInfo(geoJsonLinkInfo);
        long featureCount = geoJsonReader.getFeatureCount();
        System.out.println(featureCount);
        PostgisLinkInfo postgisWriterLinkInfo =
                new PostgisWriterLinkInfo()
                        .setBatchSize(5000).setTableName("t" + IdUtil.getSnowflakeNextIdStr())
                        .setJdbcUrl("jdbc:postgresql://192.168.0.110:5432/kashi_dth")
                        .setUsername("postgres")
                        .setPassword("tcsd2019")
                        .setSchema("public");

        PostgisGeoFileWriter postgisWriter = new PostgisGeoFileWriter();
        postgisWriter.setLinkInfo(postgisWriterLinkInfo);
        WriteConfig writeConfig = new WriteConfig().setOutPutSrid(4490);
        postgisWriter.setWriteConfig(writeConfig);
        TranContext context =
                new TranContext()
                        .setBatchSize(5000)
                        .setSkipErrorRecord(true)
                        .setTimeout(60 * 60 * 1000)
                        // 预处理：校验表是否存在
                        .setPreProcessor(
                                (reader, writer, ctx) -> {
                                    log.info("执行预处理：校验 PostGIS 表是否存在");
                                    // 自定义校验逻辑...
                                    return true; // 继续执行
                                })
                        // 后处理：归档结果
                        .setPostProcessor(
                                (result, ctx) -> {
                                    log.info("执行后处理：转换完成，归档结果");
                                    // 自定义归档逻辑（如写入日志表、发送通知）
                                    log.info("转换结果：{}", result);
                                })
                        // 自定义扩展参数
                        .putExtParam("tranId", "TRAN_20260209_001")
                        .putExtParam("operator", "admin");
        GirProgressReporter percentReporter = new GirProgressReporter(1, new GiProgressListener() {
            @Override
            public void onStart(Number total) {
                log.info("总数：{}", total);
            }

            @Override
            public void onUpdate(Number percent) {
                String progressBar = GutilPercent.getProgressBar(percent.intValue());
                log.info("{}: {}%", progressBar, percent);
            }
        });
        // 3. 构建转换处理器
        GeoFileTran tran =
                new GeoFileTranImpl()
                        // 全局异常处理器
                        .setExceptionConsumer(
                                e -> {
                                    log.error("全局异常：{}", e.getMessage());
                                })
                        // 进度监听器（实时回调）
                        .setProgressListener(
                                progress -> {
                                    percentReporter.report(progress.getTotalFeatureCount(), progress.getBatchTotalCount());
                                });

        // 4. 执行转换
        TranResult result = tran.transform(geoJsonReader, postgisWriter, context);

        // 5. 处理结果
        if (result.getStatus() == TranStatus.SUCCESS) {
            log.info(
                    "转换成功！总条数：{}，成功率：{}%，耗时：{}s",
                    result.getTotalCount(), result.getSuccessRate(), result.getElapsedTime()/1000);
        } else {
            log.error("转换失败！错误信息：{}，异常列表：{}", result.getErrorMsg(), result.getExceptions());
        }

        // 6. 关闭资源
        tran.close();
    }
}
