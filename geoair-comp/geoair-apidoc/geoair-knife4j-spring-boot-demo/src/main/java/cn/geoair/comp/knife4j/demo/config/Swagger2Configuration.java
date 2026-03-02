package cn.geoair.comp.knife4j.demo.config;

import cn.geoair.comp.knife4j.ext.config.GirSwaggerApiConfig;
import cn.geoair.comp.knife4j.ext.config.GirSwaggerProperties;
import cn.geoair.comp.knife4j.ext.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.model.DocketInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.spring.web.plugins.Docket;

import javax.annotation.Resource;

@Configuration
public class Swagger2Configuration extends GirSwaggerApiConfig {

	@Resource
	private GirSwaggerProperties girSwaggerProperties;

	private final ApiModelInfo apiModelInfo = new ApiModelInfo("demo 在线文档", "demo在线文档", "demo", "1.0");

	@Bean
	public Docket createdemo1ApiServer() {
		DocketInfo docketInfo = new DocketInfo("demo1分组", "cn.geoair.comp.knife4j.demo.controller.group1");
		return createApi(apiModelInfo, docketInfo).enable(girSwaggerProperties.isEnable());
	}

	// @Bean
	// public Docket createdemo2ApiServer() {
	// DocketInfo docketInfo = new DocketInfo("demo2分组",
	// "com.gtc.comp.knife4j.demo.controller.group2");
	// return createApi(apiModelInfo, docketInfo).enable(gtcSwaggerProperties.isEnable());
	// }

}
