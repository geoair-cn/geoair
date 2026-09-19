package cn.geoair.map.dynamic.statics.mvt.v4.input;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;

/**
 * V4 图层要素读取器：把图层声明的输入介质读成一条条 {@code GirAdvOneRow}。
 *
 * <p>与 V3 的 {@code V3FeatureReader} 的区别只有一个：V4 的签名里没有
 * {@code SparkSession}，也不需要返回 RDD —— 读取就是一次顺序遍历 + 回调。</p>
 *
 * @author 张逢吉
 */
public interface V4FeatureReader {

    /**
     * 顺序读取一个图层的全部要素。
     *
     * @param layer    图层参数（输入介质、几何字段、坐标系等都从这里取）
     * @param consumer 行回调
     */
    void read(MvtLayerSliceParameter layer, V4RowConsumer consumer) throws Exception;
}
