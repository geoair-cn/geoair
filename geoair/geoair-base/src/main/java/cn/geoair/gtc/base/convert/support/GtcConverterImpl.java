package cn.geoair.gtc.base.convert.support;

import cn.geoair.gtc.base.convert.GiConverter;
import cn.geoair.gtc.base.convert.GiConverterProvider;

public class GtcConverterImpl implements GiConverterProvider{

	@Override
	public <S, T> GiConverter<S, T> getConverter(Class<S> sourceCls, Class<T> targetCls) {
		return new SimpleConverter<S,T>(sourceCls,targetCls);
	}

	class SimpleConverter<S,T> implements GiConverter<S,T>{

		private Class<S> sourceCls;
		private Class<T> targetCls;

		public SimpleConverter(Class<S> sCls, Class<T> tCls) {
			this.sourceCls = sCls;
			this.targetCls = tCls;
		}

		@Override
		public T convert(S source) {

			if(source != null) {

				if(source instanceof String) {




				}


			}

			return null;
		}

		public Class<S> getSourceCls() {
			return sourceCls;
		}

		public Class<T> getTargetCls() {
			return targetCls;
		}

	}

}
