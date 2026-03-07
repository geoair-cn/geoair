package cn.geoair.map.dynamic.geoserver.beans;

import lombok.Data;

/**
 * @author ：张逢吉
 * @date ：Created in 13:19 @description： TODO
 */
@Data
public class GsLayer {

	/** 图层名称 */
	private String name;

	/** 空间参考系统 (SRS) */
	private String srs;

}
