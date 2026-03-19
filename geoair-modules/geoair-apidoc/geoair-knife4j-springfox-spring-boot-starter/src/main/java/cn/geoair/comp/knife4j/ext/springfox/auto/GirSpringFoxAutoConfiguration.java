package cn.geoair.comp.knife4j.ext.springfox.auto;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GirSpringFoxAutoConfiguration {

	/** 注册Docket动态注册器 这个Bean会在Spring容器启动早期执行，动态创建所有的Docket */
	@Bean
	public static GirSpringFoxDocketRunner springFoxDocketRunner() {
		return new GirSpringFoxDocketRunner();
	}

}
