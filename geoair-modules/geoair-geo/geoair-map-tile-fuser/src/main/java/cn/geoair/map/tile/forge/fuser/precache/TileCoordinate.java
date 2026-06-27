package cn.geoair.map.tile.forge.fuser.precache;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/27 13:37
 * @description： TODO
 */
@Data
@Accessors(chain = true)
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
}
