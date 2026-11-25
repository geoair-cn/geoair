package cn.geoair.comp.db.service.starter.run;

import cn.geoair.base.Gir;
import cn.geoair.base.util.GutilStr;
import cn.geoair.comp.db.service.core.config.GirDsServiceProperties;

import jakarta.annotation.Resource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author ：张俊
 * @date ：Created in 2022/8/29 9:32 @description： TODO
 */
@Configuration
public class DsRunerConfig implements ApplicationRunner {

    @Resource GirDsServiceProperties girDsServiceProperties;

    @Resource Environment environment;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String port = environment.getProperty("server.port");
        if (port == null) {
            port = "8080";
        }
        String contextPath = environment.getProperty("server.servlet.context-path");
        boolean hasContextPath = GutilStr.isNotBlank(contextPath);

        String baseUrl =
                String.format("http://localhost:%s%s", port, hasContextPath ? contextPath : "");
        String dsViewUrl = baseUrl + "/dsView/index.html";
        boolean isLoginEnabled = girDsServiceProperties.isEnableLogin();

        Gir.log.info("");
        Gir.log.info(
                "$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        Gir.log.info("              📊 DS Api 服务启动成功");
        Gir.log.info("");
        Gir.log.info("  🌐 访问地址：{}", dsViewUrl);
        Gir.log.info("  🔐 登录认证：{}", isLoginEnabled ? "✅ 开启" : "❌ 关闭");
        Gir.log.info(
                "$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        Gir.log.info("");
    }
}
