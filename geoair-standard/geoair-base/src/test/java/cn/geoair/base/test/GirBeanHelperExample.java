package cn.geoair.base.test;

import cn.geoair.base.bean.GirBeanHelper;

/**
 * GirBeanHelper 入口示例
 */
public class GirBeanHelperExample {

    public static void main(String[] args) {
        System.out.println("provider = " + GirBeanHelper.getProvider());
        System.out.println("如果没有容器实现，这里会走默认兜底逻辑。");
    }
}
