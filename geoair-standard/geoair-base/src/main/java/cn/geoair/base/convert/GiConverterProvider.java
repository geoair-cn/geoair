package cn.geoair.base.convert;

public interface GiConverterProvider {

    public <S, T> GiConverter<S, T> getConverter(Class<S> sourceCls, Class<T> targetCls);
}
