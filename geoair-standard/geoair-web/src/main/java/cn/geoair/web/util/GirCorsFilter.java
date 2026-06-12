package cn.geoair.web.util;

import cn.geoair.base.util.GutilStr;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** 跨域过滤器 */
public class GirCorsFilter implements Filter {

    // 允许的请求头（固定数组）
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
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 设置跨域响应头
        setCorsResponseHeaders(req, resp);

        // OPTIONS 预检请求直接返回，不继续执行业务逻辑
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 正常请求：放行
        chain.doFilter(request, response);
    }

    /** 设置跨域响应头 */
    private void setCorsResponseHeaders(HttpServletRequest request, HttpServletResponse response) {
        // 动态允许当前请求的Origin（支持带cookie跨域）
        String origin = request.getHeader("Origin");
        if (GutilStr.isNotBlank(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Credentials", "false");
        }

        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        response.setHeader("Access-Control-Max-Age", "3600");

        // 构建允许的请求头（固定+前端传入，自动去重）
        String allowHeaders = buildAllowHeaders(request);
        response.setHeader("Access-Control-Allow-Headers", allowHeaders);
    }

    /** 合并固定允许头 + 前端预检请求头，去重后返回 */
    private String buildAllowHeaders(HttpServletRequest request) {
        Set<String> headerSet = new HashSet<>(Arrays.asList(ALLOWED_HEADERS));

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
