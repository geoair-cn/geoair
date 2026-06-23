package cn.geoair.map.tile.forge.fuser.utils;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.tile.forge.fuser.GirFuserLayerTileHelper;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.enums.OriginType;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/22 12:13
 * @description： TODO
 */
@Slf4j
public class FuserCacheUtils {

    /**
     * 判断是否需要翻转 Y
     */
    public static boolean isNeedReverseY(String layerName) {
        try {
            PxyLayerInfo pxyLayerInfo = GirFuserLayerTileHelper.getInstance().getPxyLayerInfo(layerName);
            if (pxyLayerInfo != null) {
                String originTypeStr = pxyLayerInfo.getOriginType();
                OriginType originType = OriginType.fromMode(originTypeStr);
                // Google 坐标系需要翻转 Y（TMS 风格）
                return originType.isGoogle();
            }
        } catch (Exception e) {
            log.debug("获取图层 {} 的 OriginType 失败，默认不翻转", layerName);
        }
        // 默认不翻转
        return false;
    }

    /**
     * XYZ → TMS Y 转换（如果需要）
     *
     * @param z           层级
     * @param y           原始 Y 坐标
     * @param needReverse 是否需要翻转
     * @return 转换后的 Y 坐标
     */
    public static int getStoreY(int z, int y, boolean needReverse) {
        if (needReverse) {
            return GirAdvTools.getTileGrid3857Opt().reverseY(y, z);  // 这里使用3857的网格翻转逻辑来进行翻转Y，不进行判断43426的网格原因是因为mbtile规范并不支持4326网格，这里在4326网格的时候就把mbtiles当做一个存储器
        }
        return y;
    }
}
