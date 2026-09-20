package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * V3 单个内部图层的输入配置。
 *
 * <p>使用显式类型和对应的配置对象隔离 JDBC 与 GeoJSON 参数，避免文件输入继续携带
 * SQL、分页策略等无意义字段。</p>
 *
 * @author 张逢吉
 */
@Data
@Accessors(chain = true)
public class V3LayerInputConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 输入介质。 */
    private V3TileInputType inputType;

    /** JDBC 输入参数，仅 JDBC 类型使用。 */
    private V3JdbcInputConfig jdbc;

    /** GeoJSON 输入参数，仅 GEOJSON 类型使用。 */
    private V3GeoJsonInputConfig geoJson;

    /** 创建 JDBC 输入配置。 */
    public static V3LayerInputConfig jdbc(V3JdbcInputConfig config) {
        return new V3LayerInputConfig().setInputType(V3TileInputType.JDBC).setJdbc(config);
    }

    /** 创建 GeoJSON 输入配置。 */
    public static V3LayerInputConfig geoJson(V3GeoJsonInputConfig config) {
        return new V3LayerInputConfig().setInputType(V3TileInputType.GEOJSON).setGeoJson(config);
    }
}
