package cn.geoair.web.enums;

import cn.geoair.base.data.GiVisualValuable;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 请求方法枚举。
 * 参考 RFC 7231 和 Apache HttpComponents 设计。
 */
public enum GirHttpMethod implements GiVisualValuable<String> {

    // ====== 安全且幂等的方法 ======
    /**
     * GET 方法：获取资源。
     * 安全 (Safe): 不会改变服务器状态
     * 幂等 (Idempotent): 多次请求效果相同
     */
    GET(true, true, false),

    /**
     * HEAD 方法：获取资源的元数据（仅返回响应头）。
     * 安全 (Safe): 不会改变服务器状态
     * 幂等 (Idempotent): 多次请求效果相同
     */
    HEAD(true, true, false),

    /**
     * OPTIONS 方法：查询服务器支持的方法。
     * 安全 (Safe): 不会改变服务器状态
     * 幂等 (Idempotent): 多次请求效果相同
     */
    OPTIONS(true, true, false),

    /**
     * TRACE 方法：回显服务器收到的请求（用于调试）。
     * 安全 (Safe): 不会改变服务器状态
     * 幂等 (Idempotent): 多次请求效果相同
     */
    TRACE(true, true, false),

    // ====== 不安全但幂等的方法 ======
    /**
     * PUT 方法：替换资源（全量更新）。
     * 安全 (Safe): false - 会改变服务器状态
     * 幂等 (Idempotent): true - 多次完整替换结果相同
     */
    PUT(false, true, true),

    /**
     * DELETE 方法：删除资源。
     * 安全 (Safe): false - 会改变服务器状态
     * 幂等 (Idempotent): true - 删除多次效果相同
     */
    DELETE(false, true, false),

    // ====== 不安全且非幂等的方法 ======
    /**
     * POST 方法：创建资源或提交数据处理。
     * 安全 (Safe): false - 会改变服务器状态
     * 幂等 (Idempotent): false - 多次提交可能创建多个资源
     */
    POST(false, false, true),

    /**
     * PATCH 方法：部分更新资源。
     * 安全 (Safe): false - 会改变服务器状态
     * 幂等 (Idempotent): false - 多次部分更新可能导致不同结果
     */
    PATCH(false, false, true),

    // ====== 扩展：非标准方法 ======
    /**
     * 用于表示未知或自定义的 HTTP 方法。
     * 保守地设置为不安全且非幂等。
     */
    OTHER(false, false, false);

    // ====== 枚举属性 ======
    private final boolean safe;
    private final boolean idempotent;
    private final boolean hasRequestBody;

    // ====== 构造器 ======
    GirHttpMethod(boolean safe, boolean idempotent, boolean hasRequestBody) {
        this.safe = safe;
        this.idempotent = idempotent;
        this.hasRequestBody = hasRequestBody;
    }

    // ====== 核心查询方法 ======

    /**
     * 判断是否为安全方法。
     * 安全方法不会修改服务器状态（如 GET、HEAD）。
     */
    public boolean isSafe() {
        return safe;
    }

    /**
     * 判断是否为幂等方法。
     * 幂等方法多次执行的结果相同（如 GET、PUT、DELETE）。
     */
    public boolean isIdempotent() {
        return idempotent;
    }

    /**
     * 判断是否允许携带请求体。
     * POST、PUT、PATCH 通常有请求体，GET、DELETE 通常没有。
     */
    public boolean hasRequestBody() {
        return hasRequestBody;
    }


    /**
     * 安全地将字符串解析为 HttpMethod 枚举。
     * 与 Enum.valueOf() 不同，此方法不抛出异常。
     *
     * @param method HTTP 方法字符串（如 "GET"、"POST"）
     * @return 对应的枚举实例，如果无法识别则返回 OTHER
     */
    public static GirHttpMethod resolve(String method) {
        if (method == null || method.isEmpty()) {
            return null;
        }

        // 先尝试标准方法
        try {
            return valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 标准方法匹配失败，返回 OTHER
            return OTHER;
        }
    }

    /**
     * 安全地解析字符串，提供默认值。
     */
    public static GirHttpMethod resolve(String method, GirHttpMethod defaultValue) {
        GirHttpMethod resolved = resolve(method);
        return resolved != null ? resolved : defaultValue;
    }

    /**
     * 判断字符串是否为已知的标准 HTTP 方法。
     */
    public static boolean isKnownMethod(String method) {
        if (method == null || method.isEmpty()) {
            return false;
        }
        try {
            valueOf(method.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    private static final Map<String, GirHttpMethod> METHOD_CACHE = new HashMap<>();

    static {
        for (GirHttpMethod method : values()) {
            METHOD_CACHE.put(method.name(), method);
        }
    }

    /**
     * 使用缓存的高效解析方法（适合高频调用）。
     */
    public static GirHttpMethod resolveCached(String method) {
        if (method == null || method.isEmpty()) {
            return null;
        }
        return METHOD_CACHE.get(method.toUpperCase());
    }

    // ====== 便捷判断 ======

    /**
     * 判断是否为读取类方法（GET、HEAD、OPTIONS、TRACE）。
     */
    public boolean isReadMethod() {
        return this == GET || this == HEAD || this == OPTIONS || this == TRACE;
    }

    /**
     * 判断是否为写入类方法（POST、PUT、PATCH、DELETE）。
     */
    public boolean isWriteMethod() {
        return !isReadMethod() && this != OTHER;
    }


}
