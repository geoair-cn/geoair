package cn.geoair.map.dynamic.adv.query.dialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.mybatis.SqlEngineUtil;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.handler.StreamBeanRsHandler;
import cn.geoair.map.dynamic.adv.query.handler.StreamRsHandler;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.handler.*;
import cn.hutool.db.sql.SqlExecutor;

/**
 * 数据库查询操作抽象父类 封装所有数据库通用的查询逻辑，差异化语法由子类实现
 */
public abstract class AbstractExecAdvBaseSelectOpt implements IAdvBaseSelectOpt {

	// 注入数据源获取器
	protected IDataSourceGetter dataSourceGetter;

	// 表名处理器（差异化）
	protected DialectTableNameProcessor dialectTableNameProcessor;

	private static final String COUNT_ALIAS_PREFIX = "count_query_";

	// 日志实例
	protected static final GiLogger log = GirLogger.getLoger(AbstractExecAdvBaseSelectOpt.class);

	@Override
	public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {
		this.dataSourceGetter = dataSourceGetter;
	}

	// ========== 通用逻辑：无参数查询 ==========
	@Override
	public GirAdvOneRow bSelectOne(String sql) {
		Connection connection = dataSourceGetter.getConnection();
		try {
			String cleanSql = cleanQuerySql(sql);
			// 差异化：构建单条查询包装SQL
			String execSql = buildSelectOneWrapSql(cleanSql);

			logExecuteSql("bSelectOne", execSql);

			Entity queryResult = SqlExecutor.query(connection, execSql, new EntityHandler());
			return GirAdvOneRow.ofByEntity(queryResult);
		}
		catch (SQLException e) {
			throw new RuntimeException("执行bSelectOne查询失败，SQL：" + sql, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public List<GirAdvOneRow> bSelectList(String sql) {
		Connection connection = dataSourceGetter.getConnection();
		try {
			String cleanSql = cleanQuerySql(sql);
			logExecuteSql("bSelectList", cleanSql);

			List<Entity> queryResult = SqlExecutor.query(connection, cleanSql, new EntityListHandler());
			return GirAdvOneRow.ofByEntityList(queryResult);
		}
		catch (SQLException e) {
			throw new RuntimeException("执行bSelectList查询失败，SQL：" + sql, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public void bSelectList(String sql, Consumer<GirAdvOneRow> rowConsumer) {
		Connection connection = dataSourceGetter.getConnection();
		try {
			String cleanSql = cleanQuerySql(sql);
			logExecuteSql("bSelectList(流式)", cleanSql);

			SqlExecutor.query(connection, cleanSql, new StreamRsHandler(rowConsumer));
		}
		catch (SQLException e) {
			throw new RuntimeException("执行流式bSelectList查询失败，SQL：" + sql, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public List<List<Object>> bSelectListToValueList(String sql) {
		Connection connection = dataSourceGetter.getConnection();
		try {
			String cleanSql = cleanQuerySql(sql);
			logExecuteSql("bSelectListToValueList", cleanSql);

			return SqlExecutor.query(connection, cleanSql, new ValueListHandler());
		}
		catch (SQLException e) {
			throw new RuntimeException("执行bSelectListToValueList查询失败，SQL：" + sql, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public Number bSelectNumber(String sql) {
		Connection connection = dataSourceGetter.getConnection();
		try {
			String cleanSql = cleanQuerySql(sql);
			logExecuteSql("bSelectNumber", cleanSql);

			return SqlExecutor.query(connection, cleanSql, new NumberHandler());
		}
		catch (SQLException e) {
			throw new RuntimeException("执行bSelectNumber查询失败，SQL：" + sql, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public Number bSelectRecordRowCount(String sql) {
		String cleanSql = cleanQuerySql(sql);
		// 差异化：构建COUNT查询SQL
		String countSql = buildCountQuerySql(cleanSql);
		logExecuteSql("bSelectRecordRowCount", countSql);
		return bSelectNumber(countSql);
	}

	@Override
	public <E> E bSelectObjOne(String sql, Class<E> clazz) {
		Connection connection = dataSourceGetter.getConnection();
		try {
			String cleanSql = cleanQuerySql(sql);
			logExecuteSql("bSelectObjOne", cleanSql);

			Object queryResult = SqlExecutor.query(connection, cleanSql, BeanHandler.create(clazz));
			return (E) queryResult;
		}
		catch (SQLException e) {
			throw new RuntimeException("执行bSelectObjOne查询失败，SQL：" + sql + "，目标类型：" + clazz.getName(), e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public <E> List<E> bSelectObjList(String sql, Class<E> clazz) {
		Connection connection = dataSourceGetter.getConnection();
		try {
			String cleanSql = cleanQuerySql(sql);
			logExecuteSql("bSelectObjList", cleanSql);

			return SqlExecutor.query(connection, cleanSql, BeanListHandler.create(clazz));
		}
		catch (SQLException e) {
			throw new RuntimeException("执行bSelectObjList查询失败，SQL：" + sql + "，目标类型：" + clazz.getName(), e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public <E> void bSelectObjList(String sql, Class<E> clazz, Consumer<E> rowConsumer) {
		Connection connection = dataSourceGetter.getConnection();
		try {
			String cleanSql = cleanQuerySql(sql);
			logExecuteSql("bSelectObjList(流式)", cleanSql);

			SqlExecutor.query(connection, cleanSql, new StreamBeanRsHandler<>(rowConsumer, clazz));
		}
		catch (SQLException e) {
			throw new RuntimeException("执行流式bSelectObjList查询失败，SQL：" + sql + "，目标类型：" + clazz.getName(), e);
		}
		finally {
			closeConnection(connection);
		}
	}

	// ========== 通用逻辑：带参数查询 ==========
	@Override
	public GirAdvOneRow bSelectOne(String sqlStatement, SqlParamMap sqlParam) {
		SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
		Connection connection = dataSourceGetter.getConnection();
		try {
			// 差异化：构建单条查询包装SQL
			String execSql = buildSelectOneWrapSql(sqlMeta.getSql());
			logExecuteSql("bSelectOne(带参数)", execSql, sqlParam);

			Entity queryResult = SqlExecutor.query(connection, execSql, new EntityHandler(),
					sqlMeta.getJdbcParamValues().toArray());
			return GirAdvOneRow.ofByEntity(queryResult);
		}
		catch (SQLException e) {
			throw new RuntimeException("执行带参数bSelectOne查询失败，SQL：" + sqlStatement, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public List<GirAdvOneRow> bSelectList(String sqlStatement, SqlParamMap sqlParam) {
		SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
		Connection connection = dataSourceGetter.getConnection();
		try {
			logExecuteSql("bSelectList(带参数)", sqlMeta.getSql(), sqlParam);

			List<Entity> queryResult = SqlExecutor.query(connection, sqlMeta.getSql(), new EntityListHandler(),
					sqlMeta.getJdbcParamValues().toArray());
			return GirAdvOneRow.ofByEntityList(queryResult);
		}
		catch (SQLException e) {
			throw new RuntimeException("执行带参数bSelectList查询失败，SQL：" + sqlStatement, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public void bSelectList(String sqlStatement, SqlParamMap sqlParam, Consumer<GirAdvOneRow> rowConsumer) {
		SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
		Connection connection = dataSourceGetter.getConnection();
		try {
			logExecuteSql("bSelectList(带参数-流式)", sqlMeta.getSql(), sqlParam);

			SqlExecutor.query(connection, sqlMeta.getSql(), new StreamRsHandler(rowConsumer),
					sqlMeta.getJdbcParamValues().toArray());
		}
		catch (SQLException e) {
			throw new RuntimeException("执行带参数流式bSelectList查询失败，SQL：" + sqlStatement, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public List<List<Object>> bSelectListToValueList(String sqlStatement, SqlParamMap sqlParam) {
		SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
		Connection connection = dataSourceGetter.getConnection();
		try {
			logExecuteSql("bSelectListToValueList(带参数)", sqlMeta.getSql(), sqlParam);

			return SqlExecutor.query(connection, sqlMeta.getSql(), new ValueListHandler(),
					sqlMeta.getJdbcParamValues().toArray());
		}
		catch (SQLException e) {
			throw new RuntimeException("执行带参数bSelectListToValueList查询失败，SQL：" + sqlStatement, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public Number bSelectNumber(String sqlStatement, SqlParamMap sqlParam) {
		SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
		Connection connection = dataSourceGetter.getConnection();
		try {
			logExecuteSql("bSelectNumber(带参数)", sqlMeta.getSql(), sqlParam);

			return SqlExecutor.query(connection, sqlMeta.getSql(), new NumberHandler(),
					sqlMeta.getJdbcParamValues().toArray());
		}
		catch (SQLException e) {
			throw new RuntimeException("执行带参数bSelectNumber查询失败，SQL：" + sqlStatement, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public Number bSelectRecordRowCount(String sqlStatement, SqlParamMap sqlParam) {
		SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
		// 差异化：构建COUNT查询SQL
		String countSql = buildCountQuerySql(sqlMeta.getSql());
		SqlMeta countSqlMeta = new SqlMeta(countSql, sqlMeta.getJdbcParamValues());
		logExecuteSql("bSelectRecordRowCount(带参数)", countSql, sqlParam);

		Connection connection = dataSourceGetter.getConnection();
		try {
			return SqlExecutor.query(connection, countSqlMeta.getSql(), new NumberHandler(),
					countSqlMeta.getJdbcParamValues().toArray());
		}
		catch (SQLException e) {
			throw new RuntimeException("执行带参数bSelectRecordRowCount查询失败，SQL：" + sqlStatement, e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public <E> E bSelectObjOne(String sqlStatement, SqlParamMap sqlParam, Class<E> clazz) {
		SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
		Connection connection = dataSourceGetter.getConnection();
		try {
			logExecuteSql("bSelectObjOne(带参数)", sqlMeta.getSql(), sqlParam);

			Object queryResult = SqlExecutor.query(connection, sqlMeta.getSql(), BeanHandler.create(clazz),
					sqlMeta.getJdbcParamValues().toArray());
			return (E) queryResult;
		}
		catch (SQLException e) {
			throw new RuntimeException("执行带参数bSelectObjOne查询失败，SQL：" + sqlStatement + "，目标类型：" + clazz.getName(), e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public <E> List<E> bSelectObjList(String sqlStatement, SqlParamMap sqlParam, Class<E> clazz) {
		SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
		Connection connection = dataSourceGetter.getConnection();
		try {
			logExecuteSql("bSelectObjList(带参数)", sqlMeta.getSql(), sqlParam);

			return SqlExecutor.query(connection, sqlMeta.getSql(), BeanListHandler.create(clazz),
					sqlMeta.getJdbcParamValues().toArray());
		}
		catch (SQLException e) {
			throw new RuntimeException("执行带参数bSelectObjList查询失败，SQL：" + sqlStatement + "，目标类型：" + clazz.getName(), e);
		}
		finally {
			closeConnection(connection);
		}
	}

	@Override
	public <E> void bSelectObjList(String sqlStatement, SqlParamMap sqlParam, Class<E> clazz, Consumer<E> rowConsumer) {
		SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
		Connection connection = dataSourceGetter.getConnection();
		try {
			logExecuteSql("bSelectObjList(带参数-流式)", sqlMeta.getSql(), sqlParam);

			SqlExecutor.query(connection, sqlMeta.getSql(), new StreamBeanRsHandler<>(rowConsumer, clazz),
					sqlMeta.getJdbcParamValues().toArray());
		}
		catch (SQLException e) {
			throw new RuntimeException("执行带参数流式bSelectObjList查询失败，SQL：" + sqlStatement + "，目标类型：" + clazz.getName(), e);
		}
		finally {
			closeConnection(connection);
		}
	}

	// ========== 通用工具方法（子类无需重写） ==========

	/**
	 * 解析带参数的SQL语句，生成可执行的SQL和参数列表
	 */
	protected SqlMeta parseSqlWithParam(String sqlStatement, SqlParamMap sqlParam) {
		if (StrUtil.isEmpty(sqlStatement)) {
			throw new IllegalArgumentException("SQL语句不能为空");
		}
		String cleanSql = cleanQuerySql(sqlStatement);
		return SqlEngineUtil.getEngine().parse(cleanSql, sqlParam);
	}

	/**
	 * 清理查询SQL（移除多余空格，子类可扩展）
	 */
	protected String cleanQuerySql(String sql) {
		if (dialectTableNameProcessor != null) {
			return dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
		}
		return StrUtil.trim(sql);
	}

	/**
	 * 统一打印SQL执行日志（无参数）
	 */
	protected void logExecuteSql(String methodName, String sql) {
		log.debug("schema:[{}] db:[{}] {}执行的SQL为：{}", getSchemaName(), getDatabaseName(), methodName, sql);
	}

	/**
	 * 统一打印带参数的SQL执行日志
	 */
	protected void logExecuteSql(String methodName, String sql, SqlParamMap sqlParam) {
		log.debug("schema:[{}] db:[{}] {}执行的SQL为：{}，参数：{}", getSchemaName(), getDatabaseName(), methodName, sql,
				sqlParam);
	}

	/**
	 * 获取Schema/库名（通用封装）
	 */
	protected String getSchemaName() {
		return dataSourceGetter != null
				? GutilObject.isEmpty(dataSourceGetter.getSchemaName()) ? "" : dataSourceGetter.getSchemaName() : "";
	}

	/**
	 * 获取数据源ID（通用封装）
	 */
	protected String getDataSourceId() {
		return dataSourceGetter != null
				? GutilObject.isEmpty(dataSourceGetter.getDataSourceId()) ? "" : dataSourceGetter.getDataSourceId()
				: "";
	}

	/**
	 * 获取数据库名称（通用封装）
	 */
	protected String getDatabaseName() {
		return dataSourceGetter != null
				? GutilObject.isEmpty(dataSourceGetter.getDatabaseName()) ? "" : dataSourceGetter.getDatabaseName()
				: "";
	}

	/**
	 * 关闭连接（通用封装）
	 */
	protected void closeConnection(Connection connection) {
		if (dataSourceGetter != null) {
			dataSourceGetter.connectionClose(connection);
		}
	}

	// ========== 差异化抽象方法（子类必须实现） ==========

	protected String buildSelectOneWrapSql(String cleanSql) {
		return StrUtil.format("select * from ({}) as {} limit 1", cleanSql,
				dialectTableNameProcessor.tbGetTempAliasTableName());
	}

	protected String buildCountQuerySql(String cleanSql) {
		// PG：生成唯一临时表别名（避免冲突）
		String tempAlias = COUNT_ALIAS_PREFIX + IdUtil.simpleUUID().substring(0, 8);
		return StrUtil.format("SELECT COUNT(1) FROM ({}) AS {}", cleanSql, tempAlias);
	}

}
