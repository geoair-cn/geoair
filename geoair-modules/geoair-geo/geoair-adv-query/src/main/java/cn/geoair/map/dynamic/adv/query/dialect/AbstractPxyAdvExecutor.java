package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.apo.*;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.sql.DataSource;

/**
 * 数据库的动态高级查询执行器
 *
 * <p>聚合了基础操作、DDL操作、空间几何操作、分页操作的实现， 对外提供统一的执行入口，支持多构造器初始化（DataSourceApo/DataSource/Connection）
 *
 * @author 张逢吉
 * @date 2025/10/9 11:10
 */
public abstract class AbstractPxyAdvExecutor implements IAdvExecutor {

    protected abstract IDataSourceGetter getDataSourceGetter();

    protected abstract IAdvBaseOpt getAdvBaseOpt();

    protected abstract IAdvDDLOpt getAdvDDLOpt();

    protected abstract IAdvSimplePagePreOpt getSimplePageOpt();

    protected abstract IAdvGeoPreOpt getGeoOpt();

    public abstract IAdvWhereSelectOpt getWhereSelectOpt();

    protected abstract DialectTableNameProcessor getDialectTableNameProcessor();

    public AbstractPxyAdvExecutor(DataSourceApo dataSourceApo) {
        this.initByDataSourceApo(dataSourceApo);
    }

    public AbstractPxyAdvExecutor(DataSource dataSource) {
        this.initByDataSource(dataSource);
    }

    public AbstractPxyAdvExecutor(DataSource dataSource, String dataSourceName) {
        this.initByDataSource(dataSource, dataSourceName);
    }

    public AbstractPxyAdvExecutor() {
    }

    public AbstractPxyAdvExecutor(Connection connection) {
        this.initByConnection(connection);
    }

    /**
     * 初始化所有功能模块代理对象
     */
    public void initProxyObjects() {

    }

    // ==================== 数据源初始化与资源管理 ====================
    @Override
    public void initByDataSourceApo(DataSourceApo dataSourceApo) {
        if (dataSourceApo == null) {
            throw new IllegalArgumentException("DataSourceApo 不能为空");
        }
        this.getDataSourceGetter().initByDataSourceApo(dataSourceApo);
        this.initProxyObjects();
    }

    @Override
    public void initByDataSource(DataSource dataSource) {
        initByDataSource(dataSource, null);
    }

    @Override
    public void initByDataSource(DataSource dataSource, String dataSourceName) {
        if (dataSource == null) {
            throw new IllegalArgumentException("DataSource 不能为空");
        }
        this.getDataSourceGetter().initByDataSource(dataSource, dataSourceName);
        this.initProxyObjects();
    }

