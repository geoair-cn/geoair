package cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.json;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/1/4 09:42 @description： LayerStat
 */
@Data
public class LayerStat implements Serializable {

    private static final long serialVersionUID = 1L;

    private String layer;

    private long count; // 该图层总要素数

    private String geometry = "Point"; // 假设你的几何类型都是Point，可按需动态获取

    private int attributeCount; // 字段总数

    private List<AttributeStat> attributes;
}
