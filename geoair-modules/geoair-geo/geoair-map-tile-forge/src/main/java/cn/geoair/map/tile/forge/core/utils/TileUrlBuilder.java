package cn.geoair.map.tile.forge.core.utils;

import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.hutool.core.util.StrUtil;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/23 11:02
 * @description： 与Servlet对应的url构建逻辑
 */
public class TileUrlBuilder {


    /**
     * 构建XYZ瓦片服务URL
     */
    public static String buildXyzUrl(GirMapTileType mapTileType, String layerName, String dataId, String fileName, String format, String originType, String zxyType, String gridSet) {
        String baseUrl = null;
        if (StrUtil.isEmpty(format)) {
            baseUrl = "xyzTileService/rest/" + StrUtil.format("{}/{}/{}/{}/", mapTileType.getValue(), dataId, fileName, layerName) + "{z}/{x}/{y}";
        } else {
            baseUrl = "xyzTileService/rest/" + StrUtil.format("{}/{}/{}/{}/", mapTileType.getValue(), dataId, fileName, layerName) + "{z}/{x}/{y}." + format;
        }
        if (StrUtil.isEmpty(originType)) {
            originType = "wmts";
        }
        if (StrUtil.isEmpty(zxyType)) {
            zxyType = "zxy";
        }
        if (StrUtil.isEmpty(gridSet)) {
            gridSet = "EPSG:3857";
        }
        if (!StrUtil.startWith(gridSet, "EPSG")) {
            gridSet = "EPSG:" + gridSet;
        }
        return baseUrl + "?" + "originType=" + originType + "&zxyType=" + zxyType + "&gridSet=" + gridSet;
    }

    /**
     * 构建3Dtile瓦片服务URL
     */
    public static String buildD3TilesUrl(String dataId, String layerName, String fileName) {
        return "3dTilesService/" + StrUtil.format("{}/{}/{}/tileset.json", dataId, fileName, layerName);
    }

    /**
     * 构建S3m瓦片服务URL
     */
    public static String buildS3mUrl(String dataId, String layerName, String fileName) {
        return "3dTilesService/" + StrUtil.format("{}/{}/{}/tilesetS3MB.scp", dataId, fileName, layerName);
    }

    /**
     * 构建三维地形瓦片服务URL
     */
    public static String buildD3TerrainUrl(String dataId, String layerName, String fileName) {
        return "3dTerrainService/" + StrUtil.format("{}/{}/{}/layer.json", dataId, fileName, layerName);
    }

    /**
     * 构建mvt服务URL
     */
    public static String buildMvtTileUrl(String dataId, String layerName, String fileName) {
        return "mvtTilesService/" + StrUtil.format("{}/{}/{}/style.json", dataId, fileName, layerName);
    }


    public static void main(String[] args) {

    }
}
