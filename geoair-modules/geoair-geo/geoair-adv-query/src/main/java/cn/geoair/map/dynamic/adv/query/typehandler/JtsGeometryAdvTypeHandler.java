package cn.geoair.map.dynamic.adv.query.typehandler;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.convert.GirMysqlTran;
import cn.geoair.map.dynamic.tools.convert.GirOracleSpatialTran;
import cn.geoair.map.dynamic.tools.convert.GirOracleTran;
import cn.geoair.map.dynamic.tools.convert.GirPostGisJdbcTran;
import cn.geoair.map.dynamic.tools.convert.GirPostGisNetTran;
import cn.geoair.map.dynamic.tools.convert.GirPostGisOrgTran;
import cn.geoair.map.dynamic.tools.convert.GirPostGisTran;
import org.locationtech.jts.geom.Geometry;
import org.postgresql.util.PGobject;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： JTS空间类型处理器
 */
public class JtsGeometryAdvTypeHandler extends AdvBaseTypeHandler<Geometry> {

    @Override
    public boolean supports(Class<?> javaType, Object value) {
        return javaType != null && Geometry.class.isAssignableFrom(javaType);
    }

    @Override
    protected Geometry convertNonNullForRead(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        Geometry geometry = toGeometry(value);
        if (geometry == null) {
            return null;
        }
        return castGeometry(geometry, javaType);
    }

    @Override
    protected Object convertNonNullForWrite(
            Geometry value, Class<?> javaType, AdvTypeHandlerContext context) {
        String wkt = GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(value, true);
        if (wkt == null) {
            return null;
        }
        if (GirPostGisTran.isNetConvert()) {
            try {
                net.postgis.jdbc.PGgeometry pgGeometry = new net.postgis.jdbc.PGgeometry();
                pgGeometry.setValue(wkt);
                pgGeometry.setType("geometry");
                if (value.getSRID() != 0 && pgGeometry.getGeometry() != null) {
                    pgGeometry.getGeometry().setSrid(value.getSRID());
                }
                return pgGeometry;
            } catch (Exception e) {
                return wkt;
            }
        }
        if (GirPostGisTran.isOrgConvert()) {
            try {
                org.postgis.PGgeometry pgGeometry = new org.postgis.PGgeometry();
                pgGeometry.setValue(wkt);
                pgGeometry.setType("geometry");
                if (value.getSRID() != 0 && pgGeometry.getGeometry() != null) {
                    pgGeometry.getGeometry().setSrid(value.getSRID());
                }
                return pgGeometry;
            } catch (Exception e) {
                return wkt;
            }
        }
        if (GirOracleTran.isOracleSpatialAvailable()) {
            return GirOracleSpatialTran.jtsGeomToWkt(value);
        }
        return wkt;
    }

    private Geometry toGeometry(Object value) {
        if (value instanceof Geometry) {
            return (Geometry) value;
        }
        if (value instanceof String) {
            String text = (String) value;
            Geometry geometry = GirGeoTools.defaultInstance().getFormatOpt().wktToJtsGeometry(text, true);
            if (geometry != null) {
                return geometry;
            }
            geometry = GirGeoTools.defaultInstance().getFormatOpt().wkbToJtsGeometry(text, true);
            if (geometry != null) {
                return geometry;
            }
            return GirGeoTools.defaultInstance().getFormatOpt().geojsonToJtsGeometry(text, true);
        }
        if (value instanceof PGobject) {
            Geometry geometry = GirPostGisJdbcTran.pGobjectToJts(value);
            if (geometry != null) {
                return geometry;
            }
        }
        if (GirPostGisOrgTran.isGeometry(value)) {
            return GirPostGisOrgTran.getGeometry(value);
        }
        if (GirPostGisNetTran.isGeometry(value)) {
            return GirPostGisNetTran.getGeometry(value);
        }
        if (GirMysqlTran.isGeomValue(value)) {
            return GirMysqlTran.mysqlBinaryToJtsGeom(value);
        }
        if (GirOracleSpatialTran.isSdoGeometry(value)) {
            return GirOracleSpatialTran.sdoGeometryToJtsGeom(value);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Geometry> T castGeometry(Geometry geometry, Class<?> javaType) {
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
