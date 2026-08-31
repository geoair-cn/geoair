package cn.geoair.map.dynamic.tools.measure;

import cn.geoair.map.dynamic.tools.ToolsConfig;

import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.Locale;

/**
 * 测量方式与单位枚举 API 的回归测试。
 *
 * @author 张逢吉
 */
public class GirGeoMeasureUtilsTest {

    private final GirGeoMeasureOpt measureOpt = new GirGeoMeasureUtils(new ToolsConfig());
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Test
    public void shouldPrintDistanceDifferencesForSameCoordinates() {
        Point point1 = geometryFactory.createPoint(new Coordinate(116.40D, 39.90D));
        Point point2 = geometryFactory.createPoint(new Coordinate(116.50D, 40.00D));
        LineString line =
                geometryFactory.createLineString(
                        new Coordinate[] {point1.getCoordinate(), point2.getCoordinate()});

        double webMercatorMeters =
                measureOpt.calculatePointToPointDistance(
                        point1,
                        point2,
                        4326,
                        MeasureUnitEnum.METER,
                        MeasureMethodEnum.WEB_MERCATOR);
        double utmMeters =
                measureOpt.calculatePointToPointDistance(
                        point1, point2, 4326, MeasureUnitEnum.METER, MeasureMethodEnum.UTM);
        double geodeticMeters =
                measureOpt.calculatePointToPointDistance(
                        point1, point2, 4326, MeasureUnitEnum.METER, MeasureMethodEnum.GEODETIC);

        double geodeticLengthMeters =
                measureOpt.calculateLength(
                        line, 4326, MeasureUnitEnum.METER, MeasureMethodEnum.GEODETIC);
        double geodeticKilometers =
                measureOpt.calculatePointToPointDistance(
                        point1,
                        point2,
                        4326,
                        MeasureUnitEnum.KILOMETER,
                        MeasureMethodEnum.GEODETIC);
        double geodeticMiles =
                measureOpt.calculatePointToPointDistance(
                        point1, point2, 4326, MeasureUnitEnum.MILE, MeasureMethodEnum.GEODETIC);
        double geodeticDegrees =
                measureOpt.calculatePointToPointDistance(
                        point1, point2, 4326, MeasureUnitEnum.DEGREE, MeasureMethodEnum.GEODETIC);

        System.out.println(
                String.format(
                        Locale.ROOT,
                        "同一经纬度距离对比（116.40,39.90 -> 116.50,40.00）：WebMercator=%.3f m, UTM=%.3f m, 大地线=%.3f m",
                        webMercatorMeters,
                        utmMeters,
                        geodeticMeters));
        System.out.println(
                String.format(
                        Locale.ROOT,
                        "大地线单位输出：%.6f km, %.6f mile, %.6f degree（degree 为赤道近似换算）",
                        geodeticKilometers,
                        geodeticMiles,
                        geodeticDegrees));

        Assert.assertTrue(
                "Web Mercator 在北纬 40 度应明显大于大地线距离", webMercatorMeters > geodeticMeters * 1.2D);
        Assert.assertEquals("UTM 局部测量应接近大地线距离", geodeticMeters, utmMeters, 30.0D);
        Assert.assertEquals(geodeticMeters, geodeticLengthMeters, 0.001D);
        Assert.assertEquals(geodeticMeters / 1000.0D, geodeticKilometers, 0.0000001D);
        Assert.assertEquals(geodeticMeters / 1609.34D, geodeticMiles, 0.0000001D);
        Assert.assertEquals(geodeticMeters / 111319.9D, geodeticDegrees, 0.0000001D);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectGeodeticAreaUntilEllipsoidAreaIsProvided() {
        Polygon polygon =
                geometryFactory.createPolygon(
                        new Coordinate[] {
                            new Coordinate(116.40D, 39.90D),
                            new Coordinate(116.41D, 39.90D),
                            new Coordinate(116.41D, 39.91D),
                            new Coordinate(116.40D, 39.90D)
                        });
        measureOpt.calculateArea(
                polygon, 4326, MeasureUnitEnum.SQUARE_METER, MeasureMethodEnum.GEODETIC);
    }
}
