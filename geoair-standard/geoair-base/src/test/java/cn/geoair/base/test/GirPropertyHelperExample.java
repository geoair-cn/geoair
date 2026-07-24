package cn.geoair.base.test;

import cn.geoair.base.env.property.GiPropertier;
import cn.geoair.base.env.property.GirPropertyHelper;

/**
 * GirPropertyHelper 入口示例
 */
public class GirPropertyHelperExample {

    public static void main(String[] args) {
        GiPropertier propertier = GirPropertyHelper.getPropertier();
        String javaVersion = propertier.getProperty("java.version", "unknown");

        System.out.println("propertier = " + propertier.getClass().getName());
        System.out.println("java.version = " + javaVersion);
    }
}
