package cn.geoair.web.log;

import cn.geoair.base.Gir;
import cn.geoair.base.data.result.GiResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
     * 如果校验失败，请抛出异常
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @return 校验失败 请抛出异常
     */
    void preValidate(HttpServletRequest request, HttpServletResponse response) throws Exception;

    /**
     * preValidate这一步抛出来的异常在这里进行写流返回
     *
     * @param exception
     * @param response
     * @throws Exception
     */
    default void exceptionToResponse(Exception exception, HttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        String message = exception.getMessage();
        GiResult<Object> objectGiResult = GiResult.failureMsg(message).andCode(403);
        try {
            response.getWriter().write(Gir.toJson(objectGiResult).toString());
        } catch (IOException e) {
        }
    }

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
     * @param request             HTTP 请求对象
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

    /**
     * 如果有异常，这里采集异常的堆栈
     *
     * @param exception
     * @return
     */
    default String collectExceptionStackTrace(Exception exception) {
        StackTraceElement[] elements = exception.getStackTrace();
        if (elements != null && elements.length > 0) {
            StringBuilder sb = new StringBuilder();
            int maxLines = Math.min(10, elements.length);
            for (int i = 0; i < maxLines; i++) {
                sb.append(elements[i].toString()).append("\n");
            }
            if (elements.length > 10) {
                sb.append("... [truncated]");
            }
            return sb.toString();
        }
        return exception.getMessage();
    }


}
