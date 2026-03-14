package cn.geoair.base.gpa.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cn.geoair.base.gpa.entity.GiEntitySaveable;

public interface GiCreateDao<M extends GiEntitySaveable<PK>, PK extends Serializable> extends GiDao<M, PK> {

	@SuppressWarnings("unchecked")
	public static <M extends GiEntitySaveable<PK>, PK extends Serializable> GiCreateDao<M, PK> getDao(
			Class<M> modelCls) {
		return (GiCreateDao<M, PK>) GiDao.getDao(GiCreateDao.class, modelCls);
	}

	/**
	 * 保存一条记录(属性不判空，为空的属性插入为空，无视数据库默认值)
	 * <p>
	 * 注意：由于ORM框架会判断字段注解@Column(insertable=false)时不处理字段， 所以也不能说完全无视数据库默认值，前提是插入语句包含该字段
	 * </p>
	 * @param t 实体对象
	 * @return PK 主键
	 */
	PK gtcAccess(M t);

	/**
	 * 批量插入(属性不判空，为空的属性插入为空，无视数据库默认值)
	 * <p>
	 * 注意：由于ORM框架会判断字段注解@Column(insertable=false)时不处理字段， 所以也不能说完全无视数据库默认值，前提是插入语句包含该字段
	 * </p>
	 * @param records 实体对象列表
	 * @return 主键 List<PK>
	 */
	List<PK> gtcAccess(List<M> records);

	/**
	 * 插入一条记录(属性判空，为空的属性不做插入操作)
	 * @param t 实体对象
	 * @return PK 主键
	 */
	PK gtcAccessSelective(M t);

	/**
	 * 批量插入(属性判空，为空的属性不做插入操作)
	 * <p>
	 * 批量插入语句字段是固定的，所以默认实现只是循环调用单条插入方法
	 * </p>
	 * @param records 实体对象列表
	 * @return 主键 List<PK>
	 */
	default List<PK> gtcAccessSelective(List<M> records) {

		List<PK> res = new ArrayList<>();
		for (M item : records) {
			res.add(this.gtcAccessSelective(item));
		}
		return res;
	}

}
