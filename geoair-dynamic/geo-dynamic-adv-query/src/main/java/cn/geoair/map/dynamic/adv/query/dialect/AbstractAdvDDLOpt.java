package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.map.dynamic.adv.mybatis.SqlEngineUtil;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.utils.AdvSqlParser;
import cn.geoair.map.dynamic.ds.IDataSourceGetter;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 数据库DDL操作抽象父类 封装所有数据库通用的DDL逻辑，差异化语法由子类实现 */
public abstract class AbstractAdvDDLOpt implements IAdvDDLOpt {

    // 注入数据源获取器
    protected IDataSourceGetter dataSourceGetter;

    // 表名处理器（差异化）
    protected DialectTableNameProcessor dialectTableNameProcessor;

    // 日志实例
    protected static final GiLogger log = GirLogger.getLoger(AbstractAdvDDLOpt.class);

    // ========== 通用初始化 ==========
    public AbstractAdvDDLOpt(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
        this.dialectTableNameProcessor = getDialectTableNameProcessor();
    }

    /** 获取抽象查询对象 */
    protected abstract IAdvBaseOpt getAdvBaseOpt();

    /** 创建表名处理器（子类实现：绑定PG/MySQL版本） */
    protected abstract DialectTableNameProcessor getDialectTableNameProcessor();

    // ========== 通用逻辑：表操作 ==========
    @Override
    public void dDelTable(String tableName) {
        dTruncateTable(tableName);
    }

    @Override
    public void dTruncateTable(String tableName) {
        // 通用参数校验
        if (StrUtil.isEmpty(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }

        // 差异化：构建TRUNCATE SQL
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = buildTruncateTableSql(qualifiedTableName);

        dExecuteDDL(sql, tableName, "清空表数据");
    }

    @Override
    public void dDropTable(String tableName) {
        // 通用参数校验
        if (StrUtil.isEmpty(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }

        // 差异化：构建DROP TABLE SQL
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = buildDropTableSql(qualifiedTableName);

        dExecuteDDL(sql, tableName, "删除表");
    }

    @Override
    public void dRenameTable(String oldTableName, String newTableName) {
        // 通用参数校验
        if (StrUtil.isEmpty(oldTableName) || StrUtil.isEmpty(newTableName)) {
            throw new IllegalArgumentException("原表名和新表名都不能为空");
        }
        if (dIsTableExists(newTableName)) {
            throw new RuntimeException(StrUtil.format("新表名[{}]已存在，无法重命名", newTableName));
        }
        if (!dIsTableExists(oldTableName)) {
            throw new RuntimeException(StrUtil.format("原表名[{}]不存在，无法重命名", oldTableName));
        }

        // 差异化：构建重命名表SQL
        String oldQualifiedName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, oldTableName);
        String newQualifiedName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, newTableName);
        String sql = buildRenameTableSql(oldQualifiedName, newQualifiedName);

        dExecuteDDL(sql, oldTableName, "重命名表");
    }

    // ========== 通用逻辑：字段操作 ==========
    @Override
    public void dAlterColumn(String tableName, String oldColumnName, FieldBySchemaApo newField) {
        // 通用参数校验
        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(oldColumnName) || newField == null) {
            throw new IllegalArgumentException("表名、原字段名和新字段信息不能为空");
        }
        if (!dIsTableExists(tableName)) {
            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法修改字段", tableName));
        }

        // 通用检查：原字段是否存在
        DataFieldsApo existingFields = dGetColumnsByTable(tableName);
        boolean oldFieldExists =
                existingFields.getDataFieldList().stream()
                        .anyMatch(f -> oldColumnName.equals(f.getColumnName()));
        if (!oldFieldExists) {
            throw new RuntimeException(
                    StrUtil.format("表[{}]中原字段[{}]不存在，无法修改", tableName, oldColumnName));
        }

        // 差异化：构建修改字段SQL
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = buildAlterColumnSql(qualifiedTableName, oldColumnName, newField);

