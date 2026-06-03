package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.strategy.UpdateStrategy;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.geoair.map.dynamic.adv.query.utils.AdvLogSql;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereLambdaFilter;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.SqlExecutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 数据库更新操作抽象父类 封装所有数据库通用的更新逻辑，差异化语法由子类实现
 */
public abstract class AbstractExecAdvBaseUpdateOpt implements IAdvBaseUpdateOpt {

    Supplier<AdvQueryGlobalConfig> configAdvQueryGetter;

    public AbstractExecAdvBaseUpdateOpt(Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        this.configAdvQueryGetter = configAdvQueryGetter;
    }

    @Override
    public AdvQueryGlobalConfig getConfig() {
        return configAdvQueryGetter.get();
    }

    protected IDataSourceGetter dataSourceGetter;
    protected DialectTableNameProcessor dialectTableNameProcessor;

    protected static final GiLogger log = GirLogger.getLoger(AbstractExecAdvBaseUpdateOpt.class);
    protected static final int DEFAULT_BATCH_SIZE = 200;

    @Override
    public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
    }

    // ==================== 1. 自定义SQL更新 ====================

    @Override
    public Integer bUpdateBySql(String sqlStatement) {
        return bUpdateBySql(sqlStatement, SqlParamMap.of());
    }

    @Override
    public Integer bUpdateBySql(String dynamicSql, SqlParamMap sqlParam) {
        if (StrUtil.isEmpty(dynamicSql)) {
            throw new IllegalArgumentException("更新SQL语句不能为空");
        }
        SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParam, dialectTableNameProcessor);
        String execSql = sqlMeta.getSql();
        List<Object> jdbcParams = sqlMeta.getJdbcParamValues();
        return bUpdateBySql(execSql, SqlParamList.ofList(jdbcParams));
    }

    @Override
    public Integer bUpdateBySql(String sqlStatement, SqlParamList sqlParam) {
        if (StrUtil.isEmpty(sqlStatement)) {
            throw new IllegalArgumentException("更新SQL语句不能为空");
        }
        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result;
            if (GutilObject.isEmpty(sqlParam)) {
                result = SqlExecutor.execute(connection, sqlStatement);
            } else {
                result = SqlExecutor.execute(connection, sqlStatement, sqlParam.toArray());
            }
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), "bUpdateBySql", sqlStatement, sqlParam, cost, result);
            return result;
        } catch (SQLException e) {
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(this.getClass(), "bUpdateBySql", sqlStatement, sqlParam, e);
            throw new RuntimeException("执行自定义更新SQL失败，SQL：" + sqlStatement, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Integer bUpdateBySql(String sqlStatement, GirSqlParam sqlParam) {
        if (sqlParam == null || GutilObject.isEmpty(sqlParam)) {
            return bUpdateBySql(sqlStatement);
        } else if (sqlParam instanceof SqlParamList) {
            return bUpdateBySql(sqlStatement, (SqlParamList) sqlParam);
        } else if (sqlParam instanceof SqlParamMap) {
            return bUpdateBySql(sqlStatement, (SqlParamMap) sqlParam);
        } else {
            throw new RuntimeException("SqlParam参数不合法！");
        }
    }

    // ==================== 2. 按主键更新 ====================

    @Override
    public Integer bUpdateByPK(String tableName, String idKey, Object id, Map<String, Object> rowData) {
        validateTableName(tableName);
        validateIdKey(idKey);
        validateUpdateData(rowData);
        if (id == null) {
            throw new IllegalArgumentException("主键值不能为空");
        }

        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(
                dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        String setClause = GirAdvSqlUtils.buildSetClause(rowData, dialectTableNameProcessor);
        String execSql = StrUtil.format("UPDATE {} SET {} WHERE {} = ?",
                quoteTableName, setClause, dialectTableNameProcessor.tbQuoteFieldName(idKey));

        List<Object> params = new ArrayList<>(rowData.values());
        params.add(id);

        return executeUpdate(execSql, params, "bUpdateByPK");
    }

    @Override
    public <T> Integer bUpdateByPK(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("更新的实体对象不能为空");
        }
        String tableName = GirAdvSqlUtils.getTableName(entity.getClass());
        List<String> idKeys = GirAdvSqlUtils.getIdByAnnotation(entity.getClass());
        if (CollUtil.isEmpty(idKeys)) {
            throw new IllegalArgumentException("实体对象中未找到主键字段");
        }
        UpdateStrategy strategy = new UpdateStrategy()
                .setTableName(tableName)
                .setIdKey(idKeys.get(0))
                .setToUnderlineCase(true)
                .setIgnoreNullValue(false);
        return bUpdateByPK(entity, strategy);
    }

    @Override
    public <T> Integer bUpdateByPK(T entity, UpdateStrategy strategy) {
        if (entity == null) {
            throw new IllegalArgumentException("更新的实体对象不能为空");
        }
        if (strategy == null) {
            return bUpdateByPK(entity);
        }

        String tableName = strategy.getTableName();
        if (GutilObject.isEmpty(tableName)) {
            tableName = GirAdvSqlUtils.getTableName(entity.getClass());
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
        boolean ignoreNullValue = strategy.isIgnoreNullValue();
        List<String> ignoreFieldNames = strategy.getIgnoreFieldNames();

        Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, toUnderlineCase, ignoreNullValue, ignoreFieldNames);

        if (toUnderlineCase) {
            idKey = StrUtil.toUnderlineCase(idKey);
        }
        Object id = rowData.remove(idKey);
        if (id == null) {
            throw new IllegalArgumentException("实体对象中未找到主键字段[" + idKey + "]的值");
        }

        return bUpdateByPK(tableName, idKey, id, rowData);
    }

    @Override
    public <T> Integer bUpdateByPK(T entity, Consumer<UpdateStrategy> strategyConsumer) {
        UpdateStrategy strategy = new UpdateStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        return bUpdateByPK(entity, strategy);
    }

// ==================== 单条选择性更新 ====================

    @Override
    public <T> Integer bUpdateByPKSelective(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("更新的实体对象不能为空");
        }
        String tableName = GirAdvSqlUtils.getTableName(entity.getClass());
        List<String> idKeys = GirAdvSqlUtils.getIdByAnnotation(entity.getClass());
        if (CollUtil.isEmpty(idKeys)) {
            throw new IllegalArgumentException("实体对象中未找到主键字段");
        }
        UpdateStrategy strategy = new UpdateStrategy()
                .setTableName(tableName)
                .setIdKey(idKeys.get(0))
                .setToUnderlineCase(true)
                .setIgnoreNullValue(true);  // 选择性：忽略null值
        return bUpdateByPKSelective(entity, strategy);
    }

    @Override
    public <T> Integer bUpdateByPKSelective(T entity, UpdateStrategy strategy) {
        if (entity == null) {
            throw new IllegalArgumentException("更新的实体对象不能为空");
        }
        if (strategy == null) {
            return bUpdateByPKSelective(entity);
        }

        // 强制设置为选择性更新
        strategy.setIgnoreNullValue(true);

        String tableName = strategy.getTableName();
        if (GutilObject.isEmpty(tableName)) {
            tableName = GirAdvSqlUtils.getTableName(entity.getClass());
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
        Object id = rowData.remove(idKey);
        if (id == null) {
            throw new IllegalArgumentException("实体对象中未找到主键字段[" + idKey + "]的值");
        }

        return bUpdateByPK(tableName, idKey, id, rowData);
    }

    @Override
    public <T> Integer bUpdateByPKSelective(T entity, Consumer<UpdateStrategy> strategyConsumer) {
        UpdateStrategy strategy = new UpdateStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        return bUpdateByPKSelective(entity, strategy);
    }


    @Override
    public void bUpdateBatchByPK(String tableName, String idKey, List<Map<String, Object>> rowsData) {
        bUpdateBatchByPK(tableName, idKey, rowsData, DEFAULT_BATCH_SIZE);
    }

    @Override
    public void bUpdateBatchByPK(String tableName, String idKey, List<Map<String, Object>> rowsData, int batchSize) {
        validateTableName(tableName);
        validateIdKey(idKey);
        if (CollUtil.isEmpty(rowsData)) {
            return;
        }
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }

        List<List<Map<String, Object>>> batches = CollUtil.split(rowsData, batchSize);

        for (List<Map<String, Object>> batch : batches) {
            List<String> sqls = new ArrayList<>();
            List<Object> params = new ArrayList<>();
            for (Map<String, Object> row : batch) {
                Object id = row.get(idKey);
                if (id == null) {
                    throw new IllegalArgumentException("批量更新数据中缺少主键字段[" + idKey + "]的值");
                }
                Map<String, Object> updateData = new HashMap<>(row);
                updateData.remove(idKey);

                String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
                String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
                String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(
                        dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
                String setClause = GirAdvSqlUtils.buildSetClause(updateData, dialectTableNameProcessor);
                String sql = StrUtil.format("UPDATE {} SET {} WHERE {} = ?",
                        quoteTableName, setClause, dialectTableNameProcessor.tbQuoteFieldName(idKey));
                sqls.add(sql);
                params.addAll(updateData.values());
                params.add(id);
            }
            bUpdateBySql(StrUtil.join("; \n", sqls), SqlParamList.of(params));
        }
    }

    @Override
    public <T> void bUpdateBatchByPK(String tableName, String idKey, Collection<T> entities) {
        if (CollUtil.isEmpty(entities)) {
            return;
        }
        List<Map<String, Object>> rowsData = new ArrayList<>();
        for (T entity : entities) {
            Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, true, false, ListUtil.empty());
            rowsData.add(rowData);
        }
        bUpdateBatchByPK(tableName, idKey, rowsData);
    }

    @Override
    public <T> void bUpdateBatchByPK(Collection<T> entities, UpdateStrategy strategy) {
        if (CollUtil.isEmpty(entities)) {
            return;
        }
        if (strategy == null) {
            T first = entities.iterator().next();
            String tableName = GirAdvSqlUtils.getTableName(first.getClass());
            List<String> idKeys = GirAdvSqlUtils.getIdByAnnotation(first.getClass());
            bUpdateBatchByPK(tableName, idKeys.get(0), entities);
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
            idKey = idKeys.get(0);
        }

        List<Map<String, Object>> rowsData = new ArrayList<>();
        for (T entity : entities) {
            Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity,
                    strategy.isToUnderlineCase(), strategy.isIgnoreNullValue(), strategy.getIgnoreFieldNames());
            rowsData.add(rowData);
        }
        bUpdateBatchByPK(tableName, idKey, rowsData);
    }

    @Override
    public <T> void bUpdateBatchByPK(Collection<T> entities, Consumer<UpdateStrategy> strategyConsumer) {
        UpdateStrategy strategy = new UpdateStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        bUpdateBatchByPK(entities, strategy);
    }
