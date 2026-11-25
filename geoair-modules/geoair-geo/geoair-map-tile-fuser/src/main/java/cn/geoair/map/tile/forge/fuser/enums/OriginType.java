package cn.geoair.map.tile.forge.fuser.enums;

import cn.geoair.base.data.GiVisualValuable;
import lombok.Getter;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/15 11:01
 * @description：
 */
/** @deprecated 使用 {@link TileRowOrigin} 表示行号原点；WMTS 是协议，不应作为坐标原点名称。 */
@Deprecated
@Getter
public enum OriginType implements GiVisualValuable<String> {
    TMS("tms"),
    Google("wmts");

    private final String mode;

    OriginType(String mode) {
        this.mode = mode;
    }

    /** 根据mode值获取枚举 */
    public static OriginType fromMode(String mode) {
        for (OriginType type : OriginType.values()) {
            if (type.mode.equals(mode)) {
                return type;
            }
        }
        return Google;
    }

    /** 判断是否为网络类型 */
    public boolean isGoogle() {
        return this == Google;
    }

    /** 判断是否为本地类型 */
    public boolean isTMS() {
        return this == TMS;
    }

    @Override
    public String toString() {
        return mode;
    }
}
