package cn.geoair.spi.util;

import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.base.lang.invoke.GkMethodHand;
import cn.geoair.base.util.GutilGenericType;
import java.lang.reflect.Type;
import org.springframework.core.GenericTypeResolver;

public class SpringGenericTypeBridge {

    static {
        GkMethodHand.implFromClass(SpringGenericTypeBridge.class);
        GutilGenericType.setGenericTypeProvider(SpringGenericTypeBridge::resolveTypeArguments);
    }

    @GaMethodHandImpl(
        implClass = GutilGenericType.class,
        implMethod = "resolveTypeArguments",
        type = ImplType.expectfirst
    )
    private static Type[] resolveTypeArguments(Class<?> clazz, Class<?> genericIfc) {
        return GenericTypeResolver.resolveTypeArguments(clazz, genericIfc);
    }
}
