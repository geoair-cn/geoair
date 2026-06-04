package cn.geoair.comp.knife4j.ext.core.config;

import cn.geoair.base.Gir;
import cn.geoair.base.util.GutilStr;

import java.util.Map;
//import jakarta.annotation.Resource;
import jakarta.annotation.Resource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * GirOpenApiRunnerConfig class.
 *
 * @author ：张俊
 * @date ：Created in 2022/8/29 9:32 @description： TODO
 * @version $Id: $Id
 */
@Component
public class GirOpenApiRunnerConfig implements ApplicationRunner {

    // @Resource
    // Environment environment;

    @Resource ApplicationContext applicationContext;

    /** {@inheritDoc} */
    @Override
    public void run(ApplicationArguments args) throws Exception {

        Map<String, GirOpenApiConfig> beansOfType =
                applicationContext.getBeansOfType(GirOpenApiConfig.class);
        if (beansOfType.isEmpty()) {
            return;
        }
        boolean isLoad = beansOfType.values().stream().anyMatch(GirOpenApiConfig::isLoad);
        if (!isLoad) {
            return;
        }

        String property = applicationContext.getEnvironment().getProperty("geoair.apidoc.enable");
        String enableAuth = applicationContext.getEnvironment().getProperty("geoair.apidoc.auth.enable-auth");
        String port = applicationContext.getEnvironment().getProperty("server.port");
        if (port == null) {
            port = "8080";
        }
        String contextPath = applicationContext.getEnvironment().getProperty("server.servlet.context-path");
        boolean hasContextPath = GutilStr.isNotBlank(contextPath);

        String baseUrl = "http://localhost:" + port + (hasContextPath ? contextPath : "");
        boolean isEnabled = "true".equals(property);

        Gir.log.info("");
        Gir.log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

        Gir.log.info("                    📖 API 接口文档服务                            ");
        Gir.log.info("══════════════════════════════════════════════════════════════════");
        Gir.log.info("  状态：{}", String.format("%-46s", isEnabled ? "✅ 已启用" : "❌ 未启用"));
        Gir.log.info("  地址：{}", String.format("%-46s", baseUrl + "/doc.html") + "");

        if ("true".equals(enableAuth)) {
            String username = Gir.property.getProperty("geoair.apidoc.auth.username", "admin");
            String password = Gir.property.getProperty("geoair.apidoc.auth.password", "123456");
            Gir.log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            Gir.log.info("  🔐 认证信息（Basic Auth）                                       ");
            Gir.log.info("  用户名：{}", String.format("%-42s", username));
            Gir.log.info("  密码    ：{}", String.format("%-42s", password));
        }

        Gir.log.info("══════════════════════════════════════════════════════════════════");
        Gir.log.info("");
    }
}
