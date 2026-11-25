package cn.geoair.map.tile.forge.fuser.image;

import lombok.Getter;

/** 算法和落盘的真实阶段结果；成功时 bytes 的编码与 format 一致。 */
@Getter
public final class TileImageProcessingOutcome {
    private final TileImageProcessingResult result;
    private final TileImageWriteState writeState;
    private final String format;
    private final Throwable failure;

    public TileImageProcessingOutcome(
            TileImageProcessingResult result,
            TileImageWriteState writeState,
            String format,
            Throwable failure) {
        if (result == null || writeState == null) throw new IllegalArgumentException("算法与写入结果不能为空");
        if (writeState == TileImageWriteState.STORED
                && result.getState() != TileImageProcessingState.PROCESSED)
            throw new IllegalArgumentException("只有处理并保存成功才能报告已保存");
        this.result = result;
        this.writeState = writeState;
        this.format = format;
        this.failure = failure;
    }

    public boolean isStored() {
        return writeState == TileImageWriteState.STORED;
    }
}
