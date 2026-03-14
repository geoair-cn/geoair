package cn.geoair.comp.db.service.starter.model.dto;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.comp.db.service.core.basic.apo.GroupApo;
import cn.geoair.comp.db.service.starter.model.entity.DsApiGroupPo;
import cn.hutool.core.bean.BeanUtil;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * api分组信息(DbapiGroup)DTO
 *
 * @author zhangjun
 * @date 2025-07-31
 */
@GaModel(text = "api分组信息DTO")
public class DsApiGroupDto extends DsApiGroupPo {

	private static final long serialVersionUID = 1753953245128L;

	public static DsApiGroupDto empty() {
		return new DsApiGroupDto();
	}

	public DsApiGroupDto copy() {
		DsApiGroupDto copy = new DsApiGroupDto();
		BeanUtil.copyProperties(this, copy);
		return copy;
	}

	public static DsApiGroupPo toPo(GroupApo groupApo) {
		if (groupApo == null) {
			return null;
		}
		DsApiGroupPo thisPo = new DsApiGroupPo();
		BeanUtils.copyProperties(groupApo, thisPo);
		return thisPo;
	}

	public static GroupApo fromPo(DsApiGroupPo po) {
		if (po == null) {
			return null;
		}
		GroupApo thisVo = new GroupApo();
		BeanUtils.copyProperties(po, thisVo);
		return thisVo;
	}

	public static List<GroupApo> fromPos(List<DsApiGroupPo> pos) {
		if (pos == null) {
			return new ArrayList<>();
		}
		List<GroupApo> list = new ArrayList<>();
		for (DsApiGroupPo po : pos) {
			GroupApo thisVo = fromPo(po);
			list.add(thisVo);
		}
		return list;
	}

}
