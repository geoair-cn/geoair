package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v2;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.VectorTileCommonUtils;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

import java.io.Serializable;
import java.util.ArrayList;
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

    /**
     * 有界聚合函数 — 在合并过程中提前截断，避免内存膨胀。
     * <p>
     * 原版 {@code AggregateAndLimitFeatureFunction} 先完整合并两个 list，再截断。
     * 当高密度瓦片累积上万个要素时，合并瞬间内存翻倍。
     * <p>
     * 本函数在合并时即检查限制：如果两个 list 总和超过 limit，
     * 直接截取 list1（保留全部）+ list2 的前 N 个，不创建超大临时 list。
     * 然后再应用密度合并/过滤。
     */
    public static class BoundedAggregateFunction
            implements Function2<List<GirAdvOneRow>, List<GirAdvOneRow>, List<GirAdvOneRow>>,
            Serializable {

        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;

        public BoundedAggregateFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public List<GirAdvOneRow> call(List<GirAdvOneRow> list1, List<GirAdvOneRow> list2) throws Exception {
            // 无限制时直接合并
            if (!parameter.isFeatureLimitEnabled() || parameter.getFeatureLimit() == null) {
                List<GirAdvOneRow> merged = new ArrayList<>(list1.size() + list2.size());
                merged.addAll(list1);
                merged.addAll(list2);
                return merged;
            }

            int limit = parameter.getFeatureLimit();
            int totalSize = list1.size() + list2.size();

            // 未超限：正常合并
            if (totalSize <= limit) {
                List<GirAdvOneRow> merged = new ArrayList<>(totalSize);
                merged.addAll(list1);
                merged.addAll(list2);
                return merged;
            }

            // 超限：先有界合并（不超过 limit），再应用密度优化
            List<GirAdvOneRow> bounded = new ArrayList<>(limit);
            bounded.addAll(list1);
            int remaining = limit - list1.size();
            if (remaining > 0) {
                int take = Math.min(remaining, list2.size());
                bounded.addAll(list2.subList(0, take));
            }
            // 应用密度合并/过滤/截断（在已限制大小的 list 上操作）
            return VectorTileCommonUtils.limitTileFeatures(bounded, parameter);
        }
    }
}
