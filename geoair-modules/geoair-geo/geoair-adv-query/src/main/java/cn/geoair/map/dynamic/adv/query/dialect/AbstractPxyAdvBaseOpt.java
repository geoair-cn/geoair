package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereLambdaFilter;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 数据库的动态高级查询基础操作实现类
 *
 * <p>实现了IAdvBaseOpt接口，通过组合方式复用各细分操作类（插入/查询/更新/删除）的实现， 统一对外提供PostgreSQL数据库的全量基础操作，封装了代理对象的初始化和数据源注入。
 *
 * @author 张逢吉
 * @date 2025/10/9 10:16
 */
public abstract class AbstractPxyAdvBaseOpt implements IAdvBaseOpt {

    protected IDataSourceGetter dataSourceGetter;
    Supplier<AdvQueryGlobalConfig> configAdvQueryGetter;

    public AbstractPxyAdvBaseOpt(IDataSourceGetter dataSourceGetter, Supplier<AdvQueryGlobalConfig> configAdvQueryGetter) {
        this.dataSourceGetter = dataSourceGetter;
        this.configAdvQueryGetter = configAdvQueryGetter;
    }

    /**
     * 插入操作代理对象
     */
    protected IAdvBaseAccessOpt advBaseAccessPxyOpt;

    /**
     * 查询操作代理对象
     */
    protected IAdvBaseSelectOpt advBaseSelectPxyOpt;

    /**
     * 更新操作代理对象
     */
    protected IAdvBaseUpdateOpt advBaseUpdatePxyOpt;

    /**
     * 删除操作代理对象
     */
    protected IAdvBaseDeleteOpt advBaseDeletePxyOpt;

    public abstract IAdvBaseAccessOpt getAdvBaseAccessPxyOpt();

    public abstract IAdvBaseSelectOpt getAdvBaseSelectPxyOpt();

    public abstract IAdvBaseUpdateOpt getAdvBaseUpdatePxyOpt();

    public abstract IAdvBaseDeleteOpt getAdvBaseDeletePxyOpt();


    @Override
    public AdvQueryGlobalConfig getConfig() {
        return configAdvQueryGetter.get();
    }

    // ==================== 插入操作实现（代理调用） ====================
    @Override
    public Integer bInsertBySql(String sql) {
        return getAdvBaseAccessPxyOpt().bInsertBySql(sql);
    }

    @Override
    public Integer bInsertBySql(String dynamicSql, SqlParamMap sqlParamMap) {
        return getAdvBaseAccessPxyOpt().bInsertBySql(dynamicSql, sqlParamMap);
    }

    @Override
    public Integer bInsertOne(String tableName, Map<String, Object> rowData) {
        return getAdvBaseAccessPxyOpt().bInsertOne(tableName, rowData);
    }

    @Override
    public <T> Integer bInsertOne(String tableName, T entity) {
        return getAdvBaseAccessPxyOpt().bInsertOne(tableName, entity);
    }

    @Override
    public <T> Integer bInsertOne(String tableName, T entity, boolean isToUnderlineCase) {
        return getAdvBaseAccessPxyOpt().bInsertOne(tableName, entity, isToUnderlineCase);
    }

    @Override
    public <T> Integer bInsertOne(String tableName, T entity, boolean isToUnderlineCase, boolean ignoreNullValue) {
        return getAdvBaseAccessPxyOpt().bInsertOne(tableName, entity, isToUnderlineCase, ignoreNullValue);
    }

    @Override
    public <T> Integer bInsertOne(String tableName, T entity, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames) {
        return getAdvBaseAccessPxyOpt().bInsertOne(tableName, entity, isToUnderlineCase, ignoreNullValue, ignoreFieldNames);
    }


    @Override
    public Integer bInsertBatch(
            String tableName, List<String> headers, List<Map<String, Object>> rowsData) {
        return getAdvBaseAccessPxyOpt().bInsertBatch(tableName, headers, rowsData);
    }

    @Override
    public <T> Integer bInsertBatch(String tableName, Collection<T> entities) {
        return getAdvBaseAccessPxyOpt().bInsertBatch(tableName, entities);
    }

