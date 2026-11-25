package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingOutcome;

import lombok.Getter;

/** 一张瓦片的多阶段结果；下载落盘后处理失败，保留下载事实而整体只计一次失败。 */
@Getter
public final class TilePrecacheTileResult {
    private final int zoom;
    private final int x;
    private final int y;
    private final TileOperationStatus sourceStatus;
    private final TileOperationStatus status;
    private final TileImageProcessingOutcome processing;
    private final Throwable failure;

    public TilePrecacheTileResult(
            int zoom,
            int x,
            int y,
            TileOperationStatus sourceStatus,
            TileOperationStatus status,
            TileImageProcessingOutcome processing,
            Throwable failure) {
        if (sourceStatus == null || status == null)
            throw new IllegalArgumentException("源阶段和整张结果不能为空");
        this.zoom = zoom;
        this.x = x;
        this.y = y;
        this.sourceStatus = sourceStatus;
        this.status = status;
        this.processing = processing;
        this.failure = failure;
    }
}
