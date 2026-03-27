package cn.geoair.comp.knife4j.ext.springfox.auto;

import cn.geoair.base.exception.GirException;
import cn.geoair.comp.knife4j.ext.core.auto.AutoApiConfigScanner;
import cn.geoair.comp.knife4j.ext.core.config.GirOpenApiConfig;
import cn.geoair.comp.knife4j.ext.core.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.core.model.DocketInfo;
import cn.geoair.comp.knife4j.ext.springfox.service.SpringAddtionalModelUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import springfox.documentation.spring.web.plugins.Docket;

/** SpringFox Docket 动态注册器 在Spring容器启动早期扫描GirSwaggerApiConfig实现类，动态创建并注册Docket Bean */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GirSpringFoxDocketRunner
        implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(GirSpringFoxDocketRunner.class);

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
        Map<String, GirOpenApiConfig> apiConfigMap =
                applicationContext.getBeansOfType(GirOpenApiConfig.class);

        if (apiConfigMap.isEmpty()) {

            // log.debug("【SpringFox】未找到任何 GirSwaggerApiConfig 实现，跳过Docket注册");
            // return;

            apiConfigMap = new HashMap<>();
            apiConfigMap.put("byAutoScanner", new AutoApiConfigScanner(applicationContext));
        }

        // 遍历每个配置，注册对应的Docket
        for (Map.Entry<String, GirOpenApiConfig> entry : apiConfigMap.entrySet()) {
            String configBeanName = entry.getKey();
            GirOpenApiConfig apiConfig = entry.getValue();

            // System.out.println("【SpringFox】开始处理配置: " + configBeanName);
            registerDocketsFromConfig(apiConfig, registry);
        }
    }

    /** 从单个配置中注册所有Docket */
    private void registerDocketsFromConfig(
            GirOpenApiConfig apiConfig, BeanDefinitionRegistry registry) {
        // 获取DocketInfo列表
        apiConfig.doLoading();
        List<DocketInfo> docketInfos = apiConfig.getDocketInfos();
        if (docketInfos == null || docketInfos.isEmpty()) {
            log.debug("【SpringFox】未配置任何DocketInfo，跳过Docket注册");
            return;
        }

        // 获取API基本信息
        ApiModelInfo apiModelInfo = apiConfig.getApiModelInfo();
        if (apiModelInfo == null) {
            // 提供默认的API信息，避免启动失败
            apiModelInfo = new ApiModelInfo("API文档", "API描述", "API", "1.0.0");
            log.debug("【SpringFox】未配置ApiModelInfo，使用默认配置");
        }

        // 遍历每个DocketInfo，创建并注册Docket
        for (DocketInfo docketInfo : docketInfos) {
            registerSingleDocket(apiModelInfo, docketInfo, registry);
        }
        apiConfig.loadEnd();
    }

    HashMap<String, Integer> groupNameMap = new HashMap<>();

    /** 注册单个Docket */
    private void registerSingleDocket(
            ApiModelInfo apiModelInfo, DocketInfo docketInfo, BeanDefinitionRegistry registry) {
        // 处理分组名
        String groupName = null;
        {
            groupName = docketInfo.getGroupName();
            if (groupName == null || groupName.trim().isEmpty()) {
                groupName = "default";
            }
            if (groupNameMap.containsKey(groupName)) {
                throw new GirException("分组名重复：{}", groupName);
            }
            // groupNameMap.put(groupName, groupNameMap.getOrDefault(groupName, 0) + 1);

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
                        () ->
                                SpringAddtionalModelUtils.createApi(apiModelInfo, docketInfo)
                                        .groupName(finalGroupName)
                                        .enable(enable));

        // 设置Bean的其他属性
        docketBuilder.setLazyInit(false); // 非懒加载
        docketBuilder.setScope("singleton"); // 单例模式

        // 注册Bean定义到容器
        registry.registerBeanDefinition(beanName, docketBuilder.getBeanDefinition());

        // System.out.println(
        // "【SpringFox】成功注册Docket: "
        // + beanName
        // + ", 分组: "
        // + groupName
        // + ", 扫描包: "
        // + basePackage);
    }

    /** 生成唯一的Bean名称 */
    private String generateBeanName(String groupName, String basePackage) {
        // 清理特殊字符，确保Bean名称合法
        String cleanGroupName = groupName;
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
