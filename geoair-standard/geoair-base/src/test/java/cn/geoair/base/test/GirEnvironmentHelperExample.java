package cn.geoair.base.test;

import cn.geoair.base.env.GiEnvironmenter;
import cn.geoair.base.env.GirEnvironmentHelper;

/**
 * GirEnvironmentHelper 入口示例
 */
public class GirEnvironmentHelperExample {

    public static void main(String[] args) {
        GiEnvironmenter environmenter = GirEnvironmentHelper.getEnvironmenter();

        System.out.println("environmenter = " + environmenter.getClass().getName());
        System.out.println("containsProfile(dev) = " + environmenter.containsProfile("dev"));
        System.out.println("isDebugger = " + environmenter.isDebugger());
    }
}