// ==================== 批量选择性更新 ====================

    @Override
    public <T> void bUpdateBatchByPKSelective(String tableName, String idKey, Collection<T> entities) {
        if (CollUtil.isEmpty(entities)) {
            return;
        }
        validateTableName(tableName);
        validateIdKey(idKey);

        List<Map<String, Object>> rowsData = new ArrayList<>();
        for (T entity : entities) {
            // 忽略null值字段
            Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, true, true, ListUtil.empty());
            rowsData.add(rowData);
        }
        bUpdateBatchByPK(tableName, idKey, rowsData);
    }

    @Override
    public <T> void bUpdateBatchByPKSelective(Collection<T> entities, UpdateStrategy strategy) {
        if (CollUtil.isEmpty(entities)) {
            return;
        }

        if (strategy == null) {
            T first = entities.iterator().next();
            String tableName = GirAdvSqlUtils.getTableName(first.getClass());
            List<String> idKeys = GirAdvSqlUtils.getIdByAnnotation(first.getClass());
            bUpdateBatchByPKSelective(tableName, idKeys.get(0), entities);
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
            idKey = idKeys.get(0);
        }

        boolean toUnderlineCase = strategy.isToUnderlineCase();
        List<String> ignoreFieldNames = strategy.getIgnoreFieldNames();

        List<Map<String, Object>> rowsData = new ArrayList<>();
        for (T entity : entities) {
            // 使用策略中的配置，但强制忽略null值
            Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, toUnderlineCase, true, ignoreFieldNames);
            rowsData.add(rowData);
        }
        bUpdateBatchByPK(tableName, idKey, rowsData);
    }

    @Override
    public <T> void bUpdateBatchByPKSelective(Collection<T> entities, Consumer<UpdateStrategy> strategyConsumer) {
        UpdateStrategy strategy = new UpdateStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        bUpdateBatchByPKSelective(entities, strategy);
    }
    // ==================== 3. 简单条件更新 ====================

    @Override
    public Integer bUpdateByMap(String tableName, Map<String, Object> rowData, Map<String, Object> whereMap) {
        validateTableName(tableName);
        validateUpdateData(rowData);
        if (CollUtil.isEmpty(whereMap)) {
            throw new IllegalArgumentException("更新条件不能为空（避免全表更新）");
        }

        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(
                dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        String setClause = GirAdvSqlUtils.buildSetClause(rowData, dialectTableNameProcessor);
        String whereClause = GirAdvSqlUtils.buildWhereClause(whereMap, dialectTableNameProcessor);
        String execSql = StrUtil.format("UPDATE {} SET {} WHERE {}", quoteTableName, setClause, whereClause);

        List<Object> params = new ArrayList<>(rowData.values());
        params.addAll(whereMap.values());

        return executeUpdate(execSql, params, "bUpdateByMap");
    }

    // ==================== 4. 复杂条件更新 - Lambda表达式 ====================

    @Override
    public <T> Integer bUpdateByWhere(T entity, UpdateStrategy strategy, GirAdvWhereLambdaFilter<T> whereFilter) {
        if (entity == null) {
            throw new IllegalArgumentException("更新的实体对象不能为空");
        }
        if (whereFilter == null) {
            throw new IllegalArgumentException("更新条件不能为空（避免全表更新）");
        }

        String tableName = strategy != null ? strategy.getTableName() : null;
        if (GutilObject.isEmpty(tableName)) {
            tableName = GirAdvSqlUtils.getTableName(whereFilter.getEntityClass());
        }

        boolean toUnderlineCase = whereFilter.isToUnderlineCase();
        boolean ignoreNullValue = strategy != null && strategy.isIgnoreNullValue();
        List<String> ignoreFieldNames = strategy != null ? strategy.getIgnoreFieldNames() : ListUtil.empty();

        Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, toUnderlineCase, ignoreNullValue, ignoreFieldNames);

        return bUpdateByWhere(tableName, rowData, whereFilter.toWhereFilter());
    }

    @Override
    public <T> Integer bUpdateByWhere(T entity, UpdateStrategy strategy, Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        Class<T> entityClass = (Class<T>) entity.getClass();
        GirAdvWhereLambdaFilter<T> lambdaFilter = GirAdvWhereLambdaFilter.of(entityClass, true);
        if (consumer != null) {
            consumer.accept(lambdaFilter);
        }
        return bUpdateByWhere(entity, strategy, lambdaFilter);
    }

    @Override
    public <T> Integer bUpdateByWhere(T entity, Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        return bUpdateByWhere(entity, new UpdateStrategy(), consumer);
    }

    @Override
    public <T> Integer bUpdateByWhere(T entity, Consumer<UpdateStrategy> strategyConsumer, Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        UpdateStrategy strategy = new UpdateStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        return bUpdateByWhere(entity, strategy, consumer);
    }

    @Override
    public <T> Integer bUpdateByWhere(String tableName, T entity, Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        UpdateStrategy strategy = new UpdateStrategy().setTableName(tableName);
        return bUpdateByWhere(entity, strategy, consumer);
    }

    // ==================== 5. 复杂条件更新 - 传统Filter ====================

    @Override
    public <T> Integer bUpdateByWhere(String tableName, Map<String, Object> rowData, GirAdvWhereFilter whereFilter) {
        validateTableName(tableName);
        validateUpdateData(rowData);

        List<Object> whereParams = new ArrayList<>();
        String whereClause = GirAdvSqlUtils.buildWhereClause(whereFilter, whereParams, dialectTableNameProcessor, dataSourceGetter);
        if (GutilObject.isEmpty(whereClause)) {
            throw new RuntimeException("更新条件不能为空（避免全表更新）");
        }

        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(
                dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        String setClause = GirAdvSqlUtils.buildSetClause(rowData, dialectTableNameProcessor);
        String execSql = StrUtil.format("UPDATE {} SET {} WHERE {}", quoteTableName, setClause, whereClause);

        List<Object> params = new ArrayList<>(rowData.values());
        params.addAll(whereParams);

        return executeUpdate(execSql, params, "bUpdateByWhere");
    }

    @Override
    public <T> Integer bUpdateSelectiveByWhere(String tableName, Map<String, Object> rowData, GirAdvWhereFilter whereFilter) {
        // 过滤掉 value 为 null 的数据
        Map<String, Object> filteredRowData = rowData.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return bUpdateByWhere(tableName, filteredRowData, whereFilter);
    }

    // ==================== 6. UPSERT操作 ====================

    @Override
    public Integer bUpsert(String tableName, Map<String, Object> rowData, List<String> conflictKeys) {
        Pair<String, List<Object>> upsertSql = getUpsertSql(tableName, rowData, conflictKeys);
        return executeUpdate(upsertSql.getKey(), upsertSql.getValue(), "bUpsert");
    }


    public Pair<String, List<Object>> getUpsertSql(String tableName, Map<String, Object> rowData, List<String> conflictKeys) {
        validateTableName(tableName);
        validateUpdateData(rowData);
        if (CollUtil.isEmpty(conflictKeys)) {
            throw new IllegalArgumentException("冲突判定字段不能为空");
        }

        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(
                dataSourceGetter, tableNameNotSchema, schemaNameByTableName);

        Set<String> mapKeySet = rowData.keySet();
        List<String> dbKeyList = new ArrayList<>();
        for (String field : mapKeySet) {
            dbKeyList.add(dialectTableNameProcessor.tbQuoteFieldName(field));
        }
        String fields = String.join(",", dbKeyList);
        String placeholders = dbKeyList.stream().map(key -> "?").collect(Collectors.joining(","));

        List<String> conflictKeysList = new ArrayList<>();
        for (String field : conflictKeys) {
            conflictKeysList.add(dialectTableNameProcessor.tbQuoteFieldName(field));
        }
        String conflictFields = String.join(",", conflictKeysList);
        String updateClause = buildUpsertUpdateClause(rowData, conflictKeysList);
        String execSql = buildUpdateOrInsertSql(quoteTableName, fields, placeholders, conflictFields, updateClause);
        List<Object> params = new ArrayList<>(rowData.values());
        return Pair.of(execSql, params);
    }

    @Override
    public <T> Integer bUpsert(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("UPSERT的实体对象不能为空");
        }
        List<String> idByAnnotation = GirAdvSqlUtils.getIdByAnnotation(entity.getClass());
        if (GutilObject.isEmpty(idByAnnotation)) {
            throw new IllegalArgumentException("实体对象中未找到主键字段，无法执行UPSERT");
        }
        UpdateStrategy strategy = new UpdateStrategy()
                .setConflictKeys(idByAnnotation)
                .setToUnderlineCase(true)
                .setIgnoreNullValue(false);
        return bUpsert(entity, strategy);
    }

    @Override
    public <T> Integer bUpsert(T entity, UpdateStrategy strategy) {
        if (entity == null) {
            throw new IllegalArgumentException("UPSERT的实体对象不能为空");
        }
        if (strategy == null) {
            return bUpsert(entity);
        }

        String tableName = strategy.getTableName();
        if (GutilObject.isEmpty(tableName)) {
            tableName = GirAdvSqlUtils.getTableName(entity.getClass());
        }

        List<String> conflictKeys = strategy.getConflictKeys();
        if (CollUtil.isEmpty(conflictKeys)) {
            List<String> idByAnnotation = GirAdvSqlUtils.getIdByAnnotation(entity.getClass());
            if (CollUtil.isNotEmpty(idByAnnotation)) {
                conflictKeys = idByAnnotation;
            }
        }
        if (CollUtil.isEmpty(conflictKeys)) {
            throw new IllegalArgumentException("冲突判定字段不能为空");
        }

        boolean toUnderlineCase = strategy.isToUnderlineCase();
        boolean ignoreNullValue = strategy.isIgnoreNullValue();
        List<String> ignoreFieldNames = strategy.getIgnoreFieldNames();

        Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, toUnderlineCase, ignoreNullValue, ignoreFieldNames);

        if (toUnderlineCase) {
            List<String> underlineConflictKeys = new ArrayList<>();
            for (String key : conflictKeys) {
                underlineConflictKeys.add(StrUtil.toUnderlineCase(key));
            }
            conflictKeys = underlineConflictKeys;
        }

        return bUpsert(tableName, rowData, conflictKeys);
    }

    @Override
    public <T> Integer bUpsert(T entity, Consumer<UpdateStrategy> strategyConsumer) {
        UpdateStrategy strategy = new UpdateStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        return bUpsert(entity, strategy);
    }

    @Override
    public <T> Integer bUpsertSelective(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("UPSERT的实体对象不能为空");
        }
        List<String> idByAnnotation = GirAdvSqlUtils.getIdByAnnotation(entity.getClass());
        if (GutilObject.isEmpty(idByAnnotation)) {
            throw new IllegalArgumentException("实体对象中未找到主键字段，无法执行UPSERT");
        }
        UpdateStrategy strategy = new UpdateStrategy()
                .setConflictKeys(idByAnnotation)
                .setToUnderlineCase(true)
                .setIgnoreNullValue(true);
        return bUpsert(entity, strategy);
    }

    @Override
    public <T> Integer bUpsertSelective(T entity, UpdateStrategy strategy) {
        if (strategy == null) {
            return bUpsertSelective(entity);
        }
        strategy.setIgnoreNullValue(true);
        return bUpsert(entity, strategy);
    }

    @Override
    public <T> Integer bUpsertSelective(T entity, Consumer<UpdateStrategy> strategyConsumer) {
        UpdateStrategy strategy = new UpdateStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        strategy.setIgnoreNullValue(true);
        return bUpsert(entity, strategy);
    }


    // ==================== 批量 UPSERT ====================

    @Override
    public void bUpsertBatch(String tableName, List<Map<String, Object>> rowsData, List<String> conflictKeys) {
        validateTableName(tableName);
        if (CollUtil.isEmpty(rowsData)) {
            return;
        }
        if (CollUtil.isEmpty(conflictKeys)) {
            throw new IllegalArgumentException("冲突判定字段不能为空");
        }
        List<String> sqls = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (Map<String, Object> rowData : rowsData) {
            Pair<String, List<Object>> upsertSql = getUpsertSql(tableName, rowData, conflictKeys);
            sqls.add(dialectTableNameProcessor.tbRemoveSqlSpaces(upsertSql.getKey()));
            params.addAll(upsertSql.getValue());
        }
        bUpdateBySql(StrUtil.join("; \n", sqls), SqlParamList.of(params));
    }

    @Override
    public <T> void bUpsertBatch(Collection<T> entities, List<String> conflictKeys) {
        if (CollUtil.isEmpty(entities)) {
            return;
        }
        if (CollUtil.isEmpty(conflictKeys)) {
            T first = entities.iterator().next();
            conflictKeys = GirAdvSqlUtils.getIdByAnnotation(first.getClass());
            if (CollUtil.isEmpty(conflictKeys)) {
                throw new IllegalArgumentException("冲突判定字段不能为空");
            }
        }

        UpdateStrategy strategy = new UpdateStrategy()
                .setConflictKeys(conflictKeys)
                .setToUnderlineCase(true)
                .setIgnoreNullValue(false);
        bUpsertBatch(entities, strategy);
    }

    @Override
    public <T> void bUpsertBatch(Collection<T> entities, UpdateStrategy strategy) {
        if (CollUtil.isEmpty(entities)) {
            return;
        }
        if (strategy == null) {
            bUpsertBatch(entities, (List<String>) null);
            return;
        }

        String tableName = strategy.getTableName();
        if (GutilObject.isEmpty(tableName)) {
            T first = entities.iterator().next();
            tableName = GirAdvSqlUtils.getTableName(first.getClass());
        }

        List<String> conflictKeys = strategy.getConflictKeys();
        if (CollUtil.isEmpty(conflictKeys)) {
            T first = entities.iterator().next();
            conflictKeys = GirAdvSqlUtils.getIdByAnnotation(first.getClass());
            if (CollUtil.isEmpty(conflictKeys)) {
                throw new IllegalArgumentException("冲突判定字段不能为空");
            }
        }

        boolean toUnderlineCase = strategy.isToUnderlineCase();
        boolean ignoreNullValue = strategy.isIgnoreNullValue();
        List<String> ignoreFieldNames = strategy.getIgnoreFieldNames();
        List<Map<String, Object>> allRows = new ArrayList<>();
        List<String> finalConflictKeys = conflictKeys;
        if (toUnderlineCase) {
            finalConflictKeys = new ArrayList<>();
            for (String key : conflictKeys) {
                finalConflictKeys.add(StrUtil.toUnderlineCase(key));
            }
        }
        for (T entity : entities) {
            Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, toUnderlineCase, ignoreNullValue, ignoreFieldNames);
            allRows.add(rowData);
        }
        bUpsertBatch(tableName, allRows, finalConflictKeys);
    }

    @Override
    public <T> void bUpsertBatch(Collection<T> entities, Consumer<UpdateStrategy> strategyConsumer) {
        UpdateStrategy strategy = new UpdateStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        bUpsertBatch(entities, strategy);
    }

