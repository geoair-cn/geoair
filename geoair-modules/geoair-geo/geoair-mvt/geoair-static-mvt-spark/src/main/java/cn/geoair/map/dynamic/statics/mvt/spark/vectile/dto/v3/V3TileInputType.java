package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3;

/**
 * V3 图层输入介质。
 *
 * @author 张逢吉
 */
public enum V3TileInputType {

    /** 通过 JDBC 查询空间数据。 */
    JDBC,

    /** 从 GeoJSON 文档或 GeoJSON Lines 文件读取空间数据。 */
    GEOJSON
}
