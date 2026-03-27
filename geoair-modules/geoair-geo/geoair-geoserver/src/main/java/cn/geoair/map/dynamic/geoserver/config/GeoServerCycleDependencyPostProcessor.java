package cn.geoair.map.dynamic.geoserver.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** 修复：移除构造器注入，通过BeanFactory获取Environment，添加无参构造 */
@Component
public class GeoServerCycleDependencyPostProcessor implements BeanFactoryPostProcessor {

    public GeoServerCycleDependencyPostProcessor() {
        super();
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        // 从BeanFactory中获取Environment（替代构造器注入）
        Environment environment = beanFactory.getBean(Environment.class);
        String allowCircularRef = environment.getProperty("spring.main.allow-circular-references");
        boolean isAllowed = "true".equalsIgnoreCase(allowCircularRef);

        // 检测GeoServer循环依赖Bean
        boolean hasCycleBean =
                beanFactory.containsBeanDefinition("geoServer")
                        && beanFactory.containsBeanDefinition("resourcePoolInitializer");

        // try {
        // Class.forName(
        //
        // "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration");
        //
        // Gir.log.info("已检测到ManagementWebSecurityAutoConfiguration三方Jar依赖，会导致Geoserver报错，请移除");
        // } catch (ClassNotFoundException e) {
        //
        // }

        // 未配置循环依赖则抛出友好异常
        if (hasCycleBean && !isAllowed) {
            throw new IllegalStateException(
                    "\n【GeoServer三方Jar错误】\n"
                            + "未配置spring.main.allow-circular-references=true，无法解决以下循环依赖：\n"
                            + "geoServer → resourcePoolInitializer → geoServer\n"
                            + "请在您的项目配置文件中添加：\n"
                            + "spring.main.allow-circular-references=true\n");
        }
    }
}
