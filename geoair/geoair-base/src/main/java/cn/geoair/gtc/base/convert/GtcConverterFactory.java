package cn.geoair.gtc.base.convert;


public interface GtcConverterFactory<S, R> {


    <T extends R> GiConverter<S, T> getConverter(Class<T> targetType);

}
