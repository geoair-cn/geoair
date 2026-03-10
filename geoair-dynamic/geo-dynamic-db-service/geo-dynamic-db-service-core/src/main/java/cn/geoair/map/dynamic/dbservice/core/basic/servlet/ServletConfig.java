package cn.geoair.map.dynamic.dbservice.core.basic.servlet;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description:
 * @program: dbApi
 * @author: kensan
 * @create: 2022-04-16 12:45
 */
@Slf4j
@Configuration
public class ServletConfig {

    @Autowired private APIServlet apiServlet;

    @Bean
    public FilterRegistrationBean apiHeaderFilter() {
        // issues/I51LOI
        int apiHeaderFilterOrder = 1;
        String realApiContext = "apiServer";
        String format = String.format("/%s/*", realApiContext);
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(new ApiHeaderFilter());
        registrationBean.addUrlPatterns(format); // API Servlet 跨域
        registrationBean.setOrder(apiHeaderFilterOrder);
        registrationBean.setEnabled(true);
        log.info(
                "regist apiHeaderFilter for {} UrlPatterns, and order is {}",
                format,
                apiHeaderFilterOrder);
        return registrationBean;
    }

    @Bean
    public ServletRegistrationBean getServletRegistrationBean() {
        String realApiContext = "apiServer";
        String format = String.format("/%s/*", realApiContext);
        ServletRegistrationBean bean = new ServletRegistrationBean(apiServlet);
        bean.addUrlMappings(format);
        log.info("regist APIServlet servelet for {} urlMappings", format);
        return bean;
    }
}
