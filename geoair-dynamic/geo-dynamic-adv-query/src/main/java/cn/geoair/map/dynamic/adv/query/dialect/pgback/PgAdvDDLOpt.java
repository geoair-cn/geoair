//package cn.geoair.map.dynamic.adv.query.dialect.pgback;
//
//
//import cn.geoair.gtc.base.log.GiLogger;
//import cn.geoair.gtc.base.log.GirLogger;
//import cn.geoair.map.dynamic.adv.mybatis.SqlEngineUtil;
//import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
//import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
//import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
//import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
//import cn.geoair.map.dynamic.adv.query.apo.IndexApo;
//import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
//import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
//import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
//import cn.geoair.map.dynamic.adv.utils.AdvSqlParser;
//import cn.geoair.map.dynamic.ds.IDataSourceGetter;
//import cn.hutool.core.collection.ListUtil;
//import cn.hutool.core.io.unit.DataSizeUtil;
//import cn.hutool.core.util.ObjectUtil;
//import cn.hutool.core.util.StrUtil;
//
//
//import org.postgresql.jdbc.PgResultSetMetaData;
//
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
///**
// * @author ：张逢吉
// * @date ：Created in 2025/10/9 10:16
// * @description： 基于postgresql的基础DDl实现
// */
//
//public class PgAdvDDLOpt implements IAdvDDLOpt {
//
//    PgAdvBaseOpt baseOpt;
//
//    IDataSourceGetter dataSourceGetter;
//
//    public PgAdvDDLOpt(IDataSourceGetter dataSourceGetter) {
//        this.dataSourceGetter = dataSourceGetter;
//        baseOpt = new PgAdvBaseOpt(dataSourceGetter);
//    }
//
//    private static final GiLogger log = GirLogger.getLoger();
//    DialectTableNameProcessor dialectTableNameProcessor = PgDialectTableNameUtil.getInstance();
//
//    @Override
//    public void dDelTable(String tableName) {
//        dTruncateTable(tableName);
//    }
//
//    @Override
//    public void dTruncateTable(String tableName) {
//        if (tableName == null || tableName.trim().isEmpty()) {
//            throw new IllegalArgumentException("表名不能为空");
//        }
//
//        // 构建带schema的表名
//        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
//        String sql = StrUtil.format("TRUNCATE TABLE {} RESTART IDENTITY CASCADE", qualifiedTableName);
//
//        dExecuteDDL(sql, tableName, "清空表数据");
//    }
//
//    @Override
//    public void dDropTable(String tableName) {
//        if (tableName == null || tableName.trim().isEmpty()) {
//            throw new IllegalArgumentException("表名不能为空");
//        }
//        // 构建带schema的表名
//        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
//        String sql = StrUtil.format("DROP TABLE IF EXISTS {} CASCADE", qualifiedTableName);
//        dExecuteDDL(sql, tableName, "删除表");
//    }
//
//    @Override
//    public List<String> dGetAllSchemas() {
//        String sql = "SELECT schema_name FROM information_schema.schemata WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast') ORDER BY schema_name";
//        List<GirAdvOneRow> girAdvOneRows = baseOpt.bSelectList(sql);
//        List<String> schemas = new ArrayList<>();
//        if (ObjectUtil.isNotEmpty(girAdvOneRows)) {
//            girAdvOneRows.forEach(row -> {
//                schemas.add(row.getStr("schema_name"));
//            });
//        }
//        return schemas;
//    }
//
//
//    @Override
//    public DataFieldsApo dGetColumnsByTable(String tableName) {
//        if (StrUtil.isEmpty(tableName)) {
//            return null;
//        }
//        String schemaNameBySQL = dialectTableNameProcessor.tbExtractSchemaName(tableName);
//        ;
//        String schemaName = null;
//        if (ObjectUtil.isNotEmpty(schemaNameBySQL)) {
//            schemaName = schemaNameBySQL;
//        } else {
//            schemaName = dataSourceGetter.getSchemaName();
//        }
//        String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
//
//        // 使用StringBuilder构建SQL语句
//        StringBuilder sqlBuilder = new StringBuilder();
//
//        // SELECT子句
//        sqlBuilder.append("SELECT ")
//                .append("c.*, ")
//                .append("temp.column_comment, ")
//                .append("CASE WHEN pk.attnum IS NOT NULL THEN 't' ELSE 'f' END AS primary_key_is ");
//
//
//        // FROM和JOIN子句，增加pg_namespace关联
//        sqlBuilder.append("FROM ")
//                .append("information_schema.columns AS c ")
//                .append("JOIN pg_class AS cl ON c.table_name = cl.relname ")
//                .append("JOIN pg_namespace AS ns ON cl.relnamespace = ns.oid AND ns.nspname = c.table_schema ")
//                .append("JOIN pg_attribute AS a ON cl.oid = a.attrelid AND c.column_name = a.attname ")
//                .append("JOIN pg_type AS t ON a.atttypid = t.oid ");
//
//        // 字段注释子查询，增加schema过滤
//        sqlBuilder.append("LEFT JOIN ( ")
//                .append("    SELECT ")
//                .append("    A.attname AS COLUMN_NAME, ")
//                .append("    d.description AS column_comment ")
//                .append("    FROM ")
//                .append("    pg_class C ")
//                .append("    JOIN pg_namespace ns ON C.relnamespace = ns.oid ");
//        if (ObjectUtil.isNotEmpty(schemaName)) {
//            sqlBuilder.append("    AND ns.nspname = '").append(schemaName).append("' ");
//        }
//        sqlBuilder.append("    JOIN pg_attribute A ON C.OID = A.attrelid ")
//                .append("    LEFT JOIN pg_description d ON C.OID = d.objoid AND A.attnum = d.objsubid ")
//                .append("    WHERE ")
//                .append("    C.relname = '").append(notSchemaTableName).append("' ")
//                .append(") AS temp ON temp.COLUMN_NAME = c.column_name ");
//
//        // 主键信息子查询，增加schema过滤
//        sqlBuilder.append("LEFT JOIN ( ")
//                .append("    SELECT ")
//                .append("    unnest(conkey) AS attnum ")
//                .append("    FROM ")
//                .append("    pg_constraint ")
//                .append("    WHERE ")
//                .append("    conrelid = (SELECT oid FROM pg_class WHERE relname = '").append(notSchemaTableName).append("' ");
//        if (ObjectUtil.isNotEmpty(schemaName)) {
//            sqlBuilder.append("    AND relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = '").append(schemaName).append("') ");
//        }
//        sqlBuilder.append(") ")
//                .append("    AND contype = 'p' ")
//                .append(") AS pk ON a.attnum = pk.attnum ");
//
//
//        // WHERE子句
//        sqlBuilder.append("WHERE ")
//                .append("c.table_name = '").append(notSchemaTableName).append("' ");
//
//        // 处理schema过滤条件
//        if (ObjectUtil.isNotEmpty(schemaName)) {
//            sqlBuilder.append("AND c.\"table_schema\" = '").append(schemaName).append("' ");
//        }
//
//        // 执行查询获取字段列表
//        List<FieldBySchemaApo> fieldBySchemaApos = baseOpt.bSelectObjList(sqlBuilder.toString(), FieldBySchemaApo.class);
//
//        // 处理查询结果
//        for (FieldBySchemaApo fieldBySchemaApo : fieldBySchemaApos) {
//            fieldBySchemaApo.setOriginalColumnName(fieldBySchemaApo.getColumnName());
//        }
//
//        DataFieldsApo dataFieldsApo = new DataFieldsApo();
//        dataFieldsApo.setDataFieldList(fieldBySchemaApos);
//        return dataFieldsApo;
//    }
//
//    @Override
//    public DataFieldsApo dGetColumnsBySQL(String sqlView) {
//        // 参数校验
//        if (sqlView == null || sqlView.trim().isEmpty()) {
//            throw new IllegalArgumentException("SQL视图语句不能为空");
//        }
//        sqlView = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlView);
//        AdvSqlParser.SqlParseResult parse = AdvSqlParser.parse(sqlView);
//        String tableName = parse.getTableName();
//        List<String> fields = parse.getFields();
//        DataFieldsApo dataFieldsApoByTable = null;
//        if (ObjectUtil.isNotEmpty(tableName)) {
//            tableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName, parse.getSchema());
//            dataFieldsApoByTable = dGetColumnsByTable(tableName);
//        }
//
//        DataFieldsApo dataFieldVO = new DataFieldsApo();
//        // 构建查询元数据的SQL，使用LIMIT 0仅获取结构不获取数据
//        String fieldQuerySql = StrUtil.format("SELECT * FROM ({}) AS temp_table LIMIT 0", sqlView);
//        log.info("schema:[" + dataSourceGetter.getSchemaName() + "] " + "db:[" + dataSourceGetter.getDataSourceId() + "]" + " SQL的元数据查询：{}", fieldQuerySql);
//        Connection connection = null;
//        Statement statement = null;
//        ResultSet resultSet = null;
//
//        try {
//            connection = dataSourceGetter.getConnection();
//            if (connection == null) {
//                throw new IllegalStateException("无法获取数据库连接");
//            }
//
//            statement = connection.createStatement();
//            resultSet = statement.executeQuery(fieldQuerySql);
//            ResultSetMetaData metaData = resultSet.getMetaData();
//
//            if (metaData == null) {
//                return dataFieldVO;
//            }
//
//            int columnCount = metaData.getColumnCount();
//            List<FieldBySchemaApo> dataFieldList = new ArrayList<>();
//            for (int i = 1; i <= columnCount; i++) {
//                // 获取字段基本信息
//                String columnName = metaData.getColumnName(i); // 字段的as的名称
//                String columnLabel = metaData.getColumnLabel(i);
//                String baseColumnName = getBaseColumnName(metaData, i); // 字段的原始名称
//                String columnTypeName = metaData.getColumnTypeName(i);
//                if (dataFieldsApoByTable != null) {
//                    Optional<FieldBySchemaApo> dataField = dataFieldsApoByTable.
//                            getDataField(fieldBySchemaApo -> fieldBySchemaApo.getOriginalColumnName()
//                                    .equals(baseColumnName) ? fieldBySchemaApo : null);
//                    if (dataField.isPresent()) {
//                        FieldBySchemaApo fieldBySchemaApo = dataField.get();
//                        fieldBySchemaApo.setColumnName(columnName);
//                        dataFieldList.add(fieldBySchemaApo);
//                        continue;
//                    }
//                }
//                // 创建并添加字段信息对象
//                FieldBySchemaApo field = new FieldBySchemaApo();
//                field.setColumnName(columnName);
//                field.setOriginalColumnName(baseColumnName);
//                field.setUdtName(columnTypeName);
//                field.setIsNullable(metaData.isNullable(i) == ResultSetMetaData.columnNoNulls ? "NO" : "YES");
//                // 设置字段长度信息（根据字段类型处理）
//                setFieldLengthInfo(metaData, i, field);
//                dataFieldList.add(field);
//
//            }
//            dataFieldVO.setDataFieldList(dataFieldList);
//        } catch (SQLException e) {
//            // 记录详细的错误信息，便于调试
//            log.error("通过SQL查询字段信息失败， 错误: {}", e.getMessage(), e);
//            throw new RuntimeException("获取字段信息失败: " + e.getMessage(), e);
//        } finally {
//            // 确保资源正确关闭
//            dataSourceGetter.closeResources(resultSet, statement, connection);
//        }
//
//        return dataFieldVO;
//    }
//
//    @Override
//    public DataFieldsApo dGetColumnsBySQL(String sqlStatement, SqlParamMap sqlParam) {
//        // 参数校验
//        if (sqlStatement == null || sqlStatement.trim().isEmpty()) {
//            throw new IllegalArgumentException("SQL视图语句不能为空");
//        }
//        sqlStatement = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
//        AdvSqlParser.SqlParseResult parse = AdvSqlParser.parse(sqlStatement);
//        String tableName = parse.getTableName();
//        List<String> fields = parse.getFields();
//        DataFieldsApo dataFieldsApoByTable = null;
//        if (ObjectUtil.isNotEmpty(tableName)) {
//            tableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName, parse.getSchema());
//            dataFieldsApoByTable = dGetColumnsByTable(tableName);
//        }
//
//        DataFieldsApo dataFieldVO = new DataFieldsApo();
//        // 构建查询元数据的SQL，使用LIMIT 0仅获取结构不获取数据
//        String fieldQuerySql = StrUtil.format("SELECT * FROM ({}) AS temp_table LIMIT 0", sqlStatement);
//        log.info("schema:[" + dataSourceGetter.getSchemaName() + "] " + "db:[" + dataSourceGetter.getDataSourceId() + "]" + " SQL的元数据查询：{}", fieldQuerySql);
//        Connection connection = null;
//        PreparedStatement statement = null;
//        ResultSet resultSet = null;
//
//        try {
//            connection = dataSourceGetter.getConnection();
//            if (connection == null) {
//                throw new IllegalStateException("无法获取数据库连接");
//            }
//
//            SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(fieldQuerySql, sqlParam);
//
//            statement = connection.prepareStatement(sqlMeta.getSql());
//            List<Object> jdbcParamValues = sqlMeta.getJdbcParamValues();
//
//            for (int i = 1; i <= jdbcParamValues.size(); i++) {
//                statement.setObject(i, jdbcParamValues.get(i - 1));
//            }
//            resultSet = statement.executeQuery();
//            ResultSetMetaData metaData = resultSet.getMetaData();
//
//            if (metaData == null) {
//                return dataFieldVO;
//            }
//
//            int columnCount = metaData.getColumnCount();
//            List<FieldBySchemaApo> dataFieldList = new ArrayList<>();
//            for (int i = 1; i <= columnCount; i++) {
//                // 获取字段基本信息
//                String columnName = metaData.getColumnName(i); // 字段的as的名称
//                String columnLabel = metaData.getColumnLabel(i);
//                String baseColumnName = getBaseColumnName(metaData, i); // 字段的原始名称
//                String columnTypeName = metaData.getColumnTypeName(i);
//                if (dataFieldsApoByTable != null) {
//                    Optional<FieldBySchemaApo> dataField = dataFieldsApoByTable.
//                            getDataField(fieldBySchemaApo -> fieldBySchemaApo.getOriginalColumnName()
//                                    .equals(baseColumnName) ? fieldBySchemaApo : null);
//                    if (dataField.isPresent()) {
//                        FieldBySchemaApo fieldBySchemaApo = dataField.get();
//                        fieldBySchemaApo.setColumnName(columnName);
//                        dataFieldList.add(fieldBySchemaApo);
//                        continue;
//                    }
//                }
//                // 创建并添加字段信息对象
//                FieldBySchemaApo field = new FieldBySchemaApo();
//                field.setColumnName(columnName);
//                field.setOriginalColumnName(baseColumnName);
//                field.setUdtName(columnTypeName);
//                field.setIsNullable(metaData.isNullable(i) == ResultSetMetaData.columnNoNulls ? "NO" : "YES");
//                // 设置字段长度信息（根据字段类型处理）
//                setFieldLengthInfo(metaData, i, field);
//                dataFieldList.add(field);
//
//            }
//            dataFieldVO.setDataFieldList(dataFieldList);
//        } catch (SQLException e) {
//            // 记录详细的错误信息，便于调试
//            log.error("通过SQL查询字段信息失败， 错误: {}", e.getMessage(), e);
//            throw new RuntimeException("获取字段信息失败: " + e.getMessage(), e);
//        } finally {
//            // 确保资源正确关闭
//            dataSourceGetter.closeResources(resultSet, statement, connection);
//        }
//
//        return dataFieldVO;
//    }
//
//    @Override
//    public DataFieldsApo dGetColumnsBySQLOrTable(String tbNameOrSql) {
//        boolean b = dialectTableNameProcessor.tbTableIsSqlView(tbNameOrSql);
//        DataFieldsApo columnsEntities = null;
//        if (b) {
//            columnsEntities = dGetColumnsBySQL(tbNameOrSql);
//        } else {
//            columnsEntities = dGetColumnsByTable(tbNameOrSql);
//        }
//        return columnsEntities;
//    }
//
//    @Override
//    public void dCreateTable(String tableName, List<FieldBySchemaApo> fields, String primaryKey) {
//        throw new RuntimeException("暂时没有实现");
//    }
//
//    @Override
//    public void dRenameTable(String oldTableName, String newTableName) {
//        if (StrUtil.isEmpty(oldTableName) || StrUtil.isEmpty(newTableName)) {
//            throw new IllegalArgumentException("原表名和新表名都不能为空");
//        }
//        if (dIsTableExists(newTableName)) {
//            throw new RuntimeException(StrUtil.format("新表名[{}]已存在，无法重命名", newTableName));
//        }
//        if (!dIsTableExists(oldTableName)) {
//            throw new RuntimeException(StrUtil.format("原表名[{}]不存在，无法重命名", oldTableName));
//        }
//
//        String oldQualifiedName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, oldTableName);
//        String newQualifiedName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, newTableName);
//        String sql = StrUtil.format("ALTER TABLE {} RENAME TO {}", oldQualifiedName, newQualifiedName);
//        dExecuteDDL(sql, oldTableName, "重命名表");
//    }
//
//    @Override
//    public void dAddColumn(String tableName, FieldBySchemaApo field) {
//        throw new RuntimeException("暂时没有实现");
//    }
//
//    @Override
//    public void dAlterColumn(String tableName, String oldColumnName, FieldBySchemaApo newField) {
//        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(oldColumnName) || newField == null) {
//            throw new IllegalArgumentException("表名、原字段名和新字段信息不能为空");
//        }
//        if (!dIsTableExists(tableName)) {
//            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法修改字段", tableName));
//        }
//
//        // 检查原字段是否存在
//        DataFieldsApo existingFields = dGetColumnsByTable(tableName);
//        boolean oldFieldExists = existingFields.getDataFieldList().stream()
//                .anyMatch(f -> oldColumnName.equals(f.getColumnName()));
//        if (!oldFieldExists) {
//            throw new RuntimeException(StrUtil.format("表[{}]中原字段[{}]不存在，无法修改", tableName, oldColumnName));
//        }
//
//        // 构建修改SQL（支持：重命名、修改类型、修改非空、修改默认值）
//        StringBuilder sqlBuilder = new StringBuilder();
//        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
//
//        // 1. 重命名（如果新字段名与原字段名不同）
//        if (!oldColumnName.equals(newField.getColumnName())) {
//            sqlBuilder.append(StrUtil.format("ALTER TABLE {} RENAME COLUMN {} TO {};",
//                    qualifiedTableName, oldColumnName, newField.getColumnName()));
//        }
//
//        // 2. 修改字段类型、非空、默认值（使用最终的字段名）
//        String finalColumnName = StrUtil.isEmpty(newField.getColumnName()) ? oldColumnName : newField.getColumnName();
//        StringBuilder alterDef = new StringBuilder();
//        alterDef.append(StrUtil.format("ALTER TABLE {} ALTER COLUMN {} TYPE {}",
//                qualifiedTableName, finalColumnName, newField.getUdtName()));
//
//        // 处理类型长度/精度
//        if (StrUtil.isNotEmpty(newField.getCharacterMaximumLength())
//                && (newField.getUdtName().contains("char") || newField.getUdtName().contains("varchar"))) {
//            alterDef.append(StrUtil.format("({})", newField.getCharacterMaximumLength()));
//        } else if (StrUtil.isNotEmpty(newField.getNumericPrecision()) && StrUtil.isNotEmpty(newField.getNumericPrecisionRadix())
//                && (newField.getUdtName().contains("numeric") || newField.getUdtName().contains("decimal"))) {
//            alterDef.append(StrUtil.format("({}, {})", newField.getNumericPrecision(), newField.getNumericPrecisionRadix()));
//        }
//
//        // 处理非空约束
//        if ("NO".equals(newField.getIsNullable())) {
//            alterDef.append(" NOT NULL");
//        } else {
//            alterDef.append(" DROP NOT NULL");
//        }
//
//        // 处理默认值
//        if (StrUtil.isNotEmpty(newField.getColumnDefault())) {
//            alterDef.append(" SET DEFAULT ").append(newField.getColumnDefault());
//        } else {
//            alterDef.append(" DROP DEFAULT");
//        }
//
//        sqlBuilder.append(alterDef).append(";");
//        dExecuteDDL(sqlBuilder.toString(), tableName, "修改字段[" + oldColumnName + "→" + finalColumnName + "]");
//    }
//
//    @Override
//    public void dDropColumn(String tableName, String columnName) {
//        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(columnName)) {
//            throw new IllegalArgumentException("表名和字段名都不能为空");
//        }
//        if (!dIsTableExists(tableName)) {
//            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法删除字段", tableName));
//        }
//
//        // 检查字段是否存在
//        DataFieldsApo existingFields = dGetColumnsByTable(tableName);
//        boolean fieldExists = existingFields.getDataFieldList().stream()
//                .anyMatch(f -> columnName.equals(f.getColumnName()));
//        if (!fieldExists) {
//            log.warn("表[{}]中字段[{}]不存在，无需删除", tableName, columnName);
//            return;
//        }
//
//        // 检查字段是否为主键（主键字段需先删除主键约束）
//        List<String> primaryKeys = dGetPrimaryKeys(tableName);
//        if (primaryKeys.contains(columnName)) {
//            throw new RuntimeException(StrUtil.format("字段[{}]是主键，需先删除主键约束才能删除字段", columnName));
//        }
//
//        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
//        String sql = StrUtil.format("ALTER TABLE {} DROP COLUMN IF EXISTS {} CASCADE",
//                qualifiedTableName, columnName);
//        dExecuteDDL(sql, tableName, "删除字段[" + columnName + "]");
//    }
//
//    @Override
//    public List<String> dGetTablesBySchema(String schemaName) {
//        String actualSchema = ObjectUtil.isEmpty(schemaName) ? dataSourceGetter.getSchemaName() : schemaName;
//        String sql;
//
//        if (StrUtil.isEmpty(actualSchema)) {
//            sql = "SELECT table_name FROM information_schema.tables " +
//                    "WHERE table_type = 'BASE TABLE' AND table_schema NOT IN ('information_schema', 'pg_catalog') " +
//                    "ORDER BY table_name";
//        } else {
//            sql = StrUtil.format("SELECT table_name FROM information_schema.tables " +
//                    "WHERE table_type = 'BASE TABLE' AND table_schema = '{}' " +
//                    "ORDER BY table_name", actualSchema);
//        }
//
//        List<GirAdvOneRow> rows = baseOpt.bSelectList(sql);
//        List<String> tables = new ArrayList<>();
//        if (ObjectUtil.isNotEmpty(rows)) {
//            rows.forEach(row -> tables.add(row.getStr("table_name")));
//        }
//        return tables;
//    }
//
//    @Override
//    public boolean dIsTableExists(String tableName) {
//        if (StrUtil.isEmpty(tableName)) {
//            return false;
//        }
//        if (dialectTableNameProcessor.tbTableIsSqlView(tableName)) {
//            return false;
//        }
//
//
//        String nameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
//        String schemaName1 = dialectTableNameProcessor.tbExtractSchemaName(tableName);
//        ;
//        schemaName1 = schemaName1 == null ? dataSourceGetter.getSchemaName() : schemaName1;
//        String sql = "";
//        String sqlTemp = "SELECT COUNT(*) AS cnt FROM information_schema.tables " +
//                "WHERE table_name = '{}' AND table_type = 'BASE TABLE'";
//        sql = StrUtil.format(sqlTemp, nameNotSchema);
//
//        if (StrUtil.isNotEmpty(schemaName1)) {
//            String schemaTemp = " AND table_schema = '{}'";
//            sql += StrUtil.format(schemaTemp, schemaName1);
//        }
//        GirAdvOneRow row = baseOpt.bSelectOne(sql);
//        return row != null && row.getInt("cnt") > 0;
//    }
//
//    @Override
//    public boolean dIsFunctionExists(String functionName) {
//        if (StrUtil.isEmpty(functionName)) {
//            return false;
//        }
//
//        // 处理带schema的函数名（格式：schema.函数名）
//
//        String nameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(functionName);
//        String schemaName1 = dialectTableNameProcessor.tbExtractSchemaName(functionName);
//        schemaName1 = schemaName1 == null ? dataSourceGetter.getSchemaName() : schemaName1;
//
//        // 构建查询SQL（查询information_schema.routines判断函数是否存在）
//        String sqlTemp = "SELECT COUNT(*) AS cnt FROM information_schema.routines " +
//                "WHERE routine_name = '{}' AND routine_type = 'FUNCTION'";
//        String sql = StrUtil.format(sqlTemp, nameNotSchema);
//
//        // 增加schema过滤条件（如果指定了schema）
//        if (StrUtil.isNotEmpty(schemaName1)) {
//            String schemaTemp = " AND specific_schema = '{}'";
//            sql += StrUtil.format(schemaTemp, schemaName1);
//        }
//
//        // 执行查询并判断结果
//        GirAdvOneRow row = baseOpt.bSelectOne(sql);
//        return row != null && row.getInt("cnt") > 0;
//    }
//
//    @Override
//    public void dCreateSchema(String schemaName) {
//        if (StrUtil.isEmpty(schemaName)) {
//            throw new IllegalArgumentException("模式名不能为空");
//        }
//
//        // 检查模式是否已存在
//        String checkSql = StrUtil.format("SELECT schema_name FROM information_schema.schemata WHERE schema_name = '{}'", schemaName);
//        List<GirAdvOneRow> rows = baseOpt.bSelectList(checkSql);
//        if (ObjectUtil.isNotEmpty(rows)) {
//            log.warn("模式[{}]已存在，无需重复创建", schemaName);
//            return;
//        }
//
//        String sql = StrUtil.format("CREATE SCHEMA {}", schemaName);
//        dExecuteDDL(sql, schemaName, "创建模式");
//    }
//
//    @Override
//    public void dDropSchema(String schemaName, boolean cascade) {
//        if (StrUtil.isEmpty(schemaName)) {
//            throw new IllegalArgumentException("模式名不能为空");
//        }
//
//        // 检查模式是否存在
//        String checkSql = StrUtil.format("SELECT schema_name FROM information_schema.schemata WHERE schema_name = '{}'", schemaName);
//        List<GirAdvOneRow> rows = baseOpt.bSelectList(checkSql);
//        if (ObjectUtil.isEmpty(rows)) {
//            log.warn("模式[{}]不存在，无需删除", schemaName);
//            return;
//        }
//
//        String sql = StrUtil.format("DROP SCHEMA {} {}", schemaName, cascade ? "CASCADE" : "RESTRICT");
//        dExecuteDDL(sql, schemaName, "删除模式");
//    }
//
//    @Override
//    public void dAddPrimaryKey(String tableName, List<String> columnNames, String constraintName) {
//        if (StrUtil.isEmpty(tableName) || ObjectUtil.isEmpty(columnNames)) {
//            throw new IllegalArgumentException("表名和列名列表不能为空");
//        }
//        if (!dIsTableExists(tableName)) {
//            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法添加主键", tableName));
//        }
//
//        // 生成约束名（如果未指定）
//        String pkConstraintName = StrUtil.isEmpty(constraintName)
//                ? StrUtil.format("pk_{}_{}", tableName, System.currentTimeMillis())
//                : constraintName;
//
//        // 检查是否已存在主键
//        List<String> existingPk = dGetPrimaryKeys(tableName);
//        if (ObjectUtil.isNotEmpty(existingPk)) {
//            throw new RuntimeException(StrUtil.format("表[{}]已存在主键，无法重复添加", tableName));
//        }
//
//        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
//        String columns = String.join(", ", columnNames);
//        String sql = StrUtil.format("ALTER TABLE {} ADD CONSTRAINT {} PRIMARY KEY ({})",
//                qualifiedTableName, pkConstraintName, columns);
//
//        dExecuteDDL(sql, tableName, "添加主键约束[" + pkConstraintName + "]");
//    }
//
//    @Override
//    public void dDropPrimaryKey(String tableName, String constraintName) {
//        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(constraintName)) {
//            throw new IllegalArgumentException("表名和约束名都不能为空");
//        }
//        if (!dIsTableExists(tableName)) {
//            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法删除主键", tableName));
//        }
//
//        // 检查约束是否存在
//        String checkSql = StrUtil.format(
//                "SELECT constraint_name FROM information_schema.table_constraints " +
//                        "WHERE table_name = '{}' AND constraint_type = 'PRIMARY KEY' AND constraint_name = '{}'",
//                tableName, constraintName);
//
//        if (ObjectUtil.isEmpty(baseOpt.bSelectList(checkSql))) {
//            log.warn("表[{}]中主键约束[{}]不存在，无需删除", tableName, constraintName);
//            return;
//        }
//
//        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
//        String sql = StrUtil.format("ALTER TABLE {} DROP CONSTRAINT {}", qualifiedTableName, constraintName);
//        dExecuteDDL(sql, tableName, "删除主键约束[" + constraintName + "]");
//    }
//
//    @Override
//    public void dCreateIndex(String tableName, String indexName, List<String> columnNames, boolean isUnique) {
//        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(indexName) || ObjectUtil.isEmpty(columnNames)) {
//            throw new IllegalArgumentException("表名、索引名和列名列表不能为空");
//        }
//        if (!dIsTableExists(tableName)) {
//            throw new RuntimeException(StrUtil.format("表[{}]不存在，无法创建索引", tableName));
//        }
//
//        // 检查索引是否已存在
//
//        if (dIndexesExists(tableName, indexName)) {
//            throw new RuntimeException(StrUtil.format("表[{}]中索引[{}]已存在，无法重复创建", tableName, indexName));
//        }
//
//        String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
//        String columns = String.join(", ", columnNames);
//        String sql = StrUtil.format("CREATE {} INDEX {} ON {} ({})",
//                isUnique ? "UNIQUE" : "", indexName, qualifiedTableName, columns);
//
//        dExecuteDDL(sql, tableName, "创建" + (isUnique ? "唯一" : "") + "索引[" + indexName + "]");
//    }
//
//    @Override
//    public void dDropIndex(String tableName, String indexName) {
//        if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(indexName)) {
//            throw new IllegalArgumentException("表名和索引名都不能为空");
//        }
//
//        // 检查索引是否存在
//
//        if (!dIndexesExists(tableName, indexName)) {
//            log.warn("表[{}]中索引[{}]不存在，无需删除", tableName, indexName);
//            return;
//        }
//
//        String sql = StrUtil.format("DROP INDEX IF EXISTS {}.{}",
//                dialectTableNameProcessor.tbGetSchemaNameForSql(dataSourceGetter), indexName);
//        dExecuteDDL(sql, tableName, "删除索引[" + indexName + "]");
//    }
//
//    @Override
//    public List<String> dGetPrimaryKeys(String tableName) {
//        if (StrUtil.isEmpty(tableName) || !dIsTableExists(tableName)) {
//            return new ArrayList<>();
//        }
//        tableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
//        String sql = "SELECT kcu.column_name " +
//                "FROM information_schema.table_constraints tco " +
//                "JOIN information_schema.key_column_usage kcu " +
//                "ON tco.constraint_name = kcu.constraint_name " +
//                "WHERE tco.table_name ='{}' AND tco.constraint_type = 'PRIMARY KEY' " +
//                "ORDER BY kcu.ordinal_position";
//        sql = StrUtil.format(sql, tableName);
//
//
//        if (StrUtil.isNotEmpty(dataSourceGetter.getSchemaName())) {
//            sql += StrUtil.format(" AND tco.table_schema = '{}'", dataSourceGetter.getSchemaName());
//
//        }
//        List<GirAdvOneRow> rows = baseOpt.bSelectList(sql);
//        List<String> primaryKeys = new ArrayList<>();
//        if (ObjectUtil.isNotEmpty(rows)) {
//            rows.forEach(row -> primaryKeys.add(row.getStr("column_name")));
//        }
//        return primaryKeys;
//    }
//
//    @Override
//    public List<IndexApo> dGetIndexes(String tableName) {
//
//        if (StrUtil.isEmpty(tableName) || !dIsTableExists(tableName)) {
//            return ListUtil.empty();
//        }
//        String sql = StrUtil.format(
//                " SELECT * FROM pg_indexes WHERE tablename = '{}' {}",
//                tableName,
//                StrUtil.isEmpty(dataSourceGetter.getSchemaName()) ? "" : StrUtil.format("AND schemaname = '{}'", dataSourceGetter.getSchemaName())
//        );
//        return baseOpt.bSelectObjList(sql, IndexApo.class);
//    }
//
//    @Override
//    public boolean dIndexesExists(String tableName, String indexName) {
//        List<IndexApo> indexApos = dGetIndexes(tableName);
//        for (IndexApo indexApo : indexApos) {
//            if (indexApo.getIndexname().equals(indexName)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    @Override
//    public String dGetTableSizeFormat(String tableName) {
//        Long l = dGetTableSize(tableName);
//        if (l == null) {
//            return null;
//        }
//        return DataSizeUtil.format(l);
//    }
//
//
//    @Override
//    public Long dGetTableSize(String tableName) {
//        if (StrUtil.isEmpty(tableName)) {
//            return null;
//        }
//        String schemaTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
//        String sql = StrUtil.format(
//                "SELECT  pg_total_relation_size('{}')  AS table_size;",
//                schemaTableName
//        );
//        GirAdvOneRow girAdvOneRow = baseOpt.bSelectOne(sql);
//        return girAdvOneRow.getLong("table_size");
//    }
//
//    /**
//     * 获取基础列名
//     */
//    private String getBaseColumnName(ResultSetMetaData metaData, int columnIndex) throws SQLException {
//        if (metaData instanceof PgResultSetMetaData) {
//            return ((PgResultSetMetaData) metaData).getBaseColumnName(columnIndex);
//        }
//        return metaData.getColumnName(columnIndex);
//    }
//
//    /**
//     * 设置字段长度相关信息
//     */
//    private void setFieldLengthInfo(ResultSetMetaData metaData, int columnIndex, FieldBySchemaApo field) throws SQLException {
//        String columnTypeName = field.getUdtName();
//        if (columnTypeName == null) {
//            return;
//        }
//
//        // 处理字符类型长度
//        if (columnTypeName.contains("char") || columnTypeName.contains("varchar")) {
//            field.setCharacterMaximumLength(String.valueOf(metaData.getColumnDisplaySize(columnIndex)));
//        }
//        // 处理数值类型精度和标度
//        else if (columnTypeName.contains("int") || columnTypeName.contains("numeric") ||
//                columnTypeName.contains("decimal") || columnTypeName.contains("float")) {
//            field.setNumericPrecision(String.valueOf(metaData.getPrecision(columnIndex)));
//            field.setNumericPrecisionRadix(String.valueOf(metaData.getScale(columnIndex)));
//        }
//    }
//
//
//    /**
//     * 执行DDL语句的通用方法
//     */
//    public int dExecuteDDL(String sql, String tableName, String operation) {
//        Connection connection = null;
//        Statement statement = null;
//        int i = 0;
//        try {
//            connection = dataSourceGetter.getConnection();
//            if (connection == null) {
//                throw new IllegalStateException("无法获取数据库连接");
//            }
//
//            // 关闭自动提交，执行后手动提交
//            connection.setAutoCommit(false);
//            statement = connection.createStatement();
//            log.info("executeDDL执行的sql为：{}", sql);
//            i = statement.executeUpdate(sql);
//            connection.commit();
//
//            log.info("{}成功，表名: {}", operation, tableName);
//        } catch (SQLException e) {
//            // 发生异常时回滚
//            try {
//                connection.rollback();
//            } catch (SQLException ex) {
//                log.warn("{}失败，回滚操作出错，表名: {}", operation, tableName, ex);
//            }
//
//            log.error("{}失败，表名: {}, SQL: {}, 错误: {}", operation, tableName, sql, e.getMessage(), e);
//            throw new RuntimeException(StrUtil.format("{}失败: {}", operation, e.getMessage()), e);
//        } finally {
//            // 恢复自动提交模式
//            if (connection != null) {
//                try {
//                    connection.setAutoCommit(true);
//                } catch (SQLException e) {
//                    log.warn("恢复自动提交模式失败", e);
//                }
//            }
//            dataSourceGetter.closeResources(null, statement, connection);
//            return i;
//        }
//    }
//
//    @Override
//    public int dExecuteDDL(String sqlStatement, SqlParamMap sqlParam, String tableName, String operation) {
//        // 解析SQL（支持MyBatis标签）
//        String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlStatement);
//        SqlMeta sqlMeta = SqlEngineUtil.getEngine().parse(cleanSql, sqlParam);
//        String execSql = sqlMeta.getSql();
//        List<Object> jdbcParams = sqlMeta.getJdbcParamValues();
//        Connection connection = null;
//        PreparedStatement statement = null;
//        int count = 0;
//        try {
//            connection = dataSourceGetter.getConnection();
//            if (connection == null) {
//                throw new IllegalStateException("无法获取数据库连接");
//            }
//
//            // 关闭自动提交，执行后手动提交
//            connection.setAutoCommit(false);
//            statement = connection.prepareStatement(execSql);
//            log.info("executeDDL执行的sql为：{}", execSql);
//            for (int i = 1; i <= jdbcParams.size(); i++) {
//                statement.setObject(i, jdbcParams.get(i - 1));
//            }
//            count = statement.executeUpdate();
//            connection.commit();
//
//            log.info("{}成功，表名: {}", operation, tableName);
//        } catch (SQLException e) {
//            // 发生异常时回滚
//            try {
//                connection.rollback();
//            } catch (SQLException ex) {
//                log.warn("{}失败，回滚操作出错，表名: {}", operation, tableName, ex);
//            }
//
//            log.error("{}失败，表名: {}, SQL: {}, 错误: {}", operation, tableName, execSql, e.getMessage(), e);
//            throw new RuntimeException(StrUtil.format("{}失败: {}", operation, e.getMessage()), e);
//        } finally {
//            // 恢复自动提交模式
//            if (connection != null) {
//                try {
//                    connection.setAutoCommit(true);
//                } catch (SQLException e) {
//                    log.warn("恢复自动提交模式失败", e);
//                }
//            }
//            dataSourceGetter.closeResources(null, statement, connection);
//            return count;
//        }
//
//    }
//
//
//}
