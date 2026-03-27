package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

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

    public AbstractPxyAdvBaseOpt(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
    }

    /** 插入操作代理对象 */
    protected IAdvBaseAccessOpt advBaseAccessPxyOpt;

    /** 查询操作代理对象 */
    protected IAdvBaseSelectOpt advBaseSelectPxyOpt;

    /** 更新操作代理对象 */
    protected IAdvBaseUpdateOpt advBaseUpdatePxyOpt;

    /** 删除操作代理对象 */
    protected IAdvBaseDeleteOpt advBaseDeletePxyOpt;

    public abstract IAdvBaseAccessOpt getAdvBaseAccessPxyOpt();

    public abstract IAdvBaseSelectOpt getAdvBaseSelectPxyOpt();

    public abstract IAdvBaseUpdateOpt getAdvBaseUpdatePxyOpt();

    public abstract IAdvBaseDeleteOpt getAdvBaseDeletePxyOpt();

    // ==================== 插入操作实现（代理调用） ====================
    @Override
    public Integer bInsertBySql(String sqlStatement) {
        return getAdvBaseAccessPxyOpt().bInsertBySql(sqlStatement);
    }

    @Override
    public Integer bInsertBySql(String sqlStatement, SqlParamMap sqlParam) {
        return getAdvBaseAccessPxyOpt().bInsertBySql(sqlStatement, sqlParam);
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
    public Long bInsertOneReturnId(String tableName, Map<String, Object> rowData) {
        return getAdvBaseAccessPxyOpt().bInsertOneReturnId(tableName, rowData);
    }

    @Override
    public <T> Long bInsertOneReturnId(String tableName, T entity) {
        return getAdvBaseAccessPxyOpt().bInsertOneReturnId(tableName, entity);
    }

    @Override
    public Integer bInsertBatch(
            String tableName, Set<String> headers, List<Map<String, Object>> rowsData) {
        return getAdvBaseAccessPxyOpt().bInsertBatch(tableName, headers, rowsData);
    }

    @Override
    public <T> Integer bInsertBatch(String tableName, Collection<T> entities) {
        return getAdvBaseAccessPxyOpt().bInsertBatch(tableName, entities);
    }

    @Override
    public Integer bInsertBatchWithBatchSize(
            String tableName,
            Set<String> headers,
            List<Map<String, Object>> rowsData,
            int batchSize) {
        return getAdvBaseAccessPxyOpt()
                .bInsertBatchWithBatchSize(tableName, headers, rowsData, batchSize);
    }

    @Override
    public <T> Integer bInsertBatchWithBatchSize(
            String tableName, Collection<T> entities, int batchSize) {
        return getAdvBaseAccessPxyOpt().bInsertBatchWithBatchSize(tableName, entities, batchSize);
    }

    @Override
    public Integer bInsertIgnore(String tableName, Map<String, Object> rowData) {
        return getAdvBaseAccessPxyOpt().bInsertIgnore(tableName, rowData);
    }

    @Override
    public Integer bInsertIgnoreBatch(
            String tableName, Set<String> headers, List<Map<String, Object>> rowsData) {
        return getAdvBaseAccessPxyOpt().bInsertIgnoreBatch(tableName, headers, rowsData);
    }

    @Override
    public Integer bInsertOrUpdate(
            String tableName, Map<String, Object> rowData, Set<String> updateFields) {
        return getAdvBaseAccessPxyOpt().bInsertOrUpdate(tableName, rowData, updateFields);
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
    public Integer bDeleteByPrimaryKey(String tableName, String idKey, Object id) {
        return getAdvBaseDeletePxyOpt().bDeleteByPrimaryKey(tableName, idKey, id);
    }

    @Override
    public Integer bDeleteBatchByPrimaryKey(String tableName, String idKey, Set<Object> ids) {
        return getAdvBaseDeletePxyOpt().bDeleteBatchByPrimaryKey(tableName, idKey, ids);
    }

    @Override
    public Integer bDeleteBatchWithBatchSize(
            String tableName, String idKey, Set<Object> ids, int batchSize) {
        return getAdvBaseDeletePxyOpt().bDeleteBatchWithBatchSize(tableName, idKey, ids, batchSize);
    }

    @Override
    public Integer bDeleteByCondition(String tableName, Map<String, Object> whereMap) {
        return getAdvBaseDeletePxyOpt().bDeleteByCondition(tableName, whereMap);
    }

    @Override
    public Integer bDeleteBatchByCondition(
            String tableName, Map<String, Object> whereMap, int batchSize) {
        return getAdvBaseDeletePxyOpt().bDeleteBatchByCondition(tableName, whereMap, batchSize);
    }

    @Override
    public Integer bLogicDelete(
            String tableName, String idKey, Object id, String deleteKey, Object deleteValue) {
        return getAdvBaseDeletePxyOpt().bLogicDelete(tableName, idKey, id, deleteKey, deleteValue);
    }

    @Override
    public Integer bLogicDeleteBatch(
            String tableName, String idKey, Set<Object> ids, String deleteKey, Object deleteValue) {
        return getAdvBaseDeletePxyOpt()
                .bLogicDeleteBatch(tableName, idKey, ids, deleteKey, deleteValue);
    }

    @Override
    public Integer bSafeDeleteByCondition(
            String tableName, Map<String, Object> whereMap, int maxDelete) {
        return getAdvBaseDeletePxyOpt().bSafeDeleteByCondition(tableName, whereMap, maxDelete);
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
    public void bSelectList(String sql, Consumer<GirAdvOneRow> rowConsumer) {
        getAdvBaseSelectPxyOpt().bSelectList(sql, rowConsumer);
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
    public <E> void bSelectObjList(String sql, Class<E> clazz, Consumer<E> rowConsumer) {
        getAdvBaseSelectPxyOpt().bSelectObjList(sql, clazz, rowConsumer);
    }

    @Override
    public GirAdvOneRow bSelectOne(String sqlStatement, SqlParamMap sqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectOne(sqlStatement, sqlParam);
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sqlStatement, SqlParamMap sqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectList(sqlStatement, sqlParam);
    }

    @Override
    public void bSelectList(
            String sqlStatement, SqlParamMap sqlParam, Consumer<GirAdvOneRow> rowConsumer) {
        getAdvBaseSelectPxyOpt().bSelectList(sqlStatement, sqlParam, rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sqlStatement, SqlParamMap sqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectListToValueList(sqlStatement, sqlParam);
    }

    @Override
    public Number bSelectNumber(String sqlStatement, SqlParamMap sqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectNumber(sqlStatement, sqlParam);
    }

    @Override
    public Number bSelectRecordRowCount(String sqlStatement, SqlParamMap sqlParam) {
        return getAdvBaseSelectPxyOpt().bSelectRecordRowCount(sqlStatement, sqlParam);
    }

    @Override
    public <E> E bSelectObjOne(String sqlStatement, SqlParamMap sqlParam, Class<E> clazz) {
        return getAdvBaseSelectPxyOpt().bSelectObjOne(sqlStatement, sqlParam, clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String sqlStatement, SqlParamMap sqlParam, Class<E> clazz) {
        return getAdvBaseSelectPxyOpt().bSelectObjList(sqlStatement, sqlParam, clazz);
    }

    @Override
    public <E> void bSelectObjList(
            String sqlStatement, SqlParamMap sqlParam, Class<E> clazz, Consumer<E> rowConsumer) {
        getAdvBaseSelectPxyOpt().bSelectObjList(sqlStatement, sqlParam, clazz, rowConsumer);
    }

    // ==================== 更新操作实现（代理调用） ====================
    @Override
    public Integer bUpdateBySql(String sqlStatement) {
        return getAdvBaseUpdatePxyOpt().bUpdateBySql(sqlStatement);
    }

    @Override
    public Integer bUpdateBySql(String sqlStatement, SqlParamMap sqlParam) {
        return getAdvBaseUpdatePxyOpt().bUpdateBySql(sqlStatement, sqlParam);
    }

    @Override
    public Integer bUpdateByPrimaryKey(
            String tableName, String idKey, Object id, Map<String, Object> rowData) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPrimaryKey(tableName, idKey, id, rowData);
    }

    @Override
    public <T> Integer bUpdateByPrimaryKey(String tableName, String idKey, T entity) {
        return getAdvBaseUpdatePxyOpt().bUpdateByPrimaryKey(tableName, idKey, entity);
    }

    @Override
    public Integer bUpdateByCondition(
            String tableName, Map<String, Object> rowData, Map<String, Object> whereMap) {
        return getAdvBaseUpdatePxyOpt().bUpdateByCondition(tableName, rowData, whereMap);
    }

    @Override
    public Integer bUpdateBatchByPrimaryKey(
            String tableName, String idKey, List<Map<String, Object>> rowsData) {
        return getAdvBaseUpdatePxyOpt().bUpdateBatchByPrimaryKey(tableName, idKey, rowsData);
    }

    @Override
    public Integer bUpdateBatchWithBatchSize(
            String tableName, String idKey, List<Map<String, Object>> rowsData, int batchSize) {
        return getAdvBaseUpdatePxyOpt()
                .bUpdateBatchWithBatchSize(tableName, idKey, rowsData, batchSize);
    }

    @Override
    public <T> Integer bUpdateBatchByPrimaryKey(
            String tableName, String idKey, Collection<T> entities) {
        return getAdvBaseUpdatePxyOpt().bUpdateBatchByPrimaryKey(tableName, idKey, entities);
    }

    @Override
    public Integer bUpdateWithOptimisticLock(
            String tableName,
            String idKey,
            Object id,
            Map<String, Object> rowData,
            String versionKey,
            Integer version) {
        return getAdvBaseUpdatePxyOpt()
                .bUpdateWithOptimisticLock(tableName, idKey, id, rowData, versionKey, version);
    }

    @Override
    public Integer bUpdateOrInsert(
            String tableName, Map<String, Object> rowData, Set<String> conflictKeys) {
        return getAdvBaseUpdatePxyOpt().bUpdateOrInsert(tableName, rowData, conflictKeys);
    }
}
