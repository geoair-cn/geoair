package cn.geoair.map.tile.forge.fuser.execution;

import lombok.Getter;

/** 每张候选瓦片的处理结果，缺失与失败不能计为成功下载。 */
@Getter
public enum TileOperationStatus {
    CACHED("已有缓存"),
    DOWNLOADED("下载成功"),
    SKIPPED("范围外跳过"),
    MISSING("源瓦片缺失"),
    FAILED("处理失败"),
    CANCELLED("已取消"),
    TIMED_OUT("处理超时");
    private final String description;

    TileOperationStatus(String description) {
        this.description = description;
    }
}
