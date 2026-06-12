package cn.geoair.spi.util;

import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.base.lang.invoke.GkMethodHand;
import cn.geoair.base.util.GutilGenericType;

import org.springframework.core.GenericTypeResolver;

import java.lang.reflect.Type;

public class GenericTypeUtil4Gir {

    static {
        GkMethodHand.implFromClass(GutilGenericType.class);
    }

    @GaMethodHandImpl(
            implClass = GutilGenericType.class,
            implMethod = "resolveTypeArguments",
            type = ImplType.expectfirst)
    private static Type[] resolveTypeArguments(Class<?> clazz, Class<?> genericIfc) {
        return GenericTypeResolver.resolveTypeArguments(clazz, genericIfc);
    }
}
