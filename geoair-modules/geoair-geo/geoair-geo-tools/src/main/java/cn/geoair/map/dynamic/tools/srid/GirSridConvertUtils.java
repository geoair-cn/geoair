package cn.geoair.map.dynamic.tools.srid;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.StaticLog;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.*;
import org.locationtech.proj4j.units.Unit;
import org.locationtech.proj4j.units.Units;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * 基于GeoTools的SRID坐标转换工具类（单例模式） 支持EPSG标准SRID互转，内置常用CRS缓存
 *
 * @author 张逢吉
 * @date 2024/12/05
 */
public class GirSridConvertUtils implements GirSridConvertOpt {
    public static GiLogger log = GirLoggerFactory.getLogger();
    private static volatile GirSridConvertUtils INSTANCE;
    ToolsConfig advToolsConfig;

    public GirSridConvertUtils(ToolsConfig advToolsConfig) {
        this.advToolsConfig = advToolsConfig;
        preloadCommonTransforms();
    }

    public static GirSridConvertUtils getInstance(ToolsConfig advToolsConfig) {
        return new GirSridConvertUtils(advToolsConfig);
    }

    // 转换算子缓存：key=srcSrid_targetSrid，value=MathTransform
    private final Map<String, MathTransform> transformCache = new HashMap<>();

    // 缓存锁（保证线程安全）
    private final Lock cacheLock = new ReentrantLock();

