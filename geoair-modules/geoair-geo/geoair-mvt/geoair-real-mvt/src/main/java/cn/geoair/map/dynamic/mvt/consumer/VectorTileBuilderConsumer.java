package cn.geoair.map.dynamic.mvt.consumer;

import java.util.function.Consumer;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于Consumer模式的VectorTile PBF构建器（流式推送要素，低内存占用）
 */
@Slf4j
public abstract class VectorTileBuilderConsumer implements Consumer<GirAdvOneRow> {

	@Setter
	protected CustomTransformConsumer customTransformConsumer;

	/**
	 * 构建最终的PBF字节数组 所有要素推送完成后调用此方法
	 */
	public abstract byte[] build();

}
