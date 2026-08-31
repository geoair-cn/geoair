package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v2;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.tools.AdvMvtDensityUtils;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * V2 版本专用的瓦片工具类 — 内存优化版。
 *
 * <p>替代原版 {@link cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.VectorTileCommonUtils}
 * 中的聚合和截断方法，核心改进：
 *
 * <ul>
 *   <li>{@link #aggregateTileFeatures} 使用 {@code addAll} 替代 {@code Stream.concat().collect()}，
 *       合并时只创建一份新 List，避免三倍内存开销
 *   <li>{@link #limitTileFeatures} 始终有硬上限兜底（10000），即使用户未开启 featureLimitEnabled， 也不会让高密度瓦片无限累积要素
 * </ul>
 *
 * @author refactored from VectorTileCommonUtils
 */
public class V2VectorTileUtils implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 硬上限：无论用户是否配置 featureLimit，单个瓦片最多保留此数量的要素。 用 CPU 换内存 —— 多余的要素在聚合阶段就被丢弃，不会累积到 OOM。 */
    private static final int HARD_LIMIT = 10000;

    /**
     * 内存优化的瓦片要素聚合。
     *
     * <p>原版使用 {@code Stream.concat().collect(Collectors.toList())}， 合并过程中同时持有 list1 + list2 + 新建的
     * list 三份内存。 本方法使用 {@code ArrayList.addAll()} 直接拷贝底层数组，减少一次中间拷贝。
     */
    public static List<GirAdvOneRow> aggregateTileFeatures(
            List<GirAdvOneRow> list1, List<GirAdvOneRow> list2) {
        List<GirAdvOneRow> result = new ArrayList<>(list1.size() + list2.size());
        result.addAll(list1);
        result.addAll(list2);
        return result;
    }

    /**
     * 内存优化的瓦片要素截断 — 始终有硬上限兜底。
     *
     * <p>原版仅在 {@code featureLimitEnabled=true} 时才截断，默认配置下高密度瓦片可以无限累积。 本方法无论用户配置如何，都保证输出列表不超过 {@link
     * #HARD_LIMIT}。
     */
    public static List<GirAdvOneRow> limitTileFeatures(
            List<GirAdvOneRow> mergedList, TileSliceParameter parameter) {
        List<GirAdvOneRow> limitList = mergedList;

        // 用户配置优先，否则使用硬上限兜底
        int effectiveLimit = HARD_LIMIT;
        if (parameter.isFeatureLimitEnabled() && parameter.getFeatureLimit() != null) {
            effectiveLimit = Math.min(parameter.getFeatureLimit(), HARD_LIMIT);
        }

        if (limitList.size() > effectiveLimit) {
            // 空间密度合并
            if (parameter.isCoalesceDensestAsNeeded()) {
                limitList =
                        AdvMvtDensityUtils.doCoalesceBySpatialDensity(
                                limitList,
                                effectiveLimit,
                                parameter.getGeomFieldName(),
                                parameter.getIdFieldName(),
                                parameter.getOutGridSrid());
            }
            // 空间密度过滤
            if (limitList.size() > effectiveLimit && parameter.isDropDensestAsNeeded()) {
                limitList =
                        AdvMvtDensityUtils.doFilterBySpatialDensity(
                                limitList,
                                effectiveLimit,
                                parameter.getGeomFieldName(),
                                parameter.getIdFieldName(),
                                parameter.getOutGridSrid());
            }
            // 兜底截断
            if (limitList.size() > effectiveLimit) {
                limitList = limitList.subList(0, effectiveLimit);
            }
        }
        return limitList;
    }
}
