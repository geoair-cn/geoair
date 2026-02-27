package cn.geoair.comp.knife4j.ext.config;

import cn.geoair.comp.knife4j.ext.auto.AutoApiConfig;
import org.reflections.Reflections;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * WebMvc配置
 */

@Configuration
public class GtcKnife4JWebConfig implements WebMvcConfigurer, EnvironmentAware {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
//        registry.addViewController("/doc").setViewName("forward:doc.html");
//        registry.addViewController("/apidoc.html").setViewName("forward:/webjars/doc.html");
//        registry.addViewController("/apidoc").setViewName("forward:/webjars/doc.html");

    }


    @Override
    public void setEnvironment(Environment environment) {

    }

    @Bean
    @ConditionalOnMissingBean(Reflections.class)
    Reflections reflections() {
        return new Reflections();
    }



    @Bean
    @ConditionalOnMissingBean(GtcSwaggerApiConfig.class)
    AutoApiConfig autoApiConfig() {
        return new AutoApiConfig();
    }
}
