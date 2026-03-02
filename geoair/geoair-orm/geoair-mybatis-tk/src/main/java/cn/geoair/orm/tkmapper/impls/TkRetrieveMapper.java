package cn.geoair.orm.tkmapper.impls;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cn.geoair.base.gpa.dao.GiRetrieveDao;
import cn.geoair.base.gpa.entity.GiEntityQueryable;
import cn.geoair.base.util.GutilBean;
import cn.geoair.base.util.GutilObject;
import cn.geoair.base.util.GutilReflection;
import cn.geoair.orm.mybatis.impls.MyBatisMapper;
import tk.mybatis.mapper.common.base.BaseSelectMapper;
import tk.mybatis.mapper.common.example.SelectByExampleMapper;
import tk.mybatis.mapper.common.example.SelectCountByExampleMapper;
import tk.mybatis.mapper.entity.EntityColumn;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

public interface TkRetrieveMapper<T extends GiEntityQueryable<PK>, PK extends Serializable>
		extends MyBatisMapper<T, PK>, /* SelectByIdsMapper<T>, */BaseSelectMapper<T>, SelectByExampleMapper<T>,
		SelectCountByExampleMapper<T>, GiRetrieveDao<T, PK> {

	/**
	 * 是否能够找到主键的记录
	 * @param pk
	 * @return
	 */
	@Override
	default boolean gtcExistsWithPK(PK pk) {
		return this.existsWithPrimaryKey(pk);
	}

	/**
	 * 根据主键查询记录
	 * @param pk
	 * @return
	 */
	@Override
	default T gtcSearchByPK(PK pk) {
		T t = this.selectByPrimaryKey(pk);
		if (GutilObject.isNull(t)) {
			return null;
		}
		T copyObj = (T) GutilReflection.newInstance(t.getClass());
		GutilBean.copyProperties(t, copyObj); // 主要是防止一级缓存
		return copyObj;
	}

	/**
	 * 根据多个主键查询多个结果
	 * @param pks
	 * @return
	 */
	@Override
	default List<T> gtcSearchByPK(Set<PK> pks) {
		Set<EntityColumn> cs = EntityHelper.getPKColumns(this.getModelClass());
		if (cs.size() == 1) {
			if (this instanceof tk.mybatis.mapper.additional.idlist.SelectByIdListMapper) {
				/**
				 * 根据主键字符串进行查询，类中只有存在一个带有@Id注解的字段
				 */
				return ((tk.mybatis.mapper.additional.idlist.SelectByIdListMapper) this)
						.selectByIdList(new ArrayList<>(pks));
			}
			else {
				throw new RuntimeException(
						"根据主键批量查询如果只有一个带有@Id注解的字段，dao需要实现tk.mybatis.mapper.additional.idlist.SelectByIdListMapper接口");
			}
		}
		else {

			// 这里需要优化，单条查询有性能问题
			List<T> res = new ArrayList<>();
			for (PK pk : pks) {
				res.add(this.gtcSearchByPK(pk));
			}
			return res;
		}
	}

	/**
	 * 根据实体中的属性进行查询，只能有一个返回值，有多个结果时抛出异常，查询条件使用等号
	 * @param t
	 * @return
	 */
	@Override
	default T gtcSearchOne(T t) {
		T t1 = this.selectOne(t);
		if (GutilObject.isNull(t1)) {
			return null;
		}
		T copyObj = (T) GutilReflection.newInstance(t.getClass());
		GutilBean.copyProperties(t1, copyObj); // 主要是防止一级缓存
		return copyObj;
	}

	/**
	 * 根据实体中的属性进行查询，只返回第一条数据,查询条件使用等号
	 * @param t
	 * @return
	 */
	@Override
	default T gtcSearchFirst(T t) {
		// 这里需要优化，全查有性能问题
		Iterable<T> list = this.select(t);
		if (list.iterator().hasNext()) {
			return list.iterator().next();
		}
		return null;
	}

	/**
	 * 查询单表所有记录
	 * @return
	 */
	@Override
	default List<T> gtcSearchAll() {
		return this.selectAll();
	}

	/**
	 * 根据条件查询多条记录
	 * @param t
	 * @return
	 */
	@Override
	default List<T> gtcSearch(T t) {
		return this.select(t);
	}

	/**
	 * 根据条件查询总数
	 * @param t
	 * @return
	 */
	@Override
	default long gtcSearchCount(T t) {
		return this.selectCount(t);
	}

	/**
	 * 查询总数
	 * @param
	 * @return
	 */
	@Override
	default long gtcSearchCount() {
		// 这里要测试一下
		return this.selectCount(null);
	}

}
