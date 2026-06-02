package cn.geoair.comp.db.service.starter.byadv;

import cn.geoair.comp.db.service.core.basic.apo.GroupApo;
import cn.geoair.comp.db.service.core.dao.GirDsApiGroupDao;
import cn.geoair.comp.db.service.starter.model.dto.DsApiGroupDto;
import cn.geoair.comp.db.service.starter.model.entity.DsApiGroupPo;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
import cn.geoair.map.dynamic.adv.spring.GirSpringAdvExecutor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * api分组信息的具体实现
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@Component
public class DsApiApiGroupMapper implements GirDsApiGroupDao {

    public List<GroupApo> searchAll() {
        List<GirAdvOneRow> girAdvOneRows = GirSpringAdvExecutor.getInstance().wSelectList(GirAdvQueryRequest.builder(DsApiGroupPo.class).build());
        return GirAdvOneRow.toBeanObjList(girAdvOneRows, GroupApo.class);
    }

    public List<GroupApo> selectBatchIds(List<String> ids) {
        List<GirAdvOneRow> girAdvOneRows = GirSpringAdvExecutor.getInstance().wSelectList(
                GirAdvQueryRequest.builder(DsApiGroupPo.class)
                        .whereLambda(w -> w.in(DsApiGroupPo::getId, ids))
                        .build());
        return GirAdvOneRow.toBeanObjList(girAdvOneRows, GroupApo.class);
    }

    public GroupApo accessSelective(GroupApo t) {
        DsApiGroupPo po = DsApiGroupDto.toPo(t);
        GirSpringAdvExecutor.getInstance().bInsertSelectiveIgnore(po);
        return DsApiGroupDto.fromPo(po);
    }

    public GroupApo updateSelectiveById(GroupApo t) {
        DsApiGroupPo po = DsApiGroupDto.toPo(t);
        GirSpringAdvExecutor.getInstance().bUpdateByPK(po);
        return DsApiGroupDto.fromPo(po);
    }

    public void deleteByPK(String id) {
        DsApiGroupPo po = new DsApiGroupPo();
        po.setId(id);
        GirSpringAdvExecutor.getInstance().bDeleteByPK(po);
    }
}
