package cn.geoair.map.dynamic.tools.convert;

import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;

/**
 * 空间类型转换核心接口 定义GeoJSON/WKT/WKB/JTS/PGGeometry等空间类型的互转规范
 *
 * @author 张逢吉
 * @date 2024/10/24
 */
public interface GirGeoFormatOpt {

    // ====================== GeoJSON 转换 ======================
    /**
     * GeoJSON字符串转JTS Geometry
     *
     * @param geojson GeoJSON字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null（否则抛运行时异常）
     * @return JTS Geometry
     */
    Geometry geojsonToJtsGeometry(String geojson, boolean ifExceptionValueReturnNull);

    /**
     * GeoJSON字符串转JTS LineString
     *
     * @param geojson GeoJSON字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS LineString
     */
    LineString geojsonToJtsLineString(String geojson, boolean ifExceptionValueReturnNull);

    /**
     * GeoJSON字符串转JTS MultiLineString
     *
     * @param geojson GeoJSON字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS MultiLineString
     */
    MultiLineString geojsonToJtsMultiLineString(String geojson, boolean ifExceptionValueReturnNull);

    /**
     * GeoJSON字符串转JTS Polygon
     *
     * @param geojson GeoJSON字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS Polygon
     */
    Polygon geojsonToJtsPolygon(String geojson, boolean ifExceptionValueReturnNull);

    /**
     * GeoJSON字符串转JTS MultiPolygon
     *
     * @param geojson GeoJSON字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS MultiPolygon
     */
    MultiPolygon geojsonToJtsMultiPolygon(String geojson, boolean ifExceptionValueReturnNull);

    /**
     * GeoJSON字符串转JTS Point
     *
     * @param geojson GeoJSON字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS Point
     */
    Point geojsonToJtsPoint(String geojson, boolean ifExceptionValueReturnNull);

    /**
     * GeoJSON字符串转JTS MultiPoint
     *
     * @param geojson GeoJSON字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS MultiPoint
     */
    MultiPoint geojsonToJtsMultiPoint(String geojson, boolean ifExceptionValueReturnNull);

    /**
     * GeoJSON字符串转WKT字符串
     *
     * @param geojson GeoJSON字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return WKT字符串
     */
    String geojsonToWktString(String geojson, boolean ifExceptionValueReturnNull);

    // ====================== JTS Geometry 转换 ======================
    /**
     * JTS Geometry转WKT字符串
     *
     * @param jtsGeometry JTS Geometry对象
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return WKT字符串
     */
    String jtsGeometryToWktString(Geometry jtsGeometry, boolean ifExceptionValueReturnNull);

    /**
     * 将 JTS Geometry 转为包含 SRID 的 EWKT 字符串。
     *
     * @param jtsGeometry JTS Geometry
     * @param ifExceptionValueReturnNull 转换失败时是否返回 {@code null}
     * @return {@code SRID=xxxx;WKT} 形式的 EWKT 字符串
     */
    String jtsGeometryToEwktString(Geometry jtsGeometry, boolean ifExceptionValueReturnNull);

    /**
     * JTS Geometry转GeoJSON字符串
     *
     * @param jtsGeometry JTS Geometry对象
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return GeoJSON字符串
     */
    String jtsGeometryToGeoJson(Geometry jtsGeometry, boolean ifExceptionValueReturnNull);

    /**
     * JTS Geometry转PostGIS WKB十六进制字符串
     *
     * @param jtsGeometry JTS Geometry对象
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return WKB十六进制字符串
     */
    String jtsGeometryToPgGeometryHex(Geometry jtsGeometry, boolean ifExceptionValueReturnNull);

    // ====================== PGGeometry 转换 ======================
    /**
     * PGGeometry转JTS Geometry
     *
     * @param pgGeometry PostGIS Geometry对象
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS Geometry
     */
    Geometry pgGeometryToJtsGeometry(Object pgGeometry, boolean ifExceptionValueReturnNull);

