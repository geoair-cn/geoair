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
     * <b>这里只做"防 OOM 的兜底截断"，不做要素数限制与密度裁剪。</b>
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
     * 聚合阶段的安全阀：要素数明显超过图层声明的安全上限时直接截断，防止 executor OOM。
     * <p>只截断不排序，且只在超过「上限 × {@value #CAP_TRIGGER_FACTOR}」时才执行，避免每合并一次就拷贝一遍列表。
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
        return new ArrayList<>(features.subList(0, hardLimit));
    }
}
