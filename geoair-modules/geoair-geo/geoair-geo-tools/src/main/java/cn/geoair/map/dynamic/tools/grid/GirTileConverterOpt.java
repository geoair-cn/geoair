package cn.geoair.map.dynamic.tools.grid;

import java.util.List;
import java.util.Set;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import cn.geoair.map.dynamic.tools.grid.converter.AbstractWgs84TileConverter;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;

/**
 * 瓦片转换核心接口 定义XYZ瓦片与地理范围的互转规范，支持不同坐标系扩展
 */
public interface GirTileConverterOpt {

	/**
	 * XYZ瓦片转换为WKT格式的多边形范围
	 * @param z 缩放级别
	 * @param x 瓦片X索引
	 * @param y 瓦片Y索引
	 * @return WKT字符串（POLYGON格式）
	 */
	String xyzToWkt(int z, int x, int y, int targetSrid);

	/**
	 * XYZ瓦片转换为瓦片范围DTO
	 * @param z 缩放级别
	 * @param x 瓦片X索引
	 * @param y 瓦片Y索引
	 * @param targetSrid box网格的坐标系
	 * @return 瓦片范围DTO
	 */
	BoxReferencedEnvelope xyzToTileBox(int z, int x, int y, int targetSrid);

	/**
	 * 地理范围转换为瓦片索引范围
	 * @param z 缩放级别
	 * @param tileBox 地理范围DTO
	 * @return 瓦片索引范围DTO（xmin/xmax: 瓦片X索引；ymin/ymax: 瓦片Y索引）
	 */
	RangeApo tileRangeByBox(int z, Envelope tileBox);

	/**
	 * 将几何图形转换为瓦片坐标范围
	 * @param z 缩放级别
	 * @param geometry 几何图形对象
	 * @return 瓦片坐标范围对象
	 */
	RangeApo tileRangeByGeom(int z, Geometry geometry);

	double tileXToCoordinateX(int x, int z);

	double tileYToCoordinateY(int y, int z);

	/**
	 * TMS Y索引（原点左下角） 与 XYZ Y索引（原点左上角） 的互相转换
	 * @param y Y索引
	 * @param z 缩放级别
	 * @return 翻转后的Y索引
	 */
	int reverseY(int y, int z);

	/**
	 * 非等轴Y索引转换为等轴Y索引（4326坐标系）
	 * <p>
	 * 核心逻辑： 1. 非等轴Y索引 → 对应纬度坐标（基于非等轴跨度：180/2^z） 2. 纬度坐标 → 等轴Y索引（基于等轴跨度：360/2^z）
	 * <p>
	 * 注意：转换后的等轴Y索引可能是浮点数，需根据业务需求取整（默认向下取整）
	 * @param separateAxisY 非等轴Y索引（XYZ规范，原点左上角）
	 * @param zoom 缩放级别（0-30）
	 * @param roundingType 取整方式：FLOOR(向下取整)/CEIL(向上取整)/ROUND(四舍五入)
	 * @return 等轴Y索引（XYZ规范，原点左上角）
	 * @throws IllegalArgumentException 入参不合法时抛出
	 */
	int convertSeparateAxisYToEqualAxisY(int separateAxisY, int zoom,
			AbstractWgs84TileConverter.RoundingType roundingType);

	/**
	 * 反向转换：等轴Y索引 → 非等轴Y索引
	 * <p>
	 * 与convertSeparateAxisYToEqualAxisY互为逆运算
	 * @param equalAxisY 等轴Y索引（XYZ规范）
	 * @param zoom 缩放级别（0-30）
	 * @param roundingType 取整方式
	 * @return 非等轴Y索引（XYZ规范）
	 */
	int convertEqualAxisYToSeparateAxisY(int equalAxisY, int zoom,
			AbstractWgs84TileConverter.RoundingType roundingType);

	/**
	 * 根据指定几何图形和缩放级别获取覆盖的瓦片列表
	 * @param geometry 几何图形对象
	 * @param srcSrid 源坐标系EPSG代码
	 * @param targetZ 目标缩放级别
	 * @return 覆盖的瓦片坐标集合
	 */
	Set<TileZxyApo> zxyListByGeom(Geometry geometry, int srcSrid, int targetZ);

	/**
	 * 根据指定几何图形和多个缩放级别获取覆盖的瓦片列表
	 * @param geometry 几何图形对象
	 * @param srcSrid 源坐标系EPSG代码
	 * @param targetZs 目标缩放级别列表
	 * @return 覆盖的瓦片坐标集合
	 */
	Set<TileZxyApo> zxyListByGeom(Geometry geometry, int srcSrid, List<Integer> targetZs);

	/**
	 * 根据指定几何图形和缩放级别范围获取覆盖的瓦片列表
	 * @param geometry 几何图形对象
	 * @param srcSrid 源坐标系EPSG代码
	 * @param minZ 最小缩放级别
	 * @param maxZ 最大缩放级别
	 * @return 覆盖的瓦片坐标集合
	 */
	Set<TileZxyApo> zxyListByGeom(Geometry geometry, int srcSrid, int minZ, int maxZ);

	/**
	 * 根据指定地理范围和缩放级别获取覆盖的瓦片列表
	 * @param envelope 地理范围对象
	 * @param srcSrid 源坐标系EPSG代码
	 * @param targetZ 目标缩放级别
	 * @return 覆盖的瓦片坐标集合
	 */
	Set<TileZxyApo> zxyListByBox(Envelope envelope, int srcSrid, int targetZ);

	/**
	 * 根据指定地理范围和多个缩放级别获取覆盖的瓦片列表
	 * @param envelope 地理范围对象
	 * @param srcSrid 源坐标系EPSG代码
	 * @param targetZs 目标缩放级别列表
	 * @return 覆盖的瓦片坐标集合
	 */
	Set<TileZxyApo> zxyListByBox(Envelope envelope, int srcSrid, List<Integer> targetZs);

	/**
	 * 根据指定地理范围和缩放级别范围获取覆盖的瓦片列表
	 * @param envelope 地理范围对象
	 * @param srcSrid 源坐标系EPSG代码
	 * @param minZ 最小缩放级别
	 * @param maxZ 最大缩放级别
	 * @return 覆盖的瓦片坐标集合
	 */
	Set<TileZxyApo> zxyListByBox(Envelope envelope, int srcSrid, int minZ, int maxZ);

}
