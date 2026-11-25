package cn.geoair.comp.db.service.starter.byadv;

import cn.geoair.comp.db.service.core.basic.apo.DsDataSourceApo;
import cn.geoair.comp.db.service.core.dao.GirDsDataSourceDao;
import cn.geoair.comp.db.service.starter.model.dto.DsApiDataSourceDto;
import cn.geoair.comp.db.service.starter.model.entity.DsApiDataSourcePo;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
import cn.geoair.map.dynamic.adv.spring.GirSpringAdvExecutor;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据源信息的具体实现
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@Component
public class DsApiDataSourceMapper implements GirDsDataSourceDao {

    @Override
    public void accessSelective(DsDataSourceApo t) {
        DsApiDataSourcePo po = DsApiDataSourceDto.toPo(t);
        GirSpringAdvExecutor.getInstance().bInsertSelectiveIgnore(po);
    }

    @Override
    public void updateSelectiveById(DsDataSourceApo t) {
        DsApiDataSourcePo po = DsApiDataSourceDto.toPo(t);
        GirSpringAdvExecutor.getInstance().bUpdateByPKSelective(po);
    }

    @Override
    public void deleteByPK(String id) {
        DsApiDataSourcePo po = new DsApiDataSourceDto();
        po.setId(id);
        GirSpringAdvExecutor.getInstance().bDeleteByPK(po);
    }

    @Override
    public DsDataSourceApo getById(String id) {
        List<GirAdvOneRow> girAdvOneRows =
                GirSpringAdvExecutor.getInstance()
                        .wSelectList(
                                GirAdvQueryRequest.builder(DsApiDataSourcePo.class)
                                        .whereLambda(w -> w.eq(DsApiDataSourcePo::getId, id))
                                        .build());
        List<DsApiDataSourcePo> pos =
                GirAdvOneRow.toBeanObjList(girAdvOneRows, DsApiDataSourcePo.class);
        return pos.isEmpty() ? null : DsApiDataSourceDto.fromPo(pos.get(0));
    }

    @Override
    public List<DsDataSourceApo> searchAll() {
        List<GirAdvOneRow> girAdvOneRows =
                GirSpringAdvExecutor.getInstance()
                        .wSelectList(GirAdvQueryRequest.builder(DsApiDataSourcePo.class).build());
        List<DsApiDataSourcePo> pos =
                GirAdvOneRow.toBeanObjList(girAdvOneRows, DsApiDataSourcePo.class);
        return DsApiDataSourceDto.fromPos(pos);
    }

    @Override
    public List<DsDataSourceApo> selectBatchIds(List<String> ids) {
        List<GirAdvOneRow> girAdvOneRows =
                GirSpringAdvExecutor.getInstance()
                        .wSelectList(
                                GirAdvQueryRequest.builder(DsApiDataSourcePo.class)
                                        .whereLambda(w -> w.in(DsApiDataSourcePo::getId, ids))
                                        .build());
        List<DsApiDataSourcePo> pos =
                GirAdvOneRow.toBeanObjList(girAdvOneRows, DsApiDataSourcePo.class);
        return DsApiDataSourceDto.fromPos(pos);
    }
}
