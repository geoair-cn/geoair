package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.apo.*;
import cn.geoair.map.dynamic.adv.query.dialect.pgv2.*;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.ds.DataSourceGetter;
import cn.geoair.map.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.ds.apo.DataSourceApo;
import org.geotools.data.DataStore;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * PostgreSQL数据库的动态高级查询执行器
 * <p>
 * 聚合了基础操作、DDL操作、空间几何操作、分页操作的实现，
 * 对外提供统一的执行入口，支持多构造器初始化（DataSourceApo/DataSource/Connection）
 *
 * @author 张逢吉
 * @date 2025/10/9 11:10
 */
public abstract class AbstractAdvExecutor implements IAdvExecutor {

    // 数据源获取器（核心依赖）
    private DataSourceGetter dataSourceGetterPxy;

    public DataSourceGetter getDataSourceGetterPxy() {
        if (dataSourceGetterPxy == null) {
            dataSourceGetterPxy = new DataSourceGetter();
        }
        return dataSourceGetterPxy;
    }

    // 各功能模块代理对象
    private IAdvBaseOpt pgAdvBaseOptPxy;
    private IAdvDDLOpt pgAdvDDLOptPxy;
    private IAdvGeoPreOpt pgAdvGeoOptPxy;
    private IAdvSimplePagePreOpt pgAdvSimplePageOptPxy;
    DialectTableNameProcessor dialectTableNameProcessorPxy = PgDialectTableNameUtil.getInstance();

    protected abstract IAdvBaseOpt getAdvBaseOpt();

    protected abstract IAdvDDLOpt getAdvDDLOpt();

    protected abstract IAdvSimplePagePreOpt getSimplePageOpt();

    protected abstract IAdvGeoPreOpt getGeoOpt();

    protected abstract DialectTableNameProcessor getDialectTableNameProcessor();


    public AbstractAdvExecutor(DataSourceApo dataSourceApo) {
        this.initByDataSourceApo(dataSourceApo);
    }

    public AbstractAdvExecutor(DataSource dataSource) {
        this.initByDataSource(dataSource);
    }

    public AbstractAdvExecutor() {
    }

    public AbstractAdvExecutor(Connection connection) {
        this.initByConnection(connection);
    }

    /**
     * 初始化所有功能模块代理对象
     */
    private void initProxyObjects() {
        this.pgAdvBaseOptPxy = new PgAdvBaseOpt(this);
        this.pgAdvDDLOptPxy = new PgAdvDDLOpt(this);
        this.pgAdvGeoOptPxy = new PgAdvGeoOpt(this);
        this.pgAdvSimplePageOptPxy = new PgAdvSimplePageOpt(this);
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
        if (dataSource == null) {
            throw new IllegalArgumentException("DataSource 不能为空");
        }
        this.getDataSourceGetterPxy().initByDataSource(dataSource);
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

    @Override
    public DataStore getGeoToolsDataStore() {
        return this.getDataSourceGetterPxy().getGeoToolsDataStore();
    }

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
        return pgAdvBaseOptPxy.bInsertBySql(sqlStatement);
    }

    @Override
    public Integer bInsertBySql(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvBaseOptPxy.bInsertBySql(sqlStatement, sqlParam);
    }

    @Override
    public Integer bInsertOne(String tableName, Map<String, Object> rowData) {
        return pgAdvBaseOptPxy.bInsertOne(tableName, rowData);
    }

    @Override
    public <T> Integer bInsertOne(String tableName, T entity) {
        return pgAdvBaseOptPxy.bInsertOne(tableName, entity);
    }

    @Override
    public Long bInsertOneReturnId(String tableName, Map<String, Object> rowData) {
        return pgAdvBaseOptPxy.bInsertOneReturnId(tableName, rowData);
    }

    @Override
    public <T> Long bInsertOneReturnId(String tableName, T entity) {
        return pgAdvBaseOptPxy.bInsertOneReturnId(tableName, entity);
    }

    @Override
    public Integer bInsertBatch(String tableName, Set<String> headers, List<Map<String, Object>> rowsData) {
        return pgAdvBaseOptPxy.bInsertBatch(tableName, headers, rowsData);
    }

    @Override
    public <T> Integer bInsertBatch(String tableName, Collection<T> entities) {
        return pgAdvBaseOptPxy.bInsertBatch(tableName, entities);
    }

