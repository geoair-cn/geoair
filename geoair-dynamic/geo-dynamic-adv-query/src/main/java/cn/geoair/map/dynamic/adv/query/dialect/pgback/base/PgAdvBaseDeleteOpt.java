// package cn.geoair.map.dynamic.adv.query.dialect.pgback.base;
//
// import cn.geoair.gtc.base.log.GiLogger;
// import cn.geoair.gtc.base.log.GirLogger;
// import cn.geoair.map.dynamic.adv.query.IAdvBaseDeleteOpt;
// import cn.geoair.map.dynamic.adv.mybatis.SqlEngineUtil;
// import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
// import cn.geoair.map.dynamic.ds.IDataSourceGetter;
// import cn.hutool.core.collection.CollUtil;
// import cn.hutool.core.util.StrUtil;
// import cn.hutool.db.handler.NumberHandler;
// import cn.hutool.db.sql.SqlExecutor;
// import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
//
// import java.sql.Connection;
// import java.sql.SQLException;
// import java.util.*;
// import java.util.stream.Collectors;
//
/// **
// * PostgreSQL数据库的删除操作实现类
// * <p>
// * 实现IAdvBaseDeleteOpt接口，适配PostgreSQL的语法特性，
// * 提供高性能、安全、全场景的删除操作支持，依赖DataSourceGetter获取数据库连接
// */
// public class PgAdvBaseDeleteOpt implements IAdvBaseDeleteOpt {
//
// // 注入数据源获取器
// private IDataSourceGetter dataSourceGetter;
//
// // 日志实例
// private static final GiLogger log = GirLogger.getLoger(PgAdvBaseDeleteOpt.class);
// // 默认分批删除批次大小
// private static final int DEFAULT_BATCH_SIZE = 1000;
//
// private static final int MAX_IN_PARAMS = 1000;
//
// // 设置数据源获取器（依赖注入）
// public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {
// this.dataSourceGetter = dataSourceGetter;
// }
//
// // ==================== 自定义SQL删除 ====================
// @Override
// public Integer bDeleteBySql(String sqlStatement) {
// return bDeleteBySql(sqlStatement, SqlParamMap.of());
// }
//
// @Override
// public Integer bDeleteBySql(String sqlStatement, SqlParamMap sqlParam) {
// // 参数校验
// if (StrUtil.isEmpty(sqlStatement)) {
// throw new IllegalArgumentException("删除SQL语句不能为空");
// }
//
// // 解析SQL（支持MyBatis标签）
// SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(cleanSql(sqlStatement), sqlParam);
// String execSql = sqlMeta.getSql();
// List<Object> jdbcParams = sqlMeta.getJdbcParamValues();
//
// Connection connection = dataSourceGetter.getConnection();
// try {
// log.info("schema:[{}] db:[{}] 执行自定义删除SQL：{}，参数：{}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(), execSql,
// sqlParam);
//
// if (CollUtil.isEmpty(jdbcParams)) {
// // 无参数SQL
// return SqlExecutor.execute(connection, execSql);
// } else {
// // 带参数SQL
// return SqlExecutor.execute(connection, execSql, jdbcParams.toArray());
// }
// } catch (SQLException e) {
// throw new RuntimeException("执行自定义删除SQL失败，SQL：" + execSql, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// // ==================== 主键删除 ====================
// @Override
// public Integer bDeleteByPrimaryKey(String tableName, String idKey, Object id) {
// // 参数校验
// validateTableName(tableName);
// validateIdKeyAndValue(idKey, id);
//
// // 构建删除SQL
// String execSql = StrUtil.format("DELETE FROM {} WHERE {} = ?", tableName, idKey);
//
// Connection connection = dataSourceGetter.getConnection();
// try {
// log.info("schema:[{}] db:[{}] 按主键删除，表名：{}，主键：{}={}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(),
// tableName, idKey, id);
//
// return SqlExecutor.execute(connection, execSql, id);
// } catch (SQLException e) {
// throw new RuntimeException("按主键删除失败，表名：" + tableName + "，主键：" + idKey + "=" + id, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public Integer bDeleteBatchByPrimaryKey(String tableName, String idKey, Set<Object>
// ids) {
// // 参数校验
// validateTableName(tableName);
// validateIdKey(idKey);
// if (CollUtil.isEmpty(ids)) {
// return 0;
// }
//
// // 拆分IN参数（避免超过PostgreSQL参数限制）
// List<List<Object>> idBatches = splitCollection(ids, MAX_IN_PARAMS);
// int totalSuccess = 0;
//
// Connection connection = dataSourceGetter.getConnection();
// try {
// connection.setAutoCommit(false);
//
// for (List<Object> idBatch : idBatches) {
// // 构建IN子句占位符
// String placeholders = idBatch.stream().map(id -> "?").collect(Collectors.joining(","));
// String execSql = StrUtil.format("DELETE FROM {} WHERE {} IN ({})", tableName, idKey,
// placeholders);
//
// log.info("schema:[{}] db:[{}] 批量主键删除，表名：{}，主键数量：{}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(),
// tableName, idBatch.size());
//
// int batchSuccess = SqlExecutor.execute(connection, execSql, idBatch.toArray());
// totalSuccess += batchSuccess;
// }
//
// connection.commit();
// return totalSuccess;
// } catch (SQLException e) {
// try {
// connection.rollback();
// } catch (SQLException rollbackEx) {
// log.error("批量主键删除回滚失败", rollbackEx);
// }
// throw new RuntimeException("批量主键删除失败，表名：" + tableName, e);
// } finally {
// try {
// connection.setAutoCommit(true);
// } catch (SQLException e) {
// log.error("恢复自动提交失败", e);
// }
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public Integer bDeleteBatchWithBatchSize(String tableName, String idKey, Set<Object>
// ids, int batchSize) {
// // 参数校验
// validateTableName(tableName);
// validateIdKey(idKey);
// if (CollUtil.isEmpty(ids)) {
// return 0;
// }
// if (batchSize <= 0 || batchSize > MAX_IN_PARAMS) {
// batchSize = DEFAULT_BATCH_SIZE;
// }
//
// // 拆分批次
// List<List<Object>> idBatches = splitCollection(ids, batchSize);
// int totalSuccess = 0;
//
// for (List<Object> idBatch : idBatches) {
// totalSuccess += bDeleteBatchByPrimaryKey(tableName, idKey, new HashSet<>(idBatch));
// }
//
// log.info("schema:[{}] db:[{}] 分批次主键删除完成，表名：{}，总删除行数：{}，批次大小：{}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(),
// tableName, totalSuccess, batchSize);
// return totalSuccess;
// }
//
// // ==================== 条件删除 ====================
// @Override
// public Integer bDeleteByCondition(String tableName, Map<String, Object> whereMap) {
// // 参数校验（禁止空条件，避免全表删除）
// validateTableName(tableName);
// if (CollUtil.isEmpty(whereMap)) {
// throw new IllegalArgumentException("删除条件不能为空（禁止全表删除）");
// }
//
// // 构建WHERE子句
// String whereClause = whereMap.keySet().stream()
// .map(field -> StrUtil.format("{} = ?", field))
// .collect(Collectors.joining(" AND "));
// String execSql = StrUtil.format("DELETE FROM {} WHERE {}", tableName, whereClause);
//
// // 组装参数
// List<Object> params = new ArrayList<>(whereMap.values());
//
// Connection connection = dataSourceGetter.getConnection();
// try {
// log.info("schema:[{}] db:[{}] 条件删除，表名：{}，条件：{}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(),
// tableName, whereMap);
//
// return SqlExecutor.execute(connection, execSql, params.toArray());
// } catch (SQLException e) {
// throw new RuntimeException("条件删除失败，表名：" + tableName, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public Integer bDeleteBatchByCondition(String tableName, Map<String, Object> whereMap,
// int batchSize) {
// // 参数校验
// validateTableName(tableName);
// if (CollUtil.isEmpty(whereMap)) {
// throw new IllegalArgumentException("删除条件不能为空（禁止全表删除）");
// }
// if (batchSize <= 0) {
// batchSize = DEFAULT_BATCH_SIZE;
// }
//
// int totalSuccess = 0;
// Connection connection = dataSourceGetter.getConnection();
//
// try {
// connection.setAutoCommit(false);
//
// // 循环删除，直到无数据可删
// while (true) {
// // 构建分批删除SQL（LIMIT限制单次删除行数）
// String whereClause = whereMap.keySet().stream()
// .map(field -> StrUtil.format("{} = ?", field))
// .collect(Collectors.joining(" AND "));
// String execSql = StrUtil.format("DELETE FROM {} WHERE {} LIMIT {}",
// tableName, whereClause, batchSize);
//
// List<Object> params = new ArrayList<>(whereMap.values());
// int batchSuccess = SqlExecutor.execute(connection, execSql, params.toArray());
//
// totalSuccess += batchSuccess;
//
// // 无数据可删时退出循环
// if (batchSuccess < batchSize) {
// break;
// }
// }
//
// connection.commit();
// log.info("schema:[{}] db:[{}] 分批次条件删除完成，表名：{}，总删除行数：{}，批次大小：{}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(),
// tableName, totalSuccess, batchSize);
// return totalSuccess;
// } catch (SQLException e) {
// try {
// connection.rollback();
// } catch (SQLException rollbackEx) {
// log.error("分批次条件删除回滚失败", rollbackEx);
// }
// throw new RuntimeException("分批次条件删除失败，表名：" + tableName, e);
// } finally {
// try {
// connection.setAutoCommit(true);
// } catch (SQLException e) {
// log.error("恢复自动提交失败", e);
// }
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// // ==================== 特殊场景删除 ====================
// @Override
// public Integer bLogicDelete(String tableName, String idKey, Object id, String
// deleteKey, Object deleteValue) {
// // 参数校验
// validateTableName(tableName);
// validateIdKeyAndValue(idKey, id);
// validateIdKeyAndValue(deleteKey, deleteValue);
//
// // 逻辑删除本质是更新操作
// String execSql = StrUtil.format("UPDATE {} SET {} = ? WHERE {} = ?",
// tableName, deleteKey, idKey);
//
// Connection connection = dataSourceGetter.getConnection();
// try {
// log.info("schema:[{}] db:[{}] 逻辑删除，表名：{}，主键：{}={}，删除标记：{}={}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(),
// tableName, idKey, id, deleteKey, deleteValue);
//
// return SqlExecutor.execute(connection, execSql, deleteValue, id);
// } catch (SQLException e) {
// throw new RuntimeException("逻辑删除失败，表名：" + tableName + "，主键：" + idKey + "=" + id, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public Integer bLogicDeleteBatch(String tableName, String idKey, Set<Object> ids,
// String deleteKey, Object deleteValue) {
// // 参数校验
// validateTableName(tableName);
// validateIdKey(idKey);
// validateIdKeyAndValue(deleteKey, deleteValue);
// if (CollUtil.isEmpty(ids)) {
// return 0;
// }
//
// // 拆分IN参数
// List<List<Object>> idBatches = splitCollection(ids, MAX_IN_PARAMS);
// int totalSuccess = 0;
//
// Connection connection = dataSourceGetter.getConnection();
// try {
// connection.setAutoCommit(false);
//
// for (List<Object> idBatch : idBatches) {
// String placeholders = idBatch.stream().map(id -> "?").collect(Collectors.joining(","));
// String execSql = StrUtil.format("UPDATE {} SET {} = ? WHERE {} IN ({})",
// tableName, deleteKey, idKey, placeholders);
//
// // 组装参数：删除标记值 + 主键值列表
// List<Object> params = new ArrayList<>();
// params.add(deleteValue);
// params.addAll(idBatch);
//
// int batchSuccess = SqlExecutor.execute(connection, execSql, params.toArray());
// totalSuccess += batchSuccess;
// }
//
// connection.commit();
// log.info("schema:[{}] db:[{}] 批量逻辑删除完成，表名：{}，删除行数：{}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(),
// tableName, totalSuccess);
// return totalSuccess;
// } catch (SQLException e) {
// try {
// connection.rollback();
// } catch (SQLException rollbackEx) {
// log.error("批量逻辑删除回滚失败", rollbackEx);
// }
// throw new RuntimeException("批量逻辑删除失败，表名：" + tableName, e);
// } finally {
// try {
// connection.setAutoCommit(true);
// } catch (SQLException e) {
// log.error("恢复自动提交失败", e);
// }
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public Integer bSafeDeleteByCondition(String tableName, Map<String, Object> whereMap,
// int maxDelete) {
// // 参数校验
// validateTableName(tableName);
// if (CollUtil.isEmpty(whereMap)) {
// throw new IllegalArgumentException("删除条件不能为空（禁止全表删除）");
// }
// if (maxDelete <= 0) {
// throw new IllegalArgumentException("最大允许删除行数阈值必须大于0");
// }
//
// // 1. 先查询符合条件的行数
// String countWhereClause = whereMap.keySet().stream()
// .map(field -> StrUtil.format("{} = ?", field))
// .collect(Collectors.joining(" AND "));
// String countSql = StrUtil.format("SELECT COUNT(1) FROM {} WHERE {}", tableName,
// countWhereClause);
// List<Object> countParams = new ArrayList<>(whereMap.values());
//
// Connection connection = dataSourceGetter.getConnection();
// try {
// Number count = SqlExecutor.query(connection, countSql, new NumberHandler(),
// countParams.toArray());
// int totalCount = count.intValue();
// // 2. 校验行数是否超过阈值
// if (totalCount > maxDelete) {
// log.warn("schema:[{}] db:[{}] 安全删除触发阈值限制，表名：{}，符合条件行数：{}，阈值：{}，操作终止",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(),
// tableName, totalCount, maxDelete);
// return -1; // 超过阈值返回-1
// }
//
// // 3. 执行删除
// return bDeleteByCondition(tableName, whereMap);
// } catch (SQLException e) {
// throw new RuntimeException("安全删除校验失败，表名：" + tableName, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
//
//
// /**
// * 清理SQL语句（移除末尾分号、多余空格）
// */
// private String cleanSql(String sql) {
// if (StrUtil.isEmpty(sql)) {
// return sql;
// }
// return sql.replaceAll("\\s*;\\s*$", "").trim();
// }
//
// /**
// * 校验表名
// */
// private void validateTableName(String tableName) {
// if (StrUtil.isEmpty(tableName)) {
// throw new IllegalArgumentException("表名不能为空");
// }
// }
//
// /**
// * 校验主键字段名
// */
// private void validateIdKey(String idKey) {
// if (StrUtil.isEmpty(idKey)) {
// throw new IllegalArgumentException("主键/字段名不能为空");
// }
// }
//
// /**
// * 校验主键字段名和值
// */
// private void validateIdKeyAndValue(String idKey, Object id) {
// validateIdKey(idKey);
// if (id == null) {
// throw new IllegalArgumentException("主键/字段值不能为空");
// }
// }
//
// /**
// * 将集合拆分为指定大小的批次列表
// */
// private <T> List<List<T>> splitCollection(Collection<T> collection, int batchSize) {
// List<List<T>> batches = new ArrayList<>();
// List<T> currentBatch = new ArrayList<>();
//
// for (T item : collection) {
// currentBatch.add(item);
// if (currentBatch.size() >= batchSize) {
// batches.add(currentBatch);
// currentBatch = new ArrayList<>();
// }
// }
//
// if (!currentBatch.isEmpty()) {
// batches.add(currentBatch);
// }
//
// return batches;
// }
// }
