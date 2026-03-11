package cn.geoair.map.dynamic.dbservice.starter.mapper;

import cn.geoair.map.dynamic.dbservice.core.basic.apo.GroupApo;
import cn.geoair.map.dynamic.dbservice.core.dao.GirDsApiGroupDao;
import cn.geoair.map.dynamic.dbservice.starter.model.dto.DsApiGroupDto;
import cn.geoair.map.dynamic.dbservice.starter.model.entity.DsApiGroupPo;
import cn.geoair.orm.tkmapper.impls.TkEntityMapper;

import java.util.List;

/**
 * api分组信息Mapper接口
 *
 * @author zhangjun
 * @date 2025-07-31
 */
public interface DsApiApiGroupMapper
        extends TkEntityMapper<DsApiGroupPo, String>, GirDsApiGroupDao {

    default List<GroupApo> searchAll() {
        return DsApiGroupDto.fromPos(gtcSearchAll());
    }

    default List<GroupApo> selectBatchIds(List<String> ids) {
        return DsApiGroupDto.fromPos(gtcSearchByPK(ids));
    }

    default GroupApo accessSelective(GroupApo t) {
        DsApiGroupPo po = DsApiGroupDto.toPo(t);
        gtcAccessSelective(po);
        return DsApiGroupDto.fromPo(po);
    }

    default GroupApo updateSelectiveById(GroupApo t) {
        DsApiGroupPo po = DsApiGroupDto.toPo(t);
        gtcUpdateByPKSelective(po);
        return DsApiGroupDto.fromPo(po);
    }

    default void deleteByPK(String id) {
        gtcDeleteByPK(id);
    }
}
