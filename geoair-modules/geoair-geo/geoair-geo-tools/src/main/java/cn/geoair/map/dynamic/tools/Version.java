package cn.geoair.map.dynamic.tools;

import cn.geoair.base.Gir;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Set;

public class Version {

    private static final String VERSION = "J17.1.6";

    public static String getVersion() {
        return VERSION;
    }

    public static void main(String[] args) {
        Gir.log.info("Current version: " + VERSION);
    }

    public static Set<Class<?>> getAllUtils() {
        return ClassUtil.scanPackage(
                "cn.geoair.map.dynamic.tools",
                (clazz) -> !clazz.isInterface() && StrUtil.endWith(clazz.getSimpleName(), "Utils"));
    }
}
