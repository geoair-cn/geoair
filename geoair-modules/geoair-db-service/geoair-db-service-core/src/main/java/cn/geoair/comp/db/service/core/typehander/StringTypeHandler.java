package cn.geoair.comp.db.service.core.typehander;

import cn.geoair.base.exception.GirException;
import cn.hutool.core.map.MapUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.meta.JdbcType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:46 @description： string类型的处理器
 */
public class StringTypeHandler extends BaseTypeHandler<String> {

	@Override
	public String getNonNullParameter(Object parameter, JdbcType jdbcType) {
		return String.valueOf(parameter);
	}

	@Override
	public String getResult(Entity entity, String columnName) {
		return entity.getStr(columnName);
	}

	@Override
	public String getResult(ResultSet resultSet, String columnName) {
		try {
			return resultSet.getString(columnName);
		}
		catch (SQLException throwables) {
			throw new GirException("无法找到字段名称为《{}》的字段", columnName);
		}
	}

	@Override
	public String getResult(ResultSet resultSet, Integer columnIndex) {
		try {
			return resultSet.getString(columnIndex);
		}
		catch (SQLException throwables) {
			throw new GirException("无法找到字段序号为《{}》的字段", columnIndex);
		}
	}

	@Override
	public String getResult(Map<String, Object> row, String columnName) {
		return MapUtil.getStr(row, columnName);
	}

	@Override
	public String getResult(Object obj) {
		return String.valueOf(obj);
	}

}
