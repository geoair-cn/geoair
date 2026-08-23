package cn.geoair.map.dynamic.tools.array;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

/**
 * 几何对象与坐标数组互转核心接口
 * <p>
 * 定义 Point / LineString / Polygon 与 double 类型数组、List集合 的互相转换规范；
 * 支持自定义坐标顺序（X在前 / Y在前），支持自定义 GeometryFactory；
 * 提供常规转换（带校验）和快速转换（跳过数值校验，高性能）两种模式。
 * 所有 {@code double} 数组 API 均为二维 XY 表达，转换三维或带 M 值 Geometry 时会丢弃额外维度。
 * </p>
 *
 * @author 张逢吉
 * @date 2024/12/06
 */
public interface GirGeom2ArrayOpt {

    /**
     * 坐标顺序枚举
     * <p>
     * 用于指定坐标数组中 经度/纬度 或 X/Y 的排列顺序
     * </p>
     */
    enum CoordOrder {
        /**
         * X在前（经度/平面X），Y在后（纬度/平面Y）- 默认标准顺序
         */
        X_FIRST,

        /**
         * Y在前（纬度/平面Y），X在后（经度/平面X）
         */
        Y_FIRST
    }

    /**
     * 根据 X、Y 坐标值创建 Point 点几何对象
     *
     * @param x 经度/平面X坐标
     * @param y 纬度/平面Y坐标
     * @return JTS Point 几何对象
     */
    Point pointByDouble(double x, double y);

    /**
     * 根据字符串类型的 X、Y 坐标创建 Point 点几何对象
     *
     * @param x 字符串类型经度/平面X坐标
     * @param y 字符串类型纬度/平面Y坐标
     * @return JTS Point 几何对象
     */
    Point pointByString(String x, String y);

    // ====================== double[] 转 Point ======================

    /**
     * 一维基本类型坐标数组 转换为 Point 点几何对象（默认X在前）
     * <p>数组格式：[x, y]</p>
     *
     * @param coords 一维坐标数组，长度必须为2
     * @return JTS Point 对象
     * @throws IllegalArgumentException 坐标数组为空、长度非法时抛出
     */
    Point doubleArrayToPoint(double[] coords);

    /**
     * 一维基本类型坐标数组 转换为 Point 点几何对象（支持指定坐标顺序）
     *
     * @param coords 一维坐标数组，长度必须为2
     * @param order  坐标顺序：X_FIRST=[x,y]，Y_FIRST=[y,x]
     * @return JTS Point 对象
     * @throws IllegalArgumentException 坐标数组为空、长度非法时抛出
     */
    Point doubleArrayToPoint(double[] coords, CoordOrder order);

    /**
     * 一维基本类型坐标数组 转换为 Point 点几何对象
     * 支持指定坐标顺序 + 自定义几何工厂
     *
     * @param coords  一维坐标数组，长度必须为2
     * @param order   坐标顺序
     * @param factory 自定义几何工厂（可指定SRID、精度模型等）
     * @return JTS Point 对象
     * @throws IllegalArgumentException 坐标数组为空、长度非法时抛出
     */
    Point doubleArrayToPoint(double[] coords, CoordOrder order, GeometryFactory factory);

    /**
     * 快速转换：一维坐标数组 → Point 对象
     * <p>
     * 跳过坐标合法性校验，仅适用于可信坐标数据，性能更高
     * </p>
     *
     * @param coords 一维坐标数组
     * @param order  坐标顺序
     * @return JTS Point 对象
     */
    Point doubleArrayToPointFast(double[] coords, CoordOrder order);

    // ====================== List<Double> 转 Point ======================

    /**
     * 包装类型坐标集合 转换为 Point 点几何对象（默认X在前）
     * <p>集合格式：[x, y]</p>
     *
     * @param coords 一维坐标集合，长度必须为2
     * @return JTS Point 对象
     * @throws IllegalArgumentException 坐标集合为空、长度非法时抛出
     */
    Point doubleListToPoint(List<Double> coords);

    /**
     * 包装类型坐标集合 转换为 Point 点几何对象（支持指定坐标顺序）
     *
     * @param coords 一维坐标集合，长度必须为2
     * @param order  坐标顺序
     * @return JTS Point 对象
     * @throws IllegalArgumentException 坐标集合为空、长度非法时抛出
     */
    Point doubleListToPoint(List<Double> coords, CoordOrder order);

