package cn.geoair.gtc.base.gpa.entity;

import java.io.Serializable;

/**
 * CRUD表模型
 *
 * @author Ray
 * @param <PK> 主键类型
 */
public interface GiCrudEntity<PK extends Serializable>
		extends GiEntitySaveable<PK>, GiEntityRemovable<PK>, GiEntityAlterable<PK>, GiEntityQueryable<PK> {

}
