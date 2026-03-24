package cn.geoair.map.dynamic.tools.merge;

import org.locationtech.jts.geom.*;

import cn.geoair.base.Gir;

public class GeoMergeDemo {

	public static void main(String[] args) {
		GirGeoMergeUtils mergeUtils = GirGeoMergeUtils.getInstance();
		GeometryFactory gf = new GeometryFactory();

		// 1. LineString数组合并为MultiLineString
		LineString line1 = gf
				.createLineString(new Coordinate[] { new Coordinate(116.40, 39.90), new Coordinate(116.41, 39.90) });
		LineString line2 = gf
				.createLineString(new Coordinate[] { new Coordinate(116.41, 39.90), new Coordinate(116.42, 39.90) });
		MultiLineString multiLine = mergeUtils.mergeToMultiLineString(new LineString[] { line1, line2 });
		Gir.log.info("MultiLineString WKT: " + multiLine.toText());

		// 2. 首尾衔接的线合并为单个LineString
		LineString singleLine = mergeUtils.mergeToSingleLineString(new LineString[] { line1, line2 });
		Gir.log.info("Single LineString WKT: " + singleLine.toText());

		// 3. Polygon数组拓扑合并为单个Polygon（重叠面）
		Polygon poly1 = gf.createPolygon(new Coordinate[] { new Coordinate(116.40, 39.90),
				new Coordinate(116.41, 39.90), new Coordinate(116.41, 39.91), new Coordinate(116.40, 39.90) });
		Polygon poly2 = gf.createPolygon(new Coordinate[] { new Coordinate(116.41, 39.90),
				new Coordinate(116.42, 39.90), new Coordinate(116.42, 39.91), new Coordinate(116.41, 39.90) });
		Polygon singlePoly = mergeUtils.mergeToSinglePolygon(new Polygon[] { poly1, poly2 });
		Gir.log.info("Single Polygon WKT: " + singlePoly.toText());

		// 4. 坐标数组合并为MultiPolygon
		double[][][] polyCoordsList = { { { 116.40, 39.90 }, { 116.41, 39.90 }, { 116.41, 39.91 }, { 116.40, 39.90 } },
				{ { 116.42, 39.90 }, { 116.43, 39.90 }, { 116.43, 39.91 }, { 116.42, 39.90 } } };
		MultiPolygon multiPoly = mergeUtils.mergeToMultiPolygon(polyCoordsList);
		Gir.log.info("MultiPolygon WKT: " + multiPoly.toText());
	}

}
