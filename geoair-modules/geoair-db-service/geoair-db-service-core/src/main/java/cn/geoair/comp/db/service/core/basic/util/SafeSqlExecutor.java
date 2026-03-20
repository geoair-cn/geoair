package cn.geoair.comp.db.service.core.basic.util;

import cn.geoair.comp.db.service.core.typehander.TypeHandler;
import cn.geoair.comp.db.service.core.typehander.TypeHandlerRegistry;
import cn.hutool.core.util.StrUtil;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/** SQL安全执行工具类 功能：拦截危险SQL操作（新增/删除/修改/清空表/删除库等），仅允许查询操作 */
public class SafeSqlExecutor {

	private static final Logger log = LoggerFactory.getLogger(SafeSqlExecutor.class);

	// 危险SQL模式正则表达式（不区分大小写）
	private static final Pattern DANGEROUS_SQL_PATTERN = Pattern.compile(
			// 匹配DELETE/UPDATE/INSERT/TRUNCATE/DROP等危险操作
			"^\\s*(DELETE|UPDATE|INSERT|TRUNCATE|DROP|ALTER|CREATE|RENAME|COMMIT|ROLLBACK|MERGE)\\s+",
			Pattern.CASE_INSENSITIVE);

	// 特殊危险操作（清空表）
	private static final Pattern TRUNCATE_PATTERN = Pattern.compile("^\\s*TRUNCATE\\s+TABLE\\s+",
			Pattern.CASE_INSENSITIVE);

	// 删除数据库/表
	private static final Pattern DROP_PATTERN = Pattern.compile("^\\s*DROP\\s+(DATABASE|SCHEMA|TABLE|VIEW|INDEX)\\s+",
			Pattern.CASE_INSENSITIVE);

	/**
	 * 安全执行SQL（仅允许查询操作）
	 * @param connection 数据库连接（需调用方关闭）
	 * @param sql SQL语句
	 * @param jdbcParamValues 参数列表
	 * @param humpIs 是否转换为驼峰命名
	 * @return 执行结果（仅查询结果）
	 * @throws SecurityException 当执行危险操作时抛出
	 * @throws SQLException 数据库访问异常
	 */
	public static Object executeSafeSql(Connection connection, String sql, List<Object> jdbcParamValues, boolean humpIs)
			throws SQLException, SecurityException {
		// 参数校验
		if (StrUtil.isBlank(sql)) {
			throw new IllegalArgumentException("SQL语句不能为空");
		}
		if (jdbcParamValues == null) {
			jdbcParamValues = new ArrayList<>();
		}

		// 日志打印
		log.debug("待执行SQL: {}", sql);
		log.debug("SQL参数: {}", JSON.toJSONString(jdbcParamValues));

		// 危险SQL检测
		if (isDangerousSql(sql)) {
			String errorMsg = "拒绝执行危险SQL操作: " + sql;
			log.error(errorMsg);
			throw new SecurityException(errorMsg);
		}

		PreparedStatement statement = null;
		ResultSet rs = null;
		try {
			// 预编译SQL
			statement = connection.prepareStatement(sql);

			// 参数注入
			for (int i = 1; i <= jdbcParamValues.size(); i++) {
				statement.setObject(i, jdbcParamValues.get(i - 1));
			}

			// 执行SQL
			boolean hasResultSet = statement.execute();

			// 处理查询结果
			if (hasResultSet) {
				rs = statement.getResultSet();
				return handleResultSet(rs, humpIs);
			}
			else {
				// 非查询操作（理论上不会走到这里，因为危险操作已被拦截）
				log.warn("检测到非查询操作，已拦截: {}", sql);
				throw new SecurityException("不允许执行非查询操作");
			}
		}
		catch (SQLException e) {
			log.error("SQL执行异常: {}", e.getMessage(), e);
			throw e; // 抛出异常由调用方处理
		}
		finally {
			// 关闭结果集和语句（连接由调用方关闭）
			closeResultSet(rs);
			closeStatement(statement);
		}
	}

	/** 检测是否为危险SQL */
	private static boolean isDangerousSql(String sql) {
		// 去除注释（简单处理）
		String cleanSql = removeComments(sql);
		// 检测危险模式
		return DANGEROUS_SQL_PATTERN.matcher(cleanSql).find() || TRUNCATE_PATTERN.matcher(cleanSql).find()
				|| DROP_PATTERN.matcher(cleanSql).find();
	}

	/** 移除SQL中的注释（简单处理） */
	private static String removeComments(String sql) {
		// 移除/* */注释
		String noBlockComments = sql.replaceAll("/\\*.*?\\*/", " ");
		// 移除-- 注释
		return noBlockComments.replaceAll("--.*?$", " ");
	}

	/** 处理查询结果集 */
	private static List<JSONObject> handleResultSet(ResultSet rs, boolean humpIs) throws SQLException {
		ResultSetMetaData metaData = rs.getMetaData();
		int columnCount = metaData.getColumnCount();

		// 获取列名列表
		List<String> columns = new ArrayList<>();
		for (int i = 1; i <= columnCount; i++) {
			columns.add(metaData.getColumnLabel(i));
		}

		// 处理结果集
		List<JSONObject> resultList = new ArrayList<>();
		while (rs.next()) {
			JSONObject row = new JSONObject();
			for (String column : columns) {
				TypeHandler typeHandlerByJavaType = TypeHandlerRegistry.getTypeHandlerByJavaType(rs.getObject(column));
				Object value = typeHandlerByJavaType.getResult(rs, column);
				// 处理日期类型
				if (value instanceof Date) {
					value = formatDate((Date) value);
				}
				// 处理驼峰命名转换
				String key = humpIs ? StrUtil.toCamelCase(column) : column;
				row.put(key, value);
			}
			resultList.add(row);
		}
		return resultList;
	}

	/** 日期格式化 */
	private static String formatDate(Date date) {
		// 可根据需要修改日期格式
		return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
	}

	/** 关闭结果集 */
	private static void closeResultSet(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			}
			catch (SQLException e) {
				log.error("关闭ResultSet失败", e);
			}
		}
	}

	/** 关闭语句 */
	private static void closeStatement(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			}
			catch (SQLException e) {
				log.error("关闭Statement失败", e);
			}
		}
	}

	/** 批量执行安全检查（用于批量操作场景） */
	public static void checkBatchSqlSafety(List<String> sqlList) throws SecurityException {
		for (String sql : sqlList) {
			if (isDangerousSql(sql)) {
				String errorMsg = "批量操作中包含危险SQL: " + sql;
				log.error(errorMsg);
				throw new SecurityException(errorMsg);
			}
		}
	}

}
