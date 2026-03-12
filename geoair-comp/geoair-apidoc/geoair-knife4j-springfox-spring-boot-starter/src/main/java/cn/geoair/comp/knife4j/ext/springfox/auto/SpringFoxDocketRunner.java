package cn.geoair.comp.knife4j.ext.springfox.auto;

import cn.geoair.comp.knife4j.ext.core.config.GirSwaggerApiConfig;
import cn.geoair.comp.knife4j.ext.core.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.core.model.DocketInfo;
import cn.geoair.comp.knife4j.ext.springfox.service.SpringAddtionalModelUtils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import springfox.documentation.spring.web.plugins.Docket;

import java.util.List;
import java.util.Map;

/** SpringFox Docket 动态注册器 在Spring容器启动早期扫描GirSwaggerApiConfig实现类，动态创建并注册Docket Bean */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class SpringFoxDocketRunner
        implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private boolean enable;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        enable =
                "true"
                        .equals(
                                applicationContext
                                        .getEnvironment()
                                        .getProperty("geoair.apidoc.enable"));
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
            throws BeansException {

        if (applicationContext == null) {
            throw new RuntimeException("ApplicationContext初始化失败！请检查Spring配置");
        }

        // 获取所有 GirSwaggerApiConfig 的实现类（支持多个配置）
        Map<String, GirSwaggerApiConfig> apiConfigMap =
                applicationContext.getBeansOfType(GirSwaggerApiConfig.class);

        if (apiConfigMap.isEmpty()) {
            System.out.println("【SpringFox】未找到任何 GirSwaggerApiConfig 实现，跳过Docket注册");
            return;
        }

        // 遍历每个配置，注册对应的Docket
        for (Map.Entry<String, GirSwaggerApiConfig> entry : apiConfigMap.entrySet()) {
            String configBeanName = entry.getKey();
            GirSwaggerApiConfig apiConfig = entry.getValue();

            //            System.out.println("【SpringFox】开始处理配置: " + configBeanName);
            registerDocketsFromConfig(apiConfig, registry);
        }
    }

    /** 从单个配置中注册所有Docket */
    private void registerDocketsFromConfig(
            GirSwaggerApiConfig apiConfig, BeanDefinitionRegistry registry) {
        // 获取DocketInfo列表
        List<DocketInfo> docketInfos = apiConfig.getDocketInfos();
        if (docketInfos == null || docketInfos.isEmpty()) {
            System.out.println("【SpringFox】未配置任何DocketInfo，跳过Docket注册");
            return;
        }

        // 获取API基本信息
        ApiModelInfo apiModelInfo = apiConfig.getApiModelInfo();
        if (apiModelInfo == null) {
            // 提供默认的API信息，避免启动失败
            apiModelInfo = new ApiModelInfo("API文档", "API描述", "API", "1.0.0");
            System.out.println("【SpringFox】未配置ApiModelInfo，使用默认配置");
        }

        // 遍历每个DocketInfo，创建并注册Docket
        for (DocketInfo docketInfo : docketInfos) {
            registerSingleDocket(apiModelInfo, docketInfo, registry);
        }
    }

    /** 注册单个Docket */
    private void registerSingleDocket(
            ApiModelInfo apiModelInfo, DocketInfo docketInfo, BeanDefinitionRegistry registry) {
        // 处理分组名
        String groupName = null;
        {
            // 如果分组名为空，使用包名最后一部分作为分组名
            String basePackage = docketInfo.getBasePackage();
            if (basePackage != null && basePackage.contains(".")) {
                groupName = basePackage.substring(basePackage.lastIndexOf('.') + 1);
            } else {
                groupName = "default";
            }
        }

        // 校验扫描包非空
        String basePackage = docketInfo.getBasePackage();
        if (basePackage == null || basePackage.trim().isEmpty()) {
            throw new RuntimeException("【SpringFox】Docket分组[" + groupName + "]的basePackage未配置！");
        }

        // 生成Bean名称
        String beanName = generateBeanName(groupName, basePackage);

        // 使用BeanDefinitionBuilder创建Bean定义
        String finalGroupName = groupName;
        BeanDefinitionBuilder docketBuilder =
                BeanDefinitionBuilder.genericBeanDefinition(
                        Docket.class,
                        // 使用Lambda表达式作为实例供应商
                        () -> {
                            // 调用工具类创建Docket实例
                            Docket docket =
                                    SpringAddtionalModelUtils.createApi(apiModelInfo, docketInfo)
                                            .groupName(finalGroupName)
                                            .enable(enable);

                            return docket;
                        });

        // 设置Bean的其他属性
        docketBuilder.setLazyInit(false); // 非懒加载
        docketBuilder.setScope("singleton"); // 单例模式

        // 注册Bean定义到容器
        registry.registerBeanDefinition(beanName, docketBuilder.getBeanDefinition());

        //        System.out.println(
        //                "【SpringFox】成功注册Docket: "
        //                        + beanName
        //                        + ", 分组: "
        //                        + groupName
        //                        + ", 扫描包: "
        //                        + basePackage);
    }

    /** 生成唯一的Bean名称 */
    private String generateBeanName(String groupName, String basePackage) {
        // 清理特殊字符，确保Bean名称合法
        String cleanGroupName = groupName.replaceAll("[^a-zA-Z0-9]", "");
        if (cleanGroupName.isEmpty()) {
            cleanGroupName = "Docket";
        }

        // 使用包名的hashCode确保唯一性
        int packageHash = Math.abs(basePackage.hashCode() % 1000);
        return cleanGroupName + "Docket_" + packageHash;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        // 此处不需要额外处理
        // 如果需要修改已经注册的Bean，可以在这里进行
    }
}
