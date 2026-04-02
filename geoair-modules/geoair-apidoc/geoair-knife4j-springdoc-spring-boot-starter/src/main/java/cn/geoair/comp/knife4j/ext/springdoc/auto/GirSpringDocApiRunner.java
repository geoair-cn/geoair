package cn.geoair.comp.knife4j.ext.springdoc.auto;

import cn.geoair.comp.knife4j.ext.core.auto.AutoApiConfigScanner;
import cn.geoair.comp.knife4j.ext.core.config.GirOpenApiConfig;
import cn.geoair.comp.knife4j.ext.core.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.core.model.DocketInfo;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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

/**
 * GirSpringDocApiRunner class.
 *
 * @author Administrator
 * @version $Id: $Id
 */
@Slf4j
public class GirSpringDocApiRunner
        implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    // 1. 保存Spring应用上下文（通过ApplicationContextAware接口）
    private ApplicationContext applicationContext;

    // private final GirOpenApiConfig apiConfig;
    //
    //
    //
    // private final GirSwaggerProperties girSwaggerProperties;

    // // 构造器注入ApiConfig（推荐，替代@Resource）
    // public SpringDocApiRunner(IGirOpenApiConfig apiConfig, GirSwaggerProperties
    // girSwaggerProperties) {
    // this.apiConfig = apiConfig;
    // this.girSwaggerProperties = girSwaggerProperties;
    // }

    /** registerGroupedOpenApi. */
    @PostConstruct
    public void registerGroupedOpenApi() {
        // 2. 将ApplicationContext转换为BeanDefinitionRegistry（兼容所有Spring环境）
        if (!(applicationContext instanceof BeanDefinitionRegistry)) {
            throw new RuntimeException("无法获取BeanDefinitionRegistry，无法注册GroupedOpenApi");
        }

        Map<String, GirOpenApiConfig> beansOfType =
                applicationContext.getBeansOfType(GirOpenApiConfig.class);
        List<DocketInfo> docketInfos =
                Objects.requireNonNull(beansOfType.values().stream().findFirst().orElse(null))
                        .getDocketInfos();
        if (docketInfos == null || docketInfos.isEmpty()) {
            return; // 无配置时直接返回
        }

        // 3. 遍历每个DocketInfo，注册独立的GroupedOpenApi Bean
        for (int i = 0; i < docketInfos.size(); i++) {
            DocketInfo docketInfo = docketInfos.get(i);

            // 3.1 生成唯一的Bean名称（避免重复）
            // String beanName = "groupedOpenApi-" + i + "-" +
            // docketInfo.getGroupName().replaceAll("[^a-zA-Z0-9]", "-");

            // 3.2 生成分组名称（确保唯一，避免合并）
            String groupName = docketInfo.getGroupName();

            // 3.3 构建GroupedOpenApi的Bean定义
            BeanDefinitionBuilder builder =
                    BeanDefinitionBuilder.genericBeanDefinition(
                            GroupedOpenApi.class,
                            () ->
                                    GroupedOpenApi.builder()
                                            .group(groupName) // 分组名称

                                            .packagesToScan(docketInfo.getBasePackage()) // 扫描的包
                                            .build());

            // 3.4 注册为独立的Spring Bean（核心修复）
            BeanDefinition beanDefinition = builder.getBeanDefinition();
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) applicationContext;
            registry.registerBeanDefinition(groupName, beanDefinition);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Spring自动注入ApplicationContext，并初始化配置属性
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 基础OpenAPI配置
     *
     * @return a {@link io.swagger.v3.oas.models.OpenAPI} object
     */
    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI customOpenAPI() {
        Map<String, GirOpenApiConfig> beansOfType =
                applicationContext.getBeansOfType(GirOpenApiConfig.class);
        ApiModelInfo apiModelInfo1 =
                Objects.requireNonNull(beansOfType.values().stream().findFirst().orElse(null))
                        .getApiModelInfo();
        Contact contact = new Contact();
        contact.setName(apiModelInfo1.getAuthor());
        contact.setEmail("");
        contact.setUrl("");
        return new OpenAPI()
                .info(
                        new Info()
                                .contact(contact)
                                .title(apiModelInfo1.getTitle())
                                .version(apiModelInfo1.getVersion())
                                .termsOfService(apiModelInfo1.getAuthor())
                                .description(apiModelInfo1.getDescription()));
    }

    // ========== 1. 替代application.yml的springdoc核心配置 ==========
    /**
     * springDocConfigProperties.
     *
     * @return a {@link org.springdoc.core.SpringDocConfigProperties} object
     */
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
        properties.setDefaultSupportFormData(true);
        properties.setModelAndViewAllowed(true);

        List<SpringDocConfigProperties.GroupConfig> groupConfigs = properties.getGroupConfigs();
        Map<String, GirOpenApiConfig> beansOfType =
                applicationContext.getBeansOfType(GirOpenApiConfig.class);
        List<DocketInfo> docketInfos =
                Objects.requireNonNull(beansOfType.values().stream().findFirst().orElse(null))
                        .getDocketInfos();
        if (docketInfos != null && !docketInfos.isEmpty()) {
            for (DocketInfo docketInfo : docketInfos) {
                SpringDocConfigProperties.GroupConfig groupConfig =
                        new SpringDocConfigProperties.GroupConfig();
                groupConfig.setGroup(docketInfo.getGroupName());
                groupConfig.setDisplayName(docketInfo.getGroupName());
                groupConfig.setPackagesToScan(docketInfo.getBasePackages());
                groupConfigs.add(groupConfig);
            }
        }
        return properties;
    }

    /**
     * swaggerUiConfigProperties.
     *
     * @return a {@link org.springdoc.core.SwaggerUiConfigProperties} object
     */
    @Bean
    @Primary // 核心修复：标记为优先Bean，解决冲突
    public SwaggerUiConfigProperties swaggerUiConfigProperties() {
        SwaggerUiConfigProperties uiProperties = new SwaggerUiConfigProperties();

        // 接口排序：按方法名（对应springdoc.swagger-ui.operationsSorter: method）
        uiProperties.setOperationsSorter("method");
        // Tag排序：按字母（对应springdoc.swagger-ui.tagsSorter: alpha）
        uiProperties.setTagsSorter("alpha");
        // 启用Swagger UI分组下拉框
        uiProperties.setDisplayRequestDuration(true);
        uiProperties.setDefaultModelExpandDepth(5);
        uiProperties.setDefaultModelsExpandDepth(5);
        uiProperties.setShowExtensions(true);
        uiProperties.setDisplayOperationId(true);
        String property = applicationContext.getEnvironment().getProperty("geoair.apidoc.enable");
        uiProperties.setEnabled(Objects.equals(property, "true"));
        return uiProperties;
    }

    /** {@inheritDoc} */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
            throws BeansException {
        if (applicationContext == null) {
            throw new RuntimeException("ApplicationContext初始化失败！请检查Spring配置");
        }

        Map<String, GirOpenApiConfig> apiConfigMap =
                applicationContext.getBeansOfType(GirOpenApiConfig.class);
        if (apiConfigMap.isEmpty()) {

            apiConfigMap = new HashMap<>();
            apiConfigMap.put("byAutoScanner", new AutoApiConfigScanner(applicationContext));
        }

        // 遍历每个配置，注册对应的Docket
        for (Map.Entry<String, GirOpenApiConfig> entry : apiConfigMap.entrySet()) {
            String configBeanName = entry.getKey();
            GirOpenApiConfig apiConfig = entry.getValue();

            registerDocketsFromConfig(apiConfig, registry);
        }
    }

    private void registerDocketsFromConfig(
            GirOpenApiConfig apiConfig, BeanDefinitionRegistry registry) {
        // 获取DocketInfo列表
        apiConfig.doLoading();
        List<DocketInfo> docketInfos = apiConfig.getDocketInfos();
        if (docketInfos == null || docketInfos.isEmpty()) {
            log.debug("【SpringDoc】未配置任何DocketInfo，跳过Docket注册");
            return;
        }

        // 获取API基本信息
        ApiModelInfo apiModelInfo = apiConfig.getApiModelInfo();
        if (apiModelInfo == null) {
            // 提供默认的API信息，避免启动失败
            apiModelInfo = new ApiModelInfo("API文档", "API描述", "API", "1.0.0");
            log.debug("【SpringDoc】未配置ApiModelInfo，使用默认配置");
        }

        // 遍历每个DocketInfo，创建并注册Docket
        for (DocketInfo docketInfo : docketInfos) {
            registerSingleDocket(apiModelInfo, docketInfo, registry);
        }
        apiConfig.loadEnd();
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
            throw new RuntimeException(
                    new StringBuilder()
                            .append("【SpringDoc】 分组[")
                            .append(groupName)
                            .append("]的basePackage未配置！")
                            .toString());
        }

        // 生成Bean名称
        String beanName = generateBeanName(groupName, basePackage);

        String finalGroupName = groupName;

        BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.genericBeanDefinition(
                        GroupedOpenApi.class,
                        () ->
                                GroupedOpenApi.builder()
                                        .group(finalGroupName) // 分组名称
                                        .packagesToScan(docketInfo.getBasePackage()) // 扫描的包
                                        .build());

        BeanDefinition beanDefinition = builder.getBeanDefinition();
        // 设置Bean的其他属性
        builder.setLazyInit(false);
        builder.setScope("singleton");
        registry.registerBeanDefinition(beanName, beanDefinition);
    }

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

    /** {@inheritDoc} */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {}
}