    /**
     * 包装类型坐标集合 转换为 Point 点几何对象
     * 支持指定坐标顺序 + 自定义几何工厂
     *
     * @param coords  一维坐标集合，长度必须为2
     * @param order   坐标顺序
     * @param factory 自定义几何工厂
     * @return JTS Point 对象
     * @throws IllegalArgumentException 坐标集合为空、长度非法时抛出
     */
    Point doubleListToPoint(List<Double> coords, CoordOrder order, GeometryFactory factory);

    /**
     * 快速转换：包装类型坐标集合 → Point 对象
     * <p>跳过校验，高性能，仅用于可信数据</p>
     *
     * @param coords 一维坐标集合
     * @param order  坐标顺序
     * @return JTS Point 对象
     */
    Point doubleListToPointFast(List<Double> coords, CoordOrder order);

    // ====================== 通用数组转 Geometry ======================

    /**
     * 通用坐标对象转换为 Geometry 几何对象
     * <p>
     * 自动识别类型：
      * 一维数组 → Point
      * 二维数组 → LineString
     * </p>
     *
     * @param coords 坐标数据（一维/二维）
     * @param order  坐标顺序
     * @return 自动识别后的 Point 或 LineString
     * @throws IllegalArgumentException 坐标维度不支持时抛出
     */
    Geometry doubleArrayToGeometry(Object coords, CoordOrder order);

    /**
     * 通用坐标对象转换为 Geometry 几何对象（支持自定义几何工厂）
     *
     * @param coords  坐标数据（一维/二维）
     * @param order   坐标顺序
     * @param factory 自定义几何工厂
     * @return 自动识别后的 Point 或 LineString
     * @throws IllegalArgumentException 坐标维度不支持时抛出
     */
    Geometry doubleArrayToGeometry(Object coords, CoordOrder order, GeometryFactory factory);

    // ====================== 几何对象打散为坐标数组 ======================

    /**
     * Point 对象 转换为 一维基本类型坐标数组（默认X在前）
     *
     * @param point JTS Point 对象（非空）
     * @return 坐标数组 [x, y]，若对象为空返回 null
     */
    double[] pointToDoubleArray(Point point);

    /**
     * Point 对象 转换为 一维基本类型坐标数组（支持指定坐标顺序）
     *
     * @param point JTS Point 对象
     * @param order 坐标顺序
     * @return 坐标数组，若对象为空返回 null
     */
    double[] pointToDoubleArray(Point point, CoordOrder order);

    /**
     * LineString 对象 转换为 二维基本类型坐标数组（默认X在前）
     * <p>数组格式：[[x1,y1],[x2,y2],...]</p>
     *
     * @param lineString JTS线几何对象
     * @return 二维坐标数组，空对象返回 null
     */
    double[][] lineStringToDoubleArray(LineString lineString);

    /**
     * LineString 对象 转换为 二维基本类型坐标数组（支持指定坐标顺序）
     *
     * @param lineString JTS线几何对象
     * @param order      坐标顺序
     * @return 二维坐标数组，空对象返回 null
     */
    double[][] lineStringToDoubleArray(LineString lineString, CoordOrder order);

    /**
     * 通用几何对象 转换为 坐标数组
     * <p>
     * Point → 一维数组
     * LineString → 二维数组
     * </p>
     *
     * @param geometry 几何对象（Point/LineString）
     * @param order    坐标顺序
     * @return 一维/二维坐标数组，不支持类型返回 null
     */
    Object geometryToDoubleArray(Geometry geometry, CoordOrder order);

    // ====================== double[][] 转 LineString ======================

    /**
     * 二维基本类型坐标数组 转换为 LineString 线几何对象（默认X在前）
     * <p>数组格式：[[x1,y1],[x2,y2],...]</p>
     *
     * @param coords 二维坐标数组
     * @return JTS LineString 对象
     * @throws IllegalArgumentException 坐标数组格式非法时抛出
     */
    LineString doubleArrayToLineString(double[][] coords);

