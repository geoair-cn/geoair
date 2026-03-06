package cn.geoair.map.dynamic.geoserver.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

@ImportResource(
        locations = {"classpath*:/applicationContext.xml"
            //                ,"classpath*:/applicationSecurityContext.xml"
        })
public class GeoServerConfig
        implements BeanPostProcessor, ApplicationContextAware, BeanDefinitionRegistryPostProcessor {
    ApplicationContext applicationContext;
    @Autowired GirGeoServerProperties girGeoServerProperties;

    @Bean
    public ServletContextInitializer geoserverContextInitializer() {
        return servletContext ->
                servletContext.setInitParameter(
                        "GEOSERVER_DATA_DIR", girGeoServerProperties.getDataDir());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory registry)
            throws BeansException {
        //                // 要排除的引发循环依赖的 Bean 名称
        //                String[] cycleBeans = {"loggingFilter", "loggingInitializer"};
        //
        //                for (String beanName : cycleBeans) {
        //                    // 正确判断并移除 Bean 定义：registry 是 BeanDefinitionRegistry 实例，拥有该方法
        //                    if (registry.containsBeanDefinition(beanName)) {
        //
        // ((org.springframework.beans.factory.support.DefaultListableBeanFactory)
        //         registry)
        //                                .removeBeanDefinition(beanName);
        //                        System.out.println("成功移除循环依赖 Bean：" + beanName);
        //                    }
        //                }
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry)
            throws BeansException {}
}
