package cn.geoair.map.tile.forge.fuser.provider;


import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;

/**
 * @author ：zhangjun
 * @date ：Created in 2026/6/12 17:17
 * @description： 瓦片获取器
 */
public interface LayerTileGetter {

    Resource getTileResource(int z, int x, int y);


    ImageMime getSrcFormat();


    GridSubset getSrcGridSubset();

}