    @Override
    public void initByConnection(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection 不能为空");
        }
        this.getDataSourceGetter().initByConnection(connection);
        this.initProxyObjects();
    }

    @Override
    public String getSchemaName() {
        return getDataSourceGetter().getSchemaName();
    }

    @Override
    public String getDatabaseName() {
        return getDataSourceGetter().getDatabaseName();
    }

    @Override
    public void setDatabaseNameGetterFunction(Supplier<String> databaseNameGetterFunction) {
        this.getDataSourceGetter().setDatabaseNameGetterFunction(databaseNameGetterFunction);
    }

    @Override
    public void setSchemaNameGetterFunction(Supplier<String> schemaNameGetterFunction) {
        this.getDataSourceGetter().setSchemaNameGetterFunction(schemaNameGetterFunction);
    }

    @Override
    public String getDataSourceId() {
        return getDataSourceGetter().getDataSourceId();
    }

    @Override
    public Connection getConnection() {
        return this.getDataSourceGetter().getConnection();
    }

    @Override
    public DataSourceApo getDataSourceApo() {
        return this.getDataSourceGetter().getDataSourceApo();
    }

    @Override
    public DataSource getDataSource() {
        return this.getDataSourceGetter().getDataSource();
    }

    // @Override
    // public DataStore getGeoToolsDataStore() {
    // return this.getDataSourceGetterPxy().getGeoToolsDataStore();
    // }

    @Override
    public void connectionClose(Connection connection) {
        this.getDataSourceGetter().connectionClose(connection);
    }

    @Override
    public void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        this.getDataSourceGetter().closeResources(rs, stmt, conn);
    }

    // ==================== 基础插入操作（代理调用PgAdvBaseOpt） ====================
    @Override
    public Integer bInsertBySql(String sqlStatement) {
        return getAdvBaseOpt().bInsertBySql(sqlStatement);
    }

    @Override
    public Integer bInsertBySql(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseOpt().bInsertBySql(dynamicSql, sqlParam);
    }

    @Override
    public Integer bInsertOne(String tableName, Map<String, Object> rowData) {
        return getAdvBaseOpt().bInsertOne(tableName, rowData);
    }

    @Override
    public <T> Integer bInsertOne(String tableName, T entity) {
        return getAdvBaseOpt().bInsertOne(tableName, entity);
    }

    @Override
    public Long bInsertOneReturnId(String tableName, Map<String, Object> rowData) {
        return getAdvBaseOpt().bInsertOneReturnId(tableName, rowData);
    }

    @Override
    public <T> Long bInsertOneReturnId(String tableName, T entity) {
        return getAdvBaseOpt().bInsertOneReturnId(tableName, entity);
    }

    @Override
    public Integer bInsertBatch(
            String tableName, Set<String> headers, List<Map<String, Object>> rowsData) {
        return getAdvBaseOpt().bInsertBatch(tableName, headers, rowsData);
    }

    @Override
    public <T> Integer bInsertBatch(String tableName, Collection<T> entities) {
        return getAdvBaseOpt().bInsertBatch(tableName, entities);
    }

    @Override
    public Integer bInsertBatchWithBatchSize(
            String tableName,
            Set<String> headers,
            List<Map<String, Object>> rowsData,
            int batchSize) {
        return getAdvBaseOpt().bInsertBatchWithBatchSize(tableName, headers, rowsData, batchSize);
    }

    @Override
    public <T> Integer bInsertBatchWithBatchSize(
            String tableName, Collection<T> entities, int batchSize) {
        return getAdvBaseOpt().bInsertBatchWithBatchSize(tableName, entities, batchSize);
    }

    @Override
    public Integer bInsertIgnore(String tableName, Map<String, Object> rowData) {
        return getAdvBaseOpt().bInsertIgnore(tableName, rowData);
    }

    @Override
    public Integer bInsertIgnoreBatch(
            String tableName, Set<String> headers, List<Map<String, Object>> rowsData) {
        return getAdvBaseOpt().bInsertIgnoreBatch(tableName, headers, rowsData);
    }

    @Override
    public Integer bInsertOrUpdate(
            String tableName, Map<String, Object> rowData, Set<String> updateFields) {
        return getAdvBaseOpt().bInsertOrUpdate(tableName, rowData, updateFields);
    }

    // ==================== 基础删除操作（代理调用PgAdvBaseOpt） ====================
    @Override
    public Integer bDeleteBySql(String sqlStatement) {
        return getAdvBaseOpt().bDeleteBySql(sqlStatement);
    }

    @Override
    public Integer bDeleteBySql(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseOpt().bDeleteBySql(dynamicSql, sqlParam);
    }

    @Override
    public Integer bDeleteByPrimaryKey(String tableName, String idKey, Object id) {
        return getAdvBaseOpt().bDeleteByPrimaryKey(tableName, idKey, id);
    }

    @Override
    public Integer bDeleteBatchByPrimaryKey(String tableName, String idKey, Set<Object> ids) {
        return getAdvBaseOpt().bDeleteBatchByPrimaryKey(tableName, idKey, ids);
    }

    @Override
    public Integer bDeleteBatchWithBatchSize(
            String tableName, String idKey, Set<Object> ids, int batchSize) {
        return getAdvBaseOpt().bDeleteBatchWithBatchSize(tableName, idKey, ids, batchSize);
    }

    @Override
    public Integer bDeleteByCondition(String tableName, Map<String, Object> whereMap) {
        return getAdvBaseOpt().bDeleteByCondition(tableName, whereMap);
    }

    @Override
    public Integer bDeleteBatchByCondition(
            String tableName, Map<String, Object> whereMap, int batchSize) {
        return getAdvBaseOpt().bDeleteBatchByCondition(tableName, whereMap, batchSize);
    }

    @Override
    public Integer bLogicDelete(
            String tableName, String idKey, Object id, String deleteKey, Object deleteValue) {
        return getAdvBaseOpt().bLogicDelete(tableName, idKey, id, deleteKey, deleteValue);
    }

    @Override
    public Integer bLogicDeleteBatch(
            String tableName, String idKey, Set<Object> ids, String deleteKey, Object deleteValue) {
        return getAdvBaseOpt().bLogicDeleteBatch(tableName, idKey, ids, deleteKey, deleteValue);
    }

    @Override
    public Integer bSafeDeleteByCondition(
            String tableName, Map<String, Object> whereMap, int maxDelete) {
        return getAdvBaseOpt().bSafeDeleteByCondition(tableName, whereMap, maxDelete);
    }

    @Override
    public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {
    }

    // ==================== 基础查询操作（代理调用PgAdvBaseOpt） ====================
    @Override
    public GirAdvOneRow bSelectOne(String sql) {
        return getAdvBaseOpt().bSelectOne(sql);
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sql) {
        return getAdvBaseOpt().bSelectList(sql);
    }

    @Override
    public void bSelectListStream(String sql, Consumer<GirAdvOneRow> rowConsumer) {
        getAdvBaseOpt().bSelectListStream(sql, rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sql) {
        return getAdvBaseOpt().bSelectListToValueList(sql);
    }

    @Override
    public Number bSelectNumber(String sql) {
        return getAdvBaseOpt().bSelectNumber(sql);
    }

    @Override
    public Number bSelectRecordRowCount(String sql) {
        return getAdvBaseOpt().bSelectRecordRowCount(sql);
    }

    @Override
    public <E> E bSelectObjOne(String sql, Class<E> clazz) {
        return getAdvBaseOpt().bSelectObjOne(sql, clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String sql, Class<E> clazz) {
        return getAdvBaseOpt().bSelectObjList(sql, clazz);
    }

    @Override
    public <E> void bSelectObjListStream(String sql, Class<E> clazz, Consumer<E> rowConsumer) {
        getAdvBaseOpt().bSelectObjListStream(sql, clazz, rowConsumer);
    }

    @Override
    public GirAdvOneRow bSelectOne(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseOpt().bSelectOne(dynamicSql, sqlParam);
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseOpt().bSelectList(dynamicSql, sqlParam);
    }

    @Override
    public void bSelectListStream(
            String dynamicSql, SqlParamMap sqlParam, Consumer<GirAdvOneRow> rowConsumer) {
        getAdvBaseOpt().bSelectListStream(dynamicSql, sqlParam, rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseOpt().bSelectListToValueList(dynamicSql, sqlParam);
    }

    @Override
    public Number bSelectNumber(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseOpt().bSelectNumber(dynamicSql, sqlParam);
    }

    @Override
    public Number bSelectRecordRowCount(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseOpt().bSelectRecordRowCount(dynamicSql, sqlParam);
    }

    @Override
    public <E> E bSelectObjOne(String dynamicSql, SqlParamMap sqlParam, Class<E> clazz) {
        return getAdvBaseOpt().bSelectObjOne(dynamicSql, sqlParam, clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String dynamicSql, SqlParamMap sqlParam, Class<E> clazz) {
        return getAdvBaseOpt().bSelectObjList(dynamicSql, sqlParam, clazz);
    }

    @Override
    public <E> void bSelectObjListStream(
            String dynamicSql, SqlParamMap sqlParam, Class<E> clazz, Consumer<E> rowConsumer) {
        getAdvBaseOpt().bSelectObjListStream(dynamicSql, sqlParam, clazz, rowConsumer);
    }


    @Override
    public GirAdvOneRow bSelectOne(String sqlStatement, SqlParamList sqlParamList) {
        return getAdvBaseOpt().bSelectOne(sqlStatement, sqlParamList);
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sqlStatement, SqlParamList sqlParamList) {
        return getAdvBaseOpt().bSelectList(sqlStatement, sqlParamList);
    }

    @Override
    public void bSelectListStream(String sqlStatement, SqlParamList sqlParamList, Consumer<GirAdvOneRow> rowConsumer) {
        getAdvBaseOpt().bSelectListStream(sqlStatement, sqlParamList, rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sqlStatement, SqlParamList sqlParamList) {
        return getAdvBaseOpt().bSelectListToValueList(sqlStatement, sqlParamList);
    }

    @Override
    public Number bSelectNumber(String sqlStatement, SqlParamList sqlParamList) {
        return getAdvBaseOpt().bSelectNumber(sqlStatement, sqlParamList);
    }

    @Override
    public Number bSelectRecordRowCount(String sqlStatement, SqlParamList sqlParamList) {
        return getAdvBaseOpt().bSelectRecordRowCount(sqlStatement, sqlParamList);
    }

    @Override
    public <E> E bSelectObjOne(String sqlStatement, SqlParamList sqlParamList, Class<E> clazz) {
        return getAdvBaseOpt().bSelectObjOne(sqlStatement, sqlParamList, clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String sqlStatement, SqlParamList sqlParamList, Class<E> clazz) {
        return getAdvBaseOpt().bSelectObjList(sqlStatement, sqlParamList, clazz);
    }

    @Override
    public <E> void bSelectObjListStream(String sqlStatement, SqlParamList sqlParamList, Class<E> clazz, Consumer<E> rowConsumer) {
        getAdvBaseOpt().bSelectObjListStream(sqlStatement, sqlParamList, clazz, rowConsumer);
    }


    // ==================== 基础更新操作（代理调用PgAdvBaseOpt） ====================
    @Override
    public Integer bUpdateBySql(String sqlStatement) {
        return getAdvBaseOpt().bUpdateBySql(sqlStatement);
    }

    @Override
    public Integer bUpdateBySql(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseOpt().bUpdateBySql(dynamicSql, sqlParam);
    }

    @Override
    public Integer bUpdateByPrimaryKey(
            String tableName, String idKey, Object id, Map<String, Object> rowData) {
        return getAdvBaseOpt().bUpdateByPrimaryKey(tableName, idKey, id, rowData);
    }

    @Override
    public <T> Integer bUpdateByPrimaryKey(String tableName, String idKey, T entity) {
        return getAdvBaseOpt().bUpdateByPrimaryKey(tableName, idKey, entity);
    }

    @Override
    public Integer bUpdateByCondition(
            String tableName, Map<String, Object> rowData, Map<String, Object> whereMap) {
        return getAdvBaseOpt().bUpdateByCondition(tableName, rowData, whereMap);
    }

    @Override
    public Integer bUpdateBatchByPrimaryKey(
            String tableName, String idKey, List<Map<String, Object>> rowsData) {
        return getAdvBaseOpt().bUpdateBatchByPrimaryKey(tableName, idKey, rowsData);
    }

    @Override
    public Integer bUpdateBatchWithBatchSize(
            String tableName, String idKey, List<Map<String, Object>> rowsData, int batchSize) {
        return getAdvBaseOpt().bUpdateBatchWithBatchSize(tableName, idKey, rowsData, batchSize);
    }

    @Override
    public <T> Integer bUpdateBatchByPrimaryKey(
            String tableName, String idKey, Collection<T> entities) {
        return getAdvBaseOpt().bUpdateBatchByPrimaryKey(tableName, idKey, entities);
    }

    @Override
    public Integer bUpdateWithOptimisticLock(
            String tableName,
            String idKey,
            Object id,
            Map<String, Object> rowData,
            String versionKey,
            Integer version) {
        return getAdvBaseOpt().bUpdateWithOptimisticLock(
                tableName, idKey, id, rowData, versionKey, version);
    }

    @Override
    public Integer bUpdateOrInsert(
            String tableName, Map<String, Object> rowData, Set<String> conflictKeys) {
        return getAdvBaseOpt().bUpdateOrInsert(tableName, rowData, conflictKeys);
    }

    // ==================== DDL操作（代理调用PgAdvDDLOpt） ====================
    @Override
    public int dExecuteDDL(String sql, String tableName, String operation) {
        return getAdvDDLOpt().dExecuteDDL(sql, tableName, operation);
    }

    @Override
    public int dExecuteDDL(
            String dynamicSql, SqlParamMap sqlParam, String tableName, String operation) {
        return getAdvDDLOpt().dExecuteDDL(dynamicSql, sqlParam, tableName, operation);
    }

    @Override
    public void dDelTable(String tableName) {
        getAdvDDLOpt().dDelTable(tableName);
    }

    @Override
    public void dTruncateTable(String tableName) {
        getAdvDDLOpt().dTruncateTable(tableName);
    }

    @Override
    public void dDropTable(String tableName) {
        getAdvDDLOpt().dDropTable(tableName);
    }

    @Override
    public String dGetCurrentSchema() {
        return getAdvDDLOpt().dGetCurrentSchema();
    }

    @Override
    public String dGetCurrentDataBase() {
        return getAdvDDLOpt().dGetCurrentDataBase();
    }

    @Override
    public List<String> dGetAllSchemas() {
        return getAdvDDLOpt().dGetAllSchemas();
    }

    @Override
    public String dGetTableComment(String tableName) {
        return getAdvDDLOpt().dGetTableComment(tableName);
    }

    @Override
    public DataFieldsApo dGetColumnsByTable(String tableName) {
        return getAdvDDLOpt().dGetColumnsByTable(tableName);
    }

    @Override
    public DataFieldsApo dGetColumnsBySQL(String sqlView) {
        return getAdvDDLOpt().dGetColumnsBySQL(sqlView);
    }

    @Override
    public DataFieldsApo dGetColumnsBySQL(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvDDLOpt().dGetColumnsBySQL(dynamicSql, sqlParam);
    }

    @Override
    public DataFieldsApo dGetColumnsBySQLOrTable(String tbNameOrSql) {
        return getAdvDDLOpt().dGetColumnsBySQLOrTable(tbNameOrSql);
    }

    @Override
    public void dCreateTable(String tableName, List<FieldBySchemaApo> fields, String primaryKey) {
        getAdvDDLOpt().dCreateTable(tableName, fields, primaryKey);
    }

    @Override
    public void dRenameTable(String oldTableName, String newTableName) {
        getAdvDDLOpt().dRenameTable(oldTableName, newTableName);
    }

    @Override
    public void dAddColumn(String tableName, FieldBySchemaApo field) {
        getAdvDDLOpt().dAddColumn(tableName, field);
    }

    @Override
    public void dAlterColumn(String tableName, String oldColumnName, FieldBySchemaApo newField) {
        getAdvDDLOpt().dAlterColumn(tableName, oldColumnName, newField);
    }

    @Override
    public void dDropColumn(String tableName, String columnName) {
        getAdvDDLOpt().dDropColumn(tableName, columnName);
    }

    @Override
    public List<String> dGetTablesBySchema(String schemaName) {
        return getAdvDDLOpt().dGetTablesBySchema(schemaName);
    }

    @Override
    public List<String> dGetTablesBySchema() {
        return getAdvDDLOpt().dGetTablesBySchema();
    }

    @Override
    public List<SchemaTableApo> dGetTableAndViewBySchema(String schemaName) {
        return getAdvDDLOpt().dGetTableAndViewBySchema(schemaName);
    }

    @Override
    public List<SchemaTableApo> dGetTableAndViewBySchema() {
        return getAdvDDLOpt().dGetTableAndViewBySchema();
    }

    @Override
    public boolean dIsTableExists(String tableName) {
        return getAdvDDLOpt().dIsTableExists(tableName);
    }

    @Override
    public boolean dIsFunctionExists(String functionName) {
        return getAdvDDLOpt().dIsFunctionExists(functionName);
    }

    @Override
    public void dCreateSchema(String schemaName) {
        getAdvDDLOpt().dCreateSchema(schemaName);
    }

    @Override
    public void dDropSchema(String schemaName, boolean cascade) {
        getAdvDDLOpt().dDropSchema(schemaName, cascade);
    }

    @Override
    public void dAddPrimaryKey(String tableName, List<String> columnNames, String constraintName) {
        getAdvDDLOpt().dAddPrimaryKey(tableName, columnNames, constraintName);
    }

    @Override
    public void dAddPrimaryKey(
            String tableName,
            String pkColumnName,
            String constraintName,
            PrimaryKeyType pkType,
            Integer pkColumnLength,
            String pkValuePrefix) {
        getAdvDDLOpt().dAddPrimaryKey(
                tableName, pkColumnName, constraintName, pkType, pkColumnLength, pkValuePrefix);
    }

    @Override
    public void dAddStringPrimaryKey(
            String tableName,
            String pkColumnName,
            int pkColumnLength,
            String constraintName,
            String pkValuePrefix) {
        getAdvDDLOpt().dAddStringPrimaryKey(
                tableName, pkColumnName, pkColumnLength, constraintName, pkValuePrefix);
    }

    @Override
    public void dAddIntAutoPrimaryKey(
            String tableName, String pkColumnName, String constraintName) {
        getAdvDDLOpt().dAddIntAutoPrimaryKey(tableName, pkColumnName, constraintName);
    }

    @Override
    public void dAddBigIntAutoPrimaryKey(
            String tableName, String pkColumnName, String constraintName) {
        getAdvDDLOpt().dAddBigIntAutoPrimaryKey(tableName, pkColumnName, constraintName);
    }

    @Override
    public void dAddIntNormalPrimaryKey(
            String tableName, String pkColumnName, String constraintName) {
        getAdvDDLOpt().dAddIntNormalPrimaryKey(tableName, pkColumnName, constraintName);
    }

    @Override
    public void dAddBigIntNormalPrimaryKey(
            String tableName, String pkColumnName, String constraintName) {
        getAdvDDLOpt().dAddBigIntNormalPrimaryKey(tableName, pkColumnName, constraintName);
    }

    @Override
    public void dDropPrimaryKey(String tableName, String constraintName) {
        getAdvDDLOpt().dDropPrimaryKey(tableName, constraintName);
    }

    @Override
    public void dCreateIndex(
            String tableName, String indexName, List<String> columnNames, boolean isUnique) {
        getAdvDDLOpt().dCreateIndex(tableName, indexName, columnNames, isUnique);
    }

    @Override
    public void dDropIndex(String tableName, String indexName) {
        getAdvDDLOpt().dDropIndex(tableName, indexName);
    }

    @Override
    public List<String> dGetPrimaryKeys(String tableName) {
        return getAdvDDLOpt().dGetPrimaryKeys(tableName);
    }

    @Override
    public List<IndexApo> dGetIndexes(String tableName) {
        return getAdvDDLOpt().dGetIndexes(tableName);
    }

    @Override
    public boolean dIndexesExists(String tableName, String indexName) {
        return getAdvDDLOpt().dIndexesExists(tableName, indexName);
    }

    @Override
    public String dGetTableSizeFormat(String tableName) {
        return getAdvDDLOpt().dGetTableSizeFormat(tableName);
    }

    @Override
    public Long dGetTableSize(String tableName) {
        return getAdvDDLOpt().dGetTableSize(tableName);
    }

    // ==================== 空间几何操作（代理调用PgAdvGeoOpt） ====================
    @Override
    public GirAdvOneRow eSelectOne(String sql, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return getGeoOpt().eSelectOne(sql, advEnumsGeomOpt);
    }

    @Override
    public GirAdvOneRow eSelectOne(
            String sql, AdvEnumsGeomOpt advEnumsGeomOpt, String geomFieldName) {
        return getGeoOpt().eSelectOne(sql, advEnumsGeomOpt, geomFieldName);
    }

    @Override
    public GirAdvOneRow eSelectOne(
            String sql, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList) {
        return getGeoOpt().eSelectOne(sql, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(String sql, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return getGeoOpt().eSelectList(sql, advEnumsGeomOpt);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String sql, AdvEnumsGeomOpt advEnumsGeomOpt, String geomFieldName) {
        return getGeoOpt().eSelectList(sql, advEnumsGeomOpt, geomFieldName);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String sql, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList) {
        return getGeoOpt().eSelectList(sql, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public List<String> eGetAllGeoLayerName() {
        return getGeoOpt().eGetAllGeoLayerName();
    }

    @Override
    public boolean eIsGeomByTable(String tableName) {
        return getGeoOpt().eIsGeomByTable(tableName);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeByTable(String tableName) {
        return getGeoOpt().eGetGeoTypeByTable(tableName);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeByTable(String tableName, String geomFieldName) {
        return getGeoOpt().eGetGeoTypeByTable(tableName, geomFieldName);
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeByTable(
            String tableName, List<String> geomFieldNames) {
        return getGeoOpt().eGetGeoTypeByTable(tableName, geomFieldNames);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlView) {
        return getGeoOpt().eGetGeoTypeBySql(sqlView);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlView, String geomFieldName) {
        return getGeoOpt().eGetGeoTypeBySql(sqlView, geomFieldName);
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(
            String sqlView, List<String> geomFieldNames) {
        return getGeoOpt().eGetGeoTypeBySql(sqlView, geomFieldNames);
    }

    @Override
    public boolean eIsGeomBySql(String sqlView) {
        return getGeoOpt().eIsGeomBySql(sqlView);
    }

    @Override
    public String eGetGeomColumnNameByTable(String tableName) {
        return getGeoOpt().eGetGeomColumnNameByTable(tableName);
    }

    @Override
    public List<String> eGetGeomColumnNameListByTable(String tableName) {
        return getGeoOpt().eGetGeomColumnNameListByTable(tableName);
    }

    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListByTable(String tableName) {
        return getGeoOpt().eGetGeomColumnListByTable(tableName);
    }

    @Override
    public FieldBySchemaApo eGetGeomColumnByTable(String tableName) {
        return getGeoOpt().eGetGeomColumnByTable(tableName);
    }

    @Override
    public String eGetGeomColumnNameBySql(String sqlView) {
        return getGeoOpt().eGetGeomColumnNameBySql(sqlView);
    }

    @Override
    public List<String> eGetGeomColumnNameListBySql(String sqlView) {
        return getGeoOpt().eGetGeomColumnNameListBySql(sqlView);
    }

    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListBySql(String sqlView) {
        return getGeoOpt().eGetGeomColumnListBySql(sqlView);
    }

    @Override
    public FieldBySchemaApo eGetGeomColumnBySql(String sqlView) {
        return getGeoOpt().eGetGeomColumnBySql(sqlView);
    }

    @Override
    public boolean eIsPointTable(String tableName) {
        return getGeoOpt().eIsPointTable(tableName);
    }

    @Override
    public boolean eIsPolygonTable(String tableName) {
        return getGeoOpt().eIsPolygonTable(tableName);
    }

    @Override
    public boolean eIsLineStringTable(String tableName) {
        return getGeoOpt().eIsLineStringTable(tableName);
    }

    @Override
    public void eAddGeomColumn(
            String tableName, String geomFieldName, AdvEnumsTypeGeom geomType, int srid) {
        getGeoOpt().eAddGeomColumn(tableName, geomFieldName, geomType, srid);
    }

    @Override
    public void eDropGeomColumn(String tableName, String geomFieldName) {
        getGeoOpt().eDropGeomColumn(tableName, geomFieldName);
    }

    @Override
    public void eDropGeomColumn(String tableName) {
        getGeoOpt().eDropGeomColumn(tableName);
    }

    @Override
    public void eTransformSrid(String tableName, String geomFieldName, int targetSrid) {
        getGeoOpt().eTransformSrid(tableName, geomFieldName, targetSrid);
    }

    @Override
    public void eTransformSrid(String tableName, int targetSrid) {
        getGeoOpt().eTransformSrid(tableName, targetSrid);
    }

    @Override
    public Integer eGetSrid(String tableNameOrSqlView, String geomFieldName) {
        return getGeoOpt().eGetSrid(tableNameOrSqlView, geomFieldName);
    }

    @Override
    public Integer eGetSrid(String tableNameOrSqlView) {
        return getGeoOpt().eGetSrid(tableNameOrSqlView);
    }

    @Override
    public Map<String, Integer> eGetSrid(String tableNameOrSqlView, List<String> geomFieldNames) {
        return getGeoOpt().eGetSrid(tableNameOrSqlView, geomFieldNames);
    }

    @Override
    public void eCreateSpatialIndex(String tableName, String geomFieldName, String indexName) {
        getGeoOpt().eCreateSpatialIndex(tableName, geomFieldName, indexName);
    }

    @Override
    public void eCreateSpatialIndex(String tableName, String indexName) {
        getGeoOpt().eCreateSpatialIndex(tableName, indexName);
    }

    @Override
    public void eDropSpatialIndex(String tableName, String indexName) {
        getGeoOpt().eDropSpatialIndex(tableName, indexName);
    }

    @Override
    public List<GirAdvOneRow> eQueryIntersects(
            String tableName, String geomFieldName, String geometry, int srid) {
        return getGeoOpt().eQueryIntersects(tableName, geomFieldName, geometry, srid);
    }

    @Override
    public List<GirAdvOneRow> eQueryIntersects(String tableName, String geometry, int srid) {
        return getGeoOpt().eQueryIntersects(tableName, geometry, srid);
    }

    @Override
    public List<GirAdvOneRow> eQueryWithinBBox(
            String tableName, String geomFieldName, double[] bbox, int srid) {
        return getGeoOpt().eQueryWithinBBox(tableName, geomFieldName, bbox, srid);
    }

    @Override
    public List<GirAdvOneRow> eQueryWithinBBox(String tableName, double[] bbox, int srid) {
        return getGeoOpt().eQueryWithinBBox(tableName, bbox, srid);
    }

    @Override
    public List<GirAdvOneRow> eCalculateDistance(
            String tableName,
            String geomFieldName,
            String geometry,
            int srid,
            String distanceAlias) {
        return getGeoOpt().eCalculateDistance(
                tableName, geomFieldName, geometry, srid, distanceAlias);
    }

    @Override
    public List<GirAdvOneRow> eCalculateDistance(
            String tableName, String geometry, int srid, String distanceAlias) {
        return getGeoOpt().eCalculateDistance(tableName, geometry, srid, distanceAlias);
    }

    @Override
    public List<GirAdvOneRow> eGetCentroid(
            String tableNameOrSqlView, String geomFieldName, String centerAlias) {
        return getGeoOpt().eGetCentroid(tableNameOrSqlView, geomFieldName, centerAlias);
    }

    @Override
    public List<GirAdvOneRow> eGetCentroid(String tableNameOrSqlView, String centerAlias) {
        return getGeoOpt().eGetCentroid(tableNameOrSqlView, centerAlias);
    }

    @Override
    public List<Object> eValidateGeometries(String tableName, String geomFieldName) {
        return getGeoOpt().eValidateGeometries(tableName, geomFieldName);
    }

    @Override
    public List<Object> eValidateGeometries(String tableName) {
        return getGeoOpt().eValidateGeometries(tableName);
    }

    @Override
    public int eRepairGeometries(String tableName, String geomFieldName) {
        return getGeoOpt().eRepairGeometries(tableName, geomFieldName);
    }

    @Override
    public int eRepairGeometries(String tableName) {
        return getGeoOpt().eRepairGeometries(tableName);
    }

    @Override
    public BBoxApo eGetExtent(String tableNameOrSqlView, String geomFieldName) {
        return getGeoOpt().eGetExtent(tableNameOrSqlView, geomFieldName);
    }

    @Override
    public BBoxApo eGetExtent(String tableNameOrSqlView) {
        return getGeoOpt().eGetExtent(tableNameOrSqlView);
    }

    // ==================== 分页操作（代理调用PgAdvSimplePageOpt） ====================
    @Override
    public Long pCount(String noPageSql) {
        return getSimplePageOpt().pCount(noPageSql);
    }

    @Override
    public String pBuildPageSql(
            String noPageSql, int pageSize, int pageNum, boolean pageNumStartZero) {
        return getSimplePageOpt().pBuildPageSql(noPageSql, pageSize, pageNum, pageNumStartZero);
    }

    @Override
    public String pBuildSqlWithOrder(String baseSql, List<OrderApo> orders, String tableAlias) {
        return getSimplePageOpt().pBuildSqlWithOrder(baseSql, orders, tableAlias);
    }

    @Override
    public String pBuildSqlWithOrder(String baseSql, List<OrderApo> orders) {
        return getSimplePageOpt().pBuildSqlWithOrder(baseSql, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo,
            List<OrderApo> orders) {
        return getSimplePageOpt().pPage(
                noPageSql,
                pageNum,
                pageSize,
                pageNumStartZero,
                advEnumsGeomOpt,
                hasFieldsInfo,
                orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize) {
        return getSimplePageOpt().pPage(noPageSql, pageNum, pageSize);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return getSimplePageOpt().pPage(noPageSql, pageNum, pageSize, advEnumsGeomOpt);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql, int pageNum, int pageSize, List<OrderApo> orders) {
        return getSimplePageOpt().pPage(noPageSql, pageNum, pageSize, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            boolean hasFieldsInfo) {
        return getSimplePageOpt().pPage(
                noPageSql, pageNum, pageSize, pageNumStartZero, hasFieldsInfo);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo) {
        return getSimplePageOpt().pPage(
                noPageSql, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, hasFieldsInfo);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero) {
        return getSimplePageOpt().pPage(noPageSql, pageNum, pageSize, pageNumStartZero);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            List<OrderApo> orders) {
        return getSimplePageOpt().pPage(noPageSql, pageNum, pageSize, pageNumStartZero, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt) {
        return getSimplePageOpt().pPage(
                noPageSql, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            List<OrderApo> orders) {
        return getSimplePageOpt().pPage(
                noPageSql, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo) {
        return getSimplePageOpt().pPage(
                noPageSql, pageNum, pageSize, advEnumsGeomOpt, hasFieldsInfo);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo,
            List<OrderApo> orders) {
        return getSimplePageOpt().pPage(
                noPageSql, pageNum, pageSize, advEnumsGeomOpt, hasFieldsInfo, orders);
    }

    @Override
    public String tbGetSchemaNameForSql(IDataSourceGetter dataSourceGetter) {
        return getDialectTableNameProcessor().tbGetSchemaNameForSql(dataSourceGetter);
    }

    @Override
    public String tbGetTableNameWithSchema(IDataSourceGetter dataSourceGetter, String tableName) {
        // 代理调用：带默认Schema的完整表名拼接
        return getDialectTableNameProcessor().tbGetTableNameWithSchema(dataSourceGetter, tableName);
    }


    @Override
    public String tbGetTableNameWithSchema(
            IDataSourceGetter dataSourceGetter, String tableName, String schemaName) {
        // 代理调用：带指定Schema的完整表名拼接
        return getDialectTableNameProcessor().tbGetTableNameWithSchema(
                dataSourceGetter, tableName, schemaName);
    }

    @Override
    public String tbGetTableNameNotSchema(String fullTableName) {
        // 代理调用：从完整表名提取纯表名（去除Schema前缀）
        return getDialectTableNameProcessor().tbGetTableNameNotSchema(fullTableName);
    }

    @Override
    public String tbExtractSchemaName(String fullTableName) {
        // 代理调用：从完整表名提取Schema名称
        return getDialectTableNameProcessor().tbExtractSchemaName(fullTableName);
    }

    @Override
    public String tbQuoteTableName(String tableName) {
        // 代理调用：给表名添加PostgreSQL的标识符引号（双引号）
        return getDialectTableNameProcessor().tbQuoteTableName(tableName);
    }

    @Override
    public String tbQuoteSchemaName(String schemaName) {
        // 代理调用：给Schema名称添加PostgreSQL的标识符引号（双引号）
        return getDialectTableNameProcessor().tbQuoteSchemaName(schemaName);
    }

    @Override
    public String tbUnquoteTableName(String quotedTableName) {
        // 代理调用：移除表名的标识符引号
        return getDialectTableNameProcessor().tbUnquoteTableName(quotedTableName);
    }

    @Override
    public String tbUnquoteSchemaName(String quotedSchemaName) {
        // 代理调用：移除Schema名称的标识符引号
        return getDialectTableNameProcessor().tbUnquoteSchemaName(quotedSchemaName);
    }

    @Override
    public String tbQuoteFieldName(String fieldName) {
        return getDialectTableNameProcessor().tbQuoteFieldName(fieldName);
    }

    @Override
    public boolean tbTableIsSqlView(String tableName) {
        // 代理调用：判断表名对应的对象是否是SQL视图
        return getDialectTableNameProcessor().tbTableIsSqlView(tableName);
    }

    @Override
    public String tbGetTempAliasTableName() {
        // 代理调用：生成临时表别名（如t_temp_xxx）
        return getDialectTableNameProcessor().tbGetTempAliasTableName();
    }

    @Override
    public String tbRemoveSqlSpaces(String sqlView) {
        // 代理调用：移除SQL语句中的多余空格（保留语法结构）
        return getDialectTableNameProcessor().tbRemoveSqlSpaces(sqlView);
    }

    @Override
    public String tbGetSchemaNameForSql() {
        return tbGetSchemaNameForSql(this);
    }

    @Override
    public String tbGetTableNameWithSchema(String tableName) {
        return tbGetTableNameWithSchema(this, tableName);
    }

    @Override
    public String tbGetTableNameWithSchema(String tableName, String schemaName) {
        return tbGetTableNameWithSchema(this, tableName, schemaName);
    }

    @Override
    public String tbBuildPageSql(String noPageSql, int pageSize, long offset) {
        return getDialectTableNameProcessor().tbBuildPageSql(noPageSql, pageSize, offset);
    }

    @Override
    public String tbBuildPageSql(String noPageSql) {
        return getDialectTableNameProcessor().tbBuildPageSql(noPageSql);
    }


    @Override
    public GirAdvOneRow eSelectOne(
            String dynamicSql, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return getGeoOpt().eSelectOne(dynamicSql, sqlParam, advEnumsGeomOpt);
    }

    @Override
    public GirAdvOneRow eSelectOne(
            String sqlStatement,
            SqlParamMap sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            String geomFieldName) {
        return getGeoOpt().eSelectOne(sqlStatement, sqlParam, advEnumsGeomOpt, geomFieldName);
    }

    @Override
    public GirAdvOneRow eSelectOne(
            String sqlStatement,
            SqlParamMap sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            List<String> geomFieldNameList) {
        return getGeoOpt().eSelectOne(sqlStatement, sqlParam, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String dynamicSql, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return getGeoOpt().eSelectList(dynamicSql, sqlParam, advEnumsGeomOpt);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String sqlStatement,
            SqlParamMap sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            String geomFieldName) {
        return getGeoOpt().eSelectList(sqlStatement, sqlParam, advEnumsGeomOpt, geomFieldName);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String sqlStatement,
            SqlParamMap sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            List<String> geomFieldNameList) {
        return getGeoOpt().eSelectList(sqlStatement, sqlParam, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(String dynamicSql, SqlParamMap sqlParam) {
        return getGeoOpt().eGetGeoTypeBySql(dynamicSql, sqlParam);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(
            String dynamicSql, SqlParamMap sqlParam, String geomFieldName) {
        return getGeoOpt().eGetGeoTypeBySql(dynamicSql, sqlParam, geomFieldName);
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(
            String dynamicSql, SqlParamMap sqlParam, List<String> geomFieldNames) {
        return getGeoOpt().eGetGeoTypeBySql(dynamicSql, sqlParam, geomFieldNames);
    }

    @Override
    public boolean eIsGeomBySql(String dynamicSql, SqlParamMap sqlParam) {
        return getGeoOpt().eIsGeomBySql(dynamicSql, sqlParam);
    }

    @Override
    public String eGetGeomColumnNameBySql(String dynamicSql, SqlParamMap sqlParam) {
        return getGeoOpt().eGetGeomColumnNameBySql(dynamicSql, sqlParam);
    }

    @Override
    public List<String> eGetGeomColumnNameListBySql(String dynamicSql, SqlParamMap sqlParam) {
        return getGeoOpt().eGetGeomColumnNameListBySql(dynamicSql, sqlParam);
    }

    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListBySql(
            String dynamicSql, SqlParamMap sqlParam) {
        return getGeoOpt().eGetGeomColumnListBySql(dynamicSql, sqlParam);
    }

    @Override
    public FieldBySchemaApo eGetGeomColumnBySql(String dynamicSql, SqlParamMap sqlParam) {
        return getGeoOpt().eGetGeomColumnBySql(dynamicSql, sqlParam);
    }

    @Override
    public Long pCount(String noPageSqlStatement, SqlParamMap sqlParam) {
        return getSimplePageOpt().pCount(noPageSqlStatement, sqlParam);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            SqlParamMap sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo,
            List<OrderApo> orders) {
        return getSimplePageOpt().pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                pageNumStartZero,
                advEnumsGeomOpt,
                hasFieldsInfo,
                orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize) {
        return getSimplePageOpt().pPage(noPageSqlStatement, sqlParam, pageNum, pageSize);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            SqlParamMap sqlParam,
            int pageNum,
            int pageSize,
            AdvEnumsGeomOpt advEnumsGeomOpt) {
        return getSimplePageOpt().pPage(
                noPageSqlStatement, sqlParam, pageNum, pageSize, advEnumsGeomOpt);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            SqlParamMap sqlParam,
            int pageNum,
            int pageSize,
            List<OrderApo> orders) {
        return getSimplePageOpt().pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            SqlParamMap sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            boolean hasFieldsInfo) {
        return getSimplePageOpt().pPage(
                noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, hasFieldsInfo);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            SqlParamMap sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo) {
        return getSimplePageOpt().pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                pageNumStartZero,
                advEnumsGeomOpt,
                hasFieldsInfo);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            SqlParamMap sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero) {
        return getSimplePageOpt().pPage(
                noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            SqlParamMap sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            List<OrderApo> orders) {
        return getSimplePageOpt().pPage(
                noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            SqlParamMap sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt) {
        return getSimplePageOpt().pPage(
                noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            SqlParamMap sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            List<OrderApo> orders) {
        return getSimplePageOpt().pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                pageNumStartZero,
                advEnumsGeomOpt,
                orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            SqlParamMap sqlParam,
            int pageNum,
            int pageSize,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo) {
        return getSimplePageOpt().pPage(
                noPageSqlStatement, sqlParam, pageNum, pageSize, advEnumsGeomOpt, hasFieldsInfo);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            SqlParamMap sqlParam,
            int pageNum,
            int pageSize,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo,
            List<OrderApo> orders) {
        return getSimplePageOpt().pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                advEnumsGeomOpt,
                hasFieldsInfo,
                orders);
    }

    @Override
    public GirAdvOneRow wSelectOne(GirAdvQueryRequest query) {
        return getWhereSelectOpt().wSelectOne(query);
    }

    @Override
    public List<GirAdvOneRow> wSelectList(GirAdvQueryRequest query) {
        return getWhereSelectOpt().wSelectList(query);
    }

    @Override
    public PageApo<GirAdvOneRow> wSelectPage(GirAdvQueryRequest query) {
        return getWhereSelectOpt().wSelectPage(query);
    }

    @Override
    public void wSelectStream(GirAdvQueryRequest query, Consumer<GirAdvOneRow> rowConsumer) {
        getWhereSelectOpt().wSelectStream(query, rowConsumer);
    }

    @Override
    public Number wSelectCount(GirAdvQueryRequest query) {
        return getWhereSelectOpt().wSelectCount(query);
    }

    @Override
    public List<List<Object>> wSelectListToValueList(GirAdvQueryRequest query) {
        return getWhereSelectOpt().wSelectListToValueList(query);
    }

    @Override
    public Number wSelectNumber(GirAdvQueryRequest query) {
        return getWhereSelectOpt().wSelectNumber(query);
    }

    @Override
    public <E> E wSelectObjOne(GirAdvQueryRequest query, Class<E> clazz) {
        return getWhereSelectOpt().wSelectObjOne(query, clazz);
    }

    @Override
    public <E> List<E> wSelectObjList(GirAdvQueryRequest query, Class<E> clazz) {
        return getWhereSelectOpt().wSelectObjList(query, clazz);
    }

    @Override
    public <E> void wSelectObjStream(GirAdvQueryRequest query, Class<E> clazz, Consumer<E> rowConsumer) {
        getWhereSelectOpt().wSelectObjStream(query, clazz, rowConsumer);
    }
}
