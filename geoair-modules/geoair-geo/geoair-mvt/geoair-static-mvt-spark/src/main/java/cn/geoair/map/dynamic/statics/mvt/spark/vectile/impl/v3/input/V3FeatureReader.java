package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.input;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.SparkSession;

/**
 * V3 图层要素输入读取器。
 *
 * @author 张逢吉
 */
public interface V3FeatureReader {

    /**
     * 将一种输入介质读取并归一化为 V3 后续切片链路使用的行对象。
     *
     * @param sparkSession Spark 会话
     * @param layer V3 内部图层配置
     * @return 延迟执行的 Spark RDD
     * @throws Exception 输入配置或读取准备失败
     */
    JavaRDD<GirAdvOneRow> read(SparkSession sparkSession, MvtLayerSliceParameter layer) throws Exception;
}