        dExecuteDDL(sql, tableName, "修改字段[" + oldColumnName + "→" + newField.getColumnName() + "]");
    }

    @Override
    public void dDropColumn(String tableName, String columnName) {
        // 通用参数校验
        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(columnName)) {
            throw new IllegalArgumentException("表名和字段名都不能为空");
        }
        if (!dIsTableExists(tableName)) {
            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法删除字段", tableName));
        }

        // 通用检查：字段是否存在
        DataFieldsApo existingFields = dGetColumnsByTable(tableName);
        boolean fieldExists =
                existingFields.getDataFieldList().stream()
                        .anyMatch(f -> columnName.equals(f.getColumnName()));
        if (!fieldExists) {
            log.warn("表[{}]中字段[{}]不存在，无需删除", tableName, columnName);
            return;
        }

        // 通用检查：字段是否为主键
        List<String> primaryKeys = dGetPrimaryKeys(tableName);
        if (primaryKeys.contains(columnName)) {
            throw new RuntimeException(StrUtil.format("字段[{}]是主键，需先删除主键约束才能删除字段", columnName));
        }

        // 差异化：构建删除字段SQL
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = buildDropColumnSql(qualifiedTableName, columnName);

        dExecuteDDL(sql, tableName, "删除字段[" + columnName + "]");
    }

    @Override
    public void dAddPrimaryKey(String tableName, List<String> columnNames, String constraintName) {
        // 通用参数校验
        if (StrUtil.isEmpty(tableName) || ObjectUtil.isEmpty(columnNames)) {
            throw new IllegalArgumentException("表名和列名列表不能为空");
        }
        if (!dIsTableExists(tableName)) {
            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法添加主键", tableName));
        }

        // 通用：生成约束名（如果未指定）
        String pkConstraintName =
                StrUtil.isEmpty(constraintName)
                        ? StrUtil.format("pk_{}_{}", tableName, System.currentTimeMillis())
                        : constraintName;

        // 通用检查：是否已存在主键
        List<String> existingPk = dGetPrimaryKeys(tableName);
        if (ObjectUtil.isNotEmpty(existingPk)) {
            throw new RuntimeException(StrUtil.format("表[{}]已存在主键，无法重复添加", tableName));
        }

        // 差异化：构建添加主键SQL
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
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
        // 通用参数校验
        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(constraintName)) {
            throw new IllegalArgumentException("表名和约束名都不能为空");
        }
        if (!dIsTableExists(tableName)) {
            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法删除主键", tableName));
        }

        // 差异化：检查约束是否存在
        if (!checkConstraintExists(tableName, constraintName, "PRIMARY KEY")) {
            log.warn("表[{}]中主键约束[{}]不存在，无需删除", tableName, constraintName);
            return;
        }

        // 差异化：构建删除主键SQL
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String sql = buildDropPrimaryKeySql(qualifiedTableName, constraintName);

        dExecuteDDL(sql, tableName, "删除主键约束[" + constraintName + "]");
    }

    @Override
    public void dCreateIndex(
            String tableName, String indexName, List<String> columnNames, boolean isUnique) {
        // 通用参数校验
        if (StrUtil.isEmpty(tableName)
                || StrUtil.isEmpty(indexName)
                || ObjectUtil.isEmpty(columnNames)) {
            throw new IllegalArgumentException("表名、索引名和列名列表不能为空");
        }
        if (!dIsTableExists(tableName)) {
            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法创建索引", tableName));
        }

        // 通用检查：索引是否已存在
        if (dIndexesExists(tableName, indexName)) {
            throw new RuntimeException(
                    StrUtil.format("表[{}]中索引[{}]已存在，无法重复创建", tableName, indexName));
        }

        // 差异化：构建创建索引SQL
        String qualifiedTableName =
                dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
        String columns = String.join(", ", columnNames);
        String sql = buildCreateIndexSql(qualifiedTableName, indexName, columns, isUnique);

        dExecuteDDL(sql, tableName, "创建" + (isUnique ? "唯一" : "") + "索引[" + indexName + "]");
    }

    @Override
    public void dDropIndex(String tableName, String indexName) {
        // 通用参数校验
        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(indexName)) {
            throw new IllegalArgumentException("表名和索引名都不能为空");
        }

        // 通用检查：索引是否存在
        if (!dIndexesExists(tableName, indexName)) {
            log.warn("表[{}]中索引[{}]不存在，无需删除", tableName, indexName);
            return;
        }

        // 差异化：构建删除索引SQL
        String sql = buildDropIndexSql(tableName, indexName);

        dExecuteDDL(sql, tableName, "删除索引[" + indexName + "]");
    }

    // ========== 通用逻辑：Schema/模式操作 ==========
    @Override
    public void dCreateSchema(String schemaName) {
        // 通用参数校验
        if (StrUtil.isEmpty(schemaName)) {
            throw new IllegalArgumentException("模式名不能为空");
        }

        // 差异化：检查模式是否已存在
        if (checkSchemaExists(schemaName)) {
            log.warn("模式[{}]已存在，无需重复创建", schemaName);
            return;
        }

        // 差异化：构建创建模式SQL
        String sql = buildCreateSchemaSql(schemaName);

        dExecuteDDL(sql, schemaName, "创建模式");
    }

    @Override
    public void dDropSchema(String schemaName, boolean cascade) {
        // 通用参数校验
        if (StrUtil.isEmpty(schemaName)) {
            throw new IllegalArgumentException("模式名不能为空");
        }

        // 差异化：检查模式是否存在
        if (!checkSchemaExists(schemaName)) {
            log.warn("模式[{}]不存在，无需删除", schemaName);
            return;
        }

        // 差异化：构建删除模式SQL
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
        // 通用参数校验
        if (StrUtil.isEmpty(sqlView)) {
            throw new IllegalArgumentException("SQL视图语句不能为空");
        }
        sqlView = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlView);

        // 通用解析：提取表名和字段
        AdvSqlParser.SqlParseResult parse = AdvSqlParser.parse(sqlView);
        String tableName = parse.getTableName();
        DataFieldsApo tableFields = null;
        if (ObjectUtil.isNotEmpty(tableName)) {
            tableName =
                    dialectTableNameProcessor.tbGetTableNameWithSchema(
                            dataSourceGetter, tableName, parse.getSchema());
            tableFields = dGetColumnsByTable(tableName);
        }

        // 通用：构建元数据查询SQL（LIMIT 0/ROWNUM 0）
        String fieldQuerySql = buildMetadataQuerySql(sqlView);
        log.debug(
                "schema:[{}] db:[{}] SQL的元数据查询：{}",
                dataSourceGetter.getSchemaName(),
                dataSourceGetter.getDataSourceId(),
                fieldQuerySql);

        // 通用：获取元数据并封装结果
        return getMetadataFromSql(fieldQuerySql, tableFields, null);
    }

    @Override
    public DataFieldsApo dGetColumnsBySQL(String sqlStatement, SqlParamMap sqlParam) {
        // 通用参数校验
        if (StrUtil.isEmpty(sqlStatement)) {
            throw new IllegalArgumentException("SQL视图语句不能为空");
        }
        sqlStatement = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);

        // 通用解析：提取表名和字段
        AdvSqlParser.SqlParseResult parse = AdvSqlParser.parse(sqlStatement);
        String tableName = parse.getTableName();
        DataFieldsApo tableFields = null;
        if (ObjectUtil.isNotEmpty(tableName)) {
            tableName =
                    dialectTableNameProcessor.tbGetTableNameWithSchema(
                            dataSourceGetter, tableName, parse.getSchema());
            tableFields = dGetColumnsByTable(tableName);
        }

        // 通用：构建元数据查询SQL
        String fieldQuerySql = buildMetadataQuerySql(sqlStatement);
        log.debug(
                "schema:[{}] db:[{}] SQL的元数据查询：{}",
                dataSourceGetter.getSchemaName(),
                dataSourceGetter.getDataSourceId(),
                fieldQuerySql);

        // 通用：获取带参数的元数据并封装结果
        return getMetadataFromSqlWithParam(fieldQuerySql, sqlParam, tableFields);
    }

    // ========== 通用工具方法（DDL执行模板） ==========

    /** 通用DDL执行方法（无参数） */
    public int dExecuteDDL(String sql, String tableName, String operation) {
        Connection connection = null;
        Statement statement = null;
        int result = 0;
        try {
            connection = dataSourceGetter.getConnection();
            if (connection == null) {
                throw new IllegalStateException("无法获取数据库连接");
            }

            // 通用事务控制：关闭自动提交
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            log.debug("executeDDL执行的sql为：{}", sql);
            result = statement.executeUpdate(sql);
            connection.commit();

            log.debug("{}成功，表名: {}", operation, tableName);
        } catch (SQLException e) {
            // 通用回滚
            rollbackConnection(connection, operation, tableName);
            log.error("{}失败，表名: {}, SQL: {}, 错误: {}", operation, tableName, sql, e.getMessage(), e);
            throw new RuntimeException(StrUtil.format("{}失败: {}", operation, e.getMessage()), e);
        } finally {
            // 通用资源清理
            restoreAutoCommit(connection);
            dataSourceGetter.closeResources(null, statement, connection);
        }
        return result;
    }

    /** 通用DDL执行方法（带参数） */
    @Override
    public int dExecuteDDL(
            String sqlStatement, SqlParamMap sqlParam, String tableName, String operation) {
        // 通用SQL解析
        String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
        SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(cleanSql, sqlParam);
        String execSql = sqlMeta.getSql();
        List<Object> jdbcParams = sqlMeta.getJdbcParamValues();

        Connection connection = null;
        PreparedStatement statement = null;
        int result = 0;
        try {
            connection = dataSourceGetter.getConnection();
            if (connection == null) {
                throw new IllegalStateException("无法获取数据库连接");
            }

            // 通用事务控制
            connection.setAutoCommit(false);
            statement = connection.prepareStatement(execSql);
            log.debug("executeDDL执行的sql为：{}", execSql);

            // 通用参数设置
            for (int i = 1; i <= jdbcParams.size(); i++) {
                statement.setObject(i, jdbcParams.get(i - 1));
            }

            result = statement.executeUpdate();
            connection.commit();
            log.debug("{}成功，表名: {}", operation, tableName);
        } catch (SQLException e) {
            // 通用回滚
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
            // 通用资源清理
            restoreAutoCommit(connection);
            dataSourceGetter.closeResources(null, statement, connection);
        }
        return result;
    }

    // ========== 通用工具方法（资源/事务控制） ==========

    /** 回滚连接 */
    protected void rollbackConnection(Connection connection, String operation, String tableName) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                log.warn("{}失败，回滚操作出错，表名: {}", operation, tableName, ex);
            }
        }
    }

    /** 恢复自动提交 */
    protected void restoreAutoCommit(Connection connection) {
        if (connection != null) {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                log.warn("恢复自动提交模式失败", e);
            }
        }
    }

    /** 从SQL获取元数据（通用逻辑） */
    protected DataFieldsApo getMetadataFromSql(
            String sql, DataFieldsApo tableFields, SqlParamMap sqlParam) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        DataFieldsApo dataFieldVO = new DataFieldsApo();

        try {
            connection = dataSourceGetter.getConnection();
            if (connection == null) {
                throw new IllegalStateException("无法获取数据库连接");
            }

            // 带参数/无参数处理
            if (sqlParam != null) {
                SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(sql, sqlParam);
                statement = connection.prepareStatement(sqlMeta.getSql());
                for (int i = 1; i <= sqlMeta.getJdbcParamValues().size(); i++) {
                    statement.setObject(i, sqlMeta.getJdbcParamValues().get(i - 1));
                }
            } else {
                statement = connection.prepareStatement(sql);
            }

            resultSet = statement.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();
            if (metaData == null) {
                return dataFieldVO;
            }

            // 通用元数据解析
            List<FieldBySchemaApo> dataFieldList = new ArrayList<>();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                String baseColumnName = getBaseColumnName(metaData, i);
                String columnTypeName = getColumnTypeName(metaData, i);

                // 复用表字段元数据（如果存在）
                if (tableFields != null) {
                    Optional<FieldBySchemaApo> fieldOpt =
                            tableFields.getDataField(
                                    fieldBySchemaApo ->
                                            fieldBySchemaApo
                                                            .getOriginalColumnName()
                                                            .equals(baseColumnName)
                                                    ? fieldBySchemaApo
                                                    : null);
                    if (fieldOpt.isPresent()) {
                        FieldBySchemaApo field = fieldOpt.get();
                        field.setColumnName(columnName);
                        dataFieldList.add(field);
                        continue;
                    }
                }

                // 构建字段元数据
                FieldBySchemaApo field = new FieldBySchemaApo();
                field.setColumnName(columnName);
                field.setOriginalColumnName(baseColumnName);
                field.setUdtName(columnTypeName);
                field.setIsNullable(
                        metaData.isNullable(i) == ResultSetMetaData.columnNoNulls ? "NO" : "YES");
                // 差异化：设置字段长度信息
                setFieldLengthInfo(metaData, i, field);

                dataFieldList.add(field);
            }
            dataFieldVO.setDataFieldList(dataFieldList);
        } catch (SQLException e) {
            log.error("通过SQL查询字段信息失败，错误: {}", e.getMessage(), e);
            throw new RuntimeException("获取字段信息失败: " + e.getMessage(), e);
        } finally {
            dataSourceGetter.closeResources(resultSet, statement, connection);
        }
        return dataFieldVO;
    }

    /** 从带参数SQL获取元数据（通用封装） */
    protected DataFieldsApo getMetadataFromSqlWithParam(
            String sql, SqlParamMap sqlParam, DataFieldsApo tableFields) {
        return getMetadataFromSql(sql, tableFields, sqlParam);
    }

    // ========== 差异化抽象方法（子类必须实现） ==========
    // 1. 表操作相关
    protected abstract String buildTruncateTableSql(String qualifiedTableName);

    protected abstract String buildDropTableSql(String qualifiedTableName);

    protected abstract String buildRenameTableSql(String oldQualifiedName, String newQualifiedName);

    protected abstract String buildAlterColumnSql(
            String qualifiedTableName, String oldColumnName, FieldBySchemaApo newField);

    protected abstract String buildDropColumnSql(String qualifiedTableName, String columnName);

    protected abstract boolean checkConstraintExists(
            String tableName, String constraintName, String constraintType);

    protected abstract String buildAddPrimaryKeySql(
            String qualifiedTableName, String constraintName, String columns);

    protected abstract String buildDropPrimaryKeySql(
            String qualifiedTableName, String constraintName);

    protected abstract String buildCreateIndexSql(
            String qualifiedTableName, String indexName, String columns, boolean isUnique);

    protected abstract String buildDropIndexSql(String tableName, String indexName);

    protected abstract boolean checkSchemaExists(String schemaName);

    protected abstract String buildCreateSchemaSql(String schemaName);

    protected abstract String buildDropSchemaSql(String schemaName, boolean cascade);

    // 6. 元数据相关
    protected abstract String buildMetadataQuerySql(String sqlView);

    protected abstract String getBaseColumnName(ResultSetMetaData metaData, int columnIndex)
            throws SQLException;

    protected abstract String getColumnTypeName(ResultSetMetaData metaData, int columnIndex)
            throws SQLException;

    protected abstract void setFieldLengthInfo(
            ResultSetMetaData metaData, int columnIndex, FieldBySchemaApo field)
            throws SQLException;

    // 7. 未实现方法（子类可选择实现）
    @Override
    public void dCreateTable(String tableName, List<FieldBySchemaApo> fields, String primaryKey) {
        throw new RuntimeException("暂时没有实现");
    }

    @Override
    public void dAddColumn(String tableName, FieldBySchemaApo field) {
        throw new RuntimeException("暂时没有实现");
    }
}
