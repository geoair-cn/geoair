package cn.geoair.map.dynamic.mvt.tools.model;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/1/4 16:02 @description： VecConstant
 */
public class VecConstant {

    /**
     * 统计去重键：给每个<b>源要素</b>打一个单次任务内唯一的标记。
     *
     * <p>存在的唯一理由是统计去重 —— 一个源要素会被铺进多块瓦片，直接对铺开后的要素统计
     * 会把同一要素重复计数（{@code StatisticUtils} 按这个键 {@code reduceByKey} 只留一份）。
     * 它<b>不是业务属性</b>，也不是 MVT 的 feature id（那个由 {@code generateIds} 单独生成）。</p>
     *
     * <p>因此：链路里<b>没有统计阶段就不要写它</b>（写了没人消费，还会被"全字段输出"的
     * 编码器当成普通属性写进 PBF）；写了它的链路必须在写 PBF 前摘掉
     * （{@code AdvMvtDensityUtils} 就是这么做的）。</p>
     */
    public static final String StatisticFeatureKey = "StatisticFeatureKey";

    public static final String CoalesceDistancePointCount = "point_count";

    public static final String CoalesceDistanceSqrtPointCount = "sqrt_point_count";

    public static final String CoalesceDistanceClustered = "clustered";
}
