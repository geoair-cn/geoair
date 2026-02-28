package cn.geoair.gtc.base.convert;

public interface GirConverterFactory<S, R> {

	<T extends R> GiConverter<S, T> getConverter(Class<T> targetType);

}
