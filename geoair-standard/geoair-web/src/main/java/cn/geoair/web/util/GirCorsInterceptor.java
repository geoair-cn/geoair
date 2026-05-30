package cn.geoair.web.util;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.geoair.base.util.GutilStr;
import org.springframework.web.servlet.HandlerInterceptor;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class GirCorsInterceptor implements HandlerInterceptor {

    // ====================== 改成数组形式 ======================
    private static final String[] ALLOWED_HEADERS = {
            "authorization",
            "appId",
            "appSecret",
            "user-login-token",
            "use-static-tile",
            "X-Requested-With",
            "Content-Type",
            "Accept",
            "Origin"
    };

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        setCorsResponseHeaders(request, response);
        // OPTIONS 预检请求直接返回
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return true;
    }

    private void setCorsResponseHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (GutilStr.isNotBlank(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Credentials", "false");
        }

//        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        response.setHeader("Access-Control-Max-Age", "3600");

        // 构建安全的请求头
        String allowHeaders = buildAllowHeaders(request);
        response.setHeader("Access-Control-Allow-Headers", allowHeaders);
    }

    /**
     * 数组 + 前端传入请求头 = 最终允许的请求头（自动去重）
     */
    private String buildAllowHeaders(HttpServletRequest request) {
        Set<String> headerSet = new HashSet<>();

        headerSet.addAll(Arrays.asList(ALLOWED_HEADERS));

        String requestHeaders = request.getHeader("Access-Control-Request-Headers");
        if (GutilStr.isNotBlank(requestHeaders)) {
            String[] requestHeaderArr = requestHeaders.split(",");
            for (String header : requestHeaderArr) {
                headerSet.add(header.trim());
            }
        }

        return String.join(",", headerSet);
    }
}
