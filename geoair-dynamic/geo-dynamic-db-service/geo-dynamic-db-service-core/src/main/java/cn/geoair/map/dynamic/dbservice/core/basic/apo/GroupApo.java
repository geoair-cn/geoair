package cn.geoair.map.dynamic.dbservice.core.basic.apo;

import lombok.Data;

import java.io.Serializable;

@Data
public class GroupApo implements Serializable {

	String id;

	String name;

	String createUserId;

	String createUserName;

	String createTime;

	String updateTime;

}
