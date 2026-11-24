package cn.geoair.map.tile.forge.fuser.encoding;

/** 两套编码策略的不可变任务快照；任务记录与快照持久化由接入方负责。 */
public final class TileEncodingSnapshot {
    private final TileEncodingPolicy original;
    private final TileEncodingPolicy output;

    public TileEncodingSnapshot(TileEncodingPolicy original, TileEncodingPolicy output) {
        this.original = copy(original);
        this.output = copy(output);
    }

    public TileEncodingPolicy getOriginal() {
        return copy(original);
    }

    public TileEncodingPolicy getOutput() {
        return copy(output);
    }

    /** 覆盖只产生新的快照，不修改图层默认对象或另一套缓存策略。 */
    public TileEncodingSnapshot override(TileEncodingTarget target, TileEncodingPolicy policy) {
        if (target == null || policy == null) throw new IllegalArgumentException("覆盖目标和编码策略不能为空");
        return target == TileEncodingTarget.ORIGINAL
                ? new TileEncodingSnapshot(policy, output)
                : new TileEncodingSnapshot(original, policy);
    }

    public TileEncodingPolicy policy(TileEncodingTarget target) {
        if (target == null) throw new IllegalArgumentException("编码目标不能为空");
        return copy(target == TileEncodingTarget.ORIGINAL ? original : output);
    }

    /** 显式绑定两个逻辑缓存名，也覆盖融合过程中隐式写入的原始缓存。 */
    public TileEncodingContext bindings(String originalCache, String outputCache) {
        if (originalCache == null || outputCache == null || originalCache.equals(outputCache))
            throw new IllegalArgumentException("须提供两个不同的逻辑缓存名称");
        return TileEncodingContext.empty().with(originalCache, original).with(outputCache, output);
    }

    static TileEncodingPolicy copy(TileEncodingPolicy policy) {
        TileEncodingPolicy value =
                policy == null
                        ? new TileEncodingPolicy()
                        : new TileEncodingPolicy(policy.getMode(), policy.getQuality());
        value.validate();
        return value;
    }
}
