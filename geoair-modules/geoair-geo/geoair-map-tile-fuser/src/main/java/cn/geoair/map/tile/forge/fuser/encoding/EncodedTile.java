package cn.geoair.map.tile.forge.fuser.encoding;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 真实格式随字节返回，不能仅凭图层声明决定缓存内容。 */
@Getter
@AllArgsConstructor
public final class EncodedTile {
    private final byte[] bytes;
    private final String format;
    private final boolean transparent;
}
