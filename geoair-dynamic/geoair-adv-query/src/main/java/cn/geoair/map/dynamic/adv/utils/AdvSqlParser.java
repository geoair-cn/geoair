package cn.geoair.map.dynamic.adv.utils;

import cn.geoair.base.Gir;
import cn.hutool.core.lang.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/9 15:08 @description： SQL解析工具类
 */
public class AdvSqlParser {

	// 定义带标志的正则表达式模式
	private static final Pattern SELECT_PATTERN = Pattern.compile("^select\\s+", Pattern.CASE_INSENSITIVE);

	private static final Pattern BLOCK_COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

	private static final Pattern LINE_COMMENT_PATTERN = Pattern.compile("--.*?$", Pattern.MULTILINE);

	/**
	 * 解析SQL获取表名和字段信息
	 * @param sql 要解析的SQL语句
	 * @return SqlParseResult对象，包含表名和字段列表
	 */
	public static SqlParseResult parse(String sql) {
		SqlParseResult result = new SqlParseResult();

		if (sql == null || sql.trim().isEmpty()) {
			return result;
		}

		// 预处理SQL：去除注释、多余空格
		String processedSql = preprocessSql(sql);

		// 检查是否为SELECT语句（大小写不敏感）
		if (!SELECT_PATTERN.matcher(processedSql).find()) {
			return result;
		}

		// 提取表名
		Pair<String, String> stringStringPair = parseTableName(processedSql);

		result.setTableName(stringStringPair.getValue());
		result.setSchema(stringStringPair.getKey());

		// 提取字段列表
		List<String> fields = parseFields(processedSql);
		result.setFields(fields);

		return result;
	}

	/** 从处理后的SQL中提取表名，支持带引号和别名的表名 */
	private static Pair<String, String> parseTableName(String processedSql) {

		Pattern pattern = Pattern.compile("from\\s+((" + "\"[^\"]+\"|'[^']+'|`[^`]+`|[a-zA-Z0-9_\\u4e00-\\u9fa5]+)" // Schema部分（支持中文）
				+ "\\.)?" // 可选点号
				+ "(" + "\"[^\"]+\"|'[^']+'|`[^`]+`|[a-zA-Z0-9_\\u4e00-\\u9fa5]+" // 表名部分（支持中文）
				+ ")" + "\\s*[a-zA-Z0-9_]*\\s*" // 可选表别名
				+ "(?=where|join|on|limit|group|order|having|$)", // 补充 having 关键字
				Pattern.CASE_INSENSITIVE);

		Matcher matcher = pattern.matcher(processedSql);

		if (matcher.find()) {
			// 处理schema部分
			String schemaPart = matcher.group(2);
			// 处理表名部分
			String tablePart = matcher.group(3);

			// 去除可能的引号
			if (schemaPart != null) {
				schemaPart = schemaPart.replaceAll("^[\"'`]|[\"'`]$", "");
			}
			tablePart = tablePart.replaceAll("^[\"'`]|[\"'`]$", "");

			// 组合schema和表名
			// String fullTableName = (schemaPart != null ? schemaPart + "." : "") +
			// tablePart;
			return Pair.of(schemaPart, tablePart);
		}

		return Pair.of(null, null);
	}

