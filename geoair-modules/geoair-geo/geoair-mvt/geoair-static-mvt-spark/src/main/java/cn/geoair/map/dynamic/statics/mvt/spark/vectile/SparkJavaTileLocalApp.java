package cn.geoair.map.dynamic.statics.mvt.spark.vectile;

import org.apache.spark.sql.SparkSession;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.SparkVectorTileGeneratorAll;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/1/7 15:04 @description： 通过本地启动该切片流程
 */
public class SparkJavaTileLocalApp {

	public static void main(String[] args) throws Exception {

		String base32 = args[0];

		TileSliceParameter tileSliceParameter = TileSliceParameter.fromBase32(base32);

		SparkSession spark = SparkSession.builder().appName("spark-tile-app").master("local[*]")
				.config("spark.executor.memory", "4g") // Executor 内存
				.config("spark.driver.memory", "4g") // Driver 内存
				.config("spark.extraListeners", "cn.geoair.map.dynamic.statics.mvt.spark.listener.SparkSQLListener") // 自定义监听器

				.getOrCreate();

		SparkVectorTileGeneratorAll sparkVectorTileGenerator = new SparkVectorTileGeneratorAll(spark);

		sparkVectorTileGenerator.doGenerate(tileSliceParameter);

		spark.stop();
	}

}
