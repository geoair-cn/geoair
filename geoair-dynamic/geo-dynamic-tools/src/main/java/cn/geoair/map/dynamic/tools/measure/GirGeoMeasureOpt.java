package cn.geoair.map.dynamic.tools.measure;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

/**
 * 空间几何面积/长度/距离计算核心接口 支持多输入格式（Geometry/坐标数组/WKT）、多单位转换、多维度距离计算
 *
 * @author 张逢吉
 * @date 2024/12/05
 */
public interface GirGeoMeasureOpt {

	// ====================== 常量定义（统一单位规范） ======================
	/**
	 * 长度单位：米
	 */
	String UNIT_METER = "m";

	/**
	 * 长度单位：千米
	 */
	String UNIT_KILOMETER = "km";

	/**
	 * 长度单位：度（地理坐标系）
	 */
	String UNIT_DEGREE = "degree";

	/**
	 * 长度单位：英里
	 */
	String UNIT_MILE = "mile";

	/**
	 * 面积单位：平方米
	 */
	String UNIT_SQUARE_METER = "m²";

	/**
	 * 面积单位：平方千米
	 */
	String UNIT_SQUARE_KILOMETER = "km²";

	/**
	 * 面积单位：亩
	 */
	String UNIT_ACRE = "acre";

	/**
	 * 面积单位：公顷
	 */
	String UNIT_HECTARE = "hectare";

	/**
	 * Geometry对象计算面积（UTM投影版，高精度）
	 * @param geometry 几何对象（仅支持Polygon/MultiPolygon）
	 * @param srid 源坐标系SRID（4326/4490等地理坐标系）
	 * @param unit 输出单位（支持：m²/km²/acre/hectare）
	 * @return 面积值（保留6位小数）
	 */
	double calculateAreaByUTM(Geometry geometry, int srid, String unit);

	/**
	 * 坐标数组计算面积（UTM投影版，高精度）
	 * @param coords 坐标数组（[[x1,y1],[x2,y2]...]，需闭合，未闭合则自动补全最后一个点）
	 * @param srid 源坐标系SRID（4326/4490等地理坐标系）
	 * @param unit 输出单位（支持：m²/km²/acre/hectare）
	 * @return 面积值（保留6位小数）
	 */
	double calculateAreaByUTM(double[][] coords, int srid, String unit);

	/**
	 * WKT字符串计算面积（UTM投影版，高精度）
	 * @param wkt WKT字符串（如POLYGON((x1 y1,x2 y2,...))）
	 * @param srid 源坐标系SRID（4326/4490等地理坐标系）
	 * @param unit 输出单位（支持：m²/km²/acre/hectare）
	 * @return 面积值（保留6位小数）
	 */
	double calculateAreaByUTM(String wkt, int srid, String unit);

	// ====================== 长度计算 ======================
	/**
	 * Geometry对象计算长度（UTM投影版，高精度）
	 * @param geometry 几何对象（支持LineString/MultiLineString/Polygon/MultiPolygon）
	 * @param srid 源坐标系SRID（4326/4490等地理坐标系）
	 * @param unit 输出单位（支持：m/km/degree/mile）
	 * @return 长度值（保留6位小数）
	 */
	double calculateLengthByUTM(Geometry geometry, int srid, String unit);

	/**
	 * 坐标数组计算长度（UTM投影版，高精度）
	 * @param coords 坐标数组（[[x1,y1],[x2,y2]...]）
	 * @param srid 源坐标系SRID（4326/4490等地理坐标系）
	 * @param unit 输出单位（支持：m/km/degree/mile）
	 * @return 长度值（保留6位小数）
	 */
	double calculateLengthByUTM(double[][] coords, int srid, String unit);

	/**
	 * WKT字符串计算长度（UTM投影版，高精度）
	 * @param wkt WKT字符串（如LINESTRING(x1 y1,x2 y2,...)、POLYGON((x1 y1,x2 y2,...))）
	 * @param srid 源坐标系SRID（4326/4490等地理坐标系）
	 * @param unit 输出单位（支持：m/km/degree/mile）
	 * @return 长度值（保留6位小数）
	 */
	double calculateLengthByUTM(String wkt, int srid, String unit);

	// ====================== UTM投影带工具方法 ======================
	/**
	 * 计算几何对象所属的UTM投影带号
	 * @param geometry 几何对象（任意类型）
	 * @param srid 源坐标系SRID（4326/4490等地理坐标系）
	 * @return UTM带号（如50、51）
	 */
	int getUTMZone(Geometry geometry, int srid);

	/**
	 * 根据UTM带号和纬度判断半球，获取对应的SRID
	 * @param zone UTM带号（1-60）
	 * @param latitude 纬度（用于判断南北半球，北纬>0，南纬<0）
	 * @return UTM投影SRID（如32650=UTM 50N，32750=UTM 50S）
	 */
	int getUTMSRID(int zone, double latitude);

	// ====================== 面积计算 ======================

	/**
	 * Geometry对象计算面积
	 * @param geometry 几何对象（仅支持Polygon/MultiPolygon）
	 * @param srid 坐标系SRID（4326=WGS84，3857=Web墨卡托，4490=2000国家大地坐标系）
	 * @param unit 输出单位（支持：m²/km²/acre/hectare）
	 * @return 面积值（保留6位小数）
	 */
	double calculateArea(Geometry geometry, int srid, String unit);

	/**
	 * 坐标数组计算面积（多边形）
	 * @param coords 坐标数组（[[x1,y1],[x2,y2]...]，需闭合，未闭合则自动补全最后一个点）
	 * @param srid 坐标系SRID
	 * @param unit 输出单位（支持：m²/km²/acre/hectare）
	 * @return 面积值（保留6位小数）
	 */
	double calculateArea(double[][] coords, int srid, String unit);

