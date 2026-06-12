package cn.geoair.map.dynamic.tools.convert;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.hutool.core.util.StrUtil;
import org.locationtech.jts.geom.Geometry;
import org.postgresql.util.PGobject;

/**
 * @author ：张逢吉
 * @date ：Created in 12:24 @description： 新版本的postgis驱动
 */
public class GirPostGisJdbcTran {

    public static boolean isPGobject(Object value) {
        if (value instanceof PGobject) { // PGobject 是 PGgeometry的父类
            return true;
        }
        return false;
    }

    public static Geometry pGobjectToJts(Object value) {
        Geometry jtsGeom = null;
        if (value instanceof PGobject) { // PGobject 是 PGgeometry的父类
            /**
             * 若JDBCURL上面显示指定 currentSchema=onemap_tile_builder 。 然而空间类型的元数据（如类型定义）仅存在于 public 中，
             * 驱动在 onemap_tile_builder 下找不到对应的类型定义， 就无法将其识别为 PgGeom， 只能降级为通用的 PgObject 类型。
             */
            PGobject pObject = (PGobject) value;
            jtsGeom =
                    GirGeoTools.defaultInstance()
                            .getFormatOpt()
                            .wkbToJtsGeometry(StrUtil.toString(pObject), true);
        }
        return jtsGeom;
    }
}
