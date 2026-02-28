package cn.geoair.gtc.orm.tkmapper.impls;

import java.io.Serializable;
import java.util.List;

import cn.geoair.gtc.base.gpa.dao.GiUpdateDao;
import cn.geoair.gtc.base.gpa.entity.GiEntityAlterable;
import cn.geoair.gtc.orm.mybatis.impls.MyBatisMapper;
import cn.geoair.gtc.orm.tkmapper.support.update.UpdateBatchMapper;

import tk.mybatis.mapper.common.base.BaseUpdateMapper;
import tk.mybatis.mapper.common.example.UpdateByExampleMapper;
import tk.mybatis.mapper.common.example.UpdateByExampleSelectiveMapper;

public interface TkUpdateMapper<T extends GiEntityAlterable<PK>, PK extends Serializable>
		extends MyBatisMapper<T, PK>, UpdateBatchMapper<T>, UpdateByExampleMapper<T>, UpdateByExampleSelectiveMapper<T>,
		BaseUpdateMapper<T>, GiUpdateDao<T, PK> {

	/**
	 * 根据主键更新记录(更新所有字段)
	 * @param t
	 * @return
	 */
	@Override
	default int gtcUpdateByPK(T t) {
		return this.updateByPrimaryKey(t);
	}

	/**
	 * 根据主键更新记录(更新不为Null的字段)
	 * @param t
	 * @return
	 */
	@Override
	default int gtcUpdateByPKSelective(T t) {
		return this.updateByPrimaryKeySelective(t);
	}

	/**
	 * 根据主键批量更新
	 * @param records
	 * @return
	 */
	@Override
	default int gtcUpdateByPK(List<T> records) {
		return this.batchUpdateByPK(records);
	}

	/**
	 * 根据主键批量更新(更新不为Null的字段)
	 * @param records
	 * @return
	 */
	@Override
	default int gtcUpdateByPKSelective(List<T> records) {
		return this.batchUpdateByPKSelective(records);
	}

}
