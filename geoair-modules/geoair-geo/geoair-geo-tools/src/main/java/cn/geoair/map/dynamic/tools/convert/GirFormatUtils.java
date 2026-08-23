package cn.geoair.map.dynamic.tools.convert;

import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;

/**
 * 空间格式转换工具的 {@link GirGeoFormatOpt} 实现。
 *
 * <p>支持 GeoJSON、WKT、WKB、JTS Geometry 及运行期可识别的 PostGIS Geometry 对象互转。
 * 实例按 {@link ToolsConfig} 对象身份复用；默认单例入口仅用于兼容旧代码。</p>
 *
 * @author 张逢吉
 * @date 2024/10/24 15:29
 */
public class GirFormatUtils implements GirGeoFormatOpt {

    // 单例实例（volatile保证可见性，防止指令重排）
    private static volatile GirFormatUtils INSTANCE;

    /** 按 ToolsConfig 对象身份复用格式工具，避免同配置下重复创建工具实例。 */
    private static final Map<ToolsConfig, GirFormatUtils> CONFIGURED_INSTANCES =
            Collections.synchronizedMap(new IdentityHashMap<ToolsConfig, GirFormatUtils>());


    /** 当前实例使用的格式组件配置。 */
    private final ToolsConfig advToolsConfig;

    /**
     * 使用指定配置创建格式转换器。
     *
     * @param advToolsConfig 格式化配置，不能为空
     */
    public GirFormatUtils(ToolsConfig advToolsConfig) {
        this.advToolsConfig = advToolsConfig;
    }

