package cn.geoair.map.dynamic.adv.query.typehandler.impl;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.convert.GirMysqlTran;
import cn.hutool.core.util.StrUtil;
import org.locationtech.jts.geom.Geometry;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/8/11
 * @description： MySQL 空间类型处理器 —— MySQL binary ↔ JTS Geometry
 * <p>MySQL JDBC 驱动原生支持 WKT 字符串写入 GEOMETRY 列，
 * 读取时通过 GirMysqlTran 解析 MySQL 的二进制几何格式</p>
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
        // MySQL JDBC 驱动原生支持 WKT 字符串写入 GEOMETRY 列，配合 getSqlPlaceholder 使用
        return GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(value, true);
    }

    @Override
    public String getSqlPlaceholder(Object value) {
        if (value instanceof Geometry) {
            Geometry geom = (Geometry) value;
            int srid = geom.getSRID();
            if (srid <= 0) srid = 4326;
            return StrUtil.format("ST_GeomFromText(\"{}\",  {}, 'axis-order=long-lat')","?",srid);
        }
        return null;
    }
}
