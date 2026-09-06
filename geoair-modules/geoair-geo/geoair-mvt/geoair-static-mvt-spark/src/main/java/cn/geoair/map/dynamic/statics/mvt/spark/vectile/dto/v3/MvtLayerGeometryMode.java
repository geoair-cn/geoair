package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3;

/**
 * V3 多图层切片时写入 MVT 的几何表达方式。
 *
 * @author 张逢吉
 */
public enum MvtLayerGeometryMode {

    /** 保留数据源的原始几何。 */
    ORIGINAL,

    /** 使用原始几何的质心，适用于标注点图层。 */
    CENTROID,

    /** 使用原始几何的边界，适用于轮廓图层。 */
    BOUNDARY
}
