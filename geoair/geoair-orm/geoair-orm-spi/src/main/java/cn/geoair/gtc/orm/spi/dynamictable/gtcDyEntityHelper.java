// package cn.geoair.gtc.orm.spi.dynamictable;
//
// import cn.geoair.gtc.orm.spi. gtcEntityResolve;
// import cn.geoair.gtc.orm.spi.entity. gtcEntityColumn;
// import cn.geoair.gtc.orm.spi.entity. gtcEntityTable;
// import cn.geoair.gtc.orm.spi.support. gtcEntityHelper;
//
// import java.util.Map;
// import java.util.Set;
// import java.util.concurrent.ConcurrentHashMap;
//
/// **
// * @author ：张俊
// * @date ：Created in 2022/7/2 14:45
// * @description： 动态表的实体类解析器
// */
// public class gtcDyEntityHelper extends gtcEntityHelper {
//
// /**
// * 实体类 => 表对象
// */
// private static final Map<String, gtcEntityTable> entityTableMap = new
// ConcurrentHashMap();
//
//
// /**
// * 实体类解析器
// */
// private static gtcEntityResolve dynamictableresolve = null;
//
// /**
// * 获取表对象l
// *
// * @param taskid 任务表主键对象
// * @return
// */
// public static gtcEntityTable getEntityTable(String taskid) {
// initEntityNameMap(taskid);
// gtcEntityTable gtcEntityTable = entityTableMap.get(taskid);
// if ( gtcEntityTable == null) {
// throw new RuntimeException("无法获取任务" + taskid + "对应的表名!");
// }
// return gtcEntityTable;
// }
//
// /**
// * 更新表对象
// * @param taskid 任务id
// * @return
// */
// public static gtcEntityTable updateEntityTable(String taskid) {
// gtcEntityTable gtcEntityTable = dynamictableresolve.resolveEntity(taskid);
// if ( gtcEntityTable == null) {
// throw new RuntimeException("无法通过该解析器" + dynamictableresolve.getClass().getName() +
// "获取实体对象!");
// } else {
// entityTableMap.put(taskid, gtcEntityTable);
// }
// return gtcEntityTable;
// }
//
// /**
// * 获取默认的orderby语句
// *
// * @param taskid
// * @return
// */
// public static String getOrderByClause(String taskid) {
// gtcEntityTable table = getEntityTable(taskid);
// if (table.getOrderByClause() != null) {
// return table.getOrderByClause();
// }
// StringBuilder orderBy = new StringBuilder();
// for ( gtcEntityColumn column : table.getEntityClassColumns()) {
// if (column.getOrderBy() != null) {
// if (orderBy.length() != 0) {
// orderBy.append(",");
// }
// orderBy.append(column.getColumn()).append(" ").append(column.getOrderBy());
// }
// }
// table.setOrderByClause(orderBy.toString());
// return table.getOrderByClause();
// }
//
// /**
// * 获取全部列
// *
// * @param taskid
// * @return
// */
// public static Set< gtcEntityColumn> getColumns(String taskid) {
// return getEntityTable(taskid).getEntityClassColumns();
// }
//
//
// /**
// * 获取主键信息
// *
// * @param taskid
// * @return
// */
// public static Set< gtcEntityColumn> getPKColumns(String taskid) {
// return getEntityTable(taskid).getEntityClassPKColumns();
// }
//
// /**
// * 获取查询的Select
// *
// * @param taskid
// * @return
// */
// public static String getSelectColumns(String taskid) {
// gtcEntityTable gtcEntityTable = getEntityTable(taskid);
// if ( gtcEntityTable.getBaseSelect() != null) {
// return gtcEntityTable.getBaseSelect();
// }
// Set< gtcEntityColumn> columnList = getColumns(taskid);
// StringBuilder selectBuilder = new StringBuilder();
// for ( gtcEntityColumn gtcEntityColumn : columnList) {
// selectBuilder.append( gtcEntityColumn.getSelectColumn());
// selectBuilder.append(",");
// }
// gtcEntityTable.setBaseSelect(selectBuilder.substring(0, selectBuilder.length() - 1));
// return gtcEntityTable.getBaseSelect();
// }
//
// /**
// * 初始化实体属性
// *
// * @param taskid
// */
// public static synchronized void initEntityNameMap(String taskid) {
// if (entityTableMap.get(taskid) != null) {
// return;
// }
// //创建并缓存EntityTable
// gtcEntityTable gtcEntityTable = dynamictableresolve.resolveEntity(taskid);
// entityTableMap.put(taskid, gtcEntityTable);
// }
//
// /**
// * 设置实体类解析器
// *
// * @param resolve
// */
// public static void setResolve( gtcEntityResolve resolve) {
// gtcDyEntityHelper.dynamictableresolve = resolve;
// }
// }
