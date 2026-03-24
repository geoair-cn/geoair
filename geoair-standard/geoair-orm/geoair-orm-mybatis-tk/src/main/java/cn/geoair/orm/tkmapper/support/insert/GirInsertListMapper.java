package cn.geoair.orm.tkmapper.support.insert;

import java.util.List;

import org.apache.ibatis.annotations.InsertProvider;

import tk.mybatis.mapper.annotation.RegisterMapper;

/**
 * 批量插入
 *
 * @param <T> 不能为空
 */
@RegisterMapper
public interface GirInsertListMapper<T> {

	/**
	 * 批量插入，支持批量插入的数据库可以使用，例如MySQL,PG,H2等
	 * <p>
	 * 不支持主键策略，插入前需要设置好主键的值
	 * <p>
	 * @param recordList
	 * @return
	 */
	@InsertProvider(type = GirInsertListProvider.class, method = "dynamicSQL")
	int insertList(List<? extends T> recordList);

}
