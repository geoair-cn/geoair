package cn.geoair.map.dynamic.tools.merge;

import org.locationtech.jts.geom.*;

/**
 * 几何对象合并核心接口 支持线/面的多格式合并（Geometry对象/坐标数组/WKT），兼容单对象/多对象合并场景
 *
 * @author 张逢吉
 * @date 2024/12/05
 */
public interface GirGeoMergeOpt {

	/**
	 * points数组合并为MultiPoint（Geometry对象入参）
	 * @param points points数组（非空）
	 * @return 合并后的MultiPoint
	 */
	MultiPoint mergeToMultiPoint(Point[] points);

	/**
	 * WKT字符串数组合并为MultiPoint
	 * @param wktList Point WKT数组
	 * @return 合并后的MultiPoint
	 */
	MultiPoint mergeToMultiPoint(String[] wktList);

	// ====================== 线合并 ======================
	/**
	 * LineString数组合并为MultiLineString（Geometry对象入参）
	 * @param lineStrings LineString数组（非空）
	 * @return 合并后的MultiLineString
	 */
	MultiLineString mergeToMultiLineString(LineString[] lineStrings);

	/**
	 * 坐标数组集合合并为MultiLineString
	 * @param coordsList 线坐标数组集合（每个元素为一条线的坐标数组）
	 * @return 合并后的MultiLineString
	 */
	MultiLineString mergeToMultiLineString(double[][][] coordsList);

	/**
	 * WKT字符串数组合并为MultiLineString
	 * @param wktList LineString WKT数组（如["LINESTRING(x1 y1,x2 y2)","LINESTRING(x3 y3,x4
	 * y4)"]）
	 * @return 合并后的MultiLineString
	 */
	MultiLineString mergeToMultiLineString(String[] wktList);

	/**
	 * LineString数组合并为单个LineString（尝试首尾衔接，无法衔接则抛异常）
	 * @param lineStrings LineString数组（需首尾坐标衔接）
	 * @return 合并后的单个LineString
	 */
	LineString mergeToSingleLineString(LineString[] lineStrings);

	/**
	 * 坐标数组集合合并为单个LineString（尝试首尾衔接）
	 * @param coordsList 线坐标数组集合（需首尾坐标衔接）
	 * @return 合并后的单个LineString
	 */
	LineString mergeToSingleLineString(double[][][] coordsList);

	// ====================== 面合并 ======================
	/**
	 * Polygon数组合并为MultiPolygon（Geometry对象入参）
	 * @param polygons Polygon数组（非空）
	 * @return 合并后的MultiPolygon
	 */
	MultiPolygon mergeToMultiPolygon(Polygon[] polygons);

	/**
	 * 坐标数组集合合并为MultiPolygon
	 * @param coordsList 面坐标数组集合（每个元素为一个面的坐标数组，需闭合）
	 * @return 合并后的MultiPolygon
	 */
	MultiPolygon mergeToMultiPolygon(double[][][] coordsList);

	/**
	 * WKT字符串数组合并为MultiPolygon
	 * @param wktList Polygon WKT数组（如["POLYGON((x1 y1,x2 y2,...))","POLYGON((x3 y3,x4
	 * y4,...))"]）
	 * @return 合并后的MultiPolygon
	 */
	MultiPolygon mergeToMultiPolygon(String[] wktList);

	/**
	 * Polygon数组拓扑合并为单个Polygon（重叠/相邻面合并为一个面）
	 * @param polygons Polygon数组（支持重叠/相邻面）
	 * @return 合并后的单个Polygon
	 */
	Polygon mergeToSinglePolygon(Polygon[] polygons);

	/**
	 * 坐标数组集合拓扑合并为单个Polygon
	 * @param coordsList 面坐标数组集合（支持重叠/相邻面）
	 * @return 合并后的单个Polygon
	 */
	Polygon mergeToSinglePolygon(double[][][] coordsList);

	// ====================== 通用合并 ======================
	/**
	 * 通用Geometry数组合并（自动识别类型，返回对应Multi几何）
	 * @param geometries Geometry数组（需为同类型：全为LineString或全为Polygon）
	 * @return 合并后的MultiGeometry（MultiLineString/MultiPolygon）
	 */
	Geometry mergeToMultiGeometry(Geometry[] geometries);

	/**
	 * 通用Geometry转换接口（自动识别类型，返回对应Multi几何）
	 * @param geometries Geometry 对象
	 * @return 转换后的的MultiGeometry
	 */
	Geometry geometryToMultiGeometry(Geometry geometries);

}
