package cn.geoair.comp.db.service.starter.mapper;

import cn.geoair.base.util.GutilStr;
import cn.geoair.comp.db.service.core.basic.apo.ApiConfigApo;
import cn.geoair.comp.db.service.core.dao.GirDsApiConfigDao;
import cn.geoair.comp.db.service.starter.model.dto.DsApiConfigDto;
import cn.geoair.comp.db.service.starter.model.entity.DsApiConfigPo;
import cn.geoair.orm.tkmapper.impls.TkEntityMapper;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * api配置信息Mapper接口
 *
 * @author zhangjun
 * @date 2025-07-31
 */
public interface DsApiConfigMapper extends TkEntityMapper<DsApiConfigPo, String>, GirDsApiConfigDao {

    default List<ApiConfigApo> selectBatchIds(List<String> ids) {
        List<DsApiConfigPo> dsApiConfigPos = gtcSearchByPK(ids);
        return DsApiConfigDto.fromPos(dsApiConfigPos);
    }

    default ApiConfigApo selectByPathOnline(String path) {
        DsApiConfigPo dsApiConfigPo = new DsApiConfigPo();
        dsApiConfigPo.setPath(path);
        dsApiConfigPo.setStatus(1);
        dsApiConfigPo = gtcSearchOne(dsApiConfigPo);
        return DsApiConfigDto.fromPo(dsApiConfigPo);
    }

    default List<ApiConfigApo> search(String name, String note, String path, String groupId) {
        DsApiConfigPo dsApiConfigPo = new DsApiConfigPo();
        Example example = new Example(DsApiConfigPo.class);
        if (GutilStr.isNotEmpty(path)) {
            example.and().andLike("path", path);
        }
        if (GutilStr.isNotEmpty(name)) {
            example.and().andLike("name", name);
        }
        if (GutilStr.isNotEmpty(note)) {
            example.and().andLike("note", note);
        }
        if (GutilStr.isNotEmpty(groupId)) {
            example.and().andLike("groupId", groupId);
        }
        List<DsApiConfigPo> dsApiConfigPos = selectByExample(example);
        return DsApiConfigDto.fromPos(dsApiConfigPos);
    }

    default Integer selectCountByPath(String path) {
        DsApiConfigPo dsApiConfigPo = new DsApiConfigPo();
        dsApiConfigPo.setPath(path);
        return Math.toIntExact(gtcSearchCount(dsApiConfigPo));
    }

    default Integer selectCountByPathWhenUpdate(String path, String id) {
        DsApiConfigPo dsApiConfigPo = new DsApiConfigPo();
        dsApiConfigPo.setPath(path);
        List<DsApiConfigPo> dsApiConfigPos = gtcSearch(dsApiConfigPo);
        int count = 0;
        for (DsApiConfigPo apiConfigPo : dsApiConfigPos) {
            String id1 = apiConfigPo.getId();
            if (!id.equals(id1)) {
                count++;
            }
        }
        // select count(1) from dbapi_config where path = #{path} and id != #{id}
        return count;
    }

    default int selectCountByGroup(String id) {
        DsApiConfigPo dsApiConfigPo = new DsApiConfigPo();
        dsApiConfigPo.setGroupId(id);
        return Math.toIntExact(gtcSearchCount(dsApiConfigPo));
    }

    default List<ApiConfigApo> selectByGroup(String groupId) {
        DsApiConfigPo dsApiConfigPo = new DsApiConfigPo();
        dsApiConfigPo.setGroupId(groupId);
        List<DsApiConfigPo> dsApiConfigPos = gtcSearch(dsApiConfigPo);
        return DsApiConfigDto.fromPos(dsApiConfigPos);
    }

    default List<ApiConfigApo> searchAll() {
        List<DsApiConfigPo> dsApiConfigPos = gtcSearchAll();
        return DsApiConfigDto.fromPos(dsApiConfigPos);
    }

    default void accessSelective(ApiConfigApo t) {
        DsApiConfigPo dsApiConfigPo = DsApiConfigDto.toPo(t);
        gtcAccessSelective(dsApiConfigPo);
    }

    default void updateSelectiveById(ApiConfigApo t) {
        DsApiConfigPo dsApiConfigPo = DsApiConfigDto.toPo(t);
        gtcUpdateByPKSelective(dsApiConfigPo);
    }

    default void deleteById(String id) {
        gtcDeleteByPK(id);
    }

    default ApiConfigApo getById(String id) {
        DsApiConfigPo dsApiConfigPo = gtcSearchByPK(id);
        return DsApiConfigDto.fromPo(dsApiConfigPo);
    }

}
