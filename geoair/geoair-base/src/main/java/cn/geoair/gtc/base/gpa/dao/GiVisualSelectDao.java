package cn.geoair.gtc.base.gpa.dao;

import java.io.Serializable;
import java.util.List;

import cn.geoair.gtc.base.data.model.support.GirVisualModelKid;
import cn.geoair.gtc.base.data.page.GiPageParam;
import cn.geoair.gtc.base.data.page.GiPager;
import cn.geoair.gtc.base.gpa.entity.GiEntityVisuable;

public interface GiVisualSelectDao<M extends GiEntityVisuable<PK>,PK extends Serializable> extends GiDao<M,PK> {

	@SuppressWarnings("unchecked")
	public static<M extends GiEntityVisuable<PK>,PK extends Serializable> GiVisualSelectDao<M,PK> getDao(Class<M> modelCls) {
		return GiDao.getDao(GiVisualSelectDao.class, modelCls);
	}
	@SuppressWarnings("rawtypes")
	List<GirVisualModelKid>  gtcSearchVisualModel(String displayQuery, String[] containKeys);

	@SuppressWarnings("rawtypes")
	GiPager<GirVisualModelKid> gtcSearchVisualPage(String displayQuery, String[] containKeys, GiPageParam pageParam);


}
