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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SwaggerUiConfigProperties;
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

/**
 * SpringDoc API 文档自动配置运行器。
 *
 * <p>本类负责在 Spring 容器启动时自动扫描 {@link GirOpenApiConfig} 配置类， 并根据配置注册 {@link GroupedOpenApi} 分组
 * Bean，以实现 API 文档的分组展示。
 *
 * <p>核心功能：
 *
 * <ul>
 *   <li>自动扫描并注册 OpenAPI 分组配置
 *   <li>提供默认的 OpenAPI 文档信息
 *   <li>配置 SpringDoc 和 Swagger UI 属性
 *   <li>当未找到自定义配置时，回退到自动扫描控制器包
 * </ul>
 *
 * <p>使用方式：
 *
 * <ul>
 *   <li>实现 {@link GirOpenApiConfig} 接口来自定义分组配置
 *   <li>通过 {@code geoair.apidoc.enable} 属性启用/禁用文档
 *   <li>通过 {@code geoair.apidoc.controllerRootPackages} 配置控制器扫描路径
 * </ul>
 *
 * @author Administrator
 * @version $Id: $Id
 * @see GirOpenApiConfig
 * @see GroupedOpenApi
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GirSpringDocApiRunner
        implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    /** 日志记录器 */
    public static GiLogger log = GirLoggerFactory.getLogger();

    /** Spring 应用上下文 */
    private ApplicationContext applicationContext;

    /** 分组名映射，用于检测重复的分组名 */
    private final Map<String, Integer> groupNameMap = new HashMap<>();

    /**
     * 设置 Spring 应用上下文。
     *
     * @param applicationContext Spring 应用上下文实例
     * @throws BeansException 如果设置过程中发生错误
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 创建默认的 OpenAPI 文档配置。
     *
     * <p>当容器中不存在 {@link OpenAPI} Bean 时，此方法将被调用， 基于 {@link GirOpenApiConfig} 中的 {@link
     * ApiModelInfo} 创建默认配置。
     *
     * @return 配置好的 OpenAPI 实例
     */
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

    /**
     * 创建 SpringDoc 配置属性 Bean。
     *
     * <p>配置 API 文档的基本属性，包括路径、分组设置等。 同时会根据 {@link GirOpenApiConfig} 中的分组信息， 自动创建对应的 {@link
     * SpringDocConfigProperties.GroupConfig}。
     *
     * @return 配置好的 SpringDocConfigProperties 实例
     */
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

        List<SpringDocConfigProperties.GroupConfig> groupConfigs = properties.getGroupConfigs();
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

    /**
     * 创建 Swagger UI 配置属性 Bean。
     *
     * <p>配置 Swagger UI 的显示属性，包括排序方式、模型展开深度等。 文档的启用状态由 {@code geoair.apidoc.enable} 属性控制。
     *
     * @return 配置好的 SwaggerUiConfigProperties 实例
     */
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

    /**
     * Bean 定义注册表后处理器回调方法。
     *
     * <p>在所有 Bean 定义加载完成后执行，负责扫描 {@link GirOpenApiConfig} 配置类 并注册对应的 {@link GroupedOpenApi} Bean。
     *
     * <p>执行流程：
     *
     * <ol>
     *   <li>清空分组名映射
     *   <li>解析所有可用的 {@link GirOpenApiConfig} 配置
     *   <li>遍历配置并注册对应的分组 Bean
     * </ol>
     *
     * @param registry Bean 定义注册表
     * @throws BeansException 如果处理过程中发生错误
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
            throws BeansException {
        if (applicationContext == null) {
            throw new RuntimeException("ApplicationContext初始化失败！请检查Spring配置");
        }

        groupNameMap.clear();
        for (GirOpenApiConfig apiConfig : resolveApiConfigs(registry)) {
            registerDocketsFromConfig(apiConfig, registry);
        }
    }

    /**
     * 根据配置注册分组 Bean。
     *
     * <p>从 {@link GirOpenApiConfig} 中获取分组信息，并为每个分组注册对应的 {@link GroupedOpenApi} Bean。
     *
     * @param apiConfig API 配置实例
     * @param registry Bean 定义注册表
     */
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

    /**
     * 注册单个分组 Bean。
     *
     * <p>根据 {@link DocketInfo} 创建并注册 {@link GroupedOpenApi} Bean。 会进行分组名重复检查，并生成唯一的 Bean 名称。
     *
     * @param apiModelInfo API 模型信息
     * @param docketInfo 分组配置信息
     * @param registry Bean 定义注册表
     * @throws GirException 如果分组名重复
     */
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

    /**
     * 解析分组名称。
     *
     * <p>优先使用 {@link DocketInfo} 中配置的 groupName， 如果未配置，则从 basePackage 中提取最后一级作为分组名。
     *
     * @param docketInfo 分组配置信息
     * @return 解析后的分组名称
     */
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

    /**
     * 解析所有可用的 API 配置（在 Bean 实例化阶段调用）。
     *
     * <p>从 Spring 容器中获取 {@link GirOpenApiConfig} 类型的 Bean， 如果未找到，则使用 {@link AutoApiConfigScanner}
     * 作为回退方案进行自动扫描。
     *
     * <p>此方法在 {@code @Bean} 方法中调用，此时 Bean 已经被实例化。
     *
     * @return API 配置列表
     */
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

    /**
     * 解析所有可用的 API 配置（在 Bean 定义注册阶段调用）。
     *
     * <p>通过扫描 {@link BeanDefinitionRegistry} 查找所有 {@link GirOpenApiConfig} 实现类，
     * 然后手动实例化这些配置类。如果未找到，则使用 {@link AutoApiConfigScanner} 作为回退方案进行自动扫描。
     *
     * <p>注意：此方法在 {@link #postProcessBeanDefinitionRegistry} 中调用， 此时 {@link
     * org.springframework.context.annotation.ConfigurationClassPostProcessor} 尚未处理
     * {@code @Configuration} 类，因此需要手动扫描和实例化配置类。
     *
     * @param registry Bean 定义注册表
     * @return API 配置列表
     */
    private List<GirOpenApiConfig> resolveApiConfigs(BeanDefinitionRegistry registry) {
        List<GirOpenApiConfig> configs = new ArrayList<>();

        // 扫描所有 Bean 定义，查找 GirOpenApiConfig 实现类
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            String className = beanDefinition.getBeanClassName();
            if (className == null) {
                continue;
            }

            try {
                Class<?> beanClass = Class.forName(className);
                if (GirOpenApiConfig.class.isAssignableFrom(beanClass)
                        && !beanClass.isInterface()
                        && !java.lang.reflect.Modifier.isAbstract(beanClass.getModifiers())) {
                    // 实例化配置类
                    GirOpenApiConfig config =
                            (GirOpenApiConfig) beanClass.getDeclaredConstructor().newInstance();
                    configs.add(config);
                    log.debug("【SpringDoc】发现自定义配置类：{}", className);
                }
            } catch (ClassNotFoundException e) {
                log.debug("【SpringDoc】类未找到：{}，跳过", className);
            } catch (Exception e) {
                log.warn("【SpringDoc】实例化配置类失败：{}", className, e);
            }
        }

        if (configs.isEmpty()) {
            log.debug("【SpringDoc】未找到自定义 GirOpenApiConfig 配置，使用自动扫描");
            List<GirOpenApiConfig> fallback = new ArrayList<>();
            fallback.add(new AutoApiConfigScanner(applicationContext));
            return fallback;
        }

        log.info("【SpringDoc】找到 {} 个自定义配置", configs.size());
        return configs;
    }

    /**
     * 解析所有分组配置信息。
     *
     * <p>遍历所有 {@link GirOpenApiConfig} 配置，收集并返回所有的 {@link DocketInfo}。
     *
     * @return 分组配置信息列表
     */
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

    /**
     * 解析 API 模型信息。
     *
     * <p>从第一个可用的 {@link GirOpenApiConfig} 中获取 {@link ApiModelInfo}， 如果都未配置，则返回默认值。
     *
     * @return API 模型信息
     */
    private ApiModelInfo resolveApiModelInfo() {
        for (GirOpenApiConfig apiConfig : resolveApiConfigs()) {
            ApiModelInfo apiModelInfo = apiConfig.getApiModelInfo();
            if (apiModelInfo != null) {
                return apiModelInfo;
            }
        }
        return new ApiModelInfo("API文档", "API描述", "API", "1.0.0");
    }

    /**
     * 生成唯一的 Bean 名称。
     *
     * <p>基于分组名和包名的哈希值生成唯一的 Bean 名称， 确保不会与现有 Bean 名称冲突。
     *
     * @param groupName 分组名称
     * @param basePackage 基础包名
     * @return 生成的 Bean 名称
     */
    private String generateBeanName(String groupName, String basePackage) {
        String cleanGroupName = groupName.replaceAll("[^a-zA-Z0-9]", "");
        if (cleanGroupName.isEmpty()) {
            cleanGroupName = "GroupedOpenApi";
        }
        int packageHash = Math.abs(basePackage.hashCode() % 1000);
        return cleanGroupName + "GroupedOpenApi_" + packageHash;
    }

    /**
     * Bean 工厂后处理器回调方法。
     *
     * <p>此方法目前为空实现，预留给未来的扩展使用。
     *
     * @param beanFactory 可配置的列表Bean工厂
     * @throws BeansException 如果处理过程中发生错误
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {}
}
