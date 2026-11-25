package cn.geoair.map.dynamic.tools.simple;

import cn.geoair.base.Gir;
import cn.geoair.base.util.GutilObject;
import cn.geoair.web.util.GirHttpServletHelper;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet 请求地址、响应输出和响应头读取工具。
 *
 * <p>地址推断方法依赖当前线程中的 Servlet 请求及部分 Spring 配置；它们不解析 {@code X-Forwarded-Proto}，返回协议固定为 {@code
 * http}，部署在 HTTPS 反向代理后时 不应将结果直接视为外部访问地址。
 *
 * @author 张逢吉
 */
public class GirServletUtil extends ServletUtil {

    /** 设置严格的无缓存响应头（适用于动态内容、API 等） */
    public static void setNoCacheHeaders() {
        HttpServletResponse response = GirHttpServletHelper.getResponse();
        // HTTP 1.1
        response.setHeader(
                "Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
        // HTTP 1.0
        response.setHeader("Pragma", "no-cache");
        // 明确过期时间
        response.setDateHeader("Expires", 0);
    }

    /**
     * 从当前请求推导服务根路径，并追加 Spring 配置的 context path。
     *
     * @return 形如 {@code http://host:port/context-path} 的地址
     */
    public static String getServerPathByRequest() {
        String property = Gir.property.getProperty("server.servlet.context-path");
        if (GutilObject.isNotEmpty(property)) {
            property = StrUtil.replaceFirst(property, "/", "");
            property = "/" + property;
        } else {
            property = "";
        }
        return getHttpPathByRequest() + property;
    }

    /**
     * 从当前请求的 {@code Host} 头推导 HTTP 地址。
     *
     * <p>协议固定为 {@code http}；仅当 Host 未带端口且请求头中存在有效代理端口时才补充端口。
     *
     * @return 形如 {@code http://host:port} 的地址
     */
    public static String getHttpPathByRequest() {
        HttpServletRequest request = GirHttpServletHelper.getRequest();
        Map<String, String> headerMap = GirServletUtil.getHeaderMap(request);
        String host = headerMap.get("host");

        if (!host.contains(":")) {
            Integer originPort = getOriginPort(request);
            if (originPort != null) {
                host = host + ":" + originPort;
            }
        }
        return "http://" + host;
    }

    /**
     * 从已支持的代理端口请求头中获取原始端口。
     *
     * @param request 当前 HTTP 请求
     * @return 第一个可解析的 {@code x-forwarded-port} 或 {@code X-Real-PORT}；没有则返回 {@code null}
     */
    public static Integer getOriginPort(HttpServletRequest request) {
        // 可能的代理端口头信息，按优先级排序
        String[] portHeaders = {"x-forwarded-port", "X-Real-PORT"};

        // 遍历所有可能的端口头，获取第一个有效的端口
        for (String header : portHeaders) {
            String portStr = request.getHeader(header);
            if (portStr != null && !portStr.isEmpty() && !"unknown".equalsIgnoreCase(portStr)) {
                try {
                    return Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    // 端口格式不正确，继续尝试下一个头
                }
            }
        }
        return null;
    }

    /**
     * 收集当前请求和服务地址的诊断信息。
     *
     * @return 包含 Host、端口、context path、request URI 等信息的映射
     */
    public static Map<String, String> getServerInfoByRequest() {
        HttpServletRequest request = GirHttpServletHelper.getRequest();
        Map<String, String> headerMap = GirServletUtil.getHeaderMap(request);
        String requestHost = headerMap.get("host");
        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("requestHost", requestHost);
        infoMap.put("originPort", getOriginPort(request) + "");
        infoMap.put("serverPathBySpring", getServerPathBySpring());
        infoMap.put("serverPathByRequest", getServerPathByRequest());
        infoMap.put("contextPath", request.getContextPath());
        infoMap.put("requestURI", request.getRequestURI());
        infoMap.put("servletPath", request.getServletPath());
        return infoMap;
    }

    /**
     * 根据 Spring 配置和本机 IP 构造服务地址。
     *
     * <p>不读取当前请求，也不解析反向代理配置；若 {@code server.port} 未设置，结果会包含 字符串 {@code null}。
     *
     * @return 形如 {@code http://本机IP:端口/context-path} 的地址
     */
    public static String getServerPathBySpring() {
        String localhostStr = NetUtil.getLocalhostStr();
        String property = Gir.property.getProperty("server.servlet.context-path");
        String port = Gir.property.getProperty("server.port");
        if (GutilObject.isNotEmpty(property)) {
            property = StrUtil.replaceFirst(property, "/", "");
        } else {
            property = "";
        }
        return "http://" + localhostStr + ":" + port + "/" + property;
    }

    /**
     * 将字节数组写入 HTTP 响应并关闭输出流。
     *
     * <p>方法会设置 Content-Type 和 Content-Length；写入异常会被吞掉，调用方无法从返回值 感知失败。
     *
     * @param response HTTP 响应
     * @param re 输出字节
     * @param contentType 响应 Content-Type
     */
    public static void toResponse(HttpServletResponse response, byte[] re, String contentType) {
        ServletOutputStream outputStream = null;
        ByteArrayInputStream byteArrayInputStream = null;
        response.setContentType(contentType);
        response.setContentLengthLong(re.length);
        try {
            byteArrayInputStream = new ByteArrayInputStream(re);
            outputStream = response.getOutputStream();
            IoUtil.copy(byteArrayInputStream, outputStream);
        } catch (Exception e) {
        } finally {
            IoUtil.close(byteArrayInputStream);
            IoUtil.close(outputStream);
        }
    }

    /**
     * 将输入流复制到 HTTP 响应并关闭输入、输出流。
     *
     * <p>不会设置 Content-Length；写入异常会被吞掉。
     *
     * @param response HTTP 响应
     * @param inputStream 待输出的输入流，调用后会被关闭
     * @param contentType 响应 Content-Type
     */
    public static void toResponse(
            HttpServletResponse response, InputStream inputStream, String contentType) {
        ServletOutputStream outputStream = null;
        response.setContentType(contentType);
        try {
            outputStream = response.getOutputStream();
            IoUtil.copy(inputStream, outputStream);
        } catch (Exception e) {
        } finally {
            IoUtil.close(inputStream);
            IoUtil.close(outputStream);
        }
    }

    /**
     * 将字节数组以指定 HTTP 状态写入响应并关闭输出流。
     *
     * @param response HTTP 响应
     * @param re 输出字节
     * @param contentType 响应 Content-Type
     * @param code HTTP 状态码
     */
    public static void toResponse(
            HttpServletResponse response, byte[] re, String contentType, int code) {
        ServletOutputStream outputStream = null;
        ByteArrayInputStream byteArrayInputStream = null;
        response.setContentType(contentType);
        response.setContentLength(re.length);
        response.setStatus(code);
        try {
            byteArrayInputStream = new ByteArrayInputStream(re);
            outputStream = response.getOutputStream();
            IoUtil.copy(byteArrayInputStream, outputStream);
        } catch (Exception e) {
        } finally {
            IoUtil.close(byteArrayInputStream);
            IoUtil.close(outputStream);
        }
    }

    /**
     * 读取当前响应已设置的首个 Header 值。
     *
     * <p>对允许重复的 Header，只保留 {@link HttpServletResponse#getHeader(String)} 返回的单个值。
     *
     * @param response HTTP 响应
     * @return Header 名称到值的映射；没有 Header 时返回空映射
     */
    public static Map<String, String> getResponseHeaderMap(HttpServletResponse response) {
        Map<String, String> headers = new HashMap<>();
        Collection<String> headerNames = response.getHeaderNames();
        if (headerNames == null || headerNames.isEmpty()) {
            return headers;
        }
        for (String name : headerNames) {
            String value = response.getHeader(name);
            if (value != null) {
                headers.put(name, value);
            }
        }

        return headers;
    }
}
