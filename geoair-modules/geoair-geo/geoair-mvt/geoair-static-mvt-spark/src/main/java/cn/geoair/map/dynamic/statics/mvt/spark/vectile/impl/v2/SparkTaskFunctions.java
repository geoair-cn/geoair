package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v2;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.VectorTileCommonUtils;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * V2 版本专用的 Spark 函数集合。
 * <p>
 * 仅包含 V2 相对于原版新增或修改的函数。
 * 其余函数（IdPageFlatMapFunction、BboxFlatMapFunction、TransformFeatureFunction、
 * MapToTileFunction、AggregateAndLimitFeatureFunction 等）直接复用原版
 * {@link cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.SparkTaskSerializableUtil}。
 */
public class SparkTaskFunctions implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 单 zoom 级别的流式瓦片映射函数。
     * <p>
     * 用于统计路径，替代原版 {@code MapToTileFunctionToStatic}（HashMap 全量收集版本）。
     * 使用 {@link cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.TileIterator}
     * 懒生成瓦片，避免 OOM。
     */
    public static class MapToTileFunctionSingleZoom
            implements PairFlatMapFunction<GirAdvOneRow, String, List<GirAdvOneRow>>, Serializable {

        private static final long serialVersionUID = 1L;

        private final TileSliceParameter parameter;
        private final int zoom;

        public MapToTileFunctionSingleZoom(TileSliceParameter parameter, int zoom) {
            this.parameter = parameter;
            this.zoom = zoom;
        }

        @Override
        public Iterator<Tuple2<String, List<GirAdvOneRow>>> call(GirAdvOneRow feature) throws Exception {
            if (feature == null) {
                return Collections.emptyIterator();
            }
            // 使用流式版本，只映射到指定的单一 zoom 级别
            return VectorTileCommonUtils.mapSingleFeatureToTilesStream(
                    feature,
                    parameter.getGeomFieldName(),
                    zoom,
                    zoom,
                    parameter.getOutGridSrid());
        }
    }
}
