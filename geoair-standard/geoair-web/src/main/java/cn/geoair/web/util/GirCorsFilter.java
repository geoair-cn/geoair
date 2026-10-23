package cn.geoair.web.util;


import cn.geoair.base.util.GutilStr;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

/**
 * 跨域过滤器
 */

public class GirCorsFilter implements Filter {

    // 默认允许的请求头，同时动态允许客户端声明的请求头，不作为白名单。
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

    /**
     * 设置跨域响应头
     */
    private void setCorsResponseHeaders(HttpServletRequest request, HttpServletResponse response) {
        // 无 Origin 的响应也需要区分缓存，避免被后续跨域请求复用。
        addVaryOrigin(response);

        // 动态允许当前请求的Origin（支持带cookie跨域）
        String origin = request.getHeader("Origin");
        if (GutilStr.isNotBlank(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }else{
            response.setHeader("Access-Control-Allow-Origin", "*");
        }


        response.setHeader("Access-Control-Allow-Methods", "GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS");
        response.setHeader("Access-Control-Max-Age", "3600");

        // 构建允许的请求头（固定+前端传入，自动去重）
        String allowHeaders = buildAllowHeaders(request);
        response.setHeader("Access-Control-Allow-Headers", allowHeaders);
    }

    /**
     * 补充 Origin 缓存区分条件，保留已有 Vary 响应头且不重复添加。
     */
    private void addVaryOrigin(HttpServletResponse response) {
        for (String vary : response.getHeaders("Vary")) {
            for (String field : vary.split(",")) {
                String name = field.trim();
                // 通配符已经涵盖所有请求头，不再追加 Origin。
                if ("*".equals(name) || "Origin".equalsIgnoreCase(name)) {
                    return;
                }
            }
        }
        // 多行 Vary 与逗号分隔列表语义相同，追加可保留其他过滤器设置的条件。
        response.addHeader("Vary", "Origin");
    }

    /**
     * 合并默认允许头与前端预检请求头，按大小写不敏感去重后返回。
     */
    private String buildAllowHeaders(HttpServletRequest request) {
        // HTTP 请求头名称不区分大小写，同时保持输出顺序稳定。
        Set<String> headerSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        headerSet.addAll(Arrays.asList(ALLOWED_HEADERS));

        String requestHeaders = request.getHeader("Access-Control-Request-Headers");
        if (GutilStr.isNotBlank(requestHeaders)) {
            String[] requestHeaderArr = requestHeaders.split(",");
            for (String header : requestHeaderArr) {
                String name = header.trim();
                if (!name.isEmpty()) {
                    headerSet.add(name);
                }
            }
        }

        return String.join(",", headerSet);
    }
}
