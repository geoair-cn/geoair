package cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.json;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/1/4 09:42 @description： 单个字段的统计
 */
@Data
public class AttributeStat implements Serializable {

	private static final long serialVersionUID = 1L;

	private String attribute; // 字段名

	private long count; // 该字段非空值总数

	private String type; // number/string

	private List<Object> values; // 去重后的值列表

	private Number min; // 数值型字段最小值（字符串为null）

	private Number max; // 数值型字段最大值（字符串为null）

	// 添加valueCounts字段，记录每个值的出现次数 {"小麦":2, "玉米":3}
	private List<Long> statics;

}
