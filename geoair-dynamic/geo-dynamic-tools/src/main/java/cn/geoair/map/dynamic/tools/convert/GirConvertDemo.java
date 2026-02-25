package cn.geoair.map.dynamic.tools.convert;

import org.locationtech.jts.geom.Point;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/5 18:04
 * @description： TODO
 */
public class GirConvertDemo {
    public static void main(String[] args) {
        // 获取单例实例
        GirFormatUtils geoUtils = GirFormatUtils.getInstance();

// GeoJSON转JTS Point（便捷方法）
        Point point = (Point) geoUtils.geojsonToJtsGeometry("{\"type\":\"Point\",\"coordinates\":[116.40,39.90]}");

// WKT转GeoJSON
        String geoJson = geoUtils.wktToGeojson("POINT(116.40 39.90)", false);

// 坐标字符串转Point（默认逗号分隔）
        Point point2 = geoUtils.jtsPointByString("116.40,39.90");

// JTS Geometry转WKB字节数组
        byte[] wkbBytes = geoUtils.jtsGeometryToWkbBytes(point, false);
    }
}
