package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import java.io.Serializable;

import cn.geoair.map.dynamic.mvt.tools.model.PPbfType;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/1/21 17:47 @description： TODO
 */
@Data
@Accessors(chain = true)
public class PbfTargetInfo implements Serializable {

	// 是否仅仅生成一个pbf，用于节省内存
	private boolean isOnly = false;

	// 生成的pbf类型
	private PPbfType pPbfType = PPbfType.rootPbf;

	// 是否保存要素列表
	private boolean saveFeatureList = false;

	public static PbfTargetInfo getInstance() {
		return new PbfTargetInfo().setSaveFeatureList(false).setPPbfType(PPbfType.rootPbf).setOnly(false);
	}

}
