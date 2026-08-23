package cn.geoair.map.dynamic.tools.srid;

import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.hutool.core.util.ObjectUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
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
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;

/**
 * 基于GeoTools的SRID坐标转换工具类（单例模式） 支持EPSG标准SRID互转，内置常用CRS缓存
 *
 * @author 张逢吉
 * @date 2024/12/05
 */
 
public class GirSridConvertUtils implements GirSridConvertOpt {
    private static volatile GirSridConvertUtils INSTANCE;
    /** 按 ToolsConfig 对象身份复用 CRS 与坐标转换缓存。 */
    private static final Map<ToolsConfig, GirSridConvertUtils> CONFIGURED_INSTANCES =
            Collections.synchronizedMap(new IdentityHashMap<ToolsConfig, GirSridConvertUtils>());
    ToolsConfig advToolsConfig;

    public GirSridConvertUtils(ToolsConfig advToolsConfig) {
        this.advToolsConfig = advToolsConfig;
        preloadCommonTransforms();
    }

    public static GirSridConvertUtils getInstance(ToolsConfig advToolsConfig) {
        if (advToolsConfig == null) {
            return getInstance();
        }
        synchronized (CONFIGURED_INSTANCES) {
            GirSridConvertUtils sridConvertUtils = CONFIGURED_INSTANCES.get(advToolsConfig);
            if (sridConvertUtils == null) {
                sridConvertUtils = new GirSridConvertUtils(advToolsConfig);
                CONFIGURED_INSTANCES.put(advToolsConfig, sridConvertUtils);
            }
            return sridConvertUtils;
        }
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
        if (srid <= 0) {
            throw new IllegalArgumentException("SRID必须为正整数，实际值=" + srid);
        }
        try {
            // EPSG:4326是WGS84地理坐标系，特殊处理（提升兼容性）
            if (srid == 4326) {
                return DefaultGeographicCRS.WGS84;
            }
            // 通过SRID获取CRS（自动识别EPSG标准）
            return CRS.decode("EPSG:" + srid, true);
        } catch (FactoryException e) {
            throw new IllegalArgumentException("无法解析CRS，SRID=" + srid, e);
        }
    }

    private final org.locationtech.proj4j.CRSFactory CRS_FACTORY =
            new org.locationtech.proj4j.CRSFactory();

    /**
     * 常用 SRID 的经验判断表。
     *
     * <p>这些编码在业务中使用频率高，且坐标单位是确定的。先走该表可避免部分历史
     * EPSG 别名在不同 Proj4J 数据版本中的解析差异；未命中的 SRID 仍必须完成 CRS
     * 解析后才允许返回结果。</p>
     */
    private static final Map<Integer, Boolean> COMMON_CRS_GEOGRAPHIC_TYPES = createCommonCrsGeographicTypes();

    private final Map<Integer, Boolean> CRS_GEOGRAPHIC_CACHE = new ConcurrentHashMap<>();

    @Override
    public boolean isGeographicCRS(int srid) {
        if (srid <= 0) {
            throw new IllegalArgumentException("SRID必须为正整数，实际值=" + srid);
        }
        if (CRS_GEOGRAPHIC_CACHE.containsKey(srid)) {
            return CRS_GEOGRAPHIC_CACHE.get(srid);
        }

        Boolean knownType = COMMON_CRS_GEOGRAPHIC_TYPES.get(srid);
        if (knownType != null) {
            CRS_GEOGRAPHIC_CACHE.put(srid, knownType);
            return knownType;
        }
        if (isCommonProjectedSrid(srid)) {
            CRS_GEOGRAPHIC_CACHE.put(srid, false);
            return false;
        }

        try {
            // 未知 SRID 先交给 GeoTools 校验，避免 Proj4J 对未知编码返回默认坐标系。
            getCRS(srid);
            org.locationtech.proj4j.CoordinateReferenceSystem crs =
                    CRS_FACTORY.createFromName("EPSG:" + srid);
            if (crs == null) {
                throw new IllegalArgumentException("无法解析CRS，SRID=" + srid);
            }
            Unit units = crs.getProjection().getUnits();
            boolean isGeographic = units == Units.DEGREES || units.name.equalsIgnoreCase("degree");
            CRS_GEOGRAPHIC_CACHE.put(srid, isGeographic);
            return isGeographic;
        } catch (Exception e) {
            throw new IllegalArgumentException("无法识别CRS坐标单位，SRID=" + srid, e);
        }
    }

    /** 创建常用地理/投影坐标系的经验判断表。 */
    private static Map<Integer, Boolean> createCommonCrsGeographicTypes() {
        Map<Integer, Boolean> types = new HashMap<>();

        // 地理坐标系：坐标单位为度。
        types.put(4326, true); // WGS 84
        types.put(4979, true); // WGS 84（三维）
        types.put(4490, true); // CGCS 2000
        types.put(4480, true); // CGCS 2000（三维）
        types.put(4214, true); // 北京 1954
        types.put(4610, true); // 西安 1980
        types.put(4267, true); // NAD27
        types.put(4269, true); // NAD83
        types.put(4230, true); // ED50
        types.put(4258, true); // ETRS89
        types.put(4277, true); // OSGB36
        types.put(4283, true); // GDA94
        types.put(7844, true); // GDA2020
        types.put(4301, true); // Tokyo
        types.put(4314, true); // DHDN
        types.put(4807, true); // NTF (Paris)

        // 常见 Web Mercator 别名：坐标单位为米。
        types.put(3857, false);
        types.put(900913, false);
        types.put(3785, false);
        types.put(102100, false);
        types.put(102113, false);
        return Collections.unmodifiableMap(types);
    }

    /**
     * 判断常用投影坐标系号段。
     *
     * <p>UTM 北/南半球和 CGCS2000 高斯—克吕格号段均以米作为平面坐标单位，
     * 无需再进入 CRS 工厂解析。</p>
     */
    private static boolean isCommonProjectedSrid(int srid) {
        boolean isUtm = (srid >= 32601 && srid <= 32660)
                || (srid >= 32701 && srid <= 32760);
        boolean isCgcs2000GaussKruger = srid >= 4491 && srid <= 4554;
        return isUtm || isCgcs2000GaussKruger;
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
