package cn.geoair.map.dynamic.tools.simple.response;

import cn.geoair.base.data.GiVisualValuable;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/13 17:25
 * @description： web请求中的请求参数枚举
 */
public enum TileParamEnums implements GiVisualValuable<String> {

    /** ZXY 类型：xyz 或 zyx */
    ZXY_TYPE("zxyType", "zxy"),

    /** 坐标系：EPSG:3857, EPSG:4326, EPSG:4490 */
    GRID_SET("gridSet", "EPSG:3857"),

    /** 原点类型：tms 或 wmts */
    ORIGIN_TYPE("originType", "wmts"),

    /** 图片格式：png, jpg, webp 等 */
    FORMAT("format", "png"),

    /** 是否启用图像增强：true / false */
    ENHANCE("enhance", "false"),

    /** 锐化强度：0.5 ~ 3.0 */
    SHARPEN_AMOUNT("sa", "1.2"),

    /** 模糊半径：0.5 ~ 3.0 */
    SHARPEN_RADIUS("sr", "1.5"),

    /** 亮度阈值：0 ~ 30 */
    SHARPEN_THRESHOLD("sp", "5"),
    ;

    private final String value;
    private final String defaultValue;

    TileParamEnums(String value, String defaultValue) {
        this.value = value;
        this.defaultValue = defaultValue;
    }

    public String getValue() {
        return value;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public static TileParamEnums fromValue(String value) {
        for (TileParamEnums key : values()) {
            if (key.value.equals(value)) {
                return key;
            }
        }
        return null;
    }
}
