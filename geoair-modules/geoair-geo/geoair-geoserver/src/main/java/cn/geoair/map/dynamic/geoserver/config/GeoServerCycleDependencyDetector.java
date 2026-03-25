package cn.geoair.map.dynamic.geoserver.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/** 内置在三方Jar中的循环依赖检测初始化器 检测到GeoServer循环依赖且未配置allow-circular-references时，主动抛出友好异常 */
public class GeoServerCycleDependencyDetector
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    // 标记是否存在GeoServer循环依赖的核心Bean
    private static final String[] CYCLE_BEAN_NAMES = {
        "geoServer", "resourcePoolInitializer", "wms", "loggingInitializer"
    };

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // 1. 先检测Spring是否启用了循环依赖支持
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        String allowCircularRef = environment.getProperty("spring.main.allow-circular-references");
        String allowBeanOverride = environment.getProperty("allow-bean-definition-overriding");
        boolean isCircularRefAllowed = "true".equalsIgnoreCase(allowCircularRef);
        boolean isBeanOverrideAllowed = "true".equalsIgnoreCase(allowBeanOverride);

        // 2. 检测容器中是否存在GeoServer循环依赖相关的Bean（说明调用方引入了GeoServer）
        boolean hasGeoServerCycleBean = false;
        for (String beanName : CYCLE_BEAN_NAMES) {
            if (applicationContext.containsBeanDefinition(beanName)) {
                hasGeoServerCycleBean = true;
                break;
            }
        }

        // 3. 若存在循环依赖Bean且未启用循环依赖支持，主动抛出友好异常
        if (hasGeoServerCycleBean && !isCircularRefAllowed) {
            throw new IllegalStateException(
                    "\n===== 【GeoServer三方Jar依赖提醒】 =====\n"
                            + "检测到您的项目引入了GeoServer相关依赖，存在以下循环依赖Bean：\n"
                            + "   geoServer ↔ resourcePoolInitializer (或 geoServer ↔ loggingInitializer)\n"
                            + "Spring Boot 2.6+默认禁用循环依赖，导致容器无法启动！\n"
                            + "【解决方案】请在您的项目配置文件中添加：\n"
                            + "  application.yml: \n"
                            + "    spring:\n"
                            + "      main:\n"
                            + "        allow-circular-references: true\n"
                            + "  或 application.properties:\n"
                            + "    spring.main.allow-circular-references=true\n"
                            + "======================================");
        }
        if (!isBeanOverrideAllowed) {
            throw new IllegalStateException(
                    "\n===== 【GeoServer三方Jar依赖提醒】 =====\n"
                            + "检测到您的项目引入了GeoServer相关依赖，存在以下循环依赖Bean：\n"
                            + "Spring Boot 2.6+默认禁用bean覆盖，导致容器无法启动！\n"
                            + "【解决方案】请在您的项目配置文件中添加：\n"
                            + "  application.yml: \n"
                            + "    spring:\n"
                            + "      main:\n"
                            + "        allow-bean-definition-overriding: true\n"
                            + "  或 application.properties:\n"
                            + "    spring.main.allow-bean-definition-overriding=true\n"
                            + "======================================");
        }
    }
}
