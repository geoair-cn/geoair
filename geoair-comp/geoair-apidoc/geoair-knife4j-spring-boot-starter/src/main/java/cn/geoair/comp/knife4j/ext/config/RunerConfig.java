package cn.geoair.comp.knife4j.ext.config;


import cn.geoair.gtc.base.Gir;
import cn.geoair.gtc.base.util.GutilStr;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

/**
 * @author ：张俊
 * @date ：Created in 2022/8/29 9:32
 * @description： TODO
 */
@Configuration
public class RunerConfig implements ApplicationRunner {
    @Resource
    Environment environment;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String path = "localhost:{}";
        String port = environment.getProperty("server.port");
        if (port == null) {
            port = "8080";
        }
        String context_path = environment.getProperty("server.servlet.context-path");
        boolean notBlank = GutilStr.isNotBlank(context_path);
        if (notBlank) {
            path = path + context_path;
        }
        Gir.log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        Gir.log.info("接口文档地址：" + path + "{}", port, "/doc.html");
        Gir.log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
    }
}
