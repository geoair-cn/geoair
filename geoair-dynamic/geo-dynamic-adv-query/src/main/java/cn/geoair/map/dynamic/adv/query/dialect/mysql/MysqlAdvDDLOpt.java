package cn.geoair.map.dynamic.adv.query.dialect.mysql;

import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.apo.IndexApo;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.ds.IDataSourceGetter;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL DDL操作实现类 仅实现MySQL专属的差异化逻辑，复用抽象父类的所有通用DDL逻辑
 */
public class MysqlAdvDDLOpt extends AbstractAdvDDLOpt {

	IAdvBaseOpt baseOpt;

	public MysqlAdvDDLOpt(IDataSourceGetter dataSourceGetter) {
		super(dataSourceGetter);
		baseOpt = new MysqlAdvBaseOpt(dataSourceGetter);
	}

	@Override
	public IAdvBaseOpt getAdvBaseOpt() {
		return baseOpt;
	}

	@Override
	public DialectTableNameProcessor getDialectTableNameProcessor() {
		return MysqlDialectTableNameUtil.getInstance();
	}

	// ========== 表操作差异化实现 ==========
	@Override
	public String buildTruncateTableSql(String qualifiedTableName) {
		// MySQL专属：TRUNCATE语法（无RESTART IDENTITY，自动重置自增）
		return StrUtil.format("TRUNCATE TABLE {}", qualifiedTableName);
	}

	@Override
	public String buildDropTableSql(String qualifiedTableName) {
		return StrUtil.format("DROP TABLE IF EXISTS {}", qualifiedTableName);
	}

	@Override
	public String buildRenameTableSql(String oldQualifiedName, String newQualifiedName) {
		// MySQL专属：RENAME TABLE语法
		return StrUtil.format("RENAME TABLE {} TO {}", oldQualifiedName, newQualifiedName);
	}

	@Override
	public boolean dIsTableExists(String tableName) {
		if (StrUtil.isEmpty(tableName)) {
			return false;
		}
		if (dialectTableNameProcessor.tbTableIsSqlView(tableName)) {
			return false;
		}

		String nameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
		String schemaName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
		schemaName = schemaName == null ? dataSourceGetter.getSchemaName() : schemaName;

		// MySQL专属：表存在性检查（INFORMATION_SCHEMA.TABLES）
		String sql = StrUtil.format("SELECT COUNT(*) AS cnt FROM information_schema.tables "
				+ "WHERE table_name = '{}' AND table_type = 'BASE TABLE'", nameNotSchema);
		if (StrUtil.isNotEmpty(schemaName)) {
			sql += StrUtil.format(" AND table_schema = '{}'", schemaName);
		}

		GirAdvOneRow row = baseOpt.bSelectOne(sql);
		return row != null && row.getInt("cnt") > 0;
	}

	// ========== 字段操作差异化实现 ==========
	@Override
	public DataFieldsApo dGetColumnsByTable(String tableName) {
		if (StrUtil.isEmpty(tableName)) {
			return null;
		}
		String schemaNameBySQL = dialectTableNameProcessor.tbExtractSchemaName(tableName);
		String schemaName = ObjectUtil.isNotEmpty(schemaNameBySQL) ? schemaNameBySQL : dataSourceGetter.getSchemaName();
		String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);

		// MySQL专属：字段元数据查询（INFORMATION_SCHEMA.COLUMNS）
		String sql = StrUtil.format("SELECT " + "c.*, " + "COLUMN_COMMENT AS column_comment, "
				+ "CASE WHEN kcu.column_name IS NOT NULL THEN 't' ELSE 'f' END AS primary_key_is "
				+ "FROM information_schema.columns c " + "LEFT JOIN information_schema.key_column_usage kcu "
				+ "ON c.table_schema = kcu.table_schema " + "AND c.table_name = kcu.table_name "
				+ "AND c.column_name = kcu.column_name " + "AND kcu.constraint_name = 'PRIMARY' "
				+ "WHERE c.table_name = '{}'", notSchemaTableName);
		if (StrUtil.isNotEmpty(schemaName)) {
			sql += StrUtil.format(" AND c.table_schema = '{}'", schemaName);
		}

