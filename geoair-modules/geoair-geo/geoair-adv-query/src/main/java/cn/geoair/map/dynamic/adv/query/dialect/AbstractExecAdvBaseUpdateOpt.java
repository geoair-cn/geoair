package cn.geoair.map.dynamic.adv.query.dialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.Collection;
import java.util.stream.Collectors;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.mybatis.SqlEngineUtil;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.sql.SqlExecutor;

/** 数据库更新操作抽象父类 封装所有数据库通用的更新逻辑，差异化语法由子类实现 */
public abstract class AbstractExecAdvBaseUpdateOpt implements IAdvBaseUpdateOpt {

	// 注入数据源获取器
	protected IDataSourceGetter dataSourceGetter;

	// 表名处理器（差异化）
	protected DialectTableNameProcessor dialectTableNameProcessor;
	// 日志实例
	protected static final GiLogger log = GirLogger.getLoger(AbstractExecAdvBaseUpdateOpt.class);

	protected static final int DEFAULT_BATCH_SIZE = 1000;

	@Override
	public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {
		this.dataSourceGetter = dataSourceGetter;
	}

	// ========== 通用逻辑：自定义SQL更新 ==========
	@Override
	public Integer bUpdateBySql(String sqlStatement) {
		return bUpdateBySql(sqlStatement, SqlParamMap.of());
	}

