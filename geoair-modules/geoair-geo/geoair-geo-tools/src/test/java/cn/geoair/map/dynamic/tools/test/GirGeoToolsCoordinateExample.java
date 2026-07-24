package cn.geoair.map.dynamic.tools.test;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.coordinate.GirCoordinateConvertOpt;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/** 坐标转换 API 示例 */
public class GirGeoToolsCoordinateExample {

    public static void main(String[] args) {
        GirCoordinateConvertOpt coordinateOpt = GirGeoTools.defaultInstance().getCoordinateOpt();
        GeometryFactory geometryFactory = new GeometryFactory();

        double[] gcj02 = coordinateOpt.wgs84ToGcj02(116.40, 39.90);
        double[] bd09 = coordinateOpt.wgs84ToBd09(116.40, 39.90);
        double[] mercator = coordinateOpt.wgs84ToMercator(116.40, 39.90);
        double[] wgs84 = coordinateOpt.bd09ToWgs84(bd09[0], bd09[1]);

        System.out.println("wgs84ToGcj02 = [" + gcj02[0] + ", " + gcj02[1] + "]");
        System.out.println("wgs84ToBd09 = [" + bd09[0] + ", " + bd09[1] + "]");
        System.out.println("wgs84ToMercator = [" + mercator[0] + ", " + mercator[1] + "]");
        System.out.println("bd09ToWgs84 = [" + wgs84[0] + ", " + wgs84[1] + "]");

        Point point = geometryFactory.createPoint(new Coordinate(116.40, 39.90));
        Point pointBd09 = coordinateOpt.wgs84ToBd09(point);
        System.out.println("point wgs84ToBd09 = " + pointBd09);

        double[][] batch =
                new double[][] {
                    {116.40, 39.90},
                    {121.47, 31.23},
                    {113.26, 23.13}
                };
        double[][] batchGcj02 = coordinateOpt.wgs84ToGcj02Batch(batch, false);
        System.out.println("batch size = " + batchGcj02.length);

        Geometry line =
                geometryFactory.createLineString(
                        new Coordinate[] {
                            new Coordinate(116.40, 39.90), new Coordinate(116.45, 39.92)
                        });
        Geometry mercatorLine = coordinateOpt.wgs84ToMercatorGeometry(line);
        System.out.println("mercator line = " + mercatorLine);

        String dms = coordinateOpt.ddToDms(116.40, 39.90);
        double[] parsed = coordinateOpt.parseCoordString("116.40,39.90", ",");
        System.out.println("ddToDms = " + dms);
        System.out.println("parseCoordString = [" + parsed[0] + ", " + parsed[1] + "]");
    }
}
