package cn.geoair.map.tile.forge.fuser.mbtiles.statistics;

import lombok.Getter;

/** 按级别维护的计数与数据长度。范围失效时不能将旧范围作为当前值。 */
@Getter
public final class TransactionalStatsLevel {
    private final int zoom;
    private final long count, bytes;
    private final boolean boundsDirty;
    private final Integer minX, maxX, minY, maxY;

    public TransactionalStatsLevel(
            int zoom,
            long count,
            long bytes,
            boolean boundsDirty,
            Integer minX,
            Integer maxX,
            Integer minY,
            Integer maxY) {
        this.zoom = zoom;
        this.count = count;
        this.bytes = bytes;
        this.boundsDirty = boundsDirty;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }
}
