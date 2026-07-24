package cn.geoair.map.dynamic.tools.measure;

import cn.geoair.base.Gir;

public class GeoMeasureDemo {

    public static void main(String[] args) {
        GirGeoMeasureUtils measureUtils = GirGeoMeasureUtils.getInstance();

        // 1. 面积计算（WKT格式，WGS84坐标系，输出单位：亩）
        String polygonWkt =
                "POLYGON((116.40 39.90,116.41 39.90,116.41 39.91,116.40 39.91,116.40 39.90))";
        double area = measureUtils.calculateArea(polygonWkt, 4326, GirGeoMeasureUtils.UNIT_ACRE);
        Gir.log.info("多边形面积（亩）：" + area);

        // 2. 长度计算（坐标数组，WGS84坐标系，输出单位：千米）
        double[][] lineCoords = {{116.40, 39.90}, {116.50, 39.90}, {116.50, 40.00}};
        double length =
                measureUtils.calculateLength(lineCoords, 4326, GirGeoMeasureUtils.UNIT_KILOMETER);
        Gir.log.info("线长度（千米）：" + length);

        // 3. 两点距离计算（WGS84坐标系，输出单位：米）
        double[] point1 = {116.403874, 39.914885};
        double[] point2 = {116.413874, 39.924885};

        Gir.log.info(
                "两点距离（米）："
                        + measureUtils.calculatePointToPointDistance(
                                point1, point2, 4326, GirGeoMeasureUtils.UNIT_METER));
        Gir.log.info(
                "两点距离（米）："
                        + measureUtils.calculatePointToPointDistance(
                                point1, point2, 4326, GirGeoMeasureUtils.UNIT_METER));

        // 4. 点到线最近距离
        double[] point = {116.405, 39.905};
        double minDistance =
                measureUtils.calculatePointToLineMinDistance(
                        point, lineCoords, 4326, GirGeoMeasureUtils.UNIT_METER);
        Gir.log.info("点到线最近距离（米）：" + minDistance);
    }
}
