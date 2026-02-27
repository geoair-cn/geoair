package cn.geoair.map.dynamic.tools.convert;

import cn.geoair.map.dynamic.tools.convert.GirGeoFormatOpt;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.postgis.PGgeometry;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

/**
 * 空间类型转换工具类（单例模式）
 * 实现GeoConvertService接口，提供所有空间类型互转能力
 *
 * @author 张逢吉
 * @date 2024/10/24 15:29
 */
public class GirFormatUtils implements GirGeoFormatOpt {

    // 单例实例（volatile保证可见性，防止指令重排）
    private static volatile GirFormatUtils INSTANCE;

    // 几何工厂
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();




    // 私有构造器（防止外部实例化）
    private GirFormatUtils() {
    }

    /**
     * 获取单例实例（双重校验锁）
     *
     * @return 单例对象
     */
    public static GirFormatUtils getInstance() {
        if (INSTANCE == null) {
            synchronized (GirFormatUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GirFormatUtils();
                }
            }
        }
        return INSTANCE;
    }

    //==============================geojson转换功能==============================
    @Override
    public Geometry geojsonToJtsGeometry(String geojson, boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(geojson)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geojson");
        }
        try (Reader reader = new StringReader(geojson)) {
            return getGeometryJSON().read(reader);
        } catch (IOException e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public LineString geojsonToJtsLineString(String geojson, boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(geojson)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geojson");
        }
        try (Reader reader = new StringReader(geojson)) {
            return getGeometryJSON().readLine(reader);
        } catch (IOException e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public MultiLineString geojsonToJtsMultiLineString(String geojson, boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(geojson)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geojson");
        }
        try (Reader reader = new StringReader(geojson)) {
            return getGeometryJSON().readMultiLine(reader);
        } catch (IOException e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public Polygon geojsonToJtsPolygon(String geojson, boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(geojson)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geojson");
        }
        try (Reader reader = new StringReader(geojson)) {
            return getGeometryJSON().readPolygon(reader);
        } catch (IOException e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public MultiPolygon geojsonToJtsMultiPolygon(String geojson, boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(geojson)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geojson");
        }
        try (Reader reader = new StringReader(geojson)) {
            return getGeometryJSON().readMultiPolygon(reader);
        } catch (IOException e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public Point geojsonToJtsPoint(String geojson, boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(geojson)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geojson");
        }
        try (Reader reader = new StringReader(geojson)) {
            return getGeometryJSON().readPoint(reader);
        } catch (IOException e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public MultiPoint geojsonToJtsMultiPoint(String geojson, boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(geojson)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geojson");
        }
        try (Reader reader = new StringReader(geojson)) {
            return getGeometryJSON().readMultiPoint(reader);
        } catch (IOException e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public String geojsonToWktString(String geojson, boolean ifExceptionValueReturnNull) {
        Geometry geometry = geojsonToJtsGeometry(geojson, ifExceptionValueReturnNull);
        return handleGeometryToString(geometry, Geometry::toText, ifExceptionValueReturnNull);
    }

    //==============================jts转换功能==============================
    @Override
    public String jtsGeometryToWktString(Geometry jtsGeometry, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isNull(jtsGeometry)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("jtsGeometry");
        }
        return handleGeometryToString(jtsGeometry, Geometry::toText, ifExceptionValueReturnNull);
    }

    @Override
    public String jtsGeometryToGeoJson(Geometry jtsGeometry, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isNull(jtsGeometry)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("jtsGeometry");
        }
        try {
            return getGeometryJSON().toString(jtsGeometry);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public String jtsGeometryToPgGeometryHex(Geometry jtsGeometry, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isNull(jtsGeometry)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("jtsGeometry");
        }
        try {
            return WKBWriter.toHex(getWKBWriter().write(jtsGeometry));
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    //==============================pgGeom转换功能==============================
    @Override
    public Geometry pgGeometryToJtsGeometry(PGgeometry pgGeometry, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isNull(pgGeometry)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("pgGeometry");
        }
        org.postgis.Geometry pgGeom = pgGeometry.getGeometry();
        try {
            Geometry jtsGeom = getWKTReader().read(pgGeom.getTypeString() + pgGeom.getValue());
            jtsGeom.setSRID(pgGeom.getSrid());
            return jtsGeom;
        } catch (ParseException e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public String pgGeometryToGeoJson(PGgeometry pgGeometry, boolean ifExceptionValueReturnNull) {
        // 修复原方法的错误（原方法错误调用了toWktString）
        Geometry geometry = pgGeometryToJtsGeometry(pgGeometry, ifExceptionValueReturnNull);
        return jtsGeometryToGeoJson(geometry, ifExceptionValueReturnNull);
    }

    @Override
    public String pgGeometryToWkt(PGgeometry pgGeometry, boolean ifExceptionValueReturnNull) {
        // 修复原方法的错误（原方法错误调用了toGeoJson）
        Geometry geometry = pgGeometryToJtsGeometry(pgGeometry, ifExceptionValueReturnNull);
        return jtsGeometryToWktString(geometry, ifExceptionValueReturnNull);
    }

    //==============================wkt转换功能==============================
    @Override
    public Geometry wktToJtsGeometry(String wktString, boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(wktString)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("wktString");
        }
        try {
            return getWKTReader().read(wktString);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public LineString wktToJtsLineString(String wktString, boolean ifExceptionValueReturnNull) {
        return castGeometry(wktToJtsGeometry(wktString, ifExceptionValueReturnNull), LineString.class, ifExceptionValueReturnNull);
    }

    @Override
    public MultiLineString wktToJtsMultiLineString(String wktString, boolean ifExceptionValueReturnNull) {
        return castGeometry(wktToJtsGeometry(wktString, ifExceptionValueReturnNull), MultiLineString.class, ifExceptionValueReturnNull);
    }

    @Override
    public Polygon wktToJtsPolygon(String wktString, boolean ifExceptionValueReturnNull) {
        return castGeometry(wktToJtsGeometry(wktString, ifExceptionValueReturnNull), Polygon.class, ifExceptionValueReturnNull);
    }

    @Override
    public MultiPolygon wktToJtsMultiPolygon(String wktString, boolean ifExceptionValueReturnNull) {
        return castGeometry(wktToJtsGeometry(wktString, ifExceptionValueReturnNull), MultiPolygon.class, ifExceptionValueReturnNull);
    }

    @Override
    public Point wktToJtsPoint(String wktString, boolean ifExceptionValueReturnNull) {
        return castGeometry(wktToJtsGeometry(wktString, ifExceptionValueReturnNull), Point.class, ifExceptionValueReturnNull);
    }

    @Override
    public MultiPoint wktToJtsMultiPoint(String wktString, boolean ifExceptionValueReturnNull) {
        return castGeometry(wktToJtsGeometry(wktString, ifExceptionValueReturnNull), MultiPoint.class, ifExceptionValueReturnNull);
    }

    @Override
    public String wktToGeojson(String wktString, boolean ifExceptionValueReturnNull) {
        Geometry geometry = wktToJtsGeometry(wktString, ifExceptionValueReturnNull);
        return jtsGeometryToGeoJson(geometry, ifExceptionValueReturnNull);
    }

    @Override
    public byte[] wktToWkb(String wktString, boolean ifExceptionValueReturnNull) {
        Geometry geometry = wktToJtsGeometry(wktString, ifExceptionValueReturnNull);
        if (ObjectUtil.isNull(geometry)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geometry");
        }
        try {
            return getWKBWriter().write(geometry);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    //==============================wkb转换功能==============================
    @Override
    public String wkbToGeojson(String wkbByteString, boolean ifExceptionValueReturnNull) {
        Geometry geometry = wkbToJtsGeometry(wkbByteString, ifExceptionValueReturnNull);
        return jtsGeometryToGeoJson(geometry, ifExceptionValueReturnNull);
    }

    @Override
    public String wkbToWktString(String wkbByteString, boolean ifExceptionValueReturnNull) {
        Geometry geometry = wkbToJtsGeometry(wkbByteString, ifExceptionValueReturnNull);
        return jtsGeometryToWktString(geometry, ifExceptionValueReturnNull);
    }

    @Override
    public Geometry wkbToJtsGeometry(String wkbByteString, boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(wkbByteString)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("wkbByteString");
        }
        try {
            byte[] bytes = WKBReader.hexToBytes(wkbByteString);
            return getWKBReader().read(bytes);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    //==============================String转换功能==============================
    @Override
    public Point jtsPointByString(String pointString, String separator, boolean xFirst, boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(pointString) || StrUtil.isBlank(separator)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("pointString or separator");
        }
        try {
            List<String> split = StrUtil.split(pointString, separator);
            return jtsPointByStringList(split, xFirst, ifExceptionValueReturnNull);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public Point jtsPointByStringList(List<?> pointList, boolean xFirst, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isEmpty(pointList) || pointList.size() != 2) {
            String msg = "坐标列表必须包含且仅包含2个元素";
            return ifExceptionValueReturnNull ? null : throwRuntimeException(msg);
        }
        try {
            Object o1 = pointList.get(0);
            Object o2 = pointList.get(1);
            double x = Convert.convert(Double.class, o1);
            double y = Convert.convert(Double.class, o2);
            return xFirst ? jtsPointByXY(x, y, ifExceptionValueReturnNull) : jtsPointByXY(y, x, ifExceptionValueReturnNull);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public Point jtsPointByStringArray(Object[] pointArray, boolean xFirst, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isEmpty(pointArray) || pointArray.length != 2) {
            String msg = "坐标数组必须包含且仅包含2个元素";
            return ifExceptionValueReturnNull ? null : throwRuntimeException(msg);
        }
        try {
            List<Object> of = ListUtil.of(pointArray[0], pointArray[1]);
            return jtsPointByStringList(of, xFirst, ifExceptionValueReturnNull);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public Point jtsPointByXY(double x, double y, boolean ifExceptionValueReturnNull) {
        try {
            Coordinate coord = new Coordinate(x, y);
            return GEOMETRY_FACTORY.createPoint(coord);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    //==============================补充方法==============================
    @Override
    public Geometry wkbBytesToJtsGeometry(byte[] wkbBytes, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isEmpty(wkbBytes)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("wkbBytes");
        }
        try {
            return getWKBReader().read(wkbBytes);
        } catch (ParseException e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public byte[] jtsGeometryToWkbBytes(Geometry jtsGeometry, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isNull(jtsGeometry)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("jtsGeometry");
        }
        try {
            return getWKBWriter().write(jtsGeometry);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    @Override
    public WKTReader getWKTReader() {
        WKTReader wktReader = new WKTReader();
        wktReader.setIsOldJtsCoordinateSyntaxAllowed(false);
        return wktReader;
    }

    @Override
    public WKBWriter getWKBWriter() {
        WKBWriter wkbWriter = new WKBWriter(2, true);
        return wkbWriter;
    }

    @Override
    public WKBReader getWKBReader() {
        WKBReader wkbReader = new WKBReader();
        return wkbReader;
    }

    @Override
    public GeometryJSON getGeometryJSON() {
        GeometryJSON geometryJSON = new GeometryJSON(10);
        return geometryJSON;
    }

    //==============================私有工具方法==============================

    /**
     * 处理几何对象转字符串
     */
    private String handleGeometryToString(Geometry geometry, GeometryToStringFunction function, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isNull(geometry)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geometry");
        }
        try {
            return function.apply(geometry);
        } catch (Exception e) {
            handleException(e, ifExceptionValueReturnNull, null);
            return null;
        }
    }

    /**
     * 几何对象类型转换
     */
    private <T extends Geometry> T castGeometry(Geometry geometry, Class<T> targetClass, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isNull(geometry)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geometry");
        }
        try {
            if (targetClass.isInstance(geometry)) {
                return targetClass.cast(geometry);
            } else {
                throw new RuntimeException("几何类型不匹配，期望：" + targetClass.getSimpleName() + "，实际：" + geometry.getGeometryType());
            }
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull, null);
        }
    }

    /**
     * 异常处理通用方法
     */
    private <T> T handleException(Exception e, boolean ifExceptionValueReturnNull, T nullValue) {
        if (ifExceptionValueReturnNull) {
            return nullValue;
        } else {
            throw new RuntimeException(e);
        }
    }

    /**
     * 抛出空参数异常
     */
    private <T> T throwEmptyParamException(String paramName) {
        throw new RuntimeException("参数[" + paramName + "]不能为空");
    }

    /**
     * 抛出通用运行时异常
     */
    private <T> T throwRuntimeException(String msg) {
        throw new RuntimeException(msg);
    }

    /**
     * 几何对象转字符串函数式接口
     */
    @FunctionalInterface
    private interface GeometryToStringFunction {
        String apply(Geometry geometry);
    }
}
