package cn.geoair.map.tile.forge.fuser.image.watermark;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 仅成功修补时携带新 PNG；其他结果保留原图。 */
@Getter
@AllArgsConstructor
public class WatermarkRepairResult {
    private final WatermarkRepairState state;
    private final double score;
    private final int regions;
    private final byte[] png;
    private final String reason;
}
