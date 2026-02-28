package cn.geoair.gtc.base.gpa.entity;

import java.io.Serializable;
import java.util.List;

import cn.geoair.gtc.base.gpa.dao.GiRetrieveDao;

/**
 * 可以作为查询条件的Model
 *
 * @author Ray
 * @param 主键类型
 */
public interface GiEntityQueryable<PK extends Serializable> extends GiEntityable<PK> {

	@SuppressWarnings("unchecked")
	default <M extends GiEntityQueryable<PK>> GiRetrieveDao<M, PK> selectDao() {
		return GiRetrieveDao.getDao((Class<M>) this.getClass());
	}

	/*
	 * default boolean queryById() { gtcModelable<?> item =
	 * this.getSelectDao().selectByPK(this.getId()); if(item != null) { return true; }
	 * return false; }
	 */

	default List<? extends GiEntityQueryable<PK>> queryBySelf() {
		GiRetrieveDao<GiEntityQueryable<PK>, PK> dao = this.selectDao();
		List<GiEntityQueryable<PK>> res = dao.gtcSearch(this);
		return res;
	}

}
