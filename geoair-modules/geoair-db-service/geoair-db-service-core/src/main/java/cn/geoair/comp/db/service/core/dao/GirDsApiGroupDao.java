package cn.geoair.comp.db.service.core.dao;

import java.util.List;

import cn.geoair.comp.db.service.core.basic.apo.GroupApo;

/**
 * api分组信息Dao接口
 *
 * @author zhangjun
 * @date 2025-07-31
 */
public interface GirDsApiGroupDao {

	List<GroupApo> searchAll();

	List<GroupApo> selectBatchIds(List<String> ids);

	GroupApo accessSelective(GroupApo t);

	GroupApo updateSelectiveById(GroupApo t);

	void deleteByPK(String id);

}
