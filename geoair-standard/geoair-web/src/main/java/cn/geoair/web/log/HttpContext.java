package cn.geoair.web.log;

import cn.geoair.base.Gir;
import cn.geoair.web.enums.GirHttpMethod;
import cn.geoair.web.mime.GiMimeType;
import java.util.Arrays;
import java.util.Map;

/**
 * HTTP 请求-响应交互的完整上下文日志记录对象。
 *
 * <p>该类封装了一次完整的 HTTP 事务处理过程中的所有关键信息，包括：
 *
 * <ul>
 *   <li><b>请求信息</b>：请求方法、URI、查询参数、请求头、请求体等
 *   <li><b>响应信息</b>：状态码、响应头、响应体、内容类型等
 *   <li><b>性能指标</b>：请求到达时间、响应开始/结束时间、总耗时等
 *   <li><b>运行时信息</b>：处理线程名、异常信息等
 *   <li><b>网络信息</b>：客户端IP、User-Agent等
 * </ul>
 */
public class HttpContext {

    public static HttpContext of() {
        return new HttpContext();
    }

    // ==================== 请求信息 ====================

    /**
     * HTTP 请求方法（如 GET、POST、PUT 等）
     *
     * @see GirHttpMethod
     */
    private GirHttpMethod method;

    /**
     * 请求 URI（不包含查询字符串）
     *
     * <p>示例：{@code /api/users/123}
     */
    private String uri;

    /**
     * 查询字符串（URL 中 ? 后面的部分）
     *
     * <p>示例：{@code page=1&size=20&sort=asc}
     */
    private String queryString;

    /**
     * 客户端真实 IP 地址
     *
     * <p>注意：如果请求经过代理（如 Nginx），此字段可能需要从 {@code X-Forwarded-For} 或 {@code X-Real-IP} 头中获取真实 IP。
     */
    private String clientIp;

    /**
     * 客户端 User-Agent 头，标识客户端类型和版本
     *
     * <p>示例：{@code Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36}
     */
    private String userAgent;

    /**
     * 请求体内容（字符串形式）
     *
     * <p><b>注意：</b>
     *
     * <ul>
     *   <li>仅适用于文本类型（如 JSON、XML、Form 表单），二进制数据（如图片、文件）不记录
     *   <li>可能包含密码、Token 等敏感信息，记录前需要进行脱敏处理
     *   <li>建议设置大小上限（如 4096 字符），超出部分截断
     * </ul>
     */
    private String requestBody;

    /**
     * 请求体大小（字节数）
     *
     * <p>通过 {@code request.getContentLength()} 获取，用于分析请求大小分布。
     */
    private Long requestBodySize;

    // ==================== 响应信息 ====================

    /**
     * 响应体内容（字节数组形式）
     *
     * <p><b>注意：</b>
     *
     * <ul>
     *   <li>为避免内存溢出，建议对大响应体（如文件下载）不记录或采样记录
     *   <li>如需日志输出，建议转换为字符串并做截断处理
     *   <li>二进制响应（如图片、视频）不应记录
     * </ul>
     */
    private byte[] responseBody;

    /**
     * 响应体大小（字节数）
     *
     * <p>用于分析响应大小分布和网络传输量。
     */
    private Long responseBodySize;

    /**
     * 响应内容的 MIME 类型
     *
     * <p>从响应头 {@code Content-Type} 中解析得出。
     *
     * @see GiMimeType
     */
    private GiMimeType responseMimeType;

    /**
     * HTTP 响应状态码
     *
     * <p>常见状态码：
     *
     * <ul>
     *   <li>2xx：成功（如 200 OK、201 Created）
     *   <li>3xx：重定向（如 301 Moved Permanently）
     *   <li>4xx：客户端错误（如 400 Bad Request、404 Not Found）
     *   <li>5xx：服务端错误（如 500 Internal Server Error）
     * </ul>
     */
    private int statusCode;

    // ==================== 性能指标 ====================

    /**
     * 请求到达时间（毫秒级时间戳）
     *
     * <p>记录请求被接收的起始时间，通常在 Filter 入口处设置。
     */
    private long requestStartTime;

