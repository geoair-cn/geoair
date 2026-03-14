// package cn.geoair.map.dynamic.adv.query.dialect.pgback.base;
//
// import cn.geoair..base.log.GiLogger;
// import cn.geoair..base.log.GirLogger;
// import cn.geoair.map.dynamic.adv.query.IAdvBaseAccessOpt;
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
// import java.sql.PreparedStatement;
// import java.sql.SQLException;
// import java.util.*;
// import java.util.Collection;
// import java.util.stream.Collectors;
//
/// **
// * PostgreSQL数据库的插入操作实现类
// * <p>
// * 实现IAdvBaseAccessOpt接口，适配PostgreSQL的语法特性（如ON CONFLICT、批量插入语法），
// * 提供高性能、全场景的插入操作支持
// */
// public class PgAdvBaseAccessOpt implements IAdvBaseAccessOpt {
//
// IDataSourceGetter dataSourceGetter;
//
// public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {
// this.dataSourceGetter = dataSourceGetter;
// }
//
// private static final GiLogger log = GirLogger.getLoger(PgAdvBaseAccessOpt.class);
// // 默认分批插入批次大小
// private static final int DEFAULT_BATCH_SIZE = 1000;
// // PostgreSQL UPSERT冲突处理关键字
// private static final String PG_CONFLICT_CLAUSE = " ON CONFLICT DO ";
//
//
// // ==================== 自定义SQL插入 ====================
// @Override
// public Integer bInsertBySql(String sqlStatement) {
// return bInsertBySql(sqlStatement, SqlParamMap.of());
// }
//
// @Override
// public Integer bInsertBySql(String sqlStatement, SqlParamMap sqlParam) {
// if (StrUtil.isEmpty(sqlStatement)) {
// throw new IllegalArgumentException("插入SQL语句不能为空");
// }
//
// // 解析SQL（支持MyBatis标签）
// SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(cleanSql(sqlStatement), sqlParam);
// String execSql = sqlMeta.getSql();
// List<Object> jdbcParams = sqlMeta.getJdbcParamValues();
//
// Connection connection = dataSourceGetter.getConnection();
// try {
// log.info("schema:[{}] db:[{}] 执行自定义插入SQL：{}，参数：{}", dataSourceGetter.getSchemaName(),
// dataSourceGetter.getDataSourceId(), execSql, sqlParam);
// if (CollUtil.isEmpty(jdbcParams)) {
// // 无参数SQL
// return SqlExecutor.execute(connection, execSql);
// } else {
// // 带参数SQL
// return SqlExecutor.execute(connection, execSql, jdbcParams.toArray());
// }
// } catch (SQLException e) {
// throw new RuntimeException("执行自定义插入SQL失败，SQL：" + execSql, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// // ==================== 单条数据插入 ====================
// @Override
// public Integer bInsertOne(String tableName, Map<String, Object> rowData) {
// validateTableNameAndData(tableName, rowData);
//
// // 构建INSERT SQL
// String fields = String.join(",", rowData.keySet());
// String placeholders = rowData.keySet().stream()
// .map(key -> "?")
// .collect(Collectors.joining(","));
// String execSql = StrUtil.format("INSERT INTO {} ({}) VALUES ({})", tableName, fields,
// placeholders);
//
// List<Object> params = new ArrayList<>(rowData.values());
// Connection connection = dataSourceGetter.getConnection();
// try {
// log.info("schema:[{}] db:[{}] 执行单条插入，表名：{}，字段：{}", dataSourceGetter.getSchemaName(),
// dataSourceGetter.getDataSourceId(), tableName, fields);
// return SqlExecutor.execute(connection, execSql, params.toArray());
// } catch (SQLException e) {
// throw new RuntimeException("单条插入失败，表名：" + tableName, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public <T> Integer bInsertOne(String tableName, T entity) {
// validateTableName(tableName);
// if (entity == null) {
// throw new IllegalArgumentException("插入的实体对象不能为空");
// }
//
// // 将实体转换为Entity
// Entity entityObj = Entity.parse(entity);
// return bInsertOne(tableName, entityObj);
// }
//
// @Override
// public Long bInsertOneReturnId(String tableName, Map<String, Object> rowData) {
// validateTableNameAndData(tableName, rowData);
//
// String fields = String.join(",", rowData.keySet());
// String placeholders = rowData.keySet().stream()
// .map(key -> "?")
// .collect(Collectors.joining(","));
// // PostgreSQL返回自增主键语法：RETURNING id（默认主键字段为id，可根据实际调整）
// String execSql = StrUtil.format("INSERT INTO {} ({}) VALUES ({}) RETURNING id",
// tableName, fields, placeholders);
//
// List<Object> params = new ArrayList<>(rowData.values());
// Connection connection = dataSourceGetter.getConnection();
// try {
// log.info("schema:[{}] db:[{}] 执行单条插入并返回主键，表名：{}", dataSourceGetter.getSchemaName(),
// dataSourceGetter.getDataSourceId(), tableName);
// // 执行并获取主键
// return SqlExecutor.executeForGeneratedKey(connection, execSql,
// params.toArray()).longValue();
// } catch (SQLException e) {
// throw new RuntimeException("插入并返回主键失败，表名：" + tableName, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public <T> Long bInsertOneReturnId(String tableName, T entity) {
// validateTableName(tableName);
// if (entity == null) {
// throw new IllegalArgumentException("插入的实体对象不能为空");
// }
//
// Entity entityObj = Entity.parse(entity);
// return bInsertOneReturnId(tableName, entityObj);
// }
//
// // ==================== 批量数据插入 ====================
// @Override
// public Integer bInsertBatch(String tableName, Set<String> headers, List<Map<String,
// Object>> rowsData) {
// return bInsertBatchWithBatchSize(tableName, headers, rowsData, DEFAULT_BATCH_SIZE);
// }
//
// @Override
// public <T> Integer bInsertBatch(String tableName, Collection<T> entities) {
// return bInsertBatchWithBatchSize(tableName, entities, DEFAULT_BATCH_SIZE);
// }
//
// @Override
// public Integer bInsertBatchWithBatchSize(String tableName, Set<String> headers,
// List<Map<String, Object>> rowsData, int batchSize) {
// validateTableNameAndData(tableName, headers, rowsData);
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
// // 关闭自动提交，提升批量插入性能
// connection.setAutoCommit(false);
//
// // 构建批量插入SQL模板
// String fields = String.join(",", headers);
// String placeholders = headers.stream().map(key ->
// "?").collect(Collectors.joining(","));
// String execSql = StrUtil.format("INSERT INTO {} ({}) VALUES ({})", tableName, fields,
// placeholders);
//
// PreparedStatement pstmt = connection.prepareStatement(execSql);
//
// for (List<Map<String, Object>> batch : batches) {
// for (Map<String, Object> row : batch) {
// // 填充参数
// int paramIndex = 1;
// for (String header : headers) {
// pstmt.setObject(paramIndex++, row.get(header));
// }
// pstmt.addBatch();
// }
//
// // 执行批次
// int[] batchResults = pstmt.executeBatch();
// totalSuccess += Arrays.stream(batchResults).sum();
//
// // 清空批次
// pstmt.clearBatch();
// }
//
// connection.commit();
// log.info("schema:[{}] db:[{}] 批量插入完成，表名：{}，总条数：{}，批次大小：{}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(), tableName,
// totalSuccess, batchSize);
// return totalSuccess;
// } catch (SQLException e) {
// try {
// connection.rollback();
// } catch (SQLException rollbackEx) {
// log.error("批量插入回滚失败", rollbackEx);
// }
// throw new RuntimeException("批量插入失败，表名：" + tableName, e);
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
// public <T> Integer bInsertBatchWithBatchSize(String tableName, Collection<T> entities,
// int batchSize) {
// validateTableName(tableName);
// if (CollUtil.isEmpty(entities)) {
// return 0;
// }
// if (batchSize <= 0) {
// batchSize = DEFAULT_BATCH_SIZE;
// }
//
// // 将实体列表转换为Map列表
// List<Map<String, Object>> rowsData = entities.stream()
// .map(Entity::parse)
// .collect(Collectors.toList());
//
// // 提取字段名（取第一个实体的字段）
// Set<String> headers = rowsData.get(0).keySet();
//
// return bInsertBatchWithBatchSize(tableName, headers, rowsData, batchSize);
// }
//
// // ==================== 特殊场景插入 ====================
// @Override
// public Integer bInsertIgnore(String tableName, Map<String, Object> rowData) {
// validateTableNameAndData(tableName, rowData);
//
// // PostgreSQL ON CONFLICT DO NOTHING 语法
// String fields = String.join(",", rowData.keySet());
// String placeholders = rowData.keySet().stream().map(key ->
// "?").collect(Collectors.joining(","));
// String execSql = StrUtil.format("INSERT INTO {} ({}) VALUES ({}){}NOTHING",
// tableName, fields, placeholders, PG_CONFLICT_CLAUSE);
//
// List<Object> params = new ArrayList<>(rowData.values());
// Connection connection = dataSourceGetter.getConnection();
// try {
// log.info("schema:[{}] db:[{}] 执行插入忽略操作，表名：{}", dataSourceGetter.getSchemaName(),
// dataSourceGetter.getDataSourceId(), tableName);
// return SqlExecutor.execute(connection, execSql, params.toArray());
// } catch (SQLException e) {
// throw new RuntimeException("插入忽略操作失败，表名：" + tableName, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public Integer bInsertIgnoreBatch(String tableName, Set<String> headers,
// List<Map<String, Object>> rowsData) {
// validateTableNameAndData(tableName, headers, rowsData);
//
// // 拆分批次执行插入忽略
// List<List<Map<String, Object>>> batches = CollUtil.split(rowsData, DEFAULT_BATCH_SIZE);
// int totalSuccess = 0;
//
// for (List<Map<String, Object>> batch : batches) {
// for (Map<String, Object> row : batch) {
// totalSuccess += bInsertIgnore(tableName, row);
// }
// }
// return totalSuccess;
// }
//
// @Override
// public Integer bInsertOrUpdate(String tableName, Map<String, Object> rowData,
// Set<String> updateFields) {
// validateTableNameAndData(tableName, rowData);
//
// // 处理更新字段（为空则更新所有字段）
// Set<String> finalUpdateFields = CollUtil.isEmpty(updateFields)
// ? rowData.keySet()
// : updateFields;
//
// // 构建UPSERT SQL
// String fields = String.join(",", rowData.keySet());
// String placeholders = rowData.keySet().stream().map(key ->
// "?").collect(Collectors.joining(","));
// // 构建更新字段语句：field1 = EXCLUDED.field1, field2 = EXCLUDED.field2
// String updateClause = finalUpdateFields.stream()
// .map(field -> StrUtil.format("{} = EXCLUDED.{}", field, field))
// .collect(Collectors.joining(","));
//
// String execSql = StrUtil.format(
// "INSERT INTO {} ({}) VALUES ({}){}UPDATE SET {}",
// tableName, fields, placeholders, PG_CONFLICT_CLAUSE, updateClause
// );
//
// List<Object> params = new ArrayList<>(rowData.values());
// Connection connection = dataSourceGetter.getConnection();
// try {
// log.info("schema:[{}] db:[{}] 执行插入或更新操作，表名：{}，更新字段：{}",
// dataSourceGetter.getSchemaName(), dataSourceGetter.getDataSourceId(), tableName,
// updateClause);
// return SqlExecutor.execute(connection, execSql, params.toArray());
// } catch (SQLException e) {
// throw new RuntimeException("插入或更新操作失败，表名：" + tableName, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// // ==================== 私有工具方法 ====================
//
// /**
// * 清理SQL语句（移除末尾分号、多余空格）
// *
// * @param sql 原始SQL
// * @return 清理后的SQL
// */
// private String cleanSql(String sql) {
// if (StrUtil.isEmpty(sql)) {
// return sql;
// }
// return sql.replaceAll("\\s*;\\s*$", "").trim();
// }
//
// /**
// * 校验表名和单行数据
// */
// private void validateTableNameAndData(String tableName, Map<String, Object> rowData) {
// validateTableName(tableName);
// if (CollUtil.isEmpty(rowData)) {
// throw new IllegalArgumentException("插入的数据不能为空");
// }
// }
//
// /**
// * 校验表名、字段头和批量数据
// */
// private void validateTableNameAndData(String tableName, Set<String> headers,
// List<Map<String, Object>> rowsData) {
// validateTableName(tableName);
// if (CollUtil.isEmpty(headers)) {
// throw new IllegalArgumentException("插入的字段头不能为空");
// }
// if (CollUtil.isEmpty(rowsData)) {
// return; // 空数据直接返回0
// }
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
//
// }
