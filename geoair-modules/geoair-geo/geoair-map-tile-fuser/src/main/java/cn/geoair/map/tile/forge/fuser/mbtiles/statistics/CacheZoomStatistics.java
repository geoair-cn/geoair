package cn.geoair.map.tile.forge.fuser.mbtiles.statistics;

import lombok.Getter;

/**
 * 单个缩放级别的缓存统计。
 */
@Getter
public class CacheZoomStatistics {

    private final int zoomLevel;
    private long tileCount;
    private long size;
    private Integer minX;
    private Integer maxX;
    private Integer minY;
    private Integer maxY;

    public CacheZoomStatistics(int zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public void addTile(long tileSize) {
        tileCount++;
        size += tileSize;
    }

    /** 记录原始缓存网格中的一个 X/Y 坐标。 */
    public void addGridCoordinate(Integer x, Integer y) {
        if (x == null || y == null) {
            return;
        }
        minX = minX == null ? x : Math.min(minX, x);
        maxX = maxX == null ? x : Math.max(maxX, x);
        minY = minY == null ? y : Math.min(minY, y);
        maxY = maxY == null ? y : Math.max(maxY, y);
    }

    public void setGridRange(int minX, int maxX, int minY, int maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    /** 原始网格 X/Y 最小外包矩形中的格网数量。 */
    public long getGridCellCount() {
        if (minX == null || maxX == null || minY == null || maxY == null) {
            return 0L;
        }
        long width = (long) maxX - minX + 1L;
        long height = (long) maxY - minY + 1L;
        return width > Long.MAX_VALUE / height ? Long.MAX_VALUE : width * height;
    }

    public void setTileCount(long tileCount) {
        this.tileCount = tileCount;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
