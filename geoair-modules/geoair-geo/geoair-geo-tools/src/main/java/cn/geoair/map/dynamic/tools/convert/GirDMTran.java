package cn.geoair.map.dynamic.tools.convert;

/**
 * 达梦数据库环境检测工具类。
 *
 * <p>通过 {@code Class.forName()} 检测达梦 JDBC 驱动和空间扩展是否在 classpath 上， 结果懒加载缓存，不引入任何达梦驱动的编译期依赖。
 *
 * @author zhangjun
 */
public class GirDMTran {

    private static final String DM_DRIVER_CLASS = "dm.jdbc.driver.DmdbStruct";
    private static final String DM_GEO_UTIL_CLASS = "com.dameng.geotools.util.DmGeo2Util";

    private static Boolean dmDriverAvailable;
    private static Boolean dmGeoUtilAvailable;

    /** 达梦 JDBC 驱动是否可用（{@code dm.jdbc.driver.DmdbStruct} 是否可加载） */
    public static boolean isDmDriverAvailable() {
        if (dmDriverAvailable == null) {
            dmDriverAvailable = isClassAvailable(DM_DRIVER_CLASS);
        }
        return dmDriverAvailable;
    }

    /** 达梦空间扩展工具是否可用（{@code com.dameng.geotools.util.DmGeo2Util} 是否可加载） */
    public static boolean isDmGeoUtilAvailable() {
        if (dmGeoUtilAvailable == null) {
            dmGeoUtilAvailable = isClassAvailable(DM_GEO_UTIL_CLASS);
        }
        return dmGeoUtilAvailable;
    }

    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
