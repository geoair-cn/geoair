package cn.geoair.map.dynamic.mvt.filter;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//@Configuration
public class RealMvtContextWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CorsInterceptor())
                .addPathPatterns("/**");
    }
}
