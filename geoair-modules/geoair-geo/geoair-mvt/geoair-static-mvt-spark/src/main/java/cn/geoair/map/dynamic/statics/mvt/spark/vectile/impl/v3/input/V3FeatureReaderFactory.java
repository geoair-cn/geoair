package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.input;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3GeoJsonInputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3JdbcInputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3LayerInputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3TileInputType;

/**
 * V3 图层输入读取器工厂与参数校验入口。
 *
 * @author 张逢吉
 */
public final class V3FeatureReaderFactory {

    private static final V3FeatureReader JDBC_READER = new V3JdbcFeatureReader();
    private static final V3FeatureReader GEOJSON_READER = new V3GeoJsonFeatureReader();

    private V3FeatureReaderFactory() {
    }

    /** 根据图层输入类型返回无状态读取器。 */
    public static V3FeatureReader getReader(MvtLayerSliceParameter layer) {
        validate(layer);
        return layer.getInputConfig().getInputType() == V3TileInputType.JDBC
                ? JDBC_READER : GEOJSON_READER;
    }

    /** 校验图层及其介质专有输入参数。 */
    public static void validate(MvtLayerSliceParameter layer) {
        if (layer == null) {
            throw new IllegalArgumentException("V3 图层配置不能为空");
        }
        V3LayerInputConfig input = layer.getInputConfig();
        if (input == null || input.getInputType() == null) {
            throw new IllegalArgumentException("V3 图层 " + layer.getLayerName() + " 必须配置 inputConfig 与 inputType");
        }
        if (input.getInputType() == V3TileInputType.JDBC) {
            validateJdbc(layer, input.getJdbc());
            return;
        }
        if (input.getInputType() == V3TileInputType.GEOJSON) {
            validateGeoJson(layer, input.getGeoJson());
            return;
        }
        throw new IllegalArgumentException("V3 图层 " + layer.getLayerName() + " 使用了不支持的输入类型: "
                + input.getInputType());
    }

    private static void validateJdbc(MvtLayerSliceParameter layer, V3JdbcInputConfig jdbc) {
        if (jdbc == null || jdbc.getDataSource() == null) {
            throw new IllegalArgumentException("V3 JDBC 图层 " + layer.getLayerName() + " 的 dataSource 不能为空");
        }
        if (isBlank(jdbc.getQueryStatement())) {
            throw new IllegalArgumentException("V3 JDBC 图层 " + layer.getLayerName() + " 的 queryStatement 不能为空");
        }
        if (jdbc.getMaxPartitionNum() != null && jdbc.getMaxPartitionNum() <= 0) {
            throw new IllegalArgumentException("V3 JDBC 图层 " + layer.getLayerName() + " 的 maxPartitionNum 必须大于 0");
        }
    }

    private static void validateGeoJson(MvtLayerSliceParameter layer, V3GeoJsonInputConfig geoJson) {
        if (geoJson == null || geoJson.getPaths() == null || geoJson.getPaths().isEmpty()) {
            throw new IllegalArgumentException("V3 GeoJSON 图层 " + layer.getLayerName() + " 至少需要一个输入路径");
        }
        for (String path : geoJson.getPaths()) {
            if (isBlank(path)) {
                throw new IllegalArgumentException("V3 GeoJSON 图层 " + layer.getLayerName() + " 不能包含空路径");
            }
        }
        if (geoJson.getMinPartitionNum() != null && geoJson.getMinPartitionNum() <= 0) {
            throw new IllegalArgumentException("V3 GeoJSON 图层 " + layer.getLayerName() + " 的 minPartitionNum 必须大于 0");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
