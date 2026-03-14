package cn.geoair.orm.mybatisplus.impls;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.geoair.base.gpa.dao.GiRetrieveDao;
import cn.geoair.base.gpa.entity.GiEntityQueryable;

public interface PlusRetrieveMapper<T extends GiEntityQueryable<PK>, PK extends Serializable>
		extends BaseMapper<T>, GiRetrieveDao<T, PK> {

	/**
	 * 根据条件查询多条记录
	 * @param t
	 * @return
	 */
	default List<T> select(T t) {
		return this.selectList(new QueryWrapper<T>(t));
	}

	/**
	 * 根据条件查询总数
	 * @param t
	 * @return
	 */
	default long selectCount() {
		Long ct = this.selectCount(null);
		if (ct != null) {
			return ct.longValue();
		}
		return 0;
	}

	/**
	 * 是否能够找到主键的记录
	 * @param pk
	 * @return
	 */
	@Override
	default boolean gtcExistsWithPK(PK pk) {
		return this.selectById(pk) == null;
	}

	/**
	 * 根据主键查询记录
	 * @param pk
	 * @return
	 */
	@Override
	default T gtcSearchByPK(PK pk) {
		return this.selectById(pk);
	}

	/**
	 * 根据多个主键查询多个结果
	 * @param pks
	 * @return
	 */
	@Override
	default List<T> gtcSearchByPK(Set<PK> pks) {
		Collection<PK> coll = new LinkedList<PK>();
		pks.forEach(coll::add);
		return this.selectBatchIds(coll);
	}

	/**
	 * 根据实体中的属性进行查询，只能有一个返回值，有多个结果时抛出异常，查询条件使用等号
	 * @param t
	 * @return
	 */
	@Override
	default T gtcSearchOne(T t) {
		return this.selectOne(new QueryWrapper<T>(t));
	}

	/**
	 * 根据实体中的属性进行查询，只返回第一条数据,查询条件使用等号
	 * @param t
	 * @return
	 */
	@Override
	default T gtcSearchFirst(T t) {
		Iterable<T> it = this.select(t);
		if (it.iterator().hasNext()) {
			return it.iterator().next();
		}
		return null;
	}

	/**
	 * 查询单表所有记录
	 * @return
	 */
	@Override
	default List<T> gtcSearchAll() {
		return this.selectList(null);
	}

	/**
	 * 根据条件查询多条记录
	 * @param t
	 * @return
	 */
	@Override
	default List<T> gtcSearch(T t) {
		return this.selectList(new QueryWrapper<T>(t));
	}

	/**
	 * 根据条件查询总数
	 * @param t
	 * @return
	 */
	@Override
	default long gtcSearchCount(T t) {
		Long ct = this.selectCount(new QueryWrapper<T>(t));
		if (ct != null) {
			return ct.longValue();
		}
		return 0;
	}

	default long gtcSearchCount() {
		return selectCount();
	}

}
