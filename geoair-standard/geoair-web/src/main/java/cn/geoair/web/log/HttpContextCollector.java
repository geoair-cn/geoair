package cn.geoair.web.log;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.function.Consumer;

/**
 * HTTP 上下文采集器。
 * <p>
 * 负责采集 HTTP 请求和响应过程中的各种信息，
 * 包括 IP、请求体、响应体、头信息、参数等。
 * <p>
 * 设计为接口而非抽象类，方便不同实现按需定制采集策略。
 *
 * @author GeoAir Team
 * @since 1.0
 */
public interface HttpContextCollector {
    /**
     * 前置校验器。
     * <p>
     * 在请求处理之前执行，用于校验请求的合法性。
     * 如果校验失败，可以设置错误响应并返回 false。
     *
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     * @return true 表示校验通过，继续执行；false 表示校验失败，终止执行
     */
    boolean preValidate(HttpServletRequest request, HttpServletResponse response);


    // ==================== 请求信息采集 ====================

    /**
     * 采集客户端真实 IP 地址。
     *
     * @param request HTTP 请求对象
     * @return 客户端 IP 地址，如果无法获取则返回 null
     */
    String collectClientIp(HttpServletRequest request);

    /**
     * 采集请求体内容。
     * <p>
     * 实现者需要将请求包装为可重复读取的版本，并通过 Consumer 回调传递 body 字符串。
     *
     * @param request HTTP 请求对象
     * @param requestBodyConsumer 请求体消费者
     * @return 可重复读取的 HttpServletRequest
     */
    HttpServletRequest collectRequestBody(HttpServletRequest request, Consumer<String> requestBodyConsumer);

    /**
     * 采集请求头信息。
     *
     * @param request HTTP 请求对象
     * @return 请求头键值对，如果无需采集则返回空 Map
     */
    Map<String, String> collectRequestHeaders(HttpServletRequest request);

    /**
     * 采集请求参数。
     *
     * @param request HTTP 请求对象
     * @return 请求参数键值对，如果无需采集则返回空 Map
     */
    Map<String, String> collectRequestParameters(HttpServletRequest request);

    // ==================== 响应信息采集 ====================

    /**
     * 采集响应头信息。
     *
     * @param response HTTP 响应对象
     * @return 响应头键值对，如果无需采集则返回空 Map
     */
    Map<String, String> collectResponseHeaders(HttpServletResponse response);



}
