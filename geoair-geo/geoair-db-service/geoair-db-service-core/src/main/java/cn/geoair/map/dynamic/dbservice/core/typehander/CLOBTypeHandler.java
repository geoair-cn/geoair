package cn.geoair.map.dynamic.dbservice.core.typehander;

import cn.hutool.db.Entity;
import cn.hutool.db.meta.JdbcType;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:46 @description： 字节字段解析
 * 内置的LOB数据类型包括BLOB、CLOB、NCLOB、BFILE（外部存储）的大型化和非结构化数据，如文本、图像、视屏、空间数据存储。BLOB、CLOB、NCLOB类型
 * <p>
 * 4.1 CLOB 数据类型
 * <p>
 * 它存储单字节和多字节字符数据。支持固定宽度和可变宽度的字符集。CLOB对象可以存储最多 (4 gigabytes-1) * (database block size)
 * 大小的字符
 * <p>
 * 4.2 NCLOB 数据类型
 * <p>
 * 它存储UNICODE类型的数据，支持固定宽度和可变宽度的字符集，NCLOB对象可以存储最多(4 gigabytes-1) * (database block
 * size)大小的文本数据。
 * <p>
 * 4.3 BLOB 数据类型
 * <p>
 * 它存储非结构化的二进制数据大对象，它可以被认为是没有字符集语义的比特流，一般是图像、声音、视频等文件。BLOB对象最多存储(4 gigabytes-1) *
 * (database block size)的二进制数据。
 * <p>
 * 4.4 BFILE 数据类型
 * <p>
 * 二进制文件，存储在数据库外的系统文件，只读的，数据库会将该文件当二进制文件处理
 */
public class CLOBTypeHandler extends BaseTypeHandler<String> {

	@Override
	public String getNonNullParameter(Object parameter, JdbcType jdbcType) {
		return null;
	}

	@Override
	public String getResult(Entity entity, String columnName) {
		Object obj = entity.getObj(columnName);
		return clobToStrings(obj);
	}

	@Override
	public String getResult(ResultSet resultSet, String columnName) {
		Object obj = null;
		try {
			obj = resultSet.getObject(columnName);
		}
		catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		return clobToStrings(obj);
	}

	@Override
	public String getResult(ResultSet resultSet, Integer columnIndex) {
		Object obj = null;
		try {
			obj = resultSet.getObject(columnIndex);
		}
		catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		return clobToStrings(obj);
	}

	@Override
	public String getResult(Map<String, Object> row, String columnName) {
		Object obj = null;
		obj = row.get(columnName);
		return clobToStrings(obj);
	}

	@Override
	public String getResult(Object obj) {
		return clobToStrings(obj);
	}

	private String toString(Clob clob) throws SQLException {
		return clob == null ? null : clob.getSubString(1, (int) clob.length());
	}

	public String clobToStrings(Object value) {
		try {
			if (value instanceof Clob) {
				return ((Clob) value).getSubString(1, (int) ((Clob) value).length());
			}
			return String.valueOf(value);
		}
		catch (Exception e) {
			return "(Clob)";
		}
	}

}
