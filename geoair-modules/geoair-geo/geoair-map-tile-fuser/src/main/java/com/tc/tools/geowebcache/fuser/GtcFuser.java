package com.tc.tools.geowebcache.fuser;

import com.tc.tools.geowebcache.fuser.entity.PxyLayerInfo;
import com.tc.tools.geowebcache.fuser.provider.LayerTileGetter;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/15 11:57
 * @description： 短方法
 */
public class GtcFuser {

    public static LayerTileGetter getLayerTileGetter(String layerName) {
        return GtcFuserLayerTileGetter.getInstance().getLayerTileGetter(layerName);
    }

    public static PxyLayerInfo getPxyLayerInfo(String layerName) {
        return GtcFuserLayerTileGetter.getInstance().getPxyLayerInfo(layerName);
    }


}
