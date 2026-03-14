package cn.geoair.orm.spi.support;

import cn.geoair.base.data.tuples.GkPair;
import cn.geoair.base.json.GirJSON;
import cn.geoair.orm.spi.entity.GirEntityColumn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author ：张俊
 * @date ：Created in 2022/7/1 9:33 @description： Example 简单解析器
 */
public class GirSimpleExampleParser {

	/**
	 * 根据Example查询总数
	 * @param
	 * @return GkPair<String, List<Object>> key 是 sql value 是 值 （sql 里面使用 预编译 ？ 占位）
	 */
	public GkPair<String, List<Object>> selectCountByExample(GirExample girExample) {
		List<Object> objects = new ArrayList<>();
		StringBuilder sql = new StringBuilder("SELECT ");
		sql.append(SqlHelper.exampleCountColumn(girExample, objects));
		sql.append(SqlHelper.fromTable(girExample, girExample.tableName));
		sql.append(SqlHelper.exampleWhereClause(girExample, objects).getValue0());
		sql.append(SqlHelper.exampleForUpdate(girExample));
		return new GkPair<String, List<Object>>(sql.toString(), objects);
	}

	/**
	 * 根据Example删除
	 * @param girExample
	 * @return GkPair<String, List<Object>> key 是 sql value 是 值 （sql 里面使用 预编译 ？ 占位）
	 */
	public GkPair<String, List<Object>> deleteByExample(GirExample girExample) {
		GkPair<String, List<Object>> sqlAndContition = null;
		List<Object> objects = new ArrayList<>();
		StringBuilder sql = new StringBuilder();
		// 不允许执行不带查询条件的 delete 方法
		SqlHelper.exampleHasAtLeastOneCriteriaCheck(girExample);
		sql.append(SqlHelper.deleteFromTable(girExample, girExample.tableName));
		sql.append(SqlHelper.exampleWhereClause(girExample, objects).getValue0());
		return new GkPair<>(sql.toString(), objects);
	}

	/**
	 * 根据Example查询
	 * @param girExample
	 * @return GkPair<String, List<Object>> key 是 sql value 是 值 （sql 里面使用 预编译 ？ 占位）
	 */
	public GkPair<String, List<Object>> selectByExample(GirExample girExample) {

		StringBuilder sql = new StringBuilder("SELECT ");
		List<Object> objects = new ArrayList<>();
		if (girExample.distinct) {
			sql.append("distinct");
		}
		// 支持查询指定列
		sql.append(SqlHelper.exampleSelectColumns(girExample));
		sql.append(SqlHelper.fromTable(girExample, girExample.tableName));
		sql.append(SqlHelper.exampleWhereClause(girExample, objects).getValue0());
		sql.append(SqlHelper.exampleOrderBy(girExample));
		sql.append(SqlHelper.exampleForUpdate(girExample));
		return new GkPair<>(sql.toString(), objects);
	}

	/**
	 * 根据Example查询
	 * @param girExample
	 * @return GkPair<String, List<Object>> key 是 sql value 是 值 （sql 里面使用 预编译 ？ 占位）
	 */
	public GkPair<String, List<Object>> selectByExampleAndRowBounds(GirExample girExample) {
		return selectByExample(girExample);
	}

	/**
	 * 根据Example更新非null字段
	 * @param
	 * @return GkPair<String, List<Object>> key 是 sql value 是 值 （sql 里面使用 预编译 ？ 占位）
	 */
	public GkPair<String, List<Object>> updateByExampleSelective(Object updateEntity, GirExample girExample) {
		List<Object> objects = new ArrayList<>();
		StringBuilder sql = new StringBuilder();
		// 安全更新，Example 必须包含条件
		SqlHelper.exampleHasAtLeastOneCriteriaCheck(girExample);
		sql.append(SqlHelper.updateTable(girExample, girExample.tableName));
		sql.append(updateSetColumns(updateEntity, girExample, objects, true));
		sql.append(SqlHelper.updateByExampleWhereClause(girExample, objects).getValue0());
		return new GkPair<>(sql.toString(), objects);
	}

	/**
	 * 根据Example更新
	 * @param
	 * @return GkPair<String, List<Object>> key 是 sql value 是 值 （sql 里面使用 预编译 ？ 占位）
	 */
	public GkPair<String, List<Object>> updateByExample(Object updateEntity, GirExample girExample) {
		List<Object> objects = new ArrayList<>();
		StringBuilder sql = new StringBuilder();
		// 安全更新，Example 必须包含条件
		SqlHelper.exampleHasAtLeastOneCriteriaCheck(girExample);
		sql.append(SqlHelper.updateTable(girExample, girExample.tableName));
		sql.append(updateSetColumns(updateEntity, girExample, objects, false));
		sql.append(SqlHelper.updateByExampleWhereClause(girExample, objects).getValue0());
		return new GkPair<>(sql.toString(), objects);
	}

	private String updateSetColumns(Object updateEntity, GirExample girExample, List<Object> objects, boolean notNull) {
		// 这里判断是否为json类型

		Map<String, Object> stringObjectMap = GirJSON.toJson(updateEntity).toBean(HashMap.class);

		// Map<String, Object> stringObjectMap =
		// JSONObject.parseObject(JSONObject.toJSONString(updateEntity)).getInnerMap();
		StringBuilder sql = new StringBuilder();
		// 获取全部列
		Set<GirEntityColumn> columnSet = GirEntityHelper.getColumns(girExample.getEntityClass());
		// 当某个列有主键策略时，不需要考虑他的属性是否为空，因为如果为空，一定会根据主键策略给他生成一个值
		StringBuilder updatesql = new StringBuilder();
		for (GirEntityColumn column : columnSet) {
			if (!column.isId() && column.isUpdatable()) {
				Object columnValue = stringObjectMap.get(column.getProperty());
				if (notNull) {
					// 判断 这个column 对应的 有没有 值
					if (columnValue != null) {
						if (columnValue instanceof String) {
							if (columnValue != "") {
								updatesql.append(column.getColumn()).append(" = ").append("?").append(",");
								objects.add(column.wrapColumnValue(columnValue));
							}
						}
						else {
							updatesql.append(column.getColumn()).append(" = ").append("?").append(",");
							objects.add(column.wrapColumnValue(columnValue));
						}
					}
				}
				else {
					updatesql.append(column.getColumn()).append(" = ").append("?").append(",");
					objects.add(column.wrapColumnValue(columnValue));
				}
			}
			else if (column.isId() && column.isUpdatable()) {
				updatesql.append(column.getColumn()).append(" = ").append(column.getSelectColumn()).append(",");
			}
		}
		if (updatesql.length() != 0) {
			sql.append("set ");
			sql.append(updatesql.substring(0, updatesql.length() - 1));
		}
		return sql.toString();
	}

}
