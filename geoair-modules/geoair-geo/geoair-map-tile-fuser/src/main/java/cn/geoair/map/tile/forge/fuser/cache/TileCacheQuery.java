package cn.geoair.map.tile.forge.fuser.cache;

import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.web.mime.GiMimeType;

import lombok.Getter;

/** 列优先的闭区间网格，from/to 为当前分段的左闭右开偏移；内存只与分段大小有关。 */
@Getter
public class TileCacheQuery {
    private final String layerName;
    private final int zoom;
    private final RangeApo range;
    private final long rows, from, to;
    private final GiMimeType format;

    public TileCacheQuery(
            String layerName, int zoom, RangeApo range, long from, long to, GiMimeType format) {
        if (layerName == null
                || layerName.trim().isEmpty()
                || format == null
                || range == null
                || zoom < 0
                || range.getMinX() < 0
                || range.getMinY() < 0) throw new IllegalArgumentException("缓存查询参数无效");
        this.rows = (long) range.getMaxY() - range.getMinY() + 1;
        long columns = (long) range.getMaxX() - range.getMinX() + 1;
        if (rows <= 0
                || columns <= 0
                || from < 0
                || to < from
                || to > Math.multiplyExact(rows, columns)
                || to - from > Integer.MAX_VALUE) throw new IllegalArgumentException("缓存查询分段越界");
        this.layerName = layerName;
        this.zoom = zoom;
        this.from = from;
        this.to = to;
        this.format = format;
        this.range =
                new RangeApo(
                        range.getMinX(), range.getMaxX(), range.getMinY(), range.getMaxY(), zoom);
    }

    public int x(long index) {
        return (int) (range.getMinX() + index / rows);
    }

    public int y(long index) {
        return (int) (range.getMinY() + index % rows);
    }

    public int size() {
        return (int) (to - from);
    }

    public RangeApo getRange() {
        return new RangeApo(
                range.getMinX(), range.getMaxX(), range.getMinY(), range.getMaxY(), zoom);
    }
}
