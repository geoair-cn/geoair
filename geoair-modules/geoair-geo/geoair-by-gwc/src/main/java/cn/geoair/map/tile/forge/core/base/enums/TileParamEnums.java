package cn.geoair.map.tile.forge.core.base.enums;

import cn.geoair.base.data.GiVisualValuable;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/13 17:25
 * @description： web请求中的请求参数枚举
 */
public enum TileParamEnums implements GiVisualValuable<String> {

    /**
     * ZXY 类型：xyz 或 zyx
     */
    ZXY_TYPE("zxyType"),

    /**
     * 坐标系：EPSG:3857, EPSG:4326, EPSG:4490
     */
    GRID_SET("gridSet"),

    /**
     * 原点类型：tms 或 wmts
     */
    ORIGIN_TYPE("originType"),

    /**
     * 图片格式：png, jpg, webp 等
     */
    FORMAT("format"),

    /**
     * 锐化强度：0.5 ~ 3.0
     */
    SHARPEN_AMOUNT("sharpenAmount"),

    /**
     * 模糊半径：0.5 ~ 3.0
     */
    SHARPEN_RADIUS("sharpenRadius"),

    /**
     * 亮度阈值：0 ~ 30
     */
    SHARPEN_THRESHOLD("sharpenThreshold"),


    ;

    private final String value;

    TileParamEnums(String value) {
        this.value = value;
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
