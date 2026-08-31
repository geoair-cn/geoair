package cn.geoair.map.dynamic.adv.query.typehandler.impl;

import cn.geoair.map.dynamic.adv.query.typehandler.AdvBaseTypeHandler;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import org.locationtech.jts.geom.Geometry;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： JTS空间类型处理器抽象基类，提供公共的Geometry类型判断和格式转换工具方法。 各数据库方言需继承此类实现 {@link
 *     #readDialectGeometry(Object)} 和 {@link #writeGeometry(Geometry)}
 */
public abstract class JtsGeometryAdvTypeHandler extends AdvBaseTypeHandler<Geometry> {

    @Override
    public boolean supports(Class<?> javaType, Object value) {
        return javaType != null && Geometry.class.isAssignableFrom(javaType);
    }

    @Override
    protected Geometry convertNonNullForRead(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        Geometry geometry = readGeometry(value);
        if (geometry == null) {
            return null;
        }
        return castGeometry(geometry, javaType);
    }

    @Override
    protected Object convertNonNullForWrite(
            Geometry value, Class<?> javaType, AdvTypeHandlerContext context) {
        return writeGeometry(value);
    }

    /** 读取 Geometry：先处理通用的 String/Geometry 实例，再委托给子类的方言特定逻辑 */
    protected Geometry readGeometry(Object value) {
        // 1. 已经是 Geometry 实例
        if (value instanceof Geometry) {
            return (Geometry) value;
        }
        // 2. String → WKT → WKB → GeoJSON（所有方言通用的兜底逻辑）
        if (value instanceof String) {
            String text = (String) value;
            Geometry geometry =
                    GirGeoTools.defaultInstance().getFormatOpt().wktToJtsGeometry(text, true);
            if (geometry != null) {
                return geometry;
            }
            geometry = GirGeoTools.defaultInstance().getFormatOpt().wkbToJtsGeometry(text, true);
            if (geometry != null) {
                return geometry;
            }
            return GirGeoTools.defaultInstance().getFormatOpt().geojsonToJtsGeometry(text, true);
        }
        // 3. 方言特定的二进制格式 → 子类实现
        return readDialectGeometry(value);
    }

    /** 方言特定的二进制读取逻辑（子类实现） */
    protected abstract Geometry readDialectGeometry(Object value);

    /** 方言特定的写入逻辑（子类实现） */
    protected abstract Object writeGeometry(Geometry value);

    /** 将 Geometry 强转为目标 javaType（Point/LineString/Polygon 等子类） */
    @SuppressWarnings("unchecked")
    protected <T extends Geometry> T castGeometry(Geometry geometry, Class<?> javaType) {
        if (geometry == null || javaType == null) {
            return (T) geometry;
        }
        if (javaType.isInstance(geometry)) {
            return (T) geometry;
        }
        throw new IllegalArgumentException(
                "空间字段类型不匹配，目标类型：" + javaType.getName() + "，实际类型：" + geometry.getClass().getName());
    }
}
