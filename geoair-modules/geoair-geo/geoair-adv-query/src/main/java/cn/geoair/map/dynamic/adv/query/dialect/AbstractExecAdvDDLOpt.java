package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.apo.*;
import cn.geoair.comp.dynamic.ds.base.supplier.GirSysSupplierGetter;

import cn.geoair.map.dynamic.adv.query.supplier.GirDataBaseNameGetter;
import cn.geoair.map.dynamic.adv.query.supplier.GirSchemaNameGetter;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.geoair.map.dynamic.adv.query.utils.AdvLogSql;
import cn.geoair.map.dynamic.adv.utils.AdvSqlParser;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.dialect.DialectName;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 数据库DDL操作抽象父类 封装所有数据库通用的DDL逻辑，差异化语法由子类实现
 */
public abstract class AbstractExecAdvDDLOpt implements IAdvDDLOpt {

    // 注入数据源获取器
    protected IDataSourceGetter dataSourceGetter;

    // 表名处理器（差异化）
    protected DialectTableNameProcessor dialectTableNameProcessor;

    protected IAdvBaseOpt baseOpt;

    // 日志实例
    protected static final GiLogger log = GirLoggerFactory.getLogger(AbstractExecAdvDDLOpt.class);

    // ========== 通用初始化 ==========
    public AbstractExecAdvDDLOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt) {
        this.dataSourceGetter = dataSourceGetter;
        this.baseOpt = baseOpt;
        this.dialectTableNameProcessor = getDialectTableNameProcessor();
//        this.dataSourceGetter.setSchemaNameGetterFunction(new GirSchemaNameGetter(this));
//        this.dataSourceGetter.setDatabaseNameGetterFunction(new GirDataBaseNameGetter(this));
    }

    public IAdvBaseOpt getAdvBaseOpt() {
        return baseOpt;
    }

    @Override
    public AdvQueryGlobalConfig getConfig() {
        return getAdvBaseOpt().getConfig();
    }

    /**
     * 创建表名处理器（子类实现：绑定PG/MySQL版本）
     */
    protected abstract DialectTableNameProcessor getDialectTableNameProcessor();

    /** 获取当前数据库方言类型 */
    protected abstract DialectName getDialectName();

    // ========== 通用逻辑：表操作 ==========
    @Override
    public void dDelTable(String tableNameWithSchema) {
        dTruncateTable(tableNameWithSchema);
    }

    @Override
    public void dCopyTableByTableName(String dstTableName, String srcTableName, boolean dataSync) {
        if (StrUtil.isEmpty(dstTableName) || StrUtil.isEmpty(srcTableName)) {
            throw new IllegalArgumentException("源表名和目标表名不能为空");
        }
        String qualifiedDstTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, dstTableName);
        String qualifiedSrcTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, srcTableName);

        if (dataSync) {
            // 复制表结构及数据
            String createSql = buildCreateTableLikeSql(qualifiedDstTableName, qualifiedSrcTableName);
            dExecuteDDL(createSql, dstTableName, "复制表结构");
            String copySql = buildCreateTableFromTableSql(qualifiedDstTableName, qualifiedSrcTableName);
            dExecuteDDL(copySql, dstTableName, "复制数据");
        } else {
            // 仅复制表结构
            String createSql = buildCreateTableLikeSql(qualifiedDstTableName, qualifiedSrcTableName);
            dExecuteDDL(createSql, dstTableName, "复制表结构");
        }
    }

    @Override
    public void dCopyTableBySql(String dstTableName, String sql, boolean dataSync) {
        if (StrUtil.isEmpty(dstTableName) || StrUtil.isEmpty(sql)) {
            throw new IllegalArgumentException("目标表名和SQL不能为空");
        }
        String qualifiedDstTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, dstTableName);

        if (dataSync) {
            // 根据SQL创建表并插入数据
            String createSql = buildCreateTableFromSqlSql(qualifiedDstTableName, sql);
            dExecuteDDL(createSql, dstTableName, "根据SQL复制表结构及数据");
        } else {
            // 仅根据SQL创建表结构（不插入数据）
            String createSql = buildCreateTableFromSqlWithNoDataSql(qualifiedDstTableName, sql);
            dExecuteDDL(createSql, dstTableName, "根据SQL复制表结构");
        }
    }

    @Override
    public void dTruncateTable(String tableNameWithSchema) {
        if (StrUtil.isEmpty(tableNameWithSchema)) {
            throw new IllegalArgumentException("表名不能为空");
        }
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameWithSchema);
        String sql = buildTruncateTableSql(qualifiedTableName);
        dExecuteDDL(sql, tableNameWithSchema, "清空表数据");
    }

    @Override
    public void dDropTable(String tableNameWithSchema) {
        if (StrUtil.isEmpty(tableNameWithSchema)) {
            throw new IllegalArgumentException("表名不能为空");
        }
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableNameWithSchema);
        String sql = buildDropTableSql(qualifiedTableName);
        dExecuteDDL(sql, tableNameWithSchema, "删除表");
    }

    @Override
    public void dRenameTable(String oldTableName, String newTableName) {
        if (StrUtil.isEmpty(oldTableName) || StrUtil.isEmpty(newTableName)) {
            throw new IllegalArgumentException("原表名和新表名都不能为空");
        }
        if (dIsTableExists(newTableName)) {
            throw new RuntimeException(StrUtil.format("新表名[{}]已存在，无法重命名", newTableName));
        }
        if (!dIsTableExists(oldTableName)) {
            throw new RuntimeException(StrUtil.format("原表名[{}]不存在，无法重命名", oldTableName));
        }
        String oldQualifiedName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, oldTableName);
        String newQualifiedName =
                dialectTableNameProcessor.tbGetTableNameNotSchema(newTableName);  //  RENAME TO 不支持带模式名称

        newQualifiedName = dialectTableNameProcessor.tbQuoteTableName(newQualifiedName);

        String sql = buildRenameTableSql(oldQualifiedName, newQualifiedName);
        dExecuteDDL(sql, oldTableName, StrUtil.format(" 将表{}重命名为{} ", oldQualifiedName, newQualifiedName));
    }

    // ========== 通用逻辑：字段操作 ==========
    @Override
    public void dAlterColumn(String tableName, String oldColumnName, FieldBySchemaApo newField) {
        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(oldColumnName) || newField == null) {
            throw new IllegalArgumentException("表名、原字段名和新字段信息不能为空");
        }
        if (!dIsTableExists(tableName)) {
            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法修改字段", tableName));
        }
        DataFieldsApo existingFields = dGetColumnsByTable(tableName);
        boolean oldFieldExists =
                existingFields.getDataFieldList().stream()
                        .anyMatch(f -> oldColumnName.equals(f.getColumnName()));
        if (!oldFieldExists) {
            throw new RuntimeException(StrUtil.format("表[{}]中原字段[{}]不存在，无法修改", tableName, oldColumnName));
        }
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = buildAlterColumnSql(qualifiedTableName, dialectTableNameProcessor.tbQuoteFieldName(oldColumnName), newField);
        dExecuteDDL(sql, tableName, "修改字段[" + oldColumnName + "→" + newField.getColumnName() + "]");
    }

    @Override
    public void dDropColumn(String tableName, String columnName) {
        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(columnName)) {
            throw new IllegalArgumentException("表名和字段名都不能为空");
        }
        if (!dIsTableExists(tableName)) {
            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法删除字段", tableName));
        }
        DataFieldsApo existingFields = dGetColumnsByTable(tableName);
        boolean fieldExists =
                existingFields.getDataFieldList().stream()
                        .anyMatch(f -> columnName.equals(f.getColumnName()));
        if (!fieldExists) {
            log.warn("表[{}]中字段[{}]不存在，无需删除", tableName, columnName);
            return;
        }
        List<String> primaryKeys = dGetPrimaryKeys(tableName);
        if (primaryKeys.contains(columnName)) {
            throw new RuntimeException(StrUtil.format("字段[{}]是主键，需先删除主键约束才能删除字段", columnName));
        }
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = buildDropColumnSql(qualifiedTableName, dialectTableNameProcessor.tbQuoteFieldName(columnName));
        dExecuteDDL(sql, tableName, "删除字段[" + columnName + "]");
    }

    @Override
    public void dAddPrimaryKey(String tableName, List<String> columnNames, String constraintName) {
        if (StrUtil.isEmpty(tableName) || ObjectUtil.isEmpty(columnNames)) {
            throw new IllegalArgumentException("表名和列名列表不能为空");
        }
        if (!dIsTableExists(tableName)) {
            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法添加主键", tableName));
        }
        String pkConstraintName =
                StrUtil.isEmpty(constraintName)
                        ? StrUtil.format("pk_{}_{}", tableName, System.currentTimeMillis())
                        : constraintName;
        List<String> existingPk = dGetPrimaryKeys(tableName);
        if (ObjectUtil.isNotEmpty(existingPk)) {
            throw new RuntimeException(StrUtil.format("表[{}]已存在主键，无法重复添加", tableName));
        }
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        columnNames = columnNames.stream().map(dialectTableNameProcessor::tbQuoteFieldName).collect(Collectors.toList());
        String columns = String.join(", ", columnNames);

        String sql = buildAddPrimaryKeySql(qualifiedTableName, pkConstraintName, columns);
        dExecuteDDL(sql, tableName, "添加主键约束[" + pkConstraintName + "]");
    }

    public void dAddStringPrimaryKey(
            String tableName,
            String pkColumnName,
            int pkColumnLength,
            String constraintName,
            String pkValuePrefix) {
        dAddPrimaryKey(
                tableName,
                pkColumnName,
                constraintName,
                PrimaryKeyType.STRING,
                pkColumnLength,
                pkValuePrefix);
    }

    public void dAddIntAutoPrimaryKey(
            String tableName, String pkColumnName, String constraintName) {
        dAddPrimaryKey(
                tableName, pkColumnName, constraintName, PrimaryKeyType.INT_AUTO, null, null);
    }

    public void dAddBigIntAutoPrimaryKey(
            String tableName, String pkColumnName, String constraintName) {
        dAddPrimaryKey(
                tableName, pkColumnName, constraintName, PrimaryKeyType.BIGINT_AUTO, null, null);
    }

    public void dAddIntNormalPrimaryKey(
            String tableName, String pkColumnName, String constraintName) {
        dAddPrimaryKey(
                tableName, pkColumnName, constraintName, PrimaryKeyType.INT_NORMAL, null, null);
    }

    public void dAddBigIntNormalPrimaryKey(
            String tableName, String pkColumnName, String constraintName) {
        dAddPrimaryKey(
                tableName, pkColumnName, constraintName, PrimaryKeyType.BIGINT_NORMAL, null, null);
    }

    @Override
    public void dDropPrimaryKey(String tableName, String constraintName) {
        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(constraintName)) {
            throw new IllegalArgumentException("表名和约束名都不能为空");
        }
        if (!dIsTableExists(tableName)) {
            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法删除主键", tableName));
        }
        if (!checkConstraintExists(tableName, constraintName, "PRIMARY KEY")) {
            log.warn("表[{}]中主键约束[{}]不存在，无需删除", tableName, constraintName);
            return;
        }
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = buildDropPrimaryKeySql(qualifiedTableName, constraintName);
        dExecuteDDL(sql, tableName, "删除主键约束[" + constraintName + "]");
    }

    @Override
    public void dCreateIndex(
            String tableName, String indexName, List<String> columnNames, boolean isUnique) {
        if (StrUtil.isEmpty(tableName)
            || StrUtil.isEmpty(indexName)
            || ObjectUtil.isEmpty(columnNames)) {
            throw new IllegalArgumentException("表名、索引名和列名列表不能为空");
        }
        if (!dIsTableExists(tableName)) {
            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法创建索引", tableName));
        }
        if (dIndexesExists(tableName, indexName)) {
            throw new RuntimeException(
                    StrUtil.format("表[{}]中索引[{}]已存在，无法重复创建", tableName, indexName));
        }
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        columnNames = columnNames.stream().map(dialectTableNameProcessor::tbQuoteFieldName).collect(Collectors.toList());
        String columns = String.join(", ", columnNames);
        String sql = buildCreateIndexSql(qualifiedTableName, indexName, columns, isUnique);
        dExecuteDDL(sql, tableName, "创建" + (isUnique ? "唯一" : "") + "索引[" + indexName + "]");
    }

    @Override
    public void dDropIndex(String tableName, String indexName) {
        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(indexName)) {
            throw new IllegalArgumentException("表名和索引名都不能为空");
        }
        if (!dIndexesExists(tableName, indexName)) {
            log.warn("表[{}]中索引[{}]不存在，无需删除", tableName, indexName);
            return;
        }
        String sql = buildDropIndexSql(tableName, indexName);
        dExecuteDDL(sql, tableName, "删除索引[" + indexName + "]");
    }

    // ========== 通用逻辑：Schema/模式操作 ==========
    @Override
    public void dCreateSchema(String schemaName) {
        if (StrUtil.isEmpty(schemaName)) {
            throw new IllegalArgumentException("模式名不能为空");
        }
        if (checkSchemaExists(schemaName)) {
            log.warn("模式[{}]已存在，无需重复创建", schemaName);
            return;
        }
        String sql = buildCreateSchemaSql(schemaName);
        dExecuteDDL(sql, schemaName, "创建模式");
    }

    @Override
    public void dDropSchema(String schemaName, boolean cascade) {
        if (StrUtil.isEmpty(schemaName)) {
            throw new IllegalArgumentException("模式名不能为空");
        }
        if (!checkSchemaExists(schemaName)) {
            log.warn("模式[{}]不存在，无需删除", schemaName);
            return;
        }
        String sql = buildDropSchemaSql(schemaName, cascade);
        dExecuteDDL(sql, schemaName, "删除模式");
    }

    // ========== 通用逻辑：表大小查询 ==========
    @Override
    public String dGetTableSizeFormat(String tableName) {
        Long size = dGetTableSize(tableName);
        if (size == null) {
            return null;
        }
        return DataSizeUtil.format(size);
    }

    // ========== 通用逻辑：SQL/表字段查询 ==========
    @Override
    public DataFieldsApo dGetColumnsBySQLOrTable(String tbNameOrSql) {
        boolean isSql = dialectTableNameProcessor.tbTableIsSqlView(tbNameOrSql);
        return isSql ? dGetColumnsBySQL(tbNameOrSql) : dGetColumnsByTable(tbNameOrSql);
    }

    @Override
    public DataFieldsApo dGetColumnsBySQL(String sqlView) {
        if (StrUtil.isEmpty(sqlView)) {
            throw new IllegalArgumentException("SQL视图语句不能为空");
        }
        sqlView = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlView);
        AdvSqlParser.SqlParseResult parse = AdvSqlParser.parse(sqlView);
        String tableName = parse.getTableName();
        DataFieldsApo tableFields = null;
        if (ObjectUtil.isNotEmpty(tableName)) {
            tableName =
                    dialectTableNameProcessor.tbGetTableNameWithSchema(
                            dataSourceGetter, tableName, parse.getSchema());
            tableFields = dGetColumnsByTable(tableName);
        }
        String fieldQuerySql = buildMetadataQuerySql(sqlView);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        DataFieldsApo metadataFromSql = getMetadataFromSql(fieldQuerySql, tableFields, null);
        stopWatch.stop();
        long cost = stopWatch.getLastTaskTimeMillis();
        AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), "dGetColumnsBySQL", fieldQuerySql, cost);
        return metadataFromSql;
    }

    @Override
    public DataFieldsApo dGetColumnsBySQL(String dynamicSql, GirSqlParam sqlParam) {
        if (StrUtil.isEmpty(dynamicSql)) {
            throw new IllegalArgumentException("SQL视图语句不能为空");
        }
        dynamicSql = dialectTableNameProcessor.tbRemoveSqlSpaces(dynamicSql);
        AdvSqlParser.SqlParseResult parse = AdvSqlParser.parse(dynamicSql);
        String tableName = parse.getTableName();
        DataFieldsApo tableFields = null;
        if (ObjectUtil.isNotEmpty(tableName)) {
            tableName =
                    dialectTableNameProcessor.tbGetTableNameWithSchema(
                            dataSourceGetter, tableName, parse.getSchema());
            tableFields = dGetColumnsByTable(tableName);
        }
        String fieldQuerySql = buildMetadataQuerySql(dynamicSql);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        DataFieldsApo metadataFromSqlWithParam = getMetadataFromSqlWithParam(fieldQuerySql, sqlParam, tableFields);
        stopWatch.stop();
        long cost = stopWatch.getLastTaskTimeMillis();
        AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), "dGetColumnsBySQL(带参数)", fieldQuerySql, cost);
        return metadataFromSqlWithParam;
    }

    // ========== 通用工具方法（DDL执行模板 + 耗时统计） ==========

    /**
     * 通用DDL执行方法（无参数）
     */
    public int dExecuteDDL(String sql, String tableName, String operation) {
        Connection connection = null;
        Statement statement = null;
        int result = 0;
        StopWatch stopWatch = new StopWatch();
        try {
            connection = dataSourceGetter.getConnection();
            if (connection == null) {
                throw new IllegalStateException("无法获取数据库连接");
            }
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            stopWatch.start();
            result = statement.executeUpdate(sql);
            connection.commit();
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), operation, sql, cost, result);
            log.debug("{}成功，表名: {}", operation, tableName);
        } catch (SQLException e) {
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(this.getClass(), operation, sql, e);
            rollbackConnection(connection, operation, tableName);
            log.error("{}失败，表名: {}, SQL: {}, 错误: {}", operation, tableName, sql, e.getMessage(), e);
            throw new RuntimeException(StrUtil.format("{}失败: {}", operation, e.getMessage()), e);
        } finally {
            restoreAutoCommit(connection);
            dataSourceGetter.closeResources(null, statement, connection);
        }
        return result;
    }

    /**
     * 通用DDL执行方法（带参数）
     */
    @Override
    public int dExecuteDDL(
            String dynamicSql, SqlParamMap sqlParam, String tableName, String operation) {
        SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParam, dialectTableNameProcessor);
        String execSql = sqlMeta.getSql();
        List<Object> jdbcParams = sqlMeta.getJdbcParamValues();

        Connection connection = null;
        PreparedStatement statement = null;
        int result = 0;
        StopWatch stopWatch = new StopWatch();
        try {
            connection = dataSourceGetter.getConnection();
            if (connection == null) {
                throw new IllegalStateException("无法获取数据库连接");
            }
            connection.setAutoCommit(false);
            statement = connection.prepareStatement(execSql);
            for (int i = 1; i <= jdbcParams.size(); i++) {
                statement.setObject(i, jdbcParams.get(i - 1));
            }
            stopWatch.start();
            result = statement.executeUpdate();
            connection.commit();
            stopWatch.stop();
            long cost = stopWatch.getLastTaskTimeMillis();
            // 带参数日志
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteSql(this.getClass(), operation, execSql, jdbcParams, cost, result);
        } catch (SQLException e) {
            AdvLogSql.of(dataSourceGetter, getConfig()).logExecuteError(this.getClass(), operation, execSql, jdbcParams, e);
            rollbackConnection(connection, operation, tableName);
            log.error(
                    "{}失败，表名: {}, SQL: {}, 错误: {}",
                    operation,
                    tableName,
                    execSql,
                    e.getMessage(),
                    e);
            throw new RuntimeException(StrUtil.format("{}失败: {}", operation, e.getMessage()), e);
        } finally {
            restoreAutoCommit(connection);
            dataSourceGetter.closeResources(null, statement, connection);
        }
        return result;
    }

    // ========== 通用事务/资源 ==========
    protected void rollbackConnection(Connection connection, String operation, String tableName) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                log.warn("{}失败，回滚操作出错，表名: {}", operation, tableName, ex);
            }
        }
    }

    protected void restoreAutoCommit(Connection connection) {
        if (connection != null) {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                log.warn("恢复自动提交模式失败", e);
            }
        }
    }

    protected DataFieldsApo getMetadataFromSql(
            String dynamicSql, DataFieldsApo tableFields, GirSqlParam sqlParam) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        DataFieldsApo dataFieldVO = new DataFieldsApo();
        try {
            connection = dataSourceGetter.getConnection();
            if (connection == null) {
                throw new IllegalStateException("无法获取数据库连接");
            }
            if (sqlParam != null) {
                if (sqlParam instanceof SqlParamList) {
                    SqlParamList sqlParamList = (SqlParamList) sqlParam;
                    statement = connection.prepareStatement(dynamicSql);
                    List<Object> list = sqlParamList.toList();
                    for (int i = 1; i <= list.size(); i++) {
                        statement.setObject(i, list.get(i - 1));
                    }
                } else {
                    SqlParamMap sqlParamMap = (SqlParamMap) sqlParam;
                    SqlMeta sqlMeta = GirAdvSqlUtils.parseSqlWithParam(dynamicSql, sqlParamMap, dialectTableNameProcessor);
                    statement = connection.prepareStatement(sqlMeta.getSql());
                    for (int i = 1; i <= sqlMeta.getJdbcParamValues().size(); i++) {
                        statement.setObject(i, sqlMeta.getJdbcParamValues().get(i - 1));
                    }
                }
            } else {
                statement = connection.prepareStatement(dynamicSql);
            }
            resultSet = statement.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();
            if (metaData == null) {
                return dataFieldVO;
            }
            List<FieldBySchemaApo> dataFieldList = new ArrayList<>();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                String baseColumnName = getBaseColumnName(metaData, i);
                String columnTypeName = getColumnTypeName(metaData, i);
                if (tableFields != null) {
                    Optional<FieldBySchemaApo> fieldOpt =
                            tableFields.findField(
                                    field -> field.getOriginalColumnName().equals(baseColumnName));
                    if (fieldOpt.isPresent()) {
                        FieldBySchemaApo field = fieldOpt.get();
                        field.setColumnName(columnName);
                        dataFieldList.add(field);
                        continue;
                    }
                }
                FieldBySchemaApo field = new FieldBySchemaApo();
                field.setDialectName(getDialectName());
                field.setColumnName(columnName);
                field.setOriginalColumnName(baseColumnName);
                field.setUdtName(columnTypeName);
                field.setIsNullable(
                        metaData.isNullable(i) == ResultSetMetaData.columnNoNulls ? "NO" : "YES");
                setFieldLengthInfo(metaData, i, field);
                field.determineGeometryFieldIs();
                dataFieldList.add(field);
            }
            dataFieldVO = new DataFieldsApo(dataFieldList);
        } catch (SQLException e) {
            log.error("通过SQL查询字段信息失败，错误: {}", e.getMessage(), e);
            throw new RuntimeException("获取字段信息失败: " + e.getMessage(), e);
        } finally {
            dataSourceGetter.closeResources(resultSet, statement, connection);
        }
        return dataFieldVO;
    }

    protected DataFieldsApo getMetadataFromSqlWithParam(
            String sql, GirSqlParam sqlParam, DataFieldsApo tableFields) {
        return getMetadataFromSql(sql, tableFields, sqlParam);
    }

    // ========== 差异化抽象方法 ==========
    protected abstract String buildTruncateTableSql(String qualifiedTableName);

    protected abstract String buildDropTableSql(String qualifiedTableName);

    protected abstract String buildRenameTableSql(String oldQualifiedName, String newQualifiedName);

    protected abstract String buildAlterColumnSql(String qualifiedTableName, String oldColumnName, FieldBySchemaApo newField);

    protected abstract String buildDropColumnSql(String qualifiedTableName, String columnName);

    protected abstract boolean checkConstraintExists(String tableName, String constraintName, String constraintType);

    protected abstract String buildAddPrimaryKeySql(String qualifiedTableName, String constraintName, String columns);

    protected abstract String buildDropPrimaryKeySql(String qualifiedTableName, String constraintName);

    protected abstract String buildCreateIndexSql(String qualifiedTableName, String indexName, String columns, boolean isUnique);

    protected abstract String buildDropIndexSql(String tableName, String indexName);

    protected abstract boolean checkSchemaExists(String schemaName);

    protected abstract String buildCreateSchemaSql(String schemaName);

    protected abstract String buildDropSchemaSql(String schemaName, boolean cascade);

    protected abstract String buildMetadataQuerySql(String sqlView);

    protected abstract String getBaseColumnName(ResultSetMetaData metaData, int columnIndex) throws SQLException;

    protected abstract String getColumnTypeName(ResultSetMetaData metaData, int columnIndex) throws SQLException;

    protected abstract void setFieldLengthInfo(ResultSetMetaData metaData, int columnIndex, FieldBySchemaApo field) throws SQLException;


    /**
     * 构建复制表结构及数据的SQL（根据源表名）
     *
     * @param dstTableName 目标表名（已包含schema）
     * @param srcTableName 源表名（已包含schema）
     * @return 复制表结构及数据的SQL
     */
    protected abstract String buildCreateTableFromTableSql(String dstTableName, String srcTableName);

    /**
     * 构建仅复制表结构的SQL（根据源表名）
     *
     * @param dstTableName 目标表名（已包含schema）
     * @param srcTableName 源表名（已包含schema）
     * @return 仅复制表结构的SQL
     */
    protected abstract String buildCreateTableLikeSql(String dstTableName, String srcTableName);

    /**
     * 根据自定义SQL构建创建表并插入数据的SQL
     *
     * @param dstTableName 目标表名（已包含schema）
     * @param sql          源数据查询SQL
     * @return 创建表并插入数据的SQL
     */
    protected abstract String buildCreateTableFromSqlSql(String dstTableName, String sql);

    /**
     * 根据自定义SQL构建仅创建表结构（不插入数据）的SQL
     *
     * @param dstTableName 目标表名（已包含schema）
     * @param sql          源数据查询SQL
     * @return 仅创建表结构的SQL
     */
    protected abstract String buildCreateTableFromSqlWithNoDataSql(String dstTableName, String sql);


    @Override
    public void dCreateTable(String tableName, List<FieldBySchemaApo> fields, String primaryKey) {
        throw new RuntimeException("暂时没有实现");
    }

    @Override
    public void dAddColumn(String tableName, FieldBySchemaApo field) {
        throw new RuntimeException("暂时没有实现");
    }


}
