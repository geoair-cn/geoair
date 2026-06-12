package cn.geoair.map.dynamic.tools.convert;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import java.sql.SQLException;
import oracle.spatial.util.ByteOrder;
import oracle.spatial.util.WKB;
import oracle.sql.STRUCT;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

/**
 * Oracle Spatial 几何数据转换工具类
 *
 * <p>使用 Oracle Spatial 的 WKB 工具类进行转换，依赖 orai18n.jar
 *
 * <p>注意：使用此类前请确保 GirOracleTran.isOracleSpatialAvailable() 返回 true
 *
 * @author zhangjun
 */
public class GirOracleSpatialTran {

    /**
     * 判断值是否为 Oracle Spatial 几何数据
     *
     * @param value 待检查的值
     * @return true=是几何数据，false=不是
     */
    public static boolean isSdoGeometry(Object value) {
        return GirOracleTran.isSdoGeometryType(value);
    }

    /**
     * 将 Oracle SDO_GEOMETRY (STRUCT) 转换为 WKT 字符串
     *
     * @param value Oracle STRUCT 对象（SDO_GEOMETRY）
     * @return WKT 字符串，转换失败返回 "无法解析空间数据"
     */
    public static String sdoGeometryToWkt(Object value) {
        if (!(value instanceof STRUCT)) {
            return "无法解析空间数据：不是STRUCT类型";
        }
        try {
            // 使用 BIG_ENDIAN 字节序（Oracle 默认使用大端序）
            WKB wkb = new WKB(ByteOrder.BIG_ENDIAN);
            // 将 STRUCT 转换为字节数组
            byte[] bytes = wkb.fromSTRUCT((STRUCT) value);
            Geometry jtsGeom =
                    GirGeoTools.defaultInstance().getFormatOpt().getWKBReader().read(bytes);
            return jtsGeom.toText();
        } catch (Exception e) {
            return "无法解析空间数据：" + e.getMessage();
        }
    }

    /**
     * 将 Oracle SDO_GEOMETRY (STRUCT) 转换为 JTS Geometry 对象
     *
     * @param value Oracle STRUCT 对象（SDO_GEOMETRY）
     * @return JTS Geometry 对象，转换失败返回 null
     */
    public static Geometry sdoGeometryToJtsGeom(Object value) {
        if (!(value instanceof STRUCT)) {
            return null;
        }

        try {
            WKB wkb = new WKB(ByteOrder.BIG_ENDIAN);
            byte[] bytes = wkb.fromSTRUCT((STRUCT) value);
            return GirGeoTools.defaultInstance().getFormatOpt().getWKBReader().read(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 JTS Geometry 对象转换为 Oracle SDO_GEOMETRY (STRUCT)
     *
     * <p>注意：此方法需要数据库连接，且 JTS Geometry 需要设置 SRID
     *
     * @param geometry JTS Geometry 对象
     * @param connection 数据库连接（用于创建 STRUCT）
     * @return Oracle STRUCT 对象，转换失败返回 null
     */
    public static STRUCT jtsGeomToSdoGeometry(Geometry geometry, java.sql.Connection connection) {
        if (geometry == null || connection == null) {
            return null;
        }

        try {
            // 将 JTS Geometry 转换为 WKB 字节数组
            WKBWriter writer = new WKBWriter();
            byte[] wkbBytes = writer.write(geometry);

            // 使用 Oracle WKB 工具类转换为 STRUCT
            WKB wkb = new WKB(ByteOrder.BIG_ENDIAN);
            return wkb.toSTRUCT(wkbBytes, connection);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 WKT 字符串转换为 JTS Geometry 对象
     *
     * @param wkt WKT 字符串
     * @return JTS Geometry 对象，转换失败返回 null
     */
    public static Geometry wktToJtsGeom(String wkt) {
        if (wkt == null || wkt.isEmpty()) {
            return null;
        }

        try {
            WKTReader reader = new WKTReader();
            return reader.read(wkt);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 JTS Geometry 对象转换为 WKT 字符串
     *
     * @param geometry JTS Geometry 对象
     * @return WKT 字符串，转换失败返回 null
     */
    public static String jtsGeomToWkt(Geometry geometry) {
        if (geometry == null) {
            return null;
        }

        try {
            WKTWriter writer = new WKTWriter();
            return writer.write(geometry);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 ResultSet 中获取空间字段并转换为 JTS Geometry
     *
     * @param rs ResultSet
     * @param columnName 列名
     * @return JTS Geometry 对象
     */
    public static Geometry getGeometryFromResultSet(java.sql.ResultSet rs, String columnName) {
        try {
            Object value = rs.getObject(columnName);
            return sdoGeometryToJtsGeom(value);
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * 从 ResultSet 中获取空间字段并转换为 WKT
     *
     * @param rs ResultSet
     * @param columnName 列名
     * @return WKT 字符串
     */
    public static String getWktFromResultSet(java.sql.ResultSet rs, String columnName) {
        try {
            Object value = rs.getObject(columnName);
            return sdoGeometryToWkt(value);
        } catch (SQLException e) {
            return "无法解析空间数据：" + e.getMessage();
        }
    }

    /**
     * 批量转换 ResultSet 中的多个空间字段
     *
     * @param rs ResultSet
     * @param columnNames 列名数组
     * @return Geometry 数组
     */
    public static Geometry[] getGeometriesFromResultSet(
            java.sql.ResultSet rs, String... columnNames) {
        if (columnNames == null || columnNames.length == 0) {
            return null;
        }

        Geometry[] geometries = new Geometry[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            geometries[i] = getGeometryFromResultSet(rs, columnNames[i]);
        }
        return geometries;
    }
}
