package cn.geoair.comp.db.service.starter.byadv;

import cn.geoair.base.util.GutilStr;
import cn.geoair.comp.db.service.core.basic.apo.ApiConfigApo;
import cn.geoair.comp.db.service.core.dao.GirDsApiConfigDao;
import cn.geoair.comp.db.service.starter.model.dto.DsApiConfigDto;
import cn.geoair.comp.db.service.starter.model.entity.DsApiConfigPo;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
import cn.geoair.map.dynamic.adv.query.wherequery.queryr.QueryRequestBuilder;
import cn.geoair.map.dynamic.adv.spring.GirSpringAdvExecutor;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * api配置信息的具体实现
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@Component
public class DsApiConfigMapper implements GirDsApiConfigDao {

    @Override
    public List<ApiConfigApo> selectBatchIds(List<String> ids) {
        List<GirAdvOneRow> girAdvOneRows =
                GirSpringAdvExecutor.getInstance()
                        .wSelectList(
                                GirAdvQueryRequest.builder(DsApiConfigPo.class)
                                        .whereLambda(w -> w.in(DsApiConfigPo::getId, ids))
                                        .build());
        List<DsApiConfigPo> pos = GirAdvOneRow.toBeanObjList(girAdvOneRows, DsApiConfigPo.class);
        return DsApiConfigDto.fromPos(pos);
    }

    @Override
    public ApiConfigApo selectByPathOnline(String path) {
        List<GirAdvOneRow> girAdvOneRows =
                GirSpringAdvExecutor.getInstance()
                        .wSelectList(
                                GirAdvQueryRequest.builder(DsApiConfigPo.class)
                                        .whereLambda(
                                                w ->
                                                        w.eq(DsApiConfigPo::getPath, path)
                                                                .eq(DsApiConfigPo::getStatus, 1))
                                        .build());
        List<DsApiConfigPo> pos = GirAdvOneRow.toBeanObjList(girAdvOneRows, DsApiConfigPo.class);
        return pos.isEmpty() ? null : DsApiConfigDto.fromPo(pos.get(0));
    }

    @Override
    public List<ApiConfigApo> search(String name, String note, String path, String groupId) {
        QueryRequestBuilder<DsApiConfigPo> builder =
                GirAdvQueryRequest.builder(DsApiConfigPo.class);

        if (GutilStr.isNotEmpty(path)) {
            builder.whereLambda(w -> w.like(DsApiConfigPo::getPath, path));
        }
        if (GutilStr.isNotEmpty(name)) {
            builder.whereLambda(w -> w.like(DsApiConfigPo::getName, name));
        }
        if (GutilStr.isNotEmpty(note)) {
            builder.whereLambda(w -> w.like(DsApiConfigPo::getNote, note));
        }
        if (GutilStr.isNotEmpty(groupId)) {
            builder.whereLambda(w -> w.like(DsApiConfigPo::getGroupId, groupId));
        }

        List<GirAdvOneRow> girAdvOneRows =
                GirSpringAdvExecutor.getInstance().wSelectList(builder.build());
        List<DsApiConfigPo> pos = GirAdvOneRow.toBeanObjList(girAdvOneRows, DsApiConfigPo.class);
        return DsApiConfigDto.fromPos(pos);
    }

    @Override
    public Integer selectCountByPath(String path) {
        List<GirAdvOneRow> girAdvOneRows =
                GirSpringAdvExecutor.getInstance()
                        .wSelectList(
                                GirAdvQueryRequest.builder(DsApiConfigPo.class)
                                        .whereLambda(w -> w.eq(DsApiConfigPo::getPath, path))
                                        .build());
        return girAdvOneRows.size();
    }

    @Override
    public Integer selectCountByPathWhenUpdate(String path, String id) {
        List<GirAdvOneRow> girAdvOneRows =
                GirSpringAdvExecutor.getInstance()
                        .wSelectList(
                                GirAdvQueryRequest.builder(DsApiConfigPo.class)
                                        .whereLambda(w -> w.eq(DsApiConfigPo::getPath, path))
                                        .build());
        List<DsApiConfigPo> pos = GirAdvOneRow.toBeanObjList(girAdvOneRows, DsApiConfigPo.class);

        long count = pos.stream().filter(po -> !id.equals(po.getId())).count();
        return (int) count;
    }

    @Override
    public int selectCountByGroup(String id) {
        List<GirAdvOneRow> girAdvOneRows =
                GirSpringAdvExecutor.getInstance()
                        .wSelectList(
                                GirAdvQueryRequest.builder(DsApiConfigPo.class)
                                        .whereLambda(w -> w.eq(DsApiConfigPo::getGroupId, id))
                                        .build());
        return girAdvOneRows.size();
    }

    @Override
    public List<ApiConfigApo> selectByGroup(String groupId) {
        List<GirAdvOneRow> girAdvOneRows =
                GirSpringAdvExecutor.getInstance()
                        .wSelectList(
                                GirAdvQueryRequest.builder(DsApiConfigPo.class)
                                        .whereLambda(w -> w.eq(DsApiConfigPo::getGroupId, groupId))
                                        .build());
        List<DsApiConfigPo> pos = GirAdvOneRow.toBeanObjList(girAdvOneRows, DsApiConfigPo.class);
        return DsApiConfigDto.fromPos(pos);
    }

    @Override
    public List<ApiConfigApo> searchAll() {
        List<GirAdvOneRow> girAdvOneRows =
                GirSpringAdvExecutor.getInstance()
                        .wSelectList(GirAdvQueryRequest.builder(DsApiConfigPo.class).build());
        List<DsApiConfigPo> pos = GirAdvOneRow.toBeanObjList(girAdvOneRows, DsApiConfigPo.class);
        return DsApiConfigDto.fromPos(pos);
    }

    @Override
    public void accessSelective(ApiConfigApo t) {
        DsApiConfigPo po = DsApiConfigDto.toPo(t);
        GirSpringAdvExecutor.getInstance().bInsertSelectiveIgnore(po);
    }

    @Override
    public void updateSelectiveById(ApiConfigApo t) {
        DsApiConfigPo po = DsApiConfigDto.toPo(t);
        GirSpringAdvExecutor.getInstance().bUpdateByPKSelective(po);
    }

    @Override
    public void deleteById(String id) {
        DsApiConfigPo po = new DsApiConfigPo();
        po.setId(id);
        GirSpringAdvExecutor.getInstance().bDeleteByPK(po);
    }

    @Override
    public ApiConfigApo getById(String id) {
        List<GirAdvOneRow> girAdvOneRows =
                GirSpringAdvExecutor.getInstance()
                        .wSelectList(
                                GirAdvQueryRequest.builder(DsApiConfigPo.class)
                                        .whereLambda(w -> w.eq(DsApiConfigPo::getId, id))
                                        .build());
        List<DsApiConfigPo> pos = GirAdvOneRow.toBeanObjList(girAdvOneRows, DsApiConfigPo.class);
        return pos.isEmpty() ? null : DsApiConfigDto.fromPo(pos.get(0));
    }
}
