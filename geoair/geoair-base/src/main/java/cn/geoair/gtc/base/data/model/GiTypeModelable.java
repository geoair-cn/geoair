package cn.geoair.gtc.base.data.model;

import java.io.Serializable;

/**
 * 模型类型
 *
 * @author Ray
 * @param <ID> 主键类型
 */
public interface GiTypeModelable<ID extends Serializable> extends GiModelable<ID> {

	GiModelType gtcModelType();

}