    // Geometry pgGeometryToJtsGeometry(
    // org.postgis.PGgeometry pgGeometry, boolean ifExceptionValueReturnNull);

    /**
     * PGGeometry转GeoJSON字符串
     *
     * @param pgGeometry PostGIS Geometry对象
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return GeoJSON字符串
     */
    String pgGeometryToGeoJson(Object pgGeometry, boolean ifExceptionValueReturnNull);

    // String pgGeometryToGeoJson(
    // org.postgis.PGgeometry pgGeometry, boolean ifExceptionValueReturnNull);

    /**
     * PGGeometry转WKT字符串
     *
     * @param pgGeometry PostGIS Geometry对象
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return WKT字符串
     */
    String pgGeometryToWkt(Object pgGeometry, boolean ifExceptionValueReturnNull);

    // String pgGeometryToWkt(org.postgis.PGgeometry pgGeometry, boolean
    // ifExceptionValueReturnNull);

    // ====================== WKT 转换 ======================
    /**
     * WKT字符串转JTS Geometry
     *
     * @param wktString WKT字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS Geometry
     */
    Geometry wktToJtsGeometry(String wktString, boolean ifExceptionValueReturnNull);

    /**
     * EWKT 字符串转 JTS Geometry。
     *
     * <p>EWKT 使用 {@code SRID=4326;POINT(...)} 形式，可在文本中同时携带坐标与 SRID。</p>
     *
     * @param ewktString EWKT 字符串
     * @param ifExceptionValueReturnNull 解析失败时是否返回 {@code null}
     * @return 带 EWKT 中 SRID 的 Geometry
     */
    Geometry ewktToJtsGeometry(String ewktString, boolean ifExceptionValueReturnNull);

    /**
     * WKT字符串转JTS LineString
     *
     * @param wktString WKT字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS LineString
     */
    LineString wktToJtsLineString(String wktString, boolean ifExceptionValueReturnNull);

    /**
     * WKT字符串转JTS MultiLineString
     *
     * @param wktString WKT字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS MultiLineString
     */
    MultiLineString wktToJtsMultiLineString(String wktString, boolean ifExceptionValueReturnNull);

    /**
     * WKT字符串转JTS Polygon
     *
     * @param wktString WKT字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS Polygon
     */
    Polygon wktToJtsPolygon(String wktString, boolean ifExceptionValueReturnNull);

    /**
     * WKT字符串转JTS MultiPolygon
     *
     * @param wktString WKT字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS MultiPolygon
     */
    MultiPolygon wktToJtsMultiPolygon(String wktString, boolean ifExceptionValueReturnNull);

    /**
     * WKT字符串转JTS Point
     *
     * @param wktString WKT字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS Point
     */
    Point wktToJtsPoint(String wktString, boolean ifExceptionValueReturnNull);

    /**
     * WKT字符串转JTS MultiPoint
     *
     * @param wktString WKT字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS MultiPoint
     */
    MultiPoint wktToJtsMultiPoint(String wktString, boolean ifExceptionValueReturnNull);

    /**
     * WKT字符串转GeoJSON字符串
     *
     * @param wktString WKT字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return GeoJSON字符串
     */
    String wktToGeojson(String wktString, boolean ifExceptionValueReturnNull);

    /**
     * WKT字符串转WKB字节数组
     *
     * @param wktString WKT字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return WKB字节数组
     */
    byte[] wktToWkb(String wktString, boolean ifExceptionValueReturnNull);

    // ====================== WKB 转换 ======================
    /**
     * WKB十六进制字符串转GeoJSON字符串
     *
     * @param wkbByteString WKB十六进制字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return GeoJSON字符串
     */
    String wkbToGeojson(String wkbByteString, boolean ifExceptionValueReturnNull);

    /**
     * WKB十六进制字符串转WKT字符串
     *
     * @param wkbByteString WKB十六进制字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return WKT字符串
     */
    String wkbToWktString(String wkbByteString, boolean ifExceptionValueReturnNull);