// ==================== 批量选择性 UPSERT ====================

    @Override
    public <T> void bUpsertBatchSelective(Collection<T> entities, List<String> conflictKeys) {
        if (CollUtil.isEmpty(entities)) {
            return;
        }
        if (CollUtil.isEmpty(conflictKeys)) {
            T first = entities.iterator().next();
            conflictKeys = GirAdvSqlUtils.getIdByAnnotation(first.getClass());
            if (CollUtil.isEmpty(conflictKeys)) {
                throw new IllegalArgumentException("冲突判定字段不能为空");
            }
        }

        UpdateStrategy strategy = new UpdateStrategy()
                .setConflictKeys(conflictKeys)
                .setToUnderlineCase(true)
                .setIgnoreNullValue(true);  // 选择性：忽略null值
        bUpsertBatch(entities, strategy);
    }

    @Override
    public <T> void bUpsertBatchSelective(Collection<T> entities, UpdateStrategy strategy) {
        if (strategy == null) {
            bUpsertBatchSelective(entities, (List<String>) null);
            return;
        }
        strategy.setIgnoreNullValue(true);  // 强制选择性
        bUpsertBatch(entities, strategy);
    }

    @Override
    public <T> void bUpsertBatchSelective(Collection<T> entities, Consumer<UpdateStrategy> strategyConsumer) {
        UpdateStrategy strategy = new UpdateStrategy();
        if (strategyConsumer != null) {
            strategyConsumer.accept(strategy);
        }
        strategy.setIgnoreNullValue(true);
        bUpsertBatch(entities, strategy);
    }

    // ==================== 工具方法 ====================

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
            throw new RuntimeException("更新操作失败，SQL：" + sql, e);
        } finally {
            closeConnection(connection);
        }
    }

    protected void validateTableName(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }
    }

    protected void validateIdKey(String idKey) {
        if (StrUtil.isEmpty(idKey)) {
            throw new IllegalArgumentException("主键字段名不能为空");
        }
    }

    protected void validateUpdateData(Map<String, Object> rowData) {
        if (CollUtil.isEmpty(rowData)) {
            throw new IllegalArgumentException("更新的数据不能为空");
        }
    }

    protected void closeConnection(Connection connection) {
        if (dataSourceGetter != null) {
            dataSourceGetter.connectionClose(connection);
        }
    }

    protected String buildUpsertUpdateClause(Map<String, Object> rowData, List<String> conflictKeys) {
        return rowData.keySet()
                .stream()
                .map(field -> dialectTableNameProcessor.tbQuoteFieldName(field))
                .filter(field -> !conflictKeys.contains(field))
                .map(this::buildUpsertFieldClause)
                .collect(Collectors.joining(","));
    }

    protected abstract String buildUpsertFieldClause(String field);

    protected abstract String buildUpdateOrInsertSql(
            String tableName, String fields, String placeholders,
            String conflictFields, String updateClause);
}
