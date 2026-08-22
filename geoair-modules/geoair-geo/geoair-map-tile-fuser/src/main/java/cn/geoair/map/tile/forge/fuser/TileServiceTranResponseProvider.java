package cn.geoair.map.tile.forge.fuser;

import cn.geoair.map.dynamic.tools.simple.response.TileResponse;
import cn.geoair.map.dynamic.tools.simple.response.TileResponseProvider;

/**
 * 瓦片服务坐标转换的非 Web 调用接口。
 *
 * <p>各方法只负责构建 {@link TileResponse}，不读取或写入 Servlet 响应对象。</p>
 */
public interface TileServiceTranResponseProvider extends TileResponseProvider {

    TileResponse googleServiceTo4326RequestForTileResponse(String layerName, Integer z, Integer x, Integer y);

    TileResponse googleServiceTo4326RequestForTileResponse(
            String layerName, Integer z, Integer x, Integer y, String outputFormat);

    TileResponse googleServiceTo4326RequestDelCacheForTileResponse(
            String layerName, Integer z, Integer x, Integer y, String outputFormat);

    TileResponse grid4490ServiceTo3857RequestForTileResponse(String layerName, Integer z, Integer x, Integer y);

    TileResponse grid4490ServiceTo3857RequestForTileResponse(
            String layerName, Integer z, Integer x, Integer y, String outputFormat);

    TileResponse grid4490ServiceTo3857RequestDelCacheForTileResponse(
            String layerName, Integer z, Integer x, Integer y, String outputFormat);
}
