package cn.geoair.map.dynamic.adv.query.typehandler.impl;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.convert.GirDMSpatialTran;
import cn.geoair.map.dynamic.tools.convert.GirDMTran;

import org.locationtech.jts.geom.Geometry;

/**
 * 达梦数据库空间类型处理器。
 *
 * <p>读取：通过反射解析 {@code dm.jdbc.driver.DmdbStruct} → Gserialized → WKB → JTS Geometry。 写入：转为 WKT
 * 字符串，JDBC 原生兼容。
 *
 * @author zhangjun
 */
public class DmGeometryAdvTypeHandler extends JtsGeometryAdvTypeHandler {

    @Override
    protected Geometry readDialectGeometry(Object value) {
        if (!isDmdbStruct(value)) {
            return null;
        }
        if (GirDMTran.isDmDriverAvailable() && GirDMSpatialTran.isDmdbStruct(value)) {
            return GirDMSpatialTran.dmStructToJtsGeom(value);
        }
        return null;
    }

    @Override
    protected Object writeGeometry(Geometry value) {
        return GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(value, true);
    }

    /** 判断是否为达梦 DmdbStruct 类型（通过类名判断，避免编译期依赖达梦驱动） */
    private boolean isDmdbStruct(Object obj) {
        return obj != null && "dm.jdbc.driver.DmdbStruct".equals(obj.getClass().getName());
    }
}
