package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.apo.*;
import cn.geoair.map.dynamic.adv.query.dialect.pg.PgDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;

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

    // 数据源获取器（核心依赖）
    protected IDataSourceGetter dataSourceGetterPxy;

    protected abstract IDataSourceGetter getDataSourceGetterPxy();

    // 各功能模块代理对象
    private IAdvBaseOpt advBaseOptPxy;

    private IAdvDDLOpt advDDLOptPxy;

    private IAdvGeoPreOpt advGeoOptPxy;

    private IAdvSimplePagePreOpt advSimplePageOptPxy;

    DialectTableNameProcessor dialectTableNameProcessorPxy = PgDialectTableNameUtil.getInstance();

    protected abstract IAdvBaseOpt getAdvBaseOpt();

    protected abstract IAdvDDLOpt getAdvDDLOpt();

    protected abstract IAdvSimplePagePreOpt getSimplePageOpt();

    protected abstract IAdvGeoPreOpt getGeoOpt();

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
        this.advBaseOptPxy = getAdvBaseOpt();
        this.advDDLOptPxy = getAdvDDLOpt();
        this.advGeoOptPxy = getGeoOpt();
        this.advSimplePageOptPxy = getSimplePageOpt();
    }

    // ==================== 数据源初始化与资源管理 ====================
    @Override
    public void initByDataSourceApo(DataSourceApo dataSourceApo) {
        if (dataSourceApo == null) {
            throw new IllegalArgumentException("DataSourceApo 不能为空");
        }
        this.getDataSourceGetterPxy().initByDataSourceApo(dataSourceApo);
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
        this.getDataSourceGetterPxy().initByDataSource(dataSource, dataSourceName);
        this.initProxyObjects();
    }

    @Override
    public void initByConnection(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection 不能为空");
        }
        this.getDataSourceGetterPxy().initByConnection(connection);
        this.initProxyObjects();
    }

    @Override
    public String getSchemaName() {
        return getDataSourceGetterPxy().getSchemaName();
    }

    @Override
    public String getDatabaseName() {
        return getDataSourceGetterPxy().getDatabaseName();
    }

    @Override
    public void setDatabaseNameGetterFunction(Supplier<String> databaseNameGetterFunction) {
        this.getDataSourceGetterPxy().setDatabaseNameGetterFunction(databaseNameGetterFunction);
    }

    @Override
    public void setSchemaNameGetterFunction(Supplier<String> schemaNameGetterFunction) {
        this.getDataSourceGetterPxy().setSchemaNameGetterFunction(schemaNameGetterFunction);
    }

    @Override
    public String getDataSourceId() {
        return getDataSourceGetterPxy().getDataSourceId();
    }

    @Override
    public Connection getConnection() {
        return this.getDataSourceGetterPxy().getConnection();
    }

    @Override
    public DataSourceApo getDataSourceApo() {
        return this.getDataSourceGetterPxy().getDataSourceApo();
    }

    @Override
    public DataSource getDataSource() {
        return this.getDataSourceGetterPxy().getDataSource();
    }

    // @Override
    // public DataStore getGeoToolsDataStore() {
    // return this.getDataSourceGetterPxy().getGeoToolsDataStore();
    // }

    @Override
    public void connectionClose(Connection connection) {
        this.getDataSourceGetterPxy().connectionClose(connection);
    }

    @Override
    public void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        this.getDataSourceGetterPxy().closeResources(rs, stmt, conn);
    }

    // ==================== 基础插入操作（代理调用PgAdvBaseOpt） ====================
    @Override
    public Integer bInsertBySql(String sqlStatement) {
        return advBaseOptPxy.bInsertBySql(sqlStatement);
    }

    @Override
    public Integer bInsertBySql(String   dynamicSql, SqlParamMap sqlParam) {
        return advBaseOptPxy.bInsertBySql(dynamicSql, sqlParam);
    }

    @Override
    public Integer bInsertOne(String tableName, Map<String, Object> rowData) {
        return advBaseOptPxy.bInsertOne(tableName, rowData);
    }

    @Override
    public <T> Integer bInsertOne(String tableName, T entity) {
        return advBaseOptPxy.bInsertOne(tableName, entity);
    }

    @Override
    public Long bInsertOneReturnId(String tableName, Map<String, Object> rowData) {
        return advBaseOptPxy.bInsertOneReturnId(tableName, rowData);
    }

    @Override
    public <T> Long bInsertOneReturnId(String tableName, T entity) {
        return advBaseOptPxy.bInsertOneReturnId(tableName, entity);
    }

    @Override
    public Integer bInsertBatch(
            String tableName, Set<String> headers, List<Map<String, Object>> rowsData) {
        return advBaseOptPxy.bInsertBatch(tableName, headers, rowsData);
    }

    @Override
    public <T> Integer bInsertBatch(String tableName, Collection<T> entities) {
        return advBaseOptPxy.bInsertBatch(tableName, entities);
    }

    @Override
    public Integer bInsertBatchWithBatchSize(
            String tableName,
            Set<String> headers,
            List<Map<String, Object>> rowsData,
            int batchSize) {
        return advBaseOptPxy.bInsertBatchWithBatchSize(tableName, headers, rowsData, batchSize);
    }

    @Override
    public <T> Integer bInsertBatchWithBatchSize(
            String tableName, Collection<T> entities, int batchSize) {
        return advBaseOptPxy.bInsertBatchWithBatchSize(tableName, entities, batchSize);
    }

    @Override
    public Integer bInsertIgnore(String tableName, Map<String, Object> rowData) {
        return advBaseOptPxy.bInsertIgnore(tableName, rowData);
    }

    @Override
    public Integer bInsertIgnoreBatch(
            String tableName, Set<String> headers, List<Map<String, Object>> rowsData) {
        return advBaseOptPxy.bInsertIgnoreBatch(tableName, headers, rowsData);
    }

    @Override
    public Integer bInsertOrUpdate(
            String tableName, Map<String, Object> rowData, Set<String> updateFields) {
        return advBaseOptPxy.bInsertOrUpdate(tableName, rowData, updateFields);
    }

    // ==================== 基础删除操作（代理调用PgAdvBaseOpt） ====================
    @Override
    public Integer bDeleteBySql(String sqlStatement) {
        return advBaseOptPxy.bDeleteBySql(sqlStatement);
    }

    @Override
    public Integer bDeleteBySql(String   dynamicSql, SqlParamMap sqlParam) {
        return advBaseOptPxy.bDeleteBySql(dynamicSql, sqlParam);
    }

    @Override
    public Integer bDeleteByPrimaryKey(String tableName, String idKey, Object id) {
        return advBaseOptPxy.bDeleteByPrimaryKey(tableName, idKey, id);
    }

    @Override
    public Integer bDeleteBatchByPrimaryKey(String tableName, String idKey, Set<Object> ids) {
        return advBaseOptPxy.bDeleteBatchByPrimaryKey(tableName, idKey, ids);
    }

    @Override
    public Integer bDeleteBatchWithBatchSize(
            String tableName, String idKey, Set<Object> ids, int batchSize) {
        return advBaseOptPxy.bDeleteBatchWithBatchSize(tableName, idKey, ids, batchSize);
    }

    @Override
    public Integer bDeleteByCondition(String tableName, Map<String, Object> whereMap) {
        return advBaseOptPxy.bDeleteByCondition(tableName, whereMap);
    }

    @Override
    public Integer bDeleteBatchByCondition(
            String tableName, Map<String, Object> whereMap, int batchSize) {
        return advBaseOptPxy.bDeleteBatchByCondition(tableName, whereMap, batchSize);
    }

    @Override
    public Integer bLogicDelete(
            String tableName, String idKey, Object id, String deleteKey, Object deleteValue) {
        return advBaseOptPxy.bLogicDelete(tableName, idKey, id, deleteKey, deleteValue);
    }

    @Override
    public Integer bLogicDeleteBatch(
            String tableName, String idKey, Set<Object> ids, String deleteKey, Object deleteValue) {
        return advBaseOptPxy.bLogicDeleteBatch(tableName, idKey, ids, deleteKey, deleteValue);
    }

    @Override
    public Integer bSafeDeleteByCondition(
            String tableName, Map<String, Object> whereMap, int maxDelete) {
        return advBaseOptPxy.bSafeDeleteByCondition(tableName, whereMap, maxDelete);
    }

    @Override
    public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {
    }

    // ==================== 基础查询操作（代理调用PgAdvBaseOpt） ====================
    @Override
    public GirAdvOneRow bSelectOne(String sql) {
        return advBaseOptPxy.bSelectOne(sql);
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sql) {
        return advBaseOptPxy.bSelectList(sql);
    }

    @Override
    public void bSelectList(String sql, Consumer<GirAdvOneRow> rowConsumer) {
        advBaseOptPxy.bSelectList(sql, rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sql) {
        return advBaseOptPxy.bSelectListToValueList(sql);
    }

    @Override
    public Number bSelectNumber(String sql) {
        return advBaseOptPxy.bSelectNumber(sql);
    }

    @Override
    public Number bSelectRecordRowCount(String sql) {
        return advBaseOptPxy.bSelectRecordRowCount(sql);
    }

    @Override
    public <E> E bSelectObjOne(String sql, Class<E> clazz) {
        return advBaseOptPxy.bSelectObjOne(sql, clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String sql, Class<E> clazz) {
        return advBaseOptPxy.bSelectObjList(sql, clazz);
    }

    @Override
    public <E> void bSelectObjList(String sql, Class<E> clazz, Consumer<E> rowConsumer) {
        advBaseOptPxy.bSelectObjList(sql, clazz, rowConsumer);
    }

    @Override
    public GirAdvOneRow bSelectOne(String   dynamicSql, SqlParamMap sqlParam) {
        return advBaseOptPxy.bSelectOne(dynamicSql, sqlParam);
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String   dynamicSql, SqlParamMap sqlParam) {
        return advBaseOptPxy.bSelectList(dynamicSql, sqlParam);
    }

    @Override
    public void bSelectList(
            String   dynamicSql, SqlParamMap sqlParam, Consumer<GirAdvOneRow> rowConsumer) {
        advBaseOptPxy.bSelectList(dynamicSql, sqlParam, rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String   dynamicSql, SqlParamMap sqlParam) {
        return advBaseOptPxy.bSelectListToValueList(dynamicSql, sqlParam);
    }

    @Override
    public Number bSelectNumber(String   dynamicSql, SqlParamMap sqlParam) {
        return advBaseOptPxy.bSelectNumber(dynamicSql, sqlParam);
    }

    @Override
    public Number bSelectRecordRowCount(String   dynamicSql, SqlParamMap sqlParam) {
        return advBaseOptPxy.bSelectRecordRowCount(dynamicSql, sqlParam);
    }

    @Override
    public <E> E bSelectObjOne(String   dynamicSql, SqlParamMap sqlParam, Class<E> clazz) {
        return advBaseOptPxy.bSelectObjOne(dynamicSql, sqlParam, clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String   dynamicSql, SqlParamMap sqlParam, Class<E> clazz) {
        return advBaseOptPxy.bSelectObjList(dynamicSql, sqlParam, clazz);
    }

    @Override
    public <E> void bSelectObjList(
            String   dynamicSql, SqlParamMap sqlParam, Class<E> clazz, Consumer<E> rowConsumer) {
        advBaseOptPxy.bSelectObjList(dynamicSql, sqlParam, clazz, rowConsumer);
    }

    // ==================== 基础更新操作（代理调用PgAdvBaseOpt） ====================
    @Override
    public Integer bUpdateBySql(String sqlStatement) {
        return advBaseOptPxy.bUpdateBySql(sqlStatement);
    }

    @Override
    public Integer bUpdateBySql(String   dynamicSql, SqlParamMap sqlParam) {
        return advBaseOptPxy.bUpdateBySql(dynamicSql, sqlParam);
    }

    @Override
    public Integer bUpdateByPrimaryKey(
            String tableName, String idKey, Object id, Map<String, Object> rowData) {
        return advBaseOptPxy.bUpdateByPrimaryKey(tableName, idKey, id, rowData);
    }

    @Override
    public <T> Integer bUpdateByPrimaryKey(String tableName, String idKey, T entity) {
        return advBaseOptPxy.bUpdateByPrimaryKey(tableName, idKey, entity);
    }

    @Override
    public Integer bUpdateByCondition(
            String tableName, Map<String, Object> rowData, Map<String, Object> whereMap) {
        return advBaseOptPxy.bUpdateByCondition(tableName, rowData, whereMap);
    }

    @Override
    public Integer bUpdateBatchByPrimaryKey(
            String tableName, String idKey, List<Map<String, Object>> rowsData) {
        return advBaseOptPxy.bUpdateBatchByPrimaryKey(tableName, idKey, rowsData);
    }

    @Override
    public Integer bUpdateBatchWithBatchSize(
            String tableName, String idKey, List<Map<String, Object>> rowsData, int batchSize) {
        return advBaseOptPxy.bUpdateBatchWithBatchSize(tableName, idKey, rowsData, batchSize);
    }

    @Override
    public <T> Integer bUpdateBatchByPrimaryKey(
            String tableName, String idKey, Collection<T> entities) {
        return advBaseOptPxy.bUpdateBatchByPrimaryKey(tableName, idKey, entities);
    }

    @Override
    public Integer bUpdateWithOptimisticLock(
            String tableName,
            String idKey,
            Object id,
            Map<String, Object> rowData,
            String versionKey,
            Integer version) {
        return advBaseOptPxy.bUpdateWithOptimisticLock(
                tableName, idKey, id, rowData, versionKey, version);
    }

    @Override
    public Integer bUpdateOrInsert(
            String tableName, Map<String, Object> rowData, Set<String> conflictKeys) {
        return advBaseOptPxy.bUpdateOrInsert(tableName, rowData, conflictKeys);
    }

    // ==================== DDL操作（代理调用PgAdvDDLOpt） ====================
    @Override
    public int dExecuteDDL(String sql, String tableName, String operation) {
        return advDDLOptPxy.dExecuteDDL(sql, tableName, operation);
    }

    @Override
    public int dExecuteDDL(
            String   dynamicSql, SqlParamMap sqlParam, String tableName, String operation) {
        return advDDLOptPxy.dExecuteDDL(dynamicSql, sqlParam, tableName, operation);
    }

    @Override
    public void dDelTable(String tableName) {
        advDDLOptPxy.dDelTable(tableName);
    }

    @Override
    public void dTruncateTable(String tableName) {
        advDDLOptPxy.dTruncateTable(tableName);
    }

    @Override
    public void dDropTable(String tableName) {
        advDDLOptPxy.dDropTable(tableName);
    }

    @Override
    public String dGetCurrentSchema() {
        return advDDLOptPxy.dGetCurrentSchema();
    }

    @Override
    public String dGetCurrentDataBase() {
        return advDDLOptPxy.dGetCurrentDataBase();
    }

    @Override
    public List<String> dGetAllSchemas() {
        return advDDLOptPxy.dGetAllSchemas();
    }

    @Override
    public String dGetTableComment(String tableName) {
        return advDDLOptPxy.dGetTableComment(tableName);
    }

    @Override
    public DataFieldsApo dGetColumnsByTable(String tableName) {
        return advDDLOptPxy.dGetColumnsByTable(tableName);
    }

    @Override
    public DataFieldsApo dGetColumnsBySQL(String sqlView) {
        return advDDLOptPxy.dGetColumnsBySQL(sqlView);
    }

    @Override
    public DataFieldsApo dGetColumnsBySQL(String   dynamicSql, SqlParamMap sqlParam) {
        return advDDLOptPxy.dGetColumnsBySQL(dynamicSql, sqlParam);
    }

    @Override
    public DataFieldsApo dGetColumnsBySQLOrTable(String tbNameOrSql) {
        return advDDLOptPxy.dGetColumnsBySQLOrTable(tbNameOrSql);
    }

    @Override
    public void dCreateTable(String tableName, List<FieldBySchemaApo> fields, String primaryKey) {
        advDDLOptPxy.dCreateTable(tableName, fields, primaryKey);
    }

    @Override
    public void dRenameTable(String oldTableName, String newTableName) {
        advDDLOptPxy.dRenameTable(oldTableName, newTableName);
    }

    @Override
    public void dAddColumn(String tableName, FieldBySchemaApo field) {
        advDDLOptPxy.dAddColumn(tableName, field);
    }

    @Override
    public void dAlterColumn(String tableName, String oldColumnName, FieldBySchemaApo newField) {
        advDDLOptPxy.dAlterColumn(tableName, oldColumnName, newField);
    }

    @Override
    public void dDropColumn(String tableName, String columnName) {
        advDDLOptPxy.dDropColumn(tableName, columnName);
    }

    @Override
    public List<String> dGetTablesBySchema(String schemaName) {
        return advDDLOptPxy.dGetTablesBySchema(schemaName);
    }

    @Override
    public List<String> dGetTablesBySchema() {
        return advDDLOptPxy.dGetTablesBySchema();
    }

    @Override
    public List<SchemaTableApo> dGetTableAndViewBySchema(String schemaName) {
        return advDDLOptPxy.dGetTableAndViewBySchema(schemaName);
    }

    @Override
    public List<SchemaTableApo> dGetTableAndViewBySchema() {
        return advDDLOptPxy.dGetTableAndViewBySchema();
    }

    @Override
    public boolean dIsTableExists(String tableName) {
        return advDDLOptPxy.dIsTableExists(tableName);
    }

    @Override
    public boolean dIsFunctionExists(String functionName) {
        return advDDLOptPxy.dIsFunctionExists(functionName);
    }

    @Override
    public void dCreateSchema(String schemaName) {
        advDDLOptPxy.dCreateSchema(schemaName);
    }

    @Override
    public void dDropSchema(String schemaName, boolean cascade) {
        advDDLOptPxy.dDropSchema(schemaName, cascade);
    }

    @Override
    public void dAddPrimaryKey(String tableName, List<String> columnNames, String constraintName) {
        advDDLOptPxy.dAddPrimaryKey(tableName, columnNames, constraintName);
    }

    @Override
    public void dAddPrimaryKey(
            String tableName,
            String pkColumnName,
            String constraintName,
            PrimaryKeyType pkType,
            Integer pkColumnLength,
            String pkValuePrefix) {
        advDDLOptPxy.dAddPrimaryKey(
                tableName, pkColumnName, constraintName, pkType, pkColumnLength, pkValuePrefix);
    }

    @Override
    public void dAddStringPrimaryKey(
            String tableName,
            String pkColumnName,
            int pkColumnLength,
            String constraintName,
            String pkValuePrefix) {
        advDDLOptPxy.dAddStringPrimaryKey(
                tableName, pkColumnName, pkColumnLength, constraintName, pkValuePrefix);
    }

    @Override
    public void dAddIntAutoPrimaryKey(
            String tableName, String pkColumnName, String constraintName) {
        advDDLOptPxy.dAddIntAutoPrimaryKey(tableName, pkColumnName, constraintName);
    }

    @Override
    public void dAddBigIntAutoPrimaryKey(
            String tableName, String pkColumnName, String constraintName) {
        advDDLOptPxy.dAddBigIntAutoPrimaryKey(tableName, pkColumnName, constraintName);
    }

    @Override
    public void dAddIntNormalPrimaryKey(
            String tableName, String pkColumnName, String constraintName) {
        advDDLOptPxy.dAddIntNormalPrimaryKey(tableName, pkColumnName, constraintName);
    }

    @Override
    public void dAddBigIntNormalPrimaryKey(
            String tableName, String pkColumnName, String constraintName) {
        advDDLOptPxy.dAddBigIntNormalPrimaryKey(tableName, pkColumnName, constraintName);
    }

    @Override
    public void dDropPrimaryKey(String tableName, String constraintName) {
        advDDLOptPxy.dDropPrimaryKey(tableName, constraintName);
    }

    @Override
    public void dCreateIndex(
            String tableName, String indexName, List<String> columnNames, boolean isUnique) {
        advDDLOptPxy.dCreateIndex(tableName, indexName, columnNames, isUnique);
    }

    @Override
    public void dDropIndex(String tableName, String indexName) {
        advDDLOptPxy.dDropIndex(tableName, indexName);
    }

    @Override
    public List<String> dGetPrimaryKeys(String tableName) {
        return advDDLOptPxy.dGetPrimaryKeys(tableName);
    }

    @Override
    public List<IndexApo> dGetIndexes(String tableName) {
        return advDDLOptPxy.dGetIndexes(tableName);
    }

    @Override
    public boolean dIndexesExists(String tableName, String indexName) {
        return advDDLOptPxy.dIndexesExists(tableName, indexName);
    }

    @Override
    public String dGetTableSizeFormat(String tableName) {
        return advDDLOptPxy.dGetTableSizeFormat(tableName);
    }

    @Override
    public Long dGetTableSize(String tableName) {
        return advDDLOptPxy.dGetTableSize(tableName);
    }

    // ==================== 空间几何操作（代理调用PgAdvGeoOpt） ====================
    @Override
    public GirAdvOneRow eSelectOne(String sql, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return advGeoOptPxy.eSelectOne(sql, advEnumsGeomOpt);
    }

    @Override
    public GirAdvOneRow eSelectOne(
            String sql, AdvEnumsGeomOpt advEnumsGeomOpt, String geomFieldName) {
        return advGeoOptPxy.eSelectOne(sql, advEnumsGeomOpt, geomFieldName);
    }

    @Override
    public GirAdvOneRow eSelectOne(
            String sql, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList) {
        return advGeoOptPxy.eSelectOne(sql, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(String sql, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return advGeoOptPxy.eSelectList(sql, advEnumsGeomOpt);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String sql, AdvEnumsGeomOpt advEnumsGeomOpt, String geomFieldName) {
        return advGeoOptPxy.eSelectList(sql, advEnumsGeomOpt, geomFieldName);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String sql, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList) {
        return advGeoOptPxy.eSelectList(sql, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public List<String> eGetAllGeoLayerName() {
        return advGeoOptPxy.eGetAllGeoLayerName();
    }

    @Override
    public boolean eIsGeomByTable(String tableName) {
        return advGeoOptPxy.eIsGeomByTable(tableName);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeByTable(String tableName) {
        return advGeoOptPxy.eGetGeoTypeByTable(tableName);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeByTable(String tableName, String geomFieldName) {
        return advGeoOptPxy.eGetGeoTypeByTable(tableName, geomFieldName);
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeByTable(
            String tableName, List<String> geomFieldNames) {
        return advGeoOptPxy.eGetGeoTypeByTable(tableName, geomFieldNames);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlView) {
        return advGeoOptPxy.eGetGeoTypeBySql(sqlView);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlView, String geomFieldName) {
        return advGeoOptPxy.eGetGeoTypeBySql(sqlView, geomFieldName);
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(
            String sqlView, List<String> geomFieldNames) {
        return advGeoOptPxy.eGetGeoTypeBySql(sqlView, geomFieldNames);
    }

    @Override
    public boolean eIsGeomBySql(String sqlView) {
        return advGeoOptPxy.eIsGeomBySql(sqlView);
    }

    @Override
    public String eGetGeomColumnNameByTable(String tableName) {
        return advGeoOptPxy.eGetGeomColumnNameByTable(tableName);
    }

    @Override
    public List<String> eGetGeomColumnNameListByTable(String tableName) {
        return advGeoOptPxy.eGetGeomColumnNameListByTable(tableName);
    }

    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListByTable(String tableName) {
        return advGeoOptPxy.eGetGeomColumnListByTable(tableName);
    }

    @Override
    public FieldBySchemaApo eGetGeomColumnByTable(String tableName) {
        return advGeoOptPxy.eGetGeomColumnByTable(tableName);
    }

    @Override
    public String eGetGeomColumnNameBySql(String sqlView) {
        return advGeoOptPxy.eGetGeomColumnNameBySql(sqlView);
    }

    @Override
    public List<String> eGetGeomColumnNameListBySql(String sqlView) {
        return advGeoOptPxy.eGetGeomColumnNameListBySql(sqlView);
    }

    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListBySql(String sqlView) {
        return advGeoOptPxy.eGetGeomColumnListBySql(sqlView);
    }

    @Override
    public FieldBySchemaApo eGetGeomColumnBySql(String sqlView) {
        return advGeoOptPxy.eGetGeomColumnBySql(sqlView);
    }

    @Override
    public boolean eIsPointTable(String tableName) {
        return advGeoOptPxy.eIsPointTable(tableName);
    }

    @Override
    public boolean eIsPolygonTable(String tableName) {
        return advGeoOptPxy.eIsPolygonTable(tableName);
    }

    @Override
    public boolean eIsLineStringTable(String tableName) {
        return advGeoOptPxy.eIsLineStringTable(tableName);
    }

    @Override
    public void eAddGeomColumn(
            String tableName, String geomFieldName, AdvEnumsTypeGeom geomType, int srid) {
        advGeoOptPxy.eAddGeomColumn(tableName, geomFieldName, geomType, srid);
    }

    @Override
    public void eDropGeomColumn(String tableName, String geomFieldName) {
        advGeoOptPxy.eDropGeomColumn(tableName, geomFieldName);
    }

    @Override
    public void eDropGeomColumn(String tableName) {
        advGeoOptPxy.eDropGeomColumn(tableName);
    }

    @Override
    public void eTransformSrid(String tableName, String geomFieldName, int targetSrid) {
        advGeoOptPxy.eTransformSrid(tableName, geomFieldName, targetSrid);
    }

    @Override
    public void eTransformSrid(String tableName, int targetSrid) {
        advGeoOptPxy.eTransformSrid(tableName, targetSrid);
    }

    @Override
    public Integer eGetSrid(String tableNameOrSqlView, String geomFieldName) {
        return advGeoOptPxy.eGetSrid(tableNameOrSqlView, geomFieldName);
    }

    @Override
    public Integer eGetSrid(String tableNameOrSqlView) {
        return advGeoOptPxy.eGetSrid(tableNameOrSqlView);
    }

    @Override
    public Map<String, Integer> eGetSrid(String tableNameOrSqlView, List<String> geomFieldNames) {
        return advGeoOptPxy.eGetSrid(tableNameOrSqlView, geomFieldNames);
    }

    @Override
    public void eCreateSpatialIndex(String tableName, String geomFieldName, String indexName) {
        advGeoOptPxy.eCreateSpatialIndex(tableName, geomFieldName, indexName);
    }

    @Override
    public void eCreateSpatialIndex(String tableName, String indexName) {
        advGeoOptPxy.eCreateSpatialIndex(tableName, indexName);
    }

    @Override
    public void eDropSpatialIndex(String tableName, String indexName) {
        advGeoOptPxy.eDropSpatialIndex(tableName, indexName);
    }

    @Override
    public List<GirAdvOneRow> eQueryIntersects(
            String tableName, String geomFieldName, String geometry, int srid) {
        return advGeoOptPxy.eQueryIntersects(tableName, geomFieldName, geometry, srid);
    }

    @Override
    public List<GirAdvOneRow> eQueryIntersects(String tableName, String geometry, int srid) {
        return advGeoOptPxy.eQueryIntersects(tableName, geometry, srid);
    }

    @Override
    public List<GirAdvOneRow> eQueryWithinBBox(
            String tableName, String geomFieldName, double[] bbox, int srid) {
        return advGeoOptPxy.eQueryWithinBBox(tableName, geomFieldName, bbox, srid);
    }

    @Override
    public List<GirAdvOneRow> eQueryWithinBBox(String tableName, double[] bbox, int srid) {
        return advGeoOptPxy.eQueryWithinBBox(tableName, bbox, srid);
    }

    @Override
    public List<GirAdvOneRow> eCalculateDistance(
            String tableName,
            String geomFieldName,
            String geometry,
            int srid,
            String distanceAlias) {
        return advGeoOptPxy.eCalculateDistance(
                tableName, geomFieldName, geometry, srid, distanceAlias);
    }

    @Override
    public List<GirAdvOneRow> eCalculateDistance(
            String tableName, String geometry, int srid, String distanceAlias) {
        return advGeoOptPxy.eCalculateDistance(tableName, geometry, srid, distanceAlias);
    }

    @Override
    public List<GirAdvOneRow> eGetCentroid(
            String tableNameOrSqlView, String geomFieldName, String centerAlias) {
        return advGeoOptPxy.eGetCentroid(tableNameOrSqlView, geomFieldName, centerAlias);
    }

    @Override
    public List<GirAdvOneRow> eGetCentroid(String tableNameOrSqlView, String centerAlias) {
        return advGeoOptPxy.eGetCentroid(tableNameOrSqlView, centerAlias);
    }

    @Override
    public List<Object> eValidateGeometries(String tableName, String geomFieldName) {
        return advGeoOptPxy.eValidateGeometries(tableName, geomFieldName);
    }

    @Override
    public List<Object> eValidateGeometries(String tableName) {
        return advGeoOptPxy.eValidateGeometries(tableName);
    }

    @Override
    public int eRepairGeometries(String tableName, String geomFieldName) {
        return advGeoOptPxy.eRepairGeometries(tableName, geomFieldName);
    }

    @Override
    public int eRepairGeometries(String tableName) {
        return advGeoOptPxy.eRepairGeometries(tableName);
    }

    @Override
    public BBoxApo eGetExtent(String tableNameOrSqlView, String geomFieldName) {
        return advGeoOptPxy.eGetExtent(tableNameOrSqlView, geomFieldName);
    }

    @Override
    public BBoxApo eGetExtent(String tableNameOrSqlView) {
        return advGeoOptPxy.eGetExtent(tableNameOrSqlView);
    }

    // ==================== 分页操作（代理调用PgAdvSimplePageOpt） ====================
    @Override
    public Long pCount(String noPageSql) {
        return advSimplePageOptPxy.pCount(noPageSql);
    }

    @Override
    public String pBuildPageSql(
            String noPageSql, int pageSize, int pageNum, boolean pageNumStartZero) {
        return advSimplePageOptPxy.pBuildPageSql(noPageSql, pageSize, pageNum, pageNumStartZero);
    }

    @Override
    public String pBuildSqlWithOrder(String baseSql, List<OrderApo> orders, String tableAlias) {
        return advSimplePageOptPxy.pBuildSqlWithOrder(baseSql, orders, tableAlias);
    }

    @Override
    public String pBuildSqlWithOrder(String baseSql, List<OrderApo> orders) {
        return advSimplePageOptPxy.pBuildSqlWithOrder(baseSql, orders);
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
        return advSimplePageOptPxy.pPage(
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
        return advSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return advSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, advEnumsGeomOpt);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql, int pageNum, int pageSize, List<OrderApo> orders) {
        return advSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            boolean hasFieldsInfo) {
        return advSimplePageOptPxy.pPage(
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
        return advSimplePageOptPxy.pPage(
                noPageSql, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, hasFieldsInfo);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero) {
        return advSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, pageNumStartZero);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            List<OrderApo> orders) {
        return advSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, pageNumStartZero, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt) {
        return advSimplePageOptPxy.pPage(
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
        return advSimplePageOptPxy.pPage(
                noPageSql, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo) {
        return advSimplePageOptPxy.pPage(
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
        return advSimplePageOptPxy.pPage(
                noPageSql, pageNum, pageSize, advEnumsGeomOpt, hasFieldsInfo, orders);
    }

    @Override
    public String tbGetSchemaNameForSql(IDataSourceGetter dataSourceGetter) {
        return dialectTableNameProcessorPxy.tbGetSchemaNameForSql(dataSourceGetter);
    }

    @Override
    public String tbGetTableNameWithSchema(IDataSourceGetter dataSourceGetter, String tableName) {
        // 代理调用：带默认Schema的完整表名拼接
        return dialectTableNameProcessorPxy.tbGetTableNameWithSchema(dataSourceGetter, tableName);
    }


    @Override
    public String tbGetTableNameWithSchema(
            IDataSourceGetter dataSourceGetter, String tableName, String schemaName) {
        // 代理调用：带指定Schema的完整表名拼接
        return dialectTableNameProcessorPxy.tbGetTableNameWithSchema(
                dataSourceGetter, tableName, schemaName);
    }

    @Override
    public String tbGetTableNameNotSchema(String fullTableName) {
        // 代理调用：从完整表名提取纯表名（去除Schema前缀）
        return dialectTableNameProcessorPxy.tbGetTableNameNotSchema(fullTableName);
    }

    @Override
    public String tbExtractSchemaName(String fullTableName) {
        // 代理调用：从完整表名提取Schema名称
        return dialectTableNameProcessorPxy.tbExtractSchemaName(fullTableName);
    }

    @Override
    public String tbQuoteTableName(String tableName) {
        // 代理调用：给表名添加PostgreSQL的标识符引号（双引号）
        return dialectTableNameProcessorPxy.tbQuoteTableName(tableName);
    }

    @Override
    public String tbQuoteSchemaName(String schemaName) {
        // 代理调用：给Schema名称添加PostgreSQL的标识符引号（双引号）
        return dialectTableNameProcessorPxy.tbQuoteSchemaName(schemaName);
    }

    @Override
    public String tbUnquoteTableName(String quotedTableName) {
        // 代理调用：移除表名的标识符引号
        return dialectTableNameProcessorPxy.tbUnquoteTableName(quotedTableName);
    }

    @Override
    public String tbUnquoteSchemaName(String quotedSchemaName) {
        // 代理调用：移除Schema名称的标识符引号
        return dialectTableNameProcessorPxy.tbUnquoteSchemaName(quotedSchemaName);
    }

    @Override
    public String tbQuoteFieldName(String fieldName) {
        return dialectTableNameProcessorPxy.tbQuoteFieldName(fieldName);
    }

    @Override
    public boolean tbTableIsSqlView(String tableName) {
        // 代理调用：判断表名对应的对象是否是SQL视图
        return dialectTableNameProcessorPxy.tbTableIsSqlView(tableName);
    }

    @Override
    public String tbGetTempAliasTableName() {
        // 代理调用：生成临时表别名（如t_temp_xxx）
        return dialectTableNameProcessorPxy.tbGetTempAliasTableName();
    }

    @Override
    public String tbRemoveSqlSpaces(String sqlView) {
        // 代理调用：移除SQL语句中的多余空格（保留语法结构）
        return dialectTableNameProcessorPxy.tbRemoveSqlSpaces(sqlView);
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
        return dialectTableNameProcessorPxy.tbBuildPageSql(noPageSql, pageSize, offset);
    }

    @Override
    public String tbBuildPageSql(String noPageSql) {
        return dialectTableNameProcessorPxy.tbBuildPageSql(noPageSql);
    }


    @Override
    public GirAdvOneRow eSelectOne(
            String   dynamicSql, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return advGeoOptPxy.eSelectOne(dynamicSql, sqlParam, advEnumsGeomOpt);
    }

    @Override
    public GirAdvOneRow eSelectOne(
            String sqlStatement,
            SqlParamMap sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            String geomFieldName) {
        return advGeoOptPxy.eSelectOne(sqlStatement, sqlParam, advEnumsGeomOpt, geomFieldName);
    }

    @Override
    public GirAdvOneRow eSelectOne(
            String sqlStatement,
            SqlParamMap sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            List<String> geomFieldNameList) {
        return advGeoOptPxy.eSelectOne(sqlStatement, sqlParam, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String   dynamicSql, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return advGeoOptPxy.eSelectList(dynamicSql, sqlParam, advEnumsGeomOpt);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String sqlStatement,
            SqlParamMap sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            String geomFieldName) {
        return advGeoOptPxy.eSelectList(sqlStatement, sqlParam, advEnumsGeomOpt, geomFieldName);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(
            String sqlStatement,
            SqlParamMap sqlParam,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            List<String> geomFieldNameList) {
        return advGeoOptPxy.eSelectList(sqlStatement, sqlParam, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(String   dynamicSql, SqlParamMap sqlParam) {
        return advGeoOptPxy.eGetGeoTypeBySql(dynamicSql, sqlParam);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(
            String   dynamicSql, SqlParamMap sqlParam, String geomFieldName) {
        return advGeoOptPxy.eGetGeoTypeBySql(dynamicSql, sqlParam, geomFieldName);
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(
            String   dynamicSql, SqlParamMap sqlParam, List<String> geomFieldNames) {
        return advGeoOptPxy.eGetGeoTypeBySql(dynamicSql, sqlParam, geomFieldNames);
    }

    @Override
    public boolean eIsGeomBySql(String   dynamicSql, SqlParamMap sqlParam) {
        return advGeoOptPxy.eIsGeomBySql(dynamicSql, sqlParam);
    }

    @Override
    public String eGetGeomColumnNameBySql(String   dynamicSql, SqlParamMap sqlParam) {
        return advGeoOptPxy.eGetGeomColumnNameBySql(dynamicSql, sqlParam);
    }

    @Override
    public List<String> eGetGeomColumnNameListBySql(String   dynamicSql, SqlParamMap sqlParam) {
        return advGeoOptPxy.eGetGeomColumnNameListBySql(dynamicSql, sqlParam);
    }

    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListBySql(
            String   dynamicSql, SqlParamMap sqlParam) {
        return advGeoOptPxy.eGetGeomColumnListBySql(dynamicSql, sqlParam);
    }

    @Override
    public FieldBySchemaApo eGetGeomColumnBySql(String   dynamicSql, SqlParamMap sqlParam) {
        return advGeoOptPxy.eGetGeomColumnBySql(dynamicSql, sqlParam);
    }

    @Override
    public Long pCount(String noPageSqlStatement, SqlParamMap sqlParam) {
        return advSimplePageOptPxy.pCount(noPageSqlStatement, sqlParam);
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
        return advSimplePageOptPxy.pPage(
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
        return advSimplePageOptPxy.pPage(noPageSqlStatement, sqlParam, pageNum, pageSize);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            SqlParamMap sqlParam,
            int pageNum,
            int pageSize,
            AdvEnumsGeomOpt advEnumsGeomOpt) {
        return advSimplePageOptPxy.pPage(
                noPageSqlStatement, sqlParam, pageNum, pageSize, advEnumsGeomOpt);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            SqlParamMap sqlParam,
            int pageNum,
            int pageSize,
            List<OrderApo> orders) {
        return advSimplePageOptPxy.pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            SqlParamMap sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            boolean hasFieldsInfo) {
        return advSimplePageOptPxy.pPage(
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
        return advSimplePageOptPxy.pPage(
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
        return advSimplePageOptPxy.pPage(
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
        return advSimplePageOptPxy.pPage(
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
        return advSimplePageOptPxy.pPage(
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
        return advSimplePageOptPxy.pPage(
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
        return advSimplePageOptPxy.pPage(
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
        return advSimplePageOptPxy.pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                advEnumsGeomOpt,
                hasFieldsInfo,
                orders);
    }
}
