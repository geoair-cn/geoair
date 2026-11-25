package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingContext;

/** 调用方按真实网格或融合计划提供缩放，库不猜测坐标系和业务图层。 */
@FunctionalInterface
public interface TileProcessingContextResolver {
    TileImageProcessingContext resolve(int zoom, int x, int y) throws Exception;
}
