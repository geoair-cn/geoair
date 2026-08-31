package cn.geoair.map.dynamic.tools.measure;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

/**
 * 空间几何面积、长度和距离测量接口。
 *
 * <p>支持 Geometry、坐标数组和 WKT 三类输入。面积和长度结果取决于所选择的测量方式： Web Mercator 适合地图展示和快速估算，UTM
 * 适合单个投影带内的局部测量；大地线方式适合 两点距离和线、面边界长度。调用方应先使用 {@code GirFormatUtils} 或 JTS 将输入转换为 {@link
 * Geometry}，所有输出单位均由 {@link MeasureUnitEnum} 明确指定。
 *
 * @author 张逢吉
 * @date 2024/12/05
 */
public interface GirGeoMeasureOpt {

    /**
     * 计算几何对象所在的 UTM 投影带号。
     *
     * @param geometry 几何对象
     * @param srid 源坐标系 SRID
     * @return UTM 带号，范围为 1 至 60
     */
    int getUTMZone(Geometry geometry, int srid);

    /**
     * 根据 UTM 带号和纬度获取对应的投影坐标系 SRID。
     *
     * @param zone UTM 带号，范围为 1 至 60
     * @param latitude 纬度，用于判断南北半球
     * @return 北半球为 EPSG:326xx，南半球为 EPSG:327xx
     */
    int getUTMSRID(int zone, double latitude);

    /**
     * 按指定测量方式计算面积。
     *
     * <p>{@link MeasureMethodEnum#GEODETIC} 当前不支持面积计算；请使用 Web Mercator、UTM 或专业椭球面积算法。
     *
     * @param geometry 面几何对象
     * @param srid 源坐标系 SRID
     * @param unit 输出面积单位
     * @param method 测量方式
     * @return 面积值
     */
    double calculateArea(
            Geometry geometry, int srid, MeasureUnitEnum unit, MeasureMethodEnum method);

    /** 按指定测量方式计算几何对象的长度或周长。 */
    double calculateLength(
            Geometry geometry, int srid, MeasureUnitEnum unit, MeasureMethodEnum method);

    /**
     * 按指定测量方式计算两点距离。
     *
     * <p>Web Mercator 用于地图展示，UTM 用于局部平面测量，GEODETIC 使用椭球大地线距离。
     */
    double calculatePointToPointDistance(
            Point point1, Point point2, int srid, MeasureUnitEnum unit, MeasureMethodEnum method);

    /**
     * 按指定方式计算任意两个几何对象之间的最短距离。
     *
     * <p>Web Mercator 与 UTM 支持任意 JTS Geometry。大地线最短距离仅支持两点，其他组合会被拒绝。
     */
    double calculateGeometryToGeometryMinDistance(
            Geometry geometry1,
            Geometry geometry2,
            int srid,
            MeasureUnitEnum unit,
            MeasureMethodEnum method);

    /**
     * 在相同量纲内进行单位转换。
     *
     * @param value 原始值
     * @param srcUnit 原始单位
     * @param targetUnit 目标单位
     * @return 转换后的值
     * @throws IllegalArgumentException 长度与面积单位混用时抛出
     */
    double convertUnit(double value, MeasureUnitEnum srcUnit, MeasureUnitEnum targetUnit);
}
