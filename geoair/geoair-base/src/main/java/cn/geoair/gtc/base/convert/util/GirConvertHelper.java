package cn.geoair.gtc.base.convert.util;

import cn.geoair.gtc.base.convert.support.GirConverterImpl;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.gtc.base.lang.invoke.GkMethodHand;
import cn.geoair.gtc.base.convert.GiConverter;
import cn.geoair.gtc.base.convert.GiConverterProvider;

public class GirConvertHelper {

	static {
		GkMethodHand.implFromClass(GirConvertHelper.class);
	}

	@GaMethodHandDefine()
	public static GiConverterProvider getProvider() {
		return (GiConverterProvider) GkMethodHand.invokeSelf();
	}

	@GaMethodHandImpl(implClass = GirConvertHelper.class, implMethod = "getProvider",
			type = GaMethodHandImpl.ImplType.comity)
	private static GiConverterProvider _getProvider() {
		return new GirConverterImpl();
	}

	public static <S, T> T convert(S source, Class<T> targetClass) {

		Class<S> sc = (Class<S>) source.getClass();

		GiConverter<S, T> converter = GirConvertHelper.getProvider().getConverter(sc, targetClass);
		return converter.convert(source);
	}

}