    /**
     * 二维基本类型坐标数组 转换为 LineString 线几何对象（支持指定坐标顺序）
     *
     * @param coords 二维坐标数组
     * @param order  坐标顺序
     * @return JTS LineString 对象
     * @throws IllegalArgumentException 坐标数组格式非法时抛出
     */
    LineString doubleArrayToLineString(double[][] coords, CoordOrder order);

    /**
     * 二维基本类型坐标数组 转换为 LineString 线几何对象
     * 支持指定坐标顺序 + 自定义几何工厂
     *
     * @param coords  二维坐标数组
     * @param order   坐标顺序
     * @param factory 自定义几何工厂
     * @return JTS LineString 对象
     * @throws IllegalArgumentException 坐标数组格式非法时抛出
     */
    LineString doubleArrayToLineString(double[][] coords, CoordOrder order, GeometryFactory factory);

    /**
     * 快速转换：二维坐标数组 → LineString 对象
     * <p>跳过校验，高性能，仅用于可信数据</p>
     *
     * @param coords 二维坐标数组
     * @param order  坐标顺序
     * @return JTS LineString 对象
     */
    LineString doubleArrayToLineStringFast(double[][] coords, CoordOrder order);

    // ====================== List<double[]> 转 LineString ======================

    /**
     * 基本类型坐标集合 转换为 LineString 线几何对象（默认X在前）
     *
     * @param coords 坐标点集合，每个元素为 [x,y] 数组
     * @return JTS LineString 对象
     * @throws IllegalArgumentException 坐标集合格式非法时抛出
     */
    LineString doubleListToLineString(List<double[]> coords);

    /**
     * 基本类型坐标集合 转换为 LineString 线几何对象（支持指定坐标顺序）
     *
     * @param coords 坐标点集合
     * @param order  坐标顺序
     * @return JTS LineString 对象
     * @throws IllegalArgumentException 坐标集合格式非法时抛出
     */
    LineString doubleListToLineString(List<double[]> coords, CoordOrder order);

    /**
     * 基本类型坐标集合 转换为 LineString 线几何对象
     * 支持指定坐标顺序 + 自定义几何工厂
     *
     * @param coords  坐标点集合
     * @param order   坐标顺序
     * @param factory 自定义几何工厂
     * @return JTS LineString 对象
     * @throws IllegalArgumentException 坐标集合格式非法时抛出
     */
    LineString doubleListToLineString(List<double[]> coords, CoordOrder order, GeometryFactory factory);

    /**
     * 快速转换：坐标点集合 → LineString 对象
     * <p>跳过校验，高性能，仅用于可信数据</p>
     *
     * @param coords 坐标点集合
     * @param order  坐标顺序
     * @return JTS LineString 对象
     */
    LineString doubleListToLineStringFast(List<double[]> coords, CoordOrder order);

    // ====================== double[][] 转 Polygon ======================

    /**
     * 二维坐标数组转 Polygon，仅包含外环；未闭合外环会自动闭合。
     *
     * @param shell 外环坐标，格式为 {@code [[x1,y1], ...]}
     * @return 面几何对象
     */
    Polygon doubleArrayToPolygon(double[][] shell);

    /**
     * 二维坐标数组转 Polygon，支持坐标顺序与自定义 GeometryFactory；未闭合外环会自动闭合。
     *
     * @param shell 外环坐标
     * @param order 坐标顺序
     * @param factory 几何工厂
     * @return 面几何对象
     */
    Polygon doubleArrayToPolygon(double[][] shell, CoordOrder order, GeometryFactory factory);

    /**
     * 根据外环和洞构造 Polygon；所有环未闭合时会自动闭合。
     *
     * @param shell 外环坐标
     * @param holes 洞坐标集合，每个元素为一个环
     * @param order 坐标顺序
     * @param factory 几何工厂
     * @return 面几何对象
     */
    Polygon doubleArrayToPolygon(
            double[][] shell, List<double[][]> holes, CoordOrder order, GeometryFactory factory);

    /**
     * 将 Polygon 的全部环转为坐标数组；第一项是外环，其余项依次为洞。
     *
     * @param polygon 面几何对象
     * @param order 输出坐标顺序
     * @return 环坐标数组，格式为 {@code [ring][point][xy]}
     */
    double[][][] polygonToDoubleArrays(Polygon polygon, CoordOrder order);

}
