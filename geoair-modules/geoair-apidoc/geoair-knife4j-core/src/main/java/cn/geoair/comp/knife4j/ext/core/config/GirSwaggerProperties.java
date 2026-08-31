package cn.geoair.comp.knife4j.ext.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 仅作为配置提示使用， */
@Data
@Component
@ConfigurationProperties(prefix = "geoair.apidoc")
public class GirSwaggerProperties {

    /** Constructor for GirSwaggerProperties. */
    public GirSwaggerProperties() {}

    /** 是否启用swagger注解（全局开关） */
    private boolean enable = false;

    /** 鉴权处理 */
    ApiDocAuth auth = new ApiDocAuth();

    /** API版本号，默认为空 */
    private String version = "J8.1.6";

    /** API标题，默认为空 */
    private String title = "API 在线文档";

    /** API作者，默认为空 */
    private String author = "geoair";

    /** API描述，默认为空 */
    private String description = "API文档 VJ8.1.6";

    /** 手动指定控制器根包（优先级高于从SpringBootApplication自动提取） 示例：com.gtc.gishubteam.editor.wcs.controller */
    private String controllerRootPackage;

    /** 控制器根包列表（支持多根包扫描） 示例：["com.gtc.controller.web", "com.gtc.controller.base"] */
    private String[] controllerRootPackages = new String[0];

    /**
     * 扫描时需要排除的包（支持通配符，如：com.gtc.controller.test*） 示例：["com.gtc.controller.test",
     * "com.gtc.controller.demo"]
     */
    private String[] excludePackages = new String[0];

    /** 分组固定排序的包列表（优先级高于字母序） 示例：["com.gtc.controller.web", "com.gtc.controller.base"] */
    private String[] fixedOrderPackages = new String[0];

    /** 分组名称前缀（如："业务模块-"，最终分组名：业务模块-Web） */
    private String groupNamePrefix = "";

    /** 是否按包名最后一级生成分组名（true：com.gtc.controller.web → Web；false：使用完整包名） */
    private boolean groupNameUseLastPackage = true;

    /** 是否启用分组序号（true：1-Web；false：Web） */
    private boolean enableGroupIndex = true;

    @Data
    public static class ApiDocAuth {
        /** 是否启用swagger的鉴权 */
        private boolean enableAuth = false;

        /** 接口文档的认证用户名 */
        private String username = "admin";
        /** 接口文档的认证密码 */
        private String password = "123456";
    }
}
