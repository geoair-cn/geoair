package cn.geoair.map.tile.forge.fuser.encoding;

import lombok.Getter;

/** 编码目标由调用方显式指定，不通过业务图层名称后缀推断。 */
@Getter
public enum TileEncodingTarget {
    ORIGINAL("原始缓存"),
    OUTPUT("转换缓存");
    private final String description;

    TileEncodingTarget(String description) {
        this.description = description;
    }
}
