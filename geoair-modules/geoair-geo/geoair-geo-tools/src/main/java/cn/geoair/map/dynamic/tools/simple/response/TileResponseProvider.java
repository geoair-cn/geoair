package cn.geoair.map.dynamic.tools.simple.response;

/**
 * 将瓦片请求 URI 解析并转换为统一的 {@link TileResponse}。
 *
 * <p>接口不依赖 Web 请求对象，因此可由 servlet 和非 Web 调用方共同复用。
 */
public interface TileResponseProvider {

    /**
     * 根据手工提供的请求 URI 构建瓦片响应。
     *
     * @param requestUri 待解析的请求 URI；不包含协议、IP 或端口
     * @return 瓦片响应；请求无法处理时返回携带错误信息的响应对象
     */
    TileResponse getTileResponse(String requestUri);

    /**
     * 根据手工提供的请求 URI 与请求源构建瓦片响应。
     *
     * @param requestUri 待解析的请求 URI；不包含协议、IP 或端口
     * @param requestHost 请求源（协议、IP 或域名与端口）；为空时由实现按原有单参数逻辑处理
     * @return 瓦片响应；请求无法处理时返回携带错误信息的响应对象
     */
    default TileResponse getTileResponse(String requestUri, String requestHost) {
        return getTileResponse(requestUri);
    }
}
