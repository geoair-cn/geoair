package cn.geoair.map.dynamic.statics.mvt.v4.group;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.V3TileFeatureGroup;

/**
 * V4 聚合结果的回调：一个瓦片键对应一个聚合单元。
 *
 * <p>与 V3 的差别：V3 用 {@code reduceByKey} 把结果交给下游 RDD，V4 直接在回调里
 * 编码并写出 —— 同一个瓦片只在<b>一个</b>回调里出现，因此不存在并发覆盖同一瓦片的问题。</p>
 *
 * @author 张逢吉
 */
@FunctionalInterface
public interface V4TileConsumer {

    /**
     * 消费一个已完成聚合的瓦片。
     *
     * @param tileKey Bing QuadKey，与 V3 的聚合键完全同源
     * @param group   按 MVT 内部图层归集的要素
     */
    void accept(String tileKey, V3TileFeatureGroup group) throws Exception;
}
