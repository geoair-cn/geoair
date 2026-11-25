package cn.geoair.map.dynamic.tools.test;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.convert.GirGeoFormatOpt;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

/** 格式转换 API 示例 */
public class GirGeoToolsFormatExample {

    public static void main(String[] args) {
        GirGeoFormatOpt formatOpt = GirGeoTools.defaultInstance().getFormatOpt();

        String pointGeoJson = "{\"type\":\"Point\",\"coordinates\":[116.40,39.90]}";
        Geometry pointGeometry = formatOpt.geojsonToJtsGeometry(pointGeoJson, false);
        String pointWkt = formatOpt.jtsGeometryToWktString(pointGeometry, false);
        String pointGeoJsonBack = formatOpt.jtsGeometryToGeoJson(pointGeometry, false);

        System.out.println("geojsonToJtsGeometry = " + pointGeometry);
        System.out.println("jtsGeometryToWktString = " + pointWkt);
        System.out.println("jtsGeometryToGeoJson = " + pointGeoJsonBack);

        String polygonWkt =
                "POLYGON((116.40 39.90,116.45 39.90,116.45 39.95,116.40 39.95,116.40 39.90))";
        Geometry polygon = formatOpt.wktToJtsGeometry(polygonWkt, false);
        String polygonGeoJson = formatOpt.wktToGeojson(polygonWkt, false);
        byte[] polygonWkb = formatOpt.wktToWkb(polygonWkt, false);

        System.out.println("wktToJtsGeometry = " + polygon);
        System.out.println("wktToGeojson = " + polygonGeoJson);
        System.out.println("wktToWkb length = " + polygonWkb.length);

        Geometry polygonFromWkb = formatOpt.wkbBytesToJtsGeometry(polygonWkb, false);
        System.out.println("wkbBytesToJtsGeometry = " + polygonFromWkb);

        Point point = formatOpt.jtsPointByString("116.40,39.90");
        System.out.println("jtsPointByString = " + point);
    }
}
