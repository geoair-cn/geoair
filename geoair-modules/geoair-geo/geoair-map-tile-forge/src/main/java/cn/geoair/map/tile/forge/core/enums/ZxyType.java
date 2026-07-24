package cn.geoair.map.tile.forge.core.enums;

import cn.geoair.base.data.GiVisualValuable;
import lombok.Getter;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/6/15 11:01
 * @description：瓦片坐标模式枚举
 */
@Getter
public enum ZxyType implements GiVisualValuable<String> {
    ZXY("zxy"),
    ZYX("zyx");

    private final String mode;

    ZxyType(String mode) {
        this.mode = mode;
    }

    /**
     * 根据 mode 值获取对应的枚举
     *
     * @param mode 模式字符串（不区分大小写）
     * @return 对应的枚举，未找到时返回 ZXY
     */
    public static ZxyType fromMode(String mode) {
        if (mode == null || mode.isEmpty()) {
            return ZXY;
        }
        for (ZxyType type : ZxyType.values()) {
            if (type.mode.equalsIgnoreCase(mode)) {
                return type;
            }
        }
        return ZXY;
    }

    /** 判断是否为 ZXY 模式（坐标顺序为 Z/X/Y） */
    public boolean isZxy() {
        return this == ZXY;
    }

    /** 判断是否为 ZYX 模式（坐标顺序为 Z/Y/X） */
    public boolean isZyx() {
        return this == ZYX;
    }

    /**
     * 获取对应的瓦片请求格式
     *
     * @return 格式字符串
     */
    public String getFormat() {
        return this == ZXY ? "{z}/{x}/{y}" : "{z}/{y}/{x}";
    }

    @Override
    public String toString() {
        return mode;
    }
}
