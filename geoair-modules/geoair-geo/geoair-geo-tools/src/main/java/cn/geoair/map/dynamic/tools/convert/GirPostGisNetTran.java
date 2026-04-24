package cn.geoair.map.dynamic.tools.convert;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import net.postgis.jdbc.PGgeometry;
import org.locationtech.jts.geom.Geometry;

/**
 * @author ：张逢吉
 * @date ：Created in 12:24 @description： 新版本的postgis驱动
 */
public class GirPostGisNetTran {

    public static boolean isGeometry(Object value) {
        return value instanceof PGgeometry;
    }

    public static Geometry getGeometry(Object value) {
        Geometry jtsGeom = null;
        if (value instanceof PGgeometry) { // 判断是否为pG的空间对象
            PGgeometry pgGeometry = (PGgeometry) value;
            jtsGeom =  GirGeoTools.me().getFormatOpt().pgGeometryToJtsGeometry(pgGeometry, true);
        }
        return jtsGeom;
    }

    public static PGgeometry cast(Object pgGeometry) {
        return (PGgeometry) pgGeometry;
    }

    public static Geometry toJtsGeometry(Object pgGeometry) throws Exception {
        net.postgis.jdbc.geometry.Geometry geometry = cast(pgGeometry).getGeometry();
        Geometry jtsGeom =
                GirGeoTools.me().getFormatOpt()
                        .getWKTReader()
                        .read(geometry.getTypeString() + geometry.getValue());
        jtsGeom.setSRID(geometry.getSrid());
        return jtsGeom;
    }
}
