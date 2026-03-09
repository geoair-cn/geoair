package cn.geoair.comp.knife4j.ext.springfox.auto;

import cn.geoair.comp.knife4j.ext.config.GirSwaggerApiConfig;
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

/**
 * 最终修复方案： 1. 用BeanDefinitionRegistryPostProcessor替代ImportBeanDefinitionRegistrar，适配时序 2.
 * 先通过ApplicationContextAware获取上下文，再注册Docket 3. 最高优先级执行，保证Docket在Swagger前注册
 */
@Configuration
@EnableSwagger2
@Order(Ordered.HIGHEST_PRECEDENCE) // 最高优先级，早于Swagger加载
public class SpringFoxDocketRunner
        implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext; // 非静态，避免线程安全问题

    /** 第一步：上下文就绪后，先赋值（此时不会为空） */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        System.out.println("ApplicationContext已初始化，类型：" + applicationContext.getClass().getName());
    }

    /** 第二步：上下文就绪后，注册Docket Bean（核心修复：此时applicationContext非空） */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
            throws BeansException {
        // 双重校验：确保上下文非空
        if (applicationContext == null) {
            throw new RuntimeException("ApplicationContext初始化失败！请检查Spring配置");
        }

        // 1. 安全获取依赖（此时上下文已就绪，不会空指针）
        GirSwaggerApiConfig apiConfig = applicationContext.getBean(GirSwaggerApiConfig.class);
        SpringAddtionalModelService springAddtionalModelService =
                applicationContext.getBean(SpringAddtionalModelService.class);

        // 2. 校验配置非空
        List<DocketInfo> docketInfos = apiConfig.getDocketInfos();
        if (docketInfos == null || docketInfos.isEmpty()) {
            System.out.print("未配置任何DocketInfo，跳过Docket注册");
            return;
        }
        ApiModelInfo apiModelInfo = apiConfig.getApiModelInfo();
        if (apiModelInfo == null) {
            throw new RuntimeException("GirSwaggerApiConfig中ApiModelInfo未配置！");
        }

        // 3. 逐个注册独立Docket Bean（核心：每个Docket一个独立Bean）
        for (DocketInfo docketInfo : docketInfos) {
            String groupName = "";
            // 兜底分组名，避免空值
            groupName = docketInfo.getGroupName();
            // 校验扫描包非空
            String basePackage = docketInfo.getBasePackage();
            if (basePackage == null || basePackage.isEmpty()) {
                throw new RuntimeException("Docket分组[" + groupName + "]的basePackage未配置！");
            }

            // 4. 构建Docket BeanDefinition（延迟实例化，保证扫描规则执行）
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
                                            .enable(true));
            docketBuilder.setLazyInit(false); // 关闭懒加载，立即实例化
        }
    }

    /** 空实现（接口必填） */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        // 无需处理
    }
}
