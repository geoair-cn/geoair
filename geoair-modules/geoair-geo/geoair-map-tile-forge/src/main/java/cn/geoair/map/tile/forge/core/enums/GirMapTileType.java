package cn.geoair.map.tile.forge.core.enums;

import cn.geoair.base.data.GiVisualValuable;

/** @author ：zhangjun &#064;date ：Created in 2025/11/13 14:07 &#064;description： 瓦片类型的枚举 */
public enum GirMapTileType implements GiVisualValuable<String> {
    COMPACT_V1("compact_v1"),
    COMPACT_V2("compact_v2"),
    XYZ("xyz"), // XYZ格式
    LOOSE("loose"), // 松散型瓦片格式
    TILE_3D("3d_tiles"),
    S3M("s3m"),
    MVT_TILES("mvt_tiles"), // 包含style.json的一个zip包
    TERRAIN_3D("3d_terrain"),
    ;
    private final String value;

    GirMapTileType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
