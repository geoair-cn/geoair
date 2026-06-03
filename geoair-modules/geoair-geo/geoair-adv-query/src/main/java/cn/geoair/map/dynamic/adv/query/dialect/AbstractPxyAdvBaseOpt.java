package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.strategy.AccessStrategy;
import cn.geoair.map.dynamic.adv.query.strategy.DeleteStrategy;
import cn.geoair.map.dynamic.adv.query.strategy.UpdateStrategy;
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
    public Integer bInsertBySql(String sqlStatement, SqlParamList sqlParamList) {
        return getAdvBaseAccessPxyOpt().bInsertBySql(sqlStatement, sqlParamList);
    }

    @Override
    public Integer bInsertBySql(String sqlStatementOrDynamicSql, GirSqlParam sqlParam) {
        return getAdvBaseAccessPxyOpt().bInsertBySql(sqlStatementOrDynamicSql, sqlParam);
    }

    @Override
    public Integer bInsertOne(String tableName, Map<String, Object> rowData) {
        return getAdvBaseAccessPxyOpt().bInsertOne(tableName, rowData);
    }

    @Override
    public <T> Integer bInsertOne(T entity) {
        return getAdvBaseAccessPxyOpt().bInsertOne(entity);
    }

    @Override
    public <T> Integer bInsertOne(T entity, AccessStrategy strategy) {
        return getAdvBaseAccessPxyOpt().bInsertOne(entity, strategy);
    }

    @Override
    public <T> Integer bInsertOne(T entity, Consumer<AccessStrategy> strategyConsumer) {
        return getAdvBaseAccessPxyOpt().bInsertOne(entity, strategyConsumer);
    }

    @Override
    public <T> Integer bInsertSelectiveOne(T entity) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveOne(entity);
    }

    @Override
    public <T> Integer bInsertSelectiveOne(T entity, AccessStrategy strategy) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveOne(entity, strategy);
    }

    @Override
    public <T> Integer bInsertSelectiveOne(T entity, Consumer<AccessStrategy> strategyConsumer) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveOne(entity, strategyConsumer);
    }

    @Override
    public Integer bInsertBatch(String tableName, List<String> headers, List<Map<String, Object>> rowsData) {
        return getAdvBaseAccessPxyOpt().bInsertBatch(tableName, headers, rowsData);
    }

    @Override
    public Integer bInsertBatch(String tableName, List<String> headers, List<Map<String, Object>> rowsData, int batchSize) {
        return getAdvBaseAccessPxyOpt().bInsertBatch(tableName, headers, rowsData, batchSize);
    }

    @Override
    public <T> Integer bInsertBatch(Collection<T> entities) {
        return getAdvBaseAccessPxyOpt().bInsertBatch(entities);
    }

    @Override
    public <T> Integer bInsertBatch(Collection<T> entities, AccessStrategy strategy) {
        return getAdvBaseAccessPxyOpt().bInsertBatch(entities, strategy);
    }

    @Override
    public <T> Integer bInsertBatch(Collection<T> entities, Consumer<AccessStrategy> strategyConsumer) {
        return getAdvBaseAccessPxyOpt().bInsertBatch(entities, strategyConsumer);
    }

    @Override
    public <T> Integer bInsertBatch(String tableName, Collection<T> entities) {
        return getAdvBaseAccessPxyOpt().bInsertBatch(tableName, entities);
    }

    @Override
    public <T> Integer bInsertBatch(String tableName, Collection<T> entities, AccessStrategy strategy) {
        return getAdvBaseAccessPxyOpt().bInsertBatch(tableName, entities, strategy);
    }

    @Override
    public <T> Integer bInsertBatch(String tableName, Collection<T> entities, Consumer<AccessStrategy> strategyConsumer) {
        return getAdvBaseAccessPxyOpt().bInsertBatch(tableName, entities, strategyConsumer);
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
    public <T> Integer bInsertIgnore(T entity) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(entity);
    }

    @Override
    public <T> Integer bInsertIgnore(T entity, AccessStrategy strategy) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(entity, strategy);
    }

    @Override
    public <T> Integer bInsertIgnore(T entity, Consumer<AccessStrategy> strategyConsumer) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(entity, strategyConsumer);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnore(T entity) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveIgnore(entity);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnore(T entity, AccessStrategy strategy) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveIgnore(entity, strategy);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnore(T entity, Consumer<AccessStrategy> strategyConsumer) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveIgnore(entity, strategyConsumer);
    }

    @Override
    public Integer bInsertIgnoreBatch(String tableName, Set<String> headers, List<Map<String, Object>> rowsData, List<String> conflictKeys) {
        return getAdvBaseAccessPxyOpt().bInsertIgnoreBatch(tableName, headers, rowsData, conflictKeys);
    }

    @Override
    public <T> Integer bInsertIgnoreBatch(Collection<T> entities, List<String> conflictKeys) {
        return getAdvBaseAccessPxyOpt().bInsertIgnoreBatch(entities, conflictKeys);
    }

    @Override
    public <T> Integer bInsertIgnoreBatch(Collection<T> entities, List<String> conflictKeys, AccessStrategy strategy) {
        return getAdvBaseAccessPxyOpt().bInsertIgnoreBatch(entities, conflictKeys, strategy);
    }

    @Override
    public <T> Integer bInsertIgnoreBatch(Collection<T> entities, List<String> conflictKeys, Consumer<AccessStrategy> strategyConsumer) {
        return getAdvBaseAccessPxyOpt().bInsertIgnoreBatch(entities, conflictKeys, strategyConsumer);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnoreBatch(Collection<T> entities, List<String> conflictKeys) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveIgnoreBatch(entities, conflictKeys);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnoreBatch(Collection<T> entities, List<String> conflictKeys, AccessStrategy strategy) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveIgnoreBatch(entities, conflictKeys, strategy);
    }

    @Override
    public <T> Integer bInsertSelectiveIgnoreBatch(Collection<T> entities, List<String> conflictKeys, Consumer<AccessStrategy> strategyConsumer) {
        return getAdvBaseAccessPxyOpt().bInsertSelectiveIgnoreBatch(entities, conflictKeys, strategyConsumer);
    }
