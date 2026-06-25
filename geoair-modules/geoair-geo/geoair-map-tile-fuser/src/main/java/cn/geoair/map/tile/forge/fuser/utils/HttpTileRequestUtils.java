package cn.geoair.map.tile.forge.fuser.utils;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Web 请求工具类
 * <p>
 * 提供 HTTP 请求相关的工具方法，包括：
 * - 代理配置
 * - 瓦片请求
 * - 请求重试
 * - 请求头构建
 * </p>
 *
 * @author 张俊
 * @date Created in 2026/6/16 09:20
 */

public class HttpTileRequestUtils {
    private static GiLogger log = GirLoggerFactory.getLogger( );
    // ==================== 默认配置常量 ====================

    /**
     * 默认 User-Agent
     */
    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * 默认 Accept 头
     */
    public static final String DEFAULT_ACCEPT =
            "image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8";

    /**
     * 默认 Accept-Language 头
     */
    public static final String DEFAULT_ACCEPT_LANGUAGE =
            "zh-CN,zh;q=0.9,en;q=0.8,en-US;q=0.7";

    /**
     * 默认 Accept-Encoding 头
     */
    public static final String DEFAULT_ACCEPT_ENCODING =
            "gzip, deflate, br";

    // ==================== 代理相关方法 ====================