    /**
     * WKB十六进制字符串转JTS Geometry
     *
     * @param wkbByteString WKB十六进制字符串
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS Geometry
     */
    Geometry wkbToJtsGeometry(String wkbByteString, boolean ifExceptionValueReturnNull);

    // ====================== 字符串转Point ======================
    /**
     * 字符串转JTS Point（支持自定义分隔符）
     *
     * @param pointString 坐标字符串（如 "116.40,39.90"）
     * @param separator 分隔符（如 ","）
     * @param xFirst 是否X坐标在前
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS Point
     */
    Point jtsPointByString(
            String pointString,
            String separator,
            boolean xFirst,
            boolean ifExceptionValueReturnNull);

    /**
     * 列表转JTS Point
     *
     * @param pointList 坐标列表（需包含2个元素）
     * @param xFirst 是否X坐标在前
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS Point
     */
    Point jtsPointByStringList(
            java.util.List<?> pointList, boolean xFirst, boolean ifExceptionValueReturnNull);

    /**
     * 数组转JTS Point
     *
     * @param pointArray 坐标数组（需包含2个元素）
     * @param xFirst 是否X坐标在前
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS Point
     */
    Point jtsPointByStringArray(
            Object[] pointArray, boolean xFirst, boolean ifExceptionValueReturnNull);

    /**
     * 坐标值转JTS Point
     *
     * @param x X坐标
     * @param y Y坐标
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS Point
     */
    Point jtsPointByXY(double x, double y, boolean ifExceptionValueReturnNull);

    // ====================== 补充常用便捷方法（增强实用性） ======================
    /**
     * GeoJSON字符串转JTS Geometry（默认异常抛错）
     *
     * @param geojson GeoJSON字符串
     * @return JTS Geometry
     */
    default Geometry geojsonToJtsGeometry(String geojson) {
        return geojsonToJtsGeometry(geojson, false);
    }

    /**
     * WKT字符串转JTS Geometry（默认异常抛错）
     *
     * @param wktString WKT字符串
     * @return JTS Geometry
     */
    default Geometry wktToJtsGeometry(String wktString) {
        return wktToJtsGeometry(wktString, false);
    }

    /** EWKT 字符串转 JTS Geometry，解析失败时抛出异常。 */
    default Geometry ewktToJtsGeometry(String ewktString) {
        return ewktToJtsGeometry(ewktString, false);
    }

    /**
     * JTS Geometry转GeoJSON（默认异常抛错）
     *
     * @param jtsGeometry JTS Geometry对象
     * @return GeoJSON字符串
     */
    default String jtsGeometryToGeoJson(Geometry jtsGeometry) {
        return jtsGeometryToGeoJson(jtsGeometry, false);
    }

    /**
     * 坐标字符串转Point（默认逗号分隔、X在前、异常抛错）
     *
     * @param pointString 坐标字符串（如 "116.40,39.90"）
     * @return JTS Point
     */
    default Point jtsPointByString(String pointString) {
        return jtsPointByString(pointString, ",", true, false);
    }

    /**
     * WKB字节数组转JTS Geometry
     *
     * @param wkbBytes WKB字节数组
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return JTS Geometry
     */
    Geometry wkbBytesToJtsGeometry(byte[] wkbBytes, boolean ifExceptionValueReturnNull);

    /**
     * JTS Geometry转WKB字节数组
     *
     * @param jtsGeometry JTS Geometry对象
     * @param ifExceptionValueReturnNull 异常时是否返回null
     * @return WKB字节数组
     */
    byte[] jtsGeometryToWkbBytes(Geometry jtsGeometry, boolean ifExceptionValueReturnNull);

