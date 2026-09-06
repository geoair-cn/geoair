package cn.geoair.map.dynamic.adv.query.typehandler.impl;

import cn.geoair.map.dynamic.adv.query.typehandler.SqlPlaceholder;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import org.locationtech.jts.geom.Geometry;

import java.lang.reflect.Method;

/**
 * SQL Server {@code geometry} 类型处理器。
 * <p>
 * 该实现不直接依赖 Microsoft JDBC Driver 的 {@code SQLServerGeometry} 类，
 * 以免把 JDBC 驱动变成 AdvQuery 的强制依赖；读取时通过其公开的 {@code STAsText()} 方法反射取得 WKT，
 * 写入时使用 SQL Server 原生 {@code geometry::STGeomFromText} 表达式。
 *
 * @author 张逢吉
 */
public class SqlServerGeometryAdvTypeHandler extends JtsGeometryAdvTypeHandler {

    @Override
    protected Geometry readDialectGeometry(Object value) {
        if (value == null) {
            return null;
        }
        try {
            Method stAsText = value.getClass().getMethod("STAsText");
            Object wkt = stAsText.invoke(value);
            return wkt == null ? null
                    : GirGeoTools.defaultInstance().getFormatOpt().wktToJtsGeometry(String.valueOf(wkt), true);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException(
                    "无法读取 SQL Server geometry 值，期望 JDBC 驱动对象提供 STAsText()，实际类型："
                            + value.getClass().getName(), ex);
        }
    }

    @Override
    protected Object writeGeometry(Geometry value) {
        return GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(value, true);
    }

    @Override
    public SqlPlaceholder getSqlPlaceholder(Object value) {
        if (!(value instanceof Geometry)) {
            return null;
        }
        Geometry geometry = (Geometry) value;
        int srid = geometry.getSRID() > 0 ? geometry.getSRID() : 4326;
        String wkt = (String) writeGeometry(geometry);
        if (wkt == null) {
            return null;
        }
        return new SqlPlaceholder(
                "geometry::STGeomFromText(N'" + wkt.replace("'", "''") + "', " + srid + ")", null);
    }
}
