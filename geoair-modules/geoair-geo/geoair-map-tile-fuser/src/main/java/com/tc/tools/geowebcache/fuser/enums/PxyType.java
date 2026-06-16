package com.tc.tools.geowebcache.fuser.enums;

import lombok.Getter;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/15 11:01
 * @description： 瓦片获取器类型枚举
 */
@Getter
public enum PxyType {

    WEB("web"),
    LOCAL("local");

    private final String mode;

    PxyType(String mode) {
        this.mode = mode;
    }

    /**
     * 根据mode值获取枚举
     */
    public static PxyType fromMode(String mode) {
        for (PxyType type : PxyType.values()) {
            if (type.mode.equals(mode)) {
                return type;
            }
        }
        return WEB; // 默认返回WEB
    }

    /**
     * 判断是否为网络类型
     */
    public boolean isWeb() {
        return this == WEB;
    }

    /**
     * 判断是否为本地类型
     */
    public boolean isLocal() {
        return this == LOCAL;
    }

    @Override
    public String toString() {
        return mode;
    }
}
