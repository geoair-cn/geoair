package cn.geoair.comp.db.service.starter.model.dto;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.comp.db.service.core.basic.apo.ApiConfigApo;
import cn.geoair.comp.db.service.starter.model.entity.DsApiConfigPo;
import cn.hutool.core.bean.BeanUtil;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * api配置信息(DbapiConfig)DTO
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@GaModel(text = "api配置信息DTO")
public class DsApiConfigDto extends DsApiConfigPo {

	private static final long serialVersionUID = 1753953255411L;

	public static DsApiConfigDto empty() {
		return new DsApiConfigDto();
	}

	public DsApiConfigDto copy() {
		DsApiConfigDto copy = new DsApiConfigDto();
		BeanUtil.copyProperties(this, copy);
		return copy;
	}

	public static DsApiConfigPo toPo(ApiConfigApo apiConfigApo) {
		if (apiConfigApo == null) {
			return null;
		}
		DsApiConfigPo thisPo = new DsApiConfigPo();
		BeanUtils.copyProperties(apiConfigApo, thisPo);
		return thisPo;
	}

	public static ApiConfigApo fromPo(DsApiConfigPo po) {
		if (po == null) {
			return null;
		}
		ApiConfigApo thisVo = new ApiConfigApo();
		BeanUtils.copyProperties(po, thisVo);
		return thisVo;
	}

	public static List<ApiConfigApo> fromPos(List<DsApiConfigPo> pos) {
		if (pos == null) {
			return new ArrayList<>();
		}
		List<ApiConfigApo> list = new ArrayList<>();
		for (DsApiConfigPo po : pos) {
			ApiConfigApo thisVo = fromPo(po);
			list.add(thisVo);
		}
		return list;
	}

}