		List<FieldBySchemaApo> fields = baseOpt.bSelectObjList(sql, FieldBySchemaApo.class);
		fields.forEach(f -> f.setOriginalColumnName(f.getColumnName()));

		DataFieldsApo dataFieldsApo = new DataFieldsApo();
		dataFieldsApo.setDataFieldList(fields);
		return dataFieldsApo;
	}

	@Override
	public String buildAlterColumnSql(String qualifiedTableName, String oldColumnName, FieldBySchemaApo newField) {
		StringBuilder sqlBuilder = new StringBuilder();
		String finalColumnName = StrUtil.isEmpty(newField.getColumnName()) ? oldColumnName : newField.getColumnName();

		// MySQL专属：修改字段语法（ALTER COLUMN → MODIFY COLUMN）
		StringBuilder alterDef = new StringBuilder();
		alterDef.append(StrUtil.format("ALTER TABLE {} MODIFY COLUMN {} {}", qualifiedTableName, finalColumnName,
				newField.getUdtName()));

		// 处理长度/精度
		if (StrUtil.isNotEmpty(newField.getCharacterMaximumLength())
				&& (newField.getUdtName().contains("char") || newField.getUdtName().contains("varchar"))) {
			alterDef.append(StrUtil.format("({})", newField.getCharacterMaximumLength()));
		}
		else if (StrUtil.isNotEmpty(newField.getNumericPrecision())
				&& StrUtil.isNotEmpty(newField.getNumericPrecisionRadix())
				&& (newField.getUdtName().contains("numeric") || newField.getUdtName().contains("decimal"))) {
			alterDef.append(
					StrUtil.format("({}, {})", newField.getNumericPrecision(), newField.getNumericPrecisionRadix()));
		}

		// 处理非空
		if ("NO".equals(newField.getIsNullable())) {
			alterDef.append(" NOT NULL");
		}
		else {
			alterDef.append(" NULL");
		}

		// 处理默认值
		if (StrUtil.isNotEmpty(newField.getColumnDefault())) {
			alterDef.append(" DEFAULT ").append(newField.getColumnDefault());
		}
		else {
			alterDef.append(" DEFAULT NULL");
		}

		// MySQL专属：重名字段（单独语句）
		if (!oldColumnName.equals(finalColumnName)) {
			sqlBuilder.append(StrUtil.format("ALTER TABLE {} RENAME COLUMN {} TO {};", qualifiedTableName,
					oldColumnName, finalColumnName));
		}

		sqlBuilder.append(alterDef).append(";");
		return sqlBuilder.toString();
	}

	@Override
	public String buildDropColumnSql(String qualifiedTableName, String columnName) {
		return StrUtil.format("ALTER TABLE {} DROP COLUMN IF EXISTS {}", qualifiedTableName, columnName);
	}

	// ========== 主键/索引差异化实现 ==========
	@Override
	public List<String> dGetPrimaryKeys(String tableName) {
		if (StrUtil.isEmpty(tableName) || !dIsTableExists(tableName)) {
			return new ArrayList<>();
		}
		String schemaName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
		schemaName = schemaName == null ? dataSourceGetter.getSchemaName() : schemaName;
		String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);

		// MySQL专属：主键查询
		String sql = StrUtil.format(
				"SELECT COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE "
						+ "WHERE TABLE_SCHEMA = '{}' AND TABLE_NAME = '{}' AND CONSTRAINT_NAME = 'PRIMARY'",
				schemaName, notSchemaTableName);

		List<GirAdvOneRow> rows = baseOpt.bSelectList(sql);
		List<String> pks = new ArrayList<>();
		rows.forEach(row -> pks.add(row.getStr("COLUMN_NAME")));
		return pks;
	}

	@Override
	public boolean checkConstraintExists(String tableName, String constraintName, String constraintType) {
		String schemaName = dataSourceGetter.getSchemaName();
		String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);

		// MySQL专属：约束存在性检查
		String sql = StrUtil.format("SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS "
				+ "WHERE TABLE_SCHEMA = '{}' AND TABLE_NAME = '{}' AND CONSTRAINT_TYPE = '{}' AND CONSTRAINT_NAME = '{}'",
				schemaName, notSchemaTableName, constraintType, constraintName);
		return ObjectUtil.isNotEmpty(baseOpt.bSelectList(sql));
	}

	@Override
	public String buildAddPrimaryKeySql(String qualifiedTableName, String constraintName, String columns) {
		// MySQL专属：添加主键（约束名可选）
		return StrUtil.format("ALTER TABLE {} ADD CONSTRAINT {} PRIMARY KEY ({})", qualifiedTableName, constraintName,
				columns);
	}

	@Override
	public String buildDropPrimaryKeySql(String qualifiedTableName, String constraintName) {
		// MySQL专属：删除主键（直接DROP PRIMARY KEY）
		return StrUtil.format("ALTER TABLE {} DROP PRIMARY KEY", qualifiedTableName);
	}

	@Override
	public String buildCreateIndexSql(String qualifiedTableName, String indexName, String columns, boolean isUnique) {
		return StrUtil.format("CREATE {} INDEX {} ON {} ({})", isUnique ? "UNIQUE" : "", indexName, qualifiedTableName,
				columns);
	}

	@Override
	public String buildDropIndexSql(String tableName, String indexName) {
		String qualifiedTableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
		// MySQL专属：删除索引（ALTER TABLE DROP INDEX）
		return StrUtil.format("ALTER TABLE {} DROP INDEX {}", qualifiedTableName, indexName);
	}

	@Override
	public List<IndexApo> dGetIndexes(String tableName) {
		if (StrUtil.isEmpty(tableName) || !dIsTableExists(tableName)) {
			return ListUtil.empty();
		}
		String schemaName = dataSourceGetter.getSchemaName();
		String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);

		// MySQL专属：索引查询（SHOW INDEX）
		String sql = StrUtil.format("SHOW INDEX FROM {} WHERE TABLE_SCHEMA = '{}'", notSchemaTableName, schemaName);
		return baseOpt.bSelectObjList(sql, IndexApo.class);
	}

	@Override
	public boolean dIndexesExists(String tableName, String indexName) {
		List<IndexApo> indexes = dGetIndexes(tableName);
		return indexes.stream().anyMatch(idx -> idx.getIndexname().equals(indexName));
	}

	// ========== Schema/模式差异化实现 ==========
	@Override
	public List<String> dGetAllSchemas() {
		// MySQL：Schema = Database
		String sql = "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys') ORDER BY SCHEMA_NAME";
		List<GirAdvOneRow> rows = baseOpt.bSelectList(sql);
		List<String> schemas = new ArrayList<>();
		rows.forEach(row -> schemas.add(row.getStr("SCHEMA_NAME")));
		return schemas;
	}

	@Override
	public List<String> dGetTablesBySchema(String schemaName) {
		String actualSchema = ObjectUtil.isEmpty(schemaName) ? dataSourceGetter.getSchemaName() : schemaName;
		String sql = StrUtil.format("SELECT table_name FROM information_schema.tables "
				+ "WHERE table_type = 'BASE TABLE' AND table_schema = '{}' ORDER BY table_name", actualSchema);

		List<GirAdvOneRow> rows = baseOpt.bSelectList(sql);
		List<String> tables = new ArrayList<>();
		rows.forEach(row -> tables.add(row.getStr("table_name")));
		return tables;
	}

	@Override
	public boolean checkSchemaExists(String schemaName) {
		// MySQL：Schema = Database
		String sql = StrUtil.format("SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '{}'",
				schemaName);
		return ObjectUtil.isNotEmpty(baseOpt.bSelectList(sql));
	}

	@Override
	public String buildCreateSchemaSql(String schemaName) {
		// MySQL：CREATE SCHEMA = CREATE DATABASE
		return StrUtil.format("CREATE DATABASE IF NOT EXISTS {}", schemaName);
	}

	@Override
	public String buildDropSchemaSql(String schemaName, boolean cascade) {
		// MySQL：DROP SCHEMA = DROP DATABASE
		return StrUtil.format("DROP DATABASE IF EXISTS {}", schemaName);
	}

	// ========== 表大小差异化实现 ==========
	@Override
	public Long dGetTableSize(String tableName) {
		if (StrUtil.isEmpty(tableName)) {
			return null;
		}
		String schemaName = dataSourceGetter.getSchemaName();
		String notSchemaTableName = dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);

		// MySQL专属：表大小查询（DATA_LENGTH + INDEX_LENGTH）
		String sql = StrUtil.format(
				"SELECT (DATA_LENGTH + INDEX_LENGTH) AS table_size "
						+ "FROM information_schema.TABLES WHERE TABLE_SCHEMA = '{}' AND TABLE_NAME = '{}'",
				schemaName, notSchemaTableName);
		GirAdvOneRow row = baseOpt.bSelectOne(sql);
		return row.getLong("table_size");
	}

	// ========== 元数据差异化实现 ==========
	@Override
	public String buildMetadataQuerySql(String sqlView) {
		// MySQL专属：LIMIT 0获取元数据
		return StrUtil.format("SELECT * FROM ({}) AS temp_table LIMIT 0", sqlView);
	}

	@Override
	public String getBaseColumnName(ResultSetMetaData metaData, int columnIndex) throws SQLException {
		// MySQL：直接返回列名
		return metaData.getColumnName(columnIndex);
	}

	@Override
	public String getColumnTypeName(ResultSetMetaData metaData, int columnIndex) throws SQLException {
		// MySQL：返回列类型名
		return metaData.getColumnTypeName(columnIndex);
	}

	@Override
	public void setFieldLengthInfo(ResultSetMetaData metaData, int columnIndex, FieldBySchemaApo field)
			throws SQLException {
		String columnTypeName = field.getUdtName();
		if (columnTypeName == null) {
			return;
		}

		// MySQL专属：字段长度处理
		if (columnTypeName.contains("char") || columnTypeName.contains("varchar") || columnTypeName.contains("text")) {
			field.setCharacterMaximumLength(String.valueOf(metaData.getColumnDisplaySize(columnIndex)));
		}
		else if (columnTypeName.contains("int") || columnTypeName.contains("decimal")
				|| columnTypeName.contains("float") || columnTypeName.contains("double")) {
			field.setNumericPrecision(String.valueOf(metaData.getPrecision(columnIndex)));
			field.setNumericPrecisionRadix(String.valueOf(metaData.getScale(columnIndex)));
		}
	}

	@Override
	public boolean dIsFunctionExists(String functionName) {
		if (StrUtil.isEmpty(functionName)) {
			return false;
		}

		String nameNotSchema = dialectTableNameProcessor.tbGetTableNameNotSchema(functionName);
		String schemaName = dialectTableNameProcessor.tbExtractSchemaName(functionName);
		schemaName = schemaName == null ? dataSourceGetter.getSchemaName() : schemaName;

		// MySQL专属：函数存在性检查
		String sql = StrUtil.format("SELECT COUNT(*) AS cnt FROM information_schema.ROUTINES "
				+ "WHERE ROUTINE_NAME = '{}' AND ROUTINE_TYPE = 'FUNCTION'", nameNotSchema);
		if (StrUtil.isNotEmpty(schemaName)) {
			sql += StrUtil.format(" AND ROUTINE_SCHEMA = '{}'", schemaName);
		}

		GirAdvOneRow row = baseOpt.bSelectOne(sql);
		return row != null && row.getInt("cnt") > 0;
	}

}
