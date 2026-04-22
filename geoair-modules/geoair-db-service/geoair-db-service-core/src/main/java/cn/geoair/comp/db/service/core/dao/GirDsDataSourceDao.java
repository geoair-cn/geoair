package cn.geoair.comp.db.service.core.dao;

import cn.geoair.comp.db.service.core.basic.apo.DsDataSourceApo;

import java.util.List;

/**
 * 数据源信息Dao接口
 *
 * @author zhangjun
 * @date 2025-07-31
 */
public interface GirDsDataSourceDao {

    void accessSelective(DsDataSourceApo t);

    void updateSelectiveById(DsDataSourceApo t);

    void deleteByPK(String id);

    DsDataSourceApo getById(String id);

    List<DsDataSourceApo> searchAll();

    List<DsDataSourceApo> selectBatchIds(List<String> ids);
}
