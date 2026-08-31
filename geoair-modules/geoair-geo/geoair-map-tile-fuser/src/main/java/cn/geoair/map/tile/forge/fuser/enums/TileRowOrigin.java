package cn.geoair.map.tile.forge.fuser.enums;

import cn.geoair.base.data.GiVisualValuable;
import lombok.Getter;

/**
 * 瓦片行号的原点方向。
 *
 * <p>该枚举只描述 Y 行号的方向，不代表服务协议或坐标参考系；网格由 gridSrid 单独定义。
 */
@Getter
public enum TileRowOrigin implements GiVisualValuable<String> {

    /** XYZ / Google 风格，y=0 位于网格顶部。 */
    TOP_LEFT("top-left"),

    /** TMS 风格，y=0 位于网格底部。 */
    BOTTOM_LEFT("bottom-left");

    private final String mode;

    TileRowOrigin(String mode) {
        this.mode = mode;
    }

    public boolean isTopLeft() {
        return this == TOP_LEFT;
    }

    public static TileRowOrigin fromMode(String mode) {
        if (mode == null) {
            return null;
        }
        for (TileRowOrigin origin : values()) {
            if (origin.mode.equalsIgnoreCase(mode)) {
                return origin;
            }
        }
        return null;
    }

    /** 将旧的 OriginType 配置映射为明确的行原点。 */
    public static TileRowOrigin fromLegacyOriginType(OriginType originType) {
        return originType != null && originType.isTMS() ? BOTTOM_LEFT : TOP_LEFT;
    }
}
