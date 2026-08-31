package cn.geoair.map.dynamic.tools.measure;

import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.geoair.map.dynamic.tools.srid.GirSridConvertUtils;
import cn.hutool.core.util.ObjectUtil;

import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 空间几何面积/长度/距离计算工具类（单例模式） 基于JTS+GeoTools实现，支持多输入格式、多单位转换
 *
 * @author 张逢吉
 * @date 2024/12/05
 */
public class GirGeoMeasureUtils implements GirGeoMeasureOpt {
    // 单例实例
    private static volatile GirGeoMeasureUtils INSTANCE;

    /** 按 ToolsConfig 对象身份复用测量工具。 */
    private static final Map<ToolsConfig, GirGeoMeasureUtils> CONFIGURED_INSTANCES =
            Collections.synchronizedMap(new IdentityHashMap<ToolsConfig, GirGeoMeasureUtils>());

    ToolsConfig advToolsConfig;
    private final GirSridConvertUtils sridConvert;

    public GirGeoMeasureUtils(ToolsConfig advToolsConfig) {
        this.advToolsConfig = advToolsConfig == null ? new ToolsConfig() : advToolsConfig;
        this.sridConvert = GirSridConvertUtils.getInstance(this.advToolsConfig);
    }

    public static GirGeoMeasureUtils getInstance(ToolsConfig advToolsConfig) {
        if (advToolsConfig == null) {
            return getInstance();
        }
        synchronized (CONFIGURED_INSTANCES) {
            GirGeoMeasureUtils measureUtils = CONFIGURED_INSTANCES.get(advToolsConfig);
            if (measureUtils == null) {
                measureUtils = new GirGeoMeasureUtils(advToolsConfig);
                CONFIGURED_INSTANCES.put(advToolsConfig, measureUtils);
            }
            return measureUtils;
        }
    }