// ==================== 删除操作实现（代理调用） ====================


    @Override
    public Integer bDeleteBySql(String sqlStatement) {
        return getAdvBaseDeletePxyOpt().bDeleteBySql(sqlStatement);
    }

    @Override
    public Integer bDeleteBySql(String sqlStatement, SqlParamMap sqlParam) {
        return getAdvBaseDeletePxyOpt().bDeleteBySql(sqlStatement, sqlParam);
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
    public <T> Integer bDeleteByPK(T entity) {
        return getAdvBaseDeletePxyOpt().bDeleteByPK(entity);
    }

    @Override
    public <T> Integer bDeleteByPK(T entity, DeleteStrategy strategy) {
        return getAdvBaseDeletePxyOpt().bDeleteByPK(entity, strategy);
    }

    @Override
    public <T> Integer bDeleteByPK(T entity, Consumer<DeleteStrategy> strategyConsumer) {
        return getAdvBaseDeletePxyOpt().bDeleteByPK(entity, strategyConsumer);
    }

    @Override
    public Integer bDeleteByPKs(String tableName, String idKey, Set<Object> ids) {
        return getAdvBaseDeletePxyOpt().bDeleteByPKs(tableName, idKey, ids);
    }

    @Override
    public Integer bDeleteByPKs(String tableName, String idKey, Set<Object> ids, int batchSize) {
        return getAdvBaseDeletePxyOpt().bDeleteByPKs(tableName, idKey, ids, batchSize);
    }

    @Override
    public <T> void bDeleteBatchByPK(Collection<T> entities) {
        getAdvBaseDeletePxyOpt().bDeleteBatchByPK(entities);
    }

    @Override
    public <T> void bDeleteBatchByPK(Collection<T> entities, DeleteStrategy strategy) {
        getAdvBaseDeletePxyOpt().bDeleteBatchByPK(entities, strategy);
    }

    @Override
    public <T> void bDeleteBatchByPK(Collection<T> entities, Consumer<DeleteStrategy> strategyConsumer) {
        getAdvBaseDeletePxyOpt().bDeleteBatchByPK(entities, strategyConsumer);
    }

    @Override
    public Integer bDeleteByMap(String tableName, Map<String, Object> whereMap) {
        return getAdvBaseDeletePxyOpt().bDeleteByMap(tableName, whereMap);
    }

    @Override
    public Integer bDeleteByMap(String tableName, Map<String, Object> whereMap, int batchSize) {
        return getAdvBaseDeletePxyOpt().bDeleteByMap(tableName, whereMap, batchSize);
    }

    @Override
    public <T> Integer bDeleteByWhere(DeleteStrategy strategy, GirAdvWhereLambdaFilter<T> whereFilter) {
        return getAdvBaseDeletePxyOpt().bDeleteByWhere(strategy, whereFilter);
    }

    @Override
    public <T> Integer bDeleteByWhere(DeleteStrategy strategy, Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        return getAdvBaseDeletePxyOpt().bDeleteByWhere(strategy, consumer);
    }

    @Override
    public <T> Integer bDeleteByWhere(Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        return getAdvBaseDeletePxyOpt().bDeleteByWhere(consumer);
    }

    @Override
    public <T> Integer bDeleteByWhere(Consumer<DeleteStrategy> strategyConsumer, Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        return getAdvBaseDeletePxyOpt().bDeleteByWhere(strategyConsumer, consumer);
    }

    @Override
    public <T> Integer bDeleteByWhere(String tableName, Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        return getAdvBaseDeletePxyOpt().bDeleteByWhere(tableName, consumer);
    }

    @Override
    public <T> Integer bDeleteByWhere(String tableName, GirAdvWhereFilter whereFilter) {
        return getAdvBaseDeletePxyOpt().bDeleteByWhere(tableName, whereFilter);
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
    public Integer bUpdateBySql(String sqlStatement, SqlParamList sqlParam) {
        return getAdvBaseUpdatePxyOpt().bUpdateBySql(sqlStatement, sqlParam);
    }

    @Override
    public Integer bUpdateBySql(String dynamicSql, SqlParamMap sqlParam) {
        return getAdvBaseUpdatePxyOpt().bUpdateBySql(dynamicSql, sqlParam);
    }

    @Override
    public Integer bUpdateBySql(String sqlStatement, GirSqlParam sqlParam) {
        return getAdvBaseUpdatePxyOpt().bUpdateBySql(sqlStatement, sqlParam);
    }

    @Override
    public Integer bUpdateByPK(String tableName, String idKey, Object id, Map<String, Object> rowData) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPK(tableName, idKey, id, rowData);
    }

    @Override
    public <T> Integer bUpdateByPK(T entity) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPK(entity);
    }

    @Override
    public <T> Integer bUpdateByPK(T entity, UpdateStrategy strategy) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPK(entity, strategy);
    }

    @Override
    public <T> Integer bUpdateByPK(T entity, Consumer<UpdateStrategy> strategyConsumer) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPK(entity, strategyConsumer);
    }

    @Override
    public <T> Integer bUpdateByPKSelective(T entity) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPKSelective(entity);
    }

    @Override
    public <T> Integer bUpdateByPKSelective(T entity, UpdateStrategy strategy) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPKSelective(entity, strategy);
    }

    @Override
    public <T> Integer bUpdateByPKSelective(T entity, Consumer<UpdateStrategy> strategyConsumer) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPKSelective(entity, strategyConsumer);
    }

    @Override
    public void bUpdateBatchByPK(String tableName, String idKey, List<Map<String, Object>> rowsData) {
        getAdvBaseUpdatePxyOpt().bUpdateBatchByPK(tableName, idKey, rowsData);
    }

    @Override
    public void bUpdateBatchByPK(String tableName, String idKey, List<Map<String, Object>> rowsData, int batchSize) {
        getAdvBaseUpdatePxyOpt().bUpdateBatchByPK(tableName, idKey, rowsData, batchSize);
    }

    @Override
    public <T> void bUpdateBatchByPK(String tableName, String idKey, Collection<T> entities) {
        getAdvBaseUpdatePxyOpt().bUpdateBatchByPK(tableName, idKey, entities);
    }

    @Override
    public <T> void bUpdateBatchByPK(Collection<T> entities, UpdateStrategy strategy) {
        getAdvBaseUpdatePxyOpt().bUpdateBatchByPK(entities, strategy);
    }

    @Override
    public <T> void bUpdateBatchByPK(Collection<T> entities, Consumer<UpdateStrategy> strategyConsumer) {
        getAdvBaseUpdatePxyOpt().bUpdateBatchByPK(entities, strategyConsumer);
    }

    @Override
    public <T> void bUpdateBatchByPKSelective(String tableName, String idKey, Collection<T> entities) {
        getAdvBaseUpdatePxyOpt().bUpdateBatchByPKSelective(tableName, idKey, entities);
    }

    @Override
    public <T> void bUpdateBatchByPKSelective(Collection<T> entities, UpdateStrategy strategy) {
        getAdvBaseUpdatePxyOpt().bUpdateBatchByPKSelective(entities, strategy);
    }

    @Override
    public <T> void bUpdateBatchByPKSelective(Collection<T> entities, Consumer<UpdateStrategy> strategyConsumer) {
        getAdvBaseUpdatePxyOpt().bUpdateBatchByPKSelective(entities, strategyConsumer);
    }

    @Override
    public Integer bUpdateByMap(String tableName, Map<String, Object> rowData, Map<String, Object> whereMap) {
        return getAdvBaseUpdatePxyOpt().bUpdateByMap(tableName, rowData, whereMap);
    }

    @Override
    public <T> Integer bUpdateByWhere(T entity, UpdateStrategy strategy, GirAdvWhereLambdaFilter<T> whereFilter) {
        return getAdvBaseUpdatePxyOpt().bUpdateByWhere(entity, strategy, whereFilter);
    }

    @Override
    public <T> Integer bUpdateByWhere(T entity, UpdateStrategy strategy, Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        return getAdvBaseUpdatePxyOpt().bUpdateByWhere(entity, strategy, consumer);
    }

    @Override
    public <T> Integer bUpdateByWhere(T entity, Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        return getAdvBaseUpdatePxyOpt().bUpdateByWhere(entity, consumer);
    }

    @Override
    public <T> Integer bUpdateByWhere(T entity, Consumer<UpdateStrategy> strategyConsumer, Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        return getAdvBaseUpdatePxyOpt().bUpdateByWhere(entity, strategyConsumer, consumer);
    }

    @Override
    public <T> Integer bUpdateByWhere(String tableName, T entity, Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        return getAdvBaseUpdatePxyOpt().bUpdateByWhere(tableName, entity, consumer);
    }

    @Override
    public <T> Integer bUpdateByWhere(String tableName, Map<String, Object> rowData, GirAdvWhereFilter whereFilter) {
        return getAdvBaseUpdatePxyOpt().bUpdateByWhere(tableName, rowData, whereFilter);
    }

    @Override
    public <T> Integer bUpdateSelectiveByWhere(String tableName, Map<String, Object> rowData, GirAdvWhereFilter whereFilter) {
        return getAdvBaseUpdatePxyOpt().bUpdateSelectiveByWhere(tableName, rowData, whereFilter);
    }

    @Override
    public Integer bUpsert(String tableName, Map<String, Object> rowData, List<String> conflictKeys) {
        return getAdvBaseUpdatePxyOpt().bUpsert(tableName, rowData, conflictKeys);
    }

    @Override
    public <T> Integer bUpsert(T entity) {
        return getAdvBaseUpdatePxyOpt().bUpsert(entity);
    }

    @Override
    public <T> Integer bUpsert(T entity, UpdateStrategy strategy) {
        return getAdvBaseUpdatePxyOpt().bUpsert(entity, strategy);
    }

    @Override
    public <T> Integer bUpsert(T entity, Consumer<UpdateStrategy> strategyConsumer) {
        return getAdvBaseUpdatePxyOpt().bUpsert(entity, strategyConsumer);
    }

    @Override
    public <T> Integer bUpsertSelective(T entity) {
        return getAdvBaseUpdatePxyOpt().bUpsertSelective(entity);
    }

    @Override
    public <T> Integer bUpsertSelective(T entity, UpdateStrategy strategy) {
        return getAdvBaseUpdatePxyOpt().bUpsertSelective(entity, strategy);
    }

    @Override
    public <T> Integer bUpsertSelective(T entity, Consumer<UpdateStrategy> strategyConsumer) {
        return getAdvBaseUpdatePxyOpt().bUpsertSelective(entity, strategyConsumer);
    }

    @Override
    public void bUpsertBatch(String tableName, List<Map<String, Object>> rowsData, List<String> conflictKeys) {
        getAdvBaseUpdatePxyOpt().bUpsertBatch(tableName, rowsData, conflictKeys);
    }

    @Override
    public <T> void bUpsertBatch(Collection<T> entities, List<String> conflictKeys) {
        getAdvBaseUpdatePxyOpt().bUpsertBatch(entities, conflictKeys);
    }

    @Override
    public <T> void bUpsertBatch(Collection<T> entities, UpdateStrategy strategy) {
        getAdvBaseUpdatePxyOpt().bUpsertBatch(entities, strategy);
    }

    @Override
    public <T> void bUpsertBatch(Collection<T> entities, Consumer<UpdateStrategy> strategyConsumer) {
        getAdvBaseUpdatePxyOpt().bUpsertBatch(entities, strategyConsumer);
    }

    @Override
    public <T> void bUpsertBatchSelective(Collection<T> entities, List<String> conflictKeys) {
        getAdvBaseUpdatePxyOpt().bUpsertBatchSelective(entities, conflictKeys);
    }

    @Override
    public <T> void bUpsertBatchSelective(Collection<T> entities, UpdateStrategy strategy) {
        getAdvBaseUpdatePxyOpt().bUpsertBatchSelective(entities, strategy);
    }

    @Override
    public <T> void bUpsertBatchSelective(Collection<T> entities, Consumer<UpdateStrategy> strategyConsumer) {
        getAdvBaseUpdatePxyOpt().bUpsertBatchSelective(entities, strategyConsumer);
    }
}
