package com.gtc.comp.knife4j.demo.config;


import cn.geoair.comp.knife4j.ext.config.GtcSwaggerApiConfig;
import cn.geoair.comp.knife4j.ext.config.GtcSwaggerProperties;
import cn.geoair.comp.knife4j.ext.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.model.DocketInfo;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.*;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import springfox.documentation.spring.web.plugins.Docket;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@Configuration
public class Swagger2Configuration extends GtcSwaggerApiConfig {

    @Resource
    private GtcSwaggerProperties gtcSwaggerProperties;

    private final ApiModelInfo apiModelInfo = new ApiModelInfo("demo 在线文档", "demo在线文档", "demo", "1.0");


    @Bean
    public Docket createdemo1ApiServer() {
        DocketInfo docketInfo = new DocketInfo("demo1分组", "com.gtc.comp.knife4j.demo.controller.group1");
        return createApi(apiModelInfo, docketInfo).enable(gtcSwaggerProperties.isEnable());
    }

//    @Bean
//    public Docket createdemo2ApiServer() {
//        DocketInfo docketInfo = new DocketInfo("demo2分组", "com.gtc.comp.knife4j.demo.controller.group2");
//        return createApi(apiModelInfo, docketInfo).enable(gtcSwaggerProperties.isEnable());
//    }

}
