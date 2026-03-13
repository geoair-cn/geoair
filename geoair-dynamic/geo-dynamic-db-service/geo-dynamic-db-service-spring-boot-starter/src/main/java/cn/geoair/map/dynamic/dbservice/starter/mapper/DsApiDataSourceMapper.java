package cn.geoair.map.dynamic.dbservice.starter.mapper;

import cn.geoair.map.dynamic.dbservice.core.basic.apo.DataSourceApo;
import cn.geoair.map.dynamic.dbservice.core.dao.GirDsDataSourceDao;
import cn.geoair.map.dynamic.dbservice.starter.model.dto.DsApiDataSourceDto;
import cn.geoair.map.dynamic.dbservice.starter.model.entity.DsApiDataSourcePo;
import cn.geoair.orm.tkmapper.impls.TkEntityMapper;

import java.util.List;

/**
 * 数据源信息Mapper接口
 *
 * @author zhangjun
 * @date 2025-07-31
 */
public interface DsApiDataSourceMapper extends TkEntityMapper<DsApiDataSourcePo, String>, GirDsDataSourceDao {

	default void accessSelective(DataSourceApo t) {
		DsApiDataSourcePo po = DsApiDataSourceDto.toPo(t);
		gtcAccessSelective(po);
	}

	default void updateSelectiveById(DataSourceApo t) {
		DsApiDataSourcePo po = DsApiDataSourceDto.toPo(t);
		gtcUpdateByPKSelective(po);
	}

	default void deleteByPK(String id) {
		gtcDeleteByPK(id);
	}

	default DataSourceApo getById(String id) {
		DsApiDataSourcePo po = gtcSearchByPK(id);
		return DsApiDataSourceDto.fromPo(po);
	}

	default List<DataSourceApo> searchAll() {
		DsApiDataSourcePo po = new DsApiDataSourcePo();
		List<DsApiDataSourcePo> dsApiDataSourcePos = gtcSearchAll();
		return DsApiDataSourceDto.fromPos(dsApiDataSourcePos);
	}

	default List<DataSourceApo> selectBatchIds(List<String> ids) {
		List<DsApiDataSourcePo> dsApiDataSourcePos = gtcSearchByPK(ids);
		return DsApiDataSourceDto.fromPos(dsApiDataSourcePos);
	}

}
