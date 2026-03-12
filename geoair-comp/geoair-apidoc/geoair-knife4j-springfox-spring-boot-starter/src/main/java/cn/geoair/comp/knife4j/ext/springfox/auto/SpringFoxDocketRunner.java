package cn.geoair.comp.knife4j.ext.springfox.auto;

import cn.geoair.base.Gir;
import cn.geoair.comp.knife4j.ext.config.GirSwaggerApiConfig;
import cn.geoair.comp.knife4j.ext.config.GirSwaggerProperties;
import cn.geoair.comp.knife4j.ext.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.model.DocketInfo;
import cn.geoair.comp.knife4j.ext.springfox.service.SpringAddtionalModelService;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

@Configuration
@EnableSwagger2
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpringFoxDocketRunner
        implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private GirSwaggerProperties swaggerProperties;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.swaggerProperties = applicationContext.getBean(GirSwaggerProperties.class);
        Gir.log.info("ApplicationContext已初始化，类型：" + applicationContext.getClass().getName());
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
            throws BeansException {

        if (applicationContext == null) {
            throw new RuntimeException("ApplicationContext初始化失败！请检查Spring配置");
        }

        GirSwaggerApiConfig apiConfig = applicationContext.getBean(GirSwaggerApiConfig.class);
        SpringAddtionalModelService springAddtionalModelService =
                applicationContext.getBean(SpringAddtionalModelService.class);

        List<DocketInfo> docketInfos = apiConfig.getDocketInfos();
        if (docketInfos == null || docketInfos.isEmpty()) {
            System.out.print("未配置任何DocketInfo，跳过Docket注册");
            return;
        }
        ApiModelInfo apiModelInfo = apiConfig.getApiModelInfo();
        if (apiModelInfo == null) {
            throw new RuntimeException("GirSwaggerApiConfig中ApiModelInfo未配置！");
        }

        for (DocketInfo docketInfo : docketInfos) {
            String groupName = "";
            // 兜底分组名，避免空值
            groupName = docketInfo.getGroupName();
            // 校验扫描包非空
            String basePackage = docketInfo.getBasePackage();
            if (basePackage == null || basePackage.isEmpty()) {
                throw new RuntimeException("Docket分组[" + groupName + "]的basePackage未配置！");
            }

            BeanDefinitionBuilder docketBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(Docket.class);
            String finalGroupName = groupName;
            docketBuilder
                    .getBeanDefinition()
                    .setInstanceSupplier(
                            () ->
                                    springAddtionalModelService
                                            .createApi(apiModelInfo, docketInfo)
                                            .groupName(finalGroupName)
                                            .enable(swaggerProperties.isEnable()));
            docketBuilder.setLazyInit(false);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {}
}
