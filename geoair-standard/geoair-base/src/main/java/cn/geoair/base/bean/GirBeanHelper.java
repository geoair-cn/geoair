package cn.geoair.base.bean;

import cn.geoair.base.Gir;
import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GkMethodHand;

public class GirBeanHelper {

    private static volatile GiBeanFactory beanProvider;

    private GirBeanHelper() {}

    public static void setProvider(GiBeanFactory beanProvider) {
        GirBeanHelper.beanProvider = beanProvider;
    }

    static {
        GkMethodHand.implFromClass(GirBeanHelper.class);
    }

    @GaMethodHandDefine(expectClassName = "cn.geoair.spi.bean.SpringContextBean4Gir")
    public static GiBeanFactory getProvider() {
        GiBeanFactory provider = beanProvider;
        if (provider != null) {
            return provider;
        }
        return (GiBeanFactory) GkMethodHand.invokeSelf();
    }

    @GaMethodHandImpl(
            implClass = GirBeanHelper.class,
            implMethod = "getProvider",
            type = GaMethodHandImpl.ImplType.comity)
    private static GiBeanFactory _getProvider() {
        Gir.log.error("必须有工具提供容器，如spring");
        return null;
    }
}
