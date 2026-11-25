package cn.geoair.map.tile.forge.fuser.cache;

import lombok.Getter;

/** 缓存驱动分类。 */
@Getter
public enum CacheStorageKind {
    FILE("文件"),
    MBTILES("MBTiles"),
    OTHER("自定义驱动");
    private final String description;

    CacheStorageKind(String description) {
        this.description = description;
    }
}