    /**
     * 获取 WKT 解析器实例（Well-Known Text Reader），用于将 WKT 文本格式转换为 {@link
     * org.locationtech.jts.geom.Geometry} 几何对象。
     *
     * <p>核心特性： 1. 支持解析标准 WKT 格式，以及带 Z/M/ZM 维度扩展的 WKT 语法（JTS 1.15+）； 2.
     * 可通过配置修复结构无效的输入（如缺失坐标的线串），但不保证拓扑有效性； 3. 关键字大小写不敏感，支持非标准 "LINEARRING" 标签； 4. 数值解析兼容 Java
     * 浮点字面量语法（含科学计数法）。
     *
     * <p>返回的解析器本身非线程安全；本方法每次调用均返回独立实例，可由不同线程分别获取后使用。
     *
     * @return 独立的 WKT 解析器实例，已绑定当前 {@code ToolsConfig} 配置的 GeometryFactory
     * @see org.locationtech.jts.io.WKTReader
     */
    WKTReader getWKTReader();

    /**
     * 获取 WKB 写入器实例（Well-Known Binary Writer），用于将 {@link org.locationtech.jts.geom.Geometry} 几何对象转换为
     * WKB 二进制格式。
     *
     * <p>核心特性： 1. 遵循 OGC Simple Features 规范，兼容 PostGIS 扩展 EWKB 格式（支持 3D 坐标、SRID 写入）； 2.
     * 对空几何有明确的序列化规则（如空点用 NaN 坐标表示，空线串用 0 个点表示）； 3. LinearRing 会被序列化为 LineString 类型的 WKB； 4. SRID
     * 仅在顶层几何中写入（遵循 JTS 约定：集合内所有几何共享同一 SRID）。
     *
     * <p>返回的写入器本身非线程安全；本方法每次调用均返回独立实例，可由不同线程分别获取后使用。
     *
     * @return 独立的 WKB 写入器实例，默认支持二维坐标并写入 SRID
     * @see org.locationtech.jts.io.WKBWriter
     */
    WKBWriter getWKBWriter();

    /**
     * 获取 WKB 解析器实例（Well-Known Binary Reader），用于将 WKB 二进制格式转换为 {@link
     * org.locationtech.jts.geom.Geometry} 几何对象。
     *
     * <p>核心特性： 1. 支持标准 WKB 和 PostGIS EWKB 格式，JTS 1.15+ 额外兼容 ISO/OGC 19125
     * 规范（Spatialite/Geopackage）； 2. 自动修复结构无效的输入（如顶点不足的线串/环、未闭合的环）； 3. 校验畸形/恶意 WKB
     * 数据（如字段值超限、读取越界），异常时抛出 ParseException； 4. 空点兼容 NaN 坐标表示，SRID 未指定时继承父几何的 SRID（默认 SRID=0）。
     *
     * <p>返回的解析器本身非线程安全；本方法每次调用均返回独立实例，可由不同线程分别获取后使用。
     *
     * @return 独立的 WKB 解析器实例，已绑定当前 {@code ToolsConfig} 配置的 GeometryFactory，支持 InStream 输入流
     * @see org.locationtech.jts.io.WKBReader
     */
    WKBReader getWKBReader();

    /**
     * 获取 GeoJSON 几何序列化/反序列化工具实例，用于 {@link org.locationtech.jts.geom.Geometry} 与 GeoJSON 格式的双向转换。
     *
     * <p>核心特性： 1. 支持点、线、面、多几何、几何集合等 GeoJSON Geometry 对象的解析/生成； 2. 可配置坐标精度（默认保留 15 位小数），避免浮点精度冗余。
     * 本接口仅处理 Geometry，不负责 GeoJSON Feature 与 FeatureCollection 的属性及集合语义。
     *
     * <p>返回的转换器本身非线程安全；本方法每次调用均返回独立实例，可由不同线程分别获取后使用。
     *
     * @return GeometryJSON 实例，默认配置适配大多数 GeoJSON 场景
     * @see org.geotools.geojson.geom.GeometryJSON
     */
    GeometryJSON getGeometryJSON();
}
