// package cn.geoair.map.dynamic.adv.query.dialect.pgback.base;
//
// import cn.geoair..base.log.GiLogger;
// import cn.geoair..base.log.GirLogger;
// import cn.geoair.map.dynamic.adv.query.IAdvBaseUpdateOpt;
// import cn.geoair.map.dynamic.adv.mybatis.SqlEngineUtil;
// import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
// import cn.geoair.map.dynamic.ds.IDataSourceGetter;
// import cn.hutool.core.collection.CollUtil;
// import cn.hutool.core.util.StrUtil;
// import cn.hutool.db.Entity;
// import cn.hutool.db.sql.SqlExecutor;
// import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
//
// import java.sql.Connection;
// import java.sql.SQLException;
// import java.util.*;
// import java.util.Collection;
// import java.util.stream.Collectors;
//
/// **
// * PostgreSQL数据库的更新操作实现类
// * <p>
// * 实现IAdvBaseUpdateOpt接口，适配PostgreSQL的语法特性，
// * 提供高性能、全场景的更新操作支持，依赖DataSourceGetter获取数据库连接
// *
// * @author （可补充作者信息）
// * @version 1.0
// * @since （可补充适配版本）
// */
// public class PgAdvBaseUpdateOpt implements IAdvBaseUpdateOpt {
//
// // 注入数据源获取器
// private IDataSourceGetter dataSourceGetter;
//
// // 日志实例
// private static final GiLogger log = GirLogger.getLoger(PgAdvBaseUpdateOpt.class);
// // 默认分批更新批次大小
// private static final int DEFAULT_BATCH_SIZE = 1000;
// // PostgreSQL UPSERT冲突处理关键字
// private static final String PG_CONFLICT_CLAUSE = " ON CONFLICT ";
//
// // 设置数据源获取器（依赖注入）
// public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {
// this.dataSourceGetter = dataSourceGetter;
// }
//
// // ==================== 自定义SQL更新 ====================
// @Override
// public Integer bUpdateBySql(String sqlStatement) {
// return bUpdateBySql(sqlStatement, SqlParamMap.of());
// }
//
// @Override
// public Integer bUpdateBySql(String sqlStatement, SqlParamMap sqlParam) {
// // 参数校验
// if (StrUtil.isEmpty(sqlStatement)) {
// throw new IllegalArgumentException("更新SQL语句不能为空");
// }
//
// // 解析SQL（支持MyBatis标签）
// SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(cleanSql(sqlStatement), sqlParam);
// String execSql = sqlMeta.getSql();
// List<Object> jdbcParams = sqlMeta.getJdbcParamValues();
//
// Connection connection = dataSourceGetter.getConnection();
// try {
// log.info("schema:[{}] db:[{}] 执行自定义更新SQL：{}，参数：{}",
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
// throw new RuntimeException("执行自定义更新SQL失败，SQL：" + execSql, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// // ==================== 单条数据更新 ====================
// @Override
// public Integer bUpdateByPrimaryKey(String tableName, String idKey, Object id,
// Map<String, Object> rowData) {
// // 参数校验
// validateTableName(tableName);
// validateIdKeyAndValue(idKey, id);
// validateUpdateData(rowData);
//
// // 构建UPDATE SQL
// String setClause = rowData.keySet().stream()
// .map(field -> StrUtil.format("{} = ?", field))
// .collect(Collectors.joining(","));
// String execSql = StrUtil.format("UPDATE {} SET {} WHERE {} = ?",
// tableName, setClause, idKey);
//
// // 组装参数（更新字段值 + 主键值）
// List<Object> params = new ArrayList<>(rowData.values());
// params.add(id);
//
// Connection connection = dataSourceGetter.getConnection();
// try {
// log.info("schema:[{}] db:[{}] 按主键更新，表名：{}，主键：{}={}，更新字段：{}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(),
// tableName, idKey, id, rowData.keySet());
//
// return SqlExecutor.execute(connection, execSql, params.toArray());
// } catch (SQLException e) {
// throw new RuntimeException("按主键更新失败，表名：" + tableName + "，主键：" + idKey + "=" + id, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public <T> Integer bUpdateByPrimaryKey(String tableName, String idKey, T entity) {
// // 参数校验
// validateTableName(tableName);
// validateIdKey(idKey);
// if (entity == null) {
// throw new IllegalArgumentException("更新的实体对象不能为空");
// }
//
// // 实体转换为Map
// Entity entityObj = Entity.parse(entity);
//
//
// // 提取主键值
// Object id = entityObj.remove(idKey);
// if (id == null) {
// throw new IllegalArgumentException("实体对象中未找到主键字段[" + idKey + "]的值");
// }
//
// return bUpdateByPrimaryKey(tableName, idKey, id, entityObj);
// }
//
// @Override
// public Integer bUpdateByCondition(String tableName, Map<String, Object> rowData,
// Map<String, Object> whereMap) {
// // 参数校验
// validateTableName(tableName);
// validateUpdateData(rowData);
// if (CollUtil.isEmpty(whereMap)) {
// throw new IllegalArgumentException("更新条件不能为空（避免全表更新）");
// }
//
// // 构建SET子句和WHERE子句
// String setClause = rowData.keySet().stream()
// .map(field -> StrUtil.format("{} = ?", field))
// .collect(Collectors.joining(","));
// String whereClause = whereMap.keySet().stream()
// .map(field -> StrUtil.format("{} = ?", field))
// .collect(Collectors.joining(" AND "));
// String execSql = StrUtil.format("UPDATE {} SET {} WHERE {}",
// tableName, setClause, whereClause);
//
// // 组装参数（更新字段值 + 条件值）
// List<Object> params = new ArrayList<>(rowData.values());
// params.addAll(whereMap.values());
//
// Connection connection = dataSourceGetter.getConnection();
// try {
// log.info("schema:[{}] db:[{}] 条件更新，表名：{}，更新字段：{}，条件：{}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(),
// tableName, rowData.keySet(), whereMap);
//
// return SqlExecutor.execute(connection, execSql, params.toArray());
// } catch (SQLException e) {
// throw new RuntimeException("条件更新失败，表名：" + tableName, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// // ==================== 批量更新 ====================
// @Override
// public Integer bUpdateBatchByPrimaryKey(String tableName, String idKey,
// List<Map<String, Object>> rowsData) {
// return bUpdateBatchWithBatchSize(tableName, idKey, rowsData, DEFAULT_BATCH_SIZE);
// }
//
// @Override
// public Integer bUpdateBatchWithBatchSize(String tableName, String idKey,
// List<Map<String, Object>> rowsData, int batchSize) {
// // 参数校验
// validateTableName(tableName);
// validateIdKey(idKey);
// if (CollUtil.isEmpty(rowsData)) {
// return 0;
// }
// if (batchSize <= 0) {
// batchSize = DEFAULT_BATCH_SIZE;
// }
//
// // 拆分批次
// List<List<Map<String, Object>>> batches = CollUtil.split(rowsData, batchSize);
// int totalSuccess = 0;
//
// Connection connection = dataSourceGetter.getConnection();
// try {
// // 关闭自动提交，提升批量更新性能
// connection.setAutoCommit(false);
//
// for (List<Map<String, Object>> batch : batches) {
// int batchSuccess = 0;
// for (Map<String, Object> row : batch) {
// // 提取主键值
// Object id = row.get(idKey);
// if (id == null) {
// throw new IllegalArgumentException("批量更新数据中缺少主键字段[" + idKey + "]的值");
// }
//
// // 移除主键字段（避免更新主键）
// Map<String, Object> updateData = new HashMap<>(row);
// updateData.remove(idKey);
//
// // 执行单条更新
// batchSuccess += bUpdateByPrimaryKey(tableName, idKey, id, updateData);
// }
// totalSuccess += batchSuccess;
// }
//
// connection.commit();
// log.info("schema:[{}] db:[{}] 批量更新完成，表名：{}，总条数：{}，批次大小：{}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(),
// tableName, totalSuccess, batchSize);
// return totalSuccess;
// } catch (SQLException e) {
// try {
// connection.rollback();
// } catch (SQLException rollbackEx) {
// log.error("批量更新回滚失败", rollbackEx);
// }
// throw new RuntimeException("批量更新失败，表名：" + tableName, e);
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
// public <T> Integer bUpdateBatchByPrimaryKey(String tableName, String idKey,
// Collection<T> entities) {
// // 参数校验
// validateTableName(tableName);
// validateIdKey(idKey);
// if (CollUtil.isEmpty(entities)) {
// return 0;
// }
//
// // 实体列表转换为Map列表
// List<Map<String, Object>> rowsData = entities.stream()
// .map(Entity::parse)
// .collect(Collectors.toList());
//
// return bUpdateBatchByPrimaryKey(tableName, idKey, rowsData);
// }
//
// // ==================== 特殊场景更新 ====================
// @Override
// public Integer bUpdateWithOptimisticLock(String tableName, String idKey, Object id,
// Map<String, Object> rowData, String versionKey, Integer version) {
// // 参数校验
// validateTableName(tableName);
// validateIdKeyAndValue(idKey, id);
// validateUpdateData(rowData);
// validateIdKeyAndValue(versionKey, version);
//
// // 构建乐观锁更新SQL：WHERE 主键 = ? AND 版本号 = ?
// String setClause = rowData.keySet().stream()
// .map(field -> {
// // 版本号自增
// if (field.equals(versionKey)) {
// return StrUtil.format("{} = {} + 1", versionKey, versionKey);
// }
// return StrUtil.format("{} = ?", field);
// })
// .collect(Collectors.joining(","));
// String execSql = StrUtil.format("UPDATE {} SET {} WHERE {} = ? AND {} = ?",
// tableName, setClause, idKey, versionKey);
//
// // 组装参数（更新字段值 + 主键值 + 版本号）
// List<Object> params = new ArrayList<>(rowData.values());
// params.add(id);
// params.add(version);
//
// Connection connection = dataSourceGetter.getConnection();
// try {
// log.info("schema:[{}] db:[{}] 乐观锁更新，表名：{}，主键：{}={}，版本号：{}={}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(),
// tableName, idKey, id, versionKey, version);
//
// return SqlExecutor.execute(connection, execSql, params.toArray());
// } catch (SQLException e) {
// throw new RuntimeException("乐观锁更新失败，表名：" + tableName + "，主键：" + idKey + "=" + id, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public Integer bUpdateOrInsert(String tableName, Map<String, Object> rowData,
// Set<String> conflictKeys) {
// // 参数校验
// validateTableName(tableName);
// validateUpdateData(rowData);
// if (CollUtil.isEmpty(conflictKeys)) {
// throw new IllegalArgumentException("冲突判定字段不能为空");
// }
//
// // 构建UPSERT SQL
// String fields = String.join(",", rowData.keySet());
// String placeholders = rowData.keySet().stream().map(key ->
// "?").collect(Collectors.joining(","));
// String conflictFields = String.join(",", conflictKeys);
//
// // 构建更新子句（冲突时更新所有字段）
// String updateClause = rowData.keySet().stream()
// .filter(field -> !conflictKeys.contains(field)) // 冲突字段不更新
// .map(field -> StrUtil.format("{} = EXCLUDED.{}", field, field))
// .collect(Collectors.joining(","));
//
// String execSql = StrUtil.format(
// "INSERT INTO {} ({}) VALUES ({}){}({}) DO UPDATE SET {}",
// tableName, fields, placeholders, PG_CONFLICT_CLAUSE, conflictFields, updateClause
// );
//
// List<Object> params = new ArrayList<>(rowData.values());
// Connection connection = dataSourceGetter.getConnection();
// try {
// log.info("schema:[{}] db:[{}] 更新或插入，表名：{}，冲突字段：{}，更新字段：{}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(),
// tableName, conflictFields, updateClause);
//
// return SqlExecutor.execute(connection, execSql, params.toArray());
// } catch (SQLException e) {
// throw new RuntimeException("更新或插入失败，表名：" + tableName, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// // ==================== 私有工具方法 ====================
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
// throw new IllegalArgumentException("主键字段名不能为空");
// }
// }
//
// /**
// * 校验主键字段名和值
// */
// private void validateIdKeyAndValue(String idKey, Object id) {
// validateIdKey(idKey);
// if (id == null) {
// throw new IllegalArgumentException("主键值不能为空");
// }
// }
//
// /**
// * 校验更新数据（不能为空）
// */
// private void validateUpdateData(Map<String, Object> rowData) {
// if (CollUtil.isEmpty(rowData)) {
// throw new IllegalArgumentException("更新的数据不能为空");
// }
// }
// }
