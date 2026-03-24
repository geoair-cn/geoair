package cn.geoair.map.dynamic.geoserver.beans;

import java.util.Map;

import cn.geoair.map.dynamic.geoserver.enums.DataSourceType;

import lombok.Data;

/** 所有数据源配置的抽象父类 定义通用属性，子类扩展专属属性 */
@Data
public abstract class BaseDatastore {

	/** 数据源名称（全局唯一，关联工作区） */
	private String name;

	/** 数据源类型（PostGIS/SHP等） */
	private DataSourceType dataSourceType;

	/**
	 * 将当前数据源配置转换为 GeoServer 所需的连接参数 Map 子类需实现各自的转换逻辑
	 * @return 连接参数 Map
	 */
	public abstract Map<String, String> toConnectionParams();

}
