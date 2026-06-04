package cn.geoair.comp.knife4j.ext.core.auth;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "geoair.apidoc.auth", name = "enable-auth", havingValue = "true", matchIfMissing = false)
public class GirApiDocAuthFilterConfig {

    @Bean
    public FilterRegistrationBean<ApiDocBasicAuthFilter> swaggerAuthFilter() {
        FilterRegistrationBean<ApiDocBasicAuthFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(ApiDocBasicAuthFilter.getInstance());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
