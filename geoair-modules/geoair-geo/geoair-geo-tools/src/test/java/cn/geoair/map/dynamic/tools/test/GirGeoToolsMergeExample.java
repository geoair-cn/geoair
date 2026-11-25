package cn.geoair.map.dynamic.tools.test;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.merge.GirGeoMergeOpt;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/** 几何合并 API 示例 */
public class GirGeoToolsMergeExample {

    public static void main(String[] args) {
        GirGeoMergeOpt mergeOpt = GirGeoTools.defaultInstance().getMergeOpt();
        GeometryFactory factory = new GeometryFactory();

        Point[] points =
                new Point[] {
                    factory.createPoint(new Coordinate(116.40, 39.90)),
                    factory.createPoint(new Coordinate(116.45, 39.92))
                };
        MultiPoint multiPoint = mergeOpt.mergeToMultiPoint(points);
        System.out.println("mergeToMultiPoint = " + multiPoint);

        LineString[] lines =
                new LineString[] {
                    factory.createLineString(
                            new Coordinate[] {
                                new Coordinate(116.40, 39.90), new Coordinate(116.45, 39.92)
                            }),
                    factory.createLineString(
                            new Coordinate[] {
                                new Coordinate(116.45, 39.92), new Coordinate(116.50, 39.95)
                            })
                };
        MultiLineString multiLine = mergeOpt.mergeToMultiLineString(lines);
        LineString singleLine = mergeOpt.mergeToSingleLineString(lines);
        System.out.println("mergeToMultiLineString = " + multiLine);
        System.out.println("mergeToSingleLineString = " + singleLine);

        Polygon[] polygons =
                new Polygon[] {
                    factory.createPolygon(
                            new Coordinate[] {
                                new Coordinate(116.40, 39.90),
                                new Coordinate(116.45, 39.90),
                                new Coordinate(116.45, 39.95),
                                new Coordinate(116.40, 39.95),
                                new Coordinate(116.40, 39.90)
                            }),
                    factory.createPolygon(
                            new Coordinate[] {
                                new Coordinate(116.45, 39.90),
                                new Coordinate(116.50, 39.90),
                                new Coordinate(116.50, 39.95),
                                new Coordinate(116.45, 39.95),
                                new Coordinate(116.45, 39.90)
                            })
                };
        MultiPolygon multiPolygon = mergeOpt.mergeToMultiPolygon(polygons);
        Polygon singlePolygon = mergeOpt.mergeToSinglePolygon(polygons);
        System.out.println("mergeToMultiPolygon = " + multiPolygon);
        System.out.println("mergeToSinglePolygon = " + singlePolygon);
    }
}
