package cn.geoair.gtc.base.env.property.support;

import cn.geoair.gtc.base.convert.util.GtcConvertHelper;
import cn.geoair.gtc.base.util.GutilStr;
import cn.geoair.gtc.base.env.property.GiPropertier;

public class GtcSystemPropertyOperater implements GiPropertier{

	@Override
	public boolean containsProperty(String key) {
		return System.getProperties().containsKey(key);
	}

	@Override
	public String getProperty(String key) {
		return System.getProperty(key);
	}

	@Override
	public String getProperty(String key, String defaultValue) {

		return System.getProperty(key, defaultValue);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType) {
		String value = getProperty(key);
		if(GutilStr.hasText(value)) {
			return  GtcConvertHelper.convert(value,targetType);
		}

		return null;
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		T res = getProperty(key,targetType);
		if(res == null) {
			res = defaultValue;
		}
		return res;
	}

	@Override
	public String getRequiredProperty(String key) throws IllegalStateException {
		String res = getProperty(key);
		if(res == null) {
			throw new IllegalStateException("找不到属性名为:<"+key+">的属性");
		}
		return res;
	}

	@Override
	public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
		T res = getProperty(key,targetType);
		if(res == null) {
			throw new IllegalStateException("找不到属性名为:<"+key+">的属性");
		}
		return res;
	}

	@Override
	public String resolvePlaceholders(String text) {
		// TODO Auto-generated method stub
		return text;
	}

	@Override
	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return text;
	}






}
