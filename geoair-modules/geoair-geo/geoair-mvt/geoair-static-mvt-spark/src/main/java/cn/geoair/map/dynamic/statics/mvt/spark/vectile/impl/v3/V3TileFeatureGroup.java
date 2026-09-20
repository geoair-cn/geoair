package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 同一 XYZ 瓦片内按 MVT 内部图层分组的要素集合。
 *
 * @author 张逢吉
 */
@Data
@NoArgsConstructor
public class V3TileFeatureGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 合并时触发截断的倍数：要素数超过「安全上限 × 该倍数」才真正截断。
     * <p>截断本身要整列表拷贝一次，按倍数延迟执行可以把这份开销摊薄到几乎为零。</p>
     */
    private static final int CAP_TRIGGER_FACTOR = 2;

    /** key 为 MVT 内部图层名，使用 LinkedHashMap 保持任务配置顺序。 */
    private Map<String, List<GirAdvOneRow>> featuresByLayer = new LinkedHashMap<>();

    /** 创建只有一个图层要素的聚合单元。 */
    public static V3TileFeatureGroup single(String layerName, List<GirAdvOneRow> features) {
        V3TileFeatureGroup group = new V3TileFeatureGroup();
        group.featuresByLayer.put(layerName, new ArrayList<>(features));
        return group;
    }

    /**
     * 合并两个瓦片聚合单元，并在每个图层维度施加安全上限。
     * <p>
     * 此方法始终创建新列表，不修改 Spark 上游 RDD 的 value，避免不同 stage 重试时发生副作用。
     * <p>
     * <b>这里只做"防 OOM 的兜底削减"（按身份哈希抽样），不做要素数限制与密度裁剪。</b>
     * 该方法在 {@code reduceByKey} 中会被逐次调用（同一瓦片的第 N 条记录就调用 N-1 次），
     * 任何带排序或几何序列化的处理放到这里都会被放大成平方级开销；真正的要素数限制、
     * 密度合并与丢弃统一放在编码前做一次（见 {@code MultiLayerMvtEncoderV3}）。
     */
    public V3TileFeatureGroup merge(
            V3TileFeatureGroup other,
            Map<String, MvtLayerSliceParameter> layerParameters,
            int outGridSrid) {
        V3TileFeatureGroup merged = new V3TileFeatureGroup();
        copyInto(merged.featuresByLayer, this.featuresByLayer);
        if (other != null) {
            for (Map.Entry<String, List<GirAdvOneRow>> entry : other.featuresByLayer.entrySet()) {
                List<GirAdvOneRow> target = merged.featuresByLayer.computeIfAbsent(
                        entry.getKey(), key -> new ArrayList<>());
                target.addAll(entry.getValue());
            }
        }
        for (Map.Entry<String, List<GirAdvOneRow>> entry : merged.featuresByLayer.entrySet()) {
            MvtLayerSliceParameter layer = layerParameters.get(entry.getKey());
            if (layer != null) {
                entry.setValue(capLayerFeatures(entry.getValue(), layer));
            }
        }
        return merged;
    }

    /** 创建深层列表副本，供编码前按总 PBF 大小裁剪。 */
    public Map<String, List<GirAdvOneRow>> copyFeaturesByLayer() {
        Map<String, List<GirAdvOneRow>> copy = new LinkedHashMap<>();
        copyInto(copy, featuresByLayer);
        return copy;
    }

    private static void copyInto(
            Map<String, List<GirAdvOneRow>> target, Map<String, List<GirAdvOneRow>> source) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, List<GirAdvOneRow>> entry : source.entrySet()) {
            target.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
    }

    /**
     * 聚合阶段的安全阀：要素数明显超过图层声明的安全上限时削减到上限，防止 executor OOM。
     * <p>只在超过「上限 × {@value #CAP_TRIGGER_FACTOR}」时才执行，避免每合并一次就拷贝一遍列表。
     * <p><b>削减必须按要素身份、且必须取身份哈希最小的 N 个，不能按位置也不能按比例过滤。</b>
     * 本方法处在 {@code reduceByKey} 里，同一块瓦片每来一批记录就调用一次：
     * <ul>
     *   <li>按位置（取前缀、等间隔）时，两次调用之间列表还在增长，等于把同一批要素反复重采样，
     *       早到达的被反复砍到、晚到达的只碰上一两次，最终只剩尾部数据。实测低层级
     *       （一块瓦片七十余万要素）触发近百次后，被削空的是一条完整的纬度带，地图上就是一大块空白。</li>
     *   <li>按比例过滤（保留哈希值小于阈值的）时，上一轮保下来的要素必然仍在阈值内，削减量只落在
     *       新来的要素上，列表长度削不下去；实测会一路涨到触发点、削不动，最后仍退回等间隔。</li>
     * </ul>
     * 取最小的 N 个则每轮都严格削到上限，反复调用收敛到「全局身份哈希最小的 N 个」，
     * 与要素到达顺序无关，空间上就是均匀变稀。
     * <p>这里也是 {@code reduceByKey} 的热路径，不碰几何、不做空间计算（见 {@link #merge} 的说明）。
     */
    private static List<GirAdvOneRow> capLayerFeatures(
            List<GirAdvOneRow> features, MvtLayerSliceParameter layer) {
        Integer hardLimit = layer.getHardFeatureLimit();
        if (hardLimit == null || hardLimit <= 0) {
            return features;
        }
        if (features.size() <= hardLimit * CAP_TRIGGER_FACTOR) {
            return features;
        }
        return selectByIdentity(features, hardLimit, layer);
    }

    /**
     * 按要素身份选取，保留身份排名最靠前的 {@code keep} 个。
     * <p>排名取身份哈希再做一次雪崩混合 —— 不能直接用 {@code hashCode()}：数据源的 id
     * 常常是逐个递增的（GeoJSON 的 {@code gid} 就是 805022、805023 这样连着编的），
     * 直接取模得到的是"一段一段"的区间，配上按位置顺序编号的数据，抽出来照样是连片的要素，
     * 等于没修。混合之后连续输入才会落到互不相邻的位置。
     * <p>整层都取不到身份值、或取到的身份值几乎没有区分度（排名全挤在一起）时，
     * 按身份选取会退化成按位置取前缀，故退回等间隔 —— 那同样有偏差，但至少能削得下去。
     */
    private static List<GirAdvOneRow> selectByIdentity(
            List<GirAdvOneRow> features, int keep, MvtLayerSliceParameter layer) {
        String geomField = layer.getGeomFieldName();
        String idField = layer.getIdFieldName();
        List<V3FeatureUtils.RankedRow> ranked = new ArrayList<>(features.size());
        int resolved = 0;
        for (GirAdvOneRow row : features) {
            int rank = V3FeatureUtils.identityRank(row, geomField, idField);
            if (rank != Integer.MAX_VALUE) {
                resolved++;
            }
            ranked.add(new V3FeatureUtils.RankedRow(rank, row));
        }
        if (resolved == 0) {
            return sampleEvenly(features, keep);
        }
        ranked.sort(null);
        long span = (long) ranked.get(keep - 1).rank - (long) ranked.get(0).rank;
        if (span < keep) {
            // 前 keep 个的排名挤在一起，说明身份值几乎没有区分度（例如第一个字段大面积取到
            // 同一个空串 —— GeoJSON 里属性为 null 时就是空串）。这时"取最小的 N 个"会退化成
            // 按位置取前缀，重新制造出成片空洞，改用等间隔兜底。
            // 正常情况身份哈希均匀，前 keep 个的跨度是 keep 的上万倍，不会走到这里。
            return sampleEvenly(features, keep);
        }
        List<GirAdvOneRow> sampled = new ArrayList<>(keep);
        for (int i = 0; i < keep; i++) {
            sampled.add(ranked.get(i).row);
        }
        return sampled;
    }

    /**
     * 等间隔抽样：按 {@code step = size / keep} 每隔 step 个取一个，共取 {@code keep} 个。
     * <p><b>只作兜底用</b>：身份键取不到值、或取值完全没有区分度时才会走到这里。它的选择依赖
     * 列表位置，反复触发会累积偏差（见 {@link #capLayerFeatures}），但至少能保证削得下去。
     * <p>不做排序、不碰几何，只要一次线性遍历。
     *
     * @param features 原列表
     * @param keep     目标个数（应小于等于原列表长度）
     * @return 抽样后的新列表
     */
    private static List<GirAdvOneRow> sampleEvenly(List<GirAdvOneRow> features, int keep) {
        int size = features.size();
        int step = Math.max(1, size / keep);
        List<GirAdvOneRow> sampled = new ArrayList<>(keep);
        for (int i = 0; i < size && sampled.size() < keep; i += step) {
            sampled.add(features.get(i));
        }
        return sampled;
    }
}
