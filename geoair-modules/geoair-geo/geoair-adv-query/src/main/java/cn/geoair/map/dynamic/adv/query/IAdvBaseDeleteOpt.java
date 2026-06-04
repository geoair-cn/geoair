package cn.geoair.map.dynamic.adv.query;

import cn.geoair.comp.dynamic.ds.IDsDataSourceManger;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.strategy.DeleteStrategy;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereLambdaFilter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 数据删除基础操作接口
 *
 * @author 张逢吉
 */
public interface IAdvBaseDeleteOpt extends IAdvConfigOpt {

    /**
     * 设置数据源获取器
     */
    void setDataSourceGetter(IDsDataSourceManger dataSourceGetter);

    // ==================== 1. 自定义SQL删除 ====================

    Integer bDeleteBySql(String sqlStatement);

    Integer bDeleteBySql(String sqlStatement, SqlParamMap sqlParam);

    Integer bDeleteBySql(String sqlStatement, SqlParamList sqlParam);

    Integer bDeleteBySql(String sqlStatement, GirSqlParam sqlParam);

    // ==================== 2. 按主键删除 ====================

    /**
     * 根据主键删除（Map格式）
     */
    Integer bDeleteByPK(String tableName, String idKey, Object id);

    /**
     * 根据主键删除（实体格式，自动推断策略）
     */
    <T> Integer bDeleteByPK(T entity);

    /**
     * 根据主键删除（实体格式，自定义策略）
     */
    <T> Integer bDeleteByPK(T entity, DeleteStrategy strategy);

    /**
     * 根据主键删除（实体格式，Consumer方式配置策略）
     */
    <T> Integer bDeleteByPK(T entity, Consumer<DeleteStrategy> strategyConsumer);

    /**
     * 批量根据主键删除（主键集合）
     */
    Integer bDeleteByPKs(String tableName, String idKey, Set<Object> ids);

    Integer bDeleteByPKs(String tableName, String idKey, Set<Object> ids, int batchSize);

    /**
     * 批量根据主键删除（实体集合）
     */
    <T> void bDeleteBatchByPK(Collection<T> entities);

    <T> void bDeleteBatchByPK(Collection<T> entities, DeleteStrategy strategy);

    <T> void bDeleteBatchByPK(Collection<T> entities, Consumer<DeleteStrategy> strategyConsumer);

    // ==================== 3. 条件删除 ====================

    /**
     * 简单等值条件删除（Map格式）
     */
    Integer bDeleteByMap(String tableName, Map<String, Object> whereMap);

    Integer bDeleteByMap(String tableName, Map<String, Object> whereMap, int batchSize);

    // ==================== 4. 条件删除 - Lambda表达式 ====================

    /**
     * 复杂条件删除（Lambda表达式）
     */
    <T> Integer bDeleteByWhere(DeleteStrategy strategy, GirAdvWhereLambdaFilter<T> whereFilter);

    <T> Integer bDeleteByWhere(DeleteStrategy strategy, Consumer<GirAdvWhereLambdaFilter<T>> consumer);

    <T> Integer bDeleteByWhere(Consumer<GirAdvWhereLambdaFilter<T>> consumer);

    <T> Integer bDeleteByWhere(Consumer<DeleteStrategy> strategyConsumer, Consumer<GirAdvWhereLambdaFilter<T>> consumer);

    /**
     * 指定表名的条件删除
     */
    <T> Integer bDeleteByWhere(String tableName, Consumer<GirAdvWhereLambdaFilter<T>> consumer);

    // ==================== 5. 条件删除 - 传统Filter ====================

    /**
     * 复杂条件删除（传统Filter）
     */
    <T> Integer bDeleteByWhere(String tableName, GirAdvWhereFilter whereFilter);
}
