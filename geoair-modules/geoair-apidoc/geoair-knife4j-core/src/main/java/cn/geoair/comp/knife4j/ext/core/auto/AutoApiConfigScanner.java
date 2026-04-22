package cn.geoair.comp.knife4j.ext.core.auto;

import cn.geoair.base.Gir;
import cn.geoair.comp.knife4j.ext.core.config.GirOpenApiConfig;
import cn.geoair.comp.knife4j.ext.core.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.core.model.DocketInfo;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自动扫描控制器类，并生成DocketInfo列表
 *
 * @author Administrator
 * @version $Id: $Id
 */
@Slf4j
public class AutoApiConfigScanner extends GirOpenApiConfig {

    private ApplicationContext applicationContext;

    private Environment environment;

    /**
     * Constructor for AutoApiConfigScanner.
     *
     * @param applicationContext a {@link org.springframework.context.ApplicationContext} object
     */
    public AutoApiConfigScanner(ApplicationContext applicationContext) {
        Gir.log.info("自动扫描控制器中。。。。。");
        this.applicationContext = applicationContext;
        this.environment = applicationContext.getEnvironment();
    }

    /** 获取控制器根包列表（优先级：手动多包 → 手动单包 → 自动提取） */
    private List<String> getControllerRootPackages() {
        List<String> rootPackages = new ArrayList<>();

        // 优先级1：手动配置的多根包
        String[] multiPackages =
                environment.getProperty(
                        "geoair.apidoc.controllerRootPackages", String[].class, new String[0]);
        if (multiPackages.length > 0) {
            rootPackages.addAll(Arrays.asList(multiPackages));
        }
        // 优先级2：手动配置的单根包
        else {
            String singlePackage =
                    environment.getProperty("geoair.apidoc.controllerRootPackage", "");
            if (!singlePackage.isEmpty()) {
                rootPackages.add(singlePackage);
            }
            // 优先级3：从启动类自动提取（拼接.controller）
            else {
                String autoRootPackage = getSpringBootRootPackage();
                if (autoRootPackage != null) {
                    rootPackages.add(autoRootPackage + ".controller");
                }
            }
        }

        return rootPackages;
    }

    /** 核心：找到@SpringBootApplication标注的启动类，提取扫描根包 */
    private String getSpringBootRootPackage() {
        Map<String, Object> bootBeans =
                applicationContext.getBeansWithAnnotation(SpringBootApplication.class);
        if (!bootBeans.isEmpty()) {
            Class<?> bootClass = bootBeans.values().iterator().next().getClass();
            // 处理CGLIB代理类（获取原始类）
            if (bootClass.getName().contains("$$")) {
                bootClass = bootClass.getSuperclass();
            }

            // 读取@SpringBootApplication的scanBasePackages属性
            SpringBootApplication bootAnnotation =
                    AnnotationUtils.findAnnotation(bootClass, SpringBootApplication.class);
            String[] scanPackages = null;
            if (bootAnnotation != null) {
                scanPackages = bootAnnotation.scanBasePackages();
            }
            if (scanPackages != null && scanPackages.length > 0 && !scanPackages[0].isEmpty()) {
                return scanPackages[0];
            } else {
                // 未指定scanBasePackages，取启动类所在包
                return bootClass.getPackage().getName();
            }
        }
        return null;
    }

