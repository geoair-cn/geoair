package cn.geoair.map.dynamic.file.test.shp2pg;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.enums.TranStatus;
import cn.geoair.map.dynamic.file.core.tran.GeoFileTran;
import cn.geoair.map.dynamic.file.core.tran.GeoFileTranImpl;
import cn.geoair.map.dynamic.file.core.tran.model.TranContext;
import cn.geoair.map.dynamic.file.core.tran.model.TranResult;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;
import cn.geoair.map.dynamic.file.postgis.PostgisGeoFileWriter;
import cn.geoair.map.dynamic.file.postgis.PostgisLinkInfo;
import cn.geoair.map.dynamic.file.postgis.PostgisWriterLinkInfo;
import cn.geoair.map.dynamic.file.shp.ShpGeoFileReader;
import cn.geoair.map.dynamic.file.shp.ShpLinkInfo;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.hutool.core.util.IdUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.geotools.api.feature.simple.SimpleFeatureType;

import java.io.IOException;
import java.util.function.Consumer;

import static cn.geoair.base.Gir.log;

public class ShpToPg {

    public static void main(String[] args) throws IOException {
        ShpLinkInfo linkInfo =
                new ShpLinkInfo()
                        .setShpFilePath("H:\\项目文件\\黑龙江\\党政机关\\党政机关.shp")
                        .setCharset("UTF-8");
        ShpGeoFileReader fileReader = new ShpGeoFileReader();
        fileReader.setLinkInfo(linkInfo);

        PostgisLinkInfo postgisWriterLinkInfo =
                new PostgisWriterLinkInfo()
                        .setTableName("shp_pg_test_" + IdUtil.getSnowflakeNextIdStr())
                        .setJdbcUrl("jdbc:postgresql://192.168.0.110:5432/demo")
                        .setUsername("postgres")
                        .setPassword("tcsd2019")
                        .setSchema("public");

        PostgisGeoFileWriter postgisWriter = new PostgisGeoFileWriter();
        postgisWriter.setLinkInfo(postgisWriterLinkInfo);
        WriteConfig writeConfig = new WriteConfig().setOutPutSrid(4326);
        postgisWriter.setWriteConfig(writeConfig);
        TranContext context =
                new TranContext()
                        .setBatchLogThreshold(500)
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

        // 3. 构建转换处理器
        GeoFileTran tran =
                new GeoFileTranImpl().setHeadConsumer(new Consumer<SimpleFeatureType>() {
                            @Override
                            public void accept(SimpleFeatureType simpleFeatureType) {
//                                GeoToolsUtils.addFieldToFeatureType(simpleFeatureType,"oldGeomWkt",String.class);
//                                GeoToolsUtils.addFieldToFeatureType(simpleFeatureType,"oldGeom",Geometry.class);
                            }
                        })

                        .setGirAdvOneRowConsumer(new Consumer<GirAdvOneRow>() {
                            @Override
                            public void accept(GirAdvOneRow girAdvOneRow) {
                                Geometry geometry = girAdvOneRow.getGeometry("the_geom");
                                if (geometry != null) {
                                    if (geometry instanceof Point) {
                                        Point point = (Point) geometry;
                                        Point point1 = GirGeoTools.defaultInstance().getCoordinateOpt().wgs84ToBd09(point);
                                        point1.setSRID(point.getSRID());
                                        girAdvOneRow.put("the_geom", point1);
                                    }
//                                    girAdvOneRow.put("oldGeomWkt", GirGeoTools.me().getFormatOpt().jtsGeometryToWktString(geometry,true));
//                                    girAdvOneRow.put("oldGeom", geometry);
                                }
                            }
                        })
                        // 全局异常处理器
                        .setExceptionConsumer(
                                e -> {
                                    log.error("全局异常：{}", e.getMessage());
                                })
                        // 进度监听器（实时回调）
                        .setProgressListener(
                                progress -> {
                                    log.info(
                                            String.format(
                                                    "进度更新：已处理 %d 条，成功率 %.2f%%，状态 %s",
                                                    progress.getTotalCount(),
                                                    progress.getSuccessRate(),
                                                    progress.getStatus()));
                                });

        // 4. 执行转换
        TranResult result = tran.transform(fileReader, postgisWriter, context);

        // 5. 处理结果
        if (result.getStatus() == TranStatus.SUCCESS) {
            log.info(
                    "转换成功！总条数：{}，成功率：{:.2f}%，耗时：{}ms",
                    result.getTotalCount(), result.getSuccessRate(), result.getElapsedTime());
        } else {
            log.error("转换失败！错误信息：{}，异常列表：{}", result.getErrorMsg(), result.getExceptions());
        }

        // 6. 关闭资源
        tran.close();
    }
}
