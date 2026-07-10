//package cn.geoair.web.log;
//
//import cn.geoair.base.log.GiLogger;
//import cn.geoair.base.log.GirLoggerFactory;
//import cn.geoair.web.enums.GirHttpMethod;
//
//
//import javax.servlet.*;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.util.*;
//import java.util.function.Consumer;
//
///**
// * HTTP 上下文日志采集过滤器。
// * <p>
// * 拦截 HTTP 请求，采集请求信息并组装 {@link HttpContext} 对象。
// * 响应信息（状态码、响应头等）直接从 HttpServletResponse 中采集。
//
// */
//public class HttpLoggingFilter implements Filter {
//
//    private static final GiLogger log = GirLoggerFactory.getLogger(HttpLoggingFilter.class);
//
//    /**
//     * 需要记录日志的 URL 模式（白名单），为空则全部记录
//     */
//    private List<String> includeUrlPatterns;
//
//    /**
//     * 不需要记录日志的 URL 模式（黑名单），优先级高于白名单
//     */
//    private List<String> excludeUrlPatterns;
//
//    /**
//     * 采样率（0.0 ~ 1.0），默认 1.0 表示全部记录
//     */
//    private double samplingRate = 1.0;
//
//    /**
//     * 请求体采集器
//     */
//    private RequestBodyCollector requestBodyCollector;
//
//    /**
//     * HttpContext 消费者（用于记录日志）
//     */
//    private Consumer<HttpContext> logConsumer;
//
//    @Override
//    public void init(FilterConfig filterConfig) throws ServletException {
//        // 1. 初始化日志消费者
//        String consumerClass = filterConfig.getInitParameter("logConsumerClass");
//        if (consumerClass != null && !consumerClass.isEmpty()) {
//            try {
//                @SuppressWarnings("unchecked")
//                Class<Consumer<HttpContext>> clazz = (Class<Consumer<HttpContext>>) Class.forName(consumerClass);
//                this.logConsumer = clazz.getDeclaredConstructor().newInstance();
//            } catch (Exception e) {
//                log.error("Failed to instantiate log consumer: {}", consumerClass, e);
//                this.logConsumer = this::defaultLogConsumer;
//            }
//        } else {
//            this.logConsumer = this::defaultLogConsumer;
//        }
//
//        // 2. 初始化请求体采集器
//        String requestCollectorClass = filterConfig.getInitParameter("requestBodyCollectorClass");
//        if (requestCollectorClass != null && !requestCollectorClass.isEmpty()) {
//            try {
//                this.requestBodyCollector = (RequestBodyCollector) Class.forName(requestCollectorClass)
//                    .getDeclaredConstructor().newInstance();
//            } catch (Exception e) {
//                log.error("Failed to instantiate request body collector: {}", requestCollectorClass, e);
//                this.requestBodyCollector = new DefaultRequestBodyCollector();
//            }
//        } else {
//            this.requestBodyCollector = new DefaultRequestBodyCollector();
//        }
//
//        // 3. 初始化 URL 过滤配置
//        String includePatterns = filterConfig.getInitParameter("includeUrlPatterns");
//        if (includePatterns != null && !includePatterns.isEmpty()) {
//            this.includeUrlPatterns = Arrays.asList(includePatterns.split(","));
//        }
//
//        String excludePatterns = filterConfig.getInitParameter("excludeUrlPatterns");
//        if (excludePatterns != null && !excludePatterns.isEmpty()) {
//            this.excludeUrlPatterns = Arrays.asList(excludePatterns.split(","));
//        }
//
//        // 4. 初始化采样率
//        String samplingRateStr = filterConfig.getInitParameter("samplingRate");
//        if (samplingRateStr != null && !samplingRateStr.isEmpty()) {
//            try {
//                this.samplingRate = Double.parseDouble(samplingRateStr);
//                if (this.samplingRate < 0 || this.samplingRate > 1) {
//                    this.samplingRate = 1.0;
//                }
//            } catch (NumberFormatException e) {
//                this.samplingRate = 1.0;
//            }
//        }
//
//        log.info("HttpLoggingFilter initialized with samplingRate={}", this.samplingRate);
//    }
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//
//        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        HttpServletRequest httpRequest = (HttpServletRequest) request;
//        HttpServletResponse httpResponse = (HttpServletResponse) response;
//
//        // 1. URL 过滤判断
//        String path = httpRequest.getRequestURI();
//        if (!shouldLog(path)) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        // 2. 采样判断
//        if (!shouldSample()) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        // 3. 创建日志上下文
//        HttpContext context = new HttpContext();
//
//        // 4. 采集请求基本信息（必须在包装之前）
//        collectRequestInfo(httpRequest, context);
//
//        // 5. 包装请求（支持请求体重复读取）
//        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);
//
//        // 6. 采集请求体（通过采集器）
//        collectRequestBody(cachedRequest, context);
//
//        // 7. 记录开始时间
//        context.setRequestStartTime(System.currentTimeMillis());
//        context.setThreadName(Thread.currentThread().getName());
//
//        try {
//            // 8. 执行后续过滤器链
//            chain.doFilter(cachedRequest, httpResponse);
//
//            // 9. 采集响应信息
//            collectResponseInfo(httpResponse, context);
//
//        } catch (Exception e) {
//            // 10. 采集异常信息
//            collectExceptionInfo(context, e);
//            context.setStatusCode(500);
//            throw e;
//        } finally {
//            // 11. 完成时间采集
//            context.setResponseEndTime(System.currentTimeMillis());
//            context.setDuration(context.getResponseEndTime() - context.getRequestStartTime());
//
//            // 12. 输出日志
//            logConsumer.accept(context);
//        }
//    }
//
//    /**
//     * 采集请求基本信息
//     */
//    private void collectRequestInfo(HttpServletRequest request, HttpContext context) {
//        // 方法
//        String methodStr = request.getMethod();
//        context.setMethod(GirHttpMethod.resolve(methodStr));
//
//        // URI
//        context.setUri(request.getRequestURI());
//
//        // 查询字符串
//        context.setQueryString(request.getQueryString());
//
//        // 客户端信息
//        context.setClientIp(getClientIp(request));
//        context.setUserAgent(request.getHeader("User-Agent"));
//
//        // 请求头（只采集关键头）
//        Map<String, String> headers = new HashMap<>();
//        headers.put("Content-Type", request.getContentType());
//        headers.put("Content-Length", String.valueOf(request.getContentLength()));
//        headers.put("Accept", request.getHeader("Accept"));
//        headers.put("Accept-Encoding", request.getHeader("Accept-Encoding"));
//        headers.put("Accept-Language", request.getHeader("Accept-Language"));
//        headers.put("Authorization", maskSensitiveHeader(request.getHeader("Authorization")));
//        headers.put("Cookie", maskSensitiveHeader(request.getHeader("Cookie")));
//        headers.put("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
//        headers.put("X-Real-IP", request.getHeader("X-Real-IP"));
//        context.setRequestHeaders(headers);
//
//        // 参数
//        Map<String, String[]> paramMap = request.getParameterMap();
//        if (paramMap != null && !paramMap.isEmpty()) {
//            Map<String, String> params = new HashMap<>();
//            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
//                String[] values = entry.getValue();
//                if (values != null && values.length > 0) {
//                    // 敏感参数脱敏
//                    String key = entry.getKey();
//                    if (isSensitiveParameter(key)) {
//                        params.put(key, "***");
//                    } else {
//                        params.put(key, values.length == 1 ? values[0] : Arrays.toString(values));
//                    }
//                }
//            }
//            context.setParameters(params);
//        }
//
//        // 请求体大小
//        context.setRequestBodySize((long) request.getContentLength());
//    }
//
//    /**
//     * 采集请求体（通过采集器）
//     */
//    private void collectRequestBody(CachedBodyHttpServletRequest request, HttpContext context) {
//        if (requestBodyCollector != null) {
//            try {
//                String body = requestBodyCollector.collect(request);
//                context.setRequestBody(body);
//            } catch (Exception e) {
//                log.warn("Request body collection failed: {}", e.getMessage());
//            }
//        }
//    }
//
//    /**
//     * 采集响应信息
//     */
//    private void collectResponseInfo(HttpServletResponse response, HttpContext context) {
//        context.setStatusCode(response.getStatus());
//        context.setResponseStartTime(System.currentTimeMillis());
//
//        // 响应头
//        Map<String, String> responseHeaders = new HashMap<>();
//        responseHeaders.put("Content-Type", response.getContentType());
//        responseHeaders.put("Content-Length", String.valueOf(response.getHeader("Content-Length")));
//        responseHeaders.put("Cache-Control", response.getHeader("Cache-Control"));
//        responseHeaders.put("Date", response.getHeader("Date"));
//        context.setResponseHeaders(responseHeaders);
//
//        // Content-Type
//        String contentType = response.getContentType();
//        context.setContentType(contentType);
//        if (contentType != null) {
//            context.setResponseMimeType(GiMimeType.fromContentType(contentType));
//        }
//
//        // 响应体大小
//        String contentLengthStr = response.getHeader("Content-Length");
//        if (contentLengthStr != null) {
//            try {
//                context.setResponseBodySize(Long.parseLong(contentLengthStr));
//            } catch (NumberFormatException e) {
//                // ignore
//            }
//        }
//
//        // 响应编码
//        context.setContentEncoding(response.getHeader("Content-Encoding"));
//    }
//
//    /**
//     * 采集异常信息
//     */
//    private void collectExceptionInfo(HttpContext context, Exception e) {
//        context.setErrorMessage(e.getMessage());
//        context.setErrorType(e.getClass().getName());
//
//        // 只保留前 10 行堆栈
//        StackTraceElement[] elements = e.getStackTrace();
//        if (elements != null && elements.length > 0) {
//            StringBuilder sb = new StringBuilder();
//            int maxLines = Math.min(10, elements.length);
//            for (int i = 0; i < maxLines; i++) {
//                sb.append(elements[i].toString()).append("\n");
//            }
//            if (elements.length > 10) {
//                sb.append("... [truncated]");
//            }
//            context.setStackTrace(sb.toString());
//        }
//    }
//
//    /**
//     * 获取客户端真实 IP
//     */
//    private String getClientIp(HttpServletRequest request) {
//        String ip = request.getHeader("X-Forwarded-For");
//        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getHeader("X-Real-IP");
//        }
//        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getHeader("Proxy-Client-IP");
//        }
//        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getHeader("WL-Proxy-Client-IP");
//        }
//        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
//            ip = request.getRemoteAddr();
//        }
//        // X-Forwarded-For 可能包含多个 IP，取第一个
//        if (ip != null && ip.contains(",")) {
//            ip = ip.split(",")[0].trim();
//        }
//        return ip;
//    }
//
//    /**
//     * 敏感头脱敏
//     */
//    private String maskSensitiveHeader(String value) {
//        if (value == null || value.isEmpty()) {
//            return null;
//        }
//        if (value.length() > 8) {
//            return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
//        }
//        return "***";
//    }
//
//    /**
//     * 判断是否为敏感参数
//     */
//    private boolean isSensitiveParameter(String key) {
//        if (key == null) return false;
//        String lower = key.toLowerCase();
//        return lower.contains("password")
//            || lower.contains("pwd")
//            || lower.contains("token")
//            || lower.contains("secret")
//            || lower.contains("key")
//            || lower.contains("auth")
//            || lower.contains("credential");
//    }
//
//    /**
//     * 判断 URL 是否需要记录日志
//     */
//    private boolean shouldLog(String path) {
//        // 黑名单优先
//        if (excludeUrlPatterns != null && !excludeUrlPatterns.isEmpty()) {
//            for (String pattern : excludeUrlPatterns) {
//                if (path.matches(pattern.replace("*", ".*"))) {
//                    return false;
//                }
//            }
//        }
//
//        // 白名单
//        if (includeUrlPatterns != null && !includeUrlPatterns.isEmpty()) {
//            for (String pattern : includeUrlPatterns) {
//                if (path.matches(pattern.replace("*", ".*"))) {
//                    return true;
//                }
//            }
//            return false;
//        }
//
//        return true;
//    }
//
//    /**
//     * 采样判断
//     */
//    private boolean shouldSample() {
//        if (samplingRate >= 1.0) {
//            return true;
//        }
//        if (samplingRate <= 0) {
//            return false;
//        }
//        return Math.random() < samplingRate;
//    }
//
//    /**
//     * 默认的日志消费者
//     */
//    private void defaultLogConsumer(HttpContext context) {
//        // 这里使用 SLF4J 输出，也可以改为其他方式
//        Logger logger = LoggerFactory.getLogger("HTTP_ACCESS");
//
//        if (logger.isInfoEnabled()) {
//            StringBuilder sb = new StringBuilder();
//            sb.append("\n========== HTTP Transaction ==========\n");
//            sb.append("Method: ").append(context.getMethod()).append("\n");
//            sb.append("URI: ").append(context.getUri()).append("\n");
//            if (context.getQueryString() != null) {
//                sb.append("Query: ").append(context.getQueryString()).append("\n");
//            }
//            sb.append("Status: ").append(context.getStatusCode()).append("\n");
//            sb.append("Duration: ").append(context.getDuration()).append("ms\n");
//            sb.append("Client IP: ").append(context.getClientIp()).append("\n");
//            sb.append("Thread: ").append(context.getThreadName()).append("\n");
//
//            if (context.getRequestBody() != null) {
//                sb.append("Request Body: ").append(context.getRequestBody()).append("\n");
//            }
//
//            if (context.getErrorMessage() != null) {
//                sb.append("Error: ").append(context.getErrorType()).append(": ")
//                  .append(context.getErrorMessage()).append("\n");
//            }
//
//            sb.append("=======================================");
//            logger.info(sb.toString());
//        }
//    }
//
//    @Override
//    public void destroy() {
//        // 清理资源
//    }
//}
