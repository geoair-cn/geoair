package cn.geoair.gtc.base.data.result;

@FunctionalInterface
public interface GiResultConverter<S, T> {

	GiResult<T> convert(S source);

}
