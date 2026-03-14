package cn.geoair.orm.spi;

/**
 * @author ：zhangjun
 * @date ：Created in 2022/6/30 15:14
 * @description： TODO
 */

import cn.geoair.orm.spi.entity.GirEntityTable;

/**
 * 解析实体类接口
 *
 * @author liuzh
 */
public interface GirEntityResolve {

	/**
	 * 解析类为 EntityTable
	 * @param entityClass
	 * @param
	 * @return
	 */
	GirEntityTable resolveEntity(Class<?> entityClass);

	/**
	 * 解析类为 EntityTable
	 * @param entityidentification 实体类标识 用于拓展
	 * @param
	 * @return
	 */
	GirEntityTable resolveEntity(Object entityidentification);

}
