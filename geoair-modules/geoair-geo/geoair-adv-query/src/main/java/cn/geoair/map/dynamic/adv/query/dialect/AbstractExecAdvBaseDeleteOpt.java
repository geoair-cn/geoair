package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.strategy.DeleteStrategy;
import cn.geoair.map.dynamic.adv.query.utils.AdvLogSql;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereLambdaFilter;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.SqlExecutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 数据库删除操作抽象父类 封装所有数据库通用的删除逻辑，差异化语法由子类实现
 */
public abstract class AbstractExecAdvBaseDeleteOpt implements IAdvBaseDeleteOpt {

    protected IDataSourceGetter dataSourceGetter;
    protected DialectTableNameProcessor dialectTableNameProcessor;
    protected static final GiLogger log = GirLogger.getLoger(AbstractExecAdvBaseDeleteOpt.class);
    protected static final int DEFAULT_BATCH_SIZE = 1000;

    Supplier<AdvQueryGlobalConfig> configAdvQueryGetter;

    public AbstractExecAdvBaseDeleteOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
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

    // ========== 1. 自定义SQL删除 ==========
    @Override
    public Integer bDeleteBySql(String sqlStatement) {
        return bDeleteBySql(sqlStatement, SqlParamMap.of());
    }

    @Override
    public Integer bDeleteBySql(String dynamicSql, SqlParamMap sqlParam) {
        if (StrUtil.isEmpty(dynamicSql)) {
            throw new IllegalArgumentException("删除SQL语句不能为空");
        }
        SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParam, dialectTableNameProcessor);
        String execSql = sqlMeta.getSql();
        List<Object> jdbcParams = sqlMeta.getJdbcParamValues();
        return bDeleteBySql(execSql, SqlParamList.ofList(jdbcParams));
    }

    @Override
    public Integer bDeleteBySql(String sqlStatement, SqlParamList sqlParam) {
        if (StrUtil.isEmpty(sqlStatement)) {
            throw new IllegalArgumentException("删除SQL语句不能为空");
        }
        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result;
            if (CollUtil.isEmpty(sqlParam)) {
                result = SqlExecutor.execute(connection, sqlStatement);
            } else {
                result = SqlExecutor.execute(connection, sqlStatement, sqlParam.toArray());
            }
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), "bDeleteBySql", sqlStatement, sqlParam, cost, result);
            return result;
        } catch (SQLException e) {
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(this.getClass(), "bDeleteBySql", sqlStatement, sqlParam, e);
            throw new RuntimeException("执行自定义删除SQL失败，SQL：" + sqlStatement, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Integer bDeleteBySql(String sqlStatement, GirSqlParam sqlParam) {
        if (sqlParam == null) {
            return bDeleteBySql(sqlStatement);
        } else if (sqlParam instanceof SqlParamList) {
            return bDeleteBySql(sqlStatement, (SqlParamList) sqlParam);
        } else if (sqlParam instanceof SqlParamMap) {
            return bDeleteBySql(sqlStatement, (SqlParamMap) sqlParam);
        } else {
            throw new RuntimeException("SqlParam参数不合法！");
        }
    }

    // ========== 2. 按主键删除 ==========

    @Override
    public Integer bDeleteByPK(String tableName, String idKey, Object id) {
        validateTableName(tableName);
        validateIdKeyAndValue(idKey, id);
        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        String execSql = buildDeleteByPrimaryKeySql(quoteTableName, idKey);

        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, execSql, id);
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), "bDeleteByPK", execSql, SqlParamList.of(id), cost, result);
            return result;
        } catch (SQLException e) {
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(this.getClass(), "bDeleteByPK", execSql, SqlParamList.of(id), e);
            throw new RuntimeException("按主键删除失败，表名：" + tableName + "，主键：" + idKey + "=" + id, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public <T> Integer bDeleteByPK(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("删除的实体对象不能为空");
        }
        DeleteStrategy strategy = new DeleteStrategy()
                .setToUnderlineCase(true);
        return bDeleteByPK(entity, strategy);
    }

    @Override
    public <T> Integer bDeleteByPK(T entity, DeleteStrategy strategy) {
        if (entity == null) {
            throw new IllegalArgumentException("删除的实体对象不能为空");
        }
        if (strategy == null) {
            return bDeleteByPK(entity);
        }

        String tableName = strategy.getTableName();
        if (GutilObject.isEmpty(tableName)) {
            tableName = GirAdvSqlUtils.getTableName(entity.getClass());
        }
        if (GutilObject.isEmpty(tableName)) {
            throw new IllegalArgumentException("tableName 不能为空");
        }

        String idKey = strategy.getIdKey();
        if (GutilObject.isEmpty(idKey)) {
            List<String> idKeys = GirAdvSqlUtils.getIdByAnnotation(entity.getClass());
            if (CollUtil.isNotEmpty(idKeys)) {
                idKey = idKeys.get(0);
            }
        }
        if (GutilObject.isEmpty(idKey)) {
            throw new IllegalArgumentException("主键字段名不能为空");
        }

        boolean toUnderlineCase = strategy.isToUnderlineCase();
        List<String> ignoreFieldNames = strategy.getIgnoreFieldNames();

        Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, toUnderlineCase, true, ignoreFieldNames);

        if (toUnderlineCase) {
            idKey = StrUtil.toUnderlineCase(idKey);
        }
        Object id = rowData.get(idKey);
        if (id == null) {
            throw new IllegalArgumentException("实体对象中未找到主键字段[" + idKey + "]的值");
        }

        return bDeleteByPK(tableName, idKey, id);
    }

    @Override
    public <T> Integer bDeleteByPK(T entity, Consumer<DeleteStrategy> strategyConsumer) {
        DeleteStrategy strategy = new DeleteStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        return bDeleteByPK(entity, strategy);
    }

    @Override
    public Integer bDeleteByPKs(String tableName, String idKey, Set<Object> ids) {
        validateTableName(tableName);
        validateIdKey(idKey);
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);

        List<List<Object>> idBatches = splitCollection(ids, getMaxInParams());
        int totalSuccess = 0;

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Connection connection = dataSourceGetter.getConnection();
        try {
            connection.setAutoCommit(false);

            for (List<Object> idBatch : idBatches) {
                String placeholders = idBatch.stream().map(id -> "?").collect(Collectors.joining(","));
                String execSql = buildDeleteBatchByPrimaryKeySql(quoteTableName, idKey, placeholders);
                int batchSuccess = SqlExecutor.execute(connection, execSql, idBatch.toArray());
                totalSuccess += batchSuccess;
            }

            connection.commit();
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();

            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(),
                    "bDeleteByPKs", StrUtil.format("表名：{}，总删除行数：{} ", tableName, totalSuccess), cost, totalSuccess);
            return totalSuccess;
        } catch (SQLException e) {
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(
                    this.getClass(),
                    "bDeleteByPKs", StrUtil.format("表名：{}，总删除行数：{} ", tableName, totalSuccess), e);
            rollbackConnection(connection);
            throw new RuntimeException("批量主键删除失败，表名：" + tableName, e);
        } finally {
            restoreAutoCommit(connection);
            closeConnection(connection);
        }
    }

    @Override
    public Integer bDeleteByPKs(String tableName, String idKey, Set<Object> ids, int batchSize) {
        validateTableName(tableName);
        validateIdKey(idKey);
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }

        List<List<Object>> idBatches = splitCollection(ids, batchSize);
        int totalSuccess = 0;

        for (List<Object> idBatch : idBatches) {
            totalSuccess += bDeleteByPKs(tableName, idKey, new HashSet<>(idBatch));
        }

        return totalSuccess;
    }

    @Override
    public <T> void bDeleteBatchByPK(Collection<T> entities) {
        if (CollUtil.isEmpty(entities)) {
            return;
        }
        DeleteStrategy strategy = new DeleteStrategy()
                .setToUnderlineCase(true);
        bDeleteBatchByPK(entities, strategy);
    }

    @Override
    public <T> void bDeleteBatchByPK(Collection<T> entities, DeleteStrategy strategy) {
        if (CollUtil.isEmpty(entities)) {
            return;
        }

        if (strategy == null) {
            bDeleteBatchByPK(entities);
            return;
        }

        String tableName = strategy.getTableName();
        if (GutilObject.isEmpty(tableName)) {
            T first = entities.iterator().next();
            tableName = GirAdvSqlUtils.getTableName(first.getClass());
        }

        String idKey = strategy.getIdKey();
        if (GutilObject.isEmpty(idKey)) {
            T first = entities.iterator().next();
            List<String> idKeys = GirAdvSqlUtils.getIdByAnnotation(first.getClass());
            if (CollUtil.isNotEmpty(idKeys)) {
                idKey = idKeys.get(0);
            }
        }
        if (GutilObject.isEmpty(idKey)) {
            throw new IllegalArgumentException("主键字段名不能为空");
        }

        boolean toUnderlineCase = strategy.isToUnderlineCase();
        List<String> ignoreFieldNames = strategy.getIgnoreFieldNames();

        Set<Object> ids = new HashSet<>();
        for (T entity : entities) {
            Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, toUnderlineCase, true, ignoreFieldNames);
            String finalIdKey = toUnderlineCase ? StrUtil.toUnderlineCase(idKey) : idKey;
            Object id = rowData.get(finalIdKey);
            if (id != null) {
                ids.add(id);
            }
        }

        if (CollUtil.isNotEmpty(ids)) {
            bDeleteByPKs(tableName, idKey, ids);
        } else {
            throw new RuntimeException("主键值不能为空！");
        }
    }

    @Override
    public <T> void bDeleteBatchByPK(Collection<T> entities, Consumer<DeleteStrategy> strategyConsumer) {
        DeleteStrategy strategy = new DeleteStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        bDeleteBatchByPK(entities, strategy);
    }

    // ========== 3. 简单条件删除 ==========

    @Override
    public Integer bDeleteByMap(String tableName, Map<String, Object> whereMap) {
        validateTableName(tableName);
        if (CollUtil.isEmpty(whereMap)) {
            throw new IllegalArgumentException("删除条件不能为空（禁止全表删除）");
        }
        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        String whereClause = GirAdvSqlUtils.buildWhereClause(whereMap, dialectTableNameProcessor);
        String execSql = buildDeleteByConditionSql(quoteTableName, whereClause);
        List<Object> params = new ArrayList<>(whereMap.values());

        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, execSql, params.toArray());
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), "bDeleteByMap", execSql, params, cost, result);
            return result;
        } catch (SQLException e) {
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(this.getClass(), "bDeleteByMap", execSql, params, e);
            throw new RuntimeException("条件删除失败，表名：" + tableName, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Integer bDeleteByMap(String tableName, Map<String, Object> whereMap, int batchSize) {
        validateTableName(tableName);
        if (CollUtil.isEmpty(whereMap)) {
            throw new IllegalArgumentException("删除条件不能为空（禁止全表删除）");
        }
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }
        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);

        int totalSuccess = 0;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Connection connection = dataSourceGetter.getConnection();

        try {
            connection.setAutoCommit(false);
            while (true) {
                String whereClause = GirAdvSqlUtils.buildWhereClause(whereMap, dialectTableNameProcessor);
                String execSql = buildDeleteBatchByConditionSql(quoteTableName, whereClause, batchSize);
                List<Object> params = new ArrayList<>(whereMap.values());
                int batchSuccess = SqlExecutor.execute(connection, execSql, params.toArray());
                totalSuccess += batchSuccess;
                if (batchSuccess < batchSize) {
                    break;
                }
            }
            connection.commit();
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(),
                    "bDeleteByMap", StrUtil.format("表名：{}，总删除行数：{}，批次大小：{}", tableName, totalSuccess, batchSize), cost, totalSuccess);
            return totalSuccess;
        } catch (SQLException e) {
            rollbackConnection(connection);
            throw new RuntimeException("分批次条件删除失败，表名：" + tableName, e);
        } finally {
            restoreAutoCommit(connection);
            closeConnection(connection);
        }
    }

    // ========== 4. 条件删除 - Lambda表达式 ==========

    @Override
    public <T> Integer bDeleteByWhere(DeleteStrategy strategy, GirAdvWhereLambdaFilter<T> whereFilter) {
        if (whereFilter == null) {
            throw new IllegalArgumentException("删除条件不能为空（禁止全表删除）");
        }

        String tableName = strategy != null ? strategy.getTableName() : null;
        if (GutilObject.isEmpty(tableName)) {
            tableName = GirAdvSqlUtils.getTableName(whereFilter.getEntityClass());
        }
        if (GutilObject.isEmpty(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }

        return bDeleteByWhere(tableName, whereFilter.toWhereFilter());
    }

    @Override
    public <T> Integer bDeleteByWhere(DeleteStrategy strategy, Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("删除条件不能为空（禁止全表删除）");
        }

        Class<T> entityClass = null;
        if (strategy != null && strategy.getTableName() != null) {
            // 如果没有实体类，需要一个默认的，这里需要根据实际情况处理
            throw new IllegalArgumentException("无法确定实体类型，请使用指定表名的方法");
        }

        // 实际上这里需要知道实体类型，建议使用有表名的方法
        throw new UnsupportedOperationException("请使用带表名或实体泛型的方法");
    }

    @Override
    public <T> Integer bDeleteByWhere(Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        return bDeleteByWhere(new DeleteStrategy(), consumer);
    }

    @Override
    public <T> Integer bDeleteByWhere(Consumer<DeleteStrategy> strategyConsumer, Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        DeleteStrategy strategy = new DeleteStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        return bDeleteByWhere(strategy, consumer);
    }

    @Override
    public <T> Integer bDeleteByWhere(String tableName, Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("删除条件不能为空（禁止全表删除）");
        }
        if (GutilObject.isEmpty(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }

        // 这里需要一个默认的实体类型，因为没有实体信息
        // 建议使用者使用其他方法
        throw new UnsupportedOperationException("请使用带实体泛型的方法，如 bDeleteByWhere(Consumer<GirAdvWhereLambdaFilter<T>> consumer)");
    }

    // ========== 5. 条件删除 - 传统Filter ==========

    @Override
    public <T> Integer bDeleteByWhere(String tableName, GirAdvWhereFilter whereFilter) {
        validateTableName(tableName);
        if (whereFilter == null) {
            throw new IllegalArgumentException("删除条件不能为空（禁止全表删除）");
        }
        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        List<Object> params = new ArrayList<>();
        String whereClause = GirAdvSqlUtils.buildWhereClause(whereFilter, params, dialectTableNameProcessor, dataSourceGetter);
        if (GutilObject.isEmpty(whereClause)) {
            throw new IllegalArgumentException("删除条件不能为空（禁止全表删除）");
        }
        String execSql = buildDeleteByConditionSql(quoteTableName, whereClause);

        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, execSql, params.toArray());
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), "bDeleteByWhere", execSql, params, cost, result);
            return result;
        } catch (SQLException e) {
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(this.getClass(), "bDeleteByWhere", execSql, params, e);
            throw new RuntimeException("条件删除失败，表名：" + tableName, e);
        } finally {
            closeConnection(connection);
        }
    }

    // ====================== 工具方法 ======================

    protected void validateTableName(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }
    }

    protected void validateIdKey(String idKey) {
        if (StrUtil.isEmpty(idKey)) {
            throw new IllegalArgumentException("主键/字段名不能为空");
        }
    }

    protected void validateIdKeyAndValue(String idKey, Object id) {
        validateIdKey(idKey);
        if (id == null) {
            throw new IllegalArgumentException("主键/字段值不能为空");
        }
    }

    protected <T> List<List<T>> splitCollection(Collection<T> collection, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        List<T> currentBatch = new ArrayList<>();
        for (T item : collection) {
            currentBatch.add(item);
            if (currentBatch.size() >= batchSize) {
                batches.add(currentBatch);
                currentBatch = new ArrayList<>();
            }
        }
        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }
        return batches;
    }

    protected void closeConnection(Connection connection) {
        if (dataSourceGetter != null) {
            dataSourceGetter.connectionClose(connection);
        }
    }

    protected void rollbackConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                log.error("删除操作回滚失败", e);
            }
        }
    }

    protected void restoreAutoCommit(Connection connection) {
        if (connection != null) {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                log.error("恢复自动提交失败", e);
            }
        }
    }

    protected abstract int getMaxInParams();

    protected String buildDeleteByPrimaryKeySql(String tableName, String idKey) {
        idKey = dialectTableNameProcessor.tbQuoteFieldName(idKey);
        return StrUtil.format("DELETE FROM {} WHERE {} = ?", tableName, idKey);
    }

    protected String buildDeleteBatchByPrimaryKeySql(String tableName, String idKey, String placeholders) {
        idKey = dialectTableNameProcessor.tbQuoteFieldName(idKey);
        return StrUtil.format("DELETE FROM {} WHERE {} IN ({})", tableName, idKey, placeholders);
    }

    protected String buildDeleteByConditionSql(String tableName, String whereClause) {
        return StrUtil.format("DELETE FROM {} WHERE {}", tableName, whereClause);
    }

    protected String buildDeleteBatchByConditionSql(String tableName, String whereClause, int batchSize) {
        return StrUtil.format("DELETE FROM {} WHERE {} LIMIT {}", tableName, whereClause, batchSize);
    }
}
