package cn.geoair.base.env.property;

import cn.geoair.base.env.property.support.GirSystemPropertierOffice;
import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GkMethodHand;

public class GirPropertyHelper {

    private static volatile GiPropertier propertier;

    public static void setPropertier(GiPropertier propertier) {
        GirPropertyHelper.propertier = propertier;
    }

    // /*
    // static {
    // MethodHand.implFromClass( GirPropertyHelper.class);
    // }
    // */

    @GaMethodHandDefine(expectClassName = "cn.geoair.spi.env.SpringEnvironment4Gir")
    public static GiPropertier getPropertier() {
        GiPropertier provider = propertier;
        if (provider != null) {
            return provider;
        }
        return (GiPropertier) GkMethodHand.invokeSelf();
    }

    @GaMethodHandImpl(
            implClass = GirPropertyHelper.class,
            implMethod = "getPropertier",
            type = GaMethodHandImpl.ImplType.comity)
    protected GiPropertier _getPropertier() {
        return new GirSystemPropertierOffice().getOperater();
    }
}
