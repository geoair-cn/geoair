package cn.geoair.map.dynamic.dbservice.core.model.dbapi.dto;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.entity.DbApiGroupPo;
import cn.hutool.core.bean.BeanUtil;

/**
 * api分组信息(DbapiGroup)DTO
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@GaModel(text = "api分组信息DTO")
public class DbApiGroupDto extends DbApiGroupPo {

    private static final long serialVersionUID = 1753953245128L;

    public static DbApiGroupDto empty() {
        return new DbApiGroupDto();
    }

    public DbApiGroupDto copy() {
        DbApiGroupDto copy = new DbApiGroupDto();
        BeanUtil.copyProperties(this, copy);
        return copy;
    }

    public static DbApiGroupDto ofDbapiGroupPo(DbApiGroupPo source) {
        DbApiGroupDto target = new DbApiGroupDto();
        BeanUtil.copyProperties(source, target);
        return target;
    }

    public static DbApiGroupPo toPo(DbApiGroupDto source) {
        DbApiGroupPo target = new DbApiGroupPo();
        BeanUtil.copyProperties(source, target);
        return target;
    }
}
