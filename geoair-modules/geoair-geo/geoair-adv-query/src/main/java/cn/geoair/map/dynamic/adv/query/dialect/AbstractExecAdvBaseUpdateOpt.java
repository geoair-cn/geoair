package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.base.util.GutilArray;
import cn.geoair.base.util.GutilAssert;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.geoair.map.dynamic.adv.query.utils.AdvLogSql;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereLambdaFilter;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.sql.SqlExecutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
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
        if (sqlParam == null) {
            return bUpdateBySql(sqlStatement);
        } else if (sqlParam instanceof SqlParamList) {
            SqlParamList sqlParamList = (SqlParamList) sqlParam;
            return bUpdateBySql(sqlStatement, sqlParamList);
        } else if (sqlParam instanceof SqlParamMap) {
            SqlParamMap sqlParamMap = (SqlParamMap) sqlParam;
            return bUpdateBySql(sqlStatement, sqlParamMap);
        } else {
            throw new RuntimeException("SqlParam参数不合法！");
        }
    }

    // ========== 通用逻辑：单条数据更新（按主键） ==========
    @Override
    public Integer bUpdateByPK(
            String tableName, String idKey, Object id, Map<String, Object> rowData) {
        validateTableName(tableName);
        validateIdKeyAndValue(idKey, id);
        validateUpdateData(rowData);
        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(
                        dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        String setClause = buildSetClause(rowData);
        String execSql = buildUpdateByPrimaryKeySql(quoteTableName, setClause, idKey);

        List<Object> params = new ArrayList<>(rowData.values());
        params.add(id);

        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, execSql, params.toArray());
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), "bUpdateByPrimaryKey", execSql, params, cost, result);
            return result;
        } catch (SQLException e) {
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(this.getClass(), "bUpdateByPrimaryKey", execSql, params, e);
            throw new RuntimeException("按主键更新失败，表名：" + tableName + "，主键：" + idKey + "=" + id, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public <T> Integer bUpdateByPK(String tableName, String idKey, T entity) {
        return bUpdateByPK(tableName, idKey, entity, true);
    }

    @Override
    public <T> Integer bUpdateByPK(String tableName, String idKey, T entity, boolean isToUnderlineCase, boolean ignoreNullValue) {
        return bUpdateByPK(tableName, idKey, entity, isToUnderlineCase, ignoreNullValue, ListUtil.empty());
    }

    @Override
    public <T> Integer bUpdateByPK(String tableName, String idKey, T entity, boolean isToUnderlineCase) {
        return bUpdateByPK(tableName, idKey, entity, isToUnderlineCase, false);
    }

    @Override
    public <T> Integer bUpdateByPK(String tableName, String idKey, T entity, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames) {
        validateTableName(tableName);
        validateIdKey(idKey);
        if (entity == null) {
            throw new IllegalArgumentException("更新的实体对象不能为空");
        }
        Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, isToUnderlineCase, ignoreNullValue, ignoreFieldNames);
        if (GutilObject.isEmpty(tableName)) {
            tableName = GirAdvSqlUtils.getTableName(entity.getClass());
        }
        if (GutilObject.isEmpty(tableName)) {
            throw new IllegalArgumentException("tableName 不能为空");
        }
        if (isToUnderlineCase) {
            idKey = StrUtil.toUnderlineCase(idKey);
        }
        Object id = rowData.remove(idKey);
        if (id == null) {
            throw new IllegalArgumentException("实体对象中未找到主键字段[" + idKey + "]的值");
        }

        return bUpdateByPK(tableName, idKey, id, rowData);
    }

    @Override
    public <T> Integer bUpdateByPKSelective(String tableName, String idKey, T entity, boolean isToUnderlineCase) {
        return bUpdateByPK(tableName, idKey, entity, isToUnderlineCase, true, ListUtil.empty());
    }

    @Override
    public <T> Integer bUpdateByPKSelective(String tableName, String idKey, T entity) {
        return bUpdateByPKSelective(tableName, idKey, entity, true);
    }

    @Override
    public <T> Integer bUpdateByPKSelective(String tableName, String idKey, T entity, List<String> ignoreFieldNames) {
        return bUpdateByPK(tableName, idKey, entity, true, true, ignoreFieldNames);
    }

    // ========== 通用逻辑：条件更新 ==========
    @Override
    public Integer bUpdateByMap(
            String tableName, Map<String, Object> rowData, Map<String, Object> whereMap) {
        validateTableName(tableName);
        validateUpdateData(rowData);
        if (CollUtil.isEmpty(whereMap)) {
            throw new IllegalArgumentException("更新条件不能为空（避免全表更新）");
        }
        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(
                        dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        String setClause = buildSetClause(rowData);
        String whereClause = buildWhereClause(whereMap);
        String execSql = buildUpdateByConditionSql(quoteTableName, setClause, whereClause);

        List<Object> params = new ArrayList<>(rowData.values());
        params.addAll(whereMap.values());

        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, execSql, params.toArray());
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), "bUpdateByMap", execSql, params, cost, result);
            return result;
        } catch (SQLException e) {
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(this.getClass(), "bUpdateByMap", execSql, params, e);
            throw new RuntimeException("条件更新失败，表名：" + tableName, e);
        } finally {
            closeConnection(connection);
        }
    }

    // ========== 通用逻辑：批量更新（按主键） ==========
    @Override
    public Integer bUpdateBatchByPK(
            String tableName, String idKey, List<Map<String, Object>> rowsData) {
        return bUpdateBatchWithBatchSize(tableName, idKey, rowsData, DEFAULT_BATCH_SIZE);
    }

    @Override
    public Integer bUpdateBatchWithBatchSize(
            String tableName, String idKey, List<Map<String, Object>> rowsData, int batchSize) {
        validateTableName(tableName);
        validateIdKey(idKey);
        if (CollUtil.isEmpty(rowsData)) {
            return 0;
        }
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }

        List<List<Map<String, Object>>> batches = CollUtil.split(rowsData, batchSize);
        int totalSuccess = 0;

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Connection connection = dataSourceGetter.getConnection();
        try {
            connection.setAutoCommit(false);

            for (List<Map<String, Object>> batch : batches) {
                int batchSuccess = 0;
                for (Map<String, Object> row : batch) {
                    Object id = row.get(idKey);
                    if (id == null) {
                        throw new IllegalArgumentException("批量更新数据中缺少主键字段[" + idKey + "]的值");
                    }

                    Map<String, Object> updateData = new HashMap<>(row);
                    updateData.remove(idKey);
                    batchSuccess += bUpdateByPK(tableName, idKey, id, updateData);
                }
                totalSuccess += batchSuccess;
            }

            connection.commit();
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            String format = StrUtil.format("表名：{}，总条数：{}，批次大小：{}", tableName, totalSuccess, batchSize);
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), "bUpdateBatchWithBatchSize", format, cost, totalSuccess);
            return totalSuccess;
        } catch (SQLException e) {
            rollbackConnection(connection);
            throw new RuntimeException("批量更新失败，表名：" + tableName, e);
        } finally {
            restoreAutoCommit(connection);
            closeConnection(connection);
        }
    }

    @Override
    public <T> Integer bUpdateBatchByPK(
            String tableName, String idKey, Collection<T> entities) {
        validateTableName(tableName);
        validateIdKey(idKey);
        if (CollUtil.isEmpty(entities)) {
            return 0;
        }
        List<Map<String, Object>> rowsDatas = new ArrayList<>(entities.size());
        for (T entity : entities) {
            Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, true, false, ListUtil.empty());
            rowsDatas.add(rowData);
        }
        return bUpdateBatchByPK(tableName, idKey, rowsDatas);
    }


    // ========== 通用逻辑：更新或插入（UPSERT） ==========
    @Override
    public Integer bUpdateOrInsert(
            String tableName, Map<String, Object> rowData, List<String> conflictKeys) {
        return bUpsert(tableName, rowData, conflictKeys);
    }

    @Override
    public <T> Integer bUpsert(T entity) {
        return bUpsert(null, entity);
    }

    @Override
    public <T> Integer bUpsert(String tableName, T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("UPSERT的实体对象不能为空");
        }
        List<String> idByAnnotation = GirAdvSqlUtils.getIdByAnnotation(entity.getClass());
        if (GutilObject.isEmpty(idByAnnotation)) {
            throw new IllegalArgumentException("实体对象中未找到主键字段，无法执行UPSERT");
        }
        return bUpsert(tableName, entity, idByAnnotation);
    }

    @Override
    public Integer bUpsert(String tableName, Map<String, Object> rowData, List<String> conflictKeys) {
        validateTableName(tableName);
        validateUpdateData(rowData);
        if (CollUtil.isEmpty(conflictKeys)) {
            throw new IllegalArgumentException("冲突判定字段不能为空");
        }
        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(
                        dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        String fields = String.join(",", rowData.keySet());
        String placeholders = rowData.keySet().stream().map(key -> "?").collect(Collectors.joining(","));
        String conflictFields = String.join(",", conflictKeys);
        String updateClause = buildUpsertUpdateClause(rowData, conflictKeys);
        String execSql = buildUpdateOrInsertSql(quoteTableName, fields, placeholders, conflictFields, updateClause);

        List<Object> params = new ArrayList<>(rowData.values());
        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, execSql, params.toArray());
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), "bUpsert", execSql, params, cost, result);
            return result;
        } catch (SQLException e) {
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(this.getClass(), "bUpsert", execSql, params, e);
            throw new RuntimeException("更新或插入失败，表名：" + tableName, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys) {
        return bUpsert(tableName, entity, conflictKeys, true);
    }

    @Override
    public <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase, boolean ignoreNullValue) {
        List<String> ignoreFieldByAnnotation = GirAdvSqlUtils.getIgnoreFieldByAnnotation(entity.getClass());
        return bUpsert(tableName, entity, conflictKeys, isToUnderlineCase, ignoreNullValue, ignoreFieldByAnnotation);
    }

    @Override
    public <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase) {
        List<String> ignoreFieldByAnnotation = GirAdvSqlUtils.getIgnoreFieldByAnnotation(entity.getClass());
        return bUpsert(tableName, entity, conflictKeys, isToUnderlineCase, false, ignoreFieldByAnnotation);
    }

    @Override
    public <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames) {
        if (entity == null) {
            throw new IllegalArgumentException("UPSERT的实体对象不能为空");
        }
        Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, isToUnderlineCase, ignoreNullValue, ignoreFieldNames);
        if (GutilObject.isEmpty(tableName)) {
            tableName = GirAdvSqlUtils.getTableName(entity.getClass());
        }
        if (GutilObject.isEmpty(tableName)) {
            throw new IllegalArgumentException("tableName 不能为空");
        }
        List<String> conflictKeysCopy = new ArrayList<>();
        if (isToUnderlineCase && GutilObject.isNotEmpty(conflictKeys)) {
            for (String conflictKey : conflictKeys) {
                conflictKeysCopy.add(StrUtil.toUnderlineCase(conflictKey));
            }
            conflictKeys = conflictKeysCopy;
        }
        return bUpsert(tableName, rowData, conflictKeys);
    }

    @Override
    public <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys, List<String> ignoreFieldNames) {
        return bUpsert(tableName, entity, conflictKeys, true, false, ignoreFieldNames);
    }

    @Override
    public <T> Integer bUpsertSelective(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase) {
        List<String> ignoreFieldByAnnotation = GirAdvSqlUtils.getIgnoreFieldByAnnotation(entity.getClass());
        return bUpsert(tableName, entity, conflictKeys, isToUnderlineCase, true, ignoreFieldByAnnotation);
    }

    @Override
    public <T> Integer bUpsertSelective(String tableName, T entity, List<String> conflictKeys) {
        List<String> ignoreFieldByAnnotation = GirAdvSqlUtils.getIgnoreFieldByAnnotation(entity.getClass());
        return bUpsert(tableName, entity, conflictKeys, true, true, ignoreFieldByAnnotation);
    }

    @Override
    public <T> Integer bUpsertSelective(String tableName, T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("UPSERT的实体对象不能为空");
        }
        List<String> idByAnnotation = GirAdvSqlUtils.getIdByAnnotation(entity.getClass());
        if (GutilObject.isEmpty(idByAnnotation)) {
            throw new IllegalArgumentException("实体对象中未找到主键字段，无法执行UPSERT");
        }
        List<String> ignoreFieldByAnnotation = GirAdvSqlUtils.getIgnoreFieldByAnnotation(entity.getClass());
        // 修复：调用正确的方法，传入 conflictKeys 和 ignoreFieldNames
        return bUpsertSelective(tableName, entity, idByAnnotation, ignoreFieldByAnnotation);
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
        List<String> ignoreFieldByAnnotation = GirAdvSqlUtils.getIgnoreFieldByAnnotation(entity.getClass());
        // 修复：获取表名，然后调用正确的方法
        String tableName = GirAdvSqlUtils.getTableName(entity.getClass());
        if (GutilObject.isEmpty(tableName)) {
            throw new IllegalArgumentException("tableName 不能为空");
        }
        return bUpsertSelective(tableName, entity, idByAnnotation, ignoreFieldByAnnotation);
    }

    @Override
    public <T> Integer bUpsertSelective(String tableName, T entity, List<String> conflictKeys, List<String> ignoreFieldNames) {
        return bUpsert(tableName, entity, conflictKeys, true, true, ignoreFieldNames);
    }

    // ========== 条件更新（Lambda表达式版本） ==========
    @Override
    public <T> Integer bUpdateByWhere(String tableName, T entity, GirAdvWhereLambdaFilter<T> whereFilter, List<String> ignoreFieldNames) {
        if (entity == null) {
            throw new IllegalArgumentException("更新的实体对象不能为空");  // 修复：错误提示
        }
        if (whereFilter == null) {
            throw new IllegalArgumentException("更新条件不能为空（避免全表更新）");
        }
        if (GutilObject.isEmpty(tableName)) {
            tableName = GirAdvSqlUtils.getTableName(whereFilter.getEntityClass());
        }
        boolean toUnderlineCase = whereFilter.isToUnderlineCase();
        Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, toUnderlineCase, false, ignoreFieldNames);
        return bUpdateByWhere(tableName, rowData, whereFilter.toWhereFilter());
    }

    @Override
    public <T> Integer bUpdateByWhere(String tableName, T entity, GirAdvWhereLambdaFilter<T> whereFilter) {
        List<String> ignoreFieldByAnnotation = GirAdvSqlUtils.getIgnoreFieldByAnnotation(entity.getClass());
        return bUpdateByWhere(tableName, entity, whereFilter, ignoreFieldByAnnotation);
    }

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
        String quoteTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(
                        dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        String setClause = buildSetClause(rowData);
        String execSql = buildUpdateByConditionSql(quoteTableName, setClause, whereClause);
        List<Object> params = new ArrayList<>(rowData.values());
        params.addAll(whereParams);
        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, execSql, params.toArray());
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), "bUpdateByWhere", execSql, params, cost, result);
            return result;
        } catch (SQLException e) {
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(this.getClass(), "bUpdateByWhere", execSql, params, e);
            throw new RuntimeException("条件更新失败，表名：" + tableName, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public <T> Integer bUpdateSelectiveByWhere(String tableName, T entity, GirAdvWhereLambdaFilter<T> whereFilter, List<String> ignoreFieldNames) {
        if (entity == null) {
            throw new IllegalArgumentException("更新的实体对象不能为空");  // 修复：错误提示
        }
        if (whereFilter == null) {
            throw new IllegalArgumentException("更新条件不能为空（避免全表更新）");
        }
        boolean toUnderlineCase = whereFilter.isToUnderlineCase();
        Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, toUnderlineCase, true, ignoreFieldNames);
        return bUpdateSelectiveByWhere(tableName, rowData, whereFilter.toWhereFilter());
    }

    @Override
    public <T> Integer bUpdateSelectiveByWhere(String tableName, T entity, GirAdvWhereLambdaFilter<T> whereFilter) {
        List<String> ignoreFieldByAnnotation = GirAdvSqlUtils.getIgnoreFieldByAnnotation(entity.getClass());
        return bUpdateSelectiveByWhere(tableName, entity, whereFilter, ignoreFieldByAnnotation);
    }

    @Override
    public <T> Integer bUpdateSelectiveByWhere(T entity, GirAdvWhereLambdaFilter<T> whereFilter, List<String> ignoreFieldNames) {
        String tableName = GirAdvSqlUtils.getTableName(entity.getClass());
        if (GutilObject.isEmpty(tableName)) {
            throw new IllegalArgumentException("tableName 不能为空");
        }
        Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, true, true, ignoreFieldNames);
        return bUpdateSelectiveByWhere(tableName, rowData, whereFilter.toWhereFilter());
    }

    @Override
    public <T> Integer bUpdateSelectiveByWhere(T entity, GirAdvWhereLambdaFilter<T> whereFilter) {
        List<String> ignoreFieldByAnnotation = GirAdvSqlUtils.getIgnoreFieldByAnnotation(entity.getClass());
        return bUpdateSelectiveByWhere(entity, whereFilter, ignoreFieldByAnnotation);
    }

    @Override
    public <T> Integer bUpdateSelectiveByWhere(String tableName, Map<String, Object> rowData, GirAdvWhereFilter whereFilter) {
        validateUpdateData(rowData);
        // 过滤掉 value 为 null 的数据（保留空字符串）
        Map<String, Object> copyRowData;
        copyRowData = rowData.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return bUpdateByWhere(tableName, copyRowData, whereFilter);
    }

    // ====================== 原有工具方法 ======================
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

    protected void validateIdKeyAndValue(String idKey, Object id) {
        validateIdKey(idKey);
        if (id == null) {
            throw new IllegalArgumentException("主键值不能为空");
        }
    }

    protected void validateUpdateData(Map<String, Object> rowData) {
        if (CollUtil.isEmpty(rowData)) {
            throw new IllegalArgumentException("更新的数据不能为空");
        }
    }

    protected String buildSetClause(Map<String, Object> rowData) {
        return rowData.keySet()
                .stream()
                .map(field -> StrUtil.format("{} = ?", field))
                .collect(Collectors.joining(","));
    }

    protected String buildWhereClause(Map<String, Object> whereMap) {
        return whereMap.keySet()
                .stream()
                .map(field -> StrUtil.format("{} = ?", field))
                .collect(Collectors.joining(" AND "));
    }

    protected String buildUpsertUpdateClause(Map<String, Object> rowData, List<String> conflictKeys) {
        return rowData.keySet()
                .stream()
                .filter(field -> !conflictKeys.contains(field))
                .map(this::buildUpsertFieldClause)
                .collect(Collectors.joining(","));
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
                log.error("更新操作回滚失败", e);
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

    protected String buildUpdateByPrimaryKeySql(String tableName, String setClause, String idKey) {
        return StrUtil.format("UPDATE {} SET {} WHERE {} = ?", tableName, setClause, idKey);
    }

    protected String buildUpdateByConditionSql(String tableName, String setClause, String whereClause) {
        return StrUtil.format("UPDATE {} SET {} WHERE {}", tableName, setClause, whereClause);
    }

    protected abstract String buildUpsertFieldClause(String field);

    protected abstract String buildUpdateOrInsertSql(
            String tableName,
            String fields,
            String placeholders,
            String conflictFields,
            String updateClause);
}
