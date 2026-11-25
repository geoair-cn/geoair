package cn.geoair.map.tile.forge.fuser.image;

import lombok.Getter;

/** 算法结果与版本一起返回；只有 PROCESSED 可以携带新图片。 */
@Getter
public final class TileImageProcessingResult {
    private final TileImageProcessingState state;
    private final String ruleVersion;
    private final double score;
    private final int regions;
    private final byte[] bytes;
    private final String reason;

    public TileImageProcessingResult(
            TileImageProcessingState state,
            String ruleVersion,
            double score,
            int regions,
            byte[] bytes,
            String reason) {
        if (state == null || ruleVersion == null || ruleVersion.trim().isEmpty())
            throw new IllegalArgumentException("处理状态和规则版本不能为空");
        if (state == TileImageProcessingState.PROCESSED && (bytes == null || bytes.length == 0)
                || state != TileImageProcessingState.PROCESSED && bytes != null)
            throw new IllegalArgumentException("仅处理成功时允许携带非空结果图片");
        this.state = state;
        this.ruleVersion = ruleVersion;
        this.score = score;
        this.regions = regions;
        this.bytes = bytes == null ? null : bytes.clone();
        this.reason = reason;
    }

    public byte[] getBytes() {
        return bytes == null ? null : bytes.clone();
    }
}
