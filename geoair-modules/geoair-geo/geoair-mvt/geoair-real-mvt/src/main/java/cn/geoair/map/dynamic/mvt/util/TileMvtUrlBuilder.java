package cn.geoair.map.dynamic.mvt.util;

import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.hutool.core.util.StrUtil;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/30 13:27
 * @description： TODO
 */
public class TileMvtUrlBuilder {
    private static final String REALMVT_PREFIX =
            "vectorTileService/v2/real/{layerName}/{z}/{x}/{y}.pbf";
    private static final String REALMVT_DEBUG_PREFIX =
            "vectorTileService/v2/debug/{layerName}/{z}/{x}/{y}.pbf";

    /**
     * 构建MVT的基础访问地址
     *
     * @param tileRequestParams
     * @return
     */
    public static String buildRealMvtUrl(TileRequestParams tileRequestParams, String layerName) {
        String replaceFirst = StrUtil.replaceFirst(REALMVT_PREFIX, "{layerName}", layerName);
        return replaceFirst + "?paramTile=" + tileRequestParams.toBase32();
    }

    /**
     * 构建MVT的基础调试地址
     *
     * @param tileRequestParams
     * @return
     */
    public static String buildRealMvtDebugUrl(
            TileRequestParams tileRequestParams, String layerName) {
        String replaceFirst = StrUtil.replaceFirst(REALMVT_DEBUG_PREFIX, "{layerName}", layerName);
        return replaceFirst + "?paramTile=" + tileRequestParams.toBase32();
    }
}
