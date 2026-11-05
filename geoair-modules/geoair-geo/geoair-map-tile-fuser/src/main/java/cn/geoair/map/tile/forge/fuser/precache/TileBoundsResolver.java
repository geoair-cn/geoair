package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;

/** 把任务行列转换到融合器所需的源坐标系范围。 */
@FunctionalInterface
public interface TileBoundsResolver {
    BoundingBox resolve(int zoom, int x, int y);
}
