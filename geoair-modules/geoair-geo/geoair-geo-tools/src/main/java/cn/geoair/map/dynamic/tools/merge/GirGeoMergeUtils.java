package cn.geoair.map.dynamic.tools.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.union.UnaryUnionOp;

import cn.geoair.map.dynamic.tools.convert.GirFormatUtils;

import cn.hutool.core.util.ObjectUtil;

/**
 * 几何对象合并工具类（单例模式） 基于JTS实现线/面的多格式合并，支持拓扑合并和简单拼接
 *
 * @author 张逢吉
 * @date 2024/12/05
 */
public class GirGeoMergeUtils implements GirGeoMergeOpt {

	// 单例实例
	private static volatile GirGeoMergeUtils INSTANCE;

	// 几何工厂
	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

	// WKT解析器
	private static final GirFormatUtils CONVERT = GirFormatUtils.getInstance();

	// 锁
	private final Lock lock = new ReentrantLock();

	// 私有构造器
	private GirGeoMergeUtils() {
	}

	/**
	 * 获取单例实例
	 */
	public static GirGeoMergeUtils getInstance() {
		if (INSTANCE == null) {
			synchronized (GirGeoMergeUtils.class) {
				if (INSTANCE == null) {
					INSTANCE = new GirGeoMergeUtils();
				}
			}
		}
		return INSTANCE;
	}

	@Override
	public MultiPoint mergeToMultiPoint(Point[] points) {
		try {
			// 参数校验
			validateGeometryArray(points, Point.class);

			// 合并为MultiLineString
			return GEOMETRY_FACTORY.createMultiPoint(points);
		}
		finally {

		}
	}

	@Override
	public MultiPoint mergeToMultiPoint(String[] wktList) {

		try {
			// WKT转LineString数组
			Point[] points = new Point[wktList.length];
			for (int i = 0; i < wktList.length; i++) {
				Geometry geom = CONVERT.wktToJtsGeometry(wktList[i]);
				if (!(geom instanceof Point)) {
					throw new IllegalArgumentException("WKT[" + i + "]不是Point类型");
				}
				points[i] = (Point) geom;
			}
			return mergeToMultiPoint(points);
		}
		finally {

		}
	}

	// ====================== 线合并实现 ======================
	@Override
	public MultiLineString mergeToMultiLineString(LineString[] lineStrings) {

		try {
			// 参数校验
			validateGeometryArray(lineStrings, LineString.class);

			// 合并为MultiLineString
			return GEOMETRY_FACTORY.createMultiLineString(lineStrings);
		}
		finally {

		}
	}

	@Override
	public MultiLineString mergeToMultiLineString(double[][][] coordsList) {

		try {
			// 坐标数组转LineString数组
			LineString[] lineStrings = new LineString[coordsList.length];
			for (int i = 0; i < coordsList.length; i++) {
				Coordinate[] coords = Arrays.stream(coordsList[i]).map(coord -> new Coordinate(coord[0], coord[1]))
						.toArray(Coordinate[]::new);
				lineStrings[i] = GEOMETRY_FACTORY.createLineString(coords);
			}
			return mergeToMultiLineString(lineStrings);
		}
		finally {

		}
	}

	@Override
	public MultiLineString mergeToMultiLineString(String[] wktList) {

		try {
			// WKT转LineString数组
			LineString[] lineStrings = new LineString[wktList.length];
			for (int i = 0; i < wktList.length; i++) {
				Geometry geom = CONVERT.wktToJtsGeometry(wktList[i]);
				if (!(geom instanceof LineString)) {
					throw new IllegalArgumentException("WKT[" + i + "]不是LineString类型");
				}
				lineStrings[i] = (LineString) geom;
			}
			return mergeToMultiLineString(lineStrings);
		}
		finally {

		}
	}

