package cn.geoair.map.dynamic.adv.query.dialect.pg;

import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pg.base.PgAdvBaseAccessOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pg.base.PgAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pg.base.PgAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.pg.base.PgAdvBaseUpdateOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.ds.IDataSourceGetter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * PostgreSQL数据库的动态高级查询基础操作实现类
 * <p>
 * 实现了IAdvBaseOpt接口，通过组合方式复用各细分操作类（插入/查询/更新/删除）的实现，
 * 统一对外提供PostgreSQL数据库的全量基础操作，封装了代理对象的初始化和数据源注入。
 *
 * @author 张逢吉
 * @date 2025/10/9 10:16
 */
public class PgAdvBaseOpt implements IAdvBaseOpt {

    IDataSourceGetter dataSourceGetter;

    public PgAdvBaseOpt(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
    }

    /**
     * 插入操作代理对象
     */
    private PgAdvBaseAccessOpt pgAdvBaseAccessPxyOpt;

    /**
     * 查询操作代理对象
     */
    private PgAdvBaseSelectOpt pgAdvBaseSelectPxyOpt;

    /**
     * 更新操作代理对象
     */
    private PgAdvBaseUpdateOpt pgAdvBaseUpdatePxyOpt;

    /**
     * 删除操作代理对象
     */
    private PgAdvBaseDeleteOpt pgAdvBaseDeletePxyOpt;


