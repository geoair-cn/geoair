package cn.geoair.map.dynamic.adv.query;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.strategy.UpdateStrategy;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereLambdaFilter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 数据更新基础操作接口
 *
 * @author 张逢吉
 */
public interface IAdvBaseUpdateOpt extends IAdvConfigOpt {

    /** 设置数据源获取器 */
    void setDataSourceGetter(IDataSourceGetter dataSourceGetter);

    // ==================== 1. 自定义SQL更新 ====================

    Integer bUpdateBySql(String sqlStatement);

    Integer bUpdateBySql(String sqlStatement, SqlParamList sqlParam);

    Integer bUpdateBySql(String dynamicSql, SqlParamMap sqlParam);

    Integer bUpdateBySql(String sqlStatement, GirSqlParam sqlParam);

    // ==================== 2. 按主键更新 ====================

    /** 根据主键更新（Map格式） */
    Integer bUpdateByPK(String tableName, String idKey, Object id, Map<String, Object> rowData);

    /** 根据主键更新（实体格式，自动推断策略） */
    <T> Integer bUpdateByPK(T entity);

    /** 根据主键更新（实体格式，自定义策略） */
    <T> Integer bUpdateByPK(T entity, UpdateStrategy strategy);

    /** 根据主键更新（实体格式，Consumer方式配置策略） */
    <T> Integer bUpdateByPK(T entity, Consumer<UpdateStrategy> strategyConsumer);

    // 选择性更新（自动过滤null值）
    <T> Integer bUpdateByPKSelective(T entity);

    <T> Integer bUpdateByPKSelective(T entity, UpdateStrategy strategy);

    <T> Integer bUpdateByPKSelective(T entity, Consumer<UpdateStrategy> strategyConsumer);

    /** 批量按主键更新（Map格式） */
    void bUpdateBatchByPK(String tableName, String idKey, List<Map<String, Object>> rowsData);

    void bUpdateBatchByPK(
            String tableName, String idKey, List<Map<String, Object>> rowsData, int batchSize);

    /** 批量按主键更新（实体格式） */
    <T> void bUpdateBatchByPK(String tableName, String idKey, Collection<T> entities);

    <T> void bUpdateBatchByPK(Collection<T> entities, UpdateStrategy strategy);

    <T> void bUpdateBatchByPK(Collection<T> entities, Consumer<UpdateStrategy> strategyConsumer);

    /** 批量按主键选择性更新（自动过滤null值） */
    <T> void bUpdateBatchByPKSelective(String tableName, String idKey, Collection<T> entities);

    <T> void bUpdateBatchByPKSelective(Collection<T> entities, UpdateStrategy strategy);

    <T> void bUpdateBatchByPKSelective(
            Collection<T> entities, Consumer<UpdateStrategy> strategyConsumer);

    // ==================== 3. 简单条件更新 ====================

    /** 简单等值条件更新（Map格式） */
    Integer bUpdateByMap(
            String tableName, Map<String, Object> rowData, Map<String, Object> whereMap);

    // ==================== 4. 复杂条件更新 - Lambda表达式 ====================

    /** 复杂条件更新（实体格式，带策略和条件过滤器） */
    <T> Integer bUpdateByWhere(
            T entity, UpdateStrategy strategy, GirAdvWhereLambdaFilter<T> whereFilter);

    /** 复杂条件更新（实体格式，带策略和Consumer条件） */
    <T> Integer bUpdateByWhere(
            T entity, UpdateStrategy strategy, Consumer<GirAdvWhereLambdaFilter<T>> consumer);

    /** 复杂条件更新（实体格式，最简调用） */
    <T> Integer bUpdateByWhere(T entity, Consumer<GirAdvWhereLambdaFilter<T>> consumer);

    /** 复杂条件更新（实体格式，带策略配置） */
    <T> Integer bUpdateByWhere(
            T entity,
            Consumer<UpdateStrategy> strategyConsumer,
            Consumer<GirAdvWhereLambdaFilter<T>> consumer);

    /** 复杂条件更新（实体格式，指定表名） */
    <T> Integer bUpdateByWhere(
            String tableName, T entity, Consumer<GirAdvWhereLambdaFilter<T>> consumer);

    // ==================== 5. 复杂条件更新 - 传统Filter ====================

    /** 复杂条件更新（Map格式，传统Filter） */
    <T> Integer bUpdateByWhere(
            String tableName, Map<String, Object> rowData, GirAdvWhereFilter whereFilter);

    /** 复杂条件更新（Map格式，自动过滤空值） */
    <T> Integer bUpdateSelectiveByWhere(
            String tableName, Map<String, Object> rowData, GirAdvWhereFilter whereFilter);

    // ==================== 6. UPSERT操作 ====================
    // 单条 UPSERT
    Integer bUpsert(String tableName, Map<String, Object> rowData, List<String> conflictKeys);

    <T> Integer bUpsert(T entity);

    <T> Integer bUpsert(T entity, UpdateStrategy strategy);

    <T> Integer bUpsert(T entity, Consumer<UpdateStrategy> strategyConsumer);

    <T> Integer bUpsertSelective(T entity);

    <T> Integer bUpsertSelective(T entity, UpdateStrategy strategy);

    <T> Integer bUpsertSelective(T entity, Consumer<UpdateStrategy> strategyConsumer);

    // 批量 UPSERT
    Integer bUpsertBatch(
            String tableName,
            List<Map<String, Object>> rowsData,
            List<String> conflictKeys,
            int batchSize);

    <T> void bUpsertBatch(Collection<T> entities, List<String> conflictKeys);

    <T> void bUpsertBatch(Collection<T> entities, UpdateStrategy strategy);

    <T> void bUpsertBatch(Collection<T> entities, Consumer<UpdateStrategy> strategyConsumer);

    // 批量选择性 UPSERT（自动过滤null值）
    <T> void bUpsertBatchSelective(Collection<T> entities, List<String> conflictKeys);

    <T> void bUpsertBatchSelective(Collection<T> entities, UpdateStrategy strategy);

    <T> void bUpsertBatchSelective(
            Collection<T> entities, Consumer<UpdateStrategy> strategyConsumer);
}
