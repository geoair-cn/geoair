package cn.geoair.map.dynamic.mvt.filter;

import cn.geoair.map.dynamic.tools.simple.GirServletUtil;
import cn.hutool.core.util.StrUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/1/24 09:59 @description： 跨域请求处理拦截器，用于支持跨域资源共享
 */
@Component
public class CorsInterceptor implements HandlerInterceptor {

    // CORS相关常量定义
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS =
            "Access-Control-Allow-Credentials";

    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    private static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    private static final String DEFAULT_ALLOWED_METHODS = "POST, GET, OPTIONS, DELETE, PUT";

    private static final String DEFAULT_ALLOWED_HEADERS =
            "authorization, X-Requested-With, Content-Type";

    private static final String MAX_AGE_VALUE = "3600";

    private static final String OPTIONS_METHOD = "OPTIONS";

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        configureCorsHeaders(request, response);

        // 对于预检请求，设置完头信息后可以直接返回
        if (OPTIONS_METHOD.equals(request.getMethod())) {
            return true;
        }

        return true;
    }

    /** 配置跨域相关的响应头信息 */
    private void configureCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        // 设置允许的源站，增加空值检查
        String origin = request.getHeader("Origin");
        if (StrUtil.isNotBlank(origin)) {
            response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        }

        // 允许携带凭证信息（如Cookie）
        response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

        // 设置允许的HTTP方法
        response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, DEFAULT_ALLOWED_METHODS);

        // 设置预检请求的缓存时间
        response.setHeader(ACCESS_CONTROL_MAX_AGE, MAX_AGE_VALUE);

        // 组合默认允许的请求头和实际请求中的头信息
        Set<String> requestHeaderKeys = GirServletUtil.getHeaderMap(request).keySet();
        String requestHeaders = StrUtil.join(",", requestHeaderKeys);
        String allowedHeaders =
                StrUtil.isNotBlank(requestHeaders)
                        ? DEFAULT_ALLOWED_HEADERS + "," + requestHeaders
                        : DEFAULT_ALLOWED_HEADERS;

        response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders);
    }
}