	@Override
	public Integer bUpdateBySql(String sqlStatement, SqlParamMap sqlParam) {
		// 通用参数校验
		if (StrUtil.isEmpty(sqlStatement)) {
			throw new IllegalArgumentException("更新SQL语句不能为空");
		}

		// 解析SQL（支持MyBatis标签）
		SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(cleanSql(sqlStatement), sqlParam);
		String execSql = sqlMeta.getSql();
		List<Object> jdbcParams = sqlMeta.getJdbcParamValues();

		Connection connection = dataSourceGetter.getConnection();
		try {
			log.debug("schema:[{}] db:[{}] 执行自定义更新SQL：{}，参数：{}", getSchemaName(), getDataSourceId(), execSql, sqlParam);

			// 通用执行逻辑
			if (CollUtil.isEmpty(jdbcParams)) {
				return SqlExecutor.execute(connection, execSql);
			}
			else {
				return SqlExecutor.execute(connection, execSql, jdbcParams.toArray());
			}
		}
		catch (SQLException e) {
			throw new RuntimeException("执行自定义更新SQL失败，SQL：" + execSql, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	// ========== 通用逻辑：单条数据更新（按主键） ==========
	@Override
	public Integer bUpdateByPrimaryKey(String tableName, String idKey, Object id, Map<String, Object> rowData) {
		// 通用参数校验
		validateTableName(tableName);
		validateIdKeyAndValue(idKey, id);
		validateUpdateData(rowData);
		String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
		String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
		String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
		// 构建SET子句（通用）
		String setClause = buildSetClause(rowData);
		// 差异化：构建按主键更新SQL
		String execSql = buildUpdateByPrimaryKeySql(quoteTableName, setClause, idKey);

		// 组装参数（更新字段值 + 主键值）
		List<Object> params = new ArrayList<>(rowData.values());
		params.add(id);

		Connection connection = dataSourceGetter.getConnection();
		try {
			log.debug("schema:[{}] db:[{}] 按主键更新，表名：{}，主键：{}={}，更新字段：{}", getSchemaName(), getDataSourceId(), tableName,
					idKey, id, rowData.keySet());

			return SqlExecutor.execute(connection, execSql, params.toArray());
		}
		catch (SQLException e) {
			throw new RuntimeException("按主键更新失败，表名：" + tableName + "，主键：" + idKey + "=" + id, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public <T> Integer bUpdateByPrimaryKey(String tableName, String idKey, T entity) {
		// 通用参数校验
		validateTableName(tableName);
		validateIdKey(idKey);
		if (entity == null) {
			throw new IllegalArgumentException("更新的实体对象不能为空");
		}

		// 通用实体转换为Map
		Entity entityObj = Entity.parse(entity);

		// 提取主键值
		Object id = entityObj.remove(idKey);
		if (id == null) {
			throw new IllegalArgumentException("实体对象中未找到主键字段[" + idKey + "]的值");
		}

		return bUpdateByPrimaryKey(tableName, idKey, id, entityObj);
	}

	// ========== 通用逻辑：条件更新 ==========
	@Override
	public Integer bUpdateByCondition(String tableName, Map<String, Object> rowData, Map<String, Object> whereMap) {
		// 通用参数校验
		validateTableName(tableName);
		validateUpdateData(rowData);
		if (CollUtil.isEmpty(whereMap)) {
			throw new IllegalArgumentException("更新条件不能为空（避免全表更新）");
		}
		String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
		String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
		String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
		// 构建SET子句和WHERE子句（通用）
		String setClause = buildSetClause(rowData);
		String whereClause = buildWhereClause(whereMap);
		// 差异化：构建条件更新SQL
		String execSql = buildUpdateByConditionSql(quoteTableName, setClause, whereClause);

		// 组装参数（更新字段值 + 条件值）
		List<Object> params = new ArrayList<>(rowData.values());
		params.addAll(whereMap.values());

		Connection connection = dataSourceGetter.getConnection();
		try {
			log.debug("schema:[{}] db:[{}] 条件更新，表名：{}，更新字段：{}，条件：{}", getSchemaName(), getDataSourceId(), tableName,
					rowData.keySet(), whereMap);

			return SqlExecutor.execute(connection, execSql, params.toArray());
		}
		catch (SQLException e) {
			throw new RuntimeException("条件更新失败，表名：" + tableName, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	// ========== 通用逻辑：批量更新（按主键） ==========
	@Override
	public Integer bUpdateBatchByPrimaryKey(String tableName, String idKey, List<Map<String, Object>> rowsData) {
		return bUpdateBatchWithBatchSize(tableName, idKey, rowsData, DEFAULT_BATCH_SIZE);
	}

	@Override
	public Integer bUpdateBatchWithBatchSize(String tableName, String idKey, List<Map<String, Object>> rowsData,
			int batchSize) {
		// 通用参数校验
		validateTableName(tableName);
		validateIdKey(idKey);
		if (CollUtil.isEmpty(rowsData)) {
			return 0;
		}
		if (batchSize <= 0) {
			batchSize = DEFAULT_BATCH_SIZE;
		}

		// 通用批次拆分
		List<List<Map<String, Object>>> batches = CollUtil.split(rowsData, batchSize);
		int totalSuccess = 0;

		Connection connection = dataSourceGetter.getConnection();
		try {
			// 通用批量优化：关闭自动提交
			connection.setAutoCommit(false);

			for (List<Map<String, Object>> batch : batches) {
				int batchSuccess = 0;
				for (Map<String, Object> row : batch) {
					// 提取主键值
					Object id = row.get(idKey);
					if (id == null) {
						throw new IllegalArgumentException("批量更新数据中缺少主键字段[" + idKey + "]的值");
					}

					// 移除主键字段（避免更新主键）
					Map<String, Object> updateData = new HashMap<>(row);
					updateData.remove(idKey);

					// 执行单条更新
					batchSuccess += bUpdateByPrimaryKey(tableName, idKey, id, updateData);
				}
				totalSuccess += batchSuccess;
			}

			connection.commit();
			log.debug("schema:[{}] db:[{}] 批量更新完成，表名：{}，总条数：{}，批次大小：{}", getSchemaName(), getDataSourceId(), tableName,
					totalSuccess, batchSize);
			return totalSuccess;
		}
		catch (SQLException e) {
			rollbackConnection(connection);
			throw new RuntimeException("批量更新失败，表名：" + tableName, e);
		}
		finally {
			restoreAutoCommit(connection);
			closeConnection(connection);
		}
	}

	@Override
	public <T> Integer bUpdateBatchByPrimaryKey(String tableName, String idKey, Collection<T> entities) {
		// 通用参数校验
		validateTableName(tableName);
		validateIdKey(idKey);
		if (CollUtil.isEmpty(entities)) {
			return 0;
		}

		// 通用实体转换为Map列表
		List<Map<String, Object>> rowsData = entities.stream().map(Entity::parse).collect(Collectors.toList());

		return bUpdateBatchByPrimaryKey(tableName, idKey, rowsData);
	}

	// ========== 通用逻辑：乐观锁更新 ==========
	@Override
	public Integer bUpdateWithOptimisticLock(String tableName, String idKey, Object id, Map<String, Object> rowData,
			String versionKey, Integer version) {
		// 通用参数校验
		validateTableName(tableName);
		validateIdKeyAndValue(idKey, id);
		validateUpdateData(rowData);
		validateIdKeyAndValue(versionKey, version);
		String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
		String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
		String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
		// 构建SET子句（差异化：版本号自增逻辑）
		String setClause = buildOptimisticLockSetClause(rowData, versionKey);
		// 差异化：构建乐观锁更新SQL
		String execSql = buildUpdateWithOptimisticLockSql(quoteTableName, setClause, idKey, versionKey);

		// 组装参数（更新字段值 + 主键值 + 版本号）
		List<Object> params = new ArrayList<>(rowData.values());
		params.add(id);
		params.add(version);

		Connection connection = dataSourceGetter.getConnection();
		try {
			log.debug("schema:[{}] db:[{}] 乐观锁更新，表名：{}，主键：{}={}，版本号：{}={}", getSchemaName(), getDataSourceId(),
					tableName, idKey, id, versionKey, version);

			return SqlExecutor.execute(connection, execSql, params.toArray());
		}
		catch (SQLException e) {
			throw new RuntimeException("乐观锁更新失败，表名：" + tableName + "，主键：" + idKey + "=" + id, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	// ========== 通用逻辑：更新或插入（UPSERT） ==========
	@Override
	public Integer bUpdateOrInsert(String tableName, Map<String, Object> rowData, Set<String> conflictKeys) {
		// 通用参数校验
		validateTableName(tableName);
		validateUpdateData(rowData);
		if (CollUtil.isEmpty(conflictKeys)) {
			throw new IllegalArgumentException("冲突判定字段不能为空");
		}
		String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
		String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
		String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
		// 通用字段/占位符构建
		String fields = String.join(",", rowData.keySet());
		String placeholders = rowData.keySet().stream().map(key -> "?").collect(Collectors.joining(","));
		String conflictFields = String.join(",", conflictKeys);

		// 构建更新子句（冲突时更新非冲突字段）
		String updateClause = buildUpsertUpdateClause(rowData, conflictKeys);
		// 差异化：构建UPSERT SQL
		String execSql = buildUpdateOrInsertSql(quoteTableName, fields, placeholders, conflictFields, updateClause);

		List<Object> params = new ArrayList<>(rowData.values());
		Connection connection = dataSourceGetter.getConnection();
		try {
			log.debug("schema:[{}] db:[{}] 更新或插入，表名：{}，冲突字段：{}，更新字段：{}", getSchemaName(), getDataSourceId(), tableName,
					conflictFields, updateClause);

			return SqlExecutor.execute(connection, execSql, params.toArray());
		}
		catch (SQLException e) {
			throw new RuntimeException("更新或插入失败，表名：" + tableName, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	// ========== 通用工具方法（子类无需重写） ==========

	/** 清理SQL语句（移除末尾分号、多余空格） */
	protected String cleanSql(String sql) {
		if (StrUtil.isEmpty(sql)) {
			return sql;
		}
		return sql.replaceAll("\\s*;\\s*$", "").trim();
	}

	/** 校验表名 */
	protected void validateTableName(String tableName) {
		if (StrUtil.isEmpty(tableName)) {
			throw new IllegalArgumentException("表名不能为空");
		}
	}

	/** 校验主键字段名 */
	protected void validateIdKey(String idKey) {
		if (StrUtil.isEmpty(idKey)) {
			throw new IllegalArgumentException("主键字段名不能为空");
		}
	}

	/** 校验主键字段名和值 */
	protected void validateIdKeyAndValue(String idKey, Object id) {
		validateIdKey(idKey);
		if (id == null) {
			throw new IllegalArgumentException("主键值不能为空");
		}
	}

	/** 校验更新数据（不能为空） */
	protected void validateUpdateData(Map<String, Object> rowData) {
		if (CollUtil.isEmpty(rowData)) {
			throw new IllegalArgumentException("更新的数据不能为空");
		}
	}

	/** 构建SET子句（通用：field1 = ?, field2 = ?） */
	protected String buildSetClause(Map<String, Object> rowData) {
		return rowData.keySet().stream().map(field -> StrUtil.format("{} = ?", field)).collect(Collectors.joining(","));
	}

	/** 构建WHERE子句（通用：field1 = ? AND field2 = ?） */
	protected String buildWhereClause(Map<String, Object> whereMap) {
		return whereMap.keySet().stream().map(field -> StrUtil.format("{} = ?", field))
				.collect(Collectors.joining(" AND "));
	}

	/** 构建UPSERT更新子句（通用：过滤冲突字段） */
	protected String buildUpsertUpdateClause(Map<String, Object> rowData, Set<String> conflictKeys) {
		return rowData.keySet().stream().filter(field -> !conflictKeys.contains(field)) // 冲突字段不更新
				.map(this::buildUpsertFieldClause) // 差异化：字段更新语法
				.collect(Collectors.joining(","));
	}

	/** 获取Schema/库名（通用封装） */
	protected String getSchemaName() {
		return dataSourceGetter != null ? dataSourceGetter.getSchemaName() : "";
	}

	/** 获取数据源ID（通用封装） */
	protected String getDataSourceId() {
		return dataSourceGetter != null ? dataSourceGetter.getDataSourceId() : "";
	}

	/** 关闭连接（通用封装） */
	protected void closeConnection(Connection connection) {
		if (dataSourceGetter != null) {
			dataSourceGetter.connectionClose(connection);
		}
	}

	/** 回滚连接（通用封装） */
	protected void rollbackConnection(Connection connection) {
		if (connection != null) {
			try {
				connection.rollback();
			}
			catch (SQLException e) {
				log.error("更新操作回滚失败", e);
			}
		}
	}

	/** 恢复自动提交（通用封装） */
	protected void restoreAutoCommit(Connection connection) {
		if (connection != null) {
			try {
				connection.setAutoCommit(true);
			}
			catch (SQLException e) {
				log.error("恢复自动提交失败", e);
			}
		}
	}

	/** 构建按主键更新SQL */
	protected String buildUpdateByPrimaryKeySql(String tableName, String setClause, String idKey) {
		// PG：基础按主键更新语法
		return StrUtil.format("UPDATE {} SET {} WHERE {} = ?", tableName, setClause, idKey);
	}

	/** 构建条件更新SQL */
	protected String buildUpdateByConditionSql(String tableName, String setClause, String whereClause) {
		return StrUtil.format("UPDATE {} SET {} WHERE {}", tableName, setClause, whereClause);
	}

	/** 构建乐观锁SET子句（版本号自增逻辑） */
	protected String buildOptimisticLockSetClause(Map<String, Object> rowData, String versionKey) {
		// PG：版本号自增（version = version + 1）
		return rowData.keySet().stream().map(field -> {
			if (field.equals(versionKey)) {
				return StrUtil.format("{} = {} + 1", versionKey, versionKey);
			}
			return StrUtil.format("{} = ?", field);
		}).collect(Collectors.joining(","));
	}

	/** 构建乐观锁更新SQL */
	protected String buildUpdateWithOptimisticLockSql(String tableName, String setClause, String idKey,
			String versionKey) {
		// ：乐观锁更新语法（WHERE 主键 = ? AND 版本号 = ?）
		return StrUtil.format("UPDATE {} SET {} WHERE {} = ? AND {} = ?", tableName, setClause, idKey, versionKey);
	}

	/** 构建UPSERT字段更新子句（PG：EXCLUDED，MySQL：VALUES） */
	protected abstract String buildUpsertFieldClause(String field);

	/** 构建更新或插入（UPSERT）SQL */
	protected abstract String buildUpdateOrInsertSql(String tableName, String fields, String placeholders,
			String conflictFields, String updateClause);

}
