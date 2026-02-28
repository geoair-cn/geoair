package cn.geoair.gtc.base.gpa.dao;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.geoair.gtc.base.gpa.entity.GiEntityQueryable;

/**
 * 查询Dao接口，提供基本的查询功能
 *
 * @author Ray
 * @param <M> 实体类类型，必须实现GiEntityQueryable接口
 * @param <PK> 主键类型，必须实现Serializable接口
 */
public interface GiRetrieveDao<M extends GiEntityQueryable<PK>, PK extends Serializable> extends GiDao<M, PK> {

	/**
	 * 根据实体类获取对应的Dao实例
	 * @param <M> 实体类类型，必须实现GiEntityQueryable接口
	 * @param <PK> 主键类型，必须实现Serializable接口
	 * @param modelCls 实体类的Class对象
	 * @return 对应实体类的GiRetrieveDao实例
	 */
	@SuppressWarnings("unchecked")
	public static <M extends GiEntityQueryable<PK>, PK extends Serializable> GiRetrieveDao<M, PK> getDao(
			Class<M> modelCls) {
		return GiDao.getDao(GiRetrieveDao.class, modelCls);
	}

	/**
	 * 判断是否存在指定主键的记录
	 * @param pk 主键值
	 * @return 存在返回true，否则返回false
	 */
	boolean gtcExistsWithPK(PK pk);

	/**
	 * 根据主键查询单条记录
	 * @param pk 主键值
	 * @return 查询到的实体对象，未找到返回null
	 */
	M gtcSearchByPK(PK pk);

	/**
	 * 根据多个主键查询多条记录
	 * @param pks 主键集合
	 * @return 查询到的实体对象列表
	 */
	List<M> gtcSearchByPK(Set<PK> pks);

	/**
	 * 根据多个主键查询多条记录
	 * @param pks 主键数组
	 * @return 查询到的实体对象列表
	 */
	default List<M> gtcSearchByPK(PK[] pks) {
		Set<PK> set = new HashSet<>();
		for (PK str : pks) {
			set.add(str);
		}
		return gtcSearchByPK(set);
	}

	/**
	 * 根据多个主键查询多条记录
	 * @param pks 主键列表
	 * @return 查询到的实体对象列表
	 */
	default List<M> gtcSearchByPK(List<PK> pks) {
		return gtcSearchByPK(new HashSet<PK>(pks));
	}

	/**
	 * 根据实体中的属性进行精确查询，只能有一个返回值，有多个结果时抛出异常
	 * @param t 包含查询条件的实体对象
	 * @return 查询到的唯一实体对象
	 */
	M gtcSearchOne(M t);

	/**
	 * 根据实体中的属性进行精确查询，只返回第一条数据
	 * @param t 包含查询条件的实体对象
	 * @return 查询到的第一条实体对象，未找到返回null
	 */
	M gtcSearchFirst(M t);

	/**
	 * 查询单表所有记录
	 * @return 所有实体对象列表
	 */
	List<M> gtcSearchAll();

	/**
	 * 根据条件查询多条记录
	 * @param t 包含查询条件的实体对象
	 * @return 符合条件的实体对象列表
	 */
	List<M> gtcSearch(M t);

	/**
	 * 根据条件查询记录总数
	 * @param t 包含查询条件的实体对象
	 * @return 符合条件的记录数量
	 */
	long gtcSearchCount(M t);

	/**
	 * 查询表中记录总数
	 * @return 表中所有记录的数量
	 */
	long gtcSearchCount();

	// /**
	// * 通过 example 条件查询
	// * @param example
	// * @return
	// */
	//
	// List<M> searchByExample(Object example);

}
