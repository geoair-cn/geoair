package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v2;

import cn.geoair.base.percent.GiProgressReporter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import org.apache.spark.sql.SparkSession;

/**
 * V2 版本的本地启动入口（内存优化版 + 进度条）。
 * <p>
 * 相对于原版 {@code SparkJavaTileLocalApp} 的改进：
 * <ul>
 *   <li>使用 {@link SparkVectorTileGeneratorV2} 替代原版生成器</li>
 *   <li>支持通过 {@link GiProgressReporter} 回调进度</li>
 *   <li>可通过 {@code -Dprogress.reporter=xxx} 指定自定义进度回调类名（暂未实现，预留）</li>
 * </ul>
 *
 * @author refactored from SparkJavaTileLocalApp
 */
public class SparkJavaTileLocalAppV2 {

    public static void main(String[] args) throws Exception {
        String base32 = args[0];
        TileSliceParameter tileSliceParameter = TileSliceParameter.fromBase32(base32);
        runByTileSliceParameter(tileSliceParameter);
    }

    /**
     * 无进度回调的启动方式。
     */
    public static void runByTileSliceParameter(TileSliceParameter tileSliceParameter) throws Exception {
        runByTileSliceParameter(tileSliceParameter, null);
    }

    /**
     * 带进度回调的启动方式。
     *
     * @param tileSliceParameter 切片参数
     * @param percentReporter    进度上报回调（可为 null），在 executor 线程中直接调用，
     *                           report(allCount, currentCount)
     */
    public static void runByTileSliceParameter(TileSliceParameter tileSliceParameter,
                                               GiProgressReporter percentReporter) throws Exception {

        SparkSession spark = SparkSession.builder()
                .appName("spark-tile-app-v2")
                .master("local[*]")
                .config("spark.executor.memory", "4g")
                .config("spark.driver.memory", "8g")
                .config("spark.extraListeners",
                        "cn.geoair.map.dynamic.statics.mvt.spark.listener.SparkSQLListener")
                .getOrCreate();

        try {
            SparkVectorTileGeneratorV2 generator = new SparkVectorTileGeneratorV2(spark);
            generator.doGenerate(tileSliceParameter, percentReporter);
        } finally {
            spark.stop();
        }
    }
}
