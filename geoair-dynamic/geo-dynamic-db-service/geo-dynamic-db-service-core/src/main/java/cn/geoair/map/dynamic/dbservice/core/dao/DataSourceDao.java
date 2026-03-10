package cn.geoair.map.dynamic.dbservice.core.dao;

import cn.geoair.map.dynamic.dbservice.core.basic.apo.DataSourceApo;

import java.util.List;

/**
 * 数据源信息Dao接口
 *
 * @author zhangjun
 * @date 2025-07-31
 */
public interface DataSourceDao {

	void accessSelective(DataSourceApo t);

	void updateSelectiveById(DataSourceApo t);

	void deleteByPK(String id);

	DataSourceApo getById(String id);

	List<DataSourceApo> searchAll();

	List<DataSourceApo> selectBatchIds(List<String> ids);

}
