package cn.geoair.map.tile.forge.core.xyz;

import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;

/** 谷歌TMS瓦片XYZ坐标与BBOX转换工具 支持Web Mercator投影（EPSG:3857） */
public class GoogleTileUtils {

    // Web Mercator投影的世界范围
    private static final double WORLD_MIN = -20037508.342789244;
    private static final double WORLD_MAX = 20037508.342789244;
    private static final double WORLD_SIZE = WORLD_MAX - WORLD_MIN;

    /**
     * 根据XYZ瓦片坐标计算对应的BBOX
     *
     * @param x 瓦片X坐标
     * @param y 瓦片Y坐标
     * @param zoom 缩放级别
     * @return 瓦片对应的地理范围BBOX
     */
    public static BoundingBox tileToBBox(int x, int y, int zoom) {
        // 计算该缩放级别的瓦片总数
        int tilesPerSide = (int) Math.pow(2, zoom);

        // 计算单个瓦片的尺寸
        double tileSize = WORLD_SIZE / tilesPerSide;

        // 计算瓦片的边界
        double minX = WORLD_MIN + (x * tileSize);
        double maxX = minX + tileSize;
        double maxY = WORLD_MAX - (y * tileSize);
        double minY = maxY - tileSize;

        return new BoundingBox(minX, minY, maxX, maxY);
    }

    /**
     * 根据最大XYZ计算整个瓦片集的边界范围
     *
     * @param maxX 最大X坐标
     * @param maxY 最大Y坐标
     * @param maxZoom 最大缩放级别
     * @return 整个瓦片集的边界BBOX
     */
    public static BoundingBox maxTileToBBox(int maxX, int maxY, int maxZoom) {
        // 获取最大缩放级别的边界瓦片
        BoundingBox maxTileBBox = tileToBBox(maxX, maxY, maxZoom);

        // 获取最小瓦片(0,0)的边界
        BoundingBox minTileBBox = tileToBBox(0, 0, maxZoom);

        // 合并边界
        double minX = Math.min(minTileBBox.getMinX(), maxTileBBox.getMinX());
        double minY = Math.min(minTileBBox.getMinY(), maxTileBBox.getMinY());
        double maxX_geo = Math.max(minTileBBox.getMaxX(), maxTileBBox.getMaxX());
        double maxY_geo = Math.max(minTileBBox.getMaxY(), maxTileBBox.getMaxY());

        return new BoundingBox(minX, minY, maxX_geo, maxY_geo);
    }

    /**
     * 将Web Mercator坐标转换为经纬度（WGS84）
     *
     * @param bbox Web Mercator投影的BBOX
     * @return WGS84经纬度的BBOX
     */
    public static BoundingBox webMercatorToWgs84(BoundingBox bbox) {
        double minLon = mercatorXToLon(bbox.getMinX());
        double minLat = mercatorYToLat(bbox.getMinY());
        double maxLon = mercatorXToLon(bbox.getMaxX());
        double maxLat = mercatorYToLat(bbox.getMaxY());

        return new BoundingBox(minLon, minLat, maxLon, maxLat);
    }

    /** Web Mercator X坐标转经度 */
    private static double mercatorXToLon(double x) {
        return (x / WORLD_MAX) * 180.0;
    }

    /** Web Mercator Y坐标转纬度 */
    private static double mercatorYToLat(double y) {
        double lat = (y / WORLD_MAX) * 180.0;
        lat = 180.0 / Math.PI * (2.0 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0);
        return lat;
    }

    /** 获取指定缩放级别的瓦片总数 */
    public static int getTileCount(int zoom) {
        return (int) Math.pow(2, zoom);
    }

    /**
     * 获取指定缩放级别的分辨率（米/像素）
     *
     * @param zoom 缩放级别
     * @param tileSize 瓦片像素尺寸（通常为256）
     * @return 分辨率（米/像素）
     */
    public static double getResolution(int zoom, int tileSize) {
        return WORLD_SIZE / (getTileCount(zoom) * tileSize);
    }

    /** 示例用法 */
    public static void main(String[] args) {
        // 示例：计算最大瓦片(10, 10, 5)的边界
        int maxX = 10;
        int maxY = 10;
        int maxZoom = 5;

        // 获取Web Mercator投影的BBOX
        BoundingBox mercatorBBox = maxTileToBBox(maxX, maxY, maxZoom);
        System.out.println("Web Mercator BBOX: " + mercatorBBox);

        // 转换为WGS84经纬度
        BoundingBox wgs84BBox = webMercatorToWgs84(mercatorBBox);
        System.out.println("WGS84经纬度BBOX: " + wgs84BBox);

        // 计算单个瓦片的边界
        BoundingBox singleTileBBox = tileToBBox(5, 5, 5);
        System.out.println("瓦片(5,5,5)的BBOX: " + singleTileBBox);
        System.out.println("瓦片(5,5,5)的经纬度: " + webMercatorToWgs84(singleTileBBox));
    }
}
