package cn.geoair.web.log;

import java.nio.charset.StandardCharsets;

/**
 * 响应体上下文工具类。
 * <p>
 * 基于 ThreadLocal 实现，用于在请求处理过程中手动设置响应体内容，
 * 供日志采集器在 Filter 中获取。
 * <p>
 * 使用场景：
 * <ul>
 *   <li>在 Controller/Service 中设置业务响应内容</li>
 *   <li>在 Filter 中采集响应体用于日志记录</li>
 *   <li>解决响应体无法重复读取的问题</li>
 * </ul>
 */
public class ResponseBodyContext {


    private static final ThreadLocal<byte[]> RESPONSE_BODY_BYTES_HOLDER = new ThreadLocal<>();

    /**
     * 设置响应体内容（字符串形式）。
     *
     * @param body 响应体内容
     */
    public static void set(String body) {
        if (body != null) {
            RESPONSE_BODY_BYTES_HOLDER.set(body.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 设置响应体内容（字节数组形式）。
     * <p>
     * 适用于二进制响应（如文件下载），但二进制内容不会记录到日志中，
     * 只会记录大小。
     *
     * @param body 响应体字节数组
     */
    public static void set(byte[] body) {
        RESPONSE_BODY_BYTES_HOLDER.set(body);

    }


    /**
     * 获取响应体内容（字节数组形式）。
     *
     * @return 响应体字节数组，如果未设置则返回 null
     */
    public static byte[] getBytes() {
        return RESPONSE_BODY_BYTES_HOLDER.get();
    }


    /**
     * 判断是否已设置响应体。
     */
    public static boolean hasBody() {
        return RESPONSE_BODY_BYTES_HOLDER.get() != null;
    }

    /**
     * 清除当前线程的响应体上下文。
     * <p>
     * <b>必须在请求结束后调用，防止内存泄漏！</b>
     */
    public static void clear() {
        RESPONSE_BODY_BYTES_HOLDER.remove();
    }


}
