package cn.geoair.base.env.property;

import cn.geoair.base.env.property.support.GirSystemPropertierOffice;
import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GkMethodHand;

public class GirPropertyHelper {

    // /*
    // static {
    // MethodHand.implFromClass( GirPropertyHelper.class);
    // }
    // */

    @GaMethodHandDefine(expectClassName = "cn.geoair.spi.env.SpringEnvironment4Gir")
    public static GiPropertier getPropertier() {
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
