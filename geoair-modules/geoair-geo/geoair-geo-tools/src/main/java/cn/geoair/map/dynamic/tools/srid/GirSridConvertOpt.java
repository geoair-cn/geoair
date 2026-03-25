package cn.geoair.map.dynamic.tools.srid;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * SRID坐标转换核心接口 基于GeoTools实现不同空间参考系（EPSG SRID）的几何对象转换
 *
 * @author 张逢吉
 * @date 2024/12/05
 */
public interface GirSridConvertOpt {

    /**
     * 几何对象SRID转换（核心方法）
     *
     * @param geometry 待转换的JTS几何对象（如Point、LineString、Polygon）
     * @param srcSrid 源坐标系SRID（如4326）
     * @param targetSrid 目标坐标系SRID（如3857）
     * @return 转换后的几何对象
     */
    Geometry convert(Geometry geometry, int srcSrid, int targetSrid);

    /**
     * 几何对象SRID转换（支持异常返回null）
     *
     * @param geometry 待转换的JTS几何对象
     * @param srcSrid 源坐标系SRID
     * @param targetSrid 目标坐标系SRID
     * @param ifExceptionReturnNull 异常时是否返回null（否则抛运行时异常）
     * @return 转换后的几何对象
     */
    Geometry convert(Geometry geometry, int srcSrid, int targetSrid, boolean ifExceptionReturnNull);

    /**
     * 包围盒SRID转换
     *
     * @param envelope 待转换的JTS包围盒对象
     * @param srcSrid 源坐标系SRID
     * @param targetSrid 目标坐标系SRID
     * @return 转换后的包围盒对象
     */
    Envelope convert(Envelope envelope, int srcSrid, int targetSrid);

    /**
     * 包围盒SRID转换（支持异常返回null）
     *
     * @param envelope 待转换的JTS包围盒对象
     * @param srcSrid 源坐标系SRID
     * @param targetSrid 目标坐标系SRID
     * @param ifExceptionReturnNull 异常时是否返回null（否则抛运行时异常）
     * @return 转换后的包围盒对象
     */
    Envelope convert(Envelope envelope, int srcSrid, int targetSrid, boolean ifExceptionReturnNull);

    /**
     * 包围盒SRID转换（返回JTS几何对象）
     *
     * @param envelope 待转换的JTS包围盒对象
     * @param srcSrid 源坐标系SRID
     * @param targetSrid 目标坐标系SRID
     * @return 转换后的几何对象
     */
    Geometry convertToGeom(
            Envelope envelope, int srcSrid, int targetSrid, boolean ifExceptionReturnNull);

    /**
     * 包围盒SRID转换（返回JTS几何对象）
     *
     * @param envelope 待转换的JTS包围盒对象
     * @param srcSrid 源坐标系SRID
     * @param targetSrid 目标坐标系SRID
     * @return 转换后的几何对象
     */
    Geometry convertToGeom(Envelope envelope, int srcSrid, int targetSrid);

    /**
     * 包围盒 转换成几何对象
     *
     * @param envelope 待转换的JTS包围盒对象
     * @return 转换后的几何对象
     */
    Geometry convertToGeom(Envelope envelope);

    /**
     * 单点坐标转换（经度/纬度顺序）
     *
     * @param lng 经度（源坐标系）
     * @param lat 纬度（源坐标系）
     * @param srcSrid 源坐标系SRID
     * @param targetSrid 目标坐标系SRID
     * @return 转换后的坐标数组 [x, y]（目标坐标系）
     */
    double[] convertPoint(double lng, double lat, int srcSrid, int targetSrid);

    /**
     * 单点坐标转换（支持异常返回null）
     *
     * @param lng 经度
     * @param lat 纬度
     * @param srcSrid 源坐标系SRID
     * @param targetSrid 目标坐标系SRID
     * @param ifExceptionReturnNull 异常时是否返回null
     * @return 转换后的坐标数组 [x, y]
     */
    double[] convertPoint(
            double lng, double lat, int srcSrid, int targetSrid, boolean ifExceptionReturnNull);

    /**
     * 获取坐标参考系（CRS）
     *
     * @param srid EPSG SRID
     * @return 坐标参考系对象
     */
    CoordinateReferenceSystem getCRS(int srid);

    /**
     * 判断是经纬度坐标还是米值坐标
     *
     * @param srid
     * @return true就是经纬度坐标
     */
    boolean isGeographicCRS(int srid);

    /** 清理转换算子缓存（用于自定义CRS更新场景） */
    void clearTransformCache();

    /** 获取坐标转换算子（不带缓存） */
    MathTransform getMathTransform(int srcSrid, int targetSrid);

    /** WGS84(4326)转Web墨卡托(3857)（便捷方法） */
    Geometry wgs84ToWebMercator(Geometry geometry);

    /** Web墨卡托(3857)转WGS84(4326)（便捷方法） */
    Geometry webMercatorToWgs84(Geometry geometry);

    /** WGS84(4326)转2000国家大地坐标系(4490)（便捷方法） */
    Geometry wgs84ToCgcs2000(Geometry geometry);
}
