package cn.geoair.gtc.base.gpa.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import cn.geoair.gtc.base.gpa.entity.GiEntityRemovable;

public interface GiDeleteDao<M extends GiEntityRemovable<PK>, PK extends Serializable> extends GiDao<M, PK> {

	@SuppressWarnings("unchecked")
	public static <M extends GiEntityRemovable<PK>, PK extends Serializable> GiDeleteDao<M, PK> getDao(
			Class<M> modelCls) {
		return GiDao.getDao(GiDeleteDao.class, modelCls);
	}

	/**
	 * 根据实体属性作为条件进行删除，查询条件使用等号
	 * @param t
	 * @return
	 */
	int gtcDeleteBy(M t);

	/**
	 * 根据主键删除记录
	 * @param pk
	 *
	 */
	void gtcDeleteByPK(PK pk);

	/**
	 * 根据主键批量删除
	 * @param pks
	 *
	 */
	void gtcDeleteByPK(List<PK> pks);

	default void gtcDeleteByPK(Set<PK> pks) {
		this.gtcDeleteByPK(new ArrayList<>(pks));
	}

	default void gtcDeleteByPK(PK[] pks) {
		gtcDeleteByPK(Arrays.asList(pks));
	}

	/**
	 * 根据对象列表的主键进行批量删除
	 * @param pos
	 *
	 */
	default void gtcDeleteAll(List<M> pos) {
		if (pos != null && pos.size() > 0) {
			List<PK> ids = new ArrayList<>();
			for (M m : pos) {
				ids.add(m.id());
			}
			this.gtcDeleteByPK(ids);
		}
	}

	/**
	 * 删除所有数据
	 *
	 */
	void gtcDeleteAll();

}
