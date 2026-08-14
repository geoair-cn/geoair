package cn.geoair.map.dynamic.tools.convert;

import java.lang.reflect.Method;
import java.sql.Blob;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;

/**
 * 达梦数据库空间类型转换工具类。
 * <p>
 * 将达梦 {@code DmdbStruct}（Gserialized 格式）转换为 JTS Geometry。
 * 转换链：DmdbStruct → Blob → Gserialized bytes → WKB bytes → JTS Geometry。
 * <p>
 * 调用方应先通过 {@link GirDMTran#isDmDriverAvailable()} 确认驱动可用。
 *
 * @author zhangjun
 */
public class GirDMSpatialTran {

    private static final WKBReader WKB_READER = new WKBReader();

    /**
     * 判断值是否为达梦 DmdbStruct 类型（通过类名匹配，避免编译期依赖）
     */
    public static boolean isDmdbStruct(Object value) {
        return value != null && "dm.jdbc.driver.DmdbStruct".equals(value.getClass().getName());
    }

    /**
     * 将达梦 DmdbStruct 转换为 JTS Geometry。
     * <p>
     * 反射调用链：DmdbStruct.getAttributes() → Blob → byte[] (Gserialized)
     * → DmGeo2Util.wkbFromGser(gserialized, NDR) → byte[] (WKB) → WKBReader → Geometry
     *
     * @param value DmdbStruct 对象
     * @return JTS Geometry，转换失败返回 null
     */
    public static Geometry dmStructToJtsGeom(Object value) {
        if (!isDmdbStruct(value)) {
            return null;
        }
        try {
            // 1. 反射调用 getAttributes() 获取属性数组
            Method getAttributes = value.getClass().getMethod("getAttributes");
            Object[] attrs = (Object[]) getAttributes.invoke(value);
            if (attrs == null || attrs.length == 0 || !(attrs[0] instanceof Blob)) {
                return null;
            }
            // 2. 从 Blob 中读取 Gserialized 字节
            Blob gSerObj = (Blob) attrs[0];
            byte[] gserialized = gSerObj.getBytes(1, (int) gSerObj.length());
            // 3. Gserialized → WKB（通过达梦空间工具）
            byte[] wkb = gserializedToWkb(gserialized);
            if (wkb == null) {
                return null;
            }
            // 4. WKB → JTS Geometry
            return WKB_READER.read(wkb);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将达梦 Gserialized 字节转换为标准 WKB 字节。
     * <p>
     * 反射调用 {@code com.dameng.geotools.util.DmGeo2Util.wkbFromGser(byte[], int)}。
     *
     * @param gserialized Gserialized 格式字节
     * @return WKB 格式字节，失败返回 null
     */
    private static byte[] gserializedToWkb(byte[] gserialized) {
        try {
            Class<?> utilClass = Class.forName("com.dameng.geotools.util.DmGeo2Util");
            Method method = utilClass.getMethod("wkbFromGser", byte[].class, int.class);
            Object ndr = utilClass.getField("NDR").get(null);
            return (byte[]) method.invoke(null, gserialized, ((Number) ndr).intValue());
        } catch (Exception e) {
            return null;
        }
    }
}
