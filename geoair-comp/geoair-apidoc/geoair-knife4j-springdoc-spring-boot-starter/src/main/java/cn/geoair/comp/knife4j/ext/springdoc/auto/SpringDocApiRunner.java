package cn.geoair.comp.knife4j.ext.springdoc.auto;

import cn.geoair.base.Gir;
import cn.geoair.comp.knife4j.ext.config.GirSwaggerApiConfig;

import cn.geoair.comp.knife4j.ext.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.model.DocketInfo;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;


public class SpringDocApiRunner implements ApplicationContextAware {


    // 1. 保存Spring应用上下文（通过ApplicationContextAware接口）
    private ApplicationContext applicationContext;

    // 你的ApiConfig配置类（保持原有注入）
    private final GirSwaggerApiConfig apiConfig;

    // 构造器注入ApiConfig（推荐，替代@Resource）
    public SpringDocApiRunner(GirSwaggerApiConfig apiConfig) {
        this.apiConfig = apiConfig;
    }

    @PostConstruct
    public void registerGroupedOpenApi() {
        // 2. 将ApplicationContext转换为BeanDefinitionRegistry（兼容所有Spring环境）
        if (!(applicationContext instanceof BeanDefinitionRegistry)) {
            throw new RuntimeException("无法获取BeanDefinitionRegistry，无法注册GroupedOpenApi");
        }

        List<DocketInfo> docketInfos = apiConfig.getDocketInfos();
        if (docketInfos == null || docketInfos.isEmpty()) {
            return; // 无配置时直接返回
        }

        // 3. 遍历每个DocketInfo，注册独立的GroupedOpenApi Bean
        for (int i = 0; i < docketInfos.size(); i++) {
            DocketInfo docketInfo = docketInfos.get(i);

            // 3.1 生成唯一的Bean名称（避免重复）
            String beanName = "groupedOpenApi-" + i + "-" + docketInfo.getGroupName().replaceAll("[^a-zA-Z0-9]", "-");

            // 3.2 生成分组名称（确保唯一，避免合并）
            String groupName = docketInfo.getGroupName();

            // 3.3 构建GroupedOpenApi的Bean定义
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(GroupedOpenApi.class, () ->
                    GroupedOpenApi.builder()
                            .group(groupName) // 分组名称
                            .packagesToScan(docketInfo.getBasePackage()) // 扫描的包
                            .build()
            );

            // 3.4 注册为独立的Spring Bean（核心修复）
            BeanDefinition beanDefinition = builder.getBeanDefinition();
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) applicationContext;
            registry.registerBeanDefinition(beanName, beanDefinition);


//            Gir.log.info("成功注册GroupedOpenApi Bean：" + beanName + "，分组名称：" + groupName + "，扫描包：" + docketInfo.getBasePackage());
        }
    }

    /**
     * Spring自动注入ApplicationContext，并初始化配置属性
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }



    /**
     * 基础OpenAPI配置
     */
    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI customOpenAPI() {
        ApiModelInfo apiModelInfo1 = apiConfig.getApiModelInfo();
        Contact contact = new Contact();
        contact.setName(apiModelInfo1.getAuthor());
        contact.setEmail("");
        contact.setUrl("");
        return new OpenAPI()
                .info(new Info()
                        .contact(contact)
                        .title(apiModelInfo1.getTitle())
                        .version(apiModelInfo1.getVersion())
                        .termsOfService(apiModelInfo1.getAuthor())
                        .description(apiModelInfo1.getDescription()));
    }

    // ========== 1. 替代application.yml的springdoc核心配置 ==========
    @Bean
    @Primary
    public SpringDocConfigProperties springDocConfigProperties() {
        SpringDocConfigProperties properties = new SpringDocConfigProperties();

        // 启用分组API文档端点（对应springdoc.api-docs.groups.enabled: true）
        SpringDocConfigProperties.ApiDocs apiDocs = new SpringDocConfigProperties.ApiDocs();
        apiDocs.setPath("/v3/api-docs"); // 基础端点路径
        apiDocs.setGroups(new SpringDocConfigProperties.Groups());
        apiDocs.getGroups().setEnabled(true); // 关键：启用分组
        properties.setApiDocs(apiDocs);

        return properties;
    }






}
