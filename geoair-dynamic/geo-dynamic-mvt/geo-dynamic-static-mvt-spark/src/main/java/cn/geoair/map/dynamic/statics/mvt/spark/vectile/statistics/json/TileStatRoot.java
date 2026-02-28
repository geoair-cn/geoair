package cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.json;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

// 顶层结果
@Data
public class TileStatRoot implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<VectorLayer> vector_layers;

	private TileStats tilestats;

}
