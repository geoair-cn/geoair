package com.tc.tools.geowebcache.fuser;


import cn.geoair.base.Gir;
import com.tc.tools.geowebcache.fuser.entity.PxyLayerInfo;
import com.tc.tools.geowebcache.fuser.provider.LayerTileGetter;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/15 11:55
 * @description： TODO
 */
public interface GtcFuserLayerTileGetter {

    static GtcFuserLayerTileGetter getInstance() {
        return Gir.beans.getBean(GtcFuserLayerTileGetter.class);
    }

    LayerTileGetter getLayerTileGetter(String layerName);

    PxyLayerInfo getPxyLayerInfo(String layerName);

}
