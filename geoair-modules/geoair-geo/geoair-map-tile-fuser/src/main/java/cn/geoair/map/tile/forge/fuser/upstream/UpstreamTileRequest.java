package cn.geoair.map.tile.forge.fuser.upstream;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** 一次请求独立的可变副本，行号已转换为上游实际行号。避免持有或修改全局请求头。 */
@Getter
public class UpstreamTileRequest {
    private final String layerName;
    private final int z, x, y, attempt, previousStatus;
    @Setter private String url;
    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, String> queryParameters = new LinkedHashMap<>();

    public UpstreamTileRequest(
            String layerName, String url, int z, int x, int y, int attempt, int previousStatus) {
        this.layerName = layerName;
        this.url = url;
        this.z = z;
        this.x = x;
        this.y = y;
        this.attempt = attempt;
        this.previousStatus = previousStatus;
    }
}
