package cn.geoair.map.tile.forge.fuser.utils;

import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.execution.TileCancellationToken;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationException;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationScope;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;
import cn.geoair.map.tile.forge.fuser.upstream.UpstreamRequestOptions;
import cn.geoair.map.tile.forge.fuser.upstream.UpstreamTileRequest;
import cn.geoair.web.mime.GiMimeType;

import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.util.Map;

/** 图层级参数、鉴权扩展、重试与总截止时间共用一条上游请求链。 */
public final class LayerHttpTileClient {
    private LayerHttpTileClient() {}

    public static Resource fetch(
            PxyLayerInfo layer,
            String url,
            int z,
            int x,
            int y,
            GiMimeType format,
            int timeoutMillis) {
        TileOperationContext context = TileOperationContext.current();
        if (context == null)
            context = new TileOperationContext(new TileCancellationToken(), timeoutMillis);
        try (TileOperationScope scope = context.bind()) {
            UpstreamRequestOptions options =
                    layer.getUpstreamRequest() == null
                            ? new UpstreamRequestOptions()
                            : layer.getUpstreamRequest();
            if (options.getMaxAttempts() < 1 || options.getMaxAttempts() > 10)
                throw new IllegalArgumentException("上游尝试次数须为 1～10");
            int previousStatus = 0;
            for (int attempt = 1; attempt <= options.getMaxAttempts(); attempt++) {
                context.check();
                UpstreamTileRequest request =
                        new UpstreamTileRequest(
                                layer.getLayerName(), url, z, x, y, attempt, previousStatus);
                request.getHeaders().putAll(HttpTileRequestUtils.buildDefaultHeaders());
                if (options.getReferer() != null && !options.getReferer().trim().isEmpty())
                    request.getHeaders().put("Referer", options.getReferer());
                if (options.getOrigin() != null && !options.getOrigin().trim().isEmpty()) {
                    URI origin = URI.create(options.getOrigin());
                    if (!("http".equalsIgnoreCase(origin.getScheme())
                                    || "https".equalsIgnoreCase(origin.getScheme()))
                            || origin.getHost() == null
                            || origin.getRawUserInfo() != null
                            || origin.getRawQuery() != null
                            || origin.getRawFragment() != null
                            || (origin.getRawPath() != null
                                    && !origin.getRawPath().isEmpty()
                                    && !"/".equals(origin.getRawPath())))
                        throw new IllegalArgumentException("Origin 只允许协议、域名和端口");
                    request.getHeaders().put("Origin", options.getOrigin());
                }
                if (options.getHeaders() != null) request.getHeaders().putAll(options.getHeaders());
                if (options.getQueryParameters() != null)
                    request.getQueryParameters().putAll(options.getQueryParameters());
                if (layer.getUpstreamRequestCustomizer() != null) {
                    try {
                        layer.getUpstreamRequestCustomizer().customize(request);
                    } catch (TileOperationException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new TileOperationException(TileOperationStatus.FAILED, "上游鉴权策略执行失败");
                    }
                }
                validateHeaders(request.getHeaders());
                try {
                    URI uri = new URI(request.getUrl());
                    if (!request.getQueryParameters().isEmpty()) {
                        URIBuilder builder = new URIBuilder(uri);
                        for (Map.Entry<String, String> item :
                                request.getQueryParameters().entrySet())
                            builder.setParameter(item.getKey(), item.getValue());
                        uri = builder.build();
                    }
                    checkUri(uri);
                    for (int redirects = 0; ; redirects++) {
                        context.check();
                        TileFetchResult result =
                                HttpTileRequestUtils.executeTileRequest(
                                        uri.toString(),
                                        HttpTileRequestUtils.getHttpProxy(layer),
                                        context.remainingMillis(),
                                        format,
                                        request.getHeaders(),
                                        false);
                        previousStatus = result.statusCode;
                        if (result.resource != null) return result.resource;
                        if (result.statusCode >= 300
                                && result.statusCode < 400
                                && result.location != null
                                && options.isFollowRedirects()) {
                            if (redirects >= 5) throw new IllegalArgumentException("上游重定向过多");
                            URI next = uri.resolve(result.location);
                            checkUri(next);
                            // 携带自定义凭证的请求不可自动跨来源转发，防止服务重定向泄露密钥。
                            if (!sameOrigin(uri, next))
                                throw new IllegalArgumentException("上游跨来源重定向被拒绝，请配置最终服务地址");
                            uri = next;
                            continue;
                        }
                        break;
                    }
                } catch (TileOperationException e) {
                    throw e;
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (Exception e) {
                    context.check();
                    previousStatus = 0;
                }
                if (previousStatus >= 400 && previousStatus < 500 && previousStatus != 429) {
                    // 动态策略可根据 previousStatus 刷新一次凭证，避免无限鉴权重试。
                    if (layer.getUpstreamRequestCustomizer() == null
                            || attempt > 1
                            || (previousStatus != 401 && previousStatus != 403))
                        return finish(previousStatus);
                }
                if (attempt < options.getMaxAttempts())
                    context.sleep(Math.min(2000, 250L * attempt));
            }
            return finish(previousStatus);
        }
    }

    private static Resource finish(int status) {
        if (status == 404 || status == 204) return null;
        throw new TileOperationException(
                TileOperationStatus.FAILED, status == 0 ? "上游连接或响应解析失败" : "上游请求失败，HTTP " + status);
    }

    private static void checkUri(URI uri) {
        if (uri.getHost() == null
                || uri.getRawUserInfo() != null
                || !("http".equalsIgnoreCase(uri.getScheme())
                        || "https".equalsIgnoreCase(uri.getScheme())))
            throw new IllegalArgumentException("上游必须为不含用户信息的 HTTP/HTTPS 地址");
    }

    private static int port(URI uri) {
        return uri.getPort() >= 0
                ? uri.getPort()
                : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static boolean sameOrigin(URI a, URI b) {
        return a.getScheme().equalsIgnoreCase(b.getScheme())
                && a.getHost().equalsIgnoreCase(b.getHost())
                && port(a) == port(b);
    }

    private static void validateHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> item : headers.entrySet()) {
            String key = item.getKey(), value = item.getValue();
            if (key == null
                    || !key.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")
                    || value == null
                    || value.indexOf('\r') >= 0
                    || value.indexOf('\n') >= 0) throw new IllegalArgumentException("上游请求头格式无效");
            if ("Host".equalsIgnoreCase(key)
                    || "Content-Length".equalsIgnoreCase(key)
                    || "Connection".equalsIgnoreCase(key)
                    || "Transfer-Encoding".equalsIgnoreCase(key))
                throw new IllegalArgumentException("上游连接控制请求头不可覆盖");
        }
    }
}
