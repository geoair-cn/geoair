package cn.geoair.web.log;

import cn.geoair.web.enums.GirHttpMethod;

import java.util.Map;

public class HttpRequestLog {
    private GirHttpMethod method;
    private String uri;
    private String queryString;
    private String clientIp;
    private String userAgent;
    private String requestBody;
    private Long requestBodySize;
    private String responseBody;
    private Long responseBodySize;
    private int statusCode;
    private long startTime;
    private long duration;
    private Map<String, String> headers;


}
