package cn.geoair.map.dynamic.adv.query.typehandler.impl;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import java.lang.reflect.Method;
import java.sql.Blob;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;

/**
 * 达梦数据库空间类型处理器。
 * <p>
 * 读取：通过反射解析 {@code dm.jdbc.driver.DmdbStruct} → Gserialized → WKB → JTS Geometry。
 * 写入：转为 WKT 字符串，JDBC 原生兼容。
 *
 * @author zhangjun
 */
public class DmGeometryAdvTypeHandler extends JtsGeometryAdvTypeHandler {

    private final WKBReader wkbReader = new WKBReader();

    @Override
    protected Geometry readDialectGeometry(Object value) {
        if (!isDmdbStruct(value)) {
            return null;
        }
        return parseDmdbStruct(value);
    }

    @Override
    protected Object writeGeometry(Geometry value) {
        return GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(value, true);
    }

    /**
     * 判断是否为达梦 DmdbStruct 类型（通过类名判断，避免编译期依赖达梦驱动）
     */
    private boolean isDmdbStruct(Object obj) {
        return obj != null && "dm.jdbc.driver.DmdbStruct".equals(obj.getClass().getName());
    }

    /**
     * 反射解析 DmdbStruct：getAttributes() → Blob → Gserialized → WKB → JTS Geometry
     */
    private Geometry parseDmdbStruct(Object value) {
        try {
            Method getAttributes = value.getClass().getMethod("getAttributes");
            Object[] attrs = (Object[]) getAttributes.invoke(value);
            if (attrs == null || attrs.length == 0 || !(attrs[0] instanceof Blob)) {
                return null;
            }
            Blob gSerObj = (Blob) attrs[0];
            byte[] gserialized = gSerObj.getBytes(1, (int) gSerObj.length());
            byte[] wkb = wkbFromGser(gserialized);
            if (wkb == null) {
                return null;
            }
            return wkbReader.read(wkb);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 反射调用 com.dameng.geotools.util.DmGeo2Util.wkbFromGser(byte[], int) 将 Gserialized 转为 WKB
     */
    private byte[] wkbFromGser(byte[] gserialized) throws Exception {
        Class<?> utilClass = Class.forName("com.dameng.geotools.util.DmGeo2Util");
        Method method = utilClass.getMethod("wkbFromGser", byte[].class, int.class);
        Object ndr = utilClass.getField("NDR").get(null);
        return (byte[]) method.invoke(null, gserialized, ((Number) ndr).intValue());
    }
}
