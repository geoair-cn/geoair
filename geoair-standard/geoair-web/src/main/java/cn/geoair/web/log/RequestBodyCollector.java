package cn.geoair.web.log;

import javax.servlet.http.HttpServletRequest;

/**
 * 请求体采集器接口。
 * <p>
 * 由使用者实现，决定如何采集请求体内容。
 * 可以控制是否缓存、如何截断、是否脱敏等策略。
 */
@FunctionalInterface
public interface RequestBodyCollector {

    /**
     * 采集请求体内容。
     *
     * @param request HTTP 请求对象
     * @return 请求体内容（字符串形式），如果无需采集或采集失败可返回 null
     */
    String collect(HttpServletRequest request);
}
