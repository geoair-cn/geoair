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
 *
 * <p>仅包含 V2 相对于原版新增或修改的函数。 其余函数（IdPageFlatMapFunction、BboxFlatMapFunction、TransformFeatureFunction、
 * MapToTileFunction、AggregateAndLimitFeatureFunction 等）直接复用原版 {@link
 * cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.SparkTaskSerializableUtil}。
 */
public class SparkTaskFunctions implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 单 zoom 级别的流式瓦片映射函数。
     *
     * <p>用于统计路径，替代原版 {@code MapToTileFunctionToStatic}（HashMap 全量收集版本）。 使用 {@link
     * cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.TileIterator} 懒生成瓦片，避免 OOM。
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
        public Iterator<Tuple2<String, List<GirAdvOneRow>>> call(GirAdvOneRow feature)
                throws Exception {
            if (feature == null) {
                return Collections.emptyIterator();
            }
            // 使用流式版本，只映射到指定的单一 zoom 级别
            return VectorTileCommonUtils.mapSingleFeatureToTilesStream(
                    feature, parameter.getGeomFieldName(), zoom, zoom, parameter.getOutGridSrid());
        }
    }

    /**
     * 有界聚合函数 — 始终限制合并后的列表大小，防止 OOM。
     *
     * <p>即使用户未开启 featureLimitEnabled，也使用 DEFAULT_HARD_LIMIT 作为安全兜底， 避免高密度瓦片（如 zoom 4-6
     * 覆盖大面积区域）累积数万要素导致内存溢出。
     *
     * <p>策略：
     *
     * <ul>
     *   <li>合并前先检查两个 list 的总大小
     *   <li>超限时只保留 list1 全部 + list2 的前 N 个，不创建超大临时 list
     *   <li>合并后再应用密度优化/截断
     * </ul>
     */
    public static class BoundedAggregateFunction
            implements Function2<List<GirAdvOneRow>, List<GirAdvOneRow>, List<GirAdvOneRow>>,
                    Serializable {

        private static final long serialVersionUID = 1L;

        /** 硬上限：即使用户未配置 featureLimit，也最多保留此数量的要素。 用 CPU 换内存 —— 多余的要素会被丢弃，但不会 OOM。 */
        private static final int DEFAULT_HARD_LIMIT = 8000;

        private final TileSliceParameter parameter;

        public BoundedAggregateFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public List<GirAdvOneRow> call(List<GirAdvOneRow> list1, List<GirAdvOneRow> list2)
                throws Exception {
            int limit = getEffectiveLimit();
            int totalSize = list1.size() + list2.size();

            // 未超限：正常合并
            if (totalSize <= limit) {
                List<GirAdvOneRow> merged = new ArrayList<>(totalSize);
                merged.addAll(list1);
                merged.addAll(list2);
                return merged;
            }

            // 超限：有界合并（不超过 limit），再应用密度优化
            List<GirAdvOneRow> bounded = new ArrayList<>(limit);
            bounded.addAll(list1);
            int remaining = limit - list1.size();
            if (remaining > 0) {
                int take = Math.min(remaining, list2.size());
                bounded.addAll(list2.subList(0, take));
            }
            return V2VectorTileUtils.limitTileFeatures(bounded, parameter);
        }

        /** 获取有效限制值：用户配置优先，否则使用硬上限兜底。 */
        private int getEffectiveLimit() {
            if (parameter.isFeatureLimitEnabled() && parameter.getFeatureLimit() != null) {
                return parameter.getFeatureLimit();
            }
            return DEFAULT_HARD_LIMIT;
        }
    }
}
