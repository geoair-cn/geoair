package cn.geoair.comp.db.service.core.typehander.dm;

import cn.geoair.comp.db.service.core.typehander.BaseTypeHandler;
import cn.hutool.db.Entity;
import cn.hutool.db.meta.JdbcType;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:46
 * @description：达梦空间字段解析
 */
public class DmGeomTypeHandler extends BaseTypeHandler<String> {

    private final WKBReader wkbReader = new WKBReader();

    @Override
    public String getNonNullParameter(Object parameter, JdbcType jdbcType) {
        return null;
    }

    @Override
    public String getResult(Entity entity, String columnName) {
        Object obj = entity.getObj(columnName);
        return getResult(obj);
    }

    @Override
    public String getResult(ResultSet resultSet, String columnName) {
        try {
            return getResult(resultSet.getObject(columnName));
        } catch (SQLException e) {
            return "";
        }
    }

    @Override
    public String getResult(ResultSet resultSet, Integer columnIndex) {
        try {
            return getResult(resultSet.getObject(columnIndex));
        } catch (SQLException e) {
            return "";
        }
    }

    @Override
    public String getResult(Map<String, Object> row, String columnName) {
        return getResult(row.get(columnName));
    }

    @Override
    public String getResult(Object obj) {
        if (obj == null) {
            return null;
        }
        if (isDmdbStruct(obj)) {
            return toWkt(obj);
        }
        return String.valueOf(obj);
    }

    private boolean isDmdbStruct(Object obj) {
        return obj != null && "dm.jdbc.driver.DmdbStruct".equals(obj.getClass().getName());
    }

    private String toWkt(Object value) {
        try {
            Method getAttributes = value.getClass().getMethod("getAttributes");
            Object[] attrs = (Object[]) getAttributes.invoke(value);
            if (attrs == null || attrs.length == 0 || !(attrs[0] instanceof Blob)) {
                return String.valueOf(value);
            }
            Blob gSerObj = (Blob) attrs[0];
            int len = (int) gSerObj.length();
            byte[] gserialized = gSerObj.getBytes(1, len);
            byte[] wkb = wkbFromGser(gserialized);
            if (wkb == null) {
                return String.valueOf(value);
            }
            Geometry jtsGeom = wkbReader.read(wkb);
            return jtsGeom == null ? String.valueOf(value) : jtsGeom.toText();
        } catch (Throwable e) {
            return String.valueOf(value);
        }
    }

    private byte[] wkbFromGser(byte[] gserialized) throws Exception {
        Class<?> utilClass = Class.forName("com.dameng.geotools.util.DmGeo2Util");
        Method method = utilClass.getMethod("wkbFromGser", byte[].class, int.class);
        Object ndr = utilClass.getField("NDR").get(null);
        return (byte[]) method.invoke(null, gserialized, ((Number) ndr).intValue());
    }
}
