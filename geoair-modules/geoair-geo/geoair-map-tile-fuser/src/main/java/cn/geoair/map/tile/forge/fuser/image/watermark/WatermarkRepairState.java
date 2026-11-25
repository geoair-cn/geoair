package cn.geoair.map.tile.forge.fuser.image.watermark;

/** 未发现不等于保证没有水印；低置信度不自动修改像素。 */
public enum WatermarkRepairState {
    UNCHANGED,
    REPAIRED,
    REVIEW,
    MISSING
}
