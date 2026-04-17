package cn.geoair.map.dynamic.tools.convert;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.ParseException;

import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * @author ：张逢吉
 * @date ：Created in 12:24
 * @description：MySQL几何数据转换工具
 */
public class GirMysqlTran {

    /**
     * 判断值是否为MySQL Geometry二进制数据
     *
     * @param value 待检查的值（可能是byte[]或Blob）
     * @return true=是几何数据，false=不是
     */
    public static boolean isGeomValue(Object value) {
        if (value == null) {
            return false;
        }

        // 转换为字节数组
        byte[] bytes = toByteArray(value);
        if (bytes == null || bytes.length < 25) { // 最小几何对象（点）至少25字节
            return false;
        }

        try {
            // MySQL Geometry二进制格式：前4字节SRID + 标准WKB
            byte[] wkbData = extractWkbData(bytes);
            if (wkbData.length < 5) {
                return false;
            }

            // 验证WKB字节序（应该是0或1）
            byte byteOrder = wkbData[0];
            if (byteOrder != 0x00 && byteOrder != 0x01) {
                return false;
            }

            // 尝试解析几何类型（1-20为有效类型）
            int geomType;
            if (byteOrder == 0x00) { // 大端
                geomType = ((wkbData[1] & 0xFF) << 24) |
                        ((wkbData[2] & 0xFF) << 16) |
                        ((wkbData[3] & 0xFF) << 8) |
                        (wkbData[4] & 0xFF);
            } else { // 小端
                geomType = (wkbData[1] & 0xFF) |
                        ((wkbData[2] & 0xFF) << 8) |
                        ((wkbData[3] & 0xFF) << 16) |
                        ((wkbData[4] & 0xFF) << 24);
            }

            // 几何类型范围：1=Point, 2=LineString, 3=Polygon,
            // 4=MultiPoint, 5=MultiLineString, 6=MultiPolygon
            return geomType >= 1 && geomType <= 20;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将MySQL Geometry二进制数据转换为JTS Geometry对象
     *
     * @param value 从JDBC获取的值（byte[]或Blob类型）
     * @return JTS Geometry对象，转换失败返回null
     */
    public static Geometry mysqlBinaryToJtsGeom(Object value) {
        if (value == null) {
            return null;
        }

        Geometry jtsGeom = null;

        try {
            // 1. 转换为字节数组
            byte[] mysqlBinary = toByteArray(value);
            if (mysqlBinary == null || mysqlBinary.length < 25) {
                return null;
            }

            // 2. 提取SRID（前4字节）
            ByteBuffer buffer = ByteBuffer.wrap(mysqlBinary, 0, 4);
            int srid = buffer.getInt();

            // 3. 提取标准WKB数据（去除前4字节）
            byte[] wkbData = extractWkbData(mysqlBinary);

            // 4. 使用JTS WKBReader解析
            WKBReader reader = new WKBReader();
            jtsGeom = reader.read(wkbData);

            // 5. 设置SRID
            if (jtsGeom != null && srid != 0) {
                jtsGeom.setSRID(srid);
            }

        } catch (ParseException e) {
            // 解析失败返回null
            return null;
        } catch (Exception e) {
            return null;
        }

        return jtsGeom;
    }

    /**
     * 将各种类型的对象转换为字节数组
     *
     * @param value 原始值（byte[]或Blob）
     * @return 字节数组，转换失败返回null
     */
    private static byte[] toByteArray(Object value) {
        if (value == null) {
            return null;
        }

        // 直接是字节数组
        if (value instanceof byte[]) {
            return (byte[]) value;
        }

        // MySQL的Blob类型
        if (value instanceof Blob) {
            try {
                Blob blob = (Blob) value;
                long length = blob.length();
                if (length > Integer.MAX_VALUE || length == 0) {
                    return null;
                }
                return blob.getBytes(1, (int) length);
            } catch (SQLException e) {
                return null;
            }
        }

        // 其他类型不支持
        return null;
    }

    /**
     * 从MySQL二进制数据中提取标准WKB数据
     *
     * @param mysqlBinary MySQL格式的二进制数据（含4字节SRID头）
     * @return 标准WKB数据
     */
    private static byte[] extractWkbData(byte[] mysqlBinary) {
        if (mysqlBinary == null || mysqlBinary.length <= 4) {
            return new byte[0];
        }

        byte[] wkbData = new byte[mysqlBinary.length - 4];
        System.arraycopy(mysqlBinary, 4, wkbData, 0, mysqlBinary.length - 4);
        return wkbData;
    }
}
