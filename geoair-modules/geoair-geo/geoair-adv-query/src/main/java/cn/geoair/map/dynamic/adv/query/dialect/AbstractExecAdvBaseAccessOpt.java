package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.strategy.AccessStrategy;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.geoair.map.dynamic.adv.query.utils.AdvLogSql;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.SqlExecutor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 数据库插入操作抽象父类 封装所有数据库通用的插入逻辑
 */
public abstract class AbstractExecAdvBaseAccessOpt implements IAdvBaseAccessOpt {

    protected IDataSourceGetter dataSourceGetter;
    protected DialectTableNameProcessor dialectTableNameProcessor;
    protected static final GiLogger log = GirLogger.getLoger(AbstractExecAdvBaseAccessOpt.class);
    protected static final int DEFAULT_BATCH_SIZE = 1000;

    Supplier<AdvQueryGlobalConfig> configAdvQueryGetter;

    public AbstractExecAdvBaseAccessOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
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

    // ========== 1. 自定义SQL插入 ==========

    @Override
    public Integer bInsertBySql(String sql) {
        return bInsertBySql(sql, SqlParamMap.of());
    }

    @Override
    public Integer bInsertBySql(String dynamicSql, SqlParamMap sqlParamMap) {
        if (StrUtil.isEmpty(dynamicSql)) {
            throw new IllegalArgumentException("插入SQL语句不能为空");
        }
        SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParamMap, dialectTableNameProcessor);
        String execSql = sqlMeta.getSql();
        List<Object> jdbcParams = sqlMeta.getJdbcParamValues();
        return bInsertBySql(execSql, SqlParamList.ofList(jdbcParams));
    }

    @Override
    public Integer bInsertBySql(String sqlStatement, SqlParamList sqlParamList) {
        if (StrUtil.isEmpty(sqlStatement)) {
            throw new IllegalArgumentException("插入SQL语句不能为空");
        }
        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, sqlStatement, sqlParamList.toArray());
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), "bInsertBySql", sqlStatement, sqlParamList.toList(), cost, result);
            return result;
        } catch (SQLException e) {
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(this.getClass(), "bInsertBySql", sqlStatement, sqlParamList, e);
            throw new RuntimeException("插入失败", e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Integer bInsertBySql(String sqlStatementOrDynamicSql, GirSqlParam sqlParam) {
        if (sqlParam == null || GutilObject.isEmpty(sqlParam)) {
            return bInsertBySql(sqlStatementOrDynamicSql);
        } else if (sqlParam instanceof SqlParamMap) {
            return bInsertBySql(sqlStatementOrDynamicSql, (SqlParamMap) sqlParam);
        } else if (sqlParam instanceof SqlParamList) {
            return bInsertBySql(sqlStatementOrDynamicSql, (SqlParamList) sqlParam);
        }
        throw new RuntimeException("不支持的sqlParam参数！");
    }

    // ========== 2. 单条插入 ==========

    @Override
    public Integer bInsertOne(String tableName, Map<String, Object> rowData) {
        validateTableNameAndData(tableName, rowData);

        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);

        List<String> dbKeyList = new ArrayList<>();
        for (String field : rowData.keySet()) {
            dbKeyList.add(dialectTableNameProcessor.tbQuoteFieldName(field));
        }
        String fields = String.join(",", dbKeyList);
        String placeholders = buildPlaceholders(rowData.keySet().size());
        String execSql = buildInsertSql(quoteTableName, fields, placeholders);

        List<Object> params = new ArrayList<>(rowData.values());
        return executeUpdate(execSql, params, "bInsertOne");
    }

    @Override
    public <T> Integer bInsertOne(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("插入的实体对象不能为空");
        }
        AccessStrategy strategy = new AccessStrategy()
                .setToUnderlineCase(true)
                .setIgnoreNullValue(false);
        return bInsertOne(entity, strategy);
    }

    @Override
    public <T> Integer bInsertOne(T entity, AccessStrategy strategy) {
        if (entity == null) {
            throw new IllegalArgumentException("插入的实体对象不能为空");
        }
        if (strategy == null) {
            return bInsertOne(entity);
        }

        String tableName = strategy.getTableName();
        if (GutilObject.isEmpty(tableName)) {
            tableName = GirAdvSqlUtils.getTableName(entity.getClass());
        }
        if (GutilObject.isEmpty(tableName)) {
            throw new IllegalArgumentException("tableName 不能为空");
        }

        boolean toUnderlineCase = strategy.isToUnderlineCase();
        boolean ignoreNullValue = strategy.isIgnoreNullValue();
        List<String> ignoreFieldNames = strategy.getIgnoreFieldNames();

        Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, toUnderlineCase, ignoreNullValue, ignoreFieldNames);

        return bInsertOne(tableName, rowData);
    }

    @Override
    public <T> Integer bInsertOne(T entity, Consumer<AccessStrategy> strategyConsumer) {
        AccessStrategy strategy = new AccessStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        return bInsertOne(entity, strategy);
    }

    // ========== 3. 选择性插入 ==========

    @Override
    public <T> Integer bInsertSelectiveOne(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("插入的实体对象不能为空");
        }
        AccessStrategy strategy = new AccessStrategy()
                .setToUnderlineCase(true)
                .setIgnoreNullValue(true);
        return bInsertOne(entity, strategy);
    }

    @Override
    public <T> Integer bInsertSelectiveOne(T entity, AccessStrategy strategy) {
        if (strategy == null) {
            return bInsertSelectiveOne(entity);
        }
        strategy.setIgnoreNullValue(true);
        return bInsertOne(entity, strategy);
    }

    @Override
    public <T> Integer bInsertSelectiveOne(T entity, Consumer<AccessStrategy> strategyConsumer) {
        AccessStrategy strategy = new AccessStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        strategy.setIgnoreNullValue(true);
        return bInsertOne(entity, strategy);
    }

    // ========== 4. 批量插入 ==========

    @Override
    public Integer bInsertBatch(String tableName, List<String> headers, List<Map<String, Object>> rowsData) {
        return bInsertBatch(tableName, headers, rowsData, DEFAULT_BATCH_SIZE);
    }

    @Override
    public Integer bInsertBatch(String tableName, List<String> headers, List<Map<String, Object>> rowsData, int batchSize) {
        validateTableNameAndData(tableName, headers, rowsData);
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }

        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);

        List<List<Map<String, Object>>> batches = CollUtil.split(rowsData, batchSize);
        int totalSuccess = 0;

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Connection connection = dataSourceGetter.getConnection();
        try {
            connection.setAutoCommit(false);
            List<String> fieldNames = headers.stream()
                    .map(dialectTableNameProcessor::tbQuoteFieldName)
                    .collect(Collectors.toList());
            String fields = String.join(",", fieldNames);
            String placeholders = buildPlaceholders(fieldNames.size());
            String execSql = buildInsertSql(quoteTableName, fields, placeholders);
            PreparedStatement pstmt = connection.prepareStatement(execSql);

            for (List<Map<String, Object>> batch : batches) {
                for (Map<String, Object> row : batch) {
                    int paramIndex = 1;
                    for (String header : headers) {
                        pstmt.setObject(paramIndex++, row.get(header));
                    }
                    pstmt.addBatch();
                }
                int[] batchResults = pstmt.executeBatch();
                totalSuccess += Arrays.stream(batchResults).sum();
                pstmt.clearBatch();
            }

            connection.commit();
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(
                    this.getClass(), "bInsertBatch",
                    StrUtil.format("表名：{}，总条数：{}，批次大小：{}", tableName, totalSuccess, batchSize),
                    cost, totalSuccess);
            return totalSuccess;
        } catch (SQLException e) {
            rollbackConnection(connection);
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(
                    this.getClass(), "bInsertBatch",
                    StrUtil.format("表名：{}，总条数：{}，批次大小：{}", tableName, totalSuccess, batchSize), e);
            throw new RuntimeException("批量插入失败，表名：" + tableName, e);
        } finally {
            restoreAutoCommit(connection);
            closeConnection(connection);
        }
    }

    @Override
    public <T> Integer bInsertBatch(Collection<T> entities) {
        if (CollUtil.isEmpty(entities)) {
            return 0;
        }
        AccessStrategy strategy = new AccessStrategy()
                .setToUnderlineCase(true)
                .setIgnoreNullValue(false);
        return bInsertBatch(entities, strategy);
    }

    @Override
    public <T> Integer bInsertBatch(Collection<T> entities, AccessStrategy strategy) {
        if (CollUtil.isEmpty(entities)) {
            return 0;
        }
        if (strategy == null) {
            return bInsertBatch(entities);
        }

        String tableName = strategy.getTableName();
        if (GutilObject.isEmpty(tableName)) {
            T first = entities.iterator().next();
            tableName = GirAdvSqlUtils.getTableName(first.getClass());
        }

        boolean toUnderlineCase = strategy.isToUnderlineCase();
        boolean ignoreNullValue = strategy.isIgnoreNullValue();
        List<String> ignoreFieldNames = strategy.getIgnoreFieldNames();

        List<Map<String, Object>> rowsData = new ArrayList<>();
        for (T entity : entities) {
            Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, toUnderlineCase, ignoreNullValue, ignoreFieldNames);
            rowsData.add(rowData);
        }

        Set<String> headers = rowsData.get(0).keySet();
        return bInsertBatch(tableName, ListUtil.toList(headers), rowsData);
    }

    @Override
    public <T> Integer bInsertBatch(Collection<T> entities, Consumer<AccessStrategy> strategyConsumer) {
        AccessStrategy strategy = new AccessStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        return bInsertBatch(entities, strategy);
    }

    @Override
    public <T> Integer bInsertBatch(String tableName, Collection<T> entities) {
        AccessStrategy strategy = new AccessStrategy()
                .setTableName(tableName)
                .setToUnderlineCase(true)
                .setIgnoreNullValue(false);
        return bInsertBatch(entities, strategy);
    }

    @Override
    public <T> Integer bInsertBatch(String tableName, Collection<T> entities, AccessStrategy strategy) {
        if (strategy == null) {
            return bInsertBatch(tableName, entities);
        }
        strategy.setTableName(tableName);
        return bInsertBatch(entities, strategy);
    }

    @Override
    public <T> Integer bInsertBatch(String tableName, Collection<T> entities, Consumer<AccessStrategy> strategyConsumer) {
        AccessStrategy strategy = new AccessStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        strategy.setTableName(tableName);
        return bInsertBatch(entities, strategy);
    }

    // ========== 5. 插入或忽略 ==========

    @Override
    public Integer bInsertIgnore(String tableName, Map<String, Object> rowData) {
        return bInsertIgnore(tableName, rowData, ListUtil.empty());
    }

    @Override
    public Integer bInsertIgnore(String tableName, Map<String, Object> rowData, List<String> conflictKeys) {
        validateTableNameAndData(tableName, rowData);

        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);

        List<String> dbKeyList = new ArrayList<>();
        for (String field : rowData.keySet()) {
            dbKeyList.add(dialectTableNameProcessor.tbQuoteFieldName(field));
        }
        String fields = String.join(",", dbKeyList);
        String placeholders = buildPlaceholders(rowData.keySet().size());
        String execSql = buildInsertIgnoreSql(quoteTableName, fields, placeholders, conflictKeys);

        List<Object> params = new ArrayList<>(rowData.values());
        return executeUpdate(execSql, params, "bInsertIgnore");
    }

    @Override
    public <T> Integer bInsertIgnore(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("插入的实体对象不能为空");
        }
        AccessStrategy strategy = new AccessStrategy()
                .setToUnderlineCase(true)
                .setIgnoreNullValue(false);
        return bInsertIgnore(entity, strategy);
    }

    @Override
    public <T> Integer bInsertIgnore(T entity, AccessStrategy strategy) {
        if (entity == null) {
            throw new IllegalArgumentException("插入的实体对象不能为空");
        }
        if (strategy == null) {
            return bInsertIgnore(entity);
        }

        String tableName = strategy.getTableName();
        if (GutilObject.isEmpty(tableName)) {
            tableName = GirAdvSqlUtils.getTableName(entity.getClass());
        }

        List<String> conflictKeys = strategy.getConflictKeys();
        if (CollUtil.isEmpty(conflictKeys)) {
            conflictKeys = GirAdvSqlUtils.getIdByAnnotation(entity.getClass());
        }

        boolean toUnderlineCase = strategy.isToUnderlineCase();
        boolean ignoreNullValue = strategy.isIgnoreNullValue();
        List<String> ignoreFieldNames = strategy.getIgnoreFieldNames();

        Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, toUnderlineCase, ignoreNullValue, ignoreFieldNames);

        List<String> finalConflictKeys = conflictKeys;
        if (toUnderlineCase && CollUtil.isNotEmpty(conflictKeys)) {
            finalConflictKeys = new ArrayList<>();
            for (String key : conflictKeys) {
                finalConflictKeys.add(StrUtil.toUnderlineCase(key));
            }
        }

        return bInsertIgnore(tableName, rowData, finalConflictKeys);
    }

    @Override
    public <T> Integer bInsertIgnore(T entity, Consumer<AccessStrategy> strategyConsumer) {
        AccessStrategy strategy = new AccessStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        return bInsertIgnore(entity, strategy);
    }

    // ========== 6. 选择性插入或忽略 ==========

    @Override
    public <T> Integer bInsertSelectiveIgnore(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("插入的实体对象不能为空");
        }
        AccessStrategy strategy = new AccessStrategy()
                .setToUnderlineCase(true)
                .setIgnoreNullValue(true);
        return bInsertIgnore(entity, strategy);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnore(T entity, AccessStrategy strategy) {
        if (strategy == null) {
            return bInsertSelectiveIgnore(entity);
        }
        strategy.setIgnoreNullValue(true);
        return bInsertIgnore(entity, strategy);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnore(T entity, Consumer<AccessStrategy> strategyConsumer) {
        AccessStrategy strategy = new AccessStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        strategy.setIgnoreNullValue(true);
        return bInsertIgnore(entity, strategy);
    }

    // ========== 7. 批量插入或忽略 ==========

    @Override
    public Integer bInsertIgnoreBatch(String tableName, Set<String> headers, List<Map<String, Object>> rowsData, List<String> conflictKeys) {
        validateTableNameAndData(tableName, ListUtil.toList(headers), rowsData);

        List<List<Map<String, Object>>> batches = CollUtil.split(rowsData, DEFAULT_BATCH_SIZE);
        int totalSuccess = 0;

        for (List<Map<String, Object>> batch : batches) {
            for (Map<String, Object> row : batch) {
                totalSuccess += bInsertIgnore(tableName, row, conflictKeys);
            }
        }

        return totalSuccess;
    }

    @Override
    public <T> Integer bInsertIgnoreBatch(Collection<T> entities, List<String> conflictKeys) {
        if (CollUtil.isEmpty(entities)) {
            return 0;
        }
        AccessStrategy strategy = new AccessStrategy()
                .setToUnderlineCase(true)
                .setIgnoreNullValue(false)
                .setConflictKeys(conflictKeys);
        return bInsertIgnoreBatch(entities, conflictKeys, strategy);
    }

    @Override
    public <T> Integer bInsertIgnoreBatch(Collection<T> entities, List<String> conflictKeys, AccessStrategy strategy) {
        if (CollUtil.isEmpty(entities)) {
            return 0;
        }
        if (strategy == null) {
            return bInsertIgnoreBatch(entities, conflictKeys);
        }

        String tableName = strategy.getTableName();
        if (GutilObject.isEmpty(tableName)) {
            T first = entities.iterator().next();
            tableName = GirAdvSqlUtils.getTableName(first.getClass());
        }

        boolean toUnderlineCase = strategy.isToUnderlineCase();
        boolean ignoreNullValue = strategy.isIgnoreNullValue();
        List<String> ignoreFieldNames = strategy.getIgnoreFieldNames();

        int totalSuccess = 0;
        for (T entity : entities) {
            Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, toUnderlineCase, ignoreNullValue, ignoreFieldNames);
            List<String> finalConflictKeys = conflictKeys;
            if (toUnderlineCase && CollUtil.isNotEmpty(conflictKeys)) {
                finalConflictKeys = new ArrayList<>();
                for (String key : conflictKeys) {
                    finalConflictKeys.add(StrUtil.toUnderlineCase(key));
                }
            }
            totalSuccess += bInsertIgnore(tableName, rowData, finalConflictKeys);
        }

        return totalSuccess;
    }

    @Override
    public <T> Integer bInsertIgnoreBatch(Collection<T> entities, List<String> conflictKeys, Consumer<AccessStrategy> strategyConsumer) {
        AccessStrategy strategy = new AccessStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        return bInsertIgnoreBatch(entities, conflictKeys, strategy);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnoreBatch(Collection<T> entities, List<String> conflictKeys) {
        AccessStrategy strategy = new AccessStrategy()
                .setToUnderlineCase(true)
                .setIgnoreNullValue(true)
                .setConflictKeys(conflictKeys);
        return bInsertIgnoreBatch(entities, conflictKeys, strategy);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnoreBatch(Collection<T> entities, List<String> conflictKeys, AccessStrategy strategy) {
        if (strategy == null) {
            return bInsertSelectiveIgnoreBatch(entities, conflictKeys);
        }
        strategy.setIgnoreNullValue(true);
        return bInsertIgnoreBatch(entities, conflictKeys, strategy);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnoreBatch(Collection<T> entities, List<String> conflictKeys, Consumer<AccessStrategy> strategyConsumer) {
        AccessStrategy strategy = new AccessStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        strategy.setIgnoreNullValue(true);
        return bInsertIgnoreBatch(entities, conflictKeys, strategy);
    }

    // ====================== 工具方法 ======================

    private Integer executeUpdate(String sql, List<Object> params, String methodName) {
        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, sql, params.toArray());
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), methodName, sql, params, cost, result);
            return result;
        } catch (SQLException e) {
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(this.getClass(), methodName, sql, params, e);
            throw new RuntimeException("插入操作失败，SQL：" + sql, e);
        } finally {
            closeConnection(connection);
        }
    }

    protected String buildPlaceholders(int count) {
        return StrUtil.repeatAndJoin("?", count, ",");
    }

    protected String buildInsertSql(String tableName, String fields, String placeholders) {
        return StrUtil.format("INSERT INTO {} ({}) VALUES ({})", tableName, fields, placeholders);
    }

    protected abstract String buildInsertIgnoreSql(
            String tableName, String fields, String placeholders, List<String> conflictKeys);

    protected void validateTableNameAndData(String tableName, Map<String, Object> rowData) {
        validateTableName(tableName);
        if (CollUtil.isEmpty(rowData)) {
            throw new IllegalArgumentException("插入的数据不能为空");
        }
    }

    protected void validateTableNameAndData(String tableName, List<String> headers, List<Map<String, Object>> rowsData) {
        validateTableName(tableName);
        if (CollUtil.isEmpty(headers)) {
            throw new IllegalArgumentException("插入的字段头不能为空");
        }
        if (CollUtil.isEmpty(rowsData)) {
            throw new IllegalArgumentException("插入的数据不能为空");
        }
    }

    protected void validateTableName(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }
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
                log.error("批量插入回滚失败", e);
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
}
