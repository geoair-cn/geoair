package cn.geoair.gtc.spi.util;


import java.lang.reflect.Type;

import org.springframework.core.GenericTypeResolver;

import cn.geoair.gtc.base.lang.invoke.GkMethodHand;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import  cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.gtc.base.util.GutilGenericType;

public class GenericTypeUtil4Gir {


	static {
		GkMethodHand.implFromClass(GutilGenericType.class);
	}

	@GaMethodHandImpl(implClass = GutilGenericType.class, implMethod = "resolveTypeArguments", type = ImplType.expectfirst)
	private static Type[] resolveTypeArguments(Class<?> clazz,Class<?> genericIfc) {
		return GenericTypeResolver.resolveTypeArguments(clazz, genericIfc);
	}
}
