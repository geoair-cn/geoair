package cn.geoair.map.dynamic.tools.convert;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.hutool.core.util.StrUtil;
import org.locationtech.jts.geom.Geometry;
import org.postgresql.util.PGobject;

/**
 * @author ：张逢吉
 * @date ：Created in 12:24 @description： 判断当前的postgis驱动是什么
 */
public class GirPostGisTran {

    private static Boolean isNetConvert;

    public static boolean isNetConvert() {
        if (isNetConvert == null) {
            try {
                Class.forName("net.postgis.jdbc.PGgeometry");
                isNetConvert = true;
            } catch (ClassNotFoundException e) {
                isNetConvert = false;
            }
        }
        return isNetConvert;
    }

    private static Boolean isOrgConvert;

    public static boolean isOrgConvert() {
        if (isOrgConvert == null) {
            try {
                Class.forName("org.postgis.PGgeometry");
                isOrgConvert = true;
            } catch (ClassNotFoundException e) {
                isOrgConvert = false;
            }
        }
        return isOrgConvert;
    }

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
            jtsGeom = GirAdvTools.getFormatOpt().wkbToJtsGeometry(StrUtil.toString(pObject), true);

        }
        return jtsGeom;
    }
}