    @Override
    public Integer bInsertBatchWithBatchSize(String tableName, Set<String> headers, List<Map<String, Object>> rowsData, int batchSize) {
        return pgAdvBaseOptPxy.bInsertBatchWithBatchSize(tableName, headers, rowsData, batchSize);
    }

    @Override
    public <T> Integer bInsertBatchWithBatchSize(String tableName, Collection<T> entities, int batchSize) {
        return pgAdvBaseOptPxy.bInsertBatchWithBatchSize(tableName, entities, batchSize);
    }

    @Override
    public Integer bInsertIgnore(String tableName, Map<String, Object> rowData) {
        return pgAdvBaseOptPxy.bInsertIgnore(tableName, rowData);
    }

    @Override
    public Integer bInsertIgnoreBatch(String tableName, Set<String> headers, List<Map<String, Object>> rowsData) {
        return pgAdvBaseOptPxy.bInsertIgnoreBatch(tableName, headers, rowsData);
    }

    @Override
    public Integer bInsertOrUpdate(String tableName, Map<String, Object> rowData, Set<String> updateFields) {
        return pgAdvBaseOptPxy.bInsertOrUpdate(tableName, rowData, updateFields);
    }

    // ==================== 基础删除操作（代理调用PgAdvBaseOpt） ====================
    @Override
    public Integer bDeleteBySql(String sqlStatement) {
        return pgAdvBaseOptPxy.bDeleteBySql(sqlStatement);
    }

    @Override
    public Integer bDeleteBySql(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvBaseOptPxy.bDeleteBySql(sqlStatement, sqlParam);
    }

    @Override
    public Integer bDeleteByPrimaryKey(String tableName, String idKey, Object id) {
        return pgAdvBaseOptPxy.bDeleteByPrimaryKey(tableName, idKey, id);
    }

    @Override
    public Integer bDeleteBatchByPrimaryKey(String tableName, String idKey, Set<Object> ids) {
        return pgAdvBaseOptPxy.bDeleteBatchByPrimaryKey(tableName, idKey, ids);
    }

    @Override
    public Integer bDeleteBatchWithBatchSize(String tableName, String idKey, Set<Object> ids, int batchSize) {
        return pgAdvBaseOptPxy.bDeleteBatchWithBatchSize(tableName, idKey, ids, batchSize);
    }

    @Override
    public Integer bDeleteByCondition(String tableName, Map<String, Object> whereMap) {
        return pgAdvBaseOptPxy.bDeleteByCondition(tableName, whereMap);
    }

    @Override
    public Integer bDeleteBatchByCondition(String tableName, Map<String, Object> whereMap, int batchSize) {
        return pgAdvBaseOptPxy.bDeleteBatchByCondition(tableName, whereMap, batchSize);
    }

    @Override
    public Integer bLogicDelete(String tableName, String idKey, Object id, String deleteKey, Object deleteValue) {
        return pgAdvBaseOptPxy.bLogicDelete(tableName, idKey, id, deleteKey, deleteValue);
    }

    @Override
    public Integer bLogicDeleteBatch(String tableName, String idKey, Set<Object> ids, String deleteKey, Object deleteValue) {
        return pgAdvBaseOptPxy.bLogicDeleteBatch(tableName, idKey, ids, deleteKey, deleteValue);
    }

    @Override
    public Integer bSafeDeleteByCondition(String tableName, Map<String, Object> whereMap, int maxDelete) {
        return pgAdvBaseOptPxy.bSafeDeleteByCondition(tableName, whereMap, maxDelete);
    }

    @Override
    public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {

    }

    // ==================== 基础查询操作（代理调用PgAdvBaseOpt） ====================
    @Override
    public GirAdvOneRow bSelectOne(String sql) {
        return pgAdvBaseOptPxy.bSelectOne(sql);
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sql) {
        return pgAdvBaseOptPxy.bSelectList(sql);
    }

