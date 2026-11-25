package cn.geoair.map.tile.forge.fuser.image;

import lombok.Getter;

/** 图像处理结果；需复核和失败均不发布替换图片。 */
@Getter
public enum TileImageProcessingState {
    UNCHANGED("保持原图"), PROCESSED("已处理"), REVIEW("待复核"), SKIPPED("已跳过"), FAILED("失败");
    private final String description;
    TileImageProcessingState(String description) { this.description = description; }
}
