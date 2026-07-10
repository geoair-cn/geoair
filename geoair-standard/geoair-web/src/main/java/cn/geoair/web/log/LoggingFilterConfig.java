package cn.geoair.web.log;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/10 14:49
 * @description：
 */
public class LoggingFilterConfig {
    /**
     * 需要记录日志的 URL 模式（白名单），为空则全部记录
     */
    private List<String> includeUrlPatterns;

    /**
     * 不需要记录日志的 URL 模式（黑名单），优先级高于白名单
     */
    private List<String> excludeUrlPatterns;


    /**
     * 采样率（0.0 ~ 1.0），默认 1.0 表示全部记录
     */
    private double samplingRate = 1.0;

    /**
     * 请求体采集器
     */
    private HttpContextCollector httpContextCollector;

    /**
     * HttpContext 消费者
     */
    private Consumer<HttpContext> contextConsumer;

    public List<String> getIncludeUrlPatterns() {
        return includeUrlPatterns;
    }

    public LoggingFilterConfig setIncludeUrlPatterns(List<String> includeUrlPatterns) {
        this.includeUrlPatterns = includeUrlPatterns;
        return this;
    }

    public List<String> getExcludeUrlPatterns() {
        return excludeUrlPatterns;
    }

    public LoggingFilterConfig setExcludeUrlPatterns(List<String> excludeUrlPatterns) {
        this.excludeUrlPatterns = excludeUrlPatterns;
        return this;
    }

    public double getSamplingRate() {
        return samplingRate;
    }

    public LoggingFilterConfig setSamplingRate(double samplingRate) {
        this.samplingRate = samplingRate;
        return this;
    }

    public HttpContextCollector getHttpContextCollector() {
        return httpContextCollector;
    }

    public LoggingFilterConfig setHttpContextCollector(HttpContextCollector httpContextCollector) {
        this.httpContextCollector = httpContextCollector;
        return this;
    }

    public Consumer<HttpContext> getContextConsumer() {
        return contextConsumer;
    }

    public LoggingFilterConfig setContextConsumer(Consumer<HttpContext> contextConsumer) {
        this.contextConsumer = contextConsumer;
        return this;
    }
}
