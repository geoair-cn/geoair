package cn.geoair.comp.dynamic.ds.datasource.web;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class GirDataSourceWebContextWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new GirDataSourceWebContextInterceptor()).addPathPatterns("/**");
    }
}
