package cn.geoair.comp.knife4j.ext.auto;

import cn.geoair.comp.knife4j.ext.config.GirSwaggerProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GirSwaggerProperties.class)
public class GirSwaggerPropertiesAutoConfiguration {}
