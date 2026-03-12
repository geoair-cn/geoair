package cn.geoair.comp.knife4j.ext.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "geoair.apidoc")
@Data
public class GirSwaggerProperties {

    /** 是否启用swagger注解（全局开关） */
    private boolean enable = false;

    /** API版本号，默认为空 */
    private String version = "1.0";

    /** API标题，默认为空 */
    private String title = "API 在线文档";

    /** API作者，默认为空 */
    private String author = "geoair";

    /** API描述，默认为空 */
    private String description = "API文档 V1.0";

    /** 手动指定控制器根包（优先级高于从SpringBootApplication自动提取） 示例：com.gtc.gishubteam.editor.wcs.controller */
    private String controllerRootPackage;

    /** 控制器根包列表（支持多根包扫描） 示例：["com.gtc.controller.web", "com.gtc.controller.base"] */
    private List<String> controllerRootPackages = new ArrayList<>();

    /**
     * 扫描时需要排除的包（支持通配符，如：com.gtc.controller.test*） 示例：["com.gtc.controller.test",
     * "com.gtc.controller.demo"]
     */
    private List<String> excludePackages = new ArrayList<>();

    /** 分组固定排序的包列表（优先级高于字母序） 示例：["com.gtc.controller.web", "com.gtc.controller.base"] */
    private List<String> fixedOrderPackages = new ArrayList<>();

    /** 分组名称前缀（如："业务模块-"，最终分组名：业务模块-Web） */
    private String groupNamePrefix = "";

    /** 是否按包名最后一级生成分组名（true：com.gtc.controller.web → Web；false：使用完整包名） */
    private boolean groupNameUseLastPackage = true;

    /** 是否启用分组序号（true：1-Web；false：Web） */
    private boolean enableGroupIndex = true;

    /** 扫描缓存目录（默认：target/reflections-cache） */
    private String scanCacheDir = "target/reflections-cache";
}
