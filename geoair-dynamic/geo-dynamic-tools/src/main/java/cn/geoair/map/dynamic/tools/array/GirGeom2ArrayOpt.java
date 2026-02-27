package cn.geoair.map.dynamic.tools.array;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * 几何对象与坐标数组互转核心接口
 * 定义Point/LineString与double数组的互转规范，支持指定坐标顺序
 *
 * @author 张逢吉
 * @date 2024/12/06
 */
public interface GirGeom2ArrayOpt {

    /**
     * 坐标顺序枚举
     */
    enum CoordOrder {
        /** X在前（经度/平面X），Y在后（纬度/平面Y）- 默认 */
        X_FIRST,
        /** Y在前（纬度/平面Y），X在后（经度/平面X） */
        Y_FIRST
    }
    /**
     * 一维坐标数组转换为Point（默认X在前）
     * @param coords 一维坐标数组 [x, y]
     * @return JTS Point对象，空数组/非法格式抛出异常
     * @throws IllegalArgumentException 坐标格式错误时抛出
     */
    Point doubleArrayToPoint(double[] coords);

    /**
     * 一维坐标数组转换为Point（支持指定坐标顺序）
     * @param coords 一维坐标数组（按order指定的顺序排列）
     * @param order 坐标顺序（X_FIRST：数组是[x,y]；Y_FIRST：数组是[y,x]）
     * @return JTS Point对象，空数组/非法格式抛出异常
     * @throws IllegalArgumentException 坐标格式错误时抛出
     */
    Point doubleArrayToPoint(double[] coords, CoordOrder order);

    /**
     * 重载：支持指定几何工厂+坐标顺序
     * @param coords 一维坐标数组
     * @param order 坐标顺序（X_FIRST/Y_FIRST）
     * @param factory 自定义GeometryFactory（比如指定SRID）
     * @return Point对象
     */
    Point doubleArrayToPoint(double[] coords, CoordOrder order, GeometryFactory factory);

    /**
     * 快速转换（跳过严格校验，仅用于信任的坐标数据）
     * 性能优先，不校验数值合法性，直接转换
     * @param coords 一维坐标数组
     * @param order 坐标顺序（X_FIRST/Y_FIRST）
     * @return Point对象
     */
    Point doubleArrayToPointFast(double[] coords, CoordOrder order);


    /**
     * 通用坐标数组转换为Geometry（自动识别Point/LineString）
     * @param coords 坐标数组（一维=Point，二维=LineString）
     * @param order 坐标顺序（X_FIRST/Y_FIRST）
     * @return Point/LineString对象，其他维度抛出异常
     * @throws IllegalArgumentException 坐标维度错误时抛出
     */
    Geometry doubleArrayToGeometry(Object coords, CoordOrder order);

    /**
     * 通用坐标数组转换为Geometry（指定几何工厂）
     * @param coords 坐标数组（一维=Point，二维=LineString）
     * @param order 坐标顺序（X_FIRST/Y_FIRST）
     * @param factory 自定义GeometryFactory
     * @return Point/LineString对象
     */
    Geometry doubleArrayToGeometry(Object coords, CoordOrder order, GeometryFactory factory);
    // ====================== 几何对象打散为坐标数组 ======================
    /**
     * Point对象打散为一维坐标数组（默认X在前）
     * @param point JTS Point对象（非空）
     * @return 坐标数组 [x, y]，空对象返回null
     */
    double[] pointToDoubleArray(Point point);

    /**
     * Point对象打散为一维坐标数组（支持指定顺序）
     * @param point JTS Point对象（非空）
     * @param order 坐标顺序（X_FIRST/Y_FIRST）
     * @return 坐标数组，空对象返回null
     */
    double[] pointToDoubleArray(Point point, CoordOrder order);

    /**
     * LineString对象打散为二维坐标数组（默认X在前，严格保留点顺序）
     * @param lineString JTS LineString对象（非空）
     * @return 二维坐标数组，空对象返回null
     */
    double[][] lineStringToDoubleArray(LineString lineString);

    /**
     * LineString对象打散为二维坐标数组（支持指定顺序，严格保留点顺序）
     * @param lineString JTS LineString对象（非空）
     * @param order 坐标顺序（X_FIRST/Y_FIRST）
     * @return 二维坐标数组，空对象返回null
     */
    double[][] lineStringToDoubleArray(LineString lineString, CoordOrder order);

    /**
     * 通用几何对象打散为坐标数组（自动识别Point/LineString，支持指定顺序）
     * @param geometry JTS几何对象（仅支持Point/LineString）
     * @param order 坐标顺序（X_FIRST/Y_FIRST）
     * @return Point返回一维数组，LineString返回二维数组，其他类型返回null
     */
    Object geometryToDoubleArray(Geometry geometry, CoordOrder order);

    // ====================== 坐标数组转换为LineString ======================
    /**
     * 二维坐标数组转换为LineString（默认X在前，严格保证点顺序）
     * @param coords 二维坐标数组 [[x1,y1], [x2,y2], ...]
     * @return JTS LineString对象，空数组返回空LineString
     * @throws IllegalArgumentException 坐标格式错误时抛出
     */
    LineString doubleArrayToLineString(double[][] coords);

    /**
     * 二维坐标数组转换为LineString（支持指定坐标顺序，严格保证点顺序）
     * @param coords 二维坐标数组（按order指定的顺序排列）
     * @param order 坐标顺序（X_FIRST：数组是[x,y]；Y_FIRST：数组是[y,x]）
     * @return JTS LineString对象，空数组返回空LineString
     * @throws IllegalArgumentException 坐标格式错误时抛出
     */
    LineString doubleArrayToLineString(double[][] coords, CoordOrder order);

    /**
     * 重载：支持指定几何工厂+坐标顺序
     * @param coords 二维坐标数组
     * @param order 坐标顺序（X_FIRST/Y_FIRST）
     * @param factory 自定义GeometryFactory（比如指定SRID）
     * @return LineString对象
     */
    LineString doubleArrayToLineString(double[][] coords, CoordOrder order, GeometryFactory factory);

    /**
     * 快速转换（跳过严格校验，仅用于信任的坐标数据，支持指定顺序）
     * 性能优先，不校验数值合法性，仅保证点顺序
     * @param coords 二维坐标数组
     * @param order 坐标顺序（X_FIRST/Y_FIRST）
     * @return LineString对象
     */
    LineString doubleArrayToLineStringFast(double[][] coords, CoordOrder order);
}
