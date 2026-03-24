package cn.geoair.map.dynamic.geoserver.beans;

import java.util.HashMap;
import java.util.Map;

import cn.geoair.map.dynamic.geoserver.enums.DataSourceType;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** Shapefile（SHP）数据源配置 继承 BaseDatastore，扩展 SHP 专属参数 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ShpDatastore extends BaseDatastore {

	/** SHP 文件存储根目录（绝对路径） */
	private String shpRootPath;

	/** 字符编码（默认 GBK，适配中文） */
	private String charset = "GBK";

	/** 构造函数：默认设置数据源类型为 SHP */
	public ShpDatastore() {
		super.setDataSourceType(DataSourceType.SHAPEFILE);
	}

	/** 转换为 GeoServer SHP 连接参数 */
	@Override
	public Map<String, String> toConnectionParams() {
		Map<String, String> params = new HashMap<>();
		params.put("dbtype", "shapefile");
		params.put("url", "file:" + this.getShpRootPath()); // GeoServer 要求 file: 前缀
		params.put("charset", this.getCharset());
		// 可选：添加 SHP 额外参数
		params.put("create spatial index", "true");
		params.put("memory mapped buffer", "true");
		return params;
	}

}
