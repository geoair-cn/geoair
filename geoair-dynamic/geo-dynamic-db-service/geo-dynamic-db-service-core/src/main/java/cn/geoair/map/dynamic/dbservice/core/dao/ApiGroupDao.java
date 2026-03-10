package cn.geoair.map.dynamic.dbservice.core.dao;

import cn.geoair.map.dynamic.dbservice.core.basic.apo.GroupApo;

import java.util.List;

/**
 * api分组信息Dao接口
 *
 * @author zhangjun
 * @date 2025-07-31
 */
public interface ApiGroupDao {

	List<GroupApo> searchAll();

	List<GroupApo> selectBatchIds(List<String> ids);

	GroupApo accessSelective(GroupApo t);

	GroupApo updateSelectiveById(GroupApo t);

	GroupApo deleteByPK(String id);

}
