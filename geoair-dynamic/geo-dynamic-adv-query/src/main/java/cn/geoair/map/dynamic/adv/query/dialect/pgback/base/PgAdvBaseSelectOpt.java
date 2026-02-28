// package cn.geoair.map.dynamic.adv.query.dialect.pgback.base;
//
// import cn.geoair.gtc.base.log.GiLogger;
// import cn.geoair.gtc.base.log.GirLogger;
// import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
// import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
// import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
// import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
// import cn.geoair.map.dynamic.adv.query.dialect.pgback.PgDialectTableNameUtil;
// import cn.geoair.map.dynamic.adv.query.handler.StreamBeanRsHandler;
// import cn.geoair.map.dynamic.adv.query.handler.StreamRsHandler;
// import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
// import cn.geoair.map.dynamic.adv.mybatis.SqlEngineUtil;
// import cn.geoair.map.dynamic.ds.IDataSourceGetter;
// import cn.hutool.core.util.IdUtil;
// import cn.hutool.core.util.StrUtil;
// import cn.hutool.db.Entity;
// import cn.hutool.db.handler.*;
// import cn.hutool.db.sql.SqlExecutor;
//
// import java.sql.Connection;
// import java.sql.SQLException;
// import java.util.List;
// import java.util.function.Consumer;
//
/// **
// * PostgreSQL数据库的动态高级查询基础操作实现类
// *
// * @author 张逢吉
// */
// public class PgAdvBaseSelectOpt implements IAdvBaseSelectOpt {
//
// IDataSourceGetter dataSourceGetter;
// DialectTableNameProcessor dialectTableNameProcessor =
// PgDialectTableNameUtil.getInstance();
//
// public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {
// this.dataSourceGetter = dataSourceGetter;
// }
//
// private static final GiLogger log = GirLogger.getLoger(PgAdvBaseSelectOpt.class);
// // COUNT查询临时表别名前缀，固定后缀避免冲突
// private static final String COUNT_ALIAS_PREFIX = "count_query_";
// // 临时表别名长度限制（PostgreSQL标识符长度限制）
// private static final int TEMP_TABLE_ALIAS_LENGTH = 8;
//
// @Override
// public GirAdvOneRow bSelectOne(String sql) {
// Connection connection = dataSourceGetter.getConnection();
// try {
// String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
// // 包装SQL确保只返回一行
// String execSql = StrUtil.format("select * from ({}) as {} limit 1",
// cleanSql, dialectTableNameProcessor.tbGetTempAliasTableName());
//
// logExecuteSql("bSelectOne", execSql);
//
// Entity queryResult = SqlExecutor.query(connection, execSql, new EntityHandler());
// return GirAdvOneRow.ofByEntity(queryResult);
// } catch (SQLException e) {
// throw new RuntimeException("执行bSelectOne查询失败，SQL：" + sql, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public List<GirAdvOneRow> bSelectList(String sql) {
// Connection connection = dataSourceGetter.getConnection();
// try {
// String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
// logExecuteSql("bSelectList", cleanSql);
//
// List<Entity> queryResult = SqlExecutor.query(connection, cleanSql, new
// EntityListHandler());
// return GirAdvOneRow.ofByEntityList(queryResult);
// } catch (SQLException e) {
// throw new RuntimeException("执行bSelectList查询失败，SQL：" + sql, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public void bSelectList(String sql, Consumer<GirAdvOneRow> rowConsumer) {
// Connection connection = dataSourceGetter.getConnection();
// try {
// String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
// logExecuteSql("bSelectList(流式)", cleanSql);
//
// SqlExecutor.query(connection, cleanSql, new StreamRsHandler(rowConsumer));
// } catch (SQLException e) {
// throw new RuntimeException("执行流式bSelectList查询失败，SQL：" + sql, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public List<List<Object>> bSelectListToValueList(String sql) {
// Connection connection = dataSourceGetter.getConnection();
// try {
// String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
// logExecuteSql("bSelectListToValueList", cleanSql);
//
// return SqlExecutor.query(connection, cleanSql, new ValueListHandler());
// } catch (SQLException e) {
// throw new RuntimeException("执行bSelectListToValueList查询失败，SQL：" + sql, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public Number bSelectNumber(String sql) {
// Connection connection = dataSourceGetter.getConnection();
// try {
// String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
// logExecuteSql("bSelectNumber", cleanSql);
//
// return SqlExecutor.query(connection, cleanSql, new NumberHandler());
// } catch (SQLException e) {
// throw new RuntimeException("执行bSelectNumber查询失败，SQL：" + sql, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public Number bSelectRecordRowCount(String sql) {
// String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
// String countSql = StrUtil.format("SELECT COUNT(1) FROM ({}) AS {}{}",
// cleanSql, COUNT_ALIAS_PREFIX, IdUtil.simpleUUID().substring(0, 6));
// logExecuteSql("bSelectRecordRowCount", countSql);
// return bSelectNumber(countSql);
// }
//
// @Override
// public <E> E bSelectObjOne(String sql, Class<E> clazz) {
// Connection connection = dataSourceGetter.getConnection();
// try {
// String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
// logExecuteSql("bSelectObjOne", cleanSql);
//
// Object queryResult = SqlExecutor.query(connection, cleanSql,
// BeanHandler.create(clazz));
// return (E) queryResult;
// } catch (SQLException e) {
// throw new RuntimeException("执行bSelectObjOne查询失败，SQL：" + sql + "，目标类型：" +
// clazz.getName(), e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public <E> List<E> bSelectObjList(String sql, Class<E> clazz) {
// Connection connection = dataSourceGetter.getConnection();
// try {
// String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
// logExecuteSql("bSelectObjList", cleanSql);
//
// return SqlExecutor.query(connection, cleanSql, BeanListHandler.create(clazz));
// } catch (SQLException e) {
// throw new RuntimeException("执行bSelectObjList查询失败，SQL：" + sql + "，目标类型：" +
// clazz.getName(), e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public <E> void bSelectObjList(String sql, Class<E> clazz, Consumer<E> rowConsumer) {
// Connection connection = dataSourceGetter.getConnection();
// try {
// String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
// logExecuteSql("bSelectObjList(流式)", cleanSql);
//
// SqlExecutor.query(connection, cleanSql, new StreamBeanRsHandler<>(rowConsumer, clazz));
// } catch (SQLException e) {
// throw new RuntimeException("执行流式bSelectObjList查询失败，SQL：" + sql + "，目标类型：" +
// clazz.getName(), e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// // ==================== 带参数查询方法实现 ====================
// @Override
// public GirAdvOneRow bSelectOne(String sqlStatement, SqlParamMap sqlParam) {
// SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
// Connection connection = dataSourceGetter.getConnection();
// try {
// // 包装SQL确保只返回一行
// String execSql = StrUtil.format("select * from ({}) as {} limit 1",
// sqlMeta.getSql(), dialectTableNameProcessor.tbGetTempAliasTableName());
// logExecuteSql("bSelectOne(带参数)", execSql, sqlParam);
//
// Entity queryResult = SqlExecutor.query(connection, execSql, new EntityHandler(),
// sqlMeta.getJdbcParamValues().toArray());
// return GirAdvOneRow.ofByEntity(queryResult);
// } catch (SQLException e) {
// throw new RuntimeException("执行带参数bSelectOne查询失败，SQL：" + sqlStatement, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public List<GirAdvOneRow> bSelectList(String sqlStatement, SqlParamMap sqlParam) {
// SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
// Connection connection = dataSourceGetter.getConnection();
// try {
// logExecuteSql("bSelectList(带参数)", sqlMeta.getSql(), sqlParam);
//
// List<Entity> queryResult = SqlExecutor.query(connection, sqlMeta.getSql(), new
// EntityListHandler(), sqlMeta.getJdbcParamValues().toArray());
// return GirAdvOneRow.ofByEntityList(queryResult);
// } catch (SQLException e) {
// throw new RuntimeException("执行带参数bSelectList查询失败，SQL：" + sqlStatement, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public void bSelectList(String sqlStatement, SqlParamMap sqlParam,
// Consumer<GirAdvOneRow> rowConsumer) {
// SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
// Connection connection = dataSourceGetter.getConnection();
// try {
// logExecuteSql("bSelectList(带参数-流式)", sqlMeta.getSql(), sqlParam);
//
// SqlExecutor.query(connection, sqlMeta.getSql(), new StreamRsHandler(rowConsumer),
// sqlMeta.getJdbcParamValues().toArray());
// } catch (SQLException e) {
// throw new RuntimeException("执行带参数流式bSelectList查询失败，SQL：" + sqlStatement, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public List<List<Object>> bSelectListToValueList(String sqlStatement, SqlParamMap
// sqlParam) {
// SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
// Connection connection = dataSourceGetter.getConnection();
// try {
// logExecuteSql("bSelectListToValueList(带参数)", sqlMeta.getSql(), sqlParam);
//
// return SqlExecutor.query(connection, sqlMeta.getSql(), new ValueListHandler(),
// sqlMeta.getJdbcParamValues().toArray());
// } catch (SQLException e) {
// throw new RuntimeException("执行带参数bSelectListToValueList查询失败，SQL：" + sqlStatement, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public Number bSelectNumber(String sqlStatement, SqlParamMap sqlParam) {
// SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
// Connection connection = dataSourceGetter.getConnection();
// try {
// logExecuteSql("bSelectNumber(带参数)", sqlMeta.getSql(), sqlParam);
//
// return SqlExecutor.query(connection, sqlMeta.getSql(), new NumberHandler(),
// sqlMeta.getJdbcParamValues().toArray());
// } catch (SQLException e) {
// throw new RuntimeException("执行带参数bSelectNumber查询失败，SQL：" + sqlStatement, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public Number bSelectRecordRowCount(String sqlStatement, SqlParamMap sqlParam) {
// SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
// String countSql = StrUtil.format("SELECT COUNT(1) FROM ({}) AS {}{}",
// sqlMeta.getSql(), COUNT_ALIAS_PREFIX, IdUtil.simpleUUID().substring(0, 6));
// SqlMeta countSqlMeta = new SqlMeta(countSql, sqlMeta.getJdbcParamValues());
// logExecuteSql("bSelectRecordRowCount(带参数)", countSql, sqlParam);
//
// Connection connection = dataSourceGetter.getConnection();
// try {
// return SqlExecutor.query(connection, countSqlMeta.getSql(), new NumberHandler(),
// countSqlMeta.getJdbcParamValues());
// } catch (SQLException e) {
// throw new RuntimeException("执行带参数bSelectRecordRowCount查询失败，SQL：" + sqlStatement, e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public <E> E bSelectObjOne(String sqlStatement, SqlParamMap sqlParam, Class<E> clazz) {
// SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
// Connection connection = dataSourceGetter.getConnection();
// try {
// logExecuteSql("bSelectObjOne(带参数)", sqlMeta.getSql(), sqlParam);
//
// Object queryResult = SqlExecutor.query(connection, sqlMeta.getSql(),
// BeanHandler.create(clazz), sqlMeta.getJdbcParamValues().toArray());
// return (E) queryResult;
// } catch (SQLException e) {
// throw new RuntimeException("执行带参数bSelectObjOne查询失败，SQL：" + sqlStatement + "，目标类型：" +
// clazz.getName(), e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public <E> List<E> bSelectObjList(String sqlStatement, SqlParamMap sqlParam, Class<E>
// clazz) {
// SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
// Connection connection = dataSourceGetter.getConnection();
// try {
// logExecuteSql("bSelectObjList(带参数)", sqlMeta.getSql(), sqlParam);
//
// return SqlExecutor.query(connection, sqlMeta.getSql(), BeanListHandler.create(clazz),
// sqlMeta.getJdbcParamValues().toArray());
// } catch (SQLException e) {
// throw new RuntimeException("执行带参数bSelectObjList查询失败，SQL：" + sqlStatement + "，目标类型：" +
// clazz.getName(), e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// @Override
// public <E> void bSelectObjList(String sqlStatement, SqlParamMap sqlParam, Class<E>
// clazz, Consumer<E> rowConsumer) {
// SqlMeta sqlMeta = parseSqlWithParam(sqlStatement, sqlParam);
// Connection connection = dataSourceGetter.getConnection();
// try {
// logExecuteSql("bSelectObjList(带参数-流式)", sqlMeta.getSql(), sqlParam);
//
// SqlExecutor.query(connection, sqlMeta.getSql(), new StreamBeanRsHandler<>(rowConsumer,
// clazz), sqlMeta.getJdbcParamValues().toArray());
// } catch (SQLException e) {
// throw new RuntimeException("执行带参数流式bSelectObjList查询失败，SQL：" + sqlStatement + "，目标类型：" +
// clazz.getName(), e);
// } finally {
// dataSourceGetter.connectionClose(connection);
// }
// }
//
// /**
// * 解析带参数的SQL语句，生成可执行的SQL和参数列表
// *
// * @param sqlStatement 原始SQL语句（支持MyBatis标签）
// * @param sqlParam SQL参数映射
// * @return 解析后的SqlMeta对象（包含执行SQL和JDBC参数列表）
// */
// private SqlMeta parseSqlWithParam(String sqlStatement, SqlParamMap sqlParam) {
// if (StrUtil.isEmpty(sqlStatement)) {
// throw new IllegalArgumentException("SQL语句不能为空");
// }
// String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
// return SqlEngineUtil.getEngine().parse(cleanSql, sqlParam);
// }
//
// /**
// * 统一打印SQL执行日志
// *
// * @param methodName 执行的方法名
// * @param sql 执行的SQL语句
// */
// private void logExecuteSql(String methodName, String sql) {
// log.info("schema:[{}] db:[{}] {}执行的SQL为：{}", dataSourceGetter.getSchemaName(),
// dataSourceGetter.getDataSourceId(), methodName, sql);
// }
//
// /**
// * 统一打印带参数的SQL执行日志
// *
// * @param methodName 执行的方法名
// * @param sql 执行的SQL语句
// * @param sqlParam SQL参数
// */
// private void logExecuteSql(String methodName, String sql, SqlParamMap sqlParam) {
// log.info("schema:[{}] db:[{}] {}执行的SQL为：{}，参数：{}", dataSourceGetter.getSchemaName(),
// dataSourceGetter.getDataSourceId(), methodName, sql, sqlParam);
// }
//
// }
