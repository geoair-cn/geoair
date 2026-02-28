// package cn.geoair.map.dynamic.adv.query.dialect.pgback;
//
// import cn.geoair.gtc.base.log.GiLogger;
// import cn.geoair.gtc.base.log.GirLogger;
// import cn.geoair.map.dynamic.adv.query.IAdvGeoOpt;
// import cn.geoair.map.dynamic.adv.query.apo.BBoxApo;
// import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
// import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
// import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
// import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
// import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
// import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
// import cn.geoair.map.dynamic.ds.IDataSourceGetter;
// import cn.hutool.core.collection.CollectionUtil;
// import cn.hutool.core.collection.ListUtil;
// import cn.hutool.core.map.MapUtil;
// import cn.hutool.core.util.IdUtil;
// import cn.hutool.core.util.ObjectUtil;
// import cn.hutool.core.util.StrUtil;
//
//
// import java.sql.*;
// import java.util.*;
//
/// **
// * @author ：张逢吉
// * @date ：Created in 2025/10/9 10:16
// * @description： 基于postgresql的基础DDl实现
// */
//
//
// public class PgAdvGeoOpt implements IAdvGeoOpt {
// private static final GiLogger log = GirLogger.getLoger();
// private boolean _POSTGIS_IS;
// private String _POSTGIS_VERSION;
// DialectTableNameProcessor dialectTableNameProcessor =
// PgDialectTableNameUtil.getInstance();
//
// PgAdvBaseOpt baseOpt;
// PgAdvDDLOpt pgAdvDDLOpt;
//
// final IDataSourceGetter dataSourceGetter;
//
// public PgAdvGeoOpt(IDataSourceGetter dataSourceGetter) {
// this.dataSourceGetter = dataSourceGetter;
// baseOpt = new PgAdvBaseOpt(dataSourceGetter);
// pgAdvDDLOpt = new PgAdvDDLOpt(dataSourceGetter);
// }
//
// protected void getPostGisVersion() {
// try {
// GirAdvOneRow girAdvOneRow = baseOpt.bSelectOne("SELECT public.postgis_version();");
// _POSTGIS_VERSION = girAdvOneRow.getStr("postgis_version");
// _POSTGIS_IS = true;
// log.info("当前数据源的POSTGIS版本信息：{}", _POSTGIS_VERSION);
// } catch (Exception e) {
// log.info("当前数据源为非POSTGIS数据源", e);
// _POSTGIS_VERSION = "";
// _POSTGIS_IS = false;
// }
// }
//
// @Override
// public GirAdvOneRow eSelectOne(String sql, AdvEnumsGeomOpt advEnumsGeomOpt) {
// List<String> geomFieldNameBySql = eGetGeomColumnNameListBySql(sql);
// return eSelectOne(sql, advEnumsGeomOpt, geomFieldNameBySql);
// }
//
// @Override
// public GirAdvOneRow eSelectOne(String sql, AdvEnumsGeomOpt advEnumsGeomOpt,
// List<String> geomFieldNameList) {
// GirAdvOneRow girAdvOneRow = baseOpt.bSelectOne(sql);
// if (ObjectUtil.isNotNull(geomFieldNameList)) {
// List<GirAdvOneRow> girAdvOneRows = ListUtil.of(girAdvOneRow);
// processGeometryField(girAdvOneRows, advEnumsGeomOpt, geomFieldNameList);
// girAdvOneRow = girAdvOneRows.get(0);
// }
// return girAdvOneRow;
// }
//
// @Override
// public GirAdvOneRow eSelectOne(String sql, AdvEnumsGeomOpt advEnumsGeomOpt, String
// geomFieldName) {
// return eSelectOne(sql, advEnumsGeomOpt, ListUtil.of(geomFieldName));
// }
//
// @Override
// public List<GirAdvOneRow> eSelectList(String sql, AdvEnumsGeomOpt advEnumsGeomOpt) {
// List<String> geomFieldNameBySql = eGetGeomColumnNameListBySql(sql);
// return eSelectList(sql, advEnumsGeomOpt, geomFieldNameBySql);
// }
//
// @Override
// public List<GirAdvOneRow> eSelectList(String sql, AdvEnumsGeomOpt advEnumsGeomOpt,
// List<String> geomFieldNameList) {
// List<GirAdvOneRow> girAdvOneRows = baseOpt.bSelectList(sql);
// if (ObjectUtil.isNotNull(geomFieldNameList)) {
// processGeometryField(girAdvOneRows, advEnumsGeomOpt, geomFieldNameList);
// }
// return girAdvOneRows;
// }
//
// @Override
// public List<GirAdvOneRow> eSelectList(String sql, AdvEnumsGeomOpt advEnumsGeomOpt,
// String geomFieldName) {
// return eSelectList(sql, advEnumsGeomOpt, ListUtil.of(geomFieldName));
// }
//
// @Override
// public DataFieldsApo dGetColumnsByTable(String tableName) {
// DataFieldsApo dataFieldsApo = pgAdvDDLOpt.dGetColumnsByTable(tableName);
// List<String> geomFieldNameList = dataFieldsApo.getGeomFieldNameList();
// try {
// Map<String, AdvEnumsTypeGeom> typeMaps = eGetGeoTypeByTable(tableName,
// geomFieldNameList);
// return fillGeomType(dataFieldsApo, typeMaps);
// } catch (Exception e) {
// log.error(e.getMessage(), e);
// }
// return dataFieldsApo;
// }
//
// public DataFieldsApo dGetColumnsBySQL(String sqlView) {
// DataFieldsApo dataFieldsApo = pgAdvDDLOpt.dGetColumnsBySQL(sqlView);
// List<String> geomFieldNameList = dataFieldsApo.getGeomUnKnownTypeFieldNameList();
// try {
// Map<String, AdvEnumsTypeGeom> typeMaps = eGetGeoTypeBySql(sqlView, geomFieldNameList);
// return fillGeomType(dataFieldsApo, typeMaps);
// } catch (Exception e) {
// log.error(e.getMessage(), e);
// }
// return dataFieldsApo;
// }
//
//
// @Override
// public List<String> eGetAllGeoLayerName() {
// String sqlTemp = "SELECT table_name FROM information_schema.columns " +
// "WHERE udt_name = 'geometry' {} " +
// "GROUP BY table_name;";
//
// String schemaFilter = "";
// if (ObjectUtil.isNotEmpty(dataSourceGetter.getSchemaName())) {
// schemaFilter = StrUtil.format("AND \"table_schema\" = '{}'",
// dataSourceGetter.getSchemaName());
// }
//
// String sql = StrUtil.format(sqlTemp, schemaFilter);
//
// List<GirAdvOneRow> result = baseOpt.bSelectList(sql);
//
// List<String> layerNames = new ArrayList<>();
// if (result != null && !result.isEmpty()) {
// result.forEach(row -> {
// layerNames.add(row.getStr("table_name"));
// });
// }
// return layerNames;
// }
//
// @Override
// public boolean eIsGeomByTable(String tableName) {
// if (StrUtil.isEmpty(tableName)) {
// return false;
// }
//
// String geomField = eGetGeomColumnNameByTable(tableName);
// return ObjectUtil.isNotEmpty(geomField);
// }
//
// @Override
// public AdvEnumsTypeGeom eGetGeoTypeByTable(String tableName) {
// return eGetGeoTypeByTable(tableName, eGetGeomColumnNameByTable(tableName));
// }
//
// @Override
// public AdvEnumsTypeGeom eGetGeoTypeByTable(String tableName, String geomFieldName) {
// Map<String, AdvEnumsTypeGeom> re = eGetGeoTypeByTable(tableName,
// ListUtil.of(geomFieldName));
// if (MapUtil.isEmpty(re)) {
// return null;
// }
// return re.get(geomFieldName);
// }
//
// @Override
// public Map<String, AdvEnumsTypeGeom> eGetGeoTypeByTable(String tableName, List<String>
// geomFieldNames) {
// // 验证输入参数
// if (StrUtil.isEmpty(tableName) || ObjectUtil.isEmpty(geomFieldNames)) {
// return MapUtil.empty();
// }
// tableName = dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter,
// tableName);
// // 构建查询字段部分和非空过滤条件
// StringBuilder fieldsSql = new StringBuilder();
// StringBuilder whereSql = new StringBuilder();
//
// for (int i = 0; i < geomFieldNames.size(); i++) {
// String fieldName = geomFieldNames.get(i);
// // 为每个字段生成ST_GeometryType查询，并指定别名
// fieldsSql.append(" public.ST_GeometryType(").append(fieldName).append(") AS
// ").append(fieldName).append("_type");
//
// // 构建非空过滤条件，每个字段都不为null
// whereSql.append(fieldName).append(" IS NOT NULL");
//
// // 不是最后一个字段则添加逗号或AND分隔
// if (i != geomFieldNames.size() - 1) {
// fieldsSql.append(", ");
// whereSql.append(" AND ");
// }
// }
//
// // 构建完整的SQL语句，添加WHERE子句进行非空过滤
// String sqlTemp = "SELECT {} FROM {} WHERE {} LIMIT 1;";
// String formattedSql = StrUtil.format(sqlTemp, fieldsSql.toString(), tableName,
// whereSql.toString());
//
// // 执行查询
// GirAdvOneRow row = baseOpt.bSelectOne(formattedSql);
// if (row == null) {
// return MapUtil.empty();
// }
//
// // 处理查询结果，映射到Map中
// Map<String, AdvEnumsTypeGeom> resultMap = new HashMap<>(geomFieldNames.size());
// for (String fieldName : geomFieldNames) {
// String geomType = row.getStr(fieldName + "_type");
// if (StrUtil.isNotEmpty(geomType)) {
// resultMap.put(fieldName, getTypeGeomEnum(geomType));
// }
// }
//
// return resultMap;
// }
//
// @Override
// public AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlView) {
// String geomField = eGetGeomColumnNameBySql(sqlView);
// return eGetGeoTypeBySql(sqlView, geomField);
// }
//
// @Override
// public AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlView, String geomFieldName) {
// Map<String, AdvEnumsTypeGeom> re = eGetGeoTypeBySql(sqlView,
// ListUtil.of(geomFieldName));
// if (MapUtil.isEmpty(re)) {
// return null;
// }
// return re.get(geomFieldName);
// }
//
// @Override
// public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(String sqlView, List<String>
// geomFieldNames) {
// // 验证输入参数
// if (StrUtil.isEmpty(sqlView) || CollectionUtil.isEmpty(geomFieldNames)) {
// return MapUtil.empty();
// }
//
// // 移除SQL中的空格，保持与单个查询方法一致的处理
// sqlView = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlView);
//
// // 构建查询字段部分和非空过滤条件
// StringBuilder fieldsSql = new StringBuilder();
// StringBuilder whereSql = new StringBuilder();
//
// for (int i = 0; i < geomFieldNames.size(); i++) {
// String fieldName = geomFieldNames.get(i);
// // 为每个字段生成ST_GeometryType查询，并指定别名
// fieldsSql.append("public.ST_GeometryType(").append(fieldName).append(") AS
// ").append(fieldName).append("_type");
//
// // 构建非空过滤条件
// whereSql.append(fieldName).append(" IS NOT NULL");
//
// // 不是最后一个字段则添加分隔符
// if (i != geomFieldNames.size() - 1) {
// fieldsSql.append(", ");
// whereSql.append(" AND ");
// }
// }
//
// // 构建完整的SQL语句，将原始SQL作为子查询并添加WHERE条件
// String sql = StrUtil.format("SELECT {} FROM ({}) AS temp WHERE {} LIMIT 1;",
// fieldsSql.toString(), sqlView, whereSql.toString());
//
// // 执行查询
// GirAdvOneRow row = baseOpt.bSelectOne(sql);
// if (row == null) {
// return MapUtil.empty();
// }
//
// // 处理查询结果，映射到Map中
// Map<String, AdvEnumsTypeGeom> resultMap = new HashMap<>(geomFieldNames.size());
// for (String fieldName : geomFieldNames) {
// String geomType = row.getStr(fieldName + "_type");
// if (StrUtil.isNotEmpty(geomType)) {
// resultMap.put(fieldName, getTypeGeomEnum(geomType));
// }
// }
//
// return resultMap;
// }
//
// @Override
// public boolean eIsGeomBySql(String sqlView) {
// return StrUtil.isNotEmpty(eGetGeomColumnNameBySql(sqlView));
// }
//
//
// @Override
// public String eGetGeomColumnNameByTable(String tableName) {
// List<String> nameListByTable = eGetGeomColumnNameListByTable(tableName);
// if (ObjectUtil.isNotEmpty(nameListByTable)) {
// return nameListByTable.get(0);
// }
// return null;
// }
//
// @Override
// public List<String> eGetGeomColumnNameListByTable(String tableName) {
// if (StrUtil.isEmpty(tableName)) {
// return null;
// }
// String schemaName = dialectTableNameProcessor.tbExtractSchemaName(tableName);
//
// String notSchemaTableName =
// dialectTableNameProcessor.tbGetTableNameNotSchema(tableName);
//
// schemaName = ObjectUtil.isEmpty(schemaName) ? dataSourceGetter.getSchemaName() :
// schemaName;
//
// String sqlTemp = "SELECT column_name FROM information_schema.columns " +
// "WHERE table_name = '{}' AND udt_name = 'geometry' {};";
//
// String sql;
// if (ObjectUtil.isNotEmpty(schemaName)) {
// String schemaFilter = StrUtil.format("AND \"table_schema\" = '{}'", schemaName);
// sql = StrUtil.format(sqlTemp, notSchemaTableName, schemaFilter);
// } else {
// sql = StrUtil.format(sqlTemp, notSchemaTableName, "");
// }
//
// List<GirAdvOneRow> girAdvOneRows = baseOpt.bSelectList(sql);
// List<String> names = new ArrayList<>();
// if (ObjectUtil.isNotNull(girAdvOneRows)) {
// for (GirAdvOneRow girAdvOneRow : girAdvOneRows) {
// names.add(girAdvOneRow.getStr("column_name"));
// }
// }
// return names;
// }
//
// @Override
// public List<FieldBySchemaApo> eGetGeomColumnListByTable(String tableName) {
// DataFieldsApo dataFieldsApo = dGetColumnsByTable(tableName);
// return dataFieldsApo.getGeomFields();
// }
//
// @Override
// public FieldBySchemaApo eGetGeomColumnByTable(String tableName) {
// List<FieldBySchemaApo> fieldBySchemaApos = eGetGeomColumnListByTable(tableName);
// if (ObjectUtil.isNotEmpty(fieldBySchemaApos)) {
// return fieldBySchemaApos.get(0);
// }
// return null;
//
// }
//
// @Override
// public String eGetGeomColumnNameBySql(String sqlView) {
// if (StrUtil.isEmpty(sqlView)) {
// return null;
// }
// sqlView = dialectTableNameProcessor.tbRemoveSqlSpaces(sqlView);
// Connection connection = null;
// Statement statement = null;
// ResultSet resultSet = null;
// String aliasTableName = dialectTableNameProcessor.tbGetTempAliasTableName();
// try {
// // 构建查询元数据的SQL
// String fieldQuerySql = StrUtil.format("SELECT * FROM ({}) AS {} LIMIT 0", sqlView,
// aliasTableName);
//
// connection = dataSourceGetter.getConnection();
// if (connection == null) {
// throw new IllegalStateException("无法获取数据库连接");
// }
//
// statement = connection.createStatement();
// resultSet = statement.executeQuery(fieldQuerySql);
// ResultSetMetaData metaData = resultSet.getMetaData();
//
// if (metaData == null) {
// return null;
// }
//
// int columnCount = metaData.getColumnCount();
// // 遍历所有字段，查找空间类型字段
// for (int i = 1; i <= columnCount; i++) {
// String columnName = metaData.getColumnName(i);
// String columnTypeName = metaData.getColumnTypeName(i);
//
// // 判断是否为PostGIS几何类型
// if ("geometry".equals(columnTypeName) || "geography".equals(columnTypeName) ||
// "\"public\".\"geometry\"".equals(columnTypeName)) {
// return columnName;
// }
// }
// } catch (SQLException e) {
// log.error("通过SQL视图查询空间字段失败，SQL: {}, 错误: {}", sqlView, e.getMessage(), e);
// } finally {
// dataSourceGetter.closeResources(resultSet, statement, connection);
// }
// return null;
// }
//
// @Override
// public List<String> eGetGeomColumnNameListBySql(String sqlView) {
// List<FieldBySchemaApo> fieldBySchemaApos = eGetGeomColumnListBySql(sqlView);
// DataFieldsApo dataFieldsApo = new DataFieldsApo();
// dataFieldsApo.setDataFieldList(fieldBySchemaApos);
// return dataFieldsApo.getFieldNameList();
// }
//
// @Override
// public List<FieldBySchemaApo> eGetGeomColumnListBySql(String sqlView) {
// DataFieldsApo dataFieldsApo = dGetColumnsBySQL(sqlView);
// return dataFieldsApo.getGeomFields();
// }
//
// @Override
// public FieldBySchemaApo eGetGeomColumnBySql(String sqlView) {
// List<FieldBySchemaApo> fieldBySchemaApos = eGetGeomColumnListBySql(sqlView);
// if (ObjectUtil.isNotEmpty(fieldBySchemaApos)) {
// return fieldBySchemaApos.get(0);
// }
// return null;
// }
//
// @Override
// public boolean eIsPointTable(String tableName) {
// AdvEnumsTypeGeom geomType = eGetGeoTypeByTable(tableName);
// return AdvEnumsTypeGeom.Point.equals(geomType) ||
// AdvEnumsTypeGeom.MultiPoint.equals(geomType);
// }
//
// @Override
// public boolean eIsPolygonTable(String tableName) {
// AdvEnumsTypeGeom geomType = eGetGeoTypeByTable(tableName);
// return AdvEnumsTypeGeom.Polygon.equals(geomType) ||
// AdvEnumsTypeGeom.MultiPolygon.equals(geomType);
// }
//
// @Override
// public boolean eIsLineStringTable(String tableName) {
// AdvEnumsTypeGeom geomType = eGetGeoTypeByTable(tableName);
// return AdvEnumsTypeGeom.LineString.equals(geomType) ||
// AdvEnumsTypeGeom.MultiLineString.equals(geomType);
// }
//
// @Override
// public void eAddGeomColumn(String tableName, String geomFieldName, AdvEnumsTypeGeom
// geomType, int srid) {
// // 参数校验
// if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(geomFieldName) || geomType == null) {
// throw new IllegalArgumentException("表名、空间字段名和空间类型不能为空");
// }
// if (srid <= 0) {
// throw new IllegalArgumentException("SRID必须为正数");
// }
// if (!pgAdvDDLOpt.dIsTableExists(tableName)) {
// throw new RuntimeException(StrUtil.format("表[{}]不存在，无法添加空间字段", tableName));
// }
// if (StrUtil.isNotEmpty(eGetGeomColumnNameByTable(tableName))) {
// throw new RuntimeException(StrUtil.format("表[{}]已存在空间字段，一个表只能有一个空间字段", tableName));
// }
//
// // 构建添加空间字段的SQL
// String qualifiedTableName =
// dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
// String sql = StrUtil.format(
// "ALTER TABLE {} ADD COLUMN {} geometry({}, %d);",
// qualifiedTableName,
// geomFieldName,
// geomType.getCode(),
// srid
// );
//
// pgAdvDDLOpt.dExecuteDDL(sql, tableName, "添加空间字段[" + geomFieldName + "]");
//
// // 自动创建空间索引
// String indexName = StrUtil.format("idx_{}_{}", tableName, geomFieldName);
// eCreateSpatialIndex(tableName, geomFieldName, indexName);
// }
//
// @Override
// public void eDropGeomColumn(String tableName, String geomFieldName) {
// if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(geomFieldName)) {
// throw new IllegalArgumentException("表名和空间字段名不能为空");
// }
// if (!pgAdvDDLOpt.dIsTableExists(tableName)) {
// throw new RuntimeException(StrUtil.format("表[{}]不存在", tableName));
// }
//
// // 检查空间字段是否存在
// String existingGeomField = eGetGeomColumnNameByTable(tableName);
// if (StrUtil.isEmpty(existingGeomField) || !existingGeomField.equals(geomFieldName)) {
// log.warn("表[{}]中不存在空间字段[{}]，无需删除", tableName, geomFieldName);
// return;
// }
//
//
// // 删除空间字段
// String qualifiedTableName =
// dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
// String sql = StrUtil.format("ALTER TABLE {} DROP COLUMN {};", qualifiedTableName,
// geomFieldName);
// pgAdvDDLOpt.dExecuteDDL(sql, tableName, "删除空间字段[" + geomFieldName + "]");
// }
//
// @Override
// public void eDropGeomColumn(String tableName) {
// String geomFieldNameByTableName = eGetGeomColumnNameByTable(tableName);
// eDropGeomColumn(tableName, geomFieldNameByTableName);
// }
//
// @Override
// public void eTransformSrid(String tableName, String geomFieldName, int targetSrid) {
// if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(geomFieldName) || targetSrid <= 0) {
// throw new IllegalArgumentException("表名、空间字段名和目标SRID不能为空且SRID必须为正数");
// }
// if (!pgAdvDDLOpt.dIsTableExists(tableName)) {
// throw new RuntimeException(StrUtil.format("表[{}]不存在", tableName));
// }
// String existingGeomField = eGetGeomColumnNameByTable(tableName);
// if (StrUtil.isEmpty(existingGeomField) || !existingGeomField.equals(geomFieldName)) {
// throw new RuntimeException(StrUtil.format("表[{}]中不存在空间字段[{}]", tableName,
// geomFieldName));
// }
// AdvEnumsTypeGeom advEnumsTypeGeom = eGetGeoTypeByTable(tableName, geomFieldName);
// Integer srid = eGetSrid(tableName, geomFieldName);// 原字段声明的SRID
// if (srid == 0) {
// srid = 4326;
// }
// String qualifiedTableName =
// dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
//// 临时字段名（用于存储转换后的数据）
// String tempGeomField = "geom_" + IdUtil.simpleUUID().substring(0, 8);
//
// try {
// // 1. 新建临时字段geom_a，类型与原字段一致（暂不转换SRID）
// String createTempColSql = StrUtil.format(
// "ALTER TABLE {} ADD COLUMN {} geometry({},0);",
// qualifiedTableName,
// tempGeomField,
// advEnumsTypeGeom.name().toLowerCase()
// );
// pgAdvDDLOpt.dExecuteDDL(createTempColSql, tableName, "新建临时空间字段[" + tempGeomField +
// "]");
//
// // 2. 拷贝原字段数据到临时字段
// String copyDataSql = StrUtil.format(
// "UPDATE {} SET {} =ST_SetSRID({}, {});",
// qualifiedTableName,
// tempGeomField,
// geomFieldName,
// srid
// );
// pgAdvDDLOpt.dExecuteDDL(copyDataSql, tableName, "拷贝原空间字段[" + geomFieldName +
// "]数据到临时字段[" + tempGeomField + "]");
//
// // 3. 对临时字段执行坐标转换（修改类型并转换SRID）
//// String transformSql = StrUtil.format(
//// "ALTER TABLE {} ALTER COLUMN {} TYPE geometry(Geometry, {}) USING
// public.ST_Transform({}, {});",
//// qualifiedTableName,
//// tempGeomField,
//// targetSrid,
//// tempGeomField,
//// targetSrid
//// );
//
// String transformSql = StrUtil.format(
// "ALTER TABLE {} ALTER COLUMN {} TYPE geometry({}, {}) USING public.ST_Transform({},
// {});",
// qualifiedTableName,
// tempGeomField,
// advEnumsTypeGeom.name().toLowerCase(), // 保持几何类型一致
// targetSrid,
// tempGeomField, // 基于临时字段转换
// targetSrid
// );
//
// pgAdvDDLOpt.dExecuteDDL(transformSql, tableName, "转换临时字段[" + tempGeomField + "]的SRID为"
// + targetSrid);
//
// String renameGeomFieldName = geomFieldName + "_" + IdUtil.simpleUUID().substring(0, 8);
//
// // 4. 先重命名为旧字段名 renameGeomFieldName，避免直接删除的风险
// String renameOldColSql = StrUtil.format(
// "ALTER TABLE {} RENAME COLUMN {} TO {};",
// qualifiedTableName,
// geomFieldName,
// renameGeomFieldName
// );
// pgAdvDDLOpt.dExecuteDDL(renameOldColSql, tableName, "重命名原空间字段[" + geomFieldName + "]为
// [" + renameGeomFieldName + "]");
//
// // 5. 将临时字段重命名为原字段名
// String renameTempColSql = StrUtil.format(
// "ALTER TABLE {} RENAME COLUMN {} TO {};",
// qualifiedTableName,
// tempGeomField,
// geomFieldName
// );
// pgAdvDDLOpt.dExecuteDDL(renameTempColSql, tableName, "将临时字段[" + tempGeomField +
// "]重命名为原字段名[" + geomFieldName + "]");
//
// // 6. 彻底删除旧字段（如果确认转换成功）
// String dropOldColSql = StrUtil.format(
// "ALTER TABLE {} DROP COLUMN {};",
// qualifiedTableName,
// renameGeomFieldName
// );
// pgAdvDDLOpt.dExecuteDDL(dropOldColSql, tableName, "删除旧空间字段[" + renameGeomFieldName +
// "]");
//
// } catch (Exception e) {
// throw new RuntimeException("SRID转换失败，已回滚操作", e);
// }
// }
//
// @Override
// public void eTransformSrid(String tableName, int targetSrid) {
// String geomFieldNameByTableName = eGetGeomColumnNameByTable(tableName);
// eTransformSrid(tableName, geomFieldNameByTableName, targetSrid);
// }
//
// @Override
// public Integer eGetSrid(String tableNameOrSqlView, String geomFieldName) {
// if (StrUtil.isEmpty(tableNameOrSqlView) || StrUtil.isEmpty(geomFieldName)) {
// return null;
// }
// String qualifiedTableName = null;
// if (!pgAdvDDLOpt.dIsTableExists(tableNameOrSqlView)) {
// String tableAlias = dialectTableNameProcessor.tbGetTempAliasTableName();
// tableNameOrSqlView = dialectTableNameProcessor.tbRemoveSqlSpaces(tableNameOrSqlView);
// qualifiedTableName = StrUtil.format("( {} ) as {}", tableNameOrSqlView, tableAlias);
// } else {
// qualifiedTableName =
// dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter,
// tableNameOrSqlView);
// }
//
//
// String sql = StrUtil.format(
// "SELECT public.st_srid({}) AS srid FROM {} LIMIT 1;",
// geomFieldName,
// qualifiedTableName
// );
//
// GirAdvOneRow row = baseOpt.bSelectOne(sql);
// return row != null ? row.getInt("srid") : 0;
// }
//
// @Override
// public Integer eGetSrid(String tableNameOrSqlView) {
// boolean b = dialectTableNameProcessor.tbTableIsSqlView(tableNameOrSqlView);
// tableNameOrSqlView = dialectTableNameProcessor.tbRemoveSqlSpaces(tableNameOrSqlView);
// String geomFieldName = null;
// if (b) {
// geomFieldName = eGetGeomColumnNameBySql(tableNameOrSqlView);
// } else {
// geomFieldName = eGetGeomColumnNameByTable(tableNameOrSqlView);
// }
// return eGetSrid(tableNameOrSqlView, geomFieldName);
//
// }
//
// @Override
// public Map<String, Integer> eGetSrid(String tableNameOrSqlView, List<String>
// geomFieldNames) {
// Map<String, Integer> result = new HashMap<>();
// if (StrUtil.isEmpty(tableNameOrSqlView) || geomFieldNames == null ||
// geomFieldNames.isEmpty()) {
// return result;
// }
// for (String field : geomFieldNames) {
// if (StrUtil.isEmpty(field)) {
// return result;
// }
// }
// String qualifiedTableName;
// if (!pgAdvDDLOpt.dIsTableExists(tableNameOrSqlView)) {
// String tableAlias = dialectTableNameProcessor.tbGetTempAliasTableName();
// tableNameOrSqlView = dialectTableNameProcessor.tbRemoveSqlSpaces(tableNameOrSqlView);
// qualifiedTableName = StrUtil.format("( {} ) as {}", tableNameOrSqlView, tableAlias);
// } else {
// qualifiedTableName =
// dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter,
// tableNameOrSqlView);
// }
//
// // 1. 构建查询字段：用COALESCE标记空值（例如用-1表示字段值为空）
// StringBuilder sridSelectBuilder = new StringBuilder();
// // 2. 构建WHERE条件：至少一个字段不为NULL（避免全空记录）
// StringBuilder whereBuilder = new StringBuilder("WHERE ");
// for (int i = 0; i < geomFieldNames.size(); i++) {
// String field = geomFieldNames.get(i);
// // 字段查询：st_srid(field)，若为NULL则返回-1（标记为空值）
// sridSelectBuilder.append(StrUtil.format(
// "COALESCE(public.st_srid({}), -1) AS {}_srid",
// field,
// field
// ));
// // WHERE条件：field IS NOT NULL（多个字段用OR连接）
// whereBuilder.append(field).append(" IS NOT NULL");
//
// if (i != geomFieldNames.size() - 1) {
// sridSelectBuilder.append(", ");
// whereBuilder.append(" and ");
// }
// }
//
// // 拼接完整SQL（包含WHERE条件过滤全空记录）
// String sql = StrUtil.format(
// "SELECT {} FROM {} {} LIMIT 1;",
// sridSelectBuilder.toString(),
// qualifiedTableName,
// whereBuilder.toString()
// );
//
// // 执行查询并解析结果
// GirAdvOneRow row = baseOpt.bSelectOne(sql);
// if (row == null) {
// return result; // 所有记录中，所有空间字段都为NULL
// }
//
// Map<String, Integer> sridMap = new HashMap<>(geomFieldNames.size());
// for (String field : geomFieldNames) {
// int srid = row.getInt(field + "_srid");
// // 还原空值标记：若为-1则存NULL，否则存实际SRID
// sridMap.put(field, srid == -1 ? 0 : srid);
// }
//
// return sridMap;
// }
//
// @Override
// public void eCreateSpatialIndex(String tableName, String geomFieldName, String
// indexName) {
// if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(geomFieldName) ||
// StrUtil.isEmpty(indexName)) {
// throw new IllegalArgumentException("表名、空间字段名和索引名不能为空");
// }
// if (!pgAdvDDLOpt.dIsTableExists(tableName)) {
// throw new RuntimeException(StrUtil.format("表[{}]不存在", tableName));
// }
//
// // 检查空间字段是否存在
// String existingGeomField = eGetGeomColumnNameByTable(tableName);
// if (StrUtil.isEmpty(existingGeomField) || !existingGeomField.equals(geomFieldName)) {
// throw new RuntimeException(StrUtil.format("表[{}]中不存在空间字段[{}]", tableName,
// geomFieldName));
// }
//
// // 检查索引是否已存在
//
// if (pgAdvDDLOpt.dIndexesExists(tableName, indexName)) {
// log.warn("表[{}]中已存在空间索引[{}]，无需重复创建", tableName, indexName);
// return;
// }
//
// // 创建空间索引
// String qualifiedTableName =
// dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
// String sql = StrUtil.format(
// "CREATE INDEX {} ON {} USING GIST ({});",
// indexName,
// qualifiedTableName,
// geomFieldName
// );
// pgAdvDDLOpt.dExecuteDDL(sql, tableName, "创建空间索引[" + indexName + "]");
// }
//
// @Override
// public void eCreateSpatialIndex(String tableName, String indexName) {
// String geomFieldNameByTableName = eGetGeomColumnNameByTable(tableName);
// eCreateSpatialIndex(tableName, geomFieldNameByTableName, indexName);
// }
//
// @Override
// public void eDropSpatialIndex(String tableName, String indexName) {
// if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(indexName)) {
// throw new IllegalArgumentException("表名和索引名不能为空");
// }
//
// // 检查索引是否存在
// if (!pgAdvDDLOpt.dIndexesExists(tableName, indexName)) {
// log.warn("表[{}]中不存在索引[{}]，无需删除", tableName, indexName);
// return;
// }
//
// // 删除空间索引
// String sql = StrUtil.format(
// "DROP INDEX IF EXISTS {}.{};",
// dialectTableNameProcessor.tbGetSchemaNameForSql(dataSourceGetter),
// indexName
// );
// pgAdvDDLOpt.dExecuteDDL(sql, tableName, "删除空间索引[" + indexName + "]");
// }
//
// @Override
// public List<GirAdvOneRow> eQueryIntersects(String tableName, String geomFieldName,
// String geometry, int srid) {
// if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(geomFieldName) ||
// StrUtil.isEmpty(geometry)) {
// throw new IllegalArgumentException("表名、空间字段名和几何体WKT不能为空");
// }
// if (srid <= 0) {
// throw new IllegalArgumentException("SRID必须为正数");
// }
// String qualifiedTableName = null;
// if (!pgAdvDDLOpt.dIsTableExists(tableName)) {
// String tableAlias = dialectTableNameProcessor.tbGetTempAliasTableName();
// qualifiedTableName = StrUtil.format("( {} ) as {}", tableName, tableAlias);
// } else {
// qualifiedTableName =
// dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
// }
// String sql = StrUtil.format(
// "SELECT * FROM {} WHERE public.ST_Intersects({}, ST_GeomFromText('{}', {}));",
// qualifiedTableName,
// geomFieldName,
// geometry,
// srid
// );
//
// return baseOpt.bSelectList(sql);
// }
//
// @Override
// public List<GirAdvOneRow> eQueryIntersects(String tableName, String geometry, int srid)
// {
// String geomFieldNameByTableName = eGetGeomColumnNameByTable(tableName);
// return eQueryIntersects(tableName, geomFieldNameByTableName, geometry, srid);
// }
//
// @Override
// public List<GirAdvOneRow> eQueryWithinBBox(String tableName, String geomFieldName,
// double[] bbox, int srid) {
// if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(geomFieldName) || bbox == null ||
// bbox.length != 4) {
// throw new IllegalArgumentException("表名、空间字段名和边界框不能为空，且边界框必须包含4个元素[minx, miny, maxx,
// maxy]");
// }
// if (srid <= 0) {
// throw new IllegalArgumentException("SRID必须为正数");
// }
// String qualifiedTableName = null;
// if (!pgAdvDDLOpt.dIsTableExists(tableName)) {
// String tableAlias = dialectTableNameProcessor.tbGetTempAliasTableName();
// qualifiedTableName = StrUtil.format("( {} ) as {}", tableName, tableAlias);
// } else {
// qualifiedTableName =
// dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
// }
//
// String bboxWkt = StrUtil.format("POLYGON(({} {}, {} {}, {} {}, {} {}, {} {}))",
// bbox[0], bbox[1], // minx, miny
// bbox[0], bbox[3], // minx, maxy
// bbox[2], bbox[3], // maxx, maxy
// bbox[2], bbox[1], // maxx, miny
// bbox[0], bbox[1] // minx, miny (闭合)
// );
//
// String sql = StrUtil.format(
// "SELECT * FROM {} WHERE public.ST_Within({}, public.ST_GeomFromText('{}', {}));",
// qualifiedTableName,
// geomFieldName,
// bboxWkt,
// srid
// );
//
// return baseOpt.bSelectList(sql);
// }
//
// @Override
// public List<GirAdvOneRow> eQueryWithinBBox(String tableName, double[] bbox, int srid) {
// String geomFieldNameByTableName = eGetGeomColumnNameByTable(tableName);
// return eQueryWithinBBox(tableName, geomFieldNameByTableName, bbox, srid);
// }
//
// @Override
// public List<GirAdvOneRow> eCalculateDistance(String tableName, String geomFieldName,
// String geometry, int srid, String distanceAlias) {
// if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(geomFieldName) ||
// StrUtil.isEmpty(geometry) || StrUtil.isEmpty(distanceAlias)) {
// throw new IllegalArgumentException("表名、空间字段名、几何体WKT和距离字段别名不能为空");
// }
// if (srid <= 0) {
// throw new IllegalArgumentException("SRID必须为正数");
// }
// String qualifiedTableName = null;
// if (!pgAdvDDLOpt.dIsTableExists(tableName)) {
// String tableAlias = dialectTableNameProcessor.tbGetTempAliasTableName();
// qualifiedTableName = StrUtil.format("( {} ) as {}", tableName, tableAlias);
// } else {
// qualifiedTableName =
// dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
// }
//
// // 构建距离计算SQL
//
// String sql = StrUtil.format(
// "SELECT *, public.ST_Distance({}, public.ST_GeomFromText('{}', {})) AS {} FROM {};",
// geomFieldName,
// geometry,
// srid,
// distanceAlias,
// qualifiedTableName
// );
// return baseOpt.bSelectList(sql);
// }
//
// @Override
// public List<GirAdvOneRow> eCalculateDistance(String tableName, String geometry, int
// srid, String distanceAlias) {
// String geomFieldNameByTableName = eGetGeomColumnNameByTable(tableName);
// return eCalculateDistance(tableName, geomFieldNameByTableName, geometry, srid,
// distanceAlias);
// }
//
// @Override
// public List<GirAdvOneRow> eGetCentroid(String tableNameOrSqlView, String geomFieldName,
// String centerAlias) {
// if (StrUtil.isEmpty(tableNameOrSqlView) || StrUtil.isEmpty(geomFieldName) ||
// StrUtil.isEmpty(centerAlias)) {
// throw new IllegalArgumentException("表名、空间字段名和中心点字段别名不能为空");
// }
// String qualifiedTableName = null;
// if (!pgAdvDDLOpt.dIsTableExists(tableNameOrSqlView)) {
// String tableAlias = dialectTableNameProcessor.tbGetTempAliasTableName();
// tableNameOrSqlView = dialectTableNameProcessor.tbRemoveSqlSpaces(tableNameOrSqlView);
// qualifiedTableName = StrUtil.format("( {} ) as {}", tableNameOrSqlView, tableAlias);
// } else {
// qualifiedTableName =
// dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter,
// tableNameOrSqlView);
// }
// String sql = StrUtil.format(
// "SELECT *, public.ST_Centroid({}) AS {} FROM {};",
// geomFieldName,
// centerAlias,
// qualifiedTableName
// );
//
// return baseOpt.bSelectList(sql);
// }
//
// @Override
// public List<GirAdvOneRow> eGetCentroid(String tableNameOrSqlView, String centerAlias) {
// String geomFieldNameByTableName = eGetGeomColumnNameByTable(tableNameOrSqlView);
// return eGetCentroid(tableNameOrSqlView, geomFieldNameByTableName, centerAlias);
// }
//
// @Override
// public List<Object> eValidateGeometries(String tableName, String geomFieldName) {
// if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(geomFieldName)) {
// throw new IllegalArgumentException("表名和空间字段名不能为空");
// }
// if (!pgAdvDDLOpt.dIsTableExists(tableName)) {
// throw new RuntimeException(StrUtil.format("表[{}]不存在", tableName));
// }
//
// // 查询无效的几何体
// String qualifiedTableName =
// dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
// String sql = StrUtil.format(
// "SELECT id FROM {} WHERE NOT public.ST_IsValid({});",
// qualifiedTableName,
// geomFieldName
// );
//
// List<GirAdvOneRow> rows = baseOpt.bSelectList(sql);
// List<Object> invalidIds = new ArrayList<>();
// if (ObjectUtil.isNotEmpty(rows)) {
// rows.forEach(row -> invalidIds.add(row.get("id")));
// }
// return invalidIds;
// }
//
// @Override
// public List<Object> eValidateGeometries(String tableName) {
// String geomFieldNameByTableName = eGetGeomColumnNameByTable(tableName);
// return eValidateGeometries(tableName, geomFieldNameByTableName);
// }
//
// @Override
// public int eRepairGeometries(String tableName, String geomFieldName) {
// if (StrUtil.isEmpty(tableName) || StrUtil.isEmpty(geomFieldName)) {
// throw new IllegalArgumentException("表名和空间字段名不能为空");
// }
// if (!pgAdvDDLOpt.dIsTableExists(tableName)) {
// throw new RuntimeException(StrUtil.format("表[{}]不存在", tableName));
// }
//
// // 修复无效的几何体
// String qualifiedTableName =
// dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter, tableName);
// String sql = StrUtil.format(
// "UPDATE {} SET {} = public.ST_MakeValid({}) WHERE NOT public.ST_IsValid({});",
// qualifiedTableName,
// geomFieldName,
// geomFieldName,
// geomFieldName
// );
//
// Connection connection = null;
// Statement statement = null;
// try {
// connection = dataSourceGetter.getConnection();
// connection.setAutoCommit(false);
// statement = connection.createStatement();
// int updatedCount = statement.executeUpdate(sql);
// connection.commit();
// log.info("修复表[{}]中的无效几何体，共修复{}条记录", tableName, updatedCount);
// return updatedCount;
// } catch (SQLException e) {
// try {
// connection.rollback();
// } catch (SQLException ex) {
// log.warn("修复几何体回滚失败", ex);
// }
// log.error("修复表[{}]中的无效几何体失败", tableName, e);
// throw new RuntimeException("修复几何体失败: " + e.getMessage(), e);
// } finally {
// dataSourceGetter.closeResources(null, statement, connection);
// }
// }
//
// @Override
// public int eRepairGeometries(String tableName) {
// String geomFieldNameByTableName = eGetGeomColumnNameByTable(tableName);
// return eRepairGeometries(tableName, geomFieldNameByTableName);
// }
//
// @Override
// public BBoxApo eGetExtent(String tableNameOrSqlView, String geomFieldName) {
// if (StrUtil.isEmpty(tableNameOrSqlView) || StrUtil.isEmpty(geomFieldName)) {
// return new BBoxApo(new double[4], new double[4], 0);
// }
// String qualifiedTableName = null;
// if (!pgAdvDDLOpt.dIsTableExists(tableNameOrSqlView)) {
// String tableAlias = dialectTableNameProcessor.tbGetTempAliasTableName();
// tableNameOrSqlView = dialectTableNameProcessor.tbRemoveSqlSpaces(tableNameOrSqlView);
// qualifiedTableName = StrUtil.format("( {} ) as {}", tableNameOrSqlView, tableAlias);
// } else {
// qualifiedTableName =
// dialectTableNameProcessor.tbGetTableNameWithSchema(dataSourceGetter,
// tableNameOrSqlView);
// }
// Integer i = eGetSrid(tableNameOrSqlView, geomFieldName);
//// String sql = StrUtil.format(
//// "SELECT public.ST_XMin(extent) AS minx, public.ST_YMin(extent) AS miny, " +
//// "public.ST_XMax(extent) AS maxx, public.ST_YMax(extent) AS maxy " +
//// "FROM (SELECT public.ST_Extent({}) AS extent FROM {}) AS subquery;",
//// geomFieldName,
//// qualifiedTableName
//// );
// String sql = StrUtil.format(
// "\n SELECT public.ST_XMin ( extent ) AS minx,\n" +
// " public.ST_YMin ( extent ) AS miny,\n" +
// " public.ST_XMax ( extent ) AS maxx,\n" +
// " public.ST_YMax ( extent ) AS maxy,\n" +
// " public.ST_XMin ( public.st_transform ( extent, 4326 ) ) AS minx_gs,\n" +
// " public.ST_YMin ( public.st_transform ( extent, 4326 ) ) AS miny_gs,\n" +
// " public.ST_XMax ( public.st_transform ( extent, 4326 ) ) AS maxx_gs,\n" +
// " public.ST_YMax ( public.st_transform ( extent, 4326 ) ) AS maxy_gs \n" +
// "FROM\n" +
// " ( SELECT public.st_setsrid ( public.ST_Extent ( {} ), {} ) AS extent FROM {} ) AS
// lpl666;",
// geomFieldName, i,
// qualifiedTableName
// );
// sql = dialectTableNameProcessor.tbRemoveSqlSpaces(sql);
// GirAdvOneRow row = baseOpt.bSelectOne(sql);
// if (row == null) {
// return new BBoxApo(new double[4], new double[4], 0);
// }
// double[] bboxArray = {
// row.getDouble("minx", 0.0),
// row.getDouble("miny", 0.0),
// row.getDouble("maxx", 0.0),
// row.getDouble("maxy", 0.0)
// };
// double[] bboxArrayGs = {
// row.getDouble("minx_gs", 0.0),
// row.getDouble("miny_gs", 0.0),
// row.getDouble("maxx_gs", 0.0),
// row.getDouble("maxy_gs", 0.0)
// };
//
// return new BBoxApo(bboxArray, bboxArrayGs, i);
// }
//
// @Override
// public BBoxApo eGetExtent(String tableNameOrSqlView) {
// boolean b = dialectTableNameProcessor.tbTableIsSqlView(tableNameOrSqlView);
// String geomFieldName = null;
// if (b) {
// geomFieldName = eGetGeomColumnNameBySql(tableNameOrSqlView);
// } else {
// geomFieldName = eGetGeomColumnNameByTable(tableNameOrSqlView);
// }
// return eGetExtent(tableNameOrSqlView, geomFieldName);
// }
//
//
// /**
// * 将PostgreSQL的几何类型转换为枚举
// */
// public static AdvEnumsTypeGeom getTypeGeomEnum(String pgGeomType) {
// if (StrUtil.isEmpty(pgGeomType)) {
// return null;
// }
//
// // PostgreSQL的几何类型通常以"ST_"开头，如"ST_Point"
// String typeName = pgGeomType.replace("ST_", "").toLowerCase();
//
// for (AdvEnumsTypeGeom type : AdvEnumsTypeGeom.values()) {
// if (type.getCode().equalsIgnoreCase(typeName)) {
// return type;
// }
// }
//
// return AdvEnumsTypeGeom.Geometry; // 默认返回通用几何类型
// }
//
// /**
// * 处理空间字段的值
// */
// public static void processGeometryField(List<GirAdvOneRow> records, AdvEnumsGeomOpt
// advEnumsGeomOpt, List<String> geomFieldNameList) {
// if (ObjectUtil.isEmpty(geomFieldNameList)) {
// return;
// }
// for (GirAdvOneRow record : records) {
// for (String geomFieldName : geomFieldNameList) {
// if (!record.containsKey(geomFieldName)) {
// continue; //当输入的这个空间字段是不存在的，进行跳过
// }
// if (AdvEnumsGeomOpt.不做任何操作.equals(advEnumsGeomOpt)) {
// continue;
// }
// if (AdvEnumsGeomOpt.转换成WKT.equals(advEnumsGeomOpt)) {
// String value = record.getWktString(geomFieldName, "");
// record.put(geomFieldName, value);
// }
// if (AdvEnumsGeomOpt.转换成GeoJson.equals(advEnumsGeomOpt)) {
// String value = record.getGeoJsonStr(geomFieldName, "{}");
// record.put(geomFieldName, value);
// }
// if (AdvEnumsGeomOpt.转换成WKB.equals(advEnumsGeomOpt)) {
// String value = record.getWkBString(geomFieldName, "");
// record.put(geomFieldName, value);
// }
// if (AdvEnumsGeomOpt.转换为NULL.equals(advEnumsGeomOpt)) {
// record.put(geomFieldName, null);
// }
// if (AdvEnumsGeomOpt.转换为空字符串.equals(advEnumsGeomOpt)) {
// record.put(geomFieldName, "");
// }
// if (AdvEnumsGeomOpt.移除.equals(advEnumsGeomOpt)) {
// record.remove(geomFieldName);
// }
// }
// }
//
//
// }
//
//
// /**
// * 填充空间类型
// *
// * @param dataFieldsApo
// * @param typeMaps
// * @return
// */
// private DataFieldsApo fillGeomType(DataFieldsApo dataFieldsApo, Map<String,
// AdvEnumsTypeGeom> typeMaps) {
// if (MapUtil.isEmpty(typeMaps)) {
// return dataFieldsApo;
// }
// List<FieldBySchemaApo> dataFieldList = dataFieldsApo.getDataFieldList();
// for (FieldBySchemaApo fieldBySchemaApo : dataFieldList) {
// String columnName = fieldBySchemaApo.getColumnName();
// AdvEnumsTypeGeom advEnumsTypeGeom = typeMaps.get(columnName);
// if (ObjectUtil.isNotNull(advEnumsTypeGeom)) {
// fieldBySchemaApo.setGeomType(advEnumsTypeGeom);
// }
// }
// return dataFieldsApo;
// }
//
//
// }
