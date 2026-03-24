package cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.json;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/1/4 09:41 @description： 瓦片统计总览
 */
//
@Data
public class TileStats implements Serializable {

	private static final long serialVersionUID = 1L;

	private int layerCount;

	private List<LayerStat> layers;

}
