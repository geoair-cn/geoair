package cn.geoair.map.dynamic.dbservice.model.dbapi.dto;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.map.dynamic.dbservice.model.dbapi.entity.DbApiConfigPo;
import cn.hutool.core.bean.BeanUtil;

/**
 * api配置信息(DbapiConfig)DTO
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@GaModel(text = "api配置信息DTO")
public class DbApiConfigDto extends DbApiConfigPo {

	private static final long serialVersionUID = 1753953255411L;

	public static DbApiConfigDto empty() {
		return new DbApiConfigDto();
	}

	public DbApiConfigDto copy() {
		DbApiConfigDto copy = new DbApiConfigDto();
		BeanUtil.copyProperties(this, copy);
		return copy;
	}

	public static DbApiConfigDto ofDbapiConfigPo(DbApiConfigPo source) {
		DbApiConfigDto target = new DbApiConfigDto();
		BeanUtil.copyProperties(source, target);
		return target;
	}

	public static DbApiConfigPo toPo(DbApiConfigDto source) {
		DbApiConfigPo target = new DbApiConfigPo();
		BeanUtil.copyProperties(source, target);
		return target;
	}

}
