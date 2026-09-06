package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.tools.AdvMvtDensityUtils;
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

    /** key 为 MVT 内部图层名，使用 LinkedHashMap 保持任务配置顺序。 */
    private Map<String, List<GirAdvOneRow>> featuresByLayer = new LinkedHashMap<>();

    /** 创建只有一个图层要素的聚合单元。 */
    public static V3TileFeatureGroup single(String layerName, List<GirAdvOneRow> features) {
        V3TileFeatureGroup group = new V3TileFeatureGroup();
        group.featuresByLayer.put(layerName, new ArrayList<>(features));
        return group;
    }

    /**
     * 合并两个瓦片聚合单元，并在每个图层维度应用独立的数量上限。
     * <p>
     * 此方法始终创建新列表，不修改 Spark 上游 RDD 的 value，避免不同 stage 重试时发生副作用。
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
                entry.setValue(limitLayerFeatures(entry.getValue(), layer, outGridSrid));
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

    private static List<GirAdvOneRow> limitLayerFeatures(
            List<GirAdvOneRow> features, MvtLayerSliceParameter layer, int outGridSrid) {
        int limit = getEffectiveLimit(layer);
        if (limit <= 0 || features.size() <= limit) {
            return features;
        }
        List<GirAdvOneRow> result = features;
        // 只有显式开启业务数量限制时才做可能改变几何的密度合并；安全上限只负责阻止 OOM。
        if (layer.isFeatureLimitEnabled() && layer.isCoalesceDensestAsNeeded()) {
            result = AdvMvtDensityUtils.doCoalesceBySpatialDensity(
                    result, limit, layer.getGeomFieldName(), layer.getIdFieldName(), outGridSrid);
        }
        if (result.size() > limit && layer.isFeatureLimitEnabled() && layer.isDropDensestAsNeeded()) {
            result = AdvMvtDensityUtils.doFilterBySpatialDensity(
                    result, limit, layer.getGeomFieldName(), layer.getIdFieldName(), outGridSrid);
        }
        if (result.size() > limit) {
            return new ArrayList<>(result.subList(0, limit));
        }
        return result;
    }

    private static int getEffectiveLimit(MvtLayerSliceParameter layer) {
        if (layer.isFeatureLimitEnabled() && layer.getFeatureLimit() != null
                && layer.getFeatureLimit() > 0) {
            return layer.getFeatureLimit();
        }
        return layer.getHardFeatureLimit() == null ? 0 : layer.getHardFeatureLimit();
    }
}