    /**
     * 响应开始时间（毫秒级时间戳）
     *
     * <p>记录业务处理完成、开始输出响应的时间。 通过 {@code responseStartTime - requestStartTime} 可计算业务处理耗时。
     */
    private long responseStartTime;

    /**
     * 响应完成时间（毫秒级时间戳）
     *
     * <p>记录响应完全写回客户端的时间。 通过 {@code responseEndTime - responseStartTime} 可计算响应输出耗时。
     */
    private long responseEndTime;

    /**
     * 请求总耗时（毫秒）
     *
     * <p>从请求到达（{@code requestStartTime}）到响应完成（{@code responseEndTime}）的总时间。
     *
     * <p>总耗时 = 业务处理耗时 + 响应输出耗时 + 网络传输耗时
     */
    private long duration;

    // ==================== 运行时信息 ====================

    /**
     * 处理该请求的线程名称
     *
     * <p>用于排查线程安全问题、分析线程池使用情况。
     */
    private String threadName;

    /**
     * 异常信息（如有）
     *
     * <p>记录请求处理过程中抛出的异常消息。 示例：{@code "Connection refused: connect"}
     */
    private String errorMessage;

    /**
     * 异常类型（如有）
     *
     * <p>记录异常类的全限定名。 示例：{@code "java.net.ConnectException"}
     */
    private String errorType;

    /**
     * 异常堆栈信息
     *
     * <p><b>注意：</b>
     *
     * <ul>
     *   <li>只保留前 N 行（如前 10 行），避免日志过大
     *   <li>仅在调试模式或特定错误级别下记录
     *   <li>生产环境建议只记录 {@link #errorMessage} 和 {@link #errorType}
     * </ul>
     */
    private String stackTrace;

    // ==================== 请求头与参数 ====================

    /**
     * 请求头信息（键值对形式）
     *
     * <p>记录关键请求头，如：
     *
     * <ul>
     *   <li>{@code Authorization}：认证信息（需脱敏）
     *   <li>{@code Content-Type}：请求体类型
     *   <li>{@code Accept}：客户端可接受的响应类型
     *   <li>{@code X-Request-ID}：请求追踪ID
     * </ul>
     *
     * <p><b>注意：</b>包含 Cookie、Authorization 等敏感信息时需要脱敏。
     */
    private Map<String, String> requestHeaders;

    /**
     * 请求参数（键值对形式）
     *
     * <p>包含：
     *
     * <ul>
     *   <li>查询参数（Query Parameters）：URL 中 ? 后面的参数
     *   <li>表单参数（Form Parameters）：POST 表单提交的参数
     * </ul>
     *
     * <p>注意：对于 JSON 等非表单请求体，参数会为空，数据在 {@link #requestBody} 中。
     */
    private Map<String, String> requestParams;

    // ==================== 响应头 ====================

    /**
     * 响应头信息（键值对形式）
     *
     * <p>记录关键响应头，如：
     *
     * <ul>
     *   <li>{@code Content-Type}：响应体类型（同时记录到 {@link #responseMimeType}）
     *   <li>{@code Content-Length}：响应体大小（同时记录到 {@link #responseBodySize}）
     *   <li>{@code Cache-Control}：缓存策略
     *   <li>{@code Set-Cookie}：设置的 Cookie（需脱敏）
     * </ul>
     */
    private Map<String, String> responseHeaders;

    /**
     * 响应内容的 MIME 类型（字符串形式）
     *
     * <p>从响应头 {@code Content-Type} 中获取。
     *
     * <p>示例：{@code "application/json;charset=UTF-8"}
     *
     * <p><b>注意：</b>此字段为 {@link #responseMimeType} 的字符串补充， 当 {@code GiMimeType} 无法解析时保留原始值。
     */
    private String contentType;

    /**
     * 响应内容编码方式
     *
     * <p>从响应头 {@code Content-Encoding} 中获取。
     *
     * <p>常见值：
     *
     * <ul>
     *   <li>{@code gzip}：GZIP 压缩
     *   <li>{@code deflate}：Deflate 压缩
     *   <li>{@code br}：Brotli 压缩
     *   <li>{@code null}：无压缩（默认）
     * </ul>
     *
     * <p>用于评估压缩效果和排查解压问题。
     */
    private String contentEncoding;

