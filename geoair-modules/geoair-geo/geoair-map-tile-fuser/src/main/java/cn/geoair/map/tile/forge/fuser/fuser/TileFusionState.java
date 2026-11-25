package cn.geoair.map.tile.forge.fuser.fuser;

import lombok.Getter;

/** 出图完整性；真实无覆盖与源读取故障分别处理。 */
@Getter
public enum TileFusionState {
    COMPLETE("完整"),
    PARTIAL("部分缺片"),
    NO_COVERAGE("无覆盖"),
    FAILED("失败");
    private final String description;

    TileFusionState(String description) {
        this.description = description;
    }
}
