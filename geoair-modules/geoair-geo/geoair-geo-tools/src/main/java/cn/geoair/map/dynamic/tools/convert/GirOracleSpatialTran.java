package cn.geoair.map.dynamic.tools.convert;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import oracle.spatial.util.ByteOrder;
import oracle.sql.STRUCT;
import oracle.spatial.util.WKB;
import oracle.sql.StructDescriptor;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ByteOrderValues;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Oracle Spatial 几何数据转换工具类
 * <p>使用 Oracle Spatial 的 WKB 工具类进行转换，依赖 orai18n.jar</p>
 * <p>注意：使用此类前请确保 GirOracleTran.isOracleSpatialAvailable() 返回 true</p>
 *
 * @author zhangjun
 */
public class GirOracleSpatialTran {
    static GiLogger logger = GirLoggerFactory.getLogger();

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
            Geometry jtsGeom = GirGeoTools.defaultInstance().getFormatOpt().getWKBReader().read(bytes);
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
            logger.error(e);
            return null;
        }
    }

    /**
     * 将 JTS Geometry 对象转换为 Oracle SDO_GEOMETRY (STRUCT)
     * <p>注意：此方法需要数据库连接，且 JTS Geometry 需要设置 SRID</p>
     *
     * @param geometry   JTS Geometry 对象
     * @param connection 数据库连接（用于创建 STRUCT）
     * @return Oracle STRUCT 对象，转换失败返回 null
     */
    public static STRUCT jtsGeomToSdoGeometry(Geometry geometry, java.sql.Connection connection) {
        return jtsGeomToSdoGeometry(geometry, connection, geometry != null ? geometry.getSRID() : 0);
    }

    /**
     * 将 JTS Geometry 转换为 Oracle SDO_GEOMETRY STRUCT，含 SRID。
     */
    public static STRUCT jtsGeomToSdoGeometry(
            Geometry geometry, Connection connection, int srid) {
        if (geometry == null || connection == null) {
            return null;
        }
        try {
            if (srid > 0) geometry.setSRID(srid);
            WKBWriter writer = new WKBWriter(2, ByteOrderValues.BIG_ENDIAN, true);
            byte[] wkbBytes = writer.write(geometry);

            WKB wkb = new WKB(ByteOrder.BIG_ENDIAN);
            STRUCT sdoStruct = wkb.toSTRUCT(wkbBytes, connection);

            // 通过 STRUCT 的 setObject 或 getAttributes 修改 SRID
            // 注意：这种方式需要知道 STRUCT 的内部字段顺序
            Object[] attrs = sdoStruct.getAttributes();
            attrs[0] = srid;  // 第1个字段是 SDO_SRID
            return new STRUCT(sdoStruct.getDescriptor(), connection, attrs);

        } catch (Exception e) {
            logger.error("JTS to SDO_GEOMETRY conversion failed", e);
            return null;
        }
    }

    public static Object jtsGeomToSdoGeometryObj(Geometry geometry, java.sql.Connection connection) {
        return jtsGeomToSdoGeometry(geometry, connection);
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
     * @param rs         ResultSet
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
     * @param rs         ResultSet
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
     * @param rs          ResultSet
     * @param columnNames 列名数组
     * @return Geometry 数组
     */
    public static Geometry[] getGeometriesFromResultSet(java.sql.ResultSet rs, String... columnNames) {
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
