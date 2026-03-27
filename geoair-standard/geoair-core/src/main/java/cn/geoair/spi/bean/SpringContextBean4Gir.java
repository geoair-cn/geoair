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
import cn.geoair.base.log.GirLogger;
import cn.geoair.base.util.GutilClass;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

/**
 * 由spring提供bean容器
 *
 * @author Ray
 */
@Component
public class SpringContextBean4Gir implements GiBeanFactory, ApplicationContextAware {

    static {
        GkMethodHand.implFromClass(SpringContextBean4Gir.class);
    }

    protected final GiLogger logger = GirLogger.getLoger(SpringContextBean4Gir.class);

    @GaMethodHandImpl(
        implClass = GirBeanHelper.class,
        implMethod = "getProvider",
        type = ImplType.expectfirst
    )
    private static GiBeanFactory getProvider() {
        return beanProvider;
    }

    private static ApplicationContext springContext;

    private static GiBeanFactory beanProvider;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        beanProvider = this;
        springContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return springContext;
    }

    @Override
    public boolean containsBean(String name) {
        return getApplicationContext().containsBean(name);
    }

    @Override
    public boolean isSingleton(String name) {
        try {
            return getApplicationContext().isSingleton(name);
        } catch (NoSuchBeanDefinitionException nex) {
            throw new GirNoSuchBeanException(nex);
        }
    }

    @Override
    public boolean isPrototype(String name) {
        try {
            return getApplicationContext().isPrototype(name);
        } catch (NoSuchBeanDefinitionException nex) {
            throw new GirNoSuchBeanException(nex);
        }
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) {
        try {
            return getApplicationContext().isTypeMatch(name, typeToMatch);
        } catch (NoSuchBeanDefinitionException nex) {
            throw new GirNoSuchBeanException(nex);
        }
    }

    @Override
    public Class<?> getType(String name) {
        try {
            return getApplicationContext().getType(name);
        } catch (NoSuchBeanDefinitionException nex) {
            throw new GirNoSuchBeanException(nex);
        }
    }

    @Override
    public String[] getAliases(String name) {
        return getApplicationContext().getAliases(name);
    }

    @Override
    public Object getBean(String name) {
        try {
            return getApplicationContext().getBean(name);
        } catch (NoSuchBeanDefinitionException nex) {
            throw new GirNoSuchBeanException(nex);
        } catch (BeansException nex) {
            throw new GirBeanException(nex);
        }
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        try {
            return getApplicationContext().getBean(name, requiredType);
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
            return getApplicationContext().getBean(name, args);
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
            return getApplicationContext().getBean(requiredType);

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
            return getApplicationContext().getBean(requiredType, args);
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
            return getApplicationContext().getBeansOfType(clazz);
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
}
