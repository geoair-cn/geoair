package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v1.SparkVectorTileGeneratorV1;
import org.apache.spark.sql.SparkSession;

/**
 * V1 生成器的兼容入口。
 *
 * @deprecated 请迁移至 {@link SparkVectorTileGeneratorV1}。
 */
@Deprecated
public class SparkVectorTileGenerator extends SparkVectorTileGeneratorV1 {

    public SparkVectorTileGenerator(SparkSession sparkSession) {
        super(sparkSession);
    }
}
