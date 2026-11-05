package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;

import lombok.Getter;

/** 单级任务计划，范围为闭区间；几何和行列必须属于相同的输出网格。 */
@Getter
public final class TilePrecacheLevel {
    private final int zoom;
    private final RangeApo range;
    private final TileRegionMask mask;
    private final long total;

    public TilePrecacheLevel(int zoom, RangeApo range, TileRegionMask mask) {
        if (range == null
                || zoom < 0
                || range.getMinX() < 0
                || range.getMinY() < 0
                || range.getMaxX() < range.getMinX()
                || range.getMaxY() < range.getMinY())
            throw new IllegalArgumentException("瓦片级别或闭区间范围无效");
        this.zoom = zoom;
        this.range =
                new RangeApo(
                        range.getMinX(), range.getMaxX(), range.getMinY(), range.getMaxY(), zoom);
        this.mask = mask;
        this.total =
                Math.multiplyExact(
                        (long) range.getMaxX() - range.getMinX() + 1,
                        (long) range.getMaxY() - range.getMinY() + 1);
    }

    public RangeApo getRange() {
        return new RangeApo(
                range.getMinX(), range.getMaxX(), range.getMinY(), range.getMaxY(), zoom);
    }
}
