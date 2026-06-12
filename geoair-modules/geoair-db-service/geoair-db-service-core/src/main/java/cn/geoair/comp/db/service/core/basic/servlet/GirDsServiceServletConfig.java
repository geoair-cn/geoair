package cn.geoair.comp.db.service.core.basic.servlet;

import cn.geoair.comp.db.service.core.config.GirDsServiceProperties;
import cn.hutool.core.util.StrUtil;

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
public class GirDsServiceServletConfig {

    @Autowired private GirDsAPIServlet girDsApiServlet;

    @Autowired GirDsServiceProperties girDsServiceProperties;

    @Bean
    public FilterRegistrationBean dsApiHeaderFilter() {
        // issues/I51LOI
        int apiHeaderFilterOrder = 1;
        String realApiContext1 = girDsServiceProperties.getRealApiContext();
        String realApiContext = StrUtil.removePrefix(realApiContext1, "/");
        String format = String.format("/%s/*", realApiContext);
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(new GirDsApiHeaderFilter());
        registrationBean.addUrlPatterns(format); // API Servlet 跨域
        registrationBean.setOrder(apiHeaderFilterOrder);
        registrationBean.setEnabled(true);
        log.debug(
                "注册 apiHeaderFilter for {} UrlPatterns, and order is {}",
                format,
                apiHeaderFilterOrder);
        return registrationBean;
    }

    @Bean
    public ServletRegistrationBean dsServletRegistrationBean() {
        String realApiContext1 = girDsServiceProperties.getRealApiContext();
        String realApiContext = StrUtil.removePrefix(realApiContext1, "/");
        String format = String.format("/%s/*", realApiContext);
        ServletRegistrationBean bean = new ServletRegistrationBean(girDsApiServlet);
        bean.addUrlMappings(format);
        log.debug("注册 APIServlet servelet for {} urlMappings", format);
        return bean;
    }
}