	private static List<String> parseFields(String processedSql) {
		List<String> fields = new ArrayList<>();

		// 提取SELECT和FROM之间的字段部分（不变）
		Pattern selectPattern = Pattern.compile("select\\s+(.*?)\\s+from", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

		Matcher selectMatcher = selectPattern.matcher(processedSql);
		if (!selectMatcher.find()) {
			return fields;
		}

		String fieldsPart = selectMatcher.group(1).trim();
		String[] fieldArray = fieldsPart.split(","); // 分割字段

		for (String field : fieldArray) {
			field = field.trim();
			if (field.isEmpty()) {
				continue;
			}

			// 处理DISTINCT关键字（不变）
			if (field.equalsIgnoreCase("DISTINCT")) {
				continue;
			}

			// 关键修改1：优化别名正则，支持中文别名（添加\\u4e00-\\u9fa5）
			Pattern aliasPattern = Pattern.compile(
					"\\s+as\\s+(\"[^\"]+\"|'[^']+'|`[^`]+`|[a-zA-Z0-9_\\u4e00-\\u9fa5]+)$|\\s+(\"[^\"]+\"|'[^']+'|`[^`]+`|[a-zA-Z0-9_\\u4e00-\\u9fa5]+)$",
					Pattern.CASE_INSENSITIVE);

			Matcher aliasMatcher = aliasPattern.matcher(field);
			if (aliasMatcher.find()) {
				// 提取别名并去引号（逻辑不变，支持中文别名）
				String alias = aliasMatcher.group(1) != null ? aliasMatcher.group(1) : aliasMatcher.group(2);
				if (alias != null) {
					alias = alias.replaceAll("^[\"'`]|[\"'`]$", "");
					fields.add(alias);
				}
			}
			else {
				// 无别名时提取字段名（支持中文）
				String processedField = field.replaceAll("^[\"'`]|[\"'`]$", "");
				String[] parts = processedField.split("\\.");
				String fieldName = parts[parts.length - 1].trim();

				// 关键修改2：简化函数处理（保留完整函数表达式，而非截断为 xxx()）
				// 若需保留原始函数名（如 ST_Transform(geom, 4326)），直接用 fieldName；
				// 若仅需提取函数名（如 ST_Transform），可保留原逻辑，否则注释以下代码
				if (fieldName.contains("(")) {
					int openParenIndex = fieldName.indexOf("(");
					int closeParenIndex = fieldName.lastIndexOf(")");
					if (closeParenIndex > openParenIndex) {
						// 可选：保留完整函数表达式（推荐）
						fieldName = fieldName.substring(0, closeParenIndex + 1);
						// 若需仅保留函数名，用：fieldName = fieldName.substring(0, openParenIndex);
					}
				}

				fields.add(fieldName);
			}
		}

		return fields;
	}

	/** 预处理SQL：去除注释、多余空格等 */
	private static String preprocessSql(String sql) {
		// 先编译带标志的正则表达式，再进行替换
		String processed = BLOCK_COMMENT_PATTERN.matcher(sql).replaceAll(" ");
		processed = LINE_COMMENT_PATTERN.matcher(processed).replaceAll(" ");
		// 替换多个空格为单个空格
		processed = processed.replaceAll("\\s+", " ");
		// 去除首尾空格
		return processed.trim();
	}

	/** 解析结果封装类 */
	public static class SqlParseResult {

		private String tableName;

		private String schema;

		private List<String> fields;

		public String getSchema() {
			return schema;
		}

		public void setSchema(String schema) {
			this.schema = schema;
		}

		public SqlParseResult() {
			this.fields = new ArrayList<>();
		}

		// getter和setter方法
		public String getTableName() {
			return tableName;
		}

		public void setTableName(String tableName) {
			this.tableName = tableName;
		}

		public List<String> getFields() {
			return fields;
		}

		public void setFields(List<String> fields) {
			this.fields = fields;
		}

		@Override
		public String toString() {
			return "schema: " + schema + "表名: " + tableName + "\n字段列表: " + fields;
		}

	}

	// 测试方法
	public static void main(String[] args) {
		String[] testSqls = { "select * from \"geo_poi_list11\" where id = '1497145264652292111' limit 1", // 双引号表名
				"select * from 'geo_poi_list11' where id = '1497145264652292111'", // 单引号表名
				"select * from `geo_poi_list11` as gpl where id = '1497145264652292111'", // 反引号表名带别名
				"select a.id, b.name AS \"user_name\" from \"schema\".\"table\" b", // 带引号的表名和字段别名
				"select * from public.geo_poi_list11 where id = '1497145264652292111' limit 1",
				"SELECT id, name FROM user WHERE status = 1 ORDER BY create_time DESC",
				"select user_id as uid, user_name name FROM t_user LIMIT 10 OFFSET 5",
				"SELECT a.id, b.name AS username FROM schema.table b WHERE b.age > 18 GROUP BY a.id",
				"SELECT DISTINCT p.product_id AS pid FROM product p WHERE price > 100",
				"select a.id, b.name AS username from schema.table b", // 带表别名的情况
				"select * from public.geo_poi_list11 where id = '1497145264652292111' limit 1",
				"select * from public.geo_poi_list11 where id = '1497145264652292111' limit 1",
				"SELECT id, name FROM user WHERE status = 1", "SeLeCt user_id as uid, user_name name FrOm t_user",
				"select a.id, b.name AS username from schema.table b",
				"SELECT DISTINCT p.product_id AS pid From product p",
				"select * from public.geo_poi_list11 where id = '1497145264652292111' limit 1",
				"SELECT id, name FROM user WHERE status = 1", "SeLeCt user_id as uid, user_name name FrOm t_user",
				"select a.id, b.name AS username from schema.table b",
				"SELECT /* 这是一个注释 */ DISTINCT p.product_id AS pid From product p",
				"SELECT id, name FROM user u WHERE status = 1", // 带表别名
				"SeLeCt user_id as uid, user_name name FrOm t_user tu", // 带表别名
				"SELECT a.id, b.name AS username from schema.table b",
				"select id, 地址 as \"用户地址\", ST_Transform(geom, 4326) from test1.\"TC_ADDRESS_武汉_测试坐标转换\" where id=1",
				"select `用户姓名`, age from `public`.`学生表_测试` as t limit 10",
				"select a.编号, b.姓名 from \"测试Schema\".\"班级表_武汉\" b having count>5",
				"select `id` as '用户ID', name from `public`.`geo_poi_list11`" // 带引号的字段和表名
		};

		for (String sql : testSqls) {
			SqlParseResult result = parse(sql);
			Gir.log.info("SQL: " + sql);
			Gir.log.info(result + "");
			Gir.log.info("------------------------");
		}
	}

}
