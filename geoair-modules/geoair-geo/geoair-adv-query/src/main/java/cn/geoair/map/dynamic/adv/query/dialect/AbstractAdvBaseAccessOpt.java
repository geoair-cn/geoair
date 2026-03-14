package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.map.dynamic.adv.mybatis.SqlEngineUtil;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.IAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.sql.SqlExecutor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.Collection;
import java.util.stream.Collectors;

/** 数据库插入操作抽象父类 封装所有数据库通用的插入逻辑，差异化语法由子类实现 */
public abstract class AbstractAdvBaseAccessOpt implements IAdvBaseAccessOpt {

	protected IDataSourceGetter dataSourceGetter;

	protected static final GiLogger log = GirLogger.getLoger(AbstractAdvBaseAccessOpt.class);

	// 默认分批插入批次大小（通用常量）
	protected static final int DEFAULT_BATCH_SIZE = 1000;

	/** 构建带主键返回的插入SQL */
	protected abstract String buildInsertReturnIdSql(String tableName, String fields, String placeholders);

	/** 执行插入并返回主键 */
	protected abstract Long executeInsertReturnId(Connection connection, String execSql, Object... params)
			throws SQLException;

	/** 构建插入忽略的SQL（PG：ON CONFLICT DO NOTHING；MySQL：INSERT IGNORE） */
	protected abstract String buildInsertIgnoreSql(String tableName, String fields, String placeholders);

	/** 构建插入或更新的SQL（PG：ON CONFLICT DO UPDATE；MySQL：ON DUPLICATE KEY UPDATE） */
	protected abstract String buildInsertOrUpdateSql(String tableName, String fields, String placeholders,
			Set<String> updateFields);

	@Override
	public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {
		this.dataSourceGetter = dataSourceGetter;
	}

	// ========== 通用逻辑：自定义SQL插入 ==========
	@Override
	public Integer bInsertBySql(String sqlStatement) {
		return bInsertBySql(sqlStatement, SqlParamMap.of());
	}

