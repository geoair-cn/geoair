package cn.geoair.map.dynamic.file.test.gj2jdbc;

import cn.geoair.map.dynamic.file.core.enums.TranStatus;
import cn.geoair.map.dynamic.file.core.transfer.ErrorPolicy;
import cn.geoair.map.dynamic.file.core.transfer.GeoTransferEngine;
import cn.geoair.map.dynamic.file.core.transfer.GeoTransferOptions;
import cn.geoair.map.dynamic.file.core.transfer.GeoTransferRequest;
import cn.geoair.map.dynamic.file.core.transfer.GeoTransferResult;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;
import cn.geoair.map.dynamic.file.geojson.GeoJsonGeoFileReader;
import cn.geoair.map.dynamic.file.geojson.GeoJsonLinkInfo;
import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoWriterLinkInfo;

import static cn.geoair.base.Gir.log;

/**
 * GeoJSON 写入 JDBC 空间数据库的 Main 测试公共逻辑。
 *
 * <p>具体数据库入口只负责填写连接配置；这里保持与 {@code gj2pg} 示例相同的
 * 进度、异常和结果输出方式，同时验证新的 Core V2 传输引擎。</p>
 *
 * @author 张逢吉
 */
final class GeoJsonToJdbcTransferSupport {

    private GeoJsonToJdbcTransferSupport() {
    }

    static void transfer(String databaseName, JdbcGeoWriterLinkInfo targetLinkInfo, GeoFileWriter writer) {
        String geoJsonPath = requireProperty("geoair.file.geojson");
        int srid = intProperty("geoair.file.srid", 4326);
        int batchSize = intProperty("geoair.file.batch-size", 1000);

        GeoJsonGeoFileReader reader = new GeoJsonGeoFileReader();
        GeoJsonLinkInfo readerLinkInfo = new GeoJsonLinkInfo();
        readerLinkInfo.setGeoJsonFilePath(geoJsonPath);
        readerLinkInfo.setCharset(System.getProperty("geoair.file.charset", "UTF-8"));
        readerLinkInfo.setSrid(srid);
        reader.setLinkInfo(readerLinkInfo);

        targetLinkInfo.setBatchSize(batchSize);
        targetLinkInfo.setSrid(srid);
        writer.setLinkInfo(targetLinkInfo);

        GeoTransferRequest request = new GeoTransferRequest();
        request.setReader(reader);
        request.setWriter(writer);
        request.setWriteConfig(new WriteConfig().setOutPutSrid(srid).setOverwrite(true));
        request.setOptions(new GeoTransferOptions()
                .setBatchSize(batchSize)
                .setErrorPolicy(ErrorPolicy.SKIP_RECORD)
                .setTimeoutMillis(60L * 60L * 1000L));
        request.setProgressListener(progress -> log.info(
                "{} 传输进度：已处理 {} 条，成功 {} 条，失败 {} 条，成功率 {}%",
                databaseName,
                progress.getBatchTotalCount(),
                progress.getBatchSuccessCount(),
                progress.getBatchFailCount(),
                progress.getSuccessRate()));

        GeoTransferResult result = GeoTransferEngine.create().execute(request);
        if (result.getStatus() == TranStatus.SUCCESS) {
            log.info("{} 转换成功：总数={}，成功={}，失败={}，耗时={}ms",
                    databaseName,
                    result.getTotalCount(),
                    result.getSuccessCount(),
                    result.getFailCount(),
                    result.getElapsedTime());
            return;
        }
        log.error("{} 转换失败：{}，异常={}", databaseName, result.getErrorMessage(), result.getExceptions());
        throw new IllegalStateException(databaseName + " GeoJSON 转换失败：" + result.getErrorMessage());
    }

    static String requireProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("请在 IDEA Run Configuration 的 VM options 中设置 -D"
                    + propertyName + "=对应值");
        }
        return value.trim();
    }

    static void setOptionalProperty(String propertyName, java.util.function.Consumer<String> consumer) {
        String value = System.getProperty(propertyName);
        if (value != null && !value.trim().isEmpty()) {
            consumer.accept(value.trim());
        }
    }

    private static int intProperty(String propertyName, int defaultValue) {
        String value = System.getProperty(propertyName);
        return value == null || value.trim().isEmpty() ? defaultValue : Integer.parseInt(value.trim());
    }
}
