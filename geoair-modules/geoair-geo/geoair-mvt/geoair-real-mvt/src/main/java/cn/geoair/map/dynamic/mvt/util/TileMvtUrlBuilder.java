package cn.geoair.map.dynamic.mvt.util;

import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/30 13:27
 * @description： TODO
 */
public class TileMvtUrlBuilder {
    private static final String REALMVT_PREFIX = "vectorTileService/v2/real/{layerName}/{z}/{x}/{y}.pbf";

    /**
     *  构建MVT的基础访问地址
     * @param tileRequestParams
     * @return
     */
    public static String buildRealMvtUrl(
            TileRequestParams tileRequestParams) {
        return REALMVT_PREFIX + "?paramTile=" + tileRequestParams.toBase32();
    }

}
