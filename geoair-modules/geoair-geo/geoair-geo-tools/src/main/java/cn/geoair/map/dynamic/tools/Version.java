package cn.geoair.map.dynamic.tools;

import cn.geoair.base.Gir;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import java.util.Set;

/**
 * geo-tools 模块的版本与工具类发现入口。
 *
 * @author 张逢吉
 */
public class Version {

    /** 当前构建标识。 */
    private static final String VERSION = "J8.1.6";

    /**
     * 获取当前模块版本标识。
     *
     * @return 模块版本
     */
    public static String getVersion() {
        return VERSION;
    }

    public static void main(String[] args) {
        Gir.log.info("Current version: " + VERSION);
    }

    /**
     * 扫描本模块包下以 {@code Utils} 结尾的非接口类型。
     *
     * <p>结果依赖运行期类路径扫描，不保证顺序，也不表示这些类型都适合直接实例化。
     *
     * @return 扫描到的工具实现类集合
     */
    public static Set<Class<?>> getAllUtils() {
        return ClassUtil.scanPackage(
                "cn.geoair.map.dynamic.tools",
                (clazz) -> !clazz.isInterface() && StrUtil.endWith(clazz.getSimpleName(), "Utils"));
    }
}
