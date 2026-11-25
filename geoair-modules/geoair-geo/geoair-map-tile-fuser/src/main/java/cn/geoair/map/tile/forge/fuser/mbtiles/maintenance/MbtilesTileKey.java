package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 游标使用稳定的瓦片坐标，完整整理后也不依赖 rowid。 */
@Getter
@AllArgsConstructor
public final class MbtilesTileKey {
    private final int zoom;
    private final int x;
    private final int row;
    private final long bytes;
}