    @Override
    public Integer bInsertBatch(
            String tableName,
            List<String> headers,
            List<Map<String, Object>> rowsData,
            int batchSize) {
        return getAdvBaseAccessPxyOpt()
                .bInsertBatch(tableName, headers, rowsData, batchSize);
    }

    @Override
    public <T> Integer bInsertBatch(
            String tableName, Collection<T> entities, int batchSize) {
        return getAdvBaseAccessPxyOpt().bInsertBatch(tableName, entities, batchSize);
    }

    @Override
    public Integer bInsertIgnore(String tableName, Map<String, Object> rowData) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(tableName, rowData);
    }

    @Override
    public Integer bInsertIgnore(String tableName, Map<String, Object> rowData, List<String> conflictKeys) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(tableName, rowData, conflictKeys);
    }

    @Override
    public <T> Integer bInsertIgnore(String tableName, T entity, List<String> conflictKeys) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(tableName, entity, conflictKeys);
    }

    @Override
    public <T> Integer bInsertIgnore(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase, boolean ignoreNullValue) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(tableName, entity, conflictKeys, isToUnderlineCase, ignoreNullValue);
    }

    @Override
    public <T> Integer bInsertIgnore(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(tableName, entity, conflictKeys, isToUnderlineCase);
    }

    @Override
    public <T> Integer bInsertIgnore(String tableName, T entity, List<String> conflictKeys, List<String> ignoreFieldNames) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(tableName, entity, conflictKeys, ignoreFieldNames);
    }

    @Override
    public <T> Integer bInsertIgnore(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(tableName, entity, conflictKeys, isToUnderlineCase, ignoreNullValue, ignoreFieldNames);
    }

    @Override
    public Integer bInsertIgnoreBatch(
            String tableName, Set<String> headers, List<Map<String, Object>> rowsData, List<String> conflictKeys) {
        return getAdvBaseAccessPxyOpt().bInsertIgnoreBatch(tableName, headers, rowsData, conflictKeys);
    }


    @Override
    public Integer bInsertBySql(String sqlStatement, SqlParamList sqlParamList) {
        return getAdvBaseAccessPxyOpt().bInsertBySql(sqlStatement, sqlParamList);
    }

    @Override
    public Integer bInsertBySql(String sqlStatementOrDynamicSql, GirSqlParam sqlParam) {
        return getAdvBaseAccessPxyOpt().bInsertBySql(sqlStatementOrDynamicSql, sqlParam);
    }

    @Override
    public <T> Integer bInsertOne(T entity) {
        return getAdvBaseAccessPxyOpt().bInsertOne(entity);
    }

    @Override
    public <T> Integer bInsertOne(T entity, boolean isToUnderlineCase, boolean ignoreNullValue) {
        return getAdvBaseAccessPxyOpt().bInsertOne(entity, isToUnderlineCase, ignoreNullValue);
    }

    @Override
    public <T> Integer bInsertSelectiveOne(T entity) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveOne(entity);
    }

    @Override
    public <T> Integer bInsertSelectiveOne(T entity, boolean isToUnderlineCase) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveOne(entity, isToUnderlineCase);
    }

    @Override
    public <T> Integer bInsertIgnore(T entity) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(entity);
    }

    @Override
    public <T> Integer bInsertIgnore(T entity, List<String> conflictKeys) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(entity, conflictKeys);
    }

    @Override
    public <T> Integer bInsertIgnore(T entity, List<String> conflictKeys, boolean isToUnderlineCase) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(entity, conflictKeys, isToUnderlineCase);
    }

    @Override
    public <T> Integer bInsertIgnore(T entity, boolean isToUnderlineCase) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(entity, isToUnderlineCase);
    }

    @Override
    public <T> Integer bInsertIgnore(T entity, List<String> conflictKeys, List<String> ignoreFieldNames) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(entity, conflictKeys, ignoreFieldNames);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnore(T entity) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveIgnore(entity);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnore(T entity, List<String> conflictKeys) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveIgnore(entity, conflictKeys);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnore(String tableName, T entity, List<String> conflictKeys) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveIgnore(tableName, entity, conflictKeys);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnore(T entity, List<String> conflictKeys, boolean isToUnderlineCase) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveIgnore(entity, conflictKeys, isToUnderlineCase);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnore(T entity, boolean isToUnderlineCase) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveIgnore(entity, isToUnderlineCase);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnore(T entity, List<String> conflictKeys, List<String> ignoreFieldNames) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveIgnore(entity, conflictKeys, ignoreFieldNames);
    }

    // ==================== 删除操作实现（代理调用） ====================
    @Override
    public Integer bDeleteBySql(String sqlStatement) {
        return getAdvBaseDeletePxyOpt().bDeleteBySql(sqlStatement);
    }

    @Override
    public Integer bDeleteBySql(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseDeletePxyOpt().bDeleteBySql(dynamicSql, sqlParam);
    }

    @Override
    public Integer bDeleteBySql(String sqlStatement, SqlParamList sqlParam) {
        return getAdvBaseDeletePxyOpt().bDeleteBySql(sqlStatement, sqlParam);
    }

    @Override
    public Integer bDeleteBySql(String sqlStatement, GirSqlParam sqlParam) {
        return getAdvBaseDeletePxyOpt().bDeleteBySql(sqlStatement, sqlParam);
    }

    @Override
    public Integer bDeleteByPK(String tableName, String idKey, Object id) {
        return getAdvBaseDeletePxyOpt().bDeleteByPK(tableName, idKey, id);
    }

    @Override
    public Integer bDeleteByPKs(String tableName, String idKey, Set<Object> ids) {
        return getAdvBaseDeletePxyOpt().bDeleteByPKs(tableName, idKey, ids);
    }

    @Override
    public Integer bDeleteByPKs(
            String tableName, String idKey, Set<Object> ids, int batchSize) {
        return getAdvBaseDeletePxyOpt().bDeleteByPKs(tableName, idKey, ids, batchSize);
    }

    @Override
    public Integer bDeleteByMap(String tableName, Map<String, Object> whereMap) {
        return getAdvBaseDeletePxyOpt().bDeleteByMap(tableName, whereMap);
    }

    @Override
    public Integer bDeleteByMap(
            String tableName, Map<String, Object> whereMap, int batchSize) {
        return getAdvBaseDeletePxyOpt().bDeleteByMap(tableName, whereMap, batchSize);
    }

    @Override
    public <T> Integer bDeleteByWhere(String tableName, GirAdvWhereLambdaFilter<T> whereFilter) {
        return getAdvBaseDeletePxyOpt().bDeleteByWhere(tableName, whereFilter);
    }

    @Override
    public <T> Integer bDeleteByWhere(GirAdvWhereLambdaFilter<T> whereFilter) {
        return getAdvBaseDeletePxyOpt().bDeleteByWhere(whereFilter);
    }

    @Override
    public <T> Integer bDeleteByWhere(String tableName, GirAdvWhereFilter whereFilter) {
        return getAdvBaseDeletePxyOpt().bDeleteByWhere(tableName, whereFilter);
    }

    @Override
    public <T> Integer bDeleteByPK(T entity) {
        return getAdvBaseDeletePxyOpt().bDeleteByPK(entity);
    }

    @Override
    public <T> Integer bDeleteByPK(T entity, boolean isToUnderlineCase) {
        return getAdvBaseDeletePxyOpt().bDeleteByPK(entity, isToUnderlineCase);
    }

    @Override
    public <T> Integer bDeleteByPK(String tableName, T entity) {
        return getAdvBaseDeletePxyOpt().bDeleteByPK(tableName, entity);
    }

    @Override
    public <T> Integer bDeleteByPK(String tableName, T entity, boolean isToUnderlineCase) {
        return getAdvBaseDeletePxyOpt().bDeleteByPK(tableName, entity, isToUnderlineCase);
    }

    @Override
    public void setDataSourceGetter(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
    }

    // ==================== 查询操作实现（代理调用） ====================
    @Override
    public GirAdvOneRow bSelectOne(String sql) {
        return getAdvBaseSelectPxyOpt().bSelectOne(sql);
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sql) {
        return getAdvBaseSelectPxyOpt().bSelectList(sql);
    }

    @Override
    public void bSelectListStream(String sql, Consumer<GirAdvOneRow> rowConsumer) {
        getAdvBaseSelectPxyOpt().bSelectListStream(sql, rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sql) {
        return getAdvBaseSelectPxyOpt().bSelectListToValueList(sql);
    }

    @Override
    public Number bSelectNumber(String sql) {
        return getAdvBaseSelectPxyOpt().bSelectNumber(sql);
    }

    @Override
    public Number bSelectRecordRowCount(String sql) {
        return getAdvBaseSelectPxyOpt().bSelectRecordRowCount(sql);
    }

    @Override
    public <E> E bSelectObjOne(String sql, Class<E> clazz) {
        return getAdvBaseSelectPxyOpt().bSelectObjOne(sql, clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String sql, Class<E> clazz) {
        return getAdvBaseSelectPxyOpt().bSelectObjList(sql, clazz);
    }

    @Override
    public <E> void bSelectObjListStream(String sql, Class<E> clazz, Consumer<E> rowConsumer) {
        getAdvBaseSelectPxyOpt().bSelectObjListStream(sql, clazz, rowConsumer);
    }

    @Override
    public GirAdvOneRow bSelectOne(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectOne(dynamicSql, sqlParam);
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectList(dynamicSql, sqlParam);
    }

    @Override
    public void bSelectListStream(
            String dynamicSql, SqlParamMap sqlParam, Consumer<GirAdvOneRow> rowConsumer) {
        getAdvBaseSelectPxyOpt().bSelectListStream(dynamicSql, sqlParam, rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectListToValueList(dynamicSql, sqlParam);
    }

    @Override
    public Number bSelectNumber(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectNumber(dynamicSql, sqlParam);
    }

    @Override
    public Number bSelectRecordRowCount(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectRecordRowCount(dynamicSql, sqlParam);
    }

    @Override
    public <E> E bSelectObjOne(String dynamicSql, SqlParamMap sqlParam, Class<E> clazz) {
        return getAdvBaseSelectPxyOpt().bSelectObjOne(dynamicSql, sqlParam, clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String dynamicSql, SqlParamMap sqlParam, Class<E> clazz) {
        return getAdvBaseSelectPxyOpt().bSelectObjList(dynamicSql, sqlParam, clazz);
    }

    @Override
    public <E> void bSelectObjListStream(
            String dynamicSql, SqlParamMap sqlParam, Class<E> clazz, Consumer<E> rowConsumer) {
        getAdvBaseSelectPxyOpt().bSelectObjListStream(dynamicSql, sqlParam, clazz, rowConsumer);
    }


    @Override
    public GirAdvOneRow bSelectOne(String sqlStatement, SqlParamList sqlParamList) {
        return getAdvBaseSelectPxyOpt().bSelectOne(sqlStatement, sqlParamList);
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sqlStatement, SqlParamList sqlParamList) {
        return getAdvBaseSelectPxyOpt().bSelectList(sqlStatement, sqlParamList);
    }

    @Override
    public void bSelectListStream(String sqlStatement, SqlParamList sqlParamList, Consumer<GirAdvOneRow> rowConsumer) {
        getAdvBaseSelectPxyOpt().bSelectListStream(sqlStatement, sqlParamList, rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sqlStatement, SqlParamList sqlParamList) {
        return getAdvBaseSelectPxyOpt().bSelectListToValueList(sqlStatement, sqlParamList);
    }

    @Override
    public Number bSelectNumber(String sqlStatement, SqlParamList sqlParamList) {
        return getAdvBaseSelectPxyOpt().bSelectNumber(sqlStatement, sqlParamList);
    }

    @Override
    public Number bSelectRecordRowCount(String sqlStatement, SqlParamList sqlParamList) {
        return getAdvBaseSelectPxyOpt().bSelectRecordRowCount(sqlStatement, sqlParamList);
    }

    @Override
    public <E> E bSelectObjOne(String sqlStatement, SqlParamList sqlParamList, Class<E> clazz) {
        return getAdvBaseSelectPxyOpt().bSelectObjOne(sqlStatement, sqlParamList, clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String sqlStatement, SqlParamList sqlParamList, Class<E> clazz) {
        return getAdvBaseSelectPxyOpt().bSelectObjList(sqlStatement, sqlParamList, clazz);
    }

    @Override
    public <E> void bSelectObjListStream(String sqlStatement, SqlParamList sqlParamList, Class<E> clazz, Consumer<E> rowConsumer) {
        getAdvBaseSelectPxyOpt().bSelectObjListStream(sqlStatement, sqlParamList, clazz, rowConsumer);
    }

    //=============================================================================

    @Override
    public GirAdvOneRow bSelectOne(String sqlStatement, GirSqlParam girSqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectOne(sqlStatement, girSqlParam);
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sqlStatement, GirSqlParam girSqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectList(sqlStatement, girSqlParam);
    }

    @Override
    public void bSelectListStream(String sqlStatement, GirSqlParam girSqlParam, Consumer<GirAdvOneRow> rowConsumer) {
        getAdvBaseSelectPxyOpt().bSelectListStream(sqlStatement, girSqlParam, rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sqlStatement, GirSqlParam girSqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectListToValueList(sqlStatement, girSqlParam);
    }

    @Override
    public Number bSelectNumber(String sqlStatement, GirSqlParam girSqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectNumber(sqlStatement, girSqlParam);
    }

    @Override
    public Number bSelectRecordRowCount(String sqlStatement, GirSqlParam girSqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectRecordRowCount(sqlStatement, girSqlParam);
    }

    @Override
    public <E> E bSelectObjOne(String sqlStatement, GirSqlParam girSqlParam, Class<E> clazz) {
        return getAdvBaseSelectPxyOpt().bSelectObjOne(sqlStatement, girSqlParam, clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String sqlStatement, GirSqlParam girSqlParam, Class<E> clazz) {
        return getAdvBaseSelectPxyOpt().bSelectObjList(sqlStatement, girSqlParam, clazz);
    }

    @Override
    public <E> void bSelectObjListStream(String sqlStatement, GirSqlParam girSqlParam, Class<E> clazz, Consumer<E> rowConsumer) {
        getAdvBaseSelectPxyOpt().bSelectObjListStream(sqlStatement, girSqlParam, clazz, rowConsumer);
    }


    // ==================== 更新操作实现（代理调用） ====================
    @Override
    public Integer bUpdateBySql(String sqlStatement) {
        return getAdvBaseUpdatePxyOpt().bUpdateBySql(sqlStatement);
    }

    @Override
    public Integer bUpdateBySql(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseUpdatePxyOpt().bUpdateBySql(dynamicSql, sqlParam);
    }

    @Override
    public Integer bUpdateBySql(String sqlStatement, SqlParamList sqlParam) {
        return getAdvBaseUpdatePxyOpt().bUpdateBySql(sqlStatement, sqlParam);
    }

    @Override
    public Integer bUpdateBySql(String sqlStatement, GirSqlParam sqlParam) {
        return getAdvBaseUpdatePxyOpt().bUpdateBySql(sqlStatement, sqlParam);
    }

    @Override
    public Integer bUpdateByPK(
            String tableName, String idKey, Object id, Map<String, Object> rowData) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPK(tableName, idKey, id, rowData);
    }

    @Override
    public <T> Integer bUpdateByPK(String tableName, String idKey, T entity) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPK(tableName, idKey, entity);
    }

    @Override
    public <T> Integer bUpdateByPK(String tableName, String idKey, T entity, boolean isToUnderlineCase, boolean ignoreNullValue) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPK(tableName, idKey, entity, isToUnderlineCase, ignoreNullValue);
    }

    @Override
    public <T> Integer bUpdateByPK(String tableName, String idKey, T entity, boolean isToUnderlineCase) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPK(tableName, idKey, entity, isToUnderlineCase);
    }

    @Override
    public <T> Integer bUpdateByPK(String tableName, String idKey, T entity, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPK(tableName, idKey, entity, isToUnderlineCase, ignoreNullValue, ignoreFieldNames);
    }

    @Override
    public <T> Integer bUpdateByPKSelective(String tableName, String idKey, T entity, boolean isToUnderlineCase) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPKSelective(tableName, idKey, entity, isToUnderlineCase);
    }

    @Override
    public <T> Integer bUpdateByPKSelective(String tableName, String idKey, T entity) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPKSelective(tableName, idKey, entity);
    }

    @Override
    public <T> Integer bUpdateByPKSelective(String tableName, String idKey, T entity, List<String> ignoreFieldNames) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPKSelective(tableName, idKey, entity, ignoreFieldNames);
    }

    @Override
    public <T> Integer bUpdateByPKSelective(T entity) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPKSelective(entity);
    }

    @Override
    public <T> Integer bUpdateByPKSelective(T entity, List<String> ignoreFieldNames) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPKSelective(entity, ignoreFieldNames);
    }

    @Override
    public <T> Integer bUpdateByPKSelective(T entity, boolean isToUnderlineCase, List<String> ignoreFieldNames) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPKSelective(entity, isToUnderlineCase, ignoreFieldNames);
    }

    @Override
    public <T> Integer bUpdateByPK(T entity) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPK(entity);
    }

    @Override
    public <T> Integer bUpdateByPK(T entity, List<String> ignoreFieldNames) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPK(entity, ignoreFieldNames);
    }

    @Override
    public <T> Integer bUpdateByPK(T entity, boolean isToUnderlineCase, List<String> ignoreFieldNames) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPK(entity, isToUnderlineCase, ignoreFieldNames);
    }

    @Override
    public Integer bUpdateByMap(
            String tableName, Map<String, Object> rowData, Map<String, Object> whereMap) {
        return getAdvBaseUpdatePxyOpt().bUpdateByMap(tableName, rowData, whereMap);
    }

    @Override
    public void bUpdateBatchByPK(
            String tableName, String idKey, List<Map<String, Object>> rowsData) {
        getAdvBaseUpdatePxyOpt().bUpdateBatchByPK(tableName, idKey, rowsData);
    }

    @Override
    public void bUpdateBatchByPK(
            String tableName, String idKey, List<Map<String, Object>> rowsData, int batchSize) {
        getAdvBaseUpdatePxyOpt()
                .bUpdateBatchByPK(tableName, idKey, rowsData, batchSize);
    }

    @Override
    public <T> void bUpdateBatchByPK(
            String tableName, String idKey, Collection<T> entities) {
        getAdvBaseUpdatePxyOpt().bUpdateBatchByPK(tableName, idKey, entities);
    }


    @Override
    public Integer bUpdateOrInsert(
            String tableName, Map<String, Object> rowData, List<String> conflictKeys) {
        return getAdvBaseUpdatePxyOpt().bUpdateOrInsert(tableName, rowData, conflictKeys);
    }

    @Override
    public Integer bUpsert(String tableName, Map<String, Object> rowData, List<String> conflictKeys) {
        return getAdvBaseUpdatePxyOpt().bUpsert(tableName, rowData, conflictKeys);
    }

    @Override
    public <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys) {
        return getAdvBaseUpdatePxyOpt().bUpsert(tableName, entity, conflictKeys);
    }

    @Override
    public <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase, boolean ignoreNullValue) {
        return getAdvBaseUpdatePxyOpt().bUpsert(tableName, entity, conflictKeys, isToUnderlineCase, ignoreNullValue);
    }

    @Override
    public <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase) {
        return getAdvBaseUpdatePxyOpt().bUpsert(tableName, entity, conflictKeys, isToUnderlineCase);
    }

    @Override
    public <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames) {
        return getAdvBaseUpdatePxyOpt().bUpsert(tableName, entity, conflictKeys, isToUnderlineCase, ignoreNullValue, ignoreFieldNames);
    }

    @Override
    public <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys, List<String> ignoreFieldNames) {
        return getAdvBaseUpdatePxyOpt().bUpsert(tableName, entity, conflictKeys, ignoreFieldNames);
    }

    @Override
    public <T> Integer bUpsertSelective(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase) {
        return getAdvBaseUpdatePxyOpt().bUpsertSelective(tableName, entity, conflictKeys, isToUnderlineCase);
    }

    @Override
    public <T> Integer bUpsertSelective(String tableName, T entity, List<String> conflictKeys) {
        return getAdvBaseUpdatePxyOpt().bUpsertSelective(tableName, entity, conflictKeys);
    }

    @Override
    public <T> Integer bUpsert(T entity) {
        return getAdvBaseUpdatePxyOpt().bUpsert(entity);
    }

    @Override
    public <T> Integer bUpsert(String tableName, T entity) {
        return getAdvBaseUpdatePxyOpt().bUpsert(tableName, entity);
    }

    @Override
    public <T> Integer bUpsertSelective(String tableName, T entity) {
        return getAdvBaseUpdatePxyOpt().bUpsertSelective(tableName, entity);
    }

    @Override
    public <T> Integer bUpsertSelective(T entity) {
        return getAdvBaseUpdatePxyOpt().bUpsertSelective(entity);
    }

    @Override
    public <T> Integer bUpsertSelective(String tableName, T entity, List<String> conflictKeys, List<String> ignoreFieldNames) {
        return getAdvBaseUpdatePxyOpt().bUpsertSelective(tableName, entity, conflictKeys, ignoreFieldNames);
    }

    @Override
    public <T> Integer bUpdateByWhere(String tableName, T entity, GirAdvWhereLambdaFilter<T> whereFilter, List<String> ignoreFieldNames) {
        return getAdvBaseUpdatePxyOpt().bUpdateByWhere(tableName, entity, whereFilter, ignoreFieldNames);
    }

    @Override
    public <T> Integer bUpdateByWhere(String tableName, T entity, GirAdvWhereLambdaFilter<T> whereFilter) {
        return getAdvBaseUpdatePxyOpt().bUpdateByWhere(tableName, entity, whereFilter);
    }

    @Override
    public <T> Integer bUpdateByWhere(String tableName, Map<String, Object> rowData, GirAdvWhereFilter whereFilter) {
        return getAdvBaseUpdatePxyOpt().bUpdateByWhere(tableName, rowData, whereFilter);
    }

    @Override
    public <T> Integer bUpdateSelectiveByWhere(String tableName, T entity, GirAdvWhereLambdaFilter<T> whereFilter, List<String> ignoreFieldNames) {
        return getAdvBaseUpdatePxyOpt().bUpdateSelectiveByWhere(tableName, entity, whereFilter, ignoreFieldNames);
    }

    @Override
    public <T> Integer bUpdateSelectiveByWhere(String tableName, T entity, GirAdvWhereLambdaFilter<T> whereFilter) {
        return getAdvBaseUpdatePxyOpt().bUpdateSelectiveByWhere(tableName, entity, whereFilter);
    }

    @Override
    public <T> Integer bUpdateSelectiveByWhere(T entity, GirAdvWhereLambdaFilter<T> whereFilter, List<String> ignoreFieldNames) {
        return getAdvBaseUpdatePxyOpt().bUpdateSelectiveByWhere(entity, whereFilter, ignoreFieldNames);
    }

    @Override
    public <T> Integer bUpdateSelectiveByWhere(T entity, GirAdvWhereLambdaFilter<T> whereFilter) {
        return getAdvBaseUpdatePxyOpt().bUpdateSelectiveByWhere(entity, whereFilter);
    }

    @Override
    public <T> Integer bUpdateSelectiveByWhere(String tableName, Map<String, Object> rowData, GirAdvWhereFilter whereFilter) {
        return getAdvBaseUpdatePxyOpt().bUpdateSelectiveByWhere(tableName, rowData, whereFilter);
    }
}