    /**
     * 获取默认配置的遗留单例。
     *
     * @deprecated 请使用 {@link #getInstance(ToolsConfig)}，以显式绑定格式化配置。
     * @return 默认格式转换器
     */
    @Deprecated
    public static GirFormatUtils getInstance() {
        if (INSTANCE == null) {
            synchronized (GirFormatUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GirFormatUtils(new ToolsConfig());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 获取与配置对象绑定的格式转换器。
     *
     * @param advToolsConfig 格式化配置；为 {@code null} 时返回默认单例
     * @return 格式转换器
     */
    public static GirFormatUtils getInstance(ToolsConfig advToolsConfig) {
        if (advToolsConfig == null) {
            return getInstance();
        }
        synchronized (CONFIGURED_INSTANCES) {
            GirFormatUtils formatUtils = CONFIGURED_INSTANCES.get(advToolsConfig);
            if (formatUtils == null) {
                formatUtils = new GirFormatUtils(advToolsConfig);
                CONFIGURED_INSTANCES.put(advToolsConfig, formatUtils);
            }
            return formatUtils;
        }
    }

    // ==============================geojson转换功能==============================
    @Override
    public Geometry geojsonToJtsGeometry(String geojson, boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(geojson)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geojson");
        }
        try (Reader reader = new StringReader(geojson)) {
            return getGeometryJSON().read(reader);
        } catch (IOException e) {
            return handleException(e, ifExceptionValueReturnNull);
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
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    @Override
    public MultiLineString geojsonToJtsMultiLineString(
            String geojson, boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(geojson)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geojson");
        }
        try (Reader reader = new StringReader(geojson)) {
            return getGeometryJSON().readMultiLine(reader);
        } catch (IOException e) {
            return handleException(e, ifExceptionValueReturnNull);
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
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    @Override
    public MultiPolygon geojsonToJtsMultiPolygon(
            String geojson, boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(geojson)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geojson");
        }
        try (Reader reader = new StringReader(geojson)) {
            return getGeometryJSON().readMultiPolygon(reader);
        } catch (IOException e) {
            return handleException(e, ifExceptionValueReturnNull);
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
            return handleException(e, ifExceptionValueReturnNull);
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
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    @Override
    public String geojsonToWktString(String geojson, boolean ifExceptionValueReturnNull) {
        Geometry geometry = geojsonToJtsGeometry(geojson, ifExceptionValueReturnNull);
        return handleGeometryToString(geometry, Geometry::toText, ifExceptionValueReturnNull);
    }

    // ==============================jts转换功能==============================
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
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    @Override
    public String jtsGeometryToPgGeometryHex(
            Geometry jtsGeometry, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isNull(jtsGeometry)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("jtsGeometry");
        }
        try {
            return WKBWriter.toHex(advToolsConfig.getWkbWriter().write(jtsGeometry));
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    // ==============================pgGeom转换功能==============================
    @Override
    public Geometry pgGeometryToJtsGeometry(Object pgGeometry, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isNull(pgGeometry)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("pgGeometry");
        }
        try {
            Geometry jtsGeom = null;
            if (GirPostGisTran.isOrgConvert() && GirPostGisOrgTran.isGeometry(pgGeometry)) {
                jtsGeom = GirPostGisOrgTran.toJtsGeometry(pgGeometry);
            } else if (GirPostGisTran.isNetConvert() && GirPostGisNetTran.isGeometry(pgGeometry)) {
                jtsGeom = GirPostGisNetTran.toJtsGeometry(pgGeometry);
            }
            return jtsGeom;
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    @Override
    public String pgGeometryToGeoJson(Object pgGeometry, boolean ifExceptionValueReturnNull) {
        // 修复原方法的错误（原方法错误调用了toWktString）
        Geometry geometry = pgGeometryToJtsGeometry(pgGeometry, ifExceptionValueReturnNull);
        return jtsGeometryToGeoJson(geometry, ifExceptionValueReturnNull);
    }

    @Override
    public String pgGeometryToWkt(Object pgGeometry, boolean ifExceptionValueReturnNull) {
        // 修复原方法的错误（原方法错误调用了toGeoJson）
        Geometry geometry = pgGeometryToJtsGeometry(pgGeometry, ifExceptionValueReturnNull);
        return jtsGeometryToWktString(geometry, ifExceptionValueReturnNull);
    }

    // ==============================wkt转换功能==============================
    @Override
    public Geometry wktToJtsGeometry(String wktString, boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(wktString)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("wktString");
        }
        try {
            return advToolsConfig.getWktReaderSupplier().get().read(wktString);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    @Override
    public LineString wktToJtsLineString(String wktString, boolean ifExceptionValueReturnNull) {
        return castGeometry(
                wktToJtsGeometry(wktString, ifExceptionValueReturnNull),
                LineString.class,
                ifExceptionValueReturnNull);
    }

    @Override
    public MultiLineString wktToJtsMultiLineString(
            String wktString, boolean ifExceptionValueReturnNull) {
        return castGeometry(
                wktToJtsGeometry(wktString, ifExceptionValueReturnNull),
                MultiLineString.class,
                ifExceptionValueReturnNull);
    }

    @Override
    public Polygon wktToJtsPolygon(String wktString, boolean ifExceptionValueReturnNull) {
        return castGeometry(
                wktToJtsGeometry(wktString, ifExceptionValueReturnNull),
                Polygon.class,
                ifExceptionValueReturnNull);
    }

    @Override
    public MultiPolygon wktToJtsMultiPolygon(String wktString, boolean ifExceptionValueReturnNull) {
        return castGeometry(
                wktToJtsGeometry(wktString, ifExceptionValueReturnNull),
                MultiPolygon.class,
                ifExceptionValueReturnNull);
    }

    @Override
    public Point wktToJtsPoint(String wktString, boolean ifExceptionValueReturnNull) {
        return castGeometry(
                wktToJtsGeometry(wktString, ifExceptionValueReturnNull),
                Point.class,
                ifExceptionValueReturnNull);
    }

    @Override
    public MultiPoint wktToJtsMultiPoint(String wktString, boolean ifExceptionValueReturnNull) {
        return castGeometry(
                wktToJtsGeometry(wktString, ifExceptionValueReturnNull),
                MultiPoint.class,
                ifExceptionValueReturnNull);
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
            return advToolsConfig.getWkbWriter().write(geometry);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    // ==============================wkb转换功能==============================
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
            return advToolsConfig.getWkbReaderSupplier().get().read(bytes);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    // ==============================String转换功能==============================
    @Override
    public Point jtsPointByString(
            String pointString,
            String separator,
            boolean xFirst,
            boolean ifExceptionValueReturnNull) {
        if (StrUtil.isBlank(pointString) || StrUtil.isBlank(separator)) {
            return ifExceptionValueReturnNull
                    ? null
                    : throwEmptyParamException("pointString or separator");
        }
        try {
            List<String> split = StrUtil.split(pointString, separator);
            return jtsPointByStringList(split, xFirst, ifExceptionValueReturnNull);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    @Override
    public Point jtsPointByStringList(
            List<?> pointList, boolean xFirst, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isEmpty(pointList) || pointList.size() != 2) {
            String msg = "坐标列表必须包含且仅包含2个元素";
            return ifExceptionValueReturnNull ? null : throwRuntimeException(msg);
        }
        try {
            Object o1 = pointList.get(0);
            Object o2 = pointList.get(1);
            double x = Convert.convert(Double.class, o1);
            double y = Convert.convert(Double.class, o2);
            return xFirst
                    ? jtsPointByXY(x, y, ifExceptionValueReturnNull)
                    : jtsPointByXY(y, x, ifExceptionValueReturnNull);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    @Override
    public Point jtsPointByStringArray(
            Object[] pointArray, boolean xFirst, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isEmpty(pointArray) || pointArray.length != 2) {
            String msg = "坐标数组必须包含且仅包含2个元素";
            return ifExceptionValueReturnNull ? null : throwRuntimeException(msg);
        }
        try {
            List<Object> of = ListUtil.of(pointArray[0], pointArray[1]);
            return jtsPointByStringList(of, xFirst, ifExceptionValueReturnNull);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    @Override
    public Point jtsPointByXY(double x, double y, boolean ifExceptionValueReturnNull) {
        try {
            Coordinate coord = new Coordinate(x, y);
            return advToolsConfig.getGeometryFactory().createPoint(coord);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    // ==============================补充方法==============================
    @Override
    public Geometry wkbBytesToJtsGeometry(byte[] wkbBytes, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isEmpty(wkbBytes)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("wkbBytes");
        }
        try {
            return advToolsConfig.getWkbReaderSupplier().get().read(wkbBytes);
        } catch (ParseException e) {
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    @Override
    public byte[] jtsGeometryToWkbBytes(Geometry jtsGeometry, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isNull(jtsGeometry)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("jtsGeometry");
        }
        try {
            return advToolsConfig.getWkbWriter().write(jtsGeometry);
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    @Override
    public WKTReader getWKTReader() {
        return advToolsConfig.getWktReaderSupplier().get();
    }

    @Override
    public WKBWriter getWKBWriter() {
        return advToolsConfig.getWkbWriter();
    }

    @Override
    public WKBReader getWKBReader() {
        return advToolsConfig.getWkbReaderSupplier().get();
    }


    @Override
    public GeometryJSON getGeometryJSON() {
        return advToolsConfig.getGeometryJSON();
    }

    // ==============================私有工具方法==============================

    /**
     * 处理几何对象转字符串
     */
    private String handleGeometryToString(
            Geometry geometry,
            GeometryToStringFunction function,
            boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isNull(geometry)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geometry");
        }
        try {
            return function.apply(geometry);
        } catch (Exception e) {
            handleException(e, ifExceptionValueReturnNull);
            return null;
        }
    }

    /**
     * 几何对象类型转换
     */
    private <T extends Geometry> T castGeometry(
            Geometry geometry, Class<T> targetClass, boolean ifExceptionValueReturnNull) {
        if (ObjectUtil.isNull(geometry)) {
            return ifExceptionValueReturnNull ? null : throwEmptyParamException("geometry");
        }
        try {
            if (targetClass.isInstance(geometry)) {
                return targetClass.cast(geometry);
            } else {
                throw new RuntimeException(
                        "几何类型不匹配，期望："
                                + targetClass.getSimpleName()
                                + "，实际："
                                + geometry.getGeometryType());
            }
        } catch (Exception e) {
            return handleException(e, ifExceptionValueReturnNull);
        }
    }

    /**
     * 异常处理通用方法
     */
    private <T> T handleException(Exception e, boolean ifExceptionValueReturnNull) {
        if (ifExceptionValueReturnNull) {
            return null;
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
