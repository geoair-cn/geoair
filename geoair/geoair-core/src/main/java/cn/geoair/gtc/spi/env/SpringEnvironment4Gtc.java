package cn.geoair.gtc.spi.env;


import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import cn.geoair.gtc.base.env.GiEnvironmenter;
import cn.geoair.gtc.base.env.GtcEnvironmentHelper;
import cn.geoair.gtc.base.env.property.GtcPropertyHelper;
import cn.geoair.gtc.base.env.property.GiPropertier;
import cn.geoair.gtc.base.env.support.GtcSystemEnvironmentOperater;
import cn.geoair.gtc.base.lang.invoke.GkMethodHand;
import cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl;
import  cn.geoair.gtc.base.lang.invoke.GaMethodHandImpl.ImplType;

/**
 * 读取配置文件
 */
@Component
public class SpringEnvironment4Gtc implements GiPropertier,GiEnvironmenter, EnvironmentAware {


	static {
		GkMethodHand.implFromClass(SpringEnvironment4Gtc.class);
	}

	@GaMethodHandImpl(implClass= GtcPropertyHelper.class,implMethod="getPropertier",type=ImplType.expectfirst)
	private static GiPropertier getPropertier() {
		return me;
	}

	@GaMethodHandImpl(implClass= GtcEnvironmentHelper.class,implMethod="getEnvironmenter",type=ImplType.expectfirst)
	private static GiEnvironmenter getEnvironmenter() {
		return me;
	}

	protected static SpringEnvironment4Gtc me;
    protected static Environment environment;

    @Override
	public void setEnvironment(Environment evn) {
    	me = this;
    	environment = evn;

	}

    public static Environment getEnvironment() {
    	return environment;
    }

	@Override
	public boolean containsProperty(String key) {
		return getEnvironment().containsProperty(key);
	}

	@Override
	public String getProperty(String key) {
		return getEnvironment().getProperty(key);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return getEnvironment().getProperty(key, defaultValue);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType) {
		return getEnvironment().getProperty(key, targetType);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		return getEnvironment().getProperty(key, targetType,defaultValue);
	}

	@Override
	public String getRequiredProperty(String key) throws IllegalStateException {
		return getEnvironment().getRequiredProperty(key);
	}

	@Override
	public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
		return getEnvironment().getRequiredProperty(key, targetType);
	}

	@Override
	public String resolvePlaceholders(String text) {
		return getEnvironment().resolvePlaceholders(text);
	}

	@Override
	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
		return getEnvironment().resolveRequiredPlaceholders(text);
	}




	//env
	@Override
	public String[] getActiveProfiles() {
		return getEnvironment().getActiveProfiles();
	}

	@Override
	public String[] getDefaultProfiles() {
		return getEnvironment().getDefaultProfiles();
	}

	@Override
	public boolean isDev() {
		return false;
	}

	@Override
	public boolean isDebugger() {
		return  GtcSystemEnvironmentOperater.isDebug();
	}

}
