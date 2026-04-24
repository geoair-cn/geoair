package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.geoair.map.dynamic.adv.query.utils.AdvLogSql;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.sql.SqlExecutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据库更新操作抽象父类 封装所有数据库通用的更新逻辑，差异化语法由子类实现
 */
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
    public Integer bUpdateBySql(String dynamicSql, SqlParamMap sqlParam) {
        if (StrUtil.isEmpty(dynamicSql)) {
            throw new IllegalArgumentException("更新SQL语句不能为空");
        }
        SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParam, dialectTableNameProcessor);
        String execSql = sqlMeta.getSql();
        List<Object> jdbcParams = sqlMeta.getJdbcParamValues();

        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result;
            if (CollUtil.isEmpty(jdbcParams)) {
                result = SqlExecutor.execute(connection, execSql);
            } else {
                result = SqlExecutor.execute(connection, execSql, jdbcParams.toArray());
            }
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter). logExecuteSql ("bUpdateBySql", execSql, jdbcParams, cost);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("执行自定义更新SQL失败，SQL：" + execSql, e);
        } finally {
            closeConnection(connection);
        }
    }

    // ========== 通用逻辑：单条数据更新（按主键） ==========
    @Override
    public Integer bUpdateByPrimaryKey(
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
            AdvLogSql.of(dataSourceGetter). logExecuteSql ("bUpdateByPrimaryKey", execSql, params, cost);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("按主键更新失败，表名：" + tableName + "，主键：" + idKey + "=" + id, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public <T> Integer bUpdateByPrimaryKey(String tableName, String idKey, T entity) {
        validateTableName(tableName);
        validateIdKey(idKey);
        if (entity == null) {
            throw new IllegalArgumentException("更新的实体对象不能为空");
        }

        Entity entityObj = Entity.parse(entity);
        Object id = entityObj.remove(idKey);
        if (id == null) {
            throw new IllegalArgumentException("实体对象中未找到主键字段[" + idKey + "]的值");
        }

        return bUpdateByPrimaryKey(tableName, idKey, id, entityObj);
    }

    // ========== 通用逻辑：条件更新 ==========
    @Override
    public Integer bUpdateByCondition(
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
            AdvLogSql.of(dataSourceGetter). logExecuteSql ("bUpdateByCondition", execSql, params, cost);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("条件更新失败，表名：" + tableName, e);
        } finally {
            closeConnection(connection);
        }
    }

    // ========== 通用逻辑：批量更新（按主键） ==========
    @Override
    public Integer bUpdateBatchByPrimaryKey(
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
                    batchSuccess += bUpdateByPrimaryKey(tableName, idKey, id, updateData);
                }
                totalSuccess += batchSuccess;
            }

            connection.commit();
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            String format = StrUtil.format("表名：{}，总条数：{}，批次大小：{}", tableName, totalSuccess, batchSize);
            AdvLogSql.of(dataSourceGetter). logExecuteSql("bUpdateBatchWithBatchSize",format,cost);
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
    public <T> Integer bUpdateBatchByPrimaryKey(
            String tableName, String idKey, Collection<T> entities) {
        validateTableName(tableName);
        validateIdKey(idKey);
        if (CollUtil.isEmpty(entities)) {
            return 0;
        }

        List<Map<String, Object>> rowsData =
                entities.stream().map(Entity::parse).collect(Collectors.toList());

        return bUpdateBatchByPrimaryKey(tableName, idKey, rowsData);
    }

    // ========== 通用逻辑：乐观锁更新 ==========
    @Override
    public Integer bUpdateWithOptimisticLock(
            String tableName,
            String idKey,
            Object id,
            Map<String, Object> rowData,
            String versionKey,
            Integer version) {
        validateTableName(tableName);
        validateIdKeyAndValue(idKey, id);
        validateUpdateData(rowData);
        validateIdKeyAndValue(versionKey, version);
        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(
                        dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        String setClause = buildOptimisticLockSetClause(rowData, versionKey);
        String execSql = buildUpdateWithOptimisticLockSql(quoteTableName, setClause, idKey, versionKey);

        List<Object> params = new ArrayList<>(rowData.values());
        params.add(id);
        params.add(version);

        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, execSql, params.toArray());
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter). logExecuteSql("bUpdateWithOptimisticLock", execSql, params, cost);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("乐观锁更新失败，表名：" + tableName + "，主键：" + idKey + "=" + id, e);
        } finally {
            closeConnection(connection);
        }
    }

    // ========== 通用逻辑：更新或插入（UPSERT） ==========
    @Override
    public Integer bUpdateOrInsert(
            String tableName, Map<String, Object> rowData, Set<String> conflictKeys) {
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
            AdvLogSql.of(dataSourceGetter). logExecuteSql("bUpdateOrInsert", execSql, params, cost);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("更新或插入失败，表名：" + tableName, e);
        } finally {
            closeConnection(connection);
        }
    }



    // ====================== 原有工具方法不动 ======================
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

    protected String buildUpsertUpdateClause(Map<String, Object> rowData, Set<String> conflictKeys) {
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

    protected String buildOptimisticLockSetClause(Map<String, Object> rowData, String versionKey) {
        return rowData.keySet()
                .stream()
                .map(field -> {
                    if (field.equals(versionKey)) {
                        return StrUtil.format("{} = {} + 1", versionKey, versionKey);
                    }
                    return StrUtil.format("{} = ?", field);
                })
                .collect(Collectors.joining(","));
    }

    protected String buildUpdateWithOptimisticLockSql(
            String tableName, String setClause, String idKey, String versionKey) {
        return StrUtil.format(
                "UPDATE {} SET {} WHERE {} = ? AND {} = ?",
                tableName,
                setClause,
                idKey,
                versionKey);
    }

    protected abstract String buildUpsertFieldClause(String field);

    protected abstract String buildUpdateOrInsertSql(
            String tableName,
            String fields,
            String placeholders,
            String conflictFields,
            String updateClause);
}
