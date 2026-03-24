package cn.geoair.comp.db.service.core.typehander;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import cn.geoair.base.exception.GirException;
import cn.geoair.base.util.GutilObject;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.convert.Convert;
import cn.hutool.db.Entity;
import cn.hutool.db.meta.JdbcType;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:46 @description：byte类型的处理器
 */
public class ByteTypeHandler extends BaseTypeHandler<String> {

	@Override
	public String getNonNullParameter(Object parameter, JdbcType jdbcType) {
		if (GutilObject.isEmpty(parameter)) {
			return null;
		}
		Class<?> aClass = parameter.getClass();
		if (aClass == byte[].class) {
			return Base64.encode(Convert.toPrimitiveByteArray(parameter));
		}
		else if (aClass == Byte[].class) {
			return Base64.encode(Convert.toPrimitiveByteArray(parameter));
		}
		return null;
	}

	@Override
	public String getResult(Entity entity, String columnName) {
		Object object = entity.get(columnName);
		return getNonNullParameter(object, null);
	}

	@Override
	public String getResult(ResultSet resultSet, String columnName) {
		try {
			Object object = resultSet.getObject(columnName);
			return getNonNullParameter(object, null);
		}
		catch (SQLException throwables) {
			throw new GirException("无法找到字段名称为《{}》的字段", columnName);
		}
	}

	@Override
	public String getResult(ResultSet resultSet, Integer columnIndex) {
		try {
			Object object = resultSet.getObject(columnIndex);
			return getNonNullParameter(object, null);
		}
		catch (SQLException throwables) {
			throw new GirException("无法找到字段序号为《{}》的字段", columnIndex);
		}
	}

	@Override
	public String getResult(Map<String, Object> row, String columnName) {
		Object object = row.get(columnName);
		return getNonNullParameter(object, null);

	}

	@Override
	public String getResult(Object obj) {

		return getNonNullParameter(obj, null);
	}

}
