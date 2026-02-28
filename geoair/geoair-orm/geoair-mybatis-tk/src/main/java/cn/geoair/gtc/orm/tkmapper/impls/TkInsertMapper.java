package cn.geoair.gtc.orm.tkmapper.impls;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import cn.geoair.gtc.base.exception.GirException;
import cn.geoair.gtc.base.gpa.dao.GiCreateDao;
import cn.geoair.gtc.base.gpa.entity.GiEntitySaveable;
import cn.geoair.gtc.orm.mybatis.impls.MyBatisMapper;
import cn.geoair.gtc.orm.tkmapper.support.insert.GirInsertListMapper;
import cn.geoair.gtc.orm.tkmapper.support.insert.GirInsertMapper;

public interface TkInsertMapper<T extends GiEntitySaveable<PK>, PK extends Serializable>
		extends MyBatisMapper<T, PK>, GirInsertMapper<T>, GiCreateDao<T, PK> {

	/**
	 * 保存一条记录(属性不判空，为空的属性插入为空，无视数据库默认值)
	 * @param t
	 * @return
	 */
	@Override
	default PK gtcAccess(T t) {
		this.insert(t);
		return t.id();
	}

	/**
	 * 批量插入(属性不判空)各Mapper实现不同的InsertListMapper接口
	 * @param records
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	default List<PK> gtcAccess(List<T> records) {

		if (this instanceof GirInsertListMapper) {
			/**
			 * 批量插入，支持批量插入的数据库可以使用，例如MySQL,PG,H2等
			 * <p>
			 * 该方法支持 @KeySql 注解的 genId 方式
			 *
			 */
			((GirInsertListMapper) this).insertList(records);

		}
		else if (this instanceof tk.mybatis.mapper.additional.insert.InsertListMapper) {
			/**
			 * 批量插入，支持批量插入的数据库可以使用，例如MySQL,PG,H2等
			 * <p>
			 * 不支持主键策略，插入前需要设置好主键的值
			 * <p>
			 * 该方法支持 @KeySql 注解的 genId 方式
			 *
			 */
			((tk.mybatis.mapper.additional.insert.InsertListMapper) this).insertList(records);

		}
		else if (this instanceof tk.mybatis.mapper.common.special.InsertListMapper) {
			/**
			 * 批量插入，支持批量插入的数据库可以使用，例如MySQL,PG,H2等，另外该接口限制实体包含`id`属性并且必须为自增列
			 *
			 */
			((tk.mybatis.mapper.common.special.InsertListMapper) this).insertList(records);
		}
		else if (this instanceof tk.mybatis.mapper.additional.dialect.oracle.InsertListMapper) {
			/**
			 * <p>
			 * Oracle批量插入
			 * <p>
			 * 支持@{@link KeySql#genId()}，不支持@{@link KeySql#sql()}
			 * <p>
			 * 因INSERT ALL语法不支持序列，可手工获取序列并设置至Entity或绑定触发器
			 */
			((tk.mybatis.mapper.additional.dialect.oracle.InsertListMapper) this).insertList(records);
		}
		else {
			throw new GirException("{} 没有实现任何批量插入接口;"
					+ "支持批量插入的数据库(MySQL,PG,H2等)没有主键策略，自建组件生成方式实现 cn.geoair.orm.tkmapper.support.insert. gtcInsertListMapper;"
					// +
					// "支持批量插入的数据库(MySQL,PG,H2等)没有主键策略实现tk.mybatis.mapper.additional.insert.InsertListMapper;"
					+ "支持批量插入的数据库(MySQL,PG,H2等)自增主键策略实现tk.mybatis.mapper.common.special.InsertListMapper;"
					+ "Oracle数据库实现tk.mybatis.mapper.additional.dialect.oracle.InsertListMapper;",
					this.getClass().getName());
		}

		List<PK> res = new ArrayList<>();
		for (T item : records) {
			res.add(item.id());
		}
		return res;
	}

	/**
	 * 插入一条记录(属性判空，为空的属性不做插入操作)
	 * @param t
	 * @return
	 */
	@Override
	default PK gtcAccessSelective(T t) {
		this.insertSelective(t);
		return t.id();
	}

}
