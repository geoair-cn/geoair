package cn.geoair.map.dynamic.statics.mvt.spark.vectile;

/**
 * 空间数据读取策略
 */
public enum ReadStrategy {
    /** 按ID分页分片（避免空间分片不均） */
    ID_PAGE,
    /** 按空间范围BBox分片（空间均匀分片） */
    BBOX
}
