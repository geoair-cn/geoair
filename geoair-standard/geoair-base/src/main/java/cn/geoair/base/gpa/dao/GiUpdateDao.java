package cn.geoair.base.gpa.dao;

import java.io.Serializable;
import java.util.List;

import cn.geoair.base.gpa.entity.GiEntityAlterable;

public interface GiUpdateDao<M extends GiEntityAlterable<PK>, PK extends Serializable> extends GiDao<M, PK> {

	@SuppressWarnings("unchecked")
	public static <M extends GiEntityAlterable<PK>, PK extends Serializable> GiUpdateDao<M, PK> getDao(
			Class<M> modelCls) {
		return GiDao.getDao(GiUpdateDao.class, modelCls);
	}

	/**
	 * 根据主键更新记录(更新所有字段)
	 * @param t 待更新的实体对象
	 * @return 更新影响的行数
	 */
	int gtcUpdateByPK(M t);

	/**
	 * 根据主键更新记录(更新不为Null的字段)
	 * @param t 待更新的实体对象，只更新非空字段
	 * @return 更新影响的行数
	 */
	int gtcUpdateByPKSelective(M t);

	/**
	 * 根据主键批量更新
	 * @param records 待更新的实体对象列表
	 * @return 更新影响的行数
	 */
	int gtcUpdateByPK(List<M> records);

	/**
	 * 根据主键批量更新(更新不为Null的字段)
	 * @param records 待更新的实体对象列表，只更新每个对象中的非空字段
	 * @return 更新影响的行数
	 */
	int gtcUpdateByPKSelective(List<M> records);

}
