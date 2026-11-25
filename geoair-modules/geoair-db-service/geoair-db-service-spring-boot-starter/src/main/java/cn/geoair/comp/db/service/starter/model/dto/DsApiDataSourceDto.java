package cn.geoair.comp.db.service.starter.model.dto;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.comp.db.service.core.basic.apo.DsDataSourceApo;
import cn.geoair.comp.db.service.starter.model.entity.DsApiDataSourcePo;
import cn.hutool.core.bean.BeanUtil;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据源信息(DbapiDatasource)DTO
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@GaModel(text = "数据源信息DTO")
public class DsApiDataSourceDto extends DsApiDataSourcePo {

    private static final long serialVersionUID = 1753953250860L;

    public static DsApiDataSourceDto empty() {
        return new DsApiDataSourceDto();
    }

    public DsApiDataSourceDto copy() {
        DsApiDataSourceDto copy = new DsApiDataSourceDto();
        BeanUtil.copyProperties(this, copy);
        return copy;
    }

    public static DsApiDataSourcePo toPo(DsDataSourceApo source) {
        if (source == null) {
            return null;
        }
        DsApiDataSourcePo thisPo = new DsApiDataSourcePo();
        BeanUtils.copyProperties(source, thisPo);
        return thisPo;
    }

    public static DsDataSourceApo fromPo(DsApiDataSourcePo po) {
        if (po == null) {
            return null;
        }
        DsDataSourceApo thisVo = new DsDataSourceApo();
        BeanUtils.copyProperties(po, thisVo);
        return thisVo;
    }

    public static List<DsDataSourceApo> fromPos(List<DsApiDataSourcePo> pos) {
        if (pos == null) {
            return new ArrayList<>();
        }
        List<DsDataSourceApo> list = new ArrayList<>();
        for (DsApiDataSourcePo po : pos) {
            DsDataSourceApo thisVo = fromPo(po);
            list.add(thisVo);
        }
        return list;
    }
}
