package cn.geoair.map.tile.forge.fuser.precache.bask;

/**
 * 瓦片坐标实体类
 */
public class TileCoordinate {
    private final int x;
    private final int y;
    private final int zoom;

    public TileCoordinate(int x, int y) {
        this.x = x;
        this.y = y;
        this.zoom = 0; // 默认值
    }

    public TileCoordinate(int zoom, int x, int y) {
        this.zoom = zoom;
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZoom() {
        return zoom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TileCoordinate that = (TileCoordinate) o;
        return x == that.x && y == that.y && zoom == that.zoom;
    }



    @Override
    public String toString() {
        return String.format("TileCoordinate{zoom=%d, x=%d, y=%d}", zoom, x, y);
    }
}
