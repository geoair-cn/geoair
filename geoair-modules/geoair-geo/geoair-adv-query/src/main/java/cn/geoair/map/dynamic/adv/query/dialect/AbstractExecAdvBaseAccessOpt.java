package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.geoair.map.dynamic.adv.query.utils.AdvLogSql;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.sql.SqlExecutor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据库插入操作抽象父类 封装所有数据库通用的插入逻辑，差异化语法由子类实现
 */
public abstract class AbstractExecAdvBaseAccessOpt implements IAdvBaseAccessOpt {

    protected IDataSourceGetter dataSourceGetter;

    // 表名处理器（差异化）
    protected DialectTableNameProcessor dialectTableNameProcessor;

    protected static final GiLogger log = GirLogger.getLoger(AbstractExecAdvBaseAccessOpt.class);

    // 默认分批插入批次大小（通用常量）
    protected static final int DEFAULT_BATCH_SIZE = 1000;

    protected abstract String buildInsertIgnoreSql(
            String tableName, String fields, String placeholders, Set<String> conflictKeys);


    @Override
    public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
    }

    // ========== 通用逻辑：自定义SQL插入 ==========
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

    // ========== 通用逻辑：单条数据插入 ==========
    @Override
    public Integer bInsertOne(String tableName, Map<String, Object> rowData) {
        validateTableNameAndData(tableName, rowData);

        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        String fields = String.join(",", rowData.keySet());
        String placeholders = buildPlaceholders(rowData.keySet().size());
        String execSql = buildInsertSql(quoteTableName, fields, placeholders);

        List<Object> params = new ArrayList<>(rowData.values());
        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, execSql, params.toArray());
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter).logExecuteSql(this.getClass(), "bInsertOne", execSql, params, cost, 1);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("单条插入失败，表名：" + quoteTableName, e);
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    public <T> Integer bInsertOne(String tableName, T entity) {
        return bInsertOne(tableName, entity, true, false);
    }

    @Override
    public <T> Integer bInsertOne(String tableName, T entity, boolean isToUnderlineCase) {
        return bInsertOne(tableName, entity, isToUnderlineCase, false, ListUtil.empty());
    }

    @Override
    public <T> Integer bInsertOne(String tableName, T entity, boolean isToUnderlineCase, boolean ignoreNullValue) {
        return bInsertOne(tableName, entity, isToUnderlineCase, ignoreNullValue, ListUtil.empty());
    }

    @Override
    public <T> Integer bInsertOne(String tableName, T entity, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames) {
        if (entity == null) {
            throw new IllegalArgumentException("插入的实体对象不能为空");
        }
        Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, isToUnderlineCase, ignoreNullValue, ignoreFieldNames);
        if (GutilObject.isEmpty(tableName)) {
            tableName = StrUtil.lowerFirst(entity.getClass().getSimpleName());
        }
        return bInsertOne(tableName, rowData);
    }


    // ========== 通用逻辑：批量插入 ==========
    @Override
    public Integer bInsertBatch(String tableName, List<String> headers, List<Map<String, Object>> rowsData) {
        return bInsertBatch(tableName, headers, rowsData, DEFAULT_BATCH_SIZE);
    }

    @Override
    public <T> Integer bInsertBatch(String tableName, Collection<T> entities) {
        return bInsertBatch(tableName, entities, DEFAULT_BATCH_SIZE);
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
            String fields = String.join(",", headers);
            String placeholders = buildPlaceholders(headers.size());
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
            AdvLogSql.of(dataSourceGetter).logExecuteSql(
                    this.getClass(), "bInsertBatch", StrUtil.format("表名：{}，总条数：{}，批次大小：{}", tableName, totalSuccess, batchSize),
                    cost, totalSuccess);
            return totalSuccess;
        } catch (SQLException e) {
            rollbackConnection(connection);
            throw new RuntimeException("批量插入失败，表名：" + tableName, e);
        } finally {
            restoreAutoCommit(connection);
            closeConnection(connection);
        }
    }

    @Override
    public <T> Integer bInsertBatch(String tableName, Collection<T> entities, int batchSize) {
        validateTableName(tableName);
        if (CollUtil.isEmpty(entities)) {
            return 0;
        }
        if (batchSize <= 0) {
            batchSize = DEFAULT_BATCH_SIZE;
        }

        List<Map<String, Object>> rowsData = entities.stream().map(Entity::parse).collect(Collectors.toList());
        Set<String> headers = rowsData.get(0).keySet();
        return bInsertBatch(tableName, ListUtil.toList(headers), rowsData, batchSize);
    }

    // ========== 通用逻辑：插入忽略 ==========
    @Override
    public Integer bInsertIgnore(String tableName, Map<String, Object> rowData) {
        return bInsertIgnore(tableName, rowData, new HashSet<>());
    }

    @Override
    public Integer bInsertIgnore(String tableName, Map<String, Object> rowData, Set<String> conflictKeys) {
        validateTableNameAndData(tableName, rowData);
        String tableNameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
        String schemaNameByTableName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
        String quoteTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameNotSchema, schemaNameByTableName);
        String fields = String.join(",", rowData.keySet());
        String placeholders = buildPlaceholders(rowData.keySet().size());
        String execSql = buildInsertIgnoreSql(quoteTableName, fields, placeholders, conflictKeys);

        List<Object> params = new ArrayList<>(rowData.values());
        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, execSql, params.toArray());
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter).logExecuteSql(this.getClass(), "bInsertIgnore", execSql, params, cost, 1);
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("插入忽略操作失败，表名：" + tableName, e);
        } finally {
            closeConnection(connection);
        }
    }


    @Override
    public <T> Integer bInsertIgnore(String tableName, T entity, Set<String> conflictKeys) {
        return bInsertIgnore(tableName, entity, conflictKeys, true);
    }

    @Override
    public <T> Integer bInsertIgnore(String tableName, T entity, Set<String> conflictKeys, boolean isToUnderlineCase, boolean ignoreNullValue) {
        return bInsertIgnore(tableName, entity, conflictKeys, isToUnderlineCase, ignoreNullValue, ListUtil.empty());
    }

    @Override
    public <T> Integer bInsertIgnore(String tableName, T entity, Set<String> conflictKeys, boolean isToUnderlineCase) {
        return bInsertIgnore(tableName, entity, conflictKeys, isToUnderlineCase, false);
    }

    @Override
    public <T> Integer bInsertIgnore(String tableName, T entity, Set<String> conflictKeys, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames) {
        if (entity == null) {
            throw new IllegalArgumentException("插入的实体对象不能为空");
        }
        Map<String, Object> rowData = GirAdvSqlUtils.getRowData(entity, isToUnderlineCase, ignoreNullValue, ignoreFieldNames);
        if (GutilObject.isEmpty(tableName)) {
            tableName = StrUtil.lowerFirst(entity.getClass().getSimpleName());
        }
        return bInsertIgnore(tableName, rowData, conflictKeys);
    }


    @Override
    public Integer bInsertIgnoreBatch(String tableName, Set<String> headers, List<Map<String, Object>> rowsData, Set<String> conflictKeys) {
        validateTableNameAndData(tableName, ListUtil.toList(headers), rowsData);
        List<List<Map<String, Object>>> batches = CollUtil.split(rowsData, DEFAULT_BATCH_SIZE);
        int totalSuccess = 0;

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (List<Map<String, Object>> batch : batches) {
            for (Map<String, Object> row : batch) {
                totalSuccess += bInsertIgnore(tableName, row, conflictKeys);
            }
        }
        stopWatch.stop();
        long cost = stopWatch.getLastTaskTimeMillis();
        AdvLogSql.of(dataSourceGetter).logExecuteSql(this.getClass(), "bInsertIgnoreBatch", StrUtil.format("表名：{}，总条数：{}", tableName, totalSuccess), cost, totalSuccess);
        return totalSuccess;
    }


    @Override
    public Integer bInsertBySql(String sqlStatement, SqlParamList sqlParamList) {
        if (StrUtil.isEmpty(sqlStatement)) {
            throw new IllegalArgumentException("插入SQL语句不能为空");
        }
        StopWatch stopWatch = new StopWatch();
        Connection connection = dataSourceGetter.getConnection();
        try {
            connection.setAutoCommit(false);
            stopWatch.start();
            Integer result = SqlExecutor.execute(connection, sqlStatement, sqlParamList.toArray());
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter).logExecuteSql(this.getClass(), "bInsertBySql", sqlStatement, sqlParamList.toList(), cost, result);
            return result;
        } catch (SQLException e) {
            rollbackConnection(connection);
            throw new RuntimeException("插入失败", e);
        } finally {
            restoreAutoCommit(connection);
            closeConnection(connection);
        }
    }

    @Override
    public Integer bInsertBySql(String sqlStatementOrDynamicSql, GirSqlParam sqlParam) {
        if (sqlParam == null) {
            return bInsertBySql(sqlStatementOrDynamicSql);
        } else if (sqlParam instanceof SqlParamMap) {
            return bInsertBySql(sqlStatementOrDynamicSql, (SqlParamMap) sqlParam);
        } else if (sqlParam instanceof SqlParamList) {
            return bInsertBySql(sqlStatementOrDynamicSql, (SqlParamList) sqlParam);
        }
        throw new RuntimeException("不支持的sqlParam参数！");
    }


    // ====================== 原有工具方法不动 ======================
    protected String buildPlaceholders(int count) {
        return StrUtil.repeatAndJoin("?", count, ",");
    }

    protected String buildInsertSql(String tableName, String fields, String placeholders) {
        return StrUtil.format("INSERT INTO {} ({}) VALUES ({})", tableName, fields, placeholders);
    }

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
    }

    protected void validateTableName(String tableName) {
        if (StrUtil.isEmpty(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }
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
