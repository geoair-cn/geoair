package cn.geoair.map.dynamic.statics.mvt.spark.vectile;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.SparkVectorTileGenerator;
import org.apache.spark.sql.SparkSession;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/1/7 15:04 @description： 通过集群驱动该切片流程
 */
public class SparkJavaTileApp {

    public static void main(String[] args) throws Exception {

        String base32 = args[0];

        TileSliceParameter tileSliceParameter = TileSliceParameter.fromBase32(base32);

        SparkSession spark = SparkSession.builder().appName("spark-tile-vec-tile").getOrCreate();

        SparkVectorTileGenerator sparkVectorTileGenerator =
                new SparkVectorTileGenerator(spark);

        sparkVectorTileGenerator.doGenerate(tileSliceParameter);
    }
}
