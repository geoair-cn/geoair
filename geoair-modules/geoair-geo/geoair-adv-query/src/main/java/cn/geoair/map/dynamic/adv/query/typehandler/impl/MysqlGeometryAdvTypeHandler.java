package cn.geoair.map.dynamic.adv.query.typehandler.impl;

import cn.geoair.map.dynamic.adv.query.typehandler.SqlPlaceholder;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.convert.GirMysqlTran;

import org.locationtech.jts.geom.Geometry;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/8/11
 * @description： MySQL 空间类型处理器 —— MySQL binary ↔ JTS Geometry
 *     <p>MySQL JDBC 驱动原生支持 WKT 字符串写入 GEOMETRY 列， 读取时通过 GirMysqlTran 解析 MySQL 的二进制几何格式
 */
public class MysqlGeometryAdvTypeHandler extends JtsGeometryAdvTypeHandler {

    @Override
    protected Geometry readDialectGeometry(Object value) {
        if (GirMysqlTran.isGeomValue(value)) {
            return GirMysqlTran.mysqlBinaryToJtsGeom(value);
        }
        return null;
    }

    @Override
    protected Object writeGeometry(Geometry value) {
        return GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(value, true);
    }

    @Override
    public SqlPlaceholder getSqlPlaceholder(Object value) {
        if (value instanceof Geometry) {
            Geometry geom = (Geometry) value;
            int srid = geom.getSRID();
            if (srid <= 0) srid = 4326;
            String wkt = (String) writeGeometry(geom);
            return new SqlPlaceholder(
                    "ST_GeomFromText('"
                            + wkt.replace("'", "''")
                            + "', "
                            + srid
                            + ", 'axis-order=long-lat')",
                    null);
        }
        return null;
    }
}
