package cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics;

import java.io.Serializable;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;

public class FieldStatUtils implements Serializable {

	private static final long serialVersionUID = 1L;

	// 从AdvOneRow提取指定字段的值
	public static Object getFieldValue(GirAdvOneRow row, String fieldName) {
		Object value = row.get(fieldName);
		return value == null ? "" : value;
	}

	// 判断字段类型（Number/String）

	public static String getFieldType(Object value) {

		if (value == null) {
			return "String";
		}
		if (value instanceof Boolean) {
			return "Boolean"; // 对应JSON中的"boolean"
		}
		else if (value instanceof Number) {
			return "Number"; // 对应JSON中的"number"
		}
		else {
			return "String"; // 对应JSON中的"string"
		}
	}

}
