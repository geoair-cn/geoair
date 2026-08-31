package cn.geoair.spi.test;

import cn.geoair.base.env.GiEnvironmenter;
import cn.geoair.base.env.property.GiPropertier;
import cn.geoair.spi.env.SpringEnvironment4Gir;

/** SpringEnvironment4Gir 入口示例 */
public class SpringEnvironment4GirExample {

    public static void main(String[] args) {
        SpringEnvironment4Gir env = new SpringEnvironment4Gir();

        GiPropertier propertier = env;
        GiEnvironmenter environmenter = env;

        System.out.println("containsProfile(dev) = " + environmenter.containsProfile("dev"));
        System.out.println("isDebugger = " + environmenter.isDebugger());
        System.out.println(
                "getProperty(not-exists, default) = "
                        + propertier.getProperty("not-exists", "default-value"));
    }
}
