package cn.geoair.spi.env;

import cn.geoair.base.env.GiEnvironmenter;
import cn.geoair.base.env.GirEnvironmentHelper;
import cn.geoair.base.env.property.GiPropertier;
import cn.geoair.base.env.property.GirPropertyHelper;
import cn.geoair.base.env.support.GirSystemEnvironmentOperater;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.base.lang.invoke.GkMethodHand;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** 读取配置文件 */
@Component
public class SpringEnvironment4Gir
        implements GiPropertier,
                GiEnvironmenter,
                ApplicationContextAware,
                InitializingBean,
                BeanFactoryPostProcessor {

    static {
        GkMethodHand.implFromClass(SpringEnvironment4Gir.class);
    }

    @GaMethodHandImpl(
            implClass = GirPropertyHelper.class,
            implMethod = "getPropertier",
            type = ImplType.expectfirst)
    private static GiPropertier getPropertier() {
        return SpringEnvironmentProviderResolver.getProvider();
    }

    @GaMethodHandImpl(
            implClass = GirEnvironmentHelper.class,
            implMethod = "getEnvironmenter",
            type = ImplType.expectfirst)
    private static GiEnvironmenter getEnvironmenter() {
        return SpringEnvironmentProviderResolver.getProvider();
    }

    protected static SpringEnvironment4Gir me;

    private static ConfigurableListableBeanFactory beanFactory;

    /** Spring应用上下文环境 */
    private static ApplicationContext applicationContext;

    public static Environment getEnvironment() {
        return applicationContext.getEnvironment();
    }

    @Override
    public boolean containsProperty(String key) {
        return getEnvironment().containsProperty(key);
    }

    @Override
    public String getProperty(String key) {
        if (null == applicationContext) {
            return null;
        }
        return applicationContext.getEnvironment().getProperty(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        if (null == applicationContext) {
            return null;
        }
        return applicationContext.getEnvironment().getProperty(key, defaultValue);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType) {
        if (null == applicationContext) {
            return null;
        }
        return applicationContext.getEnvironment().getProperty(key, targetType);
    }

    @Override
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        if (null == applicationContext) {
            return null;
        }
        return applicationContext.getEnvironment().getProperty(key, targetType, defaultValue);
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

    // env
    @Override
    public String[] getActiveProfiles() {
        return getEnvironment().getActiveProfiles();
    }

    @Override
    public String[] getDefaultProfiles() {
        return getEnvironment().getDefaultProfiles();
    }

    @Override
    public boolean containsProfile(String profile) {
        String[] activeProfiles = getActiveProfiles();
        if (activeProfiles == null || activeProfiles.length == 0) {
            return false;
        }
        return Arrays.asList(activeProfiles).contains(profile);
    }

    @Override
    public boolean isDev() {
        return false;
    }

    @Override
    public boolean isDebugger() {
        return GirSystemEnvironmentOperater.isDebug();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        me = this;
        GirPropertyHelper.setPropertier(this);
        GirEnvironmentHelper.setEnvironmenter(this);
        SpringEnvironmentProviderResolver.setProvider(this);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        me = this;
        GirPropertyHelper.setPropertier(this);
        GirEnvironmentHelper.setEnvironmenter(this);
        SpringEnvironmentProviderResolver.setProvider(this);
        SpringEnvironment4Gir.applicationContext = applicationContext;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        me = this;
        GirPropertyHelper.setPropertier(this);
        GirEnvironmentHelper.setEnvironmenter(this);
        SpringEnvironmentProviderResolver.setProvider(this);
        SpringEnvironment4Gir.beanFactory = beanFactory;
    }
}
