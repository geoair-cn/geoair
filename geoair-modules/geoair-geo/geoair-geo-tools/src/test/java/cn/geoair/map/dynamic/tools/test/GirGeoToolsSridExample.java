package cn.geoair.map.dynamic.tools.test;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.srid.GirSridConvertOpt;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/** SRID 转换 API 示例 */
public class GirGeoToolsSridExample {

    public static void main(String[] args) {
        GirSridConvertOpt sridOpt = GirGeoTools.defaultInstance().getSridOpt();
        GeometryFactory factory = new GeometryFactory();

        Point point4326 = factory.createPoint(new Coordinate(116.40, 39.90));
        Geometry point3857 = sridOpt.convert(point4326, 4326, 3857);
        double[] xy3857 = sridOpt.convertPoint(116.40, 39.90, 4326, 3857);
        Envelope envelope4326 = new Envelope(116.35, 116.55, 39.85, 40.05);
        Envelope envelope3857 = sridOpt.convert(envelope4326, 4326, 3857);

        System.out.println("convert geometry 4326->3857 = " + point3857);
        System.out.println("convertPoint 4326->3857 = [" + xy3857[0] + ", " + xy3857[1] + "]");
        System.out.println("convert envelope 4326->3857 = " + envelope3857);
        System.out.println("isGeographicCRS(4326) = " + sridOpt.isGeographicCRS(4326));
        System.out.println("isGeographicCRS(3857) = " + sridOpt.isGeographicCRS(3857));
    }
}