	@Override
	public LineString mergeToSingleLineString(LineString[] lineStrings) {

		try {
			// 参数校验
			validateGeometryArray(lineStrings, LineString.class);
			if (lineStrings.length == 1) {
				return lineStrings[0];
			}

			// 拼接所有坐标（尝试首尾衔接）
			List<Coordinate> allCoords = new ArrayList<>();
			LineString prevLine = lineStrings[0];
			allCoords.addAll(Arrays.asList(prevLine.getCoordinates()));

			for (int i = 1; i < lineStrings.length; i++) {
				LineString currLine = lineStrings[i];
				Coordinate[] currCoords = currLine.getCoordinates();

				// 检查前一条线的最后一个点是否与当前线第一个点衔接
				Coordinate lastPrevCoord = allCoords.get(allCoords.size() - 1);
				Coordinate firstCurrCoord = currCoords[0];
				if (!lastPrevCoord.equals(firstCurrCoord)) {
					throw new IllegalArgumentException("第" + i + "条线与前一条线首尾不衔接，无法合并为单个LineString");
				}

				// 拼接坐标（跳过重复的第一个点）
				allCoords.addAll(Arrays.asList(Arrays.copyOfRange(currCoords, 1, currCoords.length)));
			}

			return GEOMETRY_FACTORY.createLineString(allCoords.toArray(new Coordinate[0]));
		}
		finally {

		}
	}

	@Override
	public LineString mergeToSingleLineString(double[][][] coordsList) {

		try {
			// 坐标数组转LineString数组
			LineString[] lineStrings = new LineString[coordsList.length];
			for (int i = 0; i < coordsList.length; i++) {
				Coordinate[] coords = Arrays.stream(coordsList[i]).map(coord -> new Coordinate(coord[0], coord[1]))
						.toArray(Coordinate[]::new);
				lineStrings[i] = GEOMETRY_FACTORY.createLineString(coords);
			}
			return mergeToSingleLineString(lineStrings);
		}
		finally {

		}
	}

	// ====================== 面合并实现 ======================
	@Override
	public MultiPolygon mergeToMultiPolygon(Polygon[] polygons) {

		try {
			// 参数校验
			validateGeometryArray(polygons, Polygon.class);

			// 合并为MultiPolygon
			return GEOMETRY_FACTORY.createMultiPolygon(polygons);
		}
		finally {

		}
	}

	@Override
	public MultiPolygon mergeToMultiPolygon(double[][][] coordsList) {

		try {
			// 坐标数组转Polygon数组
			Polygon[] polygons = new Polygon[coordsList.length];
			for (int i = 0; i < coordsList.length; i++) {
				Coordinate[] coords = Arrays.stream(coordsList[i]).map(coord -> new Coordinate(coord[0], coord[1]))
						.toArray(Coordinate[]::new);
				// 确保面闭合
				if (!coords[0].equals(coords[coords.length - 1])) {
					coords = Arrays.copyOf(coords, coords.length + 1);
					coords[coords.length - 1] = coords[0];
				}
				LinearRing ring = GEOMETRY_FACTORY.createLinearRing(coords);
				polygons[i] = GEOMETRY_FACTORY.createPolygon(ring);
			}
			return mergeToMultiPolygon(polygons);
		}
		finally {

		}
	}

	@Override
	public MultiPolygon mergeToMultiPolygon(String[] wktList) {

		try {
			// WKT转Polygon数组
			Polygon[] polygons = new Polygon[wktList.length];
			for (int i = 0; i < wktList.length; i++) {
				Geometry geom = CONVERT.wktToJtsGeometry(wktList[i]);
				if (!(geom instanceof Polygon)) {
					throw new IllegalArgumentException("WKT[" + i + "]不是Polygon类型");
				}
				polygons[i] = (Polygon) geom;
			}
			return mergeToMultiPolygon(polygons);
		}
		finally {

		}
	}

	@Override
	public Polygon mergeToSinglePolygon(Polygon[] polygons) {

		try {
			// 参数校验
			validateGeometryArray(polygons, Polygon.class);
			if (polygons.length == 1) {
				return polygons[0];
			}

			// 拓扑合并（Union操作）
			Geometry unionGeom = UnaryUnionOp.union(Arrays.asList(polygons));
			if (!(unionGeom instanceof Polygon)) {
				throw new IllegalArgumentException("多个面无法拓扑合并为单个Polygon（非重叠/相邻）");
			}
			return (Polygon) unionGeom;
		}
		finally {

		}
	}

