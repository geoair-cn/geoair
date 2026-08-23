package cn.geoair.map.dynamic.tools.measure;

import cn.geoair.base.Gir;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

public class GeoMeasureDemo {

    public static void main(String[] args) {
        GirGeoTools geoTools = GirGeoTools.defaultInstance();
        GirGeoMeasureOpt measureUtils = geoTools.getMeasureOpt();
        GeometryFactory geometryFactory = new GeometryFactory();

        // 1. 先由 GirFormatUtils 将 WKT 转为 Geometry，再计算面积。
        String polygonWkt =
                "POLYGON((116.40 39.90,116.41 39.90,116.41 39.91,116.40 39.91,116.40 39.90))";
        Geometry polygon = geoTools.getFormatOpt().wktToJtsGeometry(polygonWkt, false);
        double area = measureUtils.calculateArea(
                polygon, 4326, MeasureUnitEnum.MU, MeasureMethodEnum.UTM);
        Gir.log.info("多边形面积（亩）：" + area);

        // 2. 坐标数组由调用方构造为 JTS Geometry 后再计算。
        LineString line = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(116.40, 39.90),
                new Coordinate(116.50, 39.90),
                new Coordinate(116.50, 40.00)});
        double length = measureUtils.calculateLength(
                line, 4326, MeasureUnitEnum.KILOMETER, MeasureMethodEnum.UTM);
        Gir.log.info("线长度（千米）：" + length);

        // 3. 两点距离计算（WGS84坐标系，输出单位：米）
        Point pointGeometry1 = geometryFactory.createPoint(new Coordinate(116.403874, 39.914885));
        Point pointGeometry2 = geometryFactory.createPoint(new Coordinate(116.413874, 39.924885));

        Gir.log.info("两点距离（米）：" + measureUtils.calculatePointToPointDistance(
                pointGeometry1, pointGeometry2, 4326, MeasureUnitEnum.METER, MeasureMethodEnum.GEODETIC));
        Gir.log.info("两点距离（千米）：" + measureUtils.calculatePointToPointDistance(
                pointGeometry1, pointGeometry2, 4326, MeasureUnitEnum.KILOMETER, MeasureMethodEnum.GEODETIC));

        // 4. 点到线最近距离
        double minDistance = measureUtils.calculateGeometryToGeometryMinDistance(
                geometryFactory.createPoint(new Coordinate(116.405, 39.905)),
                line,
                4326,
                MeasureUnitEnum.METER,
                MeasureMethodEnum.UTM);
        Gir.log.info("点到线最近距离（米）：" + minDistance);
    }
}
