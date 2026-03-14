package cn.geoair.map.dynamic.dbservice.core.basic.apo;

import lombok.Data;

import java.io.Serializable;

/**
 * @program: dbApi
 * @description:
 * @author: 武汉刘德华
 * @create: 2021-01-20 09:52
 */
@Data
public class DataSourceApo implements Serializable {

	String id;

	String name;

	String note;

	String url;

	String username;

	String password;

	/** true 修改密码 false不修改 */
	boolean edit_password;

	String type;

	String driver;

	String tableSql;

	String createUserId;

	String createUserName;

	String createTime;

	String updateTime;

}
