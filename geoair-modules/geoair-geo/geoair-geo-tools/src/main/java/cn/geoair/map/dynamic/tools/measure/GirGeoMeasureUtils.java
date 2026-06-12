package cn.geoair.map.dynamic.tools.measure;

import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.geoair.map.dynamic.tools.convert.GirFormatUtils;
import cn.geoair.map.dynamic.tools.srid.GirSridConvertUtils;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.distance.DistanceOp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 空间几何面积/长度/距离计算工具类（单例模式） 基于JTS+GeoTools实现，支持多输入格式、多单位转换
 *
 * @author 张逢吉
 * @date 2024/12/05
 */
public class GirGeoMeasureUtils implements GirGeoMeasureOpt {
    // 单例实例
    private static volatile GirGeoMeasureUtils INSTANCE;
    ToolsConfig advToolsConfig;
    private GirFormatUtils formatOpt = GirFormatUtils.getInstance();
    private GirSridConvertUtils sridConvert = GirSridConvertUtils.getInstance();

    public GirGeoMeasureUtils(ToolsConfig advToolsConfig) {
        this.advToolsConfig = advToolsConfig;
        initUnitFactors();
        this.formatOpt = GirFormatUtils.getInstance(advToolsConfig);
        this.sridConvert = GirSridConvertUtils.getInstance(advToolsConfig);
    }

    public static GirGeoMeasureUtils getInstance(ToolsConfig advToolsConfig) {
        return new GirGeoMeasureUtils(advToolsConfig);
    }

    // SRID转换工具

    // 单位转换系数缓存
    private final Map<String, Double> unitFactorCache = new HashMap<>();

    // 锁
    private final Lock cacheLock = new ReentrantLock();

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

    // ====================== 新增UTM版面积计算实现 ======================
    @Override
    public double calculateAreaByUTM(Geometry geometry, int srid, String unit) {
        try {
            validateGeometryType(geometry, new String[] {"Polygon", "MultiPolygon"});
            // 转换为UTM投影坐标系计算高精度面积
            Geometry utmGeom = convertToUTMCRS(geometry, srid);
            double areaInM2 = utmGeom.getArea();
            // 单位转换
            return convertUnit(areaInM2, UNIT_SQUARE_METER, unit, srid);
        } catch (Exception e) {
            throw new RuntimeException("UTM投影面积计算失败", e);
        }
    }

    @Override
    public double calculateAreaByUTM(double[][] coords, int srid, String unit) {
        try {
            // 构建多边形几何对象
            Coordinate[] coordinates =
                    Arrays.stream(coords)
                            .map(coord -> new Coordinate(coord[0], coord[1]))
                            .toArray(Coordinate[]::new);
            // 确保多边形闭合
            if (!coordinates[0].equals(coordinates[coordinates.length - 1])) {
                coordinates = Arrays.copyOf(coordinates, coordinates.length + 1);
                coordinates[coordinates.length - 1] = coordinates[0];
            }
            LinearRing ring = advToolsConfig.getGeometryFactory().createLinearRing(coordinates);
            Polygon polygon = advToolsConfig.getGeometryFactory().createPolygon(ring);
            return calculateAreaByUTM(polygon, srid, unit);
        } catch (Exception e) {
            throw new RuntimeException("坐标数组转多边形UTM面积计算失败", e);
        }
    }

    @Override
    public double calculateAreaByUTM(String wkt, int srid, String unit) {
        try {
            Geometry geometry = formatOpt.wktToJtsGeometry(wkt, true);
            return calculateAreaByUTM(geometry, srid, unit);
        } catch (Exception e) {
            throw new RuntimeException("WKT解析UTM面积计算失败", e);
        }
    }

