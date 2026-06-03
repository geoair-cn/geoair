package cn.geoair.map.dynamic.adv.query;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.strategy.AccessStrategy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 数据插入相关的基础操作接口
 *
 * @author 张逢吉
 */
public interface IAdvBaseAccessOpt extends IAdvConfigOpt {

    /**
     * 设置数据源获取器
     */
    void setDataSourceGetter(IDataSourceGetter dataSourceGetter);

    // ==================== 1. 自定义SQL插入 ====================

    Integer bInsertBySql(String sql);

    Integer bInsertBySql(String dynamicSql, SqlParamMap sqlParamMap);

    Integer bInsertBySql(String sqlStatement, SqlParamList sqlParamList);

    Integer bInsertBySql(String sqlStatementOrDynamicSql, GirSqlParam sqlParam);

    // ==================== 2. 单条插入 ====================

    /**
     * 插入单条Map数据
     */
    Integer bInsertOne(String tableName, Map<String, Object> rowData);

    /**
     * 插入单条实体数据（自动推断策略）
     */
    <T> Integer bInsertOne(T entity);

    /**
     * 插入单条实体数据（自定义策略）
     */
    <T> Integer bInsertOne(T entity, AccessStrategy strategy);

    /**
     * 插入单条实体数据（Consumer方式配置策略）
     */
    <T> Integer bInsertOne(T entity, Consumer<AccessStrategy> strategyConsumer);

    // ==================== 3. 选择性插入（自动过滤null） ====================

    /**
     * 选择性插入单条实体数据（自动过滤null值）
     */
    <T> Integer bInsertSelectiveOne(T entity);

    <T> Integer bInsertSelectiveOne(T entity, AccessStrategy strategy);

    <T> Integer bInsertSelectiveOne(T entity, Consumer<AccessStrategy> strategyConsumer);

    // ==================== 4. 批量插入 ====================

    /**
     * 批量插入Map数据
     */
    Integer bInsertBatch(String tableName, List<String> headers, List<Map<String, Object>> rowsData);

    Integer bInsertBatch(String tableName, List<String> headers, List<Map<String, Object>> rowsData, int batchSize);

    /**
     * 批量插入实体数据
     */
    <T> Integer bInsertBatch(Collection<T> entities);

    <T> Integer bInsertBatch(Collection<T> entities, AccessStrategy strategy);

    <T> Integer bInsertBatch(Collection<T> entities, Consumer<AccessStrategy> strategyConsumer);

    <T> Integer bInsertBatch(String tableName, Collection<T> entities);

    <T> Integer bInsertBatch(String tableName, Collection<T> entities, AccessStrategy strategy);

    <T> Integer bInsertBatch(String tableName, Collection<T> entities, Consumer<AccessStrategy> strategyConsumer);

    // ==================== 5. 插入或忽略 ====================

    /**
     * 插入或忽略单条Map数据
     */
    Integer bInsertIgnore(String tableName, Map<String, Object> rowData);

    Integer bInsertIgnore(String tableName, Map<String, Object> rowData, List<String> conflictKeys);

    /**
     * 插入或忽略单条实体数据
     */
    <T> Integer bInsertIgnore(T entity);

    <T> Integer bInsertIgnore(T entity, AccessStrategy strategy);

    <T> Integer bInsertIgnore(T entity, Consumer<AccessStrategy> strategyConsumer);

    // ==================== 6. 选择性插入或忽略 ====================

    /**
     * 选择性插入或忽略实体数据（自动过滤null值）
     */
    <T> Integer bInsertSelectiveIgnore(T entity);

    <T> Integer bInsertSelectiveIgnore(T entity, AccessStrategy strategy);

    <T> Integer bInsertSelectiveIgnore(T entity, Consumer<AccessStrategy> strategyConsumer);

    // ==================== 7. 批量插入或忽略 ====================

    /**
     * 批量插入或忽略Map数据
     */
    Integer bInsertIgnoreBatch(String tableName, Set<String> headers, List<Map<String, Object>> rowsData, List<String> conflictKeys);

    /**
     * 批量插入或忽略实体数据
     */
    <T> Integer bInsertIgnoreBatch(Collection<T> entities, List<String> conflictKeys);

    <T> Integer bInsertIgnoreBatch(Collection<T> entities, List<String> conflictKeys, AccessStrategy strategy);

    <T> Integer bInsertIgnoreBatch(Collection<T> entities, List<String> conflictKeys, Consumer<AccessStrategy> strategyConsumer);

    /**
     * 批量选择性插入或忽略实体数据（自动过滤null值）
     */
    <T> Integer bInsertSelectiveIgnoreBatch(Collection<T> entities, List<String> conflictKeys);

    <T> Integer bInsertSelectiveIgnoreBatch(Collection<T> entities, List<String> conflictKeys, AccessStrategy strategy);

    <T> Integer bInsertSelectiveIgnoreBatch(Collection<T> entities, List<String> conflictKeys, Consumer<AccessStrategy> strategyConsumer);
}
