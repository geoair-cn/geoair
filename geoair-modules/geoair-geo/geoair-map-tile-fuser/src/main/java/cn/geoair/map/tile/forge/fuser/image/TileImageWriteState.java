package cn.geoair.map.tile.forge.fuser.image;

import lombok.Getter;

/** 写入阶段与算法阶段分开，生成图片并不代表副本已经保存。 */
@Getter
public enum TileImageWriteState {
    NOT_REQUIRED("无需写入"),
    STORED("已保存"),
    ENCODING_FAILED("编码失败"),
    FAILED("写入失败");
    private final String description;

    TileImageWriteState(String description) {
        this.description = description;
    }
}
