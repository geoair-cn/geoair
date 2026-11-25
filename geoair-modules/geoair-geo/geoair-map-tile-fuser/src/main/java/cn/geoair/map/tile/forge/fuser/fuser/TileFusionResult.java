package cn.geoair.map.tile.forge.fuser.fuser;

import lombok.Getter;

/** 一次出图的不可变快照；只有完整结果可以进入转换缓存。 */
@Getter
public final class TileFusionResult {
    private final TileFusionState state;
    private final byte[] bytes;
    private final long available;
    private final long noCoverage;
    private final long missing;
    private final long failed;
    private final Exception failure;

    public TileFusionResult(byte[] bytes, long available, long noCoverage, long missing, long failed, Exception failure) {
        if (available < 0 || noCoverage < 0 || missing < 0 || failed < 0)
            throw new IllegalArgumentException("瓦片读取计数不能为负数");
        this.available = available;
        this.noCoverage = noCoverage;
        this.missing = missing;
        this.failed = failed;
        this.failure = failure;
        boolean hasImage = bytes != null && bytes.length > 0;
        if (failure != null || !hasImage && (available > 0 || missing > 0 || failed > 0)) state = TileFusionState.FAILED;
        else if (missing > 0 || failed > 0) state = TileFusionState.PARTIAL;
        else if (available == 0 && noCoverage > 0 || !hasImage) state = TileFusionState.NO_COVERAGE;
        else state = TileFusionState.COMPLETE;
        this.bytes = state == TileFusionState.COMPLETE || state == TileFusionState.PARTIAL ? bytes.clone() : null;
    }

    public byte[] getBytes() { return bytes == null ? null : bytes.clone(); }

    public boolean isCacheable() { return state == TileFusionState.COMPLETE; }

    /** 兼容旧融合器：旧实现未提供源读取计数，成功字节按完整结果处理。 */
    public static TileFusionResult legacy(byte[] bytes) {
        return new TileFusionResult(bytes, bytes == null || bytes.length == 0 ? 0 : 1, 0, 0, 0, null);
    }
}
