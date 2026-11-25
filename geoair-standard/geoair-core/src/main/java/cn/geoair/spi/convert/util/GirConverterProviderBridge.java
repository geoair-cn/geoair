package cn.geoair.spi.convert.util;

import cn.geoair.base.convert.GiConverter;
import cn.geoair.base.convert.GiConverterProvider;
import cn.geoair.base.convert.support.GirConverterImpl;
import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.base.lang.invoke.GkMethodHand;

public class GirConverterProviderBridge {

    static {
        GkMethodHand.implFromClass(GirConverterProviderBridge.class);
        cn.geoair.base.convert.util.GirConvertHelper.setProvider(getProvider());
    }

    @GaMethodHandDefine()
    public static GiConverterProvider getProvider() {
        return (GiConverterProvider) GkMethodHand.invokeSelf();
    }

    @GaMethodHandImpl(
            implClass = GirConverterProviderBridge.class,
            implMethod = "getProvider",
            type = ImplType.comity)
    private static GiConverterProvider _getProvider() {
        return new GirConverterImpl();
    }

    public static <S, T> T convert(S source, Class<T> targetClass) {

        Class<S> sc = (Class<S>) source.getClass();

        GiConverter<S, T> converter =
                GirConverterProviderBridge.getProvider().getConverter(sc, targetClass);
        return converter.convert(source);
    }
}
