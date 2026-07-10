package cn.geoair.map.tile.forge.fuser.provider;


import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.MimeType;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.web.mime.GiMimeType;

/**
 * @author ：zhangjun
 * @date ：Created in 2026/6/12 17:17
 * @description： 瓦片获取器
 */
public interface LayerTileGetter {

    /**
     * 获取瓦片资源的请求 ，注意，这里的xyz是tms的网格
     *
     * @param z z级别
     * @param x x轴网格
     * @param y tms的Y
     * @return
     */
    Resource getTileResource(int z, int x, int y);


    ImageMime getSrcFormat();


    GridSubset getSrcGridSubset();


}
