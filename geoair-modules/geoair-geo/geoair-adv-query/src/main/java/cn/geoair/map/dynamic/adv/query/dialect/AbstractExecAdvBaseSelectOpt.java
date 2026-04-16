package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.handler.StreamBeanRsHandler;
import cn.geoair.map.dynamic.adv.query.handler.StreamRsHandler;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.handler.*;
import cn.hutool.db.sql.SqlExecutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

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

            String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
            // 差异化：构建单条查询包装SQL
            String execSql = buildSelectOneWrapSql(cleanSql);

            logExecuteSql("bSelectOne", execSql);

            Entity queryResult = SqlExecutor.query(connection, execSql, new EntityHandler());
            return GirAdvOneRow.ofByEntity(queryResult);
        } catch (SQLException e) {
            throw new RuntimeException("执行bSelectOne查询失败，SQL：" + sql, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sql) {
        Connection connection = dataSourceGetter.getConnection();
        try {
            String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
            logExecuteSql("bSelectList", cleanSql);

            List<Entity> queryResult =
                    SqlExecutor.query(connection, cleanSql, new EntityListHandler());
            return GirAdvOneRow.ofByEntityList(queryResult);
        } catch (SQLException e) {
            throw new RuntimeException("执行bSelectList查询失败，SQL：" + sql, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public void bSelectListStream(String sql, Consumer<GirAdvOneRow> rowConsumer) {
        Connection connection = dataSourceGetter.getConnection();
        try {
            String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
            logExecuteSql("bSelectList(流式)", cleanSql);

            SqlExecutor.query(connection, cleanSql, new StreamRsHandler(rowConsumer));
        } catch (SQLException e) {
            throw new RuntimeException("执行流式bSelectList查询失败，SQL：" + sql, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sql) {
        Connection connection = dataSourceGetter.getConnection();
        try {
            String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
            logExecuteSql("bSelectListToValueList", cleanSql);

            return SqlExecutor.query(connection, cleanSql, new ValueListHandler());
        } catch (SQLException e) {
            throw new RuntimeException("执行bSelectListToValueList查询失败，SQL：" + sql, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Number bSelectNumber(String sql) {
        Connection connection = dataSourceGetter.getConnection();
        try {
            String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
            logExecuteSql("bSelectNumber", cleanSql);

            return SqlExecutor.query(connection, cleanSql, new NumberHandler());
        } catch (SQLException e) {
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
        logExecuteSql("bSelectRecordRowCount", countSql);
        return bSelectNumber(countSql);
    }

    @Override
    public <E> E bSelectObjOne(String sql, Class<E> clazz) {
        Connection connection = dataSourceGetter.getConnection();
        try {
            String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
            logExecuteSql("bSelectObjOne", cleanSql);

            Object queryResult = SqlExecutor.query(connection, cleanSql, BeanHandler.create(clazz));
            return (E) queryResult;
        } catch (SQLException e) {
            throw new RuntimeException(
                    "执行bSelectObjOne查询失败，SQL：" + sql + "，目标类型：" + clazz.getName(), e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public <E> List<E> bSelectObjList(String sql, Class<E> clazz) {
        Connection connection = dataSourceGetter.getConnection();
        try {
            String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
            logExecuteSql("bSelectObjList", cleanSql);

            return SqlExecutor.query(connection, cleanSql, BeanListHandler.create(clazz));
        } catch (SQLException e) {
            throw new RuntimeException(
                    "执行bSelectObjList查询失败，SQL：" + sql + "，目标类型：" + clazz.getName(), e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public <E> void bSelectObjListStream(String sql, Class<E> clazz, Consumer<E> rowConsumer) {
        Connection connection = dataSourceGetter.getConnection();
        try {
            String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
            logExecuteSql("bSelectObjList(流式)", cleanSql);

            SqlExecutor.query(connection, cleanSql, new StreamBeanRsHandler<>(rowConsumer, clazz));
        } catch (SQLException e) {
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
        try {
            String execSql = buildSelectOneWrapSql(sqlStatement);
            logExecuteSql("bSelectOne(带参数)", execSql, sqlParamList);

            Entity queryResult =
                    SqlExecutor.query(
                            connection,
                            execSql,
                            new EntityHandler(),
                            sqlParamList.toArray());
            return GirAdvOneRow.ofByEntity(queryResult);
        } catch (SQLException e) {
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
            logExecuteSql("bSelectList(带参数)", sqlStatement, sqlParamList);

            List<Entity> queryResult =
                    SqlExecutor.query(
                            connection,
                            sqlStatement,
                            new EntityListHandler(),
                            sqlParamList.toArray());
            return GirAdvOneRow.ofByEntityList(queryResult);
        } catch (SQLException e) {
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
            logExecuteSql("bSelectList(带参数-流式)", sqlStatement, sqlParamList);

            SqlExecutor.query(
                    connection,
                    sqlStatement,
                    new StreamRsHandler(rowConsumer),
                    sqlParamList.toArray());
        } catch (SQLException e) {
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
            logExecuteSql("bSelectListToValueList(带参数)", sqlStatement, sqlParamList);

            return SqlExecutor.query(
                    connection,
                    sqlStatement,
                    new ValueListHandler(),
                    sqlParamList.toArray());
        } catch (SQLException e) {
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
            logExecuteSql("bSelectNumber(带参数)", sqlStatement, sqlParamList);
            return SqlExecutor.query(
                    connection,
                    sqlStatement,
                    new NumberHandler(),
                    sqlParamList.toArray());
        } catch (SQLException e) {
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

        logExecuteSql("bSelectRecordRowCount(带参数)", countSql, sqlParamList);

        Connection connection = dataSourceGetter.getConnection();
        try {
            return SqlExecutor.query(
                    connection,
                    countSql,
                    new NumberHandler(),
                    sqlParamList.toArray());
        } catch (SQLException e) {
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
            logExecuteSql("bSelectObjOne(带参数)", sqlStatement, sqlParamList);

            Object queryResult =
                    SqlExecutor.query(
                            connection,
                            sqlStatement,
                            BeanHandler.create(clazz),
                            sqlParamList.toArray());
            return (E) queryResult;
        } catch (SQLException e) {
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
            logExecuteSql("bSelectObjList(带参数)", sqlStatement, sqlParamList);

            return SqlExecutor.query(
                    connection,
                    sqlStatement,
                    BeanListHandler.create(clazz),
                    sqlParamList.toArray());
        } catch (SQLException e) {
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
            logExecuteSql("bSelectObjList(带参数-流式)", sqlStatement, sqlParamList);

            SqlExecutor.query(
                    connection,
                    sqlStatement,
                    new StreamBeanRsHandler<>(rowConsumer, clazz),
                    sqlParamList.toArray());
        } catch (SQLException e) {
            throw new RuntimeException(
                    "执行带参数流式bSelectObjList查询失败，SQL：" + sqlStatement + "，目标类型：" + clazz.getName(),
                    e);
        } finally {
            closeConnection(connection);
        }
    }


    /**
     * 统一打印SQL执行日志（无参数）
     */
    protected void logExecuteSql(String methodName, String sql) {
        log.debug(
                "schema:[{}] db:[{}] {}执行的SQL为：{}",
                getSchemaName(),
                getDatabaseName(),
                methodName,
                sql);
    }

    /**
     * 统一打印带参数的SQL执行日志
     */
    protected void logExecuteSql(String methodName, String sql, SqlParamList sqlParamList) {
        log.debug(
                "schema:[{}] db:[{}] {}执行的SQL为：{}，参数：{}",
                getSchemaName(),
                getDatabaseName(),
                methodName,
                sql,
                sqlParamList.toParamString());
    }

    /**
     * 获取Schema/库名（通用封装）
     */
    protected String getSchemaName() {
        return dataSourceGetter != null
                ? GutilObject.isEmpty(dataSourceGetter.getSchemaName())
                ? ""
                : dataSourceGetter.getSchemaName()
                : "";
    }

    /**
     * 获取数据源ID（通用封装）
     */
    protected String getDataSourceId() {
        return dataSourceGetter != null
                ? GutilObject.isEmpty(dataSourceGetter.getDataSourceId())
                ? ""
                : dataSourceGetter.getDataSourceId()
                : "";
    }

    /**
     * 获取数据库名称（通用封装）
     */
    protected String getDatabaseName() {
        return dataSourceGetter != null
                ? GutilObject.isEmpty(dataSourceGetter.getDatabaseName())
                ? ""
                : dataSourceGetter.getDatabaseName()
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
