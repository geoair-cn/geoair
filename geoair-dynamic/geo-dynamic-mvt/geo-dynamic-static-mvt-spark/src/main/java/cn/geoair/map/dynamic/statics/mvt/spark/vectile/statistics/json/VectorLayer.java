package cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.json;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/1/4 09:41 @description： 矢量图层元数据
 */
//
@Data
public class VectorLayer implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;

	private String description = "";

	private int minzoom = 0;

	private int maxzoom = 15;

	private Map<String, String> fields; // 字段名 -> 类型（Number/String）

}
