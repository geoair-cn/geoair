package cn.geoair.map.dynamic.dbservice.core.model.dbapi.dto;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.map.dynamic.dbservice.core.model.dbapi.entity.DbApiDataSourcePo;
import cn.hutool.core.bean.BeanUtil;

/**
 * 数据源信息(DbapiDatasource)DTO
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@GaModel(text = "数据源信息DTO")
public class DbApiDataSourceDto extends DbApiDataSourcePo {

    private static final long serialVersionUID = 1753953250860L;

    public static DbApiDataSourceDto empty() {
        return new DbApiDataSourceDto();
    }

    public DbApiDataSourceDto copy() {
        DbApiDataSourceDto copy = new DbApiDataSourceDto();
        BeanUtil.copyProperties(this, copy);
        return copy;
    }

    public static DbApiDataSourceDto ofDbapiDatasourcePo(DbApiDataSourcePo source) {
        DbApiDataSourceDto target = new DbApiDataSourceDto();
        BeanUtil.copyProperties(source, target);
        return target;
    }

    public static DbApiDataSourcePo toPo(DbApiDataSourceDto source) {
        DbApiDataSourcePo target = new DbApiDataSourcePo();
        BeanUtil.copyProperties(source, target);
        return target;
    }
}
