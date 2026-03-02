// package cn.geoair.gtc.orm.spi.support;
//
//
// import com.alibaba.fastjson.JSONObject;
// import cn.geoair.gtc.base.bean. gtcBeanHelper;
// import cn.geoair.gtc.base.util. gtcObjectUtil;
// import cn.geoair.gtc.orm.spi.entity. gtcEntityColumn;
// import javafx.util.Pair;
//
// import java.util.*;
//
/// **
// * @author ：张俊
// * @date ：Created in 2022/7/1 9:33
// * @description： Example 方法提供者
// */
// public class gtcExampleProvider<T> {
//
// /**
// * 根据Example查询总数
// *
// * @param
// * @return
// */
// public Pair<String, List<Object>> selectCountByExample( gtcExample gtcExample) {
// Pair<String, List<Object>> sqlAndContition = null;
// List<Object> objects = new ArrayList<>();
// StringBuilder sql = new StringBuilder("SELECT ");
// sql.append(SqlHelper.exampleCountColumn( gtcExample, objects));
// sql.append(SqlHelper.fromTable( gtcExample, gtcExample.tableName));
// sql.append(SqlHelper.exampleWhereClause( gtcExample, objects).getKey());
// sql.append(SqlHelper.exampleForUpdate( gtcExample));
// return new Pair<>(sql.toString(), objects);
// }
//
// /**
// * 根据Example删除
// *
// * @param gtcExample
// * @return
// */
// public Pair<String, List<Object>> deleteByExample( gtcExample gtcExample) {
// Pair<String, List<Object>> sqlAndContition = null;
// List<Object> objects = new ArrayList<>();
// StringBuilder sql = new StringBuilder();
// //不允许执行不带查询条件的 delete 方法
// SqlHelper.exampleHasAtLeastOneCriteriaCheck( gtcExample);
// sql.append(SqlHelper.deleteFromTable( gtcExample, gtcExample.tableName));
// sql.append(SqlHelper.exampleWhereClause( gtcExample, objects).getKey());
// return new Pair<>(sql.toString(), objects);
// }
//
//
// /**
// * 根据Example查询
// *
// * @param gtcExample
// * @return
// */
// public Pair<String, List<Object>> selectByExample( gtcExample gtcExample) {
//
// StringBuilder sql = new StringBuilder("SELECT ");
// List<Object> objects = new ArrayList<>();
// if ( gtcExample.distinct) {
// sql.append("distinct");
// }
// //支持查询指定列
// sql.append(SqlHelper.exampleSelectColumns( gtcExample));
// sql.append(SqlHelper.fromTable( gtcExample, gtcExample.tableName));
// sql.append(SqlHelper.exampleWhereClause( gtcExample, objects).getKey());
// sql.append(SqlHelper.exampleOrderBy( gtcExample));
// sql.append(SqlHelper.exampleForUpdate( gtcExample));
// return new Pair<>(sql.toString(), objects);
// }
//
// /**
// * 根据Example查询
// *
// * @param gtcExample
// * @return
// */
// public Pair<String, List<Object>> selectByExampleAndRowBounds( gtcExample gtcExample) {
// return selectByExample( gtcExample);
// }
//
// /**
// * 根据Example更新非null字段
// *
// * @param
// * @return
// */
// public Pair<String, List<Object>> updateByExampleSelective(T updateEntity, gtcExample
// gtcExample) {
// List<Object> objects = new ArrayList<>();
// StringBuilder sql = new StringBuilder();
// //安全更新，Example 必须包含条件
// SqlHelper.exampleHasAtLeastOneCriteriaCheck( gtcExample);
// sql.append(SqlHelper.updateTable( gtcExample, gtcExample.tableName));
// sql.append(updateSetColumns(updateEntity, gtcExample, objects, true));
// sql.append(SqlHelper.updateByExampleWhereClause( gtcExample, objects).getKey());
// return new Pair<>(sql.toString(), objects);
// }
//
// /**
// * 根据Example更新
// *
// * @param
// * @return
// */
// public Pair<String, List<Object>> updateByExample(T updateEntity, gtcExample
// gtcExample) {
// List<Object> objects = new ArrayList<>();
// StringBuilder sql = new StringBuilder();
// //安全更新，Example 必须包含条件
// SqlHelper.exampleHasAtLeastOneCriteriaCheck( gtcExample);
// sql.append(SqlHelper.updateTable( gtcExample, gtcExample.tableName));
// sql.append(updateSetColumns(updateEntity, gtcExample, objects, false));
// sql.append(SqlHelper.updateByExampleWhereClause( gtcExample, objects).getKey());
// return new Pair<>(sql.toString(), objects);
// }
//
//
// private String updateSetColumns(T updateEntity, gtcExample gtcExample, List<Object>
// objects, boolean notNull) {
// // 这里判断是否为json类型
// Map<String, Object> stringObjectMap = null;/*BeanUtil.beanToMap(updateEntity);*/
// if (updateEntity instanceof HashMap) {
// stringObjectMap = (HashMap) updateEntity;
// } else {
// String jsonString = JSONObject.toJSONString(updateEntity);
// JSONObject jsonObject = JSONObject.parseObject(jsonString);
// stringObjectMap = jsonObject.getInnerMap();
// }
//
//
// StringBuilder sql = new StringBuilder();
// //获取全部列
// Set< gtcEntityColumn> columnSet = gtcEntityHelper.getColumns(
// gtcExample.getEntityClass());
// //当某个列有主键策略时，不需要考虑他的属性是否为空，因为如果为空，一定会根据主键策略给他生成一个值
// StringBuilder updatesql = new StringBuilder();
// for ( gtcEntityColumn column : columnSet) {
// if (!column.isId() && column.isUpdatable()) {
// Object columnValue = stringObjectMap.get(column.getProperty());
// if (notNull) {
// // 判断 这个column 对应的 有没有 值
// if (columnValue != null) {
// if (columnValue instanceof String) {
// if (columnValue != "") {
// updatesql.append(column.getColumn()).append(" = ").append("?").append(",");
// objects.add(columnValue);
// }
// } else {
// updatesql.append(column.getColumn()).append(" = ").append("?").append(",");
// objects.add(columnValue);
// }
// }
// } else {
// updatesql.append(column.getColumn()).append(" = ").append("?").append(",");
// objects.add(columnValue);
// }
// } else if (column.isId() && column.isUpdatable()) {
// updatesql.append(column.getColumn()).append(" =
// ").append(column.getColumn()).append(",");
// }
// }
// if (updatesql.length() != 0) {
// sql.append("set ");
// sql.append(updatesql.substring(0, updatesql.length() - 1));
// }
// return sql.toString();
// }
//
// }
