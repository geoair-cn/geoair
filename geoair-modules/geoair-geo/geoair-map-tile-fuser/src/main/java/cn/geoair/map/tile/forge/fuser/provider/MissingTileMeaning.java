package cn.geoair.map.tile.forge.fuser.provider;

import lombok.Getter;

/** 读取器返回空值的明确语义，不能把网络故障误当成真实无覆盖。 */
@Getter
public enum MissingTileMeaning {
    UNAVAILABLE("源片不可用"), NO_COVERAGE("真实无覆盖");
    private final String description;
    MissingTileMeaning(String description) { this.description = description; }
}
