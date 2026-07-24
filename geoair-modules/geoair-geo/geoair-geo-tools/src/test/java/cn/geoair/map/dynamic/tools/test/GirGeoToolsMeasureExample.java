package cn.geoair.map.dynamic.tools.test;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.measure.GirGeoMeasureOpt;
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
                measureOpt.calculateArea(polygon, 4326, GirGeoMeasureOpt.UNIT_SQUARE_KILOMETER);
        double areaByUtm =
                measureOpt.calculateAreaByUTM(polygon, 4326, GirGeoMeasureOpt.UNIT_SQUARE_METER);
        double lengthKm = measureOpt.calculateLength(line, 4326, GirGeoMeasureOpt.UNIT_KILOMETER);
        double lengthByUtm =
                measureOpt.calculateLengthByUTM(line, 4326, GirGeoMeasureOpt.UNIT_METER);
        double pointDistance =
                measureOpt.calculatePointToPointDistance(
                        point1, point2, 4326, GirGeoMeasureOpt.UNIT_METER);
        double pointToLine =
                measureOpt.calculatePointToLineMinDistance(
                        point1, line, 4326, GirGeoMeasureOpt.UNIT_METER);
        double converted =
                measureOpt.convertUnit(
                        1000, GirGeoMeasureOpt.UNIT_METER, GirGeoMeasureOpt.UNIT_KILOMETER, 3857);

        System.out.println("calculateArea(km²) = " + areaKm2);
        System.out.println("calculateAreaByUTM(m²) = " + areaByUtm);
        System.out.println("calculateLength(km) = " + lengthKm);
        System.out.println("calculateLengthByUTM(m) = " + lengthByUtm);
        System.out.println("calculatePointToPointDistance(m) = " + pointDistance);
        System.out.println("calculatePointToLineMinDistance(m) = " + pointToLine);
        System.out.println("convertUnit 1000m -> km = " + converted);
    }
}
