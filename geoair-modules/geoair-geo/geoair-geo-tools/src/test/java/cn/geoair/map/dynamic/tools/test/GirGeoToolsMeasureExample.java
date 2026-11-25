package cn.geoair.map.dynamic.tools.test;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.measure.GirGeoMeasureOpt;
import cn.geoair.map.dynamic.tools.measure.MeasureMethodEnum;
import cn.geoair.map.dynamic.tools.measure.MeasureUnitEnum;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/** 测量计算 API 示例 */
public class GirGeoToolsMeasureExample {

    public static void main(String[] args) {
        GirGeoMeasureOpt measureOpt = GirGeoTools.defaultInstance().getMeasureOpt();
        GeometryFactory geometryFactory = new GeometryFactory();

        Polygon polygon =
                geometryFactory.createPolygon(
                        new Coordinate[] {
                            new Coordinate(116.40, 39.90),
                            new Coordinate(116.45, 39.90),
                            new Coordinate(116.45, 39.95),
                            new Coordinate(116.40, 39.95),
                            new Coordinate(116.40, 39.90)
                        });

        LineString line =
                geometryFactory.createLineString(
                        new Coordinate[] {
                            new Coordinate(116.40, 39.90),
                            new Coordinate(116.45, 39.92),
                            new Coordinate(116.50, 39.95)
                        });

        Point point1 = geometryFactory.createPoint(new Coordinate(116.40, 39.90));
        Point point2 = geometryFactory.createPoint(new Coordinate(116.50, 39.95));

        double areaKm2 =
                measureOpt.calculateArea(
                        polygon,
                        4326,
                        MeasureUnitEnum.SQUARE_KILOMETER,
                        MeasureMethodEnum.WEB_MERCATOR);
        double areaByUtm =
                measureOpt.calculateArea(
                        polygon, 4326, MeasureUnitEnum.SQUARE_METER, MeasureMethodEnum.UTM);
        double lengthKm =
                measureOpt.calculateLength(
                        line, 4326, MeasureUnitEnum.KILOMETER, MeasureMethodEnum.WEB_MERCATOR);
        double lengthByUtm =
                measureOpt.calculateLength(
                        line, 4326, MeasureUnitEnum.METER, MeasureMethodEnum.UTM);
        double pointDistance =
                measureOpt.calculatePointToPointDistance(
                        point1, point2, 4326, MeasureUnitEnum.METER, MeasureMethodEnum.GEODETIC);
        double pointToLine =
                measureOpt.calculateGeometryToGeometryMinDistance(
                        point1, line, 4326, MeasureUnitEnum.METER, MeasureMethodEnum.UTM);
        double converted =
                measureOpt.convertUnit(1000, MeasureUnitEnum.METER, MeasureUnitEnum.KILOMETER);

        System.out.println("calculateArea(km²) = " + areaKm2);
        System.out.println("calculateArea(UTM, m²) = " + areaByUtm);
        System.out.println("calculateLength(km) = " + lengthKm);
        System.out.println("calculateLength(UTM, m) = " + lengthByUtm);
        System.out.println("calculatePointToPointDistance(m) = " + pointDistance);
        System.out.println("calculatePointToLineMinDistance(m) = " + pointToLine);
        System.out.println("convertUnit 1000m -> km = " + converted);
    }
}