    @Override
    public void bSelectList(String sql, Consumer<GirAdvOneRow> rowConsumer) {
        pgAdvBaseOptPxy.bSelectList(sql, rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sql) {
        return pgAdvBaseOptPxy.bSelectListToValueList(sql);
    }

    @Override
    public Number bSelectNumber(String sql) {
        return pgAdvBaseOptPxy.bSelectNumber(sql);
    }

    @Override
    public Number bSelectRecordRowCount(String sql) {
        return pgAdvBaseOptPxy.bSelectRecordRowCount(sql);
    }

    @Override
    public <E> E bSelectObjOne(String sql, Class<E> clazz) {
        return pgAdvBaseOptPxy.bSelectObjOne(sql, clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String sql, Class<E> clazz) {
        return pgAdvBaseOptPxy.bSelectObjList(sql, clazz);
    }

    @Override
    public <E> void bSelectObjList(String sql, Class<E> clazz, Consumer<E> rowConsumer) {
        pgAdvBaseOptPxy.bSelectObjList(sql, clazz, rowConsumer);
    }

    @Override
    public GirAdvOneRow bSelectOne(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvBaseOptPxy.bSelectOne(sqlStatement, sqlParam);
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvBaseOptPxy.bSelectList(sqlStatement, sqlParam);
    }

    @Override
    public void bSelectList(String sqlStatement, SqlParamMap sqlParam, Consumer<GirAdvOneRow> rowConsumer) {
        pgAdvBaseOptPxy.bSelectList(sqlStatement, sqlParam, rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvBaseOptPxy.bSelectListToValueList(sqlStatement, sqlParam);
    }

    @Override
    public Number bSelectNumber(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvBaseOptPxy.bSelectNumber(sqlStatement, sqlParam);
    }

    @Override
    public Number bSelectRecordRowCount(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvBaseOptPxy.bSelectRecordRowCount(sqlStatement, sqlParam);
    }

    @Override
    public <E> E bSelectObjOne(String sqlStatement, SqlParamMap sqlParam, Class<E> clazz) {
        return pgAdvBaseOptPxy.bSelectObjOne(sqlStatement, sqlParam, clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String sqlStatement, SqlParamMap sqlParam, Class<E> clazz) {
        return pgAdvBaseOptPxy.bSelectObjList(sqlStatement, sqlParam, clazz);
    }

    @Override
    public <E> void bSelectObjList(String sqlStatement, SqlParamMap sqlParam, Class<E> clazz, Consumer<E> rowConsumer) {
        pgAdvBaseOptPxy.bSelectObjList(sqlStatement, sqlParam, clazz, rowConsumer);
    }

    // ==================== 基础更新操作（代理调用PgAdvBaseOpt） ====================
    @Override
    public Integer bUpdateBySql(String sqlStatement) {
        return pgAdvBaseOptPxy.bUpdateBySql(sqlStatement);
    }

    @Override
    public Integer bUpdateBySql(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvBaseOptPxy.bUpdateBySql(sqlStatement, sqlParam);
    }

    @Override
    public Integer bUpdateByPrimaryKey(String tableName, String idKey, Object id, Map<String, Object> rowData) {
        return pgAdvBaseOptPxy.bUpdateByPrimaryKey(tableName, idKey, id, rowData);
    }

    @Override
    public <T> Integer bUpdateByPrimaryKey(String tableName, String idKey, T entity) {
        return pgAdvBaseOptPxy.bUpdateByPrimaryKey(tableName, idKey, entity);
    }

    @Override
    public Integer bUpdateByCondition(String tableName, Map<String, Object> rowData, Map<String, Object> whereMap) {
        return pgAdvBaseOptPxy.bUpdateByCondition(tableName, rowData, whereMap);
    }

    @Override
    public Integer bUpdateBatchByPrimaryKey(String tableName, String idKey, List<Map<String, Object>> rowsData) {
        return pgAdvBaseOptPxy.bUpdateBatchByPrimaryKey(tableName, idKey, rowsData);
    }

    @Override
    public Integer bUpdateBatchWithBatchSize(String tableName, String idKey, List<Map<String, Object>> rowsData, int batchSize) {
        return pgAdvBaseOptPxy.bUpdateBatchWithBatchSize(tableName, idKey, rowsData, batchSize);
    }

    @Override
    public <T> Integer bUpdateBatchByPrimaryKey(String tableName, String idKey, Collection<T> entities) {
        return pgAdvBaseOptPxy.bUpdateBatchByPrimaryKey(tableName, idKey, entities);
    }

    @Override
    public Integer bUpdateWithOptimisticLock(String tableName, String idKey, Object id, Map<String, Object> rowData, String versionKey, Integer version) {
        return pgAdvBaseOptPxy.bUpdateWithOptimisticLock(tableName, idKey, id, rowData, versionKey, version);
    }

    @Override
    public Integer bUpdateOrInsert(String tableName, Map<String, Object> rowData, Set<String> conflictKeys) {
        return pgAdvBaseOptPxy.bUpdateOrInsert(tableName, rowData, conflictKeys);
    }

    // ==================== DDL操作（代理调用PgAdvDDLOpt） ====================
    @Override
    public int dExecuteDDL(String sql, String tableName, String operation) {
        return pgAdvDDLOptPxy.dExecuteDDL(sql, tableName, operation);
    }

    @Override
    public int dExecuteDDL(String sqlStatement, SqlParamMap sqlParam, String tableName, String operation) {
        return pgAdvDDLOptPxy.dExecuteDDL(sqlStatement, sqlParam, tableName, operation);
    }

    @Override
    public void dDelTable(String tableName) {
        pgAdvDDLOptPxy.dDelTable(tableName);
    }

    @Override
    public void dTruncateTable(String tableName) {
        pgAdvDDLOptPxy.dTruncateTable(tableName);
    }

    @Override
    public void dDropTable(String tableName) {
        pgAdvDDLOptPxy.dDropTable(tableName);
    }

    @Override
    public List<String> dGetAllSchemas() {
        return pgAdvDDLOptPxy.dGetAllSchemas();
    }

    @Override
    public DataFieldsApo dGetColumnsByTable(String tableName) {
        return pgAdvDDLOptPxy.dGetColumnsByTable(tableName);
    }

    @Override
    public DataFieldsApo dGetColumnsBySQL(String sqlView) {
        return pgAdvDDLOptPxy.dGetColumnsBySQL(sqlView);
    }

    @Override
    public DataFieldsApo dGetColumnsBySQL(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvDDLOptPxy.dGetColumnsBySQL(sqlStatement, sqlParam);
    }

    @Override
    public DataFieldsApo dGetColumnsBySQLOrTable(String tbNameOrSql) {
        return pgAdvDDLOptPxy.dGetColumnsBySQLOrTable(tbNameOrSql);
    }

    @Override
    public void dCreateTable(String tableName, List<FieldBySchemaApo> fields, String primaryKey) {
        pgAdvDDLOptPxy.dCreateTable(tableName, fields, primaryKey);
    }

    @Override
    public void dRenameTable(String oldTableName, String newTableName) {
        pgAdvDDLOptPxy.dRenameTable(oldTableName, newTableName);
    }

    @Override
    public void dAddColumn(String tableName, FieldBySchemaApo field) {
        pgAdvDDLOptPxy.dAddColumn(tableName, field);
    }

    @Override
    public void dAlterColumn(String tableName, String oldColumnName, FieldBySchemaApo newField) {
        pgAdvDDLOptPxy.dAlterColumn(tableName, oldColumnName, newField);
    }

    @Override
    public void dDropColumn(String tableName, String columnName) {
        pgAdvDDLOptPxy.dDropColumn(tableName, columnName);
    }

    @Override
    public List<String> dGetTablesBySchema(String schemaName) {
        return pgAdvDDLOptPxy.dGetTablesBySchema(schemaName);
    }

    @Override
    public boolean dIsTableExists(String tableName) {
        return pgAdvDDLOptPxy.dIsTableExists(tableName);
    }

    @Override
    public boolean dIsFunctionExists(String functionName) {
        return pgAdvDDLOptPxy.dIsFunctionExists(functionName);
    }

    @Override
    public void dCreateSchema(String schemaName) {
        pgAdvDDLOptPxy.dCreateSchema(schemaName);
    }

    @Override
    public void dDropSchema(String schemaName, boolean cascade) {
        pgAdvDDLOptPxy.dDropSchema(schemaName, cascade);
    }

    @Override
    public void dAddPrimaryKey(String tableName, List<String> columnNames, String constraintName) {
        pgAdvDDLOptPxy.dAddPrimaryKey(tableName, columnNames, constraintName);
    }

    @Override
    public void dDropPrimaryKey(String tableName, String constraintName) {
        pgAdvDDLOptPxy.dDropPrimaryKey(tableName, constraintName);
    }

    @Override
    public void dCreateIndex(String tableName, String indexName, List<String> columnNames, boolean isUnique) {
        pgAdvDDLOptPxy.dCreateIndex(tableName, indexName, columnNames, isUnique);
    }

    @Override
    public void dDropIndex(String tableName, String indexName) {
        pgAdvDDLOptPxy.dDropIndex(tableName, indexName);
    }

    @Override
    public List<String> dGetPrimaryKeys(String tableName) {
        return pgAdvDDLOptPxy.dGetPrimaryKeys(tableName);
    }

    @Override
    public List<IndexApo> dGetIndexes(String tableName) {
        return pgAdvDDLOptPxy.dGetIndexes(tableName);
    }

    @Override
    public boolean dIndexesExists(String tableName, String indexName) {
        return pgAdvDDLOptPxy.dIndexesExists(tableName, indexName);
    }

    @Override
    public String dGetTableSizeFormat(String tableName) {
        return pgAdvDDLOptPxy.dGetTableSizeFormat(tableName);
    }

    @Override
    public Long dGetTableSize(String tableName) {
        return pgAdvDDLOptPxy.dGetTableSize(tableName);
    }

    // ==================== 空间几何操作（代理调用PgAdvGeoOpt） ====================
    @Override
    public GirAdvOneRow eSelectOne(String sql, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return pgAdvGeoOptPxy.eSelectOne(sql, advEnumsGeomOpt);
    }

    @Override
    public GirAdvOneRow eSelectOne(String sql, AdvEnumsGeomOpt advEnumsGeomOpt, String geomFieldName) {
        return pgAdvGeoOptPxy.eSelectOne(sql, advEnumsGeomOpt, geomFieldName);
    }

    @Override
    public GirAdvOneRow eSelectOne(String sql, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList) {
        return pgAdvGeoOptPxy.eSelectOne(sql, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(String sql, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return pgAdvGeoOptPxy.eSelectList(sql, advEnumsGeomOpt);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(String sql, AdvEnumsGeomOpt advEnumsGeomOpt, String geomFieldName) {
        return pgAdvGeoOptPxy.eSelectList(sql, advEnumsGeomOpt, geomFieldName);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(String sql, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList) {
        return pgAdvGeoOptPxy.eSelectList(sql, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public List<String> eGetAllGeoLayerName() {
        return pgAdvGeoOptPxy.eGetAllGeoLayerName();
    }

    @Override
    public boolean eIsGeomByTable(String tableName) {
        return pgAdvGeoOptPxy.eIsGeomByTable(tableName);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeByTable(String tableName) {
        return pgAdvGeoOptPxy.eGetGeoTypeByTable(tableName);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeByTable(String tableName, String geomFieldName) {
        return pgAdvGeoOptPxy.eGetGeoTypeByTable(tableName, geomFieldName);
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeByTable(String tableName, List<String> geomFieldNames) {
        return pgAdvGeoOptPxy.eGetGeoTypeByTable(tableName, geomFieldNames);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlView) {
        return pgAdvGeoOptPxy.eGetGeoTypeBySql(sqlView);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlView, String geomFieldName) {
        return pgAdvGeoOptPxy.eGetGeoTypeBySql(sqlView, geomFieldName);
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(String sqlView, List<String> geomFieldNames) {
        return pgAdvGeoOptPxy.eGetGeoTypeBySql(sqlView, geomFieldNames);
    }

    @Override
    public boolean eIsGeomBySql(String sqlView) {
        return pgAdvGeoOptPxy.eIsGeomBySql(sqlView);
    }

    @Override
    public String eGetGeomColumnNameByTable(String tableName) {
        return pgAdvGeoOptPxy.eGetGeomColumnNameByTable(tableName);
    }

    @Override
    public List<String> eGetGeomColumnNameListByTable(String tableName) {
        return pgAdvGeoOptPxy.eGetGeomColumnNameListByTable(tableName);
    }

    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListByTable(String tableName) {
        return pgAdvGeoOptPxy.eGetGeomColumnListByTable(tableName);
    }

    @Override
    public FieldBySchemaApo eGetGeomColumnByTable(String tableName) {
        return pgAdvGeoOptPxy.eGetGeomColumnByTable(tableName);
    }

    @Override
    public String eGetGeomColumnNameBySql(String sqlView) {
        return pgAdvGeoOptPxy.eGetGeomColumnNameBySql(sqlView);
    }

    @Override
    public List<String> eGetGeomColumnNameListBySql(String sqlView) {
        return pgAdvGeoOptPxy.eGetGeomColumnNameListBySql(sqlView);
    }

    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListBySql(String sqlView) {
        return pgAdvGeoOptPxy.eGetGeomColumnListBySql(sqlView);
    }

    @Override
    public FieldBySchemaApo eGetGeomColumnBySql(String sqlView) {
        return pgAdvGeoOptPxy.eGetGeomColumnBySql(sqlView);
    }

    @Override
    public boolean eIsPointTable(String tableName) {
        return pgAdvGeoOptPxy.eIsPointTable(tableName);
    }

    @Override
    public boolean eIsPolygonTable(String tableName) {
        return pgAdvGeoOptPxy.eIsPolygonTable(tableName);
    }

    @Override
    public boolean eIsLineStringTable(String tableName) {
        return pgAdvGeoOptPxy.eIsLineStringTable(tableName);
    }

    @Override
    public void eAddGeomColumn(String tableName, String geomFieldName, AdvEnumsTypeGeom geomType, int srid) {
        pgAdvGeoOptPxy.eAddGeomColumn(tableName, geomFieldName, geomType, srid);
    }

    @Override
    public void eDropGeomColumn(String tableName, String geomFieldName) {
        pgAdvGeoOptPxy.eDropGeomColumn(tableName, geomFieldName);
    }

    @Override
    public void eDropGeomColumn(String tableName) {
        pgAdvGeoOptPxy.eDropGeomColumn(tableName);
    }

    @Override
    public void eTransformSrid(String tableName, String geomFieldName, int targetSrid) {
        pgAdvGeoOptPxy.eTransformSrid(tableName, geomFieldName, targetSrid);
    }

    @Override
    public void eTransformSrid(String tableName, int targetSrid) {
        pgAdvGeoOptPxy.eTransformSrid(tableName, targetSrid);
    }

    @Override
    public Integer eGetSrid(String tableNameOrSqlView, String geomFieldName) {
        return pgAdvGeoOptPxy.eGetSrid(tableNameOrSqlView, geomFieldName);
    }

    @Override
    public Integer eGetSrid(String tableNameOrSqlView) {
        return pgAdvGeoOptPxy.eGetSrid(tableNameOrSqlView);
    }

    @Override
    public Map<String, Integer> eGetSrid(String tableNameOrSqlView, List<String> geomFieldNames) {
        return pgAdvGeoOptPxy.eGetSrid(tableNameOrSqlView, geomFieldNames);
    }

    @Override
    public void eCreateSpatialIndex(String tableName, String geomFieldName, String indexName) {
        pgAdvGeoOptPxy.eCreateSpatialIndex(tableName, geomFieldName, indexName);
    }

    @Override
    public void eCreateSpatialIndex(String tableName, String indexName) {
        pgAdvGeoOptPxy.eCreateSpatialIndex(tableName, indexName);
    }

    @Override
    public void eDropSpatialIndex(String tableName, String indexName) {
        pgAdvGeoOptPxy.eDropSpatialIndex(tableName, indexName);
    }

    @Override
    public List<GirAdvOneRow> eQueryIntersects(String tableName, String geomFieldName, String geometry, int srid) {
        return pgAdvGeoOptPxy.eQueryIntersects(tableName, geomFieldName, geometry, srid);
    }

    @Override
    public List<GirAdvOneRow> eQueryIntersects(String tableName, String geometry, int srid) {
        return pgAdvGeoOptPxy.eQueryIntersects(tableName, geometry, srid);
    }

    @Override
    public List<GirAdvOneRow> eQueryWithinBBox(String tableName, String geomFieldName, double[] bbox, int srid) {
        return pgAdvGeoOptPxy.eQueryWithinBBox(tableName, geomFieldName, bbox, srid);
    }

    @Override
    public List<GirAdvOneRow> eQueryWithinBBox(String tableName, double[] bbox, int srid) {
        return pgAdvGeoOptPxy.eQueryWithinBBox(tableName, bbox, srid);
    }

    @Override
    public List<GirAdvOneRow> eCalculateDistance(String tableName, String geomFieldName, String geometry, int srid, String distanceAlias) {
        return pgAdvGeoOptPxy.eCalculateDistance(tableName, geomFieldName, geometry, srid, distanceAlias);
    }

    @Override
    public List<GirAdvOneRow> eCalculateDistance(String tableName, String geometry, int srid, String distanceAlias) {
        return pgAdvGeoOptPxy.eCalculateDistance(tableName, geometry, srid, distanceAlias);
    }

    @Override
    public List<GirAdvOneRow> eGetCentroid(String tableNameOrSqlView, String geomFieldName, String centerAlias) {
        return pgAdvGeoOptPxy.eGetCentroid(tableNameOrSqlView, geomFieldName, centerAlias);
    }

    @Override
    public List<GirAdvOneRow> eGetCentroid(String tableNameOrSqlView, String centerAlias) {
        return pgAdvGeoOptPxy.eGetCentroid(tableNameOrSqlView, centerAlias);
    }

    @Override
    public List<Object> eValidateGeometries(String tableName, String geomFieldName) {
        return pgAdvGeoOptPxy.eValidateGeometries(tableName, geomFieldName);
    }

    @Override
    public List<Object> eValidateGeometries(String tableName) {
        return pgAdvGeoOptPxy.eValidateGeometries(tableName);
    }

    @Override
    public int eRepairGeometries(String tableName, String geomFieldName) {
        return pgAdvGeoOptPxy.eRepairGeometries(tableName, geomFieldName);
    }

    @Override
    public int eRepairGeometries(String tableName) {
        return pgAdvGeoOptPxy.eRepairGeometries(tableName);
    }

    @Override
    public BBoxApo eGetExtent(String tableNameOrSqlView, String geomFieldName) {
        return pgAdvGeoOptPxy.eGetExtent(tableNameOrSqlView, geomFieldName);
    }

    @Override
    public BBoxApo eGetExtent(String tableNameOrSqlView) {
        return pgAdvGeoOptPxy.eGetExtent(tableNameOrSqlView);
    }

    // ==================== 分页操作（代理调用PgAdvSimplePageOpt） ====================
    @Override
    public Long pCount(String noPageSql) {
        return pgAdvSimplePageOptPxy.pCount(noPageSql);
    }

    @Override
    public String pBuildPageSql(String noPageSql, int pageSize, int pageNum, boolean pageNumStartZero) {
        return pgAdvSimplePageOptPxy.pBuildPageSql(noPageSql, pageSize, pageNum, pageNumStartZero);
    }

    @Override
    public String pBuildSqlWithOrder(String baseSql, List<OrderApo> orders, String tableAlias) {
        return pgAdvSimplePageOptPxy.pBuildSqlWithOrder(baseSql, orders, tableAlias);
    }

    @Override
    public String pBuildSqlWithOrder(String baseSql, List<OrderApo> orders) {
        return pgAdvSimplePageOptPxy.pBuildSqlWithOrder(baseSql, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo, List<OrderApo> orders) {
        return pgAdvSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, hasFieldsInfo, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize) {
        return pgAdvSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return pgAdvSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, advEnumsGeomOpt);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, List<OrderApo> orders) {
        return pgAdvSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, boolean hasFieldsInfo) {
        return pgAdvSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, pageNumStartZero, hasFieldsInfo);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo) {
        return pgAdvSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, hasFieldsInfo);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero) {
        return pgAdvSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, pageNumStartZero);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, List<OrderApo> orders) {
        return pgAdvSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, pageNumStartZero, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return pgAdvSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, List<OrderApo> orders) {
        return pgAdvSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo) {
        return pgAdvSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, advEnumsGeomOpt, hasFieldsInfo);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo, List<OrderApo> orders) {
        return pgAdvSimplePageOptPxy.pPage(noPageSql, pageNum, pageSize, advEnumsGeomOpt, hasFieldsInfo, orders);
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
    public String tbGetTableNameWithSchema(IDataSourceGetter dataSourceGetter, String tableName, String schemaName) {
        // 代理调用：带指定Schema的完整表名拼接
        return dialectTableNameProcessorPxy.tbGetTableNameWithSchema(dataSourceGetter, tableName, schemaName);
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
    public GirAdvOneRow eSelectOne(String sqlStatement, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return pgAdvGeoOptPxy.eSelectOne(sqlStatement, sqlParam, advEnumsGeomOpt);
    }

    @Override
    public GirAdvOneRow eSelectOne(String sqlStatement, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt, String geomFieldName) {
        return pgAdvGeoOptPxy.eSelectOne(sqlStatement, sqlParam, advEnumsGeomOpt, geomFieldName);
    }

    @Override
    public GirAdvOneRow eSelectOne(String sqlStatement, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList) {
        return pgAdvGeoOptPxy.eSelectOne(sqlStatement, sqlParam, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(String sqlStatement, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return pgAdvGeoOptPxy.eSelectList(sqlStatement, sqlParam, advEnumsGeomOpt);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(String sqlStatement, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt, String geomFieldName) {
        return pgAdvGeoOptPxy.eSelectList(sqlStatement, sqlParam, advEnumsGeomOpt, geomFieldName);
    }

    @Override
    public List<GirAdvOneRow> eSelectList(String sqlStatement, SqlParamMap sqlParam, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList) {
        return pgAdvGeoOptPxy.eSelectList(sqlStatement, sqlParam, advEnumsGeomOpt, geomFieldNameList);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvGeoOptPxy.eGetGeoTypeBySql(sqlStatement, sqlParam);
    }

    @Override
    public AdvEnumsTypeGeom eGetGeoTypeBySql(String sqlStatement, SqlParamMap sqlParam, String geomFieldName) {
        return pgAdvGeoOptPxy.eGetGeoTypeBySql(sqlStatement, sqlParam, geomFieldName);
    }

    @Override
    public Map<String, AdvEnumsTypeGeom> eGetGeoTypeBySql(String sqlStatement, SqlParamMap sqlParam, List<String> geomFieldNames) {
        return pgAdvGeoOptPxy.eGetGeoTypeBySql(sqlStatement, sqlParam, geomFieldNames);
    }

    @Override
    public boolean eIsGeomBySql(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvGeoOptPxy.eIsGeomBySql(sqlStatement, sqlParam);
    }

    @Override
    public String eGetGeomColumnNameBySql(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvGeoOptPxy.eGetGeomColumnNameBySql(sqlStatement, sqlParam);
    }

    @Override
    public List<String> eGetGeomColumnNameListBySql(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvGeoOptPxy.eGetGeomColumnNameListBySql(sqlStatement, sqlParam);
    }

    @Override
    public List<FieldBySchemaApo> eGetGeomColumnListBySql(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvGeoOptPxy.eGetGeomColumnListBySql(sqlStatement, sqlParam);
    }

    @Override
    public FieldBySchemaApo eGetGeomColumnBySql(String sqlStatement, SqlParamMap sqlParam) {
        return pgAdvGeoOptPxy.eGetGeomColumnBySql(sqlStatement, sqlParam);
    }

    @Override
    public Long pCount(String noPageSqlStatement, SqlParamMap sqlParam) {
        return pgAdvSimplePageOptPxy.pCount(noPageSqlStatement, sqlParam);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo, List<OrderApo> orders) {
        return pgAdvSimplePageOptPxy.pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, hasFieldsInfo, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize) {
        return pgAdvSimplePageOptPxy.pPage(noPageSqlStatement, sqlParam, pageNum, pageSize);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return pgAdvSimplePageOptPxy.pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, advEnumsGeomOpt);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, List<OrderApo> orders) {
        return pgAdvSimplePageOptPxy.pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, boolean pageNumStartZero, boolean hasFieldsInfo) {
        return pgAdvSimplePageOptPxy.pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, hasFieldsInfo);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo) {
        return pgAdvSimplePageOptPxy.pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, hasFieldsInfo);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, boolean pageNumStartZero) {
        return pgAdvSimplePageOptPxy.pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, boolean pageNumStartZero, List<OrderApo> orders) {
        return pgAdvSimplePageOptPxy.pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return pgAdvSimplePageOptPxy.pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, List<OrderApo> orders) {
        return pgAdvSimplePageOptPxy.pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo) {
        return pgAdvSimplePageOptPxy.pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, advEnumsGeomOpt, hasFieldsInfo);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo, List<OrderApo> orders) {
        return pgAdvSimplePageOptPxy.pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, advEnumsGeomOpt, hasFieldsInfo, orders);
    }

}
