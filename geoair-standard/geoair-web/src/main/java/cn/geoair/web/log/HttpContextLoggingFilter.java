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
 *
 * <p>拦截 HTTP 请求，采集请求信息并组装 {@link HttpContext} 对象。 响应信息（状态码、响应头等）直接从 HttpServletResponse 中采集。
 */
public class HttpContextLoggingFilter implements Filter {

    private static final GiLogger log = GirLoggerFactory.getLogger(HttpContextLoggingFilter.class);

    public static HttpContextLoggingFilter of(LoggingFilterConfig loggingFilterConfig) {
        return new HttpContextLoggingFilter(loggingFilterConfig);
    }

    public static HttpContextLoggingFilter of(
            Consumer<LoggingFilterConfig> loggingFilterConfigConsumer) {
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
        ResponseBodyContext.clear();
        if (!(request instanceof HttpServletRequest)
                || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        HttpContextCollector httpContextCollector = loggingFilterConfig.getHttpContextCollector();

        if (httpContextCollector == null) {
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
        context.setRequestStartTime(System.currentTimeMillis());
        context.setThreadName(Thread.currentThread().getName());
        String methodStr = httpRequest.getMethod();
        context.setMethod(GirHttpMethod.resolve(methodStr));
        context.setUri(httpRequest.getRequestURI());
        context.setQueryString(httpRequest.getQueryString());
        context.setUserAgent(httpRequest.getHeader("User-Agent"));
        context.setRequestBodySize((long) request.getContentLength());
        context.setClientIp(httpContextCollector.collectClientIp(httpRequest));
        context.setRequestHeaders(httpContextCollector.collectRequestHeaders(httpRequest));
        context.setRequestParams(httpContextCollector.collectRequestParameters(httpRequest));
        HttpServletRequest httpServletRequest =
                httpContextCollector.collectRequestBody(httpRequest, context::setRequestBody);
        if (httpServletRequest != null) {
            httpRequest = httpServletRequest;
        }

        try {
            try {
                httpContextCollector.preValidate(httpRequest, httpResponse);
            } catch (Exception e) {
                collectExceptionInfo(context, e);
                httpContextCollector.exceptionToResponse(e, httpResponse);
                return;
            }
            chain.doFilter(httpRequest, httpResponse);
            context.setStatusCode(httpResponse.getStatus());
            context.setResponseStartTime(System.currentTimeMillis());
            String contentType = response.getContentType();
            context.setContentType(contentType);
            context.setContentEncoding(httpResponse.getHeader("Content-Encoding"));
            if (contentType != null) {
                context.setResponseMimeType(GutilMimeType.fromContentType(contentType));
            }
            String contentLengthStr = httpResponse.getHeader("Content-Length");
            if (contentLengthStr != null) {
                try {
                    context.setResponseBodySize(Long.parseLong(contentLengthStr));
                } catch (NumberFormatException e) {

                }
            }

            Map<String, String> responseHeaders =
                    httpContextCollector.collectResponseHeaders(httpResponse);
            context.setResponseHeaders(responseHeaders);
            if (ResponseBodyContext.hasBody()) {
                context.setResponseBody(ResponseBodyContext.getBytes());
            }

        } catch (Exception e) {
            collectExceptionInfo(context, e);
            context.setStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw e;
        } finally {
            context.setResponseEndTime(System.currentTimeMillis());
            context.setDuration(context.getResponseEndTime() - context.getRequestStartTime());
            loggingFilterConfig.getContextConsumer().accept(context);
            ResponseBodyContext.clear();
        }
    }

    private void collectExceptionInfo(HttpContext context, Exception e) {
        context.setErrorMessage(e.getMessage());
        context.setErrorType(e.getClass().getName());
        String stackTrace =
                loggingFilterConfig.getHttpContextCollector().collectExceptionStackTrace(e);
        context.setStackTrace(stackTrace);
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
