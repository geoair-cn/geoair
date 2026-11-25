package cn.geoair.spi.bean;

import cn.geoair.base.bean.GiBeanFactory;
import cn.geoair.base.bean.GirBeanDefinitionStoreException;
import cn.geoair.base.bean.GirBeanException;
import cn.geoair.base.bean.GirBeanHelper;
import cn.geoair.base.bean.GirBeanNotOfRequiredTypeException;
import cn.geoair.base.bean.GirNoSuchBeanException;
import cn.geoair.base.bean.GirNoUniqueBeanException;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.base.lang.invoke.GkMethodHand;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilClass;
import cn.hutool.core.exceptions.UtilException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 由spring提供bean容器
 *
 * @author Ray
 */
@Component
public class SpringContextBean4Gir
        implements GiBeanFactory,
                ApplicationContextAware,
                BeanFactoryPostProcessor,
                InitializingBean {

    static {
        GkMethodHand.implFromClass(SpringContextBean4Gir.class);
    }

    protected final GiLogger logger = GirLoggerFactory.getLogger(SpringContextBean4Gir.class);

    @GaMethodHandImpl(
            implClass = GirBeanHelper.class,
            implMethod = "getProvider",
            type = ImplType.expectfirst)
    private static GiBeanFactory getProvider() {
        return SpringBeanProviderResolver.getProvider();
    }

    private static ApplicationContext springContext;

    private static GiBeanFactory beanProvider;

    /**
     * "@PostConstruct"注解标记的类中，由于ApplicationContext还未加载，导致空指针
     * 因此实现BeanFactoryPostProcessor注入ConfigurableListableBeanFactory实现bean的操作
     */
    private static ConfigurableListableBeanFactory beanFactory;

    static GiBeanFactory getCurrentProvider() {
        return beanProvider;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        beanProvider = this;
        GirBeanHelper.setProvider(this);
        SpringBeanProviderResolver.setProvider(this);
        SpringContextBean4Gir.beanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        beanProvider = this;
        GirBeanHelper.setProvider(this);
        SpringBeanProviderResolver.setProvider(this);
        springContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return springContext;
    }

    @Override
    public boolean containsBean(String name) {
        return getBeanFactory().containsBean(name);
    }

    @Override
    public boolean isSingleton(String name) {
        try {
            return getBeanFactory().isSingleton(name);
        } catch (NoSuchBeanDefinitionException nex) {
            throw new GirNoSuchBeanException(nex);
        }
    }

    @Override
    public boolean isPrototype(String name) {
        try {
            return getBeanFactory().isPrototype(name);
        } catch (NoSuchBeanDefinitionException nex) {
            throw new GirNoSuchBeanException(nex);
        }
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) {
        try {
            return getBeanFactory().isTypeMatch(name, typeToMatch);
        } catch (NoSuchBeanDefinitionException nex) {
            throw new GirNoSuchBeanException(nex);
        }
    }

    @Override
    public Class<?> getType(String name) {
        try {
            return getBeanFactory().getType(name);
        } catch (NoSuchBeanDefinitionException nex) {
            throw new GirNoSuchBeanException(nex);
        }
    }

    @Override
    public String[] getAliases(String name) {
        return getBeanFactory().getAliases(name);
    }

    @Override
    public Object getBean(String name) {
        try {
            return getBeanFactory().getBean(name);
        } catch (NoSuchBeanDefinitionException nex) {
            throw new GirNoSuchBeanException(nex);
        } catch (BeansException nex) {
            throw new GirBeanException(nex);
        }
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        try {
            return getBeanFactory().getBean(name, requiredType);
        } catch (BeanNotOfRequiredTypeException bne) {
            throw new GirBeanNotOfRequiredTypeException(bne);
        } catch (NoSuchBeanDefinitionException nex) {
            throw new GirNoSuchBeanException(nex);
        } catch (BeansException nex) {
            throw new GirBeanException(nex);
        }
    }

    @Override
    public Object getBean(String name, Object... args) {
        try {
            return getBeanFactory().getBean(name, args);
        } catch (BeanDefinitionStoreException ex) {
            throw new GirBeanDefinitionStoreException(ex);
        } catch (NoSuchBeanDefinitionException nex) {
            throw new GirNoSuchBeanException(nex);
        } catch (BeansException nex) {
            throw new GirBeanException(nex);
        }
    }

    @Override
    public <T> T getBean(Class<T> requiredType) {
        try {
            return getBeanFactory().getBean(requiredType);

        } catch (NoUniqueBeanDefinitionException nex) {
            throw new GirNoUniqueBeanException(nex);
        } catch (NoSuchBeanDefinitionException nex) {
            throw new GirNoSuchBeanException(nex);
        } catch (BeansException nex) {
            throw new GirBeanException(nex);
        }
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) {
        try {
            return getBeanFactory().getBean(requiredType, args);
        } catch (BeanDefinitionStoreException ex) {
            throw new GirBeanDefinitionStoreException(ex);
        } catch (NoSuchBeanDefinitionException nex) {
            throw new GirNoSuchBeanException(nex);
        } catch (BeansException ex) {
            throw new GirBeanException(ex);
        }
    }

    @Override
    public <T> Map<String, T> getBeans(Class<T> clazz) {
        try {
            return getBeanFactory().getBeansOfType(clazz);
        } catch (BeansException ex) {
            throw new GirBeanException(ex);
        }
    }

    @Override
    public <T> Map<String, T> getBeans(Class<T> clazz, Type[] genericType) throws GirBeanException {
        Map<String, T> map = getBeans(clazz);
        Map<String, T> res = new HashMap<String, T>();
        if (map.size() > 0) {
            loop1:
            for (Entry<String, T> entry : map.entrySet()) {
                ResolvableType resolvableType =
                        ResolvableType.forClass(clazz, GutilClass.getUserClass(entry.getValue()));
                for (int i = 0; i < genericType.length; i++) {
                    if (genericType[i] != resolvableType.getGeneric(i).resolve()) {
                        continue loop1;
                    }
                }
                res.put(entry.getKey(), entry.getValue());
            }
        }
        return res;
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Type[] genericType) {
        Map<String, T> map = getBeans(requiredType, genericType);

        if (map.isEmpty()) {
            throw new GirNoSuchBeanException(new NoSuchBeanDefinitionException(requiredType));
        }

        if (map.size() > 1) {
            throw new GirNoUniqueBeanException(
                    new NoUniqueBeanDefinitionException(requiredType, map.size(), "找到了不只一个bean实例"));
        }

        return map.values().iterator().next();
    }

    @Override
    public void register(String name, Class<?> beanClass) throws GirBeanDefinitionStoreException {
        register(name, beanClass, true);
    }

    @Override
    public void register(String name, Class<?> beanClass, boolean singleton)
            throws GirBeanDefinitionStoreException {
        BeanDefinitionRegistry beanRegistry =
                (BeanDefinitionRegistry) getApplicationContext().getAutowireCapableBeanFactory();

        /**
         * try { beanRegistry.getBeanDefinition(name); } catch (NoSuchBeanDefinitionException ne) {
         *
         * <p>}
         */
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClass(beanClass);
        definition.setScope(
                singleton ? BeanDefinition.SCOPE_SINGLETON : BeanDefinition.SCOPE_PROTOTYPE);

        try {
            beanRegistry.registerBeanDefinition(name, definition);
        } catch (BeanDefinitionOverrideException bse) {
            throw new GirBeanDefinitionStoreException(bse);
        } catch (BeanDefinitionStoreException bse) {
            throw new GirBeanDefinitionStoreException(bse);
        }
    }

    public static ListableBeanFactory getBeanFactory() {
        final ListableBeanFactory factory = null == beanFactory ? springContext : beanFactory;
        if (null == factory) {
            throw new UtilException(
                    "No ConfigurableListableBeanFactory or ApplicationContext injected, maybe not in the Spring environment?");
        }
        return factory;
    }

    public static ConfigurableListableBeanFactory getConfigurableBeanFactory()
            throws UtilException {
        final ConfigurableListableBeanFactory factory;
        if (null != beanFactory) {
            factory = beanFactory;
        } else if (springContext instanceof ConfigurableApplicationContext) {
            factory = ((ConfigurableApplicationContext) springContext).getBeanFactory();
        } else {
            throw new UtilException("No ConfigurableListableBeanFactory from context!");
        }
        return factory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        beanProvider = this;
        GirBeanHelper.setProvider(this);
        SpringBeanProviderResolver.setProvider(this);
    }
}