    // ====================== 新增UTM版长度计算实现 ======================
    @Override
    public double calculateLengthByUTM(Geometry geometry, int srid, String unit) {
        try {
            validateGeometryType(
                    geometry,
                    new String[] {"LineString", "MultiLineString", "Polygon", "MultiPolygon"});
            // 转换为UTM投影坐标系计算高精度长度
            Geometry utmGeom = convertToUTMCRS(geometry, srid);
            double lengthInM;
            if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
                lengthInM = utmGeom.getLength(); // 多边形计算周长
            } else {
                lengthInM = utmGeom.getLength(); // 线计算长度
            }
            // 单位转换
            return convertUnit(lengthInM, UNIT_METER, unit, srid);
        } catch (Exception e) {
            throw new RuntimeException("UTM投影长度计算失败", e);
        }
    }

    @Override
    public double calculateLengthByUTM(double[][] coords, int srid, String unit) {
        try {
            // 构建线几何对象
            Coordinate[] coordinates =
                    Arrays.stream(coords)
                            .map(coord -> new Coordinate(coord[0], coord[1]))
                            .toArray(Coordinate[]::new);
            LineString lineString =
                    advToolsConfig.getGeometryFactory().createLineString(coordinates);
            return calculateLengthByUTM(lineString, srid, unit);
        } catch (Exception e) {
            throw new RuntimeException("坐标数组转线UTM长度计算失败", e);
        }
    }

    @Override
    public double calculateLengthByUTM(String wkt, int srid, String unit) {
        try {
            Geometry geometry = formatOpt.wktToJtsGeometry(wkt, true);
            return calculateLengthByUTM(geometry, srid, unit);
        } catch (Exception e) {
            throw new RuntimeException("WKT解析UTM长度计算失败", e);
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
    public double calculateArea(Geometry geometry, int srid, String unit) {
        try {
            validateGeometryType(geometry, new String[] {"Polygon", "MultiPolygon"});
            // 转换为投影坐标系（3857）计算精确面积（地理坐标系需球面计算）
            Geometry projectedGeom = convertToProjectedCRS(geometry, srid);
            double areaInM2 = projectedGeom.getArea();
            // 单位转换
            return convertUnit(areaInM2, UNIT_SQUARE_METER, unit, srid);
        } catch (Exception e) {
            throw new RuntimeException("面积计算失败", e);
        }
    }

    @Override
    public double calculateArea(double[][] coords, int srid, String unit) {
        try {
            // 构建多边形几何对象
            Coordinate[] coordinates =
                    Arrays.stream(coords)
                            .map(coord -> new Coordinate(coord[0], coord[1]))
                            .toArray(Coordinate[]::new);
            // 确保多边形闭合
            if (!coordinates[0].equals(coordinates[coordinates.length - 1])) {
                coordinates = Arrays.copyOf(coordinates, coordinates.length + 1);
                coordinates[coordinates.length - 1] = coordinates[0];
            }
            LinearRing ring = advToolsConfig.getGeometryFactory().createLinearRing(coordinates);
            Polygon polygon = advToolsConfig.getGeometryFactory().createPolygon(ring);
            return calculateArea(polygon, srid, unit);
        } catch (Exception e) {
            throw new RuntimeException("坐标数组转多边形计算面积失败", e);
        }
    }

    @Override
    public double calculateArea(String wkt, int srid, String unit) {
        try {
            Geometry geometry = formatOpt.wktToJtsGeometry(wkt, true);
            return calculateArea(geometry, srid, unit);
        } catch (Exception e) {
            throw new RuntimeException("WKT解析计算面积失败", e);
        }
    }

    @Override
    public double calculateLength(Geometry geometry, int srid, String unit) {
        try {
            validateGeometryType(
                    geometry,
                    new String[] {"LineString", "MultiLineString", "Polygon", "MultiPolygon"});
            // 转换为投影坐标系计算长度
            Geometry projectedGeom = convertToProjectedCRS(geometry, srid);
            double lengthInM;
            if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
                lengthInM = projectedGeom.getLength(); // 多边形计算周长
            } else {
                lengthInM = projectedGeom.getLength(); // 线计算长度
            }
            // 单位转换
            return convertUnit(lengthInM, UNIT_METER, unit, srid);
        } catch (Exception e) {
            throw new RuntimeException("长度计算失败", e);
        }
    }

    @Override
    public double calculateLength(double[][] coords, int srid, String unit) {
        try {
            // 构建线几何对象
            Coordinate[] coordinates =
                    Arrays.stream(coords)
                            .map(coord -> new Coordinate(coord[0], coord[1]))
                            .toArray(Coordinate[]::new);
            LineString lineString =
                    advToolsConfig.getGeometryFactory().createLineString(coordinates);
            return calculateLength(lineString, srid, unit);
        } catch (Exception e) {
            throw new RuntimeException("坐标数组转线计算长度失败", e);
        }
    }

    @Override
    public double calculateLength(String wkt, int srid, String unit) {
        try {
            Geometry geometry = formatOpt.wktToJtsGeometry(wkt, true);
            return calculateLength(geometry, srid, unit);
        } catch (Exception e) {
            throw new RuntimeException("WKT解析计算长度失败", e);
        }
    }

    @Override
    public double calculatePointToPointDistance(Point point1, Point point2, int srid, String unit) {
        try {
            validateGeometryType(point1, new String[] {"Point"});
            validateGeometryType(point2, new String[] {"Point"});

            // 地理坐标系（4326）使用大地测量计算球面距离
            if (srid == 4326) {
                GeodeticCalculator calculator = new GeodeticCalculator();
                calculator.setStartingGeographicPoint(point1.getX(), point1.getY());
                calculator.setDestinationGeographicPoint(point2.getX(), point2.getY());
                double distanceInM = calculator.getOrthodromicDistance();
                return convertUnit(distanceInM, UNIT_METER, unit, srid);
            } else {
                // 投影坐标系直接计算欧氏距离
                Point projectedP1 = (Point) convertToProjectedCRS(point1, srid);
                Point projectedP2 = (Point) convertToProjectedCRS(point2, srid);
                double distanceInM = projectedP1.distance(projectedP2);
                return convertUnit(distanceInM, UNIT_METER, unit, srid);
            }
        } catch (Exception e) {
            throw new RuntimeException("两点距离计算失败（Point对象入参）", e);
        }
    }

    @Override
    public double calculatePointToPointDistance(
            double[] point1, double[] point2, int srid, String unit) {
        try {
            Point p1 =
                    advToolsConfig
                            .getGeometryFactory()
                            .createPoint(new Coordinate(point1[0], point1[1]));
            Point p2 =
                    advToolsConfig
                            .getGeometryFactory()
                            .createPoint(new Coordinate(point2[0], point2[1]));
            // 复用Point对象入参的方法
            return calculatePointToPointDistance(p1, p2, srid, unit);
        } catch (Exception e) {
            throw new RuntimeException("两点距离计算失败（坐标数组入参）", e);
        }
    }

    @Override
    public double calculatePointToLineMinDistance(
            Point point, LineString line, int srid, String unit) {
        try {
            validateGeometryType(point, new String[] {"Point"});
            validateGeometryType(line, new String[] {"LineString"});

            // 转换到投影坐标系
            Point projectedPoint = (Point) convertToProjectedCRS(point, srid);
            LineString projectedLine = (LineString) convertToProjectedCRS(line, srid);

            double distanceInM = projectedPoint.distance(projectedLine);
            return convertUnit(distanceInM, UNIT_METER, unit, srid);
        } catch (Exception e) {
            throw new RuntimeException("点到线最近距离计算失败（Geometry对象入参）", e);
        }
    }

    @Override
    public double calculatePointToLineMinDistance(
            double[] point, double[][] lineCoords, int srid, String unit) {
        try {
            Point p =
                    advToolsConfig
                            .getGeometryFactory()
                            .createPoint(new Coordinate(point[0], point[1]));
            Coordinate[] lineCoordinates =
                    Arrays.stream(lineCoords)
                            .map(coord -> new Coordinate(coord[0], coord[1]))
                            .toArray(Coordinate[]::new);
            LineString line = advToolsConfig.getGeometryFactory().createLineString(lineCoordinates);
            // 复用Geometry对象入参的方法
            return calculatePointToLineMinDistance(p, line, srid, unit);
        } catch (Exception e) {
            throw new RuntimeException("点到线最近距离计算失败（坐标数组入参）", e);
        }
    }

    @Override
    public double calculatePointToGeometryDistance(
            Point point, Geometry geometry, int srid, String unit) {
        try {
            validateGeometryType(point, new String[] {"Point"});

            Point projectedPoint = (Point) convertToProjectedCRS(point, srid);
            Geometry projectedGeom = convertToProjectedCRS(geometry, srid);

            double distanceInM = projectedPoint.distance(projectedGeom);
            return convertUnit(distanceInM, UNIT_METER, unit, srid);
        } catch (Exception e) {
            throw new RuntimeException("点到几何对象距离计算失败", e);
        }
    }

    @Override
    public double calculateLineToLineMinDistance(
            LineString line1, LineString line2, int srid, String unit) {
        try {
            validateGeometryType(line1, new String[] {"LineString"});
            validateGeometryType(line2, new String[] {"LineString"});

            LineString projectedLine1 = (LineString) convertToProjectedCRS(line1, srid);
            LineString projectedLine2 = (LineString) convertToProjectedCRS(line2, srid);

            // 计算线到线最短距离
            DistanceOp distanceOp = new DistanceOp(projectedLine1, projectedLine2);
            double distanceInM = distanceOp.distance();
            return convertUnit(distanceInM, UNIT_METER, unit, srid);
        } catch (Exception e) {
            throw new RuntimeException("线到线最短距离计算失败（Geometry对象入参）", e);
        }
    }

    @Override
    public double calculateLineToLineMinDistance(
            double[][] line1Coords, double[][] line2Coords, int srid, String unit) {
        try {
            Coordinate[] line1CoordsArr =
                    Arrays.stream(line1Coords)
                            .map(coord -> new Coordinate(coord[0], coord[1]))
                            .toArray(Coordinate[]::new);
            LineString line1 = advToolsConfig.getGeometryFactory().createLineString(line1CoordsArr);

            Coordinate[] line2CoordsArr =
                    Arrays.stream(line2Coords)
                            .map(coord -> new Coordinate(coord[0], coord[1]))
                            .toArray(Coordinate[]::new);
            LineString line2 = advToolsConfig.getGeometryFactory().createLineString(line2CoordsArr);

            // 复用Geometry对象入参的方法
            return calculateLineToLineMinDistance(line1, line2, srid, unit);
        } catch (Exception e) {
            throw new RuntimeException("线到线最短距离计算失败（坐标数组入参）", e);
        }
    }

    @Override
    public double convertUnit(double value, String srcUnit, String targetUnit, int srid) {
        if (StrUtil.equals(srcUnit, targetUnit)) {
            return value;
        }

        // 先转换为米/平方米（基准单位）
        double valueInBaseUnit = convertToBaseUnit(value, srcUnit, srid);
        // 再转换为目标单位
        return convertFromBaseUnit(valueInBaseUnit, targetUnit);
    }

    // ====================== 新增私有工具方法（UTM投影转换） ======================

    /** 转换为WGS84地理坐标系（4326） */
    private Geometry convertToWGS84(Geometry geometry, int srcSrid) {
        if (srcSrid == 4326) {
            return geometry;
        }
        return sridConvert.convert(geometry, srcSrid, 4326);
    }

    /** 转换为UTM投影坐标系（高精度） */
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

    // ====================== 原有私有工具方法（保持不变） ======================

    /** 初始化单位转换系数 */
    private void initUnitFactors() {
        cacheLock.lock();
        try {
            // 长度单位（基准：米）
            unitFactorCache.put(UNIT_METER, 1.0);
            unitFactorCache.put(UNIT_KILOMETER, 1000.0);
            unitFactorCache.put(UNIT_DEGREE, 111319.9); // 1度≈111319.9米（赤道）
            unitFactorCache.put(UNIT_MILE, 1609.34); // 1英里≈1609.34米

            // 面积单位（基准：平方米）
            unitFactorCache.put(UNIT_SQUARE_METER, 1.0);
            unitFactorCache.put(UNIT_SQUARE_KILOMETER, 1000000.0);
            unitFactorCache.put(UNIT_ACRE, 666.6667); // 1亩≈666.6667平方米
            unitFactorCache.put(UNIT_HECTARE, 10000.0); // 1公顷=10000平方米
        } finally {
            cacheLock.unlock();
        }
    }

    /** 转换为基准单位（米/平方米） */
    private double convertToBaseUnit(double value, String unit, int srid) {
        if (isAreaUnit(unit)) {
            // 面积单位转换
            Double factor = unitFactorCache.get(unit);
            if (ObjectUtil.isNull(factor)) {
                throw new IllegalArgumentException("不支持的面积单位：" + unit);
            }
            return value * factor;
        } else {
            // 长度单位转换
            Double factor = unitFactorCache.get(unit);
            if (ObjectUtil.isNull(factor)) {
                throw new IllegalArgumentException("不支持的长度单位：" + unit);
            }
            return value * factor;
        }
    }

    /** 从基准单位转换为目标单位 */
    private double convertFromBaseUnit(double valueInBase, String unit) {
        if (isAreaUnit(unit)) {
            // 面积单位转换
            Double factor = unitFactorCache.get(unit);
            if (ObjectUtil.isNull(factor)) {
                throw new IllegalArgumentException("不支持的面积单位：" + unit);
            }
            return valueInBase / factor;
        } else {
            // 长度单位转换
            Double factor = unitFactorCache.get(unit);
            if (ObjectUtil.isNull(factor)) {
                throw new IllegalArgumentException("不支持的长度单位：" + unit);
            }
            return valueInBase / factor;
        }
    }

    /** 判断是否为面积单位 */
    private boolean isAreaUnit(String unit) {
        return StrUtil.equalsAny(
                unit, UNIT_SQUARE_METER, UNIT_SQUARE_KILOMETER, UNIT_ACRE, UNIT_HECTARE);
    }

    /** 转换为投影坐标系（3857）用于精确计算 */
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
