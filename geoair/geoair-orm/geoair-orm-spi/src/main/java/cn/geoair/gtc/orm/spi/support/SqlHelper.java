package cn.geoair.gtc.orm.spi.support;

/**
 * @author ：张俊
 * @date ：Created in 2022/6/30 19:00
 * @description： 拼常用SQL的工具类
 */

import cn.geoair.gtc.base.data.tuples.GkPair;
import cn.geoair.gtc.base.util.GutilStr;
import cn.geoair.gtc.orm.spi.entity.GirEntityColumn;
//import javafx.util.GkPair;

import java.util.*;

public class SqlHelper {

	/**
	 * 获取所有查询列，如id,name,code...
	 * @param entityClass
	 * @return
	 */
	public static String getAllColumns(Class<?> entityClass) {
		Set<GirEntityColumn> columnSet = GirEntityHelper.getColumns(entityClass);
		StringBuilder sql = new StringBuilder();
		for (GirEntityColumn girEntityColumn : columnSet) {
			sql.append(girEntityColumn.getSelectColumn()).append(",");
		}
		return sql.substring(0, sql.length() - 1);
	}

	/**
	 * select xxx,xxx...
	 * @param entityClass
	 * @return
	 */
	public static String selectAllColumns(Class<?> entityClass) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ");
		sql.append(getAllColumns(entityClass));
		sql.append(" ");
		return sql.toString();
	}

	/**
	 * select count(x)
	 * @param entityClass
	 * @return
	 */
	public static String selectCount(Class<?> entityClass) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ");
		Set<GirEntityColumn> pkColumns = GirEntityHelper.getPKColumns(entityClass);
		if (pkColumns.size() == 1) {
			sql.append("COUNT(").append(pkColumns.iterator().next().getColumn()).append(") ");
		}
		else {
			sql.append("COUNT(*) ");
		}
		return sql.toString();
	}

	/**
	 * select case when count(x) > 0 then 1 else 0 end
	 * @param entityClass
	 * @return
	 */
	public static String selectCountExists(Class<?> entityClass) {
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT CASE WHEN ");
		Set<GirEntityColumn> pkColumns = GirEntityHelper.getPKColumns(entityClass);
		if (pkColumns.size() == 1) {
			sql.append("COUNT(").append(pkColumns.iterator().next().getColumn()).append(") ");
		}
		else {
			sql.append("COUNT(*) ");
		}
		sql.append(" > 0 THEN 1 ELSE 0 END AS result ");
		return sql.toString();
	}

	/**
	 * from tableName - 动态表名
	 * @param girExample
	 * @param defaultTableName
	 * @return
	 */
	public static String fromTable(GirExample girExample, String defaultTableName) {
		StringBuilder sql = new StringBuilder();
		sql.append(" FROM ");
		sql.append(defaultTableName);
		sql.append(" ");
		return sql.toString();
	}

	/**
	 * update tableName - 动态表名
	 * @param girExample
	 * @param defaultTableName 默认表名
	 * @return
	 */
	public static String updateTable(GirExample girExample, String defaultTableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE ");
		sql.append(girExample.tableName);
		sql.append(" ");
		return sql.toString();
	}

	public static String deleteFromTable(GirExample girExample, String defaultTableName) {
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE FROM ");
		sql.append(girExample.tableName);
		sql.append(" ");
		return sql.toString();
	}

	/**
	 * 获取默认的orderBy，通过注解设置的
	 * @param entityClass
	 * @return
	 */
	public static String orderByDefault(Class<?> entityClass) {
		StringBuilder sql = new StringBuilder();
		String orderByClause = GirEntityHelper.getOrderByClause(entityClass);
		if (orderByClause.length() > 0) {
			sql.append(" ORDER BY ");
			sql.append(orderByClause);
		}
		return sql.toString();
	}

	/**
	 * example支持查询指定列时
	 * @return
	 */
	public static String exampleSelectColumns(GirExample girExample) {
		StringBuilder sql = new StringBuilder();

		if (girExample.getSelectColumns() != null && girExample.getSelectColumns().size() > 0) {
			sql.append(getSelectColumn(null, girExample.getSelectColumns()));
		}
		else {
			// 不支持指定列的时候查询全部列
			sql.append(getSelectColumn(girExample, null));

		}
		return sql.toString();
	}

	public static String getSelectColumn(GirExample girExample, Set<String> selectColumns) {
		StringBuilder allColumn = new StringBuilder();
		if (girExample != null) {
			Set<GirEntityColumn> entityClassColumns = girExample.table.getEntityClassColumns();
			for (GirEntityColumn entityClassColumn : entityClassColumns) {
				allColumn.append(entityClassColumn.getSelectColumn()).append(",");
			}
			return allColumn.substring(0, allColumn.length() - 1);
		}
		if (selectColumns != null) {
			for (String column : selectColumns) {
				allColumn.append(" ").append(column).append(", ");
			}
			allColumn.deleteCharAt(allColumn.length());
			allColumn.deleteCharAt(allColumn.length());
			return allColumn.toString();
		}
		return null;
	}

	/**
	 * example支持查询指定列时
	 * @return
	 */
	public static String exampleCountColumn(GirExample girExample, List<Object> contidion) {
		StringBuilder sql = new StringBuilder();

		if (GutilStr.isNotEmpty(girExample.getCountColumn())) {
			if (girExample.distinct) {
				sql.append(" distinct ");
			}
			sql.append(girExample.countColumn);
		}
		else {
			sql.append("COUNT(*)");
		}

		return sql.toString();
	}

	/**
	 * example查询中的orderBy条件，会判断默认orderBy
	 * @return
	 */
	public static String exampleOrderBy(Class<?> entityClass) {
		StringBuilder sql = new StringBuilder();
		sql.append("<if test=\"orderByClause != null\">");
		sql.append("order by ${orderByClause}");
		sql.append("</if>");
		String orderByClause = GirEntityHelper.getOrderByClause(entityClass);
		if (orderByClause.length() > 0) {
			sql.append("<if test=\"orderByClause == null\">");
			sql.append("ORDER BY " + orderByClause);
			sql.append("</if>");
		}
		return sql.toString();
	}

	public static String exampleOrderBy(GirExample girExample) {
		StringBuilder sql = new StringBuilder();
		if (girExample.orderByClause != null) {
			sql.append("order by ");
			sql.append(girExample.orderByClause);
		}
		return sql.toString();
	}

	/**
	 * example 支持 for update
	 * @return
	 */
	public static String exampleForUpdate(GirExample girExample) {
		StringBuilder sql = new StringBuilder();
		if (girExample.isForUpdate()) {
			sql.append("FOR UPDATE");
		}

		return sql.toString();
	}

	/**
	 * Example查询中的where结构，用于只有一个Example参数时
	 * @param girExample
	 * @param contidion 查询条件
	 * @return
	 */
	public static GkPair<String, List<Object>> exampleWhereClause(GirExample girExample, List<Object> contidion) {
		GkPair<String, List<Object>> returnpair = null;
		StringBuilder sql = new StringBuilder();
		if (girExample == null) {
			returnpair = new GkPair<>(sql.toString(), contidion);
			return returnpair;
		}
		List<String> prefixOverrides = new ArrayList<>();
		prefixOverrides.add("and");
		prefixOverrides.add("or");
		// 对应条件中的 子sql
		if (girExample.oredCriteria.size() > 0) {
			sql.append(" where ");
		}

		StringBuilder whereSql = new StringBuilder(); //// and ( and abc = 1 and bcd = 2 )
														//// and ( and abc = 1 and bcd = 2
														//// )

		for (GirExample.Criteria oredCriterion : girExample.oredCriteria) {
			StringBuilder oredCriterionSql = new StringBuilder(); // and ( and abc = 1 and
																	// bcd = 2 )
			// and ( and abc = 1 and bcd = 2 )
			if (oredCriterion.isValid()) {
				oredCriterionSql.append(oredCriterion.getAndOr());
				// 子查询条件拼接
				StringBuilder criterionSql = new StringBuilder(); //// 循环走完 and abc = 1
																	//// and bcd = 2
				for (GirExample.Criterion criterion : oredCriterion.criteria) {

					if (criterion.isNoValue()) {
						criterionSql.append(criterion.getAndOr()).append(" ");
						criterionSql.append(criterion.getCondition()).append(" ");
					}

					if (criterion.isSingleValue()) {
						criterionSql.append(criterion.getAndOr()).append(" ");
						criterionSql.append(criterion.getCondition());
						criterionSql.append(" ? "); // 预编译占位符
						contidion.add(criterion.getValue());
					}
					if (criterion.isBetweenValue()) {
						criterionSql.append(criterion.getAndOr()).append(" ");
						;
						criterionSql.append(criterion.getCondition());
						criterionSql.append("( ? ");
						criterionSql.append(" and ");
						criterionSql.append(" ? )");
						contidion.add(criterion.getValue());
						contidion.add(criterion.getSecondValue());
					}
					if (criterion.isListValue()) {
						criterionSql.append(criterion.getAndOr()).append(" ").append(criterion.getCondition());
						StringBuilder listValueSql = new StringBuilder();
						Collection<?> value = (Collection<?>) criterion.getValue();
						listValueSql.append(" ( ");
						for (Object o : value) {
							listValueSql.append(" ?,");
							contidion.add(o);
						}
						String substring = listValueSql.substring(0, listValueSql.length() - 1);
						criterionSql.append(substring).append(" )");
					}

				}

				applyPrefix(criterionSql, criterionSql.toString().toUpperCase(Locale.ENGLISH), prefixOverrides, "(");
				applySuffix(criterionSql, criterionSql.toString().toUpperCase(Locale.ENGLISH), null, " ) ");
				oredCriterionSql.append(criterionSql);
			}
			whereSql.append(oredCriterionSql);
		}
		String trimmedUppercaseSql = whereSql.toString().toUpperCase(Locale.ENGLISH);
		if (trimmedUppercaseSql.length() > 0) {
			applyPrefix(whereSql, trimmedUppercaseSql, prefixOverrides, null);

		}
		sql.append(whereSql);
		returnpair = new GkPair<>(sql.toString(), contidion);
		return returnpair;
	}

	public static GkPair<String, List<Object>> updateByExampleWhereClause(GirExample girExample,
			List<Object> contidion) {
		GkPair<String, List<Object>> returnpair = null;
		StringBuilder sql = new StringBuilder();
		if (girExample == null) {
			returnpair = new GkPair<>(sql.toString(), contidion);
			return returnpair;
		}
		List<String> prefixOverrides = new ArrayList<>();
		prefixOverrides.add("and");
		prefixOverrides.add("or");
		// 对应条件中的 子sql
		if (girExample.oredCriteria.size() > 0) {
			sql.append(" where ");
		}

		StringBuilder whereSql = new StringBuilder(); //// and ( and abc = 1 and bcd = 2 )
														//// and ( and abc = 1 and bcd = 2
														//// )

		for (GirExample.Criteria oredCriterion : girExample.oredCriteria) {
			StringBuilder oredCriterionSql = new StringBuilder(); // and ( and abc = 1 and
																	// bcd = 2 )
			// and ( and abc = 1 and bcd = 2 )
			if (oredCriterion.isValid()) {
				oredCriterionSql.append(oredCriterion.getAndOr());
				// 子查询条件拼接
				StringBuilder criterionSql = new StringBuilder(); //// 循环走完 and abc = 1
																	//// and bcd = 2
				for (GirExample.Criterion criterion : oredCriterion.criteria) {

					if (criterion.isNoValue()) {
						criterionSql.append(" ").append(criterion.getAndOr()).append(" ");
						criterionSql.append(criterion.getCondition()).append(" ");
					}

					if (criterion.isSingleValue()) {
						criterionSql.append(criterion.getAndOr()).append(" ");
						criterionSql.append(criterion.getCondition());
						criterionSql.append(" ? "); // 预编译占位符
						contidion.add(criterion.getValue());
					}
					if (criterion.isBetweenValue()) {
						criterionSql.append(criterion.getAndOr()).append(" ");
						;
						criterionSql.append(criterion.getCondition());
						criterionSql.append("( ? ");
						criterionSql.append(" and ");
						criterionSql.append(" ? )");
						contidion.add(criterion.getValue());
						contidion.add(criterion.getSecondValue());
					}
					if (criterion.isListValue()) {
						criterionSql.append(criterion.getAndOr()).append(" ").append(criterion.getCondition());
						StringBuilder listValueSql = new StringBuilder();
						Collection<?> value = (Collection<?>) criterion.getValue();
						listValueSql.append(" ( ");
						for (Object o : value) {
							listValueSql.append(" ?,");
							contidion.add(o);
						}
						String substring = listValueSql.substring(0, listValueSql.length() - 1);
						criterionSql.append(substring).append(" )");
					}

				}

				applyPrefix(criterionSql, criterionSql.toString().toUpperCase(Locale.ENGLISH), prefixOverrides, "(");
				applySuffix(criterionSql, criterionSql.toString().toUpperCase(Locale.ENGLISH), null, " ) ");
				oredCriterionSql.append(criterionSql);
			}
			whereSql.append(oredCriterionSql);
		}
		String trimmedUppercaseSql = whereSql.toString().toUpperCase(Locale.ENGLISH);
		if (trimmedUppercaseSql.length() > 0) {
			applyPrefix(whereSql, trimmedUppercaseSql, prefixOverrides, null);

		}
		sql.append(whereSql);
		returnpair = new GkPair<>(sql.toString(), contidion);
		return returnpair;
	}

	/**
	 * from mybatis whereSqlNode
	 * @param sql 需要修剪的sql
	 * @param trimmedUppercaseSql 原始sql
	 * @param prefixesToOverride 需要修剪的 前缀
	 * @param prefix 前缀
	 */
	private static void applyPrefix(StringBuilder sql, String trimmedUppercaseSql, List<String> prefixesToOverride,
			String prefix) {
		if (prefixesToOverride != null) {
			for (String toRemove : prefixesToOverride) {
				String trim = toRemove.trim();
				if (trimmedUppercaseSql.startsWith(trim.toUpperCase())) {
					sql.delete(0, trim.length());
					break;
				}
			}
		}
		if (prefix != null) {
			sql.insert(0, " ");
			sql.insert(0, prefix);
		}
	}

	private static void applySuffix(StringBuilder sql, String trimmedUppercaseSql, List<String> suffixesToOverride,
			String suffix) {
		if (suffixesToOverride != null) {
			for (String toRemove : suffixesToOverride) {
				String trim = toRemove.trim().toUpperCase();
				if (trimmedUppercaseSql.endsWith(trim) || trimmedUppercaseSql.endsWith(trim.trim())) {
					int start = sql.length() - toRemove.trim().length();
					int end = sql.length();
					sql.delete(start, end);
					break;
				}
			}
		}
		if (suffix != null) {
			sql.append(" ");
			sql.append(suffix);
		}
	}

	/**
	 * 检查 paremeter 对象中指定的 fields 是否全是 null，如果是则抛出异常
	 * @param girExample
	 * @return
	 */
	public static boolean exampleHasAtLeastOneCriteriaCheck(GirExample girExample) {
		if (girExample != null) {
			try {
				List<GirExample.Criteria> criteriaList = ((GirExample) girExample).getOredCriteria();
				if (criteriaList != null && criteriaList.size() > 0) {
					return true;
				}
			}
			catch (Exception e) {
				throw new RuntimeException("不允许不安全的全部删除操作", e);
			}
		}
		throw new RuntimeException("不允许不安全的全部删除操作");
	}

}