    /**
     * 获取插入操作代理对象（懒加载+数据源注入）
     */
    private PgAdvBaseAccessOpt getPgAdvBaseAccessPxyOpt() {
        if (pgAdvBaseAccessPxyOpt == null) {
            pgAdvBaseAccessPxyOpt = new PgAdvBaseAccessOpt();
            pgAdvBaseAccessPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return pgAdvBaseAccessPxyOpt;
    }

    /**
     * 获取查询操作代理对象（懒加载+数据源注入）
     */
    private PgAdvBaseSelectOpt getPgAdvBaseSelectPxyOpt() {
        if (pgAdvBaseSelectPxyOpt == null) {
            pgAdvBaseSelectPxyOpt = new PgAdvBaseSelectOpt();
            pgAdvBaseSelectPxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return pgAdvBaseSelectPxyOpt;
    }

    /**
     * 获取更新操作代理对象（懒加载+数据源注入）
     */
    private PgAdvBaseUpdateOpt getPgAdvBaseUpdatePxyOpt() {
        if (pgAdvBaseUpdatePxyOpt == null) {
            pgAdvBaseUpdatePxyOpt = new PgAdvBaseUpdateOpt();
            pgAdvBaseUpdatePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return pgAdvBaseUpdatePxyOpt;
    }

    /**
     * 获取删除操作代理对象（懒加载+数据源注入）
     */
    private PgAdvBaseDeleteOpt getPgAdvBaseDeletePxyOpt() {
        if (pgAdvBaseDeletePxyOpt == null) {
            pgAdvBaseDeletePxyOpt = new PgAdvBaseDeleteOpt();
            pgAdvBaseDeletePxyOpt.setDataSourceGetter(dataSourceGetter);
        }
        return pgAdvBaseDeletePxyOpt;
    }

    // ==================== 插入操作实现（代理调用） ====================
    @Override
    public Integer bInsertBySql(String sqlStatement) {
        return getPgAdvBaseAccessPxyOpt().bInsertBySql(sqlStatement);
    }

    @Override
    public Integer bInsertBySql(String sqlStatement, Map<String, Object> param) {
        return getPgAdvBaseAccessPxyOpt().bInsertBySql(sqlStatement, param);
    }

    @Override
    public Integer bInsertOne(String tableName, Map<String, Object> rowData) {
        return getPgAdvBaseAccessPxyOpt().bInsertOne(tableName, rowData);
    }

    @Override
    public <T> Integer bInsertOne(String tableName, T entity) {
        return getPgAdvBaseAccessPxyOpt().bInsertOne(tableName, entity);
    }

    @Override
    public Long bInsertOneReturnId(String tableName, Map<String, Object> rowData) {
        return getPgAdvBaseAccessPxyOpt().bInsertOneReturnId(tableName, rowData);
    }

    @Override
    public <T> Long bInsertOneReturnId(String tableName, T entity) {
        return getPgAdvBaseAccessPxyOpt().bInsertOneReturnId(tableName, entity);
    }

    @Override
    public Integer bInsertBatch(String tableName, Set<String> headers, List<Map<String, Object>> rowsData) {
        return getPgAdvBaseAccessPxyOpt().bInsertBatch(tableName, headers, rowsData);
    }

    @Override
    public <T> Integer bInsertBatch(String tableName, Collection<T> entities) {
        return getPgAdvBaseAccessPxyOpt().bInsertBatch(tableName, entities);
    }

    @Override
    public Integer bInsertBatchWithBatchSize(String tableName, Set<String> headers, List<Map<String, Object>> rowsData, int batchSize) {
        return getPgAdvBaseAccessPxyOpt().bInsertBatchWithBatchSize(tableName, headers, rowsData, batchSize);
    }

    @Override
    public <T> Integer bInsertBatchWithBatchSize(String tableName, Collection<T> entities, int batchSize) {
        return getPgAdvBaseAccessPxyOpt().bInsertBatchWithBatchSize(tableName, entities, batchSize);
    }

    @Override
    public Integer bInsertIgnore(String tableName, Map<String, Object> rowData) {
        return getPgAdvBaseAccessPxyOpt().bInsertIgnore(tableName, rowData);
    }

    @Override
    public Integer bInsertIgnoreBatch(String tableName, Set<String> headers, List<Map<String, Object>> rowsData) {
        return getPgAdvBaseAccessPxyOpt().bInsertIgnoreBatch(tableName, headers, rowsData);
    }

    @Override
    public Integer bInsertOrUpdate(String tableName, Map<String, Object> rowData, Set<String> updateFields) {
        return getPgAdvBaseAccessPxyOpt().bInsertOrUpdate(tableName, rowData, updateFields);
    }

    // ==================== 删除操作实现（代理调用） ====================
    @Override
    public Integer bDeleteBySql(String sqlStatement) {
        return getPgAdvBaseDeletePxyOpt().bDeleteBySql(sqlStatement);
    }

    @Override
    public Integer bDeleteBySql(String sqlStatement, Map<String, Object> param) {
        return getPgAdvBaseDeletePxyOpt().bDeleteBySql(sqlStatement, param);
    }

    @Override
    public Integer bDeleteByPrimaryKey(String tableName, String idKey, Object id) {
        return getPgAdvBaseDeletePxyOpt().bDeleteByPrimaryKey(tableName, idKey, id);
    }

    @Override
    public Integer bDeleteBatchByPrimaryKey(String tableName, String idKey, Set<Object> ids) {
        return getPgAdvBaseDeletePxyOpt().bDeleteBatchByPrimaryKey(tableName, idKey, ids);
    }

    @Override
    public Integer bDeleteBatchWithBatchSize(String tableName, String idKey, Set<Object> ids, int batchSize) {
        return getPgAdvBaseDeletePxyOpt().bDeleteBatchWithBatchSize(tableName, idKey, ids, batchSize);
    }

    @Override
    public Integer bDeleteByCondition(String tableName, Map<String, Object> whereMap) {
        return getPgAdvBaseDeletePxyOpt().bDeleteByCondition(tableName, whereMap);
    }

    @Override
    public Integer bDeleteBatchByCondition(String tableName, Map<String, Object> whereMap, int batchSize) {
        return getPgAdvBaseDeletePxyOpt().bDeleteBatchByCondition(tableName, whereMap, batchSize);
    }

    @Override
    public Integer bLogicDelete(String tableName, String idKey, Object id, String deleteKey, Object deleteValue) {
        return getPgAdvBaseDeletePxyOpt().bLogicDelete(tableName, idKey, id, deleteKey, deleteValue);
    }

    @Override
    public Integer bLogicDeleteBatch(String tableName, String idKey, Set<Object> ids, String deleteKey, Object deleteValue) {
        return getPgAdvBaseDeletePxyOpt().bLogicDeleteBatch(tableName, idKey, ids, deleteKey, deleteValue);
    }

    @Override
    public Integer bSafeDeleteByCondition(String tableName, Map<String, Object> whereMap, int maxDelete) {
        return getPgAdvBaseDeletePxyOpt().bSafeDeleteByCondition(tableName, whereMap, maxDelete);
    }

    // ==================== 查询操作实现（代理调用） ====================
    @Override
    public GirAdvOneRow bSelectOne(String sql) {
        return getPgAdvBaseSelectPxyOpt().bSelectOne(sql);
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sql) {
        return getPgAdvBaseSelectPxyOpt().bSelectList(sql);
    }

    @Override
    public void bSelectList(String sql, Consumer<GirAdvOneRow> rowConsumer) {
        getPgAdvBaseSelectPxyOpt().bSelectList(sql, rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sql) {
        return getPgAdvBaseSelectPxyOpt().bSelectListToValueList(sql);
    }

    @Override
    public Number bSelectNumber(String sql) {
        return getPgAdvBaseSelectPxyOpt().bSelectNumber(sql);
    }

    @Override
    public Number bSelectRecordRowCount(String sql) {
        return getPgAdvBaseSelectPxyOpt().bSelectRecordRowCount(sql);
    }

    @Override
    public <E> E bSelectObjOne(String sql, Class<E> clazz) {
        return getPgAdvBaseSelectPxyOpt().bSelectObjOne(sql, clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String sql, Class<E> clazz) {
        return getPgAdvBaseSelectPxyOpt().bSelectObjList(sql, clazz);
    }

    @Override
    public <E> void bSelectObjList(String sql, Class<E> clazz, Consumer<E> rowConsumer) {
        getPgAdvBaseSelectPxyOpt().bSelectObjList(sql, clazz, rowConsumer);
    }

    @Override
    public GirAdvOneRow bSelectOne(String sqlStatement, Map<String, Object> sqlParam) {
        return getPgAdvBaseSelectPxyOpt().bSelectOne(sqlStatement, sqlParam);
    }

    @Override
    public List<GirAdvOneRow> bSelectList(String sqlStatement, Map<String, Object> sqlParam) {
        return getPgAdvBaseSelectPxyOpt().bSelectList(sqlStatement, sqlParam);
    }

    @Override
    public void bSelectList(String sqlStatement, Map<String, Object> sqlParam, Consumer<GirAdvOneRow> rowConsumer) {
        getPgAdvBaseSelectPxyOpt().bSelectList(sqlStatement, sqlParam, rowConsumer);
    }

    @Override
    public List<List<Object>> bSelectListToValueList(String sqlStatement, Map<String, Object> sqlParam) {
        return getPgAdvBaseSelectPxyOpt().bSelectListToValueList(sqlStatement, sqlParam);
    }

    @Override
    public Number bSelectNumber(String sqlStatement, Map<String, Object> sqlParam) {
        return getPgAdvBaseSelectPxyOpt().bSelectNumber(sqlStatement, sqlParam);
    }

    @Override
    public Number bSelectRecordRowCount(String sqlStatement, Map<String, Object> sqlParam) {
        return getPgAdvBaseSelectPxyOpt().bSelectRecordRowCount(sqlStatement, sqlParam);
    }

    @Override
    public <E> E bSelectObjOne(String sqlStatement, Map<String, Object> sqlParam, Class<E> clazz) {
        return getPgAdvBaseSelectPxyOpt().bSelectObjOne(sqlStatement, sqlParam, clazz);
    }

    @Override
    public <E> List<E> bSelectObjList(String sqlStatement, Map<String, Object> sqlParam, Class<E> clazz) {
        return getPgAdvBaseSelectPxyOpt().bSelectObjList(sqlStatement, sqlParam, clazz);
    }

    @Override
    public <E> void bSelectObjList(String sqlStatement, Map<String, Object> sqlParam, Class<E> clazz, Consumer<E> rowConsumer) {
        getPgAdvBaseSelectPxyOpt().bSelectObjList(sqlStatement, sqlParam, clazz, rowConsumer);
    }

    // ==================== 更新操作实现（代理调用） ====================
    @Override
    public Integer bUpdateBySql(String sqlStatement) {
        return getPgAdvBaseUpdatePxyOpt().bUpdateBySql(sqlStatement);
    }

    @Override
    public Integer bUpdateBySql(String sqlStatement, Map<String, Object> param) {
        return getPgAdvBaseUpdatePxyOpt().bUpdateBySql(sqlStatement, param);
    }

    @Override
    public Integer bUpdateByPrimaryKey(String tableName, String idKey, Object id, Map<String, Object> rowData) {
        return getPgAdvBaseUpdatePxyOpt().bUpdateByPrimaryKey(tableName, idKey, id, rowData);
    }

    @Override
    public <T> Integer bUpdateByPrimaryKey(String tableName, String idKey, T entity) {
        return getPgAdvBaseUpdatePxyOpt().bUpdateByPrimaryKey(tableName, idKey, entity);
    }

    @Override
    public Integer bUpdateByCondition(String tableName, Map<String, Object> rowData, Map<String, Object> whereMap) {
        return getPgAdvBaseUpdatePxyOpt().bUpdateByCondition(tableName, rowData, whereMap);
    }

    @Override
    public Integer bUpdateBatchByPrimaryKey(String tableName, String idKey, List<Map<String, Object>> rowsData) {
        return getPgAdvBaseUpdatePxyOpt().bUpdateBatchByPrimaryKey(tableName, idKey, rowsData);
    }

    @Override
    public Integer bUpdateBatchWithBatchSize(String tableName, String idKey, List<Map<String, Object>> rowsData, int batchSize) {
        return getPgAdvBaseUpdatePxyOpt().bUpdateBatchWithBatchSize(tableName, idKey, rowsData, batchSize);
    }

    @Override
    public <T> Integer bUpdateBatchByPrimaryKey(String tableName, String idKey, Collection<T> entities) {
        return getPgAdvBaseUpdatePxyOpt().bUpdateBatchByPrimaryKey(tableName, idKey, entities);
    }

    @Override
    public Integer bUpdateWithOptimisticLock(String tableName, String idKey, Object id, Map<String, Object> rowData, String versionKey, Integer version) {
        return getPgAdvBaseUpdatePxyOpt().bUpdateWithOptimisticLock(tableName, idKey, id, rowData, versionKey, version);
    }

    @Override
    public Integer bUpdateOrInsert(String tableName, Map<String, Object> rowData, Set<String> conflictKeys) {
        return getPgAdvBaseUpdatePxyOpt().bUpdateOrInsert(tableName, rowData, conflictKeys);
    }
}
