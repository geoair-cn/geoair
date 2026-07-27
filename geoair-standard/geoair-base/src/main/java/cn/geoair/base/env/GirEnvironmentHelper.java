package cn.geoair.base.env;

import cn.geoair.base.env.support.GirSystemEnvironmentOffice;
import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GkMethodHand;

public class GirEnvironmentHelper {

    private static volatile GiEnvironmenter environmentProvider;

    public static void setEnvironmenter(GiEnvironmenter environmentProvider) {
        GirEnvironmentHelper.environmentProvider = environmentProvider;
    }

    /*
     * static { MethodHand.implFromClass( GirEnvironmentHelper.class); }
     */

    @GaMethodHandDefine(expectClassName = "cn.geoair.spi.env.SpringEnvironment4Gir")
    public static GiEnvironmenter getEnvironmenter() {
        GiEnvironmenter provider = environmentProvider;
        if (provider != null) {
            return provider;
        }
        Object o = GkMethodHand.invokeSelf();
        return (GiEnvironmenter) o;
    }

    @GaMethodHandImpl(
        implClass = GirEnvironmentHelper.class,
        implMethod = "getEnvironmenter",
        type = GaMethodHandImpl.ImplType.comity
    )
    protected GiEnvironmenter _getEnvironmenter() {
        return new GirSystemEnvironmentOffice().getOperater();
    }
}
