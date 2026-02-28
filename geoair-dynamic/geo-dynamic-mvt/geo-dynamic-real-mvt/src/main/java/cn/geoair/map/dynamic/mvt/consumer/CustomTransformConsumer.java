package cn.geoair.map.dynamic.mvt.consumer;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.dto.TileGlobalConfig;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/2/3 13:17 @description： TODO
 */
public interface CustomTransformConsumer {

	void accept(GirAdvOneRow oneRow, TileGlobalConfig tileGlobalConfig);

}