    public HttpContext setMethod(GirHttpMethod method) {
        this.method = method;
        return this;
    }

    public HttpContext setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public HttpContext setQueryString(String queryString) {
        this.queryString = queryString;
        return this;
    }

    public HttpContext setClientIp(String clientIp) {
        this.clientIp = clientIp;
        return this;
    }

    public HttpContext setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public HttpContext setRequestBody(String requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public HttpContext setRequestBodySize(Long requestBodySize) {
        this.requestBodySize = requestBodySize;
        return this;
    }

    public HttpContext setResponseBody(byte[] responseBody) {
        this.responseBody = responseBody;
        return this;
    }

    public HttpContext setResponseBodySize(Long responseBodySize) {
        this.responseBodySize = responseBodySize;
        return this;
    }

    public HttpContext setResponseMimeType(GiMimeType responseMimeType) {
        this.responseMimeType = responseMimeType;
        return this;
    }

    public HttpContext setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public HttpContext setRequestStartTime(long requestStartTime) {
        this.requestStartTime = requestStartTime;
        return this;
    }

    public HttpContext setResponseStartTime(long responseStartTime) {
        this.responseStartTime = responseStartTime;
        return this;
    }

    public HttpContext setResponseEndTime(long responseEndTime) {
        this.responseEndTime = responseEndTime;
        return this;
    }

    public HttpContext setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    public HttpContext setThreadName(String threadName) {
        this.threadName = threadName;
        return this;
    }

    public HttpContext setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public HttpContext setErrorType(String errorType) {
        this.errorType = errorType;
        return this;
    }

    public HttpContext setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
        return this;
    }

    public HttpContext setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
        return this;
    }

    public HttpContext setRequestParams(Map<String, String> requestParams) {
        this.requestParams = requestParams;
        return this;
    }

    public HttpContext setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
        return this;
    }

    public HttpContext setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public HttpContext setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
        return this;
    }

    public GirHttpMethod getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public String getQueryString() {
        return queryString;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public Long getRequestBodySize() {
        return requestBodySize;
    }

    public byte[] getResponseBody() {
        return responseBody;
    }

    public Long getResponseBodySize() {
        return responseBodySize;
    }

    public GiMimeType getResponseMimeType() {
        return responseMimeType;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getRequestStartTime() {
        return requestStartTime;
    }

    public long getResponseStartTime() {
        return responseStartTime;
    }

    public long getResponseEndTime() {
        return responseEndTime;
    }

    public long getDuration() {
        return duration;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public Map<String, String> getRequestParams() {
        return requestParams;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public String toJSONString() {
        return Gir.toJson(this).toJSONString();
    }

    @Override
    public String toString() {
        return "HttpContext{"
                + "method="
                + method
                + ", uri='"
                + uri
                + '\''
                + ", queryString='"
                + queryString
                + '\''
                + ", clientIp='"
                + clientIp
                + '\''
                + ", userAgent='"
                + userAgent
                + '\''
                + ", requestBody='"
                + requestBody
                + '\''
                + ", requestBodySize="
                + requestBodySize
                + ", responseBody="
                + Arrays.toString(responseBody)
                + ", responseBodySize="
                + responseBodySize
                + ", responseMimeType="
                + responseMimeType
                + ", statusCode="
                + statusCode
                + ", requestStartTime="
                + requestStartTime
                + ", responseStartTime="
                + responseStartTime
                + ", responseEndTime="
                + responseEndTime
                + ", duration="
                + duration
                + ", threadName='"
                + threadName
                + '\''
                + ", errorMessage='"
                + errorMessage
                + '\''
                + ", errorType='"
                + errorType
                + '\''
                + ", stackTrace='"
                + stackTrace
                + '\''
                + ", requestHeaders="
                + requestHeaders
                + ", requestParams="
                + requestParams
                + ", responseHeaders="
                + responseHeaders
                + ", contentType='"
                + contentType
                + '\''
                + ", contentEncoding='"
                + contentEncoding
                + '\''
                + '}';
    }
}
