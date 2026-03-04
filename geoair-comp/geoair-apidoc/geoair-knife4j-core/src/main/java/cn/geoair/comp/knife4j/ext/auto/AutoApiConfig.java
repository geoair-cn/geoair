package cn.geoair.comp.knife4j.ext.auto;

import cn.geoair.comp.knife4j.ext.config.GirSwaggerApiConfig;
import cn.geoair.comp.knife4j.ext.config.GirSwaggerProperties;
import cn.geoair.comp.knife4j.ext.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.model.DocketInfo;
import cn.geoair.base.Gir;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于配置自动生成Swagger Docket
 */

@ConditionalOnMissingBean(GirSwaggerApiConfig.class)
public class AutoApiConfig implements ApplicationContextAware, GirSwaggerApiConfig {

    public AutoApiConfig() {
        Gir.log.info("自动扫描控制器中。。。。。");
    }

    // Spring上下文对象
    private ApplicationContext applicationContext;

    // Swagger配置属性（通过GirBeanHelper获取）
    private GirSwaggerProperties swaggerProperties;


    /**
     * Spring自动注入ApplicationContext，并初始化配置属性
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        // 初始化配置属性
        this.swaggerProperties = applicationContext.getBean(GirSwaggerProperties.class);

    }


    /**
     * 获取控制器根包列表（优先级：手动多包 → 手动单包 → 自动提取）
     */
    private List<String> getControllerRootPackages() {
        List<String> rootPackages = new ArrayList<>();

        // 优先级1：手动配置的多根包
        if (swaggerProperties.getControllerRootPackages() != null
                && !swaggerProperties.getControllerRootPackages().isEmpty()) {
            rootPackages.addAll(swaggerProperties.getControllerRootPackages());
        }
        // 优先级2：手动配置的单根包
        else if (swaggerProperties.getControllerRootPackage() != null
                && !swaggerProperties.getControllerRootPackage().isEmpty()) {
            rootPackages.add(swaggerProperties.getControllerRootPackage());
        }
        // 优先级3：从启动类自动提取（拼接.controller）
        else {
            String autoRootPackage = getSpringBootRootPackage();
            if (autoRootPackage != null) {
                rootPackages.add(autoRootPackage + ".controller");
            }
        }

        return rootPackages;
    }

    /**
     * 核心：找到@SpringBootApplication标注的启动类，提取扫描根包
     */
    private String getSpringBootRootPackage() {
        Map<String, Object> bootBeans = applicationContext.getBeansWithAnnotation(SpringBootApplication.class);
        if (!bootBeans.isEmpty()) {
            Class<?> bootClass = bootBeans.values().iterator().next().getClass();
            // 处理CGLIB代理类（获取原始类）
            if (bootClass.getName().contains("$$")) {
                bootClass = bootClass.getSuperclass();
            }

            // 读取@SpringBootApplication的scanBasePackages属性
            SpringBootApplication bootAnnotation = AnnotationUtils.findAnnotation(bootClass,
                    SpringBootApplication.class);
            String[] scanPackages = bootAnnotation.scanBasePackages();
            if (scanPackages != null && scanPackages.length > 0 && !scanPackages[0].isEmpty()) {
                return scanPackages[0];
            } else {
                // 未指定scanBasePackages，取启动类所在包
                return bootClass.getPackage().getName();
            }
        }
        return null;
    }

