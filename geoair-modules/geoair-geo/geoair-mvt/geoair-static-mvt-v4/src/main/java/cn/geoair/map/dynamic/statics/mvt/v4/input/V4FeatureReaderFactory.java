package cn.geoair.map.dynamic.statics.mvt.v4.input;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3TileInputType;

/**
 * V4 输入读取器工厂。
 *
 * <p>参数校验用 V4 自己的 {@link V4InputValidator}。<b>不能复用 V3 的
 * {@code V3FeatureReaderFactory.validate}</b>：那一版的静态字段会实例化 V3 的两个 Spark 耦合
 * 读取器，没有 Spark 时类加载直接失败（实测 {@code NoClassDefFoundError}）。</p>
 *
 * @author 张逢吉
 */
public final class V4FeatureReaderFactory {

    private static final V4FeatureReader JDBC_READER = new V4JdbcFeatureReader();
    private static final V4FeatureReader GEOJSON_READER = new V4GeoJsonFeatureReader();

    private V4FeatureReaderFactory() {
    }

    /** 按图层的输入类型取无状态读取器。 */
    public static V4FeatureReader getReader(MvtLayerSliceParameter layer) {
        V4InputValidator.validate(layer);
        return layer.getInputConfig().getInputType() == V3TileInputType.JDBC
                ? JDBC_READER : GEOJSON_READER;
    }
}
