package cn.geoair.map.tile.forge.fuser.provider;

import lombok.Getter;

/** 默认保留旧入口的异常行为，调用方可显式允许部分出图。 */
@Getter
public enum TileReadFailurePolicy {
    FAIL_FAST("读取异常向上传播"), KEEP_AVAILABLE("保留可用影像");
    private final String description;
    TileReadFailurePolicy(String description) { this.description = description; }
}
