package cn.geoair.map.dynamic.tools.srid;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;


/**
 * 基于 GeoTools 的 EPSG/SRID 坐标参考系转换契约。
 *
 * <p>除显式传入 {@code ifExceptionReturnNull=true} 的方法外，转换失败会抛出运行时异常。
 * 相同 SRID 的转换直接返回原 {@link Geometry} 或 {@link Envelope} 对象，而非副本。</p>
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
     * 包围盒 SRID 转换。
     *
     * <p>该方法转换包围盒，不保留原始几何边界形状；若需要精确投影后的边界，请先转换几何对象。
     * 相同 SRID 时返回原 {@code Envelope} 对象。</p>
     *
     * @param envelope 待转换的JTS包围盒对象
     * @param srcSrid 源坐标系SRID
     * @param targetSrid 目标坐标系SRID
     * @return 转换后的包围盒对象
     */
    Envelope convert(Envelope envelope, int srcSrid, int targetSrid);

    /**
     * 包围盒 SRID 转换（支持异常返回 {@code null}）。
     *
     * @param envelope 待转换的JTS包围盒对象
     * @param srcSrid 源坐标系SRID
     * @param targetSrid 目标坐标系SRID
     * @param ifExceptionReturnNull 异常时是否返回null（否则抛运行时异常）
     * @return 转换后的包围盒对象
     */
    Envelope convert(Envelope envelope, int srcSrid, int targetSrid, boolean ifExceptionReturnNull);

    /**
     * 转换包围盒后，将结果构造成 JTS 矩形几何对象。
     *
     * @param envelope 待转换的JTS包围盒对象
     * @param srcSrid 源坐标系SRID
     * @param targetSrid 目标坐标系SRID
     * @return 转换后的几何对象
     */
    Geometry convertToGeom(
            Envelope envelope, int srcSrid, int targetSrid, boolean ifExceptionReturnNull);

    /**
     * 转换包围盒后，将结果构造成 JTS 矩形几何对象。
     *
     * @param envelope 待转换的JTS包围盒对象
     * @param srcSrid 源坐标系SRID
     * @param targetSrid 目标坐标系SRID
     * @return 转换后的几何对象
     */
    Geometry convertToGeom(Envelope envelope, int srcSrid, int targetSrid);

    /**
     * 将包围盒直接构造成 JTS 矩形几何对象，不进行坐标转换。
     *
     * @param envelope 待转换的JTS包围盒对象
     * @return 转换后的几何对象
     */
    Geometry convertToGeom(Envelope envelope);

    /**
     * 单点坐标转换（输入数组顺序为 X/Y；对地理坐标即经度/纬度）。
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
     * 判断坐标参考系是否为地理坐标参考系。
     *
     * <p>返回 {@code true} 表示坐标轴以角度为单位；返回 {@code false} 表示其不是地理 CRS，
     * 但不保证投影坐标单位一定为米。未知或无法解析的 SRID 会抛出异常，而不会猜测结果。</p>
     *
     * @param srid EPSG SRID
     * @return 是否为地理坐标参考系
     * @throws IllegalArgumentException SRID 非法或无法识别时抛出
     */
    boolean isGeographicCRS(int srid);

    /**
     * 清理 {@link MathTransform} 转换算子缓存。
     *
     * <p>不会清理 CRS 类型判断缓存；常用 SRID 的预加载转换会在下次使用时按需重新创建。</p>
     */
    void clearTransformCache();

    /**
     * 获取源 SRID 到目标 SRID 的转换算子。
     *
     * <p>实现会缓存并复用已创建的算子；该方法名是历史 API，并非每次都重新创建。
     * 算子按宽松模式创建，以兼容轻微的 CRS 定义差异。</p>
     *
     * @param srcSrid 源 EPSG SRID
     * @param targetSrid 目标 EPSG SRID
     * @return 可复用的坐标转换算子
     */
    MathTransform getMathTransform(int srcSrid, int targetSrid);

    /** WGS84(4326)转Web墨卡托(3857)（便捷方法） */
    Geometry wgs84ToWebMercator(Geometry geometry);

    /** Web墨卡托(3857)转WGS84(4326)（便捷方法） */
    Geometry webMercatorToWgs84(Geometry geometry);

    /** WGS84(4326)转2000国家大地坐标系(4490)（便捷方法） */
    Geometry wgs84ToCgcs2000(Geometry geometry);
}
