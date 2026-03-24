package cn.geoair.orm.mybatisplus.impls;

import java.io.Serializable;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
// import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cn.geoair.base.gpa.dao.GiDeleteDao;
import cn.geoair.base.gpa.entity.GiEntityRemovable;

public interface PlusDeleteMapper<T extends GiEntityRemovable<PK>, PK extends Serializable>
		extends GiDeleteDao<T, PK>, BaseMapper<T> {

	/**
	 * 根据条件删除记录
	 * @param t
	 * @return
	 */
	@Override
	default int gtcDeleteBy(T t) {
		return this.delete(new QueryWrapper<T>(t));
	}

	/**
	 * 根据主键删除记录
	 * @param key
	 * @return
	 */
	@Override
	default void gtcDeleteByPK(PK pk) {
		this.deleteById(pk);
	}

	/**
	 * 根据主键批量删除
	 * @param key
	 * @return
	 */
	@Override
	default void gtcDeleteByPK(List<PK> pks) {
		this.deleteBatchIds(pks);
	}

	/**
	 * 删除所有数据
	 * @param key
	 * @return
	 */
	@Override
	default void gtcDeleteAll() {
		this.delete(null);
	}

}
