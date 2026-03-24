package cn.geoair.comp.knife4j.ext.core.config;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import cn.geoair.base.Gir;
import cn.geoair.base.util.GutilStr;

/**
 * @author ：张俊
 * @date ：Created in 2022/8/29 9:32 @description： TODO
 */
@Component
public class GirOpenApiRunnerConfig implements ApplicationRunner {

//    @Resource
//    Environment environment;

    @Resource
    ApplicationContext applicationContext;

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
        String path = "localhost:{}";
        String port = applicationContext.getEnvironment().getProperty("server.port");
        if (port == null) {
            port = "8080";
        }
        String context_path = applicationContext.getEnvironment().getProperty("server.servlet.context-path");
        boolean notBlank = GutilStr.isNotBlank(context_path);
        if (notBlank) {
            path = path + context_path;
        }
        Gir.log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        Gir.log.info("接口文档地址：" + path + "{} 启用状态：{}", port, "/doc.html",
                "true".equals(property));
        Gir.log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
    }

}