	@Override
	public Polygon mergeToSinglePolygon(double[][][] coordsList) {

		try {
			// 坐标数组转Polygon数组
			Polygon[] polygons = new Polygon[coordsList.length];
			for (int i = 0; i < coordsList.length; i++) {
				Coordinate[] coords = Arrays.stream(coordsList[i]).map(coord -> new Coordinate(coord[0], coord[1]))
						.toArray(Coordinate[]::new);
				// 确保面闭合
				if (!coords[0].equals(coords[coords.length - 1])) {
					coords = Arrays.copyOf(coords, coords.length + 1);
					coords[coords.length - 1] = coords[0];
				}
				LinearRing ring = GEOMETRY_FACTORY.createLinearRing(coords);
				polygons[i] = GEOMETRY_FACTORY.createPolygon(ring);
			}
			return mergeToSinglePolygon(polygons);
		}
		finally {

		}
	}

	// ====================== 通用合并实现 ======================
	@Override
	public Geometry mergeToMultiGeometry(Geometry[] geometries) {

		try {
			// 参数校验
			if (ObjectUtil.isEmpty(geometries)) {
				throw new IllegalArgumentException("Geometry数组不能为空");
			}

			// 统一几何类型
			Class<? extends Geometry> geomClass = geometries[0].getClass();
			for (Geometry geom : geometries) {
				if (!geomClass.equals(geom.getClass())) {
					throw new IllegalArgumentException("Geometry数组类型不一致，需全为同类型（LineString/Polygon）");
				}
			}

			// 合并为对应Multi几何
			if (geomClass == LineString.class) {
				return mergeToMultiLineString((LineString[]) geometries);
			}
			else if (geomClass == Polygon.class) {
				return mergeToMultiPolygon((Polygon[]) geometries);
			}
			else if (geomClass == Point.class) {
				return mergeToMultiPoint((Point[]) geometries);
			}
			else {
				throw new IllegalArgumentException("不支持的几何类型：" + geomClass.getSimpleName());
			}
		}
		finally {

		}
	}

	@Override
	public Geometry geometryToMultiGeometry(Geometry geometrie) {
		try {
			// 参数校验
			if (ObjectUtil.isEmpty(geometrie)) {
				throw new IllegalArgumentException("Geometry数组不能为空");
			}

			// 统一几何类型
			Class<? extends Geometry> geomClass = geometrie.getClass();
			// 合并为对应Multi几何
			if (geomClass == MultiLineString.class) {
				return geometrie;
			}
			if (geomClass == MultiPolygon.class) {
				return geometrie;
			}
			if (geomClass == LineString.class) {
				return mergeToMultiLineString(new LineString[] { (LineString) geometrie });
			}
			else if (geomClass == Polygon.class) {
				return mergeToMultiPolygon(new Polygon[] { (Polygon) geometrie });
			}
			else if (geomClass == Point.class) {
				return mergeToMultiPoint(new Point[] { (Point) geometrie });
			}
			else {
				throw new IllegalArgumentException("不支持的几何类型：" + geomClass.getSimpleName());
			}
		}
		finally {

		}
	}
	// ====================== 私有工具方法 ======================

	/**
	 * 校验Geometry数组
	 */
	private <T extends Geometry> void validateGeometryArray(T[] geometries, Class<T> clazz) {
		if (ObjectUtil.isEmpty(geometries)) {
			throw new IllegalArgumentException(clazz.getSimpleName() + "数组不能为空");
		}
		for (int i = 0; i < geometries.length; i++) {
			Geometry geom = geometries[i];
			if (ObjectUtil.isNull(geom) || geom.isEmpty()) {
				throw new IllegalArgumentException(clazz.getSimpleName() + "[" + i + "]为空或无效");
			}
			if (!clazz.isInstance(geom)) {
				throw new IllegalArgumentException("[" + i + "]不是" + clazz.getSimpleName() + "类型");
			}
		}
	}

}
