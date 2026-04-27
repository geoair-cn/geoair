package cn.geoair.map.dynamic.tools.coordinate;

import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.function.BiFunction;

import org.locationtech.jts.geom.*;

/**
 * 坐标转换工具类（单例模式） 实现WGS84/GCJ02/BD09坐标系互转、墨卡托投影转换、坐标格式标准化等能力 核心算法参考：高德/百度地图坐标系转换规范
 *
 * @author 张逢吉
 * @date 2024/12/05
 */
public class GirCoordinateUtils implements GirCoordinateConvertOpt {

    // 单例实例
    private static volatile GirCoordinateUtils INSTANCE;

    // 坐标系转换常量
    private static final double PI = 3.1415926535897932384626;

    private static final double A = 6378245.0; // 长半轴

    private static final double EE = 0.00669342162296594323; // 偏心率平方

    private static final double X_PI = PI * 3000.0 / 180.0;


    ToolsConfig advToolsConfig;

    public GirCoordinateUtils(ToolsConfig advToolsConfig) {
        this.advToolsConfig = advToolsConfig;
    }

    public static GirCoordinateUtils getInstance(ToolsConfig advToolsConfig) {
        return new GirCoordinateUtils(advToolsConfig);
    }

    /**
     * 获取单例实例（双重校验锁）
     *
     * @return 单例对象
     */
    @Deprecated
    public static GirCoordinateUtils getInstance() {
        if (INSTANCE == null) {
            synchronized (GirCoordinateUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GirCoordinateUtils(new ToolsConfig());
                }
            }
        }
        return INSTANCE;
    }

    // ====================== 基础单坐标转换（原方法保留） ======================
    @Override
    public double[] wgs84ToGcj02(double lng, double lat) {
        return wgs84ToGcj02(lng, lat, false);
    }

    @Override
    public double[] wgs84ToGcj02(double lng, double lat, boolean ifExceptionReturnNull) {
        try {
            // 校验坐标是否在国内（不在则不偏移）
            if (!isChinaCoord(lng, lat)) {
                return new double[]{lng, lat};
            }

            double dLat = transformLat(lng - 105.0, lat - 35.0);
            double dLng = transformLng(lng - 105.0, lat - 35.0);
            double radLat = lat / 180.0 * PI;
            double magic = Math.sin(radLat);
            magic = 1 - EE * magic * magic;
            double sqrtMagic = Math.sqrt(magic);
            dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
            dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
            double mgLat = lat + dLat;
            double mgLng = lng + dLng;
            return new double[]{mgLng, mgLat};
        } catch (Exception e) {
            return handleException(e, ifExceptionReturnNull);
        }
    }

    @Override
    public double[] gcj02ToWgs84(double lng, double lat) {
        return gcj02ToWgs84(lng, lat, false);
    }

    /**
     * GCJ02转WGS84（纠偏）
     */
    public double[] gcj02ToWgs84(double lng, double lat, boolean ifExceptionReturnNull) {
        try {
            double[] gcj = wgs84ToGcj02(lng, lat);
            double dLng = gcj[0] - lng;
            double dLat = gcj[1] - lat;
            return new double[]{lng - dLng, lat - dLat};
        } catch (Exception e) {
            return handleException(e, ifExceptionReturnNull);
        }
    }

    @Override
    public double[] gcj02ToBd09(double lng, double lat) {
        return gcj02ToBd09(lng, lat, false);
    }

    /**
     * GCJ02转BD09
     */
    public double[] gcj02ToBd09(double lng, double lat, boolean ifExceptionReturnNull) {
        try {
            double x = lng, y = lat;
            double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * X_PI);
            double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * X_PI);
            double bdLng = z * Math.cos(theta) + 0.0065;
            double bdLat = z * Math.sin(theta) + 0.006;
            return new double[]{bdLng, bdLat};
        } catch (Exception e) {
            return handleException(e, ifExceptionReturnNull);
        }
    }

    @Override
    public double[] bd09ToGcj02(double lng, double lat) {
        return bd09ToGcj02(lng, lat, false);
    }

    /**
     * BD09转GCJ02
     */
    public double[] bd09ToGcj02(double lng, double lat, boolean ifExceptionReturnNull) {
        try {
            double x = lng - 0.0065, y = lat - 0.006;
            double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * X_PI);
            double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * X_PI);
            double gcjLng = z * Math.cos(theta);
            double gcjLat = z * Math.sin(theta);
            return new double[]{gcjLng, gcjLat};
        } catch (Exception e) {
            return handleException(e, ifExceptionReturnNull);
        }
    }

    @Override
    public double[] wgs84ToBd09(double lng, double lat) {
        return wgs84ToBd09(lng, lat, false);
    }

    /**
     * WGS84转BD09（先转GCJ02，再转BD09）
     */
    public double[] wgs84ToBd09(double lng, double lat, boolean ifExceptionReturnNull) {
        try {
            double[] gcj = wgs84ToGcj02(lng, lat);
            return gcj02ToBd09(gcj[0], gcj[1]);
        } catch (Exception e) {
            return handleException(e, ifExceptionReturnNull);
        }
    }

    @Override
    public double[] bd09ToWgs84(double lng, double lat) {
        return bd09ToWgs84(lng, lat, false);
    }

    /**
     * BD09转WGS84（先转GCJ02，再转WGS84）
     */
    public double[] bd09ToWgs84(double lng, double lat, boolean ifExceptionReturnNull) {
        try {
            double[] gcj = bd09ToGcj02(lng, lat);
            return gcj02ToWgs84(gcj[0], gcj[1]);
        } catch (Exception e) {
            return handleException(e, ifExceptionReturnNull);
        }
    }

    @Override
    public double[] mercatorToWgs84(double mercatorX, double mercatorY) {
        return mercatorToWgs84(mercatorX, mercatorY, false);
    }

    /**
     * 墨卡托转WGS84
     */
    public double[] mercatorToWgs84(
            double mercatorX, double mercatorY, boolean ifExceptionReturnNull) {
        try {
            double lng = mercatorX / 20037508.34 * 180.0;
            double lat = mercatorY / 20037508.34 * 180.0;
            lat = 180.0 / PI * (2 * Math.atan(Math.exp(lat * PI / 180.0)) - PI / 2);
            return new double[]{lng, lat};
        } catch (Exception e) {
            return handleException(e, ifExceptionReturnNull);
        }
    }

    @Override
    public double[] wgs84ToMercator(double lng, double lat) {
        return wgs84ToMercator(lng, lat, false);
    }

    /**
     * WGS84转墨卡托
     */
    public double[] wgs84ToMercator(double lng, double lat, boolean ifExceptionReturnNull) {
        try {
            double x = lng * 20037508.34 / 180.0;
            double y = Math.log(Math.tan((90 + lat) * PI / 360.0)) / (PI / 180.0);
            y = y * 20037508.34 / 180.0;
            return new double[]{x, y};
        } catch (Exception e) {
            return handleException(e, ifExceptionReturnNull);
        }
    }

    @Override
    public double[] dmsToDd(String dmsStr) {
        return dmsToDd(dmsStr, false);
    }

    /**
     * 度分秒转十进制度 支持格式：116°23′45.6″E, 39°54′32.1″N 或 116°23'45.6"E 39°54'32.1"N
     */
    public double[] dmsToDd(String dmsStr, boolean ifExceptionReturnNull) {
        try {
            if (StrUtil.isBlank(dmsStr)) {
                throw new IllegalArgumentException("度分秒字符串不能为空");
            }

            // 清理特殊字符，统一分隔符
            String cleanStr =
                    StrUtil.replace(dmsStr, "′", "'").replace("″", "\"").replace("°", "d");
            String[] coordParts =
                    StrUtil.split(cleanStr, StrUtil.COMMA + StrUtil.SPACE).toArray(new String[0]);
            if (coordParts.length != 2) {
                throw new IllegalArgumentException("度分秒字符串格式错误，示例：116°23′45.6″E, 39°54′32.1″N");
            }

            double lng = dmsPartToDd(coordParts[0]);
            double lat = dmsPartToDd(coordParts[1]);
            return new double[]{lng, lat};
        } catch (Exception e) {
            return handleException(e, ifExceptionReturnNull);
        }
    }

    @Override
    public String ddToDms(double lng, double lat) {
        return ddToDms(lng, lat, false);
    }

    /**
     * 十进制度转度分秒
     */
    public String ddToDms(double lng, double lat, boolean ifExceptionReturnNull) {
        try {
            String lngDms = ddPartToDms(lng, true);
            String latDms = ddPartToDms(lat, false);
            return String.format("%s, %s", lngDms, latDms);
        } catch (Exception e) {
            return ifExceptionReturnNull ? null : throwRuntimeException(e);
        }
    }

    @Override
    public double[] parseCoordString(String coordStr, String separator) {
        return parseCoordString(coordStr, separator, false);
    }

    /**
     * 解析坐标字符串为十进制度
     */
    public double[] parseCoordString(
            String coordStr, String separator, boolean ifExceptionReturnNull) {
        try {
            if (StrUtil.isBlank(coordStr) || StrUtil.isBlank(separator)) {
                throw new IllegalArgumentException("坐标字符串和分隔符不能为空");
            }

            String[] parts = StrUtil.split(coordStr, separator).toArray(new String[0]);
            if (parts.length != 2) {
                throw new IllegalArgumentException("坐标字符串格式错误，示例：116.40,39.90");
            }

            // 先尝试直接解析为十进制度
            if (NumberUtil.isNumber(parts[0]) && NumberUtil.isNumber(parts[1])) {
                return new double[]{
                        NumberUtil.parseDouble(parts[0]), NumberUtil.parseDouble(parts[1])
                };
            }

            // 解析度分秒格式
            return dmsToDd(coordStr, ifExceptionReturnNull);
        } catch (Exception e) {
            return handleException(e, ifExceptionReturnNull);
        }
    }

    // ====================== 新增：JTS Point对象单转换实现 ======================
    @Override
    public Point wgs84ToGcj02(Point point) {
        return wgs84ToGcj02(point, false);
    }

    public Point wgs84ToGcj02(Point point, boolean ifExceptionReturnNull) {
        try {
            validatePoint(point);
            double[] gcj = wgs84ToGcj02(point.getX(), point.getY());
            return advToolsConfig.getGeometryFactory().createPoint(new Coordinate(gcj[0], gcj[1]));
        } catch (Exception e) {
            return ifExceptionReturnNull ? null : throwRuntimeException(e);
        }
    }

    @Override
    public Point gcj02ToWgs84(Point point) {
        return gcj02ToWgs84(point, false);
    }

    public Point gcj02ToWgs84(Point point, boolean ifExceptionReturnNull) {
        try {
            validatePoint(point);
            double[] wgs = gcj02ToWgs84(point.getX(), point.getY());
            return advToolsConfig.getGeometryFactory().createPoint(new Coordinate(wgs[0], wgs[1]));
        } catch (Exception e) {
            return ifExceptionReturnNull ? null : throwRuntimeException(e);
        }
    }

    @Override
    public Point gcj02ToBd09(Point point) {
        return gcj02ToBd09(point, false);
    }

    public Point gcj02ToBd09(Point point, boolean ifExceptionReturnNull) {
        try {
            validatePoint(point);
            double[] bd = gcj02ToBd09(point.getX(), point.getY());
            return advToolsConfig.getGeometryFactory().createPoint(new Coordinate(bd[0], bd[1]));
        } catch (Exception e) {
            return ifExceptionReturnNull ? null : throwRuntimeException(e);
        }
    }

    @Override
    public Point bd09ToGcj02(Point point) {
        return bd09ToGcj02(point, false);
    }

    public Point bd09ToGcj02(Point point, boolean ifExceptionReturnNull) {
        try {
            validatePoint(point);
            double[] gcj = bd09ToGcj02(point.getX(), point.getY());
            return advToolsConfig.getGeometryFactory().createPoint(new Coordinate(gcj[0], gcj[1]));
        } catch (Exception e) {
            return ifExceptionReturnNull ? null : throwRuntimeException(e);
        }
    }

    @Override
    public Point wgs84ToBd09(Point point) {
        return wgs84ToBd09(point, false);
    }

    public Point wgs84ToBd09(Point point, boolean ifExceptionReturnNull) {
        try {
            validatePoint(point);
            double[] bd = wgs84ToBd09(point.getX(), point.getY());
            return advToolsConfig.getGeometryFactory().createPoint(new Coordinate(bd[0], bd[1]));
        } catch (Exception e) {
            return ifExceptionReturnNull ? null : throwRuntimeException(e);
        }
    }

    @Override
    public Point bd09ToWgs84(Point point) {
        return bd09ToWgs84(point, false);
    }

    public Point bd09ToWgs84(Point point, boolean ifExceptionReturnNull) {
        try {
            validatePoint(point);
            double[] wgs = bd09ToWgs84(point.getX(), point.getY());
            return advToolsConfig.getGeometryFactory().createPoint(new Coordinate(wgs[0], wgs[1]));
        } catch (Exception e) {
            return ifExceptionReturnNull ? null : throwRuntimeException(e);
        }
    }

    @Override
    public Point mercatorToWgs84(Point point) {
        return mercatorToWgs84(point, false);
    }

    public Point mercatorToWgs84(Point point, boolean ifExceptionReturnNull) {
        try {
            validatePoint(point);
            double[] wgs = mercatorToWgs84(point.getX(), point.getY());
            return advToolsConfig.getGeometryFactory().createPoint(new Coordinate(wgs[0], wgs[1]));
        } catch (Exception e) {
            return ifExceptionReturnNull ? null : throwRuntimeException(e);
        }
    }

    @Override
    public Point wgs84ToMercator(Point point) {
        return wgs84ToMercator(point, false);
    }

    public Point wgs84ToMercator(Point point, boolean ifExceptionReturnNull) {
        try {
            validatePoint(point);
            double[] mercator = wgs84ToMercator(point.getX(), point.getY());
            return advToolsConfig.getGeometryFactory().createPoint(new Coordinate(mercator[0], mercator[1]));
        } catch (Exception e) {
            return ifExceptionReturnNull ? null : throwRuntimeException(e);
        }
    }

    // ====================== 新增：批量坐标转换实现 ======================
    @Override
    public double[][] wgs84ToGcj02Batch(double[][] coords, boolean ifExceptionReturnNull) {
        return batchConvert(coords, ifExceptionReturnNull, this::wgs84ToGcj02);
    }

    @Override
    public double[][] gcj02ToWgs84Batch(double[][] coords, boolean ifExceptionReturnNull) {
        return batchConvert(coords, ifExceptionReturnNull, this::gcj02ToWgs84);
    }

    @Override
    public double[][] gcj02ToBd09Batch(double[][] coords, boolean ifExceptionReturnNull) {
        return batchConvert(coords, ifExceptionReturnNull, this::gcj02ToBd09);
    }

    @Override
    public double[][] bd09ToWgs84Batch(double[][] coords, boolean ifExceptionReturnNull) {
        return batchConvert(coords, ifExceptionReturnNull, this::bd09ToWgs84);
    }

    @Override
    public double[][] wgs84ToMercatorBatch(double[][] coords, boolean ifExceptionReturnNull) {
        return batchConvert(coords, ifExceptionReturnNull, this::wgs84ToMercator);
    }

    @Override
    public double[][] mercatorToWgs84Batch(double[][] coords, boolean ifExceptionReturnNull) {
        return batchConvert(coords, ifExceptionReturnNull, this::mercatorToWgs84);
    }

    // ====================== 新增：点线面Geometry整体转换实现 ======================
    @Override
    public Geometry wgs84ToGcj02Geometry(Geometry geometry) {
        return wgs84ToGcj02Geometry(geometry, false);
    }

    @Override
    public Geometry wgs84ToGcj02Geometry(Geometry geometry, boolean ifExceptionReturnNull) {
        return convertGeometry(geometry, ifExceptionReturnNull, this::wgs84ToGcj02);
    }

    @Override
    public Geometry gcj02ToWgs84Geometry(Geometry geometry) {
        return gcj02ToWgs84Geometry(geometry, false);
    }

    @Override
    public Geometry gcj02ToWgs84Geometry(Geometry geometry, boolean ifExceptionReturnNull) {
        return convertGeometry(geometry, ifExceptionReturnNull, this::gcj02ToWgs84);
    }

    @Override
    public Geometry gcj02ToBd09Geometry(Geometry geometry) {
        return convertGeometry(geometry, false, this::gcj02ToBd09);
    }

    @Override
    public Geometry bd09ToWgs84Geometry(Geometry geometry) {
        return convertGeometry(geometry, false, this::bd09ToWgs84);
    }

    @Override
    public Geometry wgs84ToMercatorGeometry(Geometry geometry) {
        return convertGeometry(geometry, false, this::wgs84ToMercator);
    }

    @Override
    public Geometry mercatorToWgs84Geometry(Geometry geometry) {
        return convertGeometry(geometry, false, this::mercatorToWgs84);
    }

    // ====================== 私有工具方法 ======================

    /**
     * 校验坐标是否在中国境内（用于判断是否需要偏移）
     */
    private boolean isChinaCoord(double lng, double lat) {
        return lng >= 73.66 && lng <= 135.05 && lat >= 3.86 && lat <= 53.55;
    }

    /**
     * 纬度偏移计算
     */
    private double transformLat(double x, double y) {
        double ret =
                -100.0
                        + 2.0 * x
                        + 3.0 * y
                        + 0.2 * y * y
                        + 0.1 * x * y
                        + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 经度偏移计算
     */
    private double transformLng(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 单部分度分秒转十进制度
     */
    private double dmsPartToDd(String dmsPart) {
        // 提取方向（E/W/N/S）
        char dir = dmsPart.charAt(dmsPart.length() - 1);
        String numPart = StrUtil.removeSuffix(dmsPart, String.valueOf(dir)).trim();

        // 拆分度、分、秒
        String[] parts = StrUtil.split(numPart, "d'\"").toArray(new String[0]);
        double degree = NumberUtil.parseDouble(parts[0]);
        double minute = parts.length > 1 ? NumberUtil.parseDouble(parts[1]) : 0;
        double second = parts.length > 2 ? NumberUtil.parseDouble(parts[2]) : 0;

        double dd = degree + minute / 60 + second / 3600;
        // 西经/南纬取负数
        if (dir == 'W' || dir == 'S') {
            dd = -dd;
        }
        return dd;
    }

    /**
     * 十进制度转单部分度分秒
     */
    private String ddPartToDms(double dd, boolean isLongitude) {
        // 取绝对值计算度分秒
        double absDd = Math.abs(dd);
        int degree = (int) absDd;
        double remain = (absDd - degree) * 60;
        int minute = (int) remain;
        double second = (remain - minute) * 60;

        // 确定方向
        char dir;
        if (isLongitude) {
            dir = dd >= 0 ? 'E' : 'W';
        } else {
            dir = dd >= 0 ? 'N' : 'S';
        }

        return String.format("%d°%d′%.1f″%c", degree, minute, second, dir);
    }

    /**
     * 异常处理通用方法（数组返回）
     */
    private double[] handleException(Exception e, boolean ifExceptionReturnNull) {
        if (ifExceptionReturnNull) {
            return null;
        } else {
            throw new RuntimeException("坐标转换失败", e);
        }
    }

    /**
     * 抛出运行时异常
     */
    private <T> T throwRuntimeException(Exception e) {
        throw new RuntimeException("坐标转换失败", e);
    }

    /**
     * 校验Point对象有效性
     */
    private void validatePoint(Point point) {
        if (point == null || point.isEmpty()) {
            throw new IllegalArgumentException("Point对象不能为空或空几何");
        }
    }

    /**
     * 批量坐标转换通用方法
     *
     * @param coords                原始坐标二维数组
     * @param ifExceptionReturnNull 异常返回null
     * @param converter             转换函数
     * @return 转换后坐标二维数组
     */
    private double[][] batchConvert(
            double[][] coords,
            boolean ifExceptionReturnNull,
            BiFunction<Double, Double, double[]> converter) {
        try {
            if (coords == null || coords.length == 0) {
                throw new IllegalArgumentException("批量转换坐标数组不能为空");
            }
            double[][] result = new double[coords.length][2];
            for (int i = 0; i < coords.length; i++) {
                double[] coord = coords[i];
                if (coord == null || coord.length != 2) {
                    throw new IllegalArgumentException("第" + i + "个坐标格式错误，需为[lng,lat]");
                }
                result[i] = converter.apply(coord[0], coord[1]);
            }
            return result;
        } catch (Exception e) {
            if (ifExceptionReturnNull) {
                return null;
            } else {
                throw new RuntimeException("批量坐标转换失败", e);
            }
        }
    }

    /**
     * Geometry对象转换通用方法（支持Point/LineString/Polygon/Multi几何）
     *
     * @param geometry              原始几何对象
     * @param ifExceptionReturnNull 异常返回null
     * @param converter             坐标转换函数
     * @return 转换后几何对象
     */
    private Geometry convertGeometry(
            Geometry geometry,
            boolean ifExceptionReturnNull,
            BiFunction<Double, Double, double[]> converter) {
        try {
            if (geometry == null || geometry.isEmpty()) {
                throw new IllegalArgumentException("Geometry对象不能为空或空几何");
            }

            // 复制并转换所有坐标
            Coordinate[] coords = geometry.getCoordinates();
            Coordinate[] newCoords = new Coordinate[coords.length];
            for (int i = 0; i < coords.length; i++) {
                double[] newCoord = converter.apply(coords[i].x, coords[i].y);
                newCoords[i] = new Coordinate(newCoord[0], newCoord[1]);
            }

            // 根据原始几何类型创建新对象
            if (geometry instanceof Point) {
                return advToolsConfig.getGeometryFactory().createPoint(newCoords[0]);
            } else if (geometry instanceof LineString) {
                return advToolsConfig.getGeometryFactory().createLineString(newCoords);
            } else if (geometry instanceof Polygon) {
                Polygon polygon = (Polygon) geometry;
                LinearRing shell = advToolsConfig.getGeometryFactory().createLinearRing(newCoords);
                LinearRing[] holes = new LinearRing[polygon.getNumInteriorRing()];
                for (int i = 0; i < holes.length; i++) {
                    Coordinate[] holeCoords = polygon.getInteriorRingN(i).getCoordinates();
                    Coordinate[] newHoleCoords =
                            Arrays.stream(holeCoords)
                                    .map(
                                            c -> {
                                                double[] nc = converter.apply(c.x, c.y);
                                                return new Coordinate(nc[0], nc[1]);
                                            })
                                    .toArray(Coordinate[]::new);
                    holes[i] = advToolsConfig.getGeometryFactory().createLinearRing(newHoleCoords);
                }
                return advToolsConfig.getGeometryFactory().createPolygon(shell, holes);
            } else if (geometry instanceof MultiPoint) {
                return advToolsConfig.getGeometryFactory().createMultiPointFromCoords(newCoords);
            } else if (geometry instanceof MultiLineString) {
                return advToolsConfig.getGeometryFactory().createMultiLineString(
                        new LineString[]{advToolsConfig.getGeometryFactory().createLineString(newCoords)});
            } else if (geometry instanceof MultiPolygon) {
                throw new UnsupportedOperationException("暂不支持MultiPolygon直接转换，请拆分为单个Polygon转换");
            } else {
                throw new UnsupportedOperationException("不支持的几何类型：" + geometry.getGeometryType());
            }
        } catch (Exception e) {
            if (ifExceptionReturnNull) {
                return null;
            } else {
                throw new RuntimeException("Geometry对象坐标转换失败", e);
            }
        }
    }
}
