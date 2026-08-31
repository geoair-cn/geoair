package cn.geoair.map.tile.forge.fuser.utils;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.web.mime.GiMimeType;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Web 瓦片请求工具类。 */
public final class HttpTileRequestUtils {

    private static final GiLogger log = GirLoggerFactory.getLogger();
    private static final int DEFAULT_TIMEOUT_MILLIS = 10_000;
    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER =
            new PoolingHttpClientConnectionManager();
    private static final CloseableHttpClient HTTP_CLIENT;

    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    public static final String DEFAULT_ACCEPT =
            "image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8";
    public static final String DEFAULT_ACCEPT_LANGUAGE = "zh-CN,zh;q=0.9,en;q=0.8,en-US;q=0.7";

    /** 禁止自动解压未知大小的 HTTP 内容；图片本身已是压缩格式。 */
    public static final String DEFAULT_ACCEPT_ENCODING = "identity";

    static {
        CONNECTION_MANAGER.setMaxTotal(64);
        CONNECTION_MANAGER.setDefaultMaxPerRoute(16);
        HTTP_CLIENT =
                HttpClients.custom()
                        .setConnectionManager(CONNECTION_MANAGER)
                        .disableContentCompression()
                        .evictExpiredConnections()
                        .evictIdleConnections(30, TimeUnit.SECONDS)
                        .build();
    }

    private HttpTileRequestUtils() {}

    /** 调整共享 HTTP 连接池容量，应在应用初始化阶段调用。 */
    public static void setConnectionPoolLimits(int maxTotal, int maxPerRoute) {
        if (maxTotal <= 0 || maxPerRoute <= 0 || maxPerRoute > maxTotal) {
            throw new IllegalArgumentException("HTTP 连接池容量必须为正，且单路由容量不能大于总容量");
        }
        CONNECTION_MANAGER.setMaxTotal(maxTotal);
        CONNECTION_MANAGER.setDefaultMaxPerRoute(maxPerRoute);
    }

    /** 应用关闭时释放共享 HTTP 连接池。 */
    public static void closeHttpClient() {
        try {
            HTTP_CLIENT.close();
        } catch (IOException e) {
            log.warn("关闭瓦片 HTTP 连接池失败", e);
        }
    }

    public static Proxy getHttpProxy(PxyLayerInfo config) {
        if (config == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(config.getUseWebPxy())
                && config.getWebPxyHost() != null
                && !config.getWebPxyHost().trim().isEmpty()
                && config.getWebPxyPort() != null
                && config.getWebPxyPort() > 0) {
            return new Proxy(
                    Proxy.Type.HTTP,
                    new InetSocketAddress(config.getWebPxyHost(), config.getWebPxyPort()));
        }
        return null;
    }

    public static Proxy createHttpProxy(String host, int port) {
        if (host == null || host.trim().isEmpty() || port <= 0) {
            return null;
        }
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }

    public static Map<String, String> buildDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", DEFAULT_USER_AGENT);
        headers.put("Accept", DEFAULT_ACCEPT);
        headers.put("Accept-Language", DEFAULT_ACCEPT_LANGUAGE);
        headers.put("Accept-Encoding", DEFAULT_ACCEPT_ENCODING);
        headers.put("Cache-Control", "no-cache");
        headers.put("Pragma", "no-cache");
        headers.put(
                "Sec-Ch-Ua",
                "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"");
        headers.put("Sec-Ch-Ua-Mobile", "?0");
        headers.put("Sec-Ch-Ua-Platform", "\"Windows\"");
        headers.put("Sec-Fetch-Dest", "image");
        headers.put("Sec-Fetch-Mode", "no-cors");
        headers.put("Sec-Fetch-Site", "cross-site");
        return headers;
    }

    public static Map<String, String> buildHeaders(Map<String, String> customHeaders) {
        Map<String, String> headers = buildDefaultHeaders();
        if (customHeaders != null && !customHeaders.isEmpty()) {
            headers.putAll(customHeaders);
        }
        return headers;
    }

