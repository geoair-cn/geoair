package cn.geoair.comp.code.generator.multi.utils;

import cn.geoair.comp.code.generator.multi.domian.GenTableColumn;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 代码生成通用常量
 *
 * @author ray
 */
public class GenConstants {

	/**
	 * 数据库时间类型
	 */
	public static final String[] COLUMNTYPE_TIME = { "datetime", "time", "date", "timestamp", "timestamptz" };

	/**
	 * 数据库数字类型
	 */
	public static final String[] COLUMNTYPE_NUMBER = { "tinyint", "numeric", "smallint", "mediumint", "int", "number",
			"integer", "int4", "int8", "bit", "bigint", "float", "double", "serial", "bigserial", "decimal", "float8",
			"float4" };

	/**
	 * 字符串类型
	 */
	public static final String TYPE_STRING = "String";

	/**
	 * 整型
	 */
	public static final String TYPE_INTEGER = "Integer";

	/**
	 * 整型
	 */
	public static final String TYPE_BYTE = "byte[]";

	/**
	 * 长整型
	 */
	public static final String TYPE_LONG = "Long";

	/**
	 * 浮点型
	 */
	public static final String TYPE_DOUBLE = "Double";

	/**
	 * 高精度计算类型
	 */
	public static final String TYPE_BIGDECIMAL = "BigDecimal";

	public static final String TYPE_Geometry = "Geometry";

	/**
	 * 时间类型
	 */
	public static final String TYPE_DATE = "Date";

	private static final Map<String, String> DB_TYPE_TO_JAVA_TYPE = new HashMap<>();

	static {
		// 高精度小数
		DB_TYPE_TO_JAVA_TYPE.put("decimal", GenConstants.TYPE_BIGDECIMAL);
		// 浮点/双精度
		DB_TYPE_TO_JAVA_TYPE.put("numeric", GenConstants.TYPE_BIGDECIMAL);
		DB_TYPE_TO_JAVA_TYPE.put("number", GenConstants.TYPE_DOUBLE);
		DB_TYPE_TO_JAVA_TYPE.put("double", GenConstants.TYPE_DOUBLE);
		DB_TYPE_TO_JAVA_TYPE.put("float8", GenConstants.TYPE_DOUBLE);
		DB_TYPE_TO_JAVA_TYPE.put("float4", GenConstants.TYPE_DOUBLE);
		// 长整型
		DB_TYPE_TO_JAVA_TYPE.put("int8", GenConstants.TYPE_LONG);

		DB_TYPE_TO_JAVA_TYPE.put("tinyint", GenConstants.TYPE_BYTE);
		DB_TYPE_TO_JAVA_TYPE.put("smallint", GenConstants.TYPE_INTEGER);
		DB_TYPE_TO_JAVA_TYPE.put("mediumint", GenConstants.TYPE_INTEGER);
		DB_TYPE_TO_JAVA_TYPE.put("int", GenConstants.TYPE_INTEGER);
		DB_TYPE_TO_JAVA_TYPE.put("integer", GenConstants.TYPE_INTEGER);
		DB_TYPE_TO_JAVA_TYPE.put("int4", GenConstants.TYPE_INTEGER);
		DB_TYPE_TO_JAVA_TYPE.put("bit", GenConstants.TYPE_BYTE);
		DB_TYPE_TO_JAVA_TYPE.put("bigint", GenConstants.TYPE_LONG);
		DB_TYPE_TO_JAVA_TYPE.put("float", GenConstants.TYPE_DOUBLE);
		DB_TYPE_TO_JAVA_TYPE.put("serial", GenConstants.TYPE_INTEGER);
		DB_TYPE_TO_JAVA_TYPE.put("bigserial", GenConstants.TYPE_LONG);
	}

	/**
	 * 转换数据库列类型为 Java 类型
	 * @param columnType 数据库列类型（如 decimal、int8 等）
	 * @param column 待设置类型的列对象
	 */
	public static void convertDbTypeToJavaType(String columnType, GenTableColumn column) {
		// 空值校验（避免空指针）
		if (columnType == null || column == null) {
			return;
		}
		// 统一转为小写（兼容数据库类型大小写不一致的情况）
		String lowerColumnType = columnType.trim().toLowerCase();

		// 优先从映射表获取 Java 类型
		String javaType = DB_TYPE_TO_JAVA_TYPE.get(lowerColumnType);
		if (javaType != null) {
			column.setJavaType(javaType);
			return;
		}

		// 兜底处理：未匹配到的数字类型默认设为 BigDecimal（防止类型丢失）
		if (Arrays.asList(COLUMNTYPE_NUMBER).contains(lowerColumnType)) {
			column.setJavaType(GenConstants.TYPE_BIGDECIMAL);
		}
	}

}
