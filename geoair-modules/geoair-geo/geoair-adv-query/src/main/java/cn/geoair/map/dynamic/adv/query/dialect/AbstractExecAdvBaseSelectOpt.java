package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.handler.StreamBeanRsHandler;
import cn.geoair.map.dynamic.adv.query.handler.StreamRsHandler;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.geoair.map.dynamic.adv.query.utils.AdvLogSql;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.handler.*;
import cn.hutool.db.sql.SqlExecutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    Supplier<AdvQueryGlobalConfig> configAdvQueryGetter;

    public AbstractExecAdvBaseSelectOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        this.configAdvQueryGetter = configAdvQueryGetter;
    }

    @Override
    public AdvQueryGlobalConfig getConfig() {
        return configAdvQueryGetter.get();
    }

    @Override
    public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
    }

    // ========== 通用逻辑：无参数查询 ==========
    @Override
    public GirAdvOneRow bSelectOne(String sql) {
        Connection connection = dataSourceGetter.getConnection();
        String execSql = null;
        try {

            String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
            // 差异化：构建单条查询包装SQL
            execSql = buildSelectOneWrapSql(cleanSql);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Entity queryResult = SqlExecutor.query(connection, execSql, new EntityHandler());
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectOne", execSql, lastTaskTimeMillis, GutilObject.isEmpty(queryResult) ? 0 : 1);
            return GirAdvOneRow.ofByEntity(queryResult);
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectOne", execSql, e);
            throw new RuntimeException("执行bSelectOne查询失败，SQL：" + sql, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sql) {
        Connection connection = dataSourceGetter.getConnection();
        String cleanSql = "";
        try {
            cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            List<Entity> queryResult =
                    SqlExecutor.query(connection, cleanSql, new EntityListHandler());
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectList", cleanSql, lastTaskTimeMillis, GutilObject.isEmpty(queryResult) ? 0 : queryResult.size());
            return GirAdvOneRow.ofByEntityList(queryResult);
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectList", cleanSql, e);
            throw new RuntimeException("执行bSelectList查询失败，SQL：" + sql, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public void bSelectListStream(String sql, Consumer<GirAdvOneRow> rowConsumer) {
        Connection connection = dataSourceGetter.getConnection();
        String cleanSql = "";
        try {
            cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Integer query = SqlExecutor.query(connection, cleanSql, new StreamRsHandler(rowConsumer));
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectList(流式)", cleanSql, lastTaskTimeMillis, query);
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectList(流式)", cleanSql, e);
            throw new RuntimeException("执行流式bSelectList查询失败，SQL：" + sql, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sql) {
        Connection connection = dataSourceGetter.getConnection();
        String cleanSql = "";
        try {
            cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            List<List<Object>> query = SqlExecutor.query(connection, cleanSql, new ValueListHandler());
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectListToValueList", cleanSql, lastTaskTimeMillis, GutilObject.isEmpty(query) ? 0 : query.size());
            return query;
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectListToValueList", cleanSql, e);
            throw new RuntimeException("执行bSelectListToValueList查询失败，SQL：" + sql, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Number bSelectNumber(String sql) {
        Connection connection = dataSourceGetter.getConnection();
        String cleanSql = "";
        try {
            cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Number query = SqlExecutor.query(connection, cleanSql, new NumberHandler());
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectNumber", cleanSql, lastTaskTimeMillis, GutilObject.isEmpty(query) ? 0 : 1);
            return query;
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectNumber", cleanSql, e);
            throw new RuntimeException("执行bSelectNumber查询失败，SQL：" + sql, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Number bSelectRecordRowCount(String sql) {
        String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
        // 差异化：构建COUNT查询SQL
        String countSql = buildCountQuerySql(cleanSql);
        return bSelectNumber(countSql);
    }

    @Override
    public <E> E bSelectObjOne(String sql, Class<E> clazz) {
        Connection connection = dataSourceGetter.getConnection();
        String cleanSql = null;
        try {
            cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Object queryResult = SqlExecutor.query(connection, cleanSql, BeanHandler.create(clazz));
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectObjOne", cleanSql, lastTaskTimeMillis, GutilObject.isEmpty(queryResult) ? 0 : 1);
            return (E) queryResult;
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectObjOne", cleanSql, e);
            throw new RuntimeException(
                    "执行bSelectObjOne查询失败，SQL：" + sql + "，目标类型：" + clazz.getName(), e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public <E> List<E> bSelectObjList(String sql, Class<E> clazz) {
        Connection connection = dataSourceGetter.getConnection();
        String cleanSql = "";
        try {
            cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            List<E> query = SqlExecutor.query(connection, cleanSql, BeanListHandler.create(clazz));
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectObjList", cleanSql, lastTaskTimeMillis, GutilObject.isEmpty(query) ? 0 : query.size());
            return query;
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectObjList", cleanSql, e);
            throw new RuntimeException(
                    "执行bSelectObjList查询失败，SQL：" + sql + "，目标类型：" + clazz.getName(), e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public <E> void bSelectObjListStream(String sql, Class<E> clazz, Consumer<E> rowConsumer) {
        Connection connection = dataSourceGetter.getConnection();
        String cleanSql = "";
        try {
            cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Integer query = SqlExecutor.query(connection, cleanSql, new StreamBeanRsHandler<>(rowConsumer, clazz));
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectObjList(流式)", cleanSql, lastTaskTimeMillis, GutilObject.isEmpty(query) ? 0 : query);
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectObjList(流式)", cleanSql, e);
            throw new RuntimeException(
                    "执行流式bSelectObjList查询失败，SQL：" + sql + "，目标类型：" + clazz.getName(), e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public GirAdvOneRow bSelectOne(String dynamicSql, SqlParamMap sqlParam) {
        SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParam, dialectTableNameProcessor);
        return bSelectOne(sqlMeta.getSql(), SqlParamList.of(sqlMeta.getJdbcParamValues()));
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String dynamicSql, SqlParamMap sqlParam) {
        SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParam, dialectTableNameProcessor);
        return bSelectList(sqlMeta.getSql(), SqlParamList.of(sqlMeta.getJdbcParamValues()));
    }

    @Override
    public void bSelectListStream(
            String dynamicSql, SqlParamMap sqlParam, Consumer<GirAdvOneRow> rowConsumer) {
        SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParam, dialectTableNameProcessor);
        bSelectListStream(sqlMeta.getSql(), SqlParamList.of(sqlMeta.getJdbcParamValues()), rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String dynamicSql, SqlParamMap sqlParam) {
        SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParam, dialectTableNameProcessor);
        return bSelectListToValueList(sqlMeta.getSql(), SqlParamList.of(sqlMeta.getJdbcParamValues()));
    }

    @Override
    public Number bSelectNumber(String dynamicSql, SqlParamMap sqlParam) {
        SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParam, dialectTableNameProcessor);
        return bSelectNumber(sqlMeta.getSql(), SqlParamList.of(sqlMeta.getJdbcParamValues()));
    }

    @Override
    public Number bSelectRecordRowCount(String dynamicSql, SqlParamMap sqlParam) {
        SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParam, dialectTableNameProcessor);
        return bSelectRecordRowCount(sqlMeta.getSql(), SqlParamList.of(sqlMeta.getJdbcParamValues()));
    }

    @Override
    public <E> E bSelectObjOne(String dynamicSql, SqlParamMap sqlParam, Class<E> clazz) {
        SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParam, dialectTableNameProcessor);
        return bSelectObjOne(sqlMeta.getSql(), SqlParamList.of(sqlMeta.getJdbcParamValues()), clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String dynamicSql, SqlParamMap sqlParam, Class<E> clazz) {
        SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParam, dialectTableNameProcessor);
        return bSelectObjList(sqlMeta.getSql(), SqlParamList.of(sqlMeta.getJdbcParamValues()), clazz);
    }

    @Override
    public <E> void bSelectObjListStream(
            String dynamicSql, SqlParamMap sqlParam, Class<E> clazz, Consumer<E> rowConsumer) {
        SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParam, dialectTableNameProcessor);
        bSelectObjListStream(sqlMeta.getSql(), SqlParamList.of(sqlMeta.getJdbcParamValues()), clazz, rowConsumer);
    }

    @Override
    public GirAdvOneRow bSelectOne(String sqlStatement, SqlParamList sqlParamList) {
        sqlStatement = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
        Connection connection = dataSourceGetter.getConnection();
        String execSql = "";
        try {
            execSql = buildSelectOneWrapSql(sqlStatement);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Entity queryResult =
                    SqlExecutor.query(
                            connection,
                            execSql,
                            new EntityHandler(),
                            sqlParamList.toArray());
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectOne(带参数)", execSql, sqlParamList, lastTaskTimeMillis, GutilObject.isEmpty(queryResult) ? 0 : 1);
            return GirAdvOneRow.ofByEntity(queryResult);
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectOne(带参数)", execSql, sqlParamList, e);
            throw new RuntimeException("执行带参数bSelectOne查询失败，SQL：" + sqlStatement, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sqlStatement, SqlParamList sqlParamList) {
        sqlStatement = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
        Connection connection = dataSourceGetter.getConnection();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            List<Entity> queryResult =
                    SqlExecutor.query(
                            connection,
                            sqlStatement,
                            new EntityListHandler(),
                            sqlParamList.toArray());
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectList(带参数)", sqlStatement, sqlParamList, lastTaskTimeMillis, GutilObject.isEmpty(queryResult) ? 0 : queryResult.size());
            return GirAdvOneRow.ofByEntityList(queryResult);
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectList(带参数)", sqlStatement, sqlParamList, e);
            throw new RuntimeException("执行带参数bSelectList查询失败，SQL：" + sqlStatement, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public void bSelectListStream(String sqlStatement, SqlParamList sqlParamList, Consumer<GirAdvOneRow> rowConsumer) {
        sqlStatement = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
        Connection connection = dataSourceGetter.getConnection();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Integer query = SqlExecutor.query(
                    connection,
                    sqlStatement,
                    new StreamRsHandler(rowConsumer),
                    sqlParamList.toArray());
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectList(带参数-流式)", sqlStatement, sqlParamList, lastTaskTimeMillis, query);
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectList(带参数-流式)", sqlStatement, sqlParamList, e);
            throw new RuntimeException("执行带参数流式bSelectList查询失败，SQL：" + sqlStatement, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sqlStatement, SqlParamList sqlParamList) {
        sqlStatement = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
        Connection connection = dataSourceGetter.getConnection();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            List<List<Object>> query = SqlExecutor.query(
                    connection,
                    sqlStatement,
                    new ValueListHandler(),
                    sqlParamList.toArray());
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectListToValueList(带参数)", sqlStatement, sqlParamList, lastTaskTimeMillis, query == null ? 0 : query.size());
            return query;
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectList(带参数)", sqlStatement, sqlParamList, e);
            throw new RuntimeException("执行带参数bSelectListToValueList查询失败，SQL：" + sqlStatement, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Number bSelectNumber(String sqlStatement, SqlParamList sqlParamList) {
        sqlStatement = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
        Connection connection = dataSourceGetter.getConnection();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            Number query = SqlExecutor.query(
                    connection,
                    sqlStatement,
                    new NumberHandler(),
                    sqlParamList.toArray());
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectNumber(带参数)", sqlStatement, sqlParamList, lastTaskTimeMillis, query);
            return query;
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectNumber(带参数)", sqlStatement, sqlParamList, e);
            throw new RuntimeException("执行带参数bSelectNumber查询失败，SQL：" + sqlStatement, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Number bSelectRecordRowCount(String sqlStatement, SqlParamList sqlParamList) {
        sqlStatement = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
        // 差异化：构建COUNT查询SQL
        String countSql = buildCountQuerySql(sqlStatement);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Connection connection = dataSourceGetter.getConnection();
        try {
            Number query = SqlExecutor.query(
                    connection,
                    countSql,
                    new NumberHandler(),
                    sqlParamList.toArray());
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectRecordRowCount(带参数)", countSql, sqlParamList, lastTaskTimeMillis, GutilObject.isEmpty(query) ? 0 : 1);
            return query;
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectRecordRowCount(带参数)", sqlStatement, sqlParamList, e);
            throw new RuntimeException("执行带参数bSelectRecordRowCount查询失败，SQL：" + sqlStatement, e);
        } finally {
            closeConnection(connection);
        }

    }

    @Override
    public <E> E bSelectObjOne(String sqlStatement, SqlParamList sqlParamList, Class<E> clazz) {
        Connection connection = dataSourceGetter.getConnection();
        try {
            sqlStatement = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Object queryResult =
                    SqlExecutor.query(
                            connection,
                            sqlStatement,
                            BeanHandler.create(clazz),
                            sqlParamList.toArray());
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectObjOne(带参数)", sqlStatement, sqlParamList, lastTaskTimeMillis, GutilObject.isEmpty(queryResult) ? 0 : 1);
            return (E) queryResult;
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectObjOne(带参数)", sqlStatement, sqlParamList, e);
            throw new RuntimeException(
                    "执行带参数bSelectObjOne查询失败，SQL：" + sqlStatement + "，目标类型：" + clazz.getName(), e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public <E> List<E> bSelectObjList(String sqlStatement, SqlParamList sqlParamList, Class<E> clazz) {
        Connection connection = dataSourceGetter.getConnection();
        try {
            sqlStatement = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            List<E> query = SqlExecutor.query(
                    connection,
                    sqlStatement,
                    BeanListHandler.create(clazz),
                    sqlParamList.toArray());
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectObjList(带参数)", sqlStatement, sqlParamList, lastTaskTimeMillis, query == null ? 0 : query.size());
            return query;
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectObjList(带参数)", sqlStatement, sqlParamList, e);

            throw new RuntimeException(
                    "执行带参数bSelectObjList查询失败，SQL：" + sqlStatement + "，目标类型：" + clazz.getName(), e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public <E> void bSelectObjListStream(String sqlStatement, SqlParamList sqlParamList, Class<E> clazz, Consumer<E> rowConsumer) {
        Connection connection = dataSourceGetter.getConnection();
        try {
            sqlStatement = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Integer query = SqlExecutor.query(
                    connection,
                    sqlStatement,
                    new StreamBeanRsHandler<>(rowConsumer, clazz),
                    sqlParamList.toArray());
            stopWatch.stop();
            long lastTaskTimeMillis = stopWatch.getLastTaskTimeMillis();
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteSql(this.getClass(), "bSelectObjList(带参数-流式)", sqlStatement, sqlParamList, lastTaskTimeMillis, query);
        } catch (SQLException e) {
              AdvLogSql.of(dataSourceGetter,getConfig()).logExecuteError(this.getClass(), "bSelectObjList(带参数-流式)", sqlStatement, sqlParamList, e);

            throw new RuntimeException(
                    "执行带参数流式bSelectObjList查询失败，SQL：" + sqlStatement + "，目标类型：" + clazz.getName(),
                    e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public GirAdvOneRow bSelectOne(String sqlStatement, GirSqlParam girSqlParam) {
        if (girSqlParam == null||GutilObject.isEmpty(girSqlParam)) {
            return bSelectOne(sqlStatement);
        } else if (girSqlParam instanceof SqlParamList) {
            SqlParamList sqlParamList = (SqlParamList) girSqlParam;
            return bSelectOne(sqlStatement, sqlParamList);
        } else if (girSqlParam instanceof SqlParamMap) {
            SqlParamMap sqlParamMap = (SqlParamMap) girSqlParam;
            return bSelectOne(sqlStatement, sqlParamMap);
        } else {
            throw new RuntimeException("SqlParam参数不合法！");
        }

    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sqlStatement, GirSqlParam girSqlParam) {
        if (girSqlParam == null||GutilObject.isEmpty(girSqlParam)) {
            return bSelectList(sqlStatement);
        } else if (girSqlParam instanceof SqlParamList) {
            SqlParamList sqlParamList = (SqlParamList) girSqlParam;
            return bSelectList(sqlStatement, sqlParamList);
        } else if (girSqlParam instanceof SqlParamMap) {
            SqlParamMap sqlParamMap = (SqlParamMap) girSqlParam;
            return bSelectList(sqlStatement, sqlParamMap);
        } else {
            throw new RuntimeException("SqlParam参数不合法！");
        }
    }

    @Override
    public void bSelectListStream(String sqlStatement, GirSqlParam girSqlParam, Consumer<GirAdvOneRow> rowConsumer) {
        if (girSqlParam == null||GutilObject.isEmpty(girSqlParam)) {
            bSelectListStream(sqlStatement, rowConsumer);
        } else if (girSqlParam instanceof SqlParamList) {
            SqlParamList sqlParamList = (SqlParamList) girSqlParam;
            bSelectListStream(sqlStatement, sqlParamList, rowConsumer);
        } else if (girSqlParam instanceof SqlParamMap) {
            SqlParamMap sqlParamMap = (SqlParamMap) girSqlParam;
            bSelectListStream(sqlStatement, sqlParamMap, rowConsumer);
        } else {
            throw new RuntimeException("SqlParam参数不合法！");
        }
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sqlStatement, GirSqlParam girSqlParam) {
        if (girSqlParam == null||GutilObject.isEmpty(girSqlParam)) {
            return bSelectListToValueList(sqlStatement);
        } else if (girSqlParam instanceof SqlParamList) {
            SqlParamList sqlParamList = (SqlParamList) girSqlParam;
            return bSelectListToValueList(sqlStatement, sqlParamList);
        } else if (girSqlParam instanceof SqlParamMap) {
            SqlParamMap sqlParamMap = (SqlParamMap) girSqlParam;
            return bSelectListToValueList(sqlStatement, sqlParamMap);
        } else {
            throw new RuntimeException("SqlParam参数不合法！");
        }
    }

    @Override
    public Number bSelectNumber(String sqlStatement, GirSqlParam girSqlParam) {
        if (girSqlParam == null||GutilObject.isEmpty(girSqlParam)) {
            return bSelectNumber(sqlStatement);
        } else if (girSqlParam instanceof SqlParamList) {
            SqlParamList sqlParamList = (SqlParamList) girSqlParam;
            return bSelectNumber(sqlStatement, sqlParamList);
        } else if (girSqlParam instanceof SqlParamMap) {
            SqlParamMap sqlParamMap = (SqlParamMap) girSqlParam;
            return bSelectNumber(sqlStatement, sqlParamMap);
        } else {
            throw new RuntimeException("SqlParam参数不合法！");
        }
    }

    @Override
    public Number bSelectRecordRowCount(String sqlStatement, GirSqlParam girSqlParam) {
        if (girSqlParam == null||GutilObject.isEmpty(girSqlParam)) {
            return bSelectRecordRowCount(sqlStatement);
        } else if (girSqlParam instanceof SqlParamList) {
            SqlParamList sqlParamList = (SqlParamList) girSqlParam;
            return bSelectRecordRowCount(sqlStatement, sqlParamList);
        } else if (girSqlParam instanceof SqlParamMap) {
            SqlParamMap sqlParamMap = (SqlParamMap) girSqlParam;
            return bSelectRecordRowCount(sqlStatement, sqlParamMap);
        } else {
            throw new RuntimeException("SqlParam参数不合法！");
        }
    }

    @Override
    public <E> E bSelectObjOne(String sqlStatement, GirSqlParam girSqlParam, Class<E> clazz) {
        if (girSqlParam == null||GutilObject.isEmpty(girSqlParam)) {
            return bSelectObjOne(sqlStatement, clazz);
        } else if (girSqlParam instanceof SqlParamList) {
            SqlParamList sqlParamList = (SqlParamList) girSqlParam;
            return bSelectObjOne(sqlStatement, sqlParamList, clazz);
        } else if (girSqlParam instanceof SqlParamMap) {
            SqlParamMap sqlParamMap = (SqlParamMap) girSqlParam;
            return bSelectObjOne(sqlStatement, sqlParamMap, clazz);
        } else {
            throw new RuntimeException("SqlParam参数不合法！");
        }
    }

    @Override
    public <E> List<E> bSelectObjList(String sqlStatement, GirSqlParam girSqlParam, Class<E> clazz) {
        if (girSqlParam == null||GutilObject.isEmpty(girSqlParam)) {
            return bSelectObjList(sqlStatement, clazz);
        } else if (girSqlParam instanceof SqlParamList) {
            SqlParamList sqlParamList = (SqlParamList) girSqlParam;
            return bSelectObjList(sqlStatement, sqlParamList, clazz);
        } else if (girSqlParam instanceof SqlParamMap) {
            SqlParamMap sqlParamMap = (SqlParamMap) girSqlParam;
            return bSelectObjList(sqlStatement, sqlParamMap, clazz);
        } else {
            throw new RuntimeException("SqlParam参数不合法！");
        }
    }

    @Override
    public <E> void bSelectObjListStream(String sqlStatement, GirSqlParam girSqlParam, Class<E> clazz, Consumer<E> rowConsumer) {
        if (girSqlParam == null||GutilObject.isEmpty(girSqlParam)) {
            bSelectObjListStream(sqlStatement, clazz, rowConsumer);

        } else if (girSqlParam instanceof SqlParamList) {
            SqlParamList sqlParamList = (SqlParamList) girSqlParam;
            bSelectObjListStream(sqlStatement, sqlParamList, clazz, rowConsumer);

        } else if (girSqlParam instanceof SqlParamMap) {
            SqlParamMap sqlParamMap = (SqlParamMap) girSqlParam;
            bSelectObjListStream(sqlStatement, sqlParamMap, clazz, rowConsumer);

        } else {
            throw new RuntimeException("SqlParam参数不合法！");
        }
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
        return StrUtil.format(
                "select * from ({}) as {} limit 1",
                cleanSql,
                dialectTableNameProcessor.tbGetTempAliasTableName());
    }

    protected String buildCountQuerySql(String cleanSql) {
        // PG：生成唯一临时表别名（避免冲突）
        String tempAlias = COUNT_ALIAS_PREFIX + IdUtil.simpleUUID().substring(0, 8);
        return StrUtil.format("SELECT COUNT(1) FROM ({}) AS {}", cleanSql, tempAlias);
    }
}
