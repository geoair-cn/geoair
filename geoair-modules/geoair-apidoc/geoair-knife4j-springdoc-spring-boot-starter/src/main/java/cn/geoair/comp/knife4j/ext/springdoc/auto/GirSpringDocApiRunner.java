package cn.geoair.comp.knife4j.ext.springdoc.auto;

import cn.geoair.base.exception.GirException;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.knife4j.ext.core.auto.AutoApiConfigScanner;
import cn.geoair.comp.knife4j.ext.core.config.GirOpenApiConfig;
import cn.geoair.comp.knife4j.ext.core.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.core.model.DocketInfo;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.*;

/**
 * GirSpringDocApiRunner class.
 *
 * @author Administrator
 * @version $Id: $Id
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GirSpringDocApiRunner
        implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {
    public static GiLogger log = GirLoggerFactory.getLogger();

    private ApplicationContext applicationContext;

    private final Map<String, Integer> groupNameMap = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI customOpenAPI() {
        ApiModelInfo apiModelInfo = resolveApiModelInfo();
        Contact contact = new Contact();
        contact.setName(apiModelInfo.getAuthor());
        contact.setEmail("");
        contact.setUrl("");
        return new OpenAPI()
                .info(
                        new Info()
                                .contact(contact)
                                .title(apiModelInfo.getTitle())
                                .version(apiModelInfo.getVersion())
                                .termsOfService(apiModelInfo.getAuthor())
                                .description(apiModelInfo.getDescription()));
    }

    @Bean
    @Primary
    public SpringDocConfigProperties springDocConfigProperties() {
        SpringDocConfigProperties properties = new SpringDocConfigProperties();

        SpringDocConfigProperties.ApiDocs apiDocs = new SpringDocConfigProperties.ApiDocs();
        apiDocs.setPath("/v3/api-docs");
        apiDocs.setGroups(new SpringDocConfigProperties.Groups());
        apiDocs.getGroups().setEnabled(true);
        properties.setApiDocs(apiDocs);
        properties.setDefaultSupportFormData(true);
        properties.setModelAndViewAllowed(true);

        Set<SpringDocConfigProperties.GroupConfig> groupConfigs = properties.getGroupConfigs();
        for (DocketInfo docketInfo : resolveDocketInfos()) {
            SpringDocConfigProperties.GroupConfig groupConfig =
                    new SpringDocConfigProperties.GroupConfig();
            String groupName = resolveGroupName(docketInfo);
            groupConfig.setGroup(groupName);
            groupConfig.setDisplayName(groupName);
            groupConfig.setPackagesToScan(docketInfo.getBasePackages());
            groupConfigs.add(groupConfig);
        }
        return properties;
    }

    @Bean
    @Primary
    public SwaggerUiConfigProperties swaggerUiConfigProperties() {
        SwaggerUiConfigProperties uiProperties = new SwaggerUiConfigProperties();
        uiProperties.setOperationsSorter("method");
        uiProperties.setTagsSorter("alpha");
        uiProperties.setDisplayRequestDuration(true);
        uiProperties.setDefaultModelExpandDepth(5);
        uiProperties.setDefaultModelsExpandDepth(5);
        uiProperties.setShowExtensions(true);
        uiProperties.setDisplayOperationId(true);
        String property = applicationContext.getEnvironment().getProperty("geoair.apidoc.enable");
        uiProperties.setEnabled("true".equals(property));
        return uiProperties;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
            throws BeansException {
        if (applicationContext == null) {
            throw new RuntimeException("ApplicationContext初始化失败！请检查Spring配置");
        }

        groupNameMap.clear();
        for (GirOpenApiConfig apiConfig : resolveApiConfigs()) {
            registerDocketsFromConfig(apiConfig, registry);
        }
    }

    private void registerDocketsFromConfig(
            GirOpenApiConfig apiConfig, BeanDefinitionRegistry registry) {
        apiConfig.doLoading();
        List<DocketInfo> docketInfos = apiConfig.getDocketInfos();
        if (docketInfos == null || docketInfos.isEmpty()) {
            log.debug("【SpringDoc】未配置任何DocketInfo，跳过Docket注册");
            return;
        }

        ApiModelInfo apiModelInfo = apiConfig.getApiModelInfo();
        if (apiModelInfo == null) {
            apiModelInfo = new ApiModelInfo("API文档", "API描述", "API", "1.0.0");
            log.debug("【SpringDoc】未配置ApiModelInfo，使用默认配置");
        }

        for (DocketInfo docketInfo : docketInfos) {
            registerSingleDocket(apiModelInfo, docketInfo, registry);
        }
        apiConfig.loadEnd();
    }

    private void registerSingleDocket(
            ApiModelInfo apiModelInfo, DocketInfo docketInfo, BeanDefinitionRegistry registry) {
        String groupName = resolveGroupName(docketInfo);
        String basePackage = docketInfo.getBasePackage();
        if (basePackage == null || basePackage.trim().isEmpty()) {
            throw new RuntimeException("【SpringDoc】 分组[" + groupName + "]的basePackage未配置！");
        }
        if (groupNameMap.containsKey(groupName)) {
            throw new GirException("分组名重复：{}", groupName);
        }
        groupNameMap.put(groupName, 1);

        String beanName = generateBeanName(groupName, basePackage);
        String finalGroupName = groupName;
        BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.genericBeanDefinition(
                        GroupedOpenApi.class,
                        () ->
                                GroupedOpenApi.builder()
                                        .group(finalGroupName)
                                        .packagesToScan(
                                                docketInfo.getBasePackages().toArray(new String[0]))
                                        .build());

        builder.setLazyInit(false);
        builder.setScope("singleton");
        BeanDefinition beanDefinition = builder.getBeanDefinition();
        registry.registerBeanDefinition(beanName, beanDefinition);
    }

    private String resolveGroupName(DocketInfo docketInfo) {
        String groupName = docketInfo.getGroupName();
        if (groupName != null && !groupName.trim().isEmpty()) {
            return groupName.trim();
        }
        String basePackage = docketInfo.getBasePackage();
        if (basePackage != null && basePackage.contains(".")) {
            return basePackage.substring(basePackage.lastIndexOf('.') + 1);
        }
        return "default";
    }

    private List<GirOpenApiConfig> resolveApiConfigs() {
        Map<String, GirOpenApiConfig> apiConfigMap =
                applicationContext.getBeansOfType(GirOpenApiConfig.class);
        if (apiConfigMap.isEmpty()) {
            List<GirOpenApiConfig> fallback = new ArrayList<>();
            fallback.add(new AutoApiConfigScanner(applicationContext));
            return fallback;
        }
        return new ArrayList<>(new LinkedHashMap<>(apiConfigMap).values());
    }

    private List<DocketInfo> resolveDocketInfos() {
        List<DocketInfo> docketInfos = new ArrayList<>();
        for (GirOpenApiConfig apiConfig : resolveApiConfigs()) {
            apiConfig.doLoading();
            List<DocketInfo> list = apiConfig.getDocketInfos();
            if (list != null && !list.isEmpty()) {
                docketInfos.addAll(list);
            }
        }
        return docketInfos;
    }

    private ApiModelInfo resolveApiModelInfo() {
        for (GirOpenApiConfig apiConfig : resolveApiConfigs()) {
            ApiModelInfo apiModelInfo = apiConfig.getApiModelInfo();
            if (apiModelInfo != null) {
                return apiModelInfo;
            }
        }
        return new ApiModelInfo("API文档", "API描述", "API", "1.0.0");
    }

    private String generateBeanName(String groupName, String basePackage) {
        String cleanGroupName = groupName.replaceAll("[^a-zA-Z0-9]", "");
        if (cleanGroupName.isEmpty()) {
            cleanGroupName = "GroupedOpenApi";
        }
        int packageHash = Math.abs(basePackage.hashCode() % 1000);
        return cleanGroupName + "GroupedOpenApi_" + packageHash;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {}
}
