package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;

import lombok.Getter;

/** 稀疏记录需要重放的失败坐标。偏移量按当前级别计划的列优先顺序计算。 */
@Getter
public final class TilePrecacheFailure {
    private final long position;
    private final int x;
    private final int y;
    private final TileOperationStatus status;

    TilePrecacheFailure(long position, int x, int y, TileOperationStatus status) {
        this.position = position;
        this.x = x;
        this.y = y;
        this.status = status;
    }

    static boolean retryable(TileOperationStatus status) {
        return status == TileOperationStatus.MISSING
                || status == TileOperationStatus.FAILED
                || status == TileOperationStatus.TIMED_OUT;
    }
}
