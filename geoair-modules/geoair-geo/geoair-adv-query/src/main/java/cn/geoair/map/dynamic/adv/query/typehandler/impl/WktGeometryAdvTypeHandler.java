package cn.geoair.map.dynamic.adv.query.typehandler.impl;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import org.locationtech.jts.geom.Geometry;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/8/11
 * @description： 达梦 / 通用 WKT 空间类型处理器 —— WKT 字符串 ↔ JTS Geometry
 *     <p>用于达梦数据库以及其他不提供原生几何二进制驱动的数据库。 读写均通过 WKT (Well-Known Text) 字符串，JDBC 驱动原生兼容
 */
public class WktGeometryAdvTypeHandler extends JtsGeometryAdvTypeHandler {

    @Override
    protected Geometry readDialectGeometry(Object value) {
        // 达梦等数据库几何字段通常以 WKT/WKB 字符串返回，已由父类 readGeometry() 处理
        return null;
    }

    @Override
    protected Object writeGeometry(Geometry value) {
        // 达梦等数据库直接写入 WKT 字符串，JDBC 原生兼容
        return GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(value, true);
    }
}
