package cn.geoair.map.dynamic.mvt.exec.dto;

import cn.geoair.map.dynamic.mvt.tools.model.PbfInfo;

import lombok.Data;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/19 14:04 @description： 返回结果集
 */
@Data
public class TileRequest {

	/**
	 * 瓦片数据
	 */
	PbfInfo pbfInfo;

	/**
	 * 是否成功标识
	 */
	boolean successIs;

	/**
	 * http请求地址
	 */
	String httpUrl;

}
