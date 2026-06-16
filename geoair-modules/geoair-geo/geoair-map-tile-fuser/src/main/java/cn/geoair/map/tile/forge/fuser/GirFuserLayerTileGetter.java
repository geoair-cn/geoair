package cn.geoair.map.tile.forge.fuser;


import cn.geoair.base.Gir;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/15 11:55
 * @description： TODO
 */
public interface GirFuserLayerTileGetter {

    static GirFuserLayerTileGetter getInstance() {
        return Gir.beans.getBean(GirFuserLayerTileGetter.class);
    }

    LayerTileGetter getLayerTileGetter(String layerName);

    PxyLayerInfo getPxyLayerInfo(String layerName);

}