    /**
     * 从图层配置中获取 HTTP 代理
     *
     * @param config 图层配置
     * @return Proxy 对象，如果未配置代理则返回 null
     */
    public static Proxy getHttpProxy(PxyLayerInfo config) {
        if (config == null) {
            return null;
        }

        if ("true".equalsIgnoreCase(config.getUseWebPxy())
                && config.getWebPxyHost() != null
                && !config.getWebPxyHost().trim().isEmpty()
                && config.getWebPxyPort() != null
                && config.getWebPxyPort() > 0) {

            return new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(config.getWebPxyHost(), config.getWebPxyPort()));
        }
        return null;
    }

    /**
     * 创建 HTTP 代理（直接指定主机和端口）
     *
     * @param host 代理主机
     * @param port 代理端口
     * @return Proxy 对象
     */
    public static Proxy createHttpProxy(String host, int port) {
        if (host == null || host.trim().isEmpty() || port <= 0) {
            return null;
        }
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }

    // ==================== 请求头构建方法 ====================

    /**
     * 构建默认的请求头
     *
     * @return 请求头 Map
     */
    public static Map<String, String> buildDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", DEFAULT_USER_AGENT);
        headers.put("Accept", DEFAULT_ACCEPT);
        headers.put("Accept-Language", DEFAULT_ACCEPT_LANGUAGE);
        headers.put("Accept-Encoding", DEFAULT_ACCEPT_ENCODING);
        headers.put("Cache-Control", "no-cache");
        headers.put("Pragma", "no-cache");
        headers.put("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"");
        headers.put("Sec-Ch-Ua-Mobile", "?0");
        headers.put("Sec-Ch-Ua-Platform", "\"Windows\"");
        headers.put("Sec-Fetch-Dest", "image");
        headers.put("Sec-Fetch-Mode", "no-cors");
        headers.put("Sec-Fetch-Site", "cross-site");
        return headers;
    }

    /**
     * 构建自定义请求头（合并默认请求头）
     *
     * @param customHeaders 自定义请求头
     * @return 合并后的请求头 Map
     */
    public static Map<String, String> buildHeaders(Map<String, String> customHeaders) {
        Map<String, String> headers = buildDefaultHeaders();
        if (customHeaders != null && !customHeaders.isEmpty()) {
            headers.putAll(customHeaders);
        }
        return headers;
    }

    // ==================== HttpRequest 构建方法 ====================

    /**
     * 创建 HTTP GET 请求
     *
     * @param url     请求 URL
     * @param proxy   代理（可为 null）
     * @param timeout 超时时间（毫秒）
     * @param headers 请求头
     * @return HttpRequest 对象
     */
    public static HttpRequest createGetRequest(String url, Proxy proxy, int timeout, Map<String, String> headers) {
        HttpRequest request = HttpUtil.createGet(url)
                .setFollowRedirects(true)
                .timeout(timeout)
                .setConnectionTimeout(timeout)
                .setReadTimeout(timeout);

        // 设置请求头
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(request::header);
        }

        // 设置代理
        if (proxy != null) {
            request.setProxy(proxy);
        }

        return request;
    }


    // ==================== 瓦片请求方法 ====================

    /**
     * 请求瓦片数据（带重试）
     *
     * @param url           请求 URL
     * @param proxy         代理（可为 null）
     * @param timeout       超时时间（毫秒）
     * @param maxRetries    最大重试次数
     * @param retryDelay    重试延迟（毫秒）
     * @param maxRetryDelay 最大重试延迟（毫秒）
     * @param srcFormat     源图片格式
     * @param logContext    日志上下文（用于记录 z, x, y 等信息）
     * @return 瓦片 Resource，失败返回 null
     */
    public static Resource requestTileWithRetry(String url,
                                                Proxy proxy,
                                                int timeout,
                                                int maxRetries,
                                                long retryDelay,
                                                long maxRetryDelay,
                                                ImageMime srcFormat,
                                                String logContext) {
        return requestTileWithRetry(url, proxy, timeout, maxRetries, retryDelay, maxRetryDelay,
                srcFormat, logContext, null);
    }

    /**
     * 请求瓦片数据（带重试和自定义请求头）
     *
     * @param url           请求 URL
     * @param proxy         代理（可为 null）
     * @param timeout       超时时间（毫秒）
     * @param maxRetries    最大重试次数
     * @param retryDelay    重试延迟（毫秒）
     * @param maxRetryDelay 最大重试延迟（毫秒）
     * @param srcFormat     源图片格式
     * @param logContext    日志上下文（用于记录 z, x, y 等信息）
     * @param headers       自定义请求头
     * @return 瓦片 Resource，失败返回 null
     */
    public static Resource requestTileWithRetry(String url,
                                                Proxy proxy,
                                                int timeout,
                                                int maxRetries,
                                                long retryDelay,
                                                long maxRetryDelay,
                                                ImageMime srcFormat,
                                                String logContext,
                                                Map<String, String> headers) {

        // 默认重试配置
        int actualMaxRetries = maxRetries > 0 ? maxRetries : 3;
        long actualRetryDelay = retryDelay > 0 ? retryDelay : 500;
        long actualMaxRetryDelay = maxRetryDelay > 0 ? maxRetryDelay : 3000;

        for (int attempt = 1; attempt <= actualMaxRetries; attempt++) {
            HttpResponse response = null;
            try {
                HttpRequest request = createGetRequest(url, proxy, timeout,
                        headers != null ? buildHeaders(headers) : buildDefaultHeaders());

                long startTime = System.currentTimeMillis();
                response = request.execute();

                if (response.isOk() && response.bodyBytes() != null) {
                    BufferedImage image = ImageIO.read(response.bodyStream());
                    if (image != null) {
                        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                            String internalName = srcFormat != null ?
                                    srcFormat.getInternalName() : "png";
                            ImageIO.write(image, internalName, baos);

                            long elapsed = System.currentTimeMillis() - startTime;
                            byte[] byteArray = baos.toByteArray();
                            if (attempt > 1) {
                                log.info("瓦片请求重试成功: {} - {} 尝试: {} 耗时: {}ms",
                                        url, logContext, attempt, elapsed);
                            } else {
                                log.info("瓦片请求成功: {} - {} 耗时: {}ms ,bodySize:{}",
                                        url, logContext, elapsed, DataSizeUtil.format(byteArray.length));
                            }
                            return new ByteArrayResource(byteArray);
                        }
                    } else {
                        // 响应无法解析为图片
                        log.warn("响应无法解析为图片: {} - {} 尝试: {}",
                                url, logContext, attempt);
                        return null;
                    }
                } else {
                    int statusCode = response.getStatus();
                    log.warn("瓦片请求失败: {} - {} 状态码: {} 尝试: {}/{}",
                            url, logContext, statusCode, attempt, actualMaxRetries);

                    // 4xx 错误（除了429）不重试
                    if (statusCode >= 400 && statusCode < 500 && statusCode != 429) {
                        log.info("客户端错误 {}，不进行重试", statusCode);
                        return null;
                    }

                    // 最后一次尝试失败
                    if (attempt == actualMaxRetries) {
                        log.error("所有重试失败: {} - {} 状态码: {}",
                                url, logContext, statusCode);
                        return null;
                    }

                    // 等待后重试
                    long delay = calculateRetryDelay(attempt, actualRetryDelay, actualMaxRetryDelay);
                    log.info("等待 {}ms 后进行第 {} 次重试", delay, attempt + 1);
                    Thread.sleep(delay);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("重试被中断: {} - {}", url, logContext);
                return null;
            } catch (Exception e) {
                log.error("瓦片请求异常: {} - {} 尝试: {}/{}",
                        url, logContext, attempt, actualMaxRetries, e);

                if (attempt == actualMaxRetries) {
                    log.error("所有重试失败: {} - {}", url, logContext, e);
                    return null;
                }

                try {
                    long delay = calculateRetryDelay(attempt, actualRetryDelay, actualMaxRetryDelay);
                    log.debug("异常后等待 {}ms 进行第 {} 次重试", delay, attempt + 1);
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }

        log.debug("所有重试失败（循环结束）: {} - {}", url, logContext);
        return null;
    }

    /**
     * 简单瓦片请求（不重试）
     *
     * @param url        请求 URL
     * @param proxy      代理（可为 null）
     * @param timeout    超时时间（毫秒）
     * @param srcFormat  源图片格式
     * @param logContext 日志上下文
     * @return 瓦片 Resource，失败返回 null
     */
    public static Resource requestTile(String url, Proxy proxy, int timeout,
                                       ImageMime srcFormat, String logContext) {
        return requestTile(url, proxy, timeout, srcFormat, logContext, null);
    }

    /**
     * 简单瓦片请求（不重试，带自定义请求头）
     */
    public static Resource requestTile(String url, Proxy proxy, int timeout,
                                       ImageMime srcFormat, String logContext,
                                       Map<String, String> headers) {
        HttpResponse response = null;
        try {
            HttpRequest request = createGetRequest(url, proxy, timeout,
                    headers != null ? buildHeaders(headers) : buildDefaultHeaders());

            long startTime = System.currentTimeMillis();
            response = request.execute();

            if (response.isOk() && response.bodyBytes() != null) {
                BufferedImage image = ImageIO.read(response.bodyStream());
                if (image != null) {
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        String internalName = srcFormat != null ?
                                srcFormat.getInternalName() : "png";
                        ImageIO.write(image, internalName, baos);

                        log.debug("瓦片请求成功: {} - {} 耗时: {}ms",
                                url, logContext, System.currentTimeMillis() - startTime);
                        return new ByteArrayResource(baos.toByteArray());
                    }
                } else {
                    log.warn("响应无法解析为图片: {} - {}", url, logContext);
                    return null;
                }
            } else {
                log.debug("瓦片请求失败: {} - {} 状态码: {}",
                        url, logContext, response.getStatus());
                return null;
            }
        } catch (Exception e) {
            log.error("瓦片请求异常: {} - {}", url, logContext, e);
            return null;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    // ==================== 重试工具方法 ====================

    /**
     * 计算重试延迟时间（指数退避 + 随机抖动）
     *
     * @param attempt   当前尝试次数（从1开始）
     * @param baseDelay 基础延迟（毫秒）
     * @param maxDelay  最大延迟（毫秒）
     * @return 延迟时间（毫秒）
     */
    private static long calculateRetryDelay(int attempt, long baseDelay, long maxDelay) {
        // 指数退避: baseDelay * 2^(attempt-1)
        long delay = baseDelay * (1L << (attempt - 1));
        // 限制最大延迟
        delay = Math.min(delay, maxDelay);
        // 添加随机抖动（±10%）
        double jitter = 0.9 + Math.random() * 0.2;
        return (long) (delay * jitter);
    }


}