    /** 使用Reflections扫描指定包下的所有控制器类（支持缓存） */
    private Set<Class<?>> scanControllerClasses(String scanPackage) {
        try {
            ConfigurationBuilder configBuilder =
                    new ConfigurationBuilder()
                            .setUrls(ClasspathHelper.forPackage(scanPackage))
                            .setScanners(Scanners.TypesAnnotated);

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

    /** 提取控制器包名 + 过滤排除包 */
    private Set<String> extractAndFilterPackages(Set<Class<?>> classes) {
        // 提取所有包名并去重
        Set<String> allPackages =
                classes.stream()
                        .map(Class::getPackage)
                        .map(Package::getName)
                        .collect(Collectors.toSet());

        // 过滤排除包
        String[] excludePackages =
                environment.getProperty(
                        "geoair.apidoc.excludePackages", String[].class, new String[0]);
        if (excludePackages.length == 0) {
            return allPackages;
        }

        List<String> excludeList = Arrays.asList(excludePackages);
        return allPackages
                .stream()
                .filter(pkg -> !excludeList.contains(pkg)) // 精准匹配排除
                .collect(Collectors.toSet());
    }

    /** 自定义包排序（优先配置的固定顺序 → 字母序） */
    private List<String> sortPackages(Set<String> packages) {
        List<String> sorted = new ArrayList<>();
        String[] fixedOrderPackages =
                environment.getProperty(
                        "geoair.apidoc.fixedOrderPackages", String[].class, new String[0]);

        // 优先级1：配置的固定顺序包
        for (String fixedPkg : fixedOrderPackages) {
            if (packages.contains(fixedPkg)) {
                sorted.add(fixedPkg);
                packages.remove(fixedPkg);
            }
        }

        // 优先级2：剩余包按字母序排序
        sorted.addAll(packages.stream().sorted(String::compareTo).collect(Collectors.toList()));

        return sorted;
    }

    /** 构建分组名（整合配置项：前缀、序号、包名规则） */
    private String buildGroupName(int index, String packageName) {
        StringBuilder groupName = new StringBuilder();

        // 1. 添加分组名前缀（默认空）
        String prefix = environment.getProperty("geoair.apidoc.groupNamePrefix", "");
        if (prefix != null && !prefix.isEmpty()) {
            groupName.append(prefix);
        }

        // 2. 添加分组序号（可选，默认true）
        boolean enableIndex =
                environment.getProperty("geoair.apidoc.enableGroupIndex", Boolean.class, true);
        if (enableIndex) {
            groupName.append(index).append("-");
        }

        // 3. 添加包名（最后一级/完整包名，根据配置，默认true）
        boolean useLastPackage =
                environment.getProperty(
                        "geoair.apidoc.groupNameUseLastPackage", Boolean.class, true);
        String pkgPart = useLastPackage ? getLastPackageName(packageName) : packageName;
        groupName.append(pkgPart);

        return groupName.toString();
    }

    /** 美化分组名（截取包名最后一级，首字母大写） */
    private String getLastPackageName(String packageName) {
        if (packageName == null || !packageName.contains(".")) {
            return packageName;
        }
        String[] parts = packageName.split("\\.");
        String lastPart = parts[parts.length - 1];
        return lastPart.substring(0, 1).toUpperCase() + lastPart.substring(1).toLowerCase();
    }

    /** {@inheritDoc} */
    @Override
    public List<DocketInfo> getDocketInfos() {
        // 1. 校验Swagger开关是否开启（默认false）
        boolean enable = environment.getProperty("geoair.apidoc.enable", Boolean.class, false);
        if (!enable) {
            return new ArrayList<>();
        }

        // 2. 获取控制器根包（优先手动配置 → 自动提取）
        List<String> controllerRootPackages = getControllerRootPackages();
        log.info("获取控制器根包：{}", controllerRootPackages);
        if (controllerRootPackages.isEmpty()) {
            throw new RuntimeException("未配置控制器根包，且未找到@SpringBootApplication启动类！");
        }

        // 3. 扫描所有根包下的控制器类
        Set<Class<?>> allControllerClasses = new HashSet<>();
        for (String rootPackage : controllerRootPackages) {
            Set<Class<?>> controllerClasses = scanControllerClasses(rootPackage);
            if (controllerClasses.isEmpty()) {
                // 降级扫描根包（兼容controller子包不存在的情况）
                String parentPackage =
                        rootPackage.contains(".controller")
                                ? rootPackage.substring(0, rootPackage.lastIndexOf(".controller"))
                                : rootPackage;
                controllerClasses = scanControllerClasses(parentPackage);
            }
            allControllerClasses.addAll(controllerClasses);
        }

        // 4. 校验扫描结果
        if (allControllerClasses.isEmpty()) {
            return new ArrayList<>();
        }

        // 5. 提取控制器所属包名并去重 → 过滤排除包
        Set<String> controllerPackages = extractAndFilterPackages(allControllerClasses);

        // 6. 对包名排序（优先配置的固定顺序 → 字母序）
        List<String> sortedPackages = sortPackages(controllerPackages);

        // 7. 遍历生成Docket
        List<DocketInfo> docketList = new ArrayList<>();
        for (int i = 0; i < sortedPackages.size(); i++) {
            String packageName = sortedPackages.get(i);
            // 生成自定义分组名（整合配置项：前缀、序号、包名规则）
            String groupName = buildGroupName(i + 1, packageName);
            DocketInfo docketInfo = new DocketInfo(groupName, packageName, "", "");
            docketList.add(docketInfo);
        }

        return docketList;
    }

    /** {@inheritDoc} */
    @Override
    public ApiModelInfo getApiModelInfo() {
        // 读取配置，设置默认值
        String title = environment.getProperty("geoair.apidoc.title", "API 在线文档");
        String description = environment.getProperty("geoair.apidoc.description", "API文档 V1.0");
        String author = environment.getProperty("geoair.apidoc.author", "geoair");
        String version = environment.getProperty("geoair.apidoc.version", "J8-dev-SNAPSHOT");

        return new ApiModelInfo(title, description, author, version);
    }
}
