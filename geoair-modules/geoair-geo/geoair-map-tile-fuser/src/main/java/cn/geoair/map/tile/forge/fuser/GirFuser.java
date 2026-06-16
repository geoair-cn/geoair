package cn.geoair.map.tile.forge.fuser;

import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/15 11:57
 * @description： 短方法
 */
public class GirFuser {

    public static LayerTileGetter getLayerTileGetter(String layerName) {
        return GirFuserLayerTileHelper.getInstance().getLayerTileGetter(layerName);
    }

    public static PxyLayerInfo getPxyLayerInfo(String layerName) {
        return GirFuserLayerTileHelper.getInstance().getPxyLayerInfo(layerName);
    }


}
