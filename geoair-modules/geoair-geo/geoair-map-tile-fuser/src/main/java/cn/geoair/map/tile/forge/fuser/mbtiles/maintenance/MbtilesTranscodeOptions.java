package cn.geoair.map.tile.forge.fuser.mbtiles.maintenance;

import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingMode;
import cn.geoair.map.tile.forge.fuser.encoding.TileEncodingPolicy;

import lombok.Data;

/** 仅描述编码和范围版本，不携带应用图层或业务任务模型。 */
@Data
public class MbtilesTranscodeOptions {
    private String checkpointId;
    private String expectedFileIdentity;
    private TileEncodingPolicy encoding = new TileEncodingPolicy(TileEncodingMode.JPEG.name(), 75);
    private MbtilesSourceFormat sourceFormat = MbtilesSourceFormat.ANY;
    private int minZoom = 0;
    private int maxZoom = 22;
    private boolean onlyIfSmaller = true;
    private String scopeId = "all";

    public MbtilesTranscodeOptions copy() {
        MbtilesTranscodeOptions value = new MbtilesTranscodeOptions();
        value.checkpointId = checkpointId;
        value.expectedFileIdentity = expectedFileIdentity;
        if (encoding == null) throw new IllegalArgumentException("编码策略不能为空");
        value.encoding = new TileEncodingPolicy(encoding.getMode(), encoding.getQuality());
        value.encoding.validate();
        if (checkpointId == null || !checkpointId.matches("[A-Za-z0-9_.-]{1,80}"))
            throw new IllegalArgumentException("恢复点标识无效");
        TileEncodingMode mode = TileEncodingMode.parse(value.encoding.getMode());
        if (mode != TileEncodingMode.PNG && mode != TileEncodingMode.JPEG)
            throw new IllegalArgumentException("转码目标仅支持PNG/JPEG");
        if (minZoom < 0
                || maxZoom > 22
                || minZoom > maxZoom
                || sourceFormat == null
                || scopeId == null
                || scopeId.length() > 1024) throw new IllegalArgumentException("级别、源格式或范围版本无效");
        value.sourceFormat = sourceFormat;
        value.minZoom = minZoom;
        value.maxZoom = maxZoom;
        value.onlyIfSmaller = onlyIfSmaller;
        value.scopeId = scopeId;
        return value;
    }
}