	@Override
	public Integer bInsertBySql(String sqlStatement, SqlParamMap sqlParam) {
		// 通用参数校验
		if (StrUtil.isEmpty(sqlStatement)) {
			throw new IllegalArgumentException("插入SQL语句不能为空");
		}

		// 解析SQL（支持MyBatis标签）
		SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(cleanSql(sqlStatement), sqlParam);
		String execSql = sqlMeta.getSql();
		List<Object> jdbcParams = sqlMeta.getJdbcParamValues();

		Connection connection = dataSourceGetter.getConnection();
		try {
			log.debug("schema:[{}] db:[{}] 执行自定义插入SQL：{}，参数：{}", getSchemaName(), getDataSourceId(), execSql, sqlParam);
			// 通用执行逻辑
			if (CollUtil.isEmpty(jdbcParams)) {
				return SqlExecutor.execute(connection, execSql);
			}
			else {
				return SqlExecutor.execute(connection, execSql, jdbcParams.toArray());
			}
		}
		catch (SQLException e) {
			throw new RuntimeException("执行自定义插入SQL失败，SQL：" + execSql, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	// ========== 通用逻辑：单条数据插入 ==========
	@Override
	public Integer bInsertOne(String tableName, Map<String, Object> rowData) {
		// 通用校验
		validateTableNameAndData(tableName, rowData);
		String fields = String.join(",", rowData.keySet());
		String placeholders = buildPlaceholders(rowData.keySet().size());
		String execSql = buildInsertSql(tableName, fields, placeholders);

		List<Object> params = new ArrayList<>(rowData.values());
		Connection connection = dataSourceGetter.getConnection();
		try {
			log.debug("schema:[{}] db:[{}] 执行单条插入，表名：{}，字段：{}", getSchemaName(), getDataSourceId(), tableName, fields);
			return SqlExecutor.execute(connection, execSql, params.toArray());
		}
		catch (SQLException e) {
			throw new RuntimeException("单条插入失败，表名：" + tableName, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public <T> Integer bInsertOne(String tableName, T entity) {
		validateTableName(tableName);
		if (entity == null) {
			throw new IllegalArgumentException("插入的实体对象不能为空");
		}

		// 通用实体转Map
		Entity entityObj = Entity.parse(entity);
		return bInsertOne(tableName, entityObj);
	}

	// ========== 通用逻辑：单条插入返回主键 ==========
	@Override
	public Long bInsertOneReturnId(String tableName, Map<String, Object> rowData) {
		validateTableNameAndData(tableName, rowData);

		String fields = String.join(",", rowData.keySet());
		String placeholders = buildPlaceholders(rowData.keySet().size());
		// 差异化：构建带主键返回的SQL（子类实现）
		String execSql = buildInsertReturnIdSql(tableName, fields, placeholders);

		List<Object> params = new ArrayList<>(rowData.values());
		Connection connection = dataSourceGetter.getConnection();
		try {
			log.debug("schema:[{}] db:[{}] 执行单条插入并返回主键，表名：{}", getSchemaName(), getDataSourceId(), tableName);
			// 通用执行并返回主键
			return executeInsertReturnId(connection, execSql, params.toArray());
		}
		catch (SQLException e) {
			throw new RuntimeException("插入并返回主键失败，表名：" + tableName, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public <T> Long bInsertOneReturnId(String tableName, T entity) {
		validateTableName(tableName);
		if (entity == null) {
			throw new IllegalArgumentException("插入的实体对象不能为空");
		}

		Entity entityObj = Entity.parse(entity);
		return bInsertOneReturnId(tableName, entityObj);
	}

	// ========== 通用逻辑：批量插入 ==========
	@Override
	public Integer bInsertBatch(String tableName, Set<String> headers, List<Map<String, Object>> rowsData) {
		return bInsertBatchWithBatchSize(tableName, headers, rowsData, DEFAULT_BATCH_SIZE);
	}

	@Override
	public <T> Integer bInsertBatch(String tableName, Collection<T> entities) {
		return bInsertBatchWithBatchSize(tableName, entities, DEFAULT_BATCH_SIZE);
	}

	@Override
	public Integer bInsertBatchWithBatchSize(String tableName, Set<String> headers, List<Map<String, Object>> rowsData,
			int batchSize) {
		validateTableNameAndData(tableName, headers, rowsData);
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

			// 构建通用批量插入模板
			String fields = String.join(",", headers);
			String placeholders = buildPlaceholders(headers.size());
			String execSql = buildInsertSql(tableName, fields, placeholders);

			PreparedStatement pstmt = connection.prepareStatement(execSql);

			for (List<Map<String, Object>> batch : batches) {
				// 通用参数填充
				for (Map<String, Object> row : batch) {
					int paramIndex = 1;
					for (String header : headers) {
						pstmt.setObject(paramIndex++, row.get(header));
					}
					pstmt.addBatch();
				}

				// 通用批次执行
				int[] batchResults = pstmt.executeBatch();
				totalSuccess += Arrays.stream(batchResults).sum();
				pstmt.clearBatch();
			}

			connection.commit();
			log.debug("schema:[{}] db:[{}] 批量插入完成，表名：{}，总条数：{}，批次大小：{}", getSchemaName(), getDataSourceId(), tableName,
					totalSuccess, batchSize);
			return totalSuccess;
		}
		catch (SQLException e) {
			// 通用回滚逻辑
			rollbackConnection(connection);
			throw new RuntimeException("批量插入失败，表名：" + tableName, e);
		}
		finally {
			// 通用恢复自动提交
			restoreAutoCommit(connection);
			closeConnection(connection);
		}
	}

	@Override
	public <T> Integer bInsertBatchWithBatchSize(String tableName, Collection<T> entities, int batchSize) {
		validateTableName(tableName);
		if (CollUtil.isEmpty(entities)) {
			return 0;
		}
		if (batchSize <= 0) {
			batchSize = DEFAULT_BATCH_SIZE;
		}

		// 通用实体转Map列表
		List<Map<String, Object>> rowsData = entities.stream().map(Entity::parse).collect(Collectors.toList());

		// 通用提取字段头
		Set<String> headers = rowsData.get(0).keySet();

		return bInsertBatchWithBatchSize(tableName, headers, rowsData, batchSize);
	}

	// ========== 通用逻辑：插入忽略 ==========
	@Override
	public Integer bInsertIgnore(String tableName, Map<String, Object> rowData) {
		validateTableNameAndData(tableName, rowData);

		String fields = String.join(",", rowData.keySet());
		String placeholders = buildPlaceholders(rowData.keySet().size());
		// 差异化：构建插入忽略SQL（子类实现）
		String execSql = buildInsertIgnoreSql(tableName, fields, placeholders);

		List<Object> params = new ArrayList<>(rowData.values());
		Connection connection = dataSourceGetter.getConnection();
		try {
			log.debug("schema:[{}] db:[{}] 执行插入忽略操作，表名：{}", getSchemaName(), getDataSourceId(), tableName);
			return SqlExecutor.execute(connection, execSql, params.toArray());
		}
		catch (SQLException e) {
			throw new RuntimeException("插入忽略操作失败，表名：" + tableName, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public Integer bInsertIgnoreBatch(String tableName, Set<String> headers, List<Map<String, Object>> rowsData) {
		validateTableNameAndData(tableName, headers, rowsData);

		// 通用批次拆分执行
		List<List<Map<String, Object>>> batches = CollUtil.split(rowsData, DEFAULT_BATCH_SIZE);
		int totalSuccess = 0;

		for (List<Map<String, Object>> batch : batches) {
			for (Map<String, Object> row : batch) {
				totalSuccess += bInsertIgnore(tableName, row);
			}
		}
		return totalSuccess;
	}

	// ========== 通用逻辑：插入或更新（UPSERT） ==========
	@Override
	public Integer bInsertOrUpdate(String tableName, Map<String, Object> rowData, Set<String> updateFields) {
		validateTableNameAndData(tableName, rowData);

		// 通用更新字段处理
		Set<String> finalUpdateFields = CollUtil.isEmpty(updateFields) ? rowData.keySet() : updateFields;

		String fields = String.join(",", rowData.keySet());
		String placeholders = buildPlaceholders(rowData.keySet().size());
		// 差异化：构建UPSERT SQL（子类实现）
		String execSql = buildInsertOrUpdateSql(tableName, fields, placeholders, finalUpdateFields);

		List<Object> params = new ArrayList<>(rowData.values());
		Connection connection = dataSourceGetter.getConnection();
		try {
			log.debug("schema:[{}] db:[{}] 执行插入或更新操作，表名：{}，更新字段：{}", getSchemaName(), getDataSourceId(), tableName,
					String.join(",", finalUpdateFields));
			return SqlExecutor.execute(connection, execSql, params.toArray());
		}
		catch (SQLException e) {
			throw new RuntimeException("插入或更新操作失败，表名：" + tableName, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	/** 清理SQL语句（移除末尾分号、多余空格） */
	protected String cleanSql(String sql) {
		if (StrUtil.isEmpty(sql)) {
			return sql;
		}
		return sql.replaceAll("\\s*;\\s*$", "").trim();
	}

	/** 构建占位符（?,?,?） */
	protected String buildPlaceholders(int count) {
		String repeat = StrUtil.repeatAndJoin("? ", count, ",");
		return repeat;
	}

	/** 构建基础INSERT SQL */
	protected String buildInsertSql(String tableName, String fields, String placeholders) {
		return StrUtil.format("INSERT INTO {} ({}) VALUES ({})", tableName, fields, placeholders);
	}

	/** 校验表名和单行数据 */
	protected void validateTableNameAndData(String tableName, Map<String, Object> rowData) {
		validateTableName(tableName);
		if (CollUtil.isEmpty(rowData)) {
			throw new IllegalArgumentException("插入的数据不能为空");
		}
	}

	/** 校验表名、字段头和批量数据 */
	protected void validateTableNameAndData(String tableName, Set<String> headers, List<Map<String, Object>> rowsData) {
		validateTableName(tableName);
		if (CollUtil.isEmpty(headers)) {
			throw new IllegalArgumentException("插入的字段头不能为空");
		}
		if (CollUtil.isEmpty(rowsData)) {
			return; // 空数据直接返回0
		}
	}

	/** 校验表名 */
	protected void validateTableName(String tableName) {
		if (StrUtil.isEmpty(tableName)) {
			throw new IllegalArgumentException("表名不能为空");
		}
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
				log.error("批量插入回滚失败", e);
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

}
