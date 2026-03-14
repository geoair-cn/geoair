package cn.geoair.map.dynamic.dbservice.starter.run;

import cn.geoair.base.Gir;
import cn.geoair.base.util.GutilStr;
import cn.geoair.map.dynamic.dbservice.core.config.GirDsServiceProperties;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

/**
 * @author ：张俊
 * @date ：Created in 2022/8/29 9:32 @description： TODO
 */
@Configuration
public class DsRunerConfig implements ApplicationRunner {

	@Resource
	GirDsServiceProperties girDsServiceProperties;

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
		Gir.log.info("dsApi服务地址：" + path + "{}，登录启用状态：{}", port, "/dsView/index.html",
				girDsServiceProperties.isEnableLogin());
		Gir.log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
	}

}
