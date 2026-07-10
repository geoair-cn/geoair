package cn.geoair.web.log;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 请求信息采集器。
 * <p>
 * 聚合了各种请求信息的采集方法。
 * 如果只需要采集部分信息，可以实现对应的方法，其他方法返回 null。
 */
public interface RequestInfoCollector {

    /**
     * 采集客户端真实 IP 地址。
     *
     * @param request HTTP 请求对象
     * @return 客户端 IP 地址，如果无法获取则返回 null
     */
    String collectClientIp(HttpServletRequest request);

    /**
     * 采集请求体内容。
     *
     * @param request HTTP 请求对象
     * @return 请求体内容（字符串形式），如果无需采集则返回 null
     */
    HttpServletRequest collectRequestBody(HttpServletRequest request, Consumer<String> requestBodyConsumer);

    /**
     * 采集请求头信息。
     *
     * @param request HTTP 请求对象
     * @return 请求头键值对，如果无需采集则返回 null
     */
    Map<String, String> collectRequestHeaders(HttpServletRequest request);

    /**
     * 采集请求头信息。
     *
     * @param response HTTP 响应
     * @return 请求头键值对，如果无需采集则返回 null
     */
    Map<String, String> collectResponseHeaders(HttpServletResponse response);

    /**
     * 采集响应体。
     *
     * @param response HTTP 响应
     * @return 请求头键值对，如果无需采集则返回 null
     */
    HttpServletResponse collectResponseBody(HttpServletResponse response, Consumer<byte[]> responseBodyConsumer);

    /**
     * 采集请求参数。
     *
     * @param request HTTP 请求对象
     * @return 请求参数键值对，如果无需采集则返回 null
     */
    Map<String, String> collectRequestParameters(HttpServletRequest request);
}