	/**
	 * WKT字符串计算面积
	 * @param wkt WKT字符串（如POLYGON((x1 y1,x2 y2,...))）
	 * @param srid 坐标系SRID
	 * @param unit 输出单位（支持：m²/km²/acre/hectare）
	 * @return 面积值（保留6位小数）
	 */
	double calculateArea(String wkt, int srid, String unit);

	// ====================== 长度计算 ======================

	/**
	 * Geometry对象计算长度（线/多边形周长，推荐使用）
	 * @param geometry 几何对象（支持LineString/MultiLineString/Polygon/MultiPolygon）
	 * @param srid 坐标系SRID
	 * @param unit 输出单位（支持：m/km/degree/mile）
	 * @return 长度值（保留6位小数）
	 */
	double calculateLength(Geometry geometry, int srid, String unit);

	/**
	 * 坐标数组计算长度（线）
	 * @param coords 坐标数组（[[x1,y1],[x2,y2]...]）
	 * @param srid 坐标系SRID
	 * @param unit 输出单位（支持：m/km/degree/mile）
	 * @return 长度值（保留6位小数）
	 */
	double calculateLength(double[][] coords, int srid, String unit);

	/**
	 * WKT字符串计算长度
	 * @param wkt WKT字符串（如LINESTRING(x1 y1,x2 y2,...)、POLYGON((x1 y1,x2 y2,...))）
	 * @param srid 坐标系SRID
	 * @param unit 输出单位（支持：m/km/degree/mile）
	 * @return 长度值（保留6位小数）
	 */
	double calculateLength(String wkt, int srid, String unit);

	// ====================== 距离计算（新增Geometry对象入参 + 保留原数组入参） ======================

	/**
	 * 两点最短距离（Point对象入参，推荐使用） 地理坐标系（如4326）使用大地测量计算球面最短距离，投影坐标系计算欧氏距离
	 * @param point1 点1几何对象（仅支持Point类型）
	 * @param point2 点2几何对象（仅支持Point类型）
	 * @param srid 坐标系SRID
	 * @param unit 输出单位（支持：m/km/degree/mile）
	 * @return 最短距离（保留6位小数）
	 */
	double calculatePointToPointDistance(Point point1, Point point2, int srid, String unit);

	/**
	 * 两点最短距离（坐标数组入参，兼容旧调用）
	 * @param point1 点1坐标 [x1,y1]
	 * @param point2 点2坐标 [x2,y2]
	 * @param srid 坐标系SRID
	 * @param unit 输出单位（支持：m/km/degree/mile）
	 * @return 最短距离（保留6位小数）
	 */
	double calculatePointToPointDistance(double[] point1, double[] point2, int srid, String unit);

	/**
	 * 点到线的最近距离（Point+LineString对象入参，推荐使用）
	 * @param point 点几何对象（仅支持Point类型）
	 * @param line 线几何对象（仅支持LineString/MultiLineString类型）
	 * @param srid 坐标系SRID
	 * @param unit 输出单位（支持：m/km/degree/mile）
	 * @return 最近距离（保留6位小数）
	 */
	double calculatePointToLineMinDistance(Point point, LineString line, int srid, String unit);

	/**
	 * 点到线的最近距离（坐标数组入参，兼容旧调用）
	 * @param point 点坐标 [x,y]
	 * @param lineCoords 线坐标数组 [[x1,y1],[x2,y2]...]
	 * @param srid 坐标系SRID
	 * @param unit 输出单位（支持：m/km/degree/mile）
	 * @return 最近距离（保留6位小数）
	 */
	double calculatePointToLineMinDistance(double[] point, double[][] lineCoords, int srid, String unit);

	/**
	 * 点到几何对象的最短距离（保留原方法）
	 * @param point 点几何对象（仅支持Point类型）
	 * @param geometry 目标几何对象（支持任意Geometry类型）
	 * @param srid 坐标系SRID
	 * @param unit 输出单位（支持：m/km/degree/mile）
	 * @return 最短距离（保留6位小数）
	 */
	double calculatePointToGeometryDistance(Point point, Geometry geometry, int srid, String unit);

	/**
	 * 线到线的最短距离（LineString对象入参，推荐使用）
	 * @param line1 线1几何对象（仅支持LineString/MultiLineString类型）
	 * @param line2 线2几何对象（仅支持LineString/MultiLineString类型）
	 * @param srid 坐标系SRID
	 * @param unit 输出单位（支持：m/km/degree/mile）
	 * @return 最短距离（保留6位小数）
	 */
	double calculateLineToLineMinDistance(LineString line1, LineString line2, int srid, String unit);

	/**
	 * 线到线的最短距离（坐标数组入参，兼容旧调用）
	 * @param line1Coords 线1坐标数组 [[x1,y1],[x2,y2]...]
	 * @param line2Coords 线2坐标数组 [[x1,y1],[x2,y2]...]
	 * @param srid 坐标系SRID
	 * @param unit 输出单位（支持：m/km/degree/mile）
	 * @return 最短距离（保留6位小数）
	 */
	double calculateLineToLineMinDistance(double[][] line1Coords, double[][] line2Coords, int srid, String unit);

	// ====================== 单位转换 ======================

	/**
	 * 单位转换（核心方法）
	 * @param value 原始值
	 * @param srcUnit 原始单位（长度：m/km/degree/mile；面积：m²/km²/acre/hectare）
	 * @param targetUnit 目标单位（长度：m/km/degree/mile；面积：m²/km²/acre/hectare）
	 * @param srid 坐标系SRID（地理坐标系需球面转换，投影坐标系直接转换）
	 * @return 转换后值（保留6位小数）
	 */
	double convertUnit(double value, String srcUnit, String targetUnit, int srid);

}
