package cn.geoair.map.dynamic.tools.coordinate;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

/**
 * 坐标转换核心接口 定义WGS84/GCJ02/BD09等坐标系互转、平面/地理坐标转换规范 支持JTS Geometry对象、批量转换、点线面全类型转换
 *
 * @author 张逢吉
 * @date 2024/12/05
 */
public interface GirCoordinateConvertOpt {

    /**
     * WGS84转GCJ02（经纬度入参）
     *
     * @param lng WGS84经度
     * @param lat WGS84纬度
     * @return 转换后的GCJ02坐标数组 [lng,lat]
     */
    double[] wgs84ToGcj02(double lng, double lat);

    /**
     * GCJ02转WGS84（经纬度入参）
     *
     * @param lng GCJ02经度
     * @param lat GCJ02纬度
     * @return 转换后的WGS84坐标数组 [lng,lat]
     */
    double[] gcj02ToWgs84(double lng, double lat);

    /**
     * GCJ02转BD09（经纬度入参）
     *
     * @param lng GCJ02经度
     * @param lat GCJ02纬度
     * @return 转换后的BD09坐标数组 [lng,lat]
     */
    double[] gcj02ToBd09(double lng, double lat);

    /**
     * BD09转GCJ02（经纬度入参）
     *
     * @param lng BD09经度
     * @param lat BD09纬度
     * @return 转换后的GCJ02坐标数组 [lng,lat]
     */
    double[] bd09ToGcj02(double lng, double lat);

    /**
     * WGS84转BD09（经纬度入参）
     *
     * @param lng WGS84经度
     * @param lat WGS84纬度
     * @return 转换后的BD09坐标数组 [lng,lat]
     */
    double[] wgs84ToBd09(double lng, double lat);

    /**
     * BD09转WGS84（经纬度入参）
     *
     * @param lng BD09经度
     * @param lat BD09纬度
     * @return 转换后的WGS84坐标数组 [lng,lat]
     */
    double[] bd09ToWgs84(double lng, double lat);

    /**
     * 墨卡托转WGS84（墨卡托坐标入参）
     *
     * @param mercatorX 墨卡托X坐标
     * @param mercatorY 墨卡托Y坐标
     * @return 转换后的WGS84坐标数组 [lng,lat]
     */
    double[] mercatorToWgs84(double mercatorX, double mercatorY);

    /**
     * WGS84转墨卡托（经纬度入参）
     *
     * @param lng WGS84经度
     * @param lat WGS84纬度
     * @return 转换后的墨卡托坐标数组 [x,y]
     */
    double[] wgs84ToMercator(double lng, double lat);

    /**
     * 度分秒转十进制度（DMS转DD）
     *
     * @param dmsStr 度分秒格式字符串 如 "123°45′67″"
     * @return 十进制度坐标数组 [lng,lat]
     */
    double[] dmsToDd(String dmsStr);

    /**
     * 十进制度转度分秒（DD转DMS）
     *
     * @param lng 经度
     * @param lat 纬度
     * @return 度分秒格式字符串 如 "123°45′67″"
     */
    String ddToDms(double lng, double lat);

    /**
     * 解析坐标字符串为坐标数组
     *
     * @param coordStr 坐标字符串 如 "123.45,67.89"
     * @param separator 分隔符 如 ","
     * @return 坐标数组 [lng,lat]
     */
    double[] parseCoordString(String coordStr, String separator);

    /**
     * WGS84转GCJ02（经纬度入参，支持异常处理）
     *
     * @param lng WGS84经度
     * @param lat WGS84纬度
     * @param ifExceptionReturnNull 异常时是否返回null（false则抛异常）
     * @return 转换后的GCJ02坐标数组 [lng,lat] 或null
     */
    double[] wgs84ToGcj02(double lng, double lat, boolean ifExceptionReturnNull);

    /**
     * WGS84转GCJ02（Point对象入参）
     *
     * @param point WGS84坐标Point对象
     * @return 转换后的GCJ02 Point对象
     */
    Point wgs84ToGcj02(Point point);

    /**
     * GCJ02转WGS84（Point对象入参）
     *
     * @param point GCJ02坐标Point对象
     * @return 转换后的WGS84 Point对象
     */
    Point gcj02ToWgs84(Point point);

    /**
     * GCJ02转BD09（Point对象入参）
     *
     * @param point GCJ02坐标Point对象
     * @return 转换后的BD09 Point对象
     */
    Point gcj02ToBd09(Point point);

    /**
     * BD09转GCJ02（Point对象入参）
     *
     * @param point BD09坐标Point对象
     * @return 转换后的GCJ02 Point对象
     */
    Point bd09ToGcj02(Point point);

    /**
     * WGS84转BD09（Point对象入参）
     *
     * @param point WGS84坐标Point对象
     * @return 转换后的BD09 Point对象
     */
    Point wgs84ToBd09(Point point);

    /**
     * BD09转WGS84（Point对象入参）
     *
     * @param point BD09坐标Point对象
     * @return 转换后的WGS84 Point对象
     */
    Point bd09ToWgs84(Point point);

