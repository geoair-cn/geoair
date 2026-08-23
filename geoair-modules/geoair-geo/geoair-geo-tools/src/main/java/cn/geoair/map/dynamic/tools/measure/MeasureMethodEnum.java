package cn.geoair.map.dynamic.tools.measure;

import lombok.Getter;

/**
 * 空间测量采用的计算方式。
 *
 * <p>面积、长度和距离的数值会因计算方式而不同。调用方应依据业务精度、数据范围和要素类型
 * 显式选择，避免由工具类隐式决定。</p>
 *
 * @author 张逢吉
 */
@Getter
public enum MeasureMethodEnum {

    /** EPSG:3857 平面计算，适合瓦片地图与快速展示，存在随纬度增大的投影形变。 */
    WEB_MERCATOR("Web Mercator"),

    /** 以几何中心选择单个 UTM 投影带的局部平面计算，适合范围较小且基本不跨带的数据。 */
    UTM("UTM 局部投影"),

    /** 基于椭球大地线的计算方式，目前支持两点距离和线、面边界长度，不提供面积计算。 */
    GEODETIC("大地线");

    /** 面向日志、文档和界面的中文说明。 */
    private final String description;

    MeasureMethodEnum(String description) {
        this.description = description;
    }
}
