package cn.geoair.map.dynamic.adv.query.typehandler.impl;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.convert.GirOracleSpatialTran;
import cn.geoair.map.dynamic.tools.convert.GirOracleTran;
import org.locationtech.jts.geom.Geometry;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/8/11
 * @description： Oracle Spatial 空间类型处理器 —— SDO_GEOMETRY ↔ JTS Geometry
 */
public class OracleGeometryAdvTypeHandler extends JtsGeometryAdvTypeHandler {

    @Override
    protected Geometry readDialectGeometry(Object value) {
        if (GirOracleSpatialTran.isSdoGeometry(value)) {
            return GirOracleSpatialTran.sdoGeometryToJtsGeom(value);
        }
        return null;
    }

    @Override
    protected Object writeGeometry(Geometry value) {
        String wkt = GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(value, true);
        if (wkt == null) {
            return null;
        }
        if (GirOracleTran.isOracleSpatialAvailable()) {
            return GirOracleSpatialTran.jtsGeomToWkt(value);
        }
        return wkt;
    }
}
