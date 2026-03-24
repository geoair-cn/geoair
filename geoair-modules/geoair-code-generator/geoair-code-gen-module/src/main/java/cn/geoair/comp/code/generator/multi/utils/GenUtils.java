package cn.geoair.comp.code.generator.multi.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import cn.geoair.base.Gir;
import cn.geoair.comp.code.generator.multi.config.GirGeneratorConfig;
import cn.geoair.comp.code.generator.multi.domian.GenTable;
import cn.geoair.comp.code.generator.multi.domian.GenTableColumn;

public class GenUtils {

	/**
	 * 初始化表信息（移除静态依赖，改为传入配置）
	 */
	public static void initTable(GenTable genTable, GirGeneratorConfig config) {
		genTable.setClassName(convertClassName(genTable.getTableName(), config));
		genTable.setPackageName(config.getSourceRootPackage());
		genTable.setModuleName(config.getModuleName());
		genTable.setBusinessName(getBusinessName(genTable.getTableName()));
		genTable.setFunctionName(genTable.getTableComment());
		genTable.setFunctionAuthor(config.getAuthor());
	}

	/**
	 * 初始化列属性字段（修复空指针）
	 */
	public static void initColumnField(GenTableColumn column, GenTable table) {
		if (column == null || StringUtils.isEmpty(column.getColumnName())) {
			return;
		}
		// 处理枚举注释
		String columnComment = column.getColumnComment();
		if (StringUtils.isNotEmpty(columnComment) && columnComment.contains("#em=")) {
			String[] emms = columnComment.split("#em=");
			String comm1 = emms[1];
			if (comm1.contains(";")) {
				String[] en = comm1.split(";");
				Map<String, String> map = new HashMap<>();
				for (String e : en) {
					if (e.contains(":")) {
						String[] ls = e.split(":", 2); // 限制分割次数，避免值包含冒号
						map.put(ls[0], ls[1]);
					}
				}
				column.setEnums(map);
				column.setEnumsName(convertClassName(table.getTableName(), null)
						+ convertClassName(column.getColumnName(), null) + "Enum");
			}
			else {
				column.setEnumsName(comm1);
			}
			column.setColumnComment(emms[0]);
		}

		// 设置java字段名
		column.setJavaField(StringUtils.toCamelCase(column.getColumnName()));

		// 处理数据库类型转换
		String dataType = column.getColumnType();
		if (StringUtils.isEmpty(dataType)) {
			return;
		}

		if (arraysContains(GenConstants.COLUMNTYPE_TIME, dataType)) {
			column.setJavaType(GenConstants.TYPE_DATE);
		}
		else if (arraysContains(GenConstants.COLUMNTYPE_NUMBER, dataType)) {
			handleNumberType(column, dataType);
		}
		if (dataType.equals("geometry")) {
			column.setJavaType(GenConstants.TYPE_Geometry);
		}
		if (dataType.equals("geography")) {
			column.setJavaType(GenConstants.TYPE_Geometry);
		}
		if (dataType.equals("uuid")) {
			column.setJavaType(GenConstants.TYPE_STRING);
		}
		Gir.log.info(column.getJavaField() + ": 转换后" + column.getJavaType() + ": 数据库类型 " + column.getColumnType());
	}

	/**
	 * 处理数字类型转换
	 */
	private static void handleNumberType(GenTableColumn column, String dataType) {
		String columnType = column.getColumnType();
		GenConstants.convertDbTypeToJavaType(columnType, column);
	}

	/**
	 * 校验数组是否包含指定值
	 */
	public static boolean arraysContains(String[] arr, String targetValue) {
		return arr != null && Arrays.asList(arr).contains(targetValue);
	}

	public static String getBusinessName(String tableName) {
		return StringUtils.convertToCamelCase(tableName);
	}

	/**
	 * 表名转换成Java类名（修复数组越界）
	 */
	public static String convertClassName(String tableName, GirGeneratorConfig config) {
		if (StringUtils.isEmpty(tableName)) {
			return "";
		}
		// 移除表前缀
		if (config != null && config.isRemovePre() && StringUtils.isNotEmpty(config.getTablePrefix())) {
			String[] searchList = StringUtils.split(config.getTablePrefix(), ",");
			tableName = replaceFirst(tableName, searchList);
		}
		return StringUtils.convertToCamelCase(tableName);
	}

	/**
	 * 批量替换前缀（修复替换逻辑）
	 */
	public static String replaceFirst(String text, String[] searchList) {
		if (StringUtils.isEmpty(text) || searchList == null || searchList.length == 0) {
			return text;
		}
		// 按前缀长度降序排序，优先替换长前缀
		Arrays.sort(searchList, (a, b) -> b.length() - a.length());
		for (String searchString : searchList) {
			if (StringUtils.isNotEmpty(searchString) && text.startsWith(searchString)) {
				return text.substring(searchString.length());
			}
		}
		return text;
	}

}
