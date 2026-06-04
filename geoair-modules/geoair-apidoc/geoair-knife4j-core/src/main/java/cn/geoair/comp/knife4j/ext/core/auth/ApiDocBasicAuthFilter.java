package cn.geoair.comp.knife4j.ext.core.auth;


import cn.geoair.base.Gir;
import cn.hutool.core.codec.Base64;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;


public class ApiDocBasicAuthFilter implements Filter {

    public static ApiDocBasicAuthFilter getInstance() {
        return new ApiDocBasicAuthFilter();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        String uri = httpRequest.getRequestURI();

        // 只对 swagger 路径进行验证
        boolean isSwaggerPath = uri.contains("/swagger-ui/") ||
                uri.contains("/swagger-ui.html") ||
                uri.contains("/v3/api-docs") ||
                uri.contains("/swagger-resources") ||
                uri.contains("/webjars/");

        if (isSwaggerPath) {
            String authHeader = httpRequest.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Basic ")) {
                String base64Credentials = authHeader.substring(6);
                String credentials = Base64.decodeStr(base64Credentials);
                String[] values = credentials.split(":", 2);

                String USERNAME = Gir.property.getProperty("geoair.apidoc.auth.username");
                String PASSWORD = Gir.property.getProperty("geoair.apidoc.auth.password");

                if (values.length == 2 && USERNAME.equals(values[0]) && PASSWORD.equals(values[1])) {
                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                }
            }

            // 认证失败，返回 401 并弹出密码框
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"Swagger Access\"");
            httpResponse.setContentType("text/html;charset=UTF-8");
            httpResponse.getWriter().write("需要认证才能访问 Swagger");
            return;
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