    /** 保留原 API；内部瓦片请求已改用共享 Apache HTTP 连接池。 */
    public static HttpRequest createGetRequest(
            String url, Proxy proxy, int timeout, Map<String, String> headers) {
        HttpRequest request =
                HttpUtil.createGet(url)
                        .setFollowRedirects(true)
                        .timeout(normalizeTimeout(timeout))
                        .setConnectionTimeout(normalizeTimeout(timeout))
                        .setReadTimeout(normalizeTimeout(timeout));
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(request::header);
        }
        if (proxy != null) {
            request.setProxy(proxy);
        }
        return request;
    }

    public static Resource requestTileWithRetry(
            String url,
            Proxy proxy,
            int timeout,
            int maxRetries,
            long retryDelay,
            long maxRetryDelay,
            GiMimeType srcFormat,
            String logContext) {
        return requestTileWithRetry(
                url,
                proxy,
                timeout,
                maxRetries,
                retryDelay,
                maxRetryDelay,
                srcFormat,
                logContext,
                null);
    }

    public static Resource requestTileWithRetry(
            String url,
            Proxy proxy,
            int timeout,
            int maxRetries,
            long retryDelay,
            long maxRetryDelay,
            GiMimeType srcFormat,
            String logContext,
            Map<String, String> headers) {
        if (!isSupportedHttpUrl(url)) {
            log.warn("不支持的瓦片 URL: {}", url);
            return null;
        }
        int actualMaxRetries = maxRetries > 0 ? maxRetries : 3;
        long actualRetryDelay = retryDelay > 0 ? retryDelay : 500;
        long actualMaxRetryDelay = maxRetryDelay > 0 ? maxRetryDelay : 3000;
        Map<String, String> actualHeaders =
                headers != null ? buildHeaders(headers) : buildDefaultHeaders();

        for (int attempt = 1; attempt <= actualMaxRetries; attempt++) {
            try {
                long startTime = System.currentTimeMillis();
                TileFetchResult result =
                        executeTileRequest(url, proxy, timeout, srcFormat, actualHeaders);
                if (result.resource != null) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (attempt > 1) {
                        log.info(
                                "瓦片请求重试成功: {} - {} 尝试: {} 耗时: {}ms",
                                url,
                                logContext,
                                attempt,
                                elapsed);
                    } else {
                        log.info(
                                "瓦片请求成功: {} - {} 耗时: {}ms, bodySize: {}",
                                url,
                                logContext,
                                elapsed,
                                DataSizeUtil.format(result.bodySize));
                    }
                    return result.resource;
                }
                if (isNonRetryableClientError(result.statusCode)) {
                    log.info("瓦片请求返回客户端错误 {}，不重试: {} - {}", result.statusCode, url, logContext);
                    return null;
                }
                log.warn(
                        "瓦片请求失败: {} - {} 状态码: {} 尝试: {}/{}",
                        url,
                        logContext,
                        result.statusCode,
                        attempt,
                        actualMaxRetries);
            } catch (Exception e) {
                log.warn(
                        "瓦片请求异常: {} - {} 尝试: {}/{}，原因: {}",
                        url,
                        logContext,
                        attempt,
                        actualMaxRetries,
                        e.getMessage());
            }

            if (attempt < actualMaxRetries
                    && !sleepBeforeRetry(attempt, actualRetryDelay, actualMaxRetryDelay)) {
                return null;
            }
        }
        log.error("所有瓦片请求重试失败: {} - {}", url, logContext);
        return null;
    }

    public static Resource requestTile(
            String url, Proxy proxy, int timeout, GiMimeType srcFormat, String logContext) {
        return requestTile(url, proxy, timeout, srcFormat, logContext, null);
    }

    public static Resource requestTile(
            String url,
            Proxy proxy,
            int timeout,
            GiMimeType srcFormat,
            String logContext,
            Map<String, String> headers) {
        if (!isSupportedHttpUrl(url)) {
            log.warn("不支持的瓦片 URL: {}", url);
            return null;
        }
        try {
            long startTime = System.currentTimeMillis();
            TileFetchResult result =
                    executeTileRequest(
                            url,
                            proxy,
                            timeout,
                            srcFormat,
                            headers != null ? buildHeaders(headers) : buildDefaultHeaders());
            if (result.resource != null) {
                log.debug(
                        "瓦片请求成功: {} - {} 耗时: {}ms",
                        url,
                        logContext,
                        System.currentTimeMillis() - startTime);
                return result.resource;
            }
            log.debug("瓦片请求失败: {} - {} 状态码: {}", url, logContext, result.statusCode);
        } catch (Exception e) {
            log.warn("瓦片请求异常: {} - {}，原因: {}", url, logContext, e.getMessage());
        }
        return null;
    }

    private static TileFetchResult executeTileRequest(
            String url, Proxy proxy, int timeout, GiMimeType srcFormat, Map<String, String> headers)
            throws IOException {
        HttpGet request = new HttpGet(url);
        request.setConfig(buildRequestConfig(proxy, timeout));
        if (headers != null) {
            headers.forEach(request::setHeader);
        }
        try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode < 200 || statusCode >= 300) {
                return TileFetchResult.failure(statusCode);
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return TileFetchResult.failure(statusCode);
            }
            long contentLength = entity.getContentLength();
            if (contentLength > TileResourceLimits.getMaxTileBytes()) {
                throw new IOException("瓦片响应超过大小限制: " + contentLength);
            }
            byte[] encodedBytes = TileImageUtils.readAllLimited(entity.getContent());
            BufferedImage image = TileImageUtils.readImage(encodedBytes);
            if (image == null) {
                return TileFetchResult.failure(statusCode);
            }
            String internalName = srcFormat != null ? srcFormat.getInternalName() : "png";
            byte[] normalizedBytes = TileImageUtils.writeImage(image, internalName);
            return TileFetchResult.success(
                    new ByteArrayResource(normalizedBytes), normalizedBytes.length);
        }
    }

    private static RequestConfig buildRequestConfig(Proxy proxy, int timeout) {
        RequestConfig.Builder builder =
                RequestConfig.custom()
                        .setConnectTimeout(normalizeTimeout(timeout))
                        .setSocketTimeout(normalizeTimeout(timeout))
                        .setConnectionRequestTimeout(normalizeTimeout(timeout))
                        .setRedirectsEnabled(true);
        if (proxy != null
                && proxy.type() == Proxy.Type.HTTP
                && proxy.address() instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) proxy.address();
            builder.setProxy(new HttpHost(address.getHostString(), address.getPort()));
        }
        return builder.build();
    }

    private static boolean isSupportedHttpUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            URI uri = new URI(url);
            return uri.getHost() != null
                    && ("http".equalsIgnoreCase(uri.getScheme())
                            || "https".equalsIgnoreCase(uri.getScheme()));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean isNonRetryableClientError(int statusCode) {
        return statusCode >= 400 && statusCode < 500 && statusCode != 429;
    }

    private static int normalizeTimeout(int timeout) {
        return timeout > 0 ? timeout : DEFAULT_TIMEOUT_MILLIS;
    }

    private static boolean sleepBeforeRetry(int attempt, long baseDelay, long maxDelay) {
        long delay = calculateRetryDelay(attempt, baseDelay, maxDelay);
        try {
            log.debug("等待 {}ms 后重试", delay);
            Thread.sleep(delay);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static long calculateRetryDelay(int attempt, long baseDelay, long maxDelay) {
        long delay = baseDelay * (1L << Math.min(attempt - 1, 30));
        delay = Math.min(delay, maxDelay);
        return (long) (delay * (0.9 + Math.random() * 0.2));
    }

    private static final class TileFetchResult {
        private final Resource resource;
        private final int statusCode;
        private final int bodySize;

        private TileFetchResult(Resource resource, int statusCode, int bodySize) {
            this.resource = resource;
            this.statusCode = statusCode;
            this.bodySize = bodySize;
        }

        private static TileFetchResult success(Resource resource, int bodySize) {
            return new TileFetchResult(resource, 200, bodySize);
        }

        private static TileFetchResult failure(int statusCode) {
            return new TileFetchResult(null, statusCode, 0);
        }
    }
}
