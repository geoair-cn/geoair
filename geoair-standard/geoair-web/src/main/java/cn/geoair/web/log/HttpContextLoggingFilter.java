package cn.geoair.web.log;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.web.enums.GirHttpMethod;
import cn.geoair.web.util.GutilMimeType;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * HTTP 上下文日志采集过滤器。
 * <p>
 * 拦截 HTTP 请求，采集请求信息并组装 {@link HttpContext} 对象。
 * 响应信息（状态码、响应头等）直接从 HttpServletResponse 中采集。
 */
public class HttpContextLoggingFilter implements Filter {

    private static final GiLogger log = GirLoggerFactory.getLogger(HttpContextLoggingFilter.class);

    public static HttpContextLoggingFilter of(LoggingFilterConfig loggingFilterConfig) {
        return new HttpContextLoggingFilter(loggingFilterConfig);
    }

    public static HttpContextLoggingFilter of(Consumer<LoggingFilterConfig> loggingFilterConfigConsumer) {
        LoggingFilterConfig loggingFilterConfig = new LoggingFilterConfig();
        loggingFilterConfigConsumer.accept(loggingFilterConfig);
        return new HttpContextLoggingFilter(loggingFilterConfig);
    }

    LoggingFilterConfig loggingFilterConfig;

    public HttpContextLoggingFilter(LoggingFilterConfig loggingFilterConfig) {
        this.loggingFilterConfig = loggingFilterConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        if (!shouldLog(path)) {
            chain.doFilter(request, response);
            return;
        }


        if (!shouldSample()) {
            chain.doFilter(request, response);
            return;
        }

        HttpContext context = HttpContext.of();

        collectRequestInfo(httpRequest, context);
        RequestInfoCollector requestBodyCollector = loggingFilterConfig.getRequestBodyCollector();
        if (requestBodyCollector != null) {
            HttpServletRequest httpServletRequest = requestBodyCollector.collectRequestBody(httpRequest, context::setRequestBody);
            if (httpServletRequest != null) {
                httpRequest = httpServletRequest;
            }
        }


        context.setRequestStartTime(System.currentTimeMillis());
        context.setThreadName(Thread.currentThread().getName());

        try {
            if (requestBodyCollector != null) {
                HttpServletResponse httpServletResponse = requestBodyCollector.collectResponseBody(httpResponse, context::setResponseBody);
                if (httpServletResponse != null) {
                    httpResponse = httpServletResponse;
                }
            }
            chain.doFilter(httpRequest, httpResponse);
            collectResponseInfo(httpResponse, context);
        } catch (Exception e) {
            collectExceptionInfo(context, e);
            context.setStatusCode(500);
            throw e;
        } finally {
            context.setResponseEndTime(System.currentTimeMillis());
            context.setDuration(context.getResponseEndTime() - context.getRequestStartTime());
            loggingFilterConfig.getContextConsumer().accept(context);
        }
    }


    private void collectRequestInfo(HttpServletRequest request, HttpContext context) {
        String methodStr = request.getMethod();
        context.setMethod(GirHttpMethod.resolve(methodStr));
        context.setUri(request.getRequestURI());
        context.setQueryString(request.getQueryString());
        RequestInfoCollector requestBodyCollector = loggingFilterConfig.getRequestBodyCollector();
        if (requestBodyCollector != null) {
            context.setClientIp(requestBodyCollector.collectClientIp(request));
            context.setRequestHeaders(requestBodyCollector.collectRequestHeaders(request));
            context.setRequestParams(requestBodyCollector.collectRequestParameters(request));
        }
        context.setUserAgent(request.getHeader("User-Agent"));
        context.setRequestBodySize((long) request.getContentLength());
    }


    private void collectResponseInfo(HttpServletResponse response, HttpContext context) {
        context.setStatusCode(response.getStatus());
        context.setResponseStartTime(System.currentTimeMillis());
        RequestInfoCollector requestBodyCollector = loggingFilterConfig.getRequestBodyCollector();
        if (requestBodyCollector != null) {
            Map<String, String> responseHeaders = requestBodyCollector.collectResponseHeaders(response);
            context.setResponseHeaders(responseHeaders);
        }
        String contentType = response.getContentType();
        context.setContentType(contentType);
        if (contentType != null) {
            context.setResponseMimeType(GutilMimeType.fromContentType(contentType));
        }


        String contentLengthStr = response.getHeader("Content-Length");
        if (contentLengthStr != null) {
            try {
                context.setResponseBodySize(Long.parseLong(contentLengthStr));
            } catch (NumberFormatException e) {

            }
        }

        context.setContentEncoding(response.getHeader("Content-Encoding"));
    }


    private void collectExceptionInfo(HttpContext context, Exception e) {
        context.setErrorMessage(e.getMessage());
        context.setErrorType(e.getClass().getName());
        StackTraceElement[] elements = e.getStackTrace();
        if (elements != null && elements.length > 0) {
            StringBuilder sb = new StringBuilder();
            int maxLines = Math.min(10, elements.length);
            for (int i = 0; i < maxLines; i++) {
                sb.append(elements[i].toString()).append("\n");
            }
            if (elements.length > 10) {
                sb.append("... [truncated]");
            }
            context.setStackTrace(sb.toString());
        }
    }


    private boolean shouldLog(String path) {
        List<String> excludeUrlPatterns = loggingFilterConfig.getExcludeUrlPatterns();
        if (excludeUrlPatterns != null && !excludeUrlPatterns.isEmpty()) {
            for (String pattern : excludeUrlPatterns) {
                if (path.matches(pattern.replace("*", ".*"))) {
                    return false;
                }
            }
        }
        List<String> includeUrlPatterns = loggingFilterConfig.getIncludeUrlPatterns();
        if (includeUrlPatterns != null && !includeUrlPatterns.isEmpty()) {
            for (String pattern : includeUrlPatterns) {
                if (path.matches(pattern.replace("*", ".*"))) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }


    private boolean shouldSample() {
        double samplingRate = loggingFilterConfig.getSamplingRate();
        if (samplingRate >= 1.0) {
            return true;
        }
        if (samplingRate <= 0) {
            return false;
        }
        return Math.random() < samplingRate;
    }


}
