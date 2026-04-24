package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.utils.AdvLogSql;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.handler.NumberHandler;
import cn.hutool.db.sql.SqlExecutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/** 数据库删除操作抽象父类 封装所有数据库通用的删除逻辑，差异化语法由子类实现 */
public abstract class AbstractExecAdvBaseDeleteOpt implements IAdvBaseDeleteOpt {

    // 注入数据源获取器
    protected IDataSourceGetter dataSourceGetter;

    // 表名处理器（差异化）
    protected DialectTableNameProcessor dialectTableNameProcessor;

    // 日志实例
    protected static final GiLogger log = GirLogger.getLoger(AbstractExecAdvBaseDeleteOpt.class);

    // 默认分批删除批次大小（通用常量）
    protected static final int DEFAULT_BATCH_SIZE = 1000;

    @Override
    public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
    }

    // ========== 通用逻辑：自定义SQL删除 ==========
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
            AdvLogSql.of(dataSourceGetter).logExecuteSql(this.getClass(),"bDeleteBySql", execSql, jdbcParams, cost,result);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("执行自定义删除SQL失败，SQL：" + execSql, e);
        } finally {
            closeConnection(connection);
        }
    }

    // ========== 通用逻辑：主键删除 ==========
    @Override
    public Integer bDeleteByPrimaryKey(String tableName, String idKey, Object id) {
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
            AdvLogSql.of(dataSourceGetter).logExecuteSql(this.getClass(),"bDeleteByPrimaryKey", execSql, Collections.singletonList(id), cost,result);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("按主键删除失败，表名：" + tableName + "，主键：" + idKey + "=" + id, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Integer bDeleteBatchByPrimaryKey(String tableName, String idKey, Set<Object> ids) {
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
            log.debug("schema:[{}]db:[{}] 耗时:[{}ms] bDeleteBatchByPrimaryKey 批量主键删除完成，表名：{}，总删除行数：{}",
                    getSchemaName(), getDatabaseName(), cost, tableName, totalSuccess);
            return totalSuccess;
        } catch (SQLException e) {
            rollbackConnection(connection);
            throw new RuntimeException("批量主键删除失败，表名：" + tableName, e);
        } finally {
            restoreAutoCommit(connection);
            closeConnection(connection);
        }
    }

    @Override
    public Integer bDeleteBatchWithBatchSize(String tableName, String idKey, Set<Object> ids, int batchSize) {
        validateTableName(tableName);
        validateIdKey(idKey);
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        if (batchSize <= 0 || batchSize > getMaxInParams()) {
            batchSize = DEFAULT_BATCH_SIZE;
        }

        List<List<Object>> idBatches = splitCollection(ids, batchSize);
        int totalSuccess = 0;

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (List<Object> idBatch : idBatches) {
            totalSuccess += bDeleteBatchByPrimaryKey(tableName, idKey, new HashSet<>(idBatch));
        }
        stopWatch.stop();
        long cost = stopWatch.getLastTaskTimeMillis();

        log.debug("schema:[{}]db:[{}] time:[{}ms] bDeleteBatchWithBatchSize 分批次主键删除完成，表名：{}，总删除行数：{}，批次大小：{}",
                getSchemaName(), getDatabaseName(), cost, tableName, totalSuccess, batchSize);
        return totalSuccess;
    }

    // ========== 通用逻辑：条件删除 ==========
    @Override
    public Integer bDeleteByCondition(String tableName, Map<String, Object> whereMap) {
        validateTableName(tableName);
        if (CollUtil.isEmpty(whereMap)) {
            throw new IllegalArgumentException("删除条件不能为空（禁止全表删除）");
        }
        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        String whereClause = buildWhereClause(whereMap);
        String execSql = buildDeleteByConditionSql(quoteTableName, whereClause);
        List<Object> params = new ArrayList<>(whereMap.values());

        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, execSql, params.toArray());
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter).logExecuteSql(this.getClass(),"bDeleteByCondition", execSql, params, cost,result);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("条件删除失败，表名：" + tableName, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Integer bDeleteBatchByCondition(String tableName, Map<String, Object> whereMap, int batchSize) {
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
                String whereClause = buildWhereClause(whereMap);
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
            log.debug("schema:[{}]db:[{}] 耗时:[{}ms] bDeleteBatchByCondition 分批次条件删除完成，表名：{}，总删除行数：{}，批次大小：{}",
                    getSchemaName(), getDatabaseName(), cost, tableName, totalSuccess, batchSize);
            return totalSuccess;
        } catch (SQLException e) {
            rollbackConnection(connection);
            throw new RuntimeException("分批次条件删除失败，表名：" + tableName, e);
        } finally {
            restoreAutoCommit(connection);
            closeConnection(connection);
        }
    }

    // ========== 通用逻辑：特殊场景删除（逻辑删除/安全删除） ==========
    @Override
    public Integer bLogicDelete(String tableName, String idKey, Object id, String deleteKey, Object deleteValue) {
        validateTableName(tableName);
        validateIdKeyAndValue(idKey, id);
        validateIdKeyAndValue(deleteKey, deleteValue);
        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        String execSql = buildLogicDeleteSql(quoteTableName, deleteKey, idKey);

        List<Object> params = Arrays.asList(deleteValue, id);
        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, execSql, params.toArray());
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter).logExecuteSql(this.getClass(),"bLogicDelete", execSql, params, cost,result);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("逻辑删除失败，表名：" + tableName + "，主键：" + idKey + "=" + id, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public Integer bLogicDeleteBatch(String tableName, String idKey, Set<Object> ids, String deleteKey, Object deleteValue) {
        validateTableName(tableName);
        validateIdKey(idKey);
        validateIdKeyAndValue(deleteKey, deleteValue);
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
                String execSql = buildLogicDeleteBatchSql(quoteTableName, deleteKey, idKey, placeholders);
                List<Object> params = new ArrayList<>();
                params.add(deleteValue);
                params.addAll(idBatch);
                int batchSuccess = SqlExecutor.execute(connection, execSql, params.toArray());
                totalSuccess += batchSuccess;
            }

            connection.commit();
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            log.debug("schema:[{}]db:[{}] 耗时:[{}ms] bLogicDeleteBatch 批量逻辑删除完成，表名：{}，删除行数：{}",
                    getSchemaName(), getDatabaseName(), cost, tableName, totalSuccess);
            return totalSuccess;
        } catch (SQLException e) {
            rollbackConnection(connection);
            throw new RuntimeException("批量逻辑删除失败，表名：" + tableName, e);
        } finally {
            restoreAutoCommit(connection);
            closeConnection(connection);
        }
    }

    @Override
    public Integer bSafeDeleteByCondition(String tableName, Map<String, Object> whereMap, int maxDelete) {
        validateTableName(tableName);
        if (CollUtil.isEmpty(whereMap)) {
            throw new IllegalArgumentException("删除条件不能为空（禁止全表删除）");
        }
        if (maxDelete <= 0) {
            throw new IllegalArgumentException("最大允许删除行数阈值必须大于0");
        }
        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);

        String countWhereClause = buildWhereClause(whereMap);
        String countSql = buildCountByConditionSql(quoteTableName, countWhereClause);
        List<Object> countParams = new ArrayList<>(whereMap.values());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Connection connection = dataSourceGetter.getConnection();
        try {
            Number count = SqlExecutor.query(connection, countSql, new NumberHandler(), countParams.toArray());
            int totalCount = count.intValue();
            if (totalCount > maxDelete) {
                log.warn("schema:[{}]db:[{}] 安全删除触发阈值限制，表名：{}，符合条件行数：{}，阈值：{}，操作终止",
                        getSchemaName(), getDatabaseName(), tableName, totalCount, maxDelete);
                return -1;
            }

            Integer result = bDeleteByCondition(tableName, whereMap);
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            log.debug("schema:[{}]db:[{}] 耗时:[{}ms] bSafeDeleteByCondition 安全删除完成，表名：{}，删除行数：{}",
                    getSchemaName(), getDatabaseName(), cost, tableName, result);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("安全删除校验失败，表名：" + tableName, e);
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

    protected String buildWhereClause(Map<String, Object> whereMap) {
        return whereMap.keySet().stream()
                .map(field -> StrUtil.format("{} = ?", field))
                .collect(Collectors.joining(" AND "));
    }

    protected String getSchemaName() {
        return dataSourceGetter != null ? dataSourceGetter.getSchemaName() : "";
    }

    protected String getDatabaseName() {
        return dataSourceGetter != null
                ? GutilObject.isEmpty(dataSourceGetter.getDatabaseName()) ? "" : dataSourceGetter.getDatabaseName()
                : "";
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
        return StrUtil.format("DELETE FROM {} WHERE {} = ?", tableName, idKey);
    }

    protected String buildDeleteBatchByPrimaryKeySql(String tableName, String idKey, String placeholders) {
        return StrUtil.format("DELETE FROM {} WHERE {} IN ({})", tableName, idKey, placeholders);
    }

    protected String buildDeleteByConditionSql(String tableName, String whereClause) {
        return StrUtil.format("DELETE FROM {} WHERE {}", tableName, whereClause);
    }

    protected String buildDeleteBatchByConditionSql(String tableName, String whereClause, int batchSize) {
        return StrUtil.format("DELETE FROM {} WHERE {} LIMIT {}", tableName, whereClause, batchSize);
    }

    protected String buildLogicDeleteSql(String tableName, String deleteKey, String idKey) {
        return StrUtil.format("UPDATE {} SET {} = ? WHERE {} = ?", tableName, deleteKey, idKey);
    }

    protected String buildLogicDeleteBatchSql(String tableName, String deleteKey, String idKey, String placeholders) {
        return StrUtil.format("UPDATE {} SET {} = ? WHERE {} IN ({})", tableName, deleteKey, idKey, placeholders);
    }

    protected String buildCountByConditionSql(String tableName, String whereClause) {
        return StrUtil.format("SELECT COUNT(*) FROM {} WHERE {}", tableName, whereClause);
    }
}