    /**
     * 墨卡托转WGS84（Point对象入参）
     *
     * @param point 墨卡托坐标Point对象
     * @return 转换后的WGS84 Point对象
     */
    Point mercatorToWgs84(Point point);

    /**
     * WGS84转墨卡托（Point对象入参）
     *
     * @param point WGS84坐标Point对象
     * @return 转换后的墨卡托Point对象
     */
    Point wgs84ToMercator(Point point);

    /**
     * WGS84转GCJ02（批量坐标数组）
     *
     * @param coords WGS84坐标二维数组 [[lng1,lat1],[lng2,lat2]...]
     * @param ifExceptionReturnNull 异常时是否返回null（false则抛异常）
     * @return 转换后的GCJ02坐标二维数组
     */
    double[][] wgs84ToGcj02Batch(double[][] coords, boolean ifExceptionReturnNull);

    /**
     * GCJ02转WGS84（批量坐标数组）
     *
     * @param coords GCJ02坐标二维数组 [[lng1,lat1],[lng2,lat2]...]
     * @param ifExceptionReturnNull 异常时是否返回null
     * @return 转换后的WGS84坐标二维数组
     */
    double[][] gcj02ToWgs84Batch(double[][] coords, boolean ifExceptionReturnNull);

    /**
     * GCJ02转BD09（批量坐标数组）
     *
     * @param coords GCJ02坐标二维数组 [[lng1,lat1],[lng2,lat2]...]
     * @param ifExceptionReturnNull 异常时是否返回null
     * @return 转换后的BD09坐标二维数组
     */
    double[][] gcj02ToBd09Batch(double[][] coords, boolean ifExceptionReturnNull);

    /**
     * BD09转WGS84（批量坐标数组）
     *
     * @param coords BD09坐标二维数组 [[lng1,lat1],[lng2,lat2]...]
     * @param ifExceptionReturnNull 异常时是否返回null
     * @return 转换后的WGS84坐标二维数组
     */
    double[][] bd09ToWgs84Batch(double[][] coords, boolean ifExceptionReturnNull);

    /**
     * WGS84转墨卡托（批量坐标数组）
     *
     * @param coords WGS84坐标二维数组 [[lng1,lat1],[lng2,lat2]...]
     * @param ifExceptionReturnNull 异常时是否返回null
     * @return 转换后的墨卡托坐标二维数组
     */
    double[][] wgs84ToMercatorBatch(double[][] coords, boolean ifExceptionReturnNull);

    /**
     * 墨卡托转WGS84（批量坐标数组）
     *
     * @param coords 墨卡托坐标二维数组 [[x1,y1],[x2,y2]...]
     * @param ifExceptionReturnNull 异常时是否返回null
     * @return 转换后的WGS84坐标二维数组
     */
    double[][] mercatorToWgs84Batch(double[][] coords, boolean ifExceptionReturnNull);

    /**
     * WGS84转GCJ02（任意Geometry对象：Point/LineString/Polygon）
     *
     * @param geometry WGS84坐标的Geometry对象
     * @return 转换后的GCJ02坐标Geometry对象
     */
    Geometry wgs84ToGcj02Geometry(Geometry geometry);

    /**
     * GCJ02转WGS84（任意Geometry对象）
     *
     * @param geometry GCJ02坐标的Geometry对象
     * @return 转换后的WGS84坐标Geometry对象
     */
    Geometry gcj02ToWgs84Geometry(Geometry geometry);

    /**
     * GCJ02转BD09（任意Geometry对象）
     *
     * @param geometry GCJ02坐标的Geometry对象
     * @return 转换后的BD09坐标Geometry对象
     */
    Geometry gcj02ToBd09Geometry(Geometry geometry);

    /**
     * BD09转WGS84（任意Geometry对象）
     *
     * @param geometry BD09坐标的Geometry对象
     * @return 转换后的WGS84坐标Geometry对象
     */
    Geometry bd09ToWgs84Geometry(Geometry geometry);

    /**
     * WGS84转墨卡托（任意Geometry对象）
     *
     * @param geometry WGS84坐标的Geometry对象
     * @return 转换后的墨卡托坐标Geometry对象
     */
    Geometry wgs84ToMercatorGeometry(Geometry geometry);

    /**
     * 墨卡托转WGS84（任意Geometry对象）
     *
     * @param geometry 墨卡托坐标的Geometry对象
     * @return 转换后的WGS84坐标Geometry对象
     */
    Geometry mercatorToWgs84Geometry(Geometry geometry);

    /**
     * WGS84转GCJ02（Geometry对象，支持异常返回null）
     *
     * @param geometry WGS84坐标Geometry对象
     * @param ifExceptionReturnNull 异常时是否返回null
     * @return 转换后的Geometry对象或null
     */
    Geometry wgs84ToGcj02Geometry(Geometry geometry, boolean ifExceptionReturnNull);

    /**
     * GCJ02转WGS84（Geometry对象，支持异常返回null）
     *
     * @param geometry GCJ02坐标Geometry对象
     * @param ifExceptionReturnNull 异常时是否返回null
     * @return 转换后的Geometry对象或null
     */
    Geometry gcj02ToWgs84Geometry(Geometry geometry, boolean ifExceptionReturnNull);



}