    /** 获取单例实例 */
    @Deprecated
    public static GirGeoMeasureUtils getInstance() {
        if (INSTANCE == null) {
            synchronized (GirGeoMeasureUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GirGeoMeasureUtils(new ToolsConfig());
                }
            }
        }
        return INSTANCE;
    }

    // ====================== 统一测量 API ======================

    @Override
    public double calculateArea(
            Geometry geometry, int srid, MeasureUnitEnum unit, MeasureMethodEnum method) {
        validateMeasureArguments(unit, method, MeasureUnitEnum.MeasureDimension.AREA);
        switch (method) {
            case WEB_MERCATOR:
                return calculateAreaWithWebMercator(geometry, srid, unit);
            case UTM:
                return calculateAreaWithUtm(geometry, srid, unit);
            case GEODETIC:
                throw new IllegalArgumentException("大地线方式暂不支持面积计算");
            default:
                throw new IllegalArgumentException("不支持的测量方式：" + method);
        }
    }

    @Override
    public double calculateLength(
            Geometry geometry, int srid, MeasureUnitEnum unit, MeasureMethodEnum method) {
        validateMeasureArguments(unit, method, MeasureUnitEnum.MeasureDimension.LENGTH);
        validateGeometryType(
                geometry,
                new String[] {"LineString", "MultiLineString", "Polygon", "MultiPolygon"});
        switch (method) {
            case WEB_MERCATOR:
                return calculateLengthWithWebMercator(geometry, srid, unit);
            case UTM:
                return calculateLengthWithUtm(geometry, srid, unit);
            case GEODETIC:
                return convertFromBaseUnit(
                        calculateGeodeticLength(convertToWGS84(geometry, srid)), unit);
            default:
                throw new IllegalArgumentException("不支持的测量方式：" + method);
        }
    }

    /** 使用几何中心所在 UTM 投影带计算局部面积。 */
    private double calculateAreaWithUtm(Geometry geometry, int srid, MeasureUnitEnum unit) {
        try {
            validateGeometryType(geometry, new String[] {"Polygon", "MultiPolygon"});
            // 使用几何中心所在的 UTM 投影带进行局部平面测量。
            Geometry utmGeom = convertToUTMCRS(geometry, srid);
            double areaInM2 = utmGeom.getArea();
            return convertFromBaseUnit(areaInM2, unit);
        } catch (Exception e) {
            throw new RuntimeException("UTM 局部面积计算失败", e);
        }
    }

    /** 使用 Web Mercator 计算地图展示场景下的面积。 */
    private double calculateAreaWithWebMercator(Geometry geometry, int srid, MeasureUnitEnum unit) {
        try {
            validateGeometryType(geometry, new String[] {"Polygon", "MultiPolygon"});
            return convertFromBaseUnit(convertToProjectedCRS(geometry, srid).getArea(), unit);
        } catch (Exception e) {
            throw new RuntimeException("Web Mercator 面积计算失败", e);
        }
    }

    /** 使用几何中心所在 UTM 投影带计算局部长度或周长。 */
    private double calculateLengthWithUtm(Geometry geometry, int srid, MeasureUnitEnum unit) {
        try {
            validateGeometryType(
                    geometry,
                    new String[] {"LineString", "MultiLineString", "Polygon", "MultiPolygon"});
            // 使用几何中心所在的 UTM 投影带进行局部平面测量。
            Geometry utmGeom = convertToUTMCRS(geometry, srid);
            return convertFromBaseUnit(utmGeom.getLength(), unit);
        } catch (Exception e) {
            throw new RuntimeException("UTM 局部长度计算失败", e);
        }
    }

    /** 使用 Web Mercator 计算地图展示场景下的长度或周长。 */
    private double calculateLengthWithWebMercator(
            Geometry geometry, int srid, MeasureUnitEnum unit) {
        try {
            validateGeometryType(
                    geometry,
                    new String[] {"LineString", "MultiLineString", "Polygon", "MultiPolygon"});
            return convertFromBaseUnit(convertToProjectedCRS(geometry, srid).getLength(), unit);
        } catch (Exception e) {
            throw new RuntimeException("Web Mercator 长度计算失败", e);
        }
    }

    // ====================== UTM投影带工具方法实现 ======================
    @Override
    public int getUTMZone(Geometry geometry, int srid) {
        try {
            // 转换为4326地理坐标系计算UTM带号
            Geometry wgs84Geom = convertToWGS84(geometry, srid);
            Envelope envelope = wgs84Geom.getEnvelopeInternal();
            // 取几何中心经度计算UTM带号（6度分带）
            double centerLon = (envelope.getMinX() + envelope.getMaxX()) / 2;
            // UTM带号计算公式：zone = floor((lon + 180) / 6) + 1
            int zone = (int) Math.floor((centerLon + 180) / 6) + 1;
            // 边界修正（确保带号在1-60之间）
            return Math.max(1, Math.min(60, zone));
        } catch (Exception e) {
            throw new RuntimeException("计算UTM投影带号失败", e);
        }
    }

    @Override
    public int getUTMSRID(int zone, double latitude) {
        // UTM SRID规则：
        // 北纬（N）：32600 + 带号（如32650=UTM 50N）
        // 南纬（S）：32700 + 带号（如32750=UTM 50S）
        if (zone < 1 || zone > 60) {
            throw new IllegalArgumentException("UTM带号必须在1-60之间：" + zone);
        }
        return latitude >= 0 ? 32600 + zone : 32700 + zone;
    }

    @Override
    public double calculatePointToPointDistance(
            Point point1, Point point2, int srid, MeasureUnitEnum unit, MeasureMethodEnum method) {
        validateMeasureArguments(unit, method, MeasureUnitEnum.MeasureDimension.LENGTH);
        validateGeometryType(point1, new String[] {"Point"});
        validateGeometryType(point2, new String[] {"Point"});
        try {
            double distanceInM;
            switch (method) {
                case WEB_MERCATOR:
                    Point mercatorPoint1 = (Point) convertToProjectedCRS(point1, srid);
                    Point mercatorPoint2 = (Point) convertToProjectedCRS(point2, srid);
                    distanceInM = mercatorPoint1.distance(mercatorPoint2);
                    break;
                case UTM:
                    LineString sourceLine =
                            advToolsConfig
                                    .getGeometryFactory()
                                    .createLineString(
                                            new Coordinate[] {
                                                point1.getCoordinate(), point2.getCoordinate()
                                            });
                    distanceInM = convertToUTMCRS(sourceLine, srid).getLength();
                    break;
                case GEODETIC:
                    Point wgs84Point1 = (Point) convertToWGS84(point1, srid);
                    Point wgs84Point2 = (Point) convertToWGS84(point2, srid);
                    distanceInM =
                            calculateGeodeticSegmentLength(
                                    wgs84Point1.getCoordinate(), wgs84Point2.getCoordinate());
                    break;
                default:
                    throw new IllegalArgumentException("不支持的测量方式：" + method);
            }
            return convertFromBaseUnit(distanceInM, unit);
        } catch (Exception e) {
            throw new RuntimeException("两点距离计算失败（指定测量方式）", e);
        }
    }

    @Override
    public double calculateGeometryToGeometryMinDistance(
            Geometry geometry1,
            Geometry geometry2,
            int srid,
            MeasureUnitEnum unit,
            MeasureMethodEnum method) {
        validateMeasureArguments(unit, method, MeasureUnitEnum.MeasureDimension.LENGTH);
        if (geometry1 == null || geometry2 == null) {
            throw new IllegalArgumentException("参与距离计算的几何对象不能为空");
        }
        if (method == MeasureMethodEnum.GEODETIC) {
            if (geometry1 instanceof Point && geometry2 instanceof Point) {
                return calculatePointToPointDistance(
                        (Point) geometry1, (Point) geometry2, srid, unit, method);
            }
            throw new IllegalArgumentException("大地线最短距离当前仅支持两点");
        }
        try {
            Geometry projected1;
            Geometry projected2;
            if (method == MeasureMethodEnum.WEB_MERCATOR) {
                projected1 = convertToProjectedCRS(geometry1, srid);
                projected2 = convertToProjectedCRS(geometry2, srid);
            } else if (method == MeasureMethodEnum.UTM) {
                GeometryCollection sourceGeometries =
                        advToolsConfig
                                .getGeometryFactory()
                                .createGeometryCollection(new Geometry[] {geometry1, geometry2});
                GeometryCollection utmGeometries =
                        (GeometryCollection) convertToUTMCRS(sourceGeometries, srid);
                projected1 = utmGeometries.getGeometryN(0);
                projected2 = utmGeometries.getGeometryN(1);
            } else {
                throw new IllegalArgumentException("不支持的测量方式：" + method);
            }
            return convertFromBaseUnit(projected1.distance(projected2), unit);
        } catch (Exception e) {
            throw new RuntimeException("几何对象最短距离计算失败", e);
        }
    }

    @Override
    public double convertUnit(double value, MeasureUnitEnum srcUnit, MeasureUnitEnum targetUnit) {
        if (srcUnit == null || targetUnit == null) {
            throw new IllegalArgumentException("原始单位和目标单位不能为空");
        }
        if (srcUnit.getDimension() != targetUnit.getDimension()) {
            throw new IllegalArgumentException("不能在不同量纲之间转换：" + srcUnit + " -> " + targetUnit);
        }
        return value * srcUnit.getToBaseFactor() / targetUnit.getToBaseFactor();
    }

    /** 将以米或平方米表示的基准值换算为指定枚举单位。 */
    private double convertFromBaseUnit(double value, MeasureUnitEnum unit) {
        return value / unit.getToBaseFactor();
    }

    /** 校验单位量纲与测量方式。 */
    private void validateMeasureArguments(
            MeasureUnitEnum unit,
            MeasureMethodEnum method,
            MeasureUnitEnum.MeasureDimension expectedDimension) {
        if (unit == null || method == null) {
            throw new IllegalArgumentException("测量单位和测量方式不能为空");
        }
        if (unit.getDimension() != expectedDimension) {
            throw new IllegalArgumentException("测量结果需要 " + expectedDimension + " 单位，当前为：" + unit);
        }
    }

    /** 计算 WGS84 坐标序列的椭球大地线长度。 */
    private double calculateGeodeticLength(Geometry geometry) {
        if (geometry instanceof Polygon) {
            Polygon polygon = (Polygon) geometry;
            double length = calculateGeodeticLength(polygon.getExteriorRing());
            for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                length += calculateGeodeticLength(polygon.getInteriorRingN(i));
            }
            return length;
        }
        if (geometry instanceof LineString) {
            Coordinate[] coordinates = geometry.getCoordinates();
            double length = 0.0D;
            for (int i = 1; i < coordinates.length; i++) {
                length += calculateGeodeticSegmentLength(coordinates[i - 1], coordinates[i]);
            }
            return length;
        }
        if (geometry instanceof GeometryCollection) {
            double length = 0.0D;
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                length += calculateGeodeticLength(geometry.getGeometryN(i));
            }
            return length;
        }
        throw new IllegalArgumentException("大地线长度不支持的几何类型：" + geometry.getGeometryType());
    }

    /** 使用 GeoTools 椭球模型计算相邻两点的大地线距离。 */
    private double calculateGeodeticSegmentLength(Coordinate start, Coordinate end) {
        GeodeticCalculator calculator = new GeodeticCalculator();
        calculator.setStartingGeographicPoint(start.x, start.y);
        calculator.setDestinationGeographicPoint(end.x, end.y);
        return calculator.getOrthodromicDistance();
    }

    // ====================== 新增私有工具方法（UTM投影转换） ======================

    /** 转换为WGS84地理坐标系（4326） */
    private Geometry convertToWGS84(Geometry geometry, int srcSrid) {
        if (srcSrid == 4326) {
            return geometry;
        }
        return sridConvert.convert(geometry, srcSrid, 4326);
    }

    /** 转换为几何中心所在的 UTM 投影坐标系，用于局部平面测量。 */
    private Geometry convertToUTMCRS(Geometry geometry, int srcSrid) {
        // 先转换为WGS84
        Geometry wgs84Geom = convertToWGS84(geometry, srcSrid);
        Envelope envelope = wgs84Geom.getEnvelopeInternal();
        // 计算UTM带号和对应的SRID
        int utmZone = getUTMZone(wgs84Geom, 4326);
        double centerLat = (envelope.getMinY() + envelope.getMaxY()) / 2;
        int utmSrid = getUTMSRID(utmZone, centerLat);
        // 转换为UTM投影
        return sridConvert.convert(wgs84Geom, 4326, utmSrid);
    }

    /**
     * 转换为 EPSG:3857，用于地图展示和快速测量。
     *
     * <p>Web Mercator 并非等面积或等距投影；该转换仅承载历史默认行为。
     */
    private Geometry convertToProjectedCRS(Geometry geometry, int srcSrid) {
        if (srcSrid == 3857) {
            return geometry;
        }
        return sridConvert.convert(geometry, srcSrid, 3857);
    }

    /** 校验几何对象类型 */
    private void validateGeometryType(Geometry geometry, String[] allowedTypes) {
        if (ObjectUtil.isNull(geometry)) {
            throw new IllegalArgumentException("几何对象不能为空");
        }
        String geomType = geometry.getGeometryType();
        boolean isAllowed = Arrays.asList(allowedTypes).contains(geomType);
        if (!isAllowed) {
            throw new IllegalArgumentException(
                    "不支持的几何类型：" + geomType + "，允许类型：" + Arrays.toString(allowedTypes));
        }
    }
}