    /** 获取单例实例（双重校验锁） */
    @Deprecated
    public static GirSridConvertUtils getInstance() {
        if (INSTANCE == null) {
            synchronized (GirSridConvertUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GirSridConvertUtils(new ToolsConfig());
                }
            }
        }
        return INSTANCE;
    }

    // ====================== 核心转换方法实现 ======================
    @Override
    public Geometry convert(Geometry geometry, int srcSrid, int targetSrid) {
        return convert(geometry, srcSrid, targetSrid, true);
    }

    @Override
    public Geometry convert(
            Geometry geometry, int srcSrid, int targetSrid, boolean ifExceptionReturnNull) {
        try {
            // 参数校验
            validateParams(geometry, srcSrid, targetSrid);

            // 源SRID与目标SRID相同，直接返回原对象
            if (srcSrid == targetSrid) {
                return geometry;
            }

            // 获取转换算子
            MathTransform transform = getMathTransform(srcSrid, targetSrid);

            // 执行坐标转换（保留原几何对象的SRID属性）
            Geometry transformedGeom = JTS.transform(geometry, transform);
            transformedGeom.setSRID(targetSrid);
            return transformedGeom;
        } catch (Exception e) {
            return handleException(e, ifExceptionReturnNull, null);
        }
    }

    @Override
    public Envelope convert(Envelope envelope, int srcSrid, int targetSrid) {
        return convert(envelope, srcSrid, targetSrid, true);
    }

    public Envelope convert(
            Envelope envelope, int srcSrid, int targetSrid, boolean ifExceptionReturnNull) {
        try {
            // 源SRID与目标SRID相同，直接返回原对象
            if (srcSrid == targetSrid) {
                return envelope;
            }
            // 获取转换算子
            MathTransform transform = getMathTransform(srcSrid, targetSrid);

            return JTS.transform(envelope, transform);
        } catch (Exception e) {
            return handleException(e, ifExceptionReturnNull, null);
        }
    }

    @Override
    public Geometry convertToGeom(
            Envelope envelope, int srcSrid, int targetSrid, boolean ifExceptionReturnNull) {
        Envelope convert = convert(envelope, srcSrid, targetSrid, ifExceptionReturnNull);
        return convertToGeom(convert);
    }

    @Override
    public Geometry convertToGeom(Envelope envelope, int srcSrid, int targetSrid) {
        Envelope convert = convert(envelope, srcSrid, targetSrid);
        return convertToGeom(convert);
    }

    @Override
    public Geometry convertToGeom(Envelope envelope) {
        return JTS.toGeometry(envelope);
    }

    @Override
    public double[] convertPoint(double lng, double lat, int srcSrid, int targetSrid) {
        return convertPoint(lng, lat, srcSrid, targetSrid, false);
    }

    @Override
    public double[] convertPoint(
            double lng, double lat, int srcSrid, int targetSrid, boolean ifExceptionReturnNull) {
        try {
            // 构建点几何对象
            Point point = advToolsConfig.getGeometryFactory().createPoint(new Coordinate(lng, lat));
            point.setSRID(srcSrid);

            // 转换点对象
            Point transformedPoint =
                    (Point) convert(point, srcSrid, targetSrid, ifExceptionReturnNull);
            if (ObjectUtil.isNull(transformedPoint)) {
                return null;
            }

            // 返回坐标数组
            Coordinate coord = transformedPoint.getCoordinate();
            return new double[] {coord.x, coord.y};
        } catch (Exception e) {
            return handleException(e, ifExceptionReturnNull, null);
        }
    }

    @Override
    public CoordinateReferenceSystem getCRS(int srid) {
        try {
            // EPSG:4326是WGS84地理坐标系，特殊处理（提升兼容性）
            if (srid == 4326) {
                return DefaultGeographicCRS.WGS84;
            }
            // 通过SRID获取CRS（自动识别EPSG标准）
            return CRS.decode("EPSG:" + srid, true);
        } catch (FactoryException e) {
            throw new RuntimeException("获取坐标参考系失败，SRID=" + srid, e);
        }
    }

    private final org.locationtech.proj4j.CRSFactory CRS_FACTORY =
            new org.locationtech.proj4j.CRSFactory();

    private final Map<Integer, Boolean> CRS_GEOGRAPHIC_CACHE = new ConcurrentHashMap<>();

    @Override
    public boolean isGeographicCRS(int srid) {
        if (CRS_GEOGRAPHIC_CACHE.containsKey(srid)) {
            return CRS_GEOGRAPHIC_CACHE.get(srid);
        }
        boolean isGeographic = true;
        try {
            org.locationtech.proj4j.CoordinateReferenceSystem crs =
                    CRS_FACTORY.createFromName("EPSG:" + srid);
            Unit units = crs.getProjection().getUnits();
            isGeographic = units == Units.DEGREES || units.name.equalsIgnoreCase("degree");
            return isGeographic;
        } catch (Exception e) {
            log.error("识别坐标异常", e);
            if (srid == 3857 || srid == 900913) {
                isGeographic = false;
            }
            // 异常处理：默认按经验判断（4326=度，其他=米）
            StaticLog.warn("无法识别SRID={}的坐标系单位，按经验判断 {}", srid, isGeographic);
        } finally {
            CRS_GEOGRAPHIC_CACHE.put(srid, isGeographic);
        }
        return isGeographic;
    }

    @Override
    public void clearTransformCache() {
        cacheLock.lock();
        try {
            transformCache.clear();
        } finally {
            cacheLock.unlock();
        }
    }

    // ====================== 私有工具方法 ======================

    /** 预加载常用转换算子（4326↔3857、4326↔4490等） */
    private void preloadCommonTransforms() {
        try {
            // WGS84(4326) ↔ Web墨卡托(3857)
            getMathTransform(4326, 3857);
            getMathTransform(3857, 4326);

            // WGS84(4326) ↔ 2000国家大地坐标系(4490)
            getMathTransform(4326, 4490);
            getMathTransform(4490, 4326);

            // Web墨卡托(3857) ↔ 2000国家大地坐标系(4490)
            getMathTransform(3857, 4490);
            getMathTransform(4490, 3857);
        } catch (Exception e) {
            throw new RuntimeException("预加载常用转换算子失败", e);
        }
    }

    /** 获取坐标转换算子（带缓存） */
    public MathTransform getMathTransform(int srcSrid, int targetSrid) {
        String cacheKey = srcSrid + "_" + targetSrid;

        // 先从缓存获取
        cacheLock.lock();
        try {
            if (transformCache.containsKey(cacheKey)) {
                return transformCache.get(cacheKey);
            }
        } finally {
            cacheLock.unlock();
        }

        // 缓存未命中，创建转换算子
        CoordinateReferenceSystem srcCRS = getCRS(srcSrid);
        CoordinateReferenceSystem targetCRS = getCRS(targetSrid);

        // 创建转换算子（lenient=true：允许轻微的CRS不兼容）
        MathTransform transform = null;
        try {
            transform = CRS.findMathTransform(srcCRS, targetCRS, true);
        } catch (Exception e) {
            throw new RuntimeException("创建坐标转换算子失败，SRID=" + srcSrid + "->" + targetSrid, e);
        }
        // 存入缓存
        cacheLock.lock();
        try {
            transformCache.put(cacheKey, transform);
        } finally {
            cacheLock.unlock();
        }

        return transform;
    }

    /** 参数校验 */
    private void validateParams(Geometry geometry, int srcSrid, int targetSrid) {
        if (ObjectUtil.isNull(geometry)) {
            throw new IllegalArgumentException("几何对象不能为空");
        }
        if (srcSrid <= 0 || targetSrid <= 0) {
            throw new IllegalArgumentException("SRID必须为正整数（如4326、3857）");
        }
        if (geometry.isEmpty()) {
            throw new IllegalArgumentException("几何对象不能为空几何");
        }
    }

    /** 通用异常处理 */
    private <T> T handleException(Exception e, boolean ifExceptionReturnNull, T nullValue) {
        if (ifExceptionReturnNull) {
            return null;
        } else {
            if (e instanceof TransformException) {
                throw new RuntimeException("坐标转换失败：" + e.getMessage(), e);
            } else if (e instanceof FactoryException) {
                throw new RuntimeException("CRS初始化失败：" + e.getMessage(), e);
            } else {
                throw new RuntimeException("SRID转换异常", e);
            }
        }
    }

    // ====================== 扩展便捷方法 ======================

    /** WGS84(4326)转Web墨卡托(3857)（便捷方法） */
    public Geometry wgs84ToWebMercator(Geometry geometry) {
        return convert(geometry, 4326, 3857);
    }

    /** Web墨卡托(3857)转WGS84(4326)（便捷方法） */
    public Geometry webMercatorToWgs84(Geometry geometry) {
        return convert(geometry, 3857, 4326);
    }

    /** WGS84(4326)转2000国家大地坐标系(4490)（便捷方法） */
    public Geometry wgs84ToCgcs2000(Geometry geometry) {
        return convert(geometry, 4326, 4490);
    }
}
