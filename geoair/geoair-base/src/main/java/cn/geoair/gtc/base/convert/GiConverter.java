package cn.geoair.gtc.base.convert;

import cn.geoair.gtc.base.util.GutilAssert;

@FunctionalInterface
public interface GiConverter<S, T> {

	T convert(S source);


	default <U> GiConverter<S, U> andThen(GiConverter<? super T, ? extends U> after) {
		GutilAssert.notNull(after, ()->"After  gtcConverter must not be null");
		return (S s) -> {
			T initialResult = convert(s);
			return (initialResult != null ? after.convert(initialResult) : null);
		};
	}
}
