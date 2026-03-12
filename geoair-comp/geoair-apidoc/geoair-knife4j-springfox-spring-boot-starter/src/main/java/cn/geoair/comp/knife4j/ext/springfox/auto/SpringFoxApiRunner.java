package cn.geoair.comp.knife4j.ext.springfox.auto;

import cn.geoair.comp.knife4j.ext.config.GirSwaggerApiConfig;

import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;

import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
public class SpringFoxApiRunner
        implements ApplicationContextAware, BeanDefinitionRegistryPostProcessor {

    @Bean
    @ConditionalOnMissingBean(Reflections.class)
    Reflections reflections() {
        return new Reflections();
    }

    private ApplicationContext applicationContext;

    private GirSwaggerApiConfig apiConfig;

    //    private GirSwaggerProperties swaggerProperties;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
            throws BeansException {}

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {}

    private synchronized void initDependencies() {
        if (apiConfig == null) {
            this.apiConfig = applicationContext.getBean(GirSwaggerApiConfig.class);
            //            this.swaggerProperties =
            // applicationContext.getBean(GirSwaggerProperties.class);

        }
    }
}
