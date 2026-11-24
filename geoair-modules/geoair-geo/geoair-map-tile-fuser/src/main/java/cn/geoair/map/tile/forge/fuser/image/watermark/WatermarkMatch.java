package cn.geoair.map.tile.forge.fuser.image.watermark;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 检测到的模板位置及归一化相关系数。 */
@Getter @AllArgsConstructor
public class WatermarkMatch {
    private final int x;
    private final int y;
    private final double score;
}
