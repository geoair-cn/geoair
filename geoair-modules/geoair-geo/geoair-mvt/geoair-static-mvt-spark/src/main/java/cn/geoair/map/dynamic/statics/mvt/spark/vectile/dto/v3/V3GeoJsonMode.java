package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3;

/**
 * V3 GeoJSON 文件组织方式。
 *
 * @author 张逢吉
 */
public enum V3GeoJsonMode {

    /** 根据路径扩展名选择读取方式；无法识别时按普通 GeoJSON 文档读取。 */
    AUTO,

    /** 标准 GeoJSON FeatureCollection 或单个 Feature 文档。 */
    FEATURE_COLLECTION,

    /** 每行一个 GeoJSON Feature，适合由 Spark 按文件块并行读取。 */
    GEOJSON_LINES
}
