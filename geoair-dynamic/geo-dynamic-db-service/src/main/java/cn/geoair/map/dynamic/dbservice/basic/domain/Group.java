package cn.geoair.map.dynamic.dbservice.basic.domain;

import cn.geoair.map.dynamic.dbservice.model.dbapi.dto.DbApiGroupDto;
import cn.geoair.map.dynamic.dbservice.model.dbapi.entity.DbApiGroupPo;

import lombok.Data;

import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class Group implements Serializable {

	public DbApiGroupPo toPo() {
		DbApiGroupPo thisPo = new DbApiGroupPo();
		BeanUtils.copyProperties(this, thisPo);
		return thisPo;
	}

	public static Group fromPo(DbApiGroupPo po) {
		Group thisVo = new Group();
		BeanUtils.copyProperties(po, thisVo);
		return thisVo;
	}

	public static List<Group> fromPos(List<DbApiGroupPo> pos) {
		List<Group> list = new ArrayList<>();
		for (DbApiGroupPo po : pos) {
			Group thisVo = fromPo(po);
			list.add(thisVo);
		}
		return list;
	}

	public static Group fromDto(DbApiGroupDto dto) {
		return fromPo(dto);
	}

	public static List<Group> fromDtos(List<DbApiGroupDto> dtos) {
		List<Group> list = new ArrayList<>();
		for (DbApiGroupDto dto : dtos) {
			Group thisVo = fromDto(dto);
			list.add(thisVo);
		}
		return list;
	}

	String id;

	String name;

	String createUserId;

	String createTime;

	String updateTime;

}
