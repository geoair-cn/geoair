package cn.geoair.map.dynamic.statics.mvt.v4.input;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3GeoJsonInputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3JdbcInputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3LayerInputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3TileInputType;

/**
 * V4 的图层输入校验。
 *
 * <p><b>为什么不复用 V3 的 {@code V3FeatureReaderFactory.validate}：</b>
 * 那一版确实没有 Spark 的 import，但它的静态字段直接 new 了 V3 的两个读取器实现，
 * 而那两个类实现了 Spark 的函数接口 —— 没有 Spark 时 JVM 加载该类会
 * {@code NoClassDefFoundError}（实测踩到）。也就是说"没有 Spark 的 import"
 * 并不等于"可以在没有 Spark 的环境里用"，还要看它初始化时触达了谁。</p>
 *
 * <p>因此这里按 V3 的规则重写一份校验。规则逐条对齐，措辞改为 V4。</p>
 *
 * @author 张逢吉
 */
public final class V4InputValidator {

    private V4InputValidator() {
    }

    /** 校验图层及其介质专有输入参数。 */
    public static void validate(MvtLayerSliceParameter layer) {
        if (layer == null) {
            throw new IllegalArgumentException("V4 图层配置不能为空");
        }
        V3LayerInputConfig input = layer.getInputConfig();
        if (input == null || input.getInputType() == null) {
            throw new IllegalArgumentException(
                    "V4 图层 " + layer.getLayerName() + " 必须配置 inputConfig 与 inputType");
        }
        if (input.getInputType() == V3TileInputType.JDBC) {
            validateJdbc(layer, input.getJdbc());
            return;
        }
        if (input.getInputType() == V3TileInputType.GEOJSON) {
            validateGeoJson(layer, input.getGeoJson());
            return;
        }
        throw new IllegalArgumentException("V4 图层 " + layer.getLayerName()
                + " 使用了不支持的输入类型: " + input.getInputType());
    }

    private static void validateJdbc(MvtLayerSliceParameter layer, V3JdbcInputConfig jdbc) {
        if (jdbc == null || jdbc.getDataSource() == null) {
            throw new IllegalArgumentException(
                    "V4 JDBC 图层 " + layer.getLayerName() + " 的 dataSource 不能为空");
        }
        if (isBlank(jdbc.getQueryStatement())) {
            throw new IllegalArgumentException(
                    "V4 JDBC 图层 " + layer.getLayerName() + " 的 queryStatement 不能为空");
        }
        if (jdbc.getMaxPartitionNum() != null && jdbc.getMaxPartitionNum() <= 0) {
            throw new IllegalArgumentException(
                    "V4 JDBC 图层 " + layer.getLayerName() + " 的 maxPartitionNum 必须大于 0");
        }
    }

    private static void validateGeoJson(MvtLayerSliceParameter layer, V3GeoJsonInputConfig geoJson) {
        if (geoJson == null || geoJson.getPaths() == null || geoJson.getPaths().isEmpty()) {
            throw new IllegalArgumentException(
                    "V4 GeoJSON 图层 " + layer.getLayerName() + " 至少需要一个输入路径");
        }
        for (String path : geoJson.getPaths()) {
            if (isBlank(path)) {
                throw new IllegalArgumentException(
                        "V4 GeoJSON 图层 " + layer.getLayerName() + " 不能包含空路径");
            }
        }
        if (geoJson.getMinPartitionNum() != null && geoJson.getMinPartitionNum() <= 0) {
            throw new IllegalArgumentException(
                    "V4 GeoJSON 图层 " + layer.getLayerName() + " 的 minPartitionNum 必须大于 0");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
