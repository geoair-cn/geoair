package cn.geoair.map.tile.forge.core.servlet;

import cn.geoair.map.dynamic.tools.simple.response.TileResponse;

/**
 * 将瓦片请求 URI 解析并转换为统一的 {@link TileResponse}。
 *
 * <p>接口不依赖 Web 请求对象，因此可由 servlet 和非 Web 调用方共同复用。</p>
 */
public interface TileResponseProvider {

    /**
     * 根据手工提供的请求 URI 或完整 URL 构建瓦片响应。
     *
     * @param requestUri 待解析的请求 URI 或完整 URL
     * @return 瓦片响应；请求无法处理时返回携带错误信息的响应对象
     */
    TileResponse getTileResponse(String requestUri);
}
