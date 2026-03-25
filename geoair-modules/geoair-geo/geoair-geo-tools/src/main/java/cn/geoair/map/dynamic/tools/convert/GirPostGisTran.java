package cn.geoair.map.dynamic.tools.convert;

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
}
