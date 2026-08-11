package cn.geoair.map.dynamic.adv.query.typehandler.impl;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.convert.GirPostGisJdbcTran;
import cn.geoair.map.dynamic.tools.convert.GirPostGisNetTran;
import cn.geoair.map.dynamic.tools.convert.GirPostGisOrgTran;
import cn.geoair.map.dynamic.tools.convert.GirPostGisTran;
import org.locationtech.jts.geom.Geometry;
import org.postgresql.util.PGobject;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/8/11
 * @description： PostgreSQL/PostGIS 空间类型处理器 —— PGgeometry / PGobject ↔ JTS Geometry
 */
public class PostGisGeometryAdvTypeHandler extends JtsGeometryAdvTypeHandler {

    @Override
    protected Geometry readDialectGeometry(Object value) {
        // PostgreSQL JDBC 驱动返回 PGobject
        if (value instanceof PGobject) {
            Geometry geometry = GirPostGisJdbcTran.pGobjectToJts(value);
            if (geometry != null) {
                return geometry;
            }
        }
        // PostGIS org 驱动几何对象
        if (GirPostGisOrgTran.isGeometry(value)) {
            return GirPostGisOrgTran.getGeometry(value);
        }
        // PostGIS net 驱动几何对象
        if (GirPostGisNetTran.isGeometry(value)) {
            return GirPostGisNetTran.getGeometry(value);
        }
        return null;
    }

    @Override
    protected Object writeGeometry(Geometry value) {
        String wkt = GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(value, true);
        if (wkt == null) {
            return null;
        }
        // 优先使用 net 驱动
        if (GirPostGisTran.isNetConvert()) {
            try {
                return GirPostGisNetTran.toPGGeometry(value);
            } catch (Exception e) {
                return wkt;
            }
        }
        // 其次 org 驱动
        if (GirPostGisTran.isOrgConvert()) {
            try {
                return GirPostGisOrgTran.toPGGeometry(value);
            } catch (Exception e) {
                return wkt;
            }
        }
        // PG 也兼容纯 WKT 字符串
        return wkt;
    }
}