    /**
     * 使用Reflections扫描指定包下的所有控制器类（支持缓存）
     */
    private Set<Class<?>> scanControllerClasses(String scanPackage) {
        try {
            ConfigurationBuilder configBuilder = new ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forPackage(scanPackage)).setScanners(Scanners.TypesAnnotated);

            Reflections reflections = new Reflections(configBuilder);

            Set<Class<?>> controllerClasses = new HashSet<>();
            controllerClasses.addAll(reflections.getTypesAnnotatedWith(Controller.class));
            controllerClasses.addAll(reflections.getTypesAnnotatedWith(RestController.class));
            return controllerClasses;
        } catch (Exception e) {
            // 包不存在/扫描失败时返回空集合
            return new HashSet<>();
        }
    }

    /**
     * 提取控制器包名 + 过滤排除包
     */
    private Set<String> extractAndFilterPackages(Set<Class<?>> classes) {
        // 提取所有包名并去重
        Set<String> allPackages = classes.stream().map(Class::getPackage).map(Package::getName)
                .collect(Collectors.toSet());

        // 过滤排除包
        List<String> excludePackages = swaggerProperties.getExcludePackages();
        if (excludePackages == null || excludePackages.isEmpty()) {
            return allPackages;
        }

        return allPackages.stream().filter(pkg -> !excludePackages.contains(pkg)) // 精准匹配排除
                .collect(Collectors.toSet());
    }

    /**
     * 自定义包排序（优先配置的固定顺序 → 字母序）
     */
    private List<String> sortPackages(Set<String> packages) {
        List<String> sorted = new ArrayList<>();
        List<String> fixedOrderPackages = swaggerProperties.getFixedOrderPackages();

        // 优先级1：配置的固定顺序包
        if (fixedOrderPackages != null && !fixedOrderPackages.isEmpty()) {
            for (String fixedPkg : fixedOrderPackages) {
                if (packages.contains(fixedPkg)) {
                    sorted.add(fixedPkg);
                    packages.remove(fixedPkg);
                }
            }
        }

        // 优先级2：剩余包按字母序排序
        sorted.addAll(packages.stream().sorted(String::compareTo).collect(Collectors.toList()));

        return sorted;
    }

    /**
     * 构建分组名（整合配置项：前缀、序号、包名规则）
     */
    private String buildGroupName(int index, String packageName) {
        StringBuilder groupName = new StringBuilder();

        // 1. 添加分组名前缀
        String prefix = swaggerProperties.getGroupNamePrefix();
        if (prefix != null && !prefix.isEmpty()) {
            groupName.append(prefix);
        }

        // 2. 添加分组序号（可选）
        if (swaggerProperties.isEnableGroupIndex()) {
            groupName.append(index).append("-");
        }

        // 3. 添加包名（最后一级/完整包名，根据配置）
        String pkgPart = swaggerProperties.isGroupNameUseLastPackage() ? getLastPackageName(packageName) : packageName;
        groupName.append(pkgPart);

        return groupName.toString();
    }

    /**
     * 美化分组名（截取包名最后一级，首字母大写）
     */
    private String getLastPackageName(String packageName) {
        if (packageName == null || !packageName.contains(".")) {
            return packageName;
        }
        String[] parts = packageName.split("\\.");
        String lastPart = parts[parts.length - 1];
        return lastPart.substring(0, 1).toUpperCase() + lastPart.substring(1).toLowerCase();
    }

    @Override
    public List<DocketInfo> getDocketInfos() {
        // 1. 校验Swagger开关是否开启
        if (!swaggerProperties.isEnable()) {
            return new ArrayList<>();
        }


        // 3. 获取控制器根包（优先手动配置 → 自动提取）
        List<String> controllerRootPackages = getControllerRootPackages();
        if (controllerRootPackages.isEmpty()) {
            throw new RuntimeException("未配置控制器根包，且未找到@SpringBootApplication启动类！");
        }

        // 4. 扫描所有根包下的控制器类
        Set<Class<?>> allControllerClasses = new HashSet<>();
        for (String rootPackage : controllerRootPackages) {
            Set<Class<?>> controllerClasses = scanControllerClasses(rootPackage);
            if (controllerClasses.isEmpty()) {
                // 降级扫描根包（兼容controller子包不存在的情况）
                String parentPackage = rootPackage.contains(".controller")
                        ? rootPackage.substring(0, rootPackage.lastIndexOf(".controller")) : rootPackage;
                controllerClasses = scanControllerClasses(parentPackage);
            }
            allControllerClasses.addAll(controllerClasses);
        }

        // 5. 校验扫描结果
        if (allControllerClasses.isEmpty()) {
            throw new RuntimeException("在指定根包[" + controllerRootPackages + "]下未扫描到任何@Controller/@RestController类！");
        }

        // 6. 提取控制器所属包名并去重 → 过滤排除包
        Set<String> controllerPackages = extractAndFilterPackages(allControllerClasses);

        // 7. 对包名排序（优先配置的固定顺序 → 字母序）
        List<String> sortedPackages = sortPackages(controllerPackages);

        // 8. 遍历生成Docket
//        Map<String, Docket> docketMap = new LinkedHashMap<>();
        List<DocketInfo> docketMap = new ArrayList<>();
        for (int i = 0; i < sortedPackages.size(); i++) {
            String packageName = sortedPackages.get(i);
            // 生成自定义分组名（整合配置项：前缀、序号、包名规则）
            String groupName = buildGroupName(i + 1, packageName);
            DocketInfo docketInfo = new DocketInfo(groupName, packageName, "", "");
            docketMap.add(docketInfo);
        }

        return docketMap;
    }

    @Override
    public ApiModelInfo getApiModelInfo() {
        return new ApiModelInfo(swaggerProperties.getTitle(),
                swaggerProperties.getDescription(), swaggerProperties.getAuthor(), swaggerProperties.getVersion());

    }
}
