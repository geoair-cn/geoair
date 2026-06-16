package cn.geoair.map.tile.forge.fuser;


import cn.geoair.base.Gir;
import cn.geoair.base.util.GutilObject;
import cn.geoair.map.tile.forge.fuser.cache.TileCacheFactory;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.TileGetterFactory;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/15 11:55
 * @description：  PxyLayerInfo 的获取实现
 */
public interface GirFuserLayerTileGetter {

    static GirFuserLayerTileGetter getInstance() {
        return Gir.beans.getBean(GirFuserLayerTileGetter.class);
    }

    default LayerTileGetter getLayerTileGetter(String layerName) {
        PxyLayerInfo pxyLayerInfo = getPxyLayerInfo(layerName);
        if (GutilObject.isEmpty(pxyLayerInfo)) {
            return null;
        }
        return TileGetterFactory.create(pxyLayerInfo, TileCacheFactory.getDefaultCache());
    }

    PxyLayerInfo getPxyLayerInfo(String layerName);

}
